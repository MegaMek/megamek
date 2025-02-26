/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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
package megamek.common.jacksonadapters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.strategicBattleSystems.SBFFormation;
import megamek.common.strategicBattleSystems.SBFUnit;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This class has methods for reading objects such as SBF Formations, Units and AlphaStrikeElements
 * from a MMU file which uses YAML formatting.
 */
public final class MMUReader {

    static final String TYPE = "type";
    static final String SBF_FORMATION = "SBFFormation";
    static final String SBF_UNIT = "SBFUnit";
    static final String AS_ELEMENT = "ASElement";
    static final String SKILL = "skill";
    static final String SIZE = "size";
    static final String ROLE = "role";
    static final String FORCE = "force";
    static final String CHASSIS = "chassis";
    static final String MODEL = "model";
    static final String GENERAL_NAME = "generalname";
    static final String SPECIFIC_NAME = "specificname";
    static final String DAMAGE = "damage";
    static final String MOVE = "move";
    static final String MOVE_MODE = "movemode";
    static final String TRSP_MOVE = "trspmove";
    static final String TRSP_MOVE_MODE = "trspmovemode";
    static final String ARMORDAMAGE = "armordamage";
    static final String SPECIALS = "specials";
    static final String ARMOR = "armor";
    static final String JUMP = "jump";
    static final String INCLUDE = "include";
    static final String ID = "id";

    private static final ObjectMapper yamlMapper =
            new ObjectMapper(new YAMLFactory());

    private String currentDirectory = "";

    public MMUReader() { }

    public MMUReader(File file) {
        currentDirectory = file.getParent();
    }

    /**
     * Read a list of objects from a given MMU file (YAML format, see {@link MMUWriter}. The list can be empty
     * or contain one or multiple results. If reading any part of the file fails, an exception is thrown.
     *
     * @param file The MMU file to read
     * @return A list of objects (AlphaStrikeElements, SBFUnits...) contained in the MMU file
     * @throws IOException When a read error occurs
     */
    public List<Object> read(File file) throws IOException {
        currentDirectory = file.getParent();
        JsonNode node = yamlMapper.readTree(file);
        return read(node);
    }

    /**
     * Read a list of objects from a Jackson JsonNode. Internally used to parse nested objects.
     *
     * @param node The node to parse
     * @return A list of objects (AlphaStrikeElements, SBFUnits...) contained in the node
     * @throws IOException When a read error occurs
     */
    public List<Object> read(JsonNode node) throws IOException {
        return read(node, null);
    }

    /**
     * Read a list of objects from a Jackson JsonNode. Internally used to parse nested objects.
     *
     * @param node The node to parse
     * @return A list of objects (AlphaStrikeElements, SBFUnits...) contained in the node
     * @throws IOException When a read error occurs
     */
    public List<Object> read(JsonNode node, Class<?> objectType) throws IOException {
        List<Object> result = new ArrayList<>();
        if (node.isArray()) {
            for (Iterator<JsonNode> it = node.elements(); it.hasNext(); ) {
                result.addAll(parseNode(it.next(), objectType));
            }
        } else if (node.isObject()) {
            result.addAll(parseNode(node, objectType));
        } else {
            throw new IllegalArgumentException("Can't process file, neither array nor single unit");
        }
        return result;
    }

    private List<Object> parseNode(JsonNode node, Class<?> objectType) throws IOException {
        if (node.has(INCLUDE)) {
            JsonNode node2 = yamlMapper.readTree(new File(currentDirectory, node.get(INCLUDE).textValue()));
            // add the included nodes to the present node as if they had been written directly in the original file
            // node can be an array or a single element (object node)
            // the included file can have an array or a single element
            if (node2.isObject()) {
                ((ObjectNode) node).setAll((ObjectNode) node2);
            } else if (node2.isArray()) {
                return read(node2, objectType);
            } else {
                throw new IllegalArgumentException("Can't process include file, neither array nor single unit");
            }
        }

        if (objectType != null) {
            // When the object type such as AlphaStrikeElement.class is given, read it directly as such an object
            return List.of(yamlMapper.treeToValue(node, objectType));

        } else {
            // When no type is provided by the method call, the type must be given with the "type:" keyword
            if (!node.has(TYPE)) {
                throw new IOException("Missing type info in MMU file!");
            }
            try {
                return switch (node.get(TYPE).textValue()) {
                    case SBF_FORMATION -> List.of(yamlMapper.treeToValue(node, SBFFormation.class));
                    case AS_ELEMENT -> List.of(yamlMapper.treeToValue(node, AlphaStrikeElement.class));
                    case SBF_UNIT -> List.of(yamlMapper.treeToValue(node, SBFUnit.class));
                    default -> List.of();
                };
            } catch (JsonProcessingException e) {
                return List.of();
            }
        }
    }

    /**
     * Tests the given node (which should be an object node that holds a full AS element or SBF Unit) if
     * it has all of the given fields. If any of the given fields is missing, an IllegalArgumentException
     * is thrown. The given objectType can be any String, it is only used as part of the error
     * message when a field is not found.
     *
     * @param objectType The object type such as "ASElement". Only used as part of the exception message
     * @param node the node containing an object to be deserialized
     * @param fields The required field names, e.g. "chassis" or "size"
     */
    public static void requireFields(String objectType, JsonNode node, String... fields) {
        for (String field : fields) {
            if (!node.has(field)) {
                throw new IllegalArgumentException("Missing field \"" + field + "\" in " + objectType + " definition!");
            }
        }
    }

    /**
     * Tests the given node if
     * it has any combination of at least two of the given fields. If that is the case, an IllegalArgumentException
     * is thrown. The given objectType can be any String, it is only used as part of the error
     * message. If none of the fields or only a single one of the fields is found, this method does nothing.
     *
     * @param objectType The object type such as "ASElement". Only used as part of the exception message
     * @param node the node containing an object to be deserialized
     * @param fields The field names, e.g. "chassis" or "size"
     */
    public static void disallowCombinedFields(String objectType, JsonNode node, String... fields) {
        List<String> foundFields = new ArrayList<>();
        for (String field : fields) {
            if (node.has(field)) {
                foundFields.add(field);
                if (foundFields.size() > 1) {
                    throw new IllegalArgumentException("Fields " + foundFields.get(0) + " and " + foundFields.get(1)
                            + "found in " + objectType + " definition!");
                }
            }
        }
    }
}
