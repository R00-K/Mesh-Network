import 'auth_models.dart';

/// Authentication transport boundary. Firebase will implement this next.
abstract interface class AuthApi {
  /// Signs in a user using email and password.
  Future<AuthSession> signIn({required String email, required String password});
}

/// Temporary local implementation used until Firebase Auth is configured.
class LocalAuthApi implements AuthApi {
  @override
  Future<AuthSession> signIn({
    required String email,
    required String password,
  }) async {
    if (email.trim().isEmpty || password.isEmpty) {
      throw const AuthApiException('Email and password are required.');
    }

    return AuthSession(
      userId: email.trim().toLowerCase(),
      email: email.trim().toLowerCase(),
      accessToken: 'local-development-token',
    );
  }
}

/// Expected authentication failure from the API boundary.
class AuthApiException implements Exception {
  const AuthApiException(this.message);

  final String message;

  @override
  String toString() => message;
}
