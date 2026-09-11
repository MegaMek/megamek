/*
 * Copyright (C) 2025-2026 The MegaMek Team. All Rights Reserved.
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import megamek.common.Hex;
import megamek.common.HitData;
import megamek.common.IndustrialElevator;
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
import megamek.common.units.*;
import megamek.logging.MMLogger;

public class BuildingCollapseHandler extends AbstractTWRuleHandler {

    private static final MMLogger LOGGER = MMLogger.create(BuildingCollapseHandler.class);
    private final ExpandedBuildingCollapseHandler expandedCollapse;
    private final Set<IBuilding> subsurfaceCascades = new HashSet<>();
    private final Set<IBuilding> resolvingBridgeSpans = new HashSet<>();

    BuildingCollapseHandler(TWGameManager gameManager) {
        super(gameManager);
        expandedCollapse = new ExpandedBuildingCollapseHandler(gameManager, this);
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

        // If the input is meaningless, do nothing and throw no exception. An empty position map is legal: nothing
        // is standing on the board, but the building can still come down (a building burning down on an empty map).
        if ((bldg == null) || (positionMap == null) || (coords == null)
              || !bldg.isIn(coords)) {
            LOGGER.error("Illegal/null arguments");
            return false;
        }

        var foundation = megamek.common.units.BuildingFoundationRules.below(getGame(), bldg, coords);
        boolean strongFoundation = megamek.common.units.BuildingFoundationRules.stronger(foundation, bldg, coords);
        if (strongFoundation && positionMap.getOrDefault(BoardLocation.of(coords, bldg.getBoardId()), List.of()).stream()
              .anyMatch(unit -> !(unit instanceof IBuilding) && !unit.isAirborne() && !unit.isAirborneVTOLorWIGE()
                    && unit.getElevation() == 0 && unit.getWeight() > foundation.getLoadCapacity(coords))) {
            checkForCollapse(foundation, positionMap, coords, false, vPhaseReport);
        }
        if (bldg.getFloorState(coords) != null) {
            return resolveExpandedCollapse(bldg, coords, vPhaseReport);
        }
        int currentCF = bldg.getCurrentCF(coords);
        int loadCapacity = bldg.usesCapitalScale() ? bldg.getLoadCapacity(coords) : currentCF;

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
            final int numFloors = bldg instanceof AbstractBuildingEntity ? bldg.getHeight(coords)
                  : Math.max(0, curHex.terrainLevel(Terrains.BLDG_ELEV));
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
                    if (entity instanceof IBuilding || entity.isAirborne()
                          || entity.isAirborneVTOLorWIGE() && entity.getMovementMode() != EntityMovementMode.WIGE) {
                        continue;
                    }
                    // WiGEs can collapse the top floor of a building by flying over it.
                    final int entityElev = BuildingElevation.floor(bldg, entity.getPosition(), entity.getElevation());
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
                        if (wigeLoad > loadCapacity * 4) {
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
                        if (loads[floor] > loadCapacity) {
                            // If the load on any floor but the first floor
                            // exceeds the building's current CF it collapses.
                            if (floor != 0) {
                                collapse = true;
                            } else if (!strongFoundation && !bldg.getBasementCollapsed(coords)) {
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
            entities.sort(Comparator.comparingInt(Entity::getElevation));
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
                        runningCFTotal -= bldg.usesCapitalScale() ? bldg.scaleDamageToCF(cfDamage * 2) : cfDamage * 2;
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
                        runningCFTotal -= bldg.usesCapitalScale() ? bldg.scaleDamageToCF(cfDamage) : cfDamage;
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
        if (!bldg.isIn(coords)) {
            return;
        }
        if (bldg instanceof AbstractBuildingEntity authored && authored.getDesign().isOpenSpace()
              && authored.getDesign().getSite() == BuildingDesign.Site.UNDERGROUND) {
            new OpenSpaceCollapseHandler(gameManager).collapse(authored, coords, vPhaseReport);
            return;
        }
        int previousCollapsedHexes = bldg.getCollapsedHexCount();
        Set<Integer> releasedVessels = bldg instanceof MobileStructure mobile
              ? new MobileStructureNavalHandler(gameManager).releaseVesselsInHex(mobile, coords) : Set.of();
        var collapseSnapshot = expandedCollapse.capture(bldg, coords);
        int physicalBase = BuildingElevation.base(bldg, coords);
        int authoredHeight = bldg.getHeight(coords);
        if (bldg instanceof AbstractBuildingEntity authored) {
            new BuildingEnvironmentHandler(gameManager).collapsed(authored, coords, authoredHeight, vPhaseReport);
        }
        // sometimes, buildings that reach CF 0 decide against collapsing,
        // but we want them to go away anyway, as a building with CF 0 cannot stand
        final int phaseCF = bldg.getPhaseCF(coords);

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
            final int numFloors = bldg instanceof AbstractBuildingEntity ? authoredHeight
                  : Math.max(bridgeEl, curHex.terrainLevel(Terrains.BLDG_ELEV));

            // Now collapse the building in this hex, so entities fall to
            // the ground
            if (topFloor && numFloors > 1) {
                curHex.removeTerrain(Terrains.BLDG_ELEV);
                if (physicalBase + numFloors - 1 > 0) {
                    curHex.addTerrain(new Terrain(Terrains.BLDG_ELEV, physicalBase + numFloors - 1));
                }
                gameManager.sendChangedHex(coords, bldg.getBoardId());
            } else {
                bldg.setCurrentCF(0, coords);
                bldg.setPhaseCF(0, coords);
                gameManager.send(createCollapseBuildingPacket(coords, bldg));
                getGame().getBoard(bldg.getBoardId()).collapseBuilding(bldg, coords);
                disableIndustrialElevatorAt(coords, bldg);
            }

            // Sort in elevation order
            vector.sort(Comparator.comparingInt(Entity::getElevation));
            // Walk through the entities in this position.
            for (Entity entity : vector) {
                if (releasedVessels.contains(entity.getId())) {
                    continue;
                }
                // all gun emplacements are simply destroyed
                if (entity instanceof GunEmplacement) {
                    vPhaseReport.addAll(gameManager.destroyEntity(entity, "building collapse"));
                    addNewLines();
                    continue;
                }

                if (bldg.equals(entity) && entity instanceof AbstractBuildingEntity buildingEntity) {
                    int numFloorsToCollappse = topFloor ? 1 : numFloors;
                    buildingEntity.collapseFloorsOnHex(coords, numFloorsToCollappse);
                    gameManager.entityUpdate(entity.getId());
                    continue;
                }
                if (entity.isAirborne() || entity.isAirborneVTOLorWIGE()) {
                    continue;
                }

                int floor = entity.getElevation() - physicalBase;
                boolean subsurface = bldg instanceof AbstractBuildingEntity authored
                      && authored.getDesign().getSite() != BuildingDesign.Site.SURFACE;
                // A different structure at this XY coordinate is not an occupant of the collapsing volume.
                if (entity instanceof IBuilding || floor < 0 && (physicalBase != 0
                      || getGame().getBoard(bldg.getBoardId()).getBuildingsAt(coords).stream()
                            .anyMatch(other -> other != bldg && BuildingElevation.contains(other, coords, entity.getElevation())))) {
                    continue;
                }
                // If only the top floor collapses, we only care about units on the top level
                // or on the roof.
                if (topFloor && floor < numFloors - 1) {
                    continue;
                }
                // units trapped in a basement under a collapsing building are
                // destroyed
                if (floor < 0 && physicalBase == 0) {
                    vPhaseReport.addAll(gameManager.destroyEntity(entity,
                          "Crushed under building rubble",
                          false,
                          false));
                }
                if (subsurface && entity instanceof Infantry
                      && !EnvironmentalSealingRules.canOperateFullySubmerged(entity)) {
                    vPhaseReport.addAll(gameManager.destroyEntity(entity, "trapped in a collapsed subsurface structure"));
                    continue;
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
                int overburden = subsurface ? Math.max(0, -(physicalBase + numFloors)) : 0;
                int damage = (int) Math.floor(bldg.getDamageFromScale() *
                      Math.ceil((phaseCF * (numFloors + overburden - floor)) / 10.0));

                // Infantry suffer more damage.
                if (entity instanceof Infantry) {
                    if ((entity instanceof BattleArmor) || ((Infantry) entity).isMechanized()) {
                        damage *= 2;
                    } else {
                        damage *= 3;
                    }
                }

                if (subsurfaceCascades.contains(bldg)) {
                    damage /= 2;
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
                    } else if (entity.getElevation() == physicalBase + numFloors) {
                        table = ToHitData.HIT_NORMAL;
                    } else {
                        table = ToHitData.HIT_PUNCH;
                    }
                    HitData hit = entity.rollHitLocation(table, ToHitData.SIDE_FRONT);
                    hit.setGeneralDamageType(HitData.DAMAGE_PHYSICAL_NONATTACK);
                    vPhaseReport.addAll(gameManager.damageEntity(entity, hit, next));
                    remaining -= next;
                }
                vPhaseReport.add(new Report(1210, Report.PUBLIC));

                // all entities should fall
                floor = entity.getElevation() - physicalBase;
                if ((floor > 0) || (floor == bridgeEl)) {
                    // ASSUMPTION: PSR to avoid pilot damage
                    // should use mods for entity damage and
                    // 20+ points of collapse damage (if any).
                    PilotingRollData psr = entity.getBasePilotingRoll();
                    entity.addPilotingModifierForTerrain(psr, coords, bldg.getBoardId());
                    if (damage >= 20) {
                        psr.addModifier(1, "20+ damage");
                    }
                    if (physicalBase == 0) {
                        vPhaseReport.addAll(gameManager.doEntityFallsInto(entity, coords, psr, true));
                    } else {
                        // doEntityFallsInto normally measures from the terrain bed/roof. Adjust that datum to this
                        // volume's surviving floor, so a negative elevation does not increase the fall distance.
                        int landing = topFloor ? physicalBase + numFloors - 1 : physicalBase;
                        int reduction = fallReduction(curHex, entity.getElevation(), landing);
                        vPhaseReport.addAll(gameManager.doEntityFallsInto(entity, entity.getElevation(), coords, coords,
                              psr, true, reduction));
                        entity.setElevation(landing);
                    }
                }
                // Update this entity.
                // ASSUMPTION: this is the correct thing to do.
                gameManager.entityUpdate(entity.getId());
            }
        } else {
            // Update the building.
            bldg.setCurrentCF(0, coords);
            bldg.setPhaseCF(0, coords);
            gameManager.send(createCollapseBuildingPacket(coords, bldg));
            getGame().getBoard(bldg.getBoardId()).collapseBuilding(bldg, coords);
            disableIndustrialElevatorAt(coords, bldg);
        }
        if (bldg instanceof MobileStructure mobile && bldg.getCollapsedHexCount() > previousCollapsedHexes) {
            // TO:AUE p.40: loss of a hex damages its neighbors and can injure the structure's main crew.
            for (Coords adjacent : coords.allAdjacent()) {
                if (!mobile.isIn(adjacent)) {
                    continue;
                }
                int levels = mobile.usesExpandedCF() ? mobile.getHeight(adjacent) : 1;
                for (int level = 0; level < levels; level++) {
                    int cf = mobile.getCurrentCF(adjacent, level);
                    int armor = mobile.getArmor(adjacent, level);
                    mobile.setCurrentCF(cf - mobile.scaleDamageToCF(cf - cf / 2), adjacent, level);
                    mobile.setArmor(armor - mobile.scaleDamageToCF(armor - armor / 2), adjacent, level);
                }
            }
            if (Compute.d6(2) + mobile.getCollapsedHexCount() >= 10) {
                mobile.addMobileCrewHit();
            }
            gameManager.entityUpdate(mobile.getId());
        }
        expandedCollapse.collapsed(collapseSnapshot,
              topFloor && collapseSnapshot != null ? Set.of(collapseSnapshot.height() - 1) : Set.of(),
              !topFloor || !bldg.isIn(coords), vPhaseReport);
        if (bldg.getBldgClass() == IBuilding.BRIDGE) {
            collapseDisconnectedBridgeSpans(bldg, vPhaseReport);
        } else if (collapseAll && !(bldg instanceof MobileStructure)
              && bldg.getCollapsedHexCount() * 2 > bldg.getOriginalHexCount()) {
            collapseRemainingBuilding(bldg, vPhaseReport);
        } else if (bldg instanceof MobileStructure mobile && mobile.isSplit()) {
            vPhaseReport.addAll(gameManager.destroyEntity(mobile, "structure split by destroyed hexes"));
        }
    }

    /**
     * Disables any industrial elevator in a collapsed hex. A collapsed shaft level stops the elevator from functioning
     * (TO:AR), so its platform can no longer be ridden or called. Broadcasts the change so clients update their
     * elevator controls.
     * <p>
     * Environmental breaches and flooding disable the same registry entries in {@link BuildingEnvironmentHandler}.
     *
     * @param coords  the hex that collapsed
     * @param boardId the board the hex is on
     */
    private void disableIndustrialElevatorAt(Coords coords, IBuilding building) {
        boolean changed = false;
        for (IndustrialElevator elevator : getGame().getIndustrialElevators()) {
            if (elevator.getLocation().equals(BoardLocation.of(coords, building.getBoardId())) && elevator.isFunctional()
                  && elevator.getBuildingId() == (building instanceof Entity entity ? entity.getId() : Entity.NONE)) {
                elevator.setFunctional(false);
                changed = true;
            }
        }
        if (changed) {
            gameManager.sendIndustrialElevatorUpdate();
        }
    }

    /** Resolves per-floor CF, overloaded floors, and the fall of surviving upper floors (TO:AR pp.119–121). */
    boolean resolveExpandedCollapse(IBuilding building, Coords coords, Vector<Report> reports) {
        BuildingFloorState floors = building.getFloorState(coords);
        if (floors == null || floors.height() == 0) {
            return false;
        }
        List<Entity> occupants = new ArrayList<>(getGame().getEntitiesVector(coords, building.getBoardId(), true));
        occupants.removeIf(entity -> entity instanceof IBuilding || entity.isAirborne() || entity.isAirborneVTOLorWIGE());
        int oldHeight = floors.height();
        int[] oldLevels = floors.levels();
        var collapseSnapshot = expandedCollapse.capture(building, coords);
        // Ground-floor occupants never overload a building. Check each occupied level separately (TW p.176).
        for (int level = 1; level <= oldHeight; level++) {
            final int occupiedLevel = level;
            double load = occupants.stream().filter(entity -> BuildingElevation.floor(building, coords, entity.getElevation()) == occupiedLevel)
                  .mapToDouble(Entity::getWeight).sum();
            for (int floor = 0; floor < floors.size(); floor++) {
                if (oldLevels[floor] >= 0 && oldLevels[floor] < level
                      && load > floors.getCF(floor) * (building.usesCapitalScale() ? 10 : 1)) {
                    floors.setCF(floor, 0);
                }
            }
        }
        int fallingDebrisCF = java.util.stream.IntStream.range(0, floors.size())
              .filter(floor -> oldLevels[floor] > 0).map(floors::getPhaseCF).sum();
        int fallingCF = floors.resolveCollapse(subsurfaceCascades.contains(building) ? 2 : 1);
        if (Arrays.equals(oldLevels, floors.levels())) {
            return false;
        }
        if (floors.height() == 0) {
            collapseThroughFoundation(building, coords, fallingCF, fallingDebrisCF, reports);
            collapseBuilding(building, getGame().getPositionMapMulti(), coords, reports);

            collapseShortenedBuilding(building, reports);
            return true;
        }
        if (building instanceof AbstractBuildingEntity entity) {
            for (int location : entity.getLocationsAt(coords)) {
                int floor = location % floors.size();
                if (oldLevels[floor] >= 0 && floors.getLevel(floor) < 0) {
                    entity.destroyLocation(location, true);
                }
            }
        }
        building.getInternalBuilding().synchronizeFloorState(building.boardToRelative(coords));
        Hex hex = getGame().getBoard(building.getBoardId()).getHex(coords);
        if (BuildingElevation.base(building, coords) + floors.height() > 0) {
            hex.addTerrain(new Terrain(Terrains.BLDG_ELEV, BuildingElevation.base(building, coords) + floors.height()));
        }
        gameManager.sendChangedHex(coords, building.getBoardId());
        disableIndustrialElevatorAt(coords, building);
        // Lower levels settle first (TW p.177); retain original levels to measure each fall.
        occupants.sort(Comparator.comparingInt(Entity::getElevation));
        for (Entity occupant : occupants) {
            int oldLevel = BuildingElevation.floor(building, coords, occupant.getElevation());
            if (oldLevel < 0 || oldLevel > oldHeight || occupant.isAirborne()) {
                continue;
            }
            int landing = oldLevel == oldHeight ? floors.height() : 0;
            int debrisCF = 0;
            for (int floor = 0; floor < floors.size(); floor++) {
                if (oldLevels[floor] <= oldLevel && floors.getLevel(floor) >= 0) {
                    landing = Math.max(landing, floors.getLevel(floor));
                }
                if (oldLevels[floor] > oldLevel) {
                    debrisCF += floors.getPhaseCF(floor);
                }
            }
            int damage = (int) Math.floor(building.getDamageFromScale() * Math.ceil(debrisCF / 10.0));
            if (occupant instanceof Infantry infantry) {
                damage *= infantry instanceof BattleArmor || infantry.isMechanized() ? 2 : 3;
            }
            if (subsurfaceCascades.contains(building)) {
                damage /= 2;
            }
            while (damage > 0) {
                int cluster = Math.min(5, damage);
                HitData hit = occupant.rollHitLocation(occupant instanceof ProtoMek ? ToHitData.HIT_SPECIAL_PROTO
                      : oldLevel == oldHeight ? ToHitData.HIT_NORMAL : ToHitData.HIT_PUNCH, ToHitData.SIDE_FRONT);
                hit.setGeneralDamageType(HitData.DAMAGE_PHYSICAL_NONATTACK);
                reports.addAll(gameManager.damageEntity(occupant, hit, cluster));
                damage -= cluster;
            }
            if (landing >= oldLevel) {
                continue;
            }
            reports.addAll(gameManager.doEntityFallsInto(occupant, oldLevel + BuildingElevation.base(building, coords), coords, coords,
                  occupant.getBasePilotingRoll(), true,
                  fallReduction(hex, occupant.getElevation(), landing + BuildingElevation.base(building, coords))));
            occupant.setElevation(landing + BuildingElevation.base(building, coords));
            int floorHit = floors.floorAtLevel(landing);
            int impact = Math.max(1, (int) Math.ceil(occupant.getWeight() / 10.0)) * (oldLevel - landing);
            floors.setCF(floorHit, floors.getCF(floorHit) - building.scaleDamageToCF(impact));
            gameManager.entityUpdate(occupant.getId());
        }
        // Falls can overload or destroy the floor on which they land.
        resolveExpandedCollapse(building, coords, reports);
        Set<Integer> collapsedLevels = new HashSet<>();
        for (int floor = 0; floor < oldLevels.length; floor++) {
            if (oldLevels[floor] >= 0 && floors.getLevel(floor) < 0) {
                collapsedLevels.add(oldLevels[floor]);
            }
        }
        expandedCollapse.collapsed(collapseSnapshot, collapsedLevels, false, reports);
        collapseShortenedBuilding(building, reports);
        return true;
    }

    /** A top-down collapse crosses the shared ground-level ceiling at half damage, before target scaling. */
    private void collapseThroughFoundation(IBuilding surface, Coords coords, int fallingCF, int debrisCF,
          Vector<Report> reports) {
        var foundation = megamek.common.units.BuildingFoundationRules.below(getGame(), surface, coords);
        if (foundation == null || fallingCF <= 0 || foundation.getFloorState(coords) == null) {
            return;
        }
        var lower = foundation.getFloorState(coords);
        int damage = fallingCF * (surface.usesCapitalScale() ? 10 : 1) / 3 / 2;
        // Each destroyed floor passes only the excess onward; the reduction at G is applied once.
        for (int level = lower.height() - 1; level >= 0 && damage > 0; level--) {
            int floor = lower.floorAtLevel(level);
            int cf = lower.getCF(floor);
            int toCF = foundation.scaleDamageToCF(damage);
            lower.setCF(floor, cf - toCF);
            if (toCF < cf) { break; }
            int standardPerCF = foundation.usesCapitalScale() ? 10
                  : (int) Math.round(1.0 / foundation.getDamageToScale());
            damage -= cf * standardPerCF;
        }
        int toUnit = (int) Math.floor(surface.getDamageFromScale() * Math.ceil(debrisCF / 10.0) / 2);
        for (Entity unit : List.copyOf(getGame().getEntitiesVector(coords, surface.getBoardId(), true))) {
            if (unit instanceof IBuilding || !BuildingElevation.contains(foundation, coords, unit.getElevation())
                  || unit.isAirborne() || unit.isAirborneVTOLorWIGE()) {
                continue;
            }
            int remaining = toUnit;
            if (unit instanceof Infantry infantry) {
                remaining *= infantry instanceof BattleArmor || infantry.isMechanized() ? 2 : 3;
            }
            while (remaining > 0) {
                int cluster = Math.min(5, remaining);
                HitData hit = unit.rollHitLocation(unit instanceof ProtoMek ? ToHitData.HIT_SPECIAL_PROTO
                      : ToHitData.HIT_PUNCH, ToHitData.SIDE_FRONT);
                hit.setGeneralDamageType(HitData.DAMAGE_PHYSICAL_NONATTACK);
                reports.addAll(gameManager.damageEntity(unit, hit, cluster));
                remaining -= cluster;
            }
            gameManager.entityUpdate(unit.getId());
        }
        subsurfaceCascades.add(foundation);
        try {
            resolveExpandedCollapse(foundation, coords, reports);
        } finally {
            subsurfaceCascades.remove(foundation);
        }
        gameManager.sendChangedBuildings(new Vector<>(List.of(foundation)));
    }
    /** Translate the standard terrain-relative fall routine to the selected volume's physical landing floor. */
    static int fallReduction(Hex terrain, int sourceElevation, int landingElevation) {
        int terrainFloor = terrain.containsTerrain(Terrains.ICE) ? 0 : -terrain.depth(false);
        int terrainFall = Math.abs(sourceElevation - terrainFloor)
              - (terrain.containsTerrain(Terrains.BLDG_ELEV) ? terrain.terrainLevel(Terrains.BLDG_ELEV) : 0);
        return terrainFall - Math.max(0, sourceElevation - landingElevation);
    }
    private void collapseShortenedBuilding(IBuilding building, Vector<Report> reports) {
        // Mobile Structures use the split-footprint destruction rule (TO:AUE p.40).
        if (building instanceof MobileStructure) {
            return;
        }
        // TO:AR p.120: a majority of columns at half their original height collapses the entire structure.
        // Use the explicit "more than half" rule; the six-hex example is inconsistent at the exact-half boundary.
        long shortened = building.getInternalBuilding().getOriginalCoordsList().stream()
              .map(building.getInternalBuilding()::getFloorState)
              .filter(state -> state != null && state.height() * 2 <= state.size()).count();
        if (shortened * 2 > building.getOriginalHexCount()) {
            collapseRemainingBuilding(building, reports);
        }
    }

    private void collapseRemainingBuilding(IBuilding building, Vector<Report> reports) {
        for (Coords remaining : List.copyOf(building.getCoordsList())) {
            collapseBuilding(building, getGame().getPositionMapMulti(), remaining, false, reports);
        }
        if (building instanceof AbstractBuildingEntity entity && !entity.isDestroyed()) {
            reports.addAll(gameManager.destroyEntity(entity, "building collapse"));
        }
    }

    /** A bridge remnant survives only while connected to either original bank (TO:AR p.115). */
    private void collapseDisconnectedBridgeSpans(IBuilding bridge, Vector<Report> reports) {
        if (!resolvingBridgeSpans.add(bridge)) {
            return;
        }
        try {
            var span = BuildingConstruction.bridgeSpan(bridge.getInternalBuilding().getOriginalCoordsList());
            if (span == null) {
                return;
            }
            Set<Coords> anchored = new HashSet<>();
            List<Coords> pending = new ArrayList<>();
            for (var endpoint : List.of(span.start(), span.end())) {
                Coords hex = bridge.relativeToBoard(endpoint);
                if (bridge.isIn(hex)) {
                    pending.add(hex);
                    anchored.add(hex);
                }
            }
            for (int index = 0; index < pending.size(); index++) {
                for (Coords adjacent : pending.get(index).allAdjacent()) {
                    if (bridge.isIn(adjacent) && anchored.add(adjacent)) {
                        pending.add(adjacent);
                    }
                }
            }
            for (Coords hex : List.copyOf(bridge.getCoordsList())) {
                if (!anchored.contains(hex)) {
                    collapseBuilding(bridge, getGame().getPositionMapMulti(), hex, false, reports);
                }
            }
        } finally {
            resolvingBridgeSpans.remove(bridge);
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

    Packet createCollapseBuildingPacket(Coords coords, IBuilding building) {
        return building instanceof Entity entity
              ? new Packet(PacketCommand.BLDG_COLLAPSE, new Vector<>(List.of(coords)), building.getBoardId(), entity.getId())
              : createCollapseBuildingPacket(coords, building.getBoardId());
    }
}
