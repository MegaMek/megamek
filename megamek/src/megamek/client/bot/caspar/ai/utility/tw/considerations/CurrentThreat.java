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

import java.util.Map;

import static megamek.codeUtilities.MathUtility.clamp01;

/**
 * This consideration is used to determine the threat level of this field
 */
@JsonTypeName("CurrentThreat")
public class CurrentThreat extends TWConsideration {

    public CurrentThreat() {
    }

    @Override
    public double score(DecisionContext context) {
        var currentUnit = context.getCurrentUnit();
        double currentThreat = context.getQuickBoardRepresentation().getThreatLevel(currentUnit.getPosition());
        return clamp01(currentThreat);
    }

    @Override
    public CurrentThreat copy() {
        var copy = new CurrentThreat();
        copy.setCurve(getCurve().copy());
        copy.setParameters(Map.copyOf(getParameters()));
        copy.setName(getName());
        return copy;
    }
}
