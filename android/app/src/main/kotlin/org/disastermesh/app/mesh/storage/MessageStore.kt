package org.disastermesh.app.mesh.storage

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Small durable store for outgoing messages and received message IDs. */
class MessageStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "mesh_messages",
        Context.MODE_PRIVATE,
    )

    @Synchronized
    fun enqueue(message: JSONObject) {
        val messages = pendingMessages().toMutableList()
        messages.removeAll { it.optString("id") == message.optString("id") }
        messages += message.put("status", "queued").put("retryCount", 0)
        savePending(messages)
    }

    @Synchronized
    fun markAttempted(id: String) {
        val messages = pendingMessages()
        messages.firstOrNull { it.optString("id") == id }?.apply {
            put("status", "sending")
            put("retryCount", optInt("retryCount") + 1)
            put("lastAttemptAt", System.currentTimeMillis())
        }
        savePending(messages)
    }

    @Synchronized
    fun markDelivered(id: String) {
        val messages = pendingMessages().filterNot { it.optString("id") == id }
        savePending(messages)
    }

    @Synchronized
    fun pendingMessages(): List<JSONObject> {
        val stored = preferences.getString(PENDING_KEY, null) ?: return emptyList()
        val array = JSONArray(stored)
        return (0 until array.length()).map { array.getJSONObject(it) }
    }

    @Synchronized
    fun markSeen(id: String): Boolean {
        if (id.isBlank()) return false
        val seen = seenIds().toMutableSet()
        if (!seen.add(id)) return false
        val trimmed = seen.takeLast(MAX_SEEN_IDS)
        val array = JSONArray()
        trimmed.forEach { array.put(it) }
        preferences.edit().putString(SEEN_KEY, array.toString()).apply()
        return true
    }

    private fun seenIds(): List<String> {
        val stored = preferences.getString(SEEN_KEY, null) ?: return emptyList()
        val array = JSONArray(stored)
        return (0 until array.length()).map { array.getString(it) }
    }

    private fun savePending(messages: List<JSONObject>) {
        preferences.edit()
            .putString(PENDING_KEY, JSONArray(messages).toString())
            .apply()
    }

    private companion object {
        const val PENDING_KEY = "pending_messages"
        const val SEEN_KEY = "seen_message_ids"
        const val MAX_SEEN_IDS = 1_000
    }
}
