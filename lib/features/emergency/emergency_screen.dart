import 'package:flutter/material.dart';

/// Broadcast actions for emergency coordination.
class EmergencyScreen extends StatelessWidget {
  const EmergencyScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final actions = [
      ('SOS', Icons.sos, 'Broadcast an urgent distress signal'),
      ('Medical Help', Icons.medical_services_outlined, 'Request medical assistance'),
      ('Resource Request', Icons.inventory_2_outlined, 'Request water, food, or supplies'),
      ('Safe Zone', Icons.shield_outlined, 'Announce a safe location'),
    ];
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        Text('Emergency Broadcast', style: Theme.of(context).textTheme.headlineSmall),
        const SizedBox(height: 8),
        const Text('Broadcasts are queued locally and will relay across available nodes.'),
        const SizedBox(height: 16),
        ...actions.map((action) => Card(
              child: ListTile(
                leading: Icon(action.$2, color: Theme.of(context).colorScheme.error),
                title: Text(action.$1),
                subtitle: Text(action.$3),
                trailing: const Icon(Icons.chevron_right),
                onTap: () {},
              ),
            )),
      ],
    );
  }
}
