import 'package:flutter/material.dart';

import '../../shared/models/mesh_models.dart';

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
    this.connected = false,
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

class MeshChatMessage {
  const MeshChatMessage({
    required this.id,
    required this.text,
    required this.incoming,
    this.status = 'sending',
  });

  final String id;
  final String text;
  final bool incoming;
  final String status;

  MeshChatMessage copyWith({String? status}) => MeshChatMessage(
        id: id,
        text: text,
        incoming: incoming,
        status: status ?? this.status,
      );
}

/// Snapshot consumed by the dashboard widgets.
class MeshDashboardState {
  const MeshDashboardState({
    this.isActive = false,
    this.isDiscovering = false,
    this.simulationEnabled = true,
    this.nodes = const [],
    this.nativePeers = const [],
    this.messageLog = const [],
    this.chatMessages = const [],
    this.nativeMetrics = const {},
    this.routingEvaluation = const {},
    this.events = const [],
    this.pendingMessages = 0,
    this.activeRoutes = 0,
    this.routeCost = 1.42,
    this.routeLatency = 38,
    this.routeStatus = 'no_route',
    this.routeDestination,
    this.routeReason,
  });

  final bool isActive;
  final bool isDiscovering;
  final bool simulationEnabled;
  final List<MeshNode> nodes;
  final List<Peer> nativePeers;
  final List<String> messageLog;
  final List<MeshChatMessage> chatMessages;
  final Map<String, dynamic> nativeMetrics;
  final Map<String, dynamic> routingEvaluation;
  final List<MeshEvent> events;
  final int pendingMessages;
  final int activeRoutes;
  final double routeCost;
  final int routeLatency;
  final String routeStatus;
  final String? routeDestination;
  final String? routeReason;

  int get connectedPeerCount => nativePeers
      .where((peer) => peer.status == PeerStatus.connected)
      .length;

  /// Shows only the local device and currently discovered real peers.
  List<MeshNode> get topologyNodes {
    final realNodes = <MeshNode>[
      const MeshNode(
        id: 'local',
        name: 'This device',
        battery: 0,
        signal: 0,
        latency: 0,
        position: Offset(.5, .5),
        role: MeshNodeRole.local,
      ),
    ];
    for (var index = 0; index < nativePeers.length; index++) {
      final peer = nativePeers[index];
      realNodes.add(
        MeshNode(
          id: peer.id,
          name: peer.deviceName,
          battery: peer.batteryLevel.round(),
          signal: peer.signalStrength.round(),
          latency: peer.latencyMs.round(),
          position: Offset(
            .2 + (index % 3) * .3,
            .2 + (index ~/ 3) * .3,
          ),
          connected: peer.status == PeerStatus.connected,
        ),
      );
    }
    return realNodes;
  }

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
    List<Peer>? nativePeers,
    List<String>? messageLog,
    List<MeshChatMessage>? chatMessages,
    Map<String, dynamic>? nativeMetrics,
    Map<String, dynamic>? routingEvaluation,
    List<MeshEvent>? events,
    int? pendingMessages,
    int? activeRoutes,
    double? routeCost,
    int? routeLatency,
    String? routeStatus,
    String? routeDestination,
    String? routeReason,
  }) {
    return MeshDashboardState(
      isActive: isActive ?? this.isActive,
      isDiscovering: isDiscovering ?? this.isDiscovering,
      simulationEnabled: simulationEnabled ?? this.simulationEnabled,
      nodes: nodes ?? this.nodes,
      nativePeers: nativePeers ?? this.nativePeers,
      messageLog: messageLog ?? this.messageLog,
      chatMessages: chatMessages ?? this.chatMessages,
      nativeMetrics: nativeMetrics ?? this.nativeMetrics,
      routingEvaluation: routingEvaluation ?? this.routingEvaluation,
      events: events ?? this.events,
      pendingMessages: pendingMessages ?? this.pendingMessages,
      activeRoutes: activeRoutes ?? this.activeRoutes,
      routeCost: routeCost ?? this.routeCost,
      routeLatency: routeLatency ?? this.routeLatency,
      routeStatus: routeStatus ?? this.routeStatus,
      routeDestination: routeDestination ?? this.routeDestination,
      routeReason: routeReason ?? this.routeReason,
    );
  }
}
