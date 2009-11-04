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


    public static final int WEIGHT_BA_PAL = 0;
    public static final int WEIGHT_BA_LIGHT = 1;
    public static final int WEIGHT_BA_MEDIUM = 2;
    public static final int WEIGHT_BA_HEAVY = 3;
    public static final int WEIGHT_BA_ASSAULT = 4;
    public static final int WEIGHT_LIGHT = 5;
    public static final int WEIGHT_MEDIUM = 6;
    public static final int WEIGHT_HEAVY = 7;
    public static final int WEIGHT_ASSAULT = 8;
    public static final int WEIGHT_SMALL_CRAFT = 9;
    public static final int WEIGHT_SMALL_DROP = 10;
    public static final int WEIGHT_MEDIUM_DROP = 11;
    public static final int WEIGHT_LARGE_DROP = 12;
    public static final int WEIGHT_SMALL_WAR = 13;
    public static final int WEIGHT_LARGE_WAR = 14;


    private static float[] weightLimits = { 0.4f, 0.75f, 1, 1.5f, 2, 35, 55, 75, 100, 200, 2499, 9999, 100000, 749999, 2500000 };

    public static final int SIZE = weightLimits.length;

    public static int getWeightClass(float tonnage) {
        int i;
        for (i = 0; i < SIZE - 1; i++) {
            if (tonnage <= weightLimits[i]) {
                break;
            }
        }
        return i;
    }

    public static float getClassLimit(int wClass) {
        if ((wClass >= 0) && (wClass < SIZE)) {
            return weightLimits[wClass];
        }
        throw new IllegalArgumentException("Unknown Weight Class");
    }

    public static String getClassName(int wClass) {
        if ((wClass >= 0) && (wClass < SIZE)) {
            return Messages.getString("EntityWeightClass." + wClass);
        }
        throw new IllegalArgumentException("Unknown Weight Class");
    }

}
