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

public class ScreenProcessor extends DynamicTerrainProcessor {

    private Game game;
    Vector<Report> vPhaseReport;
    
    public ScreenProcessor(Server server) {
        super(server);
    }

    @Override
    void doEndPhaseChanges(Vector<Report> vPhaseReport) {
        game = server.getGame();
        this.vPhaseReport = vPhaseReport;
        resolveScreen();
        this.vPhaseReport = null;
        
    }

    /**
     * Check to see if screen clears
     */
    private void resolveScreen() {
        Board board = game.getBoard();
        int width = board.getWidth();
        int height = board.getHeight();

        // Cycle through all hexes, checking for screens
        for (int currentXCoord = 0; currentXCoord < width; currentXCoord++) {
            for (int currentYCoord = 0; currentYCoord < height; currentYCoord++) {
                Coords currentCoords = new Coords(currentXCoord, currentYCoord);
                Hex currentHex = board.getHex(currentXCoord, currentYCoord);

                // check for existence of screen
                if (currentHex.containsTerrain(Terrains.SCREEN)) {
                    if (Compute.d6(2) > 6) {
                        Report r = new Report(9075, Report.PUBLIC);
                        r.add(currentCoords.getBoardNum());
                        vPhaseReport.addElement(r);

                        currentHex.removeTerrain(Terrains.SCREEN);
                        server.getHexUpdateSet().add(currentCoords);
                    }
                }
            }

        }
    }
}
