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
import megamek.client.bot.princess.UnitBehavior;
import megamek.common.Entity;

import java.util.Map;

import static megamek.codeUtilities.MathUtility.clamp01;

/**
 * This consideration is used to determine if the unit should retreat
 */
@JsonTypeName("Retreat")
public class Retreat extends TWConsideration {

    public Retreat() {
    }

    @Override
    public double score(DecisionContext context) {
        UnitBehavior.BehaviorType behaviorType = context.getBehaviorType();
        if (behaviorType == UnitBehavior.BehaviorType.ForcedWithdrawal || behaviorType == UnitBehavior.BehaviorType.MoveToDestination) {
            int newDistanceToHome = context.getDistanceToHome();
            return clamp01(1 - (newDistanceToHome / 10.0));
        }
        return 0.0;
    }

    @Override
    public Retreat copy() {
        var copy = new Retreat();
        copy.setCurve(getCurve().copy());
        copy.setParameters(Map.copyOf(getParameters()));
        copy.setName(getName());
        return copy;
    }
}
