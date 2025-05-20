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

import megamek.logging.MMLogger;

public class MathUtility {
    private final static MMLogger LOGGER = MMLogger.create(MathUtility.class);

    private MathUtility() {
        // Do nothing Private Constructor
    }

    // region Linear Interpolation

    /**
     * @param min    the minimum value
     * @param max    the maximum value
     * @param factor location factor between the two points
     *
     * @return integer rounded graphical linear interpolation value between min and max. A factor of 0d will return the
     *       minimum, a factor of 1d will return the maximum. Otherwise, this will return the rounded integer between
     *       the two points
     */
    public static int lerp(final int min, final int max, final double factor) {
        // The order of operations is important here, to not lose precision
        return (int) Math.round(min * (1d - factor) + max * factor);
    }

    /**
     * @param min    the minimum value
     * @param max    the maximum value
     * @param factor location factor between the two points
     *
     * @return double graphical linear interpolation value between min and max. A factor of 0d will return the minimum,
     *       a factor of 1d will return the maximum. Otherwise, this will return the double value between the two
     *       points
     */
    public static double lerp(final double min, final double max, final double factor) {
        return min * (1d - factor) + max * factor;
    }

    /**
     * @param min    the minimum value
     * @param max    the maximum value
     * @param factor location factor between the two points
     *
     * @return float graphical linear interpolation value between min and max. A factor of 0f will return the minimum, a
     *       factor of 1f will return the maximum. Otherwise, this will return the float value between the two points
     */
    public static float lerp(final float min, final float max, final float factor) {
        return min * (1f - factor) + max * factor;
    }

    /**
     * @param min    the minimum value
     * @param max    the maximum value
     * @param factor location factor between the two points
     *
     * @return long rounded graphical linear interpolation value between min and max. A factor of 0d will return the
     *       minimum, a factor of 1d will return the maximum. Otherwise, this will return the rounded long between the
     *       two points
     */
    public static long lerp(final long min, final long max, final double factor) {
        // The order of operations is important here, to not lose precision
        return Math.round(min * (1L - factor) + max * factor);
    }
    // endregion Linear Interpolation

    // region Clamp

    /**
     * @param value the int value to clamp
     * @param min   the minimum limit
     * @param max   the maximum limit
     *
     * @return The value if it is inside the range given by the limits (inclusive); the min value if value is below that
     *       range and the max value if value is above that range.
     *       <ul>
     *           <li>clamp(2, 6, 8) returns 6</li>
     *           <li>clamp(7, 6, 8) returns 7</li>
     *           <li>clamp(12, 3, 5) returns 5</li>
     *       </ul>
     */
    public static int clamp(final int value, final int min, final int max) {
        return Math.min(Math.max(value, min), max);
    }

    /**
     * @param value the double value to clamp
     * @param min   the minimum limit
     * @param max   the maximum limit
     *
     * @return The value if it is inside the range given by the limits (inclusive); the min value if value is below that
     *       range and the max value if value is above that range.
     *       <ul>
     *         <li>clamp(2, 6, 8) returns 6</li>
     *         <li>clamp(7, 6, 8) returns 7</li>
     *         <li>clamp(12, 3, 5) returns 5</li>
     *       </ul>
     */
    public static double clamp(final double value, final double min, final double max) {
        return Math.min(Math.max(value, min), max);
    }

    /**
     * @param value the float value to clamp
     * @param min   the minimum limit
     * @param max   the maximum limit
     *
     * @return The value if it is inside the range given by the limits (inclusive); the min value if value is below that
     *       range and the max value if value is above that range.
     *       <ul>
     *         <li>clamp(2, 6, 8) returns 6</li>
     *         <li>clamp(7, 6, 8) returns 7</li>
     *         <li>clamp(12, 3, 5) returns 5</li>
     *       </ul>
     */
    public static float clamp(final float value, final float min, final float max) {
        return Math.min(Math.max(value, min), max);
    }

    /**
     * @param value the long value to clamp
     * @param min   the minimum limit
     * @param max   the maximum limit
     *
     * @return The value if it is inside the range given by the limits (inclusive); the min value if value is below that
     *       range and the max value if value is above that range.
     *       <ul>
     *         <li>clamp(2, 6, 8) returns 6</li>
     *         <li>clamp(7, 6, 8) returns 7</li>
     *         <li>clamp(12, 3, 5) returns 5</li>
     *       </ul>
     */
    public static long clamp(final long value, final long min, final long max) {
        return Math.min(Math.max(value, min), max);
    }

    /**
     * @param value the long value to clamp
     *
     * @return The value if it is inside the range given by the limits 0.0-1.0 (inclusive); the min value if value is
     * below that
     *       range and the max value if value is above that range.
     *       <ul>
     *         <li>clamp01(1.3) returns 1.0</li>
     *         <li>clamp01(0.3) returns 0.3</li>
     *         <li>clamp01(-5) returns 0</li>
     *       </ul>
     */
    public static double clamp01(double value) {
        return Math.min(1.0, Math.max(0.0, value));
    }

    /**
     * Clamps a double value between the limits of Math.ulp(1.0) and 1.0.
     * Math.ulp is the smallest positive double value that is greater than 0.0.
     *
     * @param value the double value to clamp between Math.ulp(1.0) and 1.0
     *
     * @return The value if it is inside the range given by the limits (inclusive); the min value if value is below that
     *       range and the max value if value is above that range.
     *       <ul>
     *         <li>clamp(0.3) returns 0.3</li>
     *         <li>clamp(7) returns 1</li>
     *         <li>clamp(-4) returns 2.220446049250313E-16</li>
     *       </ul>
     */
    public static double clampUlp1(double value) {
        return Math.min(1.0, Math.max(Math.ulp(1.0), value));
    }
    // endregion Clamp

    /**
     * Utility function to handle parsing strings into Integers and to handle the possible NumberFormatException with
     * logging and to return defaultValue.
     *
     * @param value        String value to parse.
     * @param defaultValue Default value to set if failed to parse.
     *
     * @return The <code>int</code> value or defaultValue.
     */
    public static int parseInt(final String value, int defaultValue) {
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            LOGGER.warn("Can't parse String `{}` into an Integer due to {}", value, e.getMessage());
            return defaultValue;
        }
    }

    /**
     * Attempts to parse the provided string into an integer.
     *
     * <p>If parsing fails, the method defaults to returning {@code 0}. To specify a custom default value in case of
     * failure, use the overloaded {@link #parseInt(String, int)} method.</p>
     *
     * @param value The string to parse. Can be a numeric string or null.
     *
     * @return The integer value parsed from the string, or {@code 0} if parsing fails.
     *
     * @see #parseInt(String, int)
     */
    public static int parseInt(final String value) {
        return parseInt(value, 0);
    }

    /**
     * Utility function to handle parsing strings into Doubles and to handle the possible NumberFormatException with
     * logging and to return defaultValue.
     *
     * @param value        String value to parse.
     * @param defaultValue Default value to set if failed to parse.
     *
     * @return The <code>double</code> value or defaultValue.
     */
    public static double parseDouble(final String value, double defaultValue) {
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }

        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            LOGGER.warn("Can't parse String `{}` into an Double due to {}", value, e.getMessage());
            return defaultValue;
        }
    }

    /**
     * Utility function to handle parsing strings into floats and to handle the possible NumberFormatException with
     * logging and to return defaultValue.
     *
     * @param value        String value to parse.
     * @param defaultValue Default value to set if failed to parse.
     *
     * @return The <code>float</code> value or defaultValue.
     */
    public static float parseFloat(final String value, float defaultValue) {
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }

        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            LOGGER.warn("Can't parse String `{}` into an Float due to {}", value, e.getMessage());
            return defaultValue;
        }
    }

    /**
     * Utility function to handle parsing strings into boolean and to handle the possible NumberFormatException with
     * logging and to return defaultValue.
     *
     * @param value        String value to parse.
     * @param defaultValue Default value to set if failed to parse.
     *
     * @return The <code>boolean</code> value or defaultValue.
     */
    public static boolean parseBoolean(final String value, boolean defaultValue) {
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }

        try {
            return Boolean.parseBoolean(value);
        } catch (NumberFormatException e) {
            LOGGER.warn("Can't parse String `{}` into an Boolean due to {}", value, e.getMessage());
            return defaultValue;
        }
    }
}
