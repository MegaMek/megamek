/*
 * MegaMek - Copyright (C) 2003,2004,2005,2006 Ben Mazur (bmazur@sev.org)
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

// This class is for ranges. It simply has a min/short/med/long ranges

public class RangeType {

    public static final int RANGE_MINIMUM = 0;
    public static final int RANGE_SHORT = 1;
    public static final int RANGE_MEDIUM = 2;
    public static final int RANGE_LONG = 3;
    public static final int RANGE_EXTREME = 4;
    public static final int RANGE_OUT = Integer.MAX_VALUE;

    public int r_min;
    public int r_short;
    public int r_med;
    public int r_long;
    public int r_extreme;

    public RangeType(int r_min, int r_short, int r_med, int r_long,
            int r_extreme) {
        this.r_min = r_min;
        this.r_short = r_short;
        this.r_med = r_med;
        this.r_long = r_long;
        this.r_extreme = r_extreme;

    }

    public RangeType(int r_short, int r_med, int r_long, int r_extreme) {
        this(0, r_short, r_med, r_long, r_extreme);
    }

    public RangeType(int r_short, int r_med, int r_long) {
        this(0, r_short, r_med, r_long, 2 * r_med);
    }

    // returns short/med/long range
    public int getRangeID(int range) {
        if (range <= r_short)
            return RANGE_SHORT;
        else if (range <= r_med)
            return RANGE_MEDIUM;
        else if (range <= r_long)
            return RANGE_LONG;
        else if (range <= r_extreme)
            return RANGE_EXTREME;
        else
            return RANGE_OUT;
    }

    // This quickly returns the minimum range modifier
    public int getMinRangeMod(int range) {
        if (range <= r_min)
            return (r_min - range + 1);
        return 0;
    }

    /**
     * Returns the range bracket a distance falls into.
     * 
     * @param distance - the <code>int</code> distance to the target.
     * @param ranges - the array of <code>int</code> distances of the weapon.
     * @param useExtremeRange - <code>true</code> if the maxtech extreme range
     *            rules should be used. <code>false</code> if the BMRr range
     *            rules are in effect.
     * @return the <code>int</code> constant for the range bracket.
     */
    public static int rangeBracket(int distance, int[] ranges,
            boolean useExtremeRange) {
        int range = RANGE_OUT;

        // Determine the range bracket of the distance.
        if (null == ranges) {
            range = RANGE_OUT;
        } else if (distance > ranges[RANGE_EXTREME]) {
            range = RANGE_OUT;
        } else if (distance > ranges[RANGE_LONG]) {
            if (useExtremeRange) {
                range = RANGE_EXTREME;
            } else {
                range = RANGE_OUT;
            }
        } else if (distance > ranges[RANGE_MEDIUM]) {
            range = RANGE_LONG;
        } else if (distance > ranges[RANGE_SHORT]) {
            range = RANGE_MEDIUM;
        } else if (distance > ranges[RANGE_MINIMUM]) {
            range = RANGE_SHORT;
        } else {
            range = RANGE_MINIMUM;
        }
        // Return the range.
        return range;
    }

}
