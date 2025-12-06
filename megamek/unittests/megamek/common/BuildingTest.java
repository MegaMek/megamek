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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

import megamek.common.board.CubeCoords;
import megamek.common.enums.BasementType;
import megamek.common.enums.BuildingType;
import megamek.common.units.Building;
import megamek.common.units.IBuilding;
import megamek.common.units.Terrains;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests for {@link Building}
 */
public class BuildingTest {

    // ========== Test Data Providers ==========

    static Stream<Arguments> buildingTypeAndClass() {
        return Stream.of(
            Arguments.of(BuildingType.LIGHT, IBuilding.STANDARD),
            Arguments.of(BuildingType.MEDIUM, IBuilding.HANGAR),
            Arguments.of(BuildingType.HEAVY, IBuilding.FORTRESS),
            Arguments.of(BuildingType.HARDENED, IBuilding.GUN_EMPLACEMENT)
        );
    }

    static Stream<Arguments> structureTypes() {
        return Stream.of(
            Arguments.of(Terrains.BUILDING, "Building #"),
            Arguments.of(Terrains.FUEL_TANK, "Fuel Tank #"),
            Arguments.of(Terrains.BRIDGE, "Bridge #")
        );
    }

    // ========== Constructor Tests ==========

    @ParameterizedTest(name = "BuildingType={0}, Class={1}")
    @MethodSource("buildingTypeAndClass")
    void testConstructor_BuildingTypeAndClass(BuildingType type, int bldgClass) {
        Building building = new Building(type, bldgClass, 1, Terrains.BUILDING);

        assertEquals(type, building.getBuildingType(),
            String.format("Building type: expected %s, got %s", type, building.getBuildingType()));
        assertEquals(bldgClass, building.getBldgClass(),
            String.format("Building class: expected %d, got %d", bldgClass, building.getBldgClass()));
    }

    @ParameterizedTest(name = "StructureType={0}, ExpectedName={1}")
    @MethodSource("structureTypes")
    void testConstructor_StructureTypeName(int structureType, String expectedNamePrefix) {
        Building building = new Building(BuildingType.MEDIUM, IBuilding.STANDARD, 12345, structureType);

        assertTrue(building.getName().startsWith(expectedNamePrefix),
            String.format("Name: expected to start with '%s', got '%s'", expectedNamePrefix, building.getName()));
        assertTrue(building.getName().contains("12345"),
            String.format("Name: expected to contain ID '12345', got '%s'", building.getName()));
    }

    // ========== Hex Management Tests ==========

    @ParameterizedTest(name = "BuildingType={0}")
    @EnumSource(BuildingType.class)
    void testAddHex(BuildingType type) {
        Building building = new Building(type, IBuilding.STANDARD, 1, Terrains.BUILDING);
        CubeCoords coords = new CubeCoords(0, 0, 0);

        building.addHex(coords, 50, 10, BasementType.NONE, false);

        assertTrue(building.isIn(coords),
            String.format("isIn: expected true for added hex, got false"));
        assertEquals(1, building.getCoordsList().size(),
            String.format("Coords list size: expected 1, got %d", building.getCoordsList().size()));
    }

    @ParameterizedTest(name = "BuildingType={0}, Class={1}")
    @MethodSource("buildingTypeAndClass")
    void testAddMultipleHexes(BuildingType type, int bldgClass) {
        Building building = new Building(type, bldgClass, 1, Terrains.BUILDING);
        CubeCoords coords1 = new CubeCoords(0, 0, 0);
        CubeCoords coords2 = new CubeCoords(1, -1, 0);
        CubeCoords coords3 = new CubeCoords(1, 0, -1);

        building.addHex(coords1, 50, 10, BasementType.NONE, false);
        building.addHex(coords2, 50, 10, BasementType.NONE, false);
        building.addHex(coords3, 50, 10, BasementType.NONE, false);

        assertEquals(3, building.getCoordsList().size(),
            String.format("Coords list size: expected 3, got %d", building.getCoordsList().size()));
        assertTrue(building.isIn(coords1),
            String.format("isIn: expected true for coords1, got false"));
        assertTrue(building.isIn(coords2),
            String.format("isIn: expected true for coords2, got false"));
        assertTrue(building.isIn(coords3),
            String.format("isIn: expected true for coords3, got false"));
    }

    // ========== CF Tests ==========

    @ParameterizedTest(name = "BuildingType={0}, Class={1}")
    @MethodSource("buildingTypeAndClass")
    void testCurrentCF(BuildingType type, int bldgClass) {
        Building building = new Building(type, bldgClass, 1, Terrains.BUILDING);
        CubeCoords coords = new CubeCoords(0, 0, 0);

        building.addHex(coords, 75, 0, BasementType.NONE, false);
        assertEquals(75, building.getCurrentCF(coords),
            String.format("Current CF: expected 75, got %d", building.getCurrentCF(coords)));

        building.setCurrentCF(50, coords);
        assertEquals(50, building.getCurrentCF(coords),
            String.format("Current CF after update: expected 50, got %d", building.getCurrentCF(coords)));
    }

    @ParameterizedTest(name = "BuildingType={0}, Class={1}")
    @MethodSource("buildingTypeAndClass")
    void testPhaseCF(BuildingType type, int bldgClass) {
        Building building = new Building(type, bldgClass, 1, Terrains.BUILDING);
        CubeCoords coords = new CubeCoords(0, 0, 0);

        building.addHex(coords, 75, 0, BasementType.NONE, false);
        assertEquals(75, building.getPhaseCF(coords),
            String.format("Phase CF: expected 75, got %d", building.getPhaseCF(coords)));

        building.setPhaseCF(40, coords);
        assertEquals(40, building.getPhaseCF(coords),
            String.format("Phase CF after update: expected 40, got %d", building.getPhaseCF(coords)));
    }

    @ParameterizedTest(name = "BuildingType={0}")
    @EnumSource(BuildingType.class)
    void testHasCFIn(BuildingType type) {
        Building building = new Building(type, IBuilding.STANDARD, 1, Terrains.BUILDING);
        CubeCoords withCF = new CubeCoords(0, 0, 0);
        CubeCoords withoutCF = new CubeCoords(1, -1, 0);

        building.addHex(withCF, 50, 0, BasementType.NONE, false);

        assertTrue(building.hasCFIn(withCF),
            String.format("hasCFIn: expected true for hex with CF, got false"));
        assertFalse(building.hasCFIn(withoutCF),
            String.format("hasCFIn: expected false for hex without CF, got true"));
    }

    // ========== Armor Tests ==========

    @ParameterizedTest(name = "BuildingType={0}, Class={1}")
    @MethodSource("buildingTypeAndClass")
    void testArmor(BuildingType type, int bldgClass) {
        Building building = new Building(type, bldgClass, 1, Terrains.BUILDING);
        CubeCoords coords = new CubeCoords(0, 0, 0);

        building.addHex(coords, 50, 20, BasementType.NONE, false);
        assertEquals(20, building.getArmor(coords),
            String.format("Armor: expected 20, got %d", building.getArmor(coords)));

        building.setArmor(15, coords);
        assertEquals(15, building.getArmor(coords),
            String.format("Armor after update: expected 15, got %d", building.getArmor(coords)));
    }

    // ========== Height Tests ==========

    @ParameterizedTest(name = "BuildingType={0}, Class={1}")
    @MethodSource("buildingTypeAndClass")
    void testBuildingHeight(BuildingType type, int bldgClass) {
        Building building = new Building(type, bldgClass, 1, Terrains.BUILDING);
        CubeCoords coords = new CubeCoords(0, 0, 0);

        building.setBuildingHeight(5);
        building.addHex(coords, 50, 0, BasementType.NONE, false);

        assertEquals(5, building.getBuildingHeight(),
            String.format("Building height: expected 5, got %d", building.getBuildingHeight()));
        assertEquals(5, building.getHeight(coords),
            String.format("Hex height: expected 5, got %d", building.getHeight(coords)));
    }

    @ParameterizedTest(name = "BuildingType={0}")
    @EnumSource(BuildingType.class)
    void testSetHexHeight(BuildingType type) {
        Building building = new Building(type, IBuilding.STANDARD, 1, Terrains.BUILDING);
        CubeCoords coords = new CubeCoords(0, 0, 0);

        building.setBuildingHeight(4);
        building.addHex(coords, 50, 0, BasementType.NONE, false);

        building.setHeight(0, coords);
        assertEquals(0, building.getHeight(coords),
            String.format("Hex height after destroy: expected 0, got %d", building.getHeight(coords)));
    }

    // ========== Burning Tests ==========

    @ParameterizedTest(name = "BuildingType={0}, Class={1}")
    @MethodSource("buildingTypeAndClass")
    void testBurning(BuildingType type, int bldgClass) {
        Building building = new Building(type, bldgClass, 1, Terrains.BUILDING);
        CubeCoords coords = new CubeCoords(0, 0, 0);

        building.addHex(coords, 50, 0, BasementType.NONE, false);

        assertFalse(building.isBurning(coords),
            String.format("isBurning: expected false initially, got true"));

        building.setBurning(true, coords);
        assertTrue(building.isBurning(coords),
            String.format("isBurning after setBurning(true): expected true, got false"));

        building.setBurning(false, coords);
        assertFalse(building.isBurning(coords),
            String.format("isBurning after setBurning(false): expected false, got true"));
    }

    // ========== Basement Tests ==========

    @ParameterizedTest(name = "BasementType={0}")
    @EnumSource(BasementType.class)
    void testBasementType(BasementType basementType) {
        Building building = new Building(BuildingType.MEDIUM, IBuilding.STANDARD, 1, Terrains.BUILDING);
        CubeCoords coords = new CubeCoords(0, 0, 0);

        building.addHex(coords, 50, 0, basementType, false);

        assertEquals(basementType, building.getBasement(coords),
            String.format("Basement type: expected %s, got %s", basementType, building.getBasement(coords)));
    }

    @ParameterizedTest(name = "BuildingType={0}, Class={1}")
    @MethodSource("buildingTypeAndClass")
    void testBasementCollapsed(BuildingType type, int bldgClass) {
        Building building = new Building(type, bldgClass, 1, Terrains.BUILDING);
        CubeCoords coords = new CubeCoords(0, 0, 0);

        building.addHex(coords, 50, 0, BasementType.ONE_DEEP_FEET, false);
        assertFalse(building.getBasementCollapsed(coords),
            String.format("Basement collapsed: expected false initially, got true"));

        building.setBasementCollapsed(coords, true);
        assertTrue(building.getBasementCollapsed(coords),
            String.format("Basement collapsed after setBasementCollapsed(true): expected true, got false"));
    }

    // ========== Hex Count Tests ==========

    @ParameterizedTest(name = "BuildingType={0}, Class={1}")
    @MethodSource("buildingTypeAndClass")
    void testOriginalHexCount(BuildingType type, int bldgClass) {
        Building building = new Building(type, bldgClass, 1, Terrains.BUILDING);

        building.addHex(new CubeCoords(0, 0, 0), 50, 0, BasementType.NONE, false);
        building.addHex(new CubeCoords(1, -1, 0), 50, 0, BasementType.NONE, false);
        building.addHex(new CubeCoords(1, 0, -1), 50, 0, BasementType.NONE, false);

        assertEquals(3, building.getOriginalHexCount(),
            String.format("Original hex count: expected 3, got %d", building.getOriginalHexCount()));
    }

    @ParameterizedTest(name = "BuildingType={0}")
    @EnumSource(BuildingType.class)
    void testRemoveHex(BuildingType type) {
        Building building = new Building(type, IBuilding.STANDARD, 1, Terrains.BUILDING);
        CubeCoords coords1 = new CubeCoords(0, 0, 0);
        CubeCoords coords2 = new CubeCoords(1, -1, 0);

        building.addHex(coords1, 50, 0, BasementType.NONE, false);
        building.addHex(coords2, 50, 0, BasementType.NONE, false);

        assertEquals(2, building.getCoordsList().size(),
            String.format("Coords list size before remove: expected 2, got %d", building.getCoordsList().size()));

        building.removeHex(coords1);

        assertEquals(1, building.getCoordsList().size(),
            String.format("Coords list size after remove: expected 1, got %d", building.getCoordsList().size()));
        assertFalse(building.isIn(coords1),
            String.format("isIn after remove: expected false, got true"));
        assertTrue(building.isIn(coords2),
            String.format("isIn for remaining hex: expected true, got false"));
    }

    // ========== Board ID Tests ==========

    @ParameterizedTest(name = "BuildingType={0}, Class={1}")
    @MethodSource("buildingTypeAndClass")
    void testBoardId(BuildingType type, int bldgClass) {
        Building building = new Building(type, bldgClass, 1, Terrains.BUILDING);

        building.setBoardId(42);
        assertEquals(42, building.getBoardId(),
            String.format("Board ID: expected 42, got %d", building.getBoardId()));
    }

    // ========== Demolition Charge Tests ==========

    @ParameterizedTest(name = "BuildingType={0}")
    @EnumSource(BuildingType.class)
    void testDemolitionCharges(BuildingType type) {
        Building building = new Building(type, IBuilding.STANDARD, 1, Terrains.BUILDING);
        CubeCoords coords = new CubeCoords(0, 0, 0);

        building.addHex(coords, 50, 0, BasementType.NONE, false);

        assertEquals(0, building.getDemolitionCharges().size(),
            String.format("Demolition charges initially: expected 0, got %d", building.getDemolitionCharges().size()));

        building.addDemolitionCharge(1, 50, coords);
        assertEquals(1, building.getDemolitionCharges().size(),
            String.format("Demolition charges after add: expected 1, got %d", building.getDemolitionCharges().size()));
    }
}
