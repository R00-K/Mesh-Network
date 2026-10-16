package org.disastermesh.app.mesh.discovery

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.p2p.WifiP2pManager
import org.disastermesh.core.logging.MeshLogger

/** Receives Wi-Fi Direct lifecycle broadcasts for the discovery manager. */
class MeshBroadcastReceiver(
    private val discovery: MeshDiscoveryManager,
    private val logger: MeshLogger,
    private val onConnectionChanged: () -> Unit,
) : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> logger.info("wifi_direct_state_changed")
            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> discovery.requestPeers()
            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                logger.info("wifi_direct_connection_changed")
                onConnectionChanged()
            }
            WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                logger.debug("local_device_changed")
                discovery.requestLocalDevice()
            }
        }
    }
}
