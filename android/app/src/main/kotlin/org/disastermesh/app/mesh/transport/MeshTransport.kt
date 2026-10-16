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
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.CopyOnWriteArrayList
import org.json.JSONObject
import org.disastermesh.core.logging.MeshLogger

/** TCP transport used after a Wi-Fi Direct group has formed. */
class MeshTransport(
    private val logger: MeshLogger,
    private val onStateChanged: (String) -> Unit,
    private val onEnvelopeReceived: (JSONObject) -> Unit,
) {
    private val executor: ExecutorService = Executors.newCachedThreadPool()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var serverSocket: ServerSocket? = null
    private val links = CopyOnWriteArrayList<Link>()

    val connectedLinkCount: Int
        get() = links.size

    @Synchronized
    fun start(isGroupOwner: Boolean, groupOwnerAddress: String?) {
        stop()
        if (isGroupOwner) {
            executor.execute { listen() }
        } else if (!groupOwnerAddress.isNullOrBlank()) {
            executor.execute { connect(groupOwnerAddress) }
        } else {
            emitState("failed")
        }
    }

    @Synchronized
    fun send(envelope: JSONObject): Boolean {
        if (links.isEmpty()) return false
        var sent = false
        links.toList().forEach { link ->
            try {
                synchronized(link.writer) {
                    link.writer.write(envelope.toString())
                    link.writer.newLine()
                    link.writer.flush()
                }
                sent = true
            } catch (error: Exception) {
                logger.warn("mesh_send_failed", mapOf("error" to (error.message ?: "unknown")))
                removeLink(link)
            }
        }
        return sent
    }

    @Synchronized
    fun stop() {
        try {
            links.forEach { link ->
                link.writer.close()
                link.socket.close()
            }
            serverSocket?.close()
        } catch (_: Exception) {
            // Closing an already closed transport is safe.
        } finally {
            links.clear()
            serverSocket = null
        }
    }

    private fun listen() {
        try {
            val server = ServerSocket(PORT)
            synchronized(this) { serverSocket = server }
            emitState("listening")
            while (!server.isClosed) {
                attach(server.accept())
            }
        } catch (error: Exception) {
            logger.warn("mesh_listener_failed", mapOf("error" to (error.message ?: "unknown")))
            emitState("failed")
        }
    }

    private fun connect(address: String) {
        try {
            val connected = Socket()
            connected.connect(InetSocketAddress(address, PORT), CONNECT_TIMEOUT_MS)
            attach(connected)
        } catch (error: Exception) {
            logger.warn("mesh_socket_connect_failed", mapOf("error" to (error.message ?: "unknown")))
            emitState("failed")
        }
    }

    private fun attach(connected: Socket) {
        val link = Link(
            socket = connected,
            writer = BufferedWriter(OutputStreamWriter(connected.getOutputStream(), Charsets.UTF_8)),
        )
        links += link
        emitState("connected")
        executor.execute {
            try {
                val reader = BufferedReader(InputStreamReader(connected.getInputStream(), Charsets.UTF_8))
                while (true) {
                    val line = reader.readLine() ?: break
                    onEnvelopeReceived(JSONObject(line))
                }
            } catch (error: Exception) {
                logger.warn("mesh_socket_read_failed", mapOf("error" to (error.message ?: "unknown")))
            } finally {
                removeLink(link)
            }
        }
    }

    private fun removeLink(link: Link) {
        if (links.remove(link)) {
            try {
                link.writer.close()
                link.socket.close()
            } catch (_: Exception) {
                // The peer may already have closed the socket.
            }
            if (links.isEmpty()) emitState("disconnected")
        }
    }

    private fun emitState(state: String) {
        mainHandler.post { onStateChanged(state) }
    }

    private companion object {
        const val PORT = 8988
        const val CONNECT_TIMEOUT_MS = 5_000
    }

    private data class Link(
        val socket: Socket,
        val writer: BufferedWriter,
    )
}
