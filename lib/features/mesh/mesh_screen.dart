import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../infra/api/mesh_dashboard_controller.dart';
import '../../shared/models/mesh_models.dart';

/// Nearby and connected device screen.
class MeshScreen extends ConsumerWidget {
  const MeshScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(meshDashboardProvider);
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        Text('Mesh Nodes', style: Theme.of(context).textTheme.headlineSmall),
        const SizedBox(height: 6),
        Text(
          'Discovery: ${state.isDiscovering ? 'active' : 'inactive'}  •  '
          'Connected real peers: ${state.connectedPeerCount}',
        ),
        const SizedBox(height: 16),
        if (state.simulationEnabled) ...[
          const Text('Demo data', style: TextStyle(fontWeight: FontWeight.bold)),
          const SizedBox(height: 6),
          ...state.nodes.map((node) => Card(
              child: ListTile(
                leading: CircleAvatar(
                  backgroundColor: node.connected ? const Color(0xff39d98a) : Colors.grey,
                  child: const Icon(Icons.person_outline, color: Colors.black),
                ),
                title: Text(node.name),
                subtitle: Text('${node.battery}% battery  •  ${node.latency} ms  •  signal ${node.signal}%'),
                trailing: Text(node.role.name.toUpperCase()),
              ),
            )),
        ],
        const SizedBox(height: 12),
        const Text('Real Wi‑Fi Direct peers', style: TextStyle(fontWeight: FontWeight.bold)),
        const SizedBox(height: 6),
        if (state.nativePeers.isEmpty)
          const Card(child: ListTile(title: Text('No real peers discovered')))
        else
          ...state.nativePeers.map((peer) => Card(
                child: ListTile(
                  leading: CircleAvatar(
                    backgroundColor: peer.status == PeerStatus.connected
                        ? const Color(0xff39d98a)
                        : Colors.grey,
                    child: const Icon(Icons.wifi, color: Colors.black),
                  ),
                  title: Text(peer.deviceName),
                  subtitle: Text(peer.id),
                  trailing: peer.status == PeerStatus.connected
                      ? TextButton(
                          onPressed: ref
                              .read(meshDashboardProvider.notifier)
                              .disconnectPeer,
                          child: const Text('Disconnect'),
                        )
                      : TextButton(
                          onPressed: () => ref
                              .read(meshDashboardProvider.notifier)
                              .connectPeer(peer.id),
                          child: const Text('Connect'),
                        ),
                ),
              )),
      ],
    );
  }
}
