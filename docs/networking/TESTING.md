# Networking fork — testing

## Prerequisites

The Gradle build includes `../mm-data` as a sibling of the repository root (`settings.gradle`:
`includeBuild('../mm-data')`), so the data repo must be checked out next to this one:

```
C:\Users\<you>\git\megamekplusplus   (this repo)
C:\Users\<you>\git\mm-data           git clone https://github.com/MegaMek/mm-data.git
```

JDK 21 (Temurin or equivalent). The Gradle wrapper downloads Gradle 9.4.1 on first use.

## Running the networking tests

From the repository root:

```
gradlew.bat :megamek:test -x checkstyleMain ^
  --tests "megamek.common.net.*" ^
  --tests "megamek.server.net.*" ^
  --tests "megamek.server.ServerPacketHandlingTest" ^
  --tests "megamek.server.commands.NetStatsCommandTest"
```

(`./gradlew` on Linux/macOS, same arguments.) The first run compiles the whole project and takes
several minutes; later runs are incremental. The HTML report lands in
`megamek/build/reports/tests/test/index.html`, the per-class XML in `megamek/build/test-results/test/`.

The full upstream suite, which must be green before every PR, is `gradlew.bat :megamek:test`.

## What is covered today

These are characterization tests: they pin down what the code does now, including behaviour we
intend to change, so that each later change shows up as a deliberate test edit rather than a
surprise.

| Test class | Pins down |
| --- | --- |
| `megamek.common.net.marshalling.NativeSerializationMarshallerTest` | Command sent as its ordinal, payload as one `Object[]`, fresh `ObjectOutputStream` (full stream header) per packet, rejection of out-of-range ordinals and of payload classes outside the allow list. |
| `megamek.common.net.marshalling.SanityInputFilterTest` | Which classes the deserialization filter allows and rejects; `UNDECIDED` for depth/array checks; unanchored pattern matching that admits neighbouring classes. |
| `megamek.common.net.connections.DataStreamConnectionFramingTest` | Frame layout (`boolean zipped, int type, int length, byte[]`), gzip always on, byte counters, read-back of frames, clean close on truncated frame / unknown marshalling type / EOF, length field trusted as-is. |
| `megamek.common.net.connections.SendPathTest` | `send()` marshals and writes on the calling thread, drains the queue in the same call, closes once on a failed write; `SendQueue` is a plain FIFO with a one-way `finish()`. |
| `megamek.common.net.connections.DataStreamConnectionTest` (upstream) | Issue #8668 deadlock regression. |
| `megamek.common.net.stats.NetStatsTest` | The counter registry: per-command counts, busiest-first ordering, timing gate, reset, concurrency, text formatting. |
| `megamek.common.net.connections.ConnectionNetStatsTest` | The hooks: send records count/raw/wire bytes matching what hit the stream, receive records the same payload bytes, timers only when enabled, nothing recorded on a failed unmarshal. |
| `megamek.server.net.ServerHandshakeTest` | Version check first, exact-equality version comparison, greeting / illegal-version + close, name correction (`Alice` → `Alice.2`), ghost-seat takeover, handshake order not enforced, watchdog re-polling, game packets on the `Packet Pump` thread, chat bypassing the pump, pause/unpause buffering, lobby disconnect dropping the player. |
| `megamek.server.net.InProcessHarnessTest` | Real server + two real `HeadlessClient`s agree on the lobby, chat crosses the full path, a leaving client is removed everywhere. |
| `megamek.server.commands.NetStatsCommandTest` | `/netstats` output modes, `reset` and `timing` behaviour and their restriction to non-observers. |
| `megamek.server.net.ServerNetStatsTest` | Accepted connections report into the server's registry, the pump records dispatches and queue depth, `/netstats` answers over chat, the phase summary is safe to call. |
| `megamek.server.net.ServerInstanceLeakTest` | **Known defect, pinned:** a disconnect processed by an older `Server` is broadcast through the static `Server.getServerInstance()` to the newest server's clients, which remove their own player #0. Flip this test when lifecycle hardening fixes it. |
| `megamek.server.ServerPacketHandlingTest` (upstream) | A throwing handler cannot kill the pump. |

## Known defects the tests encode

`ServerInstanceLeakTest` asserts today's wrong behaviour on purpose. Because of it, two servers must
never overlap in one test JVM: `InProcessHarness.close()` waits until every server-side `Connection N`
thread has exited (each disconnect runs to completion on its own) before calling `Server.die()`. If a harness test ever
reports a client losing its own player, check for a server from an earlier test that was still
winding down.

## Shared helpers

- `megamek.server.net.InProcessHarness` — one server, N headless clients, `await(...)` helpers and a
  player-list comparison. Extend this for integration, instrumentation and fault-injection tests.
- `megamek.server.net.RawPacketClient` — speaks the wire protocol with no client logic, for tests
  that need to drive the handshake one packet at a time or send malformed sequences.

## CI

`.github/workflows/networking-tests.yml` runs the three test patterns above on every push and pull
request (checking out `MegaMek/mm-data` `main` beside the repo). Upstream's `ci.yml` still runs the
full build on pull requests to `main`.
