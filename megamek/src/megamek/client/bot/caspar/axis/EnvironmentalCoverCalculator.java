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
import megamek.client.bot.common.StructOfUnitArrays;
import megamek.common.Coords;
import megamek.common.CubeCoords;
import megamek.common.Entity;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Calculates the environmental cover of the final position against 5 enemy units that have you in sight
 * @author Luana Coppio
 */
public class EnvironmentalCoverCalculator extends BaseAxisCalculator {

    @Override
    public float[] calculateAxis(Pathing pathing, GameState gameState) {
        // This calculates the potential of the unit to act as a decoy
        float[] cover = axis();
        Entity unit = pathing.getEntity();
        if (unit.isAirborneAeroOnGroundMap()) {
            cover[0] = 1.0f;
            return cover;
        }

        int unitHeight = unit.isMek() ? 2 : 1;

        Set<Coords> enemyPositions = unitsThreateningMe(pathing, gameState.getEnemyUnitsSOU());
        float bonusCover = calculateBonusCover(
              pathing.getFinalCoords(),
              unitHeight,
              enemyPositions,
              gameState.getBoardQuickRepresentation());

        cover[0] = normalize(bonusCover, 0, 1);

        return cover;
    }


    /**
     * Counts the number of units covering the current unit
     * @param pathing The movement path to evaluate
     * @param structOfUnitArrays The struct of unit arrays to evaluate
     * @return The number of units covering the current unit
     */
    private Set<Coords> unitsThreateningMe(Pathing pathing, StructOfUnitArrays structOfUnitArrays) {
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

    private float calculateBonusCover(Coords coverPosition, int unitHeight, Set<Coords> enemyPositions,
                                       BoardQuickRepresentation boardQuickRepresentation) {
        // Check the surrounding hexes for an elevation advantage.
        int baseLevel = boardQuickRepresentation.levelAt(coverPosition);
        float bonus = getStartingCoverBonus(coverPosition, unitHeight, baseLevel, boardQuickRepresentation);
        bonus += getCoverBonusTowardsEnemies(coverPosition, unitHeight, enemyPositions, baseLevel, boardQuickRepresentation);
        return bonus;
    }

    private float getCoverBonusTowardsEnemies(Coords coverPosition, int unitHeight, Set<Coords> enemyPositions,
                                               int baseLevel, BoardQuickRepresentation boardQuickRepresentation) {

        float bonus = 0.0f;
        float maxBonusPerEnemy = 1.0f / (enemyPositions.size() + 1.0f);
        for (Coords targetPosition : enemyPositions) {
            List<Coords> between = coverPosition.toCube().lineTo(targetPosition.toCube()).stream().map(CubeCoords::toOffset).toList();
            int woodCount = 0;
            boolean hasPartialCover = false;
            for (Coords c : between) {
                if (boardQuickRepresentation.hasFullCover(c, baseLevel, unitHeight)) {
                    bonus += maxBonusPerEnemy;
                    hasPartialCover = false;
                    break;
                } else if (boardQuickRepresentation.hasPartialCover(c, baseLevel, unitHeight)) {
                    hasPartialCover = true;
                }
                if (!c.equals(coverPosition) && boardQuickRepresentation.hasWoods(c)) {
                    woodCount++;
                    if (woodCount > 1) {
                        bonus += maxBonusPerEnemy;
                        hasPartialCover = false;
                        break;
                    }
                }
            }
            if (hasPartialCover) {
                bonus += maxBonusPerEnemy / 2;
            }
        }
        return bonus;
    }

    private static float getStartingCoverBonus(Coords coverPosition, int unitHeight, int baseLevel, BoardQuickRepresentation boardQuickRepresentation) {
        float bonus = 0.0f;

        for (Coords coords : coverPosition.allAdjacent()) {
            if (boardQuickRepresentation.hasFullCover(coords, baseLevel, unitHeight)) {
                bonus += 0.01f;
            } else if (boardQuickRepresentation.hasPartialCover(coords, baseLevel, unitHeight)
                  || boardQuickRepresentation.hasWoods(coords)) {
                bonus += 0.005f;
            }
        }
        return bonus;
    }
}
