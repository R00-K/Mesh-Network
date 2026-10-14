import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../models/auth_models.dart';
import 'auth_api.dart';

/// Authentication state exposed to the presentation layer.
class AuthState {
  const AuthState({this.session, this.isLoading = false, this.errorMessage});

  final AuthSession? session;
  final bool isLoading;
  final String? errorMessage;

  AuthState copyWith({AuthSession? session, bool? isLoading, String? errorMessage, bool clearError = false}) => AuthState(
        session: session ?? this.session,
        isLoading: isLoading ?? this.isLoading,
        errorMessage: clearError ? null : errorMessage ?? this.errorMessage,
      );
}

final authApiProvider = Provider<AuthApi>((ref) => FirebaseAuthApi());

/// Coordinates authentication actions independently of widgets.
final authControllerProvider = NotifierProvider<AuthController, AuthState>(AuthController.new);

class AuthController extends Notifier<AuthState> {
  @override
  AuthState build() => const AuthState();

  /// Signs in with Firebase email/password credentials.
  Future<bool> signIn({required String email, required String password}) async {
    state = state.copyWith(isLoading: true, clearError: true);
    try {
      state = AuthState(session: await ref.read(authApiProvider).signIn(email: email, password: password));
      return true;
    } on AuthApiException catch (error) {
      state = AuthState(errorMessage: error.message);
      return false;
    }
  }

  /// Creates a Firebase account and saves the emergency profile.
  Future<bool> createAccount({required String name, required String phoneNumber, required String email, required String password}) async {
    state = state.copyWith(isLoading: true, clearError: true);
    try {
      state = AuthState(session: await ref.read(authApiProvider).createAccount(name: name, phoneNumber: phoneNumber, email: email, password: password));
      return true;
    } on AuthApiException catch (error) {
      state = AuthState(errorMessage: error.message);
      return false;
    }
  }

  /// Signs out the active Firebase user.
  Future<void> signOut() async {
    await ref.read(authApiProvider).signOut();
    state = const AuthState();
  }
}
