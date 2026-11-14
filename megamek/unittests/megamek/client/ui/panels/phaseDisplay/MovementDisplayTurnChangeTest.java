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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.List;
import javax.swing.JLabel;

import megamek.client.Client;
import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.clientGUI.CommonMenuBar;
import megamek.client.ui.clientGUI.MegaMekGUI;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.dialogs.unitDisplay.UnitDisplayPanel;
import megamek.client.ui.util.MegaMekController;
import megamek.common.Hex;
import megamek.common.Player;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
import megamek.common.event.GameTurnChangeEvent;
import megamek.common.game.Game;
import megamek.common.game.GameTurn;
import megamek.common.units.BipedMek;
import megamek.common.units.Targetable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * Drives the movement display through the turn handover from issue #9503: a bot (ESCORT) ends its movement turn and
 * the next turn is the local player's. The reporter's status bar kept "It's ESCORT's turn to move" while the server
 * waited on them.
 */
@DisplayName("MovementDisplay turn handover")
class MovementDisplayTurnChangeTest {

    private static final int LOCAL_PLAYER_ID = 0;
    private static final int CONVOY_PLAYER_ID = 1;
    private static final int ESCORT_PLAYER_ID = 2;

    private MockedStatic<MegaMekGUI> mockedMegaMekGUI;
    private Game game;
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
        game = new Game(board);
        Player localPlayer = new Player(LOCAL_PLAYER_ID, "The Starfire Raptors");
        game.addPlayer(LOCAL_PLAYER_ID, localPlayer);
        game.addPlayer(CONVOY_PLAYER_ID, new Player(CONVOY_PLAYER_ID, "CONVOY"));
        game.addPlayer(ESCORT_PLAYER_ID, new Player(ESCORT_PLAYER_ID, "ESCORT"));

        BipedMek stalker = new BipedMek();
        stalker.setGame(game);
        stalker.setOwner(localPlayer);
        stalker.setBoardId(game.getBoard().getBoardId());
        stalker.setDeployed(true);
        game.addEntity(stalker);
        // after addEntity, which assigns the id the position lookup is keyed on
        stalker.setPosition(new Coords(1, 1));
        stalker.setDone(false);

        game.setPhase(GamePhase.MOVEMENT);
        game.setTurnVector(List.of(new GameTurn(CONVOY_PLAYER_ID), new GameTurn(ESCORT_PLAYER_ID),
              new GameTurn(LOCAL_PLAYER_ID), new GameTurn(CONVOY_PLAYER_ID)));

        Client mockClient = mock(Client.class);
        when(mockClient.getGame()).thenReturn(game);
        when(mockClient.getLocalPlayer()).thenReturn(localPlayer);
        when(mockClient.getLocalPlayerNumber()).thenReturn(LOCAL_PLAYER_ID);
        when(mockClient.isMyTurn()).thenAnswer(invocation ->
              (game.getTurn() != null) && (game.getTurn().playerId() == LOCAL_PLAYER_ID));
        when(mockClient.getFirstEntityNum()).thenReturn(stalker.getId());

        ClientGUI mockClientGUI = mock(ClientGUI.class);
        mockClientGUI.controller = mockController;
        BoardView boardView = mock(BoardView.class);
        when(mockClientGUI.getClient()).thenReturn(mockClient);
        when(mockClientGUI.boardViews()).thenReturn(List.of());
        when(mockClientGUI.getMenuBar()).thenReturn(mock(CommonMenuBar.class));
        when(mockClientGUI.getUnitDisplay()).thenReturn(mock(UnitDisplayPanel.class));
        when(mockClientGUI.getBoardView((Targetable) any())).thenReturn(boardView);
        when(mockClientGUI.getBoardView(anyInt())).thenReturn(boardView);

        movementDisplay = new MovementDisplay(mockClientGUI);
    }

    @AfterEach
    void tearDown() {
        mockedMegaMekGUI.close();
    }

    @Test
    @DisplayName("control: with nothing muting the display, ESCORT handing over the turn shows it as yours")
    void handoverWithDisplayListeningShowsYourTurn() throws Exception {
        game.setTurnIndex(1, CONVOY_PLAYER_ID);
        assertTrue(statusText().startsWith(othersTurnText("ESCORT")), "Turn 1 should name ESCORT: " + statusText());

        game.setTurnIndex(2, ESCORT_PLAYER_ID);

        assertTrue(statusText().startsWith(yourTurnText()), "Turn 2 is ours: " + statusText());
    }

    @Test
    @DisplayName("#9503: a turn handed over while a hex picker mutes the display still shows as yours afterwards")
    void handoverWhileDisplayMutedShowsYourTurnOnceUnmuted() throws Exception {
        game.setTurnIndex(1, CONVOY_PLAYER_ID);
        assertTrue(statusText().startsWith(othersTurnText("ESCORT")), "Turn 1 should name ESCORT: " + statusText());

        // what BoardRouteEditor.startPicking and HexTargetPicker do while the player picks hexes for a bot order
        movementDisplay.setIgnoringEvents(true);
        game.setTurnIndex(2, ESCORT_PLAYER_ID);
        movementDisplay.setIgnoringEvents(false);

        assertEquals(LOCAL_PLAYER_ID, game.getTurn().playerId(), "The server has handed the turn to us");
        assertTrue(statusText().startsWith(yourTurnText()),
              "Our turn arrived while the display was muted; the status bar still says: " + statusText());
    }

    @Test
    @DisplayName("muting and unmuting with no turn change in between replays nothing")
    void unmuteWithoutTurnChangeReplaysNothing() {
        game.setTurnIndex(1, CONVOY_PLAYER_ID);
        MovementDisplay display = spy(movementDisplay);

        display.setIgnoringEvents(true);
        display.setIgnoringEvents(false);

        verify(display, never()).gameTurnChange(any());
    }

    @Test
    @DisplayName("a display unmuted in a later round replays nothing; the phase change starts it afresh")
    void unmuteInLaterRoundReplaysNothing() {
        game.setTurnIndex(1, CONVOY_PLAYER_ID);
        MovementDisplay display = spy(movementDisplay);

        display.setIgnoringEvents(true);
        game.incrementCurrentRound();
        game.setTurnIndex(2, ESCORT_PLAYER_ID);
        display.setIgnoringEvents(false);

        verify(display, never()).gameTurnChange(any());
    }

    @Test
    @DisplayName("a turn dropped while muted is replayed once, for the current turn")
    void unmuteAfterTurnChangeReplaysCurrentTurnOnce() {
        game.setTurnIndex(1, CONVOY_PLAYER_ID);
        MovementDisplay display = spy(movementDisplay);

        display.setIgnoringEvents(true);
        game.setTurnIndex(2, ESCORT_PLAYER_ID);
        display.setIgnoringEvents(false);

        ArgumentCaptor<GameTurnChangeEvent> replayed = ArgumentCaptor.forClass(GameTurnChangeEvent.class);
        verify(display, times(1)).gameTurnChange(replayed.capture());
        assertEquals(LOCAL_PLAYER_ID, replayed.getValue().getPlayer().getId(), "The replay is for our turn");
        assertEquals(ESCORT_PLAYER_ID, replayed.getValue().getPreviousPlayerId(),
              "The replay names ESCORT, whose turn came before ours, as the previous player");
    }

    private String statusText() throws Exception {
        Field field = StatusBarPhaseDisplay.class.getDeclaredField("labStatus");
        field.setAccessible(true);
        return ((JLabel) field.get(movementDisplay)).getText();
    }

    private static String yourTurnText() {
        return Messages.getString("MovementDisplay.its_your_turn");
    }

    private static String othersTurnText(String playerName) {
        return Messages.getString("MovementDisplay.its_others_turn", playerName);
    }
}
