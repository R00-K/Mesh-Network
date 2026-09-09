import 'package:flutter/material.dart';

/// Store-and-forward message queue screen.
class MessagesScreen extends StatelessWidget {
  const MessagesScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        Text('Offline Messages', style: Theme.of(context).textTheme.headlineSmall),
        const SizedBox(height: 16),
        const Card(child: ListTile(leading: Icon(Icons.schedule), title: Text('No queued messages'), subtitle: Text('Messages will be stored when a route is unavailable.'))),
      ],
    );
  }
}
