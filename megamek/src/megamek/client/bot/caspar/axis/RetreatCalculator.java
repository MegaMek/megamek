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
import megamek.common.Compute;
import megamek.common.Coords;

/**
 * Calculates the retreat
 * @author Luana Coppio
 */
public class RetreatCalculator extends BaseAxisCalculator {
    @Override
    public float[] calculateAxis(Pathing pathing, GameState gameState) {
        // This calculates the retreat
        float[] retreat = axis();

        var unit = pathing.getEntity();
        float unitHealth = ((float) unit.getArmorRemainingPercent() + (float) unit.getInternalRemainingPercent()) / 2f;
        int dist = getClosestEnemy(pathing, gameState.getEnemyUnitsSOU());
        float retreatScore = (1.0f - unitHealth) * (1.0f / Math.max(1, dist / 3.0f));

        retreat[0] = retreatScore;

        return retreat;
    }


    /**
     * Calculates the distance to the closest enemy
     * @param pathing the pathing object
     * @param enemies the enemy units on a StructOfUnitArrays
     * @return the distance to the closest enemy VIP, 0 if there are no VIP enemies
     */
    private static int getClosestEnemy(Pathing pathing, StructOfUnitArrays enemies) {
        int originX = pathing.getFinalCoords().getX();
        int originY = pathing.getFinalCoords().getY();
        int x;
        int y;
        int dist = Integer.MAX_VALUE;
        for (int i = 0; i < enemies.size(); i++) {
            x = enemies.getX(i);
            y = enemies.getY(i);
            dist = Math.min(dist, Coords.distance(originX, originY, x, y));
        }
        return dist;
    }
}
