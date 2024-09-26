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

import java.util.HashSet;
import java.util.Set;

public class CircleHexArea extends AbstractHexArea {

    private final Coords center;
    private final int radius;

    public CircleHexArea(Coords center, int radius) {
        this.center = center;
        this.radius = radius;
    }

    @Override
    public boolean containsCoords(Coords coords, Board board) {
        return (coords != null) && coords.distance(center) <= radius;
    }

    @Override
    public boolean isSmall() {
        return radius < 15;
    }

    @Override
    public Set<Coords> getCoords() {
        if (isSmall()) {
            return new HashSet<>(center.allAtDistanceOrLess(radius));
        } else {
            return super.getCoords();
        }
    }
}
