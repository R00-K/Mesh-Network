package org.disastermesh.app.mesh.transport

import android.net.wifi.p2p.WifiP2pInfo
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/** TCP links for one Wi-Fi Direct group; the group owner relays broadcasts to its clients. */
class MeshTransport(
    private val localNodeId: String,
    private val onMessage: (String, Map<String, Any?>) -> Unit,
    private val onDelivery: (String, String) -> Unit,
    private val onConnection: (Boolean, String?) -> Unit,
) {
    private val io: ExecutorService = Executors.newCachedThreadPool { task ->
        Thread(task, "mesh-transport").apply { isDaemon = true }
    }
    private val peers = ConcurrentHashMap<String, PeerConnection>()
    private val ids = AtomicInteger()
    @Volatile private var server: ServerSocket? = null
    @Volatile private var client: PeerConnection? = null
    @Volatile private var groupOwner = false
    @Volatile private var groupActive = false

    fun start(info: WifiP2pInfo) {
        if (info.groupFormed && groupActive && groupOwner == info.isGroupOwner) return
        groupActive = false
        closeLinks()
        if (!info.groupFormed) return
        groupActive = true
        groupOwner = info.isGroupOwner
        if (groupOwner) {
            io.execute {
                try {
                    val listener = ServerSocket(PORT).also { server = it }
                    while (!listener.isClosed) {
                        val socket = listener.accept()
                        attachOwnerClient(socket)
                    }
                } catch (error: Exception) {
                    if (groupActive) onConnection(false, error.message ?: "Wi-Fi Direct listener stopped")
                }
            }
        } else {
            val host = info.groupOwnerAddress?.hostAddress
            if (host.isNullOrBlank()) {
                onConnection(false, "Wi-Fi Direct did not provide the group owner address")
                return
            }
            io.execute {
                var lastError: Exception? = null
                repeat(CONNECT_ATTEMPTS) { attempt ->
                    val socket = Socket()
                    try {
                        socket.connect(InetSocketAddress(host, PORT), CONNECT_TIMEOUT_MS)
                        attachClient(socket)
                        return@execute
                    } catch (error: Exception) {
                        lastError = error
                        try { socket.close() } catch (_: Exception) { }
                        if (attempt < CONNECT_ATTEMPTS - 1) Thread.sleep(RETRY_DELAY_MS)
                    }
                }
                onConnection(false, lastError?.message ?: "Could not connect to the group owner")
            }
        }
    }

    /** Originates a broadcast. A group owner sends to every attached client. */
    fun send(id: String, text: String, sentAt: Long, ttl: Int, path: List<String>): Boolean {
        if (!isConnected() || text.isBlank()) return false
        val frame = mapOf<String, Any?>(
            "type" to "message",
            "id" to id,
            "text" to text,
            "source" to localNodeId,
            "destination" to "broadcast",
            "sentAt" to sentAt,
            "ttl" to ttl,
            "path" to path,
        )
        return sendFrame(frame)
    }

    /** Forwards an already deduplicated frame, omitting the link it arrived on. */
    fun forward(frame: Map<String, Any?>, incomingPeer: String): Boolean {
        return if (groupOwner) broadcast(frame, incomingPeer) else sendFrame(frame)
    }

    fun acknowledge(id: String, peer: String): Boolean {
        val ack = mapOf<String, Any?>("type" to "ack", "id" to id)
        return if (groupOwner) sendToClient(peer, ack) else sendFrame(ack)
    }

    fun isConnected(): Boolean = if (groupOwner) peers.isNotEmpty() else client?.socket?.isConnected == true && client?.socket?.isClosed == false

    fun isGroupOwner(): Boolean = groupOwner

    fun connectedPeerCount(): Int = if (groupOwner) peers.size else if (isConnected()) 1 else 0

    fun close() {
        groupActive = false
        closeLinks()
    }

    private fun attachOwnerClient(socket: Socket) {
        val peerId = "client-${ids.incrementAndGet()}"
        val connection = PeerConnection(peerId, socket)
        peers[peerId] = connection
        onConnection(true, null)
        readLoop(connection)
    }

    private fun attachClient(socket: Socket) {
        val connection = PeerConnection(GROUP_OWNER_PEER, socket)
        client = connection
        onConnection(true, null)
        readLoop(connection)
    }

    private fun readLoop(connection: PeerConnection) {
        io.execute {
            try {
                val reader = BufferedReader(InputStreamReader(connection.socket.getInputStream(), Charsets.UTF_8))
                while (groupActive && !connection.socket.isClosed) {
                    val line = reader.readLine() ?: break
                    val frame = try { JSONObject(line).toMap() } catch (_: Exception) { continue }
                    when (frame["type"] as? String) {
                        "message" -> onMessage(connection.id, frame)
                        "ack" -> (frame["id"] as? String)?.let { onDelivery(connection.id, it) }
                    }
                }
            } catch (error: Exception) {
                if (groupActive) onConnection(false, error.message ?: "Peer connection interrupted")
            } finally {
                connection.close()
                if (groupOwner) {
                    peers.remove(connection.id)
                    onConnection(peers.isNotEmpty(), if (peers.isEmpty()) "No Wi-Fi Direct peers remain" else null)
                } else {
                    if (client === connection) client = null
                    onConnection(false, "Group owner disconnected")
                }
            }
        }
    }

    private fun sendFrame(frame: Map<String, Any?>): Boolean {
        if (groupOwner) {
            val recipients = peers.values.toList()
            if (recipients.isEmpty()) return false
            return recipients.fold(false) { sent, connection -> write(connection, frame) || sent }
        }
        val connection = client ?: return false
        return write(connection, frame)
    }

    private fun broadcast(frame: Map<String, Any?>, excludePeer: String): Boolean {
        return peers.values.filter { it.id != excludePeer }
            .fold(false) { sent, connection -> write(connection, frame) || sent }
    }

    private fun sendToClient(peer: String, frame: Map<String, Any?>): Boolean =
        peers[peer]?.let { write(it, frame) } ?: false

    private fun write(connection: PeerConnection, frame: Map<String, Any?>): Boolean = try {
        synchronized(connection.writer) {
            connection.writer.write(JSONObject(frame).toString())
            connection.writer.newLine()
            connection.writer.flush()
        }
        true
    } catch (error: Exception) {
        onConnection(false, error.message ?: "Socket write failed")
        false
    }

    private fun closeLinks() {
        try { server?.close() } catch (_: Exception) { }
        server = null
        peers.values.forEach { it.close() }
        peers.clear()
        client?.close()
        client = null
    }

    private class PeerConnection(val id: String, val socket: Socket) {
        val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8))
        fun close() { try { socket.close() } catch (_: Exception) { } }
    }

    private fun JSONObject.toMap(): Map<String, Any?> {
        val result = mutableMapOf<String, Any?>()
        val keys = keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = get(key)
            result[key] = if (value is JSONArray) {
                buildList(value.length()) { for (index in 0 until value.length()) add(value.optString(index)) }
            } else if (value == JSONObject.NULL) null else value
        }
        return result
    }

    private companion object {
        const val PORT = 8988
        const val CONNECT_ATTEMPTS = 12
        const val CONNECT_TIMEOUT_MS = 1_500
        const val RETRY_DELAY_MS = 400L
        const val GROUP_OWNER_PEER = "group-owner"
    }
}
