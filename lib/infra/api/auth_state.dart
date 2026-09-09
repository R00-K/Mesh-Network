import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'auth_api.dart';
import 'auth_models.dart';

/// Authentication state exposed to the presentation layer.
class AuthState {
  const AuthState({this.session, this.isLoading = false, this.errorMessage});

  final AuthSession? session;
  final bool isLoading;
  final String? errorMessage;

  AuthState copyWith({
    AuthSession? session,
    bool? isLoading,
    String? errorMessage,
    bool clearSession = false,
    bool clearError = false,
  }) {
    return AuthState(
      session: clearSession ? null : session ?? this.session,
      isLoading: isLoading ?? this.isLoading,
      errorMessage: clearError ? null : errorMessage ?? this.errorMessage,
    );
  }
}

final authApiProvider = Provider<AuthApi>((ref) => LocalAuthApi());

/// Controls sign-in state independently from the login widgets.
final authControllerProvider = NotifierProvider<AuthController, AuthState>(
  AuthController.new,
);

class AuthController extends Notifier<AuthState> {
  @override
  AuthState build() => const AuthState();

  /// Attempts sign-in and returns whether the request succeeded.
  Future<bool> signIn({required String email, required String password}) async {
    state = state.copyWith(isLoading: true, clearError: true);
    try {
      final session = await ref.read(authApiProvider).signIn(
            email: email,
            password: password,
          );
      state = AuthState(session: session);
      return true;
    } on AuthApiException catch (error) {
      state = AuthState(errorMessage: error.message);
      return false;
    } catch (_) {
      state = const AuthState(errorMessage: 'Unable to sign in right now.');
      return false;
    }
  }

  /// Clears the current local session.
  void signOut() => state = const AuthState();
}
