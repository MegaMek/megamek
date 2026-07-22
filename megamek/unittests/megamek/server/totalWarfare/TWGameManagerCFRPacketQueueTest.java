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
package megamek.server.totalWarfare;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import megamek.server.Server;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Tests for {@code TWGameManager.pollCFRPacket}: the CFR handlers must consume only the packet they are waiting for,
 * leave packets meant for other handlers untouched and in order, and block instead of busy-spinning when only
 * mismatched packets are queued (the busy spin caused a server livelock at 100% CPU).
 */
class TWGameManagerCFRPacketQueueTest {

    private final TWGameManager gameManager = new TWGameManager();

    @SuppressWarnings("unchecked")
    private Queue<Server.ReceivedPacket> cfrPacketQueue() throws Exception {
        Field field = TWGameManager.class.getDeclaredField("cfrPacketQueue");
        field.setAccessible(true);
        return (Queue<Server.ReceivedPacket>) field.get(gameManager);
    }

    private Server.ReceivedPacket invokePollCFRPacket(Predicate<Server.ReceivedPacket> isExpectedResponse)
          throws Exception {
        Method method = TWGameManager.class.getDeclaredMethod("pollCFRPacket", Predicate.class);
        method.setAccessible(true);
        return (Server.ReceivedPacket) method.invoke(gameManager, isExpectedResponse);
    }

    private static Server.ReceivedPacket packetFromConnection(int connectionId) {
        return new Server.ReceivedPacket(connectionId, new Packet(PacketCommand.CLIENT_FEEDBACK_REQUEST));
    }

    private static void awaitThreadState(Thread thread, Thread.State expected) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5000;
        while (thread.getState() != expected) {
            if (System.currentTimeMillis() > deadline) {
                fail("Timed out waiting for thread state " + expected + "; thread was " + thread.getState());
            }
            Thread.sleep(10);
        }
    }

    @Test
    @Timeout(10)
    void returnsMatchingPacketAndRemovesItFromQueue() throws Exception {
        Server.ReceivedPacket match = packetFromConnection(42);
        cfrPacketQueue().add(match);

        Server.ReceivedPacket result = invokePollCFRPacket(rp -> rp.getConnectionId() == 42);

        assertSame(match, result);
        assertTrue(cfrPacketQueue().isEmpty(), "Consumed packet must be removed from the queue");
    }

    @Test
    @Timeout(10)
    void leavesPacketsForOtherHandlersInQueueInOriginalOrder() throws Exception {
        Server.ReceivedPacket other1 = packetFromConnection(1);
        Server.ReceivedPacket other2 = packetFromConnection(2);
        Server.ReceivedPacket match = packetFromConnection(42);
        Queue<Server.ReceivedPacket> queue = cfrPacketQueue();
        queue.add(other1);
        queue.add(other2);
        queue.add(match);

        Server.ReceivedPacket result = invokePollCFRPacket(rp -> rp.getConnectionId() == 42);

        assertSame(match, result);
        assertEquals(List.of(other1, other2), List.copyOf(queue),
              "Packets for other handlers must remain queued in their original order");
    }

    /**
     * Regression test for the livelock: with only a mismatched packet in the queue, the old code re-queued it and
     * re-polled in a hot spin (thread stayed RUNNABLE forever). The fixed code must block in wait() and complete once
     * the matching packet arrives.
     */
    @Test
    @Timeout(10)
    void waitsInsteadOfSpinningWhenOnlyMismatchedPacketsAreQueued() throws Exception {
        Server.ReceivedPacket other = packetFromConnection(1);
        Queue<Server.ReceivedPacket> queue = cfrPacketQueue();
        queue.add(other);

        AtomicReference<Server.ReceivedPacket> result = new AtomicReference<>();
        Thread poller = new Thread(() -> {
            try {
                result.set(invokePollCFRPacket(rp -> rp.getConnectionId() == 42));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, "cfr-poller");
        poller.start();

        // The old implementation spun forever here (RUNNABLE); the fix must park in wait()
        awaitThreadState(poller, Thread.State.WAITING);

        Server.ReceivedPacket match = packetFromConnection(42);
        synchronized (queue) {
            queue.add(match);
            queue.notifyAll();
        }
        poller.join(5000);

        assertSame(match, result.get());
        assertEquals(List.of(other), List.copyOf(queue), "The mismatched packet must still be queued");
    }

    @Test
    @Timeout(10)
    void returnsNullWhenInterruptedWhileWaiting() throws Exception {
        AtomicReference<Object> result = new AtomicReference<>("not yet run");
        Thread poller = new Thread(() -> {
            try {
                result.set(invokePollCFRPacket(rp -> true));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, "cfr-poller-interrupted");
        poller.start();

        awaitThreadState(poller, Thread.State.WAITING);
        poller.interrupt();
        poller.join(5000);

        assertNull(result.get(), "An interrupted wait must return null");
    }
}
