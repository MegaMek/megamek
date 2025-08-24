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

import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.units.Entity;
import megamek.common.Hex;
import megamek.common.Report;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;
import megamek.server.totalwarfare.TWGameManager;

public class QuicksandProcessor extends DynamicTerrainProcessor {

    public QuicksandProcessor(TWGameManager gameManager) {
        super(gameManager);
    }

    @Override
    public void doEndPhaseChanges(Vector<Report> vPhaseReport) {
        resolveQuicksand();
    }

    private void resolveQuicksand() {
        for (Board board : gameManager.getGame().getBoards().values()) {
            if (board.isLowAltitude() || board.isSpace()) {
                continue;
            }
            // Cycle through all hexes, checking for quicksand
            for (int x = 0; x < board.getWidth(); x++) {
                for (int y = 0; y < board.getHeight(); y++) {
                    Coords currentCoords = new Coords(x, y);
                    Hex hex = board.getHex(x, y);

                    // Check for quicksand that has been around at least one turn (terrain level of 3),
                    // then for any new quicksand this turn (terrain level of 2)
                    if (hex.terrainLevel(Terrains.SWAMP) == 3) {
                        // sink any units that occupy this hex
                        for (Entity entity : gameManager.getGame()
                              .getEntitiesVector(currentCoords, board.getBoardId())) {
                            if (entity.isStuck()) {
                                sinkEntityInQuicksand(entity);
                            }
                        }
                    } else if (hex.terrainLevel(Terrains.SWAMP) == 2) {
                        hex.addTerrain(new Terrain(Terrains.SWAMP, 3));
                        markHexUpdate(currentCoords, board);
                    }
                }
            }
        }
    }

    private void sinkEntityInQuicksand(Entity entity) {
        gameManager.addReport(new Report(2445).with(entity));
        entity.setElevation(entity.getElevation() - 1);
        // if this means the entity is below the ground, then bye-bye!
        if (Math.abs(entity.getElevation()) > entity.getHeight()) {
            gameManager.addReport(gameManager.destroyEntity(entity, "quicksand"));
        }
    }
}
