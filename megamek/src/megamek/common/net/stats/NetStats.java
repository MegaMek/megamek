/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package megamek.common.net.stats;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.concurrent.atomic.LongAdder;

import megamek.common.net.enums.PacketCommand;

/**
 * Lightweight counters for the networking layer: packets and bytes per {@link PacketCommand} in each direction, and,
 * when timing is enabled, how long marshalling, unmarshalling and socket writes took.
 *
 * <p>Counting is always on and costs one {@link LongAdder#add} per event. Timing is off unless the JVM is started
 * with {@code -Dmegamek.net.timing=true} or {@link #setTimingEnabled(boolean)} is called; while it is off the hot path
 * makes no {@code System.nanoTime()} calls at all. Everything is lock-free and may be called from any thread.</p>
 *
 * <p>A JVM normally holds a few instances: {@link #defaultInstance()} for connections nobody has assigned one to,
 * and one each for a server and a client so a player hosting a game does not see both sides blended.</p>
 */
public final class NetStats {

    /** System property that switches the timers on at start-up. */
    public static final String TIMING_PROPERTY = "megamek.net.timing";

    // Declared before DEFAULT: the instance's field initializers size their arrays from it
    private static final PacketCommand[] COMMANDS = PacketCommand.values();

    private static final NetStats DEFAULT = new NetStats();
    private static volatile boolean timingEnabled = Boolean.getBoolean(TIMING_PROPERTY);

    /** Immutable per-command figures. Byte counts are of the frame payload (after compression) unless named raw. */
    public record CommandStats(PacketCommand command, long sent, long received, long rawBytesSent, long wireBytesSent,
          long wireBytesReceived, long maxWireBytesSent, long maxWireBytesReceived, long marshalNanos,
          long unmarshalNanos, long writeNanos) {

        public long packets() {
            return sent + received;
        }

        public long wireBytes() {
            return wireBytesSent + wireBytesReceived;
        }
    }

    /** Immutable view of everything at one instant. */
    public record Snapshot(List<CommandStats> commands, long flushCount, long flushNanos, long maxFlushNanos,
          boolean timing) {

        public long totalPackets() {
            return commands.stream().mapToLong(CommandStats::packets).sum();
        }

        public long totalWireBytes() {
            return commands.stream().mapToLong(CommandStats::wireBytes).sum();
        }

        /** @return the commands that saw any traffic, busiest (by bytes on the wire) first */
        public List<CommandStats> busiest() {
            List<CommandStats> active = new ArrayList<>();
            for (CommandStats stats : commands) {
                if (stats.packets() > 0) {
                    active.add(stats);
                }
            }
            active.sort(Comparator.comparingLong(CommandStats::wireBytes).reversed()
                  .thenComparing(CommandStats::packets, Comparator.reverseOrder()));
            return active;
        }
    }

    private final LongAdder[] sent = adders();
    private final LongAdder[] received = adders();
    private final LongAdder[] rawBytesSent = adders();
    private final LongAdder[] wireBytesSent = adders();
    private final LongAdder[] wireBytesReceived = adders();
    private final LongAccumulator[] maxWireBytesSent = maxima();
    private final LongAccumulator[] maxWireBytesReceived = maxima();
    private final LongAdder[] marshalNanos = adders();
    private final LongAdder[] unmarshalNanos = adders();
    private final LongAdder[] writeNanos = adders();
    private final LongAdder flushCount = new LongAdder();
    private final LongAdder flushNanos = new LongAdder();
    private final LongAccumulator maxFlushNanos = new LongAccumulator(Math::max, 0);

    public NetStats() {
    }

    /** @return the instance used by connections that were not given one of their own */
    public static NetStats defaultInstance() {
        return DEFAULT;
    }

    /** @return true when the marshal/unmarshal/write timers are collecting */
    public static boolean isTimingEnabled() {
        return timingEnabled;
    }

    /** Switches the timers on or off for every instance in this JVM. */
    public static void setTimingEnabled(boolean enabled) {
        timingEnabled = enabled;
    }

    /**
     * @return {@code System.nanoTime()} if timing is enabled, otherwise 0. Pair with {@link #elapsedSince(long)} so
     *       the hot path pays for the clock only when asked to.
     */
    public static long now() {
        return timingEnabled ? System.nanoTime() : 0L;
    }

    /** @return nanoseconds since a {@link #now()} value, or 0 when timing was off at that point */
    public static long elapsedSince(long start) {
        return (start == 0L) ? 0L : System.nanoTime() - start;
    }

    /** Records one packet marshalled and handed to the socket. */
    public void recordSent(PacketCommand command, long rawBytes, long wireBytes, long marshalNanos) {
        int index = command.ordinal();
        sent[index].increment();
        rawBytesSent[index].add(rawBytes);
        wireBytesSent[index].add(wireBytes);
        maxWireBytesSent[index].accumulate(wireBytes);
        if (marshalNanos > 0) {
            this.marshalNanos[index].add(marshalNanos);
        }
    }

    /** Records one packet read from the socket and unmarshalled. */
    public void recordReceived(PacketCommand command, long wireBytes, long unmarshalNanos) {
        int index = command.ordinal();
        received[index].increment();
        wireBytesReceived[index].add(wireBytes);
        maxWireBytesReceived[index].accumulate(wireBytes);
        if (unmarshalNanos > 0) {
            this.unmarshalNanos[index].add(unmarshalNanos);
        }
    }

    /** Records the time one packet's bytes took to hand to the (buffered) socket stream. */
    public void recordWrite(PacketCommand command, long nanos) {
        if (nanos > 0) {
            writeNanos[command.ordinal()].add(nanos);
        }
    }

    /**
     * Records one flush of a connection's output stream. The flush is where a slow or stalled peer shows up, since
     * the writes before it only fill a buffer.
     */
    public void recordFlush(long nanos) {
        flushCount.increment();
        if (nanos > 0) {
            flushNanos.add(nanos);
            maxFlushNanos.accumulate(nanos);
        }
    }

    public long sent(PacketCommand command) {
        return sent[command.ordinal()].sum();
    }

    public long received(PacketCommand command) {
        return received[command.ordinal()].sum();
    }

    public long wireBytesSent(PacketCommand command) {
        return wireBytesSent[command.ordinal()].sum();
    }

    public long wireBytesReceived(PacketCommand command) {
        return wireBytesReceived[command.ordinal()].sum();
    }

    public long rawBytesSent(PacketCommand command) {
        return rawBytesSent[command.ordinal()].sum();
    }

    public long marshalNanos(PacketCommand command) {
        return marshalNanos[command.ordinal()].sum();
    }

    public long unmarshalNanos(PacketCommand command) {
        return unmarshalNanos[command.ordinal()].sum();
    }

    public long writeNanos(PacketCommand command) {
        return writeNanos[command.ordinal()].sum();
    }

    public long flushCount() {
        return flushCount.sum();
    }

    public long maxFlushNanos() {
        return maxFlushNanos.get();
    }

    /** @return a consistent-enough copy of every counter; individual counters are read one at a time */
    public Snapshot snapshot() {
        List<CommandStats> commands = new ArrayList<>(COMMANDS.length);
        for (int i = 0; i < COMMANDS.length; i++) {
            commands.add(new CommandStats(COMMANDS[i], sent[i].sum(), received[i].sum(), rawBytesSent[i].sum(),
                  wireBytesSent[i].sum(), wireBytesReceived[i].sum(), maxWireBytesSent[i].get(),
                  maxWireBytesReceived[i].get(), marshalNanos[i].sum(), unmarshalNanos[i].sum(),
                  writeNanos[i].sum()));
        }
        return new Snapshot(List.copyOf(commands), flushCount.sum(), flushNanos.sum(), maxFlushNanos.get(),
              timingEnabled);
    }

    /** Zeroes every counter. Events arriving concurrently may land on either side of the reset. */
    public void reset() {
        for (int i = 0; i < COMMANDS.length; i++) {
            sent[i].reset();
            received[i].reset();
            rawBytesSent[i].reset();
            wireBytesSent[i].reset();
            wireBytesReceived[i].reset();
            maxWireBytesSent[i].reset();
            maxWireBytesReceived[i].reset();
            marshalNanos[i].reset();
            unmarshalNanos[i].reset();
            writeNanos[i].reset();
        }
        flushCount.reset();
        flushNanos.reset();
        maxFlushNanos.reset();
    }

    /**
     * Renders the busiest commands as fixed-width text lines, one command per line, suitable for a log or a chat
     * window. Times are shown only when timing is enabled.
     *
     * @param topN how many commands to list; everything else is summed into a final "other" line
     */
    public List<String> format(int topN) {
        return format(snapshot(), topN);
    }

    public static List<String> format(Snapshot snapshot, int topN) {
        List<String> lines = new ArrayList<>();
        List<CommandStats> busiest = snapshot.busiest();
        boolean timing = snapshot.timing();
        lines.add(String.format("%-34s %7s %7s %10s %10s %9s %8s %8s %8s",
              "command", "sent", "recv", "bytes-out", "bytes-in", "raw-out", "max-out",
              timing ? "marsh-ms" : "", timing ? "unmar-ms" : ""));
        long otherPackets = 0;
        long otherBytes = 0;
        int shown = 0;
        for (CommandStats stats : busiest) {
            if (shown < topN) {
                lines.add(String.format("%-34s %7d %7d %10d %10d %9d %8d %8s %8s",
                      stats.command(), stats.sent(), stats.received(), stats.wireBytesSent(),
                      stats.wireBytesReceived(), stats.rawBytesSent(), stats.maxWireBytesSent(),
                      timing ? millis(stats.marshalNanos()) : "",
                      timing ? millis(stats.unmarshalNanos()) : ""));
                shown++;
            } else {
                otherPackets += stats.packets();
                otherBytes += stats.wireBytes();
            }
        }
        if (otherPackets > 0) {
            lines.add(String.format("%-34s %7s %7s %21d  (%d more commands)", "other", "", "", otherBytes,
                  busiest.size() - shown));
        }
        lines.add(String.format("total: %d packets, %d bytes on the wire, %d flushes%s",
              snapshot.totalPackets(), snapshot.totalWireBytes(), snapshot.flushCount(),
              timing ? String.format(", slowest flush %s ms", millis(snapshot.maxFlushNanos())) : ""));
        return lines;
    }

    private static String millis(long nanos) {
        return String.format(Locale.ROOT, "%.2f", nanos / 1_000_000.0);
    }

    private static LongAdder[] adders() {
        LongAdder[] result = new LongAdder[COMMANDS.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = new LongAdder();
        }
        return result;
    }

    private static LongAccumulator[] maxima() {
        LongAccumulator[] result = new LongAccumulator[COMMANDS.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = new LongAccumulator(Math::max, 0);
        }
        return result;
    }
}
