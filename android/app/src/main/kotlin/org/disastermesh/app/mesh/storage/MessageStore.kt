package org.disastermesh.app.mesh.storage

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Durable outbox, message history, and deduplication store for mesh frames. */
class MessageStore(context: Context) {
    private val preferences: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    @Synchronized
    fun localNodeId(): String {
        preferences.getString(KEY_NODE_ID, null)?.let { return it }
        val id = UUID.randomUUID().toString()
        check(preferences.edit().putString(KEY_NODE_ID, id).commit()) { "Could not save this device ID" }
        return id
    }

    @Synchronized
    fun createOutgoing(text: String, destination: String?, localNodeId: String): StoredMessage {
        val message = StoredMessage(
            id = UUID.randomUUID().toString(),
            text = text,
            source = localNodeId,
            destination = destination ?: "broadcast",
            createdAt = System.currentTimeMillis(),
            ttl = DEFAULT_TTL,
            path = listOf(localNodeId),
            status = "queued",
            direction = "outgoing",
            retryCount = 0,
            lastAttemptAt = null,
        )
        save(allMessages() + message)
        return message
    }

    /** Returns true only the first time a message ID is received. */
    @Synchronized
    fun storeIncoming(
        id: String,
        text: String,
        source: String?,
        ttl: Int,
        path: List<String>,
    ): Boolean {
        if (id.isBlank() || allMessages().any { it.id == id }) return false
        val message = StoredMessage(
            id = id,
            text = text,
            source = source ?: "wifi-direct-peer",
            destination = "local",
            createdAt = System.currentTimeMillis(),
            ttl = ttl.coerceAtLeast(0),
            path = path,
            status = if (ttl > 0) "received" else "expired",
            direction = "incoming",
            retryCount = 0,
            lastAttemptAt = null,
        )
        save(allMessages() + message)
        return true
    }

    @Synchronized
    fun pendingOutgoing(): List<StoredMessage> = allMessages().filter {
        it.direction == "outgoing" && it.status != "delivered"
    }

    @Synchronized
    fun markAttempt(id: String): StoredMessage? = update(id) {
        if (it.status == "delivered") it
        else it.copy(retryCount = it.retryCount + 1, lastAttemptAt = System.currentTimeMillis())
    }

    @Synchronized
    fun markSent(id: String): StoredMessage? = update(id) {
        if (it.status == "delivered") it else it.copy(status = "sent")
    }

    @Synchronized
    fun markQueued(id: String): StoredMessage? = update(id) {
        if (it.status == "delivered") it else it.copy(status = "queued")
    }

    @Synchronized
    fun markDelivered(id: String): StoredMessage? = update(id) {
        if (it.direction == "outgoing") it.copy(status = "delivered") else it
    }

    @Synchronized
    fun allMessages(): List<StoredMessage> {
        val raw = preferences.getString(KEY_MESSAGES, null) ?: return emptyList()
        return try {
            val json = JSONArray(raw)
            buildList(json.length()) {
                for (index in 0 until json.length()) {
                    val message = runCatching { fromJson(json.getJSONObject(index)) }.getOrNull()
                    if (message != null) add(message)
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun update(id: String, transform: (StoredMessage) -> StoredMessage): StoredMessage? {
        var changed: StoredMessage? = null
        val next = allMessages().map { message ->
            if (message.id == id) transform(message).also { changed = it } else message
        }
        if (changed != null) save(next)
        return changed
    }

    private fun save(messages: List<StoredMessage>) {
        // Keep pending messages regardless of age; trim only old completed history.
        val pending = messages.filter { it.direction == "outgoing" && it.status != "delivered" }
        val completed = messages.filterNot { it in pending }.takeLast(MAX_COMPLETED)
        val json = JSONArray().apply {
            (completed + pending).sortedBy { it.createdAt }.forEach { put(it.toJson()) }
        }
        check(preferences.edit().putString(KEY_MESSAGES, json.toString()).commit()) {
            "Could not persist the emergency message"
        }
    }

    private fun fromJson(json: JSONObject) = StoredMessage(
        id = json.getString("id"),
        text = json.getString("text"),
        source = json.optString("source", ""),
        destination = json.optString("destination", ""),
        createdAt = json.getLong("createdAt"),
        ttl = json.optInt("ttl", DEFAULT_TTL),
        path = json.optJSONArray("path")?.let { array ->
            buildList(array.length()) { for (index in 0 until array.length()) add(array.optString(index)) }
        } ?: emptyList(),
        status = json.optString("status", "queued"),
        direction = json.optString("direction", "outgoing"),
        retryCount = json.optInt("retryCount", 0),
        lastAttemptAt = if (json.isNull("lastAttemptAt")) null else json.optLong("lastAttemptAt"),
    )

    private fun StoredMessage.toJson() = JSONObject()
        .put("id", id)
        .put("text", text)
        .put("source", source)
        .put("destination", destination)
        .put("createdAt", createdAt)
        .put("ttl", ttl)
        .put("path", JSONArray(path))
        .put("status", status)
        .put("direction", direction)
        .put("retryCount", retryCount)
        .put("lastAttemptAt", lastAttemptAt)

    private companion object {
        const val PREFERENCES = "disaster_mesh_messages"
        const val KEY_MESSAGES = "messages"
        const val KEY_NODE_ID = "node_id"
        const val MAX_COMPLETED = 500
        const val DEFAULT_TTL = 8
    }
}

data class StoredMessage(
    val id: String,
    val text: String,
    val source: String,
    val destination: String,
    val createdAt: Long,
    val ttl: Int,
    val path: List<String>,
    val status: String,
    val direction: String,
    val retryCount: Int,
    val lastAttemptAt: Long?,
)
