import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import 'core/router/app_router.dart';
import 'infra/services/theme_controller.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const ProviderScope(child: DisasterMeshApp()));
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
