package org.disastermesh.app.mesh.connection

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Looper
import androidx.core.content.ContextCompat
import org.disastermesh.core.logging.MeshLogger

/** Owns one Wi-Fi Direct group connection and exposes its state to the Flutter bridge. */
class WifiDirectConnectionManager(
    context: Context,
    private val logger: MeshLogger,
    private val onStateChanged: (String, String?) -> Unit,
) {
    private val appContext = context.applicationContext
    private val manager = appContext.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
    private val channel = manager.initialize(appContext, Looper.getMainLooper(), null)

    var connectedPeerId: String? = null
        private set
    var groupOwnerAddress: String? = null
        private set
    var isGroupOwner: Boolean = false
        private set
    var isConnected: Boolean = false
        private set

    fun connect(peerId: String) {
        if (!hasPermission()) {
            onStateChanged("permission_denied", peerId)
            return
        }
        connectedPeerId = peerId
        onStateChanged("connecting", peerId)
        val config = WifiP2pConfig().apply { deviceAddress = peerId }
        manager.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                logger.info("wifi_direct_connect_requested", mapOf("peerId" to peerId))
            }

            override fun onFailure(reason: Int) {
                logger.warn("wifi_direct_connect_failed", mapOf("peerId" to peerId, "reason" to reason))
                onStateChanged("failed", peerId)
            }
        })
    }

    fun refreshConnectionInfo() {
        if (!hasPermission()) return
        manager.requestConnectionInfo(channel) { info -> updateConnectionInfo(info) }
    }

    fun disconnect() {
        if (!hasPermission()) return
        manager.removeGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() = clearConnection("disconnected")

            override fun onFailure(reason: Int) {
                logger.warn("wifi_direct_disconnect_failed", mapOf("reason" to reason))
                clearConnection("disconnected")
            }
        })
    }

    private fun updateConnectionInfo(info: WifiP2pInfo) {
        if (!info.groupFormed) {
            clearConnection("disconnected")
            return
        }
        isGroupOwner = info.isGroupOwner
        groupOwnerAddress = info.groupOwnerAddress?.hostAddress
        isConnected = true
        onStateChanged("connected", connectedPeerId)
        logger.info(
            "wifi_direct_connected",
            mapOf("groupOwner" to isGroupOwner, "groupOwnerAddress" to groupOwnerAddress),
        )
    }

    private fun clearConnection(state: String) {
        connectedPeerId = null
        groupOwnerAddress = null
        isGroupOwner = false
        isConnected = false
        onStateChanged(state, null)
    }

    private fun hasPermission(): Boolean {
        val locationGranted = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val nearbyGranted = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.NEARBY_WIFI_DEVICES,
            ) == PackageManager.PERMISSION_GRANTED
        return locationGranted && nearbyGranted
    }
}
