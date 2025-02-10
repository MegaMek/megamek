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
import megamek.common.UnitRole;

import java.util.Map;

import static megamek.codeUtilities.MathUtility.clamp01;

/**
 * This consideration is used to determine the threat level of this field
 */
@JsonTypeName("TimeToKill")
public class TimeToKill extends TWConsideration {

    public static final String roleParam = "role";
    private static final Map<String, Class<?>> parameterTypes = Map.of(roleParam, UnitRole.class);
    private static final Map<String, ParameterTitleTooltip> parameterTooltips = Map.of(roleParam, new ParameterTitleTooltip("FavTargetUnitRole"));

    public TimeToKill() {
    }
    @Override
    public double score(DecisionContext<Entity, Entity> context) {
//        TWDecisionContext twc = (TWDecisionContext) context;
//        Entity target = twc.getPrimaryTarget().orElse(null);
//        if (target == null) return 0;
//
//        double dps = calculateDamagePerTurn(twc.getCurrentUnit(), target);
//        double ttk = target.getArmorRemaining() / dps;
//        return clamp01(1.0 - (ttk / MAX_TTK));
        return 1;
    }

    @Override
    public TimeToKill copy() {
        var copy = new TimeToKill();
        copy.setCurve(getCurve().copy());
        copy.setParameters(Map.copyOf(getParameters()));
        copy.setName(getName());
        return copy;
    }
}
