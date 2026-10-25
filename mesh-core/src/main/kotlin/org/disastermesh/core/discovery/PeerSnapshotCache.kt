package org.disastermesh.core.discovery

/** Short-lived cache for real Wi-Fi Direct peer snapshots across empty broadcasts. */
class PeerSnapshotCache<T>(private val ttlMs: Long) {
    init {
        require(ttlMs > 0)
    }

    data class Snapshot<T>(val id: String, val value: T, val lastSeenAtMs: Long)

    private val entries = linkedMapOf<String, Snapshot<T>>()

    fun update(
        peers: Collection<Pair<String, T>>,
        nowMs: Long,
        retainedIds: Set<String> = emptySet(),
    ): List<Snapshot<T>> {
        peers.forEach { (id, value) ->
            if (id.isNotBlank()) entries[id] = Snapshot(id, value, nowMs)
        }
        entries.entries.removeAll { (id, snapshot) ->
            id !in retainedIds && nowMs - snapshot.lastSeenAtMs > ttlMs
        }
        return entries.values.toList()
    }

    fun lastSeenAt(id: String): Long? = entries[id]?.lastSeenAtMs

    fun isFresh(id: String, nowMs: Long, freshnessMs: Long): Boolean =
        entries[id]?.let { nowMs - it.lastSeenAtMs <= freshnessMs } == true

    fun clear() = entries.clear()
}
