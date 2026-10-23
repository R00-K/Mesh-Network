import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

final themeModeProvider =
    NotifierProvider<ThemeModeController, ThemeMode>(ThemeModeController.new);

class ThemeModeController extends Notifier<ThemeMode> {
  static const _storageKey = 'app_theme_mode';
  final _storage = const FlutterSecureStorage();
  bool _selectionMade = false;

  @override
  ThemeMode build() {
    unawaited(_loadSavedTheme());
    return ThemeMode.light;
  }

  Future<void> setThemeMode(ThemeMode mode) async {
    _selectionMade = true;
    state = mode;
    try {
      await _storage.write(key: _storageKey, value: mode.name);
    } catch (_) {
      // Keep the selected theme for this session if storage is unavailable.
    }
  }

  Future<void> _loadSavedTheme() async {
    try {
      final savedTheme = await _storage.read(key: _storageKey);
      if (_selectionMade) return;
      if (savedTheme == ThemeMode.dark.name) {
        state = ThemeMode.dark;
      } else if (savedTheme == ThemeMode.light.name) {
        state = ThemeMode.light;
      }
    } catch (_) {
      // The default light theme remains available if storage is unavailable.
    }
  }
}
