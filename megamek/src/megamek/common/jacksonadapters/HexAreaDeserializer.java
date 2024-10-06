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
import megamek.common.Coords;
import megamek.common.Terrains;
import megamek.common.hexarea.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    /**
     * Parses a HexArea from the given YAML node. The node should be below the "area:" level.
     *
     * @param node The node to parse
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
        return new CircleHexArea(CoordsDeserializer.parseNode(node.get(CENTER)), node.get(RADIUS).intValue());
    }

    private static HexArea parseList(JsonNode node) {
        if (node.isArray()) {
            Set<Coords> coords = new HashSet<>();
            node.forEach(n -> coords.add(CoordsDeserializer.parseNode(n)));
            return new ListHexArea(coords);
        } else {
            return new ListHexArea(CoordsDeserializer.parseNode(node));
        }
    }

    private static HexArea parseRectangle(JsonNode node) {
        if (node.isArray()) {
            List<Coords> coords = new ArrayList<>();
            node.forEach(n -> coords.add(CoordsDeserializer.parseNode(n)));
            if (coords.size() == 2) {
                return new RectangleHexArea(coords.get(0).getX(), coords.get(0).getY(), coords.get(1).getX(), coords.get(1).getY());
            } else {
                throw new IllegalArgumentException("A Rectangle must be defined by two corner coords!");
            }
        } else {
            throw new IllegalArgumentException("A Rectangle must be defined by two corner coords!");
        }
    }

    private static HexArea parseHalfPlane(JsonNode node) {
        MMUReader.requireFields("Halfplane HexArea", node, DIRECTION);
        if (node.has(COORDINATE)) {
            int coordinate = node.get(COORDINATE).intValue() - 1;
            var type = HalfPlaneHexArea.HalfPlaneType.valueOf(node.get(EXTENDS).asText().toUpperCase());
            return new HalfPlaneHexArea(coordinate, type);
        } else {
            Coords point = CoordsDeserializer.parseNode(node.get(POINT));
            int direction = node.get(DIRECTION).intValue();
            var type = RowHalfPlaneHexArea.HalfPlaneType.valueOf(node.get(EXTENDS).asText().toUpperCase());
            return new RowHalfPlaneHexArea(point, direction, type);
        }
    }

    private static HexArea parseLine(JsonNode node) {
        MMUReader.requireFields("Line HexArea", node, POINT, DIRECTION);
        return new LineHexArea(CoordsDeserializer.parseNode(node.get(POINT)), node.get(DIRECTION).asInt());
    }

    private static HexArea parseRay(JsonNode node) {
        MMUReader.requireFields("Ray HexArea", node, POINT, DIRECTION);
        return new RayHexArea(CoordsDeserializer.parseNode(node.get(POINT)), node.get(DIRECTION).asInt());
    }

    private static HexArea parseBorder(JsonNode node) {
        if (node.has(EDGES)) {
            List<String> borders = TriggerDeserializer.parseArrayOrSingleNode(node.get(EDGES), NORTH, SOUTH, EAST, WEST);
            int mindistance = 0;
            int maxdistance = 0;
            if (node.has(MIN_DISTANCE)) {
                mindistance = node.get(MIN_DISTANCE).intValue();
            }
            if (node.has(MAX_DISTANCE)) {
                maxdistance = node.get(MAX_DISTANCE).intValue();
            }
            maxdistance = Math.max(maxdistance, mindistance);
            return new BorderHexArea(borders.contains(NORTH), borders.contains(SOUTH), borders.contains(EAST), borders.contains(WEST),
                mindistance, maxdistance);
        } else {
            List<String> borders = TriggerDeserializer.parseArrayOrSingleNode(node, NORTH, SOUTH, EAST, WEST);
            return new BorderHexArea(borders.contains(NORTH), borders.contains(SOUTH), borders.contains(EAST), borders.contains(WEST));
        }
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
            if (node.has(MAX_DISTANCE)) {
                maxDistance = node.get(MAX_DISTANCE).intValue();
            }
        }
        return new TerrainHexArea(terrainType, minLevel, maxLevel, minDistance, maxDistance);
    }

    private static HexArea parseHexLevelArea(JsonNode node) {
        if (node.isValueNode()) {
            return new HexLevelArea(node.asInt());
        } else {
            int minLevel = Integer.MIN_VALUE;
            int maxLevel = Integer.MAX_VALUE;
            if (node.has(MIN_LEVEL)) {
                minLevel = node.get(MIN_LEVEL).intValue();
            }
            if (node.has(MAX_LEVEL)) {
                maxLevel = node.get(MAX_LEVEL).intValue();
            }
            return new HexLevelArea(minLevel, maxLevel);
        }
    }

    private HexAreaDeserializer() { }
}
