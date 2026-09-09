package org.disastermesh.core.config

import org.disastermesh.core.model.NodeId

data class MeshConfiguration(
    val localNodeId: NodeId,
    val loggingEnabled: Boolean = true,
    val neighborExpiryMs: Long = 30_000,
    val messageExpiryMs: Long = 24 * 60 * 60 * 1_000,
) {
    init {
        require(neighborExpiryMs > 0) { "Neighbor expiry must be positive" }
        require(messageExpiryMs > 0) { "Message expiry must be positive" }
    }
}
