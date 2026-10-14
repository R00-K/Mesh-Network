package org.disastermesh.core.model

@JvmInline
value class NodeId(val value: String) {
    init {
        require(value.isNotBlank()) { "Node ID must not be blank" }
    }

    override fun toString(): String = value
}
