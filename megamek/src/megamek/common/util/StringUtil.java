/*
 * Copyright (C) 2003 Ben Mazur (bmazur@sev.org)
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

package megamek.common.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Vector;

import megamek.codeUtilities.StringUtility;
import megamek.common.preference.PreferenceManager;

public class StringUtil {
    public static Vector<String> splitString(String s, String divider) {
        if ((s == null) || s.isBlank()) {
            return new Vector<>();
        }

        Vector<String> v = new Vector<>();
        int oldIndex = 0;
        int newIndex = s.indexOf(divider);

        while (newIndex != -1) {
            String sub = s.substring(oldIndex, newIndex);
            v.addElement(sub);
            oldIndex = newIndex + 1;
            newIndex = s.indexOf(divider, oldIndex);
        }

        if (oldIndex != s.length()) {
            String sub = s.substring(oldIndex);
            v.addElement(sub);
        }

        return v;
    }

    private static final String SPACES = "                                                                     ";

    public static String makeLength(String s, int n) {
        return makeLength(s, n, false);
    }

    public static String makeLength(int s, int n) {
        return makeLength(Integer.toString(s), n, true);
    }

    /**
     * A utility for padding out a string with spaces.
     *
     * @param s             the string to pad
     * @param n             the desired length of the resultant string
     * @param bRightJustify true if the string should be right justified
     */
    public static String makeLength(String s, int n, boolean bRightJustify) {
        int l = s.length();
        if (l == n) {
            return s;
        } else if (l < n) {
            if (bRightJustify) {
                return SPACES.substring(0, n - l) + s;
            }
            return s + SPACES.substring(0, n - l);
        } else {
            return s.substring(0, n - 2) + "..";
        }
    }

    /**
     * Inserts a date/time stamp into the given filename string just before the last period. If there is no period in
     * the filename, the stamp is added to the end. The format of the stamp is dictated by the client option
     * "StampFormat", which must use the same formatting as Java's DateTimeFormatter class.
     *
     * @param filename the String containing the filename (with extension)
     *
     * @return the filename with date/time stamp added
     */
    public static String addDateTimeStamp(String filename) {
        String current = LocalDateTime.now().format(DateTimeFormatter.ofPattern(
              PreferenceManager.getClientPreferences().getStampFormat()));
        if (filename.lastIndexOf(".") == -1) {
            return filename + current;
        } else {
            return filename.substring(0, filename.lastIndexOf("."))
                  + current
                  + filename.substring(filename.lastIndexOf("."));
        }
    }

    /**
     * Returns TRUE if the passed string is a valid number.
     *
     * @param number The {@link String} to be evaluated.
     *
     * @return TRUE if the value can be parsed to a {@link Double} without throwing a {@link NumberFormatException}.
     */
    public static boolean isNumeric(String number) {
        if (StringUtility.isNullOrBlank(number)) {
            return false;
        }
        try {
            Double.parseDouble(number);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    /**
     * Returns TRUE if the passed string is a positive integer value.
     *
     * @param number The {@link String} to be evaluated.
     *
     * @return TRUE if the value can be parsed to an {@link Integer} without throwing a {@link NumberFormatException}
     *       and the parsed value is greater than or equal to zero.
     */
    public static boolean isPositiveInteger(String number) {
        if (StringUtility.isNullOrBlank(number)) {
            return false;
        }
        try {
            return Integer.parseInt(number) >= 0;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    /**
     * Returns TRUE if the passed string is an integer value.
     *
     * @param number The {@link String} to be evaluated.
     *
     * @return TRUE if the value can be parsed to an {@link Integer} without throwing a {@link NumberFormatException}
     *       and the parsed value is greater than or equal to zero.
     */
    public static boolean isInteger(String number) {
        if (StringUtility.isNullOrBlank(number)) {
            return false;
        }
        try {
            Integer.parseInt(number);
            // If we parsed without exception, we are an int
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    /**
     * Returns integer value from the passed string or default value if conversion fails.
     */
    public static int toInt(String s, int i) {
        if (s.isEmpty()) {
            return i;
        }

        try {
            return Integer.parseInt(s);
        } catch (Exception ignored) {
            return i;
        }
    }

    /**
     * Returns float value from the passed string or default value if conversion fails.
     */
    public static float toFloat(String s, float i) {
        if (s.isEmpty()) {
            return i;
        }

        try {
            return Float.parseFloat(s);
        } catch (Exception ignored) {
            return i;
        }
    }

    /**
     * Returns if value is between the start and end
     */
    public static boolean isBetween(double value, String sStart, String sEnd) {
        if (sStart.isEmpty() && sEnd.isEmpty()) {
            return true;
        }

        int iStart = StringUtil.toInt(sStart, Integer.MIN_VALUE);
        int iEnd = StringUtil.toInt(sEnd, Integer.MAX_VALUE);

        return (!(value < iStart)) && (!(value > iEnd));
    }
}
