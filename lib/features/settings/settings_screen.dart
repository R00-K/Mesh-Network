import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../infra/api/mesh_dashboard_controller.dart';

/// Mesh routing and developer settings.
class SettingsScreen extends ConsumerWidget {
  const SettingsScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(meshDashboardProvider);
    final controller = ref.read(meshDashboardProvider.notifier);
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        Text('Settings', style: Theme.of(context).textTheme.headlineSmall),
        const SizedBox(height: 12),
        const Card(
          child: Column(children: [
            ListTile(title: Text('Routing Weights'), subtitle: Text('Battery, signal, latency, loss, load, and hops')),
            ListTile(leading: Icon(Icons.battery_alert), title: Text('Battery weight'), trailing: Text('20%')),
            ListTile(leading: Icon(Icons.speed), title: Text('Latency weight'), trailing: Text('20%')),
            ListTile(leading: Icon(Icons.network_check), title: Text('Signal weight'), trailing: Text('20%')),
          ]),
        ),
        Card(
          child: SwitchListTile(
            title: const Text('Simulation mode'),
            subtitle: const Text('Show sample topology and metrics, clearly separate from discovered Wi-Fi Direct peers'),
            value: state.simulationEnabled,
            onChanged: controller.toggleSimulation,
          ),
        ),
        const Card(child: SwitchListTile(value: true, onChanged: null, title: Text('Developer logs'), subtitle: Text('Native discovery and routing events'))),
      ],
    );
  }
}
