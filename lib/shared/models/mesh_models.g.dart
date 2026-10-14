// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'mesh_models.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_$PeerImpl _$$PeerImplFromJson(Map<String, dynamic> json) => _$PeerImpl(
      id: json['id'] as String,
      deviceName: json['deviceName'] as String,
      batteryLevel: (json['batteryLevel'] as num).toDouble(),
      signalStrength: (json['signalStrength'] as num).toDouble(),
      bandwidthKbps: (json['bandwidthKbps'] as num).toDouble(),
      latencyMs: (json['latencyMs'] as num).toDouble(),
      packetLoss: (json['packetLoss'] as num).toDouble(),
      load: (json['load'] as num).toDouble(),
      lastSeen: DateTime.parse(json['lastSeen'] as String),
      status: $enumDecodeNullable(_$PeerStatusEnumMap, json['status']) ??
          PeerStatus.unknown,
    );

Map<String, dynamic> _$$PeerImplToJson(_$PeerImpl instance) =>
    <String, dynamic>{
      'id': instance.id,
      'deviceName': instance.deviceName,
      'batteryLevel': instance.batteryLevel,
      'signalStrength': instance.signalStrength,
      'bandwidthKbps': instance.bandwidthKbps,
      'latencyMs': instance.latencyMs,
      'packetLoss': instance.packetLoss,
      'load': instance.load,
      'lastSeen': instance.lastSeen.toIso8601String(),
      'status': _$PeerStatusEnumMap[instance.status]!,
    };

const _$PeerStatusEnumMap = {
  PeerStatus.nearby: 'nearby',
  PeerStatus.connected: 'connected',
  PeerStatus.disconnected: 'disconnected',
  PeerStatus.unknown: 'unknown',
};

_$LinkImpl _$$LinkImplFromJson(Map<String, dynamic> json) => _$LinkImpl(
      source: json['source'] as String,
      destination: json['destination'] as String,
      cost: (json['cost'] as num).toDouble(),
      signalStrength: (json['signalStrength'] as num).toDouble(),
      observedAt: DateTime.parse(json['observedAt'] as String),
    );

Map<String, dynamic> _$$LinkImplToJson(_$LinkImpl instance) =>
    <String, dynamic>{
      'source': instance.source,
      'destination': instance.destination,
      'cost': instance.cost,
      'signalStrength': instance.signalStrength,
      'observedAt': instance.observedAt.toIso8601String(),
    };

_$MeshRouteImpl _$$MeshRouteImplFromJson(Map<String, dynamic> json) =>
    _$MeshRouteImpl(
      destination: json['destination'] as String,
      hops: (json['hops'] as List<dynamic>).map((e) => e as String).toList(),
      totalCost: (json['totalCost'] as num).toDouble(),
    );

Map<String, dynamic> _$$MeshRouteImplToJson(_$MeshRouteImpl instance) =>
    <String, dynamic>{
      'destination': instance.destination,
      'hops': instance.hops,
      'totalCost': instance.totalCost,
    };

_$MeshMessageImpl _$$MeshMessageImplFromJson(Map<String, dynamic> json) =>
    _$MeshMessageImpl(
      id: json['id'] as String,
      source: json['source'] as String,
      destination: json['destination'] as String,
      payload: json['payload'] as String,
      ttl: (json['ttl'] as num).toInt(),
      retryCount: (json['retryCount'] as num).toInt(),
      timestamp: DateTime.parse(json['timestamp'] as String),
      status: json['status'] as String? ?? 'queued',
    );

Map<String, dynamic> _$$MeshMessageImplToJson(_$MeshMessageImpl instance) =>
    <String, dynamic>{
      'id': instance.id,
      'source': instance.source,
      'destination': instance.destination,
      'payload': instance.payload,
      'ttl': instance.ttl,
      'retryCount': instance.retryCount,
      'timestamp': instance.timestamp.toIso8601String(),
      'status': instance.status,
    };

_$MeshSnapshotImpl _$$MeshSnapshotImplFromJson(Map<String, dynamic> json) =>
    _$MeshSnapshotImpl(
      running: json['running'] as bool? ?? false,
      peers: (json['peers'] as List<dynamic>?)
              ?.map((e) => Peer.fromJson(e as Map<String, dynamic>))
              .toList() ??
          const <Peer>[],
      links: (json['links'] as List<dynamic>?)
              ?.map((e) => Link.fromJson(e as Map<String, dynamic>))
              .toList() ??
          const <Link>[],
    );

Map<String, dynamic> _$$MeshSnapshotImplToJson(_$MeshSnapshotImpl instance) =>
    <String, dynamic>{
      'running': instance.running,
      'peers': instance.peers,
      'links': instance.links,
    };
