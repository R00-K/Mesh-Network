import 'package:flutter_secure_storage/flutter_secure_storage.dart';

/// Stores an optional sign-in credential pair in platform secure storage.
class RememberedCredentials {
  RememberedCredentials({FlutterSecureStorage? storage})
      : _storage = storage ?? const FlutterSecureStorage();

  static const _emailKey = 'remembered_login_email';
  static const _passwordKey = 'remembered_login_password';

  final FlutterSecureStorage _storage;

  Future<({String email, String password})?> read() async {
    final email = await _storage.read(key: _emailKey);
    final password = await _storage.read(key: _passwordKey);
    if (email == null || password == null) return null;
    return (email: email, password: password);
  }

  Future<void> save({required String email, required String password}) async {
    await _storage.write(key: _emailKey, value: email.trim());
    await _storage.write(key: _passwordKey, value: password);
  }

  Future<void> clear() async {
    await _storage.delete(key: _emailKey);
    await _storage.delete(key: _passwordKey);
  }
}
