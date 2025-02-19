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
import megamek.ai.optimizer.UtilityPathRankerCostFunction;
import megamek.ai.utility.DecisionContext;
import megamek.ai.utility.ParameterTitleTooltip;
import megamek.client.bot.caspar.ai.utility.tw.decision.TWDecisionContext;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.UnitRole;

import java.util.Map;

import static megamek.codeUtilities.MathUtility.clamp01;

/**
 * This consideration is used to determine friendly artillery fire risk
 */
@JsonTypeName("StrategicGoal")
public class StrategicGoal extends TWConsideration {

    public StrategicGoal() {
    }

    @Override
    public double score(DecisionContext context) {
        double maxGoalUtility = 0.0;
        for (Coords goal : context.getStrategicGoalsOnCoordsQuadrant(context.getFinalPosition())) {
            double distance = context.getFinalPosition().distance(goal);
            double utility = (10.0 / (distance + 1.0));
            maxGoalUtility = Math.max(maxGoalUtility, utility);
        }
        return clamp01(maxGoalUtility);
    }

    @Override
    public StrategicGoal copy() {
        var copy = new StrategicGoal();
        copy.setCurve(getCurve().copy());
        copy.setParameters(Map.copyOf(getParameters()));
        copy.setName(getName());
        return copy;
    }
}
