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

import megamek.common.Board;
import megamek.common.Coords;

/**
 * This class represents a line of hexes starting at the given point and extending in the given hex row direction. The direction must be
 * between 0 and 5. Contrary to HexLineShape, this shape does not extend in both directions, so opposite directions result in different
 * rays.
 */
public class RayHexArea extends AbstractHexArea {

    private final Coords point;
    private final int direction;

    /**
     * Creates a line of hexes starting at the given point in the given direction. The direction must be between 0 and 5.
     *
     * @param point     A hex that this line goes through
     * @param direction The direction of the line, 0 = N, 2 = SE ...
     */
    public RayHexArea(Coords point, int direction) {
        this.point = point;
        this.direction = direction;
    }

    @Override
    public boolean containsCoords(Coords coords, Board board) {
        return (direction >= 0) && (direction <= 5)
            && (point.equals(coords) || point.isOnHexRow(direction, coords));
    }
}
