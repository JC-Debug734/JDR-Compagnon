package com.jc2.jdrcompagnon.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.PrintWriter
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.ArrayDeque

private const val SERVICE_TYPE = "_jdrcompagnon._tcp."
private const val TAG = "GameClientDiscovery"

// Delai max pour établir la connexion TCP : sans ça, Socket(host, port) peut
// rester bloqué très longtemps si le paquet part par la mauvaise interface
// réseau (ex. données mobiles actives en plus du Wi-Fi), donnant l'impression
// que la connexion "ne marche jamais" plutôt que d'échouer proprement.
private const val CONNECT_TIMEOUT_MS = 8_000

// Délai avant de retenter une résolution après FAILURE_ALREADY_ACTIVE :
// laisse le temps à l'implémentation NSD du téléphone de se libérer.
private const val RESOLVE_RETRY_DELAY_MS = 300L
private const val MAX_RESOLVE_ATTEMPTS = 4

data class DiscoveredServer(
    val serviceName: String,
    // On garde l'InetAddress résolu par NSD, pas une String reconstruite :
    // une adresse IPv6 locale au lien (fe80::...) inclut un identifiant de
    // scope (interface réseau) qui est perdu si on repasse par host.hostAddress
    // puis InetSocketAddress(String, port) — la connexion échoue alors
    // indéfiniment même si le serveur a bien été trouvé.
    val address: InetAddress,
    val port: Int
) {
    val host: String get() = address.hostAddress ?: address.toString()
}

/**
 * Recherche automatique des parties hébergées sur le même réseau Wi-Fi
 * via NSD (mDNS/Bonjour). Aucune saisie d'IP requise côté joueur.
 *
 * L'implémentation système de NsdManager varie sensiblement d'un
 * constructeur à l'autre : certains (Samsung notamment) rejettent toute
 * résolution lancée pendant qu'une autre est déjà en cours
 * (FAILURE_ALREADY_ACTIVE), ce qui peut arriver ici car onServiceFound peut
 * être invoqué plusieurs fois pour le même service. On sérialise donc les
 * résolutions dans une file et on retente automatiquement en cas d'échec,
 * pour que le comportement soit fiable sur le plus grand nombre d'appareils.
 */
class GameClientDiscovery(private val context: Context) {

    private val nsdManager by lazy {
        context.getSystemService(Context.NSD_SERVICE) as NsdManager
    }
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _servers = MutableStateFlow<List<DiscoveredServer>>(emptyList())
    val servers: StateFlow<List<DiscoveredServer>> = _servers.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    // Sans ce lock, de nombreux appareils Android filtrent les paquets
    // multicast (mDNS) dès que l'écran/Wi-Fi est en veille, ce qui rend la
    // découverte NSD très lente ou muette selon les téléphones.
    private var multicastLock: WifiManager.MulticastLock? = null

    // File des résolutions en attente + verrou d'exécution séquentielle.
    private data class ResolveRequest(val info: NsdServiceInfo, val attempt: Int)
    private val resolveQueue = ArrayDeque<ResolveRequest>()
    private var resolveInFlight = false
    private val resolveLock = Any()

    // Écoute des balises UDP broadcast émises par GameServer : filet de
    // sécurité qui ne dépend pas de la pile NSD système, indispensable sur
    // les appareils (Samsung notamment) où resolveService() peut rester
    // bloqué indéfiniment sans jamais rappeler de callback.
    private var beaconJob: Job? = null
    private var beaconSocket: DatagramSocket? = null

    private fun acquireMulticastLock() {
        if (multicastLock?.isHeld == true) return
        val wifiManager = context.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return
        multicastLock = runCatching {
            wifiManager.createMulticastLock("jdrcompagnon-discovery").apply {
                setReferenceCounted(true)
                acquire()
            }
        }.getOrNull()
    }

    private fun releaseMulticastLock() {
        multicastLock?.let { if (it.isHeld) runCatching { it.release() } }
        multicastLock = null
    }

    fun startDiscovery() {
        if (_isScanning.value) return
        _servers.value = emptyList()
        synchronized(resolveLock) {
            resolveQueue.clear()
            resolveInFlight = false
        }
        acquireMulticastLock()
        startBeaconListener()

        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                _isScanning.value = true
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                if (service.serviceType.contains("jdrcompagnon")) {
                    enqueueResolve(ResolveRequest(service, attempt = 1))
                }
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                _servers.update { list -> list.filterNot { it.serviceName == service.serviceName } }
            }

            override fun onDiscoveryStopped(serviceType: String) {
                _isScanning.value = false
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                _isScanning.value = false
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                runCatching { nsdManager.stopServiceDiscovery(this) }
            }
        }
        discoveryListener = listener
        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
    }

    fun stopDiscovery() {
        discoveryListener?.let { runCatching { nsdManager.stopServiceDiscovery(it) } }
        discoveryListener = null
        _isScanning.value = false
        synchronized(resolveLock) {
            resolveQueue.clear()
            resolveInFlight = false
        }
        releaseMulticastLock()
        stopBeaconListener()
    }

    /** Écoute en continu les balises UDP émises par GameServer et alimente
     * la même liste que la découverte NSD (dédoublonnée par serviceName). */
    private fun startBeaconListener() {
        beaconJob = scope.launch {
            val socket = try {
                DatagramSocket(null).apply {
                    reuseAddress = true
                    bind(InetSocketAddress(GameDiscoveryBeacon.PORT))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Écoute beacon UDP indisponible: ${e.message}")
                return@launch
            }
            beaconSocket = socket
            val buffer = ByteArray(512)
            try {
                while (isActive) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    try {
                        socket.receive(packet)
                    } catch (e: Exception) {
                        if (!isActive) break else continue
                    }
                    val text = String(packet.data, 0, packet.length)
                    val parts = text.split("|")
                    val address = packet.address
                    if (parts.size == 3 && parts[0] == GameDiscoveryBeacon.PREFIX && address != null) {
                        val port = parts[2].toIntOrNull() ?: continue
                        val server = DiscoveredServer(serviceName = parts[1], address = address, port = port)
                        // On remplace l'entrée existante plutôt que de l'ignorer :
                        // si le MJ a relancé le serveur (nouveau port à chaque
                        // démarrage), l'ancienne IP/port ne doit pas rester
                        // affichée — sinon le joueur tente de se connecter à un
                        // port qui n'écoute plus (ECONNREFUSED).
                        _servers.update { list ->
                            list.filterNot { it.serviceName == server.serviceName } + server
                        }
                    }
                }
            } finally {
                runCatching { socket.close() }
            }
        }
    }

    private fun stopBeaconListener() {
        beaconJob?.cancel()
        beaconJob = null
        beaconSocket?.let { runCatching { it.close() } }
        beaconSocket = null
    }

    // ── Résolution NSD sérialisée ──

    private fun enqueueResolve(request: ResolveRequest) {
        val shouldStart: Boolean
        synchronized(resolveLock) {
            resolveQueue.add(request)
            shouldStart = !resolveInFlight
            if (shouldStart) resolveInFlight = true
        }
        if (shouldStart) processNextResolve()
    }

    private fun processNextResolve() {
        val next: ResolveRequest?
        synchronized(resolveLock) {
            next = resolveQueue.poll()
            if (next == null) resolveInFlight = false
        }
        val request = next ?: return

        nsdManager.resolveService(request.info, object : NsdManager.ResolveListener {
            override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "Résolution échouée ($errorCode) pour ${info.serviceName}, tentative ${request.attempt}")
                // FAILURE_ALREADY_ACTIVE (3) survient quand une autre résolution
                // est déjà en cours côté OS malgré notre sérialisation (certains
                // OEM ont un délai de libération) : on retente après un court délai.
                if (errorCode == NsdManager.FAILURE_ALREADY_ACTIVE && request.attempt < MAX_RESOLVE_ATTEMPTS) {
                    mainHandler.postDelayed(
                        { enqueueResolve(request.copy(attempt = request.attempt + 1)) },
                        RESOLVE_RETRY_DELAY_MS
                    )
                }
                advanceQueue()
            }

            override fun onServiceResolved(info: NsdServiceInfo) {
                val address = info.host
                if (address != null) {
                    val server = DiscoveredServer(
                        serviceName = info.serviceName,
                        address = address,
                        port = info.port
                    )
                    // Même raisonnement que côté beacon : remplacer plutôt
                    // qu'ignorer, pour ne jamais rester bloqué sur un port
                    // périmé après un redémarrage du serveur.
                    _servers.update { list ->
                        list.filterNot { it.serviceName == server.serviceName } + server
                    }
                }
                advanceQueue()
            }
        })
    }

    /** Petit délai avant d'enchaîner la résolution suivante : certaines piles NSD
     * (surtout OEM) ont besoin d'un court répit entre deux résolutions. */
    private fun advanceQueue() {
        mainHandler.postDelayed({ processNextResolve() }, 50L)
    }

    /**
     * Connexion au serveur choisi ; à appeler depuis un thread/coroutine IO.
     * On tente d'abord un socket explicitement lié au réseau Wi-Fi actif
     * (nécessaire quand l'appareil a aussi une connexion mobile active, sinon
     * Android peut router la connexion par les données mobiles et échouer à
     * joindre l'IP locale du MJ silencieusement). Si cette tentative échoue
     * — le comportement du binding réseau varie aussi selon les
     * constructeurs — on retente sans forcer le binding plutôt que
     * d'abandonner, pour rester robuste sur le plus grand nombre d'appareils.
     */
    fun connect(server: DiscoveredServer, playerName: String): Socket {
        return try {
            connectInternal(server, playerName, bindToWifi = true)
        } catch (e: Exception) {
            Log.w(TAG, "Connexion avec binding Wi-Fi échouée (${e.message}), nouvelle tentative sans binding")
            connectInternal(server, playerName, bindToWifi = false)
        }
    }

    private fun connectInternal(server: DiscoveredServer, playerName: String, bindToWifi: Boolean): Socket {
        val socket = Socket()
        if (bindToWifi) bindToWifiNetwork(socket)
        socket.connect(InetSocketAddress(server.address, server.port), CONNECT_TIMEOUT_MS)
        PrintWriter(socket.getOutputStream(), true).println(playerName)
        return socket
    }

    private fun bindToWifiNetwork(socket: Socket) {
        val connectivityManager = context.applicationContext
            .getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        val wifiNetwork = connectivityManager.allNetworks.firstOrNull { network ->
            connectivityManager.getNetworkCapabilities(network)
                ?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        } ?: return
        wifiNetwork.bindSocket(socket)
    }
}