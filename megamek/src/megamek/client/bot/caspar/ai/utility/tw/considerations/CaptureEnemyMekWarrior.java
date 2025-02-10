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
 * Are we facing the closest enemy?
 */
@JsonTypeName("CaptureEnemyMekWarrior")
public class CaptureEnemyMekWarrior extends TWConsideration {

    public CaptureEnemyMekWarrior() {
    }

    @Override
    public double score(DecisionContext<Entity, Entity> context) {
//        if (captureAssigned) return 0;
//
//        TWDecisionContext twc = (TWDecisionContext) context;
//        Entity target = twc.getEnemyMekwarrior().orElse(null);
//        if (target == null) return 0;
//
//        Coords finalPos = twc.getMovePath().getFinalCoords();
//        boolean canCapture = finalPos.distance(target.getPosition()) == 0
//            && twc.getCurrentUnit().getPhysicalStrength() > target.getPhysicalStrength();
//
//        if (canCapture) {
//            captureAssigned = true;
//            return 1;
//        }
        return 1;
    }

    @Override
    public CaptureEnemyMekWarrior copy() {
        var copy = new CaptureEnemyMekWarrior();
        copy.setCurve(getCurve().copy());
        copy.setParameters(Map.copyOf(getParameters()));
        copy.setName(getName());
        return copy;
    }
}
