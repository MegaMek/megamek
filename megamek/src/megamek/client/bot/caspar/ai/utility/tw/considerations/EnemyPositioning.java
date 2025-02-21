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
import megamek.ai.utility.StructOfUnitArrays;
import megamek.common.Coords;

import java.util.Map;
import java.util.Objects;

/**
 * This consideration is used to determine friendly artillery fire risk
 */
@JsonTypeName("EnemyPositioning")
public class EnemyPositioning extends TWConsideration {
    private static final double BANDWIDTH = Math.pow(0.8, 2);

    public EnemyPositioning() {}

    @Override
    public double score(DecisionContext context) {
        StructOfUnitArrays enemies = context.getStructOfEnemiesArrays();

        if (enemies == null || enemies.length == 0) {
            return 0.0;
        }

        // Kernel density estimation on the enemy positions
        double entropyOfEnemyPositions = 0.0;
        int n = enemies.length;
        double density;

        for (var i : enemies) {
            density = 0.0;
            for (var j : enemies) {
                if (!Objects.equals(i, j)) {
                    double distance = Coords.distance(enemies.getX(i), enemies.getY(i), enemies.getX(j), enemies.getY(j));
                    density += Math.exp(-Math.pow(distance, 2) / (2 * BANDWIDTH));
                }
            }
            density /= (n - 1) * Math.sqrt(2 * Math.PI * BANDWIDTH);
            // Adding a small value to avoid log(0)
            entropyOfEnemyPositions -= density * Math.log(density + 1e-10);
        }

        return entropyOfEnemyPositions;
    }

    @Override
    public EnemyPositioning copy() {
        var copy = new EnemyPositioning();
        copy.setCurve(getCurve().copy());
        copy.setParameters(Map.copyOf(getParameters()));
        copy.setName(getName());
        return copy;
    }
}
