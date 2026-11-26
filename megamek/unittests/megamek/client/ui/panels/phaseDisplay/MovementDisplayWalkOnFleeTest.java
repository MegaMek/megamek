/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.event.ActionEvent;
import java.util.List;
import javax.swing.SwingUtilities;

import megamek.client.Client;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.clientGUI.CommonMenuBar;
import megamek.client.ui.clientGUI.MegaMekGUI;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.dialogs.ConfirmDialog;
import megamek.client.ui.dialogs.unitDisplay.UnitDisplayPanel;
import megamek.client.ui.panels.phaseDisplay.commands.MoveCommand;
import megamek.client.ui.util.MegaMekController;
import megamek.common.Hex;
import megamek.common.Player;
import megamek.common.board.Board;
import megamek.common.board.BoardType;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
import megamek.common.game.Game;
import megamek.common.game.GameTurn;
import megamek.common.units.AeroSpaceFighter;
import megamek.common.units.Targetable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * Issue #9547: a fighter that flew off comes back as a walk-on unit. It has no hex until the player places it on the
 * map edge in the movement phase. Pressing Flee before placing it crashed the movement display, and a move from no hex
 * cannot be sent anyway, so Flee stays greyed out until the unit is placed.
 */
@DisplayName("MovementDisplay Flee for a walk-on unit")
class MovementDisplayWalkOnFleeTest {

    private static final int LOCAL_PLAYER_ID = 0;

    private MockedStatic<MegaMekGUI> mockedMegaMekGUI;
    private AeroSpaceFighter eagle;
    private CommonMenuBar menuBar;
    private MovementDisplay movementDisplay;

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
        // the reporter's space intercept
        board.setBoardType(BoardType.FAR_SPACE);
        Game game = new Game(board);
        Player localPlayer = new Player(LOCAL_PLAYER_ID, "East Kerensky Cluster");
        game.addPlayer(LOCAL_PLAYER_ID, localPlayer);

        eagle = new AeroSpaceFighter();
        // an Eagle EGL-R6's safe thrust; a unit with no thrust cannot flee at all
        eagle.setOriginalWalkMP(5);
        // a fighter out of fuel has every move button greyed out, Flee included
        eagle.setFuel(400);
        eagle.setGame(game);
        eagle.setOwner(localPlayer);
        eagle.setBoardId(game.getBoard().getBoardId());
        eagle.setDeployed(false);
        game.addEntity(eagle);
        eagle.setDone(false);

        game.setPhase(GamePhase.MOVEMENT);
        game.setTurnVector(List.of(new GameTurn(LOCAL_PLAYER_ID)));

        Client mockClient = mock(Client.class);
        when(mockClient.getGame()).thenReturn(game);
        when(mockClient.getLocalPlayer()).thenReturn(localPlayer);
        when(mockClient.getLocalPlayerNumber()).thenReturn(LOCAL_PLAYER_ID);
        when(mockClient.isMyTurn()).thenReturn(true);
        when(mockClient.getFirstEntityNum()).thenReturn(eagle.getId());

        ClientGUI mockClientGUI = mock(ClientGUI.class);
        mockClientGUI.controller = mockController;
        BoardView boardView = mock(BoardView.class);
        menuBar = mock(CommonMenuBar.class);
        when(mockClientGUI.getClient()).thenReturn(mockClient);
        when(mockClientGUI.boardViews()).thenReturn(List.of());
        when(mockClientGUI.getMenuBar()).thenReturn(menuBar);
        when(mockClientGUI.getUnitDisplay()).thenReturn(mock(UnitDisplayPanel.class));
        when(mockClientGUI.getBoardView((Targetable) any())).thenReturn(boardView);
        when(mockClientGUI.getBoardView(anyInt())).thenReturn(boardView);
        // the player answers Yes to "Do you want to flee?" and to any check made before the move is sent
        when(mockClientGUI.doYesNoDialog(anyString(), anyString())).thenReturn(true);
        ConfirmDialog confirmYes = mock(ConfirmDialog.class);
        when(confirmYes.getAnswer()).thenReturn(true);
        when(confirmYes.getShowAgain()).thenReturn(true);
        when(mockClientGUI.doYesNoBotherDialog(anyString(), anyString())).thenReturn(confirmYes);

        movementDisplay = new MovementDisplay(mockClientGUI);
    }

    @AfterEach
    void tearDown() {
        mockedMegaMekGUI.close();
    }

    @Test
    @DisplayName("#9547: Flee is greyed out while the walk-on unit has not been placed")
    void fleeDisabledBeforePlacing() throws Exception {
        movementDisplay.selectEntity(eagle.getId());

        assertNull(eagle.getPosition(), "A walk-on unit has no hex until it is placed");
        assertFalse(lastFleeEnabledState(), "Flee should be greyed out until the unit is placed");
    }

    @Test
    @DisplayName("control: Flee is offered to the same fighter once it is on the map edge")
    void fleeEnabledOnceOnTheEdge() throws Exception {
        eagle.setDeployed(true);
        eagle.setPosition(new Coords(0, 0));

        movementDisplay.selectEntity(eagle.getId());

        assertTrue(lastFleeEnabledState(), "A fighter on the map edge can flee");
    }

    @Test
    @DisplayName("#9547: a Flee command before placing does not crash the movement display")
    void fleeCommandBeforePlacingDoesNotThrow() {
        movementDisplay.selectEntity(eagle.getId());

        assertDoesNotThrow(() -> movementDisplay.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED,
              MoveCommand.MOVE_FLEE.getCmd())));
    }

    /**
     * @return what the display last set the Flee menu item to; the button is set alongside it. Selecting a unit also
     *       queues a button refresh on the event thread, which is let finish first.
     */
    private boolean lastFleeEnabledState() throws Exception {
        SwingUtilities.invokeAndWait(() -> {});
        ArgumentCaptor<Boolean> enabled = ArgumentCaptor.forClass(Boolean.class);
        verify(menuBar, atLeastOnce()).setEnabled(eq(MoveCommand.MOVE_FLEE.getCmd()), enabled.capture());
        return enabled.getValue();
    }
}
