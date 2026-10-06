/*
 * Copyright (C) 2025-2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
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
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package megamek.client.bot.princess;

import megamek.common.annotations.Nullable;
import megamek.common.board.Coords;
import megamek.common.moves.MovePath;
import megamek.common.units.Entity;
import megamek.common.units.Mek;

/**
 * This class calculates the difference between a unit's current facing and its optimal facing.
 * <p>
 * Facing calculations, consider:
 * <ul>
 *   <li>Enemy positions (closest enemy, median enemy position)</li>
 *   <li>Unit armor distribution (for meks)</li>
 *   <li>Secondary targets when no enemies are present</li>
 * </ul>
 * <p>
 * The facing difference is measured in the number of hex sides (0-3) that would need to be rotated to achieve the
 * optimal facing. This information is used by bot pathfinding algorithms to evaluate movement options.
 *
 * @author Luana Coppio
 * @since 0.50.06
 */
record FacingDiffCalculator(int allowFacingTolerance) {

    /**
     * Calculates the difference between a unit's current facing and its optimal facing.
     * <p>
     * This method determines which direction the unit should face based on enemy positions and unit characteristics,
     * then calculates how many rotations would be needed from the current facing to achieve the optimal facing.
     * </p>
     *
     * @param unit                    The entity making the movement
     * @param path                    The movement path being evaluated
     * @param secondaryTargetPosition Fallback position to face if no enemies are present (typically board center)
     * @param enemyMedianPosition     The median position of nearby enemies (maybe null if no enemies)
     * @param closestEnemyPosition    The position of the closest enemy (maybe null if no enemies)
     *
     * @return The facing difference, from 0 (optimal) to 3 (opposite direction)
     */
    int getFacingDiff(final Entity unit, final MovePath path, final Coords secondaryTargetPosition,
          @Nullable final Coords enemyMedianPosition, @Nullable final Coords closestEnemyPosition) {
        return getFacingDiff(unit, path, secondaryTargetPosition, enemyMedianPosition, closestEnemyPosition, false);
    }

    /**
     * As {@link #getFacingDiff(Entity, MovePath, Coords, Coords, Coords)}, but with the option to square up
     * precisely on the closest enemy. A unit closing to melee must end the turn facing its target - a hatchet,
     * sword or kick cannot be delivered through a torso twist - so when {@code squareUpOnClosestEnemy} is
     * {@code true} the unit faces the closest enemy directly, with no armor-angling bias and no facing
     * tolerance, instead of settling for any facing within a hex side of the enemy cluster (issue #7627).
     *
     * @param unit                    The entity making the movement
     * @param path                    The movement path being evaluated
     * @param secondaryTargetPosition Fallback position to face if no enemies are present (typically board center)
     * @param enemyMedianPosition     The median position of nearby enemies (maybe {@code null} if no enemies)
     * @param closestEnemyPosition    The position of the closest enemy (maybe {@code null} if no enemies)
     * @param squareUpOnClosestEnemy  {@code true} to face the closest enemy exactly (for a unit closing to melee)
     *
     * @return The facing difference, from 0 (optimal) to 3 (opposite direction)
     */
    int getFacingDiff(final Entity unit, final MovePath path, final Coords secondaryTargetPosition,
          @Nullable final Coords enemyMedianPosition, @Nullable final Coords closestEnemyPosition,
          final boolean squareUpOnClosestEnemy) {
        Coords toFace;
        int bias;
        int tolerance;
        if (squareUpOnClosestEnemy && (closestEnemyPosition != null)) {
            toFace = closestEnemyPosition;
            bias = 0;
            tolerance = 0;
        } else {
            toFace = selectOneCoordsToFace(path, secondaryTargetPosition, enemyMedianPosition, closestEnemyPosition);
            bias = getBiasTowardsFacing(unit);
            tolerance = allowFacingTolerance;
        }
        int desiredFacing = getDesiredFacing(path, toFace, bias);
        int currentFacing = path.getFinalFacing();
        return getFacingDiff(currentFacing, desiredFacing, tolerance);
    }

    /**
     * Calculates the optimal facing direction for a unit toward a target coordinate.
     * <p>
     * This method accounts for armor distribution biases that may make it advantageous to face slightly off-center from
     * the target.
     * </p>
     *
     * @param path The movement path being evaluated
     * @param toFace The target coordinates to face toward
     * @param bias The armor-angling bias to apply ({@code -1} right, {@code 1} left, {@code 0} none)
     *
     * @return The desired facing direction (0-5)
     */
    private int getDesiredFacing(MovePath path, Coords toFace, int bias) {
        int desiredFacing = path.getFinalCoords().direction(toFace);

        // -1 is bias towards facing left, 1 is bias towards facing right
        desiredFacing += bias;
        if (desiredFacing < 0) {
            desiredFacing += 6;
        }
        desiredFacing %= 6;
        return desiredFacing;
    }

    /**
     * Selects which coordinates the unit should face toward.
     * <p>
     * Priority is given to:
     * <ol>
     *   <li>Adjacent enemies (if the path ends adjacent to the closest enemy)</li>
     *   <li>Median enemy position (if enemies are present but not adjacent)</li>
     *   <li>Secondary target position (if no enemies are present)</li>
     * </ol>
     *
     * @param path                 The movement path being evaluated
     * @param secondaryPosition    Fallback position if no enemies are present
     * @param enemyMedianPosition  The median position of nearby enemies
     * @param closestEnemyPosition The position of the closest enemy
     *
     * @return The coordinates the unit should face toward
     */
    private static Coords selectOneCoordsToFace(MovePath path, Coords secondaryPosition, Coords enemyMedianPosition,
          Coords closestEnemyPosition) {
        Coords toFace;
        if ((closestEnemyPosition != null) && closestEnemyPosition.distance(path.getFinalCoords()) == 1) {
            // The closestEnemyPosition is the position of the closest enemy, if we are moving in a direction of it and
            // landing beside it (reason why dist == 1), we want to face it.
            toFace = closestEnemyPosition;
        } else if (enemyMedianPosition != null) {
            // Otherwise, we face the median position of the closest units
            toFace = enemyMedianPosition;
        } else {
            // If we don't have any enemies, we want to face the secondary position, which usually means the center of
            // the board
            toFace = secondaryPosition;
        }
        return toFace;
    }

    /**
     * Calculates the difference between two facing directions.
     * <p>
     * Returns the minimum number of rotations needed to change from the current facing to the desired facing. The
     * result is between 0 (same direction) and 3 (opposite direction), but since there is a tolerance of 1, this means
     * that the final facing diff can only be 2.
     *
     * @param currentFacing The unit's current facing (0-5)
     * @param desiredFacing The desired facing direction (0-5)
     * @param tolerance     The number of hex sides of deviation forgiven with no penalty
     *
     * @return The facing difference after tolerance (0 to 3)
     */
    private int getFacingDiff(int currentFacing, int desiredFacing, int tolerance) {
        int facingDiff;
        if (currentFacing == desiredFacing) {
            facingDiff = 0;
        } else if ((currentFacing == ((desiredFacing + 1) % 6)) || (currentFacing == ((desiredFacing + 5) % 6))) {
            facingDiff = 1;
        } else if ((currentFacing == ((desiredFacing + 2) % 6)) || (currentFacing == ((desiredFacing + 4) % 6))) {
            facingDiff = 2;
        } else {
            facingDiff = 3;
        }
        return Math.max(0, facingDiff - tolerance);
    }

    /**
     * Determines if a unit should bias its facing direction based on armor distribution.
     * <p>
     * For Meks, this method checks the armor on the left versus right side and returns a bias value to encourage facing
     * the more heavily armored side toward the enemy.
     * </p>
     *
     * @param unit The entity being evaluated
     *
     * @return -1 for right-side bias, 1 for left-side bias, 0 for no bias
     */
    private int getBiasTowardsFacing(Entity unit) {
        int biasTowardsFacing = 0;
        if (unit.isMek()) {
            // if we're a mek, we want to face the enemy towards the side that has more armor left
            int leftArmor = unit.getArmor(Mek.LOC_LEFT_ARM)
                  + unit.getArmor(Mek.LOC_LEFT_LEG)
                  + unit.getArmor(Mek.LOC_LEFT_TORSO);
            int rightArmor = unit.getArmor(Mek.LOC_RIGHT_ARM)
                  + unit.getArmor(Mek.LOC_RIGHT_LEG)
                  + unit.getArmor(Mek.LOC_RIGHT_TORSO);
            if (leftArmor > rightArmor) {
                biasTowardsFacing = 1;
            } else if (rightArmor > leftArmor) {
                biasTowardsFacing = -1;
            }
        }
        return biasTowardsFacing;
    }

}
