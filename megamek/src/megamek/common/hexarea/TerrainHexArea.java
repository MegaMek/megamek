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

/**
 * This class represents a hex area that is based on the terrain of the hex itself or even terrain in some distance to the present hex. The
 * terrain can be limited to terrain levels and a minimum and maximum distance from the present hex can be used.
 */
public class TerrainHexArea extends AbstractHexArea {

    private final int terrainType;
    private final int minLevel;
    private final int maxLevel;
    private final int minDistance;
    private final int maxDistance;

    /**
     * Creates a hex area that includes hexes of the board that have the given terrain type of the given minimum and maximum level in hexes
     * that are the given minimum to maximum distance away. *NOTE* be careful with distances other than 0 as this multiplies calculation
     * times.
     *
     * @param terrainType The terrain type, e.g. Terrains.WATER
     * @param minLevel the minimum terrain level
     * @param maxLevel the maximum terrain level
     * @param minDistance the minimum distance to the present hex when detecting included hexes
     * @param maxDistance the maximum distance to the present hex when detecting included hexes
     */
    public TerrainHexArea(int terrainType, int minLevel, int maxLevel, int minDistance, int maxDistance) {
        this.terrainType = terrainType;
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
        this.minDistance = minDistance;
        this.maxDistance = maxDistance;
    }

    /**
     * Creates a hex area that includes hexes of the board that have the given terrain type.
     *
     * @param terrainType The terrain type, e.g. Terrains.WATER
     */
    public TerrainHexArea(int terrainType) {
        this(terrainType, 0, Integer.MAX_VALUE, 0, 0);
    }

    /**
     * Creates a hex area that includes hexes of the board that have the given terrain type of the given level-
     *
     * @param terrainType The terrain type, e.g. Terrains.WATER
     * @param terrainLevel The terrain level
     */
    public TerrainHexArea(int terrainType, int terrainLevel) {
        this(terrainType, terrainLevel, terrainLevel, 0, 0);
    }

    /**
     * Creates a hex area that includes hexes of the board that have the given terrain type of the given minimum and maximum level.
     *
     * @param terrainType The terrain type, e.g. Terrains.WATER
     * @param minLevel the minimum terrain level
     * @param maxLevel the maximum terrain level
     */
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
