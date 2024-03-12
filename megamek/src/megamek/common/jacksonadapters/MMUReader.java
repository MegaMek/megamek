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
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import megamek.common.BTObject;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.strategicBattleSystems.SBFFormation;
import megamek.common.strategicBattleSystems.SBFUnit;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public class MMUReader {

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

    static final ObjectMapper yamlMapper =
            new ObjectMapper(new YAMLFactory());

    // This is currently only for testing purposes with a file format for AS and SBF units
    public List<BTObject> read(File file) throws IOException {
        JsonNode node = yamlMapper.readTree(file);
        List<BTObject> list = read(node);
        return read(node);
    }

    public List<BTObject> read(JsonNode node) throws IOException {
        List<BTObject> result = new ArrayList<>();
        if (node.isArray()) {
            for (Iterator<JsonNode> it = node.elements(); it.hasNext(); ) {
                JsonNode arrayNode = it.next();
                parseNode(arrayNode).ifPresent(result::add);
            }
        } else {
            parseNode(node).ifPresent(result::add);
        }
        return result;
    }

    private Optional<BTObject> parseNode(JsonNode node) throws IOException {
        if (node.has(INCLUDE)) {
            node = yamlMapper.readTree(new File(node.get(INCLUDE).textValue()));
            if (node.isArray()) {
                throw new IllegalArgumentException("An included MMU file may only contain a single object, not a list!");
            }
        }
        if (!node.has(TYPE)) {
            throw new IOException("Missing type info in MMU file!");
        }
        try {
            switch (node.get(TYPE).textValue()) {
                case SBF_FORMATION:
                    return Optional.of(yamlMapper.treeToValue(node, SBFFormation.class));
                case AS_ELEMENT:
                    return Optional.of(yamlMapper.treeToValue(node, AlphaStrikeElement.class));
                case SBF_UNIT:
                    return Optional.of(yamlMapper.treeToValue(node, SBFUnit.class));
                default:
                    return Optional.empty();
            }
        } catch (JsonProcessingException e) {
            return Optional.empty();
        }
    }

    /**
     * Tests the given node (which should be an object node that holds a full AS element or SBF Unit) if
     * it has all of the given fields. If any of the given fields is missing, an IllegalArgumentException
     * is thrown.
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
}