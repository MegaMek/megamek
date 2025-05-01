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
import megamek.common.Board;
import megamek.common.preference.ClientPreferences;
import megamek.common.preference.PreferenceManager;

import javax.swing.JCheckBox;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import java.awt.BorderLayout;
import java.util.Objects;

/**
 * A dialog telling the player to select a ground board flight path for an aero on an atmospheric board.
 */
public class FlightPathNotice {

    private static final int WIDTH = 500;
    private static final String SHOW_FLIGHT_PATH_NOTICE = "ShowFlightPathNotice";
    private static final ClientPreferences preferences = PreferenceManager.getClientPreferences();

    private final JPanel contentPanel = new JPanel();
    private final JCheckBox dontShowAgain = new JCheckBox("Don't show again");
    private final ClientGUI clientGui;
    private final Board flightPathBoard;

    /**
     * Creates a dialog telling the player to select a ground board flight path for an aero on an atmospheric board.
     *
     * @param flightPathBoard The ground Board on which the flight path must be planned
     * @see #show()
     */
    public FlightPathNotice(ClientGUI clientGui, Board flightPathBoard) {
        this.clientGui = Objects.requireNonNull(clientGui);
        this.flightPathBoard = Objects.requireNonNull(flightPathBoard);
    }

    private void initialize() {
        JEditorPane messagePane = new JEditorPane();
        messagePane.setContentType("text/html");
        messagePane.setEditable(false);
        messagePane.setText(Messages.getString("MovementDisplay.flightPathNotice")
              .formatted(WIDTH, flightPathBoard.getBoardName()));
        messagePane.addHyperlinkListener(e -> {
            if (e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED)) {
                clientGui.showBoardView(flightPathBoard.getBoardId());
            }
        });

        JPanel checkboxPanel = new JPanel();
        checkboxPanel.setBorder(new EmptyBorder(10, 0, 10, 0));
        checkboxPanel.add(dontShowAgain);

        contentPanel.setLayout(new BorderLayout());
        contentPanel.add(messagePane, BorderLayout.CENTER);
        contentPanel.add(checkboxPanel, BorderLayout.SOUTH);
    }

    /**
     * Sets this flight path notice dialog visible. Will also suspend tooltips on the boardviews while the dialog is
     * shown.
     */
    public void show() {
        if (!preferences.hasProperty(SHOW_FLIGHT_PATH_NOTICE) || preferences.getBoolean(SHOW_FLIGHT_PATH_NOTICE)) {
            initialize();
            clientGui.suspendBoardTooltips();

            JOptionPane.showMessageDialog(clientGui.getFrame(), contentPanel);

            preferences.setValue(SHOW_FLIGHT_PATH_NOTICE, !dontShowAgain.isSelected());
            clientGui.activateBoardTooltips();
        }
    }
}
