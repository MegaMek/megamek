/*
 * Copyright (c) 2018, 2020 - The MegaMek Team. All Rights Reserved.
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
package megamek.utils;

import java.io.InputStream;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.StringJoiner;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;

public class MegaMekXmlUtil {
    //region Variable Declarations
    private static DocumentBuilderFactory DOCUMENT_BUILDER_FACTORY;
    private static SAXParserFactory SAX_PARSER_FACTORY;

    private static final String[] INDENTS = new String[] {
            "",
            "\t",
            "\t\t",
            "\t\t\t",
            "\t\t\t\t",
            "\t\t\t\t\t",
            "\t\t\t\t\t\t"
    };
    //endregion Variable Declarations

    /**
     * Creates a DocumentBuilder safe from XML external entities
     * attacks, and XML entity expansion attacks.
     * @return A DocumentBuilder safe to use to read untrusted XML.
     */
    public static DocumentBuilder newSafeDocumentBuilder() throws ParserConfigurationException {
        DocumentBuilderFactory dbf = DOCUMENT_BUILDER_FACTORY;
        if (null == dbf) {
            // At worst we may do this twice if multiple threads
            // hit this method. It is Ok to have more than one
            // instance of the builder factory, as long as it is
            // XXE safe.
            dbf = DocumentBuilderFactory.newInstance();

            //
            // Adapted from: https://www.owasp.org/index.php/XML_External_Entity_(XXE)_Prevention_Cheat_Sheet#JAXP_DocumentBuilderFactory.2C_SAXParserFactory_and_DOM4J
            //
            // "...The JAXP DocumentBuilderFactory setFeature method allows a
            // developer to control which implementation-specific XML processor
            // features are enabled or disabled. The features can either be set
            // on the factory or the underlying XMLReader setFeature method.
            // Each XML processor implementation has its own features that
            // govern how DTDs and external entities are processed."
            //
            // "[disable] these as well, per Timothy Morgan's 2014 paper: 'XML
            // Schema, DTD, and Entity Attacks'"
            dbf.setXIncludeAware(false);
            dbf.setExpandEntityReferences(false);

            // "This is the PRIMARY defense. If DTDs (doctypes) are disallowed,
            // almost all XML entity attacks are prevented"
            String FEATURE = "http://apache.org/xml/features/disallow-doctype-decl";
            dbf.setFeature(FEATURE, true);

            DOCUMENT_BUILDER_FACTORY = dbf;
        }

        return dbf.newDocumentBuilder();
    }

    /**
     * @return a SAX {@linkplain XMLReader} that is safe from external entities and entity expansion attacks.
     *
     * @see "https://www.owasp.org/index.php/XML_External_Entity_(XXE)_Prevention_Cheat_Sheet#JAXB_Unmarshaller"
     */
    @SuppressWarnings(value = "nls")
    public static XMLReader createSafeXMLReader() {
        if (SAX_PARSER_FACTORY == null) {
            SAXParserFactory spf = SAXParserFactory.newInstance();
            try {
                spf.setFeature("http://xml.org/sax/features/external-general-entities", false);
                spf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
                spf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            } catch (SAXNotRecognizedException | SAXNotSupportedException e) {
                throw new AssertionError("SAX implementation does not recognize or support the features we want to disable", e);
            } catch (ParserConfigurationException e) {
                throw new AssertionError(e); // Only if we messed up the CFG above
            }
            SAX_PARSER_FACTORY = spf;
        }
        try {
            return SAX_PARSER_FACTORY.newSAXParser().getXMLReader();
        } catch (ParserConfigurationException e) {
            throw new AssertionError(e); // Only if we messed up the CFG above
        } catch (SAXException e) {
            throw new AssertionError(e); // Whatever - just blow up. :-)
            // As of 2018-11, Xerces does not throw generic SAXExceptions.
            // Yes, SAX was designed when checked exception were all the rage.
        }
    }

    /**
     * @return a {@linkplain Source} for the provided input stream that is safe
     * from external entities and entity expansion attacks.
     *
     * @see "https://www.owasp.org/index.php/XML_External_Entity_(XXE)_Prevention_Cheat_Sheet#JAXB_Unmarshaller"
     */
    public static Source createSafeXmlSource(InputStream inputStream) {
        return new SAXSource(createSafeXMLReader(), new InputSource(inputStream));
    }
    //region XML Writing
    //region Open Indented Line
    public static void writeSimpleXMLOpenIndentedLine(PrintWriter pw1, int indent, String name) {
        writeSimpleXMLOpenIndentedLine(pw1, indent, name, null, null, null, null);
    }

    public static <T> void writeSimpleXMLOpenIndentedLine(PrintWriter pw1, int indent, String name,
                                                          String attr, T val) {
        writeSimpleXMLOpenIndentedLine(pw1, indent, name, attr, val, null, null);
    }

    public static <T> void writeSimpleXMLOpenIndentedLine(PrintWriter pw1, int indent, String name,
                                                          String attr, T val, String classAttr, Class<?> c) {
        final boolean hasVal = val != null;
        final boolean hasClass = c != null;
        pw1.print(indentStr(indent) + "<" + escape(name));
        if (hasVal) {
            pw1.print(" " + attr + "=\"" + val + "\"");
        }

        if (hasClass) {
            pw1.print(" " + classAttr + "=\"" + c.getName() + "\"");
        }
        pw1.print(">\n");
    }
    //endregion Open Indented Line

    //region Simple XML Tag
    public static void writeSimpleXMLTag(PrintWriter pw1, int indent, String name, UUID val) {
        if (val != null) {
            writeSimpleXmlTag(pw1, indent, name, val.toString());
        }
    }

    public static void writeSimpleXMLTag(PrintWriter pw1, int indent, String name, LocalDate val) {
        if (val != null) {
            writeSimpleXmlTag(pw1, indent, name, saveFormattedDate(val));
        }
    }

    public static void writeSimpleXMLTag(PrintWriter pw1, int indent, String name, String... values) {
        if (values.length > 0) {
            pw1.println(indentStr(indent) + "<" + name + ">" + escape(StringUtils.join(values, ',')) + "</" + name + ">");
        }
    }

    public static void writeSimpleXMLTag(PrintWriter pw1, int indent, String name, int... values) {
        if (values.length > 0) {
            pw1.println(indentStr(indent) + "<" + name + ">" + StringUtils.join(values, ',') + "</" + name + ">");
        }
    }

    public static void writeSimpleXMLTag(PrintWriter pw1, int indent, String name, boolean... values) {
        StringJoiner stringJoiner = new StringJoiner(",", "", "");
        for (boolean value : values) {
            stringJoiner.add(Boolean.toString(value));
        }
        pw1.println(indentStr(indent) + "<" + name + ">" + stringJoiner + "</" + name + ">");
    }

    public static void writeSimpleXMLTag(PrintWriter pw1, int indent, String name, long... values) {
        if (values.length > 0) {
            pw1.println(indentStr(indent) + "<" + name + ">" + StringUtils.join(values, ',') + "</" + name + ">");
        }
    }

    public static void writeSimpleXMLTag(PrintWriter pw1, int indent, String name, double... values) {
        if (values.length > 0) {
            pw1.println(indentStr(indent) + "<" + name + ">" + StringUtils.join(values, ',') + "</" + name + ">");
        }
    }

    @Deprecated
    public static void writeSimpleXmlTag(PrintWriter pw1, int indent, String name, UUID val) {
        if (val != null) {
            writeSimpleXmlTag(pw1, indent, name, val.toString());
        }
    }

    @Deprecated
    public static void writeSimpleXmlTag(PrintWriter pw1, int indent, String name, String val) {
        pw1.println(indentStr(indent) + "<" + name + ">" + escape(val) + "</" + name + ">");
    }

    @Deprecated
    public static void writeSimpleXmlTag(PrintWriter pw1, int indent, String name, int val) {
        pw1.println(indentStr(indent) + "<" + name + ">" + val + "</" + name + ">");
    }

    @Deprecated
    public static void writeSimpleXmlTag(PrintWriter pw1, int indent, String name, boolean val) {
        pw1.println(indentStr(indent) + "<" + name + ">" + val + "</" + name + ">");
    }

    @Deprecated
    public static void writeSimpleXmlTag(PrintWriter pw1, int indent, String name, long val) {
        pw1.println(indentStr(indent) + "<" + name + ">" + val + "</" + name + ">");
    }

    @Deprecated
    public static void writeSimpleXmlTag(PrintWriter pw1, int indent, String name, double val) {
        pw1.println(indentStr(indent) + "<" + name + ">" + val + "</" + name + ">");
    }
    //endregion Simple XML Tag

    //region Close Indented Line
    public static void writeSimpleXMLCloseIndentedLine(PrintWriter pw1, int indent, String name) {
        pw1.println(indentStr(indent) + "</" + escape(name) + ">");
    }
    //endregion Close Indented Line

    public static String indentStr(int level) {
        if (level < INDENTS.length) {
            return INDENTS[level];
        } else {
            return StringUtils.repeat('\t', level);
        }
    }

    /**
     * Formats a Date suitable for writing to an XML node.
     * @param date The date to format for XML.
     * @return A String suitable for writing a date to an XML node.
     */
    public static String saveFormattedDate(LocalDate date) {
        return date.toString(); // ISO-8601
    }

    /** Escapes a string to store in an XML element.
     * @param string The string to be encoded
     * @return An encoded copy of the string
     */
    public static String escape(String string) {
        return StringEscapeUtils.escapeXml10(string);
    }
    //endregion XML Writing

    //region XML Parsing
    /**
     * Parse a date from an XML node's content.
     * @param value The date from an XML node's content.
     * @return The Date retrieved from the XML node content.
     */
    public static LocalDate parseDate(String value) throws DateTimeParseException {
        // Accept (truncates): yyyy-MM-dd HH:mm:ss
        // Accept (legacy): YYYYMMDD
        // Accept (preferred): yyyy-MM-dd
        // Accept (assumes first day of month): yyyy-MM
        // Accept (assumes first day of year: yyyy
        // Accept (assumes decade specification, so multiplied by 10 at first day of year): yyy
        switch (value.length()) {
            case 3:
                return LocalDate.ofYearDay(Integer.parseInt(value) * 10, 1);
            case 4:
                return LocalDate.ofYearDay(Integer.parseInt(value), 1);
            case 6:
            case 7:
                return LocalDate.parse(value + "-01");
        }

        int firstSpace = value.indexOf(' ');
        if (firstSpace >= 0) {
            return LocalDate.parse(value.substring(0, firstSpace));
        } else if (value.indexOf('-') < 0) {
            return LocalDate.parse(value, DateTimeFormatter.ofPattern("yyyyMMdd"));
        } else { // default parsing
            return LocalDate.parse(value);
        }
    }

    public static String parseString(String value) {
        return unEscape(value);
    }
    public static String[] parseStringArray(String value) {
        return unEscape(value).split(",");
    }

    public static int[] parseIntArray(String value) {
        final String[] values = value.split(",");
        int[] ints = new int[values.length];
        for (int i = 0; i < values.length; i++) {
            ints[i] = Integer.parseInt(values[i]);
        }
        return ints;
    }

    public static boolean[] parseBooleanArray(String value) {
        final String[] values = value.split(",");
        boolean[] booleans = new boolean[values.length];
        for (int i = 0; i < values.length; i++) {
            booleans[i] = Boolean.parseBoolean(values[i]);
        }
        return booleans;
    }

    public static long[] parseLongArray(String value) {
        final String[] values = value.split(",");
        long[] longs = new long[values.length];
        for (int i = 0; i < values.length; i++) {
            longs[i] = Long.parseLong(values[i]);
        }
        return longs;
    }

    public static double[] parseDoubleArray(String value) {
        final String[] values = value.split(",");
        double[] doubles = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            doubles[i] = Double.parseDouble(values[i]);
        }
        return doubles;
    }

    /**
     * Unescape...well, it reverses escaping...
     */
    public static String unEscape(String string) {
        return StringEscapeUtils.unescapeXml(string);
    }
    //endregion XML Parsing
}
