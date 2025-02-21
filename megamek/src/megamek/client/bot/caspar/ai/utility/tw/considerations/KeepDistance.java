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
 * This consideration is used to determine if there are too many enemies too close
 */
@JsonTypeName("KeepDistance")
public class KeepDistance extends TWConsideration {

    public KeepDistance() {
    }

    @Override
    public double score(DecisionContext context) {
        long numberOfEnemiesTooClose = context.getNClosestEnemiesPositions(context.getFinalPosition(), 10).stream()
            .filter(c -> c.distance(context.getFinalPosition()) <= 12).count();
        return clamp01(numberOfEnemiesTooClose / 10);
    }

    @Override
    public KeepDistance copy() {
        var copy = new KeepDistance();
        copy.setCurve(getCurve().copy());
        copy.setParameters(Map.copyOf(getParameters()));
        copy.setName(getName());
        return copy;
    }
}
