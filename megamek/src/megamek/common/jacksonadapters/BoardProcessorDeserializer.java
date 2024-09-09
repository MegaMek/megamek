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
import megamek.common.board.postprocess.*;

import java.util.ArrayList;
import java.util.List;

public class BoardProcessorDeserializer {

    private static final String TYPE = "type";
    private static final String TYPE_TERRAIN_REMOVE = "removeterrain";
    private static final String TYPE_TERRAIN_CONVERT = "convertterrain";
    private static final String TYPE_THEME = "settheme";
    private static final String TERRAIN = "terrain";
    private static final String NEW_TERRAIN = "newterrain";
    private static final String LEVEL = "level";
    private static final String NEW_LEVEL = "newlevel";
    private static final String THEME = "theme";

    public static List<BoardProcessor> parseNode(JsonNode processNode) {
        List<BoardProcessor> processors = new ArrayList<>();
        processNode.iterator().forEachRemaining(n -> processors.add(parseSingleBoardProcessor(n)));
        return processors;
    }

    private static BoardProcessor parseSingleBoardProcessor(JsonNode node) {
        String type = node.get(TYPE).asText();
        return switch (type) {
            case TYPE_TERRAIN_CONVERT -> parseConvertTerrain(node);
            case TYPE_TERRAIN_REMOVE -> parseRemoveTerrain(node);
            case TYPE_THEME -> parseChangeTheme(node);
            default -> throw new IllegalStateException("Unexpected value: " + type);
        };
    }

    private static BoardProcessor parseConvertTerrain(JsonNode node) {
        int fromTerrainType = Terrains.getType(node.get(TERRAIN).asText());
        if (fromTerrainType == 0) {
            throw new IllegalArgumentException("Invalid terrain type: " + node.get(TERRAIN).asText());
        }
        int fromLevel = node.has(LEVEL) ? node.get(LEVEL).intValue() : Terrain.WILDCARD;


        int toTerrainType = node.has(NEW_TERRAIN) ? Terrains.getType(node.get(NEW_TERRAIN).asText()) : fromTerrainType;
        if (toTerrainType == 0) {
            throw new IllegalArgumentException("Invalid terrain type: " + node.get(NEW_TERRAIN).asText());
        }
        int toLevel = node.has(NEW_LEVEL) ? node.get(NEW_LEVEL).intValue() : Terrain.WILDCARD;

        if (toTerrainType == fromTerrainType) {
            return new TerrainLevelBoardProcessor(fromTerrainType, fromLevel, toLevel);
        } else {
            return new TerrainTypeBoardProcessor(fromTerrainType, fromLevel, toTerrainType, toLevel);
        }
    }

    private static BoardProcessor parseRemoveTerrain(JsonNode node) {
        int level = Terrain.WILDCARD;
        int terrainType = Terrains.getType(node.get(TERRAIN).asText());
        if (terrainType == 0) {
            throw new IllegalArgumentException("Invalid terrain type: " + node.get(TERRAIN).asText());
        }
        if (node.has(LEVEL)) {
            level = node.get(LEVEL).intValue();
        }
        return new TerrainRemoveBoardProcessor(terrainType, level);
    }

    private static BoardProcessor parseChangeTheme(JsonNode triggerNode) {
        return new ChangeThemeBoardProcessor(triggerNode.get(THEME).asText());
    }

    private BoardProcessorDeserializer() { }
}
