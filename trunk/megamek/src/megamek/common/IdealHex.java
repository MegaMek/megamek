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
 * Represents a hex, not in the game but in an ideal coordinate system.  Used 
 * for some calcuations.
 */
public class IdealHex 
{
    // used for turns()
    public static final int    LEFT            = 1;
    public static final int    STRAIGHT        = 0;
    public static final int    RIGHT           = -1;
    
    private final double XCONST = Math.tan(Math.PI / 6.0);
            
    public double[] x = new double[6];
    public double[] y = new double[6];
    public double cx;
    public double cy;
            
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
        x[3] = x[2];
        x[4] = x[1];
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
    public boolean isIntersectedBy(double x0, double y0, 
                                   double x1, double y1) {
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
     * Tests whether a line intersects a point or the point passes
     * to the left or right of the line.
     *
     * Deals with floating point imprecision.  Thx deadeye00 (Derek Evans)
     */
    public static int turns(double x0, double y0, double x1, double y1, 
                             double x2, double y2) {
        final double cross = (x1 - x0) * (y2 - y0) - (x2 - x0) * (y1 - y0);
        return ((cross > 0.000001) ? LEFT : ((cross < -0.000001) ? RIGHT : STRAIGHT));
    }

}
 