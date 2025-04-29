/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.swing.phaseDisplay.dialog;

import megamek.client.ui.Messages;
import megamek.client.ui.swing.ClientGUI;
import megamek.client.ui.swing.boardview.BoardView;
import megamek.common.Board;

import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.event.HyperlinkEvent;
import java.util.Objects;

/**
 * A dialog telling the player to select a ground board flight path for an aero on an atmospheric board.
 */
public class FlightPathNotice {

    private static final int WIDTH = 500;

    private final JEditorPane contentPane = new JEditorPane();
    private final ClientGUI clientGui;

    /**
     * Creates a dialog telling the player to select a ground board flight path for an aero on an atmospheric board.
     *
     * @param flightPathBoard The ground Board on which the flight path must be planned
     * @see #show()
     */
    public FlightPathNotice(ClientGUI clientGui, Board flightPathBoard) {
        this.clientGui = Objects.requireNonNull(clientGui);
        contentPane.setContentType("text/html");
        contentPane.setEditable(false);
        contentPane.setText(Messages.getString("MovementDisplay.flightPathNotice")
              .formatted(WIDTH, flightPathBoard.getBoardName()));
        contentPane.addHyperlinkListener(e -> {
            if (e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED)) {
                clientGui.showBoardView(flightPathBoard.getBoardId());
            }
        });
    }

    /**
     * Sets this flight path notice dialog visible. Will also suspend tooltips on the boardviews while the dialog is
     * shown.
     */
    public void show() {
        clientGui.suspendBoardTooltips();
        JOptionPane.showMessageDialog(clientGui.getFrame(), contentPane);
        clientGui.activateBoardTooltips();
    }
}
