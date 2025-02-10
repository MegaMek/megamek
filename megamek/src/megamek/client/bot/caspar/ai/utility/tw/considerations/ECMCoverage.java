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
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.MovePath;

import java.util.ArrayList;
import java.util.Map;

import static megamek.codeUtilities.MathUtility.clamp01;

/**
 * Are we facing the closest enemy?
 */
@JsonTypeName("ECMCoverage")
public class ECMCoverage extends TWConsideration {

    public ECMCoverage() {
    }

    @Override
    public double score(DecisionContext<Entity, Entity> context) {
//        TWDecisionContext twc = (TWDecisionContext) context;
//        if (!twc.getCurrentUnit().hasECM()) return 0;
//
//        long overlappingECM = twc.getFriendlies().stream()
//            .filter(f -> f.hasECM())
//            .filter(f -> f.getPosition().distance(twc.getFinalPosition()) < 6)
//            .count();

//        return clamp01(1.0 - (overlappingECM * 0.2));
        return 1;
    }

    private int getFacingDiff(MovePath movePath, int desiredFacing) {
        int currentFacing = movePath.getFinalFacing();
        int facingDiff;

        if (currentFacing == desiredFacing) {
            facingDiff = 0;
        } else if ((currentFacing == ((desiredFacing + 1) % 6))
            || (currentFacing == ((desiredFacing + 5) % 6))) {
            facingDiff = 1;
        } else if ((currentFacing == ((desiredFacing + 2) % 6))
            || (currentFacing == ((desiredFacing + 4) % 6))) {
            facingDiff = 2;
        } else {
            facingDiff = 3;
        }
        return facingDiff;
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
