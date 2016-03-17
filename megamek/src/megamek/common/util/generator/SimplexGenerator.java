/*
 * MegaMek - Copyright (C) 2000-2016 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.common.util.generator;

import java.util.Random;

import megamek.common.util.SimplexNoise;

public class SimplexGenerator implements ElevationGenerator {
    /** Horizontal distance component between hexagons (compared to total width of a hexagon = 1.0) */
    private static final double DIST_H = 0.75;
    /** Vertical distance between hexagons */
    private static final double DIST_V = Math.sqrt(3) / 2.0;
    
    private Random rnd;
    
    public SimplexGenerator() {
        this(new Random());
    }
    
    public SimplexGenerator(Random rnd) {
        this.rnd = rnd;
    }
    
    @Override
    public String getName() {
        return "Generator.Simplex"; //$NON-NLS-1$
    }

    @Override
    public String getTooltip() {
        return "Generator.Simplex.toolTip"; //$NON-NLS-1$
    }

    @Override public void generate(int hilliness, int width, int height, int[][] elevationMap) {
        double noiseStartX = rnd.nextDouble() * 1000000;
        double noiseStartY = rnd.nextDouble() * 1000000;
        double noiseScale = (200.0 + rnd.nextDouble() * 30.0) / (4.0 + hilliness / 5.0);
        hilliness = Math.max(hilliness, 1);
        
        for(int w = 0; w < width; ++ w) {
            for(int h = 0; h < height; ++ h) {
                double x = DIST_H * w;
                double y = DIST_V * (2 * h + (w & 1)) / 2.0;
                // SimplexNoise.noiseOctaves(..., 6) is between -6 and +6, most values are in the [-2, 2] range
                double val = SimplexNoise.noiseOctaves(
                        x / noiseScale + noiseStartX,
                        y / noiseScale + noiseStartY, 6) + 0.5;
                if(val < 0) {
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
