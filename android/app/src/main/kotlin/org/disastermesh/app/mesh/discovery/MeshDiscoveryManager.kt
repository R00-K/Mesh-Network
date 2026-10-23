package org.disastermesh.app.mesh.discovery

import android.Manifest
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.disastermesh.core.logging.MeshLogger

/** Owns WifiP2pManager discovery and exposes the live peer cache as a Flow. */
class MeshDiscoveryManager(
    private val context: Context,
    private val logger: MeshLogger,
    private val onDiscoveryStateChanged: (Boolean) -> Unit = {},
    private val onPeersChanged: () -> Unit = {},
    private val onConnectionChanged: () -> Unit = {},
    private val onLocalDeviceIdChanged: (String) -> Unit = {},
) {
    private val manager = context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
    private val channel = manager.initialize(context, Looper.getMainLooper(), null)
    private val peerRefreshHandler = Handler(Looper.getMainLooper())
    private val _peers = MutableStateFlow<List<WifiP2pDevice>>(emptyList())
    private val receiver = MeshBroadcastReceiver(this, logger, onConnectionChanged)
    private var registered = false
    private val peerRefresh = object : Runnable {
        override fun run() {
            if (!registered) return
            requestPeers()
            peerRefreshHandler.postDelayed(this, PEER_REFRESH_INTERVAL_MS)
        }
    }
    private val discoveryRetry = object : Runnable {
        override fun run() {
            if (registered && !isDiscovering) discover()
        }
    }
    var isDiscovering: Boolean = false
        private set

    /** Currently discovered Wi-Fi Direct devices. */
    val peers: StateFlow<List<WifiP2pDevice>> = _peers

    /** Starts the platform receiver and a discovery scan. */
    fun start() {
        if (!registered) {
            context.registerReceiver(receiver, filter(), receiverFlags())
            registered = true
        }
        requestLocalDevice()
        discover()
        peerRefreshHandler.removeCallbacks(peerRefresh)
        peerRefreshHandler.post(peerRefresh)
    }

    /** Stops discovery and unregisters platform callbacks. */
    fun stop() {
        peerRefreshHandler.removeCallbacksAndMessages(null)
        if (registered) {
            context.unregisterReceiver(receiver)
            registered = false
        }
        manager.stopPeerDiscovery(channel, null)
        isDiscovering = false
        onDiscoveryStateChanged(false)
        _peers.value = emptyList()
    }

    /** Requests a new Wi-Fi Direct peer scan. */
    fun discover() {
        peerRefreshHandler.removeCallbacks(discoveryRetry)
        if (!hasDiscoveryPermission()) {
            logger.warn("discovery_permission_missing")
            return
        }
        manager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                isDiscovering = true
                peerRefreshHandler.removeCallbacks(discoveryRetry)
                onDiscoveryStateChanged(true)
                logger.info("discovery_started")
            }

            override fun onFailure(reason: Int) {
                isDiscovering = false
                onDiscoveryStateChanged(false)
                logger.warn("discovery_failed", mapOf("reason" to reason))
                if (registered) {
                    peerRefreshHandler.removeCallbacks(discoveryRetry)
                    peerRefreshHandler.postDelayed(discoveryRetry, DISCOVERY_RETRY_DELAY_MS)
                }
            }
        })
    }

    internal fun requestPeers() {
        if (!hasDiscoveryPermission()) return
        manager.requestPeers(channel) { devices -> updatePeers(devices.deviceList) }
    }

    internal fun requestLocalDevice() {
        if (!hasDiscoveryPermission() || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        manager.requestDeviceInfo(channel) { device ->
            val address = device?.deviceAddress
            if (!address.isNullOrBlank()) onLocalDeviceIdChanged(address)
        }
    }

    internal fun updatePeers(devices: Collection<WifiP2pDevice>) {
        _peers.value = devices.sortedBy { it.deviceName.lowercase() }
        onPeersChanged()
        logger.debug("peers_updated", mapOf("count" to devices.size))
    }

    private fun hasDiscoveryPermission(): Boolean {
        val locationGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val nearbyGranted = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.NEARBY_WIFI_DEVICES,
            ) == PackageManager.PERMISSION_GRANTED
        return locationGranted && nearbyGranted
    }

    private fun filter() = IntentFilter().apply {
        addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
    }

    private fun receiverFlags(): Int = if (Build.VERSION.SDK_INT >= 33) {
        Context.RECEIVER_NOT_EXPORTED
    } else {
        0
    }

    private companion object {
        const val PEER_REFRESH_INTERVAL_MS = 5_000L
        const val DISCOVERY_RETRY_DELAY_MS = 5_000L
    }
}
