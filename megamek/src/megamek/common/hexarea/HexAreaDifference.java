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

import java.util.Set;

/**
 * This class represents the subtraction (difference) of two HexAreaShapes. The order of the two given
 * shapes is relevant.
 *
 * @param firstShape The first of the two shapes
 * @param secondShape The second of the two shapes; this shape is subtracted from the first shape
 */
public record HexAreaDifference(HexAreaShape firstShape, HexAreaShape secondShape) implements HexAreaShape {

    @Override
    public boolean containsCoords(Coords coords) {
        return firstShape().containsCoords(coords) && !secondShape().containsCoords(coords);
    }

    @Override
    public boolean isSmall() {
        return firstShape().isSmall() && secondShape().isSmall();
    }

    @Override
    public Set<Coords> getCoords() {
        Set<Coords> result = firstShape().getCoords();
        result.removeAll(secondShape().getCoords());
        return result;
    }
}
