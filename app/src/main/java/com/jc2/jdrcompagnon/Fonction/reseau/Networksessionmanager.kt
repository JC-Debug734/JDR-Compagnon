package com.jc2.jdrcompagnon.network

import android.content.Context
import android.net.Uri
import com.jc2.jdrcompagnon.feature_combat.domain.model.Combattant
import com.jc2.jdrcompagnon.feature_combat.domain.model.Distance
import com.jc2.jdrcompagnon.feature_combat.presentation.CombatSession
import com.jc2.jdrcompagnon.feature_combat.presentation.DeclarationAction
import com.jc2.jdrcompagnon.feature_combat.presentation.JetCombat
import com.jc2.jdrcompagnon.feature_epreuve.presentation.EpreuveActive
import com.jc2.jdrcompagnon.feature_epreuve.presentation.EpreuveOutilSession
import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.ScenarioClockData
import com.jc2.jdrcompagnon.ui.ScenarioClockState
import com.jc2.jdrcompagnon.ui.components.weatherLabel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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

    init {
        // Rediffuse l'horloge de scénario à tous les joueurs à chaque changement côté MJ
        // (toggle depuis la fenêtre de gestion, ou tic du défilement automatique). Sans effet
        // si aucun serveur n'est démarré (sendToAll no-op sur gameServer null), donc sans danger
        // à appeler aussi côté joueur ou hors hébergement.
        ScenarioClockState.setOnLocalChangeListener { state -> broadcastTimeState(state) }

        // Renvoie automatiquement au MJ toute modification de la fiche que ce
        // joueur incarne (édition, dégâts, XP...), sans attendre une demande
        // de resynchro explicite du MJ. Sans effet hors connexion joueur
        // (currentSocket null → l'envoi échoue silencieusement via runCatching).
        scope.launch {
            combine(_claimedCharacterId, GameState.characters) { claimedId, characters ->
                claimedId?.let { id -> characters.find { it.id == id } }
            }
                .filterNotNull()
                .distinctUntilChanged()
                .collect { character ->
                    if (_role.value == SessionRole.PLAYER) sendCharacterUpdateToHost(character)
                }
        }

        // Rediffuse immédiatement au joueur concerné toute modification faite côté MJ sur un
        // personnage qu'un client a en charge (ex. ajustements rapides XP/or/fatigue depuis le
        // tiroir MJ, ou toute autre édition), symétrique au relais joueur → MJ ci-dessus. Sans
        // effet hors hébergement (sendToClient no-op sur gameServer null).
        scope.launch {
            combine(_claimedCharacters, GameState.characters) { claims, characters ->
                claims.mapNotNull { (characterId, clientId) ->
                    characters.find { it.id == characterId }?.let { clientId to it }
                }
            }
                .distinctUntilChanged()
                .collect { pairs ->
                    if (_role.value == SessionRole.HOST) {
                        // Ne repousse que les fiches réellement modifiées depuis le dernier envoi à
                        // ce client : sinon la fiche venait d'être poussée juste après la réponse de
                        // réservation (et parfois avant elle), écrasant la version locale du joueur
                        // avant qu'il ait pu comparer ou proposer la sienne.
                        pairs.forEach { (clientId, character) ->
                            if (lastCharacterSentToClient[clientId] != character) pushCharacterToClient(clientId, character)
                        }
                    }
                }
        }

        // Rediffuse aux joueurs l'épreuve en cours de l'outil ÉPREUVES (réussites obtenues)
        // à chaque changement côté MJ.
        scope.launch {
            EpreuveOutilSession.etat
                .map { it?.toJoueurData() }
                .distinctUntilChanged()
                .collect { data ->
                    if (_role.value == SessionRole.HOST) {
                        val message = NetworkMessage(type = NetworkMessage.TYPE_EPREUVE_STATE, epreuve = data)
                        gameServer?.sendToAll(networkJson.encodeToString(message))
                    }
                }
        }
    }

    // ── Carte de campagne ouverte par le MJ en mode exploration (TYPE_MAP_OPENED) ──

    // Côté MJ : renseignée par l'écran de carte (CarteCampagneScreen) tant qu'il est en exploration.
    private val _carteOuverteMj = MutableStateFlow<CarteOuverteData?>(null)

    // Côté joueur : dernière carte ouverte par le MJ ; NavGraph y amène le joueur.
    private val _carteOuverteJoueur = MutableStateFlow<CarteOuverteData?>(null)
    val carteOuverteJoueur: StateFlow<CarteOuverteData?> = _carteOuverteJoueur.asStateFlow()

    /** MJ : carte affichée en mode exploration (null = quittée), diffusée aux joueurs connectés. */
    fun signalerCarteOuverte(carte: CarteOuverteData?) {
        if (_carteOuverteMj.value == carte) return
        _carteOuverteMj.value = carte
        if (_role.value == SessionRole.HOST) {
            gameServer?.sendToAll(networkJson.encodeToString(NetworkMessage(type = NetworkMessage.TYPE_MAP_OPENED, carteOuverte = carte)))
        }
    }

    /**
     * MJ : l'écran de [carte] se ferme (ou quitte l'exploration). Sans effet si une autre carte a
     * déjà été signalée entre-temps (changement de carte : le nouvel écran s'affiche avant que
     * l'ancien ne disparaisse).
     */
    fun quitterCarteOuverte(carte: CarteOuverteData) {
        if (_carteOuverteMj.value == carte) signalerCarteOuverte(null)
    }

    private fun sendCarteOuverteTo(clientId: String) {
        val carte = _carteOuverteMj.value ?: return
        gameServer?.sendToClient(clientId, networkJson.encodeToString(NetworkMessage(type = NetworkMessage.TYPE_MAP_OPENED, carteOuverte = carte)))
    }

    private fun sendTraceTo(clientId: String) {
        val data = com.jc2.jdrcompagnon.feature_carte.presentation.TraceCarteSession.etat.value ?: return
        gameServer?.sendToClient(clientId, networkJson.encodeToString(NetworkMessage(type = NetworkMessage.TYPE_MAP_TRACE, trace = data)))
    }

    private fun sendVoyageStateTo(clientId: String) {
        val data = com.jc2.jdrcompagnon.feature_carte.presentation.VoyageSession.etat.value ?: return
        gameServer?.sendToClient(clientId, networkJson.encodeToString(NetworkMessage(type = NetworkMessage.TYPE_VOYAGE_STATE, voyage = data)))
    }

    /** Groupe actif vu côté joueur (reçu via TYPE_GROUP_INFO) ; [details] : vue complète du groupe. */
    data class NetworkGroupInfo(val name: String, val members: List<CharacterSummary>, val details: GroupeJoueurData? = null)

    private fun activeGroupInfoMessage(): NetworkMessage {
        val group = _activeGroupId.value?.let { id -> GameState.mjGroups.value.find { it.id == id } }
        val members = group?.let { g ->
            GameState.characters.value
                .filter { it.id in g.memberIds && it.type == "PJ" }
                .map { CharacterSummary(it.id, it.name, it.race, it.characterClass, it.level) }
        }
        return NetworkMessage(
            type = NetworkMessage.TYPE_GROUP_INFO,
            groupName = group?.name,
            characters = members,
            groupe = group?.let { com.jc2.jdrcompagnon.feature_group.domain.GroupeJoueur.depuis(it, GameState.characters.value) },
        )
    }

    // ── Campagne et cartes partagées (voir CarteSyncReseau) ──
    private var hostContext: Context? = null
    private var campaignWatcherJob: Job? = null
    private var dernierEtatCampagne: Pair<List<com.jc2.jdrcompagnon.feature_carte.domain.model.CarteCampagne>, CampagneJoueurData>? = null
    // Signature de la dernière image envoyée à tous, par carte.
    private val imagesEnvoyees = java.util.concurrent.ConcurrentHashMap<String, String>()

    private fun campaignStateMessage(): NetworkMessage =
        NetworkMessage(type = NetworkMessage.TYPE_CAMPAIGN_STATE, campagne = dernierEtatCampagne?.second)

    /** Nouveau client : état de la campagne puis toutes les images de ses cartes. */
    private fun sendCampaignStateTo(clientId: String) {
        val context = hostContext ?: return
        scope.launch {
            gameServer?.sendToClient(clientId, networkJson.encodeToString(campaignStateMessage()))
            dernierEtatCampagne?.first?.forEach { carte ->
                com.jc2.jdrcompagnon.feature_carte.data.CarteSyncReseau.imageEncodee(context, carte)?.let { image ->
                    gameServer?.sendToClient(clientId, networkJson.encodeToString(NetworkMessage(type = NetworkMessage.TYPE_MAP_IMAGE, carteImage = image)))
                }
            }
            dernierEtatCampagne?.second?.imagesVilles?.keys?.forEach { villeId ->
                com.jc2.jdrcompagnon.feature_carte.data.CarteSyncReseau.imageVilleEncodee(context, villeId)?.let { image ->
                    gameServer?.sendToClient(clientId, networkJson.encodeToString(NetworkMessage(type = NetworkMessage.TYPE_CITY_IMAGE, villeImage = image)))
                }
            }
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private fun startCampaignWatcher(context: Context) {
        val repo = com.jc2.jdrcompagnon.di.CarteDependencies.repository
        imagesEnvoyees.clear()
        campaignWatcherJob = scope.launch {
            combine(
                GameState.currentCampaignId,
                GameState.mjCampaigns,
                com.jc2.jdrcompagnon.feature_carte.data.CarteGrillePrefs.version,
                // Image de ville ajoutée/retirée : simple fichier, à surveiller en plus de la base.
                com.jc2.jdrcompagnon.feature_carte.data.VilleImageStore.version,
                // Réglage d'affichage d'un lieu (nom caché, taille d'icône) : rediffusé aux joueurs.
                com.jc2.jdrcompagnon.feature_carte.data.CartePointPrefs.version,
            ) { id, campagnes, _, _, _ ->
                campagnes.firstOrNull { it.id == id }
            }
                .flatMapLatest { campagne ->
                    if (campagne == null) kotlinx.coroutines.flow.flowOf(null)
                    else combine(
                        repo.observerCartes(campagne.id),
                        repo.observerPoints(campagne.id),
                        com.jc2.jdrcompagnon.di.BoutiqueDependencies.repository.observerToutesLesBoutiques(),
                        repo.observerLieuxNotablesCampagne(campagne.id),
                    ) { cartes, points, boutiques, lieux ->
                        cartes to com.jc2.jdrcompagnon.feature_carte.data.CarteSyncReseau.etatCampagne(context, campagne.id, campagne.title, cartes, points, boutiques, lieux)
                    }
                }
                .distinctUntilChanged { a, b -> a?.second == b?.second }
                .collect { etat ->
                    dernierEtatCampagne = etat
                    gameServer?.sendToAll(networkJson.encodeToString(campaignStateMessage()))
                    // Images : seulement celles qui ont changé depuis le dernier envoi.
                    etat?.first?.forEach { carte ->
                        val signature = com.jc2.jdrcompagnon.feature_carte.data.CarteSyncReseau.signatureImage(context, carte) ?: return@forEach
                        if (imagesEnvoyees[carte.id] == signature) return@forEach
                        com.jc2.jdrcompagnon.feature_carte.data.CarteSyncReseau.imageEncodee(context, carte)?.let { image ->
                            gameServer?.sendToAll(networkJson.encodeToString(NetworkMessage(type = NetworkMessage.TYPE_MAP_IMAGE, carteImage = image)))
                            imagesEnvoyees[carte.id] = signature
                        }
                    }
                    etat?.second?.imagesVilles?.forEach { (villeId, signature) ->
                        val cle = "ville:$villeId"
                        if (imagesEnvoyees[cle] == signature) return@forEach
                        com.jc2.jdrcompagnon.feature_carte.data.CarteSyncReseau.imageVilleEncodee(context, villeId)?.let { image ->
                            gameServer?.sendToAll(networkJson.encodeToString(NetworkMessage(type = NetworkMessage.TYPE_CITY_IMAGE, villeImage = image)))
                            imagesEnvoyees[cle] = signature
                        }
                    }
                }
        }
    }

    /** MJ : le groupe fait halte pour un repos long ; chaque joueur connecté peut le prendre. */
    fun proposerReposLong(message: String) {
        gameServer?.sendToAll(networkJson.encodeToString(NetworkMessage(type = NetworkMessage.TYPE_REST_OFFER, reposMessage = message)))
    }

    private fun sendGroupInfoTo(clientId: String) {
        gameServer?.sendToClient(clientId, networkJson.encodeToString(activeGroupInfoMessage()))
    }

    private fun questsStateMessage(): NetworkMessage = NetworkMessage(
        type = NetworkMessage.TYPE_QUESTS_STATE,
        quests = com.jc2.jdrcompagnon.feature_quete.domain.QuetesJoueur.locales(),
    )

    private fun sendQuestsStateTo(clientId: String) {
        gameServer?.sendToClient(clientId, networkJson.encodeToString(questsStateMessage()))
    }

    // Vue joueur de l'épreuve de l'outil ÉPREUVES : réussites obtenues sur réussites requises
    // (les complications restent annoncées par le MJ).
    private fun EpreuveActive.toJoueurData() = EpreuveJoueurData(
        id = epreuve.id,
        nom = epreuve.nom,
        type = "Épreuve",
        description = epreuve.description,
        competences = emptyList(),
        progres = reussites.coerceAtMost(epreuve.reussitesRequises),
        progresMax = epreuve.reussitesRequises,
        issue = if (terminee) (if (reussie) "Réussite" else if (echouee) "Échec" else "Épreuve arrêtée") else null,
        echecs = echecs.coerceAtMost(epreuve.echecsMax),
        echecsMax = epreuve.echecsMax,
    )

    private fun sendEpreuveStateTo(clientId: String) {
        val data = EpreuveOutilSession.etat.value?.toJoueurData() ?: return
        val message = NetworkMessage(type = NetworkMessage.TYPE_EPREUVE_STATE, epreuve = data)
        gameServer?.sendToClient(clientId, networkJson.encodeToString(message))
    }

    /**
     * PJ actuellement incarnés par un joueur connecté (réservés ou envoyés par le MJ). Vide hors
     * hébergement : sert au calcul automatique du niveau de référence des épreuves.
     */
    fun connectedCharacters(): List<Character> {
        if (_role.value != SessionRole.HOST) return emptyList()
        val connectedClientIds = _connectedClients.value.map { it.id }.toSet()
        val ids = _claimedCharacters.value.filterValues { it in connectedClientIds }.keys +
            _sentCharacterByClient.value.filterKeys { it in connectedClientIds }.values
        return GameState.characters.value.filter { it.id in ids }
    }

    /**
     * Écrit un message vers le MJ sous verrou de la socket : plusieurs coroutines peuvent envoyer
     * en même temps (relais auto de la fiche + proposition, par ex.) et une fiche dépasse le tampon
     * de PrintWriter — sans verrou les deux lignes s'entremêlaient et le MJ ignorait le message
     * illisible (la proposition "n'arrivait jamais").
     */
    private fun writeToHost(socket: Socket, message: NetworkMessage) {
        val line = networkJson.encodeToString(message)
        synchronized(socket) {
            PrintWriter(socket.getOutputStream(), true).println(line)
        }
    }

    // ── Images de l'histoire personnelle (voir PersonnageFichiers) ──
    // Envoyées à part et seulement à la demande : une fiche reçue dont l'histoire cite des
    // images absentes de l'appareil les réclame à l'expéditeur (MJ ou joueur), qui les envoie une
    // par une. Une fiche resynchronisée ne renvoie donc jamais des images déjà présentes.

    // "idFiche/nomImage" déjà demandés et pas encore reçus : pas de seconde demande en attendant.
    private val imagesHistoireDemandees = java.util.Collections.synchronizedSet(HashSet<String>())

    private fun demanderImagesHistoire(context: Context?, character: Character, envoyer: (NetworkMessage) -> Unit) {
        if (context == null || character.histoirePersonnelle.isBlank()) return
        scope.launch {
            val manquants = runCatching {
                com.jc2.jdrcompagnon.ui.PersonnageFichiers.imagesManquantes(context, character)
            }.getOrDefault(emptyList()).filter { imagesHistoireDemandees.add("${character.id}/$it") }
            if (manquants.isEmpty()) return@launch
            runCatching {
                envoyer(NetworkMessage(type = NetworkMessage.TYPE_STORY_IMAGES_REQUEST, characterId = character.id, manquants = manquants))
            }.onFailure { manquants.forEach { imagesHistoireDemandees.remove("${character.id}/$it") } }
        }
    }

    private fun envoyerImagesHistoire(context: Context?, message: NetworkMessage, envoyer: (NetworkMessage) -> Unit) {
        if (context == null) return
        val characterId = message.characterId ?: return
        val character = GameState.characters.value.find { it.id == characterId } ?: return
        scope.launch {
            message.manquants.orEmpty().forEach { nom ->
                val fichier = com.jc2.jdrcompagnon.ui.PersonnageFichiers.fichierImage(context, character, nom) ?: return@forEach
                val octets = runCatching { imageAllegee(fichier) }.getOrNull() ?: return@forEach
                val base64 = android.util.Base64.encodeToString(octets, android.util.Base64.NO_WRAP)
                runCatching {
                    envoyer(NetworkMessage(type = NetworkMessage.TYPE_STORY_IMAGE, characterId = characterId, imageHistoire = ImageHistoireData(nom, base64)))
                }
            }
        }
    }

    /** Au-delà de 1,5 Mo, l'image est réduite (2048 px max, JPEG) pour ne pas bloquer la connexion. */
    private fun imageAllegee(fichier: java.io.File): ByteArray {
        val octets = fichier.readBytes()
        if (octets.size <= 1_500_000) return octets
        val bornes = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        android.graphics.BitmapFactory.decodeByteArray(octets, 0, octets.size, bornes)
        var sample = 1
        while (maxOf(bornes.outWidth, bornes.outHeight) / (sample * 2) >= 2048) sample *= 2
        val bitmap = android.graphics.BitmapFactory.decodeByteArray(octets, 0, octets.size, android.graphics.BitmapFactory.Options().apply { inSampleSize = sample })
            ?: return octets
        return java.io.ByteArrayOutputStream().use { out ->
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, out)
            out.toByteArray()
        }
    }

    private fun recevoirImageHistoire(context: Context?, message: NetworkMessage) {
        if (context == null) return
        val characterId = message.characterId ?: return
        val image = message.imageHistoire ?: return
        val character = GameState.characters.value.find { it.id == characterId } ?: return
        imagesHistoireDemandees.remove("$characterId/${image.nom}")
        scope.launch {
            val octets = runCatching { android.util.Base64.decode(image.base64, android.util.Base64.DEFAULT) }.getOrNull() ?: return@launch
            runCatching { com.jc2.jdrcompagnon.ui.PersonnageFichiers.enregistrerImage(context, character, image.nom, octets) }
        }
    }

    /** Côté MJ : fiche reçue d'un joueur, ses images d'histoire manquantes lui sont demandées. */
    private fun demanderImagesAuJoueur(clientId: String, character: Character) =
        demanderImagesHistoire(hostContext, character) { gameServer?.sendToClient(clientId, networkJson.encodeToString(it)) }

    /** Côté joueur : fiche reçue du MJ, ses images d'histoire manquantes lui sont demandées. */
    private fun demanderImagesAuMj(socket: Socket?, character: Character) {
        val s = socket ?: return
        demanderImagesHistoire(playerContext, character) { writeToHost(s, it) }
    }

    /** Pousse la fiche d'un personnage vers le MJ (côté joueur), sans attendre sa demande. */
    private fun sendCharacterUpdateToHost(character: Character) {
        val socket = currentSocket ?: return
        val message = NetworkMessage(type = NetworkMessage.TYPE_CHARACTER_SYNC_RESPONSE, character = character)
        scope.launch {
            runCatching {
                writeToHost(socket, message)
            }
        }
    }

    // ── Côté hôte (MJ) ──
    private var gameServer: GameServer? = null
    private val _connectedClients = MutableStateFlow<List<ConnectedClient>>(emptyList())
    val connectedClients: StateFlow<List<ConnectedClient>> = _connectedClients.asStateFlow()
    private var clientsWatcherJob: Job? = null
    private var groupInfoWatcherJob: Job? = null
    private var questsWatcherJob: Job? = null

    // Voyage partagé (route, allure, joueurs prêts) : rediffusé aux joueurs à chaque changement
    // côté MJ ; les joueurs connectés sont ceux dont on attend le « Prêt au départ ». Placé après
    // _connectedClients : les blocs init s'exécutent dans l'ordre du fichier.
    init {
        val voyage = com.jc2.jdrcompagnon.feature_carte.presentation.VoyageSession
        voyage.attendusProvider = {
            if (_role.value == SessionRole.HOST) _connectedClients.value.map { it.displayName } else emptyList()
        }
        scope.launch {
            voyage.etat.collect { data ->
                if (_role.value == SessionRole.HOST) {
                    gameServer?.sendToAll(networkJson.encodeToString(NetworkMessage(type = NetworkMessage.TYPE_VOYAGE_STATE, voyage = data)))
                }
            }
        }
        scope.launch {
            _connectedClients.collect { if (_role.value == SessionRole.HOST) voyage.reverifier() }
        }

        // Tracé sur une carte de campagne : celui du MJ part à tous les joueurs, celui d'un joueur
        // part au MJ (qui le rediffuse). Côté MJ, la page table suit tous les tracés.
        val trace = com.jc2.jdrcompagnon.feature_carte.presentation.TraceCarteSession
        trace.diffuseur = { data ->
            val message = NetworkMessage(type = NetworkMessage.TYPE_MAP_TRACE, trace = data)
            when (_role.value) {
                SessionRole.HOST -> gameServer?.sendToAll(networkJson.encodeToString(message))
                SessionRole.PLAYER -> currentSocket?.let { socket ->
                    scope.launch { runCatching { writeToHost(socket, message) } }
                }
                else -> Unit
            }
        }
        scope.launch {
            trace.etat.collect { data ->
                if (_role.value != SessionRole.PLAYER) com.jc2.jdrcompagnon.feature_carte.ui.CarteTable.majTrace(data)
            }
        }
    }

    // Serveur HTTP de la page d'affichage table (photos/documents envoyés par
    // le MJ), démarré/arrêté en même temps que le serveur de partie.
    private var webServer: MjWebServer? = null
    private val _webServerUrl = MutableStateFlow<String?>(null)
    val webServerUrl: StateFlow<String?> = _webServerUrl.asStateFlow()

    // Groupe de PJ associé à la partie hébergée : ses membres (memberIds)
    // forment le pool de personnages proposés aux joueurs à la connexion.
    private val _activeGroupId = MutableStateFlow<String?>(null)
    val activeGroupId: StateFlow<String?> = _activeGroupId.asStateFlow()

    // Personnage envoyé à chaque client (clientId -> characterId), pour
    // savoir quel personnage synchroniser quand le MJ le redemande.
    private val _sentCharacterByClient = MutableStateFlow<Map<String, String>>(emptyMap())
    val sentCharacterByClient: StateFlow<Map<String, String>> = _sentCharacterByClient.asStateFlow()

    // Dernière version de fiche transmise à chaque client (push, réponse de réservation, ou reçue
    // de lui), pour ne pas lui renvoyer une fiche qu'il a déjà.
    private val lastCharacterSentToClient = java.util.concurrent.ConcurrentHashMap<String, Character>()

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
        reservationsEnAttenteDeRetour.clear()
        nomsClients.clear()
        _pendingProposals.value = emptyMap()
        val appContext = context.applicationContext
        val server = GameServer(
            context = appContext,
            hostDisplayName = hostDisplayName,
            serviceName = campaignTitle?.takeIf { it.isNotBlank() } ?: "MJ-$hostDisplayName"
        )
        server.onMessageReceived = { fromClientId, message -> handleHostIncomingMessage(fromClientId, message) }
        server.onClientConnected = { client ->
            // D'abord : un joueur qui revient récupère son personnage (et sa place en combat).
            restaurerReservations(client)
            sendAvailableCharacters(client.id)
            sendTimeStateTo(client.id)
            sendEpreuveStateTo(client.id)
            sendGroupInfoTo(client.id)
            sendQuestsStateTo(client.id)
            sendCampaignStateTo(client.id)
            sendVoyageStateTo(client.id)
            sendCarteOuverteTo(client.id)
            sendTraceTo(client.id)
            sendPositionsVilleTo(client.id)
            sendBoutiqueInviteeTo(client.id)
            sendSharedImportsTo(client.id)
            reattachProposalsToReconnectedClient(client)
            sendProposalStatesTo(client.id)
            lastCombatSentToClient.remove(client.id)
            sendCombatStateTo(client.id)
            server.sendToClient(client.id, networkJson.encodeToString(discussionLockMessage()))
        }
        server.onClientDisconnected = { clientId ->
            handlePlayerLeftDiscussion(clientId, characterName = null, disconnected = true)
            releaseCharactersHeldBy(clientId)
            if (positionsVilleParClient.remove(clientId) != null) diffuserPositionsVille()
            lastCombatSentToClient.remove(clientId)
        }
        gameServer = server
        server.start()
        _role.value = SessionRole.HOST
        clientsWatcherJob = scope.launch {
            // GameServer.connectedClients est déjà un StateFlow : on le relaie tel quel.
            server.connectedClients.collect { _connectedClients.value = it }
        }
        // Rediffuse aux joueurs le groupe actif (nom + PJ) à chaque changement côté MJ : choix du
        // groupe, renommage, ajout/retrait de membre, fiche d'un membre modifiée.
        groupInfoWatcherJob = scope.launch {
            combine(_activeGroupId, GameState.mjGroups, GameState.characters) { _, _, _ -> activeGroupInfoMessage() }
                .distinctUntilChanged()
                .collect { message -> gameServer?.sendToAll(networkJson.encodeToString(message)) }
        }
        hostContext = appContext
        startCampaignWatcher(appContext)
        // Quêtes visibles de la campagne sélectionnée : rediffusées à chaque changement côté MJ.
        questsWatcherJob = scope.launch {
            combine(GameState.mjCampaigns, GameState.currentCampaignId, GameState.characters) { _, _, _ -> questsStateMessage() }
                .distinctUntilChanged()
                .collect { message -> gameServer?.sendToAll(networkJson.encodeToString(message)) }
        }
        // Combat : chaque joueur reçoit sa propre vue (sa déclaration seulement) à chaque
        // changement du combat ou des personnages incarnés.
        combatWatcherJob = scope.launch {
            combine(CombatSession.etat, _claimedCharacters, _sentCharacterByClient, _connectedClients) { _, _, _, clients -> clients }
                .collect { clients -> clients.forEach { sendCombatStateTo(it.id) } }
        }
        // Personnages joués par les joueurs : ils déclarent eux-mêmes en combat, l'IA pilote les autres.
        CombatSession.personnagesJoues = ::personnagesJoues
        joueursWatcherJob?.cancel()
        joueursWatcherJob = scope.launch {
            combine(_claimedCharacters, _sentCharacterByClient, CombatSession.etat) { _, _, combat -> combat?.id }
                .collect { CombatSession.marquerJoues(personnagesJoues()) }
        }
        startEtatsWatcher()
        startWebServer(appContext)
        LanConnectionService.ensureStarted(appContext, "Partie hébergée sur le réseau local")
    }

    // ── Imports partagés avec les joueurs (voir ImportsPartages) ──

    private fun mondeMj(): String = GameState.currentWorldId() ?: "donjon_et_dragon"

    private fun sharedImportsMessage(context: Context): NetworkMessage = NetworkMessage(
        type = NetworkMessage.TYPE_SHARED_IMPORTS,
        imports = runCatching { ImportsPartages.descriptifs(context, mondeMj()) }.getOrDefault(emptyList()),
        urlTable = _webServerUrl.value,
    )

    private fun sendSharedImportsTo(clientId: String) {
        val context = hostContext ?: return
        scope.launch { gameServer?.sendToClient(clientId, networkJson.encodeToString(sharedImportsMessage(context))) }
    }

    /** Le MJ a changé les imports partagés : les joueurs connectés reçoivent la nouvelle liste. */
    fun importsPartagesModifies() {
        val context = hostContext ?: return
        scope.launch { gameServer?.sendToAll(networkJson.encodeToString(sharedImportsMessage(context))) }
    }

    /** Contenu introuvable chez un joueur pour ce qu'il vient de récupérer (TYPE_CONTENT_MISSING). */
    data class AlerteContenuManquant(val joueur: String, val sujet: String, val manquants: List<String>)

    private val _alertesContenuManquant = MutableStateFlow<List<AlerteContenuManquant>>(emptyList())
    val alertesContenuManquant: StateFlow<List<AlerteContenuManquant>> = _alertesContenuManquant.asStateFlow()

    fun dismissAlerteContenuManquant(alerte: AlerteContenuManquant) {
        _alertesContenuManquant.value = _alertesContenuManquant.value - alerte
    }

    // ── Port de la page d'affichage table : fixe d'une session à l'autre, réglable par le MJ ──
    const val PORT_TABLE_DEFAUT = 8080
    val PLAGE_PORTS = 1024..65535
    private const val PREFS_RESEAU = "reseau_local"
    private const val CLE_PORT_TABLE = "port_table"

    fun portTable(context: Context): Int =
        context.getSharedPreferences(PREFS_RESEAU, Context.MODE_PRIVATE)
            .getInt(CLE_PORT_TABLE, PORT_TABLE_DEFAUT).takeIf { it in PLAGE_PORTS } ?: PORT_TABLE_DEFAUT

    /** Choisi par le MJ avant de démarrer le serveur ; ignoré s'il est hors de [PLAGE_PORTS]. */
    fun setPortTable(context: Context, port: Int) {
        if (port !in PLAGE_PORTS) return
        context.getSharedPreferences(PREFS_RESEAU, Context.MODE_PRIVATE).edit().putInt(CLE_PORT_TABLE, port).apply()
    }

    /**
     * Démarre la page d'affichage table (photos/documents), en plus du serveur de partie, sur le
     * port choisi ([portTable]) ; sur un port libre attribué par l'OS si celui-ci est déjà pris.
     */
    private fun startWebServer(appContext: Context) {
        val server = runCatching {
            MjWebServer(appContext, portTable(appContext)).also { it.start(fi.iki.elonen.NanoHTTPD.SOCKET_READ_TIMEOUT, true) }
        }.getOrElse { MjWebServer(appContext) }
        try {
            if (!server.isAlive) server.start(fi.iki.elonen.NanoHTTPD.SOCKET_READ_TIMEOUT, true)
            webServer = server
            val ip = getLocalIpAddress() ?: "0.0.0.0"
            _webServerUrl.value = "http://$ip:${server.listeningPort}"
            updateWebServerStatus(ScenarioClockState.state.value)
            server.updateScene(tableScene)
            server.updateExploration(tableExploration)
            server.updateRotation(tableRotation)
        } catch (e: Exception) {
            webServer = null
            _webServerUrl.value = null
        }
    }

    /** Envoie une photo ou un document (choisi par le MJ) sur la page d'affichage table. */
    fun sendMediaToWeb(sourceUri: Uri, displayName: String): Boolean {
        return webServer?.addMedia(sourceUri, displayName) ?: false
    }

    /**
     * Envoie une image stockée en interne (ex. carte de campagne, enregistrée en ".img") sur la
     * page d'affichage table. Celle-ci reconnaît une image à son extension (MjWebServer.isImage) :
     * le vrai format est donc déduit des premiers octets du fichier.
     */
    fun sendImageFileToWeb(fichier: java.io.File, nom: String): Boolean {
        if (!fichier.isFile) return false
        val entete = runCatching { fichier.inputStream().use { input -> ByteArray(12).also { input.read(it) } } }.getOrNull()
            ?: return false
        val extension = when {
            entete[0] == 0x89.toByte() && entete[1] == 'P'.code.toByte() -> "png"
            entete[0] == 0xFF.toByte() && entete[1] == 0xD8.toByte() -> "jpg"
            String(entete, 0, 4, Charsets.US_ASCII) == "RIFF" && String(entete, 8, 4, Charsets.US_ASCII) == "WEBP" -> "webp"
            String(entete, 0, 3, Charsets.US_ASCII) == "GIF" -> "gif"
            entete[0] == 'B'.code.toByte() && entete[1] == 'M'.code.toByte() -> "bmp"
            else -> "png"
        }
        return sendMediaToWeb(Uri.fromFile(fichier), "$nom.$extension")
    }

    /** Envoie une image embarquée dans les assets (ex. portrait PNJ) sur la page d'affichage table. */
    fun sendAssetToWeb(assetPath: String, displayName: String): Boolean {
        return webServer?.addAssetMedia(assetPath, displayName) ?: false
    }

    // Dernière scène lue par le MJ : gardée même sans serveur web, pour l'afficher dès son démarrage.
    private var tableScene: MjWebServer.SceneInfo? = null

    /** Nom du scénario et image de la scène en cours sur la page d'affichage table. */
    fun updateTableScene(info: MjWebServer.SceneInfo?) {
        tableScene = info
        webServer?.updateScene(info)
    }

    // Carte d'exploration (brouillard de guerre) affichée sur la table : gardée même sans serveur
    // web, pour l'afficher dès son démarrage (cf. ExplorationSession).
    private var tableExploration: MjWebServer.ExplorationInfo? = null

    /** Carte d'exploration et cases révélées sur la page d'affichage table (null = retirée). */
    /** Carte (exploration ou carte de campagne) actuellement envoyée à la page table, null = aucune. */
    val explorationSurTable: MjWebServer.ExplorationInfo? get() = tableExploration

    fun updateTableExploration(info: MjWebServer.ExplorationInfo?) {
        tableExploration = info
        webServer?.updateExploration(info)
    }

    // Orientation des images de la table (rotation de l'écran de carte du MJ), gardée même sans
    // serveur web pour l'appliquer dès son démarrage.
    private var tableRotation = 0

    /** Fait pivoter toutes les images de la page d'affichage table (degrés, par quarts de tour). */
    fun updateTableRotation(degres: Int) {
        tableRotation = degres
        webServer?.updateRotation(degres)
    }

    /** La page d'affichage table est-elle en ligne (partie hébergée) ? */
    val isTableDisplayOnline: Boolean get() = webServer != null

    /** Vide la page d'affichage table (nouvelle scène). */
    fun clearWebMedia() {
        webServer?.clearMedia()
    }

    /** Envoie un personnage complet à un client connecté (envoi ponctuel). */
    fun pushCharacterToClient(clientId: String, character: Character) {
        lastCharacterSentToClient[clientId] = character
        val message = NetworkMessage(type = NetworkMessage.TYPE_CHARACTER_PUSH, character = character)
        gameServer?.sendToClient(clientId, networkJson.encodeToString(message))
        _sentCharacterByClient.value = _sentCharacterByClient.value + (clientId to character.id)
    }

    /**
     * Diffuse le briefing d'un PNJ (nom + id de portrait, cf. characterPortraitOptions) à tous
     * les joueurs connectés — déclenché par un lien `#event:` en lecture de scénario
     * (voir PnjBriefingOverlay). Sans effet si aucun serveur n'est démarré.
     */
    /**
     * @return nombre de joueurs connectés qui reçoivent la discussion, ou -1 si aucune partie
     * n'est hébergée (rien n'est envoyé) — pour que le MJ sache si la discussion a démarré.
     */
    fun sendPnjBriefingToAll(character: Character): Int {
        val server = gameServer ?: return -1
        val message = NetworkMessage(
            type = NetworkMessage.TYPE_PNJ_BRIEFING,
            pnjName = character.name,
            pnjPortraitId = character.portrait.ifBlank { null }
        )
        server.sendToAll(networkJson.encodeToString(message))
        return _connectedClients.value.size
    }

    // ── Discussion avec un PNJ : jets de compétence sociale (côté MJ) ──

    /** Décision du MJ sur un jet social. LIGNE_ROUGE = échec automatique, présenté NORMAL au joueur. */
    enum class SocialRollMode(val label: String) {
        NORMAL("Normal"), AVANTAGE("Avantage"), DESAVANTAGE("Désavantage"), LIGNE_ROUGE("Ligne rouge")
    }

    /** Une demande de jet social reçue d'un joueur, suivie jusqu'à son résultat. */
    data class SocialRollRequest(
        val id: String,
        val clientId: String,
        val characterName: String,
        val pnjName: String,
        val skill: String,
        val bonus: Int,
        val mode: SocialRollMode? = null,
        val dice: List<Int>? = null,
        val total: Int? = null,
        // ND en vigueur à la réception du résultat (null = non fixé, le MJ tranche lui-même).
        val nd: Int? = null,
    ) {
        /** null tant que le jet n'est pas lancé ou si le ND n'est pas fixé. */
        val reussite: Boolean?
            get() = when {
                total == null -> null
                mode == SocialRollMode.LIGNE_ROUGE -> false
                nd == null -> null
                else -> total >= nd
            }
    }

    private val _socialRequests = MutableStateFlow<List<SocialRollRequest>>(emptyList())
    val socialRequests: StateFlow<List<SocialRollRequest>> = _socialRequests.asStateFlow()

    // ND de la discussion en cours (attitude du PNJ), réglé par l'aide de jeu du briefing.
    private val _discussionNd = MutableStateFlow<Int?>(null)
    val discussionNd: StateFlow<Int?> = _discussionNd.asStateFlow()
    fun setDiscussionNd(nd: Int?) { _discussionNd.value = nd }

    // Vrai tant que le briefing PNJ est ouvert chez le MJ : les demandes s'y traitent, sinon
    // HostNetworkOverlay prend le relais (une fenêtre par-dessus le briefing serait masquée).
    private val _discussionOpen = MutableStateFlow(false)
    val discussionOpen: StateFlow<Boolean> = _discussionOpen.asStateFlow()
    fun setDiscussionOpen(open: Boolean) { _discussionOpen.value = open }

    /** Le MJ fixe avantage/désavantage (ou ligne rouge) ; le joueur peut alors lancer. */
    fun decideSocialRoll(requestId: String, mode: SocialRollMode) {
        val request = _socialRequests.value.find { it.id == requestId } ?: return
        _socialRequests.value = _socialRequests.value.map { if (it.id == requestId) it.copy(mode = mode) else it }
        val modeJoueur = if (mode == SocialRollMode.LIGNE_ROUGE) SocialRollMode.NORMAL else mode
        val message = NetworkMessage(
            type = NetworkMessage.TYPE_SOCIAL_ROLL_MODE,
            socialRequestId = requestId,
            socialMode = modeJoueur.name,
        )
        gameServer?.sendToClient(request.clientId, networkJson.encodeToString(message))
    }

    /** Retire une demande traitée de la liste du MJ (fin de l'interaction : la discussion se débloque). */
    fun clearSocialRequest(requestId: String) {
        _socialRequests.value = _socialRequests.value.filterNot { it.id == requestId }
        broadcastDiscussionLock()
    }

    /**
     * Retour en arrière : le joueur ne veut plus faire cette action. La demande est retirée et le
     * joueur revient au choix des compétences ; la discussion se débloque pour les autres.
     */
    fun cancelSocialRequest(requestId: String) {
        val request = _socialRequests.value.find { it.id == requestId } ?: return
        _socialRequests.value = _socialRequests.value.filterNot { it.id == requestId }
        val message = NetworkMessage(type = NetworkMessage.TYPE_SOCIAL_ROLL_CANCEL, socialRequestId = requestId)
        gameServer?.sendToClient(request.clientId, networkJson.encodeToString(message))
        broadcastDiscussionLock()
    }

    /** Le MJ ferme la discussion chez tous les joueurs. */
    fun closeDiscussionForAll() {
        _socialRequests.value = emptyList()
        _discussionNotices.value = emptyList()
        clearWebMedia()
        gameServer?.sendToAll(networkJson.encodeToString(NetworkMessage(type = NetworkMessage.TYPE_DISCUSSION_CLOSED)))
        broadcastDiscussionLock()
    }

    // Avis au MJ (ex. « Aria a quitté la discussion »), affichés jusqu'à ce qu'il les valide.
    private val _discussionNotices = MutableStateFlow<List<String>>(emptyList())
    val discussionNotices: StateFlow<List<String>> = _discussionNotices.asStateFlow()
    fun dismissDiscussionNotice(notice: String) { _discussionNotices.value = _discussionNotices.value - notice }

    /** Joueur en train d'agir : la première demande non soldée ; les autres joueurs attendent. */
    private fun discussionLockMessage(): NetworkMessage {
        val holder = _socialRequests.value.firstOrNull()
        return NetworkMessage(
            type = NetworkMessage.TYPE_DISCUSSION_LOCK,
            socialRequestId = holder?.id,
            socialCharacterName = holder?.characterName,
        )
    }

    private fun broadcastDiscussionLock() {
        gameServer?.sendToAll(networkJson.encodeToString(discussionLockMessage()))
    }

    /** Un joueur quitte la discussion (bouton ou déconnexion) : le MJ est prévenu, ses demandes tombent. */
    private fun handlePlayerLeftDiscussion(clientId: String, characterName: String?, disconnected: Boolean) {
        val requests = _socialRequests.value.filter { it.clientId == clientId }
        // Déconnexion hors discussion : rien à signaler.
        if (disconnected && requests.isEmpty() && !_discussionOpen.value) return
        val name = characterName
            ?: requests.firstOrNull()?.characterName
            ?: _connectedClients.value.find { it.id == clientId }?.displayName
            ?: "Un joueur"
        _discussionNotices.value = _discussionNotices.value +
            (if (disconnected) "⚠ $name s'est déconnecté pendant la discussion." else "🚪 $name a quitté la discussion.")
        if (requests.isNotEmpty()) {
            _socialRequests.value = _socialRequests.value.filterNot { it.clientId == clientId }
            broadcastDiscussionLock()
        }
    }

    /** Livre directement un objet dans le sac du joueur ciblé (client réseau distant). */
    fun sendItemToClient(clientId: String, itemName: String) {
        val message = NetworkMessage(type = NetworkMessage.TYPE_ITEM_DELIVERY, itemName = itemName)
        gameServer?.sendToClient(clientId, networkJson.encodeToString(message))
    }

    /** Suivi d'une offre de butin de groupe en cours, côté MJ. */
    data class LootOfferTracking(
        val itemName: String,
        val pendingClientIds: Set<String>,
        val respondedClientIds: Set<String> = emptySet(),
    )

    private val _activeLootOffers = MutableStateFlow<Map<String, LootOfferTracking>>(emptyMap())
    val activeLootOffers: StateFlow<Map<String, LootOfferTracking>> = _activeLootOffers.asStateFlow()

    /**
     * Propose un objet à tous les membres du groupe déjà réclamés par un client connecté.
     * Chaque joueur voit une carte "je le veux" au-dessus de son inventaire (TYPE_LOOT_OFFER) ;
     * l'offre se ferme automatiquement quand tous ont répondu (voir handleHostIncomingMessage),
     * ou manuellement via [closeLootOffer].
     */
    fun sendLootOfferToGroup(groupId: String, itemName: String): String {
        val group = GameState.mjGroups.value.find { it.id == groupId }
        val offerId = java.util.UUID.randomUUID().toString()
        val targetClientIds = joueursDuGroupe(group?.memberIds.orEmpty()).keys
        _activeLootOffers.value = _activeLootOffers.value + (offerId to LootOfferTracking(itemName, targetClientIds))
        val message = NetworkMessage(type = NetworkMessage.TYPE_LOOT_OFFER, lootOfferId = offerId, itemName = itemName)
        targetClientIds.forEach { clientId -> gameServer?.sendToClient(clientId, networkJson.encodeToString(message)) }
        return offerId
    }

    /**
     * Joueurs connectés qui incarnent un membre de [memberIds] (clientId → characterId) : personnage
     * réservé par le joueur, ou envoyé par le MJ à un joueur encore connecté.
     */
    fun joueursDuGroupe(memberIds: Collection<String>): Map<String, String> {
        val connectes = _connectedClients.value.map { it.id }.toSet()
        val envoyes = _sentCharacterByClient.value.filterKeys { it in connectes }
        val reserves = _claimedCharacters.value.entries.associate { (characterId, clientId) -> clientId to characterId }
        return (envoyes + reserves).filterValues { it in memberIds }
    }

    /** Ferme une offre de butin (bouton "Fermer" du MJ, ou automatique quand tous ont répondu). */
    fun closeLootOffer(offerId: String) {
        val message = NetworkMessage(type = NetworkMessage.TYPE_LOOT_OFFER_CLOSED, lootOfferId = offerId)
        gameServer?.sendToAll(networkJson.encodeToString(message))
        _activeLootOffers.value = _activeLootOffers.value - offerId
    }

    enum class ProposalDecision { PENDING, ACCEPTED, DECLINED, PASSED }

    /** Suivi d'une proposition de récompense de groupe en cours, côté MJ (et miroir côté joueur). */
    data class GroupProposal(
        val id: String,
        val title: String,
        val description: String,
        val rewardLabel: String,
        val pendingClientIds: Set<String>,
        val decisions: Map<String, ProposalDecision> = emptyMap(),
        val clientNames: Map<String, String> = emptyMap(),
    ) {
        /**
         * Vrai tant que tout le groupe n'a pas décidé, ou si tous les joueurs ont passé (personne
         * n'a pris la récompense) : la proposition reste alors "en cours" dans le menu latéral.
         */
        val isUnresolved: Boolean
            get() {
                val finalDecisions = pendingClientIds.map { decisions[it] ?: ProposalDecision.PENDING }
                return finalDecisions.any { it == ProposalDecision.PENDING } ||
                    (finalDecisions.isNotEmpty() && finalDecisions.all { it == ProposalDecision.PASSED })
            }
    }

    private val _activeGroupProposals = MutableStateFlow<Map<String, GroupProposal>>(emptyMap())
    val activeGroupProposals: StateFlow<Map<String, GroupProposal>> = _activeGroupProposals.asStateFlow()

    /** Vrai s'il existe au moins une proposition de groupe non résolue (menu latéral "Proposition en cours"). */
    val hasPendingGroupProposals: StateFlow<Boolean> = _activeGroupProposals
        .map { it.values.any(GroupProposal::isUnresolved) }
        .stateIn(scope, SharingStarted.Eagerly, false)

    /** Propose une récompense de groupe soumise au vote de tous les joueurs actuellement connectés. */
    fun sendGroupProposalToAll(title: String, description: String, rewardLabel: String): String {
        val proposalId = java.util.UUID.randomUUID().toString()
        val clients = _connectedClients.value
        val targetClientIds = clients.map { it.id }.toSet()
        val clientNames = clients.associate { it.id to it.displayName }
        val proposal = GroupProposal(
            id = proposalId,
            title = title,
            description = description,
            rewardLabel = rewardLabel,
            pendingClientIds = targetClientIds,
            clientNames = clientNames,
        )
        _activeGroupProposals.value = _activeGroupProposals.value + (proposalId to proposal)
        val message = NetworkMessage(
            type = NetworkMessage.TYPE_GROUP_PROPOSAL,
            proposalId = proposalId,
            proposalTitle = title,
            proposalDescription = description,
            proposalReward = rewardLabel,
        )
        gameServer?.sendToAll(networkJson.encodeToString(message))
        broadcastProposalState(proposal)
        return proposalId
    }

    /** Envoie l'état des propositions de groupe en cours à un client qui (re)vient de se connecter. */
    /**
     * Un joueur qui se reconnecte reçoit un nouvel id client (GameServer en génère un par
     * connexion) : ses places dans les propositions de groupe en cours, rattachées à l'ancien id,
     * sont reportées sur le nouveau en le retrouvant par son pseudo. Sans cela son vote arrivait
     * sous un id inconnu de la proposition et n'était jamais compté. S'il n'avait pas encore
     * décidé, la proposition lui est renvoyée (son dialog a pu disparaître, ex. appli relancée).
     *
     * L'ancien id est préféré parmi ceux déjà déconnectés ; à défaut on prend quand même le
     * premier homonyme, car une coupure Wi-Fi peut laisser l'ancienne socket "vivante" côté MJ
     * quelques instants après la reconnexion.
     */
    private fun reattachProposalsToReconnectedClient(client: ConnectedClient) {
        val connectedIds = gameServer?.connectedClients?.value.orEmpty().map { it.id }.toSet()
        _activeGroupProposals.value.values.forEach { proposal ->
            if (client.id in proposal.pendingClientIds) return@forEach
            val candidates = proposal.pendingClientIds.filter { proposal.clientNames[it] == client.displayName }
            val oldId = candidates.firstOrNull { it !in connectedIds } ?: candidates.firstOrNull() ?: return@forEach
            val oldDecision = proposal.decisions[oldId]
            val updated = proposal.copy(
                pendingClientIds = proposal.pendingClientIds - oldId + client.id,
                decisions = (proposal.decisions - oldId).let { d -> if (oldDecision != null) d + (client.id to oldDecision) else d },
                clientNames = proposal.clientNames - oldId + (client.id to client.displayName),
            )
            _activeGroupProposals.value = _activeGroupProposals.value + (proposal.id to updated)
            if (oldDecision == null || oldDecision == ProposalDecision.PENDING) {
                val message = NetworkMessage(
                    type = NetworkMessage.TYPE_GROUP_PROPOSAL,
                    proposalId = proposal.id,
                    proposalTitle = proposal.title,
                    proposalDescription = proposal.description,
                    proposalReward = proposal.rewardLabel,
                )
                gameServer?.sendToClient(client.id, networkJson.encodeToString(message))
            }
            // Les autres joueurs voient la ligne de ce joueur conservée (même nom, même décision).
            broadcastProposalState(updated)
        }
    }

    private fun sendProposalStatesTo(clientId: String) {
        _activeGroupProposals.value.values.forEach { proposal ->
            gameServer?.sendToClient(clientId, networkJson.encodeToString(proposalStateMessage(proposal)))
        }
    }

    private fun broadcastProposalState(proposal: GroupProposal, closed: Boolean = false) {
        gameServer?.sendToAll(networkJson.encodeToString(proposalStateMessage(proposal, closed)))
    }

    private fun proposalStateMessage(proposal: GroupProposal, closed: Boolean = false): NetworkMessage {
        return NetworkMessage(
            type = NetworkMessage.TYPE_PROPOSAL_STATE,
            proposalId = proposal.id,
            proposalTitle = proposal.title,
            proposalDescription = proposal.description,
            proposalReward = proposal.rewardLabel,
            proposalDecisions = proposal.pendingClientIds.associateWith { clientId ->
                (proposal.decisions[clientId] ?: ProposalDecision.PENDING).name
            },
            proposalClientNames = proposal.clientNames,
            proposalClosed = closed,
        )
    }

    /**
     * Clôture une proposition de groupe (bouton "Clôturer" du MJ) : les décisions manquantes sont
     * considérées comme perdues (PASSED), l'état final est rediffusé puis la proposition retirée.
     */
    fun closeGroupProposal(proposalId: String) {
        val proposal = _activeGroupProposals.value[proposalId] ?: return
        val finalDecisions = proposal.pendingClientIds.associateWith { clientId ->
            proposal.decisions[clientId] ?: ProposalDecision.PASSED
        }
        val closed = proposal.copy(decisions = finalDecisions)
        broadcastProposalState(closed, closed = true)
        _activeGroupProposals.value = _activeGroupProposals.value - proposalId
    }

    /**
     * Appelé quand le MJ change de scène dans le lecteur de scénario : avertit chaque joueur qui
     * n'a pas encore décidé d'une proposition de groupe en cours (dernier appel avant perte de la
     * récompense). Ne clôture pas la proposition — le MJ garde la main via [closeGroupProposal].
     */
    fun warnPendingProposalsOnSceneChange() {
        _activeGroupProposals.value.values.forEach { proposal ->
            val stillPending = proposal.pendingClientIds.filter { clientId ->
                val decision = proposal.decisions[clientId]
                decision == null || decision == ProposalDecision.PENDING
            }
            if (stillPending.isNotEmpty()) {
                val message = NetworkMessage(
                    type = NetworkMessage.TYPE_PROPOSAL_FINAL_CALL,
                    proposalId = proposal.id,
                    proposalTitle = proposal.title,
                )
                stillPending.forEach { clientId ->
                    gameServer?.sendToClient(clientId, networkJson.encodeToString(message))
                }
            }
        }
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
        demanderImagesAuJoueur(clientId, character)
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
        val proposed = _pendingProposals.value[clientId]
        _pendingProposals.value = _pendingProposals.value - clientId
        // Version locale proposée après un conflit sur un PJ déjà réservé par ce joueur : on joint
        // la fiche du MJ, qui fait référence, pour que le joueur l'adopte.
        val hostVersion = proposed
            ?.takeIf { _claimedCharacters.value[it.id] == clientId }
            ?.let { p -> GameState.characters.value.find { it.id == p.id } }
        hostVersion?.let { lastCharacterSentToClient[clientId] = it }
        gameServer?.sendToClient(
            clientId,
            networkJson.encodeToString(
                NetworkMessage(type = NetworkMessage.TYPE_CHARACTER_PROPOSAL_RESULT, accepted = false, character = hostVersion)
            )
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

    private fun sendTimeStateTo(clientId: String) {
        val message = NetworkMessage(
            type = NetworkMessage.TYPE_TIME_STATE,
            scenarioClock = ScenarioClockState.state.value,
            calendar = ScenarioClockState.activeCalendar(),
        )
        gameServer?.sendToClient(clientId, networkJson.encodeToString(message))
    }

    private fun broadcastTimeState(state: ScenarioClockData) {
        // Calendrier actif joint à chaque état (quelques centaines d'octets) : un joueur qui se
        // connecte ou reçoit un changement de calendrier affiche aussitôt la même date que le MJ.
        val message = NetworkMessage(
            type = NetworkMessage.TYPE_TIME_STATE,
            scenarioClock = state,
            calendar = ScenarioClockState.activeCalendar(),
        )
        gameServer?.sendToAll(networkJson.encodeToString(message))
        updateWebServerStatus(state)
    }

    /** Pousse l'heure/météo courantes sur la page d'affichage table (voir MjWebServer.updateStatus). */
    private fun updateWebServerStatus(state: ScenarioClockData) {
        webServer?.updateStatus(
            time = ScenarioClockState.formattedCalendarDate(state.scenarioMinutes, seconds = state.scenarioSeconds),
            weatherKey = state.weather.name.lowercase(),
            weatherLabel = weatherLabel(state.weather) + " · " + state.temperature.label
        )
    }

    private fun availableCharacterSummaries(): List<CharacterSummary> {
        val groupId = _activeGroupId.value ?: return emptyList()
        val group = GameState.mjGroups.value.find { it.id == groupId } ?: return emptyList()
        val claimedIds = _claimedCharacters.value.keys
        return GameState.characters.value
            // Seuls les PJ du groupe sont proposés aux joueurs (PNJ et créatures restent au MJ).
            .filter { it.id in group.memberIds && it.id !in claimedIds && it.type == "PJ" }
            .map { CharacterSummary(it.id, it.name, it.race, it.characterClass, it.level) }
    }

    // Pseudo de chaque client (par id de connexion), et personnages réservés par un joueur qui vient
    // de se déconnecter (par pseudo) : rendus automatiquement à sa reconnexion, sous son nouvel id.
    private val nomsClients = java.util.concurrent.ConcurrentHashMap<String, String>()
    private val reservationsEnAttenteDeRetour = java.util.concurrent.ConcurrentHashMap<String, Set<String>>()

    /**
     * Un joueur se reconnecte (nouvel id client) : il retrouve les personnages qu'il avait réservés —
     * libérés à sa déconnexion, ou encore rattachés à son ancienne connexion pas encore détectée
     * comme coupée. Sans cela il n'incarnait plus son personnage (combat compris) après une coupure.
     */
    /**
     * Personnages incarnés par un joueur : réservés, envoyés par le MJ, ou réservés par un joueur
     * momentanément déconnecté (il va revenir). Vide hors hébergement.
     */
    fun personnagesJoues(): Set<String> {
        if (_role.value != SessionRole.HOST) return emptySet()
        return _claimedCharacters.value.keys + _sentCharacterByClient.value.values + reservationsEnAttenteDeRetour.values.flatten()
    }

    private fun restaurerReservations(client: ConnectedClient) {
        nomsClients[client.id] = client.displayName
        val enAttente = reservationsEnAttenteDeRetour.remove(client.displayName).orEmpty()
        val tenusParAncienneConnexion = _claimedCharacters.value
            .filter { (_, holder) -> holder != client.id && nomsClients[holder] == client.displayName }.keys
        val aRendre = (enAttente.filter { it !in _claimedCharacters.value } + tenusParAncienneConnexion).toSet()
        if (aRendre.isEmpty()) return
        _claimedCharacters.value = _claimedCharacters.value + aRendre.associateWith { client.id }
        broadcastAvailableCharacters()
    }

    private fun releaseCharactersHeldBy(clientId: String) {
        val hadClaim = _claimedCharacters.value.any { it.value == clientId }
        nomsClients[clientId]?.let { nom ->
            val tenus = _claimedCharacters.value.filterValues { it == clientId }.keys
            if (tenus.isNotEmpty()) reservationsEnAttenteDeRetour[nom] = reservationsEnAttenteDeRetour[nom].orEmpty() + tenus
        }
        _claimedCharacters.value = _claimedCharacters.value.filterValues { it != clientId }
        _pendingProposals.value = _pendingProposals.value - clientId
        lastCharacterSentToClient.remove(clientId)
        if (hadClaim) broadcastAvailableCharacters()
    }

    // ── Combat (côté MJ) ──
    private var combatWatcherJob: Job? = null
    private var joueursWatcherJob: Job? = null

    // États des joueurs (fiche et combat) : chaque état nouvellement appliqué à un PJ est annoncé
    // sur la page d'affichage table, avec son illustration (MjWebServer.annoncerEtat).
    private var etatsWatcherJob: Job? = null

    private fun startEtatsWatcher() {
        etatsWatcherJob?.cancel()
        etatsWatcherJob = scope.launch {
            var precedent: Map<String, Set<com.jc2.jdrcompagnon.feature_combat.domain.model.ConditionCombat>>? = null
            combine(GameState.characters, CombatSession.etat) { persos, combat -> etatsDesJoueurs(persos, combat?.combattants.orEmpty()) }
                .distinctUntilChanged()
                .collect { actuel ->
                    // Premier relevé : états déjà présents au lancement de la partie, pas annoncés.
                    precedent?.let { avant ->
                        actuel.forEach { (nom, etats) ->
                            (etats - avant[nom].orEmpty()).forEach { webServer?.annoncerEtat(nom, it.label) }
                        }
                    }
                    precedent = actuel
                }
        }
    }

    /**
     * États de chaque personnage joueur (par nom) : ceux de sa fiche (champ condition) et, en combat,
     * ceux de son combattant — Concentration exclue (ce n'est pas un état des règles).
     */
    internal fun etatsDesJoueurs(
        persos: List<Character>,
        combattants: List<Combattant>,
    ): Map<String, Set<com.jc2.jdrcompagnon.feature_combat.domain.model.ConditionCombat>> {
        val pj = persos.filter { it.type == "PJ" }
        val parId = pj.associateBy { it.id }
        val resultat = pj.associate { it.name to com.jc2.jdrcompagnon.feature_combat.domain.model.Etats.lire(it.condition).toMutableSet() }
        combattants.filter { !it.estMonstre }.forEach { c ->
            val nom = c.characterId?.let { parId[it]?.name } ?: c.nom.takeIf { it in resultat }
            nom?.let { resultat.getValue(it).addAll(c.conditions) }
        }
        return resultat.mapValues { (_, etats) -> etats - com.jc2.jdrcompagnon.feature_combat.domain.model.ConditionCombat.CONCENTRATION }
    }

    // Dernière vue de combat envoyée à chaque client (ligne JSON), pour ne renvoyer que les changements.
    private val lastCombatSentToClient = java.util.concurrent.ConcurrentHashMap<String, String>()

    /** Personnage incarné par un client : réservé par lui, ou envoyé par le MJ. */
    private fun characterIdOfClient(clientId: String): String? =
        _claimedCharacters.value.entries.firstOrNull { it.value == clientId }?.key
            ?: _sentCharacterByClient.value[clientId]

    private fun sendCombatStateTo(clientId: String) {
        val server = gameServer ?: return
        val data = CombatSession.etat.value?.versJoueur(characterIdOfClient(clientId))
        val line = networkJson.encodeToString(NetworkMessage(type = NetworkMessage.TYPE_COMBAT_STATE, combat = data))
        if (lastCombatSentToClient[clientId] == line) return
        lastCombatSentToClient[clientId] = line
        server.sendToClient(clientId, line)
    }

    /** Combattant du combat en cours incarné par ce client, si le message vise bien ce combat. */
    private fun combattantOfClient(clientId: String, combatId: String): Combattant? {
        val combat = CombatSession.etat.value?.takeIf { it.id == combatId } ?: return null
        val characterId = characterIdOfClient(clientId) ?: return null
        return combat.combattants.firstOrNull { it.characterId == characterId }
    }

    private fun handleCombatMessage(fromClientId: String, message: NetworkMessage) {
        when (message.type) {
            NetworkMessage.TYPE_COMBAT_DECLARATION -> {
                val d = message.combatDeclaration ?: return
                val combattant = combattantOfClient(fromClientId, d.combatId) ?: return
                val combat = CombatSession.etat.value ?: return
                if (combat.round != d.round) return
                val desengage = d.actionId == "disengage"
                val visee = d.distanceVisee?.let { runCatching { Distance.valueOf(it) }.getOrNull() }
                val deplacements = when {
                    visee != null && combat.combattants.any { it.id == d.deplacementCibleId && it.estMonstre } ->
                        mapOf(d.deplacementCibleId!! to visee)
                    // Se désengager sans précision : quitte le contact de tous les monstres.
                    desengage -> combat.distancesDe(combattant.id).filterValues { it == Distance.CONTACT }
                        .mapValues { it.value.eloigne(combattant.vitesse) }
                    else -> emptyMap()
                }
                // Sort de zone : les créatures dans la zone autour de la cible (sphère) ou devant le
                // lanceur (cône, ligne) sont ajoutées d'office à celles touchées par le joueur.
                val declarees = d.ciblesZone.filter { id -> combat.combattants.any { it.id == id } }
                val ciblesZone = com.jc2.jdrcompagnon.feature_combat.domain.model.ZoneEffet.depuisTexteCode(d.zone)
                    ?.let { zone -> (declarees + combat.dansLaZone(zone, combattant.id, declarees.firstOrNull() ?: d.cibleId).map { it.id }).distinct() }
                    ?: declarees
                CombatSession.declarer(
                    DeclarationAction(
                        combattantId = combattant.id,
                        libelle = d.libelle,
                        actionId = d.actionId,
                        cibleId = d.cibleId,
                        cibleNom = d.cibleNom,
                        detail = d.detail,
                        deplacements = if (d.zoneVisee != null) emptyMap() else deplacements,
                        zoneVisee = d.zoneVisee?.coerceIn(0, com.jc2.jdrcompagnon.feature_combat.domain.model.Terrain.derniere),
                        desengage = desengage,
                        attaquesJoueur = d.attaques,
                        ciblesZone = ciblesZone,
                    )
                )
            }
            NetworkMessage.TYPE_COMBAT_ROLL -> {
                val j = message.combatJet ?: return
                val combattant = combattantOfClient(fromClientId, j.combatId) ?: return
                CombatSession.enregistrerJet(
                    JetCombat(
                        combattantId = combattant.id,
                        round = 0,
                        libelle = j.libelle,
                        formule = j.formule,
                        des = j.des,
                        bonus = j.bonus,
                        total = j.total,
                        manuel = j.manuel,
                        formuleDegats = j.formuleDegats,
                        desDegats = j.desDegats,
                        totalDegats = j.totalDegats,
                        cibleId = j.cibleId,
                        arme = j.arme,
                        typeDegats = j.typeDegats,
                    )
                )
            }
        }
    }

    private fun handleHostIncomingMessage(fromClientId: String, message: NetworkMessage) {
        when (message.type) {
            NetworkMessage.TYPE_COMBAT_DECLARATION, NetworkMessage.TYPE_COMBAT_ROLL -> handleCombatMessage(fromClientId, message)
            NetworkMessage.TYPE_SOCIAL_ROLL_REQUEST -> {
                val id = message.socialRequestId ?: return
                val skill = message.socialSkill ?: return
                if (_socialRequests.value.any { it.id == id }) return
                // Un seul joueur agit à la fois : demande arrivée pendant l'action d'un autre
                // (clics simultanés) → refusée, le joueur voit la discussion bloquée.
                if (_socialRequests.value.isNotEmpty()) {
                    val cancel = NetworkMessage(type = NetworkMessage.TYPE_SOCIAL_ROLL_CANCEL, socialRequestId = id)
                    gameServer?.sendToClient(fromClientId, networkJson.encodeToString(cancel))
                    gameServer?.sendToClient(fromClientId, networkJson.encodeToString(discussionLockMessage()))
                    return
                }
                _socialRequests.value = _socialRequests.value + SocialRollRequest(
                    id = id,
                    clientId = fromClientId,
                    characterName = message.socialCharacterName ?: "Un joueur",
                    pnjName = message.pnjName.orEmpty(),
                    skill = skill,
                    bonus = message.socialBonus ?: 0,
                )
                broadcastDiscussionLock()
            }
            NetworkMessage.TYPE_DISCUSSION_LEFT -> {
                handlePlayerLeftDiscussion(fromClientId, message.socialCharacterName, disconnected = false)
            }
            NetworkMessage.TYPE_SOCIAL_ROLL_RESULT -> {
                val id = message.socialRequestId ?: return
                _socialRequests.value = _socialRequests.value.map {
                    if (it.id == id) it.copy(dice = message.socialDice, total = message.socialTotal, nd = _discussionNd.value) else it
                }
            }
            NetworkMessage.TYPE_CHARACTER_SYNC_RESPONSE -> {
                message.character?.let {
                    lastCharacterSentToClient[fromClientId] = it
                    GameState.upsertCharacterFromNetwork(it)
                    demanderImagesAuJoueur(fromClientId, it)
                }
            }
            NetworkMessage.TYPE_STORY_IMAGES_REQUEST -> envoyerImagesHistoire(hostContext, message) {
                gameServer?.sendToClient(fromClientId, networkJson.encodeToString(it))
            }
            NetworkMessage.TYPE_STORY_IMAGE -> recevoirImageHistoire(hostContext, message)
            NetworkMessage.TYPE_CHARACTER_CLAIM_REQUEST -> {
                val characterId = message.characterId ?: return
                val alreadyClaimed = _claimedCharacters.value.containsKey(characterId)
                val character = GameState.characters.value.find { it.id == characterId }
                if (!alreadyClaimed && character != null) {
                    // La fiche part avec la réponse : pas de push en double (cf. lastCharacterSentToClient).
                    lastCharacterSentToClient[fromClientId] = character
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
            NetworkMessage.TYPE_CONTENT_MISSING -> {
                val manquants = message.manquants.orEmpty().takeIf { it.isNotEmpty() } ?: return
                val alerte = AlerteContenuManquant(
                    joueur = nomsClients[fromClientId] ?: "Un joueur",
                    sujet = message.sujetManquant.orEmpty(),
                    manquants = manquants,
                )
                _alertesContenuManquant.value = _alertesContenuManquant.value.filterNot {
                    it.joueur == alerte.joueur && it.sujet == alerte.sujet
                } + alerte
            }
            NetworkMessage.TYPE_CHARACTER_PROPOSAL -> {
                message.character?.let { proposed ->
                    _pendingProposals.value = _pendingProposals.value + (fromClientId to proposed)
                }
            }
            NetworkMessage.TYPE_PROPOSAL_DECISION -> {
                val proposalId = message.proposalId ?: return
                val decision = message.proposalDecision?.let { runCatching { ProposalDecision.valueOf(it) }.getOrNull() } ?: return
                val proposal = _activeGroupProposals.value[proposalId] ?: return
                // Vote d'un id qui n'est pas (ou plus, après reconnexion) rattaché à la proposition.
                if (fromClientId !in proposal.pendingClientIds) return
                val updated = proposal.copy(decisions = proposal.decisions + (fromClientId to decision))
                _activeGroupProposals.value = _activeGroupProposals.value + (proposalId to updated)
                broadcastProposalState(updated)
            }
            NetworkMessage.TYPE_MOUNT_BAGGAGE -> {
                val characterId = message.characterId ?: return
                val montureId = message.montureId ?: return
                val objet = message.objetNom ?: return
                // Seulement pour le personnage que ce joueur incarne.
                val incarne = _claimedCharacters.value[characterId] == fromClientId ||
                    _sentCharacterByClient.value[fromClientId] == characterId
                if (!incarne) return
                val groupe = GameState.mjGroups.value.firstOrNull { g -> g.mounts.any { it.id == montureId } } ?: return
                if (message.versMonture == true) GameState.rangerSurMonture(groupe.id, montureId, characterId, objet)
                else GameState.reprendreDeMonture(groupe.id, montureId, characterId, objet)
            }
            NetworkMessage.TYPE_MAP_TRACE -> {
                // Tracé d'un joueur : il devient le tracé de tous (MJ, table, autres joueurs).
                val nom = _connectedClients.value.firstOrNull { it.id == fromClientId }?.displayName ?: "Un joueur"
                val data = message.trace?.copy(auteur = nom)
                com.jc2.jdrcompagnon.feature_carte.presentation.TraceCarteSession.recevoir(data)
                gameServer?.sendToAll(networkJson.encodeToString(NetworkMessage(type = NetworkMessage.TYPE_MAP_TRACE, trace = data)))
            }
            NetworkMessage.TYPE_VOYAGE_PRET -> {
                val nom = _connectedClients.value.firstOrNull { it.id == fromClientId }?.displayName ?: return
                val session = com.jc2.jdrcompagnon.feature_carte.presentation.VoyageSession
                val route = message.voyage
                val actuel = session.etat.value
                if (route != null && (actuel == null || !actuel.memeTrajet(route))) {
                    session.proposer(route, par = nom, pretNom = nom, mjPret = false)
                } else {
                    session.marquerPret(nom)
                }
            }
            NetworkMessage.TYPE_CITY_POSITION -> recevoirPositionVille(fromClientId, message.positionVille)
            NetworkMessage.TYPE_SHOP_TRADE -> recevoirCommerce(fromClientId, message)
            NetworkMessage.TYPE_QUEST_ACCEPT -> {
                val questId = message.questId ?: return
                val nomPerso = GameState.characters.value.firstOrNull { it.id == message.characterId }?.name
                    ?: nomsClients[fromClientId] ?: "Un joueur"
                val titre = GameState.mjCampaigns.value.flatMap { it.quests }.firstOrNull { it.id == questId }?.title ?: return
                scope.launch {
                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                        // Inscription (rediffusée aux joueurs avec les quêtes) puis avis au MJ.
                        GameState.accepterQuete(questId, nomPerso)
                        hostContext?.let {
                            android.widget.Toast.makeText(it, "$nomPerso accepte la quête « $titre »", android.widget.Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
            NetworkMessage.TYPE_QUEST_ABANDON -> {
                val questId = message.questId ?: return
                val nomPerso = GameState.characters.value.firstOrNull { it.id == message.characterId }?.name
                    ?: nomsClients[fromClientId] ?: return
                val titre = GameState.mjCampaigns.value.flatMap { it.quests }.firstOrNull { it.id == questId }?.title ?: return
                scope.launch {
                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                        // Retrait (rediffusé aux joueurs avec les quêtes) puis avis au MJ.
                        GameState.abandonnerQuete(questId, nomPerso)
                        hostContext?.let {
                            android.widget.Toast.makeText(it, "$nomPerso abandonne la quête « $titre »", android.widget.Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
            NetworkMessage.TYPE_SERVICE_USED -> {
                val boutiqueId = message.serviceBoutiqueId ?: return
                val serviceNom = message.serviceNom ?: return
                val nomPerso = GameState.characters.value.firstOrNull { it.id == message.characterId }?.name ?: "Un joueur"
                scope.launch {
                    val boutique = com.jc2.jdrcompagnon.feature_boutique.domain.usecase.UtiliserServiceUseCase
                        .decompter(boutiqueId, serviceNom) ?: return@launch
                    val context = hostContext ?: return@launch
                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(
                            context,
                            "$nomPerso a utilisé « $serviceNom » chez ${boutique.nom}",
                            android.widget.Toast.LENGTH_LONG,
                        ).show()
                    }
                }
            }
            NetworkMessage.TYPE_LOOT_CLAIM_RESPONSE -> {
                val offerId = message.lootOfferId ?: return
                val offer = _activeLootOffers.value[offerId] ?: return
                if (message.accepted == true) {
                    val characterId = _claimedCharacters.value.entries.find { it.value == fromClientId }?.key
                        ?: _sentCharacterByClient.value[fromClientId]
                    characterId?.let { GameState.addItemToBackpack(it, offer.itemName) }
                }
                val responded = offer.respondedClientIds + fromClientId
                if (offer.pendingClientIds.all { it in responded }) {
                    closeLootOffer(offerId)
                } else {
                    _activeLootOffers.value = _activeLootOffers.value + (offerId to offer.copy(respondedClientIds = responded))
                }
            }
        }
    }

    fun stopHosting(context: Context) {
        clientsWatcherJob?.cancel()
        oublierBoutiqueInviteeMj()
        _alertesContenuManquant.value = emptyList()
        etatsWatcherJob?.cancel()
        groupInfoWatcherJob?.cancel()
        combatWatcherJob?.cancel()
        joueursWatcherJob?.cancel()
        lastCombatSentToClient.clear()
        gameServer?.stop()
        gameServer = null
        webServer?.stop()
        webServer = null
        _webServerUrl.value = null
        _activeGroupId.value = null
        _connectedClients.value = emptyList()
        _sentCharacterByClient.value = emptyMap()
        lastCharacterSentToClient.clear()
        _claimedCharacters.value = emptyMap()
        reservationsEnAttenteDeRetour.clear()
        nomsClients.clear()
        _pendingProposals.value = emptyMap()
        _activeLootOffers.value = emptyMap()
        _activeGroupProposals.value = emptyMap()
        _socialRequests.value = emptyList()
        _discussionNotices.value = emptyList()
        positionsVilleParClient.clear()
        _positionsVille.value = emptyList()
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

    /** Briefing PNJ reçu du MJ (événement de scène), affiché plein écran jusqu'à fermeture locale. */
    data class PnjBriefingUi(val name: String, val portraitId: String?)

    private val _pendingPnjBriefing = MutableStateFlow<PnjBriefingUi?>(null)
    val pendingPnjBriefing: StateFlow<PnjBriefingUi?> = _pendingPnjBriefing.asStateFlow()

    /** Le joueur quitte la discussion : fermeture locale, et le MJ est prévenu (sa demande en cours tombe). */
    fun leavePnjDiscussion(characterName: String?) {
        _pendingPnjBriefing.value = null
        _playerSocialRoll.value = null
        _playerDiscussionNotice.value = null
        val socket = currentSocket ?: return
        val message = NetworkMessage(type = NetworkMessage.TYPE_DISCUSSION_LEFT, socialCharacterName = characterName)
        scope.launch { runCatching { writeToHost(socket, message) } }
    }

    /** Joueur en train d'agir dans la discussion (diffusé par le MJ) ; null = discussion libre. */
    data class DiscussionLock(val requestId: String, val characterName: String)

    private val _discussionLock = MutableStateFlow<DiscussionLock?>(null)
    val discussionLock: StateFlow<DiscussionLock?> = _discussionLock.asStateFlow()

    // Information à montrer au joueur dans la discussion (ex. action annulée par le MJ).
    private val _playerDiscussionNotice = MutableStateFlow<String?>(null)
    val playerDiscussionNotice: StateFlow<String?> = _playerDiscussionNotice.asStateFlow()

    /**
     * Jet social en cours côté joueur : [mode] null = en attente de la décision du MJ ; [dice]
     * renseigné une fois lancé. Le joueur ne reçoit jamais le verdict (réussite/échec).
     */
    data class PlayerSocialRoll(
        val id: String,
        val skill: String,
        val bonus: Int,
        val mode: SocialRollMode? = null,
        val dice: List<Int>? = null,
        val total: Int? = null,
    )

    private val _playerSocialRoll = MutableStateFlow<PlayerSocialRoll?>(null)
    val playerSocialRoll: StateFlow<PlayerSocialRoll?> = _playerSocialRoll.asStateFlow()

    /** Le joueur choisit une compétence sociale : la demande part au MJ. */
    fun requestSocialRoll(characterName: String, pnjName: String, skill: String, bonus: Int) {
        val socket = currentSocket ?: return
        if (_discussionLock.value != null) return
        val id = java.util.UUID.randomUUID().toString()
        _playerDiscussionNotice.value = null
        _playerSocialRoll.value = PlayerSocialRoll(id = id, skill = skill, bonus = bonus)
        val message = NetworkMessage(
            type = NetworkMessage.TYPE_SOCIAL_ROLL_REQUEST,
            socialRequestId = id,
            socialCharacterName = characterName,
            pnjName = pnjName,
            socialSkill = skill,
            socialBonus = bonus,
        )
        scope.launch { runCatching { writeToHost(socket, message) } }
    }

    /**
     * Le joueur lance son d20 (deux avec avantage/désavantage) et envoie le résultat au MJ.
     * @param manualDice valeurs saisies par le joueur qui a lancé un vrai dé (null = tirage de l'appli).
     */
    fun rollSocial(manualDice: List<Int>? = null) {
        val roll = _playerSocialRoll.value ?: return
        val mode = roll.mode ?: return
        if (roll.dice != null) return
        val nbDes = if (mode == SocialRollMode.NORMAL) 1 else 2
        if (manualDice != null && (manualDice.size != nbDes || manualDice.any { it !in 1..20 })) return
        val dice = manualDice ?: List(nbDes) { (1..20).random() }
        val retenu = when (mode) {
            SocialRollMode.AVANTAGE -> dice.max()
            SocialRollMode.DESAVANTAGE -> dice.min()
            else -> dice.first()
        }
        val total = retenu + roll.bonus
        _playerSocialRoll.value = roll.copy(dice = dice, total = total)
        val socket = currentSocket ?: return
        val message = NetworkMessage(
            type = NetworkMessage.TYPE_SOCIAL_ROLL_RESULT,
            socialRequestId = roll.id,
            socialDice = dice,
            socialTotal = total,
        )
        scope.launch { runCatching { writeToHost(socket, message) } }
    }

    /** Le joueur revient au choix des compétences (nouveau jet). */
    fun clearPlayerSocialRoll() {
        _playerSocialRoll.value = null
    }

    /** Combat en cours vu par ce joueur, diffusé par le MJ (null = aucun). */
    private val _combatEnCours = MutableStateFlow<CombatJoueurData?>(null)
    val combatEnCours: StateFlow<CombatJoueurData?> = _combatEnCours.asStateFlow()

    /** Le joueur annonce (ou change) son action pour le round en cours. */
    fun declarerActionCombat(
        libelle: String,
        actionId: String?,
        cibleId: String?,
        cibleNom: String?,
        detail: String?,
        deplacementCibleId: String? = null,
        distanceVisee: String? = null,
        attaques: List<String> = emptyList(),
        ciblesZone: List<String> = emptyList(),
        zone: String? = null,
        zoneVisee: Int? = null,
    ) {
        val combat = _combatEnCours.value ?: return
        val socket = currentSocket ?: return
        val declaration = DeclarationJoueurData(
            combat.id, combat.round, libelle, actionId, cibleId, cibleNom, detail?.takeIf { it.isNotBlank() },
            deplacementCibleId.takeIf { distanceVisee != null }, distanceVisee, attaques, ciblesZone, zone, zoneVisee,
        )
        // Affichage immédiat, confirmé par le prochain TYPE_COMBAT_STATE du MJ.
        _combatEnCours.value = combat.copy(maDeclaration = declaration)
        val message = NetworkMessage(type = NetworkMessage.TYPE_COMBAT_DECLARATION, combatDeclaration = declaration)
        scope.launch { runCatching { writeToHost(socket, message) } }
    }

    /** Envoie au MJ un jet de dés du combat (tiré par l'appli, ou valeur d'un vrai dé si [manuel]). */
    fun envoyerJetCombat(
        libelle: String,
        formule: String,
        des: List<Int>,
        bonus: Int,
        manuel: Boolean,
        // Jet d'attaque : dégâts lancés en même temps (formule, dés, bonus).
        formuleDegats: String? = null,
        desDegats: List<Int> = emptyList(),
        bonusDegats: Int = 0,
        // Cible propre à ce jet (Imposition des mains…), sinon celle de la déclaration.
        cibleId: String? = null,
        // Arme (ou sort) et type de dégâts du jet, pour que le MJ applique résistances et bonus.
        arme: String? = null,
        typeDegats: String? = null,
    ): Boolean {
        val combat = _combatEnCours.value ?: return false
        val socket = currentSocket ?: return false
        val totalDegats = formuleDegats?.let { (desDegats.sum() + bonusDegats).coerceAtLeast(0) }
        val jet = JetCombatData(combat.id, libelle, formule, des, bonus, des.sum() + bonus, manuel, formuleDegats, desDegats, totalDegats, cibleId, arme, typeDegats)
        val message = NetworkMessage(type = NetworkMessage.TYPE_COMBAT_ROLL, combatJet = jet)
        scope.launch { runCatching { writeToHost(socket, message) } }
        return true
    }

    /** Groupe actif de la partie, diffusé par le MJ (null = aucun ou hors connexion). */
    private val _networkGroup = MutableStateFlow<NetworkGroupInfo?>(null)
    val networkGroup: StateFlow<NetworkGroupInfo?> = _networkGroup.asStateFlow()

    /** Campagne sélectionnée par le MJ (id, titre), null = aucune ou hors connexion. */
    private val _networkCampagne = MutableStateFlow<Pair<String, String>?>(null)
    val networkCampagne: StateFlow<Pair<String, String>?> = _networkCampagne.asStateFlow()

    /** Proposition de repos long du MJ en attente de réponse du joueur. */
    private val _pendingRestOffer = MutableStateFlow<String?>(null)
    val pendingRestOffer: StateFlow<String?> = _pendingRestOffer.asStateFlow()
    fun dismissRestOffer() { _pendingRestOffer.value = null }

    // ── Voyage (joueur) ──
    /** Pseudo de ce joueur dans la partie (celui que le MJ voit dans la liste des « prêts »). */
    private val _nomJoueurReseau = MutableStateFlow<String?>(null)
    val nomJoueurReseau: StateFlow<String?> = _nomJoueurReseau.asStateFlow()

    // Route reçue à annoncer au joueur (« Route sélectionnée »), ou départ du groupe (enRoute).
    private val _voyageAnnonce = MutableStateFlow<VoyageData?>(null)
    val voyageAnnonce: StateFlow<VoyageData?> = _voyageAnnonce.asStateFlow()
    private var derniereAnnonce: Pair<String, Int>? = null
    private var departAnnonce: String? = null
    fun dismissVoyageAnnonce() { _voyageAnnonce.value = null }

    /**
     * Joueur : prêt au départ. [route] = sa propre route quand il en propose une (ou en modifie
     * une) ; null pour valider la route partagée.
     */
    fun voyagePret(route: VoyageData?) {
        val socket = currentSocket ?: return
        scope.launch { runCatching { writeToHost(socket, NetworkMessage(type = NetworkMessage.TYPE_VOYAGE_PRET, voyage = route)) } }
    }

    /**
     * Joueur : range [objet] du sac de son personnage dans les sacoches d'une monture
     * ([versMonture]), ou l'en reprend. Connecté, c'est le MJ qui applique (fiche et groupe) ;
     * hors connexion, le changement est fait localement.
     */
    fun transfererBagage(groupId: String, characterId: String, montureId: String, objet: String, versMonture: Boolean) {
        val socket = currentSocket
        if (_role.value == SessionRole.PLAYER && socket != null) {
            val message = NetworkMessage(
                type = NetworkMessage.TYPE_MOUNT_BAGGAGE,
                characterId = characterId,
                montureId = montureId,
                objetNom = objet,
                versMonture = versMonture,
            )
            scope.launch { runCatching { writeToHost(socket, message) } }
        } else if (versMonture) {
            GameState.rangerSurMonture(groupId, montureId, characterId, objet)
        } else {
            GameState.reprendreDeMonture(groupId, montureId, characterId, objet)
        }
    }

    private fun recevoirVoyage(data: VoyageData?) {
        com.jc2.jdrcompagnon.feature_carte.presentation.VoyageSession.recevoir(data)
        if (data == null) {
            _voyageAnnonce.value = null
            return
        }
        val moi = _nomJoueurReseau.value
        when {
            // Départ : annoncé une fois par voyage.
            data.enRoute -> if (departAnnonce != data.id) {
                departAnnonce = data.id
                _voyageAnnonce.value = data
            }
            // Nouvelle route (ou route modifiée) que ce joueur n'a pas encore validée.
            derniereAnnonce != (data.id to data.version) && (moi == null || moi !in data.prets) -> {
                derniereAnnonce = data.id to data.version
                _voyageAnnonce.value = data
            }
            moi != null && moi in data.prets && _voyageAnnonce.value?.enRoute != true -> _voyageAnnonce.value = null
        }
    }

    private var playerContext: Context? = null

    /** Quêtes visibles diffusées par le MJ (null = pas reçues : hors connexion). */
    private val _networkQuests = MutableStateFlow<List<QuestJoueurData>?>(null)
    val networkQuests: StateFlow<List<QuestJoueurData>?> = _networkQuests.asStateFlow()

    /** Épreuve environnementale en cours diffusée par le MJ (null = aucune). */
    private val _epreuveEnCours = MutableStateFlow<EpreuveJoueurData?>(null)
    val epreuveEnCours: StateFlow<EpreuveJoueurData?> = _epreuveEnCours.asStateFlow()

    // Id de l'épreuve masquée par le joueur : elle ne réapparaît que pour une nouvelle épreuve
    // ou à son issue (réussite/échec), pas à chaque point de Progrès.
    private val _epreuveMasquee = MutableStateFlow<String?>(null)
    val epreuveMasquee: StateFlow<String?> = _epreuveMasquee.asStateFlow()

    fun masquerEpreuve() {
        val epreuve = _epreuveEnCours.value ?: return
        if (epreuve.issue != null) _epreuveEnCours.value = null else _epreuveMasquee.value = epreuve.id
    }

    /** Offre de butin de groupe en cours, affichée en carte au-dessus de l'inventaire du joueur. */
    data class LootOffer(val offerId: String, val itemName: String)

    // File d'attente : le MJ peut proposer plusieurs objets d'un coup ("Tout envoyer au groupe"
    // depuis un tirage de butin) ; ils sont présentés un par un, dans l'ordre d'arrivée.
    private val _pendingLootOffers = MutableStateFlow<List<LootOffer>>(emptyList())
    val pendingLootOffer: StateFlow<LootOffer?> = _pendingLootOffers
        .map { it.firstOrNull() }
        .stateIn(scope, SharingStarted.Eagerly, null)

    /** Répond à une offre de butin de groupe ("je le veux" ou fermeture sans le prendre). */
    fun respondToLootOffer(offerId: String, wants: Boolean) {
        val socket = currentSocket
        if (socket != null) {
            val message = NetworkMessage(type = NetworkMessage.TYPE_LOOT_CLAIM_RESPONSE, lootOfferId = offerId, accepted = wants)
            scope.launch {
                runCatching { writeToHost(socket, message) }
            }
        }
        _pendingLootOffers.value = _pendingLootOffers.value.filterNot { it.offerId == offerId }
    }

    /**
     * Joueur : signale au MJ qu'un personnage a utilisé (et déjà payé, cf. CommerceLieuDialog) un
     * service d'une boutique du lieu. Sans connexion, rien n'est envoyé.
     */
    /**
     * Le joueur accepte une quête du panneau de quêtes : envoyée au MJ en partie réseau (qui
     * l'inscrit et rediffuse les quêtes), sinon inscrite directement sur cet appareil.
     */
    fun accepterQuete(questId: String, characterId: String, nomPersonnage: String) {
        val socket = currentSocket
        if (_role.value == SessionRole.PLAYER && socket != null) {
            val message = NetworkMessage(type = NetworkMessage.TYPE_QUEST_ACCEPT, questId = questId, characterId = characterId)
            scope.launch { runCatching { writeToHost(socket, message) } }
        } else {
            GameState.accepterQuete(questId, nomPersonnage)
        }
    }

    /** Le joueur abandonne une quête acceptée : envoyé au MJ en partie réseau, sinon retiré sur cet appareil. */
    fun abandonnerQuete(questId: String, characterId: String, nomPersonnage: String) {
        val socket = currentSocket
        if (_role.value == SessionRole.PLAYER && socket != null) {
            val message = NetworkMessage(type = NetworkMessage.TYPE_QUEST_ABANDON, questId = questId, characterId = characterId)
            scope.launch { runCatching { writeToHost(socket, message) } }
        } else {
            GameState.abandonnerQuete(questId, nomPersonnage)
        }
    }

    fun signalerServiceUtilise(characterId: String, boutiqueId: String, serviceNom: String) {
        val socket = currentSocket ?: return
        val message = NetworkMessage(
            type = NetworkMessage.TYPE_SERVICE_USED,
            characterId = characterId,
            serviceBoutiqueId = boutiqueId,
            serviceNom = serviceNom,
        )
        scope.launch { runCatching { writeToHost(socket, message) } }
    }

    // ── Visite d'une ville : où se trouve chaque joueur (TYPE_CITY_POSITION(S)) ──

    // Joueur : lieu de la ville où il se trouve (null = hors ville), renvoyé au MJ à chaque
    // changement et à chaque (re)connexion.
    private val _maPositionVille = MutableStateFlow<PositionVilleData?>(null)

    // Joueur : positions de tous les joueurs reçues du MJ ; MJ : positions de ses joueurs connectés.
    private val _positionsVille = MutableStateFlow<List<PositionVilleData>>(emptyList())
    val positionsVille: StateFlow<List<PositionVilleData>> = _positionsVille.asStateFlow()

    // MJ : position de chaque client connecté qui visite une ville.
    private val positionsVilleParClient = java.util.concurrent.ConcurrentHashMap<String, PositionVilleData>()

    init {
        scope.launch {
            // Personnage incarné aussi : le MJ affiche son nom à côté du pseudo.
            combine(_playerState, _maPositionVille, _claimedCharacterId) { etat, position, _ -> etat to position }
                .collect { (etat, position) ->
                    val socket = currentSocket
                    if (_role.value == SessionRole.PLAYER && etat == PlayerConnectionState.CONNECTED && socket != null) {
                        runCatching { writeToHost(socket, NetworkMessage(type = NetworkMessage.TYPE_CITY_POSITION, positionVille = position)) }
                    }
                }
        }
    }

    init {
        // MJ : un joueur qui réclame (ou rend) un personnage après avoir signalé sa position :
        // le nom du personnage affiché à côté de sa position suit.
        scope.launch {
            _claimedCharacters.collect {
                if (_role.value == SessionRole.HOST && positionsVilleParClient.isNotEmpty()) diffuserPositionsVille()
            }
        }
    }

    /** Joueur : il se rend dans un lieu de la ville ([position] null = il a quitté la ville). */
    fun signalerPositionVille(position: PositionVilleData?) {
        _maPositionVille.value = position
    }

    /** Positions avec le personnage incarné par chaque joueur lu au moment de l'envoi (réclamation tardive comprise). */
    private fun positionsAJour(): List<PositionVilleData> = positionsVilleParClient.map { (clientId, p) ->
        val personnage = characterIdOfClient(clientId)?.let { id -> GameState.characters.value.firstOrNull { it.id == id }?.name }
        p.copy(clientId = clientId, personnage = personnage ?: p.personnage)
    }

    private fun positionsVilleMessage() =
        NetworkMessage(type = NetworkMessage.TYPE_CITY_POSITIONS, positionsVille = positionsAJour())

    private fun diffuserPositionsVille() {
        _positionsVille.value = positionsAJour()
        gameServer?.sendToAll(networkJson.encodeToString(positionsVilleMessage()))
    }

    private fun sendPositionsVilleTo(clientId: String) {
        gameServer?.sendToClient(clientId, networkJson.encodeToString(positionsVilleMessage()))
    }

    /** MJ : un joueur a changé de lieu ; son pseudo et son personnage sont ajoutés avant diffusion. */
    private fun recevoirPositionVille(fromClientId: String, position: PositionVilleData?) {
        if (position == null) {
            positionsVilleParClient.remove(fromClientId)
        } else {
            val joueur = nomsClients[fromClientId]
                ?: _connectedClients.value.firstOrNull { it.id == fromClientId }?.displayName ?: "Un joueur"
            val personnage = characterIdOfClient(fromClientId)
                ?.let { id -> GameState.characters.value.firstOrNull { it.id == id }?.name }.orEmpty()
            positionsVilleParClient[fromClientId] = position.copy(joueur = joueur, personnage = personnage, clientId = fromClientId)
        }
        diffuserPositionsVille()
    }

    // ── Commerce dans une boutique du lieu (TYPE_SHOP_TRADE) ──

    /** Compte rendu de la dernière transaction (achat ou vente), affiché par l'écran de commerce. */
    data class ResultatCommerce(val accepte: Boolean, val message: String)

    private val _resultatCommerce = MutableStateFlow<ResultatCommerce?>(null)
    val resultatCommerce: StateFlow<ResultatCommerce?> = _resultatCommerce.asStateFlow()

    fun effacerResultatCommerce() {
        _resultatCommerce.value = null
    }

    /**
     * Acheter ([achat]) ou vendre [objet] dans une boutique. Connecté à une partie, c'est le MJ qui
     * vérifie et applique (stock, caisse, fiche) puis répond ; sinon, la transaction est faite sur
     * l'appareil. Le résultat arrive dans [resultatCommerce].
     */
    fun commercer(characterId: String, boutiqueId: String, objet: String, achat: Boolean) {
        val socket = currentSocket
        if (_role.value == SessionRole.PLAYER && socket != null) {
            val message = NetworkMessage(
                type = NetworkMessage.TYPE_SHOP_TRADE,
                characterId = characterId,
                commerceBoutiqueId = boutiqueId,
                objetNom = objet,
                commerceAchat = achat,
            )
            scope.launch {
                runCatching { writeToHost(socket, message) }
                    .onFailure { _resultatCommerce.value = ResultatCommerce(false, "Le MJ est injoignable, réessayez.") }
            }
        } else {
            scope.launch {
                val r = appliquerCommerce(boutiqueId, characterId, objet, achat)
                _resultatCommerce.value = ResultatCommerce(r.accepte, r.message)
            }
        }
    }

    private suspend fun appliquerCommerce(boutiqueId: String, characterId: String, objet: String, achat: Boolean) =
        if (achat) com.jc2.jdrcompagnon.feature_boutique.domain.usecase.CommerceBoutiqueUseCase.acheter(boutiqueId, characterId, objet)
        else com.jc2.jdrcompagnon.feature_boutique.domain.usecase.CommerceBoutiqueUseCase.vendre(boutiqueId, characterId, objet)

    /** MJ : transaction demandée par un joueur, pour le personnage qu'il incarne seulement. */
    private fun recevoirCommerce(fromClientId: String, message: NetworkMessage) {
        val boutiqueId = message.commerceBoutiqueId ?: return
        val objet = message.objetNom ?: return
        val characterId = message.characterId ?: return
        val incarne = _claimedCharacters.value[characterId] == fromClientId ||
            _sentCharacterByClient.value[fromClientId] == characterId
        if (!incarne) return
        scope.launch {
            val r = appliquerCommerce(boutiqueId, characterId, objet, message.commerceAchat == true)
            gameServer?.sendToClient(
                fromClientId,
                networkJson.encodeToString(
                    NetworkMessage(type = NetworkMessage.TYPE_SHOP_TRADE_RESULT, accepted = r.accepte, commerceMessage = r.message)
                ),
            )
            val context = hostContext ?: return@launch
            if (r.accepte) {
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(context, r.message, android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // ── Invitation dans une boutique (TYPE_SHOP_INVITE) ──

    // MJ : dernière boutique où les joueurs ont été invités, suivie pour leur renvoyer stock et caisse.
    private var boutiqueInviteeJob: Job? = null
    private var boutiqueInvitee: BoutiqueJoueurData? = null

    /**
     * MJ : invite tous les joueurs connectés à afficher l'écran de la boutique [boutiqueId] (lien
     * #boutique: de la lecture de scénario). Retourne false sans partie hébergée ou si la boutique
     * n'existe plus. Ses changements (achats, ventes, services) sont ensuite renvoyés en mise à jour.
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    suspend fun inviterBoutique(boutiqueId: String): Boolean {
        if (_role.value != SessionRole.HOST || gameServer == null) return false
        val repo = com.jc2.jdrcompagnon.di.BoutiqueDependencies.repository
        val boutique = repo.getBoutique(boutiqueId) ?: return false
        val data = com.jc2.jdrcompagnon.feature_carte.data.CarteSyncReseau.boutiqueJoueur(boutique)
        boutiqueInvitee = data
        gameServer?.sendToAll(networkJson.encodeToString(NetworkMessage(type = NetworkMessage.TYPE_SHOP_INVITE, boutiqueInvitation = data, accepted = true)))
        boutiqueInviteeJob?.cancel()
        boutiqueInviteeJob = scope.launch {
            repo.observerToutesLesBoutiques()
                .map { liste -> liste.firstOrNull { it.id == boutiqueId }?.let { com.jc2.jdrcompagnon.feature_carte.data.CarteSyncReseau.boutiqueJoueur(it) } }
                .filterNotNull()
                .distinctUntilChanged()
                .collect { maj ->
                    if (maj == boutiqueInvitee) return@collect
                    boutiqueInvitee = maj
                    gameServer?.sendToAll(networkJson.encodeToString(NetworkMessage(type = NetworkMessage.TYPE_SHOP_INVITE, boutiqueInvitation = maj, accepted = false)))
                }
        }
        return true
    }

    /** Joueur (re)connecté : données à jour de la boutique invitée, sans nouvelle invitation. */
    private fun sendBoutiqueInviteeTo(clientId: String) {
        val data = boutiqueInvitee ?: return
        gameServer?.sendToClient(clientId, networkJson.encodeToString(NetworkMessage(type = NetworkMessage.TYPE_SHOP_INVITE, boutiqueInvitation = data, accepted = false)))
    }

    private fun oublierBoutiqueInviteeMj() {
        boutiqueInviteeJob?.cancel()
        boutiqueInviteeJob = null
        boutiqueInvitee = null
    }

    // Joueur : boutique où le MJ l'a invité (données tenues à jour), et invitation encore sans réponse.
    private val _boutiqueInvitation = MutableStateFlow<BoutiqueJoueurData?>(null)
    val boutiqueInvitation: StateFlow<BoutiqueJoueurData?> = _boutiqueInvitation.asStateFlow()
    private val _invitationBoutiqueEnAttente = MutableStateFlow(false)
    val invitationBoutiqueEnAttente: StateFlow<Boolean> = _invitationBoutiqueEnAttente.asStateFlow()

    private fun recevoirInvitationBoutique(message: NetworkMessage) {
        val data = message.boutiqueInvitation ?: return
        // Une mise à jour d'une autre boutique que celle affichée ne la remplace pas.
        if (message.accepted != true && _boutiqueInvitation.value?.id != data.id && _boutiqueInvitation.value != null) return
        _boutiqueInvitation.value = data
        if (message.accepted == true) _invitationBoutiqueEnAttente.value = true
    }

    /** Joueur : invitation acceptée ou refusée (l'écran de la boutique est alors ouvert, ou non). */
    fun repondreInvitationBoutique() {
        _invitationBoutiqueEnAttente.value = false
    }

    private fun oublierInvitationBoutique() {
        _boutiqueInvitation.value = null
        _invitationBoutiqueEnAttente.value = false
    }

    /** Proposition de groupe affichée dans l'overlay dès sa réception, jusqu'à décision du joueur. */
    data class PendingGroupProposalUi(val proposalId: String, val title: String, val description: String, val rewardLabel: String)

    private val _pendingGroupProposal = MutableStateFlow<PendingGroupProposalUi?>(null)
    val pendingGroupProposal: StateFlow<PendingGroupProposalUi?> = _pendingGroupProposal.asStateFlow()

    /** Dernier appel du MJ (changement de scène) pour une proposition encore en attente. */
    data class ProposalFinalCallUi(val proposalId: String, val title: String)

    private val _pendingProposalFinalCall = MutableStateFlow<ProposalFinalCallUi?>(null)
    val pendingProposalFinalCall: StateFlow<ProposalFinalCallUi?> = _pendingProposalFinalCall.asStateFlow()

    /** État agrégé de la (ou des) proposition(s) de groupe en cours, pour l'écran menu latéral. */
    private val _activeProposalState = MutableStateFlow<Map<String, GroupProposal>>(emptyMap())
    val activeProposalState: StateFlow<Map<String, GroupProposal>> = _activeProposalState.asStateFlow()

    /** Répond (accepter/décliner/passer) à la proposition de groupe affichée par l'overlay. */
    fun respondToGroupProposal(proposalId: String, decision: ProposalDecision) {
        val socket = currentSocket
        if (socket != null) {
            val message = NetworkMessage(
                type = NetworkMessage.TYPE_PROPOSAL_DECISION,
                proposalId = proposalId,
                proposalDecision = decision.name,
            )
            scope.launch {
                runCatching { writeToHost(socket, message) }
            }
        }
        _pendingGroupProposal.value = null
    }

    /** Ferme localement l'alerte de dernier appel (n'annule pas la proposition, juste ce rappel). */
    fun dismissProposalFinalCall() {
        _pendingProposalFinalCall.value = null
    }

    /** Conflit détecté à la réservation/reconnexion : la fiche locale gardée diffère de celle du MJ. */
    data class CharacterVersionConflict(val local: Character, val remote: Character, val diffs: List<CharacterFieldDiff>)

    private val _pendingCharacterVersionConflict = MutableStateFlow<CharacterVersionConflict?>(null)
    val pendingCharacterVersionConflict: StateFlow<CharacterVersionConflict?> = _pendingCharacterVersionConflict.asStateFlow()

    /** Le joueur choisit d'abandonner sa version locale et d'adopter celle du MJ. */
    fun resolveCharacterConflictKeepHost() {
        val conflict = _pendingCharacterVersionConflict.value ?: return
        GameState.upsertCharacterFromNetwork(conflict.remote)
        _claimedCharacterId.value = conflict.remote.id
        demanderImagesAuMj(currentSocket, conflict.remote)
        _pendingCharacterVersionConflict.value = null
    }

    /** Le joueur choisit de proposer sa version locale au MJ pour validation (garde sa fiche en attendant). */
    fun resolveCharacterConflictProposeMine() {
        val conflict = _pendingCharacterVersionConflict.value ?: return
        proposeCharacter(conflict.local)
        _pendingCharacterVersionConflict.value = null
    }

    // ── Imports partagés par le MJ (voir ImportsPartages) et contenu manquant ──

    private var urlTableMj: String? = null
    private var importsMj: List<ImportPartageData> = emptyList()

    // Imports du MJ absents de la bibliothèque du joueur : il est invité à les télécharger.
    private val _importsManquants = MutableStateFlow<List<ImportsPartages.ImportManquant>>(emptyList())
    internal val importsManquants: StateFlow<List<ImportsPartages.ImportManquant>> = _importsManquants.asStateFlow()

    // « Plus tard » : l'invitation ne revient qu'au prochain envoi de la liste par le MJ.
    private val _importsMasques = MutableStateFlow(false)
    val importsMasques: StateFlow<Boolean> = _importsMasques.asStateFlow()

    fun masquerImportsManquants() { _importsMasques.value = true }

    /** Téléchargement en cours (import en cours de téléchargement), ou erreurs du dernier. */
    data class TelechargementImports(val enCours: String? = null, val erreurs: List<String> = emptyList())

    private val _telechargementImports = MutableStateFlow<TelechargementImports?>(null)
    val telechargementImports: StateFlow<TelechargementImports?> = _telechargementImports.asStateFlow()

    fun fermerTelechargementImports() { _telechargementImports.value = null }

    /** Télécharge depuis la page table du MJ tous les imports manquants, images comprises. */
    fun telechargerImportsManquants() {
        val context = playerContext ?: return
        val aTelecharger = _importsManquants.value
        scope.launch {
            val url = urlTableMj
            val erreurs = mutableListOf<String>()
            if (url == null) {
                erreurs += "La page table du MJ est indisponible : téléchargement impossible."
            } else {
                aTelecharger.forEach { manquant ->
                    _telechargementImports.value = TelechargementImports(enCours = manquant.import.nom)
                    runCatching { ImportsPartages.telecharger(context, url, manquant) }
                        .onFailure { erreurs += "${manquant.import.nom} : ${it.message ?: it.javaClass.simpleName}" }
                }
            }
            _importsManquants.value = runCatching { ImportsPartages.manquants(context, importsMj) }.getOrDefault(emptyList())
            _telechargementImports.value = TelechargementImports(erreurs = erreurs)
        }
    }

    // Contenu introuvable pour ce que le joueur vient de récupérer (aussi signalé au MJ).
    private val _contenuManquant = MutableStateFlow<AlerteContenuManquant?>(null)
    val contenuManquant: StateFlow<AlerteContenuManquant?> = _contenuManquant.asStateFlow()

    fun dismissContenuManquant() { _contenuManquant.value = null }

    // Derniers manques signalés par sujet : la fiche est renvoyée à chaque modification, le MJ
    // n'est prévenu qu'une fois par manque.
    private val manquantsSignales = java.util.concurrent.ConcurrentHashMap<String, Set<String>>()

    private fun verifierContenu(socket: Socket, character: Character) {
        val context = playerContext ?: return
        scope.launch {
            val manquants = runCatching { VerificationContenu.manquantsPersonnage(context, character) }.getOrDefault(emptyList())
            val dejaSignales = manquantsSignales[character.id].orEmpty()
            manquantsSignales[character.id] = manquants.toSet()
            if (manquants.isNotEmpty() && !dejaSignales.containsAll(manquants)) signalerContenuManquant(socket, character.name, manquants)
        }
    }

    private fun signalerContenuManquant(socket: Socket, sujet: String, manquants: List<String>) {
        _contenuManquant.value = AlerteContenuManquant(joueur = _nomJoueurReseau.value.orEmpty(), sujet = sujet, manquants = manquants)
        val message = NetworkMessage(type = NetworkMessage.TYPE_CONTENT_MISSING, manquants = manquants, sujetManquant = sujet)
        runCatching { writeToHost(socket, message) }
    }

    /** Instance partagée de découverte, créée à la demande. */
    fun discovery(context: Context): GameClientDiscovery {
        return discoveryInstance ?: GameClientDiscovery(context.applicationContext).also {
            discoveryInstance = it
        }
    }

    fun connectToServer(context: Context, server: DiscoveredServer, playerName: String) {
        val appContext = context.applicationContext
        playerContext = appContext
        manualDisconnect = false
        _role.value = SessionRole.PLAYER
        _nomJoueurReseau.value = playerName
        _availableCharacters.value = emptyList()
        _claimedCharacterId.value = null
        _proposalPending.value = false
        pendingProposalCharacterId = null
        _pendingCharacterVersionConflict.value = null
        _networkGroup.value = null
        _networkQuests.value = null
        _networkCampagne.value = null
        com.jc2.jdrcompagnon.feature_carte.data.CarteSyncReseau.oublierBoutiquesReseau()
        oublierInvitationBoutique()
        _positionsVille.value = emptyList()
        _resultatCommerce.value = null
        _pendingRestOffer.value = null
        recevoirVoyage(null)
        _activeProposalState.value = emptyMap()
        urlTableMj = null
        importsMj = emptyList()
        _importsManquants.value = emptyList()
        _telechargementImports.value = null
        _contenuManquant.value = null
        manquantsSignales.clear()
        reconnectJob?.cancel()
        readJob?.cancel()
        _playerState.value = PlayerConnectionState.CONNECTING
        _lastConnectionError.value = null
        scope.launch {
            try {
                val socket = discovery(appContext).connect(server, playerName)
                if (manualDisconnect) {
                    runCatching { socket.close() }
                    return@launch
                }
                currentSocket = socket
                _playerState.value = PlayerConnectionState.CONNECTED
                LanConnectionService.ensureStarted(appContext, "Connecté à la partie du MJ")
                listenUntilDisconnected(appContext, socket, server, playerName)
            } catch (e: Exception) {
                if (manualDisconnect) return@launch
                _lastConnectionError.value = e.message ?: e.javaClass.simpleName
                scheduleReconnect(appContext, server, playerName, attempt = 1)
            }
        }
    }

    /** Demande à réserver un personnage disponible du groupe. */
    fun requestClaimCharacter(characterId: String) {
        val socket = currentSocket ?: return
        val message = NetworkMessage(type = NetworkMessage.TYPE_CHARACTER_CLAIM_REQUEST, characterId = characterId)
        // Écriture socket bloquante : toujours hors thread principal (appelé
        // depuis un onClick Compose), sinon NetworkOnMainThreadException
        // silencieusement avalée par runCatching.
        scope.launch {
            runCatching {
                writeToHost(socket, message)
            }
        }
    }

    /** Propose un des personnages de son propre appareil (aucun du groupe n'était disponible). */
    fun proposeCharacter(character: Character) {
        val socket = currentSocket ?: return
        pendingProposalCharacterId = character.id
        _proposalPending.value = true
        val message = NetworkMessage(type = NetworkMessage.TYPE_CHARACTER_PROPOSAL, character = character)
        scope.launch {
            runCatching {
                writeToHost(socket, message)
            }
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
                // Le MJ n'est plus joignable : ses dialogues bloquants (butin, épreuve,
                // briefing…) ne recevront jamais de suite et coinceraient le joueur.
                clearPlayerPendingInteractions()
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
                message.character?.let {
                    GameState.upsertCharacterFromNetwork(it)
                    verifierContenu(socket, it)
                    demanderImagesAuMj(socket, it)
                }
            }
            NetworkMessage.TYPE_STORY_IMAGES_REQUEST -> envoyerImagesHistoire(playerContext, message) { writeToHost(socket, it) }
            NetworkMessage.TYPE_STORY_IMAGE -> recevoirImageHistoire(playerContext, message)
            NetworkMessage.TYPE_SHARED_IMPORTS -> {
                val context = playerContext ?: return
                urlTableMj = message.urlTable
                importsMj = message.imports.orEmpty()
                _importsManquants.value = runCatching { ImportsPartages.manquants(context, importsMj) }.getOrDefault(emptyList())
                _importsMasques.value = false
            }
            NetworkMessage.TYPE_AVAILABLE_CHARACTERS -> {
                _availableCharacters.value = message.characters ?: emptyList()
            }
            NetworkMessage.TYPE_CHARACTER_CLAIM_RESPONSE -> {
                if (message.accepted == true && message.character != null) {
                    val remote = message.character
                    verifierContenu(socket, remote)
                    val local = GameState.characters.value.find { it.id == remote.id }
                    if (local != null && remote.id == _claimedCharacterId.value) {
                        // Reconnexion : c'est déjà son personnage, sa fiche locale (à jour) est renvoyée au MJ.
                        sendCharacterUpdateToHost(local)
                    } else if (local != null && local != remote) {
                        // Le joueur avait gardé une version locale (édition hors-ligne) différente
                        // de celle du MJ : on ne l'écrase pas silencieusement, cf. point 4 du plan.
                        _pendingCharacterVersionConflict.value = CharacterVersionConflict(
                            local = local,
                            remote = remote,
                            diffs = diffCharacters(local, remote),
                        )
                    } else {
                        GameState.upsertCharacterFromNetwork(remote)
                        _claimedCharacterId.value = remote.id
                        demanderImagesAuMj(socket, remote)
                    }
                }
                // Si refusé : le joueur reste sur la liste, mise à jour via le prochain TYPE_AVAILABLE_CHARACTERS.
            }
            NetworkMessage.TYPE_CHARACTER_PROPOSAL_RESULT -> {
                _proposalPending.value = false
                if (message.accepted == true) {
                    _claimedCharacterId.value = pendingProposalCharacterId
                } else if (message.character != null) {
                    // Version locale refusée après un conflit : on adopte celle du MJ jointe au refus.
                    GameState.upsertCharacterFromNetwork(message.character)
                    _claimedCharacterId.value = message.character.id
                    demanderImagesAuMj(socket, message.character)
                }
                pendingProposalCharacterId = null
            }
            NetworkMessage.TYPE_CHARACTER_SYNC_REQUEST -> {
                val characterId = message.characterId ?: return
                val character = GameState.characters.value.find { it.id == characterId } ?: return
                val response = NetworkMessage(type = NetworkMessage.TYPE_CHARACTER_SYNC_RESPONSE, character = character)
                runCatching {
                    writeToHost(socket, response)
                }
            }
            NetworkMessage.TYPE_TIME_STATE -> {
                message.scenarioClock?.let { ScenarioClockState.applyRemote(it, message.calendar) }
            }
            NetworkMessage.TYPE_PNJ_BRIEFING -> {
                val name = message.pnjName ?: return
                _pendingPnjBriefing.value = PnjBriefingUi(name = name, portraitId = message.pnjPortraitId)
            }
            NetworkMessage.TYPE_SOCIAL_ROLL_MODE -> {
                val id = message.socialRequestId ?: return
                val mode = message.socialMode?.let { runCatching { SocialRollMode.valueOf(it) }.getOrNull() } ?: return
                val roll = _playerSocialRoll.value ?: return
                if (roll.id == id) _playerSocialRoll.value = roll.copy(mode = mode)
            }
            NetworkMessage.TYPE_SOCIAL_ROLL_CANCEL -> {
                val id = message.socialRequestId ?: return
                val roll = _playerSocialRoll.value ?: return
                if (roll.id != id) return
                _playerSocialRoll.value = null
                _playerDiscussionNotice.value = "Votre action (${roll.skill}) a été annulée."
            }
            NetworkMessage.TYPE_DISCUSSION_LOCK -> {
                val id = message.socialRequestId
                _discussionLock.value = id?.let { DiscussionLock(it, message.socialCharacterName ?: "Un joueur") }
            }
            NetworkMessage.TYPE_DISCUSSION_CLOSED -> {
                _pendingPnjBriefing.value = null
                _playerSocialRoll.value = null
                _playerDiscussionNotice.value = null
                _discussionLock.value = null
            }
            NetworkMessage.TYPE_ITEM_DELIVERY -> {
                val itemName = message.itemName ?: return
                val characterId = _claimedCharacterId.value ?: return
                GameState.addItemToBackpack(characterId, itemName)
                val context = playerContext ?: return
                val monde = GameState.characters.value.find { it.id == characterId }?.worldId
                scope.launch {
                    val manquants = runCatching { VerificationContenu.manquantsObjet(context, itemName, monde) }.getOrDefault(emptyList())
                    if (manquants.isNotEmpty()) signalerContenuManquant(socket, itemName, manquants)
                }
            }
            NetworkMessage.TYPE_LOOT_OFFER -> {
                val offerId = message.lootOfferId ?: return
                val itemName = message.itemName ?: return
                if (_pendingLootOffers.value.none { it.offerId == offerId }) {
                    _pendingLootOffers.value = _pendingLootOffers.value + LootOffer(offerId, itemName)
                }
            }
            NetworkMessage.TYPE_LOOT_OFFER_CLOSED -> {
                _pendingLootOffers.value = _pendingLootOffers.value.filterNot { it.offerId == message.lootOfferId }
            }
            NetworkMessage.TYPE_GROUP_PROPOSAL -> {
                val proposalId = message.proposalId ?: return
                _pendingGroupProposal.value = PendingGroupProposalUi(
                    proposalId = proposalId,
                    title = message.proposalTitle.orEmpty(),
                    description = message.proposalDescription.orEmpty(),
                    rewardLabel = message.proposalReward.orEmpty(),
                )
            }
            NetworkMessage.TYPE_PROPOSAL_STATE -> {
                val proposalId = message.proposalId ?: return
                if (message.proposalClosed == true) {
                    _activeProposalState.value = _activeProposalState.value - proposalId
                    if (_pendingGroupProposal.value?.proposalId == proposalId) _pendingGroupProposal.value = null
                    if (_pendingProposalFinalCall.value?.proposalId == proposalId) _pendingProposalFinalCall.value = null
                    return
                }
                val decisions = message.proposalDecisions.orEmpty().mapValues { (_, value) ->
                    runCatching { ProposalDecision.valueOf(value) }.getOrDefault(ProposalDecision.PENDING)
                }
                val proposal = GroupProposal(
                    id = proposalId,
                    title = message.proposalTitle.orEmpty(),
                    description = message.proposalDescription.orEmpty(),
                    rewardLabel = message.proposalReward.orEmpty(),
                    pendingClientIds = decisions.keys,
                    decisions = decisions,
                    clientNames = message.proposalClientNames.orEmpty(),
                )
                _activeProposalState.value = if (proposal.isUnresolved) {
                    _activeProposalState.value + (proposalId to proposal)
                } else {
                    _activeProposalState.value - proposalId
                }
            }
            NetworkMessage.TYPE_PROPOSAL_FINAL_CALL -> {
                val proposalId = message.proposalId ?: return
                _pendingProposalFinalCall.value = ProposalFinalCallUi(proposalId, message.proposalTitle.orEmpty())
            }
            NetworkMessage.TYPE_GROUP_INFO -> {
                _networkGroup.value = message.groupName?.let { NetworkGroupInfo(it, message.characters.orEmpty(), message.groupe) }
            }
            NetworkMessage.TYPE_CAMPAIGN_STATE -> {
                val data = message.campagne
                _networkCampagne.value = data?.let { it.id to it.titre }
                if (data == null) com.jc2.jdrcompagnon.feature_carte.data.CarteSyncReseau.oublierBoutiquesReseau()
                val context = playerContext
                if (data != null && context != null) {
                    scope.launch { runCatching { com.jc2.jdrcompagnon.feature_carte.data.CarteSyncReseau.appliquer(context, data) } }
                }
            }
            NetworkMessage.TYPE_MAP_IMAGE -> {
                val image = message.carteImage ?: return
                val context = playerContext ?: return
                scope.launch { runCatching { com.jc2.jdrcompagnon.feature_carte.data.CarteSyncReseau.appliquerImage(context, image) } }
            }
            NetworkMessage.TYPE_CITY_IMAGE -> {
                val image = message.villeImage ?: return
                val context = playerContext ?: return
                scope.launch { com.jc2.jdrcompagnon.feature_carte.data.CarteSyncReseau.appliquerImageVille(context, image) }
            }
            NetworkMessage.TYPE_CITY_POSITIONS -> _positionsVille.value = message.positionsVille.orEmpty()
            NetworkMessage.TYPE_SHOP_INVITE -> recevoirInvitationBoutique(message)
            NetworkMessage.TYPE_SHOP_TRADE_RESULT -> _resultatCommerce.value =
                ResultatCommerce(message.accepted == true, message.commerceMessage ?: if (message.accepted == true) "Transaction effectuée." else "Transaction refusée.")
            NetworkMessage.TYPE_VOYAGE_STATE -> recevoirVoyage(message.voyage)
            NetworkMessage.TYPE_MAP_OPENED -> _carteOuverteJoueur.value = message.carteOuverte
            NetworkMessage.TYPE_MAP_TRACE -> com.jc2.jdrcompagnon.feature_carte.presentation.TraceCarteSession.recevoir(message.trace)
            NetworkMessage.TYPE_REST_OFFER -> {
                _pendingRestOffer.value = message.reposMessage ?: "Le groupe fait halte pour un repos long."
            }
            NetworkMessage.TYPE_QUESTS_STATE -> {
                _networkQuests.value = message.quests.orEmpty()
            }
            NetworkMessage.TYPE_EPREUVE_STATE -> {
                val epreuve = message.epreuve
                // L'issue d'une épreuve masquée est toujours montrée au joueur.
                if (epreuve?.issue != null) _epreuveMasquee.value = null
                _epreuveEnCours.value = epreuve
            }
            NetworkMessage.TYPE_COMBAT_STATE -> {
                _combatEnCours.value = message.combat
                // Comme chez le MJ : pendant le combat l'horloge ne défile plus en temps réel,
                // seuls les rounds (6 s chacun, diffusés par le MJ) la font avancer.
                com.jc2.jdrcompagnon.ui.ScenarioClockState.setPauseCombat(message.combat != null)
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
                // Déconnexion demandée pendant la tentative (connect est bloquant).
                if (manualDisconnect) {
                    runCatching { socket.close() }
                    return@launch
                }
                currentSocket = socket
                _playerState.value = PlayerConnectionState.CONNECTED
                // Le joueur incarnait un personnage : il le redemande (le MJ le lui rend d'office,
                // ceci couvre un MJ qui aurait relancé sa partie entre-temps).
                _claimedCharacterId.value?.let { requestClaimCharacter(it) }
                listenUntilDisconnected(context, socket, target, playerName)
            } catch (e: Exception) {
                if (manualDisconnect) return@launch
                _lastConnectionError.value = e.message ?: e.javaClass.simpleName
                scheduleReconnect(context, target, playerName, attempt + 1)
            }
        }
    }

    /** Déconnexion volontaire du joueur : coupe aussi les tentatives de reconnexion. */
    fun disconnect(context: Context) {
        _importsManquants.value = emptyList()
        _contenuManquant.value = null
        manualDisconnect = true
        reconnectJob?.cancel()
        readJob?.cancel()
        currentSocket?.let { runCatching { it.close() } }
        currentSocket = null
        _playerState.value = PlayerConnectionState.DISCONNECTED
        clearPlayerPendingInteractions()
        _networkGroup.value = null
        _networkQuests.value = null
        _networkCampagne.value = null
        com.jc2.jdrcompagnon.feature_carte.data.CarteSyncReseau.oublierBoutiquesReseau()
        oublierInvitationBoutique()
        _positionsVille.value = emptyList()
        _resultatCommerce.value = null
        _pendingRestOffer.value = null
        recevoirVoyage(null)
        _activeProposalState.value = emptyMap()
        _discussionLock.value = null
        ScenarioClockState.clearRemoteCalendar()
        if (_role.value == SessionRole.PLAYER) _role.value = SessionRole.NONE
        LanConnectionService.stopIfIdle(context.applicationContext)
    }

    private fun clearPlayerPendingInteractions() {
        _pendingPnjBriefing.value = null
        _playerSocialRoll.value = null
        _playerDiscussionNotice.value = null
        _discussionLock.value = null
        _epreuveEnCours.value = null
        _epreuveMasquee.value = null
        _combatEnCours.value = null
        com.jc2.jdrcompagnon.ui.ScenarioClockState.setPauseCombat(false)
        _carteOuverteJoueur.value = null
        com.jc2.jdrcompagnon.feature_carte.presentation.TraceCarteSession.recevoir(null)
        _pendingLootOffers.value = emptyList()
        _pendingGroupProposal.value = null
        _pendingProposalFinalCall.value = null
        _pendingCharacterVersionConflict.value = null
    }

    private fun stopEverythingInternal() {
        clientsWatcherJob?.cancel()
        etatsWatcherJob?.cancel()
        groupInfoWatcherJob?.cancel()
        questsWatcherJob?.cancel()
        campaignWatcherJob?.cancel()
        dernierEtatCampagne = null
        combatWatcherJob?.cancel()
        joueursWatcherJob?.cancel()
        lastCombatSentToClient.clear()
        gameServer?.stop()
        gameServer = null
        webServer?.stop()
        webServer = null
        _webServerUrl.value = null
        reconnectJob?.cancel()
        readJob?.cancel()
        currentSocket?.let { runCatching { it.close() } }
        currentSocket = null
    }
}