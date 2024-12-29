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
@JsonTypeName("MyUnitRoleIs")
public class MyUnitRoleIs extends TWConsideration {

    public MyUnitRoleIs() {
        parameters = Map.of("role", UnitRole.AMBUSHER.name());
    }

    @Override
    public double score(DecisionContext<Entity, Entity> context) {
        if (!hasParameter("role")) {
            return 0d;
        }

        var currentUnit = context.getCurrentUnit().orElseThrow();
        var role = UnitRole.valueOf(getStringParameter("role"));

        return currentUnit.getRole().equals(role) ? 1d : 0d;
    }

}
