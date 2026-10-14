# Disaster Mesh

Flutter application with a native Android Wi-Fi Direct prototype for emergency communication.

## Current implementation

- Android Wi-Fi Direct peer discovery with runtime permission handling.
- Peer connection initiated from the Mesh screen.
- TCP text messaging over Wi-Fi Direct group links.
- Group-owner relay of unseen broadcasts to other TCP clients in the same Wi-Fi Direct group, with hop-limit and path checks.
- Message receive acknowledgements and connection state events in Flutter.
- Durable local outbox/history, incoming message deduplication, acknowledgement timeouts, and retry on reconnection.
- Demo topology and metrics are labeled separately from discovered devices.

## Current limitations

- Test on two physical Android devices; emulator and desktop builds do not provide Wi-Fi Direct.
- Relay currently stays inside one Wi-Fi Direct group. Android assigns one group owner; this implementation does not bridge separate groups or relay after a device leaves one group and joins another.
- Pending messages are stored locally and retry when the Wi-Fi Direct socket reconnects; they are not relayed through other devices.
- Routing metrics, routes, and AI-assisted decisions are not connected to live network measurements.
- Socket framing and acknowledgements are a prototype; message-level encryption, durable delivery, and multi-client group-owner forwarding remain future work.

## Project layout

```text
lib/                 Flutter UI and method-channel client
android/app/          Android Wi-Fi Direct host, transport, and foreground service
mesh-core/            Platform-independent Kotlin domain models
```

## Build

```bash
flutter pub get
flutter build apk
```

Run the app on two supported Android phones, grant nearby Wi-Fi Direct permissions, start discovery on both, connect from one phone, and exchange messages from the Messages screen.
