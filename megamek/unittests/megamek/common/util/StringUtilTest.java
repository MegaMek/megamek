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
    public void testStringComparator() {
        assertTrue(StringUtil.stringComparator().compare("a", "a") == 0);
        assertTrue(StringUtil.stringComparator().compare("a", "b") < 0);
        assertTrue(StringUtil.stringComparator().compare("b", "a") > 0);
        assertTrue(StringUtil.stringComparator().compare("a", "aa") < 0);
    }

    @Test
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
    public void testMakeLength() {

        try {
            StringUtil.makeLength(null, 5);
            fail();
        } catch (@SuppressWarnings("unused") NullPointerException e) {
            // expected
        }

        assertEquals("     ", StringUtil.makeLength("",5));
        
        assertEquals("1    ", StringUtil.makeLength("1",5,false));
        assertEquals("    1", StringUtil.makeLength("1",5,true));
        
        assertEquals("1    ", StringUtil.makeLength("1",5));
        assertEquals("    1", StringUtil.makeLength(1,5));

        assertEquals("12345", StringUtil.makeLength("12345",5));
        assertEquals("123..", StringUtil.makeLength("123456",5));
        assertEquals("123..", StringUtil.makeLength(123456,5));

        assertEquals("",   StringUtil.makeLength("", 0));
        assertEquals(" ",  StringUtil.makeLength("", 1));
        assertEquals("  ", StringUtil.makeLength("", 2));
        assertEquals("1",  StringUtil.makeLength("1", 1));
        assertEquals("1 ", StringUtil.makeLength("1", 2));
        assertEquals("12", StringUtil.makeLength("12", 2));
        assertEquals("..", StringUtil.makeLength("123", 2));
        assertEquals("123", StringUtil.makeLength("123", 3));
        assertEquals("1..", StringUtil.makeLength("1234", 3));

        assertEquals(69, StringUtil.makeLength("", 69).length());
        
        try {
            StringUtil.makeLength("", -1);
            fail();
        } catch (@SuppressWarnings("unused") StringIndexOutOfBoundsException e) {
            // expected
        }

        try {
            StringUtil.makeLength("1", 0);
            fail();
        } catch (@SuppressWarnings("unused") StringIndexOutOfBoundsException e) {
            // expected
        }

        try {
            StringUtil.makeLength("12", 1);
            fail();
        } catch (@SuppressWarnings("unused") StringIndexOutOfBoundsException e) {
            // expected
        }

        try {
            StringUtil.makeLength("", 70);
            fail();
        } catch (@SuppressWarnings("unused") StringIndexOutOfBoundsException e) {
            // expected
        }
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
    public void testIsNullOrEmpty() {
        assertTrue(StringUtil.isNullOrEmpty((String) null));
        assertTrue(StringUtil.isNullOrEmpty((StringBuilder) null));
        assertTrue(StringUtil.isNullOrEmpty(""));
        assertTrue(StringUtil.isNullOrEmpty(new StringBuilder("")));
        assertTrue(StringUtil.isNullOrEmpty(" "));
        assertTrue(StringUtil.isNullOrEmpty(new StringBuilder(" ")));
        assertTrue(StringUtil.isNullOrEmpty("\t"));
        assertTrue(StringUtil.isNullOrEmpty(new StringBuilder("\t")));
        assertTrue(StringUtil.isNullOrEmpty("\u0000")); // nul is whitespace
        assertTrue(StringUtil.isNullOrEmpty(new StringBuilder("\u0000")));
        assertTrue(StringUtil.isNullOrEmpty("\r\n\t "));
        assertTrue(StringUtil.isNullOrEmpty(new StringBuilder("\r\n\t ")));

        assertFalse(StringUtil.isNullOrEmpty("a"));
        assertFalse(StringUtil.isNullOrEmpty(" a "));
        assertFalse(StringUtil.isNullOrEmpty("\u00a0")); // nbsp is not considered whitespace
    }

    @Test
    public void testMakeXmlSafe() {
        assertEquals("", StringUtil.makeXmlSafe(null));
        assertEquals("", StringUtil.makeXmlSafe(""));

        assertEquals("", StringUtil.makeXmlSafe(" "));
        assertEquals("", StringUtil.makeXmlSafe("\t"));
        assertEquals("", StringUtil.makeXmlSafe("\u0000"));

        assertEquals(" a ",   StringUtil.makeXmlSafe(" a "));
        assertEquals("\ta\t", StringUtil.makeXmlSafe("\ta\t"));
        assertEquals(" a ",   StringUtil.makeXmlSafe("\u0000a\u0000"));
        
        assertEquals("&amp;",  StringUtil.makeXmlSafe("&"));
        assertEquals("&lt;", StringUtil.makeXmlSafe("<"));
        assertEquals("&gt;", StringUtil.makeXmlSafe(">"));

        assertEquals("&amp;quote;", StringUtil.makeXmlSafe("\"")); // don't know how to highlight this, but
                                                                   // result is messed-up twice
                                                                   // (xml's &quot; has no trailing "e" plus there is an extra &amp;) 

        assertEquals("&amp;",    StringUtil.makeXmlSafe("&amp;"));
        assertEquals("&amp;gt;", StringUtil.makeXmlSafe("&gt;"));
    }

    @Test
    public void testIsNumeric() {
        assertTrue(StringUtil.isNumeric("0"));
        assertTrue(StringUtil.isNumeric("0."));
        assertTrue(StringUtil.isNumeric(".0"));
        assertTrue(StringUtil.isNumeric("+0"));
        assertTrue(StringUtil.isNumeric("-0"));
        assertTrue(StringUtil.isNumeric("-0e-0"));
        assertTrue(StringUtil.isNumeric("-0E-0"));
        assertTrue(StringUtil.isNumeric("Infinity"));
        assertTrue(StringUtil.isNumeric("+Infinity"));
        assertTrue(StringUtil.isNumeric("-Infinity"));
        assertTrue(StringUtil.isNumeric("NaN"));
    }

    @Test
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
