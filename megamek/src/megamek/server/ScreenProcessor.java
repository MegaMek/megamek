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

import java.util.Vector;

import megamek.common.Hex;
import megamek.common.Report;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.units.Terrains;
import megamek.server.totalWarfare.TWGameManager;

public class ScreenProcessor extends DynamicTerrainProcessor {

    public ScreenProcessor(TWGameManager gameManager) {
        super(gameManager);
    }

    @Override
    public void doEndPhaseChanges(Vector<Report> vPhaseReport) {
        resolveScreen(vPhaseReport);
    }

    /**
     * For each hex of each space board, tests if it contains SCREEN (terrain) and if so, rolls to see if it clears.
     */
    private void resolveScreen(Vector<Report> vPhaseReport) {
        for (Board board : gameManager.getGame().getBoards().values()) {
            if (!board.isSpace()) {
                continue;
            }
            // Cycle through all hexes, checking for screens
            for (int x = 0; x < board.getWidth(); x++) {
                for (int y = 0; y < board.getHeight(); y++) {
                    Hex currentHex = board.getHex(x, y);
                    if (currentHex.containsTerrain(Terrains.SCREEN)) {
                        if (Compute.d6(2) > 6) {
                            Coords currentCoords = new Coords(x, y);
                            vPhaseReport.addElement(Report.publicReport(9075).add(currentCoords.getBoardNum()));
                            currentHex.removeTerrain(Terrains.SCREEN);
                            markHexUpdate(currentCoords, board);
                        }
                    }
                }
            }
        }
    }
}
