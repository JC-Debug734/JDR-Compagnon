package com.jc2.jdrcompagnon.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.UUID

private const val SERVICE_TYPE = "_jdrcompagnon._tcp."
private const val TAG = "GameServer"

/**
 * Balise UDP broadcast, en complément de NSD/mDNS pour la découverte.
 * Certains appareils (notamment Samsung) ont une implémentation NsdManager
 * peu fiable : les résolutions peuvent rester bloquées indéfiniment sans
 * jamais rappeler ni onServiceResolved ni onResolveFailed. Le broadcast UDP
 * ne dépend d'aucune pile mDNS système et fonctionne de façon identique sur
 * tous les appareils Android, ce qui en fait un filet de sécurité fiable.
 */
internal object GameDiscoveryBeacon {
    const val PORT = 47474
    const val PREFIX = "JDRCOMPAGNON_BEACON"
    const val INTERVAL_MS = 1_200L
}

data class ConnectedClient(
    val id: String,
    val displayName: String,
    val socket: Socket
)

/**
 * Héberge une partie sur le réseau local : ouvre un ServerSocket et publie
 * le service via NSD (mDNS/Bonjour) pour que les joueurs le trouvent
 * automatiquement dès qu'ils sont sur le même Wi-Fi.
 *
 * @param serviceName Nom affiché aux joueurs lors de la découverte (ex. le
 * titre de la campagne hébergée). Par défaut "MJ-{hostDisplayName}".
 */
class GameServer(
    private val context: Context,
    private val hostDisplayName: String,
    private val serviceName: String = "MJ-$hostDisplayName"
) {

    private var serverSocket: ServerSocket? = null
    private val nsdManager by lazy {
        context.getSystemService(Context.NSD_SERVICE) as NsdManager
    }
    private var registrationListener: NsdManager.RegistrationListener? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var acceptJob: Job? = null
    private var beaconJob: Job? = null

    private val _connectedClients = MutableStateFlow<List<ConnectedClient>>(emptyList())
    val connectedClients: StateFlow<List<ConnectedClient>> = _connectedClients.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    // Tenu pendant toute la durée de l'hébergement : sans lui, l'annonce
    // mDNS/NSD peut être filtrée côté MJ sur certains appareils, ce qui
    // explique qu'un joueur mette longtemps à trouver la partie.
    private var multicastLock: WifiManager.MulticastLock? = null

    /** Appelé pour chaque message reçu d'un client (ex. réponse de synchro). */
    var onMessageReceived: ((fromClientId: String, message: NetworkMessage) -> Unit)? = null

    /** Appelé dès qu'un client termine sa connexion (pseudo reçu). */
    var onClientConnected: ((ConnectedClient) -> Unit)? = null

    /** Appelé quand un client se déconnecte (volontairement ou non). */
    var onClientDisconnected: ((clientId: String) -> Unit)? = null

    fun start() {
        if (_isRunning.value) return
        val wifiManager = context.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as? WifiManager
        multicastLock = wifiManager?.createMulticastLock("jdrcompagnon-host")?.apply {
            setReferenceCounted(true)
            acquire()
        }
        val socket = ServerSocket(0) // port libre attribué par l'OS
        serverSocket = socket
        registerService(socket.localPort)
        startBeacon(socket.localPort)
        _isRunning.value = true

        acceptJob = scope.launch {
            try {
                while (true) {
                    val client = socket.accept()
                    handleNewClient(client)
                }
            } catch (e: Exception) {
                Log.d(TAG, "Boucle d'écoute arrêtée: ${e.message}")
            }
        }
    }

    private fun handleNewClient(socket: Socket) {
        scope.launch {
            var client: ConnectedClient? = null
            try {
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                // Le joueur envoie son pseudo en 1ère ligne à la connexion
                val playerName = reader.readLine() ?: "Joueur"
                val newClient = ConnectedClient(
                    id = UUID.randomUUID().toString(),
                    displayName = playerName,
                    socket = socket
                )
                client = newClient
                _connectedClients.update { it + newClient }
                onClientConnected?.invoke(newClient)

                // Boucle de lecture : messages du joueur (ex. réponse de synchro) + détection de déconnexion
                var line = reader.readLine()
                while (line != null) {
                    runCatching {
                        val message = networkJson.decodeFromString<NetworkMessage>(line)
                        onMessageReceived?.invoke(newClient.id, message)
                    }.onFailure {
                        Log.d(TAG, "Message illisible ignoré: ${it.message}")
                    }
                    line = reader.readLine()
                }
                _connectedClients.update { list -> list.filterNot { it.id == newClient.id } }
                onClientDisconnected?.invoke(newClient.id)
            } catch (e: Exception) {
                Log.d(TAG, "Client déconnecté: ${e.message}")
                _connectedClients.update { list -> list.filterNot { it.socket == socket } }
                client?.let { onClientDisconnected?.invoke(it.id) }
            }
        }
    }

    private fun registerService(port: Int) {
        val serviceInfo = NsdServiceInfo().apply {
            this.serviceName = this@GameServer.serviceName
            serviceType = SERVICE_TYPE
            setPort(port)
        }
        val listener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(info: NsdServiceInfo) {
                Log.d(TAG, "Service publié: ${info.serviceName}")
            }
            override fun onRegistrationFailed(info: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "Échec publication NSD: $errorCode")
            }
            override fun onServiceUnregistered(info: NsdServiceInfo) {}
            override fun onUnregistrationFailed(info: NsdServiceInfo, errorCode: Int) {}
        }
        registrationListener = listener
        nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, listener)
    }

    /** Diffuse périodiquement un paquet UDP broadcast annonçant la partie,
     * en complément de NSD (voir GameDiscoveryBeacon). */
    private fun startBeacon(tcpPort: Int) {
        val payload = "${GameDiscoveryBeacon.PREFIX}|$serviceName|$tcpPort".toByteArray()
        beaconJob = scope.launch {
            val socket = try {
                DatagramSocket().apply { broadcast = true }
            } catch (e: Exception) {
                Log.e(TAG, "Beacon UDP indisponible: ${e.message}")
                return@launch
            }
            val broadcastAddress = runCatching { InetAddress.getByName("255.255.255.255") }.getOrNull()
                ?: return@launch
            try {
                while (isActive) {
                    runCatching {
                        socket.send(DatagramPacket(payload, payload.size, broadcastAddress, GameDiscoveryBeacon.PORT))
                    }
                    delay(GameDiscoveryBeacon.INTERVAL_MS)
                }
            } finally {
                runCatching { socket.close() }
            }
        }
    }

    fun stop() {
        registrationListener?.let { runCatching { nsdManager.unregisterService(it) } }
        acceptJob?.cancel()
        beaconJob?.cancel()
        _connectedClients.value.forEach { runCatching { it.socket.close() } }
        _connectedClients.value = emptyList()
        runCatching { serverSocket?.close() }
        serverSocket = null
        _isRunning.value = false
        multicastLock?.let { if (it.isHeld) runCatching { it.release() } }
        multicastLock = null
    }

    fun sendToAll(message: String) {
        _connectedClients.value.forEach { client ->
            runCatching {
                PrintWriter(client.socket.getOutputStream(), true).println(message)
            }
        }
    }

    /** Envoie un message à un seul client, identifié par son id. */
    fun sendToClient(clientId: String, message: String) {
        _connectedClients.value.find { it.id == clientId }?.let { client ->
            runCatching {
                PrintWriter(client.socket.getOutputStream(), true).println(message)
            }
        }
    }
}