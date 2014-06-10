/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
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

package megamek.common;

/**
 * Represents a hex, not in the game but in an ideal coordinate system. Used for
 * Compute.intervening() calculations and a few others. This ideal hex is 2.0
 * units tall. Tries to keep a cache of IdealHexes requested, as intervening()
 * sure wants a lot of hexes sometimes.
 */
public class IdealHex {
    // used for turns()
    public static final int LEFT = 1;
    public static final int STRAIGHT = 0;
    public static final int RIGHT = -1;

    private static final double XCONST = Math.tan(Math.PI / 6.0);

    public double[] x = new double[6];
    public double[] y = new double[6];
    public double cx;
    public double cy;

    // cache for hexes
    private static IdealHex[] cache = null;
    private static int cacheWidth = 0;
    private static int cacheHeight = 0;

    public IdealHex(Coords c) {
        // determine origin
        double ox = c.x * XCONST * 3;
        double oy = c.y * 2 + (c.isXOdd() ? 1 : 0);

        // center
        cx = ox + (XCONST * 2);
        cy = oy + 1;

        x[0] = ox + XCONST;
        x[1] = ox + (XCONST * 3);
        x[2] = ox + (XCONST * 4);
        x[3] = x[1];
        x[4] = x[0];
        x[5] = ox;

        y[0] = oy;
        y[1] = oy;
        y[2] = cy;
        y[3] = oy + 2;
        y[4] = y[3];
        y[5] = y[2];
    }

    /**
     * Returns true if this hex is intersected by the line
     */
    public boolean isIntersectedBy(double x0, double y0, double x1, double y1) {
        int side1 = turns(x0, y0, x1, y1, x[0], y[0]);
        if (side1 == STRAIGHT) {
            return true;
        }
        for (int i = 1; i < 6; i++) {
            int j = turns(x0, y0, x1, y1, x[i], y[i]);
            if (j == STRAIGHT || j != side1) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tests whether a line intersects a point or the point passes to the left
     * or right of the line. Deals with floating point imprecision. Thx
     * deadeye00 (Derek Evans)
     */
    public static int turns(double x0, double y0, double x1, double y1,
            double x2, double y2) {
        final double cross = (x1 - x0) * (y2 - y0) - (x2 - x0) * (y1 - y0);
        return ((cross > 0.000001) ? LEFT : ((cross < -0.000001) ? RIGHT
                : STRAIGHT));
    }

    /**
     * Ensures that the cache will be at least the specified dimensions. If it
     * is not, a new cache is created. Hopefully this won't happen too much.
     * Must be called at least once before get(), since the initial size is 0.
     */
    public static void ensureCacheSize(int width, int height) {
        if (cacheWidth < width || cacheHeight < height) {
            cache = new IdealHex[width * height];
            cacheWidth = width;
            cacheHeight = height;
        }
    }

    /**
     * Gets a hex from the cache, if it exists in the cache. If the hex is not
     * cached yet, creates it. If the cache is too small, does not resize it.
     */
    public static IdealHex get(Coords coords) {
        if (cache == null || coords.x >= cacheWidth || coords.y >= cacheHeight
                || coords.x < 0 || coords.y < 0) {
            // System.err.println("IdealHex cache miss on " + coords);
            return new IdealHex(coords);
        }
        // okay, check cache
        int index = (coords.y * cacheWidth) + coords.x;
        IdealHex hex = cache[index];
        if (hex != null) {
            return hex;
        }
        hex = new IdealHex(coords);
        cache[index] = hex;
        return hex;
    }
}
