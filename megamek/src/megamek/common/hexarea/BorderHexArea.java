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
 * This class represents one or more of the borders of the any given board or rectangle. It is a relative shape,
 * i.e. its hexes depend on the rectangle that the HexArea is given when requesting its hexes.
 *
 * @param north When true, includes the hexes of the upper board edge (usually at y = 0)
 * @param south When true, includes the hexes of the lower board edge (usually at y = board height)
 * @param west When true, includes the hexes of the left board edge (usually at x = 0)
 * @param east When true, includes the hexes of the right board edge (usually at x = board width)
 */
public record BorderHexArea(boolean north, boolean south, boolean east, boolean west, int minInset, int maxInset) implements HexArea {

    public BorderHexArea(boolean north, boolean south, boolean east, boolean west) {
        this(north, south, east, west, 0, 0);
    }

    @Override
    public boolean containsCoords(Coords coords, int x1, int y1, int x2, int y2) {
        return (north && (coords.getY() >= Math.min(y1, y2) + minInset) && (coords.getY() <= Math.min(y1, y2) + maxInset))
            || (west && (coords.getX() >= Math.min(x1, x2) + minInset) && (coords.getX() <= Math.min(x1, x2) + maxInset))
            || (south && (coords.getY() <= Math.max(y1, y2) - 1 - minInset) && (coords.getY() >= Math.max(y1, y2) - 1 - maxInset))
            || (east && (coords.getX() <= Math.max(x1, x2) - 1 - minInset) && (coords.getX() >= Math.max(x1, x2) - 1 - maxInset));
    }
}
