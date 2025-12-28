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

package megamek.common.loaders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.List;
import java.util.stream.Stream;

import megamek.common.board.CubeCoords;
import megamek.common.enums.BuildingType;
import megamek.common.equipment.EquipmentType;
import megamek.common.units.AbstractBuildingEntity;
import megamek.common.units.BuildingEntity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class BLKStructureFileTest {
    static final String FILENAME_SIMPLE_BUILDING_ENTITY = "Simple Building Entity.blk";
    static final String FILENAME_SIMPLE_GUN_EMPLACEMENT_BUILDING_ENTITY = "Simple Gun Emplacement (Gauss).blk";
    static final String FILENAME_SIMPLE_LARGE_BUILDING_ENTITY = "Simple Large Building Entity.blk";
    static final String FILENAME_HARDENED_COMMAND_FORT = "Hardened Command Fort (Clan Invasion).blk";
    static final String FILENAME_LIGHT_MISSILE_GE = "Light Missile Gun Emplacement 3039.blk";

    @BeforeAll
    static void initialize() {
        EquipmentType.initializeTypes();
    }

    @Test
    void testLoadStructureBLK() throws Exception {
        AbstractBuildingEntity e = getBuildingEntity(FILENAME_SIMPLE_BUILDING_ENTITY);
        assertEquals(0, e.getOArmor(0), "Failed to load tonnage");
        assertEquals(9, e.getEquipment().size(), "Failed to load equipment");
    }

    static Stream<Arguments> buildingTestData() {
        return Stream.of(
              // filename, chassis, model, year, buildingClass, buildingType, height, cf, armor, hexCount
              Arguments.of(FILENAME_SIMPLE_BUILDING_ENTITY,
                    "Simple Building Entity",
                    "",
                    3025,
                    0,
                    BuildingType.LIGHT,
                    4,
                    15,
                    0,
                    1),
              Arguments.of(FILENAME_SIMPLE_GUN_EMPLACEMENT_BUILDING_ENTITY,
                    "Simple Gun Emplacement",
                    "(Gauss)",
                    3025,
                    3,
                    BuildingType.HEAVY,
                    1,
                    90,
                    0,
                    1),
              Arguments.of(FILENAME_SIMPLE_LARGE_BUILDING_ENTITY,
                    "Simple Large Building Entity",
                    "",
                    3025,
                    2,
                    BuildingType.MEDIUM,
                    3,
                    15,
                    10,
                    7),
                Arguments.of(FILENAME_HARDENED_COMMAND_FORT,
                        "Hardened Command Fort",
                        "(Clan Invasion)",
                        3052,
                        2,
                        BuildingType.HARDENED,
                        2,
                        150,
                        144,
                        2),
                Arguments.of(FILENAME_LIGHT_MISSILE_GE,
                        "Light Missile Gun Emplacement",
                        "(3039)",
                        3039,
                        3,
                        BuildingType.LIGHT,
                        1,
                        30,
                        20,
                        1));
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("buildingTestData")
    void testBuildingProperties(String filename, String expectedChassis, String expectedModel,
                                int expectedYear, int expectedBuildingClass, BuildingType expectedBuildingType,
                                int expectedHeight, int expectedCF, int expectedArmor, int expectedHexCount) throws Exception {
        AbstractBuildingEntity building = getBuildingEntity(filename);

        assertEquals(expectedChassis, building.getChassis(),
            String.format("Chassis: expected '%s', got '%s'", expectedChassis, building.getChassis()));
        assertEquals(expectedModel, building.getModel(),
            String.format("Model: expected '%s', got '%s'", expectedModel, building.getModel()));
        assertEquals(expectedYear, building.getYear(),
            String.format("Year: expected %d, got %d", expectedYear, building.getYear()));
        assertEquals(expectedBuildingClass, building.getInternalBuilding().getBldgClass(),
            String.format("Building class: expected %d, got %d", expectedBuildingClass, building.getInternalBuilding().getBldgClass()));
        assertEquals(expectedBuildingType, building.getInternalBuilding().getBuildingType(),
            String.format("Building type: expected %s, got %s", expectedBuildingType, building.getInternalBuilding().getBuildingType()));
        assertEquals(expectedHeight, building.getInternalBuilding().getBuildingHeight(),
            String.format("Height: expected %d, got %d", expectedHeight, building.getInternalBuilding().getBuildingHeight()));
        assertEquals(expectedCF, building.getInternalBuilding().getCurrentCF(new CubeCoords(0, 0, 0)),
            String.format("CF: expected %d, got %d", expectedCF, building.getInternalBuilding().getCurrentCF(new CubeCoords(0, 0, 0))));
        assertEquals(expectedArmor, building.getInternalBuilding().getArmor(new CubeCoords(0, 0, 0)),
            String.format("Armor: expected %d, got %d", expectedArmor, building.getInternalBuilding().getArmor(new CubeCoords(0, 0, 0))));
        assertEquals(expectedHexCount, building.getInternalBuilding().getCoordsList().size(),
            String.format("Hex count: expected %d, got %d", expectedHexCount, building.getInternalBuilding().getCoordsList().size()));
    }

    @Test
    void testLargeBuilding_SpecificCoordinates() throws Exception {
        AbstractBuildingEntity building = getBuildingEntity(FILENAME_SIMPLE_LARGE_BUILDING_ENTITY);

        List<CubeCoords> expectedCoords = List.of(
            new CubeCoords(0, 0, 0),
            new CubeCoords(1, -1, 0),
            new CubeCoords(1, 0, -1),
            new CubeCoords(0, 1, -1),
            new CubeCoords(-1, 1, 0),
            new CubeCoords(-1, 0, 1),
            new CubeCoords(0, -1, 1)
        );

        List<CubeCoords> actualCoords = building.getInternalBuilding().getCoordsList();
        assertEquals(expectedCoords.size(), actualCoords.size());

        for (int i = 0; i < expectedCoords.size(); i++) {
            assertEquals(expectedCoords.get(i), actualCoords.get(i),
                String.format("Coordinate at index %d should match", i));
        }
    }

    @Test
    void testLargeBuilding_AllHexesHaveSameCFAndArmor() throws Exception {
        AbstractBuildingEntity building = getBuildingEntity(FILENAME_SIMPLE_LARGE_BUILDING_ENTITY);

        for (CubeCoords coord : building.getInternalBuilding().getCoordsList()) {
            assertEquals(15, building.getInternalBuilding().getCurrentCF(coord),
                String.format("CF at %s should be 15", coord));
            assertEquals(10, building.getInternalBuilding().getArmor(coord),
                String.format("Armor at %s should be 10", coord));
        }
    }

    @Test
    void testLargeBuilding_EquipmentLocations() throws Exception {
        AbstractBuildingEntity building = getBuildingEntity(FILENAME_SIMPLE_LARGE_BUILDING_ENTITY);

        int height = building.getInternalBuilding().getBuildingHeight();
        int hexCount = building.getInternalBuilding().getCoordsList().size();
        int totalLocations = height * hexCount;

        // Expected equipment counts per location based on .blk file
        // Using location names: "Level <level> <q>,<r>,<s>" (cube coordinates as doubles)
        // When building is not deployed, location names use cube coordinates instead of board numbers
        int[] expectedEquipmentCount = new int[totalLocations];

        // <Level 0 0.0,0.0,0.0 Equipment> - 2 pieces of ammo & Power Generator
        expectedEquipmentCount[getLocationByName(building, "Level 0 0.0,0.0,0.0")] = 3;

        // <Level 2 1.0,-1.0,0.0 Equipment> - 2 Machine Guns (FR)
        expectedEquipmentCount[getLocationByName(building, "Level 2 1.0,-1.0,0.0")] = 2;

        // <Level 2 1.0,0.0,-1.0 Equipment> - 2 Flamers (RR)
        expectedEquipmentCount[getLocationByName(building, "Level 2 1.0,0.0,-1.0")] = 2;

        // <Level 2 0.0,1.0,-1.0 Equipment> - 2 Machine Guns (R)
        expectedEquipmentCount[getLocationByName(building, "Level 2 0.0,1.0,-1.0")] = 2;

        // <Level 2 -1.0,1.0,0.0 Equipment> - 2 Flamers (RL)
        expectedEquipmentCount[getLocationByName(building, "Level 2 -1.0,1.0,0.0")] = 2;

        // <Level 2 -1.0,0.0,1.0 Equipment> - 2 Machine Guns (FL)
        expectedEquipmentCount[getLocationByName(building, "Level 2 -1.0,0.0,1.0")] = 2;

        // <Level 2 0.0,-1.0,1.0 Equipment> - 2 Flamers (F)
        expectedEquipmentCount[getLocationByName(building, "Level 2 0.0,-1.0,1.0")] = 2;

        // Verify equipment at each location
        for (int loc = 0; loc < totalLocations; loc++) {
            int finalLoc = loc;
            int actualCount = building.getEquipment().stream()
                .filter(mounted -> mounted.getLocation() == finalLoc)
                .toList()
                .size();

            assertEquals(expectedEquipmentCount[loc], actualCount,
                String.format("Location %d (%s): expected %d equipment, got %d",
                    loc, building.getLocationNames()[loc], expectedEquipmentCount[loc], actualCount));
        }

        // Verify equipment facings for locations with equipment
        // Level 0 0.0,0.0,0.0: Ammo equipment (no facing expected, should be -1)
        int floor1_000 = getLocationByName(building, "Level 0 0.0,0.0,0.0");
        building.getEquipment().stream()
            .filter(mounted -> mounted.getLocation() == floor1_000)
            .forEach(mounted -> assertEquals(-1, mounted.getFacing(),
                String.format("Level 0 0.0,0.0,0.0 equipment '%s' should have no facing (-1)",
                    mounted.getType().getName())));

        // Level 2 1.0,-1.0,0.0: Machine Gun (FR) - facing should be 1
        int floor3_1m10 = getLocationByName(building, "Level 2 1.0,-1.0,0.0");
        building.getEquipment().stream()
            .filter(mounted -> mounted.getLocation() == floor3_1m10)
            .forEach(mounted -> assertEquals(1, mounted.getFacing(),
                String.format("Level 2 1.0,-1.0,0.0 equipment '%s' should have facing 1 (FR)",
                    mounted.getType().getName())));

        // Level 2 1.0,0.0,-1.0: Flamer (Vehicle) (RR) - facing should be 2
        int floor3_10m1 = getLocationByName(building, "Level 2 1.0,0.0,-1.0");
        building.getEquipment().stream()
            .filter(mounted -> mounted.getLocation() == floor3_10m1)
            .forEach(mounted -> assertEquals(2, mounted.getFacing(),
                String.format("Level 2 1.0,0.0,-1.0 equipment '%s' should have facing 2 (RR)",
                    mounted.getType().getName())));

        // Level 2 0.0,1.0,-1.0: Machine Gun (R) - facing should be 3
        int floor3_01m1 = getLocationByName(building, "Level 2 0.0,1.0,-1.0");
        building.getEquipment().stream()
            .filter(mounted -> mounted.getLocation() == floor3_01m1)
            .forEach(mounted -> assertEquals(3, mounted.getFacing(),
                String.format("Level 2 0.0,1.0,-1.0 equipment '%s' should have facing 3 (R)",
                    mounted.getType().getName())));

        // Level 2 -1.0,1.0,0.0: Flamer (Vehicle) (RL) - facing should be 4
        int floor3_m110 = getLocationByName(building, "Level 2 -1.0,1.0,0.0");
        building.getEquipment().stream()
            .filter(mounted -> mounted.getLocation() == floor3_m110)
            .forEach(mounted -> assertEquals(4, mounted.getFacing(),
                String.format("Level 2 -1.0,1.0,0.0 equipment '%s' should have facing 4 (RL)",
                    mounted.getType().getName())));

        // Level 2 -1.0,0.0,1.0: Machine Gun (FL) - facing should be 5
        int floor3_m101 = getLocationByName(building, "Level 2 -1.0,0.0,1.0");
        building.getEquipment().stream()
            .filter(mounted -> mounted.getLocation() == floor3_m101)
            .forEach(mounted -> assertEquals(5, mounted.getFacing(),
                String.format("Level 2 -1.0,0.0,1.0 equipment '%s' should have facing 5 (FL)",
                    mounted.getType().getName())));

        // Level 2 0.0,-1.0,1.0: Flamer (Vehicle) (F) - facing should be 0
        int floor3_0m11 = getLocationByName(building, "Level 2 0.0,-1.0,1.0");
        building.getEquipment().stream()
            .filter(mounted -> mounted.getLocation() == floor3_0m11)
            .forEach(mounted -> assertEquals(0, mounted.getFacing(),
                String.format("Level 2 0.0,-1.0,1.0 equipment '%s' should have facing 0 (F)",
                    mounted.getType().getName())));

        assertTrue(((BuildingEntity) building).hasPower());
    }

    /**
     * Helper method to find a location index by its name.
     * Location names follow the format: "Level <level> <q>,<r>,<s>" when building is not deployed,
     * where q, r, s are cube coordinates as doubles.
     * @param building The building entity
     * @param locationName The name of the location (e.g., "Level 0 0.0,0.0,0.0" or "Level 2 1.0,-1.0,0.0")
     * @return The location index, or -1 if not found
     */
    private int getLocationByName(AbstractBuildingEntity building, String locationName) {
        String[] locationNames = building.getLocationNames();
        for (int i = 0; i < locationNames.length; i++) {
            if (locationNames[i].equals(locationName)) {
                return i;
            }
        }
        return -1;
    }

    AbstractBuildingEntity getBuildingEntity(String filename) throws Exception {
        File f = new File("testresources/megamek/common/units/" + filename);
        MekFileParser mfp = new MekFileParser(f);
        return (AbstractBuildingEntity) mfp.getEntity();
    }
}
