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
import megamek.common.hexarea.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static megamek.common.hexarea.HexHalfPlaneShape.HalfPlaneType;

public final class HexAreaDeserializer {

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
    private static final String LIST = "list";
    private static final String RECTANGLE = "rectangle";
    private static final String LINE = "line";
    private static final String POINT = "point";
    private static final String RAY = "ray";

    public static HexArea parseNode(JsonNode node) {
        return new HexArea(parseShape(node));
    }

    private static HexAreaShape parseShape(JsonNode node) {
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
        } else {
            throw new IllegalStateException("Cannot parse area node!");
        }
    }

    private static HexAreaShape parseUnion(JsonNode node) {
        MMUReader.requireFields("HexArea", node, FIRST, SECOND);
        return new HexAreaUnion(parseShape(node.get(FIRST)), parseShape(node.get(SECOND)));
    }

    private static HexAreaShape parseDifference(JsonNode node) {
        MMUReader.requireFields("HexArea", node, FIRST, SECOND);
        return new HexAreaDifference(parseShape(node.get(FIRST)), parseShape(node.get(SECOND)));
    }

    private static HexAreaShape parseIntersection(JsonNode node) {
        MMUReader.requireFields("HexArea", node, FIRST, SECOND);
        return new HexAreaIntersection(parseShape(node.get(FIRST)), parseShape(node.get(SECOND)));
    }

    private static HexAreaShape parseCircle(JsonNode node) {
        MMUReader.requireFields("HexArea", node, CENTER, RADIUS);
        return new HexCircleShape(CoordsDeserializer.parseNode(node.get(CENTER)), node.get(RADIUS).intValue());
    }

    private static HexAreaShape parseList(JsonNode node) {
        if (node.isArray()) {
            Set<Coords> coords = new HashSet<>();
            node.forEach(n -> coords.add(CoordsDeserializer.parseNode(n)));
            return new HexListShape(coords);
        } else {
            return new HexListShape(CoordsDeserializer.parseNode(node));
        }
    }

    private static HexAreaShape parseRectangle(JsonNode node) {
        if (node.isArray()) {
            List<Coords> coords = new ArrayList<>();
            node.forEach(n -> coords.add(CoordsDeserializer.parseNode(n)));
            if (coords.size() == 2) {
                return new HexRectangleShape(coords.get(0).getX(), coords.get(0).getY(), coords.get(1).getX(), coords.get(1).getY());
            } else {
                throw new IllegalArgumentException("A Rectangle must be defined by two corner coords!");
            }
        } else {
            throw new IllegalArgumentException("A Rectangle must be defined by two corner coords!");
        }
    }

    private static HexAreaShape parseHalfPlane(JsonNode node) {
        MMUReader.requireFields("Halfplane HexArea", node, COORDINATE, DIRECTION);
        int coordinate = node.get(COORDINATE).intValue() - 1;
        HalfPlaneType type = HalfPlaneType.valueOf(node.get(DIRECTION).asText().toUpperCase());
        return new HexHalfPlaneShape(coordinate, type);
    }

    private static HexAreaShape parseLine(JsonNode node) {
        MMUReader.requireFields("Line HexArea", node, POINT, DIRECTION);
        return new HexLineShape(CoordsDeserializer.parseNode(node.get(POINT)), node.get(DIRECTION).asInt());
    }

    private static HexAreaShape parseRay(JsonNode node) {
        MMUReader.requireFields("Ray HexArea", node, POINT, DIRECTION);
        return new HexRayShape(CoordsDeserializer.parseNode(node.get(POINT)), node.get(DIRECTION).asInt());
    }

    private HexAreaDeserializer() { }
}
