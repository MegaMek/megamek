/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
        Coords toFace = selectOneCoordsToFace(path, secondaryTargetPosition, enemyMedianPosition, closestEnemyPosition);
        int desiredFacing = getDesiredFacing(unit, path, toFace);
        int currentFacing = path.getFinalFacing();
        return getFacingDiff(currentFacing, desiredFacing);
    }

    /**
     * Calculates the optimal facing direction for a unit toward a target coordinate.
     * <p>
     * This method accounts for armor distribution biases that may make it advantageous to face slightly off-center from
     * the target. The bias is applied so that the stronger armor side faces the enemy.
     * </p>
     *
     * @param unit   The entity making the movement
     * @param path   The movement path being evaluated
     * @param toFace The target coordinates to face toward
     *
     * @return The desired facing direction (0-5)
     */
    private int getDesiredFacing(Entity unit, MovePath path, Coords toFace) {
        int desiredFacing = path.getFinalCoords().direction(toFace);

        // Get armor bias: positive means left side has more armor, negative means right side
        int armorBias = getArmorBias(unit);

        // Apply bias to expose the stronger side to the enemy
        // If left armor > right (armorBias > 0), we want the left side facing the enemy
        // Turning left (+1) from facing the enemy directly exposes our LEFT side to them
        // Turning right (-1) from facing the enemy directly exposes our RIGHT side to them
        desiredFacing += armorBias;
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
     *
     * @return The facing difference (0-2) (there is a built-in tolerance for 1 turn click)
     */
    private int getFacingDiff(int currentFacing, int desiredFacing) {
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
        return Math.max(0, facingDiff - allowFacingTolerance);
    }

    /**
     * Determines the armor bias for a unit based on left vs right armor distribution.
     * <p>
     * For Meks, this method compares the total armor on the left side (arm, leg, torso)
     * versus the right side and returns a bias value. This bias is used to adjust the
     * desired facing direction so the unit exposes its stronger armor side to the enemy.
     * </p>
     * <p>
     * When facing direction D toward an enemy:
     * <ul>
     *   <li>Turning left (D+1) exposes the LEFT side to the enemy</li>
     *   <li>Turning right (D-1) exposes the RIGHT side to the enemy</li>
     * </ul>
     * So if left armor is stronger, we want to turn left (+1) to expose it.
     * </p>
     *
     * @param unit The entity being evaluated
     *
     * @return +1 if left side has more armor (prefer exposing left),
     *         -1 if right side has more armor (prefer exposing right),
     *         0 if armor is equal (no preference)
     */
    private int getArmorBias(Entity unit) {
        int armorBias = 0;
        if (unit.isMek()) {
            // Calculate total armor on each side
            int leftArmor = unit.getArmor(Mek.LOC_LEFT_ARM)
                  + unit.getArmor(Mek.LOC_LEFT_LEG)
                  + unit.getArmor(Mek.LOC_LEFT_TORSO);
            int rightArmor = unit.getArmor(Mek.LOC_RIGHT_ARM)
                  + unit.getArmor(Mek.LOC_RIGHT_LEG)
                  + unit.getArmor(Mek.LOC_RIGHT_TORSO);

            // Bias toward exposing the stronger side to the enemy
            if (leftArmor > rightArmor) {
                armorBias = 1;  // Prefer exposing stronger left side
            } else if (rightArmor > leftArmor) {
                armorBias = -1; // Prefer exposing stronger right side
            }
        }
        return armorBias;
    }

}
