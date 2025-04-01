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
 * Calculates the distance to the closest enemy VIP
 * @author Luana Coppio
 */
public class EnemyVipDistanceCalculator extends BaseAxisCalculator {
    @Override
    public float[] calculateAxis(Pathing pathing, GameState gameState) {
        // This calculates the distance to the closest enemy VIP
        float[] vipDistance = axis();
        int dist = getDistanceToClosestVIP(pathing, gameState.getEnemyUnitsSOU());

        vipDistance[0] = dist;
        return vipDistance;
    }

    /**
     * Calculates the distance to the closest enemy VIP
     * @param pathing the pathing object
     * @param enemies the enemy units on a StructOfUnitArrays
     * @return the distance to the closest enemy VIP, 0 if there are no VIP enemies
     */
    private static int getDistanceToClosestVIP(Pathing pathing, StructOfUnitArrays enemies) {
        int originX = pathing.getFinalCoords().getX();
        int originY = pathing.getFinalCoords().getY();
        int x;
        int y;
        int highestBV = 0;
        int dist = Integer.MAX_VALUE;
        for (int i = 0; i < enemies.size(); i++) {
            if (enemies.isVIP(i)) {
                x = enemies.getX(i);
                y = enemies.getY(i);
                dist = Math.min(dist, Coords.distance(originX, originY, x, y));
            }
        }
        if (dist == Integer.MAX_VALUE) {
            for (int i = 0; i < enemies.size(); i++) {
                if (enemies.getBV(i) > highestBV) {
                    x = enemies.getX(i);
                    y = enemies.getY(i);
                    highestBV = enemies.getBV(i);
                    dist = Coords.distance(originX, originY, x, y);
                }
            }
        }
        if (dist == Integer.MAX_VALUE) {
            dist = 0;
        }
        return dist;
    }
}
