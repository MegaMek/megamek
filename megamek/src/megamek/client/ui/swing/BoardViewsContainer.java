/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client.ui.swing;

import megamek.client.ui.swing.boardview.IBoardView;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.Board;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

/**
 * The BoardViewsContainer manages the JPanel that contains the BoardView(s) of a ClientGUI. When only
 * one BoardView is present, it is shown by itself. When multiple BoardViews are present,
 * they are arranged as tabs of a TabbedPane. The panel that holds the BoardView(s) is obtained from
 * {@link #getPanel()}.
 * <P>The display contents are not automatically updated. Use {@link #updateMapTabs()} after construction
 * and later to make it reflect the current set of BoardViews.
 */
public class BoardViewsContainer {

    /** The panel that displays the BoardView(s) */
    private final JPanel boardViewsContainer = new JPanel(new GridLayout(1, 1));

    /** The tabbed pane is used when there are multiple boards to display */
    private final JTabbedPane mapTabPane = new JTabbedPane();

    private final AbstractClientGUI clientGUI;

    /**
     * Returns a new BoardViewsContainer. Call {@link #updateMapTabs()} after construction to make it
     * reflect the current BoardViews. Requires a non-null AbstractClientGUI as parent.
     *
     * @param clientGUI The AbstractClientGUI parent
     */
    public BoardViewsContainer(AbstractClientGUI clientGUI) {
        this.clientGUI = Objects.requireNonNull(clientGUI);
    }

    /**
     * Returns the JPanel that holds the BoardView(s), either one BoardView by itself or multiple
     * BoardViews in a tabbed pane. Add this panel to the view area of the ClientGUI.
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
            mapTabPane.add(board(boardId).getMapName(), boardView(boardId).getComponent());
            mapTabPane.setToolTipTextAt(mapTabPane.getTabCount() - 1, getBoardViewTabTooltip(boardId));
        }
        boardViewsContainer.add(mapTabPane);
    }

    private void arrangeSingleBoardView() {
        // The single BoardView does not use the tabbed pane
        int boardId = clientGUI.boardViews.keySet().iterator().next();
        boardViewsContainer.add(board(boardId).getMapName(), boardView(boardId).getComponent());
    }

    private String getBoardViewTabTooltip(int boardId) {
        //TODO Add embedded and enclosing boards
//        Game game = getIClient().getGame();
//        if (game.hasEnclosingBoard(boardId)) {
//            Board enclosingBoard = game.getEnclosingBoard(boardView.getBoard());
//            tooltip += "<BR>Located at " + enclosingBoard.embeddedBoardPosition(boardId).getBoardNum() +
//                    " in " + enclosingBoard;
//        }
        return "<HTML><BODY>" + UIUtil.guiScaledFontHTML() + board(boardId).getMapName() + "</HTML>";
    }

    private Board board(int id) {
        return clientGUI.getIClient().getIGame().getBoard(id);
    }

    private IBoardView boardView(int id) {
        return clientGUI.boardViews.get(id);
    }

    public void setName(String name) {
        boardViewsContainer.setName(name);
    }
}
