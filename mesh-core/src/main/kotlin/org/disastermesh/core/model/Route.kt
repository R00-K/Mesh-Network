package org.disastermesh.core.model

data class Route(
    val nodes: List<NodeId>,
    val totalCost: Double,
) {
    init {
        require(nodes.isNotEmpty()) { "A route must contain at least one node" }
        require(totalCost.isFinite() && totalCost >= 0.0) { "Route cost must be finite and non-negative" }
    }

    val hops: Int
        get() = (nodes.size - 1).coerceAtLeast(0)
}
