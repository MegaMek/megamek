/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
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
import megamek.common.Coords;
import megamek.common.Hex;
import megamek.common.Terrain;
import megamek.common.hexarea.HexArea;

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

    private void processHex(Hex hex) {
        hex.addTerrain(new Terrain(terrainType, level));
    }
}
