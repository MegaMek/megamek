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

public class TerrainLevelBoardProcessor extends AbstractSimpleBoardProcessor {

    private final int terrainType;
    private final int fromLevel;
    private final int toLevel;

    /**
     * Creates a level converter for terrain that converts the level of the given terrain type from the
     * given fromLevel to the given toLevel. Note that terrain type and the levels are not checked for
     * validity.
     *
     * @param terrainType the terrain type
     * @param fromLevel the level that is to be converted; may be Terrain.WILDCARD to affect any terrain level
     * @param toLevel the level that replaces the fromLevel
     */
    public TerrainLevelBoardProcessor(int terrainType, int fromLevel, int toLevel) {
        this.terrainType = terrainType;
        this.fromLevel = fromLevel;
        this.toLevel = toLevel;
    }

    @Override
    public void processHex(Hex hex) {
        if (hasSuitableTerrain(hex)) {
            boolean exitsSpecified = hex.getTerrain(terrainType).hasExitsSpecified();
            int exits = hex.getTerrain(terrainType).getExits();
            hex.addTerrain(new Terrain(terrainType, toLevel, exitsSpecified, exits));
        }
    }

    private boolean hasSuitableTerrain(Hex hex) {
        return ((fromLevel == Terrain.WILDCARD) && hex.containsTerrain(terrainType))
                || hex.containsTerrain(terrainType, fromLevel);
    }
}
