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

import java.util.ArrayList;
import java.util.List;

import static megamek.common.jacksonadapters.MMUReader.requireFields;

public class CoordsDeserializer {

    private static final String X = "x";
    private static final String Y = "y";
    private static final String MESSAGE = "Illegal Coords definition. Use either a list of two values or x: and y:";

    public static Coords parseNode(JsonNode coordsNode) {
        if (coordsNode.isArray()) {
            List<Integer> xyList = new ArrayList<>();
            coordsNode.elements().forEachRemaining(n -> xyList.add(n.asInt()));
            if (xyList.size() == 2) {
                return new Coords(xyList.get(0) - 1, xyList.get(1) - 1);
            } else {
                throw new IllegalArgumentException(MESSAGE);
            }
        } else if (coordsNode.has(X) || coordsNode.has(Y)) {
            requireFields("Coords", coordsNode, X, Y);
            return new Coords(coordsNode.get(X).asInt() - 1, coordsNode.get(Y).asInt() - 1);
        } else {
            throw new IllegalArgumentException(MESSAGE);
        }
    }
}
