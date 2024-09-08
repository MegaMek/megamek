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

public class TerrainTypeBoardProcessor extends AbstractSimpleBoardProcessor {

    private final int terrainType;
    private final int newTerrainType;
    private final int fromLevel;
    private final int toLevel;

    /**
     * Creates a board processor that converts the given terrain type to the given new terrain type which must
     * be different. The conversion happens when the given terrain level fromLevel is matched or the
     * fromLevel is Terrain.WILDCARD. When the given toLevel is Terrain.WILDCARD, the level is kept unchanged,
     * otherwise it is set to toLevel.
     * Note that exits settings are not moved to the new terrain type.
     * Note that the levels are not checked for validity.
     *
     * @param terrainType the terrain type to change
     * @param fromLevel the level that is to be converted; may be Terrain.WILDCARD to affect any terrain level
     * @param newTerrainType the new terrain type to change to
     * @param toLevel the level that replaces the fromLevel; may be Terrain.WILDCARD to keep the terrain level
     */
    public TerrainTypeBoardProcessor(int terrainType, int fromLevel, int newTerrainType, int toLevel) {
        if (terrainType == newTerrainType) {
            throw new IllegalArgumentException("New terrain type cannot be the same as the old terrain type");
        }
        this.terrainType = terrainType;
        this.newTerrainType = newTerrainType;
        this.fromLevel = fromLevel;
        this.toLevel = toLevel;
    }

    /**
     * Creates a board processor that converts the given terrain type to the given new terrain type which must
     * be different. The terrain level is unaffected (kept the same).
     * Note that exits settings are not moved to the new terrain type.
     * Note that the levels are not checked for validity.
     *
     * @param terrainType the terrain type to change
     * @param newTerrainType the new terrain type to change to
     */
    public TerrainTypeBoardProcessor(int terrainType, int newTerrainType) {
        this(terrainType, Terrain.WILDCARD, newTerrainType, Terrain.WILDCARD);
    }

    @Override
    public void processHex(Hex hex) {
        if (hasSuitableTerrain(hex)) {
            int newLevel = (toLevel == Terrain.WILDCARD) ? hex.terrainLevel(terrainType) : toLevel;
            hex.addTerrain(new Terrain(newTerrainType, newLevel));
            hex.removeTerrain(terrainType);
        }
    }

    private boolean hasSuitableTerrain(Hex hex) {
        return ((fromLevel == Terrain.WILDCARD) && hex.containsTerrain(terrainType))
                || hex.containsTerrain(terrainType, fromLevel);
    }
}

