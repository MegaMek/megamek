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

/**
 * Calculates the number of nearby enemy units
 * @author Luana Coppio
 */
public class NearbyEnemyCountCalculator extends BaseAxisCalculator {

    private static final int MIN_DISTANCE = 7;
    private static final double OPTIMAL_RANGE_RATIO = 0.6d;
    @Override
    public float[] calculateAxis(Pathing pathing, GameState gameState) {
        // This calculates the number of nearby enemy units
        float[] nearbyEnemies = axis();

        int distanceToKeepEnemiesAt = Math.min(MIN_DISTANCE, (int) (pathing.getEntity().getMaxWeaponRange() * OPTIMAL_RANGE_RATIO));
        nearbyEnemies[0] = countNearbyEnemies(pathing, gameState.getEnemyUnitsSOU(), distanceToKeepEnemiesAt);
        return nearbyEnemies;
    }

    /**
     * Counts the number of units too close
     * @param pathing The movement path to evaluate
     * @param structOfUnitArrays The struct of unit arrays to evaluate
     * @return The number of units covering the current unit
     */
    private int countNearbyEnemies(Pathing pathing, StructOfUnitArrays structOfUnitArrays, int distance) {
        int xd;
        int yd;
        int x = pathing.getFinalCoords().getX();
        int y = pathing.getFinalCoords().getY();
        int originId = pathing.getEntity().getId();
        int length = structOfUnitArrays.size();

        double dist;
        int units = 0;

        for (int i = 0; i < length; i++) {
            int id = structOfUnitArrays.getId(i);
            if (id == originId) {
                continue;
            }

            xd = structOfUnitArrays.getX(i);
            yd = structOfUnitArrays.getY(i);

            dist = Coords.distance(x, y, xd, yd);
            if (dist <= distance) {
                units++;
            }
        }
        return units;
    }
}
