/*
 * Copyright (c) 2022-2023 - The MegaMek Team. All Rights Reserved.
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
    private MathUtility() {
        // Do nothing Private Constructor
    }

    // region Linear Interpolation
    /**
     * @param min the minimum value
     * @param max the maximum value
     * @param f   location factor between the two points
     * @return integer rounded graphical linear interpolation value between min and
     *         max a f of 0d will return the minimum, a f of 1d will return the
     *         maximum. Otherwise, this will return the rounded integer between the
     *         two points
     */
    public static int lerp(final int min, final int max, final double f) {
        // The order of operations is important here, to not lose precision
        return (int) Math.round(min * (1d - f) + max * f);
    }

    /**
     * @param min the minimum value
     * @param max the maximum value
     * @param f   location factor between the two points
     * @return double graphical linear interpolation value between min and max a f
     *         of 0d will return the minimum, a f of 1d will return the maximum.
     *         Otherwise, this will return the double value between the two points
     */
    public static double lerp(final double min, final double max, final double f) {
        return min * (1d - f) + max * f;
    }

    /**
     * @param min the minimum value
     * @param max the maximum value
     * @param f   location factor between the two points
     * @return float graphical linear interpolation value between min and max a f of
     *         0f will return the minimum, a f of 1f will return the maximum.
     *         Otherwise, this will return the float value between the two points
     */
    public static float lerp(final float min, final float max, final float f) {
        return min * (1f - f) + max * f;
    }

    /**
     * @param min the minimum value
     * @param max the maximum value
     * @param f   location factor between the two points
     * @return long rounded graphical linear interpolation value between min and max
     *         a f of 0d will return the minimum, a f of 1d will return the maximum.
     *         Otherwise, this will return the rounded long between the two points
     */
    public static long lerp(final long min, final long max, final double f) {
        // The order of operations is important here, to not lose precision
        return Math.round(min * (1L - f) + max * f);
    }
    // endregion Linear Interpolation

    // region Clamp
    /**
     * @param value the int value to clamp
     * @param min   the minimum limit
     * @param max   the maximum limit
     * @return The value if it is inside the range given by the limits (inclusive);
     *         the min value if value is below that range and the max value if value
     *         is above that range.
     *         clamp(2, 6, 8) returns 6,
     *         clamp(7, 6, 8) returns 7,
     *         clamp(12, 3, 5) returns 5.
     */
    public static int clamp(final int value, final int min, final int max) {
        return Math.min(Math.max(value, min), max);
    }

    /**
     * @param value the double value to clamp
     * @param min   the minimum limit
     * @param max   the maximum limit
     * @return The value if it is inside the range given by the limits (inclusive);
     *         the min value if value is below that range and the max value if value
     *         is above that range.
     *         clamp(2, 6, 8) returns 6,
     *         clamp(7, 6, 8) returns 7,
     *         clamp(12, 3, 5) returns 5.
     */
    public static double clamp(final double value, final double min, final double max) {
        return Math.min(Math.max(value, min), max);
    }

    /**
     * @param value the float value to clamp
     * @param min   the minimum limit
     * @param max   the maximum limit
     * @return The value if it is inside the range given by the limits (inclusive);
     *         the min value if value is below that range and the max value if value
     *         is above that range.
     *         clamp(2, 6, 8) returns 6,
     *         clamp(7, 6, 8) returns 7,
     *         clamp(12, 3, 5) returns 5.
     */
    public static float clamp(final float value, final float min, final float max) {
        return Math.min(Math.max(value, min), max);
    }

    /**
     * @param value the long value to clamp
     * @param min   the minimum limit
     * @param max   the maximum limit
     * @return The value if it is inside the range given by the limits (inclusive);
     *         the min value if value is below that range and the max value if value
     *         is above that range.
     *         clamp(2, 6.5, 8) returns 6.5,
     *         clamp(7, 6, 8) returns 7,
     *         clamp(12, 3, 5) returns 5.
     */
    public static long clamp(final long value, final long min, final long max) {
        return Math.min(Math.max(value, min), max);
    }
    // endregion Clamp

    // region Clamp01
    /**
     * @param value the int value to clamp
     * @return The value if it is inside the range 0-1;
     *         the 0 value if value is below that range and 1 value if value
     *         is above that range.
     *         clamp01(2) returns 1,
     *         clamp01(-1) returns 0,
     *         clamp01(1) returns 1.
     */
    public static int clamp01(final int value) {
        return Math.min(Math.max(value, 0), 1);
    }

    /**
     * @param value the double value to clamp
     * @return The value if it is inside the range 0-1;
     *         the 0 value if value is below that range and 1 value if value
     *         is above that range.
     *         clamp01(0.1d) returns 0.1d,
     *         clamp01(-1) returns 0d,
     *         clamp01(1) returns 1d.
     */
    public static double clamp01(final double value) {
        return Math.min(Math.max(value, 0d), 1d);
    }

    /**
     * @param value the float value to clamp
     * @return The value if it is inside the range 0-1;
     *         the 0 value if value is below that range and 1 value if value
     *         is above that range.
     *         clamp01(0.4f) returns 0.4f,
     *         clamp01(-1) returns 0f,
     *         clamp01(1) returns 1f.
     */
    public static float clamp01(final float value) {
        return Math.min(Math.max(value, 0f), 1f);
    }

    /**
     * @param value the long value to clamp
     * @return The value if it is inside the range 0-1;
     *         the 0 value if value is below that range and 1 value if value
     *         is above that range.
     *         clamp01(2) returns 1,
     *         clamp01(-1) returns 0,
     *         clamp01(1) returns 1.
     */
    public static long clamp01(final long value) {
        return Math.min(Math.max(value, 0), 1);
    }
    // endregion Clamp

}
