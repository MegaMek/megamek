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
 * This class represents a half plane shape. The plane is delimited by the given coordinate, which is
 * either a hex column or row (x or y) depending on the halfPlaneDirection given. A half plane with
 * coordinate 5 and direction ABOVE extends from Coords (x, 5) upwards, i.e. (0, 0) is within that plane,
 * (0, 10) is not. The given coordinate itself is part of the half plane.
 *
 * @param coordinate The x or y value where the half plane starts/ends
 * @param halfPlaneDirection The direction in which the half plane extends
 */
public record HexHalfPlaneShape(int coordinate, HalfPlaneType halfPlaneDirection) implements HexAreaShape {

    public enum HalfPlaneType { ABOVE, BELOW, RIGHT, LEFT }

    @Override
    public boolean containsCoords(Coords coords) {
        return switch (halfPlaneDirection) {
            case ABOVE -> coords.getY() <= coordinate;
            case BELOW -> coords.getY() >= coordinate;
            case RIGHT -> coords.getX() >= coordinate;
            case LEFT -> coords.getX() <= coordinate;
        };
    }
}
