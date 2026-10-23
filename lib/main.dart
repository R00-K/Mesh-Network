import 'package:flutter/material.dart';
import 'package:firebase_auth/firebase_auth.dart';
import 'package:firebase_core/firebase_core.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import 'core/router/app_router.dart';
import 'features/auth/auth_splash_screen.dart';
import 'infra/services/theme_controller.dart';

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

class DisasterMeshApp extends ConsumerWidget {
  const DisasterMeshApp({super.key, this.router});

  final GoRouter? router;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final themeMode = ref.watch(themeModeProvider);
    return MaterialApp.router(
      title: 'Disaster Mesh',
      debugShowCheckedModeBanner: false,
      routerConfig: router ?? appRouter,
      theme: _buildTheme(Brightness.light),
      darkTheme: _buildTheme(Brightness.dark),
      themeMode: themeMode,
    );
  }

  ThemeData _buildTheme(Brightness brightness) {
    final isDark = brightness == Brightness.dark;
    return ThemeData(
      brightness: brightness,
      colorScheme: ColorScheme.fromSeed(
        seedColor: isDark
            ? const Color(0xff8eaaa4)
            : const Color(0xff557b75),
        surface: isDark
            ? const Color(0xff1b2421)
            : const Color(0xfffafbf9),
        brightness: brightness,
      ),
      scaffoldBackgroundColor:
          isDark ? const Color(0xff111815) : const Color(0xfff2f5f3),
      useMaterial3: true,
    );
  }
}
