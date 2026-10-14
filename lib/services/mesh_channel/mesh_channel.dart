import 'package:flutter/services.dart';

import '../../shared/models/mesh_models.dart';

/// Typed Flutter boundary for the native Android mesh service.
class MeshChannel {
  MeshChannel({MethodChannel? methodChannel, EventChannel? eventChannel})
      : _methods = methodChannel ?? const MethodChannel(_methodName),
        _events = eventChannel ?? const EventChannel(_eventName);

  static const _methodName = 'disaster_mesh/mesh';
  static const _eventName = 'disaster_mesh/events';

  final MethodChannel _methods;
  final EventChannel _events;
  late final Stream<Map<String, dynamic>> _eventStream =
      _events.receiveBroadcastStream().map(
            (event) => Map<String, dynamic>.from(event as Map),
          ).asBroadcastStream();

  /// Calls a native mesh command.
  Future<T?> invoke<T>(String method, [Object? arguments]) {
    return _methods.invokeMethod<T>(method, arguments);
  }

  /// Starts the native mesh service.
  Future<void> startMesh() async => invoke<void>('startMesh');

  /// Stops the native mesh service.
  Future<void> stopMesh() async => invoke<void>('stopMesh');

  /// Requests nearby peer discovery.
  Future<void> discoverPeers() async => invoke<void>('discoverPeers');

  Future<String> getDiscoveryStatus() async =>
      await invoke<String>('getDiscoveryStatus') ?? 'unknown';

  Future<void> connectPeer(String peerId) async =>
      invoke<void>('connectPeer', {'peerId': peerId});

  Future<void> disconnectPeer(String peerId) async =>
      invoke<void>('disconnectPeer', {'peerId': peerId});

  Future<Map<String, dynamic>> sendMessage(String text) async {
    final result = await invoke<Map<dynamic, dynamic>>(
      'sendMessage',
      {'text': text},
    );
    return Map<String, dynamic>.from(result ?? const {});
  }

  Future<Map<String, dynamic>> getConnectionStatus() async {
    final result = await invoke<Map<dynamic, dynamic>>('getConnectionStatus');
    return Map<String, dynamic>.from(result ?? const {});
  }

  Future<List<Map<String, dynamic>>> getMessages() async {
    final result = await invoke<List<dynamic>>('getMessages') ?? const [];
    return result
        .whereType<Map<dynamic, dynamic>>()
        .map((message) => Map<String, dynamic>.from(message))
        .toList(growable: false);
  }

  /// Reads the current peer list from Android.
  Future<List<Peer>> getPeers() async {
    final value = await invoke<List<dynamic>>('getPeers') ?? const [];
    return value
        .whereType<Map<dynamic, dynamic>>()
        .map((json) => Peer.fromJson(Map<String, dynamic>.from(json)))
        .toList(growable: false);
  }

  /// Subscribes to native peer, metric, route, message, and status events.
  Stream<Map<String, dynamic>> get events => _eventStream;
}
