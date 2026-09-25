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

package megamek.common.moves;

import java.util.List;

import megamek.common.bays.AbstractSmallCraftASFBay;
import megamek.common.board.Coords;
import megamek.common.units.BuildingBayDoors;
import megamek.common.units.Entity;
import megamek.common.units.IAero;
import megamek.common.units.MobileStructure;

/** Air Mobile Structures use the fighter/small-craft recovery rules of TW pp.86–87 (TO:AUE p.38). */
public final class MobileStructureBayRecovery {
    private MobileStructureBayRecovery() { }

    /** The authored index keeps the selected physical door's two five-turn slots independent. */
    public record Entry(AbstractSmallCraftASFBay bay, int doorIndex) { }

    public static Entry entry(MobileStructure carrier, Entity unit, Coords position, int boardId,
          int facing, int velocity, int altitude) {
        if (carrier.getGame() == null || carrier.getBoardId() != boardId || position == null
              || !carrier.getGame().getBoard(boardId).contains(position)
              || !MobileStructureAirMovement.isAirborne(carrier) || !carrier.isDone()
              || carrier.isDestroyed() || carrier.isDoomed() || !carrier.isActive()
              || !(unit instanceof IAero aero) || aero.isOutControlTotal() || unit.isDestroyed() || unit.isDoomed()
              || !unit.isActive() || !(unit.isFighter() || unit.isSmallCraft())
              || !unit.isLoadableThisTurn() || unit.getTransportId() != Entity.NONE
              || unit.getOwner() == null || carrier.getOwner() == null || unit.isEnemyOf(carrier)
              || facing != carrier.getFacing() || velocity != carrier.delta_distance / 16 || altitude <= 0) {
            return null;
        }
        for (var transporter : carrier.getTransportBays()) {
            if (!(transporter instanceof AbstractSmallCraftASFBay bay) || !bay.canLoad(unit)) { continue; }
            var placements = BuildingBayDoors.placements(carrier, bay);
            if (placements.isEmpty()) {
                // Older designs have no door placement. Their operational bays retain the existing hull-hex fallback.
                if (carrier.getInternalBuilding().getCoordsList().stream()
                      .map(carrier::relativeToBoard).anyMatch(position::equals)
                      && altitude == MobileStructureAirMovement.aerospaceAltitude(carrier, position, 0)) {
                    return new Entry(bay, -1);
                }
            } else {
                for (int index : BuildingBayDoors.usablePlacementIndices(carrier, bay)) {
                    var door = placements.get(index);
                    if (bay.availableRecoverySlots(index) > 0
                          && position.equals(carrier.relativeToBoard(door.position().hex()))
                          && altitude == MobileStructureAirMovement.aerospaceAltitude(carrier, position,
                                BuildingBayDoors.physicalLevel(carrier, door))) {
                        return new Entry(bay, index);
                    }
                }
            }
        }
        return null;
    }

    /** Used by the recovery command and its chooser; multihex carriers need not occupy this hex with their origin. */
    public static List<MobileStructure> candidates(Entity unit, MovePath path) {
        return path.getGame().getEntitiesVector().stream().filter(MobileStructure.class::isInstance)
              .map(MobileStructure.class::cast).filter(carrier -> entry(carrier, unit, path.getFinalCoords(),
                    path.getFinalBoardId(), path.getFinalFacing(), path.getFinalVelocity(), path.getFinalAltitude()) != null)
              .toList();
    }
}
