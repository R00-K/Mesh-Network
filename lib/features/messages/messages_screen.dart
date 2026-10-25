import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../infra/api/mesh_dashboard_controller.dart';
import '../../services/repositories/mesh_repository.dart';

/// Direct conversation with the currently identified Wi-Fi Direct peer.
class MessagesScreen extends ConsumerStatefulWidget {
  const MessagesScreen({super.key, this.destination});

  /// Wi-Fi Direct transport address selected on the mesh screen.
  final String? destination;

  @override
  ConsumerState<MessagesScreen> createState() => _MessagesScreenState();
}

class _MessagesScreenState extends ConsumerState<MessagesScreen> {
  final _payload = TextEditingController();
  bool _sending = false;

  @override
  void dispose() {
    _payload.dispose();
    super.dispose();
  }

  void _showMessage(String text) {
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(text)));
    }
  }

  Future<void> _send(String targetAddress) async {
    final current = ref.read(meshDashboardProvider);
    if (current.transportState != 'connected' ||
        current.connectionPeerAddress != targetAddress ||
        current.connectionPeerNodeId.isEmpty) {
      _showMessage(
          'This peer is not connected and verified. Reconnect from Mesh Nodes.');
      return;
    }
    final text = _payload.text;
    if (text.trim().isEmpty) {
      _showMessage('Enter a message before sending.');
      return;
    }
    setState(() => _sending = true);
    try {
      final result = await ref.read(meshChannelProvider).sendMessage(
            destination: targetAddress,
            payload: text,
          );
      final id = result?['id'] as String?;
      final status = result?['status'] as String? ?? 'queued';
      if (id == null) throw PlatformException(code: 'EMPTY_RESPONSE');
      ref.read(meshDashboardProvider.notifier).addOutgoingMessage(
            id: id,
            peerId: current.connectionPeerNodeId,
            text: text,
            status: status,
          );
      if (status == 'sent' || status == 'delivered') {
        _payload.clear();
      } else if (status == 'queued') {
        // Native storage has accepted the message into its durable outbox.
        // Clear the composer so it is not accidentally submitted twice.
        _payload.clear();
        _showMessage('Message saved to the outbox and will retry automatically.');
      } else {
        _showMessage('Message was not sent. Your draft is still here.');
      }
    } on PlatformException catch (error) {
      _showMessage(error.message ??
          'The message could not be sent. Your draft is still here.');
    } catch (error) {
      _showMessage('The message could not be sent: $error');
    } finally {
      if (mounted) setState(() => _sending = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(meshDashboardProvider);
    final targetAddress = widget.destination ?? state.connectionPeerAddress;
    var peerName = 'Selected peer';
    for (final candidate in state.nativePeers) {
      if (candidate.id == targetAddress) {
        peerName = candidate.deviceName;
        break;
      }
    }
    final connected = targetAddress.isNotEmpty &&
        state.transportState == 'connected' &&
        state.connectionPeerAddress == targetAddress &&
        state.connectionPeerNodeId.isNotEmpty;
    final conversationPeerId = connected
        ? state.connectionPeerNodeId
        : state.peerNodeIds[targetAddress] ?? '';
    final conversation = conversationPeerId.isEmpty
        ? const []
        : state.chatMessages
            .where((message) => message.peerId == conversationPeerId)
            .toList(growable: false);

    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        Text('Messages', style: Theme.of(context).textTheme.headlineSmall),
        const SizedBox(height: 6),
        const Text(
            'Direct text messages over one verified Wi-Fi Direct connection.'),
        const SizedBox(height: 14),
        Card(
          child: ListTile(
            leading: Icon(connected ? Icons.verified_user : Icons.wifi_off),
            title: Text(targetAddress.isEmpty ? 'No peer selected' : peerName),
            subtitle: Text(
              connected
                  ? 'Node ID: ${state.connectionPeerNodeId}'
                  : targetAddress.isEmpty
                      ? 'Select and connect to a discovered node first.'
                      : 'Connection: ${state.connectionState}; transport: ${state.transportState}'
                          '${state.transportError.isEmpty ? '' : '\n${state.transportError}'}',
            ),
            trailing: targetAddress.isEmpty
                ? TextButton(
                    onPressed: () => context.go('/mesh'),
                    child: const Text('Find a peer'),
                  )
                : null,
          ),
        ),
        const SizedBox(height: 12),
        TextField(
          controller: _payload,
          enabled: connected && !_sending,
          minLines: 2,
          maxLines: 5,
          decoration: const InputDecoration(
            labelText: 'Message',
            border: OutlineInputBorder(),
          ),
        ),
        const SizedBox(height: 10),
        FilledButton.icon(
          onPressed: connected && !_sending ? () => _send(targetAddress) : null,
          icon: const Icon(Icons.send),
          label: Text(_sending ? 'Sending…' : 'Send'),
        ),
        const SizedBox(height: 18),
        const Text('Conversation',
            style: TextStyle(fontWeight: FontWeight.bold)),
        const SizedBox(height: 6),
        if (targetAddress.isEmpty)
          const Card(
            child: ListTile(
              title: Text('Select a discovered peer from Mesh Nodes.'),
            ),
          )
        else if (conversation.isEmpty)
          const Card(
              child: ListTile(title: Text('No messages with this peer yet.')))
        else
          ...conversation.map((message) => Align(
                alignment: message.incoming
                    ? Alignment.centerLeft
                    : Alignment.centerRight,
                child: Container(
                  constraints: const BoxConstraints(maxWidth: 320),
                  margin: const EdgeInsets.only(bottom: 8),
                  padding:
                      const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
                  decoration: BoxDecoration(
                    color: message.incoming
                        ? Theme.of(context).colorScheme.surfaceContainerHighest
                        : Theme.of(context).colorScheme.primaryContainer,
                    borderRadius: BorderRadius.circular(16),
                  ),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.end,
                    children: [
                      Align(
                        alignment: Alignment.centerLeft,
                        child: Text(message.text),
                      ),
                      const SizedBox(height: 3),
                      Text(
                        message.status,
                        style: Theme.of(context).textTheme.labelSmall,
                      ),
                    ],
                  ),
                ),
              )),
      ],
    );
  }
}
