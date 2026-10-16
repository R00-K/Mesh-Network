package org.disastermesh.app.mesh.routing

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Records route outcomes and provides a transparent deterministic fallback. */
class AdaptiveRouteEvaluator(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "mesh_route_outcomes",
        Context.MODE_PRIVATE,
    )

    @Synchronized
    fun recordOutcome(
        routeId: String,
        delivered: Boolean,
        latencyMs: Long?,
        routeScore: Double?,
    ) {
        val outcomes = readOutcomes().toMutableList()
        outcomes += JSONObject()
            .put("routeId", routeId)
            .put("delivered", delivered)
            .put("latencyMs", latencyMs)
            .put("routeScore", routeScore)
            .put("recordedAt", System.currentTimeMillis())
        val retained = outcomes.takeLast(MAX_OUTCOMES)
        val array = JSONArray()
        retained.forEach { array.put(it) }
        preferences.edit().putString(OUTCOMES_KEY, array.toString()).apply()
    }

    @Synchronized
    fun evaluation(): Map<String, Any?> {
        val outcomes = readOutcomes()
        val delivered = outcomes.count { it.optBoolean("delivered") }
        val successRate = if (outcomes.isEmpty()) null
        else delivered.toDouble() / outcomes.size
        return mapOf(
            "strategy" to "deterministic_with_fallback",
            "modelEnabled" to false,
            "sampleCount" to outcomes.size,
            "deliverySuccessRate" to successRate,
            "fallback" to "lowest_deterministic_route_score",
            "provenance" to "recorded_local_outcomes",
        )
    }

    private fun readOutcomes(): List<JSONObject> {
        val stored = preferences.getString(OUTCOMES_KEY, null) ?: return emptyList()
        val array = JSONArray(stored)
        return (0 until array.length()).map { array.getJSONObject(it) }
    }

    private companion object {
        const val OUTCOMES_KEY = "route_outcomes"
        const val MAX_OUTCOMES = 2_000
    }
}
