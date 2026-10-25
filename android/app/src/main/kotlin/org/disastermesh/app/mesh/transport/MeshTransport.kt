package org.disastermesh.app.mesh.transport

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.os.Handler
import android.os.Looper
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.io.IOException
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.UUID
import java.util.concurrent.ThreadLocalRandom
import org.disastermesh.core.logging.MeshLogger
import org.json.JSONObject

/** TCP transport used after a Wi-Fi Direct group has formed. */
class MeshTransport(
    context: Context,
    private val localNodeId: String,
    private val logger: MeshLogger,
    private val onStateChanged: (String, String?) -> Unit,
    private val onPeerIdentified: (String?) -> Unit,
    private val onEnvelopeReceived: (JSONObject) -> Unit,
) {
    private val connectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val executor: ExecutorService = Executors.newCachedThreadPool()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val links = CopyOnWriteArrayList<Link>()
    @Volatile private var serverSocket: ServerSocket? = null
    @Volatile private var activeGroupOwner: Boolean? = null
    @Volatile private var activeAddress: String? = null
    @Volatile private var generation = 0
    @Volatile private var connectLoopGeneration: Int? = null
    @Volatile private var reconnectTaskGeneration: Int? = null
    @Volatile private var clientReconnectAttempt = 0
    @Volatile private var listenerLoopGeneration: Int? = null
    @Volatile private var identifiedPeerId: String? = null
    @Volatile private var currentState: String = "disconnected"
    @Volatile private var lastError: String? = null
    @Volatile private var routeDiagnosticGeneration = -1

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
                logger.info("mesh_server_start_requested", mapOf("generation" to currentGeneration, "port" to PORT))
                emitState("connecting")
                executor.execute { listen(currentGeneration) }
            }
        } else if (!groupOwnerAddress.isNullOrBlank()) {
            if (connectLoopGeneration != currentGeneration &&
                links.none { it.generation == currentGeneration }
            ) {
                connectLoopGeneration = currentGeneration
                lastError = null
                logger.info(
                    "mesh_client_start_requested",
                    mapOf("generation" to currentGeneration, "groupOwnerAddress" to groupOwnerAddress),
                )
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
        reconnectTaskGeneration = null
        clientReconnectAttempt = 0
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
            logger.info("mesh_listener_started", mapOf("port" to PORT, "generation" to expectedGeneration))
            emitState("listening")
            while (!server.isClosed && generation == expectedGeneration) {
                val accepted = server.accept()
                logger.info(
                    "mesh_socket_accepted",
                    mapOf(
                        "generation" to expectedGeneration,
                        "remoteAddress" to accepted.inetAddress.hostAddress,
                        "localAddress" to accepted.localAddress.hostAddress,
                    ),
                )
                attach(accepted, expectedGeneration)
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
                val selection = findNetworkFor(address, expectedGeneration)
                    ?: throw IOException("No Android Wi-Fi Direct network route to group owner $address")
                val network = selection.network
                val socket = network.socketFactory.createSocket()
                socketToClose = socket
                socket.connect(InetSocketAddress(address, PORT), CONNECT_TIMEOUT_MS)
                if (generation != expectedGeneration) {
                    socket.close()
                    return
                }
                logger.info(
                    "mesh_socket_connected",
                    mapOf(
                        "generation" to expectedGeneration,
                        "groupOwnerAddress" to address,
                        "localAddress" to socket.localAddress.hostAddress,
                        "networkInterface" to selection.interfaceName,
                        "route" to selection.route,
                    ),
                )
                if (attach(socket, expectedGeneration)) return
                socketToClose = null
            } catch (error: Exception) {
                try { socketToClose?.close() } catch (_: Exception) {}
                socketToClose = null
                lastConnectError = error.message ?: error.javaClass.simpleName
                logger.warn(
                    "mesh_socket_connect_failed",
                    mapOf(
                        "attempt" to index + 1,
                        "generation" to expectedGeneration,
                        "groupOwnerAddress" to address,
                        "exceptionType" to error.javaClass.simpleName,
                        "error" to (error.message ?: "no detail"),
                    ),
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
            if (generation == link.generation && !link.socket.isClosed) {
                if (link.peerNodeId == null) {
                    val reason = error.message ?: error.javaClass.simpleName
                    lastError = reason
                    emitState("failed", reason)
                }
                logger.warn(
                    "mesh_socket_read_failed",
                    mapOf(
                        "generation" to link.generation,
                        "exceptionType" to error.javaClass.simpleName,
                        "error" to (error.message ?: "no detail"),
                    ),
                )
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
            reconnectTaskGeneration = null
        }
        logger.info(
            "mesh_peer_identity_verified",
            mapOf("generation" to link.generation, "peerNodeId" to nodeId),
        )
        link.socket.soTimeout = 0
        mainHandler.postDelayed({
            synchronized(this) {
                if (generation == link.generation && links.contains(link) &&
                    identifiedPeerId == nodeId
                ) {
                    clientReconnectAttempt = 0
                }
            }
        }, STABLE_CONNECTION_RESET_MS)
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
            logger.warn(
                "mesh_send_failed",
                mapOf(
                    "generation" to link.generation,
                    "exceptionType" to error.javaClass.simpleName,
                    "error" to (error.message ?: "no detail"),
                ),
            )
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
        val attempt = synchronized(this) {
            if (generation != expectedGeneration || activeGroupOwner != false ||
                reconnectTaskGeneration == expectedGeneration ||
                connectLoopGeneration == expectedGeneration ||
                links.any { it.generation == expectedGeneration }
            ) return
            reconnectTaskGeneration = expectedGeneration
            clientReconnectAttempt++
            clientReconnectAttempt - 1
        }
        val exponentialDelay = (CLIENT_RECONNECT_BASE_DELAY_MS *
            (1 shl attempt.coerceAtMost(10)).toLong())
            .coerceAtMost(CLIENT_RECONNECT_MAX_DELAY_MS)
        val jitterBound = (exponentialDelay / 4).coerceAtLeast(1L)
        val delay = (exponentialDelay + ThreadLocalRandom.current().nextLong(jitterBound))
            .coerceAtMost(CLIENT_RECONNECT_MAX_DELAY_MS)
        logger.warn(
            "mesh_client_reconnect_scheduled",
            mapOf("generation" to expectedGeneration, "attempt" to (attempt + 1), "delayMs" to delay),
        )
        executor.execute {
            try {
                Thread.sleep(delay)
            } catch (_: InterruptedException) {
                synchronized(this) {
                    if (reconnectTaskGeneration == expectedGeneration) reconnectTaskGeneration = null
                }
                return@execute
            }
            synchronized(this) {
                if (reconnectTaskGeneration == expectedGeneration) reconnectTaskGeneration = null
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

    /** Finds the Android network whose specific route reaches the group owner. */
    private fun findNetworkFor(address: String, expectedGeneration: Int): NetworkSelection? {
        val destination = try {
            InetAddress.getByName(address)
        } catch (_: Exception) {
            return null
        }
        val candidates = connectivityManager.allNetworks
            .mapNotNull { network ->
                val properties = connectivityManager.getLinkProperties(network) ?: return@mapNotNull null
                val matchingRoute = properties.routes
                    .filter { it.destination.prefixLength > 0 && it.matches(destination) }
                    .maxByOrNull { it.destination.prefixLength }
                val matchingAddress = if (matchingRoute == null) {
                    properties.linkAddresses.firstOrNull { linkAddress ->
                        sameSubnet(destination, linkAddress.address, linkAddress.prefixLength)
                    }
                } else {
                    null
                }
                val prefix = matchingRoute?.destination?.prefixLength
                    ?: matchingAddress?.prefixLength
                    ?: return@mapNotNull null
                NetworkSelection(
                    network = network,
                    interfaceName = properties.interfaceName ?: "unknown",
                    route = matchingRoute?.destination?.toString()
                        ?: "interface-address/${matchingAddress?.toString()}",
                    prefixLength = prefix,
                    localAddresses = properties.linkAddresses.map { it.toString() },
                )
            }
        val selected = candidates.maxByOrNull { it.prefixLength }
        if (selected == null) {
            if (routeDiagnosticGeneration != expectedGeneration) {
                routeDiagnosticGeneration = expectedGeneration
                val available = connectivityManager.allNetworks.mapNotNull { network ->
                    connectivityManager.getLinkProperties(network)?.let { properties ->
                        mapOf(
                            "interface" to properties.interfaceName,
                            "addresses" to properties.linkAddresses.map { it.toString() },
                            "routes" to properties.routes.map { it.destination.toString() },
                        )
                    }
                }
                logger.error(
                    "mesh_p2p_route_not_found",
                    details = mapOf(
                        "generation" to expectedGeneration,
                        "destination" to address,
                        "availableNetworks" to available,
                    ),
                )
            }
            return null
        }
        logger.info(
            "mesh_p2p_route_selected",
            mapOf(
                "generation" to expectedGeneration,
                "destination" to address,
                "interface" to selected.interfaceName,
                "route" to selected.route,
                "localAddresses" to selected.localAddresses,
            ),
        )
        return selected
    }

    private fun sameSubnet(first: InetAddress, second: InetAddress, prefixLength: Int): Boolean {
        val firstBytes = first.address
        val secondBytes = second.address
        if (firstBytes.size != secondBytes.size || prefixLength !in 1..(firstBytes.size * 8)) return false
        var remainingBits = prefixLength
        for (index in firstBytes.indices) {
            val bitsHere = remainingBits.coerceIn(0, 8)
            val mask = if (bitsHere == 0) 0 else (0xff shl (8 - bitsHere)) and 0xff
            if ((firstBytes[index].toInt() and 0xff and mask) !=
                (secondBytes[index].toInt() and 0xff and mask)
            ) return false
            remainingBits -= bitsHere
        }
        return true
    }

    private data class NetworkSelection(
        val network: Network,
        val interfaceName: String,
        val route: String,
        val prefixLength: Int,
        val localAddresses: List<String>,
    )

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
        const val CLIENT_RECONNECT_BASE_DELAY_MS = 1_000L
        const val CLIENT_RECONNECT_MAX_DELAY_MS = 30_000L
        const val STABLE_CONNECTION_RESET_MS = 15_000L
        const val PROTOCOL_VERSION = 1
        val RETRY_DELAYS_MS = listOf(0L, 1_000L, 2_000L, 4_000L, 8_000L)
    }
}
