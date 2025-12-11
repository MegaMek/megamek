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

package megamek.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

import megamek.common.board.Coords;
import megamek.common.board.CubeCoords;
import megamek.common.enums.BasementType;
import megamek.common.enums.BuildingType;
import megamek.common.units.AbstractBuildingEntity;
import megamek.common.units.Building;
import megamek.common.units.BuildingEntity;
import megamek.common.units.BuildingTerrain;
import megamek.common.units.DemolitionCharge;
import megamek.common.units.IBuilding;
import megamek.common.units.MobileStructure;
import megamek.common.units.Terrains;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Parameterized tests for {@link IBuilding} interface.
 * Tests implementations: {@link BuildingTerrain}, {@link BuildingEntity}, and eventually {@link MobileStructure}.
 */
public class IBuildingTests extends GameBoardTestCase {

    static {
        // Initialize a board with a MEDIUM building (type 2) with HANGAR class (1)
        initializeBoard("TEST_BUILDING", """
            size 1 1
            hex 0101 0 "bldg_elev:3;building:2;bldg_class:1;bldg_cf:50;bldg_armor:10" ""
            end"""
        );
    }

    static Stream<Arguments> buildingImplementations() {
        // BuildingTerrain gets hexes from the board
        BuildingTerrain terrain = new BuildingTerrain(new Coords(0, 0), getBoard("TEST_BUILDING"), Terrains.BUILDING,
              BasementType.UNKNOWN);

        // AbstractBuildingEntity starts empty, so we need to add a hex manually
        BuildingEntity buildingEntity = new BuildingEntity(BuildingType.MEDIUM, 1);
        buildingEntity.getInternalBuilding().setBuildingHeight(3);
        buildingEntity.getInternalBuilding().addHex(new CubeCoords(0, 0, 0), 50, 10, BasementType.UNKNOWN, false);
        // IMPORTANT: Set position to populate the relativeLayout map for coordinate translation
        buildingEntity.setPosition(new Coords(0, 0));

        /*
        MobileStructure mobileStructure = new MobileStructure(BuildingType.MEDIUM, 1);
        buildingEntity.getInternalBuilding().setBuildingHeight(3);
        buildingEntity.getInternalBuilding().addHex(new CubeCoords(0, 0, 0), 50, 10, BasementType.UNKNOWN, false);
        // IMPORTANT: Set position to populate the relativeLayout map for coordinate translation
        buildingEntity.setPosition(new Coords(0, 0));
        */

        return Stream.of(
            Arguments.of("BuildingTerrain", terrain),
            Arguments.of("BuildingEntity", buildingEntity)/*,
            Arguments.of(MobileStructure, mobileStructure)
            */
        );
    }

    @ParameterizedTest(name = "{0} - CF getter/setter")
    @MethodSource("buildingImplementations")
    void testCurrentCF(String implName, IBuilding building) {
        // Use existing hex from initialization
        Coords testCoords = building.getCoordsList().get(0);

        // Test initial CF (should be 50 from board initialization)
        assertEquals(50, building.getCurrentCF(testCoords),
            String.format("%s: getCurrentCF should return initial CF", implName));

        building.setCurrentCF(30, testCoords);
        assertEquals(30, building.getCurrentCF(testCoords),
            String.format("%s: getCurrentCF should return updated CF after setCurrentCF", implName));
    }

    @ParameterizedTest(name = "{0} - Phase CF getter/setter")
    @MethodSource("buildingImplementations")
    void testPhaseCF(String implName, IBuilding building) {
        // Use existing hex from initialization
        Coords testCoords = building.getCoordsList().get(0);

        // Test initial phase CF (should be 50 from board initialization)
        assertEquals(50, building.getPhaseCF(testCoords),
            String.format("%s: getPhaseCF should return initial CF", implName));

        building.setPhaseCF(25, testCoords);
        assertEquals(25, building.getPhaseCF(testCoords),
            String.format("%s: getPhaseCF should return updated CF after setPhaseCF", implName));
    }

    @ParameterizedTest(name = "{0} - Armor getter/setter")
    @MethodSource("buildingImplementations")
    void testArmor(String implName, IBuilding building) {
        // Use existing hex from initialization
        Coords testCoords = building.getCoordsList().get(0);

        // Test initial armor (should be 10 from board initialization)
        assertEquals(10, building.getArmor(testCoords),
            String.format("%s: getArmor should return initial armor", implName));

        building.setArmor(5, testCoords);
        assertEquals(5, building.getArmor(testCoords),
            String.format("%s: getArmor should return updated armor after setArmor", implName));
    }

    @ParameterizedTest(name = "{0} - Height getter/setter")
    @MethodSource("buildingImplementations")
    void testHeight(String implName, IBuilding building) {
        // Use existing hex from initialization
        Coords testCoords = building.getCoordsList().get(0);

        // Test initial height (should be 3 from board initialization)
        assertEquals(3, building.getHeight(testCoords),
            String.format("%s: getHeight should return building height", implName));

        building.setHeight(0, testCoords);
        assertEquals(0, building.getHeight(testCoords),
            String.format("%s: getHeight should return 0 after hex destroyed", implName));
    }

    @ParameterizedTest(name = "{0} - Building type")
    @MethodSource("buildingImplementations")
    void testBuildingType(String implName, IBuilding building) {
        assertEquals(BuildingType.MEDIUM, building.getBuildingType(),
            String.format("%s: getBuildingType should return MEDIUM", implName));
    }

    @ParameterizedTest(name = "{0} - Building class")
    @MethodSource("buildingImplementations")
    void testBuildingClass(String implName, IBuilding building) {
        assertEquals(1, building.getBldgClass(),
            String.format("%s: getBldgClass should return 1 (HANGAR)", implName));
    }

    @ParameterizedTest(name = "{0} - isIn / hasCFIn")
    @MethodSource("buildingImplementations")
    void testCoordinateChecks(String implName, IBuilding building) {
        // Building already has hexes from initialization, get the first one
        Coords testCoords = building.getCoordsList().get(0);
        Coords otherCoords = new Coords(10, 10);  // Guaranteed not in building

        assertTrue(building.isIn(testCoords),
            String.format("%s: isIn should return true for existing hex", implName));
        assertFalse(building.isIn(otherCoords),
            String.format("%s: isIn should return false for non-added hex", implName));

        assertTrue(building.hasCFIn(testCoords),
            String.format("%s: hasCFIn should return true for hex with CF", implName));
        assertFalse(building.hasCFIn(otherCoords),
            String.format("%s: hasCFIn should return false for hex without CF", implName));
    }

    @ParameterizedTest(name = "{0} - Burning flag")
    @MethodSource("buildingImplementations")
    void testBurning(String implName, IBuilding building) {
        // Use existing hex from initialization
        Coords testCoords = building.getCoordsList().get(0);

        assertFalse(building.isBurning(testCoords),
            String.format("%s: isBurning should return false initially", implName));

        building.setBurning(true, testCoords);
        assertTrue(building.isBurning(testCoords),
            String.format("%s: isBurning should return true after setBurning(true)", implName));

        building.setBurning(false, testCoords);
        assertFalse(building.isBurning(testCoords),
            String.format("%s: isBurning should return false after setBurning(false)", implName));
    }

    @ParameterizedTest(name = "{0} - Damage scales")
    @MethodSource("buildingImplementations")
    void testDamageScales(String implName, IBuilding building) {
        // Building class 1 is HANGAR
        assertEquals(0.5, building.getDamageFromScale(),
            String.format("%s: getDamageFromScale for HANGAR should be 0.5", implName));
        assertEquals(1.0, building.getDamageToScale(),
            String.format("%s: getDamageToScale for HANGAR should be 1.0", implName));
    }

    @ParameterizedTest(name = "{0} - Absorption")
    @MethodSource("buildingImplementations")
    void testAbsorption(String implName, IBuilding building) {
        // Use existing hex from initialization
        Coords testCoords = building.getCoordsList().get(0);

        // CF is 50 from initialization
        int expectedAbsorption = (int) Math.ceil(50 / 10.0);
        assertEquals(expectedAbsorption, building.getAbsorption(testCoords),
            String.format("%s: getAbsorption should return ceil(CF/10)", implName));
    }

    @ParameterizedTest(name = "{0} - Damage reduction from outside")
    @MethodSource("buildingImplementations")
    void testDamageReductionFromOutside(String implName, IBuilding building) {
        // MEDIUM building type
        assertEquals(0.5f, building.getDamageReductionFromOutside(),
            String.format("%s: getDamageReductionFromOutside for MEDIUM should be 0.5", implName));
    }

    @ParameterizedTest(name = "{0} - Infantry damage from inside")
    @MethodSource("buildingImplementations")
    void testInfDmgFromInside(String implName, IBuilding building) {
        // MEDIUM building type
        assertEquals(0.0, building.getInfDmgFromInside(),
            String.format("%s: getInfDmgFromInside for MEDIUM should be 0.0", implName));
    }

    @ParameterizedTest(name = "{0} - Coordinates list")
    @MethodSource("buildingImplementations")
    void testCoordsList(String implName, IBuilding building) {
        // Building already has 1 hex from initialization
        assertEquals(1, building.getCoordsList().size(),
            String.format("%s: getCoordsList should return 1 coordinate from initialization", implName));

        // Get the existing coordinate
        Coords existingCoord = building.getCoordsList().get(0);
        assertTrue(building.getCoordsList().contains(existingCoord),
            String.format("%s: getCoordsList should contain the initialized hex", implName));
    }

    @ParameterizedTest(name = "{0} - Building name")
    @MethodSource("buildingImplementations")
    void testGetName(String implName, IBuilding building) {
        assertNotNull(building.getName(),
            String.format("%s: getName should not return null", implName));
        assertFalse(building.getName().isEmpty(),
            String.format("%s: getName should not return empty string", implName));
    }

    @ParameterizedTest(name = "{0} - Basement type getter/setter")
    @MethodSource("buildingImplementations")
    void testBasement(String implName, IBuilding building) {
        Coords testCoords = building.getCoordsList().get(0);

        // Default should be UNKNOWN
        assertEquals(BasementType.UNKNOWN, building.getBasement(testCoords),
            String.format("%s: getBasement should return UNKNOWN initially", implName));


        // Set to NONE
        building.setBasement(testCoords, BasementType.NONE);
        assertEquals(BasementType.NONE, building.getBasement(testCoords),
              String.format("%s: getBasement should return NONE after setting", implName));

        // Set to STANDARD
        building.setBasement(testCoords, BasementType.ONE_DEEP_FEET);
        assertEquals(BasementType.ONE_DEEP_FEET, building.getBasement(testCoords),
            String.format("%s: getBasement should return ONE_DEEP_FEET after setting", implName));

        // Set to HARDENED
        building.setBasement(testCoords, BasementType.TWO_DEEP_HEAD);
        assertEquals(BasementType.TWO_DEEP_HEAD, building.getBasement(testCoords),
            String.format("%s: getBasement should return TWO_DEEP_HEAD after setting", implName));
    }

    @ParameterizedTest(name = "{0} - Basement collapsed flag")
    @MethodSource("buildingImplementations")
    void testBasementCollapsed(String implName, IBuilding building) {
        Coords testCoords = building.getCoordsList().get(0);

        // Initially not collapsed
        assertFalse(building.getBasementCollapsed(testCoords),
            String.format("%s: getBasementCollapsed should return false initially", implName));

        // Set collapsed
        building.setBasementCollapsed(testCoords, true);
        assertTrue(building.getBasementCollapsed(testCoords),
            String.format("%s: getBasementCollapsed should return true after setting", implName));

        // Unset collapsed
        building.setBasementCollapsed(testCoords, false);
        assertFalse(building.getBasementCollapsed(testCoords),
            String.format("%s: getBasementCollapsed should return false after unsetting", implName));
    }

    @ParameterizedTest(name = "{0} - Demolition charges")
    @MethodSource("buildingImplementations")
    void testDemolitionCharges(String implName, IBuilding building) {
        Coords testCoords = building.getCoordsList().get(0);

        // Initially empty
        assertTrue(building.getDemolitionCharges().isEmpty(),
            String.format("%s: getDemolitionCharges should be empty initially", implName));

        // Add a charge
        building.addDemolitionCharge(1, 10, testCoords);
        assertEquals(1, building.getDemolitionCharges().size(),
            String.format("%s: should have 1 demolition charge after adding", implName));

        // Add another charge
        building.addDemolitionCharge(2, 20, testCoords);
        assertEquals(2, building.getDemolitionCharges().size(),
            String.format("%s: should have 2 demolition charges after adding another", implName));

        // Remove a charge
        DemolitionCharge charge = building.getDemolitionCharges().get(0);
        building.removeDemolitionCharge(charge);
        assertEquals(1, building.getDemolitionCharges().size(),
            String.format("%s: should have 1 demolition charge after removing", implName));
    }

    @ParameterizedTest(name = "{0} - Hex counts")
    @MethodSource("buildingImplementations")
    void testHexCounts(String implName, IBuilding building) {
        // Initial counts (1 hex from initialization)
        assertEquals(1, building.getOriginalHexCount(),
            String.format("%s: getOriginalHexCount should be 1 initially", implName));
        assertEquals(0, building.getCollapsedHexCount(),
            String.format("%s: getCollapsedHexCount should be 0 initially", implName));

        // Remove a hex
        Coords testCoords = building.getCoordsList().get(0);
        building.removeHex(testCoords);

        // Original count stays same, collapsed increases
        assertEquals(1, building.getOriginalHexCount(),
            String.format("%s: getOriginalHexCount should still be 1 after removing hex", implName));
        assertEquals(1, building.getCollapsedHexCount(),
            String.format("%s: getCollapsedHexCount should be 1 after removing hex", implName));
    }

    @ParameterizedTest(name = "{0} - Coordinate translation")
    @MethodSource("buildingImplementations")
    void testCoordinateTranslation(String implName, IBuilding building) {
        // Get the board origin (may be null for Building, non-null for others)
        Coords origin = building.getBoardOrigin();

        if (origin != null) {
            // Test board <-> relative conversion
            CubeCoords relativeCoords = building.boardToRelative(origin);
            assertNotNull(relativeCoords,
                String.format("%s: boardToRelative should not return null", implName));

            Coords backToBoard = building.relativeToBoard(relativeCoords);
            assertEquals(origin, backToBoard,
                String.format("%s: round-trip conversion should return to original coords", implName));
        }
    }

    @ParameterizedTest(name = "{0} - Board facing")
    @MethodSource("buildingImplementations")
    void testBoardFacing(String implName, IBuilding building) {
        int facing = building.getBoardFacing();
        assertTrue(facing >= 0 && facing <= 5,
            String.format("%s: getBoardFacing should return value 0-5, got %d", implName, facing));
    }

    @ParameterizedTest(name = "{0} - Internal building")
    @MethodSource("buildingImplementations")
    void testInternalBuilding(String implName, IBuilding building) {
        Building internalBuilding = building.getInternalBuilding();
        assertNotNull(internalBuilding,
            String.format("%s: getInternalBuilding should not return null", implName));
        assertEquals(building.getBuildingType(), internalBuilding.getBuildingType(),
            String.format("%s: internal building should have same type", implName));
    }
}
