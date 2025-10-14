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
import megamek.common.options.OptionsConstants;
import megamek.common.pathfinder.CachedEntityState;
import megamek.common.units.Entity;

/**
 * This class handles the lateral backwards step of a unit. It is used in the MoveStep compilation to calculate the
 * movement of a unit.
 *
 * @author Luana Coppio
 * @since 0.50.07
 */
class LateralBackwardStep extends SideStepStep {
    private static final EnumSet<MoveStepType> TYPES = EnumSet.of(MoveStepType.LATERAL_LEFT_BACKWARDS,
          MoveStepType.LATERAL_RIGHT_BACKWARDS);

    @Override
    public Set<MoveStepType> getTypesOfInterest() {
        return TYPES;
    }

    @Override
    public PhasePassResult preCompilation(final MoveStep moveStep, final Game game, final Entity entity, MoveStep prev,
          final CachedEntityState cachedEntityState) {
        moveStep.moveInDir((MovePath.getAdjustedFacing(moveStep.getFacing(),
              MovePath.turnForLateralShift(moveStep.getType())) +
              3) % 6);
        moveStep.setThisStepBackwards(true);
        if (!entity.hasQuirk(OptionsConstants.QUIRK_POS_POWER_REVERSE)) {
            moveStep.setRunProhibited(true);
        }

        return PhasePassResult.COMPILE;
    }
}
