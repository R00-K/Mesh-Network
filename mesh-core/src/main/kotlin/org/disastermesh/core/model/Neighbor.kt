package org.disastermesh.core.model

enum class ConnectionStatus {
    CONNECTED,
    DISCONNECTED,
    UNKNOWN,
}

data class Neighbor(
    val node: Node,
    val status: ConnectionStatus,
    val metrics: LinkMetrics?,
    val lastSeenEpochMs: Long,
) {
    init {
        require(lastSeenEpochMs >= 0) { "Last-seen timestamp must not be negative" }
    }
}
