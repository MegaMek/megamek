/*
 * MegaMek - Copyright (C) 2003 Ben Mazur (bmazur@sev.org)
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

package megamek.common.util;

import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.Vector;

import megamek.common.preference.PreferenceManager;

public class StringUtil {

    // XML Escape Strings.
    private static final String ESC_QUOTE = "&quote;";
    private static final String ESC_APOSTROPHE = "&apos;";
    private static final String ESC_AMPERSAND = "&amp;";
    private static final String ESC_LESS_THAN = "&lt;";
    private static final String ESC_GREATER_THAN = "&gt;";

    public static Vector<String> splitString(String s, String divider) {
        if (s == null || s.equals("")) { //$NON-NLS-1$
            return new Vector<String>();
        }

        Vector<String> v = new Vector<String>();
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

    public static Comparator<String> stringComparator() {
        return new Comparator<String>() {
            public int compare(String s1, String s2) {
                return s1.compareTo(s2);
            }
        };
    }

    /**
     * Determine the <code>boolean</code> value of the given
     * <code>String</code>. Treat all <code>null</code> values as
     * <code>false</code>. The default is <code>false</code>. This ensures
     * the <code>String</code> will always be parsed against the English
     * "true"
     * 
     * @param input - the <code>String</code> to be evaluated. This value may
     *            be <code>null</code>.
     * @return The <code>boolean</code> equivalent of the input.
     */
    public static boolean parseBoolean(String input) {
        if (null == input) {
            return false;
        } else if (input.equalsIgnoreCase("true")) { //$NON-NLS-1$
            return true;
        }
        return false;
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
     * @param s the string to pad
     * @param n the desired length of the resultant string
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
     * Inserts a date/time stamp into the given filename string just before the
     * last period. If there is no period in the filename, the stamp is added to
     * the end. The format of the stamp is dictated by the client option
     * "StampFormat", which must use the same formatting as Java's
     * SimpleDateFormat class.
     * 
     * @param filename the String containing the filename (with extension)
     * @return the filname with date/time stamp added
     */
    public static String addDateTimeStamp(String filename) {
        SimpleDateFormat formatter = new SimpleDateFormat(PreferenceManager
                .getClientPreferences().getStampFormat());
        Date current = new Date();
        if (filename.lastIndexOf(".") == -1) {
            return filename + formatter.format(current);
        }
        return filename.substring(0, filename.lastIndexOf("."))
                + formatter.format(current)
                + filename.substring(filename.lastIndexOf("."));
    }

    /**
     * Returns TRUE if the passed in text is either a NULL value or is an empty string.
     *
     * @param text  The string to be evalutated.
     * @return
     */
    public static boolean isNullOrEmpty(String text) {
        return (text == null) || (text.trim().isEmpty());
    }

    /**
     * Takes in a string, and replaces all XML special characters with the approprate
     * escape strings.
     *
     * @param text The text to be made XML safe.
     * @return
     */
    public static String makeXmlSafe(String text) {
        if (StringUtil.isNullOrEmpty(text)) return "";

        text = text.replace(ESC_AMPERSAND, "&");
        text = text.replace("\"", ESC_QUOTE);
        text = text.replace("&", ESC_AMPERSAND);
        text = text.replace("'", ESC_APOSTROPHE);
        text = text.replace("<", ESC_LESS_THAN);
        text = text.replace(">", ESC_GREATER_THAN);
        text = text.replace("\u0000", " ");

        return text;
    } //public static String makeXmlSafe(String text)

    /**
     * Returns TRUE if the passed string is a valid number.
     *
     * @param number The {@link String} to be evaluated.
     * @return TRUE if the value can be parsed to a {@link Double} without throwing a {@link NumberFormatException}.
     */
    public static boolean isNumeric(String number) {
        if (isNullOrEmpty(number)) {
            return false;
        }
        try {
            Double.parseDouble(number);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        } //catch (NumberFormatException ex)
    }

    /**
     * Returns TRUE if the passed string is a positive integer value.
     *
     * @param number The {@link String} to be evaluated.
     * @return TRUE if the value can be parsed to an {@link Integer} without throwing a {@link NumberFormatException}
     *         and the parsed value is greater than or equal to zero.
     */
    public static boolean isPositiveInteger(String number) {
        if (isNullOrEmpty(number)) {
            return false;
        }
        try {
            return Integer.parseInt(number) >= 0;
        } catch (NumberFormatException ex) {
            return false;
        }
    }
}
