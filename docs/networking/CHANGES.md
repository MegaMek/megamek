# Networking fork — change log

Entries describe anything that changes what a client or server sees, when it sees it, or how the
transport can be configured. Newest first.

## Unreleased

### PR 1 — network counters (`megamek.common.net.stats.NetStats`)

- New: per-command counters for packets and bytes sent and received, kept by every connection.
  Counting is always on; cost is one `LongAdder.add` per packet. No wire change, no protocol
  version bump.
- New JVM property `megamek.net.timing=true` (default off) enables marshal, unmarshal, write and
  flush timers. With it off the send/receive path makes no clock calls.
- Changed: connection write/marshal failure log lines now include the connection id, the command and
  the payload size. Log text only; no behaviour change.
- Not yet exposed: `/netstats` and the phase-change summary arrive in PR 2.
