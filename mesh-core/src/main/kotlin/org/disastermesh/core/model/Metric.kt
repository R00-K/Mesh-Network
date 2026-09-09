package org.disastermesh.core.model

enum class MetricType {
    BATTERY_PERCENT,
    AVAILABLE_BANDWIDTH_KBPS,
    SIGNAL_DBM,
    LATENCY_MS,
    PACKET_LOSS_FRACTION,
    LOAD_FRACTION,
    STABILITY_FRACTION,
    AVAILABILITY_FRACTION,
}

data class Metric(
    val type: MetricType,
    val value: Double,
    val measuredAtEpochMs: Long,
) {
    init {
        require(value.isFinite()) { "Metric value must be finite" }
        require(measuredAtEpochMs >= 0) { "Metric timestamp must not be negative" }
    }
}
