/*
 * Copyright (C) 2014-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.common.pathfinder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import megamek.common.board.Coords;
import megamek.common.moves.MovePath;

/**
 * Node defined by coordinates and unit facing.
 *
 * @author Saginatio
 */
public record CoordsWithFacing(Coords coords, int facing) {
    /**
     * Returns a list containing six instances of CoordsWithFacing, one for each facing.
     *
     */
    public static List<CoordsWithFacing> getAllFacingsAt(Coords c) {
        List<CoordsWithFacing> allFacings = new ArrayList<>();
        for (int f = 0; f < 6; f++) {
            allFacings.add(new CoordsWithFacing(c, f));
        }
        return allFacings;
    }

    public CoordsWithFacing {
        if (coords == null) {
            throw new NullPointerException();
        }
    }

    public CoordsWithFacing(MovePath mp) {
        this(mp.getFinalCoords(), mp.getFinalFacing());
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CoordsWithFacing t)) {
            return false;
        }
        return (facing == t.facing) && Objects.equals(coords, t.coords);
    }

    @Override
    public String toString() {
        return String.format("%s f:%d", coords, facing);
    }
}
