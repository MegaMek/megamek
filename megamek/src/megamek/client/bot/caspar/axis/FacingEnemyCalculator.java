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

import java.util.HashSet;
import java.util.Set;

/**
 * Calculates the facing of the unit against the 5 closest enemy units in the final position
 * @author Luana Coppio
 */
public class FacingEnemyCalculator extends BaseAxisCalculator {
    @Override
    public float[] calculateAxis(Pathing pathing, GameState gameState) {
        // This calculates if the unit is facing the enemy
        float[] facing = axis();
        int finalFacing = pathing.getFinalFacing();
        var coordsToFace = unitsThreatening(pathing, gameState.getEnemyUnitsSOU());
        if (coordsToFace.isEmpty()) {
            // should face the direction it is moving

            int direction = pathing.getStartCoords().direction(pathing.getFinalCoords());
            int deltaFacing = direction - pathing.getFinalFacing();
            facing[0] = 1.0f - normalize(deltaFacing, 0, 3);
        } else {
            Coords toFace = Coords.median(coordsToFace);

            // its never null, but the check is important, who knows what could happen?
            int desiredFacing = toFace != null ? (toFace.direction(pathing.getFinalCoords()) + 3) % 6 : 0;
            int facingDiff = getFacingDiff(finalFacing, desiredFacing);
            facing[0] = 1f - normalize(facingDiff, 0f, 3f);
        }
        return facing;
    }

    private int getFacingDiff(int currentFacing, int desiredFacing) {
        int facingDiff;

        if (currentFacing == desiredFacing) {
            facingDiff = 0;
        } else if ((currentFacing == ((desiredFacing + 1) % 6))
              || (currentFacing == ((desiredFacing + 5) % 6))) {
            facingDiff = 1;
        } else if ((currentFacing == ((desiredFacing + 2) % 6))
              || (currentFacing == ((desiredFacing + 4) % 6))) {
            facingDiff = 2;
        } else {
            facingDiff = 3;
        }
        return facingDiff;
    }

    /**
     * Counts the number of units covering the current unit
     * @param pathing The movement path to evaluate
     * @param structOfUnitArrays The struct of unit arrays to evaluate
     * @return The number of units covering the current unit
     */
    private Set<Coords> unitsThreatening(Pathing pathing, StructOfUnitArrays structOfUnitArrays) {
        int xd;
        int yd;
        int x = pathing.getFinalCoords().getX();
        int y = pathing.getFinalCoords().getY();
        int originId = pathing.getEntity().getId();
        int length = structOfUnitArrays.size();

        double dist;

        Set<Coords> enemyPositions = new HashSet<>();
        for (int i = 0; i < length; i++) {
            int id = structOfUnitArrays.getId(i);
            if (id == originId) {
                continue;
            }

            xd = structOfUnitArrays.getX(i);
            yd = structOfUnitArrays.getY(i);

            dist = Coords.distance(x, y, xd, yd);
            if (dist <= structOfUnitArrays.getMaxWeaponRange(i)) {
                enemyPositions.add(new Coords(xd, yd));
            }
        }
        return enemyPositions;
    }

}
