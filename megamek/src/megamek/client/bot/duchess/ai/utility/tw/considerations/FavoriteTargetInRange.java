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
import megamek.common.Entity;
import megamek.common.UnitRole;

import java.util.Map;

import static megamek.codeUtilities.MathUtility.clamp01;

/**
 * This consideration is used to determine if a target is an easy target.
 */
@JsonTypeName("FavoriteTargetInRange")
public class FavoriteTargetInRange extends TWConsideration {
    public static final String descriptionKey = "FavoriteTargetInRange";

    public FavoriteTargetInRange() {
        parameters = Map.of("role", UnitRole.MISSILE_BOAT);
        parameterTypes = Map.of("role", UnitRole.class);
    }

    @Override
    public String getDescriptionKey() {
        return descriptionKey;
    }

    @Override
    public double score(DecisionContext<Entity, Entity> context) {
        var targets = context.getTargets();
        var firingUnit = context.getCurrentUnit();
        var maxRange = firingUnit.getMaxWeaponRange();
        var role = UnitRole.valueOf(getStringParameter("role"));
        var distance = Integer.MAX_VALUE;
        for (var target : targets) {
            if (target.getRole().equals(role) && target.getPosition().distance(firingUnit.getPosition()) <= maxRange) {
                return 1d;
            } else {
                distance = Math.min(distance, target.getPosition().distance(firingUnit.getPosition()));
            }
        }
        return clamp01( 1.000001d - (((double) (distance - maxRange)) / (double) maxRange));
    }
}
