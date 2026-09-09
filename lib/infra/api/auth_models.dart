import 'package:freezed_annotation/freezed_annotation.dart';

part 'auth_models.freezed.dart';
part 'auth_models.g.dart';

/// Authenticated user session returned by the authentication API.
@freezed
class AuthSession with _$AuthSession {
  const factory AuthSession({
    required String userId,
    required String email,
    required String accessToken,
  }) = _AuthSession;

  factory AuthSession.fromJson(Map<String, dynamic> json) =>
      _$AuthSessionFromJson(json);
}
