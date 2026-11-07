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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import megamek.common.net.enums.PacketCommand;
import megamek.common.net.stats.NetStats.CommandStats;
import megamek.common.net.stats.NetStats.Snapshot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NetStatsTest {

    private boolean timingBefore;
    private NetStats stats;

    @BeforeEach
    void setUp() {
        timingBefore = NetStats.isTimingEnabled();
        stats = new NetStats();
    }

    @AfterEach
    void tearDown() {
        NetStats.setTimingEnabled(timingBefore);
    }

    @Test
    void countersStartAtZero() {
        Snapshot snapshot = stats.snapshot();

        assertEquals(PacketCommand.values().length, snapshot.commands().size(), "one row per command");
        assertEquals(0, snapshot.totalPackets());
        assertEquals(0, snapshot.totalWireBytes());
        assertTrue(snapshot.busiest().isEmpty());
        assertEquals(0, stats.flushCount());
    }

    @Test
    void sentAndReceivedAreCountedPerCommand() {
        stats.recordSent(PacketCommand.CHAT, 40, 60, 0);
        stats.recordSent(PacketCommand.CHAT, 42, 70, 0);
        stats.recordReceived(PacketCommand.PLAYER_READY, 55, 0);

        assertEquals(2, stats.sent(PacketCommand.CHAT));
        assertEquals(0, stats.received(PacketCommand.CHAT));
        assertEquals(82, stats.rawBytesSent(PacketCommand.CHAT));
        assertEquals(130, stats.wireBytesSent(PacketCommand.CHAT));
        assertEquals(1, stats.received(PacketCommand.PLAYER_READY));
        assertEquals(55, stats.wireBytesReceived(PacketCommand.PLAYER_READY));
        assertEquals(0, stats.sent(PacketCommand.SERVER_GREETING), "untouched commands stay at zero");

        CommandStats chat = stats.snapshot().commands().get(PacketCommand.CHAT.ordinal());
        assertEquals(PacketCommand.CHAT, chat.command());
        assertEquals(70, chat.maxWireBytesSent(), "the largest single frame is kept");
        assertEquals(130, chat.wireBytes());
        assertEquals(2, chat.packets());
    }

    @Test
    void busiestSortsByBytesOnTheWire() {
        stats.recordSent(PacketCommand.CHAT, 10, 10, 0);
        stats.recordSent(PacketCommand.SENDING_ENTITIES, 5000, 2000, 0);
        stats.recordReceived(PacketCommand.PLAYER_READY, 500, 0);

        List<CommandStats> busiest = stats.snapshot().busiest();

        assertEquals(3, busiest.size());
        assertEquals(PacketCommand.SENDING_ENTITIES, busiest.get(0).command());
        assertEquals(PacketCommand.PLAYER_READY, busiest.get(1).command());
        assertEquals(PacketCommand.CHAT, busiest.get(2).command());
    }

    @Test
    void timersRecordOnlyWhenTimingIsOn() {
        NetStats.setTimingEnabled(false);
        assertEquals(0L, NetStats.now(), "no clock read while timing is off");
        assertEquals(0L, NetStats.elapsedSince(0L));

        NetStats.setTimingEnabled(true);
        long started = NetStats.now();
        assertTrue(started != 0L, "a real clock value while timing is on");
        assertTrue(NetStats.elapsedSince(started) >= 0L);

        stats.recordSent(PacketCommand.CHAT, 10, 10, 1_500);
        stats.recordReceived(PacketCommand.CHAT, 10, 2_500);
        stats.recordWrite(PacketCommand.CHAT, 300);
        stats.recordFlush(4_000);
        stats.recordFlush(9_000);

        assertEquals(1_500, stats.marshalNanos(PacketCommand.CHAT));
        assertEquals(2_500, stats.unmarshalNanos(PacketCommand.CHAT));
        assertEquals(300, stats.writeNanos(PacketCommand.CHAT));
        assertEquals(2, stats.flushCount());
        assertEquals(9_000, stats.maxFlushNanos());
        assertTrue(stats.snapshot().timing());
    }

    @Test
    void zeroDurationsAreIgnoredButFlushesAreStillCounted() {
        stats.recordWrite(PacketCommand.CHAT, 0);
        stats.recordFlush(0);

        assertEquals(0, stats.writeNanos(PacketCommand.CHAT));
        assertEquals(1, stats.flushCount(), "a flush happened even if nobody timed it");
        assertEquals(0, stats.maxFlushNanos());
    }

    @Test
    void resetClearsEverything() {
        stats.recordSent(PacketCommand.CHAT, 10, 10, 5);
        stats.recordReceived(PacketCommand.CHAT, 10, 5);
        stats.recordFlush(7);

        stats.reset();

        Snapshot snapshot = stats.snapshot();
        assertEquals(0, snapshot.totalPackets());
        assertEquals(0, snapshot.totalWireBytes());
        assertEquals(0, snapshot.flushCount());
        assertEquals(0, snapshot.maxFlushNanos());
        assertEquals(0, stats.snapshot().commands().get(PacketCommand.CHAT.ordinal()).maxWireBytesSent());
    }

    @Test
    void concurrentUpdatesAreNotLost() throws InterruptedException {
        int threads = 4;
        int perThread = 50_000;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        for (int t = 0; t < threads; t++) {
            Thread worker = new Thread(() -> {
                try {
                    start.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                for (int i = 0; i < perThread; i++) {
                    stats.recordSent(PacketCommand.ENTITY_UPDATE, 3, 2, 1);
                    stats.recordReceived(PacketCommand.ENTITY_UPDATE, 2, 1);
                }
                done.countDown();
            });
            worker.setDaemon(true);
            worker.start();
        }
        start.countDown();
        assertTrue(done.await(30, TimeUnit.SECONDS));

        long expected = (long) threads * perThread;
        assertEquals(expected, stats.sent(PacketCommand.ENTITY_UPDATE));
        assertEquals(expected, stats.received(PacketCommand.ENTITY_UPDATE));
        assertEquals(expected * 2, stats.wireBytesSent(PacketCommand.ENTITY_UPDATE));
        assertEquals(expected * 3, stats.rawBytesSent(PacketCommand.ENTITY_UPDATE));
        assertEquals(expected, stats.marshalNanos(PacketCommand.ENTITY_UPDATE));
    }

    @Test
    void formatListsTheBusiestThenFoldsTheRest() {
        NetStats.setTimingEnabled(false);
        stats.recordSent(PacketCommand.SENDING_ENTITIES, 5000, 2000, 0);
        stats.recordSent(PacketCommand.CHAT, 10, 10, 0);
        stats.recordReceived(PacketCommand.PLAYER_READY, 500, 0);
        stats.recordFlush(0);

        List<String> lines = stats.format(2);

        // header + 2 rows + "other" + total
        assertEquals(5, lines.size(), String.join("\n", lines));
        assertTrue(lines.get(0).startsWith("command"));
        assertTrue(lines.get(1).startsWith("SENDING_ENTITIES"));
        assertTrue(lines.get(2).startsWith("PLAYER_READY"));
        assertTrue(lines.get(3).startsWith("other"));
        assertTrue(lines.get(3).contains("(1 more commands)"));
        assertTrue(lines.get(4).startsWith("total: 3 packets, 2510 bytes on the wire, 1 flushes"));
        assertFalse(lines.get(4).contains("slowest flush"), "no timing column while timing is off");
    }

    @Test
    void formatShowsTimesWhenTimingIsOn() {
        NetStats.setTimingEnabled(true);
        stats.recordSent(PacketCommand.CHAT, 10, 10, 2_000_000);
        stats.recordFlush(3_000_000);

        List<String> lines = stats.format(5);

        assertTrue(lines.get(0).contains("marsh-ms"));
        assertTrue(lines.get(1).contains("2.00"), lines.get(1));
        assertTrue(lines.get(lines.size() - 1).contains("slowest flush 3.00 ms"), lines.get(lines.size() - 1));
    }

    @Test
    void theDefaultInstanceIsShared() {
        assertSame(NetStats.defaultInstance(), NetStats.defaultInstance());
    }
}
