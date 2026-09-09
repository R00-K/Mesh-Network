import 'package:flutter/material.dart';

/// Role a node currently plays in the mesh.
enum MeshNodeRole { local, relay, destination, peer }

/// A presentation-ready node supplied by simulation or native discovery.
class MeshNode {
  const MeshNode({
    required this.id,
    required this.name,
    required this.battery,
    required this.signal,
    required this.latency,
    required this.position,
    this.role = MeshNodeRole.peer,
    this.connected = true,
  });

  final String id;
  final String name;
  final int battery;
  final int signal;
  final int latency;
  final Offset position;
  final MeshNodeRole role;
  final bool connected;
}

/// A notable event in the live mesh feed.
class MeshEvent {
  const MeshEvent({
    required this.title,
    required this.detail,
    required this.time,
    required this.icon,
    required this.color,
  });

  final String title;
  final String detail;
  final String time;
  final IconData icon;
  final Color color;
}

/// Snapshot consumed by the dashboard widgets.
class MeshDashboardState {
  const MeshDashboardState({
    this.isActive = false,
    this.isDiscovering = false,
    this.simulationEnabled = true,
    this.nodes = const [],
    this.events = const [],
    this.pendingMessages = 0,
    this.activeRoutes = 1,
    this.routeCost = 1.42,
    this.routeLatency = 38,
  });

  final bool isActive;
  final bool isDiscovering;
  final bool simulationEnabled;
  final List<MeshNode> nodes;
  final List<MeshEvent> events;
  final int pendingMessages;
  final int activeRoutes;
  final double routeCost;
  final int routeLatency;

  int get averageBattery {
    if (nodes.isEmpty) return 0;
    return nodes.map((node) => node.battery).reduce((a, b) => a + b) ~/
        nodes.length;
  }

  MeshDashboardState copyWith({
    bool? isActive,
    bool? isDiscovering,
    bool? simulationEnabled,
    List<MeshNode>? nodes,
    List<MeshEvent>? events,
    int? pendingMessages,
    int? activeRoutes,
    double? routeCost,
    int? routeLatency,
  }) {
    return MeshDashboardState(
      isActive: isActive ?? this.isActive,
      isDiscovering: isDiscovering ?? this.isDiscovering,
      simulationEnabled: simulationEnabled ?? this.simulationEnabled,
      nodes: nodes ?? this.nodes,
      events: events ?? this.events,
      pendingMessages: pendingMessages ?? this.pendingMessages,
      activeRoutes: activeRoutes ?? this.activeRoutes,
      routeCost: routeCost ?? this.routeCost,
      routeLatency: routeLatency ?? this.routeLatency,
    );
  }
}
