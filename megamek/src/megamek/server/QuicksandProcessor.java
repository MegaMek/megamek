/*
 * MegaMek -
 * Copyright (C) 2000-2005 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.server;

import java.util.Vector;

import megamek.common.*;
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
            for (int currentXCoord = 0; currentXCoord < board.getWidth(); currentXCoord++) {
                for (int currentYCoord = 0; currentYCoord < board.getHeight(); currentYCoord++) {
                    Coords currentCoords = new Coords(currentXCoord, currentYCoord);
                    Hex currentHex = board.getHex(currentXCoord, currentYCoord);

                    // Check for quicksand that has been around at least one turn (terrain level of 3),
                    // then for any new quicksand this turn (terrain level of 2)
                    if (currentHex.terrainLevel(Terrains.SWAMP) == 3) {
                        // sink any units that occupy this hex
                        for (Entity entity : gameManager.getGame().getEntitiesVector(currentCoords, board.getBoardId())) {
                            if (entity.isStuck()) {
                                sinkEntityInQuicksand(entity);
                            }
                        }
                    } else if (currentHex.terrainLevel(Terrains.SWAMP) == 2) {
                        currentHex.removeTerrain(Terrains.SWAMP);
                        currentHex.addTerrain(new Terrain(Terrains.SWAMP, 3));
                        markHexUpdate(currentCoords, board.getBoardId());
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
