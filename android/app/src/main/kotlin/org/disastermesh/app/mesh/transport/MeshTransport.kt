package org.disastermesh.app.mesh.transport

import android.os.Handler
import android.os.Looper
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.UUID
import org.disastermesh.core.logging.MeshLogger
import org.json.JSONObject

/** TCP transport used after a Wi-Fi Direct group has formed. */
class MeshTransport(
    private val localNodeId: String,
    private val logger: MeshLogger,
    private val onStateChanged: (String, String?) -> Unit,
    private val onPeerIdentified: (String?) -> Unit,
    private val onEnvelopeReceived: (JSONObject) -> Unit,
) {
    private val executor: ExecutorService = Executors.newCachedThreadPool()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val links = CopyOnWriteArrayList<Link>()
    @Volatile private var serverSocket: ServerSocket? = null
    @Volatile private var activeGroupOwner: Boolean? = null
    @Volatile private var activeAddress: String? = null
    @Volatile private var generation = 0
    @Volatile private var connectLoopGeneration: Int? = null
    @Volatile private var listenerLoopGeneration: Int? = null
    @Volatile private var identifiedPeerId: String? = null
    @Volatile private var currentState: String = "disconnected"
    @Volatile private var lastError: String? = null

    val connectedLinkCount: Int
        get() = links.count { it.peerNodeId != null }

    val peerNodeId: String?
        get() = identifiedPeerId

    val state: String
        get() = currentState

    val error: String?
        get() = lastError

    @Synchronized
    fun start(isGroupOwner: Boolean, groupOwnerAddress: String?) {
        // The group owner never dials an address. Ignore platform address changes
        // on that side so repeated connection-info callbacks cannot restart its listener.
        val transportAddress = if (isGroupOwner) null else groupOwnerAddress?.trim()
        if (activeGroupOwner != isGroupOwner || activeAddress != transportAddress) {
            stopLocked()
            activeGroupOwner = isGroupOwner
            activeAddress = transportAddress
        }
        val currentGeneration = generation
        if (isGroupOwner) {
            if (serverSocket == null && listenerLoopGeneration != currentGeneration) {
                listenerLoopGeneration = currentGeneration
                lastError = null
                emitState("connecting")
                executor.execute { listen(currentGeneration) }
            }
        } else if (!groupOwnerAddress.isNullOrBlank()) {
            if (connectLoopGeneration != currentGeneration &&
                links.none { it.generation == currentGeneration }
            ) {
                connectLoopGeneration = currentGeneration
                lastError = null
                emitState("connecting")
                executor.execute { connectWithRetry(groupOwnerAddress, currentGeneration) }
            }
        } else {
            emitState("failed", "Wi-Fi Direct did not provide a group owner address")
        }
    }

    @Synchronized
    fun send(envelope: JSONObject): Boolean {
        val link = links.firstOrNull { it.peerNodeId != null } ?: return false
        return write(link, envelope)
    }

    @Synchronized
    fun stop() = stopLocked()

    @Synchronized
    private fun stopLocked() {
        generation += 1
        connectLoopGeneration = null
        listenerLoopGeneration = null
        activeGroupOwner = null
        activeAddress = null
        identifiedPeerId = null
        currentState = "disconnected"
        onPeerIdentified(null)
        links.toList().forEach(::removeLink)
        try {
            serverSocket?.close()
        } catch (_: Exception) {
            // The listener may already have stopped.
        }
        serverSocket = null
        emitState("disconnected")
    }

    private fun listen(expectedGeneration: Int) {
        try {
            val server = ServerSocket().apply {
                reuseAddress = true
                bind(InetSocketAddress(PORT))
            }
            synchronized(this) {
                if (generation != expectedGeneration || activeGroupOwner != true) {
                    server.close()
                    return
                }
                serverSocket = server
            }
            emitState("listening")
            while (!server.isClosed && generation == expectedGeneration) {
                attach(server.accept(), expectedGeneration)
            }
        } catch (error: Exception) {
            if (generation == expectedGeneration) {
                synchronized(this) {
                    if (generation == expectedGeneration) serverSocket = null
                }
                listenerLoopGeneration = null
                val reason = error.message ?: error.javaClass.simpleName
                logger.warn("mesh_listener_failed", mapOf("error" to reason))
                emitState("failed", reason)
                scheduleListenerRestart(expectedGeneration)
            }
        }
    }

    private fun connectWithRetry(address: String, expectedGeneration: Int) {
        var lastConnectError = "Could not connect to the Wi-Fi Direct group owner"
        RETRY_DELAYS_MS.forEachIndexed { index, delayMs ->
            if (generation != expectedGeneration) return
            if (delayMs > 0) Thread.sleep(delayMs)
            if (generation != expectedGeneration) return
            var socketToClose: Socket? = null
            try {
                val socket = Socket()
                socketToClose = socket
                socket.connect(InetSocketAddress(address, PORT), CONNECT_TIMEOUT_MS)
                if (generation != expectedGeneration) {
                    socket.close()
                    return
                }
                if (attach(socket, expectedGeneration)) return
                socketToClose = null
            } catch (error: Exception) {
                try { socketToClose?.close() } catch (_: Exception) {}
                socketToClose = null
                lastConnectError = error.message ?: error.javaClass.simpleName
                logger.warn(
                    "mesh_socket_connect_failed",
                    mapOf("attempt" to index + 1, "error" to (error.message ?: "unknown")),
                )
            }
        }
        if (generation == expectedGeneration) {
            synchronized(this) {
                if (generation == expectedGeneration) connectLoopGeneration = null
            }
            lastError = lastConnectError
            emitState("failed", lastConnectError)
            scheduleClientReconnect(expectedGeneration)
        }
    }

    private fun attach(socket: Socket, expectedGeneration: Int): Boolean {
        var attachedLink: Link? = null
        try {
            val link = Link(
                socket = socket,
                reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8)),
                writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8)),
                generation = expectedGeneration,
            )
            synchronized(this) {
                if (generation != expectedGeneration) {
                    socket.close()
                    return false
                }
                links += link
                attachedLink = link
            }
            socket.soTimeout = HELLO_TIMEOUT_MS
            if (!write(link, JSONObject()
                    .put("type", "hello")
                    .put("protocolVersion", PROTOCOL_VERSION)
                    .put("nodeId", localNodeId), allowUnidentified = true)) {
                removeLink(link)
                return false
            }
            executor.execute { readLoop(link) }
            return true
        } catch (error: Exception) {
            logger.warn("mesh_socket_attach_failed", mapOf("error" to (error.message ?: "unknown")))
            val link = attachedLink
            if (link != null) removeLink(link) else try { socket.close() } catch (_: Exception) {}
            return false
        }
    }

    private fun readLoop(link: Link) {
        try {
            while (!link.socket.isClosed && generation == link.generation) {
                val line = link.reader.readLine() ?: break
                val envelope = JSONObject(line)
                if (link.peerNodeId == null) {
                    acceptHello(link, envelope)
                } else if (envelope.optString("type") == "hello") {
                    throw IllegalArgumentException("Unexpected second hello")
                } else {
                    onEnvelopeReceived(envelope)
                }
            }
        } catch (error: Exception) {
            if (generation == link.generation) {
                if (link.peerNodeId == null) {
                    val reason = error.message ?: error.javaClass.simpleName
                    lastError = reason
                    emitState("failed", reason)
                }
                logger.warn("mesh_socket_read_failed", mapOf("error" to (error.message ?: "unknown")))
            }
        } finally {
            removeLink(link)
        }
    }

    private fun acceptHello(link: Link, envelope: JSONObject) {
        val nodeId = envelope.optString("nodeId")
        if (envelope.optString("type") != "hello") {
            throw IllegalArgumentException("Peer did not send a mesh hello")
        }
        val peerProtocolVersion = envelope.optInt("protocolVersion", -1)
        if (peerProtocolVersion != PROTOCOL_VERSION) {
            throw IllegalArgumentException(
                "Incompatible mesh protocol: this app uses $PROTOCOL_VERSION, peer uses $peerProtocolVersion",
            )
        }
        if (!isNodeId(nodeId) || nodeId == localNodeId) {
            throw IllegalArgumentException("Peer sent an invalid or duplicate device identity")
        }
        synchronized(this) {
            if (generation != link.generation || link.peerNodeId != null) return
            if (links.any { it !== link && it.peerNodeId == nodeId }) {
                throw IllegalArgumentException("Duplicate peer connection")
            }
            link.peerNodeId = nodeId
            identifiedPeerId = nodeId
            currentState = "connected"
        }
        link.socket.soTimeout = 0
        mainHandler.post {
            if (generation == link.generation && links.contains(link) &&
                identifiedPeerId == nodeId
            ) {
                onPeerIdentified(nodeId)
                onStateChanged("connected", null)
            }
        }
    }

    private fun write(
        link: Link,
        envelope: JSONObject,
        allowUnidentified: Boolean = false,
    ): Boolean {
        if (link.generation != generation ||
            link.peerNodeId == null &&
                !(allowUnidentified && envelope.optString("type") == "hello")
        ) return false
        return try {
            synchronized(link.writer) {
                link.writer.write(envelope.toString())
                link.writer.newLine()
                link.writer.flush()
            }
            true
        } catch (error: Exception) {
            logger.warn("mesh_send_failed", mapOf("error" to (error.message ?: "unknown")))
            removeLink(link)
            false
        }
    }

    private fun removeLink(link: Link) {
        val (wasIdentifiedPeer, shouldReconnect, hasPendingLink) = synchronized(this) {
            if (!links.remove(link)) return
            val peerId = link.peerNodeId
            val wasIdentifiedPeer = peerId != null && identifiedPeerId == peerId &&
                links.none { it.peerNodeId == peerId }
            if (wasIdentifiedPeer) identifiedPeerId = null

            val shouldReconnect = activeGroupOwner == false && generation == link.generation &&
                links.none { it.generation == link.generation }
            if (shouldReconnect) connectLoopGeneration = null
            Triple(wasIdentifiedPeer, shouldReconnect, links.any { it.generation == generation })
        }
        try { link.socket.close() } catch (_: Exception) {}
        if (shouldReconnect) scheduleClientReconnect(link.generation)
        if (wasIdentifiedPeer) {
            mainHandler.post {
                if (generation == link.generation && identifiedPeerId == null) {
                    onPeerIdentified(null)
                }
            }
        }
        if (links.none { it.peerNodeId != null }) {
            val stateAfterRemoval = when {
                currentState == "failed" -> "failed"
                hasPendingLink -> "connecting"
                else -> "disconnected"
            }
            emitState(stateAfterRemoval, if (stateAfterRemoval == "failed") lastError else null)
        }
    }

    /** Restarts the client loop after a failed handshake, socket close, or exhausted retry batch. */
    private fun scheduleClientReconnect(expectedGeneration: Int) {
        executor.execute {
            try {
                Thread.sleep(RECONNECT_DELAY_MS)
            } catch (_: InterruptedException) {
                return@execute
            }
            synchronized(this) {
                val address = activeAddress
                if (generation != expectedGeneration || activeGroupOwner != false ||
                    address.isNullOrBlank() || connectLoopGeneration == expectedGeneration ||
                    links.any { it.generation == expectedGeneration }
                ) return@synchronized
                connectLoopGeneration = expectedGeneration
                emitState("connecting")
                executor.execute { connectWithRetry(address, expectedGeneration) }
            }
        }
    }

    private fun scheduleListenerRestart(expectedGeneration: Int) {
        executor.execute {
            try {
                Thread.sleep(RECONNECT_DELAY_MS)
            } catch (_: InterruptedException) {
                return@execute
            }
            synchronized(this) {
                if (generation != expectedGeneration || activeGroupOwner != true ||
                    listenerLoopGeneration == expectedGeneration
                ) return@synchronized
                listenerLoopGeneration = expectedGeneration
                lastError = null
                emitState("connecting")
                executor.execute { listen(expectedGeneration) }
            }
        }
    }

    private fun emitState(state: String, error: String? = null) {
        val stateGeneration: Int
        synchronized(this) {
            if (state != "connected" && links.any { it.peerNodeId != null }) return
            currentState = state
            if (state == "failed") lastError = error
            stateGeneration = generation
        }
        mainHandler.post {
            if (generation == stateGeneration && currentState == state) {
                onStateChanged(state, error)
            }
        }
    }

    private fun isNodeId(value: String): Boolean = try {
        UUID.fromString(value).toString() == value.lowercase()
    } catch (_: IllegalArgumentException) {
        false
    }

    private data class Link(
        val socket: Socket,
        val reader: BufferedReader,
        val writer: BufferedWriter,
        val generation: Int,
        @Volatile var peerNodeId: String? = null,
    )

    private companion object {
        const val PORT = 8988
        const val CONNECT_TIMEOUT_MS = 5_000
        const val HELLO_TIMEOUT_MS = 5_000
        const val RECONNECT_DELAY_MS = 1_000L
        const val PROTOCOL_VERSION = 1
        val RETRY_DELAYS_MS = listOf(0L, 1_000L, 2_000L, 4_000L, 8_000L)
    }
}
