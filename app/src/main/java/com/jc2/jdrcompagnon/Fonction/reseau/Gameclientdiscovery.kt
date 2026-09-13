package com.jc2.jdrcompagnon.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.PrintWriter
import java.net.Socket

private const val SERVICE_TYPE = "_jdrcompagnon._tcp."
private const val TAG = "GameClientDiscovery"

data class DiscoveredServer(
    val serviceName: String,
    val host: String,
    val port: Int
)

/**
 * Recherche automatique des parties hébergées sur le même réseau Wi-Fi
 * via NSD (mDNS/Bonjour). Aucune saisie d'IP requise côté joueur.
 */
class GameClientDiscovery(private val context: Context) {

    private val nsdManager by lazy {
        context.getSystemService(Context.NSD_SERVICE) as NsdManager
    }
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _servers = MutableStateFlow<List<DiscoveredServer>>(emptyList())
    val servers: StateFlow<List<DiscoveredServer>> = _servers.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    fun startDiscovery() {
        if (_isScanning.value) return
        _servers.value = emptyList()

        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                _isScanning.value = true
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                if (service.serviceType.contains("jdrcompagnon")) {
                    nsdManager.resolveService(service, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) {
                            Log.e(TAG, "Résolution échouée: $errorCode")
                        }
                        override fun onServiceResolved(info: NsdServiceInfo) {
                            val address = info.host?.hostAddress ?: return
                            val server = DiscoveredServer(
                                serviceName = info.serviceName,
                                host = address,
                                port = info.port
                            )
                            _servers.update { list ->
                                if (list.any { it.serviceName == server.serviceName }) list
                                else list + server
                            }
                        }
                    })
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
    }

    /** Connexion au serveur choisi ; à appeler depuis un thread/coroutine IO. */
    fun connect(server: DiscoveredServer, playerName: String): Socket {
        val socket = Socket(server.host, server.port)
        PrintWriter(socket.getOutputStream(), true).println(playerName)
        return socket
    }
}