package org.disastermesh.app.mesh.routing

import org.disastermesh.core.logging.MeshLogger

/** Tracks the currently usable direct route and its recovery state. */
class RouteCoordinator(
    private val logger: MeshLogger,
    private val onChanged: (RouteSnapshot) -> Unit,
) {
    var snapshot: RouteSnapshot = RouteSnapshot(status = "no_route")
        private set

    fun connectionChanged(state: String, peerId: String?) {
        snapshot = when (state) {
            "connecting" -> RouteSnapshot("recovering", peerId, reason = "connecting")
            "connected" -> RouteSnapshot("active", peerId, hops = listOfNotNull(peerId))
            "failed", "disconnected" -> RouteSnapshot("recovering", peerId, reason = state)
            else -> snapshot
        }
        logger.debug("route_state_changed", mapOf("state" to snapshot.status))
        onChanged(snapshot)
    }

    fun transportChanged(state: String, peerId: String?) {
        snapshot = when (state) {
            "connected" -> RouteSnapshot("active", peerId, hops = listOfNotNull(peerId))
            "disconnected", "failed" -> RouteSnapshot("recovering", peerId, reason = state)
            else -> snapshot
        }
        onChanged(snapshot)
    }

    fun noRoute(reason: String) {
        snapshot = RouteSnapshot("no_route", reason = reason)
        onChanged(snapshot)
    }
}

data class RouteSnapshot(
    val status: String,
    val destination: String? = null,
    val hops: List<String> = emptyList(),
    val reason: String? = null,
)
