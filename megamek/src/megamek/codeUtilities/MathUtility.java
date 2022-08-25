/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.codeUtilities;

public class MathUtility {
    //region Linear Interpolation
    /**
     * @return integer rounded linear interpolation value between min and max
     */
    public static int lerp(final int min, final int max, final double f) {
        // The order of operations is important here, to not lose precision
        return (int) Math.round(min * (1d - f) + max * f);
    }

    /**
     * @return double linear interpolation value between min and max
     */
    public static double lerp(final double min, final double max, final double f) {
        // The order of operations is important here, to not lose precision
        return min * (1d - f) + max * f;
    }

    /**
     * @return float linear interpolation value between min and max
     */
    public static float lerp(final float min, final float max, final float f) {
        // The order of operations is important here, to not lose precision
        return min * (1f - f) + max * f;
    }

    /**
     * @return long linear interpolation value between min and max
     */
    public static long lerp(final long min, final long max, final long f) {
        // The order of operations is important here, to not lose precision
        return min * (1L - f) + max * f;
    }
    //endregion Linear Interpolation

    //region Clamp
    /**
     *  @return The value if it is inside the range given by the limits (inclusive);
     *  the min value if value is below that range and the max value if
     *  value is above that range.
     *  clamp(2, 6, 8) returns 6, clamp(7, 6, 8) returns 7, clamp(12, 3, 5) returns 5.
     */
    public static int clamp(final int value, final int min, final int max) {
        return Math.min(Math.max(value, min), max);
    }

    public static double clamp(final double value, final double min, final double max) {
        return Math.min(Math.max(value, min), max);
    }

    public static float clamp(final float value, final float min, final float max) {
        return Math.min(Math.max(value, min), max);
    }

    public static long clamp(final long value, final long min, final long max) {
        return Math.min(Math.max(value, min), max);
    }
    //endregion Clamp
}
