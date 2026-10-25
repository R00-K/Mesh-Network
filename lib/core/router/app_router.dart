import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../features/dashboard/dashboard_screen.dart';
import '../../features/emergency/emergency_screen.dart';
import '../../features/mesh/mesh_screen.dart';
import '../../features/messages/messages_screen.dart';
import '../../features/settings/settings_screen.dart';

/// Application navigation configuration.
final appRouter = createAppRouter();

/// Creates the app router, opening directly on the dashboard.
GoRouter createAppRouter({String initialLocation = '/'}) {
  return GoRouter(
  initialLocation: initialLocation,
  routes: [
    ShellRoute(
      builder: (context, state, child) => _AppShell(child: child),
      routes: [
        GoRoute(path: '/', builder: (context, state) => const DashboardScreen()),
        GoRoute(path: '/mesh', builder: (context, state) => const MeshScreen()),
        GoRoute(
          path: '/messages',
          builder: (context, state) => MessagesScreen(
            destination: state.uri.queryParameters['destination'],
          ),
        ),
        GoRoute(path: '/emergency', builder: (context, state) => const EmergencyScreen()),
        GoRoute(path: '/settings', builder: (context, state) => const SettingsScreen()),
      ],
    ),
  ],
  );
}

class _AppShell extends StatelessWidget {
  const _AppShell({required this.child});

  final Widget child;

  int _index(BuildContext context) {
    final path = GoRouterState.of(context).uri.path;
    return switch (path) {
      '/mesh' => 1,
      '/messages' => 2,
      '/emergency' => 3,
      '/settings' => 4,
      _ => 0,
    };
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: child,
      bottomNavigationBar: NavigationBar(
        selectedIndex: _index(context),
        onDestinationSelected: (index) => context.go([
          '/',
          '/mesh',
          '/messages',
          '/emergency',
          '/settings',
        ][index]),
        destinations: const [
          NavigationDestination(icon: Icon(Icons.dashboard_outlined), selectedIcon: Icon(Icons.dashboard), label: 'Dashboard'),
          NavigationDestination(icon: Icon(Icons.hub_outlined), selectedIcon: Icon(Icons.hub), label: 'Mesh'),
          NavigationDestination(icon: Icon(Icons.chat_bubble_outline), selectedIcon: Icon(Icons.chat), label: 'Messages'),
          NavigationDestination(icon: Icon(Icons.warning_amber_outlined), selectedIcon: Icon(Icons.warning), label: 'Emergency'),
          NavigationDestination(icon: Icon(Icons.settings_outlined), selectedIcon: Icon(Icons.settings), label: 'Settings'),
        ],
      ),
    );
  }
}
