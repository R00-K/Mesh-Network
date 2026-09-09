package org.disastermesh.core.model

data class Message(
    val id: String,
    val source: NodeId,
    val destination: NodeId,
    val payload: ByteArray,
    val ttlHops: Int,
    val createdAtEpochMs: Long,
    val path: List<NodeId> = emptyList(),
) {
    init {
        require(id.isNotBlank()) { "Message ID must not be blank" }
        require(ttlHops > 0) { "Message TTL must be positive" }
        require(createdAtEpochMs >= 0) { "Message timestamp must not be negative" }
    }

    override fun equals(other: Any?): Boolean =
        other is Message &&
            id == other.id &&
            source == other.source &&
            destination == other.destination &&
            payload.contentEquals(other.payload) &&
            ttlHops == other.ttlHops &&
            createdAtEpochMs == other.createdAtEpochMs &&
            path == other.path

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + source.hashCode()
        result = 31 * result + destination.hashCode()
        result = 31 * result + payload.contentHashCode()
        result = 31 * result + ttlHops
        result = 31 * result + createdAtEpochMs.hashCode()
        result = 31 * result + path.hashCode()
        return result
    }
}
