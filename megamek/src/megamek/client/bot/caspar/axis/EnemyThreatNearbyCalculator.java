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
import megamek.common.Coords;

import java.util.List;

/**
 * Calculates the enemy threat in nearby hexes (37 values).
 * @author Luana Coppio
 */
public class EnemyThreatNearbyCalculator extends BaseAxisCalculator {

    private static final int NUMBER_OF_HEXES_ON_3_DIST_RADIUS = 37; // 6 * 3 + 6 * 2 + 6 + 1;

    @Override
    public double[] axis() {
        return new double[NUMBER_OF_HEXES_ON_3_DIST_RADIUS];
    }

    @Override
    public double[] calculateAxis(Pathing pathing, GameState gameState) {
        double[] threats = axis();
        List<Coords> nearbyHexes = pathing.getFinalCoords().allAtDistanceOrLess(3);

        var quickBoardRepresentation = gameState.getBoardQuickRepresentation();
        int i = 0;
        for (Coords coords : nearbyHexes) {
            threats[i++] = quickBoardRepresentation.getThreatLevel(coords);
        }

        return threats;
    }
}
