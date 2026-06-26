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
    /**
     * @param bank      a hex adjacent to a bridge site (e.g. the bridgelayer's own hex)
     * @param targetHex the hex a bridge would be placed in
     *
     * @return whether the bank can anchor a bridge in the target hex: over water it must be land/shallow (or already
     *       hold a bridge); over a dry gap it must be a rim higher than the target floor (or hold a bridge). Exposes
     *       the internal anchor test for callers that must require a <em>specific</em> bank to anchor - e.g. the AVLB
     *       bridgelayer's own hex, which must be a rim/land rather than the canyon floor. TM p.242 / TW.
     */
    public static boolean isAnchoringBank(Hex bank, Hex targetHex) {
        return anchorsBridge(bank, targetHex);
    }

    private static boolean anchorsBridge(Hex bank, Hex targetHex) {
        if (bank.containsTerrain(Terrains.BRIDGE)) {
            return true;
        }
        return isOverWater(targetHex) ? !isOverWater(bank) : (bankSurfaceLevel(bank) > targetHex.getLevel());
    }

    /**
     * @param hex the hex to check
     *
     * @return {@code true} if the hex is a water hex for bridge purposes. A water hex (Total Warfare p.32) is one
     *       covered by a stream, river, swamp, pond or lake, so this is any hex holding water of any depth
     *       (streams/rivers/ponds/lakes), a swamp, or rapids. A bridge may be placed in any water hex (when adjacent to
     *       a land hex or another bridge), and a bridge over water gains double CF from its flotation devices. Shared by
     *       Bridge-Building Engineers (TO:AUE p.152) and the Bridge-Layer / AVLB (TM p.242 / TW) so both use the same
     *       water-hex definition.
     */
    public static boolean isOverWater(Hex hex) {
        return hex.containsAnyTerrainOf(Terrains.WATER, Terrains.SWAMP, Terrains.RAPIDS);
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

        // The deck sits at the lowest anchoring bank (land, rim, or previous span) so units cross at grade and a
        // chain of spans steps down the lower rim, spanning the water or canyon below. See bridgeDeckLevel().
        int surfaceLevel = bridgeDeckLevel(firstBank, secondBank, targetHex);
        IBuilding bridge = addBridgeStructure(board, target, targetHex, exits, bridgeType, cf, surfaceLevel);
        LOGGER.info("[BuildBridge] placed a type-{} bridge at {}: CF {}, elevation {}, exits bitmask {}, "
                    + "registered as structure", bridgeType, target, cf, Math.max(0, surfaceLevel - targetHex.getLevel()),
              exits & 63);
        return bridge;
    }

    /**
     * The reason a hex is or is not a valid bridge-<i>repair</i> site, so callers can explain a rejection to the
     * player. Repairing a destroyed bridge section is an unofficial option (not part of any published ruleset): a
     * Bridge-Building Engineer platoon refills one missing hex of an existing bridge, working from an adjacent
     * surviving span or bank. See {@link #bridgeRepairIssue}.
     */
    public enum BridgeRepairIssue {
        /** The site is a valid repair gap. */
        VALID,
        /** The exits are not two distinct hexsides (cannot happen through the normal UI). */
        BAD_EXITS,
        /** The site hex or one of its connected neighbors is off the board. */
        OFF_BOARD,
        /** The hex already holds a bridge, building or fuel tank - there is no gap to repair. */
        NOT_A_GAP,
        /** Neither connected neighbor is a surviving span of a bridge pointing into the hex - nothing to reconnect. */
        NO_SURVIVING_SPAN,
        /**
         * With one surviving span, the far side does not continue the run: it is not the straight hexside opposite the
         * span, or it does not reach a bank a unit can step on/off of (open water or deep canyon).
         */
        FAR_SIDE_UNANCHORED
    }

    /**
     * @param board  the board to repair on
     * @param target the gap hex the section would be rebuilt in, or null
     * @param exits  exits bitmask of the two hexsides the repaired section would connect
     *
     * @return {@code true} if a destroyed bridge section may be rebuilt in the target hex (unofficial). See
     *       {@link #bridgeRepairIssue(Board, Coords, int)} for the full set of conditions.
     */
    public static boolean isBridgeRepairSite(Board board, @Nullable Coords target, int exits) {
        return bridgeRepairIssue(board, target, exits) == BridgeRepairIssue.VALID;
    }

    /**
     * Checks whether a single destroyed bridge section may be rebuilt in the target hex (unofficial bridge-repair
     * option). A repair fills a gap - a hex that currently holds no structure - and must reconnect the broken run: at
     * least one connected neighbor is a surviving span of a bridge pointing back into the hex (that span also fixes the
     * repaired deck height, so the section lines up with the rest of the bridge), and the far side either continues the
     * bridge with another surviving span or reaches a bank a unit can step on/off of. A gap flanked only by banks is a
     * fresh build, not a repair, and a span whose far side is open water or deep canyon with no bank cannot be repaired
     * (the run is rebuilt inward from a span or bank). Any underlying rubble or water left by the collapse is
     * preserved.
     *
     * @param board  the board to repair on
     * @param target the gap hex the section would be rebuilt in, or null
     * @param exits  exits bitmask of the two hexsides the repaired section would connect
     *
     * @return Why the site is or is not a repairable gap (see {@link BridgeRepairIssue}).
     */
    public static BridgeRepairIssue bridgeRepairIssue(Board board, @Nullable Coords target, int exits) {
        if (!isValidBridgeExits(exits)) {
            return BridgeRepairIssue.BAD_EXITS;
        }
        if ((target == null) || !board.contains(target)) {
            return BridgeRepairIssue.OFF_BOARD;
        }
        Hex targetHex = board.getHex(target);
        if (targetHex == null) {
            return BridgeRepairIssue.OFF_BOARD;
        }
        // A repair fills a gap left by a destroyed section: the hex must currently hold no structure of its own.
        if (targetHex.containsAnyTerrainOf(Terrains.BRIDGE, Terrains.BUILDING, Terrains.FUEL_TANK)) {
            return BridgeRepairIssue.NOT_A_GAP;
        }

        int[] exitDirections = exitDirections(exits);
        Coords firstBankCoords = target.translated(exitDirections[0]);
        Coords secondBankCoords = target.translated(exitDirections[1]);
        if (!board.contains(firstBankCoords) || !board.contains(secondBankCoords)) {
            return BridgeRepairIssue.OFF_BOARD;
        }
        Hex firstBank = board.getHex(firstBankCoords);
        Hex secondBank = board.getHex(secondBankCoords);

        boolean firstIsSpan = isSurvivingSpanToward(firstBank, exitDirections[0]);
        boolean secondIsSpan = isSurvivingSpanToward(secondBank, exitDirections[1]);
        // A repair must reconnect to at least one surviving span of the broken bridge; that span also fixes the
        // deck height. A hex flanked only by banks is a fresh build (use isValidBridgeSite), not a repair.
        if (!firstIsSpan && !secondIsSpan) {
            return BridgeRepairIssue.NO_SURVIVING_SPAN;
        }
        // Two surviving spans pointing into the gap fully fix the orientation (straight or, for a curved bridge,
        // bent) - the repaired section simply reconnects them.
        if (firstIsSpan && secondIsSpan) {
            return BridgeRepairIssue.VALID;
        }
        // Exactly one surviving span (an end-of-run gap): the far side must be the straight continuation of the run
        // (the hexside opposite the span) onto a bank a unit can step on/off of. Requiring the straight opposite
        // keeps the repaired section in the bridge's line rather than bending it to an arbitrary side bank; an
        // originally curved end span is the one case this cannot repair (logged by the caller).
        int spanSide = firstIsSpan ? exitDirections[0] : exitDirections[1];
        int farSide = firstIsSpan ? exitDirections[1] : exitDirections[0];
        Hex farBank = firstIsSpan ? secondBank : firstBank;
        if ((farSide != ((spanSide + 3) % 6)) || !anchorsBridge(farBank, targetHex)) {
            return BridgeRepairIssue.FAR_SIDE_UNANCHORED;
        }
        return BridgeRepairIssue.VALID;
    }

    /**
     * Rebuilds a single destroyed bridge section in the gap hex (unofficial repair option): adds the bridge terrain at
     * the surviving span's deck height so the section reconnects, registers it as a board structure, and recomputes
     * terrain exits. Underlying rubble or water left by the collapse is preserved (only bridge terrain is added). The
     * caller is responsible for validating the site with {@link #isBridgeRepairSite(Board, Coords, int)} and for
     * sending the changed hex and the new structure to the clients.
     *
     * @param board      the board to repair on
     * @param target     the gap hex the section is rebuilt in
     * @param exits      exits bitmask of the two hexsides the repaired section connects
     * @param bridgeType the bridge type as a {@link megamek.common.enums.BuildingType} value (1 = light, 2 = medium)
     * @param cf         the Construction Factor of the rebuilt section
     *
     * @return The newly registered bridge structure, for sending to the clients.
     */
    public static IBuilding placeRepairedBridge(Board board, Coords target, int exits, int bridgeType, int cf) {
        Hex targetHex = board.getHex(target);
        int[] exitDirections = exitDirections(exits);
        Hex firstBank = board.getHex(target.translated(exitDirections[0]));
        Hex secondBank = board.getHex(target.translated(exitDirections[1]));

        // Match the surviving span's deck (not the lowest bank) so the repaired section lines up with the rest of
        // the bridge and units cross the run at grade. See repairedDeckLevel().
        int surfaceLevel = repairedDeckLevel(firstBank, secondBank, exitDirections, targetHex);
        IBuilding bridge = addBridgeStructure(board, target, targetHex, exits, bridgeType, cf, surfaceLevel);
        // Mark the hex as a field repair so the board view can badge it (removed if the section is later destroyed).
        targetHex.addTerrain(new Terrain(Terrains.BRIDGE_REPAIRED, 1));
        LOGGER.info("[BridgeRepair] rebuilt a type-{} section at {}: CF {}, elevation {}, exits bitmask {}, matched "
                    + "surviving span deck {}", bridgeType, target, cf, Math.max(0, surfaceLevel - targetHex.getLevel()),
              exits & 63, surfaceLevel);
        return bridge;
    }

    /**
     * Adds bridge terrain (type, elevation, CF) to the target hex at the given absolute deck level, recomputes the
     * surrounding terrain exits, and registers the span as a board structure. Shared by {@link #placeBridge} and
     * {@link #placeRepairedBridge}; only bridge terrain is added, so any underlying rubble or water is preserved.
     *
     * @param board        the board to build on
     * @param target       the hex the bridge is placed in
     * @param targetHex    the hex at {@code target} (passed in to avoid a second lookup)
     * @param exits        exits bitmask of the two hexsides the bridge connects
     * @param bridgeType   the bridge type (1 = light, 2 = medium)
     * @param cf           the Construction Factor of the new bridge
     * @param surfaceLevel the absolute level the deck sits at
     *
     * @return The newly registered bridge structure.
     */
    private static IBuilding addBridgeStructure(Board board, Coords target, Hex targetHex, int exits, int bridgeType,
          int cf, int surfaceLevel) {
        int bridgeElevation = Math.max(0, surfaceLevel - targetHex.getLevel());
        targetHex.addTerrain(new Terrain(Terrains.BRIDGE, bridgeType, true, exits & 63));
        targetHex.addTerrain(new Terrain(Terrains.BRIDGE_ELEV, bridgeElevation));
        targetHex.addTerrain(new Terrain(Terrains.BRIDGE_CF, cf));
        board.initializeAround(target.getX(), target.getY());

        IBuilding bridge = new BuildingTerrain(target, board, Terrains.BRIDGE, BasementType.NONE);
        board.addBuildingToBoard(bridge);
        return bridge;
    }

    /**
     * @param neighbor  a hex adjacent to a repair gap, reached by stepping {@code direction}
     * @param direction the hexside direction (0-5) from the gap to this neighbor
     *
     * @return {@code true} if the neighbor holds a bridge whose exits point back into the gap (i.e. it is a surviving
     *       span of the broken bridge that the repaired section should reconnect to).
     */
    private static boolean isSurvivingSpanToward(Hex neighbor, int direction) {
        if (!neighbor.containsTerrain(Terrains.BRIDGE)) {
            return false;
        }
        int backDirection = (direction + 3) % 6;
        return (neighbor.getTerrain(Terrains.BRIDGE).getExits() & (1 << backDirection)) != 0;
    }

    /**
     * @param firstBank      one connected neighbor
     * @param secondBank     the other connected neighbor
     * @param exitDirections the two hexside directions (0-5) from the gap to the neighbors
     * @param targetHex      the gap hex
     *
     * @return The deck level for a repaired section: the surviving span's deck, so the section lines up with the rest
     *       of the bridge. If (defensively) both sides are spans at different decks, the lower is used; if neither side
     *       is a span the gap level is returned (a non-repair site never reaches here).
     */
    private static int repairedDeckLevel(Hex firstBank, Hex secondBank, int[] exitDirections, Hex targetHex) {
        int deckLevel = Integer.MAX_VALUE;
        if (isSurvivingSpanToward(firstBank, exitDirections[0])) {
            deckLevel = Math.min(deckLevel, bankSurfaceLevel(firstBank));
        }
        if (isSurvivingSpanToward(secondBank, exitDirections[1])) {
            deckLevel = Math.min(deckLevel, bankSurfaceLevel(secondBank));
        }
        return (deckLevel == Integer.MAX_VALUE) ? targetHex.getLevel() : deckLevel;
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
