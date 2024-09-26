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

import megamek.client.bot.princess.BotGeometry;
import megamek.common.Board;
import megamek.common.Coords;

import java.util.Objects;

/**
 * This class represents a half plane shape. The plane is delimited by a hex line in any hex row direction
 * and extends either to the left or right of the line. When the line is vertical (direction 0 or 3), the result
 * is the same as a vertical HexHalfPlaneShape. When the line is tilted (directions 1, 2, 4 and 5), the plane
 * extends to the (lower or upper) right or left.
 */
public class RowHalfPlaneHexArea extends AbstractHexArea {

    public enum HalfPlaneType { RIGHT, LEFT }

    private final HalfPlaneType planeDirection;
    private final BotGeometry.HexLine hexLine;

    /**
     * Creates a half plane, delimited by a hex line through the given point going in the given direction,
     * wherein the plane extends to either the left or right as given by the planeDirection. Opposite directions
     * (e.g., 0 and 3) result in the same half plane.
     *
     * @param point A hex that the hex line goes through
     * @param direction The direction of the hex line, 0 = N, 2 = SE ...
     * @param planeDirection The direction the plane extends to
     */
    public RowHalfPlaneHexArea(Coords point, int direction, HalfPlaneType planeDirection) {
        this.planeDirection = Objects.requireNonNull(planeDirection);
        hexLine = new BotGeometry.HexLine(Objects.requireNonNull(point), direction);
    }

    @Override
    public boolean containsCoords(Coords coords, Board board) {
        int comparison = hexLine.isAbsoluteLeftOrRight(coords);
        return (comparison == 0)
            || ((comparison == 1) && (planeDirection == HalfPlaneType.RIGHT))
            || ((comparison == -1) && (planeDirection == HalfPlaneType.LEFT));
    }
}
