/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package megamek.server.nuclear;

import java.util.List;
import java.util.Vector;

import megamek.common.Hex;
import megamek.common.Report;
import megamek.common.board.Coords;
import megamek.common.game.Game;
import megamek.common.rolls.PilotingRollData;
import megamek.common.units.Entity;
import megamek.common.units.IBuilding;
import megamek.common.units.Tank;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;
import megamek.common.units.VTOL;
import megamek.server.totalWarfare.TWGameManager;

/**
 * Resolves the detonation of a nuclear weapon on a ground map: the crater, the blast, the terrain damage and the
 * secondary effects (IO:AE pp. 164-166).
 */
public class NuclearDetonationResolver {
    private final TWGameManager gameManager;
    private final Game game;

    /**
     * @param gameManager the game manager running the game the detonation happens in
     */
    public NuclearDetonationResolver(TWGameManager gameManager) {
        this.gameManager = gameManager;
        this.game = gameManager.getGame();
    }

    /**
     * explode a nuke
     *
     * @param position        the position that will be hit by the nuke
     * @param baseDamage      the base damage from the blast
     * @param degradation     how fast the blast's power degrades
     * @param secondaryRadius the secondary blast radius
     * @param craterDepth     the depth of the crater created by the blast
     * @param vDesc           a vector that contains the output report
     */
    public void resolve(Coords position, int baseDamage, int degradation, int secondaryRadius, int craterDepth,
          Vector<Report> vDesc) {
        // Just in case.
        if (vDesc == null) {
            vDesc = new Vector<>();
        }

        // First, crater the terrain.
        // All terrain, units, buildings... EVERYTHING in here is just gone.
        // Got to love nukes.
        Report r = new Report(1215, Report.PUBLIC);

        r.indent();
        r.add(position.getBoardNum(), true);
        vDesc.add(r);

        int curDepth = craterDepth;
        int range = 0;
        while (range < (2 * craterDepth)) {
            // Get the set of hexes at this range.
            List<Coords> hexSet = position.allAtDistance(range);

            // Iterate through the hexes.
            for (Coords myHexCoords : hexSet) {
                // ignore out of bounds coordinates
                if (!game.getBoard().contains(myHexCoords)) {
                    continue;
                }

                Hex myHex = game.getBoard().getHex(myHexCoords);
                // In each hex, first, sink the terrain if necessary.
                myHex.setLevel((myHex.getLevel() - curDepth));

                // Then, remove ANY terrains here.
                // I mean ALL of them; they're all just gone.
                // No ruins, no water, no rough, no nothing.
                if (myHex.containsTerrain(Terrains.WATER)) {
                    myHex.setLevel(myHex.floor());
                }
                myHex.removeAllTerrains();
                myHex.clearExits();

                gameManager.sendChangedHex(myHexCoords);
            }

            // Lastly, if the next distance is a multiple of 2...
            // The crater depth goes down one.
            if ((range > 0) && ((range % 2) == 0)) {
                curDepth--;
            }

            // Now that the hexes are dealt with, increment the distance.
            range++;
        }

        // This is technically part of cratering, but...
        // Now we destroy all the units inside the cratering range.
        for (Entity entity : game.getEntitiesVector()) {
            // loaded units and off board units don't have a position,
            // so we don't count 'em here
            if ((entity.getTransportId() != Entity.NONE) || (entity.getPosition() == null)) {
                continue;
            }

            // If it's too far away for this...
            if (position.distance(entity.getPosition()) >= range) {
                continue;
            }

            // If it's already destroyed...
            if (entity.isDestroyed()) {
                continue;
            }

            vDesc.addAll(gameManager.destroyEntity(entity, "nuclear explosion proximity", false, false));
            // Kill the crew
            entity.getCrew().setDoomed(true);
        }

        // Then, do actual blast damage.
        // Use the standard blast function for this.
        Vector<Report> tmpV = new Vector<>();
        Vector<Integer> blastedUnitsVec = new Vector<>();

        gameManager.doExplosion(baseDamage, degradation, true, position, true, tmpV, blastedUnitsVec, -1, false);
        Report.indentAll(tmpV, 2);
        vDesc.addAll(tmpV);

        // Everything that was blasted by the explosion has to make a piloting
        // check at +6.
        for (int i : blastedUnitsVec) {
            Entity o = game.getEntity(i);

            if (o == null) {
                continue;
            }

            if (o.canFall()) {
                // Needs a piloting check at +6 to avoid falling over.
                game.addPSR(new PilotingRollData(o.getId(), 6, "hit by nuclear blast"));
            } else if (o instanceof VTOL vt) {
                // Needs a piloting check at +6 to avoid crashing.

                // Check only applies if it's in the air.
                // FIXME: is this actually correct? What about
                // buildings/bridges?
                if (vt.getElevation() > 0) {
                    game.addPSR(new PilotingRollData(vt.getId(), 6, "hit by nuclear blast"));
                }
            } else if (o instanceof Tank) {
                // As per official answer on the rules questions board...
                // Needs a piloting check at +6 to avoid a 1-level fall...
                // But ONLY if a hover-tank.
                // TODO : Fix me
            }
        }

        // This ISN'T part of the blast, but if there's ANYTHING in the ground
        // zero hex, destroy it.
        IBuilding tmpB = game.getBoard().getBuildingAt(position);
        if (tmpB != null) {
            r = new Report(2415);
            r.add(tmpB.getName());
            gameManager.addReport(r);
            tmpB.setCurrentCF(0, position);
        }
        Hex gzHex = game.getBoard().getHex(position);
        if (gzHex.containsTerrain(Terrains.WATER)) {
            gzHex.setLevel(gzHex.floor());
        }
        gzHex.removeAllTerrains();

        // Next, for whatever's left, do terrain effects
        // such as clearing, roughing, and boiling off water.
        boolean damageFlag = true;
        int damageAtRange = baseDamage - (degradation * range);
        if (damageAtRange > 0) {
            for (int x = range; damageFlag; x++) {
                // Damage terrain as necessary.
                // Get all the hexes, and then iterate through them.
                List<Coords> hexSet = position.allAtDistance(x);

                // Iterate through the hexes.
                for (Coords myHexCoords : hexSet) {
                    // ignore out of bounds coordinates
                    if (!game.getBoard().contains(myHexCoords)) {
                        continue;
                    }

                    Hex myHex = game.getBoard().getHex(myHexCoords);

                    // For each 3000 damage, water level is reduced by 1.
                    if ((damageAtRange >= 3000) && (myHex.containsTerrain(Terrains.WATER))) {
                        int numCleared = damageAtRange / 3000;
                        int oldLevel = myHex.terrainLevel(Terrains.WATER);
                        myHex.removeTerrain(Terrains.WATER);
                        if (oldLevel > numCleared) {
                            myHex.setLevel(myHex.getLevel() - numCleared);
                            myHex.addTerrain(new Terrain(Terrains.WATER, oldLevel - numCleared));
                        } else {
                            myHex.setLevel(myHex.getLevel() - oldLevel);
                        }
                    }

                    // ANY non-water hex that takes 200 becomes rough.
                    if ((damageAtRange >= 200) && (!myHex.containsTerrain(Terrains.WATER))) {
                        myHex.removeAllTerrains();
                        myHex.clearExits();
                        myHex.addTerrain(new Terrain(Terrains.ROUGH, 1));
                    } else if ((damageAtRange >= 20) &&
                          ((myHex.containsTerrain(Terrains.WOODS)) ||
                                (myHex.containsTerrain(Terrains.JUNGLE)))) {
                        // Each 20 clears woods by 1 level.
                        int numCleared = damageAtRange / 20;
                        int terrainType = (myHex.containsTerrain(Terrains.WOODS) ? Terrains.WOODS : Terrains.JUNGLE);
                        int oldLevel = myHex.terrainLevel(terrainType);
                        int oldEl = myHex.terrainLevel(Terrains.FOLIAGE_ELEV);
                        myHex.removeTerrain(terrainType);
                        if (oldLevel > numCleared) {
                            myHex.addTerrain(new Terrain(terrainType, oldLevel - numCleared));
                            if (oldEl != 1) {
                                myHex.addTerrain(new Terrain(Terrains.FOLIAGE_ELEV,
                                      oldLevel - numCleared == 3 ? 3 : 2));
                            }
                        } else {
                            myHex.removeTerrain(Terrains.FOLIAGE_ELEV);
                        }
                    }

                    gameManager.sendChangedHex(myHexCoords);
                }

                // Initialize for the next iteration.
                damageAtRange = baseDamage - ((degradation * x) + 1);

                // If the damage is less than 20, it has no terrain effect.
                if (damageAtRange < 20) {
                    damageFlag = false;
                }
            }
        }

        // Lastly, do secondary effects.
        for (Entity entity : game.getEntitiesVector()) {
            // loaded units and off board units don't have a position,
            // so we don't count 'em here
            if ((entity.getTransportId() != Entity.NONE) || (entity.getPosition() == null)) {
                continue;
            }

            // If it's already destroyed...
            if ((entity.isDoomed()) || (entity.isDestroyed())) {
                continue;
            }

            // If it's too far away for this...
            if (position.distance(entity.getPosition()) > secondaryRadius) {
                continue;
            }

            // Actually do secondary effects against it.
            // Since the effects are unit-dependant, we'll just define it in the
            // entity.
            gameManager.applySecondaryNuclearEffects(entity, position, vDesc);
        }

        // All right. We're done.
        r = new Report(1216, Report.PUBLIC);
        r.indent();
        r.newlines = 2;
        vDesc.add(r);
    }
}
