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

import megamek.common.compute.Compute;
import megamek.common.enums.MoveStepType;
import megamek.common.game.Game;
import megamek.common.pathfinder.CachedEntityState;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;
import megamek.common.units.TripodMek;

/**
 * This class handles the turn step of a unit. It is used in the MoveStep compilation to calculate the movement of a
 * unit.
 *
 * @author Luana Coppio
 * @since 0.50.07
 */
class TurnStep implements PhasePass {

    private static final EnumSet<MoveStepType> TYPES = EnumSet.of(MoveStepType.TURN_LEFT,
          MoveStepType.TURN_RIGHT);

    @Override
    public Set<MoveStepType> getTypesOfInterest() {
        return TYPES;
    }

    @Override
    public PhasePassResult preCompilation(final MoveStep moveStep, final Game game, final Entity entity, MoveStep prev,
          final CachedEntityState cachedEntityState) {
        // Check for pavement movement.
        if (!entity.isAirborne() && Compute.canMoveOnPavement(game, prev.getPosition(), moveStep.getPosition(),
              moveStep)) {
            moveStep.setPavementStep(true);
        } else {
            moveStep.setPavementStep(false);
            moveStep.setOnlyPavementOrRoad(false);
        }

        // Infantry can turn for free, except for field artillery
        moveStep.setMp((moveStep.isJumping() ||
              moveStep.isHasJustStood() ||
              (entity instanceof Infantry infantry && !infantry.hasActiveFieldArtillery())) ? 0 : 1);
        moveStep.setNStraight(0);
        if (entity.isAirborne() && (entity.isAero())) {
            moveStep.setMp(moveStep.asfTurnCost(game, moveStep.getType(), entity));
            moveStep.setNTurns(moveStep.getNTurns() + 1);

            if (moveStep.useAeroAtmosphere(game, entity)) {
                moveStep.setFreeTurn(false);
            }
        }

        // tripods with all their legs only pay for their first facing change
        if ((entity instanceof TripodMek mek) &&
              mek.atLeastOneBadLeg() &&
              getTypesOfInterest().contains(prev.getType())) {
            moveStep.setMp(0);
        }
        if (entity.isDropping()) {
            moveStep.setMp(0);
        }
        moveStep.adjustFacing(moveStep.getType());
        return PhasePassResult.BREAK;
    }
}
