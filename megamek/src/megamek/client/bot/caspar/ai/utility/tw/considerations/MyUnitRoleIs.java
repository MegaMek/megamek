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

/**
 * Check if the current unit has a specific role. Usually to be used to filter certain types of decisions to specific units.
 */
@JsonTypeName("MyUnitRoleIs")
public class MyUnitRoleIs extends TWConsideration {
    public static final String roleParam = "my unit's role";
    private static final Map<String, Class<?>> parameterTypes = Map.of(roleParam, UnitRole.class);
    private static final Map<String, ParameterTitleTooltip> parameterTooltips = Map.of(roleParam, new ParameterTitleTooltip("MyUnitRole"));

    public MyUnitRoleIs() {
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
    public double score(DecisionContext<Entity, Entity> context) {
        var currentUnit = context.getCurrentUnit();
        var role = getParameter(roleParam, UnitRole.class);
        if (role.equals(UnitRole.UNDETERMINED)) {
            return 1;
        }
        return currentUnit.getRole().equals(role) ? 1d : 0d;
    }

    @Override
    public MyUnitRoleIs copy() {
        var copy = new MyUnitRoleIs();
        copy.setCurve(getCurve().copy());
        copy.setParameters(Map.copyOf(getParameters()));
        copy.setName(getName());
        return copy;
    }
}
