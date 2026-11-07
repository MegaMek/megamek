# MegaMek networking fork — code review and Phase 1 plan

Date: 2026-11-06. Reviewed tree: `megamekplusplus` at upstream commit `eee91f76ae`
(version 0.51.01.4). Java 21, Gradle, JUnit 6.0.3 / Mockito 5.23, XStream 1.4.21
(save files only — the wire uses Java native serialization).

## 1. Current networking behavior, as read from the code

The whole transport is small: `megamek.common.net` is about 3,500 lines across
`connections/`, `marshalling/`, `packets/`, `enums/`, `events/`, `listeners/`,
`factories/`, with a single existing test (`DataStreamConnectionTest`, three tests
covering the issue-8668 deadlock fix).

**Wire format.** `DataStreamConnection` writes each packet as
`boolean zipped, int marshallingType, int length, byte[length]`. The payload is produced by
`NativeSerializationMarshaller.marshall`: a fresh `ObjectOutputStream` per packet writing
`command.ordinal()` then `writeObject(packet.data())` (an `Object[]`). Every packet is
gzipped — `AbstractConnection.zipData` defaults to `true` and the only setter is deprecated.
There is exactly one marshaller (`NATIVE_SERIALIZATION_MARSHALING = 0`).

**Send path (server and client).** `AbstractConnection.send(Packet)` takes the connection
monitor, constructs a `SendPacket` (which marshals and gzips on the *caller's* thread),
appends it to `SendQueue`, and immediately drains the queue with `sendQueuedPackets()`, which
writes to the socket under the same lock. `Server.send(Packet)` is
`connections.forEach(c -> c.send(packet))`. So a broadcast on the packet pump thread
marshals the packet once per connection and writes each socket synchronously, while holding
`serverLock` and usually `GAME_LOCK`. `SendQueue` is not a real queue in practice; it is
always drained in the same call that filled it.

**Receive path.** One `Connection N` thread per server connection (`ConnectionHandler`) and
one `Client Connection` thread per client. Both loop `flush(); update();` but
`update()` never returns until the socket fails, because `readNetworkPacket()` blocks in
`readFully`, so the `flush()` in that loop runs effectively once. Deserialization happens on
the receive thread (`AbstractConnection.processPacket` → `unmarshall` with
`SanityInputFilter`), then `PacketReceivedEvent` is fired.

On the server, `Server.connectionListener.packetReceived` routes `CLIENT_FEEDBACK_REQUEST`
to `gameManager.handleCfrPacket`, handles `CLOSE_CONNECTION`, `CLIENT_NAME`,
`CLIENT_VERSIONS` and `CHAT` **immediately on the receive thread**, buffers everything while
paused in `pausedWaitingList`, and otherwise enqueues on `packetQueue`
(`ConcurrentLinkedQueue` plus `wait/notifyAll`). The `Packet Pump` thread drains it, calling
`handle()` under `synchronized (serverLock)`, and `handle()` takes `GAME_LOCK` around
`gameManager.handlePacket`. `handle()` already catches `RuntimeException` so a bad handler no
longer kills the pump.

On the client, `AbstractClient.connectionListener` posts every packet to the Swing EDT with
`SwingUtilities.invokeLater(() -> handlePacket(...))`; all `Game` mutation on the client is
EDT-confined. Princess uses the same `Client` path, and its `Precognition` is a hand-copied
clone that must be kept in step.

**Handshake.** Server accepts, creates the connection as *pending*, sends
`SERVER_VERSION_CHECK`, and schedules a `ConnectionWatchdog` every 500 ms. Client answers
`CLIENT_VERSIONS` with `SuiteConstants.VERSION`; server requires exact equality
(`Version.is` → `equals`), replies `SERVER_GREETING` or `ILLEGAL_CLIENT_VERSION` + close.
Client then sends `CLIENT_NAME`; `receivePlayerName` moves the connection to active, assigns
the player, and `sendCurrentInfo` pushes the full state. A client whose name matches a
ghost player is treated as *returning* and takes over that player id — so a crude reconnect
already exists.

**Diagnostics today.** `AbstractConnection.bytesSent/bytesReceived` (approximate, counted at
marshal time) surfaced only through `/who`. Nothing per command, no timings, no queue
depth, no RTT.

## 2. Findings, ranked

Each finding names the file and the phase where it should be addressed. Nothing here is
changed in Phase 1 except where noted; Phase 1 exists to measure these before touching them.

1. **Unbounded allocation from the wire.** `DataStreamConnection.readNetworkPacket` does
   `new byte[len]` with `len` straight from `in.readInt()`. A malformed or hostile peer can
   force a 2 GB allocation per connection. Also `encoding` is unvalidated (it only fails
   later through `Objects.requireNonNull` in `processPacket`). → Phase 2, but Phase 1 should
   record the max observed packet size per command so the cap is chosen from data.

2. **Synchronous fan-out on the pump thread.** `Server.send(Packet)` →
   `AbstractConnection.send` marshals, gzips and writes per connection on the caller, under
   `serverLock` and `GAME_LOCK`. One client with a full TCP window stalls every other client
   and the game. The same object graph is also serialized N times for N clients. → Phases 6
   and 4. Phase 1 measures socket write time per connection and marshal time per command to
   quantify both.

3. **No timeouts, keepalive, or TCP_NODELAY anywhere.** No `setSoTimeout`, `setKeepAlive`,
   `setTcpNoDelay` or connect timeout in the tree; the client's `open()` does
   `new Socket(host, port)` (blocking, OS-default timeout). Half-open connections are
   discovered only when a write eventually fails. → Phase 2.

4. **Game state touched outside the pump's locks.** The immediately-handled commands
   (`CHAT` including every `/command`, `CLIENT_NAME`, `CLIENT_VERSIONS`,
   `CLOSE_CONNECTION`) run on the connection's receive thread with neither `serverLock` nor
   `GAME_LOCK`; `ServerCommand` implementations freely mutate the game from there.
   `LOAD_GAME` calls `setGame` on the pump without `GAME_LOCK`. `pausedWaitingList` is a
   plain `ArrayList` shared by all receive threads. → Phase 2 (hand these to the pump or
   state the lock). Phase 1 must not add to this: the stats registry is lock-free.

5. **Write failures are partly swallowed.** `AbstractConnection.sendNow` catches
   `Exception` and logs `""` with the stack trace; the failure does not propagate, so
   `sendQueuedPackets` returns `true` and the connection stays open until the later
   `out.flush()` fails. Logs lack connection id, player, command and size (rule 6).
   → Phase 2, but Phase 1 should make these log lines carry id/command/size since the
   instrumentation will already have them in hand.

6. **Per-packet serialization overhead.** A new `ObjectOutputStream` per packet means a
   stream header and full class descriptors in every packet, and every packet is gzipped
   including tiny ones (`CHAT`, `PLAYER_READY`), where the gzip header exceeds the payload.
   → Phases 4/5, only after Phase 1 reports raw vs. compressed bytes per command.

7. **`SanityInputFilter` is weaker than it looks.** Patterns are matched with unanchored
   `find()` and additionally with `className.contains(pattern.toString())`; `megamek.*`
   allows the entire application classpath; `null` class returns `UNDECIDED`; and the
   filter sets no `maxdepth`, `maxarray`, `maxrefs` or `maxbytes` limits, which
   `ObjectInputFilter` supports directly. → Phase 2.

8. **`ConnectionWatchdog` is noisy and chatty.** It logs `error("Bark Bark")` twice a second
   per pending connection for up to 60 s and re-sends `SERVER_VERSION_CHECK` each time.
   → Phase 2 (fold into the lifecycle timeouts).

9. **Version handshake has no fork protocol slot.** `CLIENT_VERSIONS` carries only
   `SuiteConstants.VERSION` and is compared for equality. A fork protocol version can be
   added additively as a second element of the packet data; the server must tolerate its
   absence. → Needed before the first wire change (first candidate: PING/PONG in Phase 2).

10. **`MULTI_PACKET` exists but is only used by SBF.** `SBFGameManager` batches via
    `MULTI_PACKET`; TW does not. This is a ready-made, zero-wire-change batching mechanism
    for Phase 6. The client handles it recursively on the EDT.

11. **Reconnect base exists but is untested.** `receivePlayerName` ghost takeover plus
    `sendCurrentInfo` full resend. No test covers it. → Phase 3 builds on it.

12. **Test coverage.** One test class for the transport (`DataStreamConnectionTest`) and
    one for the pump (`ServerPacketHandlingTest`, which already starts a real `Server`
    in `setUp` — a useful seed for the harness). No in-process server-plus-clients
    harness. Phase 1 must add the harness because every later
    before/after number depends on it.

13. **A dying server leaks packets into the next one (found by the harness).**
    `AbstractGameManager.send()` transmits through the static `Server.getServerInstance()`,
    which is whichever `Server` was constructed last, not the server that owns the manager; and
    `Server.die()` returns without waiting for its connection threads. Host a game, end it, host
    again while a client is still disconnecting, and the old manager's `PLAYER_REMOVE` and
    "disconnected" chat reach the new server's clients, whose player ids collide from 0 — the new
    host's client silently removes its own player. Pinned by
    `megamek.server.net.ServerInstanceLeakTest`. → Phase 2: `die()` must quiesce connection
    threads, and the manager should send through its own server (or `die()` must detach the
    static instance). Found 2026-11-06 on the first full run of the characterization suite.

14. **Working tree hygiene.** `git status` shows modifications in ~10 weapon classes under
    `common/weapons/lrms/clan/` with no fork commits on top of upstream; almost certainly
    CRLF normalization. Confirm with `git diff --stat` and `git config core.autocrlf` before
    branching so the first fork PR does not carry out-of-scope noise.

## 3. Phase 1 — Instrumentation

### Scope and compatibility

In scope: `megamek.common.net.*`, `Server`, `ConnectionHandler`, `AbstractClient`, a new
`ServerCommand`, and one hook in `AbstractGameManager.changePhase`. No `PacketCommand`
is added, no packet payload changes, no state class changes, no save-file impact, so no
protocol version bump is required. `/netstats` is a new chat command (additive; existing
commands untouched). One new JVM system property, `megamek.net.timing` (additive).

Deviation from the priority list: per-connection RTT needs a PING/PONG exchange, which
is the first wire change. It is deferred to Phase 2 where keepalive needs the same packets,
so Phase 1 stays wire-neutral and can land without the fork protocol version machinery.

### Design

**`megamek.common.net.stats.NetStats`** (new package, ~200 lines). A registry keyed by
`PacketCommand` holding `LongAdder`s for: packets sent, packets received, raw bytes (before
gzip), wire bytes (after gzip, i.e. `SendPacket.data.length` and `NetworkPacket.data.length`),
marshal nanos, unmarshal nanos, socket-write nanos, max wire bytes seen. Plus aggregate
gauges: pump queue depth (current and high-water), per-command handle nanos on the pump,
and time-in-queue. Counters are always on (a `LongAdder.add` is a few nanoseconds);
`System.nanoTime()` timers are gated by `megamek.net.timing=true` so the default build adds
no clock calls. `snapshot()` returns an immutable view; `format(topN)` renders a fixed-width
table sorted by wire bytes; `reset()` zeroes. Two instances per JVM: one owned by `Server`,
one by `AbstractClient`, so a hosting player's client and server do not blend.

**Threading rule.** All counters are `LongAdder`/`AtomicLong`; no lock is taken, nothing
touches `Game`. Hooks run on whichever thread already does the work (caller thread for
marshal and socket write, receive thread for unmarshal, pump thread for handle time).

**Hook points** (each a one- or two-line insertion, no reformatting):

- `SendPacket` constructor: wrap the marshaller's stream in a counting stream to get raw
  bytes; time `marshall` and gzip; record wire bytes. The `AbstractConnection` gets a
  `NetStats` field (default: a shared no-op-cost global) with a package-private setter.
- `AbstractConnection.processPacket(INetworkPacket)`: time `unmarshall`, record wire bytes
  and command.
- `DataStreamConnection.sendNetworkPacket`: time the `out.write` (this is the slow-client
  signal for finding 2).
- `Server.ReceivedPacket`: add an `enqueuedNanos` field; pump records time-in-queue and
  handle time; `packetReceived` records queue depth on enqueue.
- `Server.run()` (accept): call `c.setNetStats(serverStats)`. `AbstractClient.connect()`:
  likewise with the client instance.
- `AbstractGameManager.changePhase`: after `getGame().setPhase(newPhase)`, call
  `Server.getServerInstance().logNetStatsSummary(newPhase)` (null-guarded; the manager
  already reaches the server through that static elsewhere), which logs the top-10 table at INFO and resets the
  per-phase window (cumulative totals kept).
- `AbstractConnection.sendNow` and `sendQueuedPackets` error logs: include connection id,
  command name, and byte size (the data is already in scope). No behavior change.

**`/netstats` command.** `megamek.server.commands.NetStatsCommand`, registered from
`TWGameManager.getCommandList` next to `WhoCommand`. `/netstats` prints totals, pump
high-water depth, and the top-15 commands by wire bytes; `/netstats all` prints every
command; `/netstats conn` prints per-connection bytes and max write time; `/netstats reset`
resets (restricted via `canRunRestrictedCommand`). Output goes through
`server.sendServerChat(connId, ...)` in lines of ≤ 100 chars.

### Tests

- `NetStatsTest`: counters accumulate per command, snapshot is stable under concurrent
  adders (4 threads × 100k adds), reset clears, format sorts by wire bytes, timing is
  skipped when the property is off.
- `SendPacketStatsTest` and `AbstractConnectionStatsTest`: following the existing
  `DataStreamConnectionTest` mock-socket pattern, send a known packet through a
  `DataStreamConnection` pair over piped streams and assert sent/received counts, that wire
  bytes equals the bytes actually written to the stream, and that the unmarshal counter
  fired.
- `NetStatsCommandTest` (alongside the existing `unittests/megamek/server/commands/`
  tests): mocked `Server`, assert the chat lines produced for empty and
  populated stats and that `reset` is restricted.
- `HeadlessNetHarness` (new, under `unittests/megamek/server/net/`): starts a real `Server`
  on an ephemeral port and N real `Client`s in-process, drives them through connect →
  lounge → add units → deployment via the existing packet API, waits on a latch per phase,
  and asserts equal `Game` phase, round, player list and entity ids on every client. Phase 1
  uses it for one test: after two phases, server stats show `SENDING_ENTITIES` sent exactly
  N times per full send and the pump high-water depth is > 0. Everything later adds to this
  harness rather than writing new one-offs.

### Baseline to capture and publish

Scenario per the project notes: 4 human clients + 2 Princess bots, 40 units, 3×3 board set,
run through deployment and two full turns, timing on. Record per command: packet count,
raw bytes, wire bytes, marshal and unmarshal time, max socket write; plus pump high-water
depth and wall time from `/done` by the last player to every client seeing the next phase.
Also capture the same with 1 client to see the per-connection cost. Commit the tables to
`docs/networking/BASELINE-0.51.01.4.md` with JVM and machine. This table is what Phases
2–6 are measured against.

### Deliverables and order

1. Branch hygiene: confirm the CRLF diff, create `upstream` tracking branch, start
   `fork/phase1-netstats`. Add `docs/networking/README.md` and an empty `CHANGES.md`.
2. PR 1: `NetStats` + connection hooks + tests (no user-visible change).
3. PR 2: pump instrumentation + `/netstats` + phase-change log + `NetStatsCommandTest`.
   `CHANGES.md` entry: new command, new log line at phase change, new system property.
4. PR 3: headless harness + first harness test.
5. Baseline document.

Estimated size: ~600 lines of production code, ~700 lines of tests, all additive.

### Acceptance

- Full upstream test suite green (`./gradlew :megamek:test`).
- Game runs with and without `-Dmegamek.net.timing=true`; with it off, a profiler shows
  no `nanoTime` calls in the send path.
- `/netstats` output matches the harness-measured counts for a scripted game.
- Baseline tables published.

## 4. What Phase 2 will take from Phase 1

The max-wire-bytes-per-command numbers set the read cap for finding 1; socket-write timings
decide whether bounded per-connection writers (finding 2) are urgent or can wait for Phase
6; raw-vs-wire bytes decide which commands should stop being gzipped (finding 6); and the
harness is where the fault-injection tests (drop mid-phase, slow reader, oversized frame)
get written.
