import 'package:disaster_mesh/main.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  testWidgets('opens on the login screen', (tester) async {
    await tester.pumpWidget(
      const ProviderScope(child: DisasterMeshApp()),
    );
    await tester.pumpAndSettle();

    expect(find.text('Disaster Mesh'), findsOneWidget);
    expect(find.text('Sign in'), findsOneWidget);
  });

  testWidgets('renders the email and password login form', (tester) async {
    await tester.pumpWidget(
      const ProviderScope(child: DisasterMeshApp()),
    );
    await tester.pumpAndSettle();

    expect(find.text('Email'), findsOneWidget);
    expect(find.text('Password'), findsOneWidget);
    expect(find.text('Sign in'), findsOneWidget);
  });
}
