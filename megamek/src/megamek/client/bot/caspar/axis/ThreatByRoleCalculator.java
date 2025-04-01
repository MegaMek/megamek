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
import megamek.common.UnitRole;

import static megamek.codeUtilities.MathUtility.clamp01;

/**
 * Calculates the threat by role
 * @author Luana Coppio
 */
public class ThreatByRoleCalculator extends BaseAxisCalculator {
    @Override
    public float[] axis() {
        return new float[3];
    }

    @Override
    public float[] calculateAxis(Pathing pathing, GameState gameState) {

        // This calculates the threat by role
        float[] threatByRole = axis();
        //            "threat_by_sniper",
        threatByRole[0] = getThreatByRole(pathing, gameState.getEnemyUnitsSOU(), UnitRole.SNIPER);
        //            "threat_by_missile_boat",
        threatByRole[1] = getThreatByRole(pathing, gameState.getEnemyUnitsSOU(), UnitRole.MISSILE_BOAT);
        //            "threat_by_juggernaut",
        threatByRole[2] = getThreatByRole(pathing, gameState.getEnemyUnitsSOU(), UnitRole.JUGGERNAUT);
        return threatByRole;
    }


    /**
     * Calculates the distance to the closest enemy VIP
     * @param pathing the pathing object
     * @param enemies the enemy units on a StructOfUnitArrays
     * @return the distance to the closest enemy VIP, 0 if there are no VIP enemies
     */
    private static float getThreatByRole(Pathing pathing, StructOfUnitArrays enemies, UnitRole unitRole) {
        int originX = pathing.getFinalCoords().getX();
        int originY = pathing.getFinalCoords().getY();
        int x;
        int y;
        float maxRange;
        float distanceFactor;
        float damageFactor;
        int totalDamage;
        float unitHealth = Math.max(pathing.getEntity().getTotalArmor() + pathing.getEntity().getTotalInternal(), 1);
        float threatLevel = 0.0f;

        for (int i = 0; i < enemies.size(); i++) {
            if (enemies.getRole(i).equals(unitRole)) {
                x = enemies.getX(i);
                y = enemies.getY(i);
                maxRange = enemies.getMaxWeaponRange(i);
                totalDamage = enemies.getTotalDamage(i);
                if (Coords.distance(originX, originY, x, y) <= maxRange) {
                    // Normalize damage and apply distance modifier
                    damageFactor = clamp01(totalDamage / unitHealth);
                    distanceFactor = 1f - (Coords.distance(originX, originY, x, y) / maxRange);
                    threatLevel += damageFactor * distanceFactor;
                }
            }
        }

        return threatLevel;
    }

}
