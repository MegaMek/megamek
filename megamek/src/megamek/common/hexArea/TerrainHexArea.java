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

import megamek.common.Hex;
import megamek.common.board.Board;
import megamek.common.board.Coords;

/**
 * This class represents a hex area that is based on the terrain of the hex itself or even terrain in some distance to
 * the present hex. The terrain can be limited to terrain levels and a minimum and maximum distance from the present hex
 * can be used.
 */
public class TerrainHexArea extends AbstractHexArea {

    private final int terrainType;
    private final int minLevel;
    private final int maxLevel;
    private final int minDistance;
    private final int maxDistance;

    /**
     * Creates a hex area that includes hexes of the board that have the given terrain type of the given minimum and
     * maximum level in hexes that are the given minimum to maximum distance away. *NOTE* be careful with distances
     * other than 0 as this multiplies calculation times.
     *
     * @param terrainType The terrain type, e.g. Terrains.WATER
     * @param minLevel    the minimum terrain level
     * @param maxLevel    the maximum terrain level
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
     * @param terrainType  The terrain type, e.g. Terrains.WATER
     * @param terrainLevel The terrain level
     */
    public TerrainHexArea(int terrainType, int terrainLevel) {
        this(terrainType, terrainLevel, terrainLevel, 0, 0);
    }

    /**
     * Creates a hex area that includes hexes of the board that have the given terrain type of the given minimum and
     * maximum level.
     *
     * @param terrainType The terrain type, e.g. Terrains.WATER
     * @param minLevel    the minimum terrain level
     * @param maxLevel    the maximum terrain level
     */
    public TerrainHexArea(int terrainType, int minLevel, int maxLevel) {
        this(terrainType, minLevel, maxLevel, 0, 0);
    }

    @Override
    public boolean containsCoords(Coords coords, Board board) {
        if (!matchesBoardId(board)) {
            return false;
        } else if (maxDistance == 0) {
            return board.contains(coords) && hasCorrectTerrain(board.getHex(coords));
        } else {
            for (int distance = minDistance; distance <= maxDistance; distance++) {
                for (Coords coordsToCheck : coords.allAtDistance(distance)) {
                    if (board.contains(coordsToCheck) && hasCorrectTerrain(board.getHex(coordsToCheck))) {
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
