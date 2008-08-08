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

import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;

import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.IBoard;
import megamek.common.IHex;
import megamek.common.ITerrainFactory;
import megamek.common.Report;
import megamek.common.Terrains;

/**
 * This class allows for dynamic Geysers to be added to maps which will go off
 * every few turns.
 */
public class GeyserProcessor extends DynamicTerrainProcessor {

    private Vector<GeyserInfo> geysers = null;

    /**
     * Create a new GeyseProcessor for the given server.
     * 
     * @param server the server for which this runs.
     */
    public GeyserProcessor(Server server) {
        super(server);
    }

    @Override
    public void doEndPhaseChanges(Vector<Report> vPhaseReport) {
        // 1st time, find geysers on board
        if (geysers == null || server.getGame().getRoundCount() == 1) {
            geysers = new Vector<GeyserInfo>();
            findGeysers();
        }

        Report r;
        ITerrainFactory tf = Terrains.getTerrainFactory();
        for (Iterator<GeyserInfo> gs = geysers.iterator(); gs.hasNext();) {
            GeyserInfo g = gs.next();
            if (g.turnsToGo > 0) {
                g.turnsToGo--;
            } else {
                IHex hex = server.getGame().getBoard().getHex(g.position);
                if (hex.terrainLevel(Terrains.GEYSER) == 2) {
                    r = new Report(5275);
                    r.add(g.position.getBoardNum());
                    vPhaseReport.add(r);
                    hex.removeTerrain(Terrains.GEYSER);
                    hex.addTerrain(tf.createTerrain(Terrains.GEYSER, 1));
                    server.sendChangedHex(g.position);
                } else if (Compute.d6() == 1) {
                    if (hex.terrainLevel(Terrains.GEYSER) == 3) {
                        r = new Report(5285);
                        r.add(g.position.getBoardNum());
                        vPhaseReport.add(r);
                        hex.removeAllTerrains();
                        hex.addTerrain(tf.createTerrain(Terrains.MAGMA, 2));
                        server.sendChangedHex(g.position);
                        gs.remove();
                        for (Enumeration<Entity> e = server.getGame()
                                .getEntities(g.position); e.hasMoreElements();) {
                            server.doMagmaDamage(e.nextElement(), true);
                        }
                    } else {
                        r = new Report(5280);
                        r.add(g.position.getBoardNum());
                        vPhaseReport.add(r);
                        hex.removeTerrain(Terrains.GEYSER);
                        hex.addTerrain(tf.createTerrain(Terrains.GEYSER, 2));
                        server.sendChangedHex(g.position);
                        g.turnsToGo = Compute.d6() - 1;
                    }
                }
            }
        }
    }

    private void findGeysers() {
        IBoard b = server.getGame().getBoard();
        int height = b.getHeight();
        int width = b.getWidth();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (b.getHex(x, y).containsTerrain(Terrains.GEYSER)) {
                    geysers.add(new GeyserInfo(new Coords(x, y)));
                }
            }
        }
    }

    private class GeyserInfo {
        Coords position;
        int turnsToGo;

        GeyserInfo(Coords c) {
            position = c;
            turnsToGo = 0;
        }
    }
}
