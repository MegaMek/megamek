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

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import megamek.server.Server;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Tests for {@link TWGameManager#pollCFRPacket}: the CFR handlers must consume only the packet they are waiting for,
 * leave packets meant for other handlers untouched and in order, discard malformed packets that no handler could ever
 * consume, and block instead of busy-spinning when no matching packet is queued (the busy spin caused a server
 * livelock at 100% CPU).
 */
class TWGameManagerCFRPacketQueueTest {

    private final TWGameManager gameManager = new TWGameManager();

    /** A well-formed CFR response: the CFR type is the first data element, as all client responses send it. */
    private static Server.ReceivedPacket cfrResponse(int connectionId, PacketCommand cfrType) {
        return new Server.ReceivedPacket(connectionId, new Packet(PacketCommand.CLIENT_FEEDBACK_REQUEST, cfrType));
    }

    /** A malformed packet: no recognizable CFR type in the first data element. */
    private static Server.ReceivedPacket malformedPacket(int connectionId) {
        return new Server.ReceivedPacket(connectionId, new Packet(PacketCommand.CLIENT_FEEDBACK_REQUEST));
    }

    private static Predicate<Server.ReceivedPacket> hiddenPBSResponseFrom(int connectionId) {
        return rp -> {
            final PacketCommand cfrType = rp.getPacket().getPacketCommand(0);
            return (cfrType != null) && cfrType.isCFRHiddenPBS() && (rp.getConnectionId() == connectionId);
        };
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
    void returnsMatchingPacketAndRemovesItFromQueue() {
        Server.ReceivedPacket match = cfrResponse(42, PacketCommand.CFR_HIDDEN_PBS);
        gameManager.handleCfrPacket(match);

        Server.ReceivedPacket result = gameManager.pollCFRPacket(hiddenPBSResponseFrom(42));

        assertSame(match, result);
        assertTrue(gameManager.cfrPacketQueue.isEmpty(), "Consumed packet must be removed from the queue");
    }

    @Test
    @Timeout(10)
    void leavesPacketsForOtherHandlersInQueueInOriginalOrder() {
        Server.ReceivedPacket otherPlayer = cfrResponse(1, PacketCommand.CFR_HIDDEN_PBS);
        Server.ReceivedPacket otherType = cfrResponse(42, PacketCommand.CFR_TAG_TARGET);
        Server.ReceivedPacket match = cfrResponse(42, PacketCommand.CFR_HIDDEN_PBS);
        gameManager.handleCfrPacket(otherPlayer);
        gameManager.handleCfrPacket(otherType);
        gameManager.handleCfrPacket(match);

        Server.ReceivedPacket result = gameManager.pollCFRPacket(hiddenPBSResponseFrom(42));

        assertSame(match, result);
        assertEquals(List.of(otherPlayer, otherType), List.copyOf(gameManager.cfrPacketQueue),
              "Packets for other handlers must remain queued in their original order");
    }

    /**
     * Regression test for consuming malformed packets as responses: a packet from the awaited player without a
     * recognizable CFR type must not be mistaken for their answer (it previously read as "player declined").
     */
    @Test
    @Timeout(10)
    void malformedPacketFromAwaitedPlayerIsNotConsumedAsResponse() {
        gameManager.handleCfrPacket(malformedPacket(42));
        Server.ReceivedPacket realResponse = cfrResponse(42, PacketCommand.CFR_HIDDEN_PBS);
        gameManager.handleCfrPacket(realResponse);

        Server.ReceivedPacket result = gameManager.pollCFRPacket(hiddenPBSResponseFrom(42));

        assertSame(realResponse, result, "The real response must be returned, not the malformed packet");
    }

    /** Malformed packets can never match any handler, so they are discarded rather than accumulating forever. */
    @Test
    @Timeout(10)
    void malformedPacketsAreDiscarded() {
        gameManager.handleCfrPacket(malformedPacket(1));
        gameManager.handleCfrPacket(malformedPacket(2));
        Server.ReceivedPacket match = cfrResponse(42, PacketCommand.CFR_HIDDEN_PBS);
        gameManager.handleCfrPacket(match);

        Server.ReceivedPacket result = gameManager.pollCFRPacket(hiddenPBSResponseFrom(42));

        assertSame(match, result);
        assertTrue(gameManager.cfrPacketQueue.isEmpty(), "Malformed packets must be discarded, not retained");
    }

    /**
     * Regression test for the livelock: with only a mismatched packet in the queue, the old code re-queued it and
     * re-polled in a hot spin (thread stayed RUNNABLE forever). The fixed code must block in wait() and complete once
     * the matching packet arrives.
     */
    @Test
    @Timeout(10)
    void waitsInsteadOfSpinningWhenOnlyMismatchedPacketsAreQueued() throws Exception {
        Server.ReceivedPacket other = cfrResponse(1, PacketCommand.CFR_HIDDEN_PBS);
        gameManager.handleCfrPacket(other);

        AtomicReference<Server.ReceivedPacket> result = new AtomicReference<>();
        Thread poller = new Thread(() -> result.set(gameManager.pollCFRPacket(hiddenPBSResponseFrom(42))),
              "cfr-poller");
        poller.start();

        // The old implementation spun forever here (RUNNABLE); the fix must park in wait()
        awaitThreadState(poller, Thread.State.WAITING);

        Server.ReceivedPacket match = cfrResponse(42, PacketCommand.CFR_HIDDEN_PBS);
        gameManager.handleCfrPacket(match);
        poller.join(5000);

        assertSame(match, result.get());
        assertEquals(List.of(other), List.copyOf(gameManager.cfrPacketQueue),
              "The mismatched packet must still be queued");
    }

    @Test
    @Timeout(10)
    void returnsNullAndRestoresInterruptFlagWhenInterrupted() throws Exception {
        AtomicReference<Object> result = new AtomicReference<>("not yet run");
        AtomicBoolean interruptFlagRestored = new AtomicBoolean(false);
        Thread poller = new Thread(() -> {
            result.set(gameManager.pollCFRPacket(rp -> true));
            interruptFlagRestored.set(Thread.currentThread().isInterrupted());
        }, "cfr-poller-interrupted");
        poller.start();

        awaitThreadState(poller, Thread.State.WAITING);
        poller.interrupt();
        poller.join(5000);

        assertNull(result.get(), "An interrupted wait must return null");
        assertTrue(interruptFlagRestored.get(), "The interrupt flag must be restored after the InterruptedException");
    }
}
