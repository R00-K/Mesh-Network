package org.disastermesh.core.logging

interface MeshLogger {
    fun debug(event: String, details: Map<String, Any?> = emptyMap())
    fun info(event: String, details: Map<String, Any?> = emptyMap())
    fun warn(event: String, details: Map<String, Any?> = emptyMap())
    fun error(event: String, throwable: Throwable? = null, details: Map<String, Any?> = emptyMap())
}

object NoOpMeshLogger : MeshLogger {
    override fun debug(event: String, details: Map<String, Any?>) = Unit
    override fun info(event: String, details: Map<String, Any?>) = Unit
    override fun warn(event: String, details: Map<String, Any?>) = Unit
    override fun error(event: String, throwable: Throwable?, details: Map<String, Any?>) = Unit
}
