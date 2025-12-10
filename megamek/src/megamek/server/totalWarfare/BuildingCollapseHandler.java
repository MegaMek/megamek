/*
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

package megamek.server.totalWarfare;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import megamek.common.Hex;
import megamek.common.HitData;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.board.BoardLocation;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.equipment.GunEmplacement;
import megamek.common.equipment.MiscType;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import megamek.common.rolls.PilotingRollData;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.IBuilding;
import megamek.common.units.Infantry;
import megamek.common.units.Mek;
import megamek.common.units.ProtoMek;
import megamek.common.units.Tank;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;
import megamek.logging.MMLogger;

public class BuildingCollapseHandler extends AbstractTWRuleHandler {

    private static final MMLogger LOGGER = MMLogger.create(BuildingCollapseHandler.class);

    BuildingCollapseHandler(TWGameManager gameManager) {
        super(gameManager);
    }

    /**
     * Determine if the given building should collapse. If so, inflict the appropriate amount of damage on each entity
     * in the building and update the clients. If the building does not collapse, determine if any entities crash
     * through its floor into its basement. Again, apply appropriate damage.
     *
     * @param bldg                 the Building being checked
     * @param coords               the Coords of the building hex to be checked
     * @param checkBecauseOfDamage ?
     * @param vPhaseReport         The current phase reports to attach new reports to
     *
     * @return True if the building hex collapsed.
     */
    public boolean checkForCollapse(IBuilding bldg, Coords coords, boolean checkBecauseOfDamage,
          Vector<Report> vPhaseReport) {
        return checkForCollapse(bldg, getGame().getPositionMapMulti(), coords, checkBecauseOfDamage, vPhaseReport);
    }

    /**
     * Determine if the given building should collapse. If so, inflict the appropriate amount of damage on each entity
     * in the building and update the clients. If the building does not collapse, determine if any entities crash
     * through its floor into its basement. Again, apply appropriate damage.
     *
     * @param bldg        the Building being checked.
     * @param positionMap a Hashtable that maps the Coords positions or each unit in the game to a Vector of Entity's at
     *                    that position.
     * @param coords      the Coords of the building hex to be checked
     *
     * @return true if the building collapsed.
     */
    boolean checkForCollapse(IBuilding bldg, Map<BoardLocation, List<Entity>> positionMap, Coords coords,
          boolean checkBecauseOfDamage, Vector<Report> vPhaseReport) {

        // If the input is meaningless, do nothing and throw no exception.
        if ((bldg == null) || (positionMap == null) || positionMap.isEmpty() || (coords == null)
              || !bldg.isIn(coords) || !bldg.hasCFIn(coords)) {
            LOGGER.error("Illegal/null arguments");
            return false;
        }

        int currentCF = bldg.getCurrentCF(coords);

        // Track all units that fall into the building's basement by Coords.
        Map<BoardLocation, List<Entity>> basementMap = new HashMap<>();

        // look for a collapse.
        boolean collapse = false;
        boolean basementCollapse = false;
        boolean topFloorCollapse = false;

        if (checkBecauseOfDamage && (currentCF <= 0)) {
            collapse = true;
        }

        final List<Entity> unitsInHex = positionMap.get(BoardLocation.of(coords, bldg.getBoardId()));

        // Are there any Entities at these coords?
        if (unitsInHex != null) {
            // How many levels does this building have in this hex?
            final Hex curHex = getGame().getBoard(bldg.getBoardId()).getHex(coords);
            final int numFloors = Math.max(0, curHex.terrainLevel(Terrains.BLDG_ELEV));
            final int bridgeEl = curHex.terrainLevel(Terrains.BRIDGE_ELEV);
            int numLoads = numFloors;
            if (bridgeEl != Terrain.LEVEL_NONE) {
                numLoads++;
            }
            if (numLoads < 1) {
                LOGGER.error("Check for collapse: hex {} has no bridge or building", coords);
                return false;
            }

            // Track the load of each floor (and of the roof) separately.
            // Track all units that fall into the basement in this hex.
            // track all floors, ground at index 0, the first floor is at
            // index 1, the second is at index 2, etc., and the roof is
            // at index (numFloors).
            // if bridge is present, bridge will be numFloors+1
            double[] loads = new double[numLoads + 1];
            // WiGEs flying over the building are also tracked, but can only collapse the
            // top floor
            // and only count 25% of their tonnage.
            double wigeLoad = 0;
            // track all units that might fall into the basement
            Vector<Entity> basement = new Vector<>();

            boolean recheckLoop = true;
            for (int i = 0; (i < 2) && recheckLoop; i++) {
                recheckLoop = false;
                Arrays.fill(loads, 0);

                // Walk through the entities in this position.
                Vector<Entity> unitsAsVector = new Vector<>(unitsInHex);
                Enumeration<Entity> entities = unitsAsVector.elements();
                while (!collapse && entities.hasMoreElements()) {
                    final Entity entity = entities.nextElement();
                    // WiGEs can collapse the top floor of a building by flying over it.
                    final int entityElev = entity.getElevation();
                    final boolean wigeFlyover = entity.getMovementMode() == EntityMovementMode.WIGE &&
                          entityElev == numFloors + 1;

                    if (entityElev != bridgeEl && !wigeFlyover) {
                        // Ignore entities not *inside* the building
                        if (entityElev > numFloors) {
                            continue;
                        }
                    }

                    // if we're under a bridge, we can't collapse the bridge
                    if (entityElev < bridgeEl) {
                        continue;
                    }

                    if ((entity.getMovementMode() == EntityMovementMode.HYDROFOIL) ||
                          (entity.getMovementMode() == EntityMovementMode.NAVAL) ||
                          (entity.getMovementMode() == EntityMovementMode.SUBMARINE) ||
                          (entity.getMovementMode() == EntityMovementMode.INF_UMU) ||
                          entity.hasWorkingMisc(MiscType.F_FULLY_AMPHIBIOUS)) {
                        continue; // under the bridge even at same level
                    }

                    if (entityElev == 0) {
                        basement.add(entity);
                    }

                    // units already in the basement
                    if (entityElev < 0) {
                        continue;
                    }

                    // Add the weight to the correct floor.
                    double load = entity.getWeight();
                    int floor = entityElev;
                    if (floor == bridgeEl) {
                        floor = numLoads;
                    }
                    // Entities on the roof fall to the previous top floor/new roof
                    if (topFloorCollapse && floor == numFloors) {
                        floor--;
                    }

                    if (wigeFlyover) {
                        wigeLoad += load;
                        if (wigeLoad > currentCF * 4) {
                            topFloorCollapse = true;
                            // There are bridges with 0 elevation, so the numFloors is 0, meaning that
                            // loads[numFloors-1] would cause an out-of-bounds exception.
                            // which is why there are so many checks and safeguards in the next few lines.
                            if (numFloors < loads.length) {
                                if (numFloors > 0) {
                                    loads[numFloors - 1] += loads[numFloors];
                                }
                                loads[numFloors] = 0;
                            }
                        }
                    } else {
                        loads[floor] += load;
                        if (loads[floor] > currentCF) {
                            // If the load on any floor but the first floor
                            // exceeds the building's current CF it collapses.
                            if (floor != 0) {
                                collapse = true;
                            } else if (!bldg.getBasementCollapsed(coords)) {
                                basementCollapse = true;
                            }
                        }
                    } // End increase-load
                } // Handle the next entity.

                // Track all entities that fell into the basement.
                if (basementCollapse) {
                    basementMap.put(BoardLocation.of(coords, bldg.getBoardId()), basement);
                }

                // did anyone fall into the basement?
                if (!basementMap.isEmpty() && !bldg.getBasement(coords).isNone() && !collapse) {
                    collapseBasement(bldg, basementMap, coords, vPhaseReport);
                    if (currentCF == 0) {
                        collapse = true;
                    } else {
                        recheckLoop = true; // basement collapse might cause a further collapse
                    }
                }
            } // End have-entities-here
        }

        // Collapse the building if the flag is set.
        if (collapse) {
            Report r = new Report(2375, Report.PUBLIC);
            r.add(bldg.getName());
            vPhaseReport.add(r);

            collapseBuilding(bldg, positionMap, coords, false, vPhaseReport);
        } else if (topFloorCollapse) {
            Report r = new Report(2376, Report.PUBLIC);
            r.add(bldg.getName());
            vPhaseReport.add(r);

            collapseBuilding(bldg, positionMap, coords, false, true, vPhaseReport);
        }

        // Return true if the building collapsed.
        return collapse || topFloorCollapse;

    }

    void collapseBuilding(IBuilding bldg, Map<BoardLocation, List<Entity>> positionMap, Coords coords,
          Vector<Report> vPhaseReport) {
        collapseBuilding(bldg, positionMap, coords, true, false, vPhaseReport);
    }

    void collapseBuilding(IBuilding bldg, Map<BoardLocation, List<Entity>> positionMap, Coords coords,
          boolean collapseAll, Vector<Report> vPhaseReport) {
        collapseBuilding(bldg, positionMap, coords, collapseAll, false, vPhaseReport);
    }

    /**
     * Collapse a building basement. Inflict the appropriate amount of damage on all entities that fell to the basement.
     * Update all clients.
     *
     * @param bldg        the Building that has collapsed.
     * @param positionMap a Hashtable that maps the Coords positions or each unit in the game to a Vector of Entity's at
     *                    that position. This value should not be null.
     * @param coords      The Coords of the building basement hex that has collapsed
     */
    public void collapseBasement(IBuilding bldg, Map<BoardLocation, List<Entity>> positionMap, Coords coords,
          Vector<Report> vPhaseReport) {
        if (!bldg.hasCFIn(coords)) {
            return;
        }
        int runningCFTotal = bldg.getCurrentCF(coords);

        // Get the Vector of Entities at these coordinates.
        final List<Entity> entities = positionMap.get(BoardLocation.of(coords, bldg.getBoardId()));

        if (bldg.getBasement(coords).isNone()) {
            return;
        } else {
            bldg.collapseBasement(coords, getGame().getBoard(bldg.getBoardId()), vPhaseReport);
        }

        // Are there any Entities at these coords?
        if (entities != null) {

            // Sort in elevation order
            entities.sort((a, b) -> {
                if (a.getElevation() > b.getElevation()) {
                    return -1;
                } else if (a.getElevation() > b.getElevation()) {
                    return 1;
                }
                return 0;
            });
            // Walk through the entities in this position.
            for (Entity entity : entities) {

                // int floor = entity.getElevation();

                int cfDamage = (int) Math.ceil(Math.round(entity.getWeight() / 10.0));

                // all entities should fall
                // ASSUMPTION: PSR to avoid pilot damage
                PilotingRollData psr = entity.getBasePilotingRoll();
                entity.addPilotingModifierForTerrain(psr, coords, bldg.getBoardId());

                // fall into basement
                switch (bldg.getBasement(coords)) {
                    case NONE:
                    case ONE_DEEP_NORMAL_INFANTRY_ONLY:
                        LOGGER.error("{} is not falling into {}", entity.getDisplayName(), coords.toString());
                        break;
                    case TWO_DEEP_HEAD:
                    case TWO_DEEP_FEET:
                        LOGGER.info("{} is falling 2 floors into {}", entity.getDisplayName(), coords.toString());
                        // Damage is determined by the depth of the basement, so a fall of 0
                        // elevation is correct in this case
                        vPhaseReport.addAll(gameManager.doEntityFall(entity, coords, 0, Compute.d6(), psr, true,
                              false));
                        runningCFTotal -= cfDamage * 2;
                        break;
                    default:
                        LOGGER.info("{} is falling 1 floor into {}", entity.getDisplayName(), coords.toString());
                        // Damage is determined by the depth of the basement, so a fall of 0
                        // elevation is correct in this case
                        vPhaseReport.addAll(gameManager.doEntityFall(entity,
                              coords,
                              0,
                              Compute.d6(),
                              psr,
                              true,
                              false));
                        runningCFTotal -= cfDamage;
                        break;
                }

                // Update this entity.
                // ASSUMPTION: this is the correct thing to do.
                gameManager.entityUpdate(entity.getId());
            } // Handle the next entity.
        }

        // Update the building
        if (runningCFTotal < 0) {
            bldg.setCurrentCF(0, coords);
            bldg.setPhaseCF(0, coords);
        } else {
            bldg.setCurrentCF(runningCFTotal, coords);
            bldg.setPhaseCF(runningCFTotal, coords);
        }
        gameManager.sendChangedHex(coords, bldg.getBoardId());
        Vector<IBuilding> buildings = new Vector<>();
        buildings.add(bldg);
        gameManager.sendChangedBuildings(buildings);
    }

    /**
     * Collapse a building hex. Inflict the appropriate amount of damage on all entities in the building. Update all
     * clients.
     *
     * @param bldg        the Building that has collapsed.
     * @param positionMap a Hashtable that maps the Coords positions or each unit in the game to a Vector of Entity's at
     *                    that position. This value should not be null.
     * @param coords      The Coords of the building hex that has collapsed
     * @param collapseAll A boolean indicating whether this collapse of a hex should be able to collapse the whole
     *                    building
     * @param topFloor    A boolean indicating that only the top floor collapses (from a WiGE flying over the top).
     */
    void collapseBuilding(IBuilding bldg, Map<BoardLocation, List<Entity>> positionMap, Coords coords,
          boolean collapseAll, boolean topFloor, Vector<Report> vPhaseReport) {
        // sometimes, buildings that reach CF 0 decide against collapsing,
        // but we want them to go away anyway, as a building with CF 0 cannot stand
        final int phaseCF = bldg.hasCFIn(coords) ? bldg.getPhaseCF(coords) : 0;

        // Loop through the hexes in the building, and apply
        // damage to all entities inside or on top of the building.
        Report r;

        // Get the Vector of Entities at these coordinates.
        final List<Entity> vector = positionMap.get(BoardLocation.of(coords, bldg.getBoardId()));

        // Are there any Entities at these coords?
        if (vector != null) {
            // How many levels does this building have in this hex?
            final Hex curHex = getGame().getBoard(bldg.getBoardId()).getHex(coords);
            final int bridgeEl = curHex.terrainLevel(Terrains.BRIDGE_ELEV);
            final int numFloors = Math.max(bridgeEl, curHex.terrainLevel(Terrains.BLDG_ELEV));

            // Now collapse the building in this hex, so entities fall to
            // the ground
            if (topFloor && numFloors > 1) {
                curHex.removeTerrain(Terrains.BLDG_ELEV);
                curHex.addTerrain(new Terrain(Terrains.BLDG_ELEV, numFloors - 1));
                gameManager.sendChangedHex(coords, bldg.getBoardId());
            } else {
                bldg.setCurrentCF(0, coords);
                bldg.setPhaseCF(0, coords);
                gameManager.send(createCollapseBuildingPacket(coords, bldg.getBoardId()));
                getGame().getBoard(bldg.getBoardId()).collapseBuilding(coords);
            }

            // Sort in elevation order
            vector.sort((a, b) -> {
                if (a.getElevation() > b.getElevation()) {
                    return -1;
                } else if (a.getElevation() > b.getElevation()) {
                    return 1;
                }
                return 0;
            });
            // Walk through the entities in this position.
            for (Entity entity : vector) {
                // all gun emplacements are simply destroyed
                if (entity instanceof GunEmplacement) {
                    vPhaseReport.addAll(gameManager.destroyEntity(entity, "building collapse"));
                    addNewLines();
                    continue;
                }

                int floor = entity.getElevation();
                // If only the top floor collapses, we only care about units on the top level
                // or on the roof.
                if (topFloor && floor < numFloors - 1) {
                    continue;
                }
                // units trapped in a basement under a collapsing building are
                // destroyed
                if (floor < 0) {
                    vPhaseReport.addAll(gameManager.destroyEntity(entity,
                          "Crushed under building rubble",
                          false,
                          false));
                }

                // Ignore units above the building / bridge.
                if (floor > numFloors) {
                    continue;
                }

                // Treat units on the roof like
                // they were in the top floor.
                if (floor == numFloors) {
                    floor--;
                }

                // Calculate collapse damage for this entity.
                int damage = (int) Math.floor(bldg.getDamageFromScale() *
                      Math.ceil((phaseCF * (numFloors - floor)) / 10.0));

                // Infantry suffer more damage.
                if (entity instanceof Infantry) {
                    if ((entity instanceof BattleArmor) || ((Infantry) entity).isMechanized()) {
                        damage *= 2;
                    } else {
                        damage *= 3;
                    }
                }

                // Apply collapse damage the entity.
                r = new Report(6455);
                r.indent();
                r.subject = entity.getId();
                r.add(entity.getDisplayName());
                r.add(damage);
                vPhaseReport.add(r);
                int remaining = damage;
                int cluster = damage;
                if ((entity instanceof BattleArmor) || (entity instanceof Mek) || (entity instanceof Tank)) {
                    cluster = 5;
                }
                while (remaining > 0) {
                    int next = Math.min(cluster, remaining);
                    int table;
                    if (entity instanceof ProtoMek) {
                        table = ToHitData.HIT_SPECIAL_PROTO;
                    } else if (entity.getElevation() == numFloors) {
                        table = ToHitData.HIT_NORMAL;
                    } else {
                        table = ToHitData.HIT_PUNCH;
                    }
                    HitData hit = entity.rollHitLocation(table, ToHitData.SIDE_FRONT);
                    hit.setGeneralDamageType(HitData.DAMAGE_PHYSICAL);
                    vPhaseReport.addAll(gameManager.damageEntity(entity, hit, next));
                    remaining -= next;
                }
                vPhaseReport.add(new Report(1210, Report.PUBLIC));

                // all entities should fall
                floor = entity.getElevation();
                if ((floor > 0) || (floor == bridgeEl)) {
                    // ASSUMPTION: PSR to avoid pilot damage
                    // should use mods for entity damage and
                    // 20+ points of collapse damage (if any).
                    PilotingRollData psr = entity.getBasePilotingRoll();
                    entity.addPilotingModifierForTerrain(psr, coords, bldg.getBoardId());
                    if (damage >= 20) {
                        psr.addModifier(1, "20+ damage");
                    }
                    vPhaseReport.addAll(gameManager.doEntityFallsInto(entity, coords, psr, true));
                }
                // Update this entity.
                // ASSUMPTION: this is the correct thing to do.
                gameManager.entityUpdate(entity.getId());
            }
        } else {
            // Update the building.
            bldg.setCurrentCF(0, coords);
            bldg.setPhaseCF(0, coords);
            gameManager.send(createCollapseBuildingPacket(coords, bldg.getBoardId()));
            getGame().getBoard(bldg.getBoardId()).collapseBuilding(coords);
        }
        // if more than half of the hexes are gone, collapse all
        if (bldg.getCollapsedHexCount() > (bldg.getOriginalHexCount() / 2)) {
            for (Enumeration<Coords> coordsEnum = bldg.getCoords(); coordsEnum.hasMoreElements(); ) {
                coords = coordsEnum.nextElement();
                collapseBuilding(bldg, getGame().getPositionMapMulti(), coords, false, vPhaseReport);
            }
        }
    }

    /**
     * Tell the clients to replace the given building with rubble hexes.
     *
     * @param coords - the Coords that has collapsed.
     *
     * @return a Packet for the command.
     */
    Packet createCollapseBuildingPacket(Coords coords, int boardId) {
        return new Packet(PacketCommand.BLDG_COLLAPSE, new Vector<>(List.of(coords)), boardId);
    }
}
