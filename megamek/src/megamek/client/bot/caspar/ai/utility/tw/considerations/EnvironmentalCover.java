/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 */

package megamek.client.bot.caspar.ai.utility.tw.considerations;

import com.fasterxml.jackson.annotation.JsonTypeName;
import megamek.ai.utility.DecisionContext;
import megamek.ai.utility.ParameterTitleTooltip;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.Targetable;

import java.util.List;
import java.util.Map;

import static megamek.codeUtilities.MathUtility.clamp01;

/**
 * Calculates the amount of cover the final position will give the unit against the closest N enemies
 */
@JsonTypeName("EnvironmentalCover")
public class EnvironmentalCover extends TWConsideration {

    public static final String coverAgainstHowMany = "Cover against how many";
    private static final Map<String, Class<?>> parameterTypes = Map.of(coverAgainstHowMany, int.class);
    private static final Map<String, ParameterTitleTooltip> parameterTooltips = Map.of(coverAgainstHowMany, new ParameterTitleTooltip("CoverAgainstHowMany"));

    public EnvironmentalCover() {
        parameters.put(coverAgainstHowMany, 3);
    }

    @Override
    public Map<String, Class<?>> getParameterTypes() {
        return parameterTypes;
    }

    @Override
    public Map<String, ParameterTitleTooltip> getParameterTooltips() {
        return parameterTooltips;
    }

    @Override
    public double score(DecisionContext context) {

        Coords coverPosition = context.getFinalPosition();
        var unit = context.getCurrentUnit();

        int unitHeight = unit.isAirborneAeroOnGroundMap() ? Integer.MAX_VALUE : unit.isMek() ? 2 : 1;

        if (unitHeight == Integer.MAX_VALUE) {
            return 1.0;
        }

        List<Targetable> enemies = context.getEnemiesWithinRange(coverPosition, Integer.MAX_VALUE);
        List<Coords> enemyPositions = enemies.stream().map(Targetable::getPosition).limit(getIntParameter(coverAgainstHowMany)).toList();

        double bonus = getBonus(context, coverPosition, unitHeight, enemyPositions);
        return clamp01(bonus);
    }

    private double getBonus(DecisionContext context, Coords coverPosition, int unitHeight, List<Coords> enemyPositions) {
        // Check the surrounding hexes for an elevation advantage.
        int baseLevel = context.getQuickBoardRepresentation().levelAt(coverPosition);
        double bonus = getStartingCoverBonus(context, coverPosition, unitHeight, baseLevel);
        bonus += getCoverBonusTowardsEnemies(context, coverPosition, unitHeight, enemyPositions, baseLevel);
        return bonus;
    }

    private double getCoverBonusTowardsEnemies(DecisionContext context, Coords coverPosition, int unitHeight, List<Coords> enemyPositions, int baseLevel) {
        double bonus = 0.0;
        double maxBonusPerEnemy = (1.0 - 0.18) / getIntParameter(coverAgainstHowMany);
        for (Coords targetPosition : enemyPositions) {
            List<Coords> between = Coords.line(coverPosition, targetPosition);
            int woodCount = 0;
            boolean hasPartialCover = false;
            for (Coords c : between) {
                if (context.getQuickBoardRepresentation().hasFullCover(c, baseLevel, unitHeight)) {
                    bonus += maxBonusPerEnemy;
                    hasPartialCover = false;
                    break;
                } else if (context.getQuickBoardRepresentation().hasPartialCover(c, baseLevel, unitHeight)) {
                    hasPartialCover = true;
                }
                if (!c.equals(coverPosition) && context.getQuickBoardRepresentation().hasWoods(c)) {
                    woodCount++;
                    if (woodCount > 1) {
                        bonus += maxBonusPerEnemy;
                        hasPartialCover = false;
                        break;
                    }
                }
            }
            if (hasPartialCover) {
                bonus += maxBonusPerEnemy / 4;
            }
        }
        return bonus;
    }

    private static double getStartingCoverBonus(DecisionContext context, Coords coverPosition, int unitHeight, int baseLevel) {
        double bonus = 0.0;
        // max of 0.18
        for (Coords c : coverPosition.allAtDistanceOrLess(2)) {
            if (c.equals(coverPosition)) {
                continue;
            }
            if (context.getQuickBoardRepresentation().hasFullCover(c, baseLevel, unitHeight)) {
                bonus += 0.01;
            } else if (context.getQuickBoardRepresentation().hasPartialCover(c, baseLevel, unitHeight)) {
                bonus += 0.005;
            }
        }
        return bonus;
    }

    @Override
    public EnvironmentalCover copy() {
        var copy = new EnvironmentalCover();
        copy.setCurve(getCurve().copy());
        copy.setParameters(Map.copyOf(getParameters()));
        copy.setName(getName());
        return copy;
    }
}
