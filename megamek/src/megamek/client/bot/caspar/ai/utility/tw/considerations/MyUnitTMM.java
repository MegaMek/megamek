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
import megamek.client.bot.caspar.ai.utility.tw.context.TWWorld;
import megamek.client.bot.caspar.ai.utility.tw.decision.TWDecisionContext;
import megamek.common.Compute;
import megamek.common.Entity;

import java.util.Map;

import static megamek.codeUtilities.MathUtility.clamp01;

/**
 * This consideration is used to determine the defense due to the movement.
 */
@JsonTypeName("MyUnitTMM")
public class MyUnitTMM extends TWConsideration {

    public MyUnitTMM() {
    }

    @Override
    public double score(DecisionContext<Entity, Entity> context) {
        var currentUnit = context.getCurrentUnit();
        TWDecisionContext twContext = (TWDecisionContext) context;
        var movePath = twContext.getMovePath();
        var hexesMoved = movePath.getHexesMoved();
        var tmm = Compute.getTargetMovementModifier(
            hexesMoved,
            movePath.isJumping(),
            movePath.getFinalAltitude() > 0 && !currentUnit.isAerospace(),
            ((TWWorld) context.getWorld()).getGame());

        return clamp01(tmm.getValue() / 8.0d);
    }

    @Override
    public MyUnitTMM copy() {
        var copy = new MyUnitTMM();
        copy.setCurve(getCurve().copy());
        copy.setParameters(Map.copyOf(getParameters()));
        copy.setName(getName());
        return copy;
    }
}
