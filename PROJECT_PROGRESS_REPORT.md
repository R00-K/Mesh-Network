# AI Swarm Emergency Communication Network

## Project Progress Report

**Project:** Disaster Mesh  
**Branch:** mesh  
**Report date:** 19 October 2026  
**Platform:** Flutter application with native Android/Kotlin networking

## 1. Project objective

The project is developing an emergency communication network that allows nearby
Android devices to discover one another and exchange messages without Internet
access. The planned system uses Wi-Fi Direct for local connectivity and
store-and-forward routing for multi-hop communication.

## 2. Current implementation summary

The project has progressed from a UI and domain-model prototype to an Android
Wi-Fi Direct networking prototype. The current branch contains:

- Flutter dashboard and WhatsApp-style chat interface.
- Native Android Wi-Fi Direct discovery.
- Permission-safe discovery startup.
- Peer connection and disconnection handling.
- TCP socket transport over a Wi-Fi Direct group.
- Offline text message envelopes.
- Durable message queueing.
- Acknowledgements and duplicate detection.
- Multi-client relay transport and message forwarding.
- Hop limits and forwarding-path tracking.
- Link recovery and route-state reporting.
- Measured Android metrics and deterministic route scoring.
- Recorded routing outcomes with a deterministic fallback strategy.

## 3. Completed work by milestone

### Milestone 1: Clear application state

Completed:

- Demo/simulated data is separated from real Wi-Fi Direct data.
- The Mesh screen displays real Wi-Fi Direct peers only.
- The dashboard topology displays the local device and discovered real peers.
- Demo users are no longer shown as real connections.
- Discovery state is displayed as active or inactive.
- Real connected-peer count is separated from simulated topology data.
- Device battery is displayed only when measured Android data is available.
- The topology displays “No real connections available” when no peers are found.

### Milestone 2: Single-hop Wi-Fi Direct communication

Completed in code:

- Location and Nearby Devices permissions are requested before discovery.
- Discovery starts only after permission approval.
- Wi-Fi Direct peers are obtained through WifiP2pManager.
- A selected peer can be connected using WifiP2pManager.connect().
- Connection broadcasts are handled.
- Group-owner information is obtained.
- A TCP listener is created for the Wi-Fi Direct group owner.
- A client connects to the group-owner address.
- Text messages are sent using JSON envelopes over the socket.
- Connection and transport state are exposed to Flutter.
- A WhatsApp-style chat interface was added.

The message envelope contains:

- Message ID
- Source device ID
- Destination device ID
- Text payload
- Creation timestamp
- Hop limit
- Forwarding path

### Milestone 3: Reliable message handling

Completed in code:

- Outgoing messages are stored before transmission.
- Messages are persisted using Android SharedPreferences.
- Messages receive unique UUIDs.
- Messages have queued, sending, awaiting-acknowledgement, delivered, and
  retry-related states.
- ACK envelopes are sent after message reception.
- Delivered messages are removed from the outgoing queue.
- Received message IDs are persisted for duplicate detection.
- Duplicate messages are ignored.
- Queued messages are retried when a transport reconnects.
- The chat displays outgoing and incoming message bubbles.

### Milestone 4: Multi-hop forwarding

Completed in code:

- Group owners can maintain multiple TCP socket links.
- Messages can be forwarded to other connected links.
- Hop limits are decreased during forwarding.
- The current node is appended to the forwarding path.
- Messages with expired hop limits are discarded.
- Messages containing the current node in their path are rejected to prevent
  loops.
- Duplicate messages are suppressed before forwarding.
- Forwarding status events are reported to Flutter.

### Milestone 5: Link and route recovery

Completed in code:

- Wi-Fi Direct connection and disconnection broadcasts are monitored.
- Transport failures are detected.
- Route states include no_route, recovering, and active.
- A failed transport schedules a connection-information refresh.
- The existing Wi-Fi Direct link is retried after a short recovery delay.
- Route status and route reason are displayed in the dashboard.
- Duplicate Android connection broadcasts no longer restart and close an
  already active socket.

### Milestone 6: Routing metrics

Completed in code:

- Android battery percentage is collected.
- Connected socket-link count is measured.
- Send attempts and successful socket writes are counted.
- Acknowledgement count is recorded.
- Acknowledgement latency is measured when available.
- Packet loss is estimated from unsuccessful send attempts.
- Link availability is reported.
- A deterministic route score is calculated from battery, latency, packet loss,
  and availability penalties.
- Metric provenance is reported as measured_local_android.

### Milestone 7: Adaptive-routing foundation

Completed in code:

- Route delivery outcomes are stored locally.
- Successful and failed route attempts are recorded.
- Delivery success rate and sample count are calculated.
- The active strategy is reported as deterministic_with_fallback.
- A fallback strategy based on the lowest deterministic route score is exposed.
- The UI reports that an adaptive model is not currently enabled.

Not yet completed:

- Training or evaluation of a machine-learning model.
- Comparison of a learned model against the deterministic strategy.
- On-device model deployment.

## 4. Current architecture

    Flutter UI
        |
        | MethodChannel and EventChannel
        v
    Native Android bridge
        |
        +-- Wi-Fi Direct discovery
        +-- Wi-Fi Direct connection manager
        +-- TCP mesh transport
        +-- Message store
        +-- Message forwarding
        +-- Route coordinator
        +-- Metrics collector
        +-- Routing outcome evaluator
        |
        v
    Platform-independent mesh-core models

Important implementation areas:

- lib/: Flutter UI, routing, chat, and native event handling.
- android/app/src/main/kotlin/.../mesh/discovery/: Wi-Fi Direct discovery.
- android/app/src/main/kotlin/.../mesh/connection/: peer connections.
- android/app/src/main/kotlin/.../mesh/transport/: TCP transport.
- android/app/src/main/kotlin/.../mesh/storage/: durable messages and
  duplicate tracking.
- android/app/src/main/kotlin/.../mesh/routing/: route recovery and outcome
  evaluation.
- android/app/src/main/kotlin/.../mesh/metrics/: Android measurements.
- mesh-core/: platform-independent domain models and tests.

## 5. Validation completed

Completed validation:

- Source changes pass git diff --check.
- The application was launched on a physical Android phone using flutter run.
- Android compilation exposed and led to the fix for the Kotlin takeLast()
  collection error.
- ADB logs were inspected during Wi-Fi Direct connection testing.
- Logs confirmed Wi-Fi Direct group formation.
- Logs identified a transport race caused by duplicate connection-change
  broadcasts.
- The duplicate transport-start race was fixed.

## 6. Known limitations

The following items still require work:

- End-to-end two-phone message delivery must be revalidated after the latest
  transport race fix.
- The development environment currently has one ADB-connected phone at a time.
- Message persistence currently uses SharedPreferences; a larger deployment
  should use Room or another structured database.
- Routing currently broadcasts through available transport links rather than
  selecting a complete multi-hop route graph.
- Signal strength and bandwidth are not yet measured consistently across
  Android devices.
- Background execution and device restart behavior require real-device testing.
- The adaptive model is not implemented; deterministic routing remains active.
- Full automated Flutter and Gradle validation is still required.

## 7. Remaining work

### Immediate priority

Validate the latest build on two Android phones:

1. Install the latest mesh branch on both devices.
2. Enable Wi-Fi and required permissions.
3. Start discovery on both devices.
4. Connect the phones.
5. Confirm both transports report connected.
6. Send a chat message.
7. Confirm the receiving bubble appears.
8. Confirm the sender changes from awaiting ACK to delivered.

### Final validation milestone

Test and document:

- Two-device direct messaging.
- Three-device relay messaging.
- Queued delivery.
- Duplicate messages.
- Interrupted links.
- Device restarts.
- App restarts.
- Peer movement out of range.
- Delivery rate.
- Message latency.
- Recovery time.
- Battery impact.

The final report should include supported Android versions, tested phone
models, known Wi-Fi Direct limitations, and measured results.

## 8. Recent implementation commits

- 9d53bec — Prevent duplicate connection events closing sockets.
- 1a0b629 — Fix direct message identity matching.
- 607a092 — Add chat delivery states and normalize peer IDs.
- a2fa48b — Use real peer identities for delivery.
- 575ae06 — Open messages directly for mesh peers.
- 85255d4 — Show discovered peers in live topology.
- 1791a37 — Add adaptive routing outcome evaluation.
- 5748af2 — Add measured mesh metrics and route scoring.
- b895b2b — Add link recovery and route state.
- 01e344f — Add multi-hop forwarding and loop protection.
- 9eb982a — Add durable message delivery handling.

## 9. Overall status

The project has completed the core implementation of discovery, connection,
messaging, reliability, forwarding, recovery, metrics, and deterministic
routing layers. It is currently at the real-device integration and validation
stage.

The adaptive machine-learning routing stage and formal multi-device performance
testing remain outstanding.
