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
import megamek.client.bot.princess.ArtilleryTargetingControl;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.UnitRole;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static megamek.codeUtilities.MathUtility.clamp01;

/**
 * This consideration is used to determine friendly artillery fire risk
 */
@JsonTypeName("FriendlyArtilleryFire")
public class FriendlyArtilleryFire extends TWConsideration {

    public FriendlyArtilleryFire() {
    }

    @Override
    public double score(DecisionContext context) {
        Map<Coords, Double> artyDamage = context.getIncomingFriendlyArtilleryDamage();
        double friendlyArtilleryDamage = 0.0;
        for (Coords c : context.getFinalPosition().allAtDistanceOrLess(2)) {
            if (artyDamage.containsKey(c)) {
                friendlyArtilleryDamage = artyDamage.get(c);
                break;
            }
        }
        var currentUnit = context.getCurrentUnit();
        return clamp01(friendlyArtilleryDamage / Math.max(1.0, currentUnit.getTotalArmor()));
    }

    @Override
    public FriendlyArtilleryFire copy() {
        var copy = new FriendlyArtilleryFire();
        copy.setCurve(getCurve().copy());
        copy.setParameters(Map.copyOf(getParameters()));
        copy.setName(getName());
        return copy;
    }
}
