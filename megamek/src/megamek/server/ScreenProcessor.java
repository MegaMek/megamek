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

public class ScreenProcessor extends DynamicTerrainProcessor {

    Vector<Report> vPhaseReport;

    public ScreenProcessor(TWGameManager gameManager) {
        super(gameManager);
    }

    @Override
    public void doEndPhaseChanges(Vector<Report> vPhaseReport) {
        this.vPhaseReport = vPhaseReport;
        resolveScreen();
        this.vPhaseReport = null;
    }

    /**
     * Check to see if screen clears
     */
    private void resolveScreen() {
        for (Board board : gameManager.getGame().getBoards().values()) {
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
