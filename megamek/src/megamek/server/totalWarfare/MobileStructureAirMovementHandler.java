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

import java.util.LinkedHashMap;
import java.util.List;

import megamek.common.Report;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.enums.MoveStepType;
import megamek.common.moves.MobileStructureAirMovement;
import megamek.common.moves.MobileStructureLinkage;
import megamek.common.moves.MovePath;
import megamek.common.rolls.PilotingRollData;
import megamek.common.units.MobileStructure;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;

/** TO:AUE p.33; TW pp.87–88, 204. Vertical maneuvers replace movement and retain the mobile quarter-MP cycle. */
final class MobileStructureAirMovementHandler extends AbstractTWRuleHandler {
    MobileStructureAirMovementHandler(TWGameManager manager) {
        super(manager);
    }

    void process(MobileStructure unit, MoveStepType action, MobileStructureMovementHandler movement) {
        var modules = MobileStructureLinkage.group(unit);
        if (!MobileStructureAirMovement.canAttempt(unit, action) || modules.stream().anyMatch(m -> m.mpUsed > 0)) {
            return;
        }
        int cost = MobileStructureAirMovement.actionCost(action);
        int elevation = MobileStructureAirMovement.actionElevation(unit, action);
        var destinations = new LinkedHashMap<MobileStructure, MobileStructureLinkage.Pose>();
        modules.forEach(module -> destinations.put(module,
              MobileStructureLinkage.pose(unit, module, unit.getPosition(), unit.getFacing(), elevation)));
        boolean complete = true;
        for (var entry : destinations.entrySet()) {
            var pose = entry.getValue();
            complete &= entry.getKey().advanceMovement(pose.position(), pose.facing(), pose.elevation(), cost,
                  entry.getKey().getMaximumMPQuarters());
            entry.getKey().delta_distance = 0;
        }
        if (!complete) {
            Report progress = new Report(9891);
            progress.subject = unit.getId();
            progress.addDesc(unit);
            progress.add(Double.toString(unit.getMovementProgress().quarters() / 4.0));
            progress.add(Double.toString(cost / 4.0));
            addReport(progress);
            return;
        }
        boolean landing = action == MoveStepType.VERTICAL_LAND;
        var step = new MovePath(getGame(), unit).addStep(action).getLastStep();
        for (var entry : destinations.entrySet()) {
            var memberStep = MobileStructureLinkage.step(entry.getKey(), step, entry.getValue());
            if ((landing && !movement.clearBuildings(entry.getKey(), memberStep))
                  || !new MobileStructureCollisionHandler(gameManager).resolve(entry.getKey(), memberStep)) {
                return;
            }
        }
        int margin = roll(unit, MobileStructureAirMovement.controlRoll(unit, action), landing);
        boolean liftsOff = true;
        if (landing && margin > 0) {
            damageAft(unit, modules, 10 * margin, "failed vertical landing");
        } else if (!landing && margin > 0) {
            modules.forEach(m -> m.setAirLandingGearDamaged(true));
            if (margin >= 3) {
                // TW p.88 explicitly calls for an unmodified second roll at MoF 3–4.
                liftsOff = margin <= 4 && roll(unit,
                      new PilotingRollData(unit.getId(), unit.getCrew().getPiloting(), "Base piloting skill"), false) == 0;
                if (!liftsOff) {
                    damageAft(unit, modules, margin <= 4 ? 20 : margin == 5 ? 50 : 100, "failed vertical takeoff");
                }
            }
            addReport(new Report(9125).subject(unit.getId()));
        }
        if (!landing) {
            addReport(new Report(liftsOff ? 9322 : 9323).subject(unit.getId()));
        }
        if (landing || liftsOff) {
            destinations.keySet().removeIf(module -> module.isDestroyed() || module.isDoomed() || module.getCoordsList().isEmpty());
            movement.relocateGroup(destinations);
        }
        if (landing) {
            for (var module : destinations.keySet()) {
                int plane = MobileStructureAirMovement.absoluteBase(module, module.getPosition(), module.getElevation());
                boolean water = false;
                for (Coords coords : module.getCoordsList()) {
                    var hex = getGame().getBoard(module).getHex(coords);
                    if (hex == null || hex.getLevel() != plane) {
                        continue;
                    }
                    if (hex.containsTerrain(Terrains.ROUGH) || hex.containsTerrain(Terrains.RUBBLE)) {
                        module.setAirLandingGearDamaged(true);
                    }
                    water |= hex.depth() > 0 && !hex.containsTerrain(Terrains.ICE);
                    // The structure damages contacted ground; that newly created rough does not damage its gear.
                    for (int terrain : new int[] { Terrains.WOODS, Terrains.JUNGLE, Terrains.FOLIAGE_ELEV,
                                                  Terrains.ROAD, Terrains.PAVEMENT, Terrains.FIELDS }) {
                        hex.removeTerrain(terrain);
                    }
                    if (!hex.containsTerrain(Terrains.RUBBLE)) {
                        hex.addTerrain(new Terrain(Terrains.ROUGH, 1));
                    }
                    gameManager.sendChangedHex(coords, module.getBoardId());
                }
                if (water) {
                    addReport(gameManager.destroyEntity(module, "air mobile structure landed in water"));
                }
            }
        }
    }

    private int roll(MobileStructure unit, PilotingRollData target, boolean landing) {
        if (target.isAutomaticSuccess()) {
            return 0;
        }
        if (target.isImpossible() || target.isAutomaticFail()) {
            return 10;
        }
        Report report = new Report(landing ? 9605 : 9320);
        report.subject = unit.getId();
        report.addDesc(unit);
        report.add(target.getLastPlainDesc(), true);
        addReport(report);
        var dice = Compute.rollD6(2);
        report = new Report(landing ? 9606 : 9321);
        report.subject = unit.getId();
        report.add(target.getValueAsString());
        report.add(target.getDesc());
        report.add(dice);
        if (landing) {
            report.choose(dice.getIntValue() >= target.getValue());
        }
        addReport(report);
        return Math.max(0, target.getValue() - dice.getIntValue());
    }

    private record Impact(MobileStructure module, Coords coords, double aft) { }

    /** Mobile structures have no AFT hit table. Preserve TW's total damage, using aftmost actual bottom-floor hexes. */
    void damageAft(MobileStructure leader, List<MobileStructure> modules, int damage, String why) {
        for (int left = damage; left > 0; left -= 5) {
            var candidates = modules.stream().flatMap(module -> module.getCoordsList().stream()
                  .filter(coords -> module.getCurrentCF(coords) > 0).map(coords -> {
                      var local = MobileStructureLinkage.rotate(coords.toCube().subtract(leader.getPosition().toCube()),
                            -leader.getFacing());
                      return new Impact(module, coords, local.q() + 2 * local.r());
                  })).toList();
            double aft = candidates.stream().mapToDouble(Impact::aft).max().orElse(Double.NaN);
            var targets = candidates.stream().filter(target -> target.aft() == aft).toList();
            if (targets.isEmpty()) {
                break;
            }
            var target = targets.get(Compute.randomInt(targets.size()));
            addReport(gameManager.damageBuilding(target.module(), Math.min(5, left), why, target.coords(), 0, null, false));
        }
        gameManager.applyBuildingDamage();
    }
}
