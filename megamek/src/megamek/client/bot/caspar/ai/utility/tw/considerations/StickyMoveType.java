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

/**
 * This consideration is used to see if the unit wants to stick to the previous movement type taken.
 */
@JsonTypeName("StickyMoveType")
public class StickyMoveType extends TWConsideration {

    public StickyMoveType() {
    }

    @Override
    public double score(DecisionContext<Entity, Entity> context) {
        var twContext = (TWDecisionContext) context;
        var previousPathTaken = twContext.getPreviouslyRanked();
        if (previousPathTaken.isEmpty()) {
            return 1;
        }
        if (twContext.getMovePath().getLastStepMovementType().equals(previousPathTaken.get().getPath().getLastStepMovementType())) {
            return 1;
        }

        return 0;
    }

    @Override
    public StickyMoveType copy() {
        var copy = new StickyMoveType();
        copy.setCurve(getCurve().copy());
        copy.setParameters(Map.copyOf(getParameters()));
        copy.setName(getName());
        return copy;
    }
}
