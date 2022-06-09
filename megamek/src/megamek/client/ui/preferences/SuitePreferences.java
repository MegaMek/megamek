/*
 * Copyright (c) 2019-2021 - The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.preferences;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import org.apache.logging.log4j.LogManager;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class SuitePreferences {
    //region Variable Declarations
    private static final String PREFERENCES_TOKEN = "preferences";
    private static final String CLASS_TOKEN = "class";
    private static final String ELEMENTS_TOKEN = "elements";
    private static final String NAME_TOKEN = "element";
    private static final String VALUE_TOKEN = "value";
    private final Map<String, PreferencesNode> nameToPreferencesMap;
    //endregion Variable Declarations

    //region Constructors
    public SuitePreferences() {
        nameToPreferencesMap = new HashMap<>();
    }
    //endregion Constructors

    //region Getters/Setters
    public Map<String, PreferencesNode> getNameToPreferencesMap() {
        return nameToPreferencesMap;
    }
    //endregion Getters/Setters

    public PreferencesNode forClass(final Class<?> classToManage) {
        return getNameToPreferencesMap().computeIfAbsent(classToManage.getName(), c -> new PreferencesNode(classToManage));
    }

    //region Write To File
    public void saveToFile(final String filePath) {
        LogManager.getLogger().debug("Saving nameToPreferencesMap to: " + filePath);
        final JsonFactory factory = new JsonFactory();
        try (FileOutputStream output = new FileOutputStream(filePath)) {
            try (JsonGenerator writer = factory.createGenerator(output).useDefaultPrettyPrinter()) {
                writer.enable(JsonGenerator.Feature.STRICT_DUPLICATE_DETECTION);

                writer.writeStartObject();
                writer.writeFieldName(PREFERENCES_TOKEN);
                writer.writeStartArray();

                // Write each PreferencesNode
                for (final Map.Entry<String, PreferencesNode> preferences : getNameToPreferencesMap().entrySet()) {
                    writePreferencesNode(writer, preferences);
                }

                writer.writeEndArray();
                writer.writeEndObject();
            }
        } catch (FileNotFoundException ex) {
            LogManager.getLogger().error("Could not save nameToPreferencesMap to: " + filePath, ex);
        } catch (IOException ex) {
            LogManager.getLogger().error("Error writing to the nameToPreferencesMap file: " + filePath, ex);
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
        }
    }

    private static void writePreferencesNode(final JsonGenerator writer,
                                             final Entry<String, PreferencesNode> nodeInfo)
            throws Exception {
        writer.writeStartObject();
        writer.writeStringField(CLASS_TOKEN, nodeInfo.getKey());
        writer.writeFieldName(ELEMENTS_TOKEN);
        writer.writeStartArray();

        // Write all PreferenceElement in this node
        for (final Map.Entry<String, String> element : nodeInfo.getValue().getFinalValues().entrySet()) {
            writePreferenceElement(writer, element);
        }

        writer.writeEndArray();
        writer.writeEndObject();
    }

    private static void writePreferenceElement(final JsonGenerator writer,
                                               final Map.Entry<String, String> element) throws IOException {
        writer.writeStartObject();
        writer.writeStringField(NAME_TOKEN, element.getKey());
        writer.writeStringField(VALUE_TOKEN, element.getValue());
        writer.writeEndObject();
    }
    //endregion Write To File

    //region Load From File
    public void loadFromFile(final String filePath) {
        LogManager.getLogger().info("Loading user preferences from: " + filePath);
        try (FileInputStream input = new FileInputStream(filePath)) {
            final JsonFactory factory = new JsonFactory();
            try (JsonParser parser = factory.createParser(input)) {
                if (parser.nextToken() != JsonToken.START_OBJECT) {
                    throw new IOException("Expected an object start ({)" + getParserInformation(parser));
                } else if (parser.nextToken() != JsonToken.FIELD_NAME && !parser.getCurrentName().equals(PREFERENCES_TOKEN)) {
                    throw new IOException("Expected a field called (" + PREFERENCES_TOKEN + ")" + getParserInformation(parser));
                } else if (parser.nextToken() != JsonToken.START_ARRAY) {
                    throw new IOException("Expected an array start ([)" + getParserInformation(parser));
                }

                // Parse all PreferencesNode
                while (parser.nextToken() != JsonToken.END_ARRAY) {
                    try {
                        readPreferencesNode(parser, getNameToPreferencesMap());
                    } catch (IOException e) {
                        LogManager.getLogger().error("Error reading node. " + getParserInformation(parser), e);
                    }
                }
            }
            LogManager.getLogger().info("Finished loading user preferences");
        } catch (FileNotFoundException ignored) {

        } catch (Exception e) {
            LogManager.getLogger().error("Error reading from the user preferences file: " + filePath, e);
        }
    }

    private static String getParserInformation(final JsonParser parser) throws IOException {
        return (parser == null) ? "" : ". Current token: " + parser.getCurrentName() +
                ". Line number: " + parser.getCurrentLocation().getLineNr() +
                ". Column number: " + parser.getCurrentLocation().getColumnNr();
    }

    private static void readPreferencesNode(final JsonParser parser,
                                            final Map<String, PreferencesNode> nodes) throws IOException {
        if (parser.currentToken() != JsonToken.START_OBJECT) {
            return;
        } else if ((parser.nextToken() != JsonToken.FIELD_NAME) && !parser.getCurrentName().equals(CLASS_TOKEN)) {
            return;
        }

        final String className = parser.nextTextValue();

        if ((parser.nextToken() != JsonToken.FIELD_NAME) && !parser.getCurrentName().equals(ELEMENTS_TOKEN)) {
            return;
        } else if (parser.nextToken() != JsonToken.START_ARRAY) {
            return;
        }

        final Map<String, String> elements = new HashMap<>();

        // Parse all PreferenceElement in this node
        while (parser.nextToken() != JsonToken.END_ARRAY) {
            try {
                readPreferenceElement(parser, elements);
            } catch (IOException e) {
                LogManager.getLogger().warn("Error reading elements for node: " + className + ".", e);
            }
        }

        try {
            final PreferencesNode node = new PreferencesNode(Class.forName(className));
            node.initialize(elements);
            nodes.put(node.getNode().getName(), node);
        } catch (ClassNotFoundException ex) {
            LogManager.getLogger().error("No class with name " + className + " found", ex);
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
        }
    }

    private static void readPreferenceElement(final JsonParser parser, final Map<String, String> elements) throws IOException {
        if (parser.currentToken() != JsonToken.START_OBJECT) {
            return;
        } else if ((parser.nextToken() != JsonToken.FIELD_NAME) && !parser.getCurrentName().equals(NAME_TOKEN)) {
            return;
        }

        final String name = parser.nextTextValue();

        if ((parser.nextToken() != JsonToken.FIELD_NAME) && !parser.getCurrentName().equals(VALUE_TOKEN)) {
            return;
        }

        final String value = parser.nextTextValue();

        if (parser.nextToken() != JsonToken.END_OBJECT) {
            return;
        }

        elements.put(name, value);
    }
    //endregion Load From File
}
