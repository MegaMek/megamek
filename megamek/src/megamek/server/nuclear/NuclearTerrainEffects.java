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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import megamek.common.Hex;
import megamek.common.board.Coords;
import megamek.common.game.Game;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import megamek.common.units.IBuilding;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;
import megamek.logging.MMLogger;
import megamek.server.totalWarfare.TWGameManager;

/**
 * Changes the terrain around a nuclear detonation: the crater, the Ground Zero hex and the terrain damage of the blast
 * (IO:AE pp. 164-165).
 */
class NuclearTerrainEffects {
    private static final MMLogger LOGGER = MMLogger.create(NuclearTerrainEffects.class);
    /** Marks a kind of terrain damage that reached no ring at all, for the log. */
    private static final int NO_RING = -1;
    /** Rubble left in a crater hex that held no building. */
    static final int ORDINARY_RUBBLE = 1;

    private final TWGameManager gameManager;
    private final Game game;

    NuclearTerrainEffects(TWGameManager gameManager) {
        this.gameManager = gameManager;
        this.game = gameManager.getGame();
    }

    /**
     * Sinks the crater hexes and replaces everything in them with rubble (IO:AE p.164). A building in the crater
     * collapses into its own kind of rubble; every other crater hex gets ordinary rubble (HammerGS, 2026-10-28).
     *
     * @param groundZero   the Ground Zero hex
     * @param profile      the detonation's numbers
     * @param changedHexes filled with every hex this changes
     */
    void crater(Coords groundZero, NuclearBlastProfile profile, Set<Coords> changedHexes) {
        int craterRadius = profile.craterRadius();
        List<String> craterDepths = new ArrayList<>();
        for (int range = 0; range < craterRadius; range++) {
            // The crater is 1 level shallower for every 2 hexes from Ground Zero (IO:AE p.164).
            int curDepth = profile.craterDepthAt(range);
            craterDepths.add(String.valueOf(curDepth));
            // Get the set of hexes at this range.
            List<Coords> hexSet = groundZero.allAtDistance(range);

            // Iterate through the hexes.
            for (Coords myHexCoords : hexSet) {
                // ignore out of bounds coordinates
                if (!game.getBoard().contains(myHexCoords)) {
                    continue;
                }

                int rubbleLevel = collapseCraterBuilding(myHexCoords);
                Hex myHex = game.getBoard().getHex(myHexCoords);
                // In each hex, first, sink the terrain if necessary.
                myHex.setLevel((myHex.getLevel() - curDepth));

                // Then everything here is gone, and rubble is all that is left.
                if (myHex.containsTerrain(Terrains.WATER)) {
                    myHex.setLevel(myHex.floor());
                }
                myHex.removeAllTerrains();
                myHex.clearExits();
                myHex.addTerrain(new Terrain(Terrains.RUBBLE, rubbleLevel));

                changedHexes.add(myHexCoords);
            }
        }
        LOGGER.info("[Nukes] Crater: {}", craterDepths.isEmpty()
              ? "none"
              : "rings 0-" + (craterRadius - 1) + " lowered " + String.join(", ", craterDepths) + " level(s)");
    }

    /**
     * Takes a building in a crater hex off the board for the server and every client, the way a collapse does. The
     * units inside are not dropped or damaged here: the crater destroys them.
     *
     * @return the rubble the hex keeps: the building's own kind, or ordinary rubble if no building stood there
     */
    private int collapseCraterBuilding(Coords coords) {
        IBuilding building = game.getBoard().getBuildingAt(coords);
        if (building == null) {
            return ORDINARY_RUBBLE;
        }
        int rubbleLevel = building.getBuildingType().getTypeValue();
        building.setCurrentCF(0, coords);
        building.setPhaseCF(0, coords);
        gameManager.send(new Packet(PacketCommand.BLDG_COLLAPSE, new Vector<>(List.of(coords)),
              building.getBoardId()));
        game.getBoard().collapseBuilding(coords);
        LOGGER.info("[Nukes] Building {} at hex {} is in the crater: collapsed, leaves rubble level {}",
              building.getName(), coords.getBoardNum(), rubbleLevel);
        return rubbleLevel;
    }

    /**
     * Strips the Ground Zero hex bare, unless a building strong enough to withstand the impact still stands there
     * (IO:AE p.164); the blast has already destroyed any weaker one. A weapon that craters leaves Ground Zero as the
     * crater's rubble instead (HammerGS, 2026-10-29).
     *
     * @param groundZero   the Ground Zero hex
     * @param profile      the detonation's numbers
     * @param changedHexes filled with the Ground Zero hex if it changes
     */
    void clearGroundZero(Coords groundZero, NuclearBlastProfile profile, Set<Coords> changedHexes) {
        if (profile.craterRadius() > 0) {
            LOGGER.info("[Nukes] Ground Zero {} is in the crater: it keeps the crater's rubble", groundZero.getBoardNum());
            return;
        }
        IBuilding groundZeroBuilding = game.getBoard().getBuildingAt(groundZero);
        if ((groundZeroBuilding == null) || (groundZeroBuilding.getCurrentCF(groundZero) <= 0)) {
            Hex gzHex = game.getBoard().getHex(groundZero);
            if (gzHex.containsTerrain(Terrains.WATER)) {
                gzHex.setLevel(gzHex.floor());
            }
            gzHex.removeAllTerrains();
            changedHexes.add(groundZero);
        } else {
            LOGGER.info("[Nukes] The building at Ground Zero {} survives with CF {}", groundZero.getBoardNum(),
                  groundZeroBuilding.getCurrentCF(groundZero));
        }
    }

    /**
     * Damages the terrain beyond the crater: clearing, roughing, and boiling off water. Each ring takes the blast
     * damage for its own distance from Ground Zero (IO:AE p.165).
     *
     * @param groundZero   the Ground Zero hex
     * @param profile      the detonation's numbers
     * @param changedHexes filled with every hex this changes
     */
    void damageTerrain(Coords groundZero, NuclearBlastProfile profile, Set<Coords> changedHexes) {
        int craterRadius = profile.craterRadius();
        int lastRoughRing = NO_RING;
        int lastThinnedRing = NO_RING;
        int woodsTurnedRough = 0;
        for (int x = craterRadius; profile.damageAt(x) >= NuclearBlastProfile.MINIMUM_TERRAIN_DAMAGE; x++) {
            int damageAtRange = profile.damageAt(x);
            if (damageAtRange >= 200) {
                lastRoughRing = x;
            } else {
                lastThinnedRing = x;
            }
            // Damage terrain as necessary.
            // Get all the hexes, and then iterate through them.
            List<Coords> hexSet = groundZero.allAtDistance(x);

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

                // ANY non-water hex that takes 200 becomes rough, unless a building there survived the blast
                // (HammerGS, 2026-10-28); only Advanced Buildings are that strong.
                if ((damageAtRange >= 200) && (!myHex.containsTerrain(Terrains.WATER))) {
                    if (isStandingBuilding(myHexCoords, damageAtRange)) {
                        continue;
                    }
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
                        // Woods or jungle reduced below Light become Rough (IO:AE p.165)
                        myHex.removeTerrain(Terrains.FOLIAGE_ELEV);
                        myHex.addTerrain(new Terrain(Terrains.ROUGH, 1));
                        woodsTurnedRough++;
                    }
                }

                changedHexes.add(myHexCoords);
            }
        }

        int lastRing = craterRadius;
        while (profile.damageAt(lastRing) >= NuclearBlastProfile.MINIMUM_TERRAIN_DAMAGE) {
            lastRing++;
        }
        LOGGER.info("[Nukes] Terrain: dry hexes rough out to ring {}, woods and jungle thinned out to ring {}, "
              + "stops at ring {} ({} damage is below {})", ringOrNone(lastRoughRing), ringOrNone(lastThinnedRing),
              lastRing, profile.damageAt(lastRing), NuclearBlastProfile.MINIMUM_TERRAIN_DAMAGE);
        LOGGER.info("[Nukes] Terrain: {} woods or jungle hex(es) reduced below Light became Rough", woodsTurnedRough);
    }

    /**
     * @return {@code true} if a building still stands in the hex after the blast, so the hex keeps it
     */
    private boolean isStandingBuilding(Coords coords, int damage) {
        IBuilding building = game.getBoard().getBuildingAt(coords);
        if ((building == null) || (building.getCurrentCF(coords) <= 0)) {
            return false;
        }
        LOGGER.info("[Nukes] Building {} at hex {} survived {} damage with CF {}: the hex keeps it instead of turning "
              + "Rough", building.getName(), coords.getBoardNum(), damage, building.getCurrentCF(coords));
        return true;
    }

    private static String ringOrNone(int ring) {
        return (ring == NO_RING) ? "none" : String.valueOf(ring);
    }
}
