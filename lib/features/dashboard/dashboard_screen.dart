import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../infra/api/mesh_dashboard_controller.dart';
import '../../shared/widgets/mesh_metric_card.dart';
import '../../shared/widgets/mesh_topology.dart';

/// Main live operations dashboard.
class DashboardScreen extends ConsumerWidget {
  const DashboardScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(meshDashboardProvider);
    final controller = ref.read(meshDashboardProvider.notifier);
    final theme = Theme.of(context);
    return CustomScrollView(
      slivers: [
        SliverAppBar(
          pinned: true,
          title: const Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('VitalGuard Mesh'),
              Text('RESCUE OPERATIONS', style: TextStyle(fontSize: 10)),
            ],
          ),
          actions: [
            _StatusChip(
              label: state.isActive ? 'DISCOVERY ACTIVE' : 'NOT CONNECTED',
              color: state.isActive ? const Color(0xff42a5f5) : Colors.orange,
            ),
            const SizedBox(width: 12),
            IconButton(onPressed: () {}, icon: const Icon(Icons.settings_outlined)),
          ],
        ),
        SliverPadding(
          padding: const EdgeInsets.all(16),
          sliver: SliverList(
            delegate: SliverChildListDelegate([
              if (!state.isActive)
                Padding(
                  padding: const EdgeInsets.only(bottom: 14),
                  child: FilledButton.icon(
                    onPressed: controller.startMesh,
                    icon: const Icon(Icons.wifi_tethering),
                    label: const Text('Start Mesh Discovery'),
                  ),
                ),
              Card(
                child: ListTile(
                  leading: Icon(state.isActive ? Icons.wifi_find : Icons.info_outline),
                  title: Text(state.discoveryError ??
                      (state.isDiscovering ? 'Starting Wi-Fi Direct discovery…' :
                       state.isActive ? 'Wi-Fi Direct discovery is active' : 'No Wi-Fi Direct connection')),
                  subtitle: Text('${state.discoveredPeers.length} real nearby peer(s) found. Discovery does not connect devices or send messages.'),
                  trailing: IconButton(
                    tooltip: 'Refresh nearby devices',
                    onPressed: controller.refreshPeers,
                    icon: const Icon(Icons.refresh),
                  ),
                ),
              ),
              if (state.simulationEnabled)
                const Padding(
                  padding: EdgeInsets.only(top: 12, bottom: 4),
                  child: Text('DEMO DATA · the topology and network metrics below are simulated'),
                ),
              GridView.count(
                crossAxisCount: MediaQuery.sizeOf(context).width > 700 ? 4 : 2,
                shrinkWrap: true,
                physics: const NeverScrollableScrollPhysics(),
                mainAxisSpacing: 10,
                crossAxisSpacing: 10,
                childAspectRatio: 1.7,
                children: [
                  MeshMetricCard(
                    label: 'Demo Nodes',
                    value: '${state.nodes.length}',
                    icon: Icons.hub_outlined,
                    color: const Color(0xff42a5f5),
                  ),
                  MeshMetricCard(
                    label: 'Demo Routes',
                    value: '${state.activeRoutes}',
                    icon: Icons.alt_route,
                    color: const Color(0xff64ff8c),
                  ),
                  MeshMetricCard(
                    label: 'Demo Queue',
                    value: '${state.pendingMessages}',
                    icon: Icons.mark_unread_chat_alt_outlined,
                    color: Colors.orange,
                  ),
                  MeshMetricCard(
                    label: 'Demo Battery',
                    value: '${state.averageBattery}%',
                    icon: Icons.battery_5_bar,
                    color: const Color(0xff64ff8c),
                  ),
                ],
              ),
              const SizedBox(height: 16),
              LayoutBuilder(
                builder: (context, constraints) {
                  final topology = _Panel(
                    title: 'Live Mesh Topology',
                    icon: Icons.account_tree_outlined,
                    child: SizedBox(
                      height: 320,
                      child: MeshTopology(nodes: state.nodes),
                    ),
                  );
                  final events = _Panel(
                    title: 'Recent Network Events',
                    icon: Icons.bolt_outlined,
                    child: Column(
                      children: state.events.isEmpty
                          ? [
                              const Padding(
                                padding: EdgeInsets.all(28),
                                child: Text('Start discovery to monitor the mesh.'),
                              ),
                            ]
                          : state.events
                              .map((event) => ListTile(
                                    dense: true,
                                    leading: Icon(event.icon, color: event.color),
                                    title: Text(event.title),
                                    subtitle: Text(event.detail),
                                    trailing: Text(event.time),
                                  ))
                              .toList(),
                    ),
                  );
                  if (constraints.maxWidth < 760) {
                    return Column(children: [topology, const SizedBox(height: 16), events]);
                  }
                  return Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Expanded(flex: 3, child: topology),
                      const SizedBox(width: 16),
                      Expanded(flex: 2, child: events),
                    ],
                  );
                },
              ),
              const SizedBox(height: 16),
              _Panel(
                title: 'Demo Route',
                icon: Icons.route_outlined,
                child: ListTile(
                  leading: const Icon(Icons.my_location, color: Color(0xff42a5f5)),
                  title: const Text('YOU  →  Alpha  →  Charlie'),
                  subtitle: Text(
                    'Cost ${state.routeCost.toStringAsFixed(2)}  •  ${state.routeLatency} ms  •  2 hops',
                  ),
                  trailing: OutlinedButton(
                    onPressed: controller.recalculateRoute,
                    child: const Text('Recalculate'),
                  ),
                ),
              ),
              const SizedBox(height: 16),
              Text('Emergency Controls', style: theme.textTheme.titleMedium),
              const SizedBox(height: 8),
              Wrap(
                spacing: 8,
                runSpacing: 8,
                children: [
                  FilledButton.icon(
                    onPressed: () {},
                    icon: const Icon(Icons.sos),
                    label: const Text('SOS Broadcast'),
                  ),
                  OutlinedButton.icon(
                    onPressed: () {},
                    icon: const Icon(Icons.medical_services_outlined),
                    label: const Text('Medical Emergency'),
                  ),
                  OutlinedButton.icon(
                    onPressed: () {},
                    icon: const Icon(Icons.location_on_outlined),
                    label: const Text('Share Location'),
                  ),
                ],
              ),
            ]),
          ),
        ),
      ],
    );
  }
}

class _Panel extends StatelessWidget {
  const _Panel({required this.title, required this.icon, required this.child});

  final String title;
  final IconData icon;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(12, 12, 12, 8),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(children: [Icon(icon, size: 18, color: const Color(0xff42a5f5)), const SizedBox(width: 8), Text(title)]),
            const Divider(),
            child,
          ],
        ),
      ),
    );
  }
}

class _StatusChip extends StatelessWidget {
  const _StatusChip({required this.label, required this.color});

  final String label;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Chip(
      avatar: Icon(Icons.circle, size: 10, color: color),
      label: Text(label, style: const TextStyle(fontSize: 10)),
      side: BorderSide(color: color.withAlpha(90)),
    );
  }
}
