import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../infra/api/mesh_dashboard_controller.dart';

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
        Text('${state.nodes.length} nodes visible • simulated discovery'),
        const SizedBox(height: 16),
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
    );
  }
}
