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
import megamek.common.Entity;
import megamek.common.UnitRole;

import java.util.List;
import java.util.stream.Collectors;

import static megamek.codeUtilities.MathUtility.clamp01;

/**
 * Calculates the scouting
 * @author Luana Coppio
 */
public class ScoutingCalculator extends BaseAxisCalculator {
    @Override
    public float[] calculateAxis(Pathing pathing, GameState gameState) {
        float[] scouting = axis();
        Entity unit = pathing.getEntity();
        int maxMp = unit.getRunMP();
        int hexesMoved = pathing.getHexesMoved();
        boolean isScout = UnitRole.SCOUT.equals(unit.getRole());
        boolean isFast = maxMp >= 8;

        // Base scouting score
        double scoutingScore = 0.0;
        if (isScout) {
            scoutingScore += 0.4;
        }
        if (isFast) {
            scoutingScore += 0.3;
        }

        // Movement factor: ratio of hexes moved to max movement scaled by 0.2
        double movementFactor = ((double) hexesMoved / Math.max(maxMp, 1)) * 0.2;

        // Compute average team center using friendly units on the same team
        var formation = gameState.getFormationFor(unit);
        double distanceFactor = 0.0;
        if (formation.isPresent()) {
            Coords teamCenter = formation.get().getFormationCenter();
            double averageTeamDistance = unit.getPosition().distance(teamCenter);
            distanceFactor = Math.min(averageTeamDistance / 15.0, 1.0) * 0.1;
        }

        // Total scouting score clamped between 0 and 1
        double totalScore = clamp01(scoutingScore + movementFactor + distanceFactor);
        scouting[0] = (float) totalScore;
        return scouting;
    }
}
