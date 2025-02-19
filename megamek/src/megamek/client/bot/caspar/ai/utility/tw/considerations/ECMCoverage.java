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

import java.util.Map;

import static megamek.codeUtilities.MathUtility.clamp01;

@JsonTypeName("ECMCoverage")
public class ECMCoverage extends TWConsideration {

    public ECMCoverage() {
    }

    @Override
    public double score(DecisionContext context) {
        var currentUnit = context.getCurrentUnit();
        if (!currentUnit.hasECM()) return 0.0;

        Coords finalPosition = context.getFinalPosition();
        long overlappingECM = context.getFriendliesWithinRange(finalPosition, 6).stream()
            .filter(e -> ((Entity) e).hasECM())
            .count();

        return clamp01(1.0 / (overlappingECM + 1));
    }

    @Override
    public ECMCoverage copy() {
        var copy = new ECMCoverage();
        copy.setCurve(getCurve().copy());
        copy.setParameters(Map.copyOf(getParameters()));
        copy.setName(getName());
        return copy;
    }
}
