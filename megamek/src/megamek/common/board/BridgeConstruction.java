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

import megamek.common.Hex;
import megamek.common.annotations.Nullable;
import megamek.common.enums.BasementType;
import megamek.common.units.BuildingTerrain;
import megamek.common.units.IBuilding;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;
import megamek.logging.MMLogger;

/**
 * Validates sites for and places single-hex bridges that are constructed during a game, as opposed to bridges that are
 * part of the board file. Used by Bridge-Building Engineer infantry (TO:AUE) and intended for reuse by vehicle
 * Bridge-Layer equipment (TM p.242), whose placement rules this class implements: a bridge may be placed in any water
 * hex that connects to at least one land hex or another bridge, or across a gap whose banks differ by no more than one
 * level.
 * <p>
 * The Construction Factor of the new bridge is supplied by the caller, since it differs by source: engineers raise
 * bridges with CF 15/40 (doubled over water), while Bridge-Layer equipment deploys bridges with its own CF values and
 * no water doubling.
 */
public final class BridgeConstruction {

    private static final MMLogger LOGGER = MMLogger.create(BridgeConstruction.class);

    /** The maximum level difference between the two banks a constructed bridge connects, TM p.242. */
    public static final int MAX_BANK_LEVEL_DIFFERENCE = 1;

    private BridgeConstruction() {
    }

    /**
     * @param exits the exits bitmask to check
     *
     * @return {@code true} if the bitmask describes a legal single-hex bridge orientation: exactly two connected
     *       hexsides, opposite each other. A constructed bridge cannot bend, TM p.242.
     */
    public static boolean isValidBridgeExits(int exits) {
        for (int direction = 0; direction < 3; direction++) {
            int oppositePair = (1 << direction) | (1 << (direction + 3));
            if (exits == oppositePair) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether a single-hex bridge with the given orientation may be constructed in the target hex: the hex must
     * exist, not already hold a structure, both connected banks must be on the board with a level difference of at most
     * {@link #MAX_BANK_LEVEL_DIFFERENCE}, and a bridge over water must connect to at least one land hex or another
     * bridge. TM p.242.
     *
     * @param board  the board to build on
     * @param target the hex the bridge would be placed in, or null (returns {@code false})
     * @param exits  exits bitmask of the two hexsides the bridge would connect
     *
     * @return {@code true} if the bridge may be constructed there.
     */
    public static boolean isValidBridgeSite(Board board, @Nullable Coords target, int exits) {
        if ((target == null) || !board.contains(target) || !isValidBridgeExits(exits)) {
            return false;
        }
        Hex targetHex = board.getHex(target);
        if ((targetHex == null) || targetHex.containsAnyTerrainOf(Terrains.BRIDGE, Terrains.BUILDING,
              Terrains.FUEL_TANK)) {
            return false;
        }

        Coords firstBankCoords = target.translated(firstExitDirection(exits));
        Coords secondBankCoords = target.translated(firstExitDirection(exits) + 3);
        if (!board.contains(firstBankCoords) || !board.contains(secondBankCoords)) {
            return false;
        }
        Hex firstBank = board.getHex(firstBankCoords);
        Hex secondBank = board.getHex(secondBankCoords);

        int levelDifference = Math.abs(bankSurfaceLevel(firstBank) - bankSurfaceLevel(secondBank));
        if (levelDifference > MAX_BANK_LEVEL_DIFFERENCE) {
            return false;
        }

        // A bridge over water must connect to at least one land hex or another bridge, TM p.242
        if (isOverWater(targetHex)) {
            boolean firstBankConnects = firstBank.containsTerrain(Terrains.BRIDGE) || !isOverWater(firstBank);
            boolean secondBankConnects = secondBank.containsTerrain(Terrains.BRIDGE) || !isOverWater(secondBank);
            return firstBankConnects || secondBankConnects;
        }
        return true;
    }

    /**
     * @param hex the hex to check
     *
     * @return {@code true} if the hex holds water of depth 1 or more. Engineers double the CF of a bridge raised over
     *       water, TO:AUE; depth 0 water is treated as land.
     */
    public static boolean isOverWater(Hex hex) {
        return hex.terrainLevel(Terrains.WATER) > 0;
    }

    /**
     * Places a finished single-hex bridge in the target hex: adds the bridge terrain, registers the bridge as a board
     * structure so it can take damage and collapse, and recomputes terrain exits around the hex. The bridge surface
     * sits level with the lower of the two connected banks. The caller is responsible for validating the site with
     * {@link #isValidBridgeSite(Board, Coords, int)} and for sending the changed hex and the new building to the
     * clients.
     *
     * @param board      the board to build on
     * @param target     the hex the bridge is placed in
     * @param exits      exits bitmask of the two hexsides the bridge connects
     * @param bridgeType the bridge type as a {@link megamek.common.enums.BuildingType} value (1 = light, 2 = medium)
     * @param cf         the Construction Factor of the new bridge
     *
     * @return The newly registered bridge structure, for sending to the clients.
     */
    public static IBuilding placeBridge(Board board, Coords target, int exits, int bridgeType, int cf) {
        Hex targetHex = board.getHex(target);
        Hex firstBank = board.getHex(target.translated(firstExitDirection(exits)));
        Hex secondBank = board.getHex(target.translated(firstExitDirection(exits) + 3));

        int surfaceLevel = Math.min(bankSurfaceLevel(firstBank), bankSurfaceLevel(secondBank));
        int bridgeElevation = Math.max(0, surfaceLevel - targetHex.getLevel());

        targetHex.addTerrain(new Terrain(Terrains.BRIDGE, bridgeType, true, exits & 63));
        targetHex.addTerrain(new Terrain(Terrains.BRIDGE_ELEV, bridgeElevation));
        targetHex.addTerrain(new Terrain(Terrains.BRIDGE_CF, cf));
        board.initializeAround(target.getX(), target.getY());

        IBuilding bridge = new BuildingTerrain(target, board, Terrains.BRIDGE, BasementType.NONE);
        board.addBuildingToBoard(bridge);
        LOGGER.info("[BuildBridge] placed a type-{} bridge at {}: CF {}, elevation {}, exits bitmask {}, "
              + "registered as structure", bridgeType, target, cf, bridgeElevation, exits & 63);
        return bridge;
    }

    /**
     * @param exits exits bitmask with exactly two opposite hexsides set
     *
     * @return The lower of the two exit directions (0-2).
     */
    private static int firstExitDirection(int exits) {
        return Integer.numberOfTrailingZeros(exits);
    }

    /**
     * @param bank a hex a constructed bridge would connect to
     *
     * @return The surface level units would cross the bridge at from this bank: the bridge surface for a hex holding a
     *       bridge, the hex level otherwise (for water, its surface).
     */
    private static int bankSurfaceLevel(Hex bank) {
        if (bank.containsTerrain(Terrains.BRIDGE)) {
            return bank.getLevel() + bank.terrainLevel(Terrains.BRIDGE_ELEV);
        }
        return bank.getLevel();
    }
}
