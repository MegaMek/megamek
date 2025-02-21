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

import java.util.Map;

import static megamek.codeUtilities.MathUtility.clamp01;

/**
 * This consideration to determine if the unit used as much as it can of its movement.
 */
@JsonTypeName("MyUnitMoved")
public class MyUnitMoved extends TWConsideration {

    public MyUnitMoved() {
    }

    @Override
    public double score(DecisionContext context) {
        int distanceMoved = context.getDistanceMoved();
        return clamp01(distanceMoved / (double) context.getCurrentUnitMaxRunMP());
    }

    @Override
    public MyUnitMoved copy() {
        var copy = new MyUnitMoved();
        copy.setCurve(getCurve().copy());
        copy.setParameters(Map.copyOf(getParameters()));
        copy.setName(getName());
        return copy;
    }
}
