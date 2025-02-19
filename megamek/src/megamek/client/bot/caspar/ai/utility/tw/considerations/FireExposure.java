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
import megamek.common.UnitRole;

import java.util.Map;

/**
 * This consideration is used to determine how expose to enemy threat is this unit
 */
@JsonTypeName("FireExposure")
public class FireExposure extends TWConsideration {

    public FireExposure() {}

    @Override
    public double score(DecisionContext context) {
        return context.getEnemyThreat(context.getFinalPosition());
    }

    @Override
    public FireExposure copy() {
        var copy = new FireExposure();
        copy.setCurve(getCurve().copy());
        copy.setParameters(Map.copyOf(getParameters()));
        copy.setName(getName());
        return copy;
    }
}
