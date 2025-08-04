/*
 * Copyright (C) 2000-2003 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.MessageFormat;
import java.util.Comparator;
import java.util.List;
import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JList;

import megamek.client.Client;
import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.common.IGame;
import megamek.common.Player;
import megamek.common.Team;

public class PlayerListDialog extends JDialog implements ActionListener {

    private static final long serialVersionUID = 7270469195373150106L;

    private JList<String> playerList = new JList<>(new DefaultListModel<>());

    private Client client;
    private JButton butOkay;
    private boolean modal;
    private static final GUIPreferences GUIP = GUIPreferences.getInstance();

    public PlayerListDialog(JFrame parent, Client client, boolean modal) {
        super(parent, "", false);
        this.setTitle(Messages.getString("PlayerListDialog.title"));
        this.client = client;
        this.modal = modal;

        add(playerList, BorderLayout.CENTER);
        add(Box.createHorizontalStrut(20), BorderLayout.LINE_START);
        add(Box.createHorizontalStrut(20), BorderLayout.LINE_END);

        butOkay = new JButton(Messages.getString("Okay"));
        butOkay.addActionListener(this);
        add(butOkay, BorderLayout.PAGE_END);

        // closing the window is the same as hitting butOkay
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                actionPerformed(new ActionEvent(butOkay,
                      ActionEvent.ACTION_PERFORMED, butOkay.getText()));
            }
        });

        refreshPlayerList();
        setMinimumSize(new Dimension(300, 260));
        pack();
        setResizable(false);

        if (modal) {
            setModal(true);
            setLocation(parent.getLocation().x + (parent.getSize().width / 2) - (getSize().width / 2),
                  parent.getLocation().y + (parent.getSize().height / 2) - (getSize().height / 2));
        } else {
            setModal(false);
            setLocation(GUIP.getPlayerListPosX(), GUIP.getPlayerListPosY());
        }
    }

    public void refreshPlayerList(JList<String> playerList,
          Client client) {
        refreshPlayerList(playerList, client, false);
    }

    /** @return The game's players list sorted by id. */
    private static List<Player> sortedPlayerList(IGame game) {
        List<Player> playerList = game.getPlayersList();
        playerList.sort(Comparator.comparingInt(Player::getId));
        return playerList;
    }

    /**
     * Refreshes the player list component with information from the game object.
     */
    public void refreshPlayerList(JList<String> playerList,
          Client client, boolean displayTeam) {
        ((DefaultListModel<String>) playerList.getModel()).removeAllElements();

        for (Player player : sortedPlayerList(client.getGame())) {
            StringBuffer playerDisplay = new StringBuffer(String.format("%-12s", player.getName()));

            // Append team information
            if (displayTeam) {
                Team team = client.getGame().getTeamForPlayer(player);
                if (team != null) {
                    if (team.getId() == Player.TEAM_NONE) {
                        playerDisplay.append(Messages.getString("PlayerListDialog.NoTeam"));
                    } else {
                        playerDisplay.append(MessageFormat.format(Messages.getString("PlayerListDialog.Team"),
                              team.getId()));
                    }
                } else {
                    playerDisplay.append(Messages.getString("PlayerListDialog.TeamLess"));
                }
            }

            if (player.isGameMaster()) {
                playerDisplay.append(Messages.getString("PlayerListDialog.player_gm"));
            }

            if (player.isGhost()) {
                playerDisplay.append(Messages.getString("PlayerListDialog.player_ghost"));
            } else {
                if (player.isBot()) {
                    playerDisplay.append(Messages.getString("PlayerListDialog.player_bot"));
                } else {
                    playerDisplay.append(Messages.getString("PlayerListDialog.player_human"));
                }
                if (player.isObserver()) {
                    playerDisplay.append(Messages.getString("PlayerListDialog.player_observer"));
                } else if (player.isDone()) {
                    playerDisplay.append(Messages.getString("PlayerListDialog.player_done"));
                }
            }

            // this may be too much detail long term, but is useful for understanding the modes
            // during testing
            if (player.getSeeAll()) {
                playerDisplay.append(Messages.getString("PlayerListDialog.player_seeall"));
            }

            if (player.getSingleBlind()) {
                playerDisplay.append(Messages.getString("PlayerListDialog.player_singleblind"));
            }

            if (player.canIgnoreDoubleBlind()) {
                playerDisplay.append(Messages.getString("PlayerListDialog.player_ignoreDoubleBlind"));
            }

            ((DefaultListModel<String>) playerList.getModel()).addElement(playerDisplay.toString());
        }
    }

    public void refreshPlayerList() {
        refreshPlayerList(playerList, client, true);
        pack();
    }

    public Player getSelected() {
        if (!playerList.isSelectionEmpty()) {
            return sortedPlayerList(client.getGame()).get(playerList.getSelectedIndex());
        }

        return null;
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        if (ae.getSource().equals(butOkay)) {
            setVisible(false);
            if (!modal) {
                GUIP.setPlayerListEnabled(false);
            }
        }
    }

    public void saveSettings() {
        GUIP.setPlayerListPosX(getLocation().x);
        GUIP.setPlayerListPosY(getLocation().y);
    }

    @Override
    protected void processWindowEvent(WindowEvent e) {
        super.processWindowEvent(e);
        if ((e.getID() == WindowEvent.WINDOW_DEACTIVATED) || (e.getID() == WindowEvent.WINDOW_CLOSING)) {
            if (!modal) {
                saveSettings();
            }
        }
    }
}
