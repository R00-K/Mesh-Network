import 'dart:async';
import 'dart:math';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../services/repositories/mesh_repository.dart';
import 'mesh_dashboard_models.dart';

final meshDashboardProvider = NotifierProvider.autoDispose<
    MeshDashboardController, MeshDashboardState>(MeshDashboardController.new);

/// Coordinates simulated dashboard data and the native mesh channel.
class MeshDashboardController extends AutoDisposeNotifier<MeshDashboardState> {
  Timer? _timer;
  final _random = Random(7);

  @override
  MeshDashboardState build() {
    ref.onDispose(() => _timer?.cancel());
    final events = ref.read(meshChannelProvider).events.listen((event) {
      if (event['type'] == 'DiscoveryStatusChanged') {
        _refreshNativeStatus();
      }
      if (event['type'] == 'ConnectionStatusChanged') {
        final payload = Map<String, dynamic>.from(event['payload'] as Map);
        final status = payload['status'] as String? ?? 'disconnected';
        state = state.copyWith(
          connectionStatus: status,
          connectionRole: payload['role'] as String?,
          clearConnectionRole: status != 'connected',
          connectedPeerCount: (payload['peerCount'] as num?)?.toInt() ?? 0,
          connectedPeerId: payload['peerId'] as String?,
          clearConnectedPeer: status == 'disconnected',
          connectionError: payload['detail'] as String?,
          clearConnectionError: status == 'connected' || status == 'disconnected',
        );
      }
    }, onError: (_) {});
    ref.onDispose(events.cancel);
    final nodes = _simulatedNodes();
    _timer = Timer.periodic(const Duration(seconds: 5), (_) => _refresh());
    return MeshDashboardState(nodes: nodes);
  }

  Future<void> startMesh() async {
    state = state.copyWith(isDiscovering: true);
    try {
      await ref.read(meshChannelProvider).startMesh();
      await _refreshNativeStatus();
      await _loadRealPeers();
    } catch (_) {
      state = state.copyWith(
        isActive: false,
        discoveryError: 'Wi-Fi Direct discovery is only available on Android.',
      );
    }
    state = state.copyWith(isDiscovering: false);
  }

  Future<void> _refresh() async {
    _tick();
    if (state.isActive) await _loadRealPeers();
  }

  Future<void> _loadRealPeers() async {
    try {
      final peers = await ref.read(meshChannelProvider).getPeers();
      state = state.copyWith(
        discoveredPeers: peers,
        clearDiscoveryError: state.isActive,
      );
    } catch (_) {
      state = state.copyWith(
        discoveryError: 'Could not read Wi-Fi Direct peers on this platform.',
      );
    }
  }

  Future<void> _refreshNativeStatus() async {
    try {
      final status = await ref.read(meshChannelProvider).getDiscoveryStatus();
      state = state.copyWith(
        isActive: status == 'discovering',
        isDiscovering: status == 'starting',
        discoveryError: switch (status) {
          'permission_required' => 'Allow Nearby devices and location permissions to discover peers.',
          'permission_denied' => 'Permissions denied. Grant them in Android settings to discover peers.',
          'discovery_failed' => 'Android could not start Wi-Fi Direct discovery. Check Wi-Fi and try again.',
          _ => null,
        },
        clearDiscoveryError: status == 'discovering' || status == 'stopped',
      );
    } catch (_) {
      // Unsupported platforms are reported by the peer snapshot request.
    }
  }

  void toggleSimulation(bool enabled) {
    state = state.copyWith(
      simulationEnabled: enabled,
      nodes: enabled ? _simulatedNodes() : const [],
    );
  }

  Future<void> refreshPeers() => _loadRealPeers();

  Future<void> connectPeer(String peerId) async {
    state = state.copyWith(
      connectionStatus: 'connecting',
      connectedPeerId: peerId,
      clearConnectionError: true,
    );
    try {
      await ref.read(meshChannelProvider).connectPeer(peerId);
    } catch (error) {
      state = state.copyWith(
        connectionStatus: 'failed',
        connectionError: error.toString(),
      );
    }
  }

  Future<void> disconnectPeer() async {
    try {
      await ref.read(meshChannelProvider).disconnectPeer(
            state.connectedPeerId ?? 'connected',
          );
    } catch (error) {
      state = state.copyWith(connectionError: error.toString());
    }
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
