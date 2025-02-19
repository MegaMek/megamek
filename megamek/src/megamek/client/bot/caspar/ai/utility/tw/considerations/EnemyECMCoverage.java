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
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.Targetable;

import java.util.Map;

import static megamek.codeUtilities.MathUtility.clamp01;

/**
 * Are we facing the closest enemy?
 */
@JsonTypeName("EnemyECMCoverage")
public class EnemyECMCoverage extends TWConsideration {

    public EnemyECMCoverage() {
    }

    @Override
    public double score(DecisionContext context) {
        Coords finalPosition = context.getFinalPosition();
        long overlappingECM = 0;
        for (Targetable target : context.getEnemiesWithinRange(finalPosition, 6)) {
            if (target instanceof Entity enemy) {
                if (enemy.hasECM()) {
                    overlappingECM++;
                }
            }
        }
        return clamp01(1 / (1.0 + overlappingECM));
    }

    @Override
    public EnemyECMCoverage copy() {
        var copy = new EnemyECMCoverage();
        copy.setCurve(getCurve().copy());
        copy.setParameters(Map.copyOf(getParameters()));
        copy.setName(getName());
        return copy;
    }
}
