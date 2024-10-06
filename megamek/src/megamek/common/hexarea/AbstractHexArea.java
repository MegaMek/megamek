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
import java.util.stream.Collectors;

/**
 * This is a base class for HexAreas that provides an implementation for {@link #getCoords(Board)}. The {@link #isSmall()} method can be
 * overridden when a shape has not too many hexes and is independent of the board. A HexArea composed of only small shapes can be evaluated
 * quickly even on a big board.
 */
abstract class AbstractHexArea implements HexArea {

    /**
     * @return True if this shape is, by itself, finite and small enough and absolute (independent of a board) that its coords can be given
     * directly. If false, its coords cannot be retrieved, only {@link #containsCoords(Coords, Board)} can be used. Always call this method
     * and only if it returns true, call {@link #getCoords()}.
     * @apiNote By default, this method returns false. It may be overridden to return true for finite, small shapes, such as a hex circle of
     * diameter 4. In that case, getCoords must also be overriden to return the coords of this shape.
     */
    boolean isSmall() {
        // Some shapes, even if finite, have 10000 or more Coords. It may be good to avoid retrieving
        // those to find the resulting Coords on a small board.
        // On the other hand, the board may have 10000 hexes and this shape may only have a handful,
        // making it better to process these coords directly rather than cycle the whole board.
        // This method exists so both cases can be dealt with as efficiently as possible.
        return false;
    }

    /**
     * Returns all coords of this shape, if it is finite and small enough and an absolute shape. Only use this when {@link #isSmall()}
     * returns true - it will throw an exception otherwise.
     *
     * @return All Coords of this shape
     * @throws IllegalStateException when this method is called on a shape where {@link #isSmall()} returns false
     * @apiNote Throws an exception by default. Override together with {@link #isSmall()} for small board-independent shapes.
     */
    Set<Coords> getCoords() {
        throw new IllegalStateException("Can only be used on small, finite shapes.");
    }

    @Override
    public final Set<Coords> getCoords(Board board) {
        if (isSmall()) {
            return getCoords().stream().filter(board::contains).collect(Collectors.toSet());
        } else {
            Set<Coords> result = new HashSet<>();
            for (int y = 0; y < board.getHeight(); y++) {
                for (int x = 0; x < board.getWidth(); x++) {
                    Coords coords = new Coords(x, y);
                    if (containsCoords(coords, board)) {
                        result.add(coords);
                    }
                }
            }
            return result;
        }
    }
}
