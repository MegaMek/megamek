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
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.clientGUI.CommonMenuBar;
import megamek.client.ui.clientGUI.MegaMekGUI;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.util.MegaMekController;
import megamek.common.Hex;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.game.Game;
import megamek.common.enums.MoveStepType;
import megamek.common.moves.MovePath;
import megamek.common.units.BipedMek;
import megamek.common.units.Targetable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@DisplayName("MovementDisplay Unit Tests")
class MovementDisplayTest {

    private MovementDisplay movementDisplay;
    private ClientGUI mockClientGUI;
    private Client mockClient;
    private MockedStatic<MegaMekGUI> mockedMegaMekGUI;
    private Game game;

    @BeforeEach
    void beforeEach() {
        MegaMekController mockController = mock(MegaMekController.class);
        mockedMegaMekGUI = mockStatic(MegaMekGUI.class);
        mockedMegaMekGUI.when(MegaMekGUI::getKeyDispatcher).thenReturn(mockController);

        Board board = new Board(4, 4);
        for (int x = 0; x < board.getWidth(); x++) {
            for (int y = 0; y < board.getHeight(); y++) {
                board.setHex(x, y, new Hex());
            }
        }

        mockClientGUI = mock(ClientGUI.class);
        mockClient = mock(Client.class);
        game = new Game(board);
        BoardView boardView = mock(BoardView.class);
        CommonMenuBar menuBar = mock(CommonMenuBar.class);
        mockClientGUI.controller = mockController;

        when(mockClientGUI.getClient()).thenReturn(mockClient);
        when(mockClient.getGame()).thenReturn(game);
        when(mockClientGUI.boardViews()).thenReturn(List.of());
        when(mockClientGUI.getMenuBar()).thenReturn(menuBar);
        when(mockClientGUI.getBoardView((Targetable) any())).thenReturn(boardView);
        when(mockClientGUI.getBoardView(anyInt())).thenReturn(boardView);

        movementDisplay = new MovementDisplay(mockClientGUI);
    }

    @AfterEach
    void tearDown() {
        if (mockedMegaMekGUI != null) {
            mockedMegaMekGUI.close();
        }
    }

    @Test
    @DisplayName("clear resets a walk-on movement path without losing deployment")
    void clearWithWalkPathKeepsDeploymentAnchored() throws Exception {
        BipedMek mek = new BipedMek();
        mek.setGame(game);
        mek.setPosition(new Coords(0, 0));
        mek.setBoardId(game.getBoard().getBoardId());
        game.addEntity(mek);
        movementDisplay.currentEntity = mek.getId();

        MovePath walkPath = new MovePath(game, mek);
        walkPath.addStep(MoveStepType.DEPLOY);
        setField(movementDisplay, "cmd", walkPath);
        setField(movementDisplay, "gear", MovementDisplay.GEAR_LAND);

        movementDisplay.clear();

        assertTrue(mek.isDeployed(), "Walk-on clear should keep the deployment anchor intact.");
        assertEquals(MovementDisplay.GEAR_LAND, readField(movementDisplay, "gear"),
                "clear should reset gear to walk after a walk-on move path.");

        MovePath refreshed = (MovePath) readField(movementDisplay, "cmd");
        assertNotNull(refreshed, "clear should rebuild the move path.");
        assertTrue(refreshed.getStepVector().stream().anyMatch(step -> step.getType() == MoveStepType.DEPLOY),
                "clear should preserve the deployment step on a walk path.");
    }

    @Test
    @DisplayName("clear resets jump movement paths to the default gear while keeping deployment anchor")
    void clearWithJumpPathResetsToLandGear() throws Exception {
        BipedMek mek = new BipedMek();
        mek.setGame(game);
        mek.setPosition(new Coords(0, 0));
        mek.setBoardId(game.getBoard().getBoardId());
        game.addEntity(mek);
        movementDisplay.currentEntity = mek.getId();

        MovePath jumpPath = new MovePath(game, mek);
        jumpPath.addStep(MoveStepType.DEPLOY);
        setField(movementDisplay, "cmd", jumpPath);
        setField(movementDisplay, "gear", MovementDisplay.GEAR_JUMP);

        movementDisplay.clear();

        assertTrue(mek.isDeployed(), "clear should keep the deployed position for a jump path.");
        assertEquals(MovementDisplay.GEAR_LAND, readField(movementDisplay, "gear"),
                "clear should reset to the default land gear after reinitializing a jump path.");

        MovePath refreshed = (MovePath) readField(movementDisplay, "cmd");
        assertNotNull(refreshed, "clear should rebuild the jump path.");
        assertTrue(refreshed.getStepVector().stream().anyMatch(step -> step.getType() == MoveStepType.DEPLOY),
                "clear should retain the deployment step before a jump move is reinitialized.");
    }

    @Test
    @DisplayName("movement path rebuild keeps a deploy step for walk-on deployments")
    void clearRebuildsWalkOnMovementPathWithDeployStep() throws Exception {
        BipedMek mek = new BipedMek();
        mek.setGame(game);
        mek.setPosition(new Coords(0, 0));
        mek.setBoardId(game.getBoard().getBoardId());
        game.addEntity(mek);
        movementDisplay.currentEntity = mek.getId();

        MovePath walkPath = new MovePath(game, mek);
        walkPath.addStep(MoveStepType.DEPLOY);
        setField(movementDisplay, "cmd", walkPath);
        setField(movementDisplay, "gear", MovementDisplay.GEAR_LAND);

        movementDisplay.clear();

        MovePath refreshed = (MovePath) readField(movementDisplay, "cmd");
        assertNotNull(refreshed, "clear should rebuild the walk-on movement path.");
        assertTrue(refreshed.getStepVector().stream().anyMatch(step -> step.getType() == MoveStepType.DEPLOY),
                "clear should preserve the deploy step when rebuilding a walk-on movement path.");
        assertTrue(mek.isDeployed(), "clear should keep the deployment anchor active for walk-on movement.");
    }

    @Test
    @DisplayName("deployment position keeps the current facing when all facings remain valid")
    void determineDeploymentPositionPreservesCurrentFacingWhenFacingsAreValid() {
        Board board = new Board(7, 7);
        for (int x = 0; x < board.getWidth(); x++) {
            for (int y = 0; y < board.getHeight(); y++) {
                board.setHex(x, y, new Hex());
            }
        }

        BipedMek unit = new BipedMek();
        unit.setGame(game);
        unit.setStartingPos(Board.START_EDGE);
        unit.setFacing(3);
        Coords deploymentCoords = new Coords(0, 0);

        var result = new DeploymentHelper(mockClientGUI)
              .determineDeploymentPosition(unit, deploymentCoords, board, new HashSet<>(), null);

        assertNotNull(result, "A valid deployment should return a facing.");
        assertEquals(unit.getFacing(), result.facing(),
                "Deployment helpers keep the unit's current facing when no facing restriction is applied.");
    }

    @Test
    @DisplayName("center deployment keeps the current facing when all facings remain valid")
    void determineDeploymentPositionKeepsCurrentFacingForCenterDeployment() {
        Board board = new Board(7, 7);
        for (int x = 0; x < board.getWidth(); x++) {
            for (int y = 0; y < board.getHeight(); y++) {
                board.setHex(x, y, new Hex());
            }
        }

        BipedMek unit = new BipedMek();
        unit.setGame(game);
        unit.setStartingPos(Board.START_CENTER);
        unit.setFacing(5);
        Coords deploymentCoords = new Coords(0, 0);

        var result = new DeploymentHelper(mockClientGUI)
              .determineDeploymentPosition(unit, deploymentCoords, board, new HashSet<>(), null);

        assertNotNull(result, "A valid center deployment should return a facing.");
        assertEquals(unit.getFacing(), result.facing(),
                "Center deployments do not force a facing override when all facings remain valid.");
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = MovementDisplay.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Object readField(Object target, String fieldName) throws Exception {
        Field field = MovementDisplay.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }
}
