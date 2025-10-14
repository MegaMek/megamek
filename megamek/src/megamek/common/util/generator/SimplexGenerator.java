/*
 * Copyright (C) 2000-2016 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2016-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common.util.generator;

import java.util.Random;

import megamek.common.util.SimplexNoise;

public class SimplexGenerator implements ElevationGenerator {
    /** Horizontal distance component between hexagons (compared to total width of a hexagon = 1.0) */
    private static final double DIST_H = 0.75;
    /** Vertical distance between hexagons */
    private static final double DIST_V = Math.sqrt(3) / 2.0;

    private final Random rnd;

    public SimplexGenerator() {
        this(new Random());
    }

    public SimplexGenerator(Random rnd) {
        this.rnd = rnd;
    }

    @Override
    public String getName() {
        return "Generator.Simplex";
    }

    @Override
    public String getTooltip() {
        return "Generator.Simplex.toolTip";
    }

    @Override
    public void generate(int hilliness, int width, int height, int[][] elevationMap) {
        double noiseStartX = rnd.nextDouble() * 1000000;
        double noiseStartY = rnd.nextDouble() * 1000000;
        double noiseScale = (200.0 + rnd.nextDouble() * 30.0) / (4.0 + hilliness / 5.0);
        hilliness = Math.max(hilliness, 1);

        for (int w = 0; w < width; ++w) {
            for (int h = 0; h < height; ++h) {
                double x = DIST_H * w;
                double y = DIST_V * (2 * h + (w & 1)) / 2.0;
                // SimplexNoise.noiseOctaves(..., 6) is between -6 and +6, most values are in the [-2, 2] range
                double val = SimplexNoise.noiseOctaves(
                      x / noiseScale + noiseStartX,
                      y / noiseScale + noiseStartY, 6) + 0.5;
                if (val < 0) {
                    // Flatten out the bottom part
                    // TODO: Make this configurable?
                    val = 0;
                } else {
                    // Hilliness - make the hilltops more extreme thus the lower values more common
                    val = Math.pow(val / 7.0, 10.0 / hilliness + 1.0) * 7.0;
                }
                // Give the map scaler enough value range to work with
                elevationMap[w][h] = (int) (val * 1000);
            }
        }
    }
}
