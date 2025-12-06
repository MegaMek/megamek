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

package megamek.server.totalWarfare;

import megamek.common.GameBoardTestCase;
import megamek.common.Hex;
import megamek.common.Player;
import megamek.common.Report;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.board.CubeCoords;
import megamek.common.enums.BasementType;
import megamek.common.enums.BuildingType;
import megamek.common.game.Game;
import megamek.common.units.BuildingEntity;
import megamek.common.units.IBuilding;
import megamek.common.units.Terrains;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Method;
import java.util.Vector;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DeploymentProcessor}
 */
public class DeploymentProcessorTest extends GameBoardTestCase {

    DeploymentProcessor deploymentProcessor;
    TWGameManager mockTWGameManager;
    Game game;
    Board board;

    @BeforeEach
    void beforeEach() {
        // Create real Game instance
        game = new Game();

        // Create a fresh board for each test (boards are reused from static cache, so we need a fresh copy)
        // Re-initialize the board each time to avoid terrain contamination between tests
        initializeBoard("EMPTY_3x3", """
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
            end"""
        );
        board = getBoard("EMPTY_3x3");
        game.setBoard(0, board);

        // Add a test player
        game.addPlayer(0, new Player(0, "Test Player"));

        // Mock TWGameManager to return the real game
        mockTWGameManager = mock(TWGameManager.class);
        when(mockTWGameManager.getGame()).thenReturn(game);
        doNothing().when(mockTWGameManager).sendNewBuildings(any());
        doNothing().when(mockTWGameManager).sendChangedHex(any(Coords.class), anyInt());
        doNothing().when(mockTWGameManager).entityUpdate(anyInt());
        doNothing().when(mockTWGameManager).addReport(any(Report.class));

        // Create DeploymentProcessor with mocked manager
        deploymentProcessor = new DeploymentProcessor(mockTWGameManager);
    }

    /**
     * Helper method to call the private processDeployment method via reflection
     */
    private void callProcessDeployment(megamek.common.units.Entity entity, Coords coords) throws Exception {
        Method processDeployment = DeploymentProcessor.class.getDeclaredMethod(
            "processDeployment",
            megamek.common.units.Entity.class, Coords.class, int.class, int.class, int.class, Vector.class, boolean.class
        );
        processDeployment.setAccessible(true);
        processDeployment.invoke(deploymentProcessor, entity, coords, 0, 0, 0, new Vector<>(), false);
    }

    static {
        // Empty 3x3 board for BuildingEntity deployment tests
        initializeBoard("EMPTY_3x3", """
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
            end"""
        );
    }

    // ========== Test Data Providers ==========

    static Stream<Arguments> buildingTypeAndClass() {
        return Stream.of(
            Arguments.of(BuildingType.LIGHT, IBuilding.STANDARD, 1),
            Arguments.of(BuildingType.MEDIUM, IBuilding.HANGAR, 2),
            Arguments.of(BuildingType.HEAVY, IBuilding.FORTRESS, 3),
            Arguments.of(BuildingType.HARDENED, IBuilding.GUN_EMPLACEMENT, 4)
        );
    }

    // ========== BuildingEntity Deployment Tests (Lines 340-387) ==========

    @ParameterizedTest(name = "BuildingType={0}, Class={1}")
    @MethodSource("buildingTypeAndClass")
    void testDeployBuildingEntity_AddsTerrainToHex(BuildingType buildingType, int buildingClass, int expectedTypeValue) throws Exception {
        // Create BuildingEntity with specific properties
        BuildingEntity buildingEntity = new BuildingEntity(buildingType, buildingClass);
        buildingEntity.setOwner(game.getPlayer(0));
        buildingEntity.getInternalBuilding().setBuildingHeight(4);
        buildingEntity.getInternalBuilding().addHex(new CubeCoords(0, 0, 0), 50, 10, BasementType.ONE_DEEP_FEET, false);
        buildingEntity.refreshLocations();
        buildingEntity.refreshAdditionalLocations();
        buildingEntity.setId(0);
        game.addEntity(buildingEntity);

        Hex targetHex = board.getHex(new Coords(0, 0));

        // Verify hex starts empty
        assertFalse(targetHex.containsTerrain(Terrains.BUILDING),
            String.format("Hex should not contain BUILDING terrain before deployment"));

        // Call processDeployment
        callProcessDeployment(buildingEntity, new Coords(0, 0));

        // Verify BUILDING terrain was added with correct type value
        assertTrue(targetHex.containsTerrain(Terrains.BUILDING),
            String.format("Hex should contain BUILDING terrain after deployment"));
        assertEquals(expectedTypeValue, targetHex.terrainLevel(Terrains.BUILDING),
            String.format("BUILDING terrain level: expected %d, got %d",
                expectedTypeValue, targetHex.terrainLevel(Terrains.BUILDING)));

        // Verify BLDG_CLASS terrain
        assertEquals(buildingClass, targetHex.terrainLevel(Terrains.BLDG_CLASS),
            String.format("BLDG_CLASS: expected %d, got %d",
                buildingClass, targetHex.terrainLevel(Terrains.BLDG_CLASS)));

        // Verify BLDG_CF terrain
        assertEquals(50, targetHex.terrainLevel(Terrains.BLDG_CF),
            String.format("BLDG_CF: expected 50, got %d", targetHex.terrainLevel(Terrains.BLDG_CF)));

        // Verify BLDG_ARMOR terrain
        assertEquals(10, targetHex.terrainLevel(Terrains.BLDG_ARMOR),
            String.format("BLDG_ARMOR: expected 10, got %d", targetHex.terrainLevel(Terrains.BLDG_ARMOR)));

        // Verify BLDG_ELEV terrain
        assertEquals(4, targetHex.terrainLevel(Terrains.BLDG_ELEV),
            String.format("BLDG_ELEV: expected 4, got %d", targetHex.terrainLevel(Terrains.BLDG_ELEV)));

        // Verify BLDG_BASEMENT_TYPE terrain
        assertEquals(BasementType.ONE_DEEP_FEET.ordinal(), targetHex.terrainLevel(Terrains.BLDG_BASEMENT_TYPE),
            String.format("BLDG_BASEMENT_TYPE: expected %d, got %d",
                BasementType.ONE_DEEP_FEET.ordinal(), targetHex.terrainLevel(Terrains.BLDG_BASEMENT_TYPE)));
    }

    @Test
    void testDeployBuildingEntity_NoArmorSkipsArmorTerrain() throws Exception {
        // Create BuildingEntity with NO armor
        BuildingEntity buildingEntity = new BuildingEntity(BuildingType.MEDIUM, IBuilding.STANDARD);
        buildingEntity.setOwner(game.getPlayer(0));
        buildingEntity.getInternalBuilding().setBuildingHeight(3);
        buildingEntity.getInternalBuilding().addHex(new CubeCoords(0, 0, 0), 40, 0, BasementType.NONE, false);
        buildingEntity.refreshLocations();
        buildingEntity.refreshAdditionalLocations();
        buildingEntity.setId(1);
        game.addEntity(buildingEntity);

        Hex targetHex = board.getHex(new Coords(0, 0));

        // Call processDeployment
        callProcessDeployment(buildingEntity, new Coords(0, 0));

        // Verify armor terrain was NOT added (armor = 0)
        assertFalse(targetHex.containsTerrain(Terrains.BLDG_ARMOR),
            String.format("Hex should NOT contain BLDG_ARMOR terrain when armor is 0"));

        // Verify other terrains were still added
        assertTrue(targetHex.containsTerrain(Terrains.BUILDING),
            String.format("Hex should contain BUILDING terrain"));
        assertTrue(targetHex.containsTerrain(Terrains.BLDG_CF),
            String.format("Hex should contain BLDG_CF terrain"));
    }

    @Test
    void testDeployBuildingEntity_NoBasementSkipsBasementTerrain() throws Exception {
        // Create BuildingEntity with NO basement
        BuildingEntity buildingEntity = new BuildingEntity(BuildingType.HEAVY, IBuilding.FORTRESS);
        buildingEntity.setOwner(game.getPlayer(0));
        buildingEntity.getInternalBuilding().setBuildingHeight(5);
        buildingEntity.getInternalBuilding().addHex(new CubeCoords(0, 0, 0), 60, 15, null, false);
        buildingEntity.refreshLocations();
        buildingEntity.refreshAdditionalLocations();
        buildingEntity.setId(2);
        game.addEntity(buildingEntity);

        Hex targetHex = board.getHex(new Coords(0, 0));

        // Call processDeployment
        callProcessDeployment(buildingEntity, new Coords(0, 0));

        // Verify basement terrain was NOT added (basement = null)
        assertFalse(targetHex.containsTerrain(Terrains.BLDG_BASEMENT_TYPE),
            String.format("Hex should NOT contain BLDG_BASEMENT_TYPE terrain when basement is null"));

        // Verify other terrains were still added
        assertTrue(targetHex.containsTerrain(Terrains.BUILDING),
            String.format("Hex should contain BUILDING terrain"));
        assertTrue(targetHex.containsTerrain(Terrains.BLDG_ELEV),
            String.format("Hex should contain BLDG_ELEV terrain"));
    }

    @Test
    void testDeployBuildingEntity_MultipleHexes() throws Exception {
        // Create BuildingEntity spanning 3 adjacent hexes
        BuildingEntity buildingEntity = new BuildingEntity(BuildingType.MEDIUM, IBuilding.HANGAR);
        buildingEntity.setOwner(game.getPlayer(0));
        buildingEntity.getInternalBuilding().setBuildingHeight(3);
        buildingEntity.getInternalBuilding().addHex(new CubeCoords(0, 0, 0), 50, 10, BasementType.NONE, false);
        buildingEntity.getInternalBuilding().addHex(new CubeCoords(1, -1, 0), 50, 10, BasementType.NONE, false);
        buildingEntity.getInternalBuilding().addHex(new CubeCoords(1, 0, -1), 50, 10, BasementType.NONE, false);
        buildingEntity.refreshLocations();
        buildingEntity.refreshAdditionalLocations();
        buildingEntity.setId(3);
        game.addEntity(buildingEntity);

        // Call processDeployment
        callProcessDeployment(buildingEntity, new Coords(0, 0));

        // Verify all 3 hexes have building terrain
        assertEquals(3, buildingEntity.getCoordsList().size(),
            String.format("Building should occupy 3 hexes"));

        for (Coords buildingCoords : buildingEntity.getCoordsList()) {
            Hex hex = board.getHex(buildingCoords);
            assertTrue(hex.containsTerrain(Terrains.BUILDING),
                String.format("Hex %s should contain BUILDING terrain", buildingCoords));
            assertEquals(BuildingType.MEDIUM.getTypeValue(), hex.terrainLevel(Terrains.BUILDING),
                String.format("Hex %s BUILDING type: expected %d, got %d",
                    buildingCoords, BuildingType.MEDIUM.getTypeValue(), hex.terrainLevel(Terrains.BUILDING)));
            assertEquals(IBuilding.HANGAR, hex.terrainLevel(Terrains.BLDG_CLASS),
                String.format("Hex %s BLDG_CLASS: expected %d, got %d",
                    buildingCoords, IBuilding.HANGAR, hex.terrainLevel(Terrains.BLDG_CLASS)));
        }
    }

}
