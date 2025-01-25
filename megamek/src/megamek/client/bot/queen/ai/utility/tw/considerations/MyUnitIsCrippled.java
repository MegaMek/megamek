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

package megamek.client.bot.queen.ai.utility.tw.considerations;

import com.fasterxml.jackson.annotation.JsonTypeName;
import megamek.ai.utility.DecisionContext;
import megamek.common.Entity;

import static megamek.codeUtilities.MathUtility.clamp01;

/**
 * Just check if the current unit is crippled or not.
 */
@JsonTypeName("MyUnitIsCrippled")
public class MyUnitIsCrippled extends TWConsideration {
    public MyUnitIsCrippled() {
    }

    @Override
    public double score(DecisionContext<Entity, Entity> context) {
        var currentUnit = context.getCurrentUnit();
        return currentUnit.isCrippled(true) ? 1d : 0d;
    }

}
