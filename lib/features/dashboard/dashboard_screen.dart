import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

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
    final measuredBattery = state.nativeMetrics['batteryPercent'];
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
              label: state.isActive ? 'MESH ACTIVE' : 'OFFLINE',
              color: state.isActive ? const Color(0xff39d98a) : Colors.orange,
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
              GridView.count(
                crossAxisCount: MediaQuery.sizeOf(context).width > 700 ? 4 : 2,
                shrinkWrap: true,
                physics: const NeverScrollableScrollPhysics(),
                mainAxisSpacing: 10,
                crossAxisSpacing: 10,
                childAspectRatio: 1.7,
                children: [
                  MeshMetricCard(
                    label: 'Connected Nodes',
                    value: '${state.connectedPeerCount}',
                    icon: Icons.hub_outlined,
                    color: const Color(0xff42a5f5),
                  ),
                  MeshMetricCard(
                    label: 'Active Routes',
                    value: '${state.activeRoutes}',
                    icon: Icons.alt_route,
                    color: const Color(0xff64ff8c),
                  ),
                  MeshMetricCard(
                    label: 'Pending Messages',
                    value: '${state.pendingMessages}',
                    icon: Icons.mark_unread_chat_alt_outlined,
                    color: Colors.orange,
                  ),
                  MeshMetricCard(
                    label: 'Device Battery',
                    value: measuredBattery == null
                        ? '—'
                        : measuredBattery.toString() + '%',
                    icon: Icons.battery_5_bar,
                    color: const Color(0xff64ff8c),
                  ),
                ],
              ),
              const SizedBox(height: 8),
              Text(
                'Discovery is ${state.isDiscovering ? 'active' : 'inactive'} • '
                'Real topology only',
                style: theme.textTheme.bodySmall,
              ),
              if (state.nativeMetrics.isNotEmpty)
                Text(
                  'Measured battery: ${state.nativeMetrics['batteryPercent'] ?? 'unavailable'}% • '
                  'Measured route score: ${(state.nativeMetrics['routeScore'] as num?)?.toStringAsFixed(2) ?? 'unavailable'}',
                  style: theme.textTheme.bodySmall,
                ),
              const SizedBox(height: 16),
              LayoutBuilder(
                builder: (context, constraints) {
                  final topology = _Panel(
                    title: 'Live Mesh Topology',
                    icon: Icons.account_tree_outlined,
                    child: SizedBox(
                      height: 320,
                      child: state.topologyNodes.length <= 1
                          ? const Center(child: Text('No real connections available'))
                          : MeshTopology(
                              nodes: state.topologyNodes,
                              onNodeTap: (node) {
                                if (node.id == 'local' ||
                                    !state.nativePeers.any((peer) => peer.id == node.id)) {
                                  return;
                                }
                                context.go(
                                  '/messages?destination=' +
                                      Uri.encodeComponent(node.id),
                                );
                              },
                            ),
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
                title: 'Active Route',
                icon: Icons.route_outlined,
                child: ListTile(
                  leading: const Icon(Icons.my_location, color: Color(0xff42a5f5)),
                  title: Text(
                    state.routeDestination == null
                        ? 'No active route'
                        : 'YOU  →  ${state.routeDestination}',
                  ),
                  subtitle: Text(
                    '${state.routeStatus.toUpperCase()}'
                    '${state.routeReason == null ? '' : '  •  ${state.routeReason}'}'
                    '  •  Cost ${state.routeCost.toStringAsFixed(2)}',
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
