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
import megamek.client.bot.common.formation.Formation;
import megamek.common.Coords;
import megamek.common.Entity;

import java.util.Optional;

import static megamek.codeUtilities.MathUtility.clamp01;


/**
 * Calculates the formation cohesion of the unit based on boids principles.
 * This evaluates how well a unit stays with its formation by measuring its
 * position relative to the formation center and expected position.
 *
 * @author Luana Coppio
 */
public class FormationCohesionCalculator extends BaseAxisCalculator {

    // Weight factors for different aspects of cohesion
    private static final double DISTANCE_WEIGHT = 0.6;
    private static final double SPEED_WEIGHT = 0.4;

    @Override
    public float[] calculateAxis(Pathing pathing, GameState gameState) {
        // This calculates the formation cohesion of the unit
        float[] formationCohesion = axis();
        Entity unit = pathing.getEntity();

        // Get the future position of the unit being evaluated
        Coords futurePosition = pathing.getFinalCoords();

        Optional<Formation> formationOpt = gameState.getFormationFor(unit);
        if (formationOpt.isPresent()) {
            Formation formation = formationOpt.get();
            double distanceFactor = calculateDistanceFactor(formation, futurePosition);
            double speedFactor = calculateSpeedFactor(formation, pathing);
            formationCohesion[0] = (float) ((DISTANCE_WEIGHT * distanceFactor) + (SPEED_WEIGHT * speedFactor));
        }

        return formationCohesion;
    }

    /**
     * Calculates how well the unit's future position stays within the formation's
     * expected distance based on formation type.
     *
     * @param formation The formation the unit belongs to
     * @param futurePosition The future position of the unit
     * @return Distance factor between 0.0 (poor) and 1.0 (excellent)
     */
    private float calculateDistanceFactor(Formation formation, Coords futurePosition) {
        Coords formationCenter = formation.getFormationCenter();
        int distance = formationCenter.distance(futurePosition);

        // Get maximum acceptable distance based on formation type
        int maxDistance = formation.getFormationType().getMaxDistance();

        // Calculate normalized distance factor (1.0 when at center, decreasing as it moves away)
        return Math.max(0.0f, 1.0f - ((float) distance / maxDistance));
    }

    /**
     * Calculates how well the unit's movement speed matches the formation's speed.
     *
     * @param formation The formation the unit belongs to
     * @param pathing The pathing information for the unit
     * @return Speed factor between 0.0 (poor) and 1.0 (excellent)
     */
    private float calculateSpeedFactor(Formation formation, Pathing pathing) {
        Entity unit = pathing.getEntity();
        int formationSpeed = formation.getFormationSpeed();

        // Calculate how much distance the unit is covering relative to formation speed
        Coords startPosition = unit.getPosition();
        Coords endPosition = pathing.getFinalCoords();
        int distanceCovered = startPosition.distance(endPosition);

        // If unit is not moving when formation is, or vice versa, reduce factor
        if ((distanceCovered == 0 && formationSpeed > 0) ||
              (distanceCovered > 0 && formationSpeed == 0)) {
            return 0.2f; // Significant penalty for not matching formation movement state
        }

        // If both are stationary, that's fine
        if (distanceCovered == 0 && formationSpeed == 0) {
            return 1.0f;
        }

        // Calculate how close the unit's movement is to the formation's speed
        return Math.min(distanceCovered, formationSpeed) /
              (float) Math.max(distanceCovered, formationSpeed);
    }
}
