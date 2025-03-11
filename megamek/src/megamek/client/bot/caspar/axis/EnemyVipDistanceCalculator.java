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
import megamek.client.ratgenerator.MissionRole;
import megamek.common.Coords;
import megamek.common.Entity;

import java.util.Optional;

/**
 * Calculates the distance to the closest enemy VIP
 * @author Luana Coppio
 */
public class EnemyVipDistanceCalculator extends BaseAxisCalculator {
    private static final int MAX_DISTANCE = 34; // approximately 2 boards

    @Override
    public double[] calculateAxis(Pathing pathing, GameState gameState) {
        // This calculates the distance to the closest enemy VIP
        double[] vipDistance = axis();
        int dist = getDistanceToClosestVIP(pathing, gameState.getEnemyUnitsSOU());

        vipDistance[0] = normalize(MAX_DISTANCE - dist, 0, MAX_DISTANCE);
        return vipDistance;
    }

    private static int getDistanceToClosestVIP(Pathing pathing, StructOfUnitArrays enemies) {
        int originX = pathing.getFinalCoords().getX();
        int originY = pathing.getFinalCoords().getY();
        int x;
        int y;
        int dist = Integer.MAX_VALUE;
        for (int i = 0; i < enemies.size(); i++) {
            if (enemies.isVIP(i)) {
                x = enemies.getX(i);
                y = enemies.getY(i);
                dist = Math.min(dist, Coords.distance(originX, originY, x, y));
            }
        }
        return dist;
    }


}
