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

package megamek.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import megamek.server.Server.ReceivedPacket;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests the server's pause gate: packets are held while paused, released in order on unpause, and thrown away when the
 * game returns to the lobby while paused.
 */
class PacketPauseGateTest {

    private static final int CONNECTION_ID = 0;

    private final PacketPauseGate pauseGate = new PacketPauseGate();

    private static ReceivedPacket doneReceivedPacket() {
        return new ReceivedPacket(CONNECTION_ID, new Packet(PacketCommand.PLAYER_READY, true));
    }

    @Test
    @DisplayName("When not paused, packets pass straight through")
    void packetsPassWhenNotPaused() {
        assertFalse(pauseGate.holdIfPaused(doneReceivedPacket()), "an unpaused game should not hold a packet");
        assertTrue(pauseGate.unpause().isEmpty(), "nothing should have been held");
    }

    @Test
    @DisplayName("While paused, packets are held and released in order on unpause")
    void packetsHeldWhilePausedAndReleasedInOrder() {
        ReceivedPacket firstPacket = doneReceivedPacket();
        ReceivedPacket secondPacket = doneReceivedPacket();

        assertTrue(pauseGate.pause(), "the first pause should report that the game was newly paused");
        assertFalse(pauseGate.pause(), "a second pause should report that the game was already paused");
        assertTrue(pauseGate.holdIfPaused(firstPacket), "a paused game should hold the packet");
        assertTrue(pauseGate.holdIfPaused(secondPacket), "a paused game should hold the packet");

        List<ReceivedPacket> releasedPackets = pauseGate.unpause();

        assertEquals(2, releasedPackets.size(), "both held packets should be released");
        assertSame(firstPacket, releasedPackets.get(0), "the packets should be released in the order they arrived");
        assertSame(secondPacket, releasedPackets.get(1), "the packets should be released in the order they arrived");
        assertFalse(pauseGate.isPaused(), "the game should no longer be paused");
    }

    @Test
    @DisplayName("Returning to the lobby while paused ends the pause and throws the held packets away")
    void lobbyEndsPauseAndDiscardsHeldPackets() {
        pauseGate.pause();
        pauseGate.holdIfPaused(doneReceivedPacket());
        pauseGate.holdIfPaused(doneReceivedPacket());

        int discardedPackets = pauseGate.endPauseForLobby();

        assertEquals(2, discardedPackets, "both held packets should be thrown away");
        assertFalse(pauseGate.isPaused(), "the lobby should not be paused");
        assertFalse(pauseGate.holdIfPaused(doneReceivedPacket()),
              "a Done press in the lobby should pass straight through");
        assertTrue(pauseGate.unpause().isEmpty(), "the old game's packets should not be released later");
    }
}
