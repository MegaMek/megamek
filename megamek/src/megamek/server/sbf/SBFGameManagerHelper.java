/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.server.sbf;

import megamek.common.ReportEntry;
import megamek.common.net.packets.Packet;
import megamek.common.strategicBattleSystems.SBFGame;
import megamek.server.GameManagerPacketHelper;

public interface SBFGameManagerHelper {

    SBFGameManager gameManager();

    default SBFGame game() {
        return gameManager().getGame();
    }

    default void addReport(ReportEntry reportEntry) {
        gameManager().addReport(reportEntry);
    }

    default void send(Packet packet) {
        gameManager().send(packet);
    }

    default void send(int playerId, Packet packet) {
        gameManager().send(playerId, packet);
    }

    default GameManagerPacketHelper packetHelper() {
        return gameManager().getPacketHelper();
    }
}
