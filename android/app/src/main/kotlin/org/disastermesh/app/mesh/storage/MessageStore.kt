package org.disastermesh.app.mesh.storage

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

enum class IncomingMessageResult { STORED, DUPLICATE, CONFLICT, PERSISTENCE_FAILED }

/** Durable outgoing queue, failed-message history, and received-message deduplication. */
class MessageStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "mesh_messages",
        Context.MODE_PRIVATE,
    )

    @Synchronized
    fun enqueue(message: JSONObject): Boolean {
        val messages = allOutgoingMessages().toMutableList()
        messages.removeAll { it.optString("id") == message.optString("id") }
        messages += JSONObject(message.toString())
            .put("status", "queued")
            .put("attemptCount", 0)
            .put("retryCount", 0)
        return saveOutgoing(messages)
    }

    /** Records only a successful socket write and returns the resulting attempt count. */
    @Synchronized
    fun recordAttempt(id: String): Int? {
        val messages = allOutgoingMessages()
        val message = messages.firstOrNull { it.optString("id") == id } ?: return null
        val attempts = message.optInt("attemptCount", message.optInt("retryCount")) + 1
        message.put("attemptCount", attempts)
            .put("retryCount", attempts)
            .put("lastAttemptAt", System.currentTimeMillis())
            .put("status", "sent")
        saveOutgoing(messages)
        return attempts
    }

    @Synchronized
    fun pendingMessages(): List<JSONObject> = allOutgoingMessages()
        .filter { it.optString("status") != "failed" }

    @Synchronized
    fun pendingMessage(id: String): JSONObject? = pendingMessages()
        .firstOrNull { it.optString("id") == id }

    @Synchronized
    fun markQueued(id: String) {
        val messages = allOutgoingMessages()
        messages.firstOrNull { it.optString("id") == id }?.put("status", "queued")
        saveOutgoing(messages)
    }

    @Synchronized
    fun markAllQueued(): List<String> {
        val messages = allOutgoingMessages()
        val ids = messages.filter { it.optString("status") != "failed" }
            .map { it.optString("id") }
        messages.filter { it.optString("status") != "failed" }
            .forEach { it.put("status", "queued") }
        saveOutgoing(messages)
        return ids
    }

    @Synchronized
    fun markFailed(id: String, reason: String) {
        val messages = allOutgoingMessages()
        messages.firstOrNull { it.optString("id") == id }
            ?.put("status", "failed")
            ?.put("failureReason", reason)
        saveOutgoing(messages)
    }

    @Synchronized
    fun markDelivered(id: String): Boolean {
        val messages = allOutgoingMessages()
        val delivered = messages.firstOrNull {
            it.optString("id") == id && it.optString("status") != "failed"
        } ?: return false
        delivered.put("status", "delivered")
        return saveOutgoing(messages.filterNot { it.optString("id") == id }, listOf(delivered))
    }

    /** Persists an accepted incoming envelope and its deduplication ID before UI delivery. */
    @Synchronized
    fun acceptIncoming(message: JSONObject): IncomingMessageResult {
        val id = message.optString("id")
        if (id.isBlank()) return IncomingMessageResult.PERSISTENCE_FAILED
        val seen = seenIds().toMutableSet()
        if (!seen.add(id)) {
            val stored = receivedMessages().firstOrNull { it.optString("id") == id }
                ?: return IncomingMessageResult.DUPLICATE
            return if (stored.optString("source") == message.optString("source") &&
                stored.optString("destination") == message.optString("destination") &&
                stored.optString("payload") == message.optString("payload") &&
                stored.optLong("createdAt") == message.optLong("createdAt") &&
                stored.optLong("expiresAt") == message.optLong("expiresAt") &&
                stored.optInt("protocolVersion") == message.optInt("protocolVersion")
            ) {
                IncomingMessageResult.DUPLICATE
            } else {
                IncomingMessageResult.CONFLICT
            }
        }

        val trimmedSeen = seen.toList().takeLast(MAX_SEEN_IDS)
        val received = receivedMessages().toMutableList()
        received += JSONObject(message.toString())
        val trimmedReceived = received.takeLast(MAX_RECEIVED_MESSAGES)
        val seenArray = JSONArray().also { array -> trimmedSeen.forEach(array::put) }
        val receivedArray = JSONArray().also { array -> trimmedReceived.forEach(array::put) }
        val persisted = preferences.edit()
            .putString(SEEN_KEY, seenArray.toString())
            .putString(RECEIVED_KEY, receivedArray.toString())
            .commit()
        return if (persisted) IncomingMessageResult.STORED else IncomingMessageResult.PERSISTENCE_FAILED
    }

    @Synchronized
    fun storedMessages(): List<JSONObject> {
        val messages = linkedMapOf<String, JSONObject>()
        historyMessages().forEach { message ->
            messages[message.optString("id")] = JSONObject(message.toString())
                .put("direction", "outgoing")
        }
        allOutgoingMessages().forEach { message ->
            messages.putIfAbsent(
                message.optString("id"),
                JSONObject(message.toString()).put("direction", "outgoing"),
            )
        }
        receivedMessages().forEach { message ->
            messages[message.optString("id")] = JSONObject(message.toString())
                .put("direction", "incoming")
        }
        return messages.values.sortedBy { it.optLong("createdAt") }
    }

    private fun allOutgoingMessages(): List<JSONObject> {
        val stored = preferences.getString(PENDING_KEY, null) ?: return emptyList()
        val array = JSONArray(stored)
        return (0 until array.length()).map { JSONObject(array.getJSONObject(it).toString()) }
    }

    private fun receivedMessages(): List<JSONObject> {
        val stored = preferences.getString(RECEIVED_KEY, null) ?: return emptyList()
        val array = JSONArray(stored)
        return (0 until array.length()).map { JSONObject(array.getJSONObject(it).toString()) }
    }

    private fun seenIds(): List<String> {
        val stored = preferences.getString(SEEN_KEY, null) ?: return emptyList()
        val array = JSONArray(stored)
        return (0 until array.length()).map { array.getString(it) }
    }

    private fun historyMessages(): List<JSONObject> {
        val stored = preferences.getString(HISTORY_KEY, null) ?: return emptyList()
        val array = JSONArray(stored)
        return (0 until array.length()).map { JSONObject(array.getJSONObject(it).toString()) }
    }

    private fun saveOutgoing(
        messages: List<JSONObject>,
        historyUpdates: List<JSONObject> = emptyList(),
    ): Boolean {
        val historyById = linkedMapOf<String, JSONObject>()
        historyMessages().forEach { historyById[it.optString("id")] = it }
        messages.forEach { historyById[it.optString("id")] = JSONObject(it.toString()) }
        historyUpdates.forEach { historyById[it.optString("id")] = JSONObject(it.toString()) }
        val history = historyById.values.toList().takeLast(MAX_OUTGOING_HISTORY)
        val activeMessages = messages.filter { it.optString("status") != "failed" }
        val failedMessages = messages.filter { it.optString("status") == "failed" }
            .takeLast(MAX_FAILED_MESSAGES)
        val retainedOutgoing = (activeMessages + failedMessages)
            .sortedBy { it.optLong("createdAt") }
        return preferences.edit()
            .putString(PENDING_KEY, JSONArray(retainedOutgoing).toString())
            .putString(HISTORY_KEY, JSONArray(history).toString())
            .commit()
    }

    private companion object {
        const val PENDING_KEY = "pending_messages"
        const val SEEN_KEY = "seen_message_ids"
        const val RECEIVED_KEY = "received_messages"
        const val HISTORY_KEY = "outgoing_message_history"
        const val MAX_SEEN_IDS = 1_000
        const val MAX_RECEIVED_MESSAGES = 1_000
        const val MAX_OUTGOING_HISTORY = 1_000
        const val MAX_FAILED_MESSAGES = 1_000
    }
}
