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

package megamek.server.totalWarfare;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import megamek.common.Report;
import megamek.common.board.Coords;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;
import megamek.common.units.IBuilding;
import megamek.common.units.MobileStructure;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;
import megamek.common.weapons.handlers.AreaEffectHelper;

/** Connected-hex damage and debris, independently enabled by TO:AR pp.121-123's Expanded Building Collapse option. */
final class ExpandedBuildingCollapseHandler extends AbstractTWRuleHandler {
    record Snapshot(IBuilding building, Coords origin, int height, int cf, Set<Coords> footprint, Set<Coords> blockers) { }
    private record Collapse(Snapshot snapshot, Set<Integer> levels, boolean wholeHex) { }

    private final BuildingCollapseHandler collapseHandler;
    private final ArrayDeque<Collapse> pending = new ArrayDeque<>();
    private boolean processing;

    ExpandedBuildingCollapseHandler(TWGameManager manager, BuildingCollapseHandler collapseHandler) {
        super(manager);
        this.collapseHandler = collapseHandler;
    }

    Snapshot capture(IBuilding building, Coords origin) {
        if (!getGame().getOptions().booleanOption(OptionsConstants.ADVANCED_BUILDING_EXPANDED_COLLAPSE)) {
            return null;
        }
        int height = building.getHeight(origin);
        int cf = building.getInternalBuilding().getStartTurnCF(building.boardToRelative(origin));
        Set<Coords> footprint = new HashSet<>();
        building.getInternalBuilding().getOriginalCoordsList().forEach(hex -> footprint.add(building.relativeToBoard(hex)));
        Set<Coords> blockers = new HashSet<>();
        var board = getGame().getBoard(building.getBoardId());
        int base = board.getHex(origin).getLevel();
        for (int distance = 1; distance <= height / 12; distance++) {
            for (Coords hex : origin.allAtDistance(distance)) {
                var terrain = board.getHex(hex);
                if (terrain != null && terrain.ceiling() - base >= height / 2) {
                    blockers.add(hex);
                }
            }
        }
        return new Snapshot(building, origin, height, cf, footprint, blockers);
    }

    void collapsed(Snapshot snapshot, Set<Integer> levels, boolean wholeHex, Vector<Report> reports) {
        if (snapshot == null) {
            return;
        }
        pending.add(new Collapse(snapshot, Set.copyOf(levels), wholeHex));
        if (processing) {
            return;
        }
        processing = true;
        try {
            while (!pending.isEmpty()) {
                Collapse collapse = pending.removeFirst();
                damageConnectedFloors(collapse);
                if (collapse.wholeHex()) {
                    scatterDebris(collapse.snapshot(), reports);
                }
                // Finish this collapse's debris before starting any collapse it caused.
                resolveNextCollapse(reports);
            }
        } finally {
            processing = false;
        }
    }

    private void damageConnectedFloors(Collapse collapse) {
        IBuilding building = collapse.snapshot().building();
        if (building instanceof MobileStructure) {
            return; // TO:AUE always applies its own CF-and-armor loss, independently of this option.
        }
        for (Coords neighbor : collapse.snapshot().origin().allAdjacent()) {
            if (!building.isIn(neighbor)) {
                continue;
            }
            int height = building.usesExpandedCF() ? building.getHeight(neighbor) : 1;
            for (int level = 0; level < height; level++) {
                if (!collapse.wholeHex() && building.usesExpandedCF() && !collapse.levels().contains(level)) {
                    continue;
                }
                var floors = building.getFloorState(neighbor);
                int identity = floors == null ? 0 : floors.floorAtLevel(level);
                if (!getGame().getBuildingDamageTracker().claimCollapseDamage(building, neighbor, identity,
                      getGame().getRoundCount(), getGame().getPhase())) {
                    continue;
                }
                int cf = building.getCurrentCF(neighbor, level);
                int damage = (cf - cf / 2) * (building.usesCapitalScale() ? 10 : 1);
                building.setCurrentCF(cf - building.scaleDamageToCF(damage), neighbor, level);
            }
        }
        gameManager.sendChangedBuildings(new Vector<>(List.of(building)));
    }

    private void scatterDebris(Snapshot snapshot, Vector<Report> reports) {
        IBuilding source = snapshot.building();
        var board = getGame().getBoard(source.getBoardId());
        int cf = snapshot.cf() * (source.usesCapitalScale() ? 10 : 1);
        for (int distance = 1; distance <= snapshot.height() / 12; distance++) {
            int damage = (int) Math.ceil(cf * (double) snapshot.height() / (10 * (distance + 1)));
            for (Coords coords : snapshot.origin().allAtDistance(distance)) {
                var hex = board.getHex(coords);
                if (hex == null || snapshot.footprint().contains(coords) || Coords.intervening(snapshot.origin(), coords, true)
                      .stream().filter(c -> !c.equals(snapshot.origin()) && !c.equals(coords)).anyMatch(snapshot.blockers()::contains)) {
                    continue;
                }
                IBuilding target = getGame().getBuildingAt(coords, source.getBoardId()).orElse(null);
                if (target != null) {
                    int levels = target.usesExpandedCF() ? target.getHeight(coords) : 1;
                    for (int level = 0; level < levels; level++) {
                        reports.addAll(gameManager.damageBuilding(target, damage, "collapsing building debris", coords,
                              level, null, false));
                    }
                }
                for (Entity entity : List.copyOf(getGame().getEntitiesVector(coords, source.getBoardId()))) {
                    if (!(entity instanceof IBuilding) && !entity.isAirborne()) {
                        int toUnit = (int) Math.floor(damage * source.getDamageFromScale()
                              / (source.usesCapitalScale() ? 10 : 1));
                        AreaEffectHelper.artilleryDamageEntity(entity, toUnit, null, 0, false, false, false,
                              entity.getElevation() + hex.getLevel(), snapshot.origin(), null, coords, false,
                              null, hex, Entity.NONE, reports, gameManager);
                        gameManager.entityUpdate(entity.getId());
                    }
                }
                reports.addAll(gameManager.tryClearHex(coords, source.getBoardId(), damage, Entity.NONE));
                if (!hex.containsTerrain(Terrains.RUBBLE)) {
                    hex.addTerrain(new Terrain(Terrains.RUBBLE, source.getBuildingType().getTypeValue()));
                }
                gameManager.sendChangedHex(coords, source.getBoardId());
            }
        }
    }

    private void resolveNextCollapse(Vector<Report> reports) {
        record Candidate(IBuilding building, Coords coords, int targets) { }
        var candidates = new java.util.ArrayList<Candidate>();
        for (var board : getGame().getBoards().values()) {
            for (IBuilding building : List.copyOf(board.getBuildingsVector())) {
                for (Coords coords : List.copyOf(building.getCoordsList())) {
                    if (building.getCurrentCF(coords) == 0 || (building.getFloorState(coords) != null
                          && building.getFloorState(coords).hasChanges())) {
                        int radius = building.getHeight(coords) / 12;
                        int targets = 0;
                        for (int distance = 1; distance <= radius; distance++) {
                            for (Coords affected : coords.allAtDistance(distance)) {
                                if (!building.isIn(affected)) {
                                    targets += getGame().getEntitiesVector(affected, building.getBoardId()).size();
                                    targets += getGame().getBuildingAt(affected, building.getBoardId()).isPresent() ? 1 : 0;
                                }
                            }
                        }
                        candidates.add(new Candidate(building, coords, targets));
                    }
                }
            }
        }
        candidates.sort(java.util.Comparator.comparingInt(Candidate::targets).reversed());
        for (Candidate candidate : candidates) {
            IBuilding building = candidate.building();
            if (building.getFloorState(candidate.coords()) != null) {
                if (collapseHandler.resolveExpandedCollapse(building, candidate.coords(), reports)) {
                    return;
                }
            } else if (building.getCurrentCF(candidate.coords()) == 0) {
                collapseHandler.collapseBuilding(building, getGame().getPositionMapMulti(), candidate.coords(), reports);
                return;
            }
        }
    }
}
