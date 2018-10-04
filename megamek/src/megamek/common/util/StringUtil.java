/*
 * MegaMek - Copyright (C) 2018 the MegaMek Team
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

import static java.util.Objects.requireNonNull;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Vector;
import java.util.regex.Pattern;

import megamek.common.preference.PreferenceManager;

/**
 * Inevitable string utility class.
 */
public class StringUtil {

    private StringUtil() {
        // no instances
    }

    /**
     * Abbreviates the given string to the given maximum length, using the given
     * ellipsis if possible.
     */
    @SuppressWarnings("nls")
    public static String abbr(String string, int length, String ellipsis) {
        if (string.length() <= length) {
            requireNonNull(ellipsis);
            return string;
        }
        if (length <= 0) {
            requireNonNull(ellipsis);
            if (length == 0) {
                return "";
            } else {
                throw new IllegalArgumentException("negative length");
            }
        }
        int remainingLen = length - ellipsis.length();
        if (remainingLen >= 1) {
            // there is space for at least 1 char of s (eg: "a...")
            return string.substring(0, remainingLen) + ellipsis;
        } else {
            // ellipsis doesn't leave space for any chars of s: omit it
            return string.substring(0, length);
        }
    }
    
    /**
     * Convenience for {@code abbr(s, length, "..")}.
     */
    @SuppressWarnings("nls")
    public static String abbr(String string, int length) {
        return abbr(string, length, "..");
    }

    private static void appendNTimes(StringBuilder sb, char c, int times) {
        for (int i = 0; i < times; i++) {
            sb.append(c);
        }
    }

    /**
     * Adds the given character to the left or to the right of the specified
     * string until its length is at least equal to the specified one.
     */
    public static String pad(String string, int length, boolean left, char paddingChar) {
        if (string.length() == length) {
            return string;
        } else {
            StringBuilder sb = new StringBuilder();
            if (left)  {
                appendNTimes(sb, paddingChar, length - string.length());
                sb.append(string);
            } else {
                sb.append(string);
                appendNTimes(sb, paddingChar, length - string.length());
            }
            return sb.toString();
        }
    }

    /**
     * Convenience for {@code pad(string, length, true, paddingChar)}.
     */
    public static String lpad(String string, int length, char paddingChar) {
        return pad(string, length, true, paddingChar);
    }

    /**
     * Convenience for {@code pad(string, length, false, paddingChar)}.
     */
    public static String rpad(String string, int length, char paddingChar) {
        return pad(string, length, false, paddingChar);
    }

    /**
     * Convenience for {@code lpad(string, length, ' ')}.
     */
    public static String lpad(String string, int length) {
        return pad(string, length, true, ' ');
    }

    /**
     * Convenience for {@code rpad(string, length, ' ')}.
     */
    public static String rpad(String string, int length) {
        return pad(string, length, false, ' ');
    }

    /**
     * Combines {@link #pad(String, int, boolean, char)} and
     * {@link #abbr(String, int, String)} to yield a string of the specified
     * length.  
     */
    public static String padAbbr(String string, int length, boolean left, String ellipsis, char paddingChar) {
        return pad(abbr(string, length, ellipsis), length, left , paddingChar);
    }

    /**
     * Convenience for {@code padAbbr(s, length, left, "..", ' ')}.
     */
    public static String padAbbr(String string, int length, boolean left) {
        return padAbbr(string, length, left, "..", ' '); //$NON-NLS-1$
    }

    /**
     * Convenience for {@code padAbbr(s, length, true)}.
     */
    public static String lpadAbbr(String string, int length) {
        return padAbbr(string, length, true, "..", ' '); //$NON-NLS-1$
    }

    /**
     * Convenience for {@code padAbbr(s, length, false)}. 
     */
    public static String rpadAbbr(String string, int length) {
        return padAbbr(string, length, false, "..", ' '); //$NON-NLS-1$
    }

    private static final int[] NONBREAKING_SPACE_CHARS = {'\u00a0', '\u2007', '\u202f' };
    static { Arrays.sort(NONBREAKING_SPACE_CHARS); }

    /**
     * Tests whether the given code point is whitespace or a non-breaking space.
     * <p>
     * This is equivalent to {@code Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint) }.
     * 
     * @return whether the given code point is whitespace or a non-breaking
     *         space
     * @see Character#isWhitespace(int)
     * @see Character#isSpaceChar(int)
     */
    public static boolean isSpace(int codePoint) {
        return Character.isWhitespace(codePoint)
            || codePoint == 0x00a0
            || codePoint == 0x2007
            || codePoint == 0x202f;
    }

    /**
     * @return whether the given character sequence is null or all spaces.
     * 
     * @see #isSpace(int)
     */
    public static boolean isNullOrBlank(CharSequence text) {
        return text == null
             ? true
             : text.codePoints().allMatch(StringUtil::isSpace);
    }

    private static boolean isXml10AllowedCodePoint(int cp) {
        return cp < 0x20 // control char?
             ? cp == 0x09 || cp == 0x0A || cp == 0x0D // \t \n \r
             : cp <= 0xD7FF || (cp >= 0xE000  && cp <= 0xFFFD)
                            || (cp >= 0x10000 && cp <= 0x10FFFF);
    }

    private static final int[] SPECIAL_CHARS = {'&', '\'', '"', '<', '>' };
    static { Arrays.sort(SPECIAL_CHARS); }

    private static boolean isXml10SpecialCodePoint(int cp) {
        return Arrays.binarySearch(SPECIAL_CHARS, cp) >= 0;
    }

    /**
     * Escapes the characters in a {@code String} using XML entities and strips
     * characters not in: 
     * <pre>{@literal
     * U+0009, U+000A, U+000D,
     * U+0020  - U+D7FF,
     * U+E000  - U+FFFD,
     * U+10000 - U+10FFFF
     * }</pre>
     * that can't be represented in xml 1.0.
     */
    @SuppressWarnings("nls")
    public static String escapeXml10(String string) {
        int i = 0;
        while (true) { // try avoiding copying the string if possible
            if (i >= string.length()) {
                return string;
            }
            char c = string.charAt(i);
            if (!isXml10AllowedCodePoint(c) || isXml10SpecialCodePoint(c)) {
                break;
            }
            i++;
        }
        StringBuilder sb = new StringBuilder(string.substring(0, i));
        string.substring(i).codePoints().filter(StringUtil::isXml10AllowedCodePoint)
        .forEach(cp -> {
             switch (cp) {
                 case '&':  sb.append("&amp;");  break;
                 case '\'': sb.append("&apos;"); break;
                 case '"':  sb.append("&quot;"); break;
                 case '<':  sb.append("&lt;");   break;
                 case '>':  sb.append("&gt;");   break;
                 default:   sb.appendCodePoint(cp);
             }
        });
        return sb.toString();
    }

    /**
     * Adds the current timestamp formatted according to
     * {@code PreferenceManager.getClientPreferences().getStampFormat()}
     * to the given filename.
     * <p> 
     * If the filename has a dot, then the timestamp is inserted right before
     * the last dot (with no separator, eg: "file2018-09-30.ext"), otherwise it
     * is appended to the filename.
     */
    @SuppressWarnings("nls")
    public static String addDateTimeStamp(String filename) {
        String tstamp = new SimpleDateFormat(PreferenceManager.getClientPreferences().getStampFormat()).format(new Date());
        int lastdot = filename.lastIndexOf(".");
        return lastdot < 0
             ? filename + tstamp
             : filename.substring(0, lastdot) + tstamp + filename.substring(lastdot);
    }

    // hic sunt deprecata

    /** @deprecated use {@link String#split(String)} in conjunction with {@link Pattern#quote(String)} instead */
    @Deprecated
    public static Vector<String> splitString(String s, String divider) {
        return (s == null) || s.equals("") //$NON-NLS-1$
             ? new Vector<>()
             : new Vector<>(Arrays.asList(s.split(Pattern.quote(divider))));
    }

    /** @deprecated use {@link Comparator#naturalOrder()} instead */
    @Deprecated
    public static Comparator<String> stringComparator() {
        return Comparator.naturalOrder();
    }

    /** @deprecated use {@link Boolean#parseBoolean(String)} instead */
    @Deprecated
    public static boolean parseBoolean(String input) {
        return Boolean.parseBoolean(input);
    }

    /** @deprecated use {@link #padAbbr(String, int, boolean)} instead */
    @Deprecated
    public static String makeLength(String string, int n, boolean bRightJustify) {
        return padAbbr(string, n, bRightJustify);
    }

    /** @deprecated use {@link #padAbbr(String, int, boolean)} instead */
    @Deprecated
    public static String makeLength(String string, int n) {
        return makeLength(string, n, false);
    }

    /** @deprecated use {@link #padAbbr(String, int, boolean)} instead */
    @Deprecated
    public static String makeLength(int s, int n) {
        return makeLength(Integer.toString(s), n, true);
    }

    /** @deprecated use {@link #isNullOrBlank(CharSequence)} */
    @Deprecated
    public static boolean isNullOrEmpty(String text) {
        return isNullOrBlank(text);
    }

    /** @deprecated use {@link #isNullOrBlank(CharSequence)} */
    @Deprecated
    public static boolean isNullOrEmpty(StringBuilder text) {
        return isNullOrBlank(text);
    }

    /**  @deprecated with no replacement (just try parsing with {@link Integer#parseInt(String)}, handle the {@linkplain NumberFormatException}) and validate the result */
    @Deprecated
    public static boolean isPositiveInteger(String number) {
        try {
            return Integer.parseInt(number) >= 0;
        } catch (@SuppressWarnings("unused") NumberFormatException ex) {
            return false;
        }
    }

    /**  @deprecated with no replacement (just try parsing with {@link Integer#parseInt(String)} and handle the {@linkplain NumberFormatException}) */
    @Deprecated
    public static boolean isInteger(String number) {
        try {
            Integer.parseInt(number);
            return  true;
        } catch (@SuppressWarnings("unused") NumberFormatException ex) {
            return false;
        }
    }

    /**  @deprecated with no replacement (just try parsing with {@link Double#parseDouble(String)}, handle the {@linkplain NumberFormatException} and validate the result through {@link Double#isFinite(double)}) */
    @Deprecated
    public static boolean isNumeric(String number) {
        try {
            return Double.isFinite(Double.parseDouble(number));
        } catch (@SuppressWarnings("unused") NumberFormatException ex) {
            return false;
        }
    }

    /**
     * @deprecated use {@link #escapeXml10(String)} instead. */
    @Deprecated
    @SuppressWarnings("nls")
    public static String makeXmlSafe(String text) {
        return isNullOrBlank(text) ? "" : escapeXml10(text);
    }

}
