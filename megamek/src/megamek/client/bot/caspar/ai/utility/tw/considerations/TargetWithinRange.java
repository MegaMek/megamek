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
import megamek.client.bot.caspar.ai.utility.tw.decision.TWDecisionContext;
import megamek.common.Entity;

import java.util.Map;

import static megamek.codeUtilities.MathUtility.clamp01;

/**
 * This consideration is used to determine if a target is an easy target.
 */
@JsonTypeName("TargetWithinRange")
public class TargetWithinRange extends TWConsideration {

    public TargetWithinRange() {
    }

    @Override
    public double score(DecisionContext<Entity, Entity> context) {
        var firingUnit = context.getCurrentUnit();
        var maxRange = firingUnit.getMaxWeaponRange();
        var distance = ((TWDecisionContext) context).getDistanceToClosestEnemy(firingUnit);

        if (distance.isEmpty()) {
            return 0d;
        }

        return clamp01(distance.getAsInt() / (double) maxRange);
    }

    @Override
    public TargetWithinRange copy() {
        var copy = new TargetWithinRange();
        copy.setCurve(getCurve().copy());
        copy.setParameters(Map.copyOf(getParameters()));
        copy.setName(getName());
        return copy;
    }
}
