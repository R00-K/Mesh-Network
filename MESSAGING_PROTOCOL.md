# Messaging Identity and Delivery Contract

This contract defines Phase 1 for direct messaging between Android devices. The remaining phases implement and verify it.

## Identity

- Each app installation has a persistent `nodeId`: a randomly generated UUID stored in app-private persistent storage. It is the identity used by message envelopes, acknowledgements, duplicate tracking, and conversations.
- A Wi-Fi Direct `deviceAddress` is a `transportAddress`. It is used only to ask Android to connect to a discovered device. It is not a message destination and must not be presented as the durable node identity.
- Discovery initially yields a transport address and display name. After the TCP connection is established, peers exchange a versioned `hello` envelope containing their `nodeId`. The transport binds that node ID to that socket. A node is messageable by node ID only while that binding is live.
- A message whose destination is not the connected remote node ID is rejected locally. The initial release supports direct, single-hop delivery only; relaying is out of scope until separately specified and tested.
- A reconnect repeats the hello exchange. If a remote node's persistent ID differs from the saved conversation identity, the app treats it as a different node and does not silently redirect messages.

## Message envelope and acknowledgement

Every message has a unique ID, protocol version, source node ID, destination node ID, UTF-8 text payload, creation time, and expiry time. An acknowledgement carries the same message ID, the recipient as source, and the original sender as destination. An acknowledgement is accepted only when both IDs match the pending message and local node.

The recipient persists/deduplicates an accepted message before displaying it, then acknowledges it. If a duplicate message arrives after a lost acknowledgement, the recipient does not display it again but sends the acknowledgement again. Unknown protocol versions, malformed fields, expired messages, and messages addressed to another node are rejected without being displayed or acknowledged as delivered.

## User-visible state semantics

| State | Meaning |
| --- | --- |
| `queued` | Message is durably stored on the sender and awaits a usable connection or retry. |
| `sent` | The sender wrote the message to the active socket. This does not mean the recipient received it. |
| `delivered` | A valid acknowledgement was received from the intended destination for this message ID. |
| `failed` | The retry policy was exhausted or the message expired. The UI should retain the message and explain that delivery was not confirmed. |

`connecting` describes the peer connection, not a message status. Internal `sending`/`awaiting_ack` values may be used, but the UI must map them to the states above and must never show `delivered` based only on a successful socket write.

## Initial retry and expiry policy

- While disconnected, retain outgoing messages in `queued` state and try them after the intended node reconnects.
- Once connected, retry unacknowledged messages at 1, 2, 4, 8, and 16 second delays (five retransmissions after the first write). Wait one final 16-second acknowledgement window after the last write, then mark the message failed if no acknowledgement arrived. Stop earlier when acknowledged.
- Expire a message 24 hours after creation. On expiry or exhausted retries, transition it to `failed`; do not delete it silently.
- Retransmissions reuse the same message ID so the receiver can deduplicate and re-acknowledge them.

## Implementation status

Phases 2–4 have implemented persistent node UUIDs, the TCP hello binding, direct message/ack validation, durable outgoing and incoming history, deduplication, retry/expiry behavior, and Flutter status display. The contract is implemented in code but still requires the two-device checks listed in [MESSAGING_DEVICE_TEST_MATRIX.md](MESSAGING_DEVICE_TEST_MATRIX.md). The initial transport uses plain TCP and its hello exchange is not cryptographic authentication.
