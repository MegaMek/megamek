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


package megamek.common.board.postprocess;

import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.hexArea.HexArea;

public class HexLevelPostProcessor implements BoardProcessor {

    private final int level;
    private final HexArea area;

    /**
     * Creates a board processor that sets the hex level of each hex of the given HexArea to the given level.
     *
     * @param level the hex level to set
     * @param area  the HexArea to apply the change to
     */
    public HexLevelPostProcessor(int level, HexArea area) {
        this.level = level;
        this.area = area;
    }


    @Override
    public void processBoard(Board board) {
        if (area.matchesBoardId(board)) {
            for (int y = 0; y < board.getHeight(); y++) {
                for (int x = 0; x < board.getWidth(); x++) {
                    if (area.containsCoords(new Coords(x, y), board)) {
                        board.getHex(x, y).setLevel(level);
                    }
                }
            }
        }
    }
}
