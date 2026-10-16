package org.disastermesh.app.mesh.channels

import android.content.Context
import android.content.Intent
import android.os.Build
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import org.disastermesh.app.mesh.discovery.MeshDiscoveryManager
import org.disastermesh.app.mesh.service.MeshForegroundService
import org.disastermesh.core.logging.MeshLogger

/** Initial Flutter bridge for the native mesh service boundary. */
class MeshChannelHandler(
    private val context: Context,
    private val logger: MeshLogger,
    private val requestPermissions: () -> Unit,
) : MethodChannel.MethodCallHandler, EventChannel.StreamHandler {
    private val discovery = MeshDiscoveryManager(
        context = context.applicationContext,
        logger = logger,
        onDiscoveryStateChanged = { active ->
            emit("MeshStatusChanged", mapOf("running" to running, "discovering" to active))
        },
        onPeersChanged = { emitPeers() },
    )
    private var eventSink: EventChannel.EventSink? = null
    private var running = false
    private var pendingStart = false

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "startMesh" -> {
                pendingStart = true
                emit("MeshStatusChanged", mapOf("state" to "requesting_permissions"))
                requestPermissions()
                result.success(null)
            }
            "stopMesh" -> {
                running = false
                pendingStart = false
                discovery.stop()
                context.stopService(Intent(context, MeshForegroundService::class.java))
                logger.info("mesh_stopped")
                emit("MeshStatusChanged", mapOf("running" to false, "discovering" to false, "state" to "stopped"))
                result.success(null)
            }
            "discoverPeers" -> {
                logger.info("peer_discovery_requested")
                discovery.discover()
                result.success(null)
            }
            "connectPeer", "disconnectPeer" -> {
                val peerId = call.argument<String>("peerId")
                if (peerId.isNullOrBlank()) {
                    result.error("INVALID_ARGUMENT", "peerId is required", null)
                } else {
                    logger.info(call.method, mapOf("peerId" to peerId))
                    result.success(null)
                }
            }
            "sendMessage" -> result.success(null)
            "getPeers" -> {
                val peers = discovery.peers.value.map { peer ->
                    mapOf<String, Any?>(
                        "id" to peer.deviceAddress,
                        "deviceName" to peer.deviceName,
                        "batteryLevel" to 0.0,
                        "signalStrength" to 0.0,
                        "bandwidthKbps" to 0.0,
                        "latencyMs" to 0.0,
                        "packetLoss" to 0.0,
                        "load" to 0.0,
                        "lastSeen" to java.time.Instant.now().toString(),
                        "status" to "nearby",
                    )
                }
                emit("PeerUpdate", mapOf("peers" to peers))
                result.success(peers)
            }
            "getMetrics" -> result.success(emptyMap<String, Any?>())
            "getRoute" -> result.success(null)
            else -> result.notImplemented()
        }
    }

    override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
        eventSink = events
        emit("MeshStatusChanged", mapOf("running" to running, "discovering" to discovery.isDiscovering))
        emitPeers()
    }

    override fun onCancel(arguments: Any?) {
        eventSink = null
    }

    private fun emit(type: String, payload: Map<String, Any?>) {
        eventSink?.success(mapOf("type" to type, "payload" to payload))
    }

    fun onPermissionsResult(granted: Boolean) {
        if (!pendingStart) return
        pendingStart = false
        if (!granted) {
            emit("MeshStatusChanged", mapOf("running" to false, "discovering" to false, "state" to "permission_denied"))
            return
        }
        running = true
        discovery.start()
        val serviceIntent = Intent(context, MeshForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
        logger.info("mesh_started")
        emit("MeshStatusChanged", mapOf("running" to true, "discovering" to discovery.isDiscovering, "state" to "discovering"))
    }

    private fun emitPeers() {
        val peers = discovery.peers.value.map { peer ->
            mapOf<String, Any?>(
                "id" to peer.deviceAddress,
                "deviceName" to peer.deviceName,
                "batteryLevel" to 0.0,
                "signalStrength" to 0.0,
                "bandwidthKbps" to 0.0,
                "latencyMs" to 0.0,
                "packetLoss" to 0.0,
                "load" to 0.0,
                "lastSeen" to java.time.Instant.now().toString(),
                "status" to "nearby",
            )
        }
        emit("PeerUpdate", mapOf("peers" to peers))
    }
}
