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

/**
 * Calculates if the favorite target role type is in range
 * @author Luana Coppio
 */
public class FavoriteTargetInRangeCalculator extends BaseAxisCalculator {

    @Override
    public float[] calculateAxis(Pathing pathing, GameState gameState) {
        // calculate if the favorite target role type is in range
        float[] favoriteTarget = axis();
        Entity unit = pathing.getEntity();
        UnitRole role = unit.getRole();
        float scoutWeight = 0.3f;
        float strikerWeight = 0.5f;
        float juggernautWeight = 0.7f;
        float sniperWeight = 0.9f;
        float missileBoatWeight = 0.9f;
        if (role == UnitRole.SNIPER) {
            scoutWeight = 0.5f;
        } else if (role == UnitRole.JUGGERNAUT) {
            juggernautWeight = 1.0f;
        } else if (role == UnitRole.STRIKER) {
            scoutWeight = 0.5f;
            strikerWeight = 0.6f;
            juggernautWeight = 0.4f;
        } else if (role == UnitRole.MISSILE_BOAT) {
            scoutWeight = 0.6f;
            strikerWeight = 0.7f;
            juggernautWeight = 0.3f;
        }
        int maxRange = unit.getMaxWeaponRange();
        favoriteTarget[0] = getFavoriteTargetScore(pathing, gameState.getEnemyUnitsSOU(),
              maxRange, strikerWeight, scoutWeight, juggernautWeight, sniperWeight, missileBoatWeight);
        return favoriteTarget;
    }

    /**
     * Counts the number of units covering the current unit
     * @param pathing The movement path to evaluate
     * @param structOfUnitArrays The struct of unit arrays to evaluate
     * @return The number of units covering the current unit
     */
    private float getFavoriteTargetScore(Pathing pathing, StructOfUnitArrays structOfUnitArrays,
                                         int distance, float strikerWeight, float scoutWeight,
                                         float juggernautWeight, float sniperWeight,
                                         float missileBoatWeight) {
        int x = pathing.getFinalCoords().getX();
        int y = pathing.getFinalCoords().getY();
        int length = structOfUnitArrays.size();
        UnitRole unitRole;
        int xd;
        int yd;
        float dist;
        float targetScore = 0f;
        for (int i = 0; i < length; i++) {
            xd = structOfUnitArrays.getX(i);
            yd = structOfUnitArrays.getY(i);
            dist = Coords.distance(x, y, xd, yd);
            unitRole = structOfUnitArrays.getRole(i);
            if (dist <= distance) {
                if (unitRole == UnitRole.STRIKER) {
                    targetScore = Math.max(targetScore, strikerWeight * (1 - (dist / distance)));
                } else if (unitRole == UnitRole.SCOUT) {
                    targetScore = Math.max(targetScore, scoutWeight * (1 - (dist / distance)));
                } else if (unitRole == UnitRole.JUGGERNAUT) {
                    targetScore = Math.max(targetScore, juggernautWeight * (1 - (dist / distance)));
                } else if (unitRole == UnitRole.SNIPER) {
                    targetScore = Math.max(targetScore, sniperWeight * (1 - (dist / distance)));
                } else if (unitRole == UnitRole.MISSILE_BOAT) {
                    targetScore = Math.max(targetScore, missileBoatWeight * (1 - (dist / distance)));
                }
            }
        }
        return targetScore;
    }
}
