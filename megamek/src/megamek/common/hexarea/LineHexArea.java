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
package megamek.common.hexarea;

import megamek.common.Coords;

/**
 * This class represents a line of hexes through the given point in one of the hex row directions. The direction
 * must be between 0 and 5. Opposite directions are equal, e.g. directions 1 and 4 result in the same line.
 *
 * @param point A hex that this line goes through
 * @param direction The direction of the line, 0 = N, 2 = SE ...
 */
public record LineHexArea(Coords point, int direction) implements HexArea {

    @Override
    public boolean containsCoords(Coords coords, int x1, int y1, int x2, int y2) {
        return (direction >= 0) && (direction <= 5)
                && (point.equals(coords)
                || point.isOnHexRow(direction, coords)
                || point.isOnHexRow((direction + 3) % 6, coords));
    }
}
