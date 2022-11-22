/*
 * Copyright (c) 2018-2022 - The MegaMek Team. All Rights Reserved.
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
package megamek.utilities.xml;

import megamek.common.annotations.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.xml.sax.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import java.io.InputStream;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.StringJoiner;
import java.util.UUID;

public class MMXMLUtility {
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
    public static XMLReader createSafeXMLReader() {
        if (SAX_PARSER_FACTORY == null) {
            SAXParserFactory spf = SAXParserFactory.newInstance();
            try {
                spf.setFeature("http://xml.org/sax/features/external-general-entities", false);
                spf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
                spf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            } catch (SAXNotRecognizedException | SAXNotSupportedException ex) {
                throw new AssertionError("SAX implementation does not recognize or support the features we want to disable", ex);
            } catch (ParserConfigurationException ex) {
                throw new AssertionError(ex); // Only if we messed up the CFG above
            }
            SAX_PARSER_FACTORY = spf;
        }

        try {
            return SAX_PARSER_FACTORY.newSAXParser().getXMLReader();
        } catch (ParserConfigurationException | SAXException ex) {
            throw new AssertionError(ex);
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
    //region Open Tag
    /**
     * This writes an open XML tag
     * @param pw the PrintWriter to use
     * @param indent the indent to write at
     * @param name the name of the XML tag
     */
    public static void writeSimpleXMLOpenTag(final PrintWriter pw, final int indent,
                                             final String name) {
        writeSimpleXMLOpenTag(pw, indent, name, null, null, null, null);
    }

    /**
     * This writes an open XML tag, with the possible addition of an attribute and its value
     * @param pw the PrintWriter to use
     * @param indent the indent to write at
     * @param name the name of the XML tag
     * @param attributeName the attribute to write as part of the XML tag
     * @param attributeValue the value of the attribute
     */
    public static <T> void writeSimpleXMLOpenTag(final PrintWriter pw, final int indent,
                                                 final String name, final String attributeName,
                                                 final T attributeValue) {
        writeSimpleXMLOpenTag(pw, indent, name, attributeName, attributeValue, null, null);
    }

    /**
     * This writes an open XML tag, with the possible addition of a class attribute and its class
     * @param pw the PrintWriter to use
     * @param indent the indent to write at
     * @param name the name of the XML tag
     * @param classAttribute the class attribute to write as part of the tag
     * @param c the class to write as part of the tag
     */
    public static void writeSimpleXMLOpenTag(final PrintWriter pw, final int indent,
                                             final String name, final String classAttribute,
                                             final Class<?> c) {
        writeSimpleXMLOpenTag(pw, indent, name, null, null, classAttribute, c);
    }

    /**
     * This writes an open XML tag, with the possible addition of an attribute and its value
     * and/or the possible addition of a class attribute and its class
     * @param pw the PrintWriter to use
     * @param indent the indent to write at
     * @param name the name of the XML tag
     * @param attributeName the attribute to write as part of the XML tag
     * @param attributeValue the value of the attribute
     * @param classAttribute the class attribute to write as part of the tag
     * @param c the class to write as part of the tag
     */
    public static <T> void writeSimpleXMLOpenTag(final PrintWriter pw, final int indent,
                                                 final String name,
                                                 final @Nullable String attributeName,
                                                 @Nullable T attributeValue,
                                                 final @Nullable String classAttribute,
                                                 final @Nullable Class<?> c) {
        final boolean hasValue = attributeValue != null;
        final boolean hasClass = c != null;
        pw.print(indentStr(indent) + '<' + name);
        if (hasValue) {
            pw.print(' ' + attributeName + "=\"" + escape(attributeValue.toString()) + '"');
        }

        if (hasClass) {
            pw.print(' ' + classAttribute + "=\"" + c.getName() + '"');
        }
        pw.print(">\n");
    }
    //endregion Open Tag

    //region Simple XML Tag
    /**
     * This writes a UUID or an array of UUIDs to file
     * @param pw the PrintWriter to use
     * @param indent the indent to write at
     * @param name the name of the XML tag
     * @param values the UUID or UUID[] to write to XML
     */
    public static void writeSimpleXMLTag(final PrintWriter pw, final int indent, final String name,
                                         final UUID... values) {
        if (values.length > 0) {
            final StringJoiner stringJoiner = new StringJoiner(",");
            for (final UUID value : values) {
                if (value != null) {
                    stringJoiner.add(value.toString());
                }
            }

            if (!stringJoiner.toString().isBlank()) {
                pw.println(indentStr(indent) + '<' + name + '>' + stringJoiner + "</" + name + '>');
            }
        }
    }

    /**
     * This writes a LocalDate or an array of LocalDates to file
     * @param pw the PrintWriter to use
     * @param indent the indent to write at
     * @param name the name of the XML tag
     * @param values the LocalDate or LocalDate[] to write to XML
     */
    public static void writeSimpleXMLTag(final PrintWriter pw, final int indent, final String name,
                                         final LocalDate... values) {
        if (values.length > 0) {
            final StringJoiner stringJoiner = new StringJoiner(",");
            for (final LocalDate value : values) {
                if (value != null) {
                    stringJoiner.add(value.toString());
                }
            }

            if (!stringJoiner.toString().isBlank()) {
                pw.println(indentStr(indent) + '<' + name + '>' + stringJoiner + "</" + name + '>');
            }
        }
    }

    /**
     * This writes a collection of objects that are saved based on their toString value.
     * Examples are UUID and LocalDate
     * @param pw the PrintWriter to use
     * @param indent the indent to write at
     * @param name the name of the XML tag
     * @param values the collection to write to XML
     */
    public static void writeSimpleXMLTag(final PrintWriter pw, final int indent, final String name,
                                         final Collection<?> values) {
        if (!values.isEmpty()) {
            final StringJoiner stringJoiner = new StringJoiner(",");
            values.forEach(v -> stringJoiner.add(v.toString()));
            pw.println(indentStr(indent) + '<' + name + '>' + escape(stringJoiner.toString()) + "</" + name + '>');
        }
    }

    /**
     * This writes a String or an array of Strings to file
     * @param pw the PrintWriter to use
     * @param indent the indent to write at
     * @param name the name of the XML tag
     * @param values the String or String[] to write to XML
     */
    public static void writeSimpleXMLTag(final PrintWriter pw, final int indent, final String name,
                                         final String... values) {
        writeSimpleXMLAttributedTag(pw, indent, name, null, null, values);
    }

    /**
     * This writes a String or an array of Strings to file, with an the possible addition of an
     * attribute and its value
     * @param pw the PrintWriter to use
     * @param indent the indent to write at
     * @param name the name of the XML tag
     * @param attributeName the attribute to write as part of the XML tag
     * @param attributeValue the value of the attribute
     * @param values the String or String[] to write to XML
     */
    public static <T> void writeSimpleXMLAttributedTag(final PrintWriter pw, final int indent,
                                                       final String name,
                                                       final @Nullable String attributeName,
                                                       final @Nullable T attributeValue,
                                                       final String... values) {
        if (values.length > 0) {
            final boolean hasAttribute = attributeValue != null;
            pw.print(indentStr(indent) + '<' + name);
            if (hasAttribute) {
                pw.print(' ' + attributeName + "=\"" + escape(attributeValue.toString()) + '"');
            }
            pw.print('>' + escape(StringUtils.join(values, ',')) + "</" + name + ">\n");
        }
    }

    /**
     * This writes an int or an array of ints to file
     * @param pw the PrintWriter to use
     * @param indent the indent to write at
     * @param name the name of the XML tag
     * @param values the int or int[] to write to XML
     */
    public static void writeSimpleXMLTag(final PrintWriter pw, final int indent, final String name,
                                         final int... values) {
        if (values.length > 0) {
            pw.println(indentStr(indent) + '<' + name + '>' + StringUtils.join(values, ',') + "</" + name + '>');
        }
    }

    /**
     * This writes a boolean or an array of booleans to file
     * @param pw the PrintWriter to use
     * @param indent the indent to write at
     * @param name the name of the XML tag
     * @param values the boolean or boolean[] to write to XML
     */
    public static void writeSimpleXMLTag(final PrintWriter pw, final int indent, final String name,
                                         final boolean... values) {
        if (values.length > 0) {
            final StringJoiner stringJoiner = new StringJoiner(",");
            for (final boolean value : values) {
                stringJoiner.add(Boolean.toString(value));
            }
            pw.println(indentStr(indent) + '<' + name + '>' + stringJoiner + "</" + name + '>');
        }
    }

    /**
     * This writes a long or an array of longs to file
     * @param pw the PrintWriter to use
     * @param indent the indent to write at
     * @param name the name of the XML tag
     * @param values the long or long[] to write to XML
     */
    public static void writeSimpleXMLTag(final PrintWriter pw, final int indent, final String name,
                                         final long... values) {
        if (values.length > 0) {
            pw.println(indentStr(indent) + '<' + name + '>' + StringUtils.join(values, ',') + "</" + name + '>');
        }
    }

    /**
     * This writes a double or an array of doubles to file
     * @param pw the PrintWriter to use
     * @param indent the indent to write at
     * @param name the name of the XML tag
     * @param values the double or double[] to write to XML
     */
    public static void writeSimpleXMLTag(final PrintWriter pw, final int indent, final String name,
                                         final double... values) {
        if (values.length > 0) {
            pw.println(indentStr(indent) + '<' + name + '>' + StringUtils.join(values, ',') + "</" + name + '>');
        }
    }
    //endregion Simple XML Tag

    //region Close Tag
    /**
     * This writes a XML close tag to file
     * @param pw the PrintWriter to use
     * @param indent the indent to write at
     * @param name the name of the XML tag
     */
    public static void writeSimpleXMLCloseTag(final PrintWriter pw, final int indent,
                                              final String name) {
        pw.println(indentStr(indent) + "</" + name + '>');
    }
    //endregion Close Tag

    /**
     * @param level the level to indent up to
     * @return a string containing level tab indents
     */
    public static String indentStr(final int level) {
        return (level < INDENTS.length) ? INDENTS[level] : StringUtils.repeat('\t', level);
    }

    /**
     * Formats a Date suitable for writing to an XML node.
     * @param date The date to format for XML.
     * @return A String suitable for writing a date to an XML node.
     */
    @Deprecated
    public static String saveFormattedDate(final LocalDate date) {
        return date.toString(); // ISO-8601
    }

    /**
     * Escapes a string to store in an XML element.
     * @param string The string to be encoded
     * @return An encoded copy of the string
     */
    public static String escape(final String string) {
        return StringEscapeUtils.escapeXml10(string);
    }
    //endregion XML Writing

    //region XML Parsing
    /**
     * This parses an collection of UUIDs
     * @param values  the XML text to parse containing a comma separated UUID collection
     * @param ids the collection to load the UUIDs into
     */
    public static void parseUUIDCollection(final String values, final Collection<UUID> ids) {
        for (final String value : values.split(",")) {
            ids.add(UUID.fromString(value));
        }
    }

    /**
     * Parse a date from an XML node's content.
     * @param value The date from an XML node's content.
     * @return The Date retrieved from the XML node content.
     */
    public static LocalDate parseDate(final String value) throws DateTimeParseException {
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
            default:
                break;
        }

        final int firstSpace = value.indexOf(' ');
        if (firstSpace >= 0) {
            return LocalDate.parse(value.substring(0, firstSpace));
        } else if (value.indexOf('-') < 0) {
            return LocalDate.parse(value, DateTimeFormatter.ofPattern("yyyyMMdd"));
        } else { // default parsing
            return LocalDate.parse(value);
        }
    }

    /**
     * This parses an collection of LocalDates
     * @param values the XML text to parse containing a comma separated LocalDate collection
     * @param dates the collection to load the LocalDates into
     */
    public static void parseDateCollection(final String values, final Collection<LocalDate> dates) {
        for (final String value : values.split(",")) {
            dates.add(parseDate(value));
        }
    }

    /**
     * @param value the XML text to parse containing a single String
     * @return the parsed String
     */
    public static String parseString(final String value) {
        return unEscape(value);
    }

    /**
     * @param value the XML text to parse containing a comma spaced String array
     * @return the parsed String Array
     */
    public static String[] parseStringArray(final String value) {
        return unEscape(value).split(",");
    }

    /**
     * @param value the XML text to parse containing a comma spaced int array
     * @return the parsed int Array
     */
    public static int[] parseIntArray(final String value) {
        return Arrays.stream(value.split(",")).mapToInt(Integer::parseInt).toArray();
    }

    /**
     * @param value the XML text to parse containing a comma spaced boolean array
     * @return the parsed boolean Array
     */
    public static boolean[] parseBooleanArray(final String value) {
        final String[] values = value.split(",");
        final boolean[] booleans = new boolean[values.length];
        for (int i = 0; i < values.length; i++) {
            booleans[i] = Boolean.parseBoolean(values[i]);
        }
        return booleans;
    }

    /**
     * @param value the XML text to parse containing a comma spaced long array
     * @return the parsed long Array
     */
    public static long[] parseLongArray(final String value) {
        return Arrays.stream(value.split(",")).mapToLong(Long::parseLong).toArray();
    }

    /**
     * @param value the XML text to parse containing a comma spaced double array
     * @return the parsed double Array
     */
    public static double[] parseDoubleArray(final String value) {
        return Arrays.stream(value.split(",")).mapToDouble(Double::parseDouble).toArray();
    }

    /**
     * Unescape... well, it reverses escaping...
     */
    public static String unEscape(final String string) {
        return StringEscapeUtils.unescapeXml(string);
    }
    //endregion XML Parsing
}
