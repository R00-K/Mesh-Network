import 'package:freezed_annotation/freezed_annotation.dart';

part 'mesh_models.freezed.dart';
part 'mesh_models.g.dart';

/// Connectivity state reported by the native mesh service.
enum PeerStatus { nearby, connected, disconnected, unknown }

/// A peer visible to the local Android mesh service.
@freezed
class Peer with _$Peer {
  const factory Peer({
    required String id,
    required String deviceName,
    required double batteryLevel,
    required double signalStrength,
    required double bandwidthKbps,
    required double latencyMs,
    required double packetLoss,
    required double load,
    required DateTime lastSeen,
    @Default(PeerStatus.unknown) PeerStatus status,
  }) = _Peer;

  factory Peer.fromJson(Map<String, dynamic> json) => _$PeerFromJson(json);
}

/// A measured link between two mesh nodes.
@freezed
class Link with _$Link {
  const factory Link({
    required String source,
    required String destination,
    required double cost,
    required double signalStrength,
    required DateTime observedAt,
  }) = _Link;

  factory Link.fromJson(Map<String, dynamic> json) => _$LinkFromJson(json);
}

/// A route selected by the native routing engine.
@freezed
class MeshRoute with _$MeshRoute {
  const factory MeshRoute({
    required String destination,
    required List<String> hops,
    required double totalCost,
  }) = _MeshRoute;

  factory MeshRoute.fromJson(Map<String, dynamic> json) =>
      _$MeshRouteFromJson(json);
}

/// A message exchanged by the mesh transport.
@freezed
class MeshMessage with _$MeshMessage {
  const factory MeshMessage({
    required String id,
    required String source,
    required String destination,
    required String payload,
    required int ttl,
    required int retryCount,
    required DateTime timestamp,
    @Default('queued') String status,
  }) = _MeshMessage;

  factory MeshMessage.fromJson(Map<String, dynamic> json) =>
      _$MeshMessageFromJson(json);
}

/// Aggregate status exposed by the native service.
@freezed
class MeshSnapshot with _$MeshSnapshot {
  const factory MeshSnapshot({
    @Default(false) bool running,
    @Default(<Peer>[]) List<Peer> peers,
    @Default(<Link>[]) List<Link> links,
  }) = _MeshSnapshot;

  factory MeshSnapshot.fromJson(Map<String, dynamic> json) =>
      _$MeshSnapshotFromJson(json);
}
