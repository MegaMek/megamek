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

import java.util.EnumSet;
import java.util.Set;

import megamek.common.Hex;
import megamek.common.board.Coords;
import megamek.common.enums.MoveStepType;
import megamek.common.game.Game;
import megamek.common.pathfinder.CachedEntityState;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementType;
import megamek.common.units.Mek;
import megamek.logging.MMLogger;

/**
 * PhasePass handler for the CLIMB step type (TO:AR p.20).
 *
 * <p>Handles the movement cost calculation for climbing one level up or down a cliff face.
 * Validates that the entity is a Mek capable of climbing, and sets the MP cost
 * based on the number of functional arms. Each CLIMB step raises the entity's
 * elevation by one level toward the facing hex.</p>
 */
class ClimbStep implements PhasePass {
    private static final MMLogger LOGGER = MMLogger.create(ClimbStep.class);
    private static final EnumSet<MoveStepType> TYPES = EnumSet.of(MoveStepType.CLIMB);

    @Override
    public Set<MoveStepType> getTypesOfInterest() {
        return TYPES;
    }

    @Override
    public PhasePassResult preCompilation(final MoveStep moveStep, final Game game, final Entity entity,
          MoveStep prev, final CachedEntityState cachedEntityState) {
        LOGGER.info("ClimbStep: preCompilation for entity {} at position {} elevation {}",
              entity.getDisplayName(), moveStep.getPosition(), moveStep.getElevation());

        // Only Meks can climb using these rules
        if (!ClimbingHelper.canClimb(entity)) {
            LOGGER.info("ClimbStep: entity cannot climb (not a Mek or missing arm requirements)");
            moveStep.setMovementType(EntityMovementType.MOVE_ILLEGAL);
            return PhasePassResult.BREAK;
        }

        Mek mek = (Mek) entity;
        int climbableArms = ClimbingHelper.countClimbableArms(mek);
        int mpCost = ClimbingHelper.getClimbingMPCostPerLevel(mek);

        // Determine the target hex (the hex the entity is facing)
        Coords currentPosition = moveStep.getPosition();
        int facing = moveStep.getFacing();
        Coords targetCoords = currentPosition.translated(facing);

        Hex currentHex = game.getBoard(moveStep.getBoardId()).getHex(currentPosition);
        Hex targetHex = game.getBoard(moveStep.getBoardId()).getHex(targetCoords);

        if ((currentHex == null) || (targetHex == null)) {
            LOGGER.info("ClimbStep: current or target hex is null");
            moveStep.setMovementType(EntityMovementType.MOVE_ILLEGAL);
            return PhasePassResult.BREAK;
        }

        int currentLevel = currentHex.getLevel();
        int targetLevel = targetHex.getLevel();
        int currentElevation = moveStep.getElevation();
        int absolutePosition = currentLevel + currentElevation;
        int elevationDifference = targetLevel - absolutePosition;

        LOGGER.info("ClimbStep: currentLevel={}, targetLevel={}, currentElevation={}, " +
                    "absolutePosition={}, elevationDifference={}, climbableArms={}, mpCost={}",
              currentLevel, targetLevel, currentElevation,
              absolutePosition, elevationDifference, climbableArms, mpCost);

        // Must have somewhere to climb - target must be higher than current position
        if (elevationDifference <= 0) {
            LOGGER.info("ClimbStep: target is not higher than current position, cannot climb up");
            moveStep.setMovementType(EntityMovementType.MOVE_ILLEGAL);
            return PhasePassResult.BREAK;
        }

        // Set MP cost and climbing state
        moveStep.setMp(mpCost);
        moveStep.setIsClimbing(true);

        // Increment elevation by 1 level
        moveStep.setElevation(currentElevation + 1);
        moveStep.setMovementType(EntityMovementType.MOVE_WALK);

        LOGGER.info("ClimbStep: climbing one level, new elevation={}, mp cost={}",
              moveStep.getElevation(), mpCost);

        return PhasePassResult.BREAK;
    }
}
