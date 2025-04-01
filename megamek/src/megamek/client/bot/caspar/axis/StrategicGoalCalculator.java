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

import java.util.Objects;

/**
 * Calculates the strategic goal
 * @author Luana Coppio
 */
public class StrategicGoalCalculator extends BaseAxisCalculator {
    @Override
    public float[] calculateAxis(Pathing pathing, GameState gameState) {
        // This calculates the strategic goal
        float[] strategicGoal = axis();

        var formationOpt = gameState.getFormationFor(pathing.getEntity());
        if (formationOpt.isPresent()) {
            var formation = formationOpt.get();
            var tacticalOpt = gameState.getTacticalPlanner().getAssignedObjective(formation);
            if (tacticalOpt.isPresent()) {
                var tacticalObjective = tacticalOpt.get();
                var waypoint = tacticalObjective.getPosition();
                int distFromStart = pathing.getStartCoords().distance(waypoint);
                int distFromFinalCoords = pathing.getFinalCoords().distance(waypoint);
                strategicGoal[0] = distFromStart > distFromFinalCoords ? 1 : 0;
            }
        } else {
            int selectedCoords = -1;
            int selectedCoordsFinal = -1;
            int dist;
            int distance = Integer.MAX_VALUE;
            for (var coords : gameState.getStrategicPoints()) {
                if (coords.getX() > 0 && coords.getY() > 0) {
                    dist = pathing.getStartCoords().distance(coords);
                    if (dist < distance) {
                        selectedCoords = dist;
                    }
                    dist = pathing.getFinalCoords().distance(coords);
                    if (dist < distance) {
                        selectedCoordsFinal = dist;
                    }
                }
            }
            strategicGoal[0] = selectedCoords > selectedCoordsFinal ? 1 : 0;
        }

        return strategicGoal;
    }
}
