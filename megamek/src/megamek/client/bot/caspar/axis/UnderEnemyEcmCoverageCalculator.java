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
 * Calculates if the unit is under enemy ECM coverage
 * @author Luana Coppio
 */
public class UnderEnemyEcmCoverageCalculator extends BaseAxisCalculator {

    @Override
    public float[] calculateAxis(Pathing pathing, GameState gameState) {
        // This calculates the potential of the unit to act as a decoy
        float[] ecmCoverage = axis();
        var unit = pathing.getEntity();

        int overlappingECMs = this.unitUnderECM(pathing, gameState.getEnemyUnitsSOU());
        if (unit.hasActiveECCM()) {
            overlappingECMs--;
        }
        ecmCoverage[0] = Math.min(overlappingECMs, 1);

        return ecmCoverage;
    }

    /**
     * Counts the number ECM bubbles that are overlapping with the unit
     * @param pathing The movement path to evaluate
     * @param structOfUnitArrays The struct of unit arrays to evaluate
     * @return The number of ECM bubbles this unit is under
     */
    private int unitUnderECM(Pathing pathing, StructOfUnitArrays structOfUnitArrays) {
        int xd;
        int yd;
        int x = pathing.getFinalCoords().getX();
        int y = pathing.getFinalCoords().getY();
        int length = structOfUnitArrays.size();

        double dist;
        int number_of_ecm = 0;

        for (int i = 0; i < length; i++) {
            xd = structOfUnitArrays.getX(i);
            yd = structOfUnitArrays.getY(i);

            dist = Coords.distance(x, y, xd, yd);
            if (dist <= 6 && structOfUnitArrays.hasECM(i)) {
                number_of_ecm++;
            }
        }
        return number_of_ecm;
    }
}
