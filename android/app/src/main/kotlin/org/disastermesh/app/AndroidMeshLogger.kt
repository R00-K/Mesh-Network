package org.disastermesh.app

import android.util.Log
import org.disastermesh.core.logging.MeshLogger

/** Android logger for native discovery, routing, and channel events. */
class AndroidMeshLogger(
    private val tag: String = "DisasterMesh",
) : MeshLogger {
    override fun debug(event: String, details: Map<String, Any?>) {
        Log.d(tag, format(event, details))
    }

    override fun info(event: String, details: Map<String, Any?>) {
        Log.i(tag, format(event, details))
    }

    override fun warn(event: String, details: Map<String, Any?>) {
        Log.w(tag, format(event, details))
    }

    override fun error(
        event: String,
        throwable: Throwable?,
        details: Map<String, Any?>,
    ) {
        Log.e(tag, format(event, details), throwable)
    }

    private fun format(event: String, details: Map<String, Any?>): String =
        if (details.isEmpty()) event else "$event $details"
}
