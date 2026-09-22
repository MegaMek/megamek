/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import megamek.client.Client;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.common.board.Board;
import megamek.common.enums.GamePhase;
import megamek.common.game.Game;
import org.junit.jupiter.api.Test;

class BoardViewsContainerTest {
    @Test
    void savedClassicStyleStartsClassicAndTheViewMenuOnlyOffersTheOtherBoard() throws Exception {
        GUIPreferences preferences = GUIPreferences.getInstance();
        boolean style = preferences.getUse3DBoard();
        SwingUtilities.invokeAndWait(() -> {
            CommonMenuBar menu = CommonMenuBar.getMenuBarForGame();
            try {
                menu.setPhase(GamePhase.STARTING_SCENARIO);
                ClientGUI gui = mock(ClientGUI.class);
                Client client = mock(Client.class);
                Game game = new Game();
                game.setPhase(GamePhase.STARTING_SCENARIO);
                when(gui.getClient()).thenReturn(client);
                when(client.getGame()).thenReturn(game);
                when(gui.getMenuBar()).thenReturn(menu);
                JFrame frame = mock(JFrame.class);
                when(gui.getFrame()).thenReturn(frame);
                doCallRealMethod().when(gui).showDefaultBoard(ClientGUI.CG_STARTING_SCENARIO);
                preferences.setUse3DBoard(false);
                gui.showDefaultBoard(ClientGUI.CG_STARTING_SCENARIO);
                verify(frame).setVisible(true);
                verify(gui).setClassicBoardViewEnabled(true);
                assertFalse(preferences.getUse3DBoard());
                JMenu view = java.util.Arrays.stream(menu.getComponents()).filter(JMenu.class::isInstance)
                      .map(JMenu.class::cast).filter(group -> group.getItemCount() > 0 && group.getItem(0) != null
                            && ClientGUI.VIEW_GPU_BOARD.equals(group.getItem(0).getActionCommand()))
                      .findFirst().orElseThrow();
                assertEquals("3D Board", view.getItem(0).getText());
                menu.setBoardView3D(true);
                assertEquals("2D Board", view.getItem(0).getText());
                assertEquals(ClientGUI.VIEW_CLASSIC_BOARD, view.getItem(0).getActionCommand());
                assertEquals(1, java.util.Arrays.stream(view.getMenuComponents())
                      .filter(javax.swing.JMenuItem.class::isInstance).map(javax.swing.JMenuItem.class::cast)
                      .filter(item -> java.util.Set.of(ClientGUI.VIEW_GPU_BOARD, ClientGUI.VIEW_CLASSIC_BOARD)
                            .contains(item.getActionCommand())).count());
            } finally {
                menu.die();
                preferences.setUse3DBoard(style);
            }
        });
    }

    @Test
    void nativeSelectionNeedsNoClassicComponentsAndSurvivesSwitchingViews() throws Exception {
        ClientGUI gui = mock(ClientGUI.class);
        var views = AbstractClientGUI.class.getDeclaredField("boardViews");
        views.setAccessible(true);
        views.set(gui, new HashMap<>());
        Client client = mock(Client.class);
        Game game = new Game();
        game.setBoard(0, new Board(1, 1));
        game.setBoard(1, new Board(1, 1));
        when(gui.getClient()).thenReturn(client);
        when(client.getGame()).thenReturn(game);
        BoardView first = mock(BoardView.class);
        BoardView second = mock(BoardView.class);
        when(first.getComponent()).thenReturn(new JPanel());
        when(second.getComponent()).thenReturn(new JPanel());
        SwingUtilities.invokeAndWait(() -> {
            BoardViewsContainer container = new BoardViewsContainer(gui);
            container.setClassicViewEnabled(false);
            assertTrue(container.getCurrentBoardView().isEmpty());
            gui.boardViews.put(0, first);
            gui.boardViews.put(1, second);
            container.updateMapTabs();
            assertSame(first, container.getCurrentBoardView().orElseThrow());
            container.showBoardView(1);
            container.updateMapTabs();
            assertSame(second, container.getCurrentBoardView().orElseThrow());
            verify(first, never()).getComponent();
            verify(second, never()).getComponent();
            assertEquals(0, ((JPanel) container.getPanel()).getComponentCount());

            container.setClassicViewEnabled(true);
            JTabbedPane tabs = (JTabbedPane) ((JPanel) container.getPanel()).getComponent(0);
            assertSame(second.getComponent(), tabs.getSelectedComponent());
            tabs.setSelectedComponent(first.getComponent());
            assertSame(first, container.getCurrentBoardView().orElseThrow());
            container.setClassicViewEnabled(false);
            assertEquals(0, ((JPanel) container.getPanel()).getComponentCount());
            assertSame(first, container.getCurrentBoardView().orElseThrow());

            gui.boardViews.remove(0);
            container.updateMapTabs();
            assertSame(second, container.getCurrentBoardView().orElseThrow());
            gui.boardViews.clear();
            container.updateMapTabs();
            assertTrue(container.getCurrentBoardView().isEmpty());
        });
    }
}
