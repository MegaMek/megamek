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

import java.io.File;
import java.util.List;
import java.util.stream.Stream;

import megamek.common.board.CubeCoords;
import megamek.common.enums.BuildingType;
import megamek.common.equipment.EquipmentType;
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

    @BeforeAll
    static void initialize() {
        EquipmentType.initializeTypes();
    }

    @Test
    void testLoadStructureBLK() throws Exception {
        BuildingEntity e = getBuildingEntity(FILENAME_SIMPLE_BUILDING_ENTITY);
        assertEquals(0, e.getOArmor(0), "Failed to load tonnage");
        assertEquals(0, e.getEquipment().size(), "Failed to load equipment");
    }

    static Stream<Arguments> buildingTestData() {
        return Stream.of(
            // filename, chassis, model, year, buildingClass, buildingType, height, cf, armor, hexCount
            Arguments.of(
                FILENAME_SIMPLE_BUILDING_ENTITY,
                "Simple Building Entity",
                "",
                3025,
                0,
                BuildingType.LIGHT,
                4,
                15,
                0,
                1
            ),
            Arguments.of(
                FILENAME_SIMPLE_GUN_EMPLACEMENT_BUILDING_ENTITY,
                "Simple Gun Emplacement",
                "(Gauss)",
                3025,
                3,
                BuildingType.HEAVY,
                1,
                90,
                0,
                1
            ),
            Arguments.of(
                FILENAME_SIMPLE_LARGE_BUILDING_ENTITY,
                "Simple Large Building Entity",
                "",
                3025,
                2,
                BuildingType.MEDIUM,
                3,
                15,
                10,
                7
            )
        );
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("buildingTestData")
    void testBuildingProperties(String filename, String expectedChassis, String expectedModel,
                                int expectedYear, int expectedBuildingClass, BuildingType expectedBuildingType,
                                int expectedHeight, int expectedCF, int expectedArmor, int expectedHexCount) throws Exception {
        BuildingEntity building = getBuildingEntity(filename);

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
        BuildingEntity building = getBuildingEntity(FILENAME_SIMPLE_LARGE_BUILDING_ENTITY);

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
        BuildingEntity building = getBuildingEntity(FILENAME_SIMPLE_LARGE_BUILDING_ENTITY);

        for (CubeCoords coord : building.getInternalBuilding().getCoordsList()) {
            assertEquals(15, building.getInternalBuilding().getCurrentCF(coord),
                String.format("CF at %s should be 15", coord));
            assertEquals(10, building.getInternalBuilding().getArmor(coord),
                String.format("Armor at %s should be 10", coord));
        }
    }

    @Test
    void testLargeBuilding_EquipmentLocations() throws Exception {
        BuildingEntity building = getBuildingEntity(FILENAME_SIMPLE_LARGE_BUILDING_ENTITY);

        int height = building.getInternalBuilding().getBuildingHeight();
        int hexCount = building.getInternalBuilding().getCoordsList().size();
        int totalLocations = height * hexCount;

        // Expected equipment counts per location based on .blk file
        // Location = floor + (hexIndex * height)
        // Height = 3, so locations are: 0-2 (hex 0), 3-5 (hex 1), 6-8 (hex 2), etc.
        int[] expectedEquipmentCount = new int[totalLocations];

        // <0(0.0,0.0,0.0) Equipment> - floor 0, hex index 0 = location 0
        expectedEquipmentCount[0] = 2;

        // <2(1.0,-1.0,0.0) Equipment> - floor 2, hex index 1 = 2 + (1 * 3) = location 5
        expectedEquipmentCount[5] = 2;  // hex index 1, floor 2

        // <2(1.0,0.0,-1.0) Equipment> - floor 2, hex index 2 = 2 + (2 * 3) = location 8
        expectedEquipmentCount[8] = 2;  // hex index 2, floor 2

        // <2(0.0,1.0,-1.0) Equipment> - floor 2, hex index 3 = 2 + (3 * 3) = location 11
        expectedEquipmentCount[11] = 2;  // hex index 3, floor 2

        // <2(-1.0,1.0,0.0) Equipment> - floor 2, hex index 4 = 2 + (4 * 3) = location 14
        expectedEquipmentCount[14] = 2; // hex index 4, floor 2

        // <2(-1.0,0.0,1.0) Equipment> - floor 2, hex index 5 = 2 + (5 * 3) = location 17
        expectedEquipmentCount[17] = 2; // hex index 5, floor 2

        // <2(0.0,-1.0,1.0) Equipment> - floor 2, hex index 6 = 2 + (6 * 3) = location 20
        expectedEquipmentCount[20] = 2; // hex index 6, floor 2

        // Verify equipment at each location
        for (int loc = 0; loc < totalLocations; loc++) {
            int finalLoc = loc;
            int actualCount = building.getEquipment().stream()
                .filter(mounted -> mounted.getLocation() == finalLoc)
                .toList()
                .size();

            assertEquals(expectedEquipmentCount[loc], actualCount,
                String.format("Location %d: expected %d equipment, got %d",
                    loc, expectedEquipmentCount[loc], actualCount));
        }
    }

    BuildingEntity getBuildingEntity(String filename) throws Exception {
        File f = new File("testresources/megamek/common/units/" + filename);
        MekFileParser mfp = new MekFileParser(f);
        return (BuildingEntity) mfp.getEntity();
    }
}
