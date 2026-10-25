package org.disastermesh.app.mesh.discovery

import android.Manifest
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.disastermesh.core.discovery.PeerSnapshotCache
import org.disastermesh.core.logging.MeshLogger

/** Owns WifiP2pManager discovery and exposes the live peer cache as a Flow. */
class MeshDiscoveryManager(
    private val context: Context,
    private val logger: MeshLogger,
    private val onDiscoveryStateChanged: (Boolean) -> Unit = {},
    private val onDiscoveryError: (String) -> Unit = {},
    private val onPeersChanged: () -> Unit = {},
    private val onConnectionChanged: () -> Unit = {},
    private val onLocalDeviceIdChanged: (String) -> Unit = {},
    private val retainedPeerId: () -> String? = { null },
) {
    private val manager = context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
    private val channel = manager.initialize(context, Looper.getMainLooper(), null)
    private val peerRefreshHandler = Handler(Looper.getMainLooper())
    private val _peers = MutableStateFlow<List<WifiP2pDevice>>(emptyList())
    private val peerCache = PeerSnapshotCache<WifiP2pDevice>(PEER_CACHE_TTL_MS)
    private val receiver = MeshBroadcastReceiver(this, logger, onConnectionChanged)
    private var registered = false
    private var discoveryGeneration = 0
    private var discoveryStartPending = false
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
        discoveryGeneration += 1
        discoveryStartPending = false
        peerRefreshHandler.removeCallbacksAndMessages(null)
        if (registered) {
            context.unregisterReceiver(receiver)
            registered = false
        }
        manager.stopPeerDiscovery(channel, null)
        isDiscovering = false
        onDiscoveryStateChanged(false)
        peerCache.clear()
        _peers.value = emptyList()
    }

    /** Requests a new Wi-Fi Direct peer scan. */
    fun discover(force: Boolean = false) {
        peerRefreshHandler.removeCallbacks(discoveryRetry)
        if (discoveryStartPending) {
            requestPeers()
            return
        }
        if (isDiscovering && !force) {
            requestPeers()
            return
        }
        if (!hasDiscoveryPermission()) {
            logger.warn("discovery_permission_missing")
            onDiscoveryError("Allow Nearby Wi-Fi (Android 13+) or Location (older Android) permission to discover peers.")
            return
        }
        if (!isLocationEnabled()) {
            onDiscoveryError("Turn on Location services in Android Settings to use Wi-Fi Direct discovery.")
            return
        }
        onDiscoveryError("")
        val expectedGeneration = ++discoveryGeneration
        discoveryStartPending = true
        if (force && isDiscovering) {
            try {
                manager.stopPeerDiscovery(channel, object : WifiP2pManager.ActionListener {
                    override fun onSuccess() = startPlatformDiscovery(expectedGeneration)

                    override fun onFailure(reason: Int) {
                        if (expectedGeneration != discoveryGeneration || !registered) return
                        logger.debug("discovery_scan_stop_completed_with_error", mapOf("reason" to reason))
                        startPlatformDiscovery(expectedGeneration)
                    }
                })
            } catch (error: SecurityException) {
                discoveryStartPending = false
                logger.warn("discovery_scan_restart_denied", mapOf("error" to (error.message ?: "permission denied")))
                onDiscoveryError("Android denied the Wi-Fi Direct scan. Check Nearby Wi-Fi permission and Location services.")
            }
        } else {
            startPlatformDiscovery(expectedGeneration)
        }
    }

    private fun startPlatformDiscovery(expectedGeneration: Int) {
        if (expectedGeneration != discoveryGeneration || !registered) return
        try {
            manager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                if (expectedGeneration != discoveryGeneration || !registered) return
                discoveryStartPending = false
                isDiscovering = true
                peerRefreshHandler.removeCallbacks(discoveryRetry)
                onDiscoveryStateChanged(true)
                logger.info("discovery_started")
            }

            override fun onFailure(reason: Int) {
                if (expectedGeneration != discoveryGeneration || !registered) return
                discoveryStartPending = false
                isDiscovering = false
                onDiscoveryStateChanged(false)
                logger.warn("discovery_failed", mapOf("reason" to reason))
                onDiscoveryError("Android could not start Wi-Fi Direct discovery (reason $reason). Check that Wi-Fi is on and try again.")
                if (registered) {
                    peerRefreshHandler.removeCallbacks(discoveryRetry)
                    peerRefreshHandler.postDelayed(discoveryRetry, DISCOVERY_RETRY_DELAY_MS)
                }
            }
            })
        } catch (error: SecurityException) {
            if (expectedGeneration != discoveryGeneration) return
            discoveryStartPending = false
            isDiscovering = false
            onDiscoveryStateChanged(false)
            logger.warn("discovery_security_exception", mapOf("error" to (error.message ?: "permission denied")))
            onDiscoveryError("Android denied Wi-Fi Direct discovery. Check Nearby Wi-Fi permission and Location services.")
        }
    }

    internal fun requestPeers() {
        if (!hasDiscoveryPermission()) return
        try {
            manager.requestPeers(channel) { devices -> updatePeers(devices.deviceList) }
        } catch (error: SecurityException) {
            logger.warn("peer_list_permission_failed", mapOf("error" to (error.message ?: "permission denied")))
            onDiscoveryError("Android denied access to the Wi-Fi Direct peer list. Check Nearby Wi-Fi permission and Location services.")
        }
    }

    internal fun requestLocalDevice() {
        if (!hasDiscoveryPermission() || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        manager.requestDeviceInfo(channel) { device ->
            val address = device?.deviceAddress
            if (!address.isNullOrBlank()) onLocalDeviceIdChanged(address)
        }
    }

    internal fun updatePeers(devices: Collection<WifiP2pDevice>) {
        val now = System.currentTimeMillis()
        val retained = setOfNotNull(retainedPeerId())
        val snapshots = peerCache.update(
            peers = devices.map { it.deviceAddress to it },
            nowMs = now,
            retainedIds = retained,
        )
        _peers.value = snapshots
            .map { it.value }
            .sortedBy { it.deviceName.lowercase() }
        onPeersChanged()
        logger.debug("peers_updated", mapOf("count" to _peers.value.size, "freshCount" to devices.size))
    }

    fun lastSeenAt(deviceAddress: String): Long? = peerCache.lastSeenAt(deviceAddress)

    fun isRecentlySeen(deviceAddress: String): Boolean =
        peerCache.isFresh(deviceAddress, System.currentTimeMillis(), PEER_FRESHNESS_MS)

    private fun hasDiscoveryPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.NEARBY_WIFI_DEVICES,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            locationManager.isLocationEnabled
        } else {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }
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
        const val PEER_CACHE_TTL_MS = 5 * 60_000L
        const val PEER_FRESHNESS_MS = 15_000L
    }
}
