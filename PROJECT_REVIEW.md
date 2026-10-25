# Disaster Mesh — Project Review

## Project description

Disaster Mesh is an Android prototype for direct communication between nearby devices when cellular or internet service is unavailable. It uses Wi-Fi Direct for the local connection and supports verified, single-hop text messaging between two connected devices.

## Existing systems and proposed approach

- **Cellular and internet messaging:** Familiar and feature-rich, but depends on network coverage and infrastructure.
- **Bluetooth-based communication:** Works locally, but generally offers shorter range and lower throughput.
- **Disaster Mesh:** Uses Android Wi-Fi Direct to form a local group, then exchanges messages over TCP without routing the message through a cloud server.

The current implementation is a prototype; it has not yet been validated in a two-device field test.

## How the project works

1. Users start discovery and grant the required Android permissions.
2. The app discovers nearby Wi-Fi Direct devices and displays them in **Mesh Nodes**.
3. The user selects a peer. Android forms a Wi-Fi Direct group; one device becomes group owner and listens on TCP port `8988`, while the other connects to it.
4. The apps exchange a protocol-versioned hello containing persistent app UUIDs. Messaging is enabled after the remote identity is verified.
5. Flutter sends messages to native Android code through a `MethodChannel`. Android stores each message, then sends a JSON frame over TCP.
6. The receiving device validates and stores the message, suppresses duplicates, displays it through an `EventChannel`, and sends an acknowledgement.
7. The sender marks a message **delivered** only after receiving a matching acknowledgement.

## Technology stack

- **Frontend:** Flutter, Dart, Material 3
- **State and navigation:** Riverpod, GoRouter
- **Android networking:** Kotlin, `WifiP2pManager`, TCP sockets, JSON
- **Flutter–Android bridge:** MethodChannel and EventChannel
- **Local persistence:** Android SharedPreferences for identity, message queue, history, and deduplication
- **Authentication/profile:** Firebase Authentication and Cloud Firestore
- **Shared native logic:** Kotlin `mesh-core` module for models and retry policy

The mesh uses raw TCP with newline-delimited JSON; it is not WebSocket-based. Firebase is used for account/profile features, not for message transport.

## Current features and limits

- Peer discovery, Wi-Fi Direct connection, UUID identity handshake, direct messaging, acknowledgement, local queue, duplicate suppression, and delivery status.
- Retries use 1, 2, 4, 8, and 16 second delays; messages expire after 24 hours. Payload limit: 8 KiB.
- Messaging currently supports one direct connection at a time. Multi-hop relaying is not implemented.
- Dashboard simulation and emergency action buttons are not live mesh features yet.
- The UUID hello is not cryptographic authentication, and TCP messages are not encrypted end to end.
- Automated checks and APK build pass, but two-device Wi-Fi Direct testing remains pending.

## Future improvements

- Run a two-device test plan across Android versions, both group-owner roles, and reconnect/background scenarios.
- Add cryptographic peer authentication and end-to-end encryption.
- Implement and test multi-hop forwarding with routing and acknowledgement across several devices.
- Turn emergency actions into real message types and replace simulated dashboard data with verified live metrics.
- Improve storage for larger message histories and add clear resend/recovery controls in the UI.

## Suggested demo

Start discovery on two Android devices, connect one to the other, wait for UUID verification, send a message, show it on the receiving device, and confirm the sender changes to **delivered** after the acknowledgement.
