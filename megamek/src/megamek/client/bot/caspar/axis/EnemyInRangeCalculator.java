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

import java.util.List;

import static megamek.codeUtilities.MathUtility.clamp01;

/**
 * Calculates the enemy threat in nearby hexes (37 values).
 * @author Luana Coppio
 */
public class EnemyInRangeCalculator extends BaseAxisCalculator {

    @Override
    public float[] calculateAxis(Pathing pathing, GameState gameState) {
        float[] distance = axis();
        var unit = pathing.getEntity();
        var maxRange = unit.getMaxWeaponRange();
        var enemies = gameState.getEnemyUnitsSOU();
        var closestEnemy = getClosestEnemy(pathing, enemies);

        if (maxRange > 0 && closestEnemy < maxRange) {
            distance[0] = clamp01(1 - (float) closestEnemy / maxRange);
        }

        return distance;
    }

    private static int getClosestEnemy(Pathing pathing, StructOfUnitArrays enemies) {
        int originX = pathing.getFinalCoords().getX();
        int originY = pathing.getFinalCoords().getY();
        int x;
        int y;
        int closestEnemy = Integer.MAX_VALUE;
        int distance;
        for (int i = 0; i < enemies.size(); i++) {
            x = enemies.getX(i);
            y = enemies.getY(i);
            distance = Coords.distance(originX, originY, x, y);
            closestEnemy = Math.min(Coords.distance(originX, originY, x, y), closestEnemy);
        }
        return closestEnemy;
    }
}
