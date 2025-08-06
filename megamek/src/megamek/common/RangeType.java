/*
  Copyright (C) 2003, 2004, 2005, 2006 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common;

// This class is for ranges. It simply has a min/short/med/long ranges
public class RangeType {

    public static final int RANGE_MINIMUM = 0;
    public static final int RANGE_SHORT = 1;
    public static final int RANGE_MEDIUM = 2;
    public static final int RANGE_LONG = 3;
    public static final int RANGE_EXTREME = 4;
    public static final int RANGE_LOS = 5;
    public static final int RANGE_OUT = Integer.MAX_VALUE;
    public static final int RANGE_BEARINGS_ONLY_MINIMUM = 1;
    public static final int RANGE_BEARINGS_ONLY_OUT = 5000;

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
        if (range <= r_short) {
            return RANGE_SHORT;
        } else if (range <= r_med) {
            return RANGE_MEDIUM;
        } else if (range <= r_long) {
            return RANGE_LONG;
        } else if (range <= r_extreme) {
            return RANGE_EXTREME;
        } else {
            return RANGE_OUT;
        }
    }

    // This quickly returns the minimum range modifier
    public int getMinRangeMod(int range) {
        return (range > r_min) ? 0 : (r_min - range + 1);
    }

    /**
     * Returns the range bracket a distance falls into.
     *
     * @param distance        - the distance to the target
     * @param ranges          - the array of distances of the weapon
     * @param useExtremeRange - true if extreme range rules should be used
     * @param useLOSRange     - true if LOS range rules should be used
     *
     * @return the constant for the range bracket
     */
    public static int rangeBracket(int distance, int[] ranges,
          boolean useExtremeRange, boolean useLOSRange) {
        return calculateRangeBracket(distance, distance, ranges, useExtremeRange, useLOSRange, false);
    }

    /**
     * Returns the range bracket considering C3 network data.
     *
     * @param c3SpotterDistance - the distance from the spotter to the target
     * @param attackerDistance  - the distance from the attacker to the target
     * @param ranges            - the array of distances of the weapon
     * @param useExtremeRange   - true if extreme range rules should be used
     * @param useLOSRange       - true if LOS range rules should be used
     *
     * @return the constant for the range bracket
     */
    public static int rangeBracketC3(int c3SpotterDistance, int attackerDistance, int[] ranges,
          boolean useExtremeRange, boolean useLOSRange) {
        return calculateRangeBracket(c3SpotterDistance, attackerDistance, ranges, useExtremeRange, useLOSRange, true);
    }

    /**
     * Helper method to calculate range bracket with or without C3 considerations.
     *
     * @param primaryDistance   - the primary distance to check (spotter distance for C3)
     * @param secondaryDistance - the secondary distance (attacker distance for C3)
     * @param ranges            - the array of distances of the weapon
     * @param useExtremeRange   - true if extreme range rules should be used
     * @param useLOSRange       - true if LOS range rules should be used
     * @param isC3              - true if using C3 network calculation rules
     *
     * @return the constant for the range bracket
     */
    private static int calculateRangeBracket(int primaryDistance, int secondaryDistance, int[] ranges,
          boolean useExtremeRange, boolean useLOSRange, boolean isC3) {
        if (null == ranges) {
            return RANGE_OUT;
        }

        if (primaryDistance > ranges[RANGE_EXTREME]) {
            return useLOSRange ? RANGE_LOS : RANGE_OUT;
        }

        if (primaryDistance > ranges[RANGE_LONG]) {
            return useExtremeRange ? RANGE_EXTREME : RANGE_OUT;
        }

        if (primaryDistance > ranges[RANGE_MEDIUM]) {
            return RANGE_LONG;
        }

        if (primaryDistance > ranges[RANGE_SHORT]) {
            return RANGE_MEDIUM;
        }

        if (isC3
              ? (primaryDistance > ranges[RANGE_MINIMUM] || secondaryDistance > ranges[RANGE_MINIMUM])
              : (primaryDistance > ranges[RANGE_MINIMUM])) {
            return RANGE_SHORT;
        }

        return RANGE_MINIMUM;
    }
}
