/*

 * Copyright (C) 2000-2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

import java.util.Collection;
import java.util.Vector;

import megamek.common.board.Board;
import megamek.common.board.BoardLocation;
import megamek.common.board.Coords;
import megamek.common.Report;
import megamek.server.totalwarfare.TWGameManager;

public abstract class DynamicTerrainProcessor {
    protected TWGameManager gameManager;

    DynamicTerrainProcessor(TWGameManager gameManager) {
        this.gameManager = gameManager;
    }

    /**
     * Process terrain changes in the end phase
     *
     * @param vPhaseReport reports for the server to send out
     */
    public abstract void doEndPhaseChanges(Vector<Report> vPhaseReport);

    /**
     * Marks the given hex as changed. All changes are sent to the clients after terrain processing is completed.
     *
     * @param coords  The coords of the hex
     * @param boardId The board ID of the hex
     */
    void markHexUpdate(Coords coords, int boardId) {
        gameManager.getHexUpdateSet().add(BoardLocation.of(coords, boardId));
    }

    /**
     * Marks the given hex on the given Board as changed. All changes are sent to the clients after terrain processing
     * is completed.
     *
     * @param coords The coords of the hex
     * @param board  The board of the hex
     */
    void markHexUpdate(Coords coords, Board board) {
        markHexUpdate(coords, board.getBoardId());
    }

    /**
     * Marks the given hexes as changed. All changes are sent to the clients after terrain processing is completed.
     *
     * @param coords  The coords of the hexes
     * @param boardId The board ID of all hexes
     */
    void markHexUpdate(Collection<Coords> coords, int boardId) {
        coords.forEach(c -> markHexUpdate(c, boardId));
    }
}
