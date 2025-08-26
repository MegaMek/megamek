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

package megamek.common.hexArea;

import megamek.common.board.Board;
import megamek.common.board.Coords;

/**
 * This class represents one or more of the edge of any given board. It is a relative shape, i.e. its hexes depend on
 * the board that the HexArea is given when checking its coords.
 */
public class BorderHexArea extends AbstractHexArea {

    private final boolean north;
    private final boolean south;
    private final boolean east;
    private final boolean west;
    private final int minInset;
    private final int maxInset;

    /**
     * Creates a border hex area for the given edges of the board. minInset gives the minimum distance from the board
     * edge; 0 means the edge itself. maxInset gives the maximum distance from the board edge; 0 means the edge itself.
     * maxInset is always set to at least the value of minInset. Negative values are set to 0.
     *
     * @param north    When true, includes the hexes of the upper board edge (at y = 0)
     * @param south    When true, includes the hexes of the lower board edge (at y = board height)
     * @param west     When true, includes the hexes of the left board edge (at x = 0)
     * @param east     When true, includes the hexes of the right board edge (at x = board width)
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
        return matchesBoardId(board) && (north && (coords.getY() >= minInset) && (coords.getY() <= maxInset))
              || (west && (coords.getX() >= minInset) && (coords.getX() <= maxInset))
              || (south && (coords.getY() <= board.getHeight() - 1 - minInset) && (coords.getY()
              >= board.getHeight() - 1 - maxInset))
              || (east && (coords.getX() <= board.getWidth() - 1 - minInset) && (coords.getX()
              >= board.getWidth() - 1 - maxInset));
    }
}
