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
import megamek.common.actions.ArtilleryAttackAction;

import java.util.Enumeration;

/**
 * Calculates the friendly artillery fire potential
 * @author Luana Coppio
 */
public class FriendlyArtilleryFireCalculator extends BaseAxisCalculator {
    @Override
    public float[] calculateAxis(Pathing pathing, GameState gameState) {
        // This calculates the friendly artillery fire potential
        float[] artilleryFire = axis();
        float maxRange = pathing.getEntity().getMaxWeaponRange();
        float artillerySupport = getArtillerySupport(pathing, gameState.getFriendlyUnitsSOU());
        artillerySupport = Math.max(artillerySupport, getArtillerySupport(pathing, gameState.getOwnUnitsSOU()));

        artilleryFire[0] = artillerySupport / (maxRange + 1f);
        return artilleryFire;
    }

    private static int getArtillerySupport(Pathing pathing, StructOfUnitArrays units) {
        int originX = pathing.getFinalCoords().getX();
        int originY = pathing.getFinalCoords().getY();
        int x;
        int y;
        int maxRange;
        int distance;
        int support = 0;
        for (int i = 0; i < units.size(); i++) {
            maxRange = units.getMaxWeaponRange(i);
            if (maxRange >= 22) {
                x = units.getX(i);
                y = units.getY(i);
                distance = Coords.distance(originX, originY, x, y);
                if (distance <= 17) {
                    support = Math.max(support, maxRange - distance);
                }
            }
        }
        return support;
    }
}
