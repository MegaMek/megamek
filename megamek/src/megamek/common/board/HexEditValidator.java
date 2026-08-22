/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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

package megamek.common.board;

import java.util.ArrayList;
import java.util.List;

import megamek.common.Hex;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;

/**
 * Says whether a hex a gamemaster is building would be a legal hex, and why not when it would not.
 *
 * <p>The rules live here rather than on either side of the connection because both sides need them and they must
 * agree. The dialog asks as the gamemaster builds an edit, so an illegal hex can be refused before it is sent and the
 * reason shown while there is still something to change; the server asks again when the edit arrives, because it is
 * the server that decides what the board is and the hex may have changed in between.</p>
 */
public final class HexEditValidator {

    /**
     * The terrains that make up a structure. A structure standing in a hex constrains what the ground under it may
     * become, and is not itself changed by a terrain edit.
     */
    private static final List<Integer> STRUCTURE_TERRAINS = List.of(
          Terrains.BUILDING, Terrains.BLDG_CF, Terrains.BLDG_ELEV, Terrains.BLDG_ARMOR, Terrains.BLDG_CLASS,
          Terrains.BLDG_BASEMENT_TYPE, Terrains.BLDG_BASE_COLLAPSED,
          Terrains.BRIDGE, Terrains.BRIDGE_CF, Terrains.BRIDGE_ELEV,
          Terrains.FUEL_TANK, Terrains.FUEL_TANK_CF, Terrains.FUEL_TANK_ELEV, Terrains.FUEL_TANK_MAGN);

    private HexEditValidator() {
    }

    /** @return the terrains that make up a structure, which a terrain edit carries through unchanged */
    public static List<Integer> structureTerrains() {
        return STRUCTURE_TERRAINS;
    }

    /**
     * Lists everything wrong with a hex a gamemaster has built.
     *
     * @param edited The hex as it would be after the edit
     *
     * @return the problems with it, in words a gamemaster can act on; empty when the hex is legal
     */
    public static List<String> problemsWith(Hex edited) {
        List<String> problems = new ArrayList<>();
        edited.isValid(problems);
        if (standsAStructureInWater(edited)) {
            problems.add("A building or fuel tank cannot stand in water.");
        }
        return problems;
    }

    /**
     * Whether the hex would leave a structure standing in water. The hex's own validation does not catch this: it
     * checks that a structure in a hex is complete, not that it is somewhere it could have been built.
     *
     * @param edited The hex as it would be after the edit
     *
     * @return {@code true} if the hex would hold both a structure and water for it to stand in
     */
    public static boolean standsAStructureInWater(Hex edited) {
        boolean hasStructure = edited.containsTerrain(Terrains.BUILDING) || edited.containsTerrain(Terrains.FUEL_TANK);
        return hasStructure && (edited.depth() > 0);
    }

    /**
     * Whether a terrain can be set to a level at all, according to the terrain's own rules about which levels it has.
     * This is what makes the difference between offering a gamemaster Light, Heavy and Ultra Woods and offering them a
     * spinner that runs to six.
     *
     * @param terrainType  The terrain, from {@link Terrains}
     * @param terrainLevel The level to test
     *
     * @return {@code true} when that terrain has that level
     */
    public static boolean isLegalLevel(int terrainType, int terrainLevel) {
        return new Terrain(terrainType, terrainLevel).isValid(null);
    }

    /**
     * The levels a terrain can be set to, in order.
     *
     * @param terrainType   The terrain, from {@link Terrains}
     * @param highestToTest The highest level worth offering; terrains with open-ended levels, such as water depth, are
     *                      cut off here rather than running forever
     *
     * @return the legal levels for that terrain
     */
    public static List<Integer> legalLevelsFor(int terrainType, int highestToTest) {
        List<Integer> levels = new ArrayList<>();
        for (int level = 1; level <= highestToTest; level++) {
            if (isLegalLevel(terrainType, level)) {
                levels.add(level);
            }
        }
        return levels;
    }
}
