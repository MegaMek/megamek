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
package megamek.common.moves;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.GameBoardTestCase;
import megamek.common.enums.MoveStepType;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.LandAirMek;
import megamek.common.units.SupportTank;
import megamek.common.units.VTOL;
import megamek.testUtilities.MMTestUtilities;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Issue #9091: an airborne WiGE (and a LAM in AirMek mode, which moves as a WiGE) could enter a hex any number of
 * levels higher than its current hex for no extra MP. TW p.55: a WiGE may enter a hex one level higher than its current
 * hex, but never one two or more levels higher. A WiGE holding its altitude over lower terrain measures from that
 * altitude (TW p.56 example). Units start at 0101 facing south, so each FORWARDS step enters the next hex down the
 * board.
 */
class WiGEElevationChangeTest extends GameBoardTestCase {

    static {
        initializeBoard("CLIFF_LEVEL_6", """
              size 1 3
              hex 0101 0 "" ""
              hex 0102 6 "" ""
              hex 0103 6 "" ""
              end""");

        initializeBoard("RISE_ONE_THEN_ONE", """
              size 1 3
              hex 0101 0 "" ""
              hex 0102 1 "" ""
              hex 0103 2 "" ""
              end""");

        initializeBoard("RISE_TWO", """
              size 1 2
              hex 0101 0 "" ""
              hex 0102 2 "" ""
              end""");

        initializeBoard("BUILDING_ONE_LEVEL", """
              size 1 2
              hex 0101 0 "" ""
              hex 0102 0 "bldg_elev:1;building:2:0;bldg_cf:100" ""
              end""");

        initializeBoard("BUILDING_TWO_LEVELS", """
              size 1 2
              hex 0101 0 "" ""
              hex 0102 0 "bldg_elev:2;building:2:0;bldg_cf:100" ""
              end""");

        initializeBoard("VALLEY_THEN_LEVEL_5", """
              size 1 3
              hex 0101 4 "" ""
              hex 0102 2 "" ""
              hex 0103 5 "" ""
              end""");

        initializeBoard("VALLEY_THEN_LEVEL_6", """
              size 1 3
              hex 0101 4 "" ""
              hex 0102 2 "" ""
              hex 0103 6 "" ""
              end""");

        initializeBoard("DROP_FROM_LEVEL_6", """
              size 1 2
              hex 0101 6 "" ""
              hex 0102 0 "" ""
              end""");
    }

    @Nested
    class WiGEVehicle {

        @Test
        void cannotFlyUpACliff() {
            setBoard("CLIFF_LEVEL_6");
            MovePath path = getMovePathFor(new SupportTank(), 1, EntityMovementMode.WIGE, MoveStepType.FORWARDS);

            assertFalse(path.isMoveLegal(), "an airborne WiGE must not enter a hex 6 levels higher");
        }

        @Test
        void cannotFlyUpACliffWithKeepElevationOn() {
            setBoard("CLIFF_LEVEL_6");
            MovePath path = getMovePathFor(new SupportTank(), 1, EntityMovementMode.WIGE,
                  MoveStepType.CLIMB_MODE_ON, MoveStepType.FORWARDS);

            assertFalse(path.isMoveLegal(), "Keep Elevation must not let an airborne WiGE climb a cliff");
        }

        @Test
        void cannotFlyUpACliffWithKeepElevationOff() {
            setBoard("CLIFF_LEVEL_6");
            MovePath path = getMovePathFor(new SupportTank(), 1, EntityMovementMode.WIGE,
                  MoveStepType.CLIMB_MODE_OFF, MoveStepType.FORWARDS);

            assertFalse(path.isMoveLegal(), "End Keep Elevation must not let an airborne WiGE climb a cliff");
        }

        @Test
        void canRiseOneLevelPerHex() {
            setBoard("RISE_ONE_THEN_ONE");
            MovePath path = getMovePathFor(new SupportTank(), 1, EntityMovementMode.WIGE,
                  MoveStepType.FORWARDS, MoveStepType.FORWARDS);

            assertTrue(path.isMoveLegal(), "a WiGE may enter a hex one level higher, twice in a row");
            assertMovePathElevations(path, 1, 1);
            assertEquals(2, path.getMpUsed(), "a one level rise costs no extra MP");
        }

        @Test
        void cannotRiseTwoLevels() {
            setBoard("RISE_TWO");
            MovePath path = getMovePathFor(new SupportTank(), 1, EntityMovementMode.WIGE, MoveStepType.FORWARDS);

            assertFalse(path.isMoveLegal(), "an airborne WiGE must not enter a hex two levels higher");
        }

        @Test
        void canFlyOntoOneLevelBuilding() {
            setBoard("BUILDING_ONE_LEVEL");
            MovePath path = getMovePathFor(new SupportTank(), 1, EntityMovementMode.WIGE, MoveStepType.FORWARDS);

            assertTrue(path.isMoveLegal(), "a WiGE may fly over a one level building");
            assertMovePathElevations(path, 2);
        }

        @Test
        void cannotFlyOntoTwoLevelBuilding() {
            setBoard("BUILDING_TWO_LEVELS");
            MovePath path = getMovePathFor(new SupportTank(), 1, EntityMovementMode.WIGE, MoveStepType.FORWARDS);

            assertFalse(path.isMoveLegal(), "a building roof two levels up counts as a two level rise");
        }

        @Test
        void holdingAltitudeCanEnterHexOneLevelAboveTheValley() {
            setBoard("VALLEY_THEN_LEVEL_5");
            MovePath path = getMovePathFor(new SupportTank(), 1, EntityMovementMode.WIGE,
                  MoveStepType.CLIMB_MODE_ON, MoveStepType.FORWARDS, MoveStepType.FORWARDS);

            assertTrue(path.isMoveLegal(), "a WiGE holding altitude 5 may enter a level 5 hex");
            assertMovePathElevations(path, 1, 3, 1);
        }

        @Test
        void holdingAltitudeCannotEnterHexTwoLevelsAboveThatAltitude() {
            setBoard("VALLEY_THEN_LEVEL_6");
            MovePath path = getMovePathFor(new SupportTank(), 1, EntityMovementMode.WIGE,
                  MoveStepType.CLIMB_MODE_ON, MoveStepType.FORWARDS, MoveStepType.FORWARDS);

            assertFalse(path.isMoveLegal(), "a WiGE holding altitude 5 must not enter a level 6 hex");
        }

        @Test
        void canDescendAnyNumberOfLevels() {
            setBoard("DROP_FROM_LEVEL_6");
            MovePath path = getMovePathFor(new SupportTank(), 1, EntityMovementMode.WIGE,
                  MoveStepType.CLIMB_MODE_OFF, MoveStepType.FORWARDS);

            assertTrue(path.isMoveLegal(), "a WiGE may fly down any number of levels");
            assertMovePathElevations(path, 1, 1);
        }

        @Test
        void takeOffThenRiseOneLevel() {
            setBoard("RISE_ONE_THEN_ONE");
            MovePath path = getMovePathFor(new SupportTank(), 0, EntityMovementMode.WIGE,
                  MoveStepType.UP, MoveStepType.FORWARDS);

            assertTrue(path.isMoveLegal(), "a WiGE that just took off may enter a hex one level higher");
        }

        @Test
        void takeOffThenCannotRiseTwoLevels() {
            setBoard("RISE_TWO");
            MovePath path = getMovePathFor(new SupportTank(), 0, EntityMovementMode.WIGE,
                  MoveStepType.UP, MoveStepType.FORWARDS);

            assertFalse(path.isMoveLegal(), "a WiGE that just took off must not enter a hex two levels higher");
        }
    }

    @Nested
    class AirMek {

        private LandAirMek airMek() {
            LandAirMek lam = (LandAirMek) MMTestUtilities.getEntityForUnitTesting("Shadow Hawk LAM SHD-X2", false);
            assertNotNull(lam, "the Shadow Hawk LAM test unit should load");
            // The SHD-X2 is bimodal (no AirMek mode); make it a standard LAM so AirMek mode is legal
            lam.setLAMType(LandAirMek.LAM_STANDARD);
            lam.setConversionMode(LandAirMek.CONV_MODE_AIR_MEK);
            return lam;
        }

        @Test
        void cannotFlyUpACliff() {
            setBoard("CLIFF_LEVEL_6");
            MovePath path = getMovePathFor(airMek(), 1, null, MoveStepType.FORWARDS);

            assertFalse(path.isMoveLegal(), "an AirMek at elevation 1 must not enter a hex 6 levels higher");
        }

        @Test
        void cannotFlyUpACliffWithKeepElevationOff() {
            setBoard("CLIFF_LEVEL_6");
            MovePath path = getMovePathFor(airMek(), 1, null, MoveStepType.CLIMB_MODE_OFF, MoveStepType.FORWARDS);

            assertFalse(path.isMoveLegal(), "End Keep Elevation must not let an AirMek climb a cliff");
        }

        @Test
        void canEnterTheCliffAfterClimbingToAltitudeSix() {
            setBoard("CLIFF_LEVEL_6");
            MovePath path = getMovePathFor(airMek(), 1, null,
                  MoveStepType.UP, MoveStepType.UP, MoveStepType.UP, MoveStepType.UP, MoveStepType.UP,
                  MoveStepType.FORWARDS);

            assertTrue(path.isMoveLegal(), "an AirMek at altitude 6 may enter a level 6 hex");
            assertEquals(1, path.getFinalElevation(), "the AirMek keeps one elevation above the cliff top");
        }

        /** Altitude 5 is one clearance above level 4, so a level 6 hex is still two levels up. */
        @Test
        void cannotEnterTheCliffFromAltitudeFive() {
            setBoard("CLIFF_LEVEL_6");
            MovePath path = getMovePathFor(airMek(), 1, null,
                  MoveStepType.UP, MoveStepType.UP, MoveStepType.UP, MoveStepType.UP, MoveStepType.FORWARDS);

            assertFalse(path.isMoveLegal(), "an AirMek at altitude 5 must not enter a level 6 hex");
        }
    }

    /** VTOL movement is not WiGE movement; a VTOL above the cliff top still flies over it. */
    @Test
    void vtolAboveTheCliffIsUnaffected() {
        setBoard("CLIFF_LEVEL_6");
        MovePath path = getMovePathFor(new VTOL(), 7, EntityMovementMode.VTOL, MoveStepType.FORWARDS);

        assertTrue(path.isMoveLegal(), "a VTOL at elevation 7 may fly over a level 6 hex");
    }
}
