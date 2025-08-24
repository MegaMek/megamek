/*
 * Copyright (C) 2014-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.common.pathfinder.filters;

import megamek.common.board.Coords;
import megamek.common.game.Game;
import megamek.common.moves.MovePath;
import megamek.common.moves.MoveStep;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementType;

/**
 * Filters edges that are illegal.
 * <p>
 * Current implementation uses MoveStep.isMovementPossible() to verify legality.
 */
public class MovePathLegalityFilter extends Filter<MovePath> {
    Game game;

    public MovePathLegalityFilter(Game game) {
        this.game = game;
    }

    @Override
    public boolean shouldStay(MovePath edge) {
        if (edge.getEntity().isAero()) {
            /*
             * isMovementPossible is currently not working for aero units,
             * so we have to use a substitute.
             */
            if (edge.length() == 0) {
                return true;
            } else {
                MoveStep lastStep = edge.getLastStep();
                return (lastStep == null) || lastStep.getMovementType(true) != EntityMovementType.MOVE_ILLEGAL;
            }
        }
        Coords previousPosition;
        int previousElevation;
        if (edge.length() > 1) {
            MoveStep previousStep = edge.getSecondLastStep();
            previousElevation = previousStep.getElevation();
            previousPosition = previousStep.getPosition();
        } else {
            Entity entity = edge.getEntity();
            previousElevation = entity.getElevation();
            previousPosition = entity.getPosition();
        }
        return edge.getLastStep().isMovementPossible(
              game, previousPosition, previousElevation, edge.getCachedEntityState());
    }
}
