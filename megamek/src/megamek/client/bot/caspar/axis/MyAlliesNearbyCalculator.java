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
 * Calculates the number of enemies close by.
 * @author Luana Coppio
 */
public class MyAlliesNearbyCalculator extends BaseAxisCalculator {

    @Override
    public float[] calculateAxis(Pathing pathing, GameState gameState) {
        float[] enemiesNearby = axis();

        int units = getUnitsAtRange(8, pathing, gameState.getOwnUnitsSOU());
        units += getUnitsAtRange(8, pathing, gameState.getFriendlyUnitsSOU());

        enemiesNearby[0] = units;
        return enemiesNearby;
    }

    /**
     * Calculates the distance to the closest enemy VIP
     * @param pathing the pathing object
     * @param friendlies the enemy units on a StructOfUnitArrays
     * @return the distance to the closest enemy VIP, 0 if there are no VIP enemies
     */
    private static int getUnitsAtRange(int distance, Pathing pathing, StructOfUnitArrays friendlies) {
        int originX = pathing.getFinalCoords().getX();
        int originY = pathing.getFinalCoords().getY();
        int id = pathing.getEntity().getId();
        int x;
        int y;
        int units = 0;
        for (int i = 0; i < friendlies.size(); i++) {
            x = friendlies.getX(i);
            y = friendlies.getY(i);
            // Ignore the unit itself
            if (friendlies.getId(i) == id) {
                continue;
            }
            if (Coords.distance(originX, originY, x, y) < distance) {
                units++;
            }
        }
        return units;
    }
}
