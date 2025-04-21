/*
 * MegaMek -
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import megamek.common.*;
import megamek.server.totalwarfare.TWGameManager;

/**
 * This class allows for dynamic Geysers to be added to maps which will go off
 * every few turns.
 */
public class GeyserProcessor extends DynamicTerrainProcessor {

    private List<GeyserInfo> geysers = null;

    /**
     * Create a new GeyserProcessor for the given server.
     * 
     * @param gameManager the game manager for which this runs.
     */
    public GeyserProcessor(TWGameManager gameManager) {
        super(gameManager);
    }

    @Override
    public void doEndPhaseChanges(Vector<Report> vPhaseReport) {
        // 1st time, find geysers on board
        if ((geysers == null) || (gameManager.getGame().getRoundCount() == 1)) {
            geysers = new ArrayList<>();
            findGeysers();
        }

        Report r;
        for (Iterator<GeyserInfo> gs = geysers.iterator(); gs.hasNext();) {
            GeyserInfo g = gs.next();
            if (g.turnsToGo > 0) {
                g.turnsToGo--;
            } else {
                Hex hex = gameManager.getGame().getHex(g.position, g.boardId);
                if (hex.terrainLevel(Terrains.GEYSER) == 2) {
                    r = new Report(5275, Report.PUBLIC);
                    r.add(g.position.getBoardNum());
                    vPhaseReport.add(r);
                    hex.removeTerrain(Terrains.GEYSER);
                    hex.addTerrain(new Terrain(Terrains.GEYSER, 1));
                    markHexUpdate(g.position, g.boardId);
                } else if (Compute.d6() == 1) {
                    if (hex.terrainLevel(Terrains.GEYSER) == 3) {
                        r = new Report(5285, Report.PUBLIC);
                        r.add(g.position.getBoardNum());
                        vPhaseReport.add(r);
                        hex.removeAllTerrains();
                        hex.addTerrain(new Terrain(Terrains.MAGMA, 2));
                        markHexUpdate(g.position, g.boardId);
                        gs.remove();
                        for (Entity e : gameManager.getGame().getEntitiesVector(g.position, g.boardId)) {
                            gameManager.doMagmaDamage(e, true);
                        }
                    } else {
                        r = new Report(5280, Report.PUBLIC);
                        r.add(g.position.getBoardNum());
                        vPhaseReport.add(r);
                        hex.removeTerrain(Terrains.GEYSER);
                        hex.addTerrain(new Terrain(Terrains.GEYSER, 2));
                        markHexUpdate(g.position, g.boardId);
                        g.turnsToGo = Compute.d6() - 1;
                    }
                }
            }
        }
    }

    private void findGeysers() {
        for (Board board : gameManager.getGame().getBoards().values()) {
            for (int x = 0; x < board.getWidth(); x++) {
                for (int y = 0; y < board.getHeight(); y++) {
                    if (board.getHex(x, y).containsTerrain(Terrains.GEYSER)) {
                        geysers.add(new GeyserInfo(new Coords(x, y), board.getBoardId()));
                    }
                }
            }
        }
    }

    private static class GeyserInfo {
        Coords position;
        int boardId;
        int turnsToGo;

        GeyserInfo(Coords c, int boardId) {
            position = c;
            this.boardId = boardId;
            turnsToGo = 0;
        }
    }
}
