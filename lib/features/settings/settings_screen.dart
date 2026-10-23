import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../infra/api/mesh_dashboard_controller.dart';
import '../../infra/services/auth_state.dart';
import '../../infra/services/theme_controller.dart';

/// Mesh routing and developer settings.
class SettingsScreen extends ConsumerStatefulWidget {
  const SettingsScreen({super.key});

  @override
  ConsumerState<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends ConsumerState<SettingsScreen> {
  bool _signingOut = false;

  Future<void> _signOut() async {
    setState(() => _signingOut = true);
    try {
      await ref.read(authControllerProvider.notifier).signOut();
      if (mounted) context.go('/login');
    } catch (error) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Could not log out: $error')),
        );
      }
    } finally {
      if (mounted) setState(() => _signingOut = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(meshDashboardProvider);
    final controller = ref.read(meshDashboardProvider.notifier);
    final themeMode = ref.watch(themeModeProvider);
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        Text('Settings', style: Theme.of(context).textTheme.headlineSmall),
        const SizedBox(height: 12),
        Card(
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('Theme', style: Theme.of(context).textTheme.titleMedium),
                const SizedBox(height: 12),
                SizedBox(
                  width: double.infinity,
                  child: SegmentedButton<ThemeMode>(
                    segments: const [
                      ButtonSegment(
                        value: ThemeMode.light,
                        icon: Icon(Icons.light_mode_outlined),
                        label: Text('Light'),
                      ),
                      ButtonSegment(
                        value: ThemeMode.dark,
                        icon: Icon(Icons.dark_mode_outlined),
                        label: Text('Dark'),
                      ),
                    ],
                    selected: {themeMode},
                    onSelectionChanged: (selection) => ref
                        .read(themeModeProvider.notifier)
                        .setThemeMode(selection.first),
                  ),
                ),
              ],
            ),
          ),
        ),
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
            subtitle: const Text('Generate deterministic nodes until devices connect'),
            value: state.simulationEnabled,
            onChanged: controller.toggleSimulation,
          ),
        ),
        Card(
          child: ListTile(
            leading: const Icon(Icons.auto_graph),
            title: const Text('Routing strategy'),
            subtitle: Text(
              '${state.routingEvaluation['strategy'] ?? 'deterministic_with_fallback'}'
              ' • Model enabled: ${state.routingEvaluation['modelEnabled'] ?? false}',
            ),
          ),
        ),
        const Card(child: SwitchListTile(value: true, onChanged: null, title: Text('Developer logs'), subtitle: Text('Native discovery and routing events'))),
        const SizedBox(height: 12),
        Card(
          child: ListTile(
            leading: const Icon(Icons.logout),
            title: const Text('Log out'),
            subtitle: const Text('Sign out of this account'),
            trailing: _signingOut
                ? const SizedBox(
                    width: 20,
                    height: 20,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                : null,
            onTap: _signingOut ? null : _signOut,
          ),
        ),
      ],
    );
  }
}
