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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import megamek.common.board.Board;
import megamek.common.board.Coords;

/**
 * This class represents a shape formed by a given list of one or more Coords.
 */
public class ListHexArea extends AbstractHexArea {

    private final Set<Coords> coordList = new HashSet<>();

    /**
     * Creates a shape containing the given coords. The coords in the list need not be contiguous.
     *
     * @param coordList A collection of coords to form the shape
     */
    public ListHexArea(Collection<Coords> coordList) {
        this.coordList.addAll(coordList);
    }

    /**
     * Creates a shape containing the given coord(s). The coords in the list need not be contiguous.
     *
     * @param coords     A coord to form the shape
     * @param moreCoords optional further coords to form the shape
     */
    public ListHexArea(Coords coords, Coords... moreCoords) {
        coordList.add(coords);
        if ((moreCoords != null) && moreCoords.length > 0) {
            coordList.addAll(Arrays.asList(moreCoords));
        }
    }

    @Override
    public boolean containsCoords(Coords coords, Board board) {
        return matchesBoardId(board) && coordList.contains(coords);
    }

    @Override
    public boolean isSmall() {
        return coordList.size() < 1000;
    }

    @Override
    public Set<Coords> getCoords() {
        return new HashSet<>(coordList);
    }
}
