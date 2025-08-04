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
import megamek.common.Terrains;

public class TerrainRemoveBoardProcessor extends AbstractSimpleBoardProcessor {

    private final int terrainType;
    private final int terrainLevel;

    /**
     * Creates a Board Processor that removes the given terrain type from all hexes of the board regardless of its
     * terrain level.
     *
     * @param terrainType The terrain type to remove
     *
     * @see Terrains
     */
    public TerrainRemoveBoardProcessor(int terrainType) {
        this(terrainType, Terrain.WILDCARD);
    }

    /**
     * Creates a Board Processor that removes the given terrain type from all hexes of the board where it has the given
     * terrain level.
     *
     * @param terrainType The terrain type to remove
     *
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
