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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import megamek.common.board.Board;
import megamek.common.board.Coords;

/**
 * This is a base class for HexAreas that provides an implementation for {@link #getCoords(Board)}. The
 * {@link #isSmall()} method can be overridden when a shape has not too many hexes and is independent of the board. A
 * HexArea composed of only small shapes can be evaluated quickly even on a big board.
 */
abstract class AbstractHexArea implements HexArea {

    /**
     * The board IDs that this area is on. When empty, the area is on all boards.
     */
    private final List<Integer> boardIds = new ArrayList<>();

    /**
     * @return True if this shape is, by itself, finite and small enough and absolute (independent of a board) that its
     *       coords can be given directly. If false, its coords cannot be retrieved, only
     *       {@link #containsCoords(Coords, Board)} can be used. Always call this method and only if it returns true,
     *       call {@link #getCoords()}. By default, this method returns false. It may be overridden to return true for
     *       finite, small shapes, such as a hex circle of diameter 4. In that case, getCoords must also be overridden
     *       to return the coords of this shape.
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
     * Returns all coords of this shape, if it is finite and small enough and an absolute shape. Only use this when
     * {@link #isSmall()} returns true - it will throw an exception otherwise. Throws an exception by default. Override
     * together with {@link #isSmall()} for small board-independent shapes.
     * <p>
     * Note that this is independent of the board - this means that a) not all (or even any) of the coords may actually
     * lie within any of the boards (i.e., use {@link Board#contains(Coords)}) and b) the area itself may not be active
     * for all boards (i.e., use {@link #matchesBoardId(Board)}).
     *
     * @return All Coords of this shape
     *
     * @throws IllegalStateException when this method is called on a shape where {@link #isSmall()} returns false
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

    @Override
    public boolean matchesBoardId(Board board) {
        return boardIds.isEmpty() || boardIds.contains(board.getBoardId());
    }

    @Override
    public void setBoardIds(List<Integer> boardIds) {
        this.boardIds.clear();
        this.boardIds.addAll(boardIds);
    }
}
