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

import java.util.List;
import java.util.Map;

import static megamek.codeUtilities.MathUtility.clamp01;

/**
 * This consideration is used to determine the armor percent.
 */
@JsonTypeName("FlankingPosition")
public class FlankingPosition extends TWConsideration {

    public FlankingPosition() {
    }

    @Override
    public double score(DecisionContext context) {
        Coords position = context.getFinalPosition();
        List<Coords> target = context.getNClosestEnemiesPositions(position, 1);
        if (target.isEmpty()) return 0;
        int attackAngle = target.get(0).direction(position);

        int angleDiff = Math.abs(context.getFinalFacing() - attackAngle) % 6;
        angleDiff = Math.min(angleDiff, 6 - angleDiff);

        // 0 = front, 3 = rear
        return clamp01(angleDiff / 3.0);
    }

    @Override
    public FlankingPosition copy() {
        var copy = new FlankingPosition();
        copy.setCurve(getCurve().copy());
        copy.setParameters(Map.copyOf(getParameters()));
        copy.setName(getName());
        return copy;
    }
}
