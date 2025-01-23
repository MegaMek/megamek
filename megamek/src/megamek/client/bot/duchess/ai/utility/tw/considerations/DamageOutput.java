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

package megamek.client.bot.duchess.ai.utility.tw.considerations;

import com.fasterxml.jackson.annotation.JsonTypeName;
import megamek.ai.utility.DecisionContext;
import megamek.client.bot.duchess.ai.utility.tw.decision.TWDecisionContext;
import megamek.common.Entity;

import java.util.Map;

import static megamek.codeUtilities.MathUtility.clamp01;

/**
 * This consideration is used to determine if you are an easy target.
 */
@JsonTypeName("DamageOutput")
public class DamageOutput extends TWConsideration {
    public static final String descriptionKey = "DamageOutput";

    public DamageOutput() {
        parameters = Map.of("damage factor", "2:1");
        parameterTypes = Map.of("damage factor", String.class);
    }

    @Override
    public String getDescriptionKey() {
        return descriptionKey;
    }

    @Override
    public double score(DecisionContext<Entity, Entity> context) {
        var twContext = (TWDecisionContext) context;
        var totalDamage = twContext.getFiringDamage() + twContext.getPhysicalDamage();
        var damageTaken = twContext.getExpectedDamage();

        var damageFactor = getStringParameter("damage factor");
        var damageFactorSplit = damageFactor.split(":");
        var damageFactorNum = Double.parseDouble(damageFactorSplit[0]);
        var damageFactorDenom = Double.parseDouble(damageFactorSplit[1]);
        var damageFactorValue = damageFactorNum / damageFactorDenom;

        double ratio = damageTaken == 0 ? Double.POSITIVE_INFINITY : totalDamage / damageTaken;
        double score = ratio / (ratio + damageFactorValue);
        return clamp01(score);
    }
}
