/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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
 * weight class limits and names
 */
public class EntityWeightClass {

    public static final int WEIGHT_LIGHT = 0;
    public static final int WEIGHT_MEDIUM = 1;
    public static final int WEIGHT_HEAVY = 2;
    public static final int WEIGHT_ASSAULT = 3;
    public static final int WEIGHT_SMALL_CRAFT =4;
    public static final int WEIGHT_SMALL_DROP = 5;
    public static final int WEIGHT_MEDIUM_DROP = 6;
    public static final int WEIGHT_LARGE_DROP = 7;
    public static final int WEIGHT_SMALL_WAR = 8;
    public static final int WEIGHT_LARGE_WAR = 9;
    

    private static int[] weightLimits = { 35, 55, 75, 100, 200, 2499, 9999, 100000, 749999, 2500000 };

    public static final int SIZE = weightLimits.length;

    public static int getWeightClass(int tonnage) {
        int i;
        for (i = 0; i < SIZE - 1; i++) {
            if (tonnage <= weightLimits[i])
                break;
        }
        return i;
    }

    public static int getClassLimit(int wClass) {
        if (wClass >= 0 && wClass < SIZE) {
            return weightLimits[wClass];
        }
        throw new IllegalArgumentException("Unknown Weight Class");
    }

    public static String getClassName(int wClass) {
        if (wClass >= 0 && wClass < SIZE) {
            return Messages.getString("EntityWeightClass." + wClass);
        }
        throw new IllegalArgumentException("Unknown Weight Class");
    }

}
