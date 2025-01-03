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

import java.util.HashMap;
import java.util.Map;

import static megamek.codeUtilities.MathUtility.clamp01;

/**
 * This consideration is used to determine if a target is an easy target.
 */
@JsonTypeName("MyUnitUnderThreat")
public class MyUnitUnderThreat extends TWConsideration {

    public MyUnitUnderThreat() {
    }

    @Override
    public double score(DecisionContext<Entity, Entity> context) {
        var currentUnit = context.getCurrentUnit().orElseThrow();
        Map<Integer, Double> threat = new HashMap<>();

        for (var enemyUnit : context.getEnemyUnits()) {
            var enemyRange = enemyUnit.getPosition().distance(currentUnit.getPosition());
            if (enemyUnit.getPosition().distance(currentUnit.getPosition()) <= enemyUnit.getMaxWeaponRange()) {
                var maxDamage = context.getUnitMaxDamageAtRange(enemyUnit, enemyRange);
                threat.put(enemyRange, threat.getOrDefault(enemyRange, 0d) + maxDamage);
            }
        }

        var maxThreat = threat.values().stream().max(Double::compareTo).orElse(0d);

        return clamp01( maxThreat / currentUnit.getTotalArmor());
    }
}
