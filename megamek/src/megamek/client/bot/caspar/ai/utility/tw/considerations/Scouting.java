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
import megamek.common.UnitRole;

import java.util.List;
import java.util.Map;

import static megamek.codeUtilities.MathUtility.clamp01;

/**
 * This consideration is used to determine the threat level of this field
 */
@JsonTypeName("Scouting")
public class Scouting extends TWConsideration {

    public static final String roleParam = "role";
    private static final Map<String, Class<?>> parameterTypes = Map.of(roleParam, UnitRole.class);
    private static final Map<String, ParameterTitleTooltip> parameterTooltips = Map.of(roleParam, new ParameterTitleTooltip("FavTargetUnitRole"));

    public Scouting() {
        parameters.put(roleParam, UnitRole.SCOUT);
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
        var currentUnit = context.getCurrentUnit();
        if (currentUnit.getRole().equals(getParameter(roleParam, UnitRole.class))) {
            return calculateFlankingPosition(context.getMaxWeaponRange(), context.getFinalPosition(),
                context.getStrategicGoalsOnCoordsQuadrant(context.getFinalPosition()));
        }
        return 0;
    }

    private double calculateFlankingPosition(int maxRange, Coords unitPos, List<Coords> strategicGoals) {
        if (strategicGoals.isEmpty()) {
            return 0;
        }
        double sumX = 0, sumY = 0;
        for (Coords goal : strategicGoals) {
            sumX += goal.getX();
            sumY += goal.getY();
        }
        int avgX = (int) Math.round(sumX / strategicGoals.size());
        int avgY = (int) Math.round(sumY / strategicGoals.size());
        Coords avgGoal = new Coords(avgX, avgY);
        double distance = unitPos.distance(avgGoal);
        return clamp01(distance / (maxRange + 1));
    }

    @Override
    public Scouting copy() {
        var copy = new Scouting();
        copy.setCurve(getCurve().copy());
        copy.setParameters(Map.copyOf(getParameters()));
        copy.setName(getName());
        return copy;
    }
}
