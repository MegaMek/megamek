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
import megamek.client.bot.princess.UnitBehavior;
import megamek.common.Entity;

import static megamek.codeUtilities.MathUtility.clamp01;

/**
 * This consideration is used to determine the defense due to the movement.
 */
@JsonTypeName("MyUnitIsFleeing")
public class MyUnitIsMovingTowardsWaypoint extends TWConsideration {

    public static final String descriptionKey = "MyUnitIsFleeing";

    public MyUnitIsMovingTowardsWaypoint() {
    }

    @Override
    public String getDescriptionKey() {
        return descriptionKey;
    }

    @Override
    public double score(DecisionContext<Entity, Entity> context) {
        TWDecisionContext twContext = (TWDecisionContext) context;
        var movePath = twContext.getMovePath();
        var self = twContext.getCurrentUnit();
        var behaviorType = ((TWDecisionContext) context).getUnitBehavior(self);
        if (behaviorType == UnitBehavior.BehaviorType.MoveToDestination) {
//            int newDistanceToHome = distanceToHomeEdge(path.getFinalCoords(), getOwner().getHomeEdge(movingUnit), game);
//            double selfPreservation = getOwner().getBehaviorSettings().getSelfPreservationValue();
//            double selfPreservationMod = 0;
//
//            // normally, we favor being closer to the edge we're trying to get to
//            if (newDistanceToHome > 0) {
//                selfPreservationMod = newDistanceToHome * selfPreservation;
//                // if this path gets us to the edge, we value it considerably more than we do
//                // paths that don't get us there
//            } else {
//                selfPreservationMod = -ARRIVED_AT_DESTINATION_FACTOR;
//            }
//
//            formula.append(" - selfPreservationMod [")
//                .append(LOG_DECIMAL.format(selfPreservationMod))
//                .append(" = ").append(LOG_DECIMAL.format(newDistanceToHome))
//                .append(" * ")
//                .append(LOG_DECIMAL.format(selfPreservation)).append("]");
//            return selfPreservationMod;
        }

//        return 0.0;

        return clamp01(0d);
    }

}
