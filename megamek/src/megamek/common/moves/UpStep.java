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

import megamek.common.Entity;
import megamek.common.EntityMovementMode;
import megamek.common.Game;
import megamek.common.ProtoMek;
import megamek.common.pathfinder.CachedEntityState;

import java.util.EnumSet;
import java.util.Set;

/**
 * This class handles the up step of a unit.
 * It is used in the MoveStep compilation to calculate the movement of a unit.
 * @author Luana Coppio
 * @since 0.50.07
 */
class UpStep implements PhasePass {
    private static final EnumSet<MovePath.MoveStepType> TYPES = EnumSet.of(MovePath.MoveStepType.UP);

    @Override
    public Set<MovePath.MoveStepType> getTypesOfInterest() {
        return TYPES;
    }

    @Override
    public PhasePassResult preCompilation(final MoveStep moveStep, final Game game, final Entity entity, MoveStep prev,
          final CachedEntityState cachedEntityState) {
        if (entity.isAirborne()) {
            moveStep.setAltitude(moveStep.getAltitude() + 1);
            moveStep.setMp(2);
        } else {
            if (entity.getMovementMode() == EntityMovementMode.WIGE) {
                // If on the ground, pay liftoff cost. If airborne, pay 1 MP to increase
                // elevation (LAMs and glider ProtoMeks only)
                if (moveStep.getClearance() == 0) {
                    moveStep.setMp((entity instanceof ProtoMek) ? 4 : 5);
                } else {
                    moveStep.setMp(1);
                }
            } else {
                if (entity instanceof ProtoMek) {
                    moveStep.setMp(moveStep.isJumping() ? 0 : 2);
                } else {
                    moveStep.setMp(moveStep.isJumping() ? 0 : 1);
                }
            }
            moveStep.setElevation(moveStep.getElevation() + 1);
        }
        return PhasePassResult.BREAK;
    }
}
