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
    private Integer selectedBoardId;
    private boolean classicViewEnabled = true;
    private boolean updatingTabs;

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
        updateSelection();
        updatingTabs = true;
        try {
            boardViewsContainer.removeAll();
            mapTabPane.removeAll();
            shownBoardViews.clear();
            if (classicViewEnabled) {
                if (clientGUI.boardViews.size() > 1) {
                    arrangeMultipleBoardViews();
                    showBoardView(selectedBoardId);
                } else if (clientGUI.boardViews.size() == 1) {
                    arrangeSingleBoardView();
                }
            }
        } finally {
            updatingTabs = false;
        }
        updateBoardViewKeyStatus();
        boardViewsContainer.validate();
        boardViewsContainer.repaint();
    }

    /** The native board needs selection, but never needs to construct the legacy map components. */
    public void setClassicViewEnabled(boolean enabled) {
        if (classicViewEnabled != enabled) {
            classicViewEnabled = enabled;
            updateMapTabs();
        }
    }

    private void arrangeMultipleBoardViews() {
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
     * Returns the selected board independently of the visualization. Classic tabs reflect this selection only when
     * enabled; native startup and navigation do not depend on a Swing component being constructed or painted.
     *
     * @return The currently shown {@link megamek.client.ui.clientGUI.boardview.BoardView}, if any
     */
    public Optional<IBoardView> getCurrentBoardView() {
        updateSelection();
        return Optional.ofNullable(clientGUI.boardViews.get(selectedBoardId));
    }

    public void setName(String name) {
        boardViewsContainer.setName(name);
    }

    public void showBoardView(int boardId) {
        if (!clientGUI.boardViews.containsKey(boardId)) {
            return;
        }
        selectedBoardId = boardId;
        updateBoardViewKeyStatus();
        if (classicViewEnabled && mapTabPane.getTabCount() > 1) {
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
        if (!updatingTabs) {
            Integer boardId = shownBoardViews.get(mapTabPane.getSelectedComponent());
            if (boardId != null) {
                selectedBoardId = boardId;
                updateBoardViewKeyStatus();
            }
        }
    }

    public boolean isClassicViewEnabled() {
        return classicViewEnabled;
    }

    private void updateSelection() {
        if (!clientGUI.boardViews.containsKey(selectedBoardId)) {
            selectedBoardId = clientGUI.boardViews.keySet().stream().min(Integer::compareTo).orElse(null);
        }
    }

    private void updateBoardViewKeyStatus() {
        clientGUI.boardViews.forEach((id, view) -> {
            if (view instanceof BoardView boardView) {
                boardView.setShouldIgnoreKeys(!id.equals(selectedBoardId));
            }
        });
    }
}
