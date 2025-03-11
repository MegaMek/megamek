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
import java.util.Set;

import static megamek.codeUtilities.MathUtility.clamp01;

/**
 * Calculates the formation separation of the unit
 * @author Luana Coppio
 */
public class FormationSeparationCalculator extends BaseAxisCalculator {

    @Override
    public double[] calculateAxis(Pathing pathing, GameState gameState) {
        // This calculates the formation separation of the unit
        double[] formationSeparation = axis();
        Entity unit = pathing.getEntity();

        // Get the future position of the unit being evaluated
        Coords futurePosition = pathing.getFinalCoords();

        Optional<Formation> formationOpt = gameState.getFormationFor(unit);
        if (formationOpt.isPresent()) {
            Formation formation = formationOpt.get();
            Set<Entity> formationMembers = formation.getMembers();

            // Calculate separation factor - how well the unit avoids being too close to others
            double separationFactor = calculateSeparationFactor(
                  formation.getFormationType(),
                  futurePosition,
                  formationMembers,
                  unit);

            // Set the separation value
            formationSeparation[0] = separationFactor;
        }

        return formationSeparation;
    }

    /**
     * Calculates how well the unit maintains appropriate separation from other units.
     *
     * @param formationType The type of formation
     * @param futurePosition The planned future position of the unit
     * @param formationMembers All units in the formation
     * @param currentUnit The unit being evaluated
     * @return Separation factor between 0.0 (poor) and 1.0 (excellent)
     */
    private double calculateSeparationFactor(
          Formation.FormationType formationType,
          Coords futurePosition,
          Set<Entity> formationMembers,
          Entity currentUnit) {

        int minDistance = formationType.getMinDistance();
        double closestDistance = Double.MAX_VALUE;

        // Check distance to each other unit in the formation
        for (Entity otherUnit : formationMembers) {
            // Skip comparing to self
            if (otherUnit.getId() == currentUnit.getId()) {
                continue;
            }

            // Use current positions of other units since we don't know their future positions
            Coords otherPosition = otherUnit.getPosition();
            int distance = futurePosition.distance(otherPosition);

            // Track the closest unit
            if (distance < closestDistance) {
                closestDistance = distance;
            }
        }

        // If there are no other units in the formation
        if (closestDistance == Double.MAX_VALUE) {
            return 1.0;
        }

        // Calculate separation factor based on minimum distance
        if (closestDistance < minDistance) {
            // Too close - apply penalty based on how close
            return clamp01(closestDistance / (double) minDistance);
        } else {
            // Sufficient separation
            return 1.0;
        }
    }

}
