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
 * part of the board file. Used by Bridge-Building Engineer infantry (TO:AUE p.152) and intended for reuse by vehicle
 * Bridge-Layer equipment (TM p.242). Placement rules (TO:AUE p.152): a bridge may be placed in any water hex that
 * connects to at least one land hex or another bridge, or across a dry canyon of any depth (a hex below its rims). A
 * single-hex span changes at most one level (the multi-level bridge rule of at most 1 level per hex), so its two
 * mount banks must be within one level; a deeper canyon-floor or deep-water far side is exempt and continued by a
 * further span. A building hex cannot be bridged. Note: ramping multi-hex bridges (a sloped deck) are not modelled -
 * each bridge hex has a single flat deck level.
 * <p>
 * The Construction Factor of the new bridge is supplied by the caller, since it differs by source: engineers raise
 * bridges with CF 15/40 (doubled over water), while Bridge-Layer equipment deploys bridges with its own CF values and
 * no water doubling.
 */
public final class BridgeConstruction {

    private static final MMLogger LOGGER = MMLogger.create(BridgeConstruction.class);

    private BridgeConstruction() {
    }

    /**
     * @param exits the exits bitmask to check
     *
     * @return {@code true} if the bitmask describes a legal single-hex bridge orientation: exactly two distinct
     *       connected hexsides. Opposite sides give a straight bridge; non-opposite sides give a curved bridge. The
     *       finished bridge uses the tileset image matching this exits value.
     */
    public static boolean isValidBridgeExits(int exits) {
        return (exits >= 0) && (exits <= 63) && (Integer.bitCount(exits) == 2);
    }

    /**
     * @param firstDirection  one connected hexside direction (0-5)
     * @param secondDirection the other connected hexside direction (0-5)
     *
     * @return The exits bitmask connecting the two given hexsides.
     */
    public static int exitsFor(int firstDirection, int secondDirection) {
        return (1 << firstDirection) | (1 << secondDirection);
    }

    /**
     * The reason a bridge site is or is not buildable, so callers can explain a rejection to the player. TO:AUE p.152.
     */
    public enum BridgeSiteIssue {
        /** The site is valid. */
        VALID,
        /** The exits are not two distinct hexsides (cannot happen through the normal UI). */
        BAD_EXITS,
        /** The site hex or one of its banks is off the board. */
        OFF_BOARD,
        /** The site hex already holds a bridge, building or fuel tank. */
        OCCUPIED,
        /** Neither bank can anchor the span: flat ground, or water touching only deeper water. */
        NO_ANCHOR,
        /** Both banks are mount points but more than one level apart, so a single flat span cannot reach both. */
        RIMS_TOO_STEEP
    }

    /**
     * Checks whether a single-hex bridge with the given orientation may be constructed in the target hex (TO:AUE
     * p.152): the hex exists and holds no structure, both banks are on the board, and at least one bank anchors the
     * span (land/rim over a canyon, or a bridge). If both banks are mount points they must be within one level (a
     * single-hex span changes at most one level); a deeper canyon-floor or deep-water far side is exempt (chaining).
     *
     * @param board  the board to build on
     * @param target the hex the bridge would be placed in, or null (returns {@code false})
     * @param exits  exits bitmask of the two hexsides the bridge would connect
     *
     * @return {@code true} if the bridge may be constructed there.
     */
    public static boolean isValidBridgeSite(Board board, @Nullable Coords target, int exits) {
        return bridgeSiteIssue(board, target, exits) == BridgeSiteIssue.VALID;
    }

    /**
     * @param board  the board to build on
     * @param target the hex the bridge would be placed in, or null
     * @param exits  exits bitmask of the two hexsides the bridge would connect
     *
     * @return Why the site is or is not buildable (see {@link BridgeSiteIssue}). The single source of truth for bridge
     *       placement validity; {@link #isValidBridgeSite} returns whether this is {@code VALID}.
     */
    public static BridgeSiteIssue bridgeSiteIssue(Board board, @Nullable Coords target, int exits) {
        if (!isValidBridgeExits(exits)) {
            return BridgeSiteIssue.BAD_EXITS;
        }
        if ((target == null) || !board.contains(target)) {
            return BridgeSiteIssue.OFF_BOARD;
        }
        Hex targetHex = board.getHex(target);
        if (targetHex == null) {
            return BridgeSiteIssue.OFF_BOARD;
        }
        if (targetHex.containsAnyTerrainOf(Terrains.BRIDGE, Terrains.BUILDING, Terrains.FUEL_TANK)) {
            return BridgeSiteIssue.OCCUPIED;
        }

        int[] exitDirections = exitDirections(exits);
        Coords firstBankCoords = target.translated(exitDirections[0]);
        Coords secondBankCoords = target.translated(exitDirections[1]);
        if (!board.contains(firstBankCoords) || !board.contains(secondBankCoords)) {
            return BridgeSiteIssue.OFF_BOARD;
        }
        Hex firstBank = board.getHex(firstBankCoords);
        Hex secondBank = board.getHex(secondBankCoords);

        boolean firstAnchors = anchorsBridge(firstBank, targetHex);
        boolean secondAnchors = anchorsBridge(secondBank, targetHex);
        // A bridge needs at least one bank that can anchor it (a rim/land/bridge); flat ground anchors nothing.
        if (!firstAnchors && !secondAnchors) {
            return BridgeSiteIssue.NO_ANCHOR;
        }
        // If both sides are mount/dismount banks, the single-hex span may change at most one level (TO:AuE p.152
        // multi-level bridge rule: at most 1 level of change per hex of bridge), so the banks must be within one
        // level for the flat deck to be reachable from both. Larger climbs are made by chaining spans up terrain
        // that steps one level per hex. The opposite side may instead be deep water or deeper canyon floor (not a
        // mount point) - then there is no level limit and a further span continues the crossing.
        if (firstAnchors && secondAnchors
              && (Math.abs(bankSurfaceLevel(firstBank) - bankSurfaceLevel(secondBank)) > 1)) {
            return BridgeSiteIssue.RIMS_TOO_STEEP;
        }
        return BridgeSiteIssue.VALID;
    }

    /**
     * @param bank      a hex adjacent to a bridge site
     * @param targetHex the bridge site hex
     *
     * @return {@code true} if the bank can anchor a bridge - a unit can get on or off the span there. Over water this
     *       means the bank is dry land (or shallow water) or holds a bridge; over a dry canyon it means the bank is a
     *       rim higher than the canyon floor, or holds a bridge. A deep-water bank, or a canyon-floor bank no higher
     *       than the site, cannot anchor; a span may still point that way to be continued by a further span (wide
     *       rivers/canyons). Flat ground anchors nothing, so a flat site is never valid.
     */
    private static boolean anchorsBridge(Hex bank, Hex targetHex) {
        if (bank.containsTerrain(Terrains.BRIDGE)) {
            return true;
        }
        return isOverWater(targetHex) ? !isOverWater(bank) : (bankSurfaceLevel(bank) > targetHex.getLevel());
    }

    /**
     * @param hex the hex to check
     *
     * @return {@code true} if the hex holds water of depth 1 or more. Engineers double the CF of a bridge raised over
     *       water, TO:AUE p.152; depth 0 water is treated as land.
     */
    public static boolean isOverWater(Hex hex) {
        return hex.terrainLevel(Terrains.WATER) > 0;
    }

    /**
     * Places a finished single-hex bridge in the target hex: adds the bridge terrain, registers the bridge as a board
     * structure so it can take damage and collapse, and recomputes terrain exits around the hex. The deck sits at the
     * lower anchoring rim (the two rims are within one level) so units cross at grade and a chain of spans stays
     * level. The caller is responsible for validating the site with
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
        int[] exitDirections = exitDirections(exits);
        Hex firstBank = board.getHex(target.translated(exitDirections[0]));
        Hex secondBank = board.getHex(target.translated(exitDirections[1]));

        // The deck sits at the highest anchoring bank (land, rim, or previous span) so units cross at grade and a
        // chain of spans stays level, spanning the water or canyon below.
        int surfaceLevel = bridgeDeckLevel(firstBank, secondBank, targetHex);
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
     * @param exits exits bitmask with exactly two hexsides set
     *
     * @return The two connected hexside directions (0-5), in ascending order.
     */
    private static int[] exitDirections(int exits) {
        int firstDirection = Integer.numberOfTrailingZeros(exits);
        int secondDirection = Integer.numberOfTrailingZeros(exits & ~(1 << firstDirection));
        return new int[] { firstDirection, secondDirection };
    }

    /**
     * @param firstBank  one connected bank
     * @param secondBank the other connected bank
     * @param targetHex  the bridge site hex
     *
     * @return The deck level: the lowest surface level among the banks that can anchor the bridge (land/rim, or a
     *       previous span). Two anchoring rims are within one level (see isValidBridgeSite), so the lower-rim deck is
     *       reachable from both; with one anchor and a deeper continue side, the deck meets the rim/previous span at
     *       grade rather than dipping to the water surface or canyon floor.
     */
    private static int bridgeDeckLevel(Hex firstBank, Hex secondBank, Hex targetHex) {
        int deckLevel = Integer.MAX_VALUE;
        if (anchorsBridge(firstBank, targetHex)) {
            deckLevel = Math.min(deckLevel, bankSurfaceLevel(firstBank));
        }
        if (anchorsBridge(secondBank, targetHex)) {
            deckLevel = Math.min(deckLevel, bankSurfaceLevel(secondBank));
        }
        // Defensive fallback (a valid site always has an anchoring bank): sit at the lower bank
        return (deckLevel == Integer.MAX_VALUE) ? Math.min(bankSurfaceLevel(firstBank), bankSurfaceLevel(secondBank))
              : deckLevel;
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
