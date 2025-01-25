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
import megamek.client.bot.queen.ai.utility.tw.decision.TWDecisionContext;
import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.MovePath;

import java.util.ArrayList;

import static megamek.codeUtilities.MathUtility.clamp01;

/**
 * Are we facing the closest enemy?
 */
@JsonTypeName("FacingTheEnemy")
public class FacingTheEnemy extends TWConsideration {

    public FacingTheEnemy() {
    }

    @Override
    public double score(DecisionContext<Entity, Entity> context) {
        var firingUnit = context.getCurrentUnit();
        var twDecisionContext = (TWDecisionContext) context;
        var targets = twDecisionContext.getEnemiesAtRange(firingUnit.getPosition(), firingUnit.getMaxWeaponRange());
        var movePath = twDecisionContext.getMovePath();
        var coordsToFace = new ArrayList<Coords>();
        for (var enemy : targets) {
            var currentDistance = enemy.getPosition().distance(firingUnit.getPosition());
            if (enemy.getMaxWeaponRange() >= currentDistance) {
                coordsToFace.add(enemy.getPosition());
            }
        }

        Coords toFace = Compute.randomListElement(coordsToFace);
        int desiredFacing = (toFace.direction(firingUnit.getPosition()) + 3) % 6;
        int facingDiff = getFacingDiff(movePath, desiredFacing);

        return clamp01((3 - facingDiff) / 3.0);
    }

    private int getFacingDiff(MovePath movePath, int desiredFacing) {
        int currentFacing = movePath.getFinalFacing();
        int facingDiff;

        if (currentFacing == desiredFacing) {
            facingDiff = 0;
        } else if ((currentFacing == ((desiredFacing + 1) % 6))
            || (currentFacing == ((desiredFacing + 5) % 6))) {
            facingDiff = 1;
        } else if ((currentFacing == ((desiredFacing + 2) % 6))
            || (currentFacing == ((desiredFacing + 4) % 6))) {
            facingDiff = 2;
        } else {
            facingDiff = 3;
        }
        return facingDiff;
    }
}
