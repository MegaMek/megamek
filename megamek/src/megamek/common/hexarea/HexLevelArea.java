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
 * This area is defined by the hex level (aka floor height) being in the range defined by the given level(s).
 */
public class HexLevelArea extends AbstractHexArea {

    private final int minLevel;
    private final int maxLevel;

    /**
     * Creates an area defined by the hex level (aka floor height) being in the range defined by the given levels. maxLevel must be equal or
     * greater than minLevel or the area is empty.
     *
     * @param minLevel The minimum hex level to include in the area
     * @param maxLevel The maximum hex level to include in the area
     */
    public HexLevelArea(int minLevel, int maxLevel) {
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
    }

    /**
     * Creates an area defined by the hex level (aka floor height) being equal to the given level.
     *
     * @param level The hex level to include in the area
     */
    public HexLevelArea(int level) {
        this(level, level);
    }

    @Override
    public boolean containsCoords(Coords coords, Board board) {
        return board.contains(coords) && (board.getHex(coords).getLevel() >= minLevel) && (board.getHex(coords).getLevel() <= maxLevel);
    }
}
