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
import megamek.common.Mek;
import megamek.common.options.OptionsConstants;

import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.stream.Stream;

import static megamek.codeUtilities.MathUtility.clamp01;

/**
 * This consideration is used to how many turns it will take for the closest enemy to reach you
 * considering your current move path
 */
@JsonTypeName("TurnsToEncounter")
public class TurnsToEncounter extends TWConsideration {

    private static final String turnsParam = "turns";
    private static final Map<String, Class<?>> parameterTypes = Map.of(turnsParam, Integer.class);
    private static final Map<String, ParameterTitleTooltip> parameterTooltips = Map.of(turnsParam, new ParameterTitleTooltip("Turns"));

    public TurnsToEncounter() {
    }

    @Override
    public double score(DecisionContext<Entity, Entity> context) {
        TWDecisionContext twContext = (TWDecisionContext) context;
        var targetOpt = twContext.getClosestEnemyFromMovePathFinalPosition();

        if (targetOpt.isPresent()) {
            var target = targetOpt.get();
            int distance = target.getPosition().distance(twContext.getMovePath().getFinalCoords());
            int bestMp = getBooleanParameter(OptionsConstants.ADVGRNDMOV_TACOPS_SPRINT) ? target.getSprintMP() : target.getRunMP();

            int turns = (int) Math.ceil(distance / (double) bestMp);
            if (turns == 0) {
                return 1.0;
            }

            return clamp01(getDoubleParameter(turnsParam) / turns);
        }
        return 0.0;
    }

    @Override
    public TurnsToEncounter copy() {
        var copy = new TurnsToEncounter();
        copy.setCurve(getCurve().copy());
        copy.setParameters(Map.copyOf(getParameters()));
        copy.setName(getName());
        return copy;
    }
}
