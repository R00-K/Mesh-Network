package org.disastermesh.app.mesh.channels

import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import org.disastermesh.core.logging.MeshLogger

/** Initial Flutter bridge for the native mesh service boundary. */
class MeshChannelHandler(
    private val logger: MeshLogger,
) : MethodChannel.MethodCallHandler, EventChannel.StreamHandler {
    private var eventSink: EventChannel.EventSink? = null
    private var running = false

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "startMesh" -> {
                running = true
                logger.info("mesh_started")
                emit("MeshStatusChanged", mapOf("running" to true))
                result.success(null)
            }
            "stopMesh" -> {
                running = false
                logger.info("mesh_stopped")
                emit("MeshStatusChanged", mapOf("running" to false))
                result.success(null)
            }
            "discoverPeers" -> {
                logger.info("peer_discovery_requested")
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
            "getPeers" -> result.success(emptyList<Map<String, Any?>>())
            "getMetrics" -> result.success(emptyMap<String, Any?>())
            "getRoute" -> result.success(null)
            else -> result.notImplemented()
        }
    }

    override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
        eventSink = events
        emit("MeshStatusChanged", mapOf("running" to running))
    }

    override fun onCancel(arguments: Any?) {
        eventSink = null
    }

    private fun emit(type: String, payload: Map<String, Any?>) {
        eventSink?.success(mapOf("type" to type, "payload" to payload))
    }
}
