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
import megamek.server.gameManager.GameManager;

public class QuicksandProcessor extends DynamicTerrainProcessor {

    private Game game;
    Vector<Report> vPhaseReport;

    public QuicksandProcessor(GameManager gameManager) {
        super(gameManager);
    }

    @Override
    void doEndPhaseChanges(Vector<Report> vPhaseReport) {
        game = gameManager.getGame();
        this.vPhaseReport = vPhaseReport;
        resolveQuicksand();
        this.vPhaseReport = null;

    }

    /**
     * Check or quicksand stuff
     */
    private void resolveQuicksand() {
        Board board = game.getBoard();
        int width = board.getWidth();
        int height = board.getHeight();

        // Cycle through all hexes, checking for screens
        for (int currentXCoord = 0; currentXCoord < width; currentXCoord++) {
            for (int currentYCoord = 0; currentYCoord < height; currentYCoord++) {
                Coords currentCoords = new Coords(currentXCoord, currentYCoord);
                Hex currentHex = board.getHex(currentXCoord, currentYCoord);

                // Check for quicksand that has been around at least one turn (terrain level of 3),
                // then for any new quicksand this turn (terrain level of 2)
                if (currentHex.terrainLevel(Terrains.SWAMP) == 3) {
                    // sink any units that occupy this hex
                    for (Entity entity : game.getEntitiesVector(currentCoords)) {
                        if (entity.isStuck()) {
                            gameManager.doSinkEntity(entity);
                        }
                    }
                } else if (currentHex.terrainLevel(Terrains.SWAMP) == 2) {
                    currentHex.removeTerrain(Terrains.SWAMP);
                    currentHex.addTerrain(new Terrain(Terrains.SWAMP, 3));
                    gameManager.getHexUpdateSet().add(currentCoords);
                }
            }

        }
    }
}
