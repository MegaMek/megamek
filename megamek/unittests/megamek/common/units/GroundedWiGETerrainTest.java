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

package megamek.common.units;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.GameBoardTestCase;
import megamek.common.board.Coords;
import megamek.common.enums.MoveStepType;
import megamek.common.moves.MovePath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * A grounded WiGE is a hover vehicle for terrain restrictions (TW p.55), and combat and support WiGEs follow the same
 * rule. Airborne WiGEs keep their own rules: they fly one elevation above the ground and ignore rough and rubble.
 */
class GroundedWiGETerrainTest extends GameBoardTestCase {

    private static final Coords START = new Coords(0, 0);
    private static final Coords AHEAD = new Coords(0, 1);
    private static final int BOARD_ID = 0;

    static {
        // A unit starts at 0101 facing south, so one FORWARDS step enters 0102.
        initializeBoard("ULTRA_ROUGH_AHEAD", """
              size 1 2
              hex 0101 0 "" ""
              hex 0102 0 "rough:2" ""
              end""");
        initializeBoard("ROUGH_AHEAD", """
              size 1 2
              hex 0101 0 "" ""
              hex 0102 0 "rough:1" ""
              end""");
        initializeBoard("LIQUID_MAGMA_AHEAD", """
              size 1 2
              hex 0101 0 "" ""
              hex 0102 0 "magma:2" ""
              end""");
        initializeBoard("ULTRA_RUBBLE_AHEAD", """
              size 1 2
              hex 0101 0 "" ""
              hex 0102 0 "rubble:6" ""
              end""");
        initializeBoard("JUNGLE_AHEAD", """
              size 1 2
              hex 0101 0 "" ""
              hex 0102 0 "jungle:1;foliage_elev:2" ""
              end""");
        initializeBoard("INDUSTRIAL_AHEAD", """
              size 1 2
              hex 0101 0 "" ""
              hex 0102 0 "heavy_industrial:1" ""
              end""");
        initializeBoard("WOODS_ROAD_AHEAD", """
              size 1 2
              hex 0101 0 "road:1" ""
              hex 0102 0 "woods:1;foliage_elev:2;road:1" ""
              end""");
        initializeBoard("WOODS_START", """
              size 1 2
              hex 0101 0 "woods:2;foliage_elev:2" ""
              hex 0102 0 "" ""
              end""");
        initializeBoard("CLEAR", """
              size 1 2
              hex 0101 0 "" ""
              hex 0102 0 "" ""
              end""");
        initializeBoard("CLEAR_SECOND_BOARD", """
              size 1 2
              hex 0101 0 "" ""
              hex 0102 0 "" ""
              end""");
    }

    /**
     * Places the WiGE on the board at the given elevation and asks whether it may be in the hex ahead at that
     * elevation. Elevation 0 is grounded; elevation 1 is airborne, flying one elevation above the ground.
     */
    private boolean prohibitedAhead(Tank wigeVehicle, int elevation) {
        getMovePathFor(wigeVehicle, elevation, EntityMovementMode.WIGE);
        return wigeVehicle.isLocationProhibited(AHEAD, BOARD_ID, elevation);
    }

    @Nested
    class HoverRestrictionsWhileGrounded {

        @Test
        @DisplayName("A grounded combat WiGE cannot move into ultra-rough terrain")
        void groundedCombatWiGECannotEnterUltraRough() {
            setBoard("ULTRA_ROUGH_AHEAD");
            MovePath path = getMovePathFor(new Tank(), 0, EntityMovementMode.WIGE, MoveStepType.FORWARDS);

            assertFalse(path.isMoveLegal(), "a hovercraft cannot enter ultra-rough, so neither can a grounded WiGE");
        }

        @Test
        @DisplayName("A grounded support WiGE cannot move into ultra-rough terrain")
        void groundedSupportWiGECannotEnterUltraRough() {
            setBoard("ULTRA_ROUGH_AHEAD");
            MovePath path = getMovePathFor(new SupportTank(), 0, EntityMovementMode.WIGE, MoveStepType.FORWARDS);

            assertFalse(path.isMoveLegal(), "a support WiGE follows the same grounded rule as a combat WiGE");
        }

        @Test
        @DisplayName("A grounded WiGE may still move into ordinary rough terrain, as a hovercraft may")
        void groundedWiGECanEnterOrdinaryRough() {
            setBoard("ROUGH_AHEAD");
            MovePath path = getMovePathFor(new Tank(), 0, EntityMovementMode.WIGE, MoveStepType.FORWARDS);

            assertTrue(path.isMoveLegal(), "hovercraft may enter rough, so a grounded WiGE may too");
        }

        @Test
        @DisplayName("Grounded combat and support WiGEs cannot be in liquid magma")
        void groundedWiGEsBarredFromLiquidMagma() {
            setBoard("LIQUID_MAGMA_AHEAD");

            assertTrue(prohibitedAhead(new Tank(), 0), "combat WiGE, grounded in liquid magma");
            assertTrue(prohibitedAhead(new SupportTank(), 0), "support WiGE, grounded in liquid magma");
        }

        @Test
        @DisplayName("Grounded combat and support WiGEs cannot be in ultra-rubble")
        void groundedWiGEsBarredFromUltraRubble() {
            setBoard("ULTRA_RUBBLE_AHEAD");

            assertTrue(prohibitedAhead(new Tank(), 0), "combat WiGE, grounded in ultra-rubble");
            assertTrue(prohibitedAhead(new SupportTank(), 0), "support WiGE, grounded in ultra-rubble");
        }
    }

    @Nested
    class CombatAndSupportAgree {

        @Test
        @DisplayName("A grounded support WiGE cannot be in jungle, the same as a combat WiGE")
        void groundedSupportWiGEBarredFromJungle() {
            setBoard("JUNGLE_AHEAD");

            assertTrue(prohibitedAhead(new Tank(), 0), "combat WiGE, grounded in jungle");
            assertTrue(prohibitedAhead(new SupportTank(), 0), "support WiGE, grounded in jungle");
        }

        @Test
        @DisplayName("A grounded support WiGE cannot be in heavy industrial terrain, the same as a combat WiGE")
        void groundedSupportWiGEBarredFromIndustrial() {
            setBoard("INDUSTRIAL_AHEAD");

            assertTrue(prohibitedAhead(new Tank(), 0), "combat WiGE, grounded in heavy industrial terrain");
            assertTrue(prohibitedAhead(new SupportTank(), 0), "support WiGE, grounded in heavy industrial terrain");
        }

        @Test
        @DisplayName("A support WiGE, like a combat WiGE, may follow a road through woods (TW p.55)")
        void supportWiGEMayFollowARoadThroughWoods() {
            setBoard("WOODS_ROAD_AHEAD");

            assertFalse(prohibitedAhead(new Tank(), 1), "combat WiGE on a road through woods");
            assertFalse(prohibitedAhead(new SupportTank(), 1), "support WiGE on a road through woods");
        }

        @Test
        @DisplayName("A support WiGE cannot go below ground level, the same as a combat WiGE")
        void supportWiGEBarredBelowGround() {
            setBoard("CLEAR");

            assertTrue(prohibitedAhead(new Tank(), -1), "combat WiGE below ground");
            assertTrue(prohibitedAhead(new SupportTank(), -1), "support WiGE below ground");
        }
    }

    @Nested
    class AirborneUnchanged {

        @Test
        @DisplayName("An airborne WiGE still flies over ultra-rough, liquid magma and ultra-rubble")
        void airborneWiGEFliesOverHoverOnlyTerrain() {
            setBoard("ULTRA_ROUGH_AHEAD");
            assertFalse(prohibitedAhead(new Tank(), 1), "airborne combat WiGE over ultra-rough");
            setBoard("LIQUID_MAGMA_AHEAD");
            assertFalse(prohibitedAhead(new Tank(), 1), "airborne combat WiGE over liquid magma");
            setBoard("ULTRA_RUBBLE_AHEAD");
            assertFalse(prohibitedAhead(new SupportTank(), 1), "airborne support WiGE over ultra-rubble");
        }

        @Test
        @DisplayName("An airborne WiGE may fly forward over ultra-rough terrain")
        void airborneWiGEMovesOverUltraRough() {
            setBoard("ULTRA_ROUGH_AHEAD");
            MovePath path = getMovePathFor(new Tank(), 1, EntityMovementMode.WIGE, MoveStepType.FORWARDS);

            assertTrue(path.isMoveLegal(), "an airborne WiGE is unaffected by rough terrain (TW p.55)");
        }
    }

    @Test
    @DisplayName("The WiGE terrain check reads the hex from the board being tested, not the first board")
    void wigeCheckUsesTheTestedBoard() {
        // Board 0 has tall woods at 0101; board 1 is clear there. A WiGE flying one elevation over board 1 must not
        // be stopped by the woods on board 0.
        setBoard("WOODS_START");
        getGame().setBoard(1, getBoard("CLEAR_SECOND_BOARD"));
        Tank combatVehicle = new Tank();
        getMovePathFor(combatVehicle, 1, EntityMovementMode.WIGE);

        assertTrue(combatVehicle.isLocationProhibited(START, BOARD_ID, 1),
              "on board 0 the WiGE is below the treetops");
        assertFalse(combatVehicle.isLocationProhibited(START, 1, 1),
              "on board 1 the hex is clear, so the WiGE may fly there");
    }
}
