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

import java.util.HashSet;

import megamek.common.Report;
import megamek.common.compute.Compute;
import megamek.common.moves.MobileStructureAirMovement;
import megamek.common.moves.MoveStep;
import megamek.common.units.BuildingBayDoors;
import megamek.common.units.MobileStructure;
import megamek.common.units.MobileStructureBayLaunch;

/** Resolves an air mobile carrier's actual bay launches, including unsafe launch rates and door damage. */
final class MobileStructureBayLaunchHandler extends AbstractTWRuleHandler {
    MobileStructureBayLaunchHandler(TWGameManager manager) { super(manager); }

    boolean launch(MobileStructure carrier, MoveStep step) {
        if (!MobileStructureBayLaunch.valid(carrier, carrier.getPosition(), carrier.getFacing(), carrier.getElevation(),
              step.getLaunched())) {
            return false;
        }
        var bays = carrier.getFighterBays();
        for (var entry : step.getLaunched().entrySet()) {
            var bay = bays.get(entry.getKey());
            int doors = bay.getUsableDoors();
            int previous = bay.getNumberUnloadedThisTurn();
            int total = previous + entry.getValue().size();
            var placements = BuildingBayDoors.usablePlacements(carrier, bay).stream().limit(doors).toList();
            var damagedDoors = new HashSet<Integer>();
            var report = new Report(9380);
            report.subject = carrier.getId();
            report.add(carrier.getDisplayName());
            report.add(entry.getValue().size());
            report.add("bay " + bay.getBayNumber() + " (" + doors + " doors)");
            addReport(report);
            for (int i = 0; i < entry.getValue().size(); i++) {
                int ordinal = previous + i;
                int doorIndex = ordinal % doors;
                int perDoor = total / doors + (doorIndex < total % doors ? 1 : 0);
                var door = placements.isEmpty() ? null : placements.get(doorIndex);
                var position = door == null ? carrier.getCoordsList().stream()
                      .filter(c -> getGame().getBoard(carrier).contains(c)
                            && c.allAdjacent().stream().anyMatch(n -> getGame().getBoard(carrier).contains(n) && !carrier.isIn(n)))
                      .findFirst().orElse(null) : carrier.relativeToBoard(door.position().hex());
                if (position == null) { continue; }
                int facing = door == null ? carrier.getFacing() : Math.floorMod(door.facing() + carrier.getFacing(), 6);
                var fighter = getGame().getEntity(entry.getValue().get(i));
                int altitude = MobileStructureAirMovement.aerospaceAltitude(carrier, position,
                      door == null ? 0 : BuildingBayDoors.physicalLevel(carrier, door));
                gameManager.launchUnit(carrier, fighter, position, facing, carrier.delta_distance / 16, altitude,
                      new int[6], Math.max(0, perDoor - 2));
                if (ordinal / doors >= 2 && !damagedDoors.contains(doorIndex)) {
                    var roll = Compute.rollD6(2);
                    var doorReport = new Report(9378);
                    doorReport.subject = carrier.getId();
                    doorReport.add(roll);
                    doorReport.choose(roll.getIntValue() == 2);
                    doorReport.indent(2);
                    addReport(doorReport);
                    if (roll.getIntValue() == 2) {
                        damagedDoors.add(doorIndex);
                    }
                }
            }
            // All declared launches use the doors available at their start; subsequent actions see the damage.
            damagedDoors.forEach(index -> BuildingBayDoors.damage(carrier, bay, placements.isEmpty() ? null : placements.get(index)));
        }
        gameManager.entityUpdate(carrier.getId());
        return true;
    }
}
