package org.disastermesh.core.routing

import kotlin.test.Test
import kotlin.test.assertEquals

class RouteCoordinatorTest {
    @Test
    fun formedGroupIsNotAnActiveRouteUntilTransportIdentitySucceeds() {
        val coordinator = coordinator()

        coordinator.connectionChanged("connecting", "peer-address")
        assertEquals("connecting", coordinator.snapshot.status)
        coordinator.connectionChanged("connected", "peer-address")
        assertEquals("verifying", coordinator.snapshot.status)

        coordinator.transportChanged("connected", "peer-address")
        assertEquals("active", coordinator.snapshot.status)
        assertEquals(listOf("peer-address"), coordinator.snapshot.hops)
    }

    @Test
    fun duplicateHealthyEventsAreIdempotentAndDisconnectClearsRoute() {
        val coordinator = coordinator()
        coordinator.connectionChanged("connected", "peer-address")
        coordinator.transportChanged("connected", "peer-address")
        repeat(4) {
            coordinator.connectionChanged("connected", "peer-address")
            coordinator.transportChanged("connected", "peer-address")
        }
        assertEquals("active", coordinator.snapshot.status)

        coordinator.connectionChanged("disconnected", "peer-address")
        assertEquals("no_route", coordinator.snapshot.status)
        assertEquals(null, coordinator.snapshot.destination)
    }

    @Test
    fun failedTransportEntersRecoveryUntilRouteIsExplicitlyCleared() {
        val coordinator = coordinator()
        coordinator.connectionChanged("connected", "peer-address")
        coordinator.transportChanged("failed", "peer-address")

        assertEquals("recovering", coordinator.snapshot.status)
        coordinator.noRoute("mesh_stopped")
        assertEquals("no_route", coordinator.snapshot.status)
        assertEquals("mesh_stopped", coordinator.snapshot.reason)
    }

    private fun coordinator() = RouteCoordinator(
        logger = org.disastermesh.core.logging.NoOpMeshLogger,
        onChanged = {},
    )
}
