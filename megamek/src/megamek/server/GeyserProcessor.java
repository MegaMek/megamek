/*

 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import megamek.common.Hex;
import megamek.common.Report;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.units.Entity;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;
import megamek.server.totalWarfare.TWGameManager;

/**
 * This class allows for dynamic Geysers to be added to maps which will go off every few turns.
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
        for (Iterator<GeyserInfo> gs = geysers.iterator(); gs.hasNext(); ) {
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
            if (board.isLowAltitude() || board.isSpace()) {
                continue;
            }
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
