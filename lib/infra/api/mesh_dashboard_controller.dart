import 'dart:async';
import 'dart:math';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../services/repositories/mesh_repository.dart';
import '../../shared/models/mesh_models.dart';
import 'mesh_dashboard_models.dart';

final meshDashboardProvider = NotifierProvider.autoDispose<
    MeshDashboardController, MeshDashboardState>(MeshDashboardController.new);

/// Coordinates simulated dashboard data and the native mesh channel.
class MeshDashboardController extends AutoDisposeNotifier<MeshDashboardState> {
  Timer? _timer;
  StreamSubscription<Map<String, dynamic>>? _eventsSubscription;
  final _random = Random(7);

  @override
  MeshDashboardState build() {
    ref.onDispose(() {
      _timer?.cancel();
      _eventsSubscription?.cancel();
    });
    final nodes = _simulatedNodes();
    _timer = Timer.periodic(const Duration(seconds: 5), (_) => _tick());
    final channel = ref.read(meshChannelProvider);
    _eventsSubscription = channel.events.listen(_handleNativeEvent);
    _loadNativePeers();
    return MeshDashboardState(nodes: nodes);
  }

  Future<void> startMesh() async {
    state = state.copyWith(isActive: true, isDiscovering: true);
    try {
      await ref.read(meshChannelProvider).startMesh();
    } catch (_) {
      // The simulation remains available on unsupported platforms and tests.
    }
  }

  Future<void> _loadNativePeers() async {
    try {
      final peers = await ref.read(meshChannelProvider).getPeers();
      state = state.copyWith(nativePeers: peers);
    } catch (_) {
      // Native mesh is unavailable on non-Android platforms.
    }
  }

  Future<void> connectPeer(String peerId) async {
    await ref.read(meshChannelProvider).connectPeer(peerId);
  }

  Future<void> disconnectPeer() async {
    await ref.read(meshChannelProvider).disconnectPeer();
  }

  void addOutgoingMessage(String id, String text) {
    state = state.copyWith(
      chatMessages: [
        ...state.chatMessages,
        MeshChatMessage(id: id, text: text, incoming: false),
      ],
    );
  }

  void _handleNativeEvent(Map<String, dynamic> event) {
    final payload = Map<String, dynamic>.from(event['payload'] as Map? ?? {});
    if (event['type'] == 'MeshStatusChanged') {
      state = state.copyWith(
        isActive: payload['running'] as bool? ?? state.isActive,
        isDiscovering: payload['discovering'] as bool? ?? state.isDiscovering,
      );
    } else if (event['type'] == 'TransportChanged') {
      state = state.copyWith(
        transportState: payload['state'] as String? ?? state.transportState,
      );
    } else if (event['type'] == 'MetricsChanged') {
      state = state.copyWith(
        nativeMetrics: Map<String, dynamic>.from(payload),
      );
    } else if (event['type'] == 'RoutingEvaluationChanged') {
      state = state.copyWith(
        routingEvaluation: Map<String, dynamic>.from(payload),
      );
    } else if (event['type'] == 'PeerUpdate') {
      final peers = (payload['peers'] as List? ?? const [])
          .whereType<Map>()
          .map((peer) => Peer.fromJson(Map<String, dynamic>.from(peer)))
          .toList(growable: false);
      state = state.copyWith(nativePeers: peers);
    } else if (event['type'] == 'MessageReceived') {
      final source = payload['source'] ?? 'unknown';
      final message = payload['payload'] ?? '';
      state = state.copyWith(
        chatMessages: [
          ...state.chatMessages,
          MeshChatMessage(
            id: payload['id'] as String? ?? DateTime.now().toIso8601String(),
            text: message.toString(),
            incoming: true,
            status: 'received',
          ),
        ],
        messageLog: ['Received from $source: $message', ...state.messageLog]
            .take(50)
            .toList(growable: false),
      );
    } else if (event['type'] == 'MessageStatusChanged') {
      final id = payload['id'] ?? 'unknown';
      final status = payload['status'] ?? 'unknown';
      state = state.copyWith(
        chatMessages: [
          for (final message in state.chatMessages)
            message.id == id ? message.copyWith(status: status) : message,
        ],
        messageLog: ['Message $id: $status', ...state.messageLog]
            .take(50)
            .toList(growable: false),
      );
    } else if (event['type'] == 'RouteChanged') {
      final status = payload['status'] as String? ?? 'no_route';
      state = state.copyWith(
        routeStatus: status,
        routeDestination: payload['destination'] as String?,
        routeReason: payload['reason'] as String?,
        activeRoutes: status == 'active' ? 1 : 0,
      );
    }
  }

  void toggleSimulation(bool enabled) {
    state = state.copyWith(
      simulationEnabled: enabled,
      nodes: enabled ? _simulatedNodes() : const [],
    );
  }

  void recalculateRoute() {
    state = state.copyWith(
      routeCost: 1.1 + _random.nextDouble() * .8,
      routeLatency: 24 + _random.nextInt(30),
      events: [
        const MeshEvent(
          title: 'Route recalculated',
          detail: 'Lowest-cost path selected',
          time: 'now',
          icon: Icons.alt_route,
          color: Color(0xff42a5f5),
        ),
        ...state.events,
      ].take(8).toList(),
    );
  }

  void _tick() {
    if (!state.simulationEnabled) return;
    final nodes = state.nodes
        .map((node) => MeshNode(
              id: node.id,
              name: node.name,
              battery: max(12, node.battery - _random.nextInt(3)),
              signal: max(42, node.signal - _random.nextInt(4)),
              latency: max(18, node.latency + _random.nextInt(7) - 3),
              position: node.position,
              role: node.role,
            ))
        .toList();
    state = state.copyWith(nodes: nodes);
  }

  List<MeshNode> _simulatedNodes() => const [
        MeshNode(
          id: 'local',
          name: 'You',
          battery: 86,
          signal: 100,
          latency: 0,
          position: Offset(.5, .5),
          role: MeshNodeRole.local,
        ),
        MeshNode(
          id: 'alpha',
          name: 'Alpha',
          battery: 79,
          signal: 91,
          latency: 18,
          position: Offset(.5, .16),
          role: MeshNodeRole.relay,
        ),
        MeshNode(
          id: 'bravo',
          name: 'Bravo',
          battery: 68,
          signal: 82,
          latency: 23,
          position: Offset(.82, .34),
        ),
        MeshNode(
          id: 'charlie',
          name: 'Charlie',
          battery: 93,
          signal: 74,
          latency: 31,
          position: Offset(.78, .78),
          role: MeshNodeRole.destination,
        ),
        MeshNode(
          id: 'delta',
          name: 'Delta',
          battery: 54,
          signal: 69,
          latency: 44,
          position: Offset(.22, .78),
        ),
        MeshNode(
          id: 'echo',
          name: 'Echo',
          battery: 61,
          signal: 77,
          latency: 28,
          position: Offset(.18, .34),
        ),
      ];
}
