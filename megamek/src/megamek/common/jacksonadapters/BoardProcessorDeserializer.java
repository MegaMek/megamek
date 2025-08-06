/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common.jacksonadapters;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import megamek.common.Terrain;
import megamek.common.Terrains;
import megamek.common.board.postprocess.BoardProcessor;
import megamek.common.board.postprocess.ChangeThemeBoardProcessor;
import megamek.common.board.postprocess.HexLevelPostProcessor;
import megamek.common.board.postprocess.TerrainAddPostProcessor;
import megamek.common.board.postprocess.TerrainLevelBoardProcessor;
import megamek.common.board.postprocess.TerrainRemoveBoardProcessor;
import megamek.common.board.postprocess.TerrainTypeBoardProcessor;
import megamek.common.hexarea.HexArea;

public class BoardProcessorDeserializer {

    private static final String TYPE = "type";
    private static final String TYPE_TERRAIN_REMOVE = "removeterrain";
    private static final String TYPE_TERRAIN_CONVERT = "convertterrain";
    private static final String TYPE_TERRAIN_ADD = "addterrain";
    private static final String TYPE_THEME = "settheme";
    private static final String TYPE_HEX_LEVEL = "hexlevel";
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
            case TYPE_TERRAIN_ADD -> parseAddTerrain(node);
            case TYPE_THEME -> parseChangeTheme(node);
            case TYPE_HEX_LEVEL -> parseHexLevel(node);
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

    private static BoardProcessor parseAddTerrain(JsonNode node) {
        int terrainType = Terrains.getType(node.get(TERRAIN).asText());
        if (terrainType == 0) {
            throw new IllegalArgumentException("Invalid terrain type: " + node.get(TERRAIN).asText());
        }
        int level = node.get(LEVEL).intValue();
        HexArea area = HexAreaDeserializer.parseShape(node.get("area"));
        return new TerrainAddPostProcessor(terrainType, level, area);
    }

    private static BoardProcessor parseHexLevel(JsonNode node) {
        int level = node.get(LEVEL).intValue();
        HexArea area = HexAreaDeserializer.parseShape(node.get("area"));
        return new HexLevelPostProcessor(level, area);
    }

    private static BoardProcessor parseChangeTheme(JsonNode triggerNode) {
        return new ChangeThemeBoardProcessor(triggerNode.get(THEME).asText());
    }

    private BoardProcessorDeserializer() {}
}
