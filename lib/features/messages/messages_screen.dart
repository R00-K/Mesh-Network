import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../infra/api/mesh_dashboard_controller.dart';
import '../../services/mesh_channel/mesh_channel.dart';
import '../../services/repositories/mesh_repository.dart';

/// Live broadcast chat for the current Wi-Fi Direct group.
class MessagesScreen extends ConsumerStatefulWidget {
  const MessagesScreen({super.key});

  @override
  ConsumerState<MessagesScreen> createState() => _MessagesScreenState();
}

class _MessagesScreenState extends ConsumerState<MessagesScreen> {
  final _composer = TextEditingController();
  final _scroll = ScrollController();
  final List<_ChatMessage> _messages = [];
  late final MeshChannel _channel;
  StreamSubscription<Map<String, dynamic>>? _events;
  bool _sending = false;

  @override
  void initState() {
    super.initState();
    _channel = ref.read(meshChannelProvider);
    _events = _channel.events.listen(_onEvent, onError: (_) {});
    _loadHistory();
  }

  @override
  void dispose() {
    _events?.cancel();
    _composer.dispose();
    _scroll.dispose();
    super.dispose();
  }

  void _onEvent(Map<String, dynamic> event) {
    final type = event['type'];
    final payload = Map<String, dynamic>.from(event['payload'] as Map? ?? const {});
    if (type == 'MessageUpdated') {
      _upsert(payload);
    } else if (type == 'MessageTransportError') {
      final id = payload['id'] as String?;
      final index = _messages.indexWhere((message) => message.id == id);
      if (index >= 0) {
        setState(() => _messages[index] = _messages[index].copyWith(status: 'queued'));
      }
    }
  }

  Future<void> _loadHistory() async {
    try {
      final saved = await _channel.getMessages();
      if (!mounted) return;
      for (final message in saved) {
        _upsert(message, scroll: false);
      }
      _scrollToLatest();
    } catch (_) {
      // Native persistence is only available on Android.
    }
  }

  void _upsert(Map<String, dynamic> payload, {bool scroll = true}) {
    final id = payload['id'] as String?;
    if (id == null || !mounted) return;
    final index = _messages.indexWhere((message) => message.id == id);
    final timestamp = payload['timestamp'] ?? payload['sentAt'];
    final date = timestamp is num
        ? DateTime.fromMillisecondsSinceEpoch(timestamp.toInt())
        : timestamp is String
            ? DateTime.tryParse(timestamp) ?? DateTime.now()
            : DateTime.now();
    final message = _ChatMessage(
      id: id,
      text: payload['text'] as String? ?? '',
      outgoing: payload['direction'] == 'outgoing',
      status: payload['status'] as String? ?? 'queued',
      time: date,
      retryCount: (payload['retryCount'] as num?)?.toInt() ?? 0,
      ttl: (payload['ttl'] as num?)?.toInt() ?? 0,
    );
    setState(() {
      if (index < 0) {
        _messages.add(message);
      } else {
        _messages[index] = message;
      }
      _messages.sort((a, b) => a.time.compareTo(b.time));
    });
    if (scroll) _scrollToLatest();
  }

  Future<void> _send() async {
    final text = _composer.text.trim();
    if (text.isEmpty || _sending) return;
    setState(() => _sending = true);
    try {
      await _channel.sendMessage(text);
      _composer.clear();
    } catch (error) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Message was not sent: $error')),
        );
      }
    } finally {
      if (mounted) setState(() => _sending = false);
    }
  }

  void _scrollToLatest() {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (_scroll.hasClients) {
        _scroll.animateTo(
          _scroll.position.maxScrollExtent,
          duration: const Duration(milliseconds: 180),
          curve: Curves.easeOut,
        );
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    final mesh = ref.watch(meshDashboardProvider);
    final connected = mesh.connectionStatus == 'connected';
    return Column(
      children: [
        Material(
          color: Theme.of(context).colorScheme.surfaceContainerHighest,
          child: ListTile(
            leading: Icon(connected ? Icons.link : Icons.link_off),
            title: Text(connected ? 'Wi-Fi Direct link ready' : 'No device connected'),
            subtitle: Text(connected
                ? 'Group broadcast · group owner relays to connected clients'
                : 'Messages you send are saved in the outbox and retry when a link is available.'),
          ),
        ),
        Expanded(
          child: _messages.isEmpty
              ? const Center(child: Text('Your message history and offline outbox will appear here.'))
              : ListView.builder(
                  controller: _scroll,
                  padding: const EdgeInsets.all(16),
                  itemCount: _messages.length,
                  itemBuilder: (context, index) {
                    final message = _messages[index];
                    return Align(
                      alignment: message.outgoing ? Alignment.centerRight : Alignment.centerLeft,
                      child: Card(
                        color: message.outgoing
                            ? Theme.of(context).colorScheme.primaryContainer
                            : Theme.of(context).colorScheme.surfaceContainerHighest,
                        child: Padding(
                          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
                          child: ConstrainedBox(
                            constraints: const BoxConstraints(maxWidth: 300),
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.end,
                              children: [
                                Align(alignment: Alignment.centerLeft, child: Text(message.text)),
                                const SizedBox(height: 4),
                                Text(
                                  '${TimeOfDay.fromDateTime(message.time).format(context)} · ${message.status} · TTL ${message.ttl}${message.retryCount > 0 ? ' · try ${message.retryCount}' : ''}',
                                  style: Theme.of(context).textTheme.labelSmall,
                                ),
                              ],
                            ),
                          ),
                        ),
                      ),
                    );
                  },
                ),
        ),
        SafeArea(
          top: false,
          child: Padding(
            padding: const EdgeInsets.fromLTRB(12, 8, 12, 12),
            child: Row(
              children: [
                Expanded(
                  child: TextField(
                    controller: _composer,
                    enabled: !_sending,
                    textInputAction: TextInputAction.send,
                    onSubmitted: (_) => _send(),
                    decoration: const InputDecoration(
                      hintText: 'Emergency message',
                      border: OutlineInputBorder(),
                    ),
                  ),
                ),
                const SizedBox(width: 8),
                IconButton.filled(
                  onPressed: !_sending ? _send : null,
                  icon: _sending
                      ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2))
                      : const Icon(Icons.send),
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }
}

class _ChatMessage {
  const _ChatMessage({
    required this.id,
    required this.text,
    required this.outgoing,
    required this.status,
    required this.time,
    this.retryCount = 0,
    this.ttl = 0,
  });

  final String id;
  final String text;
  final bool outgoing;
  final String status;
  final DateTime time;
  final int retryCount;
  final int ttl;

  _ChatMessage copyWith({String? status}) => _ChatMessage(
        id: id,
        text: text,
        outgoing: outgoing,
        status: status ?? this.status,
        time: time,
        retryCount: retryCount,
        ttl: ttl,
      );
}
