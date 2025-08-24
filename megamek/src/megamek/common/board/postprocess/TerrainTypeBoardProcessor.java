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


package megamek.common.board.postprocess;

import megamek.common.Hex;
import megamek.common.units.Terrain;

public class TerrainTypeBoardProcessor extends AbstractSimpleBoardProcessor {

    private final int terrainType;
    private final int newTerrainType;
    private final int fromLevel;
    private final int toLevel;

    /**
     * Creates a board processor that converts the given terrain type to the given new terrain type which must be
     * different. The conversion happens when the given terrain level fromLevel is matched or the fromLevel is
     * Terrain.WILDCARD. When the given toLevel is Terrain.WILDCARD, the level is kept unchanged, otherwise it is set to
     * toLevel. Note that exits settings are not moved to the new terrain type. Note that the levels are not checked for
     * validity.
     *
     * @param terrainType    the terrain type to change
     * @param fromLevel      the level that is to be converted; may be Terrain.WILDCARD to affect any terrain level
     * @param newTerrainType the new terrain type to change to
     * @param toLevel        the level that replaces the fromLevel; may be Terrain.WILDCARD to keep the terrain level
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
     * Creates a board processor that converts the given terrain type to the given new terrain type which must be
     * different. The terrain level is unaffected (kept the same). Note that exits settings are not moved to the new
     * terrain type. Note that the levels are not checked for validity.
     *
     * @param terrainType    the terrain type to change
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

