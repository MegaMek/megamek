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
package megamek.client.bot.princess;

import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;

import megamek.common.enums.GamePhase;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * A movement turn waits for the Precognition thread to pause. These tests check that the wait always ends, whatever
 * happens to that thread (issue #9476: the game froze on the bot's movement turn with nothing in the log).
 */
class PrecognitionTest {

    private static final Duration TURN_LIMIT = Duration.ofSeconds(10);

    private Precognition precognition;
    private Thread precognitionThread;

    @AfterEach
    void stopPrecognition() {
        if (precognition != null) {
            precognition.signalDone();
        }
        if (precognitionThread != null) {
            precognitionThread.interrupt();
        }
    }

    private Precognition createPrecognition() {
        Princess owner = mock(Princess.class);
        when(owner.getName()).thenReturn("Test Bot");
        return spy(new Precognition(owner));
    }

    /**
     * Queues a phase change to movement. Processing it calls {@code getPathEnumerator().clear()}, which each test
     * stubs to fail.
     */
    private void queueMovementPhase() {
        precognition.handlePacket(new Packet(PacketCommand.PHASE_CHANGE, GamePhase.MOVEMENT));
    }

    private void startPrecognitionThread() {
        precognitionThread = new Thread(precognition, "Precognition test thread");
        precognitionThread.setDaemon(true);
        // Keep a deliberately thrown Error out of the test output
        precognitionThread.setUncaughtExceptionHandler((thread, throwable) -> { });
        precognitionThread.start();
    }

    @Test
    void failedPassKeepsThreadAliveAndMovementTurnEnds() {
        precognition = createPrecognition();
        doThrow(new IllegalStateException("simulated failure")).when(precognition).getPathEnumerator();
        queueMovementPhase();
        startPrecognitionThread();
        // The thread must actually hit the failure, or the turn below would end for the wrong reason
        verify(precognition, timeout(5000).atLeastOnce()).getPathEnumerator();

        assertTimeoutPreemptively(TURN_LIMIT, precognition::ensureUpToDate);
        assertTrue(precognitionThread.isAlive(), "One failed pass must not end the Precognition thread");
    }

    @Test
    void deadThreadDoesNotFreezeMovementTurn() {
        precognition = createPrecognition();
        doThrow(new AssertionError("simulated fatal error")).when(precognition).getPathEnumerator();
        queueMovementPhase();
        startPrecognitionThread();
        // The thread must actually hit the failure, or the turn below would end for the wrong reason
        verify(precognition, timeout(5000).atLeastOnce()).getPathEnumerator();

        assertTimeoutPreemptively(TURN_LIMIT, precognition::ensureUpToDate);
    }
}
