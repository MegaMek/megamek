/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.GameBoardTestCase;
import megamek.common.MPCalculationSetting;
import megamek.common.board.Coords;
import megamek.common.enums.MoveStepType;
import megamek.common.options.OptionsConstants;
import megamek.common.options.PilotOptions;
import megamek.common.planetaryConditions.Atmosphere;
import megamek.common.planetaryConditions.PlanetaryConditions;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.Infantry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * In-game movement tests for Prosthetic Wings, Powered Flight (IO p.85). Tests actual movement paths and validates VTOL
 * movement behavior.
 */
public class InfantryPoweredFlightMovementTest extends GameBoardTestCase {

    static {
        // Simple 3x3 clear terrain board
        initializeBoard("BOARD_3x3_CLEAR", """
              size 3 3
              hex 0101 0 "" ""
              hex 0201 0 "" ""
              hex 0301 0 "" ""
              hex 0102 0 "" ""
              hex 0202 0 "" ""
              hex 0302 0 "" ""
              hex 0103 0 "" ""
              hex 0203 0 "" ""
              hex 0303 0 "" ""
              end""");

        // Board with elevation changes
        initializeBoard("BOARD_3x1_ELEVATION", """
              size 3 1
              hex 0101 0 "" ""
              hex 0201 1 "" ""
              hex 0301 2 "" ""
              end""");

        // Board with building
        initializeBoard("BOARD_2x1_BUILDING", """
              size 2 1
              hex 0101 0 "" ""
              hex 0201 0 "bldg_elev:2;building:2:8;bldg_cf:50" ""
              end""");

        // Board with a cliff (level 2 drop - requires climbing for regular infantry)
        // 1 column x 2 rows: entity starts at (0,0) facing south, moves to (0,1)
        initializeBoard("BOARD_1x2_CLIFF", """
              size 1 2
              hex 0101 2 "" ""
              hex 0102 0 "" ""
              end""");

        // Board with a level 1 slope (not a cliff - any infantry can traverse)
        initializeBoard("BOARD_1x2_SLOPE", """
              size 1 2
              hex 0101 1 "" ""
              hex 0102 0 "" ""
              end""");
    }

    /**
     * Creates infantry with powered flight wings enabled.
     */
    private Infantry createPoweredFlightInfantry() {
        Infantry infantry = new Infantry();
        infantry.setId(1);
        infantry.setMovementMode(EntityMovementMode.INF_LEG);
        infantry.setSquadSize(7);
        infantry.setSquadCount(4);
        infantry.autoSetInternal();
        infantry.setOriginalWalkMP(1);
        infantry.setOriginalJumpMP(0);

        Crew crew = new Crew(CrewType.INFANTRY_CREW);
        crew.setGunnery(4, crew.getCrewType().getGunnerPos());
        crew.setPiloting(5, crew.getCrewType().getPilotPos());
        crew.setName("Powered Flight Test Crew", 0);

        PilotOptions options = new PilotOptions();
        options.getOption(OptionsConstants.MD_PL_FLIGHT).setValue(true);
        crew.setOptions(options);
        infantry.setCrew(crew);

        return infantry;
    }

    /**
     * Creates regular leg infantry without powered flight.
     */
    private Infantry createRegularInfantry() {
        Infantry infantry = new Infantry();
        infantry.setId(2);
        infantry.setMovementMode(EntityMovementMode.INF_LEG);
        infantry.setSquadSize(7);
        infantry.setSquadCount(4);
        infantry.autoSetInternal();
        infantry.setOriginalWalkMP(1);
        infantry.setOriginalJumpMP(0);

        Crew crew = new Crew(CrewType.INFANTRY_CREW);
        crew.setGunnery(4, crew.getCrewType().getGunnerPos());
        crew.setPiloting(5, crew.getCrewType().getPilotPos());
        crew.setName("Regular Test Crew", 0);
        infantry.setCrew(crew);

        return infantry;
    }

    /**
     * Creates infantry with glider wings enabled (fall damage protection only, cannot traverse cliffs).
     */
    private Infantry createGliderInfantry() {
        Infantry infantry = new Infantry();
        infantry.setId(3);
        infantry.setMovementMode(EntityMovementMode.INF_LEG);
        infantry.setSquadSize(7);
        infantry.setSquadCount(4);
        infantry.autoSetInternal();
        infantry.setOriginalWalkMP(1);
        infantry.setOriginalJumpMP(0);

        Crew crew = new Crew(CrewType.INFANTRY_CREW);
        crew.setGunnery(4, crew.getCrewType().getGunnerPos());
        crew.setPiloting(5, crew.getCrewType().getPilotPos());
        crew.setName("Glider Test Crew", 0);

        PilotOptions options = new PilotOptions();
        options.getOption(OptionsConstants.MD_PL_GLIDER).setValue(true);
        crew.setOptions(options);
        infantry.setCrew(crew);

        return infantry;
    }

    /**
     * Creates native VTOL infantry (microlite - can traverse cliffs).
     */
    private Infantry createNativeVTOLInfantry() {
        Infantry infantry = new Infantry();
        infantry.setId(4);
        infantry.setMovementMode(EntityMovementMode.VTOL);
        infantry.setSquadSize(7);
        infantry.setSquadCount(4);
        infantry.autoSetInternal();
        infantry.setOriginalWalkMP(1);
        infantry.setOriginalJumpMP(0);
        infantry.setMicrolite(true);

        Crew crew = new Crew(CrewType.INFANTRY_CREW);
        crew.setGunnery(4, crew.getCrewType().getGunnerPos());
        crew.setPiloting(5, crew.getCrewType().getPilotPos());
        crew.setName("VTOL Test Crew", 0);
        infantry.setCrew(crew);

        return infantry;
    }

    /**
     * Sets the atmosphere for the current game.
     */
    private void setAtmosphere(Atmosphere atmosphere) {
        PlanetaryConditions conditions = new PlanetaryConditions();
        conditions.setAtmosphere(atmosphere);
        getGame().setPlanetaryConditions(conditions);
    }

    @Nested
    @DisplayName("Powered Flight VTOL Capability Tests")
    class PoweredFlightVTOLCapabilityTests {

        @Test
        @DisplayName("Powered flight infantry has VTOL movement capability")
        void poweredFlightInfantryHasVTOLCapability() {
            setBoard("BOARD_3x3_CLEAR");
            setAtmosphere(Atmosphere.STANDARD);

            Infantry infantry = createPoweredFlightInfantry();
            getGame().addEntity(infantry);
            infantry.setGame(getGame());

            assertTrue(infantry.hasVTOLMovementCapability(),
                  "Infantry with powered flight should have VTOL movement capability");
            assertEquals(2, infantry.getVTOLMP(),
                  "Powered flight should provide 2 VTOL MP");
            assertEquals(2, infantry.getPoweredFlightMP(),
                  "getPoweredFlightMP() should return 2");
        }

        @Test
        @DisplayName("Regular infantry does NOT have VTOL movement capability")
        void regularInfantryNoVTOLCapability() {
            setBoard("BOARD_3x3_CLEAR");
            setAtmosphere(Atmosphere.STANDARD);

            Infantry infantry = createRegularInfantry();
            getGame().addEntity(infantry);
            infantry.setGame(getGame());

            assertFalse(infantry.hasVTOLMovementCapability(),
                  "Regular infantry should NOT have VTOL movement capability");
            assertEquals(0, infantry.getVTOLMP(),
                  "Regular infantry should have 0 VTOL MP");
        }

        @Test
        @DisplayName("Powered flight disabled in vacuum")
        void poweredFlightDisabledInVacuum() {
            setBoard("BOARD_3x3_CLEAR");
            setAtmosphere(Atmosphere.VACUUM);

            Infantry infantry = createPoweredFlightInfantry();
            getGame().addEntity(infantry);
            infantry.setGame(getGame());

            assertFalse(infantry.canUsePoweredFlightWings(),
                  "Powered flight should be disabled in vacuum");
            assertFalse(infantry.hasVTOLMovementCapability(),
                  "VTOL capability should be disabled in vacuum");
            assertEquals(0, infantry.getVTOLMP(),
                  "VTOL MP should be 0 in vacuum");
        }

        @Test
        @DisplayName("Powered flight disabled in trace atmosphere")
        void poweredFlightDisabledInTrace() {
            setBoard("BOARD_3x3_CLEAR");
            setAtmosphere(Atmosphere.TRACE);

            Infantry infantry = createPoweredFlightInfantry();
            getGame().addEntity(infantry);
            infantry.setGame(getGame());

            assertFalse(infantry.canUsePoweredFlightWings(),
                  "Powered flight should be disabled in trace atmosphere");
            assertEquals(0, infantry.getVTOLMP(),
                  "VTOL MP should be 0 in trace atmosphere");
        }

        @Test
        @DisplayName("Powered flight works in thin atmosphere")
        void poweredFlightWorksInThinAtmosphere() {
            setBoard("BOARD_3x3_CLEAR");
            setAtmosphere(Atmosphere.THIN);

            Infantry infantry = createPoweredFlightInfantry();
            getGame().addEntity(infantry);
            infantry.setGame(getGame());

            assertTrue(infantry.canUsePoweredFlightWings(),
                  "Powered flight should work in thin atmosphere");
            assertEquals(2, infantry.getVTOLMP(),
                  "VTOL MP should be 2 in thin atmosphere");
        }
    }

    @Nested
    @DisplayName("Powered Flight Movement Validation Tests")
    class PoweredFlightMovementValidationTests {

        @Test
        @DisplayName("Powered flight infantry canGoUp returns true")
        void poweredFlightInfantryCanGoUp() {
            setBoard("BOARD_3x3_CLEAR");
            setAtmosphere(Atmosphere.STANDARD);

            Infantry infantry = createPoweredFlightInfantry();
            getGame().addEntity(infantry);
            infantry.setGame(getGame());
            infantry.setPosition(new Coords(0, 0));

            // Verify canGoUp returns true for powered flight
            assertTrue(infantry.canGoUp(0, new Coords(0, 0), 0),
                  "Powered flight infantry canGoUp should return true");
        }

        @Test
        @DisplayName("Regular infantry canGoUp returns false on clear terrain")
        void regularInfantryCannotGoUp() {
            setBoard("BOARD_3x3_CLEAR");
            setAtmosphere(Atmosphere.STANDARD);

            Infantry infantry = createRegularInfantry();
            getGame().addEntity(infantry);
            infantry.setGame(getGame());
            infantry.setPosition(new Coords(0, 0));

            // Regular infantry cannot go up on clear terrain (no buildings)
            assertFalse(infantry.canGoUp(0, new Coords(0, 0), 0),
                  "Regular infantry canGoUp should return false on clear terrain");
        }

        @Test
        @DisplayName("Powered flight infantry has correct VTOL MP")
        void poweredFlightInfantryHasCorrectVTOLMP() {
            setBoard("BOARD_3x3_CLEAR");
            setAtmosphere(Atmosphere.STANDARD);

            Infantry infantry = createPoweredFlightInfantry();
            getGame().addEntity(infantry);
            infantry.setGame(getGame());

            assertEquals(2, infantry.getVTOLMP(),
                  "Powered flight should provide 2 VTOL MP");
            assertEquals(2, infantry.getPoweredFlightMP(),
                  "getPoweredFlightMP should return 2");
        }

        @Test
        @DisplayName("Powered flight disabled in vacuum prevents canGoUp")
        void poweredFlightInVacuumCannotGoUp() {
            setBoard("BOARD_3x3_CLEAR");
            setAtmosphere(Atmosphere.VACUUM);

            Infantry infantry = createPoweredFlightInfantry();
            getGame().addEntity(infantry);
            infantry.setGame(getGame());
            infantry.setPosition(new Coords(0, 0));

            // In vacuum, powered flight is disabled, so canGoUp falls back to normal infantry behavior
            assertFalse(infantry.canGoUp(0, new Coords(0, 0), 0),
                  "Powered flight infantry in vacuum should not be able to go up");
        }
    }

    @Nested
    @DisplayName("Glider Benefits Tests (IO p.85)")
    class GliderBenefitsTests {

        @Test
        @DisplayName("Powered flight infantry can assault drop")
        void poweredFlightCanAssaultDrop() {
            setBoard("BOARD_3x3_CLEAR");
            setAtmosphere(Atmosphere.STANDARD);

            Infantry infantry = createPoweredFlightInfantry();
            getGame().addEntity(infantry);
            infantry.setGame(getGame());

            assertTrue(infantry.canAssaultDrop(),
                  "Powered flight infantry should be able to assault drop");
        }

        @Test
        @DisplayName("Regular leg infantry cannot assault drop")
        void regularInfantryCannotAssaultDrop() {
            setBoard("BOARD_3x3_CLEAR");
            setAtmosphere(Atmosphere.STANDARD);

            Infantry infantry = createRegularInfantry();
            getGame().addEntity(infantry);
            infantry.setGame(getGame());

            assertFalse(infantry.canAssaultDrop(),
                  "Regular leg infantry should NOT be able to assault drop");
        }

        @Test
        @DisplayName("Powered flight infantry is protected from fall damage")
        void poweredFlightProtectedFromFallDamage() {
            setBoard("BOARD_3x3_CLEAR");
            setAtmosphere(Atmosphere.STANDARD);

            Infantry infantry = createPoweredFlightInfantry();
            getGame().addEntity(infantry);
            infantry.setGame(getGame());

            assertTrue(infantry.isProtectedFromFallDamage(),
                  "Powered flight infantry should be protected from fall damage");
        }

        @Test
        @DisplayName("Powered flight NOT protected from fall damage in vacuum")
        void poweredFlightNotProtectedInVacuum() {
            setBoard("BOARD_3x3_CLEAR");
            setAtmosphere(Atmosphere.VACUUM);

            Infantry infantry = createPoweredFlightInfantry();
            getGame().addEntity(infantry);
            infantry.setGame(getGame());

            assertFalse(infantry.isProtectedFromFallDamage(),
                  "Powered flight infantry should NOT be protected in vacuum");
        }

        @Test
        @DisplayName("Powered flight cannot assault drop in vacuum")
        void poweredFlightCannotAssaultDropInVacuum() {
            setBoard("BOARD_3x3_CLEAR");
            setAtmosphere(Atmosphere.VACUUM);

            Infantry infantry = createPoweredFlightInfantry();
            getGame().addEntity(infantry);
            infantry.setGame(getGame());

            assertFalse(infantry.canAssaultDrop(),
                  "Powered flight infantry should NOT assault drop in vacuum");
        }
    }

    @Nested
    @DisplayName("Airborne VTOL Status Tests")
    class AirborneVTOLStatusTests {

        @Test
        @DisplayName("Powered flight infantry is airborne when at elevation > 0")
        void poweredFlightIsAirborneAtElevation() {
            setBoard("BOARD_3x3_CLEAR");
            setAtmosphere(Atmosphere.STANDARD);

            Infantry infantry = createPoweredFlightInfantry();
            getGame().addEntity(infantry);
            infantry.setGame(getGame());
            infantry.setPosition(new Coords(1, 1));
            infantry.setElevation(1);

            assertTrue(infantry.isAirborneVTOLorWIGE(),
                  "Powered flight infantry at elevation > 0 should be considered airborne VTOL");
        }

        @Test
        @DisplayName("Powered flight infantry is NOT airborne at elevation 0")
        void poweredFlightNotAirborneAtGround() {
            setBoard("BOARD_3x3_CLEAR");
            setAtmosphere(Atmosphere.STANDARD);

            Infantry infantry = createPoweredFlightInfantry();
            getGame().addEntity(infantry);
            infantry.setGame(getGame());
            infantry.setPosition(new Coords(1, 1));
            infantry.setElevation(0);

            assertFalse(infantry.isAirborneVTOLorWIGE(),
                  "Powered flight infantry at elevation 0 should NOT be considered airborne");
        }

        @Test
        @DisplayName("Regular infantry is NOT airborne even at elevation > 0")
        void regularInfantryNotAirborne() {
            setBoard("BOARD_3x3_CLEAR");
            setAtmosphere(Atmosphere.STANDARD);

            Infantry infantry = createRegularInfantry();
            getGame().addEntity(infantry);
            infantry.setGame(getGame());
            infantry.setPosition(new Coords(1, 1));
            infantry.setElevation(1);

            assertFalse(infantry.isAirborneVTOLorWIGE(),
                  "Regular infantry should NOT be considered airborne VTOL");
        }

        @Test
        @DisplayName("Powered flight NOT airborne in vacuum even at elevation")
        void poweredFlightNotAirborneInVacuum() {
            setBoard("BOARD_3x3_CLEAR");
            setAtmosphere(Atmosphere.VACUUM);

            Infantry infantry = createPoweredFlightInfantry();
            getGame().addEntity(infantry);
            infantry.setGame(getGame());
            infantry.setPosition(new Coords(1, 1));
            infantry.setElevation(1);

            assertFalse(infantry.isAirborneVTOLorWIGE(),
                  "Powered flight infantry in vacuum should NOT be considered airborne");
        }
    }

    @Nested
    @DisplayName("Elevation Change Tests (IO p.85)")
    class ElevationChangeTests {

        /**
         * Creates infantry for elevation tests without setting ID so initializeUnit handles setup.
         */
        private Infantry createElevationTestInfantry(boolean poweredFlight, boolean glider, boolean nativeVTOL) {
            Infantry infantry = new Infantry();
            // Don't set ID - let initializeUnit handle it
            if (nativeVTOL) {
                infantry.setMovementMode(EntityMovementMode.VTOL);
                infantry.setMicrolite(true);
            } else {
                infantry.setMovementMode(EntityMovementMode.INF_LEG);
            }
            infantry.setSquadSize(7);
            infantry.setSquadCount(4);
            infantry.autoSetInternal();

            Crew crew = new Crew(CrewType.INFANTRY_CREW);
            crew.setGunnery(4, crew.getCrewType().getGunnerPos());
            crew.setPiloting(5, crew.getCrewType().getPilotPos());
            crew.setName("Elevation Test Crew", 0);

            if (poweredFlight || glider) {
                PilotOptions options = new PilotOptions();
                if (poweredFlight) {
                    options.getOption(OptionsConstants.MD_PL_FLIGHT).setValue(true);
                }
                if (glider) {
                    options.getOption(OptionsConstants.MD_PL_GLIDER).setValue(true);
                }
                crew.setOptions(options);
            }
            infantry.setCrew(crew);

            return infantry;
        }

        @Test
        @DisplayName("Powered flight infantry CAN descend 2 levels (VTOL movement)")
        void poweredFlightCanDescendTwoLevels() {
            setBoard("BOARD_1x2_CLIFF");
            setAtmosphere(Atmosphere.STANDARD);

            Infantry infantry = createElevationTestInfantry(true, false, false);

            // Move from level 2 hex to level 0 hex using VTOL mode
            MovePath movePath = getMovePathFor(infantry, 0, EntityMovementMode.VTOL,
                  MoveStepType.FORWARDS);

            assertTrue(movePath.isMoveLegal(),
                  "Powered flight infantry should be able to descend 2 levels using VTOL movement");
        }

        @Test
        @DisplayName("Native VTOL infantry CAN descend 2 levels")
        void nativeVTOLCanDescendTwoLevels() {
            setBoard("BOARD_1x2_CLIFF");
            setAtmosphere(Atmosphere.STANDARD);

            Infantry infantry = createElevationTestInfantry(false, false, true);

            // Move from level 2 hex to level 0 hex
            MovePath movePath = getMovePathFor(infantry, 0, EntityMovementMode.VTOL,
                  MoveStepType.FORWARDS);

            assertTrue(movePath.isMoveLegal(),
                  "Native VTOL infantry should be able to descend 2 levels");
        }

        @Test
        @DisplayName("Glider infantry CAN descend 2 levels (unlimited descent per IO p.85)")
        void gliderCanDescendTwoLevels() {
            setBoard("BOARD_1x2_CLIFF");
            setAtmosphere(Atmosphere.STANDARD);

            Infantry infantry = createElevationTestInfantry(false, true, false);

            // Glider infantry have unlimited max elevation down per IO p.85
            // Note: Cliff terrain (CLIFF_TOP) would still block them - this tests elevation only
            MovePath movePath = getMovePathFor(infantry, 0, EntityMovementMode.INF_LEG,
                  MoveStepType.FORWARDS);

            assertTrue(movePath.isMoveLegal(),
                  "Glider infantry should be able to descend any number of levels (IO p.85)");
        }

        @Test
        @DisplayName("Regular infantry CANNOT descend 2 levels")
        void regularInfantryCannotDescendTwoLevels() {
            setBoard("BOARD_1x2_CLIFF");
            setAtmosphere(Atmosphere.STANDARD);

            Infantry infantry = createElevationTestInfantry(false, false, false);

            // Regular infantry can only descend 1 level max
            MovePath movePath = getMovePathFor(infantry, 0, EntityMovementMode.INF_LEG,
                  MoveStepType.FORWARDS);

            assertFalse(movePath.isMoveLegal(),
                  "Regular infantry should NOT be able to descend more than 1 level");
        }

        @Test
        @DisplayName("All infantry CAN descend 1 level")
        void allInfantryCanDescendOneLevel() {
            setBoard("BOARD_1x2_SLOPE");
            setAtmosphere(Atmosphere.STANDARD);

            // Regular infantry can descend 1 level
            Infantry regularInfantry = createElevationTestInfantry(false, false, false);

            MovePath regularPath = getMovePathFor(regularInfantry, 0, EntityMovementMode.INF_LEG,
                  MoveStepType.FORWARDS);

            assertTrue(regularPath.isMoveLegal(),
                  "Regular infantry should be able to descend 1 level");
        }

        @Test
        @DisplayName("Powered flight infantry in vacuum CANNOT descend 2 levels")
        void poweredFlightInVacuumCannotDescendTwoLevels() {
            setBoard("BOARD_1x2_CLIFF");
            setAtmosphere(Atmosphere.VACUUM);

            Infantry infantry = createElevationTestInfantry(true, false, false);

            // In vacuum, powered flight is disabled, so infantry cannot use VTOL mode
            MovePath movePath = getMovePathFor(infantry, 0, EntityMovementMode.INF_LEG,
                  MoveStepType.FORWARDS);

            assertFalse(movePath.isMoveLegal(),
                  "Powered flight infantry in vacuum should NOT descend 2 levels - flight disabled");
        }

        @Test
        @DisplayName("Glider infantry protected from fall damage and has unlimited descent")
        void gliderProtectedFromFallDamageAndUnlimitedDescent() {
            setBoard("BOARD_1x2_CLIFF");
            setAtmosphere(Atmosphere.STANDARD);

            Infantry infantry = createGliderInfantry();
            getGame().addEntity(infantry);
            infantry.setGame(getGame());

            // Glider has fall damage protection AND unlimited descent
            assertTrue(infantry.isProtectedFromFallDamage(),
                  "Glider infantry should be protected from fall damage");
            assertFalse(infantry.hasVTOLMovementCapability(),
                  "Glider infantry should NOT have VTOL movement capability");
            assertEquals(Integer.MAX_VALUE, infantry.getMaxElevationDown(0),
                  "Glider infantry should have unlimited descent");
        }
    }

    @Nested
    @DisplayName("Battle Value Tests (IO p.85)")
    class BattleValueTests {

        @Test
        @DisplayName("Powered flight infantry has 2 jump MP for BV calculation")
        void poweredFlightHasJumpMPForBV() {
            setBoard("BOARD_3x3_CLEAR");
            setAtmosphere(Atmosphere.STANDARD);

            Infantry infantry = createPoweredFlightInfantry();
            getGame().addEntity(infantry);
            infantry.setGame(getGame());

            // Powered flight contributes 2 MP to BV calculation via getJumpMP
            int jumpMPForBV = infantry.getJumpMP(MPCalculationSetting.BV_CALCULATION);
            assertEquals(2, jumpMPForBV,
                  "Powered flight infantry should have 2 jump MP for BV calculation (IO p.85)");
        }

        @Test
        @DisplayName("Regular infantry has 0 jump MP for BV calculation")
        void regularInfantryHasNoJumpMPForBV() {
            setBoard("BOARD_3x3_CLEAR");
            setAtmosphere(Atmosphere.STANDARD);

            Infantry infantry = createRegularInfantry();
            getGame().addEntity(infantry);
            infantry.setGame(getGame());

            // Regular infantry without jump packs has 0 jump MP
            int jumpMPForBV = infantry.getJumpMP(MPCalculationSetting.BV_CALCULATION);
            assertEquals(0, jumpMPForBV,
                  "Regular infantry without jump capability should have 0 jump MP for BV");
        }

        @Test
        @DisplayName("Glider infantry has 0 jump MP for BV calculation (no BV impact)")
        void gliderHasNoJumpMPForBV() {
            setBoard("BOARD_3x3_CLEAR");
            setAtmosphere(Atmosphere.STANDARD);

            Infantry infantry = createGliderInfantry();
            getGame().addEntity(infantry);
            infantry.setGame(getGame());

            // Glider wings do NOT contribute to BV - no jump MP
            int jumpMPForBV = infantry.getJumpMP(MPCalculationSetting.BV_CALCULATION);
            assertEquals(0, jumpMPForBV,
                  "Glider infantry should have 0 jump MP for BV (no BV impact per IO p.85)");
        }

        @Test
        @DisplayName("Powered flight BV higher than regular infantry due to movement")
        void poweredFlightBVHigherThanRegular() {
            setBoard("BOARD_3x3_CLEAR");
            setAtmosphere(Atmosphere.STANDARD);

            Infantry poweredFlight = createPoweredFlightInfantry();
            getGame().addEntity(poweredFlight);
            poweredFlight.setGame(getGame());

            Infantry regular = createRegularInfantry();
            getGame().addEntity(regular);
            regular.setGame(getGame());

            // Calculate BV for both
            int poweredFlightBV = poweredFlight.calculateBattleValue();
            int regularBV = regular.calculateBattleValue();

            assertTrue(poweredFlightBV > regularBV,
                  "Powered flight infantry BV (" + poweredFlightBV +
                        ") should be higher than regular infantry BV (" + regularBV +
                        ") due to VTOL movement modifier (IO p.85)");
        }

        @Test
        @DisplayName("Glider infantry BV same as regular infantry (no BV impact)")
        void gliderBVSameAsRegular() {
            setBoard("BOARD_3x3_CLEAR");
            setAtmosphere(Atmosphere.STANDARD);

            Infantry glider = createGliderInfantry();
            getGame().addEntity(glider);
            glider.setGame(getGame());

            Infantry regular = createRegularInfantry();
            getGame().addEntity(regular);
            regular.setGame(getGame());

            // Calculate BV for both
            int gliderBV = glider.calculateBattleValue();
            int regularBV = regular.calculateBattleValue();

            assertEquals(regularBV, gliderBV,
                  "Glider infantry BV should equal regular infantry BV (no BV impact per IO p.85)");
        }
    }
}
