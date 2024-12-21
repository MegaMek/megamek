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

package megamek.client.bot.duchess.considerations;

import megamek.ai.utility.Consideration;
import megamek.ai.utility.Curve;
import megamek.ai.utility.DecisionContext;

import static megamek.codeUtilities.MathUtility.clamp01;

/**
 * This consideration is used to determine if a target is an easy target.
 */
public class TargetWithinRange extends Consideration {


    protected TargetWithinRange(Curve curve) {
        super(curve);
    }

    @Override
    public double score(DecisionContext context) {
        var target = context.getTarget();
        var firingUnit = context.getFiringUnit();

        if (target.isEmpty() || firingUnit.isEmpty()) {
            return 0d;
        }

        var targetEntity = target.get();
        var firingEntity = firingUnit.get();

        if (!firingEntity.hasFiringSolutionFor(targetEntity.getId())) {
            return 0d;
        }

        var distance = firingEntity.getPosition().distance(targetEntity.getPosition());
        var maxRange = firingEntity.getMaxWeaponRange();
        return clamp01(1.00001d - (double) distance / maxRange);
    }
}
