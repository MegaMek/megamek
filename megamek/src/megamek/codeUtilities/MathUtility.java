/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.codeUtilities;

import java.util.List;

import megamek.logging.MMLogger;

public class MathUtility {
    private final static MMLogger LOGGER = MMLogger.create(MathUtility.class);

    private MathUtility() {
        // Do nothing Private Constructor
    }

    /**
     * Rounds a double value away from zero ("up" for both positive and negative values).
     *
     * <p>For positive values, this method returns the least integer greater than or equal to the value. For negative
     * values, it returns the greatest integer less than or equal to the value (i.e., rounds further away from
     * zero).</p>
     *
     * <p><b>Special cases:</b></p>
     * <ul>
     *   <li>If the input is {@code NaN}, returns {@code 0} and logs a warning.</li>
     *   <li>If the rounded value is out of {@code int} range, returns {@link Integer#MAX_VALUE} or
     *   {@link Integer#MIN_VALUE} as appropriate and logs a warning.</li>
     *   <li>If the input is positive or negative infinity, returns {@link Integer#MAX_VALUE} or
     *   {@link Integer#MIN_VALUE} and logs a warning.</li>
     * </ul>
     *
     * @param value The double value to round.
     *
     * @return The rounded integer value, away from zero.
     *
     * @author Illiani
     * @since 0.50.10
     */
    public static int roundAwayFromZero(double value) {
        if (Double.isNaN(value)) {
            LOGGER.warn("roundAwayFromZero: Cannot round NaN value. Returning 0.");
            return 0;
        }
        if (value == Double.POSITIVE_INFINITY) {
            LOGGER.warn("roundAwayFromZero: Value is positive infinity. Returning Integer.MAX_VALUE.");
            return Integer.MAX_VALUE;
        }
        if (value == Double.NEGATIVE_INFINITY) {
            LOGGER.warn("roundAwayFromZero: Value is negative infinity. Returning Integer.MIN_VALUE.");
            return Integer.MIN_VALUE;
        }

        double roundedValue = value > 0 ? Math.ceil(value) : Math.floor(value);

        try {
            return Math.toIntExact((long) roundedValue);
        } catch (ArithmeticException e) {
            int extreme = value > 0 ? Integer.MAX_VALUE : Integer.MIN_VALUE;
            LOGGER.warn("Can't round '{}' away from zero due to {}. Returning {}.", value, e.getMessage(), extreme);
            return extreme;
        }
    }

    /**
     * Rounds a double value toward zero ("down" for both positive and negative values).
     *
     * <p>For positive values, this method returns the greatest integer less than or equal to the value. For negative
     * values, it returns the least integer greater than or equal to the value (i.e., rounds closer to zero).</p>
     *
     * <p><b>Special cases:</b></p>
     * <ul>
     *   <li>If the input is {@code NaN}, returns {@code 0} and logs a warning.</li>
     *   <li>If the rounded value is out of {@code int} range, returns {@link Integer#MAX_VALUE} or
     *   {@link Integer#MIN_VALUE} as appropriate and logs a warning.</li>
     *   <li>If the input is positive or negative infinity, returns {@link Integer#MAX_VALUE} or
     *   {@link Integer#MIN_VALUE} and logs a warning.</li>
     * </ul>
     *
     * @param value The double value to round.
     *
     * @return The rounded integer value, toward zero.
     *
     * @author Illiani
     * @since 0.50.10
     */
    public static int roundTowardsZero(double value) {
        if (Double.isNaN(value)) {
            LOGGER.warn("roundTowardsZero: Cannot round NaN value. Returning 0.");
            return 0;
        }
        if (value == Double.POSITIVE_INFINITY) {
            LOGGER.warn("roundTowardsZero: Value is positive infinity. Returning Integer.MAX_VALUE.");
            return Integer.MAX_VALUE;
        }
        if (value == Double.NEGATIVE_INFINITY) {
            LOGGER.warn("roundTowardsZero: Value is negative infinity. Returning Integer.MIN_VALUE.");
            return Integer.MIN_VALUE;
        }

        double roundedValue = value > 0 ? Math.floor(value) : Math.ceil(value);

        try {
            return Math.toIntExact((long) roundedValue);
        } catch (ArithmeticException e) {
            int extreme = value > 0 ? Integer.MAX_VALUE : Integer.MIN_VALUE;
            LOGGER.warn("Can't round '{}' towards zero due to {}. Returning {}.", value, e.getMessage(), extreme);
            return extreme;
        }
    }

    /**
     * This is a convenience method for calling {@link #getGaussianAverage(List, double)} with a strictness of 1.0
     * (neutral).
     *
     * @param values the list of integer values to average; must not be {@code null}
     *
     * @return the Gaussian-weighted average as an {@code int}, or {@code 0} if the list is empty
     *
     * @author Illiani
     * @since 0.50.10
     */
    public static int getGaussianAverage(List<Integer> values) {
        return getGaussianAverage(values, 1.0);
    }

    /**
     * Calculates a Gaussian-weighted average from a list of integer values.
     *
     * <p>This method computes a "soft" average that down-weights statistical outliers using a Gaussian
     * (normal-distribution) weighting function. This is useful when a handful of extreme values should not influence
     * the final result as strongly as values clustered near the center of the distribution.</p>
     *
     * <p><b>How It Works</b></p>
     * <ol>
     *   <li>Compute the arithmetic mean of all values.</li>
     *   <li>Compute the standard deviation. This measures how far values typically lie from the mean.</li>
     *   <li>If the standard deviation is zero (all values identical), simply return the mean. This avoids a
     *   divide-by-zero error when standardizing distances.</li>
     *   <li>Apply a configurable strictness factor to the standard deviation. Values less than {@code 1.0} increase
     *   strictness by shrinking the effective deviation (causing outliers to be down-weighted more aggressively).
     *   Values greater than {@code 1.0} reduce strictness.</li>
     *   <li>For each value, compute its standardized distance from the mean and apply a Gaussian weighting function:
     *       {@code weight = exp( -0.5 * ((value - mean) / adjustedDeviation)^2)}. Values closer to the mean receive
     *       weights near {@code 1.0}, while more distant values rapidly approach zero weight.</li>
     *   <li>Return the ratio of the weighted sum of values to the total weight.</li>
     * </ol>
     *
     * <p>The result behaves like a robust average: representative values dominate the calculation, while extreme
     * outliers exert proportionally less influence. This is especially useful when working with mixed-force Battle
     * Value (BV) or unit count arrays where unusually large or unusually small BVs/counts should not skew budgeting
     * logic.</p>
     *
     * <p><b>Strictness Guidelines</b></p>
     * <ul>
     *   <li>{@code strictness < 1.0}: more strict; outliers are strongly suppressed.</li>
     *   <li>{@code strictness = 1.0}: neutral; standard Gaussian weighting (default).</li>
     *   <li>{@code strictness > 1.0}: less strict; outliers retain more influence.</li>
     * </ul>
     *
     * <p>The strictness value should rarely be set below {@code 0.5} or above {@code 2.0}. The default of
     * {@code 1.0} should work well for most distributions.</p>
     *
     * <p>To protect against overflow during conversion to {@code int}, the final result is clamped within
     * {@link Integer#MIN_VALUE} and {@link Integer#MIN_VALUE} using {@link MathUtility#clamp(int, int, int)}.</p>
     *
     * @param values     the list of integer values to average; must not be {@code null}
     * @param strictness how strict the calculations should be. Used to increase or decrease the influence of outliers.
     *
     * @return the Gaussian-weighted average as an {@code int}, or {@code 0} if the list is empty
     *
     * @author Illiani
     * @since 0.50.10
     */
    public static int getGaussianAverage(List<Integer> values, double strictness) {
        if (values.isEmpty()) {
            return 0;
        }

        // Start with the crude mean, this is going to be the middle-ground we use to weight everything
        double total = 0;
        for (int value : values) {
            total += value;
        }
        double crudeMean = total / values.size();

        // Then, we need to calculate standard deviation.
        double varianceTotal = 0;
        for (int value : values) {
            double difference = value - crudeMean;
            varianceTotal += difference * difference;
        }
        double variance = varianceTotal / values.size();
        double baseDeviation = Math.sqrt(variance);

        // Miraculously, all the values are identical (most likely because there is only one value). This guards
        // against divide by 0 errors.
        if (baseDeviation == 0) {
            // We use MathUtility.clamp() to avoid the risk of an overflow when dealing with insanely large player
            // campaigns. This probably isn't necessary, but no reason not to include it.
            return MathUtility.clamp((int) Math.round(crudeMean), Integer.MIN_VALUE, Integer.MAX_VALUE);
        }

        // Below, we define how strict we want the calculations. strictness < 1.0 -> more strict. strictness > 1.0 ->
        // less strict.
        double adjustedDeviation = baseDeviation * strictness;

        // This is where the magic happens.
        // weight(value) = exp( -0.5 * ((value - mean) / adjustedDeviation)^2 )
        double weightedSum = 0;
        double weightTotal = 0;

        for (int value : values) {
            double distanceFromMean = (value - crudeMean) / adjustedDeviation;
            double weight = Math.exp(-0.5 * distanceFromMean * distanceFromMean);
            weightedSum += value * weight;
            weightTotal += weight;
        }

        double gaussianMean = weightedSum / weightTotal;

        // Using Math.min() for the same reason as outlined above.
        return MathUtility.clamp((int) Math.round(gaussianMean), Integer.MIN_VALUE, Integer.MAX_VALUE);
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
     *       below that range and the max value if value is above that range.
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
     * Clamps a double value between the limits of Math.ulp(1.0) and 1.0. Math.ulp is the smallest positive double value
     * that is greater than 0.0.
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
     * Utility function to handle parsing strings into Integers and to handle the possible NumberFormatException with
     * logging and to return defaultValue.
     *
     * @param value        String value to parse.
     * @param defaultValue Default value to set if failed to parse.
     *
     * @return The <code>int</code> value or defaultValue.
     */
    public static long parseLong(final String value, long defaultValue) {
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }

        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            LOGGER.warn("Can't parse String `{}` into an Long due to {}", value, e.getMessage());
            return defaultValue;
        }
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
    public static Double parseDouble(final String value, Double defaultValue) {
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
     * Parses the provided string into a double. If parsing fails, a default value of 0.0 is returned. This method
     * delegates to {@link #parseDouble(String, Double)} with a default value.
     *
     * @param value the string to parse. Can be a numeric string or {@code null}.
     *
     * @return the parsed double value, or 0.0 if parsing fails.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public static double parseDouble(final String value) {
        return parseDouble(value, 0.0);
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
