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
package megamek.common.compute;

import megamek.common.game.Game;
import megamek.common.moves.MovePath;
import megamek.common.units.CrewType;
import megamek.common.units.EntityMovementType;
import megamek.common.units.VTOL;

/**
 * The defensive movement modifier a unit would have after moving a given path: the number the Ctrl-W
 * modifier-envelope overlay shows the player for each reachable hex.
 *
 * <p>One computation, shared: the overlay sprite and the bot's position evaluation both call this, so the
 * hexes the bot values are exactly the hexes the overlay shows a player as good, and the two can never
 * drift. The number is the target movement modifier for the path's distance and mode, plus the evasion
 * bonus of a dual-cockpit Mek with a dedicated pilot.</p>
 */
public final class DefensiveMovementModifier {

    private DefensiveMovementModifier() {
    }

    /**
     * The defensive to-hit modifier the moving unit would enjoy after this path.
     *
     * @param movePath the path being considered
     * @param game     the current game
     *
     * @return the modifier's value, higher meaning harder to hit
     */
    public static int forPath(MovePath movePath, Game game) {
        boolean vtolMovement = movePath.getEntity() instanceof VTOL
              || (movePath.getLastStepMovementType() == EntityMovementType.MOVE_VTOL_WALK)
              || (movePath.getLastStepMovementType() == EntityMovementType.MOVE_VTOL_RUN)
              || (movePath.getLastStepMovementType() == EntityMovementType.MOVE_VTOL_SPRINT);
        int modifier = Compute.getTargetMovementModifier(movePath.getHexesMoved(),
              movePath.isJumping(),
              vtolMovement,
              game).getValue();
        // Dual-cockpit Meks with a dedicated pilot gain an evasion bonus when moving on the ground.
        if (movePath.getEntity().getCrew().getCrewType().equals(CrewType.DUAL)
              && movePath.getEntity().getCrew().hasDedicatedPilot()
              && !movePath.isJumping() && movePath.getHexesMoved() > 0) {
            modifier++;
        }
        return modifier;
    }
}
