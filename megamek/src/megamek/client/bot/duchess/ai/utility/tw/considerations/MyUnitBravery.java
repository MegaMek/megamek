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
import megamek.client.bot.duchess.ai.utility.tw.decision.TWDecisionContext;
import megamek.common.Compute;
import megamek.common.Entity;

import static megamek.codeUtilities.MathUtility.clamp01;

/**
 * Decides that it is too dangerous to stay under enemy weapons range.
 */
@JsonTypeName("MyUnitBravery")
public class MyUnitBravery extends TWConsideration {

    public static final String descriptionKey = "MyUnitBravery";

    public MyUnitBravery() {
    }

    @Override
    public String getDescriptionKey() {
        return descriptionKey;
    }

    @Override
    public double score(DecisionContext<Entity, Entity> context) {
        TWDecisionContext twContext = (TWDecisionContext) context;
        var self = twContext.getCurrentUnit();
        var myWeaponsDamage = Compute.computeTotalDamage(self.getTotalWeaponList());
        var totalDamageFraction = clamp01(twContext.getTotalDamage() / (double) myWeaponsDamage);
        var damageCap = clamp01(twContext.getExpectedDamage() / (double) self.getTotalArmor()) / 2;
        double braveryValue = twContext.getDuchess().getBehaviorSettings().getBraveryIndex();
        double braveryFraction = braveryValue / 10.0;
        double braveryMod = totalDamageFraction * braveryFraction - (1 - damageCap);
        return clamp01(braveryMod);
    }

}
