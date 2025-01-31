/*
 * Copyright (c) 2021-2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
*/

package megamek.client.bot.princess;

import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.EntityMovementMode;
import megamek.common.Mek;
import megamek.common.Minefield;
import megamek.common.MovePath;
import megamek.common.MoveStep;
import megamek.common.annotations.Nullable;

/**
 * This class contains logic to evaluate the damage a unit could sustain from
 * moving a long a move path containing minefields
 *
 * @author NickAragua
 */
public class MinefieldUtil {
    /**
     * Calculate how much damage we'll take from stepping on mines over a particular
     * path
     */
    public static double checkPathForMinefieldHazards(MovePath path) {
        double hazardAccumulator = 0;

        for (MoveStep step : path.getStepVector()) {
            hazardAccumulator += calcMinefieldHazardForHex(step, path.getEntity(),
                    path.isJumping(), step.equals(path.getLastStep()));
        }

        // TODO: Teach bot to activate minesweepers

        return hazardAccumulator;
    }

    /**
     * Calculate how much damage we'll take from stepping on mines in a particular
     * hex
     */
    public static double calcMinefieldHazardForHex(@Nullable MoveStep step, Entity movingUnit,
            boolean isJumping, boolean lastStep) {
        // if we're not actually taking a step, no minefield hazard
        if ((step == null) || !step.getType().entersNewHex()) {
            return 0;
        }

        // if our movement mode does not result in minefield detonation, no minefield
        // hazard
        if (!movingUnit.getMovementMode().detonatesGroundMinefields()) {
            return 0;
        }

        double hazardAccumulator = 0;
        // hovercraft and WIGEs grinding along the ground detonate minefields on a 12
        boolean hoverMovement = (movingUnit.getMovementMode() == EntityMovementMode.HOVER) ||
                ((movingUnit.getMovementMode() == EntityMovementMode.WIGE) && (movingUnit.getElevation() == 0));
        double hoverMovementMultiplier = hoverMovement ? Compute.oddsAbove(Minefield.HOVER_WIGE_DETONATION_TARGET) : 1;

        for (Minefield minefield : movingUnit.getGame().getMinefields(step.getPosition())) {
            switch (minefield.getType()) {
                case Minefield.TYPE_CONVENTIONAL:
                case Minefield.TYPE_INFERNO:
                    // if we're either not jumping or it's the last step
                    if (!isJumping || lastStep) {
                        hazardAccumulator += minefield.getDensity() * hoverMovementMultiplier;
                    }
                    break;
                case Minefield.TYPE_ACTIVE:
                    hazardAccumulator += minefield.getDensity() * hoverMovementMultiplier;
                    break;
                case Minefield.TYPE_VIBRABOMB:
                    // only meks interact with VibraBombs
                    // meks > 10 tons the "setting" will set off VibraBombs before they
                    // get to them so we don't particularly care
                    if (movingUnit instanceof Mek && (!isJumping || lastStep) &&
                            (movingUnit.getWeight() >= minefield.getSetting()) &&
                            (movingUnit.getWeight() <= minefield.getSetting() + 10)) {
                        hazardAccumulator += minefield.getDensity();
                    }
                    break;
            }
        }

        return hazardAccumulator;
    }
}
