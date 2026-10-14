// coverage:ignore-file
// GENERATED CODE - DO NOT MODIFY BY HAND
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'mesh_models.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

T _$identity<T>(T value) => value;

final _privateConstructorUsedError = UnsupportedError(
    'It seems like you constructed your class using `MyClass._()`. This constructor is only meant to be used by freezed and you are not supposed to need it nor use it.\nPlease check the documentation here for more information: https://github.com/rrousselGit/freezed#adding-getters-and-methods-to-our-models');

Peer _$PeerFromJson(Map<String, dynamic> json) {
  return _Peer.fromJson(json);
}

/// @nodoc
mixin _$Peer {
  String get id => throw _privateConstructorUsedError;
  String get deviceName => throw _privateConstructorUsedError;
  double get batteryLevel => throw _privateConstructorUsedError;
  double get signalStrength => throw _privateConstructorUsedError;
  double get bandwidthKbps => throw _privateConstructorUsedError;
  double get latencyMs => throw _privateConstructorUsedError;
  double get packetLoss => throw _privateConstructorUsedError;
  double get load => throw _privateConstructorUsedError;
  DateTime get lastSeen => throw _privateConstructorUsedError;
  PeerStatus get status => throw _privateConstructorUsedError;

  /// Serializes this Peer to a JSON map.
  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;

  /// Create a copy of Peer
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $PeerCopyWith<Peer> get copyWith => throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $PeerCopyWith<$Res> {
  factory $PeerCopyWith(Peer value, $Res Function(Peer) then) =
      _$PeerCopyWithImpl<$Res, Peer>;
  @useResult
  $Res call(
      {String id,
      String deviceName,
      double batteryLevel,
      double signalStrength,
      double bandwidthKbps,
      double latencyMs,
      double packetLoss,
      double load,
      DateTime lastSeen,
      PeerStatus status});
}

/// @nodoc
class _$PeerCopyWithImpl<$Res, $Val extends Peer>
    implements $PeerCopyWith<$Res> {
  _$PeerCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of Peer
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? deviceName = null,
    Object? batteryLevel = null,
    Object? signalStrength = null,
    Object? bandwidthKbps = null,
    Object? latencyMs = null,
    Object? packetLoss = null,
    Object? load = null,
    Object? lastSeen = null,
    Object? status = null,
  }) {
    return _then(_value.copyWith(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as String,
      deviceName: null == deviceName
          ? _value.deviceName
          : deviceName // ignore: cast_nullable_to_non_nullable
              as String,
      batteryLevel: null == batteryLevel
          ? _value.batteryLevel
          : batteryLevel // ignore: cast_nullable_to_non_nullable
              as double,
      signalStrength: null == signalStrength
          ? _value.signalStrength
          : signalStrength // ignore: cast_nullable_to_non_nullable
              as double,
      bandwidthKbps: null == bandwidthKbps
          ? _value.bandwidthKbps
          : bandwidthKbps // ignore: cast_nullable_to_non_nullable
              as double,
      latencyMs: null == latencyMs
          ? _value.latencyMs
          : latencyMs // ignore: cast_nullable_to_non_nullable
              as double,
      packetLoss: null == packetLoss
          ? _value.packetLoss
          : packetLoss // ignore: cast_nullable_to_non_nullable
              as double,
      load: null == load
          ? _value.load
          : load // ignore: cast_nullable_to_non_nullable
              as double,
      lastSeen: null == lastSeen
          ? _value.lastSeen
          : lastSeen // ignore: cast_nullable_to_non_nullable
              as DateTime,
      status: null == status
          ? _value.status
          : status // ignore: cast_nullable_to_non_nullable
              as PeerStatus,
    ) as $Val);
  }
}

/// @nodoc
abstract class _$$PeerImplCopyWith<$Res> implements $PeerCopyWith<$Res> {
  factory _$$PeerImplCopyWith(
          _$PeerImpl value, $Res Function(_$PeerImpl) then) =
      __$$PeerImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {String id,
      String deviceName,
      double batteryLevel,
      double signalStrength,
      double bandwidthKbps,
      double latencyMs,
      double packetLoss,
      double load,
      DateTime lastSeen,
      PeerStatus status});
}

/// @nodoc
class __$$PeerImplCopyWithImpl<$Res>
    extends _$PeerCopyWithImpl<$Res, _$PeerImpl>
    implements _$$PeerImplCopyWith<$Res> {
  __$$PeerImplCopyWithImpl(_$PeerImpl _value, $Res Function(_$PeerImpl) _then)
      : super(_value, _then);

  /// Create a copy of Peer
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? deviceName = null,
    Object? batteryLevel = null,
    Object? signalStrength = null,
    Object? bandwidthKbps = null,
    Object? latencyMs = null,
    Object? packetLoss = null,
    Object? load = null,
    Object? lastSeen = null,
    Object? status = null,
  }) {
    return _then(_$PeerImpl(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as String,
      deviceName: null == deviceName
          ? _value.deviceName
          : deviceName // ignore: cast_nullable_to_non_nullable
              as String,
      batteryLevel: null == batteryLevel
          ? _value.batteryLevel
          : batteryLevel // ignore: cast_nullable_to_non_nullable
              as double,
      signalStrength: null == signalStrength
          ? _value.signalStrength
          : signalStrength // ignore: cast_nullable_to_non_nullable
              as double,
      bandwidthKbps: null == bandwidthKbps
          ? _value.bandwidthKbps
          : bandwidthKbps // ignore: cast_nullable_to_non_nullable
              as double,
      latencyMs: null == latencyMs
          ? _value.latencyMs
          : latencyMs // ignore: cast_nullable_to_non_nullable
              as double,
      packetLoss: null == packetLoss
          ? _value.packetLoss
          : packetLoss // ignore: cast_nullable_to_non_nullable
              as double,
      load: null == load
          ? _value.load
          : load // ignore: cast_nullable_to_non_nullable
              as double,
      lastSeen: null == lastSeen
          ? _value.lastSeen
          : lastSeen // ignore: cast_nullable_to_non_nullable
              as DateTime,
      status: null == status
          ? _value.status
          : status // ignore: cast_nullable_to_non_nullable
              as PeerStatus,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$PeerImpl implements _Peer {
  const _$PeerImpl(
      {required this.id,
      required this.deviceName,
      required this.batteryLevel,
      required this.signalStrength,
      required this.bandwidthKbps,
      required this.latencyMs,
      required this.packetLoss,
      required this.load,
      required this.lastSeen,
      this.status = PeerStatus.unknown});

  factory _$PeerImpl.fromJson(Map<String, dynamic> json) =>
      _$$PeerImplFromJson(json);

  @override
  final String id;
  @override
  final String deviceName;
  @override
  final double batteryLevel;
  @override
  final double signalStrength;
  @override
  final double bandwidthKbps;
  @override
  final double latencyMs;
  @override
  final double packetLoss;
  @override
  final double load;
  @override
  final DateTime lastSeen;
  @override
  @JsonKey()
  final PeerStatus status;

  @override
  String toString() {
    return 'Peer(id: $id, deviceName: $deviceName, batteryLevel: $batteryLevel, signalStrength: $signalStrength, bandwidthKbps: $bandwidthKbps, latencyMs: $latencyMs, packetLoss: $packetLoss, load: $load, lastSeen: $lastSeen, status: $status)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$PeerImpl &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.deviceName, deviceName) ||
                other.deviceName == deviceName) &&
            (identical(other.batteryLevel, batteryLevel) ||
                other.batteryLevel == batteryLevel) &&
            (identical(other.signalStrength, signalStrength) ||
                other.signalStrength == signalStrength) &&
            (identical(other.bandwidthKbps, bandwidthKbps) ||
                other.bandwidthKbps == bandwidthKbps) &&
            (identical(other.latencyMs, latencyMs) ||
                other.latencyMs == latencyMs) &&
            (identical(other.packetLoss, packetLoss) ||
                other.packetLoss == packetLoss) &&
            (identical(other.load, load) || other.load == load) &&
            (identical(other.lastSeen, lastSeen) ||
                other.lastSeen == lastSeen) &&
            (identical(other.status, status) || other.status == status));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(
      runtimeType,
      id,
      deviceName,
      batteryLevel,
      signalStrength,
      bandwidthKbps,
      latencyMs,
      packetLoss,
      load,
      lastSeen,
      status);

  /// Create a copy of Peer
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$PeerImplCopyWith<_$PeerImpl> get copyWith =>
      __$$PeerImplCopyWithImpl<_$PeerImpl>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$PeerImplToJson(
      this,
    );
  }
}

abstract class _Peer implements Peer {
  const factory _Peer(
      {required final String id,
      required final String deviceName,
      required final double batteryLevel,
      required final double signalStrength,
      required final double bandwidthKbps,
      required final double latencyMs,
      required final double packetLoss,
      required final double load,
      required final DateTime lastSeen,
      final PeerStatus status}) = _$PeerImpl;

  factory _Peer.fromJson(Map<String, dynamic> json) = _$PeerImpl.fromJson;

  @override
  String get id;
  @override
  String get deviceName;
  @override
  double get batteryLevel;
  @override
  double get signalStrength;
  @override
  double get bandwidthKbps;
  @override
  double get latencyMs;
  @override
  double get packetLoss;
  @override
  double get load;
  @override
  DateTime get lastSeen;
  @override
  PeerStatus get status;

  /// Create a copy of Peer
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$PeerImplCopyWith<_$PeerImpl> get copyWith =>
      throw _privateConstructorUsedError;
}

Link _$LinkFromJson(Map<String, dynamic> json) {
  return _Link.fromJson(json);
}

/// @nodoc
mixin _$Link {
  String get source => throw _privateConstructorUsedError;
  String get destination => throw _privateConstructorUsedError;
  double get cost => throw _privateConstructorUsedError;
  double get signalStrength => throw _privateConstructorUsedError;
  DateTime get observedAt => throw _privateConstructorUsedError;

  /// Serializes this Link to a JSON map.
  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;

  /// Create a copy of Link
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $LinkCopyWith<Link> get copyWith => throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $LinkCopyWith<$Res> {
  factory $LinkCopyWith(Link value, $Res Function(Link) then) =
      _$LinkCopyWithImpl<$Res, Link>;
  @useResult
  $Res call(
      {String source,
      String destination,
      double cost,
      double signalStrength,
      DateTime observedAt});
}

/// @nodoc
class _$LinkCopyWithImpl<$Res, $Val extends Link>
    implements $LinkCopyWith<$Res> {
  _$LinkCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of Link
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? source = null,
    Object? destination = null,
    Object? cost = null,
    Object? signalStrength = null,
    Object? observedAt = null,
  }) {
    return _then(_value.copyWith(
      source: null == source
          ? _value.source
          : source // ignore: cast_nullable_to_non_nullable
              as String,
      destination: null == destination
          ? _value.destination
          : destination // ignore: cast_nullable_to_non_nullable
              as String,
      cost: null == cost
          ? _value.cost
          : cost // ignore: cast_nullable_to_non_nullable
              as double,
      signalStrength: null == signalStrength
          ? _value.signalStrength
          : signalStrength // ignore: cast_nullable_to_non_nullable
              as double,
      observedAt: null == observedAt
          ? _value.observedAt
          : observedAt // ignore: cast_nullable_to_non_nullable
              as DateTime,
    ) as $Val);
  }
}

/// @nodoc
abstract class _$$LinkImplCopyWith<$Res> implements $LinkCopyWith<$Res> {
  factory _$$LinkImplCopyWith(
          _$LinkImpl value, $Res Function(_$LinkImpl) then) =
      __$$LinkImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {String source,
      String destination,
      double cost,
      double signalStrength,
      DateTime observedAt});
}

/// @nodoc
class __$$LinkImplCopyWithImpl<$Res>
    extends _$LinkCopyWithImpl<$Res, _$LinkImpl>
    implements _$$LinkImplCopyWith<$Res> {
  __$$LinkImplCopyWithImpl(_$LinkImpl _value, $Res Function(_$LinkImpl) _then)
      : super(_value, _then);

  /// Create a copy of Link
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? source = null,
    Object? destination = null,
    Object? cost = null,
    Object? signalStrength = null,
    Object? observedAt = null,
  }) {
    return _then(_$LinkImpl(
      source: null == source
          ? _value.source
          : source // ignore: cast_nullable_to_non_nullable
              as String,
      destination: null == destination
          ? _value.destination
          : destination // ignore: cast_nullable_to_non_nullable
              as String,
      cost: null == cost
          ? _value.cost
          : cost // ignore: cast_nullable_to_non_nullable
              as double,
      signalStrength: null == signalStrength
          ? _value.signalStrength
          : signalStrength // ignore: cast_nullable_to_non_nullable
              as double,
      observedAt: null == observedAt
          ? _value.observedAt
          : observedAt // ignore: cast_nullable_to_non_nullable
              as DateTime,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$LinkImpl implements _Link {
  const _$LinkImpl(
      {required this.source,
      required this.destination,
      required this.cost,
      required this.signalStrength,
      required this.observedAt});

  factory _$LinkImpl.fromJson(Map<String, dynamic> json) =>
      _$$LinkImplFromJson(json);

  @override
  final String source;
  @override
  final String destination;
  @override
  final double cost;
  @override
  final double signalStrength;
  @override
  final DateTime observedAt;

  @override
  String toString() {
    return 'Link(source: $source, destination: $destination, cost: $cost, signalStrength: $signalStrength, observedAt: $observedAt)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$LinkImpl &&
            (identical(other.source, source) || other.source == source) &&
            (identical(other.destination, destination) ||
                other.destination == destination) &&
            (identical(other.cost, cost) || other.cost == cost) &&
            (identical(other.signalStrength, signalStrength) ||
                other.signalStrength == signalStrength) &&
            (identical(other.observedAt, observedAt) ||
                other.observedAt == observedAt));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(
      runtimeType, source, destination, cost, signalStrength, observedAt);

  /// Create a copy of Link
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$LinkImplCopyWith<_$LinkImpl> get copyWith =>
      __$$LinkImplCopyWithImpl<_$LinkImpl>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$LinkImplToJson(
      this,
    );
  }
}

abstract class _Link implements Link {
  const factory _Link(
      {required final String source,
      required final String destination,
      required final double cost,
      required final double signalStrength,
      required final DateTime observedAt}) = _$LinkImpl;

  factory _Link.fromJson(Map<String, dynamic> json) = _$LinkImpl.fromJson;

  @override
  String get source;
  @override
  String get destination;
  @override
  double get cost;
  @override
  double get signalStrength;
  @override
  DateTime get observedAt;

  /// Create a copy of Link
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$LinkImplCopyWith<_$LinkImpl> get copyWith =>
      throw _privateConstructorUsedError;
}

MeshRoute _$MeshRouteFromJson(Map<String, dynamic> json) {
  return _MeshRoute.fromJson(json);
}

/// @nodoc
mixin _$MeshRoute {
  String get destination => throw _privateConstructorUsedError;
  List<String> get hops => throw _privateConstructorUsedError;
  double get totalCost => throw _privateConstructorUsedError;

  /// Serializes this MeshRoute to a JSON map.
  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;

  /// Create a copy of MeshRoute
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $MeshRouteCopyWith<MeshRoute> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $MeshRouteCopyWith<$Res> {
  factory $MeshRouteCopyWith(MeshRoute value, $Res Function(MeshRoute) then) =
      _$MeshRouteCopyWithImpl<$Res, MeshRoute>;
  @useResult
  $Res call({String destination, List<String> hops, double totalCost});
}

/// @nodoc
class _$MeshRouteCopyWithImpl<$Res, $Val extends MeshRoute>
    implements $MeshRouteCopyWith<$Res> {
  _$MeshRouteCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of MeshRoute
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? destination = null,
    Object? hops = null,
    Object? totalCost = null,
  }) {
    return _then(_value.copyWith(
      destination: null == destination
          ? _value.destination
          : destination // ignore: cast_nullable_to_non_nullable
              as String,
      hops: null == hops
          ? _value.hops
          : hops // ignore: cast_nullable_to_non_nullable
              as List<String>,
      totalCost: null == totalCost
          ? _value.totalCost
          : totalCost // ignore: cast_nullable_to_non_nullable
              as double,
    ) as $Val);
  }
}

/// @nodoc
abstract class _$$MeshRouteImplCopyWith<$Res>
    implements $MeshRouteCopyWith<$Res> {
  factory _$$MeshRouteImplCopyWith(
          _$MeshRouteImpl value, $Res Function(_$MeshRouteImpl) then) =
      __$$MeshRouteImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call({String destination, List<String> hops, double totalCost});
}

/// @nodoc
class __$$MeshRouteImplCopyWithImpl<$Res>
    extends _$MeshRouteCopyWithImpl<$Res, _$MeshRouteImpl>
    implements _$$MeshRouteImplCopyWith<$Res> {
  __$$MeshRouteImplCopyWithImpl(
      _$MeshRouteImpl _value, $Res Function(_$MeshRouteImpl) _then)
      : super(_value, _then);

  /// Create a copy of MeshRoute
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? destination = null,
    Object? hops = null,
    Object? totalCost = null,
  }) {
    return _then(_$MeshRouteImpl(
      destination: null == destination
          ? _value.destination
          : destination // ignore: cast_nullable_to_non_nullable
              as String,
      hops: null == hops
          ? _value._hops
          : hops // ignore: cast_nullable_to_non_nullable
              as List<String>,
      totalCost: null == totalCost
          ? _value.totalCost
          : totalCost // ignore: cast_nullable_to_non_nullable
              as double,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$MeshRouteImpl implements _MeshRoute {
  const _$MeshRouteImpl(
      {required this.destination,
      required final List<String> hops,
      required this.totalCost})
      : _hops = hops;

  factory _$MeshRouteImpl.fromJson(Map<String, dynamic> json) =>
      _$$MeshRouteImplFromJson(json);

  @override
  final String destination;
  final List<String> _hops;
  @override
  List<String> get hops {
    if (_hops is EqualUnmodifiableListView) return _hops;
    // ignore: implicit_dynamic_type
    return EqualUnmodifiableListView(_hops);
  }

  @override
  final double totalCost;

  @override
  String toString() {
    return 'MeshRoute(destination: $destination, hops: $hops, totalCost: $totalCost)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$MeshRouteImpl &&
            (identical(other.destination, destination) ||
                other.destination == destination) &&
            const DeepCollectionEquality().equals(other._hops, _hops) &&
            (identical(other.totalCost, totalCost) ||
                other.totalCost == totalCost));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(runtimeType, destination,
      const DeepCollectionEquality().hash(_hops), totalCost);

  /// Create a copy of MeshRoute
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$MeshRouteImplCopyWith<_$MeshRouteImpl> get copyWith =>
      __$$MeshRouteImplCopyWithImpl<_$MeshRouteImpl>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$MeshRouteImplToJson(
      this,
    );
  }
}

abstract class _MeshRoute implements MeshRoute {
  const factory _MeshRoute(
      {required final String destination,
      required final List<String> hops,
      required final double totalCost}) = _$MeshRouteImpl;

  factory _MeshRoute.fromJson(Map<String, dynamic> json) =
      _$MeshRouteImpl.fromJson;

  @override
  String get destination;
  @override
  List<String> get hops;
  @override
  double get totalCost;

  /// Create a copy of MeshRoute
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$MeshRouteImplCopyWith<_$MeshRouteImpl> get copyWith =>
      throw _privateConstructorUsedError;
}

MeshMessage _$MeshMessageFromJson(Map<String, dynamic> json) {
  return _MeshMessage.fromJson(json);
}

/// @nodoc
mixin _$MeshMessage {
  String get id => throw _privateConstructorUsedError;
  String get source => throw _privateConstructorUsedError;
  String get destination => throw _privateConstructorUsedError;
  String get payload => throw _privateConstructorUsedError;
  int get ttl => throw _privateConstructorUsedError;
  int get retryCount => throw _privateConstructorUsedError;
  DateTime get timestamp => throw _privateConstructorUsedError;
  String get status => throw _privateConstructorUsedError;

  /// Serializes this MeshMessage to a JSON map.
  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;

  /// Create a copy of MeshMessage
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $MeshMessageCopyWith<MeshMessage> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $MeshMessageCopyWith<$Res> {
  factory $MeshMessageCopyWith(
          MeshMessage value, $Res Function(MeshMessage) then) =
      _$MeshMessageCopyWithImpl<$Res, MeshMessage>;
  @useResult
  $Res call(
      {String id,
      String source,
      String destination,
      String payload,
      int ttl,
      int retryCount,
      DateTime timestamp,
      String status});
}

/// @nodoc
class _$MeshMessageCopyWithImpl<$Res, $Val extends MeshMessage>
    implements $MeshMessageCopyWith<$Res> {
  _$MeshMessageCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of MeshMessage
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? source = null,
    Object? destination = null,
    Object? payload = null,
    Object? ttl = null,
    Object? retryCount = null,
    Object? timestamp = null,
    Object? status = null,
  }) {
    return _then(_value.copyWith(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as String,
      source: null == source
          ? _value.source
          : source // ignore: cast_nullable_to_non_nullable
              as String,
      destination: null == destination
          ? _value.destination
          : destination // ignore: cast_nullable_to_non_nullable
              as String,
      payload: null == payload
          ? _value.payload
          : payload // ignore: cast_nullable_to_non_nullable
              as String,
      ttl: null == ttl
          ? _value.ttl
          : ttl // ignore: cast_nullable_to_non_nullable
              as int,
      retryCount: null == retryCount
          ? _value.retryCount
          : retryCount // ignore: cast_nullable_to_non_nullable
              as int,
      timestamp: null == timestamp
          ? _value.timestamp
          : timestamp // ignore: cast_nullable_to_non_nullable
              as DateTime,
      status: null == status
          ? _value.status
          : status // ignore: cast_nullable_to_non_nullable
              as String,
    ) as $Val);
  }
}

/// @nodoc
abstract class _$$MeshMessageImplCopyWith<$Res>
    implements $MeshMessageCopyWith<$Res> {
  factory _$$MeshMessageImplCopyWith(
          _$MeshMessageImpl value, $Res Function(_$MeshMessageImpl) then) =
      __$$MeshMessageImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {String id,
      String source,
      String destination,
      String payload,
      int ttl,
      int retryCount,
      DateTime timestamp,
      String status});
}

/// @nodoc
class __$$MeshMessageImplCopyWithImpl<$Res>
    extends _$MeshMessageCopyWithImpl<$Res, _$MeshMessageImpl>
    implements _$$MeshMessageImplCopyWith<$Res> {
  __$$MeshMessageImplCopyWithImpl(
      _$MeshMessageImpl _value, $Res Function(_$MeshMessageImpl) _then)
      : super(_value, _then);

  /// Create a copy of MeshMessage
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? source = null,
    Object? destination = null,
    Object? payload = null,
    Object? ttl = null,
    Object? retryCount = null,
    Object? timestamp = null,
    Object? status = null,
  }) {
    return _then(_$MeshMessageImpl(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as String,
      source: null == source
          ? _value.source
          : source // ignore: cast_nullable_to_non_nullable
              as String,
      destination: null == destination
          ? _value.destination
          : destination // ignore: cast_nullable_to_non_nullable
              as String,
      payload: null == payload
          ? _value.payload
          : payload // ignore: cast_nullable_to_non_nullable
              as String,
      ttl: null == ttl
          ? _value.ttl
          : ttl // ignore: cast_nullable_to_non_nullable
              as int,
      retryCount: null == retryCount
          ? _value.retryCount
          : retryCount // ignore: cast_nullable_to_non_nullable
              as int,
      timestamp: null == timestamp
          ? _value.timestamp
          : timestamp // ignore: cast_nullable_to_non_nullable
              as DateTime,
      status: null == status
          ? _value.status
          : status // ignore: cast_nullable_to_non_nullable
              as String,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$MeshMessageImpl implements _MeshMessage {
  const _$MeshMessageImpl(
      {required this.id,
      required this.source,
      required this.destination,
      required this.payload,
      required this.ttl,
      required this.retryCount,
      required this.timestamp,
      this.status = 'queued'});

  factory _$MeshMessageImpl.fromJson(Map<String, dynamic> json) =>
      _$$MeshMessageImplFromJson(json);

  @override
  final String id;
  @override
  final String source;
  @override
  final String destination;
  @override
  final String payload;
  @override
  final int ttl;
  @override
  final int retryCount;
  @override
  final DateTime timestamp;
  @override
  @JsonKey()
  final String status;

  @override
  String toString() {
    return 'MeshMessage(id: $id, source: $source, destination: $destination, payload: $payload, ttl: $ttl, retryCount: $retryCount, timestamp: $timestamp, status: $status)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$MeshMessageImpl &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.source, source) || other.source == source) &&
            (identical(other.destination, destination) ||
                other.destination == destination) &&
            (identical(other.payload, payload) || other.payload == payload) &&
            (identical(other.ttl, ttl) || other.ttl == ttl) &&
            (identical(other.retryCount, retryCount) ||
                other.retryCount == retryCount) &&
            (identical(other.timestamp, timestamp) ||
                other.timestamp == timestamp) &&
            (identical(other.status, status) || other.status == status));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(runtimeType, id, source, destination, payload,
      ttl, retryCount, timestamp, status);

  /// Create a copy of MeshMessage
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$MeshMessageImplCopyWith<_$MeshMessageImpl> get copyWith =>
      __$$MeshMessageImplCopyWithImpl<_$MeshMessageImpl>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$MeshMessageImplToJson(
      this,
    );
  }
}

abstract class _MeshMessage implements MeshMessage {
  const factory _MeshMessage(
      {required final String id,
      required final String source,
      required final String destination,
      required final String payload,
      required final int ttl,
      required final int retryCount,
      required final DateTime timestamp,
      final String status}) = _$MeshMessageImpl;

  factory _MeshMessage.fromJson(Map<String, dynamic> json) =
      _$MeshMessageImpl.fromJson;

  @override
  String get id;
  @override
  String get source;
  @override
  String get destination;
  @override
  String get payload;
  @override
  int get ttl;
  @override
  int get retryCount;
  @override
  DateTime get timestamp;
  @override
  String get status;

  /// Create a copy of MeshMessage
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$MeshMessageImplCopyWith<_$MeshMessageImpl> get copyWith =>
      throw _privateConstructorUsedError;
}

MeshSnapshot _$MeshSnapshotFromJson(Map<String, dynamic> json) {
  return _MeshSnapshot.fromJson(json);
}

/// @nodoc
mixin _$MeshSnapshot {
  bool get running => throw _privateConstructorUsedError;
  List<Peer> get peers => throw _privateConstructorUsedError;
  List<Link> get links => throw _privateConstructorUsedError;

  /// Serializes this MeshSnapshot to a JSON map.
  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;

  /// Create a copy of MeshSnapshot
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $MeshSnapshotCopyWith<MeshSnapshot> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $MeshSnapshotCopyWith<$Res> {
  factory $MeshSnapshotCopyWith(
          MeshSnapshot value, $Res Function(MeshSnapshot) then) =
      _$MeshSnapshotCopyWithImpl<$Res, MeshSnapshot>;
  @useResult
  $Res call({bool running, List<Peer> peers, List<Link> links});
}

/// @nodoc
class _$MeshSnapshotCopyWithImpl<$Res, $Val extends MeshSnapshot>
    implements $MeshSnapshotCopyWith<$Res> {
  _$MeshSnapshotCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of MeshSnapshot
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? running = null,
    Object? peers = null,
    Object? links = null,
  }) {
    return _then(_value.copyWith(
      running: null == running
          ? _value.running
          : running // ignore: cast_nullable_to_non_nullable
              as bool,
      peers: null == peers
          ? _value.peers
          : peers // ignore: cast_nullable_to_non_nullable
              as List<Peer>,
      links: null == links
          ? _value.links
          : links // ignore: cast_nullable_to_non_nullable
              as List<Link>,
    ) as $Val);
  }
}

/// @nodoc
abstract class _$$MeshSnapshotImplCopyWith<$Res>
    implements $MeshSnapshotCopyWith<$Res> {
  factory _$$MeshSnapshotImplCopyWith(
          _$MeshSnapshotImpl value, $Res Function(_$MeshSnapshotImpl) then) =
      __$$MeshSnapshotImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call({bool running, List<Peer> peers, List<Link> links});
}

/// @nodoc
class __$$MeshSnapshotImplCopyWithImpl<$Res>
    extends _$MeshSnapshotCopyWithImpl<$Res, _$MeshSnapshotImpl>
    implements _$$MeshSnapshotImplCopyWith<$Res> {
  __$$MeshSnapshotImplCopyWithImpl(
      _$MeshSnapshotImpl _value, $Res Function(_$MeshSnapshotImpl) _then)
      : super(_value, _then);

  /// Create a copy of MeshSnapshot
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? running = null,
    Object? peers = null,
    Object? links = null,
  }) {
    return _then(_$MeshSnapshotImpl(
      running: null == running
          ? _value.running
          : running // ignore: cast_nullable_to_non_nullable
              as bool,
      peers: null == peers
          ? _value._peers
          : peers // ignore: cast_nullable_to_non_nullable
              as List<Peer>,
      links: null == links
          ? _value._links
          : links // ignore: cast_nullable_to_non_nullable
              as List<Link>,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$MeshSnapshotImpl implements _MeshSnapshot {
  const _$MeshSnapshotImpl(
      {this.running = false,
      final List<Peer> peers = const <Peer>[],
      final List<Link> links = const <Link>[]})
      : _peers = peers,
        _links = links;

  factory _$MeshSnapshotImpl.fromJson(Map<String, dynamic> json) =>
      _$$MeshSnapshotImplFromJson(json);

  @override
  @JsonKey()
  final bool running;
  final List<Peer> _peers;
  @override
  @JsonKey()
  List<Peer> get peers {
    if (_peers is EqualUnmodifiableListView) return _peers;
    // ignore: implicit_dynamic_type
    return EqualUnmodifiableListView(_peers);
  }

  final List<Link> _links;
  @override
  @JsonKey()
  List<Link> get links {
    if (_links is EqualUnmodifiableListView) return _links;
    // ignore: implicit_dynamic_type
    return EqualUnmodifiableListView(_links);
  }

  @override
  String toString() {
    return 'MeshSnapshot(running: $running, peers: $peers, links: $links)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$MeshSnapshotImpl &&
            (identical(other.running, running) || other.running == running) &&
            const DeepCollectionEquality().equals(other._peers, _peers) &&
            const DeepCollectionEquality().equals(other._links, _links));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(
      runtimeType,
      running,
      const DeepCollectionEquality().hash(_peers),
      const DeepCollectionEquality().hash(_links));

  /// Create a copy of MeshSnapshot
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$MeshSnapshotImplCopyWith<_$MeshSnapshotImpl> get copyWith =>
      __$$MeshSnapshotImplCopyWithImpl<_$MeshSnapshotImpl>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$MeshSnapshotImplToJson(
      this,
    );
  }
}

abstract class _MeshSnapshot implements MeshSnapshot {
  const factory _MeshSnapshot(
      {final bool running,
      final List<Peer> peers,
      final List<Link> links}) = _$MeshSnapshotImpl;

  factory _MeshSnapshot.fromJson(Map<String, dynamic> json) =
      _$MeshSnapshotImpl.fromJson;

  @override
  bool get running;
  @override
  List<Peer> get peers;
  @override
  List<Link> get links;

  /// Create a copy of MeshSnapshot
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$MeshSnapshotImplCopyWith<_$MeshSnapshotImpl> get copyWith =>
      throw _privateConstructorUsedError;
}
