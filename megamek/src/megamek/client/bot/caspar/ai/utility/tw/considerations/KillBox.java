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

/**
 * This consideration is used to determine the attack tactic to use
 * It sets how many units should be in the front line, how many in the second line, how many should flank, the unit role of the unit in
 * each category.
 */
@JsonTypeName("AttackTactics")
public class KillBox extends TWConsideration {

    public static final String roleParam = "role";
    private static final Map<String, Class<?>> parameterTypes = Map.of(roleParam, UnitRole.class);
    private static final Map<String, ParameterTitleTooltip> parameterTooltips = Map.of(roleParam, new ParameterTitleTooltip("FavTargetUnitRole"));

    public KillBox() {
    }

    @Override
    public double score(DecisionContext<Entity, Entity> context) {
//        TWDecisionContext twc = (TWDecisionContext) context;
//        UnitRole role = getParameter("role", UnitRole.class);
//        String tactic = getParameter("preferredTactic", String.class);
//
//        if (!twc.getCurrentUnit().getRole().equals(role)) return 0;
//
//        return switch (tactic) {
//            case "flanking" -> calculateFlankingScore(twc);
//            case "firing_line" -> calculateFiringLineScore(twc);
//            case "hit_and_run" -> calculateHitAndRunScore(twc);
//            default -> 0;
//        };
        return 1;
    }

    private double calculateFlankingScore(TWDecisionContext context) {
        // Implementation for flanking position analysis
//        return clamp01(context.getFlankingAngle() / 180.0);
        return 1;
    }


    @Override
    public KillBox copy() {
        var copy = new KillBox();
        copy.setCurve(getCurve().copy());
        copy.setParameters(Map.copyOf(getParameters()));
        copy.setName(getName());
        return copy;
    }
}
