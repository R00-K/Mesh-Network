import 'package:flutter/services.dart';

class MeshPlatform {
  static const MethodChannel _channel = MethodChannel('disaster_mesh/mesh');

  Future<String> getStatus() async {
    final status = await _channel.invokeMethod<String>('getStatus');
    return status ?? 'Native mesh core returned no status';
  }
}
