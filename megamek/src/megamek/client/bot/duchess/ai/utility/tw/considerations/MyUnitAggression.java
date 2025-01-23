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

package megamek.client.bot.duchess.ai.utility.tw.considerations;

import com.fasterxml.jackson.annotation.JsonTypeName;
import megamek.ai.utility.DecisionContext;
import megamek.client.bot.duchess.ai.utility.tw.decision.TWDecisionContext;
import megamek.common.Entity;
import megamek.common.Infantry;

import static megamek.codeUtilities.MathUtility.clamp01;

/**
 * Decides that it is too dangerous to stay under enemy weapons range.
 */
@JsonTypeName("MyUnitAggression")
public class MyUnitAggression extends TWConsideration {

    public static final String descriptionKey = "MyUnitAggression";

    public MyUnitAggression() {
    }

    @Override
    public String getDescriptionKey() {
        return descriptionKey;
    }

    @Override
    public double score(DecisionContext<Entity, Entity> context) {
        TWDecisionContext twContext = (TWDecisionContext) context;
        var self = twContext.getCurrentUnit();
        var closestEnemy = twContext.getClosestEnemy(self.getPosition());
        if (closestEnemy == null) {
            return 0d;
        }
        double distToEnemy = closestEnemy.getPosition().distance(self.getPosition());

        if ((distToEnemy == 0) && !(self instanceof Infantry)) {
            distToEnemy = 2;
        }

        double aggression = twContext.getDuchess().getBehaviorSettings().getHyperAggressionIndex();
        double aggressionFraction = aggression / 10.0;
        double closeness = 1.0 / (1.0 + distToEnemy);
        double aggressionMod = aggressionFraction * closeness;
        return clamp01(aggressionMod);
    }

}
