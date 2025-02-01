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
import megamek.common.Entity;

import java.util.Map;

import static megamek.codeUtilities.MathUtility.clamp01;

/**
 * This consideration is used to determine if a target is inside the optimal range
 */
@JsonTypeName("TargetWithinOptimalRange")
public class TargetWithinOptimalRange extends TWConsideration {
    public TargetWithinOptimalRange() {
    }

    @Override
    public double score(DecisionContext<Entity, Entity> context) {
        var targets = context.getTargets();
        var firingUnit = context.getCurrentUnit();
        var distance = targets.stream().map(Entity::getPosition)
                .mapToInt(coords -> firingUnit.getPosition().distance(coords)).max()
                .orElse(Integer.MAX_VALUE);;

        var maxRange = firingUnit.getMaxWeaponRange();
        var bestRange = firingUnit.getOptimalRange();

        if (distance <= bestRange) {
            return 1d;
        }

        return clamp01(1.0001d - (double) (distance - bestRange) / (maxRange - bestRange));
    }


    @Override
    public TargetWithinOptimalRange copy() {
        var copy = new TargetWithinOptimalRange();
        copy.setCurve(getCurve().copy());
        copy.setParameters(Map.copyOf(getParameters()));
        copy.setName(getName());
        return copy;
    }
}
