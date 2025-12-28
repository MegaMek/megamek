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
import megamek.common.HitData;
import megamek.common.Player;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.board.Board;
import megamek.common.board.BoardLocation;
import megamek.common.board.Coords;
import megamek.common.board.CubeCoords;
import megamek.common.enums.BasementType;
import megamek.common.enums.BuildingType;
import megamek.common.game.Game;
import megamek.common.game.IGame;
import megamek.common.net.packets.Packet;
import megamek.common.rolls.PilotingRollData;
import megamek.common.units.AbstractBuildingEntity;
import megamek.common.units.BuildingEntity;
import megamek.common.units.BuildingTerrain;
import megamek.common.units.Entity;
import megamek.common.units.IBuilding;
import megamek.common.units.Mek;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BuildingCollapseHandlerTest extends GameBoardTestCase {

    static {
        // Initialize a test board with multiple hexes for building collapse tests
        initializeBoard("COLLAPSE_TEST_BOARD", """
              size 16 17
              hex 0101 0 "" ""
              hex 0102 0 "" ""
              hex 0103 0 "" ""
              end"""
        );

        // Initialize a board with a HEAVY building for BuildingTerrain parameterized tests
        initializeBoard("HEAVY_BUILDING_BOARD", """
              size 1 1
              hex 0101 0 "bldg_elev:4;building:3;bldg_class:2;bldg_cf:80;bldg_armor:15" ""
              end"""
        );
    }

    private TWGameManager gameManager;
    private Game game;
    private Board board;
    private BuildingCollapseHandler collapseHandler;

    @BeforeEach
    void beforeEach() {
        Player player = new Player(0, "Test");
        gameManager = Mockito.spy(new TWGameManager());

        // Mock methods that require Server to avoid NullPointerException
        Mockito.doNothing().when(gameManager).send(any(Packet.class));
        Mockito.doNothing().when(gameManager).sendChangedHex(any(Coords.class), any(int.class));
        Mockito.doNothing().when(gameManager).entityUpdate(any(int.class));
        Mockito.doNothing().when(gameManager).sendChangedBuildings(any());

        game = gameManager.getGame();
        game.addPlayer(0, player);

        // Use the test board
        board = getBoard("COLLAPSE_TEST_BOARD");
        game.setBoard(board);

        collapseHandler = new BuildingCollapseHandler(gameManager);
    }

    private void initializeBuildingToGameAndBoard(IBuilding building, Coords position) {
        if (building instanceof AbstractBuildingEntity buildingEntity) {
            // For AbstractBuildingEntity, we need to add it to the game and set position
            buildingEntity.setOwner(game.getPlayer(0));
            buildingEntity.refreshLocations();
            buildingEntity.refreshAdditionalLocations();
            buildingEntity.setId(0);
            game.addEntity(buildingEntity);

            // Use first position as primary position
            buildingEntity.setPosition(position);
            buildingEntity.updateBuildingEntityHexes(board.getBoardId(), gameManager);
        }
    }

    /**
     * Resets the gameManager spy and re-applies the base mocks needed for tests.
     * Should be called at the start of parameterized tests to prevent interaction accumulation.
     */
    private void resetGameManagerSpy() {
        Mockito.reset(gameManager);
        Mockito.doNothing().when(gameManager).send(any(Packet.class));
        Mockito.doNothing().when(gameManager).sendChangedHex(any(Coords.class), any(int.class));
        Mockito.doNothing().when(gameManager).entityUpdate(any(int.class));
        Mockito.doNothing().when(gameManager).sendChangedBuildings(any());
    }

    /**
     * Switches the game board to the provided test board.
     */
    private void setupBoardForTest(Board testBoard) {
        game.setBoard(testBoard);
        board = testBoard;
    }

    /**
     * Initializes CF values for all hexes in a building.
     */
    private void initializeBuildingCF(IBuilding building, int cf) {
        for (Coords hexCoords : building.getCoordsList()) {
            building.setCurrentCF(cf, hexCoords);
            building.setPhaseCF(cf, hexCoords);
        }
    }

    /**
     * Creates a mock Mek with standard configuration for collapse tests.
     */
    private Mek createMockMek(int id, int elevation, String name, double weight) {
        return createMockMek(id, elevation, name, weight, null);
    }

    /**
     * Creates a mock Mek with standard configuration for collapse tests, with optional position.
     */
    private Mek createMockMek(int id, int elevation, String name, double weight, Coords position) {
        Mek mockMek = Mockito.mock(Mek.class);
        when(mockMek.getId()).thenReturn(id);
        when(mockMek.getElevation()).thenReturn(elevation);
        when(mockMek.getDisplayName()).thenReturn(name);
        when(mockMek.getWeight()).thenReturn(weight);

        if (position != null) {
            when(mockMek.getPosition()).thenReturn(position);
        }

        // Mock the piloting roll data
        PilotingRollData mockPSR = Mockito.mock(PilotingRollData.class);
        when(mockMek.getBasePilotingRoll()).thenReturn(mockPSR);

        // Mock hit location rolling
        HitData mockHit = new HitData(Mek.LOC_CENTER_TORSO);
        when(mockMek.rollHitLocation(anyInt(), anyInt())).thenReturn(mockHit);

        return mockMek;
    }

    /**
     * Mocks gameManager methods that are called during entity damage/fall processing.
     */
    private void mockGameManagerDamageMethods() {
        Mockito.doReturn(new Vector<Report>())
              .when(gameManager)
              .damageEntity(any(Entity.class), any(HitData.class), anyInt());
        Mockito.doReturn(new Vector<Report>())
              .when(gameManager)
              .doEntityFallsInto(any(Entity.class),
                    any(Coords.class),
                    any(PilotingRollData.class),
                    any(boolean.class));
    }

    /**
     * Tests for single-hex buildings
     */
    @Nested
    class SingleHexBuildingTests {

        @BeforeEach
        void beforeEachSingleHexTest() {
            // No additional setup needed for non-parameterized tests
            // Parameterized tests will call resetGameManagerSpy() individually
        }

        @Test
        void testCollapseBuildingEntity_BuildingInPositionMap() {
            // Create a AbstractBuildingEntity
            AbstractBuildingEntity buildingEntity = new BuildingEntity(BuildingType.MEDIUM, IBuilding.STANDARD);
            buildingEntity.setOwner(game.getPlayer(0));
            buildingEntity.getInternalBuilding().setBuildingHeight(3);
            buildingEntity.getInternalBuilding().addHex(new CubeCoords(0, 0, 0), 50, 10, BasementType.NONE, false);
            buildingEntity.refreshLocations();
            buildingEntity.refreshAdditionalLocations();
            buildingEntity.setId(0);
            game.addEntity(buildingEntity);

            // Position the BuildingEntity on the board at a valid coordinate
            Coords position = new Coords(0, 0);
            buildingEntity.setPosition(position);
            board.addBuildingToBoard(buildingEntity);

            // Create a position map that includes the BuildingEntity itself
            // Must use mutable list because BuildingCollapseHandler sorts the list
            Map<BoardLocation, List<Entity>> positionMap = new HashMap<>();
            List<Entity> entities = new ArrayList<>();
            entities.add(buildingEntity);
            positionMap.put(BoardLocation.of(position, IGame.DEFAULT_BOARD_ID), entities);

            // Create a phase report vector
            Vector<Report> vPhaseReport = new Vector<>();

            // Set the BuildingEntity to have CF so it can collapse
            buildingEntity.setCurrentCF(50, position);
            buildingEntity.setPhaseCF(50, position);

            // Call collapseBuilding with the BuildingEntity as both the building parameter and in the position map
            collapseHandler.collapseBuilding(buildingEntity, positionMap, position, true, vPhaseReport);

            // Verify that the building's CF is set to 0 after collapse
            assertEquals(0, buildingEntity.getCurrentCF(position),
                  "BuildingEntity CF should be 0 after collapse");
            assertEquals(0, buildingEntity.getPhaseCF(position),
                  "BuildingEntity phase CF should be 0 after collapse");

            // The BuildingEntity should still be in the game (it's not destroyed like a GunEmplacement)
            // BuildingEntities are marked as doomed but not immediately destroyed
            assertTrue(game.getEntitiesVector().contains(buildingEntity),
                  "BuildingEntity should still exist in game after collapse");

            // Should be doomed because over 1/2 its hexes are collapsed
            // (destroyEntity marks as doomed, actual destruction happens in phase processing)
            assertTrue(buildingEntity.isDoomed(), "BuildingEntity should be doomed after collapse");

            // Verify the special BuildingEntity collapse handling was called
            // (the method should not throw an exception when BuildingEntity is in the position map)
        }

        @Test
        void testCollapseBuildingEntity_WithOtherEntitiesInSameHex() {
            // Create a BuildingEntity
            AbstractBuildingEntity buildingEntity = new BuildingEntity(BuildingType.HEAVY, IBuilding.FORTRESS);
            buildingEntity.setOwner(game.getPlayer(0));
            buildingEntity.getInternalBuilding().setBuildingHeight(4);
            buildingEntity.getInternalBuilding().addHex(new CubeCoords(0, 0, 0), 80, 15, BasementType.NONE, false);
            buildingEntity.refreshLocations();
            buildingEntity.refreshAdditionalLocations();
            buildingEntity.setId(0);
            game.addEntity(buildingEntity);

            Coords position = new Coords(1, 0);
            buildingEntity.setPosition(position);
            board.addBuildingToBoard(buildingEntity);
            buildingEntity.setCurrentCF(80, position);
            buildingEntity.setPhaseCF(80, position);

            // Create the position map with the BuildingEntity
            // Must use mutable list because BuildingCollapseHandler sorts the list
            Map<BoardLocation, List<Entity>> positionMap = new HashMap<>();
            List<Entity> entities = new ArrayList<>();
            entities.add(buildingEntity);
            positionMap.put(BoardLocation.of(position, IGame.DEFAULT_BOARD_ID), entities);

            Vector<Report> vPhaseReport = new Vector<>();

            // Collapse the building
            collapseHandler.collapseBuilding(buildingEntity, positionMap, position, true, vPhaseReport);

            // Verify collapse occurred - CF should be set to 0
            assertEquals(0, buildingEntity.getCurrentCF(position),
                  "BuildingEntity CF should be 0 after collapse");

            // BuildingEntity should still exist (not destroyed like GunEmplacement)
            assertTrue(game.getEntitiesVector().contains(buildingEntity),
                  "BuildingEntity should still exist in game after collapse");

            // Should be doomed because over 1/2 its hexes are collapsed
            // (destroyEntity marks as doomed, actual destruction happens in phase processing)
            assertTrue(buildingEntity.isDoomed(), "BuildingEntity should be doomed after collapse");
        }

        /**
         * Provides IBuilding implementations for parameterized testing. Returns both BuildingTerrain and BuildingEntity
         * instances with their respective boards.
         */
        static Stream<Arguments> buildingImplementations() {
            // BuildingTerrain - create a fresh board for this test to avoid interference
            // Re-initialize the board to ensure it has the building terrain
            initializeBoard("TERRAIN_TEST_BOARD", """
                  size 1 1
                  hex 0101 0 "bldg_elev:4;building:3;bldg_class:2;bldg_cf:80;bldg_armor:15" ""
                  end"""
            );
            Board terrainBoard = getBoard("TERRAIN_TEST_BOARD");
            Coords terrainCoords = new Coords(0, 0);
            BuildingTerrain terrain = new BuildingTerrain(terrainCoords, terrainBoard,
                  Terrains.BUILDING, BasementType.UNKNOWN);
            Coords terrainPosition = terrainCoords;

            // BuildingEntity - manually constructed, uses COLLAPSE_TEST_BOARD at a different position
            Board entityBoard = getBoard("COLLAPSE_TEST_BOARD");
            BuildingEntity entity = new BuildingEntity(BuildingType.HEAVY, IBuilding.FORTRESS);
            entity.getInternalBuilding().setBuildingHeight(4);
            entity.getInternalBuilding().addHex(new CubeCoords(0, 0, 0), 80, 15, BasementType.NONE, false);
            Coords entityPosition = new Coords(1, 0);

            return Stream.of(
                  Arguments.of(terrain, terrainBoard, terrainPosition),
                  Arguments.of(entity, entityBoard, entityPosition)
            );
        }

        @ParameterizedTest(name = "{0} - Collapse without entities in same hex")
        @MethodSource(value = "buildingImplementations")
        void testCollapseBuilding_WithoutEntitiesInSameHex(IBuilding building, Board testBoard, Coords position) {
            setupBoardForTest(testBoard);
            initializeBuildingToGameAndBoard(building, position);

            // Create the position map
            Map<BoardLocation, List<Entity>> positionMap = new HashMap<>();
            List<Entity> entities = new ArrayList<>();

            // Only add BuildingEntity to position map if it's an entity
            if (building instanceof BuildingEntity) {
                entities.add((BuildingEntity) building);
            }
            positionMap.put(BoardLocation.of(position, IGame.DEFAULT_BOARD_ID), entities);

            Vector<Report> vPhaseReport = new Vector<>();

            // Collapse the building
            collapseHandler.collapseBuilding(building, positionMap, position, true, vPhaseReport);

            // Verify collapse occurred - CF should be set to 0
            assertEquals(0, building.getCurrentCF(position),
                  String.format("%s: CF should be 0 after collapse", building.getClass().getSimpleName()));
            assertEquals(0, building.getPhaseCF(position),
                  String.format("%s: Phase CF should be 0 after collapse", building.getClass().getSimpleName()));

            // For BuildingEntity, verify it's marked as doomed
            if (building instanceof BuildingEntity buildingEntity) {
                assertTrue(buildingEntity.isDoomed(),
                      String.format("%s: BuildingEntity should be doomed after collapse (>50%% hexes collapsed)",
                            building.getClass().getSimpleName()));
            }
        }

        @ParameterizedTest(name = "{0} - Collapse with mock entity in hex")
        @MethodSource(value = "buildingImplementations")
        void testCollapseBuilding_WithMockEntityInHex(IBuilding building, Board testBoard, Coords position) {
            setupBoardForTest(testBoard);
            initializeBuildingToGameAndBoard(building, position);
            initializeBuildingCF(building, 80);

            // Create a mock Mek entity in the building
            Mek mockMek = createMockMek(1, 2, "Test Mek", 50.0);
            game.addEntity(mockMek);

            // Create the position map with the mock entity
            Map<BoardLocation, List<Entity>> positionMap = new HashMap<>();
            List<Entity> entities = new ArrayList<>();

            // Add the BuildingEntity if it exists
            if (building instanceof BuildingEntity) {
                entities.add((BuildingEntity) building);
            }
            // Add the mock Mek
            entities.add(mockMek);

            positionMap.put(BoardLocation.of(position, IGame.DEFAULT_BOARD_ID), entities);

            Vector<Report> vPhaseReport = new Vector<>();

            mockGameManagerDamageMethods();

            // Collapse the building
            collapseHandler.collapseBuilding(building, positionMap, position, true, vPhaseReport);

            // Verify collapse occurred - CF should be set to 0
            assertEquals(0, building.getCurrentCF(position),
                  String.format("%s: CF should be 0 after collapse", building.getClass().getSimpleName()));
            assertEquals(0, building.getPhaseCF(position),
                  String.format("%s: Phase CF should be 0 after collapse", building.getClass().getSimpleName()));

            // Verify entity methods were called
            // getElevation() is called multiple times: once for sorting, once for checking floor, once for falling
            verify(mockMek, atLeastOnce()).getElevation();

            // getDisplayName() is called for report generation
            verify(mockMek, atLeastOnce()).getDisplayName();

            // getBasePilotingRoll() is called once for the PSR check before falling
            verify(mockMek, times(1)).getBasePilotingRoll();

            // addPilotingModifierForTerrain() is called once
            verify(mockMek, times(1)).addPilotingModifierForTerrain(any(PilotingRollData.class),
                  eq(position),
                  anyInt());

            // Verify gameManager methods were called
            // damageEntity should be called multiple times (damage is applied in clusters)
            verify(gameManager, atLeastOnce()).damageEntity(eq(mockMek), any(HitData.class), anyInt());

            // doEntityFallsInto should be called once (entity is at elevation 2, which is > 0)
            verify(gameManager, times(1)).doEntityFallsInto(eq(mockMek),
                  eq(position),
                  any(PilotingRollData.class),
                  eq(true));

            // entityUpdate should be called at least once for the mock entity
            // (may be called additional times for BuildingEntity if present)
            verify(gameManager, atLeastOnce()).entityUpdate(eq(mockMek.getId()));

            // For BuildingEntity, verify it's marked as doomed
            if (building instanceof BuildingEntity buildingEntity) {
                assertTrue(buildingEntity.isDoomed(),
                      String.format("%s: BuildingEntity should be doomed after collapse (>50%% hexes collapsed)",
                            building.getClass().getSimpleName()));
            }
        }

        @ParameterizedTest(name = "{0} - Collapse with mock entity in adjacent hex")
        @MethodSource(value = "buildingImplementations")
        void testCollapseBuilding_WithMockEntityInAdjacentHex(IBuilding building, Board testBoard, Coords position) {
            resetGameManagerSpy();
            setupBoardForTest(testBoard);
            initializeBuildingToGameAndBoard(building, position);
            initializeBuildingCF(building, 80);

            // Create a mock Mek entity in an adjacent hex (one hex to the south)
            Coords adjacentPosition = position.translated(3);
            // Ensure adjacentPosition is actually different from building position
            assertNotEquals(position, adjacentPosition,
                  "Adjacent position should be different from building position");

            Mek mockMek = createMockMek(1, 0, "Test Mek", 50.0, adjacentPosition);
            game.addEntity(mockMek);

            // Create the position map with entities in their respective hexes
            Map<BoardLocation, List<Entity>> positionMap = new HashMap<>();

            // Building position entities
            List<Entity> buildingHexEntities = new ArrayList<>();
            if (building instanceof BuildingEntity) {
                buildingHexEntities.add((BuildingEntity) building);
            }
            positionMap.put(BoardLocation.of(position, IGame.DEFAULT_BOARD_ID), buildingHexEntities);

            // Adjacent hex entities
            List<Entity> adjacentHexEntities = new ArrayList<>();
            adjacentHexEntities.add(mockMek);
            positionMap.put(BoardLocation.of(adjacentPosition, IGame.DEFAULT_BOARD_ID), adjacentHexEntities);

            Vector<Report> vPhaseReport = new Vector<>();

            mockGameManagerDamageMethods();

            // Collapse the building
            collapseHandler.collapseBuilding(building, positionMap, position, true, vPhaseReport);

            // Verify collapse occurred - CF should be set to 0
            assertEquals(0, building.getCurrentCF(position),
                  String.format("%s: CF should be 0 after collapse", building.getClass().getSimpleName()));
            assertEquals(0, building.getPhaseCF(position),
                  String.format("%s: Phase CF should be 0 after collapse", building.getClass().getSimpleName()));

            // Verify the mock entity in the adjacent hex was NOT damaged
            // damageEntity should never be called with the mock entity
            verify(gameManager, Mockito.never()).damageEntity(eq(mockMek), any(HitData.class), anyInt());

            // doEntityFallsInto should never be called for the adjacent entity
            verify(gameManager, Mockito.never()).doEntityFallsInto(eq(mockMek),
                  any(Coords.class),
                  any(PilotingRollData.class),
                  any(boolean.class));

            // For BuildingEntity, verify it's marked as doomed
            if (building instanceof BuildingEntity buildingEntity) {
                assertTrue(buildingEntity.isDoomed(),
                      String.format("%s: BuildingEntity should be doomed after collapse (>50%% hexes collapsed)",
                            building.getClass().getSimpleName()));
            }
        }
    }

    /**
     * Tests for multi-hex buildings (3 hexes)
     */
    @Nested
    class MultiHexBuildingTests {

        @BeforeEach
        void beforeEachMultiHexTest() {
            // Parameterized tests will call resetGameManagerSpy() individually
        }

        /**
         * Provides multi-hex IBuilding implementations for parameterized testing. Returns both BuildingTerrain and
         * BuildingEntity instances with 3 hexes.
         */
        static Stream<Arguments> multiHexBuildingImplementations() {
            // BuildingTerrain - create a 3-hex building in a line (north-south)
            // Buildings REQUIRE explicit exits because Terrain.java line 114 sets exitsSpecified=true
            // This prevents setExits() from calculating implicit connections
            // Exits: North=0(bit 1), South=3(bit 8), so top=8, middle=9, bottom=1
            // Board layout: hexes are placed in row-major order (left-to-right, top-to-bottom)
            // For 3x3 board: indices 0,1,2 are row 0; indices 3,4,5 are row 1; indices 6,7,8 are row 2
            // For vertical line at column 0: need indices 0, 3, 6 = coords (0,0), (0,1), (0,2)
            initializeBoard("MULTI_HEX_TERRAIN_BOARD", """
                  size 3 3
                  hex 0101 0 "bldg_elev:4;building:3:8;bldg_class:2;bldg_cf:80;bldg_armor:15" ""
                  hex 0201 0 "" ""
                  hex 0301 0 "" ""
                  hex 0102 0 "bldg_elev:4;building:3:9;bldg_class:2;bldg_cf:80;bldg_armor:15" ""
                  hex 0202 0 "" ""
                  hex 0302 0 "" ""
                  hex 0103 0 "bldg_elev:4;building:3:1;bldg_class:2;bldg_cf:80;bldg_armor:15" ""
                  hex 0203 0 "" ""
                  hex 0303 0 "" ""
                  end"""
            );
            Board terrainBoard = getBoard("MULTI_HEX_TERRAIN_BOARD");

            Coords terrainCoords = new Coords(0, 0);
            // Get the existing BuildingTerrain from the board (created during board load)
            BuildingTerrain terrain = (BuildingTerrain) terrainBoard.getBuildings().nextElement();
            Coords terrainPosition = terrainCoords;

            // BuildingEntity - manually constructed with 3 hexes
            Board entityBoard = getBoard("COLLAPSE_TEST_BOARD");
            BuildingEntity entity = new BuildingEntity(BuildingType.HEAVY, IBuilding.FORTRESS);
            entity.getInternalBuilding().setBuildingHeight(4);
            // Add 3 hexes in a line
            entity.getInternalBuilding().addHex(new CubeCoords(0, 0, 0), 80, 15, BasementType.NONE, false);
            entity.getInternalBuilding().addHex(new CubeCoords(0, 1, -1), 80, 15, BasementType.NONE, false);
            entity.getInternalBuilding().addHex(new CubeCoords(0, 2, -2), 80, 15, BasementType.NONE, false);
            Coords entityPosition = new Coords(1, 0);

            return Stream.of(
                  Arguments.of(terrain, terrainBoard, terrainPosition),
                  Arguments.of(entity, entityBoard, entityPosition)
            );
        }

        @ParameterizedTest(name = "{0} - Single hex destroyed, building still standing")
        @MethodSource(value = "multiHexBuildingImplementations")
        void testCollapseBuilding_SingleHexDestroyed_BuildingStillStanding(IBuilding building, Board testBoard,
              Coords position) {
            resetGameManagerSpy();
            setupBoardForTest(testBoard);
            initializeBuildingToGameAndBoard(building, position);
            initializeBuildingCF(building, 80);

            // Create position map
            Map<BoardLocation, List<Entity>> positionMap = new HashMap<>();
            List<Entity> entities = new ArrayList<>();
            if (building instanceof BuildingEntity) {
                entities.add((BuildingEntity) building);
            }
            positionMap.put(BoardLocation.of(position, IGame.DEFAULT_BOARD_ID), entities);

            Vector<Report> vPhaseReport = new Vector<>();

            // Verify building has 3 hexes before collapse
            var hexList = building.getCoordsList();
            assertEquals(3, hexList.size(),
                  String.format("%s: Building should have 3 hexes before collapse", building.getClass().getSimpleName()));

            // Collapse only the first hex
            collapseHandler.collapseBuilding(building, positionMap, position, true, vPhaseReport);

            assertEquals(0, building.getCurrentCF(position),
                  String.format("%s: First hex CF should be 0 after collapse", building.getClass().getSimpleName()));
            assertEquals(0, building.getPhaseCF(position),
                  String.format("%s: First hex phase CF should be 0 after collapse",
                        building.getClass().getSimpleName()));

            // Verify the other two hexes still have their original CF
            Coords secondHex = hexList.get(1);
            assertEquals(80, building.getCurrentCF(secondHex),
                  String.format("%s: Second hex CF should still be 80", building.getClass().getSimpleName()));
            assertEquals(80, building.getPhaseCF(secondHex),
                  String.format("%s: Second hex phase CF should still be 80", building.getClass().getSimpleName()));

            Coords thirdHex = hexList.get(2);
            assertEquals(80, building.getCurrentCF(thirdHex),
                  String.format("%s: Third hex CF should still be 80", building.getClass().getSimpleName()));
            assertEquals(80, building.getPhaseCF(thirdHex),
                  String.format("%s: Third hex phase CF should still be 80", building.getClass().getSimpleName()));

            // For BuildingEntity, verify it's NOT doomed (only 1 of 3 hexes collapsed = 33%, < 50%)
            if (building instanceof BuildingEntity buildingEntity) {
                assertFalse(buildingEntity.isDoomed(),
                      String.format("%s: BuildingEntity should NOT be doomed (only 33%% hexes collapsed)",
                            building.getClass().getSimpleName()));
            }
        }

        @ParameterizedTest(name = "{0} - Two hexes destroyed, building collapses")
        @MethodSource(value = "multiHexBuildingImplementations")
        void testCollapseBuilding_TwoHexesDestroyed_BuildingCollapses(IBuilding building, Board testBoard,
              Coords position) {
            resetGameManagerSpy();
            setupBoardForTest(testBoard);
            initializeBuildingToGameAndBoard(building, position);
            initializeBuildingCF(building, 80);

            // Create position map
            Map<BoardLocation, List<Entity>> positionMap = new HashMap<>();

            Vector<Report> vPhaseReport = new Vector<>();

            // Collapse the first hex
            List<Entity> entities1 = new ArrayList<>();
            if (building instanceof BuildingEntity) {
                entities1.add((BuildingEntity) building);
            }
            positionMap.put(BoardLocation.of(position, IGame.DEFAULT_BOARD_ID), entities1);

            // Verify building has 3 hexes before collapse
            var hexList = building.getCoordsList();
            assertEquals(3, hexList.size(),
                  String.format("%s: Building should have 3 hexes before collapse", building.getClass().getSimpleName()));

            collapseHandler.collapseBuilding(building, positionMap, position, true, vPhaseReport);

            // Collapse the second hex

            Coords secondHex = hexList.get(1);
            List<Entity> entities2 = new ArrayList<>();
            if (building instanceof BuildingEntity) {
                entities2.add((BuildingEntity) building);
            }
            positionMap.put(BoardLocation.of(secondHex, IGame.DEFAULT_BOARD_ID), entities2);
            collapseHandler.collapseBuilding(building, positionMap, secondHex, true, vPhaseReport);

            // Verify both destroyed hexes have CF = 0
            assertEquals(0, building.getCurrentCF(position),
                  String.format("%s: First hex CF should be 0 after collapse",
                        building.getClass().getSimpleName()));
            assertEquals(0, building.getPhaseCF(position),
                  String.format("%s: First hex phase CF should be 0 after collapse",
                        building.getClass().getSimpleName()));
            assertEquals(0, building.getCurrentCF(secondHex),
                  String.format("%s: Second hex CF should be 0 after collapse",
                        building.getClass().getSimpleName()));
            assertEquals(0, building.getPhaseCF(secondHex),
                  String.format("%s: Second hex phase CF should be 0 after collapse",
                        building.getClass().getSimpleName()));

            // Verify the third hex also has CF = 0 (entire building collapses when > 50% hexes destroyed)
            Coords thirdHex = hexList.get(2);
            assertEquals(0, building.getCurrentCF(thirdHex),
                  String.format("%s: Third hex CF should be 0 after building collapse",
                        building.getClass().getSimpleName()));
            assertEquals(0, building.getPhaseCF(thirdHex),
                  String.format("%s: Third hex phase CF should be 0 after building collapse",
                        building.getClass().getSimpleName()));


            // For BuildingEntity, verify it IS doomed (2 of 3 hexes collapsed = 67%, > 50%)
            if (building instanceof BuildingEntity buildingEntity) {
                assertTrue(buildingEntity.isDoomed(),
                      String.format("%s: BuildingEntity should be doomed (67%% hexes collapsed)",
                            building.getClass().getSimpleName()));
            }

        }

        @ParameterizedTest(name = "{0} - Single hex with mock entity destroyed, building still standing")
        @MethodSource(value = "multiHexBuildingImplementations")
        void testCollapseBuilding_SingleHexWithMockEntity_BuildingStillStanding(IBuilding building, Board testBoard,
              Coords position) {
            resetGameManagerSpy();
            setupBoardForTest(testBoard);
            initializeBuildingToGameAndBoard(building, position);
            initializeBuildingCF(building, 80);

            // Create a mock Mek entity in the building
            Mek mockMek = createMockMek(1, 2, "Test Mek", 50.0);
            game.addEntity(mockMek);

            // Create position map with mock entity
            Map<BoardLocation, List<Entity>> positionMap = new HashMap<>();
            List<Entity> entities = new ArrayList<>();
            if (building instanceof BuildingEntity) {
                entities.add((BuildingEntity) building);
            }
            entities.add(mockMek);
            positionMap.put(BoardLocation.of(position, IGame.DEFAULT_BOARD_ID), entities);

            Vector<Report> vPhaseReport = new Vector<>();

            // Verify building has 3 hexes before collapse
            var hexList = building.getCoordsList();
            assertEquals(3, hexList.size(),
                  String.format("%s: Building should have 3 hexes before collapse", building.getClass().getSimpleName()));

            mockGameManagerDamageMethods();

            // Collapse only the first hex
            collapseHandler.collapseBuilding(building, positionMap, position, true, vPhaseReport);

            // Verify collapse occurred
            assertEquals(0, building.getCurrentCF(position),
                  String.format("%s: CF should be 0 after collapse", building.getClass().getSimpleName()));

            // Verify entity was damaged (in the collapsing building)
            verify(mockMek, atLeastOnce()).getDisplayName();
            verify(gameManager, atLeastOnce()).damageEntity(eq(mockMek), any(HitData.class), anyInt());

            // For BuildingEntity, verify it's NOT doomed (only 1 of 3 hexes = 33%)
            if (building instanceof BuildingEntity buildingEntity) {
                assertFalse(buildingEntity.isDoomed(),
                      String.format("%s: BuildingEntity should NOT be doomed (only 33%% hexes collapsed)",
                            building.getClass().getSimpleName()));
            }
        }

        @ParameterizedTest(name = "{0} - Mock entity in adjacent hex not damaged")
        @MethodSource(value = "multiHexBuildingImplementations")
        void testCollapseBuilding_MockEntityAdjacentNotDamaged(IBuilding building, Board testBoard, Coords position) {
            resetGameManagerSpy();
            setupBoardForTest(testBoard);
            initializeBuildingToGameAndBoard(building, position);
            initializeBuildingCF(building, 80);

            // Create mock entity in adjacent hex
            Coords adjacentPosition = position.translated(3);
            Mek mockMek = createMockMek(1, 0, "Test Mek", 50.0, adjacentPosition);
            game.addEntity(mockMek);

            // Create position map
            Map<BoardLocation, List<Entity>> positionMap = new HashMap<>();
            List<Entity> buildingEntities = new ArrayList<>();
            if (building instanceof BuildingEntity) {
                buildingEntities.add((BuildingEntity) building);
            }
            positionMap.put(BoardLocation.of(position, IGame.DEFAULT_BOARD_ID), buildingEntities);

            List<Entity> adjacentEntities = new ArrayList<>();
            adjacentEntities.add(mockMek);
            positionMap.put(BoardLocation.of(adjacentPosition, IGame.DEFAULT_BOARD_ID), adjacentEntities);

            Vector<Report> vPhaseReport = new Vector<>();

            // Verify building has 3 hexes before collapse
            var hexList = building.getCoordsList();
            assertEquals(3, hexList.size(),
                  String.format("%s: Building should have 3 hexes before collapse", building.getClass().getSimpleName()));

            mockGameManagerDamageMethods();

            // Collapse first hex
            collapseHandler.collapseBuilding(building, positionMap, position, true, vPhaseReport);

            // Verify collapse occurred
            assertEquals(0, building.getCurrentCF(position));

            // Verify adjacent entity was NOT damaged
            verify(gameManager, Mockito.never()).damageEntity(eq(mockMek), any(HitData.class), anyInt());
        }
    }
}
