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

/**
 * This consideration is used to determine if a specific role is present in between the targets.
 */
@JsonTypeName("TargetUnitsHaveRole")
public class TargetUnitsHaveRole extends TWConsideration {
    public static final String descriptionKey = "TargetUnitsHaveRole";
    public TargetUnitsHaveRole() {
        parameters = Map.of("role", UnitRole.AMBUSHER);
        parameterTypes = Map.of("role", UnitRole.class);
    }

    @Override
    public String getDescriptionKey() {
        return descriptionKey;
    }

    @Override
    public double score(DecisionContext<Entity, Entity> context) {
        if (!hasParameter("role")) {
            return 0d;
        }

        for (var target : context.getTargets()) {
            var role = UnitRole.valueOf(getStringParameter("role"));
            if (target.getRole().equals(role)) {
                return 1d;
            }
        }

        return 0d;
    }

}
