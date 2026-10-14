package org.disastermesh.core.model

data class Link(
    val from: NodeId,
    val to: NodeId,
    val metrics: LinkMetrics,
    val observedAtEpochMs: Long,
    val status: ConnectionStatus = ConnectionStatus.CONNECTED,
) {
    init {
        require(from != to) { "A link must connect two different nodes" }
        require(observedAtEpochMs >= 0) { "Observation timestamp must not be negative" }
    }
}
