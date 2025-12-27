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
package megamek.common.moves;

import java.util.EnumSet;
import java.util.Set;

import megamek.common.enums.MoveStepType;
import megamek.common.game.Game;
import megamek.common.pathfinder.CachedEntityState;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Handles the elevator ascend step for industrial elevators.
 * <p>
 * Industrial elevators move 1 level per turn and cost 1 Walking/Cruising MP per level. Units may only use
 * Walking/Cruising MP when using an industrial elevator.
 *
 * @author MegaMek Team
 * @since 0.50.07
 */
class ElevatorAscendStep implements PhasePass {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final EnumSet<MoveStepType> TYPES = EnumSet.of(MoveStepType.ELEVATOR_ASCEND);

    @Override
    public Set<MoveStepType> getTypesOfInterest() {
        return TYPES;
    }

    @Override
    public PhasePassResult preCompilation(final MoveStep moveStep, final Game game, final Entity entity,
          MoveStep prev, final CachedEntityState cachedEntityState) {
        int oldElevation = moveStep.getElevation();
        // Industrial elevators always cost 1 MP per level moved
        moveStep.setMp(1);
        moveStep.addMpUsed(1);
        // Increase elevation by 1
        moveStep.setElevation(moveStep.getElevation() + 1);
        // Elevator movement prohibits running
        moveStep.setRunProhibited(true);
        // Set movement type to WALK (elevator use is walking movement)
        moveStep.setMovementType(EntityMovementType.MOVE_WALK);
        LOGGER.info(
              "[ELEVATOR] ElevatorAscendStep.preCompilation: entity={}, oldElevation={}, newElevation={}, position={}, movementType={}",
              entity.getDisplayName(),
              oldElevation,
              moveStep.getElevation(),
              moveStep.getPosition(),
              moveStep.getMovementType(false));
        return PhasePassResult.BREAK;
    }
}
