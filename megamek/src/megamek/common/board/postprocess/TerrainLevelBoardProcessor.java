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
import megamek.common.Terrain;

public class TerrainLevelBoardProcessor extends AbstractSimpleBoardProcessor {

    private final int terrainType;
    private final int fromLevel;
    private final int toLevel;

    /**
     * Creates a level converter for terrain that converts the level of the given terrain type from the given fromLevel
     * to the given toLevel. Note that terrain type and the levels are not checked for validity.
     *
     * @param terrainType the terrain type
     * @param fromLevel   the level that is to be converted; may be Terrain.WILDCARD to affect any terrain level
     * @param toLevel     the level that replaces the fromLevel
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
