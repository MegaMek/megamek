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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.util.List;

import megamek.client.Client;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.clientGUI.CommonMenuBar;
import megamek.client.ui.clientGUI.MegaMekGUI;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.dialogs.ConfirmDialog;
import megamek.client.ui.dialogs.unitDisplay.UnitDisplayPanel;
import megamek.client.ui.util.MegaMekController;
import megamek.common.Hex;
import megamek.common.Player;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.game.GameTurn;
import megamek.common.options.OptionsConstants;
import megamek.common.turns.CounterGrappleTurn;
import megamek.common.units.BipedMek;
import megamek.common.units.Targetable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.MockedStatic;

/**
 * Regression tests for issue #9204: in a simultaneous physical phase, a grappler that breaks the grapple gives the
 * grappled unit a counter grapple turn. The grappled player's display must open that turn even though the turn change
 * came from the grappler's player, or the phase waits on it forever.
 */
@DisplayName("PhysicalDisplay counter grapple in simultaneous physical")
class PhysicalDisplayCounterGrappleTest {

    private static final int GRAPPLED_PLAYER_ID = 0;
    private static final int GRAPPLER_PLAYER_ID = 1;

    private MockedStatic<MegaMekGUI> mockedMegaMekGUI;
    private ClientGUI mockClientGUI;
    private Game game;
    private BipedMek grappledMek;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

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
        game.getOptions().getOption(OptionsConstants.INIT_SIMULTANEOUS_PHYSICAL).setValue(true);
        game.addPlayer(GRAPPLED_PLAYER_ID, new Player(GRAPPLED_PLAYER_ID, "Grappled"));
        game.addPlayer(GRAPPLER_PLAYER_ID, new Player(GRAPPLER_PLAYER_ID, "Grappler"));

        grappledMek = createMek(GRAPPLED_PLAYER_ID, new Coords(1, 1));
        BipedMek grapplerMek = createMek(GRAPPLER_PLAYER_ID, new Coords(1, 2));
        grappledMek.setGrappled(grapplerMek.getId(), false);
        grapplerMek.setGrappled(grappledMek.getId(), true);
        game.setPhase(GamePhase.PHYSICAL);

        Client mockClient = mock(Client.class);
        when(mockClient.getGame()).thenReturn(game);
        when(mockClient.getLocalPlayerNumber()).thenReturn(GRAPPLED_PLAYER_ID);
        when(mockClient.getMyTurn()).thenAnswer(invocation -> game.getTurnForPlayer(GRAPPLED_PLAYER_ID));
        when(mockClient.isMyTurn()).thenAnswer(invocation -> game.getTurnForPlayer(GRAPPLED_PLAYER_ID) != null);

        mockClientGUI = mock(ClientGUI.class);
        mockClientGUI.controller = mockController;
        BoardView boardView = mock(BoardView.class);
        when(mockClientGUI.getClient()).thenReturn(mockClient);
        when(mockClientGUI.boardViews()).thenReturn(List.of());
        when(mockClientGUI.getMenuBar()).thenReturn(mock(CommonMenuBar.class));
        when(mockClientGUI.getBoardView((Targetable) any())).thenReturn(boardView);
        when(mockClientGUI.getBoardView(anyInt())).thenReturn(boardView);
        when(mockClientGUI.getUnitDisplay()).thenReturn(mock(UnitDisplayPanel.class,
              withSettings().defaultAnswer(Answers.RETURNS_DEEP_STUBS)));
        // The player declines the counter grapple, then confirms ending the turn with no attack.
        when(mockClientGUI.doYesNoDialog(anyString(), anyString())).thenReturn(false);
        ConfirmDialog noAttackConfirm = mock(ConfirmDialog.class);
        when(noAttackConfirm.getAnswer()).thenReturn(true);
        when(noAttackConfirm.getShowAgain()).thenReturn(true);
        when(mockClientGUI.doYesNoBotherDialog(anyString(), anyString())).thenReturn(noAttackConfirm);

        new PhysicalDisplay(mockClientGUI);
    }

    @AfterEach
    void tearDown() {
        if (mockedMegaMekGUI != null) {
            mockedMegaMekGUI.close();
        }
    }

    private BipedMek createMek(int ownerId, Coords position) {
        BipedMek mek = new BipedMek();
        mek.setGame(game);
        mek.setOwner(game.getPlayer(ownerId));
        mek.setPosition(position);
        mek.setBoardId(game.getBoard().getBoardId());
        mek.setDeployed(true);
        game.addEntity(mek);
        return mek;
    }

    /**
     * Puts the turn list in the state the server leaves it after both players declared a break: both regular turns
     * used, and a counter grapple turn for the grappled unit as the current turn. The turn change comes from the
     * grappler's player, exactly as the server sends it.
     */
    private void serverHandsOutCounterGrappleTurn() {
        grappledMek.setDone(true);
        game.setTurnVector(List.of(new GameTurn(GRAPPLED_PLAYER_ID),
              new GameTurn(GRAPPLER_PLAYER_ID),
              new CounterGrappleTurn(GRAPPLED_PLAYER_ID, grappledMek.getId())));
        game.setTurnIndex(2, GRAPPLER_PLAYER_ID);
    }

    @Test
    @DisplayName("A counter grapple turn from the grappler's turn change opens the counter grapple dialog")
    void counterGrappleTurnFromOtherPlayerOpens() {
        serverHandsOutCounterGrappleTurn();

        verify(mockClientGUI, times(1)).doYesNoDialog(anyString(), anyString());
        verify(mockClientGUI.getClient(), times(1)).sendAttackData(anyInt(), any());
    }

    @Test
    @DisplayName("A later turn change from another player does not open the same counter grapple a second time")
    void counterGrappleTurnOpensOnlyOnce() {
        serverHandsOutCounterGrappleTurn();
        // Another player's turn ends while the server has not yet processed the counter grapple reply.
        game.setTurnIndex(2, GRAPPLER_PLAYER_ID);

        verify(mockClientGUI, times(1)).doYesNoDialog(anyString(), anyString());
    }
}
