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
package megamek.common.hexarea;

import megamek.common.Board;
import megamek.common.Coords;
import megamek.common.Hex;

public class TerrainHexArea extends AbstractHexArea {

    private final int terrainType;
    private final int minLevel;
    private final int maxLevel;
    private final int minDistance;
    private final int maxDistance;

    public TerrainHexArea(int terrainType, int minLevel, int maxLevel, int minDistance, int maxDistance) {
        this.terrainType = terrainType;
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
        this.minDistance = minDistance;
        this.maxDistance = maxDistance;
    }

    public TerrainHexArea(int terrainType) {
        this(terrainType, Integer.MIN_VALUE, Integer.MAX_VALUE, 0, 0);
    }

    public TerrainHexArea(int terrainType, int terrainLevel) {
        this(terrainType, terrainLevel, terrainLevel, 0, 0);
    }

    public TerrainHexArea(int terrainType, int minLevel, int maxLevel) {
        this(terrainType, minLevel, maxLevel, 0, 0);
    }

    @Override
    public boolean containsCoords(Coords coords, Board board) {
        if (maxDistance == 0) {
            return board.contains(coords) && hasCorrectTerrain(board.getHex(coords));
        } else {
            for (int distance = minDistance; distance <= maxDistance; distance++) {
                for (Coords coordsToCheck : coords.allAtDistance(distance)) {
                    if (hasCorrectTerrain(board.getHex(coordsToCheck))) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    private boolean hasCorrectTerrain(Hex hex) {
        return hex.containsTerrain(terrainType)
            && (hex.terrainLevel(terrainType) >= minLevel) && (hex.terrainLevel(terrainType) <= maxLevel);
    }
}
