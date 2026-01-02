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

package megamek.client.ui.panels.phaseDisplay;

import megamek.client.Client;
import megamek.client.event.BoardViewEvent;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.clientGUI.MegaMekGUI;
import megamek.client.ui.panels.phaseDisplay.DeploymentDisplay.BoardValidationResult;
import megamek.client.ui.panels.phaseDisplay.DeploymentDisplay.DeploymentPosition;
import megamek.client.ui.util.MegaMekController;
import megamek.common.GameBoardTestCase;
import megamek.common.Player;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.units.BipedMek;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.SmallCraft;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("DeploymentDisplay Unit Tests")
public class DeploymentDisplayTest {

    private DeploymentDisplay deploymentDisplay;
    private ClientGUI mockClientGUI;
    private Client mockClient;
    private MockedStatic<MegaMekGUI> mockedMegaMekGUI;
    private Game game;
    private Board board;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void beforeEach() {
        // Mock the static MegaMekGUI.getKeyDispatcher() to avoid NPE in AbstractPhaseDisplay constructor
        MegaMekController mockController = mock(MegaMekController.class);
        mockedMegaMekGUI = mockStatic(MegaMekGUI.class);
        mockedMegaMekGUI.when(MegaMekGUI::getKeyDispatcher).thenReturn(mockController);

        // Create mocks for dependencies
        mockClientGUI = mock(ClientGUI.class);
        mockClient = mock(Client.class);

        // Set up the game with a board
        board = mock(Board.class);
        game = new Game(board);
        when(mockClientGUI.getClient()).thenReturn(mockClient);
        when(mockClient.getGame()).thenReturn(getGame());

        // Create DeploymentDisplay with mocked dependencies
        deploymentDisplay = new DeploymentDisplay(mockClientGUI);
    }

    @AfterEach
    void tearDown() {
        // Close the static mock to avoid memory leaks and interference with other tests
        if (mockedMegaMekGUI != null) {
            mockedMegaMekGUI.close();
        }
    }

    /**
     * Tests for {@link DeploymentDisplay#shouldProcessDeployment(BoardViewEvent, Coords, Entity)}
     * <br><br>
     * Note: {@link #getGame()} isn't {@link DeploymentDisplayTest#getGame()}
     */
    @Nested
    @DisplayName("shouldProcessDeployment Tests")
    class ShouldProcessDeploymentTests extends GameBoardTestCase {

        static {
            initializeBoard("BOARD", """
                  size 2 2
                  hex 0101 0 "" ""
                  hex 0201 0 "" ""
                  hex 0102 0 "" ""
                  hex 0202 0 "" ""
                  end"""
            );
        }

        private BoardViewEvent mockEvent;
        private Coords coords;
        private Entity entity;

        @BeforeEach
        void beforeEach() {
            coords = new Coords(1, 1);
            entity = new BipedMek();
            getGame().addEntity(entity);
            setBoard("BOARD");

            mockEvent = mock(BoardViewEvent.class);
            when(mockEvent.getBoardId()).thenReturn(getGame().getBoard().getBoardId());
            when(mockEvent.getType()).thenReturn(BoardViewEvent.BOARD_HEX_DRAGGED);
            when(mockEvent.getButton()).thenReturn(MouseEvent.BUTTON1);
            when(mockEvent.getModifiers()).thenReturn(0);
            when(mockClient.isMyTurn()).thenReturn(true);
            when(mockClient.getGame()).thenReturn(getGame());

            // Create DeploymentDisplay with mocked dependencies
            deploymentDisplay = new DeploymentDisplay(mockClientGUI);
        }

        @Test
        @DisplayName("should return true when all conditions are met")
        void shouldReturnTrue_WhenAllConditionsMet() {
            // Arrange: All setup done in @BeforeEach

            // Act
            boolean result = deploymentDisplay.shouldProcessDeployment(mockEvent, coords, entity);

            // Assert
            assertTrue(result, "Should process deployment when all conditions are valid");
        }

        @Test
        @DisplayName("should return false when entity is null")
        void shouldReturnFalse_WhenEntityIsNull() {
            // Arrange: Entity is null

            // Act
            boolean result = deploymentDisplay.shouldProcessDeployment(mockEvent, coords, null);

            // Assert
            assertFalse(result, "Should not process deployment when entity is null");
        }

        @Test
        @DisplayName("should return false when not player's turn")
        void shouldReturnFalse_WhenNotPlayersTurn() {
            // Arrange
            when(mockClient.isMyTurn()).thenReturn(false);

            // Act
            boolean result = deploymentDisplay.shouldProcessDeployment(mockEvent, coords, entity);

            // Assert
            assertFalse(result, "Should not process deployment when it's not player's turn");
        }

        @Test
        @DisplayName("should return false when wrong button is clicked")
        void shouldReturnFalse_WhenWrongButton() {
            // Arrange
            when(mockEvent.getButton()).thenReturn(MouseEvent.BUTTON3);

            // Act
            boolean result = deploymentDisplay.shouldProcessDeployment(mockEvent, coords, entity);

            // Assert
            assertFalse(result, "Should not process deployment with right mouse button");
        }

        @Test
        @DisplayName("should return false when CTRL is held")
        void shouldReturnFalse_WhenCtrlHeld() {
            // Arrange
            when(mockEvent.getModifiers()).thenReturn(InputEvent.CTRL_DOWN_MASK);

            // Act
            boolean result = deploymentDisplay.shouldProcessDeployment(mockEvent, coords, entity);

            // Assert
            assertFalse(result, "Should not process deployment when CTRL is held");
        }

        @Test
        @DisplayName("should return false when ALT is held")
        void shouldReturnFalse_WhenAltHeld() {
            // Arrange
            when(mockEvent.getModifiers()).thenReturn(InputEvent.ALT_DOWN_MASK);

            // Act
            boolean result = deploymentDisplay.shouldProcessDeployment(mockEvent, coords, entity);

            // Assert
            assertFalse(result, "Should not process deployment when ALT is held");
        }

        @Test
        @DisplayName("should return false when wrong event type")
        void shouldReturnFalse_WhenWrongEventType() {
            // Arrange
            when(mockEvent.getType()).thenReturn(BoardViewEvent.BOARD_HEX_CLICKED);

            // Act
            boolean result = deploymentDisplay.shouldProcessDeployment(mockEvent, coords, entity);

            // Assert
            assertFalse(result, "Should not process deployment for click events");
        }

        @Test
        @DisplayName("should return false when ignoring events")
        void shouldReturnFalse_WhenIgnoringEvents() {
            // Arrange
            DeploymentDisplay spyDisplay = spy(deploymentDisplay);
            doReturn(true).when(spyDisplay).isIgnoringEvents();

            // Act
            boolean result = spyDisplay.shouldProcessDeployment(mockEvent, coords, entity);

            // Assert
            assertFalse(result, "Should not process deployment when ignoring events");
        }

        @Test
        @DisplayName("should return true when SHIFT is held (other modifiers are OK)")
        void shouldReturnTrue_WhenShiftHeld() {
            // Arrange
            when(mockEvent.getModifiers()).thenReturn(InputEvent.SHIFT_DOWN_MASK);

            // Act
            boolean result = deploymentDisplay.shouldProcessDeployment(mockEvent, coords, entity);

            // Assert
            assertTrue(result, "Should process deployment when only SHIFT is held");
        }
    }

    /**
     * Tests for {@link DeploymentDisplay#validateDeploymentBoard(Entity, Board, Coords)}
     * <br><br>
     * Note: {@link #getGame()} isn't {@link DeploymentDisplayTest#getGame()}
     */
    @Nested
    @DisplayName("validateDeploymentBoard Tests")
    class ValidateDeploymentBoardTests extends GameBoardTestCase {

        static {
            initializeBoard("GROUND_BOARD", """
                  size 1 1
                  hex 0101 0 "" ""
                  end"""
            );
        }

        private Entity groundEntity;
        private Board groundBoard;
        private Coords coords;
        private Player player;

        @BeforeEach
        void setUpBoard() {
            player = new Player(0, "Test Player");
            getGame().addPlayer(0, player);

            groundEntity = new BipedMek();
            groundEntity.setOwner(player);
            groundBoard = getBoard("GROUND_BOARD");
            coords = new Coords(0, 0);
            getGame().addEntity(groundEntity);
        }

        @Test
        @DisplayName("should return VALID for normal ground deployment")
        void shouldReturnValid_ForNormalGroundDeployment() {
            // Act
            BoardValidationResult result = deploymentDisplay.validateDeploymentBoard(
                  groundEntity, groundBoard, coords);

            // Assert
            assertEquals(BoardValidationResult.VALID, result,
                  "Should return VALID for legal ground deployment");
        }

        @Test
        @DisplayName("should return WRONG_BOARD_TYPE when entity cannot deploy on board type")
        void shouldReturnWrongBoardType_WhenBoardProhibited() {
            // Arrange
            Entity spyEntity = spy(groundEntity);
            when(spyEntity.isBoardProhibited(groundBoard)).thenReturn(true);

            // Act
            BoardValidationResult result = deploymentDisplay.validateDeploymentBoard(
                  spyEntity, groundBoard, coords);

            // Assert
            assertEquals(BoardValidationResult.WRONG_BOARD_TYPE, result,
                  "Should return WRONG_BOARD_TYPE when board is prohibited");
        }

        @Test
        @DisplayName("should return OUTSIDE_DEPLOYMENT_AREA when coords are illegal")
        void shouldReturnOutsideDeploymentArea_WhenIllegalCoords() {
            // Arrange
            Coords illegalCoords = new Coords(-1, -1);

            // Act
            BoardValidationResult result = deploymentDisplay.validateDeploymentBoard(
                  groundEntity, groundBoard, illegalCoords);

            // Assert
            assertEquals(BoardValidationResult.OUTSIDE_DEPLOYMENT_AREA, result,
                  "Should return OUTSIDE_DEPLOYMENT_AREA for illegal coordinates");
        }
    }

    /**
     * Tests for {@link DeploymentDisplay#applyDeploymentToEntity(Entity, DeploymentPosition)}
     */
    @Nested
    @DisplayName("applyDeploymentToEntity Tests")
    class ApplyDeploymentToEntityTests {

        @Test
        @DisplayName("should set elevation for ground unit")
        void shouldSetElevation_ForGroundUnit() {
            // Arrange
            Entity groundUnit = new BipedMek();
            DeploymentPosition position = new DeploymentPosition(2, 3);

            // Act
            deploymentDisplay.applyDeploymentToEntity(groundUnit, position);

            // Assert
            assertEquals(2, groundUnit.getElevation(),
                  "Ground unit elevation should be set to 2");
            assertEquals(3, groundUnit.getFacing(),
                  "Ground unit facing should be set to 3");
            assertEquals(0, groundUnit.getAltitude(),
                  "Ground unit should not have altitude.");
        }

        @Test
        @DisplayName("should set altitude for aero unit at non-zero elevation")
        void shouldSetAltitude_ForAeroUnitNonZero() {
            // Arrange
            SmallCraft aeroUnit = new SmallCraft();
            DeploymentPosition position = new DeploymentPosition(5, 2);

            // Act
            deploymentDisplay.applyDeploymentToEntity(aeroUnit, position);

            // Assert
            assertEquals(5, aeroUnit.getAltitude(),
                  "Aero unit altitude should be set to 5");
            assertEquals(2, aeroUnit.getFacing(),
                  "Aero unit facing should be set to 2");
            assertNotEquals(EntityMovementMode.WHEELED, aeroUnit.getMovementMode(),
                  "Aero unit should not be wheeled (ground) movement mode.");
        }

        @Test
        @DisplayName("should land aero unit at zero elevation")
        void shouldLandAero_AtZeroElevation() {
            // Arrange
            SmallCraft aeroUnit = new SmallCraft();
            DeploymentPosition position = new DeploymentPosition(0, 4);

            // Act
            deploymentDisplay.applyDeploymentToEntity(aeroUnit, position);

            // Assert
            assertEquals(0, aeroUnit.getAltitude(),
                  "Aero unit altitude should be 0");
            assertEquals(4, aeroUnit.getFacing(),
                  "Aero unit facing should be set to 4");
            assertEquals(EntityMovementMode.WHEELED, aeroUnit.getMovementMode(),
                  "Aero should be landed on ground");
        }

        @Test
        @DisplayName("should handle different facing values correctly")
        void shouldHandleFacings_Correctly() {
            // Arrange
            Entity entity = new BipedMek();

            // Act & Assert
            for (int facing = 0; facing < 6; facing++) {
                DeploymentPosition position = new DeploymentPosition(1, facing);
                deploymentDisplay.applyDeploymentToEntity(entity, position);

                assertEquals(facing, entity.getFacing(),
                      "Entity facing should be set to " + facing);
            }
        }

        @Test
        @DisplayName("should handle negative elevation for underwater deployment")
        void shouldHandleNegativeElevation_ForUnderwater() {
            // Arrange
            Entity entity = new BipedMek();
            DeploymentPosition position = new DeploymentPosition(-2, 0);

            // Act
            deploymentDisplay.applyDeploymentToEntity(entity, position);

            // Assert
            assertEquals(-2, entity.getElevation(),
                  "Entity should have negative elevation for underwater deployment");
        }
    }

    /**
     * Tests for {@link DeploymentPosition} record
     */
    @Nested
    @DisplayName("DeploymentPosition Record Tests")
    class DeploymentPositionTests {

        @Test
        @DisplayName("should create DeploymentPosition with correct values")
        void shouldCreateDeploymentPosition() {
            // Arrange & Act
            DeploymentPosition position = new DeploymentPosition(3, 4);

            // Assert
            assertEquals(3, position.elevation(), "Elevation should be 3");
            assertEquals(4, position.facing(), "Facing should be 4");
        }

        @Test
        @DisplayName("should support equality comparison")
        void shouldSupportEquality() {
            // Arrange
            DeploymentPosition pos1 = new DeploymentPosition(2, 3);
            DeploymentPosition pos2 = new DeploymentPosition(2, 3);
            DeploymentPosition pos3 = new DeploymentPosition(2, 4);

            // Act & Assert
            assertEquals(pos1, pos2, "Same values should be equal");
            assertNotEquals(pos1, pos3, "Different values should not be equal");
        }
    }

    /**
     * Tests for {@link BoardValidationResult} enum
     */
    @Nested
    @DisplayName("BoardValidationResult Enum Tests")
    class BoardValidationResultTests {

        @Test
        @DisplayName("should have all expected enum values")
        void shouldHaveAllEnumValues() {
            // Arrange & Act
            BoardValidationResult[] values = BoardValidationResult.values();

            // Assert
            assertEquals(3, values.length, "Should have exactly 3 validation results");
            assertNotNull(BoardValidationResult.valueOf("VALID"));
            assertNotNull(BoardValidationResult.valueOf("WRONG_BOARD_TYPE"));
            assertNotNull(BoardValidationResult.valueOf("OUTSIDE_DEPLOYMENT_AREA"));
        }
    }

    private Game getGame() {
        return game;
    }
}
