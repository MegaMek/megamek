/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
        return size - 1;
    }

    public int getSouthMostPosition() {
        return 0;
    }
}
