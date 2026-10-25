import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../infra/api/mesh_dashboard_controller.dart';
import '../../shared/models/mesh_models.dart';

/// Nearby and connected device screen.
class MeshScreen extends ConsumerWidget {
  const MeshScreen({super.key});

  Future<void> _connect(
      BuildContext context, WidgetRef ref, String address) async {
    try {
      await ref.read(meshDashboardProvider.notifier).connectPeer(address);
    } catch (error) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Could not connect to peer: $error')),
        );
      }
    }
  }

  Future<void> _disconnect(BuildContext context, WidgetRef ref) async {
    try {
      await ref.read(meshDashboardProvider.notifier).disconnectPeer();
    } catch (error) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Could not disconnect: $error')),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(meshDashboardProvider);
    final colors = Theme.of(context).colorScheme;
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        Text('Mesh Nodes', style: Theme.of(context).textTheme.headlineSmall),
        const SizedBox(height: 6),
        Text(
          'Discovery: ${state.isDiscovering ? 'active' : 'inactive'}  •  '
          'Connected peers: ${state.connectedPeerCount}',
        ),
        if (state.discoveryError.isNotEmpty) ...[
          const SizedBox(height: 8),
          Text(state.discoveryError, style: TextStyle(color: colors.error)),
        ],
        const SizedBox(height: 16),
        Align(
          alignment: Alignment.centerLeft,
          child: OutlinedButton.icon(
            onPressed: () async {
              try {
                await ref.read(meshDashboardProvider.notifier).startDiscovery();
              } catch (error) {
                if (context.mounted) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(content: Text('Could not start discovery: $error')),
                  );
                }
              }
            },
            icon: const Icon(Icons.wifi_find),
            label: Text(state.isDiscovering ? 'Scan again' : 'Start discovery'),
          ),
        ),
        const SizedBox(height: 10),
        const Text('Discovered Wi-Fi Direct devices',
            style: TextStyle(fontWeight: FontWeight.bold)),
        const SizedBox(height: 6),
        if (state.nativePeers.isEmpty)
          const Card(child: ListTile(title: Text('No real peers discovered')))
        else
          ...state.nativePeers.map((peer) {
            final peerAge = DateTime.now().difference(peer.lastSeen);
            final isCurrentPeer = state.connectionPeerAddress == peer.id;
            final transportReady = isCurrentPeer &&
                state.transportState == 'connected' &&
                state.connectionPeerNodeId.isNotEmpty;
            final isConnecting = isCurrentPeer &&
                (state.connectionState == 'connecting' ||
                    state.connectionState == 'connected' &&
                        state.transportState != 'failed' &&
                        state.transportState != 'disconnected' &&
                        !transportReady);
            final isConnected = transportReady ||
                peer.status == PeerStatus.connected && isCurrentPeer;
            return Card(
              child: ListTile(
                leading: CircleAvatar(
                  backgroundColor:
                      isConnected ? colors.primary : colors.surfaceContainerHighest,
                  child: Icon(
                    Icons.wifi,
                    color: isConnected ? colors.onPrimary : colors.onSurfaceVariant,
                  ),
                ),
                title: Text(peer.deviceName),
                subtitle: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('Wi-Fi Direct address: ${peer.id}'),
                    if (peerAge.inSeconds >= 5)
                      Text('Last seen ${peerAge.inSeconds}s ago', softWrap: true),
                    if (transportReady)
                      Text('Node ID: ${state.connectionPeerNodeId}')
                    else if (isConnecting)
                      Text(
                        state.transportError.isNotEmpty
                            ? 'Retrying connection: ${state.transportError}'
                            : state.connectionState == 'connecting' &&
                                    state.transportState == 'disconnected'
                                ? 'Waiting for Wi-Fi Direct negotiation. Accept the system connection request on the other phone if prompted.'
                                : state.transportState == 'listening'
                                    ? 'Waiting for the peer messaging connection and identity check…'
                                    : state.transportState == 'connecting'
                                        ? 'Connecting the messaging transport…'
                                        : 'Verifying peer identity…',
                        softWrap: true,
                      )                    else if (isCurrentPeer &&
                        state.connectionState == 'connected' &&
                        state.transportState == 'failed')
                      Text(
                        state.transportError.isEmpty
                            ? 'Messaging link failed. Disconnect, then reconnect.'
                            : 'Messaging link failed: ${state.transportError}',
                      )
                    else if (isCurrentPeer &&
                        state.connectionState == 'connected' &&
                        state.transportState == 'disconnected')
                      const Text('Messaging link is not connected. Disconnect, then reconnect.')
                    else if (isCurrentPeer && state.connectionState == 'failed')
                      Text(state.transportError.isEmpty
                          ? 'Connection failed. Try again.'
                          : state.transportError),
                    const SizedBox(height: 8),
                    Wrap(
                      spacing: 4,
                      runSpacing: 4,
                      children: [
                        if (transportReady)
                          TextButton(
                            onPressed: () => context.go(
                              '/messages?destination=${Uri.encodeComponent(peer.id)}',
                            ),
                            child: const Text('Message'),
                          )
                        else if (isConnecting)
                          const Padding(
                            padding: EdgeInsets.all(12),
                            child: SizedBox(
                              width: 18,
                              height: 18,
                              child: CircularProgressIndicator(strokeWidth: 2),
                            ),
                          )
                        else
                          TextButton(
                            onPressed: () => _connect(context, ref, peer.id),
                            child: const Text('Connect'),
                          ),
                        if (isCurrentPeer &&
                            state.connectionState != 'disconnected')
                          TextButton(
                            onPressed: () => _disconnect(context, ref),
                            child: const Text('Disconnect'),
                          ),
                      ],
                    ),
                  ],
                ),
                isThreeLine: transportReady || isConnecting,
              ),
            );
          }),
      ],
    );
  }
}
