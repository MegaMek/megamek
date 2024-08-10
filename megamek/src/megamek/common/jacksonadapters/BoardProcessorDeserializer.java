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

import com.fasterxml.jackson.databind.JsonNode;
import megamek.common.Terrain;
import megamek.common.Terrains;
import megamek.common.board.postprocess.BoardProcessor;
import megamek.common.board.postprocess.TerrainLevelConverter;

import java.util.ArrayList;
import java.util.List;

public class BoardProcessorDeserializer {

    private static final String TYPE = "type";
    private static final String TYPE_TERRAIN_LEVEL = "convertterrainlevel";
    private static final String TERRAIN = "terrain";
    private static final String FROM = "from";
    private static final String TO = "to";
    private static final String LEVEL = "level";

    public static List<BoardProcessor> parseNode(JsonNode processNode) {
        List<BoardProcessor> processors = new ArrayList<>();
        processNode.iterator().forEachRemaining(n -> processors.add(parseSingleBoardProcessor(n)));
        return processors;
    }

    private static BoardProcessor parseSingleBoardProcessor(JsonNode node) {
        String type = node.get(TYPE).asText();
        return switch (type) {
            case TYPE_TERRAIN_LEVEL -> parseConvertTerrainLevel(node);
            default -> throw new IllegalStateException("Unexpected value: " + type);
        };
    }

    private static BoardProcessor parseConvertTerrainLevel(JsonNode triggerNode) {
        JsonNode from = triggerNode.get(FROM);
        JsonNode to = triggerNode.get(TO);
        int fromLevel = Terrain.WILDCARD;
        int terrainType = Terrains.getType(from.get(TERRAIN).asText());
        if (terrainType == 0) {
            throw new IllegalArgumentException("Invalid terrain type: " + from.get(TERRAIN).asText());
        }
        if (from.has(LEVEL)) {
            fromLevel = from.get(LEVEL).intValue();
        }
        int toLevel = to.get(LEVEL).intValue();
        return new TerrainLevelConverter(terrainType, fromLevel, toLevel);
    }

    private BoardProcessorDeserializer() { }
}
