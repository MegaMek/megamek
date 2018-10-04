package megamek.common.util;

import static org.junit.Assert.*;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Vector;

import org.junit.Test;

import megamek.common.preference.PreferenceManager;

@SuppressWarnings({"javadoc","nls"})
public class StringUtilTest {

    @Test
    @SuppressWarnings("deprecation")
    public void testSplitString() {
        // simple cases
        assertEquals(vec("a","b"), StringUtil.splitString("a-b", "-"));
        assertEquals(vec("a","","b"), StringUtil.splitString("a--b", "-"));
        // last elem dropped if empty
        assertEquals(vec(),        StringUtil.splitString("", "-"));
        assertEquals(vec("a"),     StringUtil.splitString("a-", "-"));
        assertEquals(vec("a","b"), StringUtil.splitString("a-b-", "-"));
        // first elem not dropped if empty
        assertEquals(vec("","a"),     StringUtil.splitString("-a", "-"));
        assertEquals(vec("","a","b"), StringUtil.splitString("-a-b", "-"));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testStringComparator() {
        assertTrue(StringUtil.stringComparator().compare("a", "a") == 0);
        assertTrue(StringUtil.stringComparator().compare("a", "b") < 0);
        assertTrue(StringUtil.stringComparator().compare("b", "a") > 0);
        assertTrue(StringUtil.stringComparator().compare("a", "aa") < 0);
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testParseBoolean() {
        assertTrue(StringUtil.parseBoolean("true"));
        assertTrue(StringUtil.parseBoolean("TRUE"));
        assertTrue(StringUtil.parseBoolean("tRuE"));

        assertFalse(StringUtil.parseBoolean(null));
        assertFalse(StringUtil.parseBoolean(""));
        assertFalse(StringUtil.parseBoolean("false"));
        assertFalse(StringUtil.parseBoolean("t"));
        assertFalse(StringUtil.parseBoolean(" true"));
        assertFalse(StringUtil.parseBoolean("true "));
    }

    @Test
    public void testAbbr() {
        assertEquals("", StringUtil.abbr("", 0, ""));
        assertEquals("", StringUtil.abbr("1", 0, "..."));

        assertEquals("",     StringUtil.abbr("1234", 0, ".."));
        assertEquals("1",    StringUtil.abbr("1234", 1, ".."));
        assertEquals("12",   StringUtil.abbr("1234", 2, ".."));
        assertEquals("1..",  StringUtil.abbr("1234", 3, ".."));
        assertEquals("1234", StringUtil.abbr("1234", 4, ".."));
        assertEquals("1234", StringUtil.abbr("1234", 5, ".."));
        
        try {
            StringUtil.abbr(null, -1, "");
            fail();
        } catch (@SuppressWarnings("unused") NullPointerException e) {
            // expected
        }

        try {
            StringUtil.abbr("", -1, "");
            fail();
        } catch (@SuppressWarnings("unused") IllegalArgumentException e) {
            // expected
        }

        try {
            StringUtil.abbr("", 1, null);
            fail();
        } catch (@SuppressWarnings("unused") NullPointerException e) {
            // expected
        }
    }

    @Test
    public void testPad() {
        assertEquals("1234",  StringUtil.pad("1234", 0, true, '-'));
        assertEquals("1234",  StringUtil.pad("1234", 1, true, '-'));
        assertEquals("1234",  StringUtil.pad("1234", 2, true, '-'));
        assertEquals("1234",  StringUtil.pad("1234", 3, true, '-'));
        assertEquals("1234",  StringUtil.pad("1234", 4, true, '-'));
        assertEquals("-1234", StringUtil.pad("1234", 5, true, '-'));

        assertEquals("1234",  StringUtil.pad("1234", 0, false, '-'));
        assertEquals("1234",  StringUtil.pad("1234", 1, false, '-'));
        assertEquals("1234",  StringUtil.pad("1234", 2, false, '-'));
        assertEquals("1234",  StringUtil.pad("1234", 3, false, '-'));
        assertEquals("1234",  StringUtil.pad("1234", 4, false, '-'));
        assertEquals("1234-", StringUtil.pad("1234", 5, false, '-'));

        try {
            StringUtil.pad(null, 2, false, '-');
            fail();
        } catch (@SuppressWarnings("unused") NullPointerException e) {
            // expected
        }
    }

    @Test
    public void testPadAbbr() {
        assertEquals("",      StringUtil.padAbbr("1234", 0, true));
        assertEquals("1",     StringUtil.padAbbr("1234", 1, true));
        assertEquals("12",    StringUtil.padAbbr("1234", 2, true));
        assertEquals("1..",   StringUtil.padAbbr("1234", 3, true));
        assertEquals("1234",  StringUtil.padAbbr("1234", 4, true));
        assertEquals(" 1234", StringUtil.padAbbr("1234", 5, true));

        assertEquals("",      StringUtil.padAbbr("1234", 0, false));
        assertEquals("1",     StringUtil.padAbbr("1234", 1, false));
        assertEquals("12",    StringUtil.padAbbr("1234", 2, false));
        assertEquals("1..",   StringUtil.padAbbr("1234", 3, false));
        assertEquals("1234",  StringUtil.padAbbr("1234", 4, false));
        assertEquals("1234 ", StringUtil.padAbbr("1234", 5, false));
    }


    @Test
    public void testAddDateTimeStamp() {
        String today = new SimpleDateFormat(PreferenceManager.getClientPreferences().getStampFormat()).format(new Date());

        assertEquals(String.format("a%s.ext", today), StringUtil.addDateTimeStamp("a.ext"));
        assertEquals(String.format("a.b%s.ext", today), StringUtil.addDateTimeStamp("a.b.ext"));

        assertEquals(String.format("%s", today), StringUtil.addDateTimeStamp(""));
        assertEquals(String.format("%s.", today), StringUtil.addDateTimeStamp("."));
        assertEquals(String.format(".%s.", today), StringUtil.addDateTimeStamp(".."));

        try {
            StringUtil.addDateTimeStamp(null);
            fail();
        } catch (@SuppressWarnings("unused") NullPointerException e) {
            // expected
        }
    }

    @Test
    public void testIsNullOrBlank() {
        assertTrue(StringUtil.isNullOrBlank(null));
        assertTrue(StringUtil.isNullOrBlank(""));
        assertTrue(StringUtil.isNullOrBlank(" "));
        assertTrue(StringUtil.isNullOrBlank("\u00a0"));
        assertTrue(StringUtil.isNullOrBlank("\r\n\t "));

        assertFalse(StringUtil.isNullOrBlank("\u0000"));
        assertFalse(StringUtil.isNullOrBlank("a"));
        assertFalse(StringUtil.isNullOrBlank(" a "));
    }

    @Test
    public void testEscapeXml10() {
        assertEquals("", StringUtil.escapeXml10(""));
        assertEquals(" \t\r\n", StringUtil.escapeXml10(" \t\r\n"));

        assertEquals("ab", StringUtil.escapeXml10("\u0000a\u0001b\u0002"));
        assertEquals("ab", StringUtil.escapeXml10("\uFFFEab\uFFFF"));

        assertEquals("&amp;",  StringUtil.escapeXml10("&"));
        assertEquals("&lt;",   StringUtil.escapeXml10("<"));
        assertEquals("&gt;",   StringUtil.escapeXml10(">"));
        assertEquals("&quot;", StringUtil.escapeXml10("\""));
        assertEquals("&apos;", StringUtil.escapeXml10("'"));

        assertEquals("&amp;amp;", StringUtil.escapeXml10("&amp;"));
        
        try {
            StringUtil.escapeXml10(null);
            fail();
        } catch (@SuppressWarnings("unused") NullPointerException e) {
            // expected
        }
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testIsNumeric() {
        assertTrue(StringUtil.isNumeric("0"));
        assertTrue(StringUtil.isNumeric("0."));
        assertTrue(StringUtil.isNumeric(".0"));
        assertTrue(StringUtil.isNumeric("+0"));
        assertTrue(StringUtil.isNumeric("-0"));
        assertTrue(StringUtil.isNumeric("-0e-0"));
        assertTrue(StringUtil.isNumeric("-0E-0"));
        
        assertFalse(StringUtil.isNumeric("Infinity"));
        assertFalse(StringUtil.isNumeric("+Infinity"));
        assertFalse(StringUtil.isNumeric("-Infinity"));
        assertFalse(StringUtil.isNumeric("NaN"));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testIsPositiveInteger() {
        assertTrue(StringUtil.isPositiveInteger("1"));
        assertTrue(StringUtil.isPositiveInteger("0"));
        assertTrue(StringUtil.isPositiveInteger("+0"));
        assertTrue(StringUtil.isPositiveInteger("-0"));
        assertFalse(StringUtil.isPositiveInteger("-1"));
        assertFalse(StringUtil.isPositiveInteger(" "));
        assertFalse(StringUtil.isPositiveInteger("a"));
        assertFalse(StringUtil.isPositiveInteger(" 1"));
        assertFalse(StringUtil.isPositiveInteger("1 "));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testIsInteger() {
        assertTrue(StringUtil.isInteger("1"));
        assertTrue(StringUtil.isInteger("0"));
        assertTrue(StringUtil.isInteger("+0"));
        assertTrue(StringUtil.isInteger("-0"));
        assertTrue(StringUtil.isInteger("-1"));
        assertFalse(StringUtil.isInteger(" "));
        assertFalse(StringUtil.isInteger("a"));
        assertFalse(StringUtil.isInteger(" 1"));
        assertFalse(StringUtil.isInteger("1 "));
    }

    @SafeVarargs
    private static <E> Vector<E> vec(E...elems) {
        return new Vector<>(Arrays.asList(elems));
    }

}
