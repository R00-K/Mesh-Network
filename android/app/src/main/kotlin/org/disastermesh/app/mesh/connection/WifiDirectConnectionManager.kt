package org.disastermesh.app.mesh.connection

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Handler
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
    private val handler = Handler(Looper.getMainLooper())
    private var disconnectCheckGeneration = 0

    var connectedPeerId: String? = null
        private set
    var groupOwnerAddress: String? = null
        private set
    var isGroupOwner: Boolean = false
        private set
    var isConnected: Boolean = false
        private set
    @Volatile private var operationGeneration = 0

    private val confirmDisconnect = Runnable {
        val expectedGeneration = disconnectCheckGeneration
        val expectedOperation = operationGeneration
        if (!hasPermission()) return@Runnable
        manager.requestConnectionInfo(channel) { latestInfo ->
            if (expectedGeneration != disconnectCheckGeneration ||
                expectedOperation != operationGeneration
            ) return@requestConnectionInfo
            if (latestInfo.groupFormed) {
                updateConnectionInfo(latestInfo)
            } else {
                clearConnection("disconnected")
            }
        }
    }

    fun connect(peerId: String) {
        if (!hasPermission()) {
            onStateChanged("permission_denied", peerId)
            return
        }
        if (isConnected && connectedPeerId == peerId) {
            refreshConnectionInfo()
            return
        }
        val expectedOperation = ++operationGeneration
        handler.removeCallbacks(confirmDisconnect)
        disconnectCheckGeneration += 1
        onStateChanged("connecting", peerId)
        val switchingPeer = isConnected || connectedPeerId != null
        if (switchingPeer) {
            manager.removeGroup(channel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() = prepareNextConnection(peerId, expectedOperation)

                override fun onFailure(reason: Int) {
                    logger.warn("wifi_direct_switch_disconnect_failed", mapOf("reason" to reason))
                    prepareNextConnection(peerId, expectedOperation)
                }
            })
            return
        }
        requestPeerConnection(peerId, expectedOperation)
    }

    private fun prepareNextConnection(peerId: String, expectedOperation: Int) {
        if (expectedOperation != operationGeneration) return
        clearConnection(
            "disconnected",
            invalidateOperation = false,
            preservePeerId = peerId,
        )
        handler.postDelayed(
            { if (expectedOperation == operationGeneration) requestPeerConnection(peerId, expectedOperation) },
            GROUP_SWITCH_DELAY_MS,
        )
    }

    private fun requestPeerConnection(peerId: String, expectedOperation: Int) {
        if (expectedOperation != operationGeneration) return
        connectedPeerId = peerId
        onStateChanged("connecting", peerId)
        val config = WifiP2pConfig().apply { deviceAddress = peerId }
        manager.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                if (expectedOperation == operationGeneration) {
                    logger.info("wifi_direct_connect_requested", mapOf("peerId" to peerId))
                }
            }

            override fun onFailure(reason: Int) {
                if (expectedOperation != operationGeneration) return
                logger.warn("wifi_direct_connect_failed", mapOf("peerId" to peerId, "reason" to reason))
                isConnected = false
                isGroupOwner = false
                groupOwnerAddress = null
                onStateChanged("failed", peerId)
            }
        })
    }

    fun refreshConnectionInfo() {
        if (!hasPermission()) return
        val expectedOperation = operationGeneration
        manager.requestConnectionInfo(channel) { info ->
            if (expectedOperation == operationGeneration) updateConnectionInfo(info)
        }
    }

    fun disconnect() {
        if (!hasPermission()) return
        val expectedOperation = ++operationGeneration
        handler.removeCallbacks(confirmDisconnect)
        disconnectCheckGeneration += 1
        manager.removeGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                if (expectedOperation == operationGeneration) clearConnection("disconnected")
            }

            override fun onFailure(reason: Int) {
                if (expectedOperation != operationGeneration) return
                logger.warn("wifi_direct_disconnect_failed", mapOf("reason" to reason))
                clearConnection("disconnected")
            }
        })
    }

    private fun updateConnectionInfo(info: WifiP2pInfo) {
        if (!info.groupFormed) {
            // Android can briefly report no formed group while updating P2P info.
            // Confirm loss before closing a working TCP transport.
            if (isConnected) {
                handler.removeCallbacks(confirmDisconnect)
                disconnectCheckGeneration += 1
                handler.postDelayed(confirmDisconnect, DISCONNECT_CONFIRMATION_MS)
            } else if (connectedPeerId == null) {
                clearConnection("disconnected")
            }
            return
        }
        handler.removeCallbacks(confirmDisconnect)
        disconnectCheckGeneration += 1
        isGroupOwner = info.isGroupOwner
        groupOwnerAddress = info.groupOwnerAddress?.hostAddress
        isConnected = true
        onStateChanged("connected", connectedPeerId)
        logger.info(
            "wifi_direct_connected",
            mapOf("groupOwner" to isGroupOwner, "groupOwnerAddress" to groupOwnerAddress),
        )
    }

    private fun clearConnection(
        state: String,
        invalidateOperation: Boolean = true,
        preservePeerId: String? = null,
    ) {
        handler.removeCallbacks(confirmDisconnect)
        disconnectCheckGeneration += 1
        if (invalidateOperation) operationGeneration += 1
        connectedPeerId = preservePeerId
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

    private companion object {
        const val DISCONNECT_CONFIRMATION_MS = 2_000L
        const val GROUP_SWITCH_DELAY_MS = 500L
    }
}
