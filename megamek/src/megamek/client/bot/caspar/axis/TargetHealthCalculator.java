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

import java.util.List;

/**
 * Calculates the target health
 * @author Luana Coppio
 */
public class TargetHealthCalculator extends BaseAxisCalculator {
    @Override
    public float[] calculateAxis(Pathing pathing, GameState gameState) {
        // This calculates the target health
        float[] targetHealth = axis();
        float enemyHealth = getClosestEnemyHealth(pathing, gameState.getEnemyUnits());
        targetHealth[0] = enemyHealth;
        return targetHealth;
    }

    /**
     * Calculates the health of the closest enemy
     * @param pathing the pathing object
     * @param entities the enemy units
     * @return the health of the closest enemy
     */
    private static float getClosestEnemyHealth(Pathing pathing, List<Entity> entities) {
        int distance = Integer.MAX_VALUE;
        float health = 0f;
        for (Entity entity : entities) {
            if (entity.getPosition() != null && entity.getPosition().distance(pathing.getFinalCoords()) < distance) {
                distance = entity.getPosition().distance(pathing.getFinalCoords());
                health = (float) entity.getArmorRemainingPercent() + (float) entity.getInternalRemainingPercent();
            }
        }
        return health;
    }
}
