import 'package:flutter/material.dart';
import 'package:firebase_auth/firebase_auth.dart';
import 'package:firebase_core/firebase_core.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import 'core/router/app_router.dart';
import 'features/auth/auth_splash_screen.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await Firebase.initializeApp();
  runApp(const AuthBootstrap());
}

/// Restores Firebase Auth before choosing login or the dashboard.
class AuthBootstrap extends StatelessWidget {
  const AuthBootstrap({super.key});

  @override
  Widget build(BuildContext context) {
    return StreamBuilder<User?>(
      stream: FirebaseAuth.instance.authStateChanges(),
      builder: (context, snapshot) {
        if (snapshot.connectionState == ConnectionState.waiting) {
          return const AuthSplashScreen();
        }
        return ProviderScope(
          child: DisasterMeshApp(
            router: createAppRouter(
              initialLocation: snapshot.hasData ? '/' : '/login',
            ),
          ),
        );
      },
    );
  }
}

class DisasterMeshApp extends StatelessWidget {
  const DisasterMeshApp({super.key, this.router});

  final GoRouter? router;

  @override
  Widget build(BuildContext context) {
    return MaterialApp.router(
      title: 'Disaster Mesh',
      routerConfig: router ?? appRouter,
      theme: ThemeData(
        brightness: Brightness.dark,
        colorScheme: ColorScheme.fromSeed(
          seedColor: Colors.deepOrange,
          brightness: Brightness.dark,
        ),
        useMaterial3: true,
      ),
    );
  }
}
