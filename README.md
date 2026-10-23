# Disaster Mesh

Flutter UI with an Android Kotlin transport for direct text messaging over Wi-Fi Direct.

## Current behavior

- Android devices discover peers using Wi-Fi Direct and form a TCP link on the group network.
- A persistent app-install UUID identifies a node. The TCP hello exchange verifies the remote UUID before messaging is enabled.
- Messages are persisted locally, use a stable ID, and receive `queued`, `sent`, `delivered`, or `failed` state updates. `delivered` requires an acknowledgement from the intended node.
- Incoming messages are persisted and deduplicated before display. A duplicate retry is acknowledged without displaying it again.
- Retries use 1, 2, 4, 8, and 16 second delays, followed by a final 16 second acknowledgement window. Messages expire after 24 hours.
- The supported messaging path is direct and single-hop between the two connected Android devices. The app does not currently forward messages through intermediate nodes.

The current TCP hello establishes a peer identity association but does not provide cryptographic authentication or encryption. Treat this as a development transport, not a secure channel for sensitive content. Android Wi-Fi Direct behavior, permissions, and background execution also vary by device and OS version.

## Run on Android devices

1. Install and launch the app on two Android devices with Wi-Fi Direct support.
2. Grant the requested nearby/Wi-Fi permissions on both devices and start mesh discovery.
3. Open **Mesh Nodes**, select the other device, and connect. Wait for the verified node identity before opening its conversation.
4. Send a message in each direction. Confirm the sender reaches `delivered` after the recipient receives it.
5. For full coverage, follow [the two-device test matrix](MESSAGING_DEVICE_TEST_MATRIX.md).

The dashboard's simulated nodes are not real peers and cannot receive messages.

## Project layout

- `lib/` contains Flutter screens, state, and the native method/event channel client.
- `android/` contains Wi-Fi Direct discovery/connection, TCP transport, identity, message persistence, and protocol handling.
- `mesh-core/` contains platform-independent Kotlin models and the retry policy.
- [MESSAGING_PROTOCOL.md](MESSAGING_PROTOCOL.md) defines identity, validation, acknowledgement, retry, and expiry behavior.
- [MESSAGING_IMPLEMENTATION_PLAN.md](MESSAGING_IMPLEMENTATION_PLAN.md) records phase status and remaining acceptance work.

## Build and checks

```bash
flutter pub get
flutter analyze
flutter test
flutter build apk
```

Run the platform-independent Kotlin unit tests with the repository Gradle wrapper:

```bash
./gradlew :mesh-core:test
```

An Android build does not substitute for the two-device Wi-Fi Direct checks. Phase 5's hardware matrix is pending until two compatible devices can run the app.
