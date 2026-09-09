package org.disastermesh.app

import android.Manifest
import android.content.pm.PackageManager
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.EventChannel
import org.disastermesh.core.config.MeshConfiguration
import org.disastermesh.core.model.NodeId
import org.disastermesh.app.mesh.channels.MeshChannelHandler

class MainActivity : FlutterActivity() {
    private val channelName = "disaster_mesh/mesh"

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        val configuration = MeshConfiguration(localNodeId = NodeId("android-node"))
        AndroidMeshLogger().info(
            "application_started",
            mapOf("nodeId" to configuration.localNodeId.toString()),
        )
        val handler = MeshChannelHandler(this, AndroidMeshLogger()) {
            val permissions = buildList {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
                add(Manifest.permission.ACCESS_COARSE_LOCATION)
                if (android.os.Build.VERSION.SDK_INT >= 33) {
                    add(Manifest.permission.NEARBY_WIFI_DEVICES)
                }
            }.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
            if (permissions.isNotEmpty()) requestPermissions(permissions.toTypedArray(), 2402)
        }
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, channelName)
            .setMethodCallHandler(handler)
        EventChannel(flutterEngine.dartExecutor.binaryMessenger, "disaster_mesh/events")
            .setStreamHandler(handler)
    }
}
