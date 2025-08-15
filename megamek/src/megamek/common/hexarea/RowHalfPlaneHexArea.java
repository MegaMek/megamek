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

package megamek.common.hexarea;

import java.util.Objects;

import megamek.client.bot.princess.geometry.HexLine;
import megamek.common.Board;
import megamek.common.Coords;

/**
 * This class represents a half plane shape. The plane is delimited by a hex line in any hex row direction and extends
 * either to the left or right of the line. When the line is vertical (direction 0 or 3), the result is the same as a
 * vertical HexHalfPlaneShape. When the line is tilted (directions 1, 2, 4 and 5), the plane extends to the (lower or
 * upper) right or left.
 */
public class RowHalfPlaneHexArea extends AbstractHexArea {

    public enum HalfPlaneType {RIGHT, LEFT}

    private final HalfPlaneType planeDirection;
    private final HexLine hexLine;

    /**
     * Creates a half plane, delimited by a hex line through the given point going in the given direction, wherein the
     * plane extends to either the left or right as given by the planeDirection. Opposite directions (e.g., 0 and 3)
     * result in the same half plane.
     *
     * @param point          A hex that the hex line goes through
     * @param direction      The direction of the hex line, 0 = N, 2 = SE ...
     * @param planeDirection The direction the plane extends to
     */
    public RowHalfPlaneHexArea(Coords point, int direction, HalfPlaneType planeDirection) {
        this.planeDirection = Objects.requireNonNull(planeDirection);
        hexLine = new HexLine(Objects.requireNonNull(point), direction);
    }

    @Override
    public boolean containsCoords(Coords coords, Board board) {
        int comparison = hexLine.isAbsoluteLeftOrRight(coords);
        return matchesBoardId(board) && ((comparison == 0)
              || ((comparison == 1) && (planeDirection == HalfPlaneType.RIGHT))
              || ((comparison == -1) && (planeDirection == HalfPlaneType.LEFT)));
    }
}
