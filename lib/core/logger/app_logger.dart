import 'dart:developer' as developer;

/// Small centralized logger used by Flutter presentation and infrastructure.
class AppLogger {
  const AppLogger._();

  /// Writes a debug event to the platform log.
  static void debug(String event, [Map<String, Object?> details = const {}]) {
    developer.log('$event $details', name: 'DisasterMesh');
  }

  /// Writes an error event to the platform log.
  static void error(String event, Object error, StackTrace stackTrace) {
    developer.log(
      event,
      name: 'DisasterMesh',
      error: error,
      stackTrace: stackTrace,
      level: 1000,
    );
  }
}
