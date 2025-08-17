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
import megamek.common.units.Terrain;

public class TerrainAddPostProcessor implements BoardProcessor {

    private final int terrainType;
    private final int level;
    private final HexArea area;

    /**
     * Creates a board processor that adds the given terrain to each hex of the given HexArea.
     * <p>
     * Note that no exits settings are added. Note also that the level is not checked for validity.
     *
     * @param terrainType the terrain type to add
     * @param level       the terrain level to add
     * @param area        the HexArea to apply the change to
     */
    public TerrainAddPostProcessor(int terrainType, int level, HexArea area) {
        this.terrainType = terrainType;
        this.level = level;
        this.area = area;
    }


    @Override
    public void processBoard(Board board) {
        if (area.matchesBoardId(board)) {
            for (int y = 0; y < board.getHeight(); y++) {
                for (int x = 0; x < board.getWidth(); x++) {
                    if (area.containsCoords(new Coords(x, y), board)) {
                        board.getHex(x, y).addTerrain(new Terrain(terrainType, level));
                    }
                }
            }
        }
    }
}
