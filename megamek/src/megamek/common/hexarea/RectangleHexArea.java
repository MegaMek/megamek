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
 * This class represents a rectangle shape. The rectangle includes the corner coordinates.
 */
public class RectangleHexArea extends AbstractHexArea {

    private final int x1;
    private final int y1;
    private final int x2;
    private final int y2;

    /**
     * Creates a rectangle shape. The rectangle includes the corner coordinates. The coordinates do not have to be sorted, i.e. x1 &gt; x2 and
     * x1 &lt; x2 have the same result. When x1 = x2 and/or y1 = y2, the rectangle consists of a single line or single hex.
     *
     * @param x1 The first x corner coordinate
     * @param x2 The second x corner coordinate
     * @param y1 The first y corner coordinate
     * @param y2 The second y corner coordinate
     */

    public RectangleHexArea(int x1, int y1, int x2, int y2) {
        this.x1 = Math.min(x1, x2);
        this.x2 = Math.max(x1, x2);
        this.y1 = Math.min(y1, y2);
        this.y2 = Math.max(y1, y2);
    }

    @Override
    public boolean containsCoords(Coords coords, Board board) {
        return (coords.getX() >= x1) && (coords.getX() <= x2) && (coords.getY() >= y1) && (coords.getY() <= y2);
    }
}
