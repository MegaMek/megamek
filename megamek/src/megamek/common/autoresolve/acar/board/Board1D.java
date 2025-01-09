/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 */

package megamek.common.autoresolve.acar.board;

import megamek.common.Board;

public class Board1D {

    private enum Type {
        GROUND,
        ATMOSPHERE,
        SPACE
    }

    private enum TerrainType {
        URBAN,
        LIGHT_WOOD,
        HEAVY_WOODS,
        JUNGLE,
        SHALLOW_WATER,
        DEEP_WATER,
        MARSH,
        SWAMP,
        LAVA,
        THIN_ICE,
        THICK_ICE,
        DESERT,
        SNOW,
        ROUGH,
    }

    private final int size;

    public Board1D(Board board) {
        var c1 = board.getHeight() * board.getHeight();
        var c2 = board.getWidth() * board.getWidth();
        this.size = (int) Math.sqrt(c1 + c2);
    }

    public int getSize() {
        return size;
    }

    public int getNorthMostPosition() {
        return size-1;
    }

    public int getSouthMostPosition() {
        return 0;
    }
}
