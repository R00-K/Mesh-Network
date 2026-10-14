package org.disastermesh.app.mesh.channels

import android.content.Context
import android.content.Intent
import android.net.wifi.p2p.WifiP2pInfo
import android.os.Build
import android.os.Handler
import android.os.Looper
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import org.disastermesh.app.mesh.discovery.MeshDiscoveryManager
import org.disastermesh.app.mesh.service.MeshForegroundService
import org.disastermesh.app.mesh.storage.MessageStore
import org.disastermesh.app.mesh.storage.StoredMessage
import org.disastermesh.app.mesh.transport.MeshTransport
import org.disastermesh.core.logging.MeshLogger
import java.util.concurrent.ConcurrentHashMap

/** Flutter bridge for Wi-Fi Direct discovery, group links, and relayed messaging. */
class MeshChannelHandler(
    private val context: Context,
    private val logger: MeshLogger,
    private val requestPermissions: () -> Unit,
) : MethodChannel.MethodCallHandler, EventChannel.StreamHandler {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var eventSink: EventChannel.EventSink? = null
    private var running = false
    private var permissionRequestPending = false
    private var discoveryState = "stopped"
    private var connectionState = "disconnected"
    private var selectedPeerId: String? = null
    private var pendingConnectionResult: MethodChannel.Result? = null
    private val deliveryRetryTimers = mutableMapOf<String, Runnable>()
    private val relayOrigins = ConcurrentHashMap<String, String>()
    private val messageStore = MessageStore(context)
    private val localNodeId = messageStore.localNodeId()
    private val connectionTimeout = Runnable {
        if (connectionState == "connecting") {
            if (transport.isConnected()) {
                connectionState = "connected"
                emit("ConnectionStatusChanged", mapOf("status" to connectionState, "peerId" to selectedPeerId))
            } else {
                transport.close()
                connectionState = "failed"
                val peerId = selectedPeerId
                selectedPeerId = null
                emit("ConnectionStatusChanged", mapOf("status" to connectionState, "peerId" to peerId, "detail" to "Connection timed out. Keep both devices nearby and retry."))
            }
        }
    }
    private val transport = MeshTransport(
        localNodeId = localNodeId,
        onMessage = { fromPeer, message -> receiveMessage(fromPeer, message) },
        onDelivery = { fromPeer, id ->
            val origin = relayOrigins.remove(id)
            if (origin != null && fromPeer != origin) {
                transport.acknowledge(id, origin)
            } else {
                messageStore.markDelivered(id)?.let { delivered ->
                    if (delivered.direction == "outgoing") {
                        emit("MessageUpdated", delivered.asMap())
                        mainHandler.post { deliveryRetryTimers.remove(id)?.let(mainHandler::removeCallbacks) }
                    }
                }
            }
        },
        onConnection = { connected, detail ->
            mainHandler.post {
                connectionState = if (connected) "connected" else "disconnected"
                emit("ConnectionStatusChanged", mapOf(
                    "status" to connectionState,
                    "peerId" to selectedPeerId,
                    "role" to if (connected && transport.isGroupOwner()) "group_owner" else if (connected) "client" else null,
                    "peerCount" to transport.connectedPeerCount(),
                    "detail" to detail,
                ))
                if (connected) flushPendingMessages()
            }
        },
    )
    private val discovery = MeshDiscoveryManager(context.applicationContext, logger, ::handleConnectionInfo)

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "startMesh" -> {
                running = true
                if (discovery.hasDiscoveryPermission()) {
                    startDiscovery()
                } else {
                    permissionRequestPending = true
                    discoveryState = "permission_required"
                    requestPermissions()
                    emit("DiscoveryStatusChanged", mapOf("status" to discoveryState))
                }
                val serviceIntent = Intent(context, MeshForegroundService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(serviceIntent)
                else context.startService(serviceIntent)
                logger.info("mesh_started")
                emit("MeshStatusChanged", mapOf("running" to true))
                result.success(null)
            }
            "stopMesh" -> {
                running = false
                permissionRequestPending = false
                discoveryState = "stopped"
                connectionState = "disconnected"
                pendingConnectionResult?.error("STOPPED", "Mesh stopped while connecting", null)
                pendingConnectionResult = null
                deliveryRetryTimers.values.forEach { mainHandler.removeCallbacks(it) }
                deliveryRetryTimers.clear()
                transport.close()
                discovery.stop()
                context.stopService(Intent(context, MeshForegroundService::class.java))
                emit("ConnectionStatusChanged", mapOf("status" to connectionState))
                emit("MeshStatusChanged", mapOf("running" to false))
                result.success(null)
            }
            "discoverPeers" -> {
                if (discovery.hasDiscoveryPermission()) {
                    discovery.discover(::setDiscoveryResult)
                    discoveryState = "starting"
                } else {
                    permissionRequestPending = true
                    discoveryState = "permission_required"
                    requestPermissions()
                }
                emit("DiscoveryStatusChanged", mapOf("status" to discoveryState))
                result.success(null)
            }
            "connectPeer" -> {
                val peerId = call.argument<String>("peerId")
                if (peerId.isNullOrBlank()) {
                    result.error("INVALID_ARGUMENT", "peerId is required", null)
                } else if (!discovery.hasDiscoveryPermission()) {
                    result.error("PERMISSION_REQUIRED", "Grant Wi-Fi Direct permissions before connecting", null)
                } else {
                    selectedPeerId = peerId
                    connectionState = "connecting"
                    pendingConnectionResult = result
                    emit("ConnectionStatusChanged", mapOf("status" to connectionState, "peerId" to peerId))
                    discovery.connect(peerId) { accepted, reason ->
                        if (!accepted) {
                            connectionState = if (transport.isConnected()) "connected" else "failed"
                            pendingConnectionResult = null
                            emit("ConnectionStatusChanged", mapOf("status" to connectionState, "peerId" to peerId, "reason" to reason, "detail" to "Android could not add this peer to the Wi-Fi Direct group."))
                            result.error("CONNECT_FAILED", "Android could not start the Wi-Fi Direct connection (reason $reason)", null)
                        } else {
                            pendingConnectionResult = null
                            result.success(null)
                            mainHandler.postDelayed(connectionTimeout, 30_000)
                        }
                    }
                }
            }
            "disconnectPeer" -> {
                discovery.disconnect { removed ->
                    transport.close()
                    selectedPeerId = null
                    connectionState = if (removed) "disconnected" else "failed"
                    emit("ConnectionStatusChanged", mapOf("status" to connectionState))
                    result.success(removed)
                }
            }
            "sendMessage" -> {
                val text = call.argument<String>("text")?.trim().orEmpty()
                if (text.isEmpty()) {
                    result.error("INVALID_ARGUMENT", "Message text is required", null)
                } else {
                    try {
                        val message = messageStore.createOutgoing(text, "broadcast", localNodeId)
                        val updated = transmit(message) ?: message
                        emit("MessageUpdated", updated.asMap())
                        result.success(mapOf("id" to updated.id, "status" to updated.status))
                    } catch (error: Exception) {
                        result.error("STORAGE_FAILED", error.message ?: "Could not save the message", null)
                    }
                }
            }
            "getMessages" -> result.success(messageStore.allMessages().map { it.asMap() })
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
                        "status" to if (peer.deviceAddress == selectedPeerId && connectionState == "connected") "connected" else "nearby",
                    )
                }
                emit("PeerUpdate", mapOf("peers" to peers))
                result.success(peers)
            }
            "getDiscoveryStatus" -> result.success(discoveryState)
            "getConnectionStatus" -> result.success(mapOf(
                "status" to connectionState,
                "peerId" to selectedPeerId,
                "role" to if (connectionState == "connected" && transport.isGroupOwner()) "group_owner" else if (connectionState == "connected") "client" else null,
                "peerCount" to transport.connectedPeerCount(),
            ))
            "getMetrics" -> result.success(emptyMap<String, Any?>())
            "getRoute" -> result.success(null)
            else -> result.notImplemented()
        }
    }

    override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
        eventSink = events
        emit("MeshStatusChanged", mapOf("running" to running))
        emit("ConnectionStatusChanged", mapOf(
            "status" to connectionState,
            "peerId" to selectedPeerId,
            "role" to if (connectionState == "connected" && transport.isGroupOwner()) "group_owner" else if (connectionState == "connected") "client" else null,
            "peerCount" to transport.connectedPeerCount(),
        ))
    }

    override fun onCancel(arguments: Any?) { eventSink = null }

    fun onPermissionsResult(granted: Boolean) {
        if (!permissionRequestPending) return
        permissionRequestPending = false
        if (granted) startDiscovery()
        else {
            discoveryState = "permission_denied"
            emit("DiscoveryStatusChanged", mapOf("status" to discoveryState))
        }
    }

    private fun startDiscovery() {
        discovery.start(::setDiscoveryResult)
        discoveryState = "starting"
        emit("DiscoveryStatusChanged", mapOf("status" to discoveryState))
    }

    private fun setDiscoveryResult(started: Boolean) {
        discoveryState = if (started) "discovering" else "discovery_failed"
        emit("DiscoveryStatusChanged", mapOf("status" to discoveryState))
    }

    private fun handleConnectionInfo(info: WifiP2pInfo) {
        mainHandler.post {
            if (info.groupFormed) {
                mainHandler.removeCallbacks(connectionTimeout)
                if (!transport.isConnected()) {
                    connectionState = "connecting"
                    emit("ConnectionStatusChanged", mapOf("status" to connectionState, "peerId" to selectedPeerId))
                    mainHandler.postDelayed(connectionTimeout, 30_000)
                }
                transport.start(info)
            } else if (connectionState != "disconnected") {
                transport.start(info)
                pendingConnectionResult?.error("CONNECTION_LOST", "Wi-Fi Direct group was not formed", null)
                pendingConnectionResult = null
                selectedPeerId = null
                connectionState = "disconnected"
                emit("ConnectionStatusChanged", mapOf("status" to connectionState))
            }
        }
    }

    private fun receiveMessage(fromPeer: String, message: Map<String, Any?>) {
        val id = message["id"] as? String ?: return
        val text = message["text"] as? String ?: return
        val source = message["source"] as? String
        val ttl = (message["ttl"] as? Number)?.toInt() ?: 0
        @Suppress("UNCHECKED_CAST")
        val path = message["path"] as? List<String> ?: emptyList()
        val isNew = messageStore.storeIncoming(id, text, source, ttl, path)
        if (isNew) messageStore.allMessages().firstOrNull { it.id == id }?.let { emit("MessageUpdated", it.asMap()) }

        // A Wi-Fi Direct group owner relays each unseen broadcast at most once.
        if (transport.isGroupOwner() && ttl > 1 && localNodeId !in path) {
            val forwarded = message.toMutableMap().apply {
                put("ttl", ttl - 1)
                put("path", path + localNodeId)
            }
            relayOrigins[id] = fromPeer
            mainHandler.postDelayed({ relayOrigins.remove(id, fromPeer) }, RELAY_ORIGIN_TTL_MS)
            if (!transport.forward(forwarded, fromPeer)) {
                relayOrigins.remove(id)
                transport.acknowledge(id, fromPeer)
            }
        } else {
            // A leaf acknowledges only after the incoming ID and content are persisted.
            transport.acknowledge(id, fromPeer)
        }
    }

    private fun flushPendingMessages() {
        for (message in messageStore.pendingOutgoing()) {
            val updated = transmit(message) ?: continue
            emit("MessageUpdated", updated.asMap())
            if (!transport.isConnected()) break
        }
    }

    private fun transmit(message: StoredMessage): StoredMessage? {
        if (!transport.isConnected()) return null
        val attempted = messageStore.markAttempt(message.id) ?: return null
        if (attempted.status == "delivered") return attempted
        val sent = transport.send(attempted.id, attempted.text, attempted.createdAt, attempted.ttl, attempted.path)
        if (!sent) {
            deliveryRetryTimers.remove(attempted.id)?.let(mainHandler::removeCallbacks)
            return messageStore.markQueued(attempted.id)
        }
        return messageStore.markSent(attempted.id)?.also { scheduleDeliveryRetry(it.id) }
    }

    private fun scheduleDeliveryRetry(id: String) {
        deliveryRetryTimers.remove(id)?.let(mainHandler::removeCallbacks)
        val retry = Runnable {
            deliveryRetryTimers.remove(id)
            if (!transport.isConnected()) return@Runnable
            val message = messageStore.pendingOutgoing().firstOrNull { it.id == id } ?: return@Runnable
            if (message.retryCount >= MAX_AUTOMATIC_ATTEMPTS) {
                messageStore.markQueued(id)?.let { emit("MessageUpdated", it.asMap()) }
            } else {
                transmit(message)?.let { emit("MessageUpdated", it.asMap()) }
            }
        }
        deliveryRetryTimers[id] = retry
        mainHandler.postDelayed(retry, DELIVERY_ACK_TIMEOUT_MS)
    }

    private fun StoredMessage.asMap() = mapOf<String, Any?>(
        "id" to id,
        "text" to text,
        "source" to source,
        "destination" to destination,
        "timestamp" to createdAt,
        "ttl" to ttl,
        "path" to path,
        "status" to status,
        "direction" to direction,
        "retryCount" to retryCount,
        "lastAttemptAt" to lastAttemptAt,
    )

    private companion object {
        const val MAX_AUTOMATIC_ATTEMPTS = 5
        const val DELIVERY_ACK_TIMEOUT_MS = 8_000L
        const val RELAY_ORIGIN_TTL_MS = 120_000L
    }

    private fun emit(type: String, payload: Map<String, Any?>) {
        mainHandler.post { eventSink?.success(mapOf("type" to type, "payload" to payload)) }
    }
}
