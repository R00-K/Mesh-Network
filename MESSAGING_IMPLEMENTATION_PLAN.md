# Messaging Implementation Plan

## Goal

Allow two Android devices running the app to discover each other, establish a Wi-Fi Direct connection, exchange text messages in both directions, and show whether each message is queued, sent, delivered, or failed. Messages are currently single-hop over one active connection. Multi-hop forwarding is a later capability and should not be presented as working until it is verified end to end.

## Phase 1 — Define identity and delivery behavior (complete)

**Work**

- **Decision:** use a persistent app-install UUID as `nodeId`; use the Wi-Fi Direct device address only as a `transportAddress` for connection setup.
- Bind node IDs to live TCP sockets through a versioned hello exchange. Route direct messages and acknowledgements by node ID, not by Wi-Fi Direct address.
- Define the delivery states and a bounded retry policy: five retransmissions with 1/2/4/8/16 second delays, queued while offline, and 24-hour message expiry.
- Require the recipient to persist/deduplicate before display and re-acknowledge duplicates, so loss of an acknowledgement does not cause duplicate display or endless sender retries.
- Keep the initial supported scope explicit: direct messages between two connected Android nodes; no multi-hop guarantee.
- See [MESSAGING_PROTOCOL.md](MESSAGING_PROTOCOL.md) for the protocol contract and state definitions.

**Acceptance criteria**

- Identity, destination validation, acknowledgement correlation, duplicate behavior, status meanings, retry limits, and expiry are documented in `MESSAGING_PROTOCOL.md`.
- Implementation acceptance checks are listed in Phases 2–5; Phase 1 itself defines the contract and does not claim that these behaviors are already implemented.

## Phase 2 — Make peer connection and TCP transport reliable (implementation added; device validation pending)

**Work**

- Make transport startup idempotent without suppressing recovery: a failed client socket attempt must be retryable even if the Wi-Fi Direct group owner and address have not changed.
- Add bounded connection retries with backoff and cancellation on disconnect/stop. Start the group-owner listener before or alongside client attempts; do not depend on a one-time timing race.
- Set socket connect/read behavior deliberately, close sockets and executors on teardown, and report listener, connect, and disconnect failures through structured transport events.
- Reconcile Wi-Fi Direct state with socket state after app lifecycle changes and group-owner changes.
- Ensure Android permissions and runtime permission denial are surfaced to the user, and check platform/version requirements for discovery and connection.

**Implementation notes**

- Added a persistent app-install UUID and a versioned hello exchange. A TCP link is reported as connected only after the remote UUID is verified; connection info and a `PeerIdentityChanged` event expose that UUID separately from the Wi-Fi Direct address.
- Added bounded client socket attempts and made failed/disconnected attempts eligible to restart on the existing Wi-Fi Direct group.
- Full acceptance still needs an Android build and two-device validation, including both group-owner roles and recovery after link loss.

**Acceptance criteria**

- Two physical Android devices can discover and connect regardless of which becomes group owner.
- A temporary listener startup delay or dropped socket recovers without requiring the user to disconnect/reconnect manually.
- Disconnecting or stopping the mesh closes the transport and cancels retries.

## Phase 3 — Verify message protocol and acknowledgements (implementation added; device validation pending)

**Work**

- Validate incoming envelopes (type, ID, source, destination, payload, timestamp, hop limit) before processing them.
- Confirm destination matching uses the Phase 1 identity on both sender and receiver.
- Retain message IDs and deduplicate safely; make delivery acknowledgement address the original sender and correlate it to the original message.
- Retry unacknowledged messages with bounded backoff. Avoid duplicate display on the recipient when a sender retries.
- Ensure a forwarding node only reports forwarding when it actually transmitted the envelope; defer multi-hop claims until routing and acknowledgement behavior are demonstrated across three devices.

**Implementation notes**

- Added protocol version, UUID, sender/recipient, payload-size, timestamp, and expiry validation for direct messages and acknowledgements. Messages for another node are rejected; the current implementation does not relay them.
- Incoming messages and deduplication IDs are committed before the Flutter receive event. A byte-for-byte equivalent retry is acknowledged again without a second receive event; a reused ID with changed content is rejected.
- Outgoing messages are persisted before sending, retried after 1/2/4/8/16 second delays, and failed after the final acknowledgement window or 24-hour expiry. Delivery requires a correlated acknowledgement from the connected destination node.
- Full acceptance still needs two-device validation, including a dropped acknowledgement, duplicate retry, malformed envelope, and offline expiry/reconnect case.

**Acceptance criteria**

- A sends to B; B displays exactly one incoming message; A reaches `delivered` only after B's acknowledgement.
- Disconnecting before delivery leaves a visible queued/failed state, and reconnecting retries according to policy.
- Malformed, duplicate, expired, and incorrectly addressed envelopes do not crash the app or produce false delivery status.

## Phase 4 — Connect Flutter UI to actual messaging state (implementation added; device validation pending)

**Work**

- Make peer discovery/connection state observable in the mesh screen and permit opening a conversation only when the intended peer is connected (or explicitly connect first).
- Bind a conversation to a selected peer and verify the selected peer matches the native connection before sending.
- Preserve the draft and show a useful error if the native call fails. Do not clear the composer merely because the native side queued the message.
- Render lifecycle states from native events, show queued/sent/delivered/failed distinctly, and expose incoming sender and time.
- Handle event-stream errors/re-subscription and app resume so message updates are not silently lost.

**Implementation notes**

- The mesh screen now distinguishes a Wi-Fi Direct transport address from the verified node UUID and only opens messaging after the TCP identity handshake completes.
- Conversations are bound to the active peer's node ID, display only that peer's messages, and enable send only while the selected peer is the verified active connection.
- The native send response reports `sent` versus `queued`; Flutter keeps queued/failed drafts, displays lifecycle updates, and restores persisted conversation history after provider rebuild, stream reconnection, and app resume.
- Native event delivery is marshalled to the main thread, and the Flutter controller resubscribes after stream errors or completion.
- Full acceptance still needs a device run covering navigation away/back, background/resume, peer disconnect during send, and incoming messages while the conversation is closed.

**Acceptance criteria**

- The user can select a discovered peer, connect, send, receive, and see delivery state without manually copying IDs.
- Native invocation errors and unavailable transport are visible and recoverable.
- Incoming messages appear in the correct conversation and outgoing messages remain associated with their destination.

## Phase 5 — Test on devices and harden release behavior (code/docs added; hardware validation pending)

**Work**

- Add focused unit tests for identity normalization, envelope validation, lifecycle transitions, duplicate handling, acknowledgement correlation, and retry policy.
- Add transport tests around connect/listen race, socket closure, retry cancellation, and malformed input where the Android test setup permits.
- Run the app on at least two physical Android devices. Exercise both group-owner roles, permissions granted/denied, app background/resume, Wi-Fi Direct loss, reconnect, repeated sends, and larger payloads.
- Add diagnostic logs that correlate a message ID across enqueue, socket send, receive, ack, retry, and final status without logging message bodies.
- Update `README.md` to describe implemented behavior, device requirements, known limits, and setup steps; it currently says networking and messaging are not implemented.

**Implementation notes**

- Extracted the bounded retry schedule into `mesh-core` and added JVM tests for each retry interval, the final acknowledgement window, and invalid attempt numbers. `:mesh-core:test` passed.
- Added metadata-only lifecycle logs keyed by message ID for enqueue, socket write, receive, duplicate, acknowledgement, retry, and failure. Message bodies are not logged.
- Rewrote `README.md` to describe current single-hop Android behavior, setup, retry/expiry semantics, and transport security limitations.
- Added `MESSAGING_DEVICE_TEST_MATRIX.md`. All physical-device rows remain pending because no two-device run is available in this environment.
- `:app:compileDebugKotlin` passed after wiring the Android handler to the shared retry policy.
- Dedicated Android transport/store tests and protocol-validation unit tests are not yet present; the available JVM test target cannot instantiate Android sockets or `SharedPreferences`. Those cases are represented in the device matrix for execution with a debug harness or on devices.

**Acceptance criteria**

- A documented two-device test matrix passes, including both devices sending to and receiving from the other. This acceptance criterion remains pending hardware execution.
- No tested path reports delivered without recipient acknowledgement, and reconnect behavior matches the stated retry policy.
- Documentation and UI accurately describe single-hop support and any remaining limits.

## Suggested delivery order

Complete Phases 1–3 before polishing the UI, since the Flutter layer currently depends on native IDs and status events. Implement Phase 4 against the agreed lifecycle contract, then complete Phase 5 on real hardware. Do not treat simulated dashboard nodes as discovered or messageable peers.

## Current code areas to review

- `android/app/src/main/kotlin/org/disastermesh/app/mesh/discovery/MeshDiscoveryManager.kt` — peer and local device IDs.
- `android/app/src/main/kotlin/org/disastermesh/app/mesh/connection/WifiDirectConnectionManager.kt` — Wi-Fi Direct connection state.
- `android/app/src/main/kotlin/org/disastermesh/app/mesh/transport/MeshTransport.kt` — TCP listener, client connection, and retry behavior.
- `android/app/src/main/kotlin/org/disastermesh/app/mesh/channels/MeshChannelHandler.kt` — envelope handling, queue, acknowledgements, and Flutter events.
- `android/app/src/main/kotlin/org/disastermesh/app/mesh/storage/MessageStore.kt` — persistence and duplicate tracking.
- `lib/features/mesh/mesh_screen.dart`, `lib/features/messages/messages_screen.dart`, and `lib/infra/api/mesh_dashboard_controller.dart` — peer selection and message presentation.
- `README.md` — currently inconsistent with the Android transport code.
- `MESSAGING_PROTOCOL.md` — Phase 1 identity and delivery contract.
- `MESSAGING_DEVICE_TEST_MATRIX.md` — physical two-device validation cases and run status.
