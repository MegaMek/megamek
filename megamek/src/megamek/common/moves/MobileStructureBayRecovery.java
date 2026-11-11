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

import jakarta.annotation.Nullable;
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
        boolean isPositionOnCarrierBoard = isOnCarrierBoard(carrier, position, boardId);
        boolean isCarrierAbleToRecover = isCarrierAbleToRecover(carrier);
        boolean isUnitRecoverable = isRecoverableCraft(unit);
        boolean isUnitFriendly = isFriendlyTo(unit, carrier);
        boolean matchesCarrierFacing = facing == carrier.getFacing();
        boolean matchesCarrierVelocity = velocity == carrier.delta_distance / 16;
        boolean isAboveGround = altitude > 0;
        if (!isPositionOnCarrierBoard ||!isCarrierAbleToRecover || !isUnitRecoverable || !isUnitFriendly
              || !matchesCarrierFacing || !matchesCarrierVelocity || !isAboveGround) {
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

    /** @return {@code true} when the position is on the board the carrier is on */
    private static boolean isOnCarrierBoard(MobileStructure carrier, @Nullable Coords position, int boardId) {
        if (carrier.getGame() == null || position == null) {
            return false;
        }
        if (carrier.getBoardId() != boardId) {
            return false;
        }
        return carrier.getGame().getBoard(boardId).contains(position);
    }

    /** @return {@code true} when the carrier is airborne, has finished moving and is still operational */
    private static boolean isCarrierAbleToRecover(MobileStructure carrier) {
        boolean isAirborne = MobileStructureAirMovement.isAirborne(carrier);
        boolean hasFinishedMoving = carrier.isDone();
        boolean isOperational = !carrier.isDestroyed() && !carrier.isDoomed() && carrier.isActive();
        return isAirborne && hasFinishedMoving && isOperational;
    }

    /** @return {@code true} when the unit is a controlled, operational fighter or small craft free to be loaded */
    private static boolean isRecoverableCraft(Entity unit) {
        if (!(unit instanceof IAero aero)) {
            return false;
        }
        boolean isInControl = !aero.isOutControlTotal();
        boolean isOperational = !unit.isDestroyed() && !unit.isDoomed() && unit.isActive();
        boolean isFighterOrSmallCraft = unit.isFighter() || unit.isSmallCraft();
        boolean isLoadableThisTurn = unit.isLoadableThisTurn();
        boolean isNotAlreadyCarried = unit.getTransportId() == Entity.NONE;
        return isInControl && isOperational && isFighterOrSmallCraft && isLoadableThisTurn && isNotAlreadyCarried;
    }

    /** @return {@code true} when both units have owners and the unit is not an enemy of the carrier */
    private static boolean isFriendlyTo(Entity unit, MobileStructure carrier) {
        if (unit.getOwner() == null || carrier.getOwner() == null) {
            return false;
        }
        return !unit.isEnemyOf(carrier);
    }

    /** Used by the recovery command and its chooser; multihex carriers need not occupy this hex with their origin. */
    public static List<MobileStructure> candidates(Entity unit, MovePath path) {
        return path.getGame().getEntitiesVector().stream().filter(MobileStructure.class::isInstance)
              .map(MobileStructure.class::cast).filter(carrier -> entry(carrier, unit, path.getFinalCoords(),
                    path.getFinalBoardId(), path.getFinalFacing(), path.getFinalVelocity(), path.getFinalAltitude()) != null)
              .toList();
    }
}
