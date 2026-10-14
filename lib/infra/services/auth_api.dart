import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:firebase_auth/firebase_auth.dart';

import '../models/auth_models.dart';

/// Authentication transport boundary used by the auth presentation layer.
abstract interface class AuthApi {
  /// Signs in an existing user.
  Future<AuthSession> signIn({required String email, required String password});

  /// Creates a user and stores their emergency profile.
  Future<AuthSession> createAccount({
    required String name,
    required String phoneNumber,
    required String email,
    required String password,
  });

  /// Signs out the active user.
  Future<void> signOut();
}

/// Firebase email/password authentication and profile service.
class FirebaseAuthApi implements AuthApi {
  FirebaseAuthApi({FirebaseAuth? auth, FirebaseFirestore? firestore})
      : _auth = auth ?? FirebaseAuth.instance,
        _firestore = firestore ?? FirebaseFirestore.instance;

  final FirebaseAuth _auth;
  final FirebaseFirestore _firestore;

  @override
  Future<AuthSession> signIn({required String email, required String password}) async {
    try {
      final credential = await _auth.signInWithEmailAndPassword(email: email.trim(), password: password);
      return _sessionFor(credential.user, fallbackEmail: email.trim());
    } on FirebaseAuthException catch (error) {
      throw AuthApiException(_messageFor(error.code));
    }
  }

  @override
  Future<AuthSession> createAccount({
    required String name,
    required String phoneNumber,
    required String email,
    required String password,
  }) async {
    try {
      final credential = await _auth.createUserWithEmailAndPassword(email: email.trim(), password: password);
      final user = credential.user;
      if (user == null) throw const AuthApiException('No user session returned.');
      await user.updateDisplayName(name.trim());
      await _firestore.collection('users').doc(user.uid).set({
        'name': name.trim(),
        'phoneNumber': phoneNumber.trim(),
        'email': email.trim().toLowerCase(),
        'createdAt': FieldValue.serverTimestamp(),
      });
      return _sessionFor(user, fallbackEmail: email.trim());
    } on FirebaseAuthException catch (error) {
      throw AuthApiException(_messageFor(error.code));
    } on FirebaseException catch (error) {
      throw AuthApiException(
        error.code == 'permission-denied'
            ? 'Account created, but Firestore denied saving your profile. Check Firestore rules.'
            : 'Profile storage error: ${error.message ?? error.code}',
      );
    }
  }

  AuthSession _sessionFor(User? user, {required String fallbackEmail}) {
    if (user == null) throw const AuthApiException('No user session returned.');
    return AuthSession(
      userId: user.uid,
      email: user.email ?? fallbackEmail,
      accessToken: '',
    );
  }

  @override
  Future<void> signOut() => _auth.signOut();

  String _messageFor(String code) => switch (code) {
        'invalid-credential' || 'wrong-password' || 'user-not-found' => 'Email or password is incorrect.',
        'invalid-email' => 'Enter a valid email address.',
        'email-already-in-use' => 'An account already exists for this email.',
        'weak-password' => 'Choose a stronger password.',
        'too-many-requests' => 'Too many attempts. Try again later.',
        'network-request-failed' => 'Network unavailable. Try again.',
        'operation-not-allowed' => 'Email/password authentication is disabled in Firebase Console.',
        'app-not-authorized' => 'This Android app is not authorized for the Firebase project.',
        'configuration-not-found' => 'Firebase Authentication is not configured for this project.',
        'internal-error' => 'Firebase returned an internal error. Try again.',
        _ => 'Unable to complete authentication right now.',
      };
}

/// Authentication error presented by the service boundary.
class AuthApiException implements Exception {
  const AuthApiException(this.message);

  final String message;

  @override
  String toString() => message;
}
