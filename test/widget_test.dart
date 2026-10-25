import 'package:disaster_mesh/main.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  testWidgets('opens on the dashboard without layout errors', (tester) async {
    await tester.pumpWidget(
      const ProviderScope(child: DisasterMeshApp()),
    );
    await tester.pumpAndSettle();

    expect(find.text('VitalGuard Mesh'), findsOneWidget);
    expect(find.text('Connected Nodes'), findsOneWidget);
    expect(tester.takeException(), isNull);
  });

  testWidgets('opens Mesh Nodes from the shared navigation', (tester) async {
    await tester.pumpWidget(
      const ProviderScope(child: DisasterMeshApp()),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.text('Mesh').last);
    await tester.pumpAndSettle();

    expect(find.text('Mesh Nodes'), findsOneWidget);
    expect(find.text('Discovered Wi-Fi Direct devices'), findsOneWidget);
    expect(tester.takeException(), isNull);
  });
}
