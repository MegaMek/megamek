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
import megamek.client.bot.caspar.ai.utility.tw.decision.TWDecisionContext;
import megamek.common.Entity;
import megamek.common.MovePath;
import megamek.common.UnitRole;

import java.util.Map;

import static megamek.codeUtilities.MathUtility.clamp01;

/**
 * This consideration is used to determine the armor percent.
 */
@JsonTypeName("FlankingPosition")
public class FlankingPosition extends TWConsideration {

    public static final String roleParam = "role";
    private static final Map<String, Class<?>> parameterTypes = Map.of(roleParam, UnitRole.class);
    private static final Map<String, ParameterTitleTooltip> parameterTooltips = Map.of(roleParam, new ParameterTitleTooltip("FavTargetUnitRole"));

    public FlankingPosition() {
    }

    @Override
    public double score(DecisionContext<Entity, Entity> context) {
//        TWDecisionContext twContext = (TWDecisionContext) context;
//        MovePath movePath = twContext.getMovePath();
//        Entity target = twContext.getPrimaryThreat(movePath.getFinalCoords()).orElse(null);
//        if (target == null) return 0;
//
//        int targetFacing = target.getFacing();
//        int attackAngle = target.getPosition().direction(
//            twContext.getMovePath().getFinalCoords());
//
//        int angleDiff = Math.abs(targetFacing - attackAngle) % 6;
//        angleDiff = Math.min(angleDiff, 6 - angleDiff);
//
//        // 0 = front, 3 = rear
//        return clamp01(angleDiff / 3.0);
        return 1;
    }

    @Override
    public FlankingPosition copy() {
        var copy = new FlankingPosition();
        copy.setCurve(getCurve().copy());
        copy.setParameters(Map.copyOf(getParameters()));
        copy.setName(getName());
        return copy;
    }
}
