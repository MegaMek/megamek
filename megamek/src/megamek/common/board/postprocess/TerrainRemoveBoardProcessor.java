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

import megamek.common.Hex;
import megamek.common.Terrain;
import megamek.common.Terrains;

public class TerrainRemoveBoardProcessor extends AbstractSimpleBoardProcessor {

    private final int terrainType;
    private final int terrainLevel;

    /**
     * Creates a Board Processor that removes the given terrain type from all hexes of the board regardless of
     * its terrain level.
     *
     * @param terrainType The terrain type to remove
     * @see Terrains
     */
    public TerrainRemoveBoardProcessor(int terrainType) {
        this(terrainType, Terrain.WILDCARD);
    }

    /**
     * Creates a Board Processor that removes the given terrain type from all hexes of the board where it has
     * the given terrain level.
     *
     * @param terrainType The terrain type to remove
     * @see Terrains
     */
    public TerrainRemoveBoardProcessor(int terrainType, int terrainLevel) {
        this.terrainType = terrainType;
        this.terrainLevel = terrainLevel;
    }

    @Override
    public void processHex(Hex hex) {
        if ((terrainLevel == Terrain.WILDCARD) || (hex.terrainLevel(terrainType) == terrainLevel)) {
            hex.removeTerrain(terrainType);
        }
    }
}
