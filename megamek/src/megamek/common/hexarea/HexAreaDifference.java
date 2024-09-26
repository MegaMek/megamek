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

import java.util.Set;

/**
 * This class represents the subtraction (difference) of two HexAreaShapes. The order of the two given shapes is relevant.
 */
public class HexAreaDifference extends AbstractHexArea {

    private final HexArea firstShape;
    private final HexArea secondShape;

    /**
     * Creates an area that is the difference between the two given hex areas, firstShape - secondShape.
     *
     * @param firstShape  The first of the two shapes; the ordering of the shapes is relevant
     * @param secondShape The second of the two shapes
     */
    public HexAreaDifference(HexArea firstShape, HexArea secondShape) {
        this.firstShape = firstShape;
        this.secondShape = secondShape;
    }

    @Override
    public boolean containsCoords(Coords coords, Board board) {
        return firstShape.containsCoords(coords, board) && !secondShape.containsCoords(coords, board);
    }

    @Override
    public boolean isSmall() {
        return (firstShape instanceof AbstractHexArea firstAbstractHexArea) && firstAbstractHexArea.isSmall()
            && (secondShape instanceof AbstractHexArea secondAbstractHexArea) && secondAbstractHexArea.isSmall();
    }

    @Override
    public Set<Coords> getCoords() {
        if (isSmall()) {
            Set<Coords> result = ((AbstractHexArea) firstShape).getCoords();
            result.removeAll(((AbstractHexArea) secondShape).getCoords());
            return result;
        } else {
            return super.getCoords();
        }
    }
}
