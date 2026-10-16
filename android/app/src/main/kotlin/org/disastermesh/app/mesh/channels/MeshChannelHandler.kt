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
import org.disastermesh.app.mesh.routing.RouteCoordinator
import org.disastermesh.app.mesh.routing.AdaptiveRouteEvaluator
import org.disastermesh.app.mesh.metrics.MetricsCollector
import android.os.Handler
import android.os.Looper
import org.disastermesh.core.logging.MeshLogger
import org.json.JSONObject
import org.json.JSONArray
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import android.provider.Settings

/** Initial Flutter bridge for the native mesh service boundary. */
class MeshChannelHandler(
    private val context: Context,
    private val logger: MeshLogger,
    private val requestPermissions: () -> Unit,
) : MethodChannel.MethodCallHandler, EventChannel.StreamHandler {
    private val messageStore = MessageStore(context.applicationContext)
    private val metrics = MetricsCollector(context.applicationContext)
    private val sentAt = ConcurrentHashMap<String, Long>()
    private val metricsHandler = Handler(Looper.getMainLooper())
    private val recoveryHandler = Handler(Looper.getMainLooper())
    private val routeCoordinator = RouteCoordinator(logger) { route ->
        emit(
            "RouteChanged",
            mapOf(
                "status" to route.status,
                "destination" to route.destination,
                "hops" to route.hops,
                "reason" to route.reason,
            ),
        )
    }
    private val routeEvaluator = AdaptiveRouteEvaluator(context.applicationContext)
    private val localNodeId = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ANDROID_ID,
    ) ?: "android-node"
    private val transport = MeshTransport(
        logger = logger,
        onStateChanged = { state ->
            emit("TransportChanged", mapOf("state" to state))
            routeCoordinator.transportChanged(state, connection.connectedPeerId)
            if (state == "connected") retryPendingMessages()
            if (state == "disconnected" || state == "failed") scheduleLinkRecovery()
            emitMetrics()
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
            routeCoordinator.connectionChanged(state, peerId)
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
                recoveryHandler.removeCallbacksAndMessages(null)
                metricsHandler.removeCallbacksAndMessages(null)
                routeCoordinator.noRoute("mesh_stopped")
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
            "getRoute" -> result.success(
                mapOf(
                    "status" to routeCoordinator.snapshot.status,
                    "destination" to routeCoordinator.snapshot.destination,
                    "hops" to routeCoordinator.snapshot.hops,
                    "reason" to routeCoordinator.snapshot.reason,
                ),
            )
            "getRoutingEvaluation" -> result.success(routeEvaluator.evaluation())
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
                        .put("hopLimit", 4)
                        .put("path", JSONArray().put(localNodeId))
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
            "getMetrics" -> result.success(metrics.snapshot(transport.connectedLinkCount))
            else -> result.notImplemented()
        }
    }

    override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
        eventSink = events
        emit("MeshStatusChanged", mapOf("running" to running, "discovering" to discovery.isDiscovering))
        emitPeers()
        emit("RoutingEvaluationChanged", routeEvaluator.evaluation())
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
        metricsHandler.removeCallbacksAndMessages(null)
        metricsHandler.post(metricsRunnable)
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
                if (envelope.optString("destination") == localNodeId) {
                    messageStore.markDelivered(id)
                    val latency = sentAt.remove(id)?.let { System.currentTimeMillis() - it }
                    metrics.recordAcknowledgement(latency)
                    routeEvaluator.recordOutcome(
                        routeId = connection.connectedPeerId ?: "unknown",
                        delivered = true,
                        latencyMs = latency,
                        routeScore = metrics.snapshot(transport.connectedLinkCount)["routeScore"] as? Double,
                    )
                    emit("MessageStatusChanged", mapOf("id" to id, "status" to "delivered"))
                    emitMetrics()
                    emit("RoutingEvaluationChanged", routeEvaluator.evaluation())
                }
            }
            "message" -> handleIncomingMessage(envelope)
        }
    }

    private fun handleIncomingMessage(envelope: JSONObject) {
        val id = envelope.optString("id")
        val path = envelope.optJSONArray("path") ?: JSONArray()
        if (containsPath(path, localNodeId)) {
            emit("MessageStatusChanged", mapOf("id" to id, "status" to "loop_ignored"))
            return
        }

        val firstDelivery = messageStore.markSeen(id)
        if (!firstDelivery) {
            emit("MessageStatusChanged", mapOf("id" to id, "status" to "duplicate_ignored"))
            return
        }

        if (envelope.optString("destination") == localNodeId) {
            sendAcknowledgement(envelope)
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
            return
        }

        val remainingHops = envelope.optInt("hopLimit", 0)
        if (remainingHops <= 1) {
            emit("MessageStatusChanged", mapOf("id" to id, "status" to "expired"))
            return
        }
        path.put(localNodeId)
        envelope.put("path", path)
        envelope.put("hopLimit", remainingHops - 1)
        val forwarded = transport.send(envelope)
        emit(
            "MessageStatusChanged",
            mapOf("id" to id, "status" to if (forwarded) "forwarded" else "forward_failed"),
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
        val id = message.optString("id")
        sentAt[id] = System.currentTimeMillis()
        val sent = transport.send(message)
        metrics.recordSend(sent)
        if (!sent) {
            routeEvaluator.recordOutcome(
                routeId = connection.connectedPeerId ?: "unknown",
                delivered = false,
                latencyMs = null,
                routeScore = metrics.snapshot(transport.connectedLinkCount)["routeScore"] as? Double,
            )
            emit("RoutingEvaluationChanged", routeEvaluator.evaluation())
        }
        return sent
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

    private fun containsPath(path: JSONArray, nodeId: String): Boolean =
        (0 until path.length()).any { path.optString(it) == nodeId }

    private fun scheduleLinkRecovery() {
        val peerId = connection.connectedPeerId ?: return
        routeCoordinator.connectionChanged("failed", peerId)
        recoveryHandler.removeCallbacksAndMessages(null)
        recoveryHandler.postDelayed(
            {
                if (connection.isConnected) {
                    connection.refreshConnectionInfo()
                }
            },
            RECOVERY_DELAY_MS,
        )
    }

    private val metricsRunnable = object : Runnable {
        override fun run() {
            emitMetrics()
            if (running) metricsHandler.postDelayed(this, METRICS_INTERVAL_MS)
        }
    }

    private fun emitMetrics() {
        emit("MetricsChanged", metrics.snapshot(transport.connectedLinkCount))
    }

    private companion object {
        const val RECOVERY_DELAY_MS = 2_000L
        const val METRICS_INTERVAL_MS = 5_000L
    }
}
