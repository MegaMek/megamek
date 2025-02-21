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
@JsonTypeName("FriendlyPositioning")
public class FriendlyPositioning extends TWConsideration {
    private static final double BANDWIDTH = Math.pow(0.8, 2);

    public FriendlyPositioning() {
    }

    @Override
    public double score(DecisionContext context) {

        StructOfUnitArrays friends = context.getStructOfAlliesArrays();
        if (friends == null || friends.length == 0) {
            return 0.0;
        }

        // Kernel density estimation on the enemy positions
        double entropy = 0.0;
        int n = friends.length;
        double density;

        for (var i : friends) {
            density = 0.0;
            for (var j : friends) {
                if (!Objects.equals(i, j)) {
                    double distance = Coords.distance(friends.getX(i), friends.getY(i), friends.getX(j), friends.getY(j));
                    density += Math.exp(-Math.pow(distance, 2) / (2 * BANDWIDTH));
                }
            }
            density /= (n - 1) * Math.sqrt(2 * Math.PI * BANDWIDTH);
            // Adding a small value to avoid log(0)
            entropy -= density * Math.log(density + 1e-10);
        }

        return entropy;
    }

    @Override
    public FriendlyPositioning copy() {
        var copy = new FriendlyPositioning();
        copy.setCurve(getCurve().copy());
        copy.setParameters(Map.copyOf(getParameters()));
        copy.setName(getName());
        return copy;
    }
}
