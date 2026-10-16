package org.disastermesh.app.mesh.channels

import android.content.Context
import android.content.Intent
import android.os.Build
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import org.disastermesh.app.mesh.discovery.MeshDiscoveryManager
import org.disastermesh.app.mesh.connection.WifiDirectConnectionManager
import org.disastermesh.app.mesh.service.MeshForegroundService
import org.disastermesh.app.mesh.transport.MeshTransport
import org.disastermesh.app.mesh.storage.MessageStore
import org.disastermesh.core.logging.MeshLogger
import org.json.JSONObject
import java.util.UUID
import android.provider.Settings

/** Initial Flutter bridge for the native mesh service boundary. */
class MeshChannelHandler(
    private val context: Context,
    private val logger: MeshLogger,
    private val requestPermissions: () -> Unit,
) : MethodChannel.MethodCallHandler, EventChannel.StreamHandler {
    private val messageStore = MessageStore(context.applicationContext)
    private val localNodeId = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ANDROID_ID,
    ) ?: "android-node"
    private val transport = MeshTransport(
        logger = logger,
        onStateChanged = { state ->
            emit("TransportChanged", mapOf("state" to state))
            if (state == "connected") retryPendingMessages()
        },
        onEnvelopeReceived = ::handleEnvelope,
    )
    private val connection: WifiDirectConnectionManager by lazy {
        WifiDirectConnectionManager(
        context = context.applicationContext,
        logger = logger,
        onStateChanged = { state, peerId ->
            if (state == "connected") {
                transport.start(connection.isGroupOwner, connection.groupOwnerAddress)
            } else if (state == "disconnected" || state == "failed") {
                transport.stop()
            }
            emit("ConnectionChanged", mapOf("state" to state, "peerId" to peerId))
            emitPeers()
        },
        )
    }
    private val discovery = MeshDiscoveryManager(
        context = context.applicationContext,
        logger = logger,
        onDiscoveryStateChanged = { active ->
            emit("MeshStatusChanged", mapOf("running" to running, "discovering" to active))
        },
        onPeersChanged = { emitPeers() },
        onConnectionChanged = { connection.refreshConnectionInfo() },
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
                transport.stop()
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
            "connectPeer" -> {
                val peerId = call.argument<String>("peerId")
                if (peerId.isNullOrBlank()) {
                    result.error("INVALID_ARGUMENT", "peerId is required", null)
                } else {
                    connection.connect(peerId)
                    logger.info("connect_peer", mapOf("peerId" to peerId))
                    result.success(null)
                }
            }
            "disconnectPeer" -> {
                connection.disconnect()
                logger.info("disconnect_peer")
                result.success(null)
            }
            "getConnectionInfo" -> result.success(
                mapOf(
                    "peerId" to connection.connectedPeerId,
                    "groupOwnerAddress" to connection.groupOwnerAddress,
                    "isGroupOwner" to connection.isGroupOwner,
                ),
            )
            "sendMessage" -> {
                val destination = call.argument<String>("destination")
                val payload = call.argument<String>("payload")
                if (destination.isNullOrBlank() || payload == null) {
                    result.error("INVALID_ARGUMENT", "destination and payload are required", null)
                } else {
                    val id = UUID.randomUUID().toString()
                    val envelope = JSONObject()
                        .put("type", "message")
                        .put("id", id)
                        .put("source", localNodeId)
                        .put("destination", destination)
                        .put("payload", payload)
                        .put("createdAt", System.currentTimeMillis())
                        .put("hopLimit", 1)
                    messageStore.enqueue(envelope)
                    val sent = sendStoredMessage(envelope)
                    emit(
                        "MessageStatusChanged",
                        mapOf("id" to id, "status" to if (sent) "sent" else "queued"),
                    )
                    result.success(id)
                }
            }
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
                        "status" to peerStatus(peer.deviceAddress),
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
                "status" to peerStatus(peer.deviceAddress),
            )
        }
        emit("PeerUpdate", mapOf("peers" to peers))
    }

    private fun peerStatus(peerId: String): String =
        if (connection.isConnected && connection.connectedPeerId == peerId) "connected" else "nearby"

    private fun handleEnvelope(envelope: JSONObject) {
        when (envelope.optString("type")) {
            "ack" -> {
                val id = envelope.optString("id")
                messageStore.markDelivered(id)
                emit("MessageStatusChanged", mapOf("id" to id, "status" to "delivered"))
            }
            "message" -> handleIncomingMessage(envelope)
        }
    }

    private fun handleIncomingMessage(envelope: JSONObject) {
        val id = envelope.optString("id")
        if (envelope.optString("destination") != localNodeId) return

        val firstDelivery = messageStore.markSeen(id)
        sendAcknowledgement(envelope)
        if (!firstDelivery) {
            emit("MessageStatusChanged", mapOf("id" to id, "status" to "duplicate_ignored"))
            return
        }

        emit(
            "MessageReceived",
            mapOf(
                "id" to id,
                "source" to envelope.optString("source"),
                "destination" to envelope.optString("destination"),
                "payload" to envelope.optString("payload"),
                "createdAt" to envelope.optLong("createdAt"),
            ),
        )
    }

    private fun sendAcknowledgement(message: JSONObject) {
        transport.send(
            JSONObject()
                .put("type", "ack")
                .put("id", message.optString("id"))
                .put("source", localNodeId)
                .put("destination", message.optString("source"))
                .put("createdAt", System.currentTimeMillis()),
        )
    }

    private fun sendStoredMessage(message: JSONObject): Boolean {
        messageStore.markAttempted(message.optString("id"))
        return transport.send(message)
    }

    private fun retryPendingMessages() {
        messageStore.pendingMessages().forEach { message ->
            val sent = sendStoredMessage(message)
            emit(
                "MessageStatusChanged",
                mapOf(
                    "id" to message.optString("id"),
                    "status" to if (sent) "sent_retry" else "queued",
                ),
            )
        }
    }
}
