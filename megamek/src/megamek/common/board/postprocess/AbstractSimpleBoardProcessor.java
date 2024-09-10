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
package megamek.common.board.postprocess;

import megamek.common.Board;
import megamek.common.Hex;

/**
 * This is a base class for simple BoardPostProcessors where each hex can be processed independently
 * from other hexes. An example is a processor that converts all woods hexes to heavy woods or
 * removes all water.
 */
public abstract class AbstractSimpleBoardProcessor implements BoardProcessor {

    @Override
    public final void processBoard(Board board) {
        for (int y = 0; y < board.getHeight(); y++) {
            for (int x = 0; x < board.getWidth(); x++) {
                processHex(board.getHex(x, y));
            }
        }
    }

    /**
     * Processes a single hex of the board. This method is called for each hex of the board.
     *
     * @param hex The hex to process
     */
    public abstract void processHex(Hex hex);
}
