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
import megamek.common.Coords;
import megamek.common.GunEmplacement;
import megamek.common.Mek;
import megamek.common.Tank;

import java.util.Map;

import static megamek.codeUtilities.MathUtility.clamp01;

/**
 * This consideration is used to determine the attack tactic to use
 * It sets how many units should be in the front line, how many in the second line, how many should flank, the unit role of the unit in
 * each category.
 */
@JsonTypeName("CrowdingEnemies")
public class CrowdingEnemies extends TWConsideration {

    public static final String percentOfMaxWeaponRange = "percent of max weapon range";
    private static final Map<String, Class<?>> parameterTypes = Map.of(percentOfMaxWeaponRange, Double.class);
    private static final Map<String, ParameterTitleTooltip> parameterTooltips = Map.of(
        percentOfMaxWeaponRange, new ParameterTitleTooltip("PercentOfMaxWeaponRange"));

    public CrowdingEnemies() {
        parameters.put(percentOfMaxWeaponRange, 0.6);
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
    public double score(DecisionContext context) {
        var self = context.getCurrentUnit();
        Coords position = context.getFinalPosition();
        if (!(self instanceof Mek) && !(self instanceof Tank) || (self instanceof GunEmplacement)) {
            return 1.0;
        }

        final double closingDistance = Math.ceil(Math.max(3.0, 12 * 0.6));
        int crowdingEnemies = context.getEnemiesWithinRange(position, (int) closingDistance).size();
        double divider = 6 * closingDistance + 1.0;
        return clamp01(crowdingEnemies / divider);
    }

    @Override
    public CrowdingEnemies copy() {
        var copy = new CrowdingEnemies();
        copy.setCurve(getCurve().copy());
        copy.setParameters(Map.copyOf(getParameters()));
        copy.setName(getName());
        return copy;
    }
}
