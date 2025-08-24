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

import static megamek.common.jacksonAdapters.MMUReader.requireFields;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import megamek.common.board.Coords;

public class CoordsDeserializer {

    private static final String X = "x";
    private static final String Y = "y";
    private static final String MESSAGE = "Illegal Coords definition. Use either a list of two values or x: and y:";

    /**
     * Returns a parsed Coords from the given node. Note that 1 is subtracted from the x and y values so that a coords
     * given as [ 3, 3 ] is the hex 0303 (rather than 0202) on the board when the grid numbers are shown.
     *
     * @param coordsNode The Json node to parse; it should be either an array of two values or have x: and y: nodes.
     *
     * @return The parsed coords
     */
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

    private CoordsDeserializer() {}
}
