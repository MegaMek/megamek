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

import java.util.HashSet;
import java.util.Set;

import megamek.common.board.Board;
import megamek.common.board.Coords;

/**
 * This class represents a hex area that is a filled "circle" around a center (all hexes up to a given maximum
 * distance).
 */
public class CircleHexArea extends AbstractHexArea {

    private final Coords center;
    private final int radius;

    /**
     * Creates a hex circle around the given center with the given radius. The circle includes all hexes within (it is
     * filled). A radius of 0 is the center only, a radius of 1 includes the hexes adjacent to the center (7 hexes all
     * in all).
     *
     * @param center The center coords
     * @param radius The radius of the circle
     */
    public CircleHexArea(Coords center, int radius) {
        this.center = center;
        this.radius = radius;
    }

    @Override
    public boolean containsCoords(Coords coords, Board board) {
        return matchesBoardId(board) && (coords != null) && (coords.distance(center) <= radius);
    }

    @Override
    public boolean isSmall() {
        return radius < 15;
    }

    @Override
    public Set<Coords> getCoords() {
        if (isSmall()) {
            // TODO: Check if this function is not suffering of an off-by-one error
            return new HashSet<>(center.allLessThanDistance(radius));
        } else {
            return super.getCoords();
        }
    }
}
