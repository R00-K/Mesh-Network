import 'dart:async';
import 'dart:math';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../services/repositories/mesh_repository.dart';
import '../../shared/models/mesh_models.dart';
import 'mesh_dashboard_models.dart';

final meshDashboardProvider =
    NotifierProvider.autoDispose<MeshDashboardController, MeshDashboardState>(
        MeshDashboardController.new);

/// Coordinates simulated dashboard data and the native mesh channel.
class MeshDashboardController extends AutoDisposeNotifier<MeshDashboardState>
    with WidgetsBindingObserver {
  Timer? _timer;
  Timer? _eventReconnectTimer;
  StreamSubscription<Map<String, dynamic>>? _eventsSubscription;
  final _observedMessageStatuses = <String, String>{};
  final _random = Random(7);
  bool _disposed = false;
  int _nativeRefreshRequest = 0;
  int _connectionEventRevision = 0;

  @override
  MeshDashboardState build() {
    _disposed = false;
    WidgetsBinding.instance.addObserver(this);
    ref.onDispose(() {
      _disposed = true;
      _timer?.cancel();
      _eventReconnectTimer?.cancel();
      _eventsSubscription?.cancel();
      WidgetsBinding.instance.removeObserver(this);
    });
    final nodes = _simulatedNodes();
    _timer = Timer.periodic(const Duration(seconds: 5), (_) => _tick());
    _subscribeToNativeEvents();
    unawaited(refreshNativeState());
    return MeshDashboardState(nodes: nodes);
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      unawaited(refreshNativeState());
      if (_eventsSubscription == null) _subscribeToNativeEvents();
    }
  }

  void _subscribeToNativeEvents() {
    if (_disposed) return;
    _eventReconnectTimer?.cancel();
    _eventsSubscription = ref.read(meshChannelProvider).events.listen(
      _handleNativeEvent,
      onError: (Object error, StackTrace stackTrace) {
        _scheduleEventReconnect();
      },
      onDone: _scheduleEventReconnect,
    );
  }

  void _scheduleEventReconnect() {
    _eventsSubscription?.cancel();
    _eventsSubscription = null;
    if (_disposed) return;
    _eventReconnectTimer?.cancel();
    _eventReconnectTimer = Timer(const Duration(seconds: 2), () {
      if (_disposed) return;
      _subscribeToNativeEvents();
      unawaited(refreshNativeState());
    });
  }

  Future<void> startMesh() async {
    state = state.copyWith(isActive: true, isDiscovering: true);
    try {
      await ref.read(meshChannelProvider).startMesh();
    } catch (_) {
      // The simulation remains available on unsupported platforms and tests.
    }
  }

  Future<void> startDiscovery() async {
    if (!state.isActive) {
      await startMesh();
      return;
    }
    await ref.read(meshChannelProvider).discoverPeers();
  }

  Future<void> refreshNativeState() async {
    final refreshRequest = ++_nativeRefreshRequest;
    final connectionRevision = _connectionEventRevision;
    try {
      final channel = ref.read(meshChannelProvider);
      final peers = await channel.getPeers();
      final messages = await channel.getMessages();
      final connection = await channel.getConnectionInfo();
      if (_disposed || refreshRequest != _nativeRefreshRequest) return;
      final connectionSnapshotIsCurrent =
          connectionRevision == _connectionEventRevision;
      final nativeAddress = connection['peerId'] as String? ?? '';
      final nodeId = connection['peerNodeId'] as String? ?? '';
      final transportState =
          connection['transportState'] as String? ?? state.transportState;
      final address = nativeAddress.isNotEmpty
          ? nativeAddress
          : transportState == 'connected'
              ? state.connectionPeerAddress
              : '';
      final connected = connection['isConnected'] == true ||
          transportState == 'connected' && nodeId.isNotEmpty;
      final peerNodeIds = Map<String, String>.from(state.peerNodeIds);
      if (connectionSnapshotIsCurrent &&
          address.isNotEmpty &&
          nodeId.isNotEmpty) {
        peerNodeIds[address] = nodeId;
      }
      final restoredById = <String, MeshChatMessage>{};
      for (final message in messages) {
        final id = message['id']?.toString() ?? '';
        final peerId = message['direction'] == 'incoming'
            ? message['source']?.toString() ?? ''
            : message['destination']?.toString() ?? '';
        if (id.isEmpty || peerId.isEmpty) continue;
        final incoming = message['direction'] == 'incoming';
        restoredById[id] = MeshChatMessage(
          id: id,
          peerId: peerId,
          text: message['payload']?.toString() ?? '',
          incoming: incoming,
          status: incoming
              ? 'received'
              : message['status']?.toString() ??
                  _observedMessageStatuses[id] ??
                  'queued',
        );
      }
      for (final existing in state.chatMessages) {
        restoredById.putIfAbsent(existing.id, () => existing);
      }
      state = state.copyWith(
        nativePeers: peers,
        chatMessages: restoredById.values.toList(growable: false),
        connectionState: connectionSnapshotIsCurrent
            ? connected
                ? 'connected'
                : address.isNotEmpty
                    ? 'connecting'
                    : 'disconnected'
            : state.connectionState,
        connectionPeerAddress: connectionSnapshotIsCurrent
            ? address
            : state.connectionPeerAddress,
        connectionPeerNodeId: connectionSnapshotIsCurrent
            ? nodeId
            : state.connectionPeerNodeId,
        transportState:
            connectionSnapshotIsCurrent ? transportState : state.transportState,
        transportError: connectionSnapshotIsCurrent
            ? connection['transportError'] as String? ?? state.transportError
            : state.transportError,
        peerNodeIds: peerNodeIds,
      );
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

  void addOutgoingMessage({
    required String id,
    required String peerId,
    required String text,
    required String status,
  }) {
    final currentStatus = _observedMessageStatuses[id] ?? status;
    final message = MeshChatMessage(
      id: id,
      peerId: peerId,
      text: text,
      incoming: false,
      status: currentStatus,
    );
    final existingIndex =
        state.chatMessages.indexWhere((item) => item.id == id);
    final chatMessages = [...state.chatMessages];
    if (existingIndex >= 0) {
      chatMessages[existingIndex] = message;
    } else {
      chatMessages.add(message);
    }
    state = state.copyWith(
      chatMessages: chatMessages,
    );
  }

  void _handleNativeEvent(Map<String, dynamic> event) {
    final payload = Map<String, dynamic>.from(event['payload'] as Map? ?? {});
    if (event['type'] == 'TransportChanged' ||
        event['type'] == 'ConnectionChanged' ||
        event['type'] == 'PeerIdentityChanged') {
      _connectionEventRevision++;
    }
    if (event['type'] == 'MeshStatusChanged') {
      state = state.copyWith(
        isActive: payload['running'] as bool? ?? state.isActive,
        isDiscovering: payload['discovering'] as bool? ?? state.isDiscovering,
      );
    } else if (event['type'] == 'TransportChanged') {
      final transportState = payload['state'] as String? ?? state.transportState;
      state = state.copyWith(
        transportState: transportState,
        connectionState: transportState == 'connected'
            ? 'connected'
            : state.connectionState,
        transportError: transportState == 'connecting' ||
                transportState == 'connected'
            ? ''
            : payload['error'] as String? ?? state.transportError,
      );
    } else if (event['type'] == 'ConnectionChanged') {
      var connectionState = payload['state'] as String? ?? 'unknown';
      final transportVerified = state.transportState == 'connected' &&
          state.connectionPeerNodeId.isNotEmpty;
      if (connectionState == 'connecting' && transportVerified) {
        connectionState = 'connected';
      }
      state = state.copyWith(
        connectionState: connectionState,
        connectionPeerAddress:
            connectionState == 'disconnected' || connectionState == 'failed'
                ? ''
                : payload['peerId'] as String? ?? state.connectionPeerAddress,
        connectionPeerNodeId: connectionState == 'connecting' ||
                connectionState == 'disconnected' ||
                connectionState == 'failed'
            ? ''
            : state.connectionPeerNodeId,
      );
    } else if (event['type'] == 'PeerIdentityChanged') {
      final nodeId = payload['nodeId'] as String? ?? '';
      final address =
          payload['transportAddress'] as String? ?? state.connectionPeerAddress;
      final peerNodeIds = Map<String, String>.from(state.peerNodeIds);
      if (address.isNotEmpty && nodeId.isNotEmpty) {
        peerNodeIds[address] = nodeId;
      }
      state = state.copyWith(
        connectionState: nodeId.isNotEmpty ? 'connected' : state.connectionState,
        connectionPeerNodeId: nodeId,
        connectionPeerAddress: address,
        peerNodeIds: peerNodeIds,
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
        chatMessages: state.chatMessages.any((item) => item.id == payload['id'])
            ? state.chatMessages
            : [
                ...state.chatMessages,
                MeshChatMessage(
                  id: payload['id'] as String? ??
                      DateTime.now().toIso8601String(),
                  peerId: source.toString(),
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
      _observedMessageStatuses[id.toString()] = status.toString();
      if (_observedMessageStatuses.length > 500) {
        _observedMessageStatuses.remove(_observedMessageStatuses.keys.first);
      }
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
          color: Color(0xff557b75),
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
