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
    private var socket: Socket? = null
    private var writer: BufferedWriter? = null

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
        val currentWriter = writer ?: return false
        return try {
            currentWriter.write(envelope.toString())
            currentWriter.newLine()
            currentWriter.flush()
            true
        } catch (error: Exception) {
            logger.warn("mesh_send_failed", mapOf("error" to (error.message ?: "unknown")))
            emitState("disconnected")
            false
        }
    }

    @Synchronized
    fun stop() {
        try {
            writer?.close()
            socket?.close()
            serverSocket?.close()
        } catch (_: Exception) {
            // Closing an already closed transport is safe.
        } finally {
            writer = null
            socket = null
            serverSocket = null
        }
    }

    private fun listen() {
        try {
            val server = ServerSocket(PORT)
            synchronized(this) { serverSocket = server }
            emitState("listening")
            val accepted = server.accept()
            attach(accepted)
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
        synchronized(this) {
            socket = connected
            writer = BufferedWriter(OutputStreamWriter(connected.getOutputStream(), Charsets.UTF_8))
        }
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
                synchronized(this) {
                    if (socket === connected) {
                        writer = null
                        socket = null
                    }
                }
                emitState("disconnected")
            }
        }
    }

    private fun emitState(state: String) {
        mainHandler.post { onStateChanged(state) }
    }

    private companion object {
        const val PORT = 8988
        const val CONNECT_TIMEOUT_MS = 5_000
    }
}
