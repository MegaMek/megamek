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

import megamek.common.board.Board;
import megamek.common.board.Coords;

/**
 * This area is defined by the hex level (aka floor height) being in the range defined by the given level(s).
 */
public class HexLevelArea extends AbstractHexArea {

    private final int minLevel;
    private final int maxLevel;

    /**
     * Creates an area defined by the hex level (aka floor height) being in the range defined by the given levels.
     * maxLevel must be equal or greater than minLevel or the area is empty.
     *
     * @param minLevel The minimum hex level to include in the area
     * @param maxLevel The maximum hex level to include in the area
     */
    public HexLevelArea(int minLevel, int maxLevel) {
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
    }

    /**
     * Creates an area defined by the hex level (aka floor height) being equal to the given level.
     *
     * @param level The hex level to include in the area
     */
    public HexLevelArea(int level) {
        this(level, level);
    }

    @Override
    public boolean containsCoords(Coords coords, Board board) {
        return matchesBoardId(board) && board.contains(coords) && (board.getHex(coords).getLevel() >= minLevel) && (
              board.getHex(coords).getLevel()
                    <= maxLevel);
    }
}
