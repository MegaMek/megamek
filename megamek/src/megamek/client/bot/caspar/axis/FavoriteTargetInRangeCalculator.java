/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 2 or (at your option) any later version,
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
 */
package megamek.client.bot.caspar.axis;

import megamek.client.bot.common.GameState;
import megamek.client.bot.common.Pathing;
import megamek.client.bot.common.StructOfUnitArrays;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.UnitRole;

import static megamek.codeUtilities.MathUtility.clamp;
import static megamek.codeUtilities.MathUtility.clamp01;

/**
 * Calculates if the favorite target role type is in range
 * @author Luana Coppio
 */
public class FavoriteTargetInRangeCalculator extends BaseAxisCalculator {

    @Override
    public float[] axis() {
        return new float[UnitRole.values().length];
    }

    @Override
    public float[] calculateAxis(Pathing pathing, GameState gameState) {
        // calculate if the favorite target role type is in range
        float[] favoriteTarget = axis();
        Entity unit = pathing.getEntity();
        int maxRange = unit.getMaxWeaponRange();

        favoriteTarget[0] = getNormalizedDistanceToClosestEnemyWithRole(pathing, gameState.getEnemyUnitsSOU(), maxRange,
              UnitRole.SNIPER);
        favoriteTarget[1] = getNormalizedDistanceToClosestEnemyWithRole(pathing, gameState.getEnemyUnitsSOU(), maxRange,
              UnitRole.MISSILE_BOAT);
        favoriteTarget[2] = getNormalizedDistanceToClosestEnemyWithRole(pathing, gameState.getEnemyUnitsSOU(), maxRange,
              UnitRole.JUGGERNAUT);

        return favoriteTarget;
    }

    /**
     * Counts the number of units covering the current unit
     * @param pathing The movement path to evaluate
     * @param structOfUnitArrays The struct of unit arrays to evaluate
     * @return The number of units covering the current unit
     */
    private float getNormalizedDistanceToClosestEnemyWithRole(Pathing pathing, StructOfUnitArrays structOfUnitArrays, int distance, UnitRole role) {
        int x = pathing.getFinalCoords().getX();
        int y = pathing.getFinalCoords().getY();
        int length = structOfUnitArrays.size();

        int xd;
        int yd;
        float dist;
        float closedDist = Integer.MAX_VALUE;
        for (int i = 0; i < length; i++) {
            xd = structOfUnitArrays.getX(i);
            yd = structOfUnitArrays.getY(i);
            if (role == structOfUnitArrays.getRole(i)) {
                dist = Coords.distance(x, y, xd, yd);
                closedDist = Math.min(closedDist, dist);
            }
        }
        return clamp01(1 - clamp01(closedDist / (distance + 1f)));
    }
}
