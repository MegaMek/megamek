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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

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
     * @param coords A coord to form the shape
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
        return coordList.contains(coords);
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
