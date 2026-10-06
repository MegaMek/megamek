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
package megamek.common;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.board.Coords;
import megamek.common.units.BipedMek;
import megamek.common.units.LargeSupportTank;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Water gives a Mek partial cover when it is exactly as deep as the Mek is tall and the Mek stands on the bottom, so
 * that its top is level with the surface: Depth 1 for a standard Mek, Depth 2 for a superheavy (TW p.102; Core p.66,
 * p.240).
 * <p>
 * The server, the swarm secondary attack path and Princess each used to carry their own copy of this rule and the
 * copies had drifted. These tests pin the shared predicate they now all call, and in particular the two cases
 * Princess used to get wrong: a superheavy in Depth 2 water, and a Mek held above the surface.
 */
@DisplayName("Partial cover from water")
class PartialWaterCoverTest extends GameBoardTestCase {

    /** A Mek over 100 tons is superheavy, so it stands three levels high and needs Depth 2 to be covered. */
    private static final double SUPERHEAVY_TONS = 135.0;
    private static final double STANDARD_TONS = 50.0;

    /** The top of a Mek standing on the bottom sits level with the surface, so its relative height is 0. */
    private static final int TOP_AT_SURFACE = 0;

    static {
        initializeBoard("BOARD_DEPTH_ONE_WATER", """
              size 1 2
              hex 0101 0 "water:1" ""
              hex 0102 0 "" ""
              end""");
        initializeBoard("BOARD_DEPTH_TWO_WATER", """
              size 1 2
              hex 0101 0 "water:2" ""
              hex 0102 0 "" ""
              end""");
        initializeBoard("BOARD_DEPTH_THREE_WATER", """
              size 1 2
              hex 0101 0 "water:3" ""
              hex 0102 0 "" ""
              end""");
        initializeBoard("BOARD_DRY_GROUND", """
              size 1 2
              hex 0101 0 "" ""
              hex 0102 0 "" ""
              end""");
    }

    private Hex hexOf(String boardName) {
        return getBoard(boardName).getHex(new Coords(0, 0));
    }

    private BipedMek mekOf(double tons) {
        BipedMek mek = new BipedMek();
        mek.setId(1);
        mek.setWeight(tons);
        return mek;
    }

    @Test
    @DisplayName("A standard Mek standing in Depth 1 water is covered")
    void standardMekInDepthOneWaterIsCovered() {
        assertTrue(PartialCover.isInPartialWater(mekOf(STANDARD_TONS),
                    hexOf("BOARD_DEPTH_ONE_WATER"),
                    TOP_AT_SURFACE),
              "Depth 1 is waist-deep on a standard Mek (TW p.102; Core p.66)");
    }

    @Test
    @DisplayName("A superheavy Mek needs Depth 2, not Depth 1")
    void superheavyMekNeedsDepthTwo() {
        assertTrue(PartialCover.isInPartialWater(mekOf(SUPERHEAVY_TONS),
                    hexOf("BOARD_DEPTH_TWO_WATER"),
                    TOP_AT_SURFACE),
              "a superheavy stands three levels high, so Depth 2 reaches its waist (Core p.240)");
        assertFalse(PartialCover.isInPartialWater(mekOf(SUPERHEAVY_TONS),
                    hexOf("BOARD_DEPTH_ONE_WATER"),
                    TOP_AT_SURFACE),
              "Depth 1 is below a superheavy's waist, so it covers nothing (Core p.240)");
    }

    @Test
    @DisplayName("Depth 2 does not cover a standard Mek")
    void depthTwoDoesNotCoverAStandardMek() {
        assertFalse(PartialCover.isInPartialWater(mekOf(STANDARD_TONS),
                    hexOf("BOARD_DEPTH_TWO_WATER"),
                    TOP_AT_SURFACE),
              "Depth 2 submerges a standard Mek and blocks line of sight rather than covering it (TW p.102)");
    }

    @Test
    @DisplayName("Depth 3 covers nobody")
    void depthThreeCoversNobody() {
        assertFalse(PartialCover.isInPartialWater(mekOf(STANDARD_TONS),
                    hexOf("BOARD_DEPTH_THREE_WATER"),
                    TOP_AT_SURFACE),
              "a standard Mek is fully submerged in Depth 3");
        assertFalse(PartialCover.isInPartialWater(mekOf(SUPERHEAVY_TONS),
                    hexOf("BOARD_DEPTH_THREE_WATER"),
                    TOP_AT_SURFACE),
              "a superheavy is fully submerged in Depth 3 (Core p.240)");
    }

    @Test
    @DisplayName("A Mek held above the surface is in the open")
    void aMekAboveTheSurfaceIsNotCovered() {
        assertFalse(PartialCover.isInPartialWater(mekOf(STANDARD_TONS),
                    hexOf("BOARD_DEPTH_ONE_WATER"),
                    TOP_AT_SURFACE + 1),
              "a Mek on a bridge over the water stands clear of it and gets no cover");
    }

    @Test
    @DisplayName("A prone Mek is not covered")
    void aProneMekIsNotCovered() {
        BipedMek prone = mekOf(STANDARD_TONS);
        prone.setProne(true);

        assertFalse(PartialCover.isInPartialWater(prone, hexOf("BOARD_DEPTH_ONE_WATER"), TOP_AT_SURFACE),
              "prone Meks cannot receive partial cover (Core p.66)");
    }

    @Test
    @DisplayName("Only a Mek is covered by water")
    void onlyMeksAreCoveredByWater() {
        LargeSupportTank tank = new LargeSupportTank();
        tank.setId(1);
        tank.setWeight(STANDARD_TONS);

        assertFalse(PartialCover.isInPartialWater(tank, hexOf("BOARD_DEPTH_ONE_WATER"), TOP_AT_SURFACE),
              "large support vehicles never receive partial cover, however tall (TW p.102; Core p.66)");
        assertFalse(PartialCover.isInPartialWater(null, hexOf("BOARD_DEPTH_ONE_WATER"), TOP_AT_SURFACE),
              "no target, no cover");
    }

    @Test
    @DisplayName("Dry ground and a missing hex give no cover")
    void dryGroundAndMissingHexGiveNoCover() {
        assertFalse(PartialCover.isInPartialWater(mekOf(STANDARD_TONS), hexOf("BOARD_DRY_GROUND"), TOP_AT_SURFACE),
              "there is no water to hide in");
        assertFalse(PartialCover.isInPartialWater(mekOf(STANDARD_TONS), null, TOP_AT_SURFACE),
              "no hex, no cover");
    }
}
