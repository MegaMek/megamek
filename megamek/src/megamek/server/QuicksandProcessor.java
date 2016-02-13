/*
 * MegaMek -
 * Copyright (C) 2000,2001,2002,2003,2004,2005 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.server;

import java.util.Vector;

import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.IBoard;
import megamek.common.IGame;
import megamek.common.IHex;
import megamek.common.Report;
import megamek.common.Terrains;

public class QuicksandProcessor extends DynamicTerrainProcessor {

    private IGame game;
    Vector<Report> vPhaseReport;

    public QuicksandProcessor(Server server) {
        super(server);
    }

    @Override
    void doEndPhaseChanges(Vector<Report> vPhaseReport) {
        game = server.getGame();
        this.vPhaseReport = vPhaseReport;
        resolveQuicksand();
        this.vPhaseReport = null;

    }

    /**
     * This debug/profiling function will print the current time
     * (in milliseconds) to the log.  If the boolean is true, the
     * garbage collector will be called in an attempt to minimize
     * timing errors.  You should try and minimize applications
     * being run in the background when using this function.
     * Note that MS Windows only has 10 milisecond resolution.
     *
     * The function should be optimized completely out of the code
     * when the first if-statement below reads "if (false)...", so
     * performance shouldn't be impacted if you leave calls to this
     * function in the code (I think).
     */
    @SuppressWarnings("unused")
    private void debugTime(String s, boolean collectGarbage) {
        //Change the "false" below to "true" to enable this function
        if (false) {
            if (collectGarbage) {
                System.gc();
            }
            System.out.println(s + ": " + System.currentTimeMillis());
        }
    }

    /**
     * Check or quicksand stuff
     */
    private void resolveQuicksand() {
        IBoard board = game.getBoard();
        int width = board.getWidth();
        int height = board.getHeight();
        // Cycle through all hexes, checking for screens
        debugTime("resolve quicksand 1", true);

        for (int currentXCoord = 0; currentXCoord < width; currentXCoord++ ) {

            for (int currentYCoord = 0; currentYCoord < height; currentYCoord++) {
                Coords currentCoords = new Coords(currentXCoord, currentYCoord);
                IHex currentHex = board.getHex(currentXCoord, currentYCoord);

                //check for quicksand that has been around at least one turn
                if(currentHex.terrainLevel(Terrains.SWAMP) == 3) {
                    //sink any units that occupy this hex
                    for (Entity entity : game.getEntitiesVector(currentCoords)) {
                        if(entity.isStuck()) {
                            server.doSinkEntity(entity);
                        }
                    }
                }
                // check for any quicksand created this turn
                else if (currentHex.terrainLevel(Terrains.SWAMP) == 2){
                    currentHex.removeTerrain(Terrains.SWAMP);
                    currentHex.addTerrain(Terrains.getTerrainFactory().createTerrain(Terrains.SWAMP, 3));
                    server.getHexUpdateSet().add(currentCoords);
                }
            }

        }
        debugTime("resolve quicksand 1 end", true);
    }

}
