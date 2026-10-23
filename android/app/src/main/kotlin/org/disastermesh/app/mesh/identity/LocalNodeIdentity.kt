package org.disastermesh.app.mesh.identity

import android.content.Context
import java.util.UUID

/** Persistent protocol identity for this app installation. */
class LocalNodeIdentity(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    val nodeId: String
        get() = synchronized(preferences) {
            preferences.getString(NODE_ID_KEY, null) ?: UUID.randomUUID().toString().also {
                preferences.edit().putString(NODE_ID_KEY, it).commit()
            }
        }

    private companion object {
        const val PREFERENCES_NAME = "mesh_identity"
        const val NODE_ID_KEY = "node_id"
    }
}
