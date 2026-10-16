import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../infra/api/mesh_dashboard_controller.dart';
import '../../services/repositories/mesh_repository.dart';

/// Single-hop text messaging screen.
class MessagesScreen extends ConsumerStatefulWidget {
  const MessagesScreen({super.key});

  @override
  ConsumerState<MessagesScreen> createState() => _MessagesScreenState();
}

class _MessagesScreenState extends ConsumerState<MessagesScreen> {
  final _destination = TextEditingController();
  final _payload = TextEditingController();
  bool _sending = false;

  @override
  void dispose() {
    _destination.dispose();
    _payload.dispose();
    super.dispose();
  }

  Future<void> _send() async {
    if (_destination.text.trim().isEmpty || _payload.text.isEmpty) return;
    setState(() => _sending = true);
    try {
      await ref.read(meshChannelProvider).sendMessage(
            destination: _destination.text.trim(),
            payload: _payload.text,
          );
      _payload.clear();
    } finally {
      if (mounted) setState(() => _sending = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(meshDashboardProvider);
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        Text('Offline Messages', style: Theme.of(context).textTheme.headlineSmall),
        const SizedBox(height: 6),
        const Text('Single-hop text over the active Wi-Fi Direct connection.'),
        const SizedBox(height: 16),
        TextField(
          controller: _destination,
          decoration: const InputDecoration(
            labelText: 'Destination peer ID',
            border: OutlineInputBorder(),
          ),
        ),
        const SizedBox(height: 10),
        TextField(
          controller: _payload,
          minLines: 2,
          maxLines: 5,
          decoration: const InputDecoration(
            labelText: 'Message',
            border: OutlineInputBorder(),
          ),
        ),
        const SizedBox(height: 10),
        FilledButton.icon(
          onPressed: _sending ? null : _send,
          icon: const Icon(Icons.send),
          label: Text(_sending ? 'Sending…' : 'Send offline message'),
        ),
        const SizedBox(height: 18),
        const Text('Transport events', style: TextStyle(fontWeight: FontWeight.bold)),
        const SizedBox(height: 6),
        if (state.messageLog.isEmpty)
          const Card(child: ListTile(title: Text('No message events yet.')))
        else
          ...state.messageLog.map((message) => Card(
                child: ListTile(
                  dense: true,
                  leading: const Icon(Icons.message_outlined),
                  title: Text(message),
                ),
              )),
      ],
    );
  }
}
