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
import megamek.common.options.OptionsConstants;

import java.util.Map;

import static megamek.codeUtilities.MathUtility.clamp01;

/**
 * This consideration is used to how many turns it will take for the closest enemy to reach you
 * considering your current move path
 */
@JsonTypeName("TurnsToEncounter")
public class TurnsToEncounter extends TWConsideration {

    public TurnsToEncounter() {
    }

    @Override
    public double score(DecisionContext context) {
        var targetOpt = context.getClosestEnemy();

        if (targetOpt.isPresent() && targetOpt.get() instanceof Entity target) {
            int distance = target.getPosition().distance(context.getFinalPosition());
            int bestMp = getBooleanParameter(OptionsConstants.ADVGRNDMOV_TACOPS_SPRINT) ? target.getSprintMP() : target.getRunMP();
            if (bestMp <= 0) {
                return 0.0;
            }
            int turns = (int) Math.ceil(distance / (double) bestMp);
            return clamp01(turns);
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
