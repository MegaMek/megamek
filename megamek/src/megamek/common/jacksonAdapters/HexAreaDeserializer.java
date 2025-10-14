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

package megamek.common.jacksonAdapters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import megamek.common.board.Coords;
import megamek.common.hexArea.*;
import megamek.common.units.Terrains;

public final class HexAreaDeserializer {

    // Possible future improvements:
    // read board-relative sizes, like mindistance = 50%W for half board width
    // read rectangle as UL corner and W/H

    private static final String UNION = "union";
    private static final String DIFFERENCE = "difference";
    private static final String INTERSECTION = "intersection";
    private static final String FIRST = "first";
    private static final String SECOND = "second";

    private static final String CIRCLE = "circle";
    private static final String CENTER = "center";
    private static final String RADIUS = "radius";
    private static final String HALF_PLANE = "halfplane";
    private static final String COORDINATE = "coordinate";
    private static final String DIRECTION = "direction";
    private static final String EXTENDS = "extends";
    private static final String LIST = "list";
    private static final String RECTANGLE = "rectangle";
    private static final String LINE = "line";
    private static final String POINT = "point";
    private static final String RAY = "ray";
    private static final String BORDER = "border";
    private static final String MIN_DISTANCE = "mindistance";
    private static final String MAX_DISTANCE = "maxdistance";
    private static final String NORTH = "north";
    private static final String SOUTH = "south";
    private static final String WEST = "west";
    private static final String EAST = "east";
    private static final String EDGES = "edges";
    private static final String EMPTY = "empty";
    private static final String TERRAIN = "terrain";
    private static final String TYPE = "type";
    private static final String LEVEL = "level";
    private static final String HEX_LEVEL = "hexlevel";
    private static final String MIN_LEVEL = "minlevel";
    private static final String MAX_LEVEL = "maxlevel";
    private static final String ALL = "all";
    private static final String BOARDS = "boards";

    /**
     * Parses a HexArea from the given YAML node. The node should be below the "area:" level.
     *
     * @param node The node to parse
     *
     * @return A HexArea parsed from the given node
     */
    public static HexArea parseShape(JsonNode node) {
        if (node.has(UNION)) {
            return parseUnion(node.get(UNION));
        } else if (node.has(DIFFERENCE)) {
            return parseDifference(node.get(DIFFERENCE));
        } else if (node.has(INTERSECTION)) {
            return parseIntersection(node.get(INTERSECTION));
        } else if (node.has(CIRCLE)) {
            return parseCircle(node.get(CIRCLE));
        } else if (node.has(HALF_PLANE)) {
            return parseHalfPlane(node.get(HALF_PLANE));
        } else if (node.has(LIST)) {
            return parseList(node.get(LIST));
        } else if (node.has(RECTANGLE)) {
            return parseRectangle(node.get(RECTANGLE));
        } else if (node.has(LINE)) {
            return parseLine(node.get(LINE));
        } else if (node.has(RAY)) {
            return parseRay(node.get(RAY));
        } else if (node.has(BORDER)) {
            return parseBorder(node.get(BORDER));
        } else if (node.has(EMPTY)) {
            return HexArea.EMPTY_AREA;
        } else if (node.has(TERRAIN)) {
            return parseTerrainArea(node.get(TERRAIN));
        } else if (node.has(HEX_LEVEL)) {
            return parseHexLevelArea(node.get(HEX_LEVEL));
        } else if (node.has(ALL)) {
            return new AllHexArea();
        } else {
            throw new IllegalStateException("Cannot parse area node!");
        }
    }

    private static HexArea parseUnion(JsonNode node) {
        MMUReader.requireFields("HexArea", node, FIRST, SECOND);
        return new HexAreaUnion(parseShape(node.get(FIRST)), parseShape(node.get(SECOND)));
    }

    private static HexArea parseDifference(JsonNode node) {
        MMUReader.requireFields("HexArea", node, FIRST, SECOND);
        return new HexAreaDifference(parseShape(node.get(FIRST)), parseShape(node.get(SECOND)));
    }

    private static HexArea parseIntersection(JsonNode node) {
        MMUReader.requireFields("HexArea", node, FIRST, SECOND);
        return new HexAreaIntersection(parseShape(node.get(FIRST)), parseShape(node.get(SECOND)));
    }

    private static HexArea parseCircle(JsonNode node) {
        MMUReader.requireFields("HexArea", node, CENTER, RADIUS);
        var area = new CircleHexArea(CoordsDeserializer.parseNode(node.get(CENTER)), node.get(RADIUS).intValue());
        area.setBoardIds(parseBoardsList(node));
        return area;
    }

    private static List<Integer> parseBoardsList(JsonNode node) {
        if (node.has(BOARDS)) {
            JsonNode boardsNode = node.get(BOARDS);
            if (boardsNode.isArray()) {
                List<Integer> boardsList = new ArrayList<>();
                boardsNode.elements().forEachRemaining(n -> boardsList.add(n.asInt()));
                if (boardsList.isEmpty()) {
                    throw new IllegalArgumentException("Must give one or more board IDs!");
                } else {
                    return boardsList;
                }
            } else {
                return List.of(boardsNode.asInt());
            }
        } else {
            return Collections.emptyList();
        }
    }

    private static HexArea parseList(JsonNode node) {
        ListHexArea area;
        if (node.isArray()) {
            Set<Coords> coords = new HashSet<>();
            node.forEach(n -> coords.add(CoordsDeserializer.parseNode(n)));
            area = new ListHexArea(coords);
        } else {
            area = new ListHexArea(CoordsDeserializer.parseNode(node));
        }
        area.setBoardIds(parseBoardsList(node));
        return area;
    }

    private static HexArea parseRectangle(JsonNode node) {
        if (node.isArray()) {
            List<Coords> coords = new ArrayList<>();
            node.forEach(n -> coords.add(CoordsDeserializer.parseNode(n)));
            if (coords.size() == 2) {
                var area = new RectangleHexArea(coords.get(0).getX(), coords.get(0).getY(),
                      coords.get(1).getX(), coords.get(1).getY());
                area.setBoardIds(parseBoardsList(node));
                return area;
            } else {
                throw new IllegalArgumentException("A Rectangle must be defined by two corner coords!");
            }
        } else {
            throw new IllegalArgumentException("A Rectangle must be defined by two corner coords!");
        }
    }

    private static HexArea parseHalfPlane(JsonNode node) {
        HexArea area;
        MMUReader.requireFields("Halfplane HexArea", node, DIRECTION);
        if (node.has(COORDINATE)) {
            int coordinate = node.get(COORDINATE).intValue() - 1;
            var type = HalfPlaneHexArea.HalfPlaneType.valueOf(node.get(EXTENDS).asText().toUpperCase());
            area = new HalfPlaneHexArea(coordinate, type);
        } else {
            Coords point = CoordsDeserializer.parseNode(node.get(POINT));
            int direction = node.get(DIRECTION).intValue();
            var type = RowHalfPlaneHexArea.HalfPlaneType.valueOf(node.get(EXTENDS).asText().toUpperCase());
            area = new RowHalfPlaneHexArea(point, direction, type);
        }
        area.setBoardIds(parseBoardsList(node));
        return area;
    }

    private static HexArea parseLine(JsonNode node) {
        MMUReader.requireFields("Line HexArea", node, POINT, DIRECTION);
        var area = new LineHexArea(CoordsDeserializer.parseNode(node.get(POINT)), node.get(DIRECTION).asInt());
        area.setBoardIds(parseBoardsList(node));
        return area;
    }

    private static HexArea parseRay(JsonNode node) {
        MMUReader.requireFields("Ray HexArea", node, POINT, DIRECTION);
        var area = new RayHexArea(CoordsDeserializer.parseNode(node.get(POINT)), node.get(DIRECTION).asInt());
        area.setBoardIds(parseBoardsList(node));
        return area;
    }

    private static HexArea parseBorder(JsonNode node) {
        BorderHexArea area;
        if (node.has(EDGES)) {
            List<String> borders = TriggerDeserializer.parseArrayOrSingleNode(node.get(EDGES),
                  NORTH,
                  SOUTH,
                  EAST,
                  WEST);
            int minDistance = 0;
            int maxDistance = 0;
            if (node.has(MIN_DISTANCE)) {
                minDistance = node.get(MIN_DISTANCE).intValue();
            }
            if (node.has(MAX_DISTANCE)) {
                maxDistance = node.get(MAX_DISTANCE).intValue();
            }
            maxDistance = Math.max(maxDistance, minDistance);
            area = new BorderHexArea(borders.contains(NORTH), borders.contains(SOUTH), borders.contains(EAST),
                  borders.contains(WEST),
                  minDistance, maxDistance);
        } else {
            List<String> borders = TriggerDeserializer.parseArrayOrSingleNode(node, NORTH, SOUTH, EAST, WEST);
            area = new BorderHexArea(borders.contains(NORTH), borders.contains(SOUTH), borders.contains(EAST),
                  borders.contains(WEST));
        }
        area.setBoardIds(parseBoardsList(node));
        return area;
    }

    private static HexArea parseTerrainArea(JsonNode node) {
        int minLevel = 0;
        int maxLevel = Integer.MAX_VALUE;
        int terrainType = Terrains.getType(node.get(TYPE).asText());
        if (terrainType == 0) {
            throw new IllegalArgumentException("Invalid terrain type: " + node.get(TERRAIN).asText());
        }
        if (node.has(LEVEL)) {
            minLevel = node.get(LEVEL).intValue();
        } else if (node.has(MIN_LEVEL)) {
            minLevel = node.get(MIN_LEVEL).intValue();
            if (node.has(MAX_LEVEL)) {
                maxLevel = node.get(MAX_LEVEL).intValue();
            }
        }

        int minDistance = 0;
        int maxDistance = 0;
        if (node.has(MIN_DISTANCE)) {
            minDistance = node.get(MIN_DISTANCE).intValue();
        }
        if (node.has(MAX_DISTANCE)) {
            maxDistance = node.get(MAX_DISTANCE).intValue();
        }
        var area = new TerrainHexArea(terrainType, minLevel, maxLevel, minDistance, maxDistance);
        area.setBoardIds(parseBoardsList(node));
        return area;
    }

    private static HexArea parseHexLevelArea(JsonNode node) {
        HexLevelArea area;
        if (node.isValueNode()) {
            area = new HexLevelArea(node.asInt());
        } else {
            int minLevel = Integer.MIN_VALUE;
            int maxLevel = Integer.MAX_VALUE;
            if (node.has(MIN_LEVEL)) {
                minLevel = node.get(MIN_LEVEL).intValue();
            }
            if (node.has(MAX_LEVEL)) {
                maxLevel = node.get(MAX_LEVEL).intValue();
            }
            area = new HexLevelArea(minLevel, maxLevel);
        }
        area.setBoardIds(parseBoardsList(node));
        return area;
    }

    private HexAreaDeserializer() {}
}
