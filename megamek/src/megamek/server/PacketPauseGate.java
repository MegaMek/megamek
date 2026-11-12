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

import java.util.ArrayList;
import java.util.List;

import megamek.server.Server.ReceivedPacket;

/**
 * Holds the server's pause state. While the game is paused, incoming packets are held here instead of being processed,
 * and they are released in order when the game is unpaused.
 *
 * <p>The packets are received on the connection threads, while a reset to the lobby runs on whichever thread handled
 * the reset, so every method is synchronized.</p>
 */
class PacketPauseGate {

    private boolean isPaused = false;
    private final List<ReceivedPacket> heldPackets = new ArrayList<>();

    /**
     * Pauses packet handling.
     *
     * @return {@code true} if the game was not paused before this call
     */
    synchronized boolean pause() {
        boolean wasPaused = isPaused;
        isPaused = true;
        return !wasPaused;
    }

    /** @return {@code true} while packet handling is paused */
    synchronized boolean isPaused() {
        return isPaused;
    }

    /**
     * Holds the packet if the game is paused.
     *
     * @param receivedPacket the packet just received
     *
     * @return {@code true} if the packet was held and must not be processed now; {@code false} if the game is not
     *       paused and the caller should process it
     */
    synchronized boolean holdIfPaused(ReceivedPacket receivedPacket) {
        if (isPaused) {
            heldPackets.add(receivedPacket);
        }
        return isPaused;
    }

    /**
     * Unpauses packet handling and hands back the packets held during the pause, in the order they arrived, for the
     * caller to process.
     *
     * @return the held packets; empty if none were held
     */
    synchronized List<ReceivedPacket> unpause() {
        isPaused = false;
        List<ReceivedPacket> releasedPackets = new ArrayList<>(heldPackets);
        heldPackets.clear();
        return releasedPackets;
    }

    /**
     * Ends the pause as the game returns to the lobby. The held packets belong to the game that was just ended (moves,
     * attacks, phase Done signals), so they are thrown away rather than processed in the lobby.
     *
     * @return the number of held packets thrown away
     */
    synchronized int endPauseForLobby() {
        int discardedPackets = heldPackets.size();
        heldPackets.clear();
        isPaused = false;
        return discardedPackets;
    }
}
