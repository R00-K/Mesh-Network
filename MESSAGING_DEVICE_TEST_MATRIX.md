# Messaging device test matrix

Status: **not run; requires two physical Android devices with Wi-Fi Direct**. Record device models, Android versions, test date, and observed result for each run.

| Case | Procedure | Expected result | Result |
| --- | --- | --- | --- |
| Both group-owner roles | Repeat connection with each device becoming group owner | TCP identity handshake succeeds in either role; verified UUIDs match the connected devices | Pending |
| Permission grant | Grant all requested permissions and start discovery | Discovery starts and nearby device is shown | Pending |
| Permission denial | Deny a requested permission, then attempt start | App reports permission denied; no false running/connected status | Pending |
| Bidirectional delivery | Send one message A→B and another B→A | Each recipient shows one message; each sender reaches delivered only after acknowledgement | Pending |
| Duplicate delivery | Interrupt/drop an acknowledgement so sender retries same message ID | Recipient shows one copy and re-acknowledges; sender eventually reaches delivered | Pending |
| Disconnect and reconnect | Disconnect after queueing a message, then reconnect to the same node | Message remains queued and retries after the intended node reconnects | Pending |
| Link loss during send | Disable Wi-Fi Direct during a send and restore it | No false delivery; queued/retry state recovers or becomes failed under policy | Pending |
| Background and resume | Background the app on either side during discovery and messaging, then resume | Connection and persisted message history reconcile; events resume without duplicate display | Pending |
| Invalid/oversized input | Inject malformed envelope and payload above 8 KiB using a debug harness | Input is rejected, app remains running, no delivery acknowledgement is emitted | Pending |
| Expiry | Keep a queued message beyond its 24-hour expiry (or use a controlled clock in a test build) | Message becomes failed and is retained in history | Pending |

For each case, retain sanitized logs containing message IDs, lifecycle events, and rejection reasons. Never collect message payloads in diagnostics.
