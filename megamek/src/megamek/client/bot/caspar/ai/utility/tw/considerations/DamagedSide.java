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
 * Calculates the amount of damage per "damage table"
 */
@JsonTypeName("Crowding")
public class DamagedSide extends TWConsideration {

    public static final String roleParam = "role";
    private static final Map<String, Class<?>> parameterTypes = Map.of(roleParam, UnitRole.class);
    private static final Map<String, ParameterTitleTooltip> parameterTooltips = Map.of(roleParam, new ParameterTitleTooltip("FavTargetUnitRole"));

    public DamagedSide() {
    }

    @Override
    public double score(DecisionContext<Entity, Entity> context) {
//        Entity unit = context.getCurrentUnit();
//        int damagedFacing = calculateMostDamagedFacing(unit);
//        int enemyDirection = getPrimaryEnemyDirection(context);
//
//        return (damagedFacing == enemyDirection) ? 0 : 1;
        return 1;
    }

    private int calculateMostDamagedFacing(Entity unit) {
        // Implementation analyzing armor per facing
        return 0;
    }

    @Override
    public DamagedSide copy() {
        var copy = new DamagedSide();
        copy.setCurve(getCurve().copy());
        copy.setParameters(Map.copyOf(getParameters()));
        copy.setName(getName());
        return copy;
    }
}
