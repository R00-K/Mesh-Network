import 'package:flutter/material.dart';

/// Brief startup screen while Firebase restores the saved authentication state.
class AuthSplashScreen extends StatelessWidget {
  const AuthSplashScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Scaffold(
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.hub_outlined, size: 64, color: colors.primary),
            const SizedBox(height: 20),
            Text('Disaster Mesh', style: Theme.of(context).textTheme.headlineSmall),
            const SizedBox(height: 8),
            Text('Restoring secure session', style: Theme.of(context).textTheme.bodyMedium),
            const SizedBox(height: 24),
            SizedBox(width: 28, height: 28, child: CircularProgressIndicator(color: colors.primary, strokeWidth: 2.5)),
          ],
        ),
      ),
    );
  }
}
