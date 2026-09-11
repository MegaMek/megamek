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

import java.util.List;
import java.util.Vector;

import megamek.common.HitData;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import megamek.common.units.AbstractBuildingEntity;
import megamek.common.units.BuildingElevation;
import megamek.common.units.Entity;
import megamek.common.units.EnvironmentalSealingRules;
import megamek.common.units.IBuilding;
import megamek.common.units.Infantry;
import megamek.common.units.ProtoMek;

/** TO:AR p.137 underground open-space cave-ins, replacing ordinary room-by-room debris. */
final class OpenSpaceCollapseHandler extends AbstractTWRuleHandler {
    OpenSpaceCollapseHandler(TWGameManager manager) {
        super(manager);
    }

    static int debrisDamage(int originalCF, int cfScale, int height, int groundCover) {
        return (int) Math.ceil(originalCF * cfScale / 10.0 * (height + (groundCover + 4) / 5));
    }

    void collapse(AbstractBuildingEntity building, Coords origin, Vector<Report> reports) {
        int radius = Compute.d6();
        var board = getGame().getBoard(building.getBoardId());
        var footprint = List.copyOf(building.getCoordsList());
        int firstLocation = building.getLocationsAt(origin).stream().findFirst().orElse(Entity.LOC_NONE);
        int originalCF = firstLocation < 0 ? building.getPhaseCF(origin) : building.getOInternal(firstLocation);
        int damage = debrisDamage(originalCF, (int) building.getConstructionCFScale(), building.getHeight(origin),
              BuildingElevation.groundCover(getGame(), building, origin));
        for (Coords coords : footprint) {
            boolean exteriorWall = coords.allAdjacent().stream().anyMatch(adjacent -> !footprint.contains(adjacent));
            boolean anotherBuilding = board.getBuildingsAt(coords).stream().anyMatch(other -> other != building
                  && other.getBldgClass() != IBuilding.WALL && other.getBldgClass() != IBuilding.FENCE);
            if (!coords.equals(origin) && (coords.distance(origin) > radius || exteriorWall || anotherBuilding)) {
                continue;
            }
            new BuildingEnvironmentHandler(gameManager).collapsed(building, coords, building.getHeight(coords), reports);
            for (Entity occupant : List.copyOf(getGame().getEntitiesVector(coords, building.getBoardId()))) {
                if (occupant instanceof IBuilding || occupant.isDestroyed() || occupant.isAirborne()
                      || !building.getBuildingRuntimeState().encloses(building, occupant)) {
                    continue;
                }
                if (occupant instanceof Infantry && !EnvironmentalSealingRules.canOperateFullySubmerged(occupant)) {
                    reports.addAll(gameManager.destroyEntity(occupant, "trapped in an underground cave-in"));
                } else {
                    int remaining = damage;
                    while (remaining > 0) {
                        int cluster = Math.min(5, remaining);
                        HitData hit = occupant.rollHitLocation(occupant instanceof ProtoMek ? ToHitData.HIT_SPECIAL_PROTO
                              : ToHitData.HIT_PUNCH, ToHitData.SIDE_FRONT);
                        hit.setGeneralDamageType(HitData.DAMAGE_PHYSICAL_NONATTACK);
                        reports.addAll(gameManager.damageEntity(occupant, hit, cluster));
                        remaining -= cluster;
                    }
                }
                gameManager.entityUpdate(occupant.getId());
            }
            building.getEquipmentInHex(building.boardToRelative(coords)).forEach(mount -> mount.setDestroyed(true));
            int floors = building.getHeight(coords);
            building.setCurrentCF(0, coords);
            building.setPhaseCF(0, coords);
            gameManager.send(new Packet(PacketCommand.BLDG_COLLAPSE, new Vector<>(List.of(coords)),
                  building.getBoardId(), building.getId()));
            board.collapseBuilding(building, coords);
            building.collapseFloorsOnHex(coords, floors);
            gameManager.sendChangedHex(coords, building.getBoardId());
        }
        gameManager.entityUpdate(building.getId());
    }
}
