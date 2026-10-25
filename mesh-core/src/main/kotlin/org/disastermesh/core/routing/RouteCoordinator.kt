package org.disastermesh.core.routing

import org.disastermesh.core.logging.MeshLogger

/** Tracks a direct route; only a verified application transport is active. */
class RouteCoordinator(
    private val logger: MeshLogger,
    private val onChanged: (RouteSnapshot) -> Unit,
) {
    var snapshot: RouteSnapshot = RouteSnapshot(status = "no_route")
        private set

    fun connectionChanged(state: String, peerId: String?) {
        val next = when (state) {
            "connecting" -> RouteSnapshot("connecting", peerId, reason = "wifi_direct_negotiation")
            "connected" -> RouteSnapshot("verifying", peerId, reason = "transport_handshake")
            "failed" -> RouteSnapshot("failed", peerId, reason = "wifi_direct_failed")
            "disconnected" -> RouteSnapshot("no_route", reason = "disconnected")
            else -> snapshot
        }
        publish(next, "connection_$state")
    }

    fun transportChanged(state: String, peerId: String?) {
        val next = when (state) {
            "connected" -> RouteSnapshot("active", peerId, hops = listOfNotNull(peerId))
            "connecting", "listening" -> RouteSnapshot("verifying", peerId, reason = "transport_$state")
            "disconnected", "failed" -> RouteSnapshot("recovering", peerId, reason = "transport_$state")
            else -> snapshot
        }
        publish(next, "transport_$state")
    }

    fun noRoute(reason: String) {
        publish(RouteSnapshot("no_route", reason = reason), reason)
    }

    private fun publish(next: RouteSnapshot, cause: String) {
        if (next == snapshot) return
        snapshot = next
        logger.debug(
            "route_state_changed",
            mapOf("state" to snapshot.status, "reason" to cause, "destination" to snapshot.destination),
        )
        onChanged(snapshot)
    }
}

data class RouteSnapshot(
    val status: String,
    val destination: String? = null,
    val hops: List<String> = emptyList(),
    val reason: String? = null,
)
