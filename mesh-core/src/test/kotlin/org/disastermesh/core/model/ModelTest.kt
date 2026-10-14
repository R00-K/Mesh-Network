package org.disastermesh.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ModelTest {
    @Test
    fun nodeIdsAreValueObjects() {
        assertEquals(NodeId("a"), NodeId("a"))
        assertEquals("a", NodeId("a").toString())
        assertFailsWith<IllegalArgumentException> { NodeId(" ") }
    }

    @Test
    fun linkMetricsValidateRanges() {
        val metrics = LinkMetrics(
            batteryPercent = 75.0,
            availableBandwidthKbps = 1_000.0,
            signalDbm = -55.0,
            latencyMs = 20.0,
            packetLossFraction = 0.01,
            loadFraction = 0.25,
            stabilityFraction = 0.95,
            availabilityFraction = 1.0,
        )

        assertEquals(75.0, metrics.batteryPercent)
        assertFailsWith<IllegalArgumentException> {
            metrics.copy(packetLossFraction = 1.1)
        }
    }

    @Test
    fun routeReportsHopCount() {
        val route = Route(
            nodes = listOf(NodeId("a"), NodeId("b"), NodeId("c")),
            totalCost = 1.5,
        )

        assertEquals(2, route.hops)
        assertTrue(route.totalCost >= 0.0)
    }

    @Test
    fun messagesCompareBytePayloadsByContent() {
        val first = Message(
            id = "message-1",
            source = NodeId("a"),
            destination = NodeId("b"),
            payload = "hello".toByteArray(),
            ttlHops = 5,
            createdAtEpochMs = 1_000,
        )
        val second = first.copy(payload = "hello".toByteArray())

        assertEquals(first, second)
        assertFailsWith<IllegalArgumentException> { first.copy(ttlHops = 0) }
    }

    @Test
    fun disconnectedNeighborCanStillBeRepresentedForExpiryHandling() {
        val neighbor = Neighbor(
            node = Node(NodeId("b")),
            status = ConnectionStatus.DISCONNECTED,
            metrics = null,
            lastSeenEpochMs = 2_000,
        )

        assertEquals(ConnectionStatus.DISCONNECTED, neighbor.status)
        assertEquals(null, neighbor.metrics)
    }
}
