package org.disastermesh.app.mesh.service

import org.disastermesh.core.logging.MeshLogger

/** Lifecycle boundary for the future foreground mesh service. */
class MeshServiceController(private val logger: MeshLogger) {
    fun start() = logger.debug("mesh_service_start_requested")

    fun stop() = logger.debug("mesh_service_stop_requested")
}
