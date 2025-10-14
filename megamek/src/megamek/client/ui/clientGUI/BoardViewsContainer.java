/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.clientGUI;

import java.awt.Component;
import java.awt.GridLayout;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;

import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.clientGUI.boardview.IBoardView;
import megamek.common.board.Board;
import megamek.common.game.IGame;

/**
 * The BoardViewsContainer manages the JPanel that contains the BoardView(s) of a ClientGUI. When only one BoardView is
 * present, it is shown by itself. When multiple BoardViews are present, they are arranged as tabs of a TabbedPane. The
 * panel that holds the BoardView(s) is obtained from {@link #getPanel()}.
 * <p>
 * The display contents are not automatically updated. Use {@link #updateMapTabs()} after construction and later to make
 * it reflect the current set of BoardViews.
 */
public class BoardViewsContainer {

    /** The panel that displays the BoardView(s) */
    private final JPanel boardViewsContainer = new JPanel(new GridLayout(1, 1));

    /** The tabbed pane is used when there are multiple boards to display */
    private final JTabbedPane mapTabPane = new JTabbedPane();

    /**
     * The {@link megamek.client.ui.clientGUI.boardview.BoardView} components of the game with the board ID as the map
     * value. Used to retrieve the active {@link megamek.client.ui.clientGUI.boardview.BoardView}
     */
    protected final Map<Component, Integer> shownBoardViews = new HashMap<>();

    private final AbstractClientGUI clientGUI;

    /**
     * Returns a new BoardViewsContainer. Call {@link #updateMapTabs()} after construction to make it reflect the
     * current BoardViews. Requires a non-null AbstractClientGUI as parent.
     *
     * @param clientGUI The AbstractClientGUI parent
     */
    public BoardViewsContainer(AbstractClientGUI clientGUI) {
        this.clientGUI = Objects.requireNonNull(clientGUI);
        mapTabPane.addChangeListener(this::updateBoardViewKeyStatus);
    }

    /**
     * Returns the JPanel that holds the BoardView(s), either one BoardView by itself or multiple BoardViews in a tabbed
     * pane. Add this panel to the view area of the ClientGUI.
     *
     * @return The panel holding all present BoardViews
     */
    public Component getPanel() {
        return boardViewsContainer;
    }

    /**
     * Updates the BoardViewsContainer to reflect the current state of ClientGUI's BoardViews.
     */
    public void updateMapTabs() {
        boardViewsContainer.removeAll();
        shownBoardViews.clear();
        if (clientGUI.boardViews.size() > 1) {
            arrangeMultipleBoardViews();
        } else if (clientGUI.boardViews.size() == 1) {
            arrangeSingleBoardView();
        }
        boardViewsContainer.validate();
    }

    private void arrangeMultipleBoardViews() {
        mapTabPane.removeAll();
        for (int boardId : clientGUI.boardViews.keySet()) {
            Component boardComponent = boardView(boardId).getComponent();
            boardComponent.setName(String.valueOf(boardId));
            mapTabPane.add(board(boardId).getBoardName(), boardComponent);
            mapTabPane.setToolTipTextAt(mapTabPane.getTabCount() - 1, getBoardViewTabTooltip(boardId));
            shownBoardViews.put(boardComponent, boardId);
        }
        boardViewsContainer.add(mapTabPane);
    }

    private void arrangeSingleBoardView() {
        // The single BoardView does not use the tabbed pane
        int boardId = clientGUI.boardViews.keySet().iterator().next();
        Component boardComponent = boardView(boardId).getComponent();
        boardViewsContainer.add(board(boardId).getBoardName(), boardComponent);
        shownBoardViews.put(boardComponent, boardId);
    }

    private String getBoardViewTabTooltip(int boardId) {
        IGame game = clientGUI.getClient().getGame();
        String tooltip = String.format("<HTML>%s (Board #%d)", game.getBoard(boardId).getBoardName(), boardId);
        Optional<Board> enclosingBoard = game.getEnclosingBoard(boardId);
        if (enclosingBoard.isPresent()) {
            tooltip += "<BR>Located at %s in %s".formatted(enclosingBoard.get()
                  .embeddedBoardPosition(boardId)
                  .getBoardNum(), enclosingBoard.get().getBoardName());
        }
        return tooltip;
    }

    /**
     * Returns the currently shown {@link megamek.client.ui.clientGUI.boardview.BoardView}. If there is only a single
     * {@link megamek.client.ui.clientGUI.boardview.BoardView} (no tabbed pane), this will be returned. With multiple
     * {@link megamek.client.ui.clientGUI.boardview.BoardView}'s, the one in the currently selected tab is returned.
     * <p>
     * Unfortunately it is possible to have no selected tab in a JTabbedPane; also, theoretically, there could be no
     * {@link megamek.client.ui.clientGUI.boardview.BoardView}. Therefore, the result is returned as an Optional.
     *
     * @return The currently shown {@link megamek.client.ui.clientGUI.boardview.BoardView}, if any
     */
    public Optional<IBoardView> getCurrentBoardView() {
        if ((clientGUI.boardViews.size() > 1)) {
            Component shownComponent = mapTabPane.getSelectedComponent();
            // The components that the tabbed pane shows are JScrollPanes that wrap the board views
            if ((shownComponent != null) && shownBoardViews.containsKey(shownComponent)) {
                int boardId = shownBoardViews.get(shownComponent);
                return Optional.of(boardView(boardId));
            } else {
                return Optional.empty();
            }
        } else if (clientGUI.boardViews.size() == 1) {
            return Optional.of(boardView(clientGUI.boardViews.keySet().iterator().next()));
        } else {
            return Optional.empty();
        }
    }

    public void setName(String name) {
        boardViewsContainer.setName(name);
    }

    public void showBoardView(int boardId) {
        if (mapTabPane.getTabCount() > 1) {
            String componentName = String.valueOf(boardId);
            for (int i = 0; i < mapTabPane.getTabCount(); i++) {
                if (componentName.equals(mapTabPane.getComponentAt(i).getName())) {
                    mapTabPane.setSelectedIndex(i);
                    return;
                }
            }
        }
    }

    private Board board(int id) {
        return clientGUI.getClient().getGame().getBoard(id);
    }

    private IBoardView boardView(int id) {
        return clientGUI.boardViews.get(id);
    }

    /**
     * Sets the {@link megamek.client.ui.clientGUI.boardview.BoardView}'s that are not shown to ignore key presses from
     * the MegamekController (Key Dispatcher) and the currently shown
     * {@link megamek.client.ui.clientGUI.boardview.BoardView} to accept them.
     *
     * @param changeEvent The changeEvent (not used)
     */
    private void updateBoardViewKeyStatus(ChangeEvent changeEvent) {
        if (clientGUI.boardViews.size() > 1) {
            // Set all board views to ignore key presses
            for (IBoardView boardView : clientGUI.boardViews()) {
                if (boardView instanceof BoardView bv) {
                    bv.setShouldIgnoreKeys(true);
                }
            }
            // set the currently visible boardview to process key presses
            Optional<IBoardView> ibv = getCurrentBoardView();
            if (ibv.isPresent() && (ibv.get() instanceof BoardView bv)) {
                bv.setShouldIgnoreKeys(false);
            }
        }
    }
}
