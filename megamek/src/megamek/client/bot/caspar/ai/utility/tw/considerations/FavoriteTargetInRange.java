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
import megamek.common.Entity;
import megamek.common.UnitRole;

import java.util.Map;

import static megamek.codeUtilities.MathUtility.clamp01;

/**
 * This consideration is used to determine if the favorite target is the closest at the final step.
 */
@JsonTypeName("FavoriteTargetInRange")
public class FavoriteTargetInRange extends TWConsideration {
    public static final String roleParam = "role";
    private static final Map<String, Class<?>> parameterTypes = Map.of(roleParam, UnitRole.class);
    private static final Map<String, ParameterTitleTooltip> parameterTooltips = Map.of(roleParam, new ParameterTitleTooltip("FavTargetUnitRole"));

    public FavoriteTargetInRange() {
        parameters = Map.of(roleParam, UnitRole.UNDETERMINED);
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
        var finalPosition = context.getFinalPosition();
        if (finalPosition == null) {
            return 0;
        }

        var role = getParameter(roleParam, UnitRole.class);
        var maxRange = context.getMaxWeaponRange();
        var distance = context.getDistanceToClosestEnemyWithRole(role);
        if (distance.isEmpty()) {
            return 0;
        }

        var distanceValue = distance.getAsInt();
        if (distanceValue == 0) {
            distanceValue = 2;
        }

        return clamp01(maxRange / (double) distanceValue);
    }

    @Override
    public FavoriteTargetInRange copy() {
        var copy = new FavoriteTargetInRange();
        copy.setCurve(getCurve().copy());
        copy.setParameters(Map.copyOf(getParameters()));
        copy.setName(getName());
        return copy;
    }
}
