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
import org.disastermesh.app.mesh.identity.LocalNodeIdentity
import org.disastermesh.app.mesh.storage.IncomingMessageResult
import android.os.Handler
import android.os.Looper
import org.disastermesh.core.logging.MeshLogger
import org.disastermesh.core.messaging.MessageRetryPolicy
import org.json.JSONObject
import org.json.JSONArray
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** Initial Flutter bridge for the native mesh service boundary. */
class MeshChannelHandler(
    private val context: Context,
    private val logger: MeshLogger,
    private val requestPermissions: () -> Unit,
) : MethodChannel.MethodCallHandler, EventChannel.StreamHandler {
    private val messageStore = MessageStore(context.applicationContext)
    private val localNodeId = LocalNodeIdentity(context).nodeId
    private val metrics = MetricsCollector(context.applicationContext)
    private val sentAt = ConcurrentHashMap<String, Long>()
    private val metricsHandler = Handler(Looper.getMainLooper())
    private val recoveryHandler = Handler(Looper.getMainLooper())
    private val retryHandler = Handler(Looper.getMainLooper())
    private val retryTasks = ConcurrentHashMap<String, Runnable>()
    private val expiryTasks = ConcurrentHashMap<String, Runnable>()
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
    private val transport = MeshTransport(
        localNodeId = localNodeId,
        logger = logger,
        onStateChanged = { state, error ->
            emit("TransportChanged", mapOf("state" to state, "error" to error))
            routeCoordinator.transportChanged(state, connection.connectedPeerId)
            if (state == "connected") {
                retryPendingMessages()
            } else if (state == "disconnected" || state == "failed") {
                cancelRetryTasks()
                messageStore.markAllQueued().forEach { id ->
                    emit("MessageStatusChanged", mapOf("id" to id, "status" to "queued"))
                }
                scheduleLinkRecovery()
            }
            emitMetrics()
        },
        onPeerIdentified = { nodeId ->
            emit("PeerIdentityChanged", mapOf("nodeId" to nodeId, "transportAddress" to connection.connectedPeerId))
            emitPeers()
        },
        onEnvelopeReceived = ::handleEnvelope,
    )
    private val connection: WifiDirectConnectionManager by lazy {
        WifiDirectConnectionManager(
        context = context.applicationContext,
        logger = logger,
        onStateChanged = { state, peerId, error ->
            if (state == "connected") {
                transport.start(connection.isGroupOwner, connection.groupOwnerAddress)
            } else if (state == "disconnected" || state == "failed") {
                transport.stop()
            }
            routeCoordinator.connectionChanged(state, peerId)
            emit("ConnectionChanged", mapOf("state" to state, "peerId" to peerId, "error" to error))
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
        onDiscoveryError = { error ->
            emit("MeshStatusChanged", mapOf("running" to running, "error" to error))
        },
        onPeersChanged = { emitPeers() },
        onConnectionChanged = { connection.refreshConnectionInfo() },
        onLocalDeviceIdChanged = { address ->
            emit("LocalDeviceChanged", mapOf("nodeId" to localNodeId, "transportAddress" to address))
        },
    )
    private var eventSink: EventChannel.EventSink? = null
    private var running = false
    private var pendingStart = false
    private var pendingConnectPeerId: String? = null

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
                cancelRetryTasks()
                metricsHandler.removeCallbacksAndMessages(null)
                routeCoordinator.noRoute("mesh_stopped")
                discovery.stop()
                connection.disconnect()
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
                    pendingConnectPeerId = peerId
                    requestPermissions()
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
                    "peerNodeId" to transport.peerNodeId,
                    "localNodeId" to localNodeId,
                    "isConnected" to connection.isConnected,
                    "transportState" to transport.state,
                    "transportError" to transport.error,
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
                    val transportAddress = normalizeNodeId(destination)
                    val protocolDestination = if (
                        transportAddress == normalizeNodeId(connection.connectedPeerId.orEmpty())
                    ) {
                        transport.peerNodeId
                    } else {
                        destination
                    }
                    if (protocolDestination.isNullOrBlank()) {
                        result.error("PEER_NOT_IDENTIFIED", "Connect to the peer and wait for identity verification before sending", null)
                        return
                    }
                    val id = UUID.randomUUID().toString()
                    val now = System.currentTimeMillis()
                    val envelope = JSONObject()
                        .put("type", "message")
                        .put("protocolVersion", PROTOCOL_VERSION)
                        .put("id", id)
                        .put("source", localNodeId)
                        .put("destination", normalizeNodeId(protocolDestination))
                        .put("payload", payload)
                        .put("createdAt", now)
                        .put("expiresAt", now + MESSAGE_EXPIRY_MS)
                        .put("hopLimit", 1)
                        .put("path", JSONArray().put(localNodeId))
                    if (!messageStore.enqueue(envelope)) {
                        result.error("MESSAGE_STORE_FAILED", "Could not save the message on this device", null)
                        return
                    }
                    logger.info("mesh_message_enqueued", mapOf("id" to id, "destination" to normalizeNodeId(protocolDestination)))
                    scheduleExpiry(id, now + MESSAGE_EXPIRY_MS)
                    val status = attemptMessage(envelope)
                    result.success(mapOf("id" to id, "status" to status))
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
            "getMessages" -> result.success(
                messageStore.storedMessages().map { message ->
                    mapOf<String, Any?>(
                        "id" to message.optString("id"),
                        "source" to message.optString("source"),
                        "destination" to message.optString("destination"),
                        "payload" to message.optString("payload"),
                        "direction" to message.optString("direction"),
                        "status" to message.optString("status", "received"),
                        "createdAt" to message.optLong("createdAt"),
                    )
                },
            )
            "getMetrics" -> result.success(metrics.snapshot(transport.connectedLinkCount))
            else -> result.notImplemented()
        }
    }

    override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
        eventSink = events
        emit("MeshStatusChanged", mapOf("running" to running, "discovering" to discovery.isDiscovering))
        val connectionState = when {
            connection.isConnected -> "connected"
            connection.connectedPeerId != null -> "connecting"
            else -> "disconnected"
        }
        emit(
            "ConnectionChanged",
            mapOf("state" to connectionState, "peerId" to connection.connectedPeerId),
        )
        emit("TransportChanged", mapOf("state" to transport.state))
        emit(
            "PeerIdentityChanged",
            mapOf("nodeId" to transport.peerNodeId, "transportAddress" to connection.connectedPeerId),
        )
        emitPeers()
        emit("RoutingEvaluationChanged", routeEvaluator.evaluation())
    }

    override fun onCancel(arguments: Any?) {
        eventSink = null
    }

    /** Detaches the Flutter event bridge when its engine is being destroyed. */
    fun detach() {
        eventSink = null
        retryHandler.removeCallbacksAndMessages(null)
        expiryHandler.removeCallbacksAndMessages(null)
        recoveryHandler.removeCallbacksAndMessages(null)
        metricsHandler.removeCallbacksAndMessages(null)
        discovery.stop()
        transport.stop()
    }

    private fun emit(type: String, payload: Map<String, Any?>) {
        val event = mapOf("type" to type, "payload" to payload)
        if (Looper.myLooper() == Looper.getMainLooper()) {
            deliverEvent(event)
        } else {
            Handler(Looper.getMainLooper()).post { deliverEvent(event) }
        }
    }

    private fun deliverEvent(event: Map<String, Any?>) {
        val sink = eventSink ?: return
        try {
            sink.success(event)
        } catch (error: IllegalStateException) {
            // FlutterJNI can detach before the platform stream cancellation
            // callback arrives during Activity or engine teardown.
            if (eventSink === sink) eventSink = null
            logger.warn("mesh_event_delivery_failed", mapOf("error" to (error.message ?: "engine_detached")))
        }
    }

    fun onPermissionsResult(granted: Boolean) {
        if (!pendingStart && pendingConnectPeerId == null) return
        val shouldStart = pendingStart
        val peerToConnect = pendingConnectPeerId
        pendingStart = false
        pendingConnectPeerId = null
        if (!granted) {
            if (shouldStart) {
                emit("MeshStatusChanged", mapOf("running" to false, "discovering" to false, "state" to "permission_denied"))
            }
            peerToConnect?.let(connection::connect)
            return
        }
        if (shouldStart) {
            running = true
            discovery.start()
            connection.refreshConnectionInfo()
            schedulePendingExpiries()
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
        peerToConnect?.let(connection::connect)
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
        if (connection.isConnected && transport.peerNodeId != null &&
            normalizeNodeId(connection.connectedPeerId.orEmpty()) == normalizeNodeId(peerId)
        ) {
            "connected"
        } else {
            "nearby"
        }

    private fun handleEnvelope(envelope: JSONObject) {
        when (envelope.optString("type")) {
            "ack" -> handleAcknowledgement(envelope)
            "message" -> handleIncomingMessage(envelope)
            else -> logger.warn("mesh_envelope_rejected", mapOf("reason" to "unknown_type"))
        }
    }

    private fun handleAcknowledgement(ack: JSONObject) {
        val id = ack.optString("id")
        val source = normalizeNodeId(ack.optString("source"))
        val destination = normalizeNodeId(ack.optString("destination"))
        val pending = messageStore.pendingMessage(id) ?: return
        val expectedPeer = normalizeNodeId(pending.optString("destination"))
        if (!validCommonEnvelope(ack) ||
            !isNodeId(id) ||
            normalizeNodeId(pending.optString("source")) != localNodeId ||
            destination != localNodeId ||
            source != expectedPeer ||
            transport.peerNodeId?.let(::normalizeNodeId) != source
        ) {
            logger.warn("mesh_envelope_rejected", mapOf("reason" to "invalid_ack", "id" to id))
            return
        }
        if (!messageStore.markDelivered(id)) return
        logger.info("mesh_message_delivered", mapOf("id" to id, "source" to source))
        cancelRetry(id)
        cancelExpiry(id)
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

    private fun handleIncomingMessage(envelope: JSONObject) {
        val id = envelope.optString("id")
        val source = normalizeNodeId(envelope.optString("source"))
        val destination = normalizeNodeId(envelope.optString("destination"))
        val payload = envelope.optString("payload")
        val createdAt = envelope.optLong("createdAt", -1L)
        val expiresAt = envelope.optLong("expiresAt", -1L)
        if (!validCommonEnvelope(envelope) ||
            !isNodeId(id) ||
            !isNodeId(source) ||
            !isNodeId(destination) ||
            envelope.opt("payload") !is String ||
            payload.toByteArray(Charsets.UTF_8).size > MAX_PAYLOAD_BYTES ||
            createdAt <= 0 ||
            expiresAt <= createdAt ||
            expiresAt > createdAt + MESSAGE_EXPIRY_MS ||
            source != transport.peerNodeId?.let(::normalizeNodeId) ||
            destination != localNodeId
        ) {
            logger.warn("mesh_envelope_rejected", mapOf("reason" to "invalid_message", "id" to id))
            return
        }
        when (messageStore.acceptIncoming(envelope)) {
            IncomingMessageResult.PERSISTENCE_FAILED -> {
                logger.warn("mesh_message_persist_failed", mapOf("id" to id))
                return
            }
            IncomingMessageResult.DUPLICATE -> {
                logger.info("mesh_message_duplicate", mapOf("id" to id))
                sendAcknowledgement(envelope)
                return
            }
            IncomingMessageResult.CONFLICT -> {
                logger.warn("mesh_envelope_rejected", mapOf("reason" to "message_id_conflict", "id" to id))
                return
            }
            IncomingMessageResult.STORED -> {
                logger.info("mesh_message_received", mapOf("id" to id, "source" to source))
                sendAcknowledgement(envelope)
                emit(
                    "MessageReceived",
                    mapOf(
                        "id" to id,
                        "source" to source,
                        "destination" to destination,
                        "payload" to payload,
                        "createdAt" to createdAt,
                    ),
                )
            }
        }
    }

    private fun sendAcknowledgement(message: JSONObject) {
        if (normalizeNodeId(message.optString("source")) !=
            transport.peerNodeId?.let(::normalizeNodeId)
        ) return
        val ack = JSONObject()
            .put("type", "ack")
            .put("protocolVersion", PROTOCOL_VERSION)
            .put("id", message.optString("id"))
            .put("source", localNodeId)
            .put("destination", normalizeNodeId(message.optString("source")))
            .put("createdAt", System.currentTimeMillis())
        if (!transport.send(ack)) {
            logger.warn("mesh_ack_send_failed", mapOf("id" to message.optString("id")))
        }
    }

    private fun attemptMessage(message: JSONObject): String {
        val id = message.optString("id")
        val pending = messageStore.pendingMessage(id) ?: return "delivered"
        if (expireIfNeeded(pending)) return "failed"
        if (!validCommonEnvelope(pending) ||
            !isNodeId(id) ||
            normalizeNodeId(pending.optString("source")) != localNodeId ||
            !isNodeId(normalizeNodeId(pending.optString("destination"))) ||
            pending.opt("payload") !is String ||
            pending.optString("payload").toByteArray(Charsets.UTF_8).size > MAX_PAYLOAD_BYTES
        ) {
            failMessage(id, "invalid_stored_message")
            return "failed"
        }
        if (normalizeNodeId(pending.optString("destination")) !=
            transport.peerNodeId?.let(::normalizeNodeId)
        ) {
            messageStore.markQueued(id)
            emit("MessageStatusChanged", mapOf("id" to id, "status" to "queued"))
            return "queued"
        }
        val previousAttempts = pending.optInt("attemptCount", pending.optInt("retryCount"))
        if (previousAttempts >= MAX_SEND_ATTEMPTS) {
            failMessage(id, "retry_limit")
            return "failed"
        }
        val sendTime = System.currentTimeMillis()
        sentAt[id] = sendTime
        if (!transport.send(pending)) {
            sentAt.remove(id)
            messageStore.markQueued(id)
            emit("MessageStatusChanged", mapOf("id" to id, "status" to "queued"))
            // A verified socket can still reject a write during a transient race.
            // Keep retrying while the message remains queued instead of waiting for
            // another connection event that may never arrive.
            scheduleRetry(id, previousAttempts.coerceAtLeast(1))
            return "queued"
        }
        logger.info("mesh_message_socket_write", mapOf("id" to id, "destination" to pending.optString("destination")))
        val attempts = messageStore.recordAttempt(id) ?: return "delivered"
        metrics.recordSend(true)
        emit("MessageStatusChanged", mapOf("id" to id, "status" to "sent"))
        scheduleRetry(id, attempts)
        return "sent"
    }

    private fun retryPendingMessages() {
        messageStore.pendingMessages().forEach { message ->
            scheduleExpiry(
                message.optString("id"),
                message.optLong("expiresAt", message.optLong("createdAt") + MESSAGE_EXPIRY_MS),
            )
            attemptMessage(message)
        }
    }

    private fun scheduleRetry(id: String, attempts: Int) {
        cancelRetry(id)
        val delay = MessageRetryPolicy.delayAfterAttempt(attempts)
        val task = Runnable {
            retryTasks.remove(id)
            val pending = messageStore.pendingMessage(id) ?: return@Runnable
            if (expireIfNeeded(pending)) return@Runnable
            if (normalizeNodeId(pending.optString("destination")) !=
                transport.peerNodeId?.let(::normalizeNodeId)
            ) {
                messageStore.markQueued(id)
                emit("MessageStatusChanged", mapOf("id" to id, "status" to "queued"))
            } else if (pending.optInt("attemptCount", pending.optInt("retryCount")) >= MAX_SEND_ATTEMPTS) {
                failMessage(id, "retry_limit")
            } else {
                logger.info("mesh_message_retry", mapOf("id" to id, "attempt" to (pending.optInt("attemptCount", pending.optInt("retryCount")) + 1)))
                attemptMessage(pending)
            }
        }
        retryTasks[id] = task
        retryHandler.postDelayed(task, delay)
    }

    private fun expireIfNeeded(message: JSONObject): Boolean {
        val id = message.optString("id")
        val createdAt = message.optLong("createdAt", -1L)
        val expiresAt = message.optLong("expiresAt", createdAt + MESSAGE_EXPIRY_MS)
        if (createdAt <= 0 || expiresAt <= System.currentTimeMillis()) {
            failMessage(id, "expired")
            return true
        }
        return false
    }

    private fun schedulePendingExpiries() {
        messageStore.pendingMessages().forEach { message ->
            scheduleExpiry(
                message.optString("id"),
                message.optLong("expiresAt", message.optLong("createdAt") + MESSAGE_EXPIRY_MS),
            )
        }
    }

    private fun scheduleExpiry(id: String, expiresAt: Long) {
        if (expiresAt <= 0 || expiryTasks.containsKey(id)) return
        val task = Runnable {
            expiryTasks.remove(id)
            val pending = messageStore.pendingMessage(id) ?: return@Runnable
            if (pending.optLong("expiresAt", pending.optLong("createdAt") + MESSAGE_EXPIRY_MS) <=
                System.currentTimeMillis()
            ) {
                failMessage(id, "expired")
            }
        }
        expiryTasks[id] = task
        expiryHandler.postDelayed(task, (expiresAt - System.currentTimeMillis()).coerceAtLeast(0L))
    }

    private fun failMessage(id: String, reason: String) {
        cancelRetry(id)
        cancelExpiry(id)
        messageStore.markFailed(id, reason)
        sentAt.remove(id)
        logger.warn("mesh_message_failed", mapOf("id" to id, "reason" to reason))
        emit("MessageStatusChanged", mapOf("id" to id, "status" to "failed", "reason" to reason))
    }

    private fun cancelRetry(id: String) {
        retryTasks.remove(id)?.let(retryHandler::removeCallbacks)
    }

    private fun cancelExpiry(id: String) {
        expiryTasks.remove(id)?.let(expiryHandler::removeCallbacks)
    }

    private fun cancelRetryTasks() {
        retryTasks.values.forEach(retryHandler::removeCallbacks)
        retryTasks.clear()
    }

    private val expiryHandler = Handler(Looper.getMainLooper())

    private fun validCommonEnvelope(envelope: JSONObject): Boolean =
        envelope.optInt("protocolVersion", -1) == PROTOCOL_VERSION &&
            envelope.optLong("createdAt", -1L) > 0

    private fun isNodeId(value: String): Boolean = try {
        UUID.fromString(value).toString() == value.lowercase()
    } catch (_: IllegalArgumentException) {
        false
    }

    private fun normalizeNodeId(value: String): String {
        val normalized = value.trim().lowercase()
        // Node IDs are canonical UUID strings and must keep their hyphens in
        // protocol envelopes. Wi-Fi Direct transport addresses are MAC strings,
        // where separators are insignificant for comparisons.
        return if (isNodeId(normalized)) {
            normalized
        } else {
            normalized.replace(":", "").replace("-", "")
        }
    }

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
        const val PROTOCOL_VERSION = 1
        const val MAX_PAYLOAD_BYTES = 8 * 1024
        const val MAX_SEND_ATTEMPTS = MessageRetryPolicy.MAX_SEND_ATTEMPTS
        const val MESSAGE_EXPIRY_MS = 24 * 60 * 60 * 1_000L
    }
}
