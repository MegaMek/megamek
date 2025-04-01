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

import megamek.client.bot.common.BoardQuickRepresentation;
import megamek.client.bot.common.GameState;
import megamek.client.bot.common.Pathing;
import megamek.common.Coords;
import megamek.common.CubeCoords;
import megamek.common.Entity;


import java.util.List;

import static megamek.codeUtilities.MathUtility.clamp01;

/**
 * Calculates if the unit is in a flanking position relative to enemy units.
 * A flanking position is one where the unit can attack an enemy from the side or rear,
 * or is positioned to move into such a position in the next turn.
 *
 * @author Luana Coppio
 */
public class FlankingPositionCalculator extends BaseAxisCalculator {

    // Constants for scoring
    private static final float SIDE_ATTACK_SCORE = 0.75f;
    private static final float REAR_ATTACK_SCORE = 1.0f;
    private static final float FRONT_ATTACK_SCORE = 0.2f;

    // Distance thresholds
    private static final int MAX_FLANKING_DISTANCE = 12; // Max distance to consider for flanking
    private static final int OPTIMAL_FLANKING_DISTANCE = 7; // Optimal distance for flanking
    private static final int TOO_CLOSE_TO_FLANK = 3; // Optimal distance for flanking

    @Override
    public float[] calculateAxis(Pathing pathing, GameState gameState) {
        // This calculates if the unit is in a flanking position
        float[] flanking = axis();
        Entity unit = pathing.getEntity();

        // Get the unit's planned final position and facing
        Coords finalPosition = pathing.getFinalCoords();
        int finalFacing = pathing.getFinalFacing();

        // Get visible enemies
        List<Entity> visibleEnemies = gameState.getEnemyUnits();

        // Calculate the best flanking score based on position relative to enemies
        float bestFlankingScore = calculateBestFlankingScore(unit, finalPosition, finalFacing, visibleEnemies, gameState);

        flanking[0] = bestFlankingScore;
        return flanking;
    }

    /**
     * Calculates the best flanking score against any visible enemy.
     *
     * @param unit The unit to evaluate
     * @param unitPosition The unit's position
     * @param unitFacing The unit's facing
     * @param visibleEnemies List of enemies
     * @param gameState The current game state
     * @return Flanking score between 0.0 and 1.0
     */
    private float calculateBestFlankingScore(Entity unit, Coords unitPosition, int unitFacing,
                                             List<Entity> visibleEnemies, GameState gameState) {

        // If no visible enemies, no flanking possible
        if (visibleEnemies.isEmpty()) {
            return 0.0f;
        }

        // Check each visible enemy for best flanking opportunity
        float bestScore = 0.0f;
        for (Entity enemy : visibleEnemies) {
            float flankingScore = calculateFlankingScore(unit, unitPosition, unitFacing, enemy, gameState);

            // Keep track of the best flanking score
            bestScore = Math.max(bestScore, flankingScore);
        }

        return bestScore;
    }

    /**
     * Calculates the flanking score against a specific enemy entity.
     *
     * @param unit The unit to evaluate
     * @param unitPosition The unit's position
     * @param unitFacing The unit's facing
     * @param enemy The enemy entity
     * @param gameState The current game state
     * @return Flanking score between 0.0 and 1.0
     */
    private float calculateFlankingScore(Entity unit, Coords unitPosition, int unitFacing,
                                          Entity enemy, GameState gameState) {

        Coords enemyPosition = enemy.getPosition();
        int enemyFacing = enemy.getFacing();

        // Skip if enemy is too far away to consider for flanking
        int distance = unitPosition.distance(enemyPosition);
        if (distance > MAX_FLANKING_DISTANCE) {
            return 0f;
        }

        // 1. Calculate distance factor - optimal flanking distance is about 5 hexes
        float distanceFactor = calculateDistanceFactor(distance);

        // 2. Calculate flank angle - determines if attacking from side, rear, or front
        float angleScore = calculateAngleScore(unitPosition, enemyPosition, enemyFacing);

        // 3. Calculate line of fire - check there is no full cover blocking the line of fire
        float lineOfFireFactor = calculateLineOfFireFactor(unitPosition, enemyPosition, gameState);

        // 4. Calculate support - check if other friendly units are also flanking
        float supportFactor = calculateSupportFactor(unit, enemy, gameState);

        // 5. Calculate exposure - check if the flanking position exposes the unit to other enemies
        float exposureFactor = calculateExposureFactor(unitPosition, unitFacing, enemy, gameState);

        // Combine factors with weights
        return (0.3f * angleScore) + (0.25f * distanceFactor) + (0.2f * lineOfFireFactor) +
              (0.15f * supportFactor) + (0.1f * exposureFactor);
    }

    /**
     * Calculates the optimal distance factor for flanking.
     *
     * @param distance Distance to the enemy
     * @return Distance factor between 0.0 and 1.0
     */
    private float calculateDistanceFactor(int distance) {
        // Too close is dangerous, too far is ineffective
        if (distance < TOO_CLOSE_TO_FLANK) {
            return 0.3f; // Too close to maneuver effectively
        } else if (distance <= OPTIMAL_FLANKING_DISTANCE) {
            // Scale from 0.5 at TOO_CLOSE_TO_FLANK to 1.0 at optimal distance
            return 0.5f + (0.5f * (distance - TOO_CLOSE_TO_FLANK) / (float)( OPTIMAL_FLANKING_DISTANCE - TOO_CLOSE_TO_FLANK));
        } else if (distance <= MAX_FLANKING_DISTANCE) {
            // Scale from 1.0 at optimal distance to 0.0 at max distance
            return 1.0f - ((distance - OPTIMAL_FLANKING_DISTANCE) / (float) (MAX_FLANKING_DISTANCE - OPTIMAL_FLANKING_DISTANCE));
        } else {
            return 0.0f; // Too far to be effective
        }
    }

    /**
     * Calculates the angle score based on the position relative to the enemy's facing.
     * Higher scores for side and rear positions, lower for frontal approach.
     *
     * @param unitPosition The unit's position
     * @param enemyPosition The enemy's position
     * @param enemyFacing The enemy's facing
     * @return Angle score between 0.0 and 1.0
     */
    private float calculateAngleScore(Coords unitPosition, Coords enemyPosition, int enemyFacing) {
        // Calculate the direction from enemy to unit
        int directionToUnit = enemyPosition.direction(unitPosition);

        // Calculate the angle difference
        int angleDiff = Math.abs(directionToUnit - enemyFacing);
        if (angleDiff > 3) {
            angleDiff = 6 - angleDiff; // Handle wrap-around
        }

        // Convert angle to attack zone (front, side, rear)
        if (angleDiff <= 1) {
            // Front arc (60° front arc)
            return FRONT_ATTACK_SCORE;
        } else if (angleDiff == 3) {
            // Rear arc (60° rear arc)
            return REAR_ATTACK_SCORE;
        } else {
            // Side arcs (60° on each side)
            return SIDE_ATTACK_SCORE;
        }
    }

    /**
     * Checks if there are friendly units blocking the line of fire to the enemy.
     *
     * @param unitPosition The unit's position
     * @param enemyPosition The enemy's position
     * @param gameState The current game state
     * @return Line of fire factor between 0.0 and 1.0
     */
    private float calculateLineOfFireFactor(Coords unitPosition, Coords enemyPosition, GameState gameState) {
        // Get all hexes between unit and enemy
        List<Coords> intervening = unitPosition.toCube().lineTo(enemyPosition.toCube()).stream().map(CubeCoords::toOffset).toList();
        // List<Coords> intervening = Coords.intervening(unitPosition, enemyPosition);

        BoardQuickRepresentation boardQuickRepresentation = gameState.getBoardQuickRepresentation();
        // Check if any of those hexes contain friendly units
        int woods = 0;
        int level;
        for (Coords coord : intervening) {
            level = boardQuickRepresentation.levelDifference(unitPosition, coord);
            if (level >= 2) {
                return 0;
            }
            if (boardQuickRepresentation.hasWoods(coord)) {
                woods++;
            }
            if (woods >= 2) {
                return 0;
            }
        }

        return 1.0f;
    }

    /**
     * Checks if there are other friendly units also in flanking positions against this enemy.
     * Coordinated flanking is more effective than solo flanking.
     *
     * @param unit The unit to evaluate
     * @param enemy The enemy entity
     * @param gameState The current game state
     * @return Support factor between 0.0 and 1.0
     */
    private float calculateSupportFactor(Entity unit, Entity enemy, GameState gameState) {
        Coords enemyPosition = enemy.getPosition();
        int enemyFacing = enemy.getFacing();

        // Get all friendly units
        List<Entity> friendlyUnits = gameState.getMyTeamUnits();
        // Count friendly units in flanking positions
        int flankingSupportCount = 0;

        for (Entity friendly : friendlyUnits) {
            // Skip self and units too far away
            if (friendly.getId() == unit.getId() ||
                  friendly.getPosition().distance(enemyPosition) > MAX_FLANKING_DISTANCE) {
                continue;
            }

            // Check if the friendly unit is in a flanking position
            int directionToFriendly = enemyPosition.direction(friendly.getPosition());
            int angleDiff = Math.abs(directionToFriendly - enemyFacing);
            if (angleDiff > 3) {
                angleDiff = 6 - angleDiff; // Handle wrap-around
            }

            // Count units in side or rear positions
            if (angleDiff >= 2) {
                flankingSupportCount++;
            }
        }

        // Calculate support factor (cap at 3 supporting units)
        return flankingSupportCount / 3.0f;
    }

    /**
     * Checks if the flanking position exposes the unit to other enemies.
     *
     * @param unitPosition The unit's position
     * @param unitFacing The unit's facing
     * @param targetEnemy The target enemy entity
     * @param gameState The current game state
     * @return Exposure factor between 0.0 and 1.0
     */
    private float calculateExposureFactor(Coords unitPosition, int unitFacing,
                                           Entity targetEnemy, GameState gameState) {
        List<Entity> visibleEnemies = gameState.getEnemyUnits();

        // Calculate exposures from each enemy other than the target
        int exposedToCount = 0;
        int dangerousExposureCount = 0;

        for (Entity otherEnemy : visibleEnemies) {
            // Skip non-entities and the target enemy
            if (otherEnemy.getId() == targetEnemy.getId()) {
                continue;
            }

            Coords otherEnemyPos = otherEnemy.getPosition();

            // Check if within threatening range
            int distance = unitPosition.distance(otherEnemyPos);
            if (distance <= otherEnemy.getMaxWeaponRange()) {
                exposedToCount++;

                // Check if the other enemy can attack our rear
                int directionToUnit = otherEnemyPos.direction(unitPosition);
                int angleDiff = Math.abs(directionToUnit - unitFacing);
                if (angleDiff > 3) {
                    angleDiff = 6 - angleDiff; // Handle wrap-around
                }

                // Count enemies that can target our rear
                if (angleDiff == 3 && distance <= 9) {
                    dangerousExposureCount++;
                }
            }
        }

        // Calculate exposure factor
        float exposureFactor;

        if (dangerousExposureCount > 0) {
            // Severe penalty for exposing rear to enemies
            exposureFactor = 0.1f;
        } else if (exposedToCount > 3) {
            // Exposed to too many enemies
            exposureFactor = 0.3f;
        } else if (exposedToCount > 0) {
            // Some exposure, but manageable
            exposureFactor = 0.7f;
        } else {
            // No additional exposure
            exposureFactor = 1.0f;
        }

        return exposureFactor;
    }
}
