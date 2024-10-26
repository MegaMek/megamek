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
 * This class represents one or more of the edge of any given board. It is a relative shape, i.e. its hexes depend on the
 * board that the HexArea is given when checking its coords.
 */
public class BorderHexArea extends AbstractHexArea {

    private final boolean north;
    private final boolean south;
    private final boolean east;
    private final boolean west;
    private final int minInset;
    private final int maxInset;

    /**
     * Creates a border hex area for the given edges of the board. minInset gives the minimum distance from the board edge; 0 means the edge
     * itself. maxInset gives the maximum distance from the board edge; 0 means the edge itself. maxInset is always set to at least the
     * value of minInset. Negative values are set to 0.
     *
     * @param north When true, includes the hexes of the upper board edge (at y = 0)
     * @param south When true, includes the hexes of the lower board edge (at y = board height)
     * @param west  When true, includes the hexes of the left board edge (at x = 0)
     * @param east  When true, includes the hexes of the right board edge (at x = board width)
     * @param minInset the distance from the edges the area begins
     * @param maxInset the distance from the edges the area ends
     */
    public BorderHexArea(boolean north, boolean south, boolean east, boolean west, int minInset, int maxInset) {
        this.north = north;
        this.south = south;
        this.east = east;
        this.west = west;
        this.minInset = Math.max(minInset, 0);
        this.maxInset = Math.max(minInset, maxInset);
    }

    /**
     * Creates a border hex area for the given edges of the board.
     *
     * @param north When true, includes the hexes of the upper board edge (at y = 0)
     * @param south When true, includes the hexes of the lower board edge (at y = board height)
     * @param west  When true, includes the hexes of the left board edge (at x = 0)
     * @param east  When true, includes the hexes of the right board edge (at x = board width)
     */
    public BorderHexArea(boolean north, boolean south, boolean east, boolean west) {
        this(north, south, east, west, 0, 0);
    }

    @Override
    public boolean containsCoords(Coords coords, Board board) {
        return (north && (coords.getY() >= minInset) && (coords.getY() <= maxInset))
            || (west && (coords.getX() >= minInset) && (coords.getX() <= maxInset))
            || (south && (coords.getY() <= board.getHeight() - 1 - minInset) && (coords.getY() >= board.getHeight() - 1 - maxInset))
            || (east && (coords.getX() <= board.getWidth() - 1 - minInset) && (coords.getX() >= board.getWidth() - 1 - maxInset));
    }
}
