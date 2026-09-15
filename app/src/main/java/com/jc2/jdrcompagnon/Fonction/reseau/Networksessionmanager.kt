package com.jc2.jdrcompagnon.network

import android.content.Context
import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.GameState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket

enum class SessionRole { NONE, HOST, PLAYER }

enum class PlayerConnectionState { DISCONNECTED, CONNECTING, CONNECTED, RECONNECTING }

/**
 * Source unique de vérité pour la session réseau local (hôte ou joueur).
 * Vit au niveau application (object) : survit à la navigation entre écrans
 * Compose et, tant que LanConnectionService tourne en foreground, au passage
 * de l'app en arrière-plan. Gère la reconnexion automatique côté joueur.
 */
object NetworkSessionManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _role = MutableStateFlow(SessionRole.NONE)
    val role: StateFlow<SessionRole> = _role.asStateFlow()

    // ── Côté hôte (MJ) ──
    private var gameServer: GameServer? = null
    private val _connectedClients = MutableStateFlow<List<ConnectedClient>>(emptyList())
    val connectedClients: StateFlow<List<ConnectedClient>> = _connectedClients.asStateFlow()
    private var clientsWatcherJob: Job? = null

    // Groupe de PJ associé à la partie hébergée : ses membres (memberIds)
    // forment le pool de personnages proposés aux joueurs à la connexion.
    private val _activeGroupId = MutableStateFlow<String?>(null)
    val activeGroupId: StateFlow<String?> = _activeGroupId.asStateFlow()

    // Personnage envoyé à chaque client (clientId -> characterId), pour
    // savoir quel personnage synchroniser quand le MJ le redemande.
    private val _sentCharacterByClient = MutableStateFlow<Map<String, String>>(emptyMap())
    val sentCharacterByClient: StateFlow<Map<String, String>> = _sentCharacterByClient.asStateFlow()

    // Réservations en cours (characterId -> clientId) : un personnage du
    // groupe réservé par un joueur n'est plus proposé aux autres. Libéré
    // automatiquement si ce joueur se déconnecte.
    private val _claimedCharacters = MutableStateFlow<Map<String, String>>(emptyMap())
    val claimedCharacters: StateFlow<Map<String, String>> = _claimedCharacters.asStateFlow()

    // Propositions de personnage en attente de validation du MJ (clientId -> personnage proposé)
    private val _pendingProposals = MutableStateFlow<Map<String, Character>>(emptyMap())
    val pendingProposals: StateFlow<Map<String, Character>> = _pendingProposals.asStateFlow()

    /**
     * @param groupId Groupe de PJ dont les membres seront proposés aux
     * joueurs qui se connectent. Si null, aucun personnage n'est proposé
     * automatiquement (les joueurs pourront quand même proposer un des
     * leurs).
     * @param campaignTitle Si fourni, utilisé comme nom du service réseau
     * (visible des joueurs lors de la découverte) à la place de
     * "MJ-{hostDisplayName}".
     */
    fun startHosting(
        context: Context,
        hostDisplayName: String,
        groupId: String? = null,
        campaignTitle: String? = null,
    ) {
        if (_role.value == SessionRole.HOST) return
        stopEverythingInternal()
        _activeGroupId.value = groupId
        _claimedCharacters.value = emptyMap()
        _pendingProposals.value = emptyMap()
        val appContext = context.applicationContext
        val server = GameServer(
            context = appContext,
            hostDisplayName = hostDisplayName,
            serviceName = campaignTitle?.takeIf { it.isNotBlank() } ?: "MJ-$hostDisplayName"
        )
        server.onMessageReceived = { fromClientId, message -> handleHostIncomingMessage(fromClientId, message) }
        server.onClientConnected = { client -> sendAvailableCharacters(client.id) }
        server.onClientDisconnected = { clientId -> releaseCharactersHeldBy(clientId) }
        gameServer = server
        server.start()
        _role.value = SessionRole.HOST
        clientsWatcherJob = scope.launch {
            // GameServer.connectedClients est déjà un StateFlow : on le relaie tel quel.
            server.connectedClients.collect { _connectedClients.value = it }
        }
        LanConnectionService.ensureStarted(appContext, "Partie hébergée sur le réseau local")
    }

    /** Envoie un personnage complet à un client connecté (envoi ponctuel). */
    fun pushCharacterToClient(clientId: String, character: Character) {
        val message = NetworkMessage(type = NetworkMessage.TYPE_CHARACTER_PUSH, character = character)
        gameServer?.sendToClient(clientId, networkJson.encodeToString(message))
        _sentCharacterByClient.value = _sentCharacterByClient.value + (clientId to character.id)
    }

    /** Demande au client de renvoyer l'état actuel du personnage qu'on lui a envoyé. */
    fun requestCharacterSync(clientId: String) {
        val characterId = _sentCharacterByClient.value[clientId] ?: return
        val message = NetworkMessage(type = NetworkMessage.TYPE_CHARACTER_SYNC_REQUEST, characterId = characterId)
        gameServer?.sendToClient(clientId, networkJson.encodeToString(message))
    }

    /**
     * Change le groupe actif pendant que le serveur tourne déjà (ex. depuis
     * l'écran serveur, sans repasser par le tableau de bord MJ). Les
     * réservations déjà faites sont conservées ; la liste dispo est
     * recalculée et rediffusée à tous les joueurs connectés.
     */
    fun setActiveGroup(groupId: String?) {
        _activeGroupId.value = groupId
        broadcastAvailableCharacters()
    }

    /** Accepte la proposition de personnage d'un joueur : l'ajoute au roster du MJ et le réserve pour lui. */
    fun acceptProposal(clientId: String) {
        val character = _pendingProposals.value[clientId] ?: return
        GameState.upsertCharacterFromNetwork(character)
        _claimedCharacters.value = _claimedCharacters.value + (character.id to clientId)
        _pendingProposals.value = _pendingProposals.value - clientId
        gameServer?.sendToClient(
            clientId,
            networkJson.encodeToString(NetworkMessage(type = NetworkMessage.TYPE_CHARACTER_PROPOSAL_RESULT, accepted = true))
        )
        broadcastAvailableCharacters()
    }

    /** Refuse la proposition de personnage d'un joueur. */
    fun rejectProposal(clientId: String) {
        _pendingProposals.value = _pendingProposals.value - clientId
        gameServer?.sendToClient(
            clientId,
            networkJson.encodeToString(NetworkMessage(type = NetworkMessage.TYPE_CHARACTER_PROPOSAL_RESULT, accepted = false))
        )
    }

    private fun sendAvailableCharacters(clientId: String) {
        val message = NetworkMessage(type = NetworkMessage.TYPE_AVAILABLE_CHARACTERS, characters = availableCharacterSummaries())
        gameServer?.sendToClient(clientId, networkJson.encodeToString(message))
    }

    private fun broadcastAvailableCharacters() {
        val message = NetworkMessage(type = NetworkMessage.TYPE_AVAILABLE_CHARACTERS, characters = availableCharacterSummaries())
        gameServer?.sendToAll(networkJson.encodeToString(message))
    }

    private fun availableCharacterSummaries(): List<CharacterSummary> {
        val groupId = _activeGroupId.value ?: return emptyList()
        val group = GameState.mjGroups.value.find { it.id == groupId } ?: return emptyList()
        val claimedIds = _claimedCharacters.value.keys
        return GameState.characters.value
            .filter { it.id in group.memberIds && it.id !in claimedIds }
            .map { CharacterSummary(it.id, it.name, it.race, it.characterClass, it.level) }
    }

    private fun releaseCharactersHeldBy(clientId: String) {
        val hadClaim = _claimedCharacters.value.any { it.value == clientId }
        _claimedCharacters.value = _claimedCharacters.value.filterValues { it != clientId }
        _pendingProposals.value = _pendingProposals.value - clientId
        if (hadClaim) broadcastAvailableCharacters()
    }

    private fun handleHostIncomingMessage(fromClientId: String, message: NetworkMessage) {
        when (message.type) {
            NetworkMessage.TYPE_CHARACTER_SYNC_RESPONSE -> {
                message.character?.let { GameState.upsertCharacterFromNetwork(it) }
            }
            NetworkMessage.TYPE_CHARACTER_CLAIM_REQUEST -> {
                val characterId = message.characterId ?: return
                val alreadyClaimed = _claimedCharacters.value.containsKey(characterId)
                val character = GameState.characters.value.find { it.id == characterId }
                if (!alreadyClaimed && character != null) {
                    _claimedCharacters.value = _claimedCharacters.value + (characterId to fromClientId)
                    gameServer?.sendToClient(
                        fromClientId,
                        networkJson.encodeToString(
                            NetworkMessage(type = NetworkMessage.TYPE_CHARACTER_CLAIM_RESPONSE, accepted = true, character = character)
                        )
                    )
                    broadcastAvailableCharacters()
                } else {
                    gameServer?.sendToClient(
                        fromClientId,
                        networkJson.encodeToString(NetworkMessage(type = NetworkMessage.TYPE_CHARACTER_CLAIM_RESPONSE, accepted = false))
                    )
                }
            }
            NetworkMessage.TYPE_CHARACTER_PROPOSAL -> {
                message.character?.let { proposed ->
                    _pendingProposals.value = _pendingProposals.value + (fromClientId to proposed)
                }
            }
        }
    }

    fun stopHosting(context: Context) {
        clientsWatcherJob?.cancel()
        gameServer?.stop()
        gameServer = null
        _activeGroupId.value = null
        _connectedClients.value = emptyList()
        _sentCharacterByClient.value = emptyMap()
        _claimedCharacters.value = emptyMap()
        _pendingProposals.value = emptyMap()
        if (_role.value == SessionRole.HOST) _role.value = SessionRole.NONE
        LanConnectionService.stopIfIdle(context.applicationContext)
    }

    // ── Côté joueur ──
    private var discoveryInstance: GameClientDiscovery? = null
    private var currentSocket: Socket? = null
    private var readJob: Job? = null
    private var reconnectJob: Job? = null
    private var manualDisconnect = false
    private var pendingProposalCharacterId: String? = null

    private val _playerState = MutableStateFlow(PlayerConnectionState.DISCONNECTED)
    val playerState: StateFlow<PlayerConnectionState> = _playerState.asStateFlow()

    // Dernier message d'erreur de connexion (ex. "Connection timed out",
    // "Connection refused") pour ne plus laisser le joueur dans le noir
    // quand la connexion échoue silencieusement en boucle.
    private val _lastConnectionError = MutableStateFlow<String?>(null)
    val lastConnectionError: StateFlow<String?> = _lastConnectionError.asStateFlow()

    // Personnages du groupe encore disponibles, envoyés par le MJ.
    private val _availableCharacters = MutableStateFlow<List<CharacterSummary>>(emptyList())
    val availableCharacters: StateFlow<List<CharacterSummary>> = _availableCharacters.asStateFlow()

    // Id du personnage que ce joueur incarne une fois réservé/accepté.
    private val _claimedCharacterId = MutableStateFlow<String?>(null)
    val claimedCharacterId: StateFlow<String?> = _claimedCharacterId.asStateFlow()

    // Statut de la proposition en cours (null si aucune, true si en attente).
    private val _proposalPending = MutableStateFlow(false)
    val proposalPending: StateFlow<Boolean> = _proposalPending.asStateFlow()

    /** Instance partagée de découverte, créée à la demande. */
    fun discovery(context: Context): GameClientDiscovery {
        return discoveryInstance ?: GameClientDiscovery(context.applicationContext).also {
            discoveryInstance = it
        }
    }

    fun connectToServer(context: Context, server: DiscoveredServer, playerName: String) {
        val appContext = context.applicationContext
        manualDisconnect = false
        _role.value = SessionRole.PLAYER
        _availableCharacters.value = emptyList()
        _claimedCharacterId.value = null
        _proposalPending.value = false
        pendingProposalCharacterId = null
        reconnectJob?.cancel()
        readJob?.cancel()
        _playerState.value = PlayerConnectionState.CONNECTING
        _lastConnectionError.value = null
        scope.launch {
            try {
                val socket = discovery(appContext).connect(server, playerName)
                currentSocket = socket
                _playerState.value = PlayerConnectionState.CONNECTED
                LanConnectionService.ensureStarted(appContext, "Connecté à la partie du MJ")
                listenUntilDisconnected(appContext, socket, server, playerName)
            } catch (e: Exception) {
                _lastConnectionError.value = e.message ?: e.javaClass.simpleName
                scheduleReconnect(appContext, server, playerName, attempt = 1)
            }
        }
    }

    /** Demande à réserver un personnage disponible du groupe. */
    fun requestClaimCharacter(characterId: String) {
        val socket = currentSocket ?: return
        val message = NetworkMessage(type = NetworkMessage.TYPE_CHARACTER_CLAIM_REQUEST, characterId = characterId)
        runCatching {
            PrintWriter(socket.getOutputStream(), true).println(networkJson.encodeToString(message))
        }
    }

    /** Propose un des personnages de son propre appareil (aucun du groupe n'était disponible). */
    fun proposeCharacter(character: Character) {
        val socket = currentSocket ?: return
        pendingProposalCharacterId = character.id
        _proposalPending.value = true
        val message = NetworkMessage(type = NetworkMessage.TYPE_CHARACTER_PROPOSAL, character = character)
        runCatching {
            PrintWriter(socket.getOutputStream(), true).println(networkJson.encodeToString(message))
        }
    }

    private fun listenUntilDisconnected(
        context: Context,
        socket: Socket,
        server: DiscoveredServer,
        playerName: String,
    ) {
        readJob = scope.launch {
            try {
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                var line = reader.readLine()
                while (line != null) {
                    runCatching {
                        val message = networkJson.decodeFromString<NetworkMessage>(line)
                        handlePlayerIncomingMessage(socket, message)
                    }
                    line = reader.readLine()
                }
            } catch (e: Exception) {
                // Connexion perdue (Wi-Fi coupé, MJ fermé, etc.)
            } finally {
                currentSocket = null
                if (manualDisconnect) {
                    _playerState.value = PlayerConnectionState.DISCONNECTED
                    LanConnectionService.stopIfIdle(context)
                } else {
                    scheduleReconnect(context, server, playerName, attempt = 1)
                }
            }
        }
    }

    private fun handlePlayerIncomingMessage(socket: Socket, message: NetworkMessage) {
        when (message.type) {
            NetworkMessage.TYPE_CHARACTER_PUSH -> {
                message.character?.let { GameState.upsertCharacterFromNetwork(it) }
            }
            NetworkMessage.TYPE_AVAILABLE_CHARACTERS -> {
                _availableCharacters.value = message.characters ?: emptyList()
            }
            NetworkMessage.TYPE_CHARACTER_CLAIM_RESPONSE -> {
                if (message.accepted == true && message.character != null) {
                    GameState.upsertCharacterFromNetwork(message.character)
                    _claimedCharacterId.value = message.character.id
                }
                // Si refusé : le joueur reste sur la liste, mise à jour via le prochain TYPE_AVAILABLE_CHARACTERS.
            }
            NetworkMessage.TYPE_CHARACTER_PROPOSAL_RESULT -> {
                _proposalPending.value = false
                if (message.accepted == true) {
                    _claimedCharacterId.value = pendingProposalCharacterId
                }
                pendingProposalCharacterId = null
            }
            NetworkMessage.TYPE_CHARACTER_SYNC_REQUEST -> {
                val characterId = message.characterId ?: return
                val character = GameState.characters.value.find { it.id == characterId } ?: return
                val response = NetworkMessage(type = NetworkMessage.TYPE_CHARACTER_SYNC_RESPONSE, character = character)
                runCatching {
                    PrintWriter(socket.getOutputStream(), true).println(networkJson.encodeToString(response))
                }
            }
        }
    }

    private fun scheduleReconnect(
        context: Context,
        server: DiscoveredServer,
        playerName: String,
        attempt: Int,
    ) {
        if (manualDisconnect) return
        _playerState.value = PlayerConnectionState.RECONNECTING
        reconnectJob = scope.launch {
            val delayMs = (2_000L * attempt).coerceAtMost(15_000L)
            delay(delayMs)
            if (manualDisconnect) return@launch
            // On retente avec la version la plus fraîche connue de ce serveur
            // (le MJ a pu relancer son serveur entre-temps, ce qui change le
            // port) plutôt que de rejouer indéfiniment l'IP/port d'origine,
            // potentiellement périmés — cause probable d'un échec de
            // reconnexion qui semble ne jamais aboutir.
            val target = discovery(context).servers.value
                .find { it.serviceName == server.serviceName } ?: server
            try {
                val socket = discovery(context).connect(target, playerName)
                currentSocket = socket
                _playerState.value = PlayerConnectionState.CONNECTED
                listenUntilDisconnected(context, socket, target, playerName)
            } catch (e: Exception) {
                _lastConnectionError.value = e.message ?: e.javaClass.simpleName
                scheduleReconnect(context, target, playerName, attempt + 1)
            }
        }
    }

    /** Déconnexion volontaire du joueur : coupe aussi les tentatives de reconnexion. */
    fun disconnect(context: Context) {
        manualDisconnect = true
        reconnectJob?.cancel()
        readJob?.cancel()
        currentSocket?.let { runCatching { it.close() } }
        currentSocket = null
        _playerState.value = PlayerConnectionState.DISCONNECTED
        if (_role.value == SessionRole.PLAYER) _role.value = SessionRole.NONE
        LanConnectionService.stopIfIdle(context.applicationContext)
    }

    private fun stopEverythingInternal() {
        clientsWatcherJob?.cancel()
        gameServer?.stop()
        gameServer = null
        reconnectJob?.cancel()
        readJob?.cancel()
        currentSocket?.let { runCatching { it.close() } }
        currentSocket = null
    }
}