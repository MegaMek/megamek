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
 * This consideration is used to determine the armor percent of enemies.
 */
@JsonTypeName("TargetUnitsArmor")
public class TargetUnitsArmor extends TWConsideration {
    public static final String descriptionKey = "TargetUnitsArmor";
    public enum Aggregation {
        AVERAGE,
        MIN,
        MAX
    }

    @Override
    public String getDescriptionKey() {
        return descriptionKey;
    }

    public TargetUnitsArmor() {
        parameters = Map.of("aggregation", Aggregation.AVERAGE);
        parameterTypes = Map.of("aggregation", Aggregation.class);
    }

    @Override
    public double score(DecisionContext<Entity, Entity> context) {
        var targets = context.getTargets();
        var currentUnit = context.getCurrentUnit();
        if (targets.isEmpty()) {
            return 0d;
        }

        var armorPercent = 0d;
        var targetsNumber = 0;
        var maxDistance = currentUnit.getMaxWeaponRange(currentUnit.isAirborne());
        for (var target : targets) {
            if (currentUnit.getPosition().distance(target.getPosition()) > maxDistance) {
                continue;
            }
            armorPercent += target.getArmorRemainingPercent();
            targetsNumber++;
        }

        armorPercent = switch (Aggregation.valueOf(getStringParameter("aggregation"))) {
            case AVERAGE -> targetsNumber == 0d ? 0d : armorPercent / targetsNumber;
            case MIN -> targets.stream().mapToDouble(Entity::getArmorRemainingPercent).min().orElse(0d);
            case MAX -> targets.stream().mapToDouble(Entity::getArmorRemainingPercent).max().orElse(0d);
        };

        return clamp01(armorPercent);
    }

}
