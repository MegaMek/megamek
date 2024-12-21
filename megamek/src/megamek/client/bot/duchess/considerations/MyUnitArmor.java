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
public class MyUnitArmor extends Consideration {


    protected MyUnitArmor(Curve curve) {
        super(curve);
    }

    @Override
    public double score(DecisionContext context) {
        var currentUnit = context.getCurrentUnit();
        if (currentUnit.isEmpty()) {
            return 0d;
        }

        var currentEntity = currentUnit.get();

        return clamp01(currentEntity.getArmorRemainingPercent());
    }
}
