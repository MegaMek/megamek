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

import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static megamek.codeUtilities.MathUtility.clamp01;

/**
 * Calculates the amount of cover the final position will give the unit against the closest N enemies
 */
@JsonTypeName("EnvironmentalHazard")
public class EnvironmentalHazard extends TWConsideration {

    public EnvironmentalHazard() {}

    @Override
    public double score(DecisionContext context) {

        var unit = context.getCurrentUnit();
        if (!context.getQuickBoardRepresentation().onGround() || unit.isAirborneAeroOnGroundMap()) {
            return 1.0;
        }

        Set<Coords> path = context.getCoordsSet();
        AtomicInteger hazards = new AtomicInteger();
        path.forEach(coords -> {
            if (context.getQuickBoardRepresentation().hasHazard(coords)) {
                hazards.getAndIncrement();
            }
        });
        return clamp01(1.0 - (hazards.get() / (double) path.size()));
    }

    @Override
    public EnvironmentalHazard copy() {
        var copy = new EnvironmentalHazard();
        copy.setCurve(getCurve().copy());
        copy.setParameters(Map.copyOf(getParameters()));
        copy.setName(getName());
        return copy;
    }
}
