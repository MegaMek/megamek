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

package megamek.common.board;

import megamek.common.GameBoardTestCase;
import megamek.common.Hex;
import megamek.common.Player;
import megamek.common.enums.BasementType;
import megamek.common.enums.BuildingType;
import megamek.common.game.Game;
import megamek.common.units.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName(value = "AllowedDeploymentHelper Unit Tests")
public class AllowedDeploymentHelperTest extends GameBoardTestCase {

    private Game game;
    private Board board;
    private Player player;

    @BeforeAll
    static void beforeAll() {
        // Initialize any static resources if needed
    }

    @BeforeEach
    void beforeEach() {
        player = new Player(0, "Test Player");
        game = getGame();
        game.addPlayer(0, player);
    }

    // Provider method for ground units - shared across all nested test classes
    static Stream<Entity> groundUnits() {
        return Stream.of(
              newEntity(BipedMek.class),
              newEntity(Tank.class),
              newEntity(SupportTank.class)
        );
    }

    // Provider method for ground units - shared across all nested test classes
    static Stream<Entity> elevationUnits() {
        return Stream.of(
              newEntity(VTOL.class),
              newEntity(SupportVTOL.class)
        );
    }

    // Provider method for aerospace units - shared across all nested test classes
    static Stream<Entity> aerospaceUnits() {
        return Stream.of(
              newEntity(AeroSpaceFighter.class),
              newEntity(ConvFighter.class)
        );
    }

    // Provider method for building entities - shared across all nested test classes
    static Stream<Entity> singleHexAbstractBuildings() {
        return Stream.of(
              newSingleHexBuildingEntity()
        );
    }

    // Provider method for building entities - shared across all nested test classes
    static Stream<Entity> multiHexAbstractBuildings() {
        return Stream.of(
              newLongMultiHexBuildingEntity(),
              newXShapeMultiHexBuildingEntity()
        );
    }

    // Provider method for all single-hex units - shared across all nested test classes
    static Stream<Entity> singleHexUnits() {
        return Stream.concat(
              Stream.concat(groundUnits(), aerospaceUnits()),
              singleHexAbstractBuildings()
        );
    }

    // Provider method for all units - shared across all nested test classes
    static Stream<Entity> allUnits() {
        return Stream.concat(singleHexUnits(), multiHexAbstractBuildings());
    }

    static private <T extends Entity> T newEntity(Class<T> entityType) {
        Entity newEntity;
        if (entityType.isInstance(AbstractBuildingEntity.class)) {
            throw new ExceptionInInitializerError("Don't use this method to make AbstractBuildingEntity!");
        }
        try {
            newEntity = entityType.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create entity of type " + entityType.getName(), e);
        }
        if (newEntity != null) {
            newEntity.setId(0);
        }
        return (T) newEntity;
    }

    static private Entity newSingleHexBuildingEntity() {
        final int HEIGHT = 1;
        final int CF = 10;
        final int ARMOR = 0;

        BuildingEntity buildingEntity = new BuildingEntity(BuildingType.LIGHT, IBuilding.STANDARD);
        buildingEntity.getInternalBuilding().setBuildingHeight(HEIGHT);
        buildingEntity.getInternalBuilding().addHex(CubeCoords.ZERO, CF, ARMOR, BasementType.NONE, false);
        buildingEntity.setId(0);
        return buildingEntity;
    }

    static private Entity newLongMultiHexBuildingEntity() {
        final int HEIGHT = 1;
        final int CF = 10;
        final int ARMOR = 0;

        BuildingEntity buildingEntity = new BuildingEntity(BuildingType.LIGHT, IBuilding.STANDARD);
        buildingEntity.getInternalBuilding().setBuildingHeight(HEIGHT);
        buildingEntity.getInternalBuilding().addHex(CubeCoords.ZERO, CF, ARMOR, BasementType.NONE, false);
        buildingEntity.getInternalBuilding().addHex(new CubeCoords(0.0, -1.0, 1.0), CF, ARMOR, BasementType.NONE,
              false);
        buildingEntity.getInternalBuilding().addHex(new CubeCoords(0.0, 1.0, -1.0), CF, ARMOR, BasementType.NONE,
              false);
        buildingEntity.setId(0);
        return buildingEntity;
    }

    static private Entity newXShapeMultiHexBuildingEntity() {
        final int HEIGHT = 1;
        final int CF = 10;
        final int ARMOR = 0;

        BuildingEntity buildingEntity = new BuildingEntity(BuildingType.LIGHT, IBuilding.STANDARD);
        buildingEntity.getInternalBuilding().setBuildingHeight(HEIGHT);
        buildingEntity.getInternalBuilding().addHex(CubeCoords.ZERO, CF, ARMOR, BasementType.NONE, false);
        buildingEntity.getInternalBuilding().addHex(new CubeCoords(0.0, -1.0, 1.0), CF, ARMOR, BasementType.NONE,
              false);
        buildingEntity.getInternalBuilding().addHex(new CubeCoords(0.0, 1.0, -1.0), CF, ARMOR, BasementType.NONE,
              false);
        buildingEntity.getInternalBuilding().addHex(new CubeCoords(-1.0, 1.0, 0.0), CF, ARMOR, BasementType.NONE,
              false);
        buildingEntity.getInternalBuilding().addHex(new CubeCoords(1.0, -1.0, 0.0), CF, ARMOR, BasementType.NONE,
              false);
        buildingEntity.setId(0);
        return buildingEntity;
    }

    /**
     * Tests for {@link AllowedDeploymentHelper#findAllowedElevations()} and
     * {@link AllowedDeploymentHelper#findAllowedElevations(DeploymentElevationType)}
     */
    @Nested
    @DisplayName(value = "findAllowedElevations Tests")
    class FindAllowedElevationsTests {

        static {
            initializeBoard("GROUND_BOARD", """
                  size 3 3
                  option exit_roads_to_pavement false
                  hex 0101 0 "" ""
                  hex 0201 0 "" ""
                  hex 0301 0 "" ""
                  hex 0102 0 "" ""
                  hex 0202 0 "" ""
                  hex 0302 0 "" ""
                  hex 0103 0 "" ""
                  hex 0203 0 "" ""
                  hex 0303 0 "" ""
                  end
                  """);

            initializeBoard("WATER_BOARD", """
                  size 2 2
                  option exit_roads_to_pavement false
                  hex 0101 0 "water:2" ""
                  hex 0201 0 "water:2" ""
                  hex 0102 0 "water:2" ""
                  hex 0202 0 "water:2" ""
                  end
                  """);

            initializeBoard("LOW_ALTITUDE_BOARD", """
                  size 2 2
                  option exit_roads_to_pavement false
                  option set_atmospheric true
                  hex 0101 0 "" ""
                  hex 0201 0 "" ""
                  hex 0102 0 "" ""
                  hex 0202 0 "" ""
                  end
                  """);
        }

        @BeforeEach
        void beforeEach() {
            setBoard("GROUND_BOARD");
            board = getBoard("GROUND_BOARD");
        }

        @ParameterizedTest(name = "{index} - {0}")
        @MethodSource(value = "megamek.common.board.AllowedDeploymentHelperTest#groundUnits")
        @DisplayName(value = "should return ground elevation for ground units on ground board")
        void shouldReturnGroundElevation_ForGroundUnits(Entity entity) {
            // Arrange
            entity.setOwner(player);
            game.addEntity(entity);
            Coords coords = new Coords(0, 0);
            Hex hex = board.getHex(coords);
            AllowedDeploymentHelper helper = new AllowedDeploymentHelper(entity, coords, board, hex, game);

            // Act
            List<ElevationOption> elevations = helper.findAllowedElevations();

            // Assert
            assertFalse(elevations.isEmpty(), "Ground units should have at least one elevation option");
            assertTrue(elevations.stream().anyMatch(e -> e.elevation() == 0),
                  "Ground units should be able to deploy at elevation 0");
        }

        @ParameterizedTest(name = "{index} - {0}")
        @MethodSource(value = "megamek.common.board.AllowedDeploymentHelperTest#aerospaceUnits")
        @DisplayName(value = "should return altitude options for aerospace units")
        void shouldReturnAltitudeOptions_ForAerospaceUnits(Entity entity) {
            // Arrange
            entity.setOwner(player);
            game.addEntity(entity);
            Coords coords = new Coords(0, 0);
            Hex hex = board.getHex(coords);
            AllowedDeploymentHelper helper = new AllowedDeploymentHelper(entity, coords, board, hex, game);

            // Act
            List<ElevationOption> elevations = helper.findAllowedElevations();

            // Assert
            assertFalse(elevations.isEmpty(), "Aerospace units should have elevation options");
            assertTrue(elevations.stream().anyMatch(e -> e.type() == DeploymentElevationType.ALTITUDE),
                  "Aerospace units should have altitude options");
        }

        @Test
        @DisplayName(value = "should return water surface elevation for water hex")
        void shouldReturnWaterSurfaceElevation_ForWaterHex() {
            // Arrange
            setBoard("WATER_BOARD");
            Board waterBoard = getBoard("WATER_BOARD");
            Entity entity = new Tank();
            entity.setId(0);
            entity.setOwner(player);
            game.addEntity(entity);
            Coords coords = new Coords(0, 0);
            Hex hex = waterBoard.getHex(coords);
            AllowedDeploymentHelper helper = new AllowedDeploymentHelper(entity, coords, waterBoard, hex, game);

            // Act
            List<ElevationOption> elevations = helper.findAllowedElevations();

            // Assert
            assertFalse(elevations.isEmpty(), "Naval units should have elevation options on water");
        }

        @ParameterizedTest(name = "{index} - {0}")
        @MethodSource(value = "megamek.common.board.AllowedDeploymentHelperTest#aerospaceUnits")
        @DisplayName(value = "should include ground elevation for aerospace units on ground board")
        void shouldIncludeGroundElevation_ForAerospaceUnitsOnGroundBoard(Entity entity) {
            // Arrange
            entity.setOwner(player);
            game.addEntity(entity);
            Coords coords = new Coords(0, 0);
            Hex hex = board.getHex(coords);
            AllowedDeploymentHelper helper = new AllowedDeploymentHelper(entity, coords, board, hex, game);

            // Act
            List<ElevationOption> elevations = helper.findAllowedElevations();

            // Assert
            assertTrue(elevations.stream()
                        .anyMatch(e -> e.elevation() == 0 && e.type() == DeploymentElevationType.ON_GROUND),
                  "Aerospace units should be able to land at elevation 0 on ground boards");
        }

        @ParameterizedTest(name = "{index} - {0}")
        @MethodSource(value = "megamek.common.board.AllowedDeploymentHelperTest#aerospaceUnits")
        @DisplayName(value = "should start at altitude 1 for aerospace units on low altitude board")
        void shouldStartAtAltitude1_ForAerospaceUnitsOnLowAltitudeBoard(Entity entity) {
            // Arrange
            setBoard("LOW_ALTITUDE_BOARD");
            Board lowAltBoard = getBoard("LOW_ALTITUDE_BOARD");
            entity.setOwner(player);
            game.addEntity(entity);
            Coords coords = new Coords(0, 0);
            Hex hex = lowAltBoard.getHex(coords);
            AllowedDeploymentHelper helper = new AllowedDeploymentHelper(entity, coords, lowAltBoard, hex, game);

            // Act
            List<ElevationOption> elevations = helper.findAllowedElevations(DeploymentElevationType.ALTITUDE);

            // Assert
            assertFalse(elevations.isEmpty(), "Aerospace units should have altitude options");
            assertTrue(elevations.stream().allMatch(e -> e.elevation() >= 1),
                  "On low altitude boards, aerospace units should start at altitude 1 or higher");
        }

        @ParameterizedTest(name = "{index} - {0}")
        @MethodSource(value = "megamek.common.board.AllowedDeploymentHelperTest#groundUnits")
        @DisplayName(value = "should filter by deployment type when limitToType is specified")
        void shouldFilterByDeploymentType_WhenLimitToTypeSpecified(Entity entity) {
            // Arrange
            entity.setOwner(player);
            game.addEntity(entity);
            Coords coords = new Coords(0, 0);
            Hex hex = board.getHex(coords);
            AllowedDeploymentHelper helper = new AllowedDeploymentHelper(entity, coords, board, hex, game);

            // Act
            List<ElevationOption> elevations = helper.findAllowedElevations(DeploymentElevationType.ON_GROUND);

            // Assert
            assertTrue(elevations.stream().allMatch(e -> e.type() == DeploymentElevationType.ON_GROUND),
                  "When limitToType is specified, all returned elevations should match that type");
        }

        @Test
        @DisplayName(value = "should throw IllegalStateException for space board")
        void shouldThrowIllegalStateException_ForSpaceBoard() {
            // Arrange
            Board spaceBoard = Board.getSpaceBoard(1, 1);
            spaceBoard.setMapName("SPACE_BOARD");
            Entity entity = new AeroSpaceFighter();
            entity.setOwner(player);
            game.addEntity(entity);
            Coords coords = new Coords(0, 0);
            Hex hex = spaceBoard.getHex(coords);
            AllowedDeploymentHelper helper = new AllowedDeploymentHelper(entity, coords, spaceBoard, hex, game);

            // Act & Assert
            assertThrows(IllegalStateException.class, helper::findAllowedElevations,
                  "Should throw IllegalStateException when board is space");
        }
    }

    /**
     * Tests for {@link AllowedDeploymentHelper#findAllowedFacings(int)}
     */
    @Nested
    @DisplayName(value = "findAllowedFacings Tests")
    class FindAllowedFacingsTests {

        static {
            initializeBoard("FACING_BOARD", """
                  size 3 3
                  option exit_roads_to_pavement false
                  hex 0101 0 "" ""
                  hex 0201 0 "" ""
                  hex 0301 0 "" ""
                  hex 0102 1 "" ""
                  hex 0202 0 "" ""
                  hex 0302 0 "" ""
                  hex 0103 0 "" ""
                  hex 0203 0 "" ""
                  hex 0303 1 "" ""
                  end
                  """);
        }

        @BeforeEach
        void beforeEach() {
            setBoard("FACING_BOARD");
            board = getBoard("FACING_BOARD");
        }

        @ParameterizedTest(name = "{index} - {0}")
        @MethodSource(value = "megamek.common.board.AllowedDeploymentHelperTest#singleHexUnits")
        @DisplayName(value = "should return all facings for single-hex units")
        void shouldReturnAllFacings_ForSingleHexUnits(Entity entity) {
            // Arrange
            entity.setOwner(player);
            game.addEntity(entity);
            Coords coords = new Coords(1, 1);
            Hex hex = board.getHex(coords);
            AllowedDeploymentHelper helper = new AllowedDeploymentHelper(entity, coords, board, hex, game);

            // Act
            FacingOption facingOption = helper.findAllowedFacings(0);

            // Assert
            assertNotNull(facingOption, "Single-hex units should have facing options");
            assertEquals(6, facingOption.getValidFacings().size(),
                  "Single-hex units should have all 6 facings available");
        }

        @ParameterizedTest(name = "{index} - {0}")
        @MethodSource(value = "megamek.common.board.AllowedDeploymentHelperTest#allUnits")
        @DisplayName(value = "should return FacingOption with correct coords and elevation")
        void shouldReturnFacingOptionWithCorrectCoordsAndElevation(Entity entity) {
            // Arrange
            entity.setOwner(player);
            game.addEntity(entity);
            Coords coords = new Coords(1, 1);
            Hex hex = board.getHex(coords);
            AllowedDeploymentHelper helper = new AllowedDeploymentHelper(entity, coords, board, hex, game);
            int elevation = 2;

            // Act
            FacingOption facingOption = helper.findAllowedFacings(elevation);

            // Assert
            assertNotNull(facingOption);
            assertEquals(coords, facingOption.getPosition(), "FacingOption should have correct coordinates");
            assertEquals(elevation, facingOption.getElevation(), "FacingOption should have correct elevation");
        }

        @Test
        @DisplayName(value = "should return 4 facings for the long building on the test map")
        void shouldReturn4Facings_ForLongBuilding() {
            // Arrange
            Entity entity = newLongMultiHexBuildingEntity();
            entity.setOwner(player);
            game.addEntity(entity);
            Coords coords = new Coords(1, 1);
            Hex hex = board.getHex(coords);
            AllowedDeploymentHelper helper = new AllowedDeploymentHelper(entity, coords, board, hex, game);

            // Act
            FacingOption facingOption = helper.findAllowedFacings(0);

            // Assert
            assertNotNull(facingOption, "Multi-hex units should have facing options");
            assertEquals(4, facingOption.getValidFacings().size(),
                  "Long multi hex building should have 4 facings available available with the test layout");
        }

        @Test
        @DisplayName(value = "should return 2 facings for the x shaped building on the test map")
        void shouldReturn2Facings_ForXShapeBuilding() {
            // Arrange
            Entity entity = newXShapeMultiHexBuildingEntity();
            entity.setOwner(player);
            game.addEntity(entity);
            Coords coords = new Coords(1, 1);
            Hex hex = board.getHex(coords);
            AllowedDeploymentHelper helper = new AllowedDeploymentHelper(entity, coords, board, hex, game);

            // Act
            FacingOption facingOption = helper.findAllowedFacings(0);

            // Assert
            assertNotNull(facingOption, "Multi-hex units should have facing options");
            assertEquals(2, facingOption.getValidFacings().size(),
                  "X shaped multi hex building should have 2 facings available available with the test layout");
        }
    }
}
