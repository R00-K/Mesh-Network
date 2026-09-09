package org.disastermesh.app

import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import org.disastermesh.core.config.MeshConfiguration
import org.disastermesh.core.model.NodeId

class MainActivity : Activity() {
    private val logger = AndroidMeshLogger()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val configuration = MeshConfiguration(localNodeId = NodeId("android-node"))
        logger.info("application_started", mapOf("nodeId" to configuration.localNodeId))

        setContentView(TextView(this).apply {
            text = "Disaster Mesh\nFoundation ready"
            textSize = 20f
            setPadding(32, 32, 32, 32)
        })
    }
}
