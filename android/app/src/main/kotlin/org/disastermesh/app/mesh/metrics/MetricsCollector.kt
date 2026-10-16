package org.disastermesh.app.mesh.metrics

import android.content.Context
import android.os.BatteryManager
import kotlin.math.max
import kotlin.math.min

/** Collects measurements available locally and keeps delivery counters for route scoring. */
class MetricsCollector(context: Context) {
    private val batteryManager = context.applicationContext
        .getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    private var sends = 0
    private var successfulSends = 0
    private var acknowledgements = 0
    private var latencyTotalMs = 0L
    private var latencySamples = 0

    @Synchronized
    fun recordSend(success: Boolean) {
        sends += 1
        if (success) successfulSends += 1
    }

    @Synchronized
    fun recordAcknowledgement(latencyMs: Long?) {
        acknowledgements += 1
        if (latencyMs != null && latencyMs >= 0) {
            latencyTotalMs += latencyMs
            latencySamples += 1
        }
    }

    @Synchronized
    fun snapshot(connectedLinks: Int): Map<String, Any?> {
        val battery = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            .takeIf { it in 0..100 }
        val successRate = if (sends == 0) null else successfulSends.toDouble() / sends
        val averageLatency = if (latencySamples == 0) null
        else latencyTotalMs.toDouble() / latencySamples
        val packetLoss = successRate?.let { 1.0 - it }
        val availability = if (connectedLinks > 0) 1.0 else 0.0
        val score = routeScore(
            batteryPercent = battery?.toDouble(),
            latencyMs = averageLatency,
            packetLoss = packetLoss,
            availability = availability,
        )
        return mapOf(
            "batteryPercent" to battery,
            "latencyMs" to averageLatency,
            "packetLossFraction" to packetLoss,
            "availabilityFraction" to availability,
            "connectedLinks" to connectedLinks,
            "sendCount" to sends,
            "acknowledgementCount" to acknowledgements,
            "routeScore" to score,
            "provenance" to "measured_local_android",
        )
    }

    private fun routeScore(
        batteryPercent: Double?,
        latencyMs: Double?,
        packetLoss: Double?,
        availability: Double,
    ): Double {
        val batteryPenalty = batteryPercent?.let { 1.0 - it / 100.0 } ?: 0.5
        val latencyPenalty = latencyMs?.let { min(1.0, max(0.0, it / 1_000.0)) } ?: 0.5
        val lossPenalty = packetLoss ?: 0.5
        val availabilityPenalty = 1.0 - availability
        return 0.25 * batteryPenalty +
            0.30 * latencyPenalty +
            0.25 * lossPenalty +
            0.20 * availabilityPenalty
    }
}
