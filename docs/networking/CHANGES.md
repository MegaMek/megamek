# Networking fork — change log

Entries describe anything that changes what a client or server sees, when it sees it, or how the
transport can be configured. Newest first.

## Unreleased

### PR 2 — `/netstats`, pump counters, phase-change summary

- New chat command `/netstats` (server side, all game types): `/netstats` lists the busiest 15
  packet commands with packets and bytes in each direction; `/netstats all` lists every command
  seen; `/netstats conn` lists per-connection byte totals; `/netstats reset` zeroes the counters
  and `/netstats timing on|off` switches the timers at runtime (both restricted to non-observers
  when `restrict_game_commands` is set). Existing commands are unchanged.
- New: the server and each client keep their own `NetStats`, so a player hosting a game sees the
  two sides separately. The server's packet pump records how many packets it dispatched per
  command, the queue high-water mark and, with timing on, time-in-queue and handler time.
- New log line at INFO on every phase change, e.g.
  `netstats MOVEMENT -> FIRING: 312 packets, 184211 bytes on the wire; busiest: ENTITY_UPDATE 120400/85, ...; pump max depth 4`
  covering the traffic since the previous phase change. One line per phase change; nothing else
  about what or when the client sees has changed.

### PR 1 — network counters (`megamek.common.net.stats.NetStats`)

- New: per-command counters for packets and bytes sent and received, kept by every connection.
  Counting is always on; cost is one `LongAdder.add` per packet. No wire change, no protocol
  version bump.
- New JVM property `megamek.net.timing=true` (default off) enables marshal, unmarshal, write and
  flush timers. With it off the send/receive path makes no clock calls.
- Changed: connection write/marshal failure log lines now include the connection id, the command and
  the payload size. Log text only; no behaviour change.
- Not yet exposed: `/netstats` and the phase-change summary arrive in PR 2.
