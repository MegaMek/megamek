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
import megamek.ai.utility.ParameterTitleTooltip;
import megamek.client.bot.caspar.ai.utility.tw.decision.TWDecisionContext;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.UnitRole;

import java.util.Map;

import static megamek.codeUtilities.MathUtility.clamp01;

/**
 * This consideration is used to determine if the unit is
 * in the same place as before
 */
@JsonTypeName("StandStill")
public class StandStill extends TWConsideration {

    public StandStill() {
    }

    @Override
    public double score(DecisionContext context) {
        var currentUnit = context.getCurrentUnit();
        Coords currentPosition = currentUnit.getPosition();
        Coords finalPosition = context.getFinalPosition();
        if (currentPosition != null) {
            return  1.0 / Math.max(finalPosition.distance(currentPosition), context.getHexesMoved());
        }
        return 0.0;
    }

    @Override
    public StandStill copy() {
        var copy = new StandStill();
        copy.setCurve(getCurve().copy());
        copy.setParameters(Map.copyOf(getParameters()));
        copy.setName(getName());
        return copy;
    }
}
