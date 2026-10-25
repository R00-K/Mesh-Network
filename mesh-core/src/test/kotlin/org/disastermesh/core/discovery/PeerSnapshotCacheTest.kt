package org.disastermesh.core.discovery

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PeerSnapshotCacheTest {
    @Test
    fun emptyPeerUpdateRetainsRecentlyDiscoveredPeerWithOriginalLastSeenTime() {
        val cache = PeerSnapshotCache<String>(300_000)
        cache.update(listOf("peer-a" to "Oppo"), nowMs = 1_000)

        val snapshot = cache.update(emptyList(), nowMs = 20_000)

        assertEquals(listOf("peer-a"), snapshot.map { it.id })
        assertEquals(1_000L, snapshot.single().lastSeenAtMs)
        assertFalse(cache.isFresh("peer-a", nowMs = 20_000, freshnessMs = 15_000))
    }

    @Test
    fun currentFailedAttemptRemainsCachedBeyondNormalTtlUntilUnpinned() {
        val cache = PeerSnapshotCache<String>(10_000)
        cache.update(listOf("peer-a" to "Oppo", "peer-b" to "Realme"), nowMs = 0)

        val whileAttemptIsCurrent = cache.update(
            emptyList(),
            nowMs = 20_000,
            retainedIds = setOf("peer-a"),
        )
        assertEquals(listOf("peer-a"), whileAttemptIsCurrent.map { it.id })

        val afterAttemptEnds = cache.update(emptyList(), nowMs = 20_001)
        assertTrue(afterAttemptEnds.isEmpty())
        assertNull(cache.lastSeenAt("peer-a"))
    }

    @Test
    fun rediscoveryRefreshesTheSnapshotAndFreshness() {
        val cache = PeerSnapshotCache<String>(10_000)
        cache.update(listOf("peer-a" to "old name"), nowMs = 0)
        val snapshot = cache.update(listOf("peer-a" to "updated name"), nowMs = 5_000)

        assertEquals("updated name", snapshot.single().value)
        assertEquals(5_000L, cache.lastSeenAt("peer-a"))
        assertTrue(cache.isFresh("peer-a", nowMs = 10_000, freshnessMs = 10_000))
    }
}
