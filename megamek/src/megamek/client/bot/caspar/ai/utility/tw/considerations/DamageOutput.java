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
import megamek.common.Entity;

import java.util.Map;

import static megamek.codeUtilities.MathUtility.clamp01;

/**
 * How much damage this unit can dish out compared to how much it is expected to take.
 */
@JsonTypeName("DamageOutput")
public class DamageOutput extends TWConsideration {

    private static final String damageFactorParam = "damage factor";
    private static final Map<String, Class<?>> parameterTypes = Map.of(damageFactorParam, Integer.class);
    private static final Map<String, ParameterTitleTooltip> parameterTooltips = Map.of(damageFactorParam, new ParameterTitleTooltip("DamageFactor"));

    public DamageOutput() {
        parameters = Map.of(damageFactorParam, 2);
    }

    @Override
    public Map<String, Class<?>> getParameterTypes() {
        return parameterTypes;
    }

    @Override
    public Map<String, ParameterTitleTooltip> getParameterTooltips() {
        return parameterTooltips;
    }

    @Override
    public double score(DecisionContext<Entity, Entity> context) {
        var twContext = (TWDecisionContext) context;
        var totalDamage = twContext.getFiringDamage() + twContext.getPhysicalDamage();
        var damageTaken = twContext.getExpectedDamage();

        double ratio = (damageTaken == 0) ? Double.POSITIVE_INFINITY : (totalDamage / damageTaken);
        double score = ratio / getIntParameter(damageFactorParam);
        return clamp01(score);
    }

    @Override
    public DamageOutput copy() {
        var copy = new DamageOutput();
        copy.setCurve(getCurve().copy());
        copy.setParameters(Map.copyOf(getParameters()));
        copy.setName(getName());
        return copy;
    }
}
