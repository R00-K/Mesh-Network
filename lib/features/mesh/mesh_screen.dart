import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../infra/api/mesh_dashboard_controller.dart';

/// Nearby Wi-Fi Direct peers and clearly separated demo topology.
class MeshScreen extends ConsumerWidget {
  const MeshScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(meshDashboardProvider);
    final controller = ref.read(meshDashboardProvider.notifier);
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        Text('Mesh Nodes', style: Theme.of(context).textTheme.headlineSmall),
        const SizedBox(height: 12),
        Card(
          child: ListTile(
            leading: Icon(state.isActive ? Icons.wifi_find : Icons.wifi_off),
            title: Text(state.isActive ? 'Wi-Fi Direct discovery active' : 'Discovery not started'),
            subtitle: Text(state.discoveryError ??
                '${state.discoveredPeers.length} nearby device(s) discovered.'),
            trailing: state.isActive
                ? IconButton(
                    tooltip: 'Refresh nearby devices',
                    onPressed: controller.refreshPeers,
                    icon: const Icon(Icons.refresh),
                  )
                : FilledButton(
                    onPressed: controller.startMesh,
                    child: const Text('Start'),
                  ),
          ),
        ),
        Card(
          child: ListTile(
            leading: Icon(state.connectionStatus == 'connected' ? Icons.link : Icons.link_off),
            title: Text('Link: ${state.connectionStatus}'),
            subtitle: Text(state.connectionError ??
                (state.connectionStatus == 'connected'
                    ? state.connectionRole == 'group_owner'
                        ? 'Group owner · ${state.connectedPeerCount} client(s) connected; broadcasts are relayed to them.'
                        : 'Group client · connected to the owner; broadcasts reach its group.'
                    : 'Connect to nearby Android devices to form a Wi-Fi Direct group.')),
            trailing: state.connectionStatus == 'connected'
                ? TextButton(onPressed: controller.disconnectPeer, child: const Text('Leave group'))
                : null,
          ),
        ),
        const SizedBox(height: 16),
        Text('Nearby Wi-Fi Direct devices', style: Theme.of(context).textTheme.titleMedium),
        const SizedBox(height: 8),
        if (state.discoveredPeers.isEmpty)
          const Card(child: ListTile(title: Text('No real peers discovered yet'))),
        ...state.discoveredPeers.map((peer) => Card(
              child: ListTile(
                leading: const CircleAvatar(child: Icon(Icons.devices)),
                title: Text(peer.deviceName.isEmpty ? 'Unnamed device' : peer.deviceName),
                subtitle: Text('${peer.status.name} · Wi-Fi Direct peer'),
                trailing: state.connectionStatus == 'connecting' && state.connectedPeerId == peer.id
                    ? const SizedBox(width: 24, height: 24, child: CircularProgressIndicator(strokeWidth: 2))
                    : FilledButton.tonal(
                        onPressed: state.connectionStatus == 'connecting' ||
                                (state.connectionStatus == 'connected' && state.connectedPeerId == peer.id)
                            ? null
                            : () => controller.connectPeer(peer.id),
                        child: Text(state.connectedPeerId == peer.id && state.connectionStatus == 'connected'
                            ? 'Connected'
                            : 'Connect'),
                      ),
              ),
            )),
        if (state.simulationEnabled) ...[
          const SizedBox(height: 20),
          Text('Demo topology · simulated devices', style: Theme.of(context).textTheme.titleMedium),
          const SizedBox(height: 8),
          ...state.nodes.where((node) => node.id != 'local').map((node) => Card(
                child: ListTile(
                  leading: const CircleAvatar(child: Icon(Icons.person_outline)),
                  title: Text(node.name),
                  subtitle: const Text('Simulated data · not a real device'),
                  trailing: Text(node.role.name.toUpperCase()),
                ),
              )),
        ],
      ],
    );
  }
}
