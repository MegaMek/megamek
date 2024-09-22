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
import megamek.common.annotations.Nullable;

import java.util.Set;

public interface HexAreaShape {

    /**
     * @return True if this shape contains the given coords. Returns false when the given coords is null.
     */
    boolean containsCoords(@Nullable Coords coords);

    /**
     * @return True if this shape is, by itself, finite and small enough that its coords can
     * be given directly. If false, its coords cannot be retrieved, only containsCoords can be
     * used. Always call this method before calling getCoords.
     * <BR>
     * By default, this method returns false.
     * <BR>
     * This method may be overridden to return true for finite, small shapes, such as a hex circle
     * of diameter 4. In that case, getCoords must also be overriden to return the coords of this
     * shape.
     */
    default boolean isSmall() {
        // Some shapes, even if finite, have 10000 or more Coords. It may be good to avoid retrieving
        // those to find the resulting Coords on a small board.
        // On the other hand, the board may have 10000 hexes and this shape may only have a handful,
        // making it better to process these coords directly rather than cycle the whole board.
        // This method exists so both cases can be dealt with efficiently.
        return false;
    }

    /**
     * Returns the set of coords of this shape, if it is finite and small enough. Throws an exception
     * by default. Only use this when isSmall() returns true!
     *
     * @return
     */
    default Set<Coords> getCoords() {
        throw new IllegalArgumentException("Can only be used on small, finite shapes.");
    }
}
