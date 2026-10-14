import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../mesh_channel/mesh_channel.dart';
import '../../shared/models/mesh_models.dart';

/// Provides the native mesh boundary to the application.
final meshChannelProvider = Provider<MeshChannel>((ref) => MeshChannel());

/// Exposes the current native mesh snapshot to Flutter widgets.
final meshSnapshotProvider = FutureProvider<MeshSnapshot>((ref) async {
  final peers = await ref.watch(meshChannelProvider).getPeers();
  return MeshSnapshot(peers: peers);
});
