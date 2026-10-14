package org.disastermesh.core.model

data class LinkMetrics(
    val batteryPercent: Double,
    val availableBandwidthKbps: Double,
    val signalDbm: Double,
    val latencyMs: Double,
    val packetLossFraction: Double,
    val loadFraction: Double,
    val stabilityFraction: Double,
    val availabilityFraction: Double,
) {
    init {
        require(batteryPercent in 0.0..100.0) { "Battery must be between 0 and 100" }
        require(availableBandwidthKbps >= 0.0) { "Bandwidth must not be negative" }
        require(latencyMs >= 0.0) { "Latency must not be negative" }
        require(packetLossFraction in 0.0..1.0) { "Packet loss must be between 0 and 1" }
        require(loadFraction in 0.0..1.0) { "Load must be between 0 and 1" }
        require(stabilityFraction in 0.0..1.0) { "Stability must be between 0 and 1" }
        require(availabilityFraction in 0.0..1.0) { "Availability must be between 0 and 1" }
    }
}
