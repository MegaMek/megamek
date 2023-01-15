/*
 * MegaMek - Copyright (C) 2000-2003 Ben Mazur (bmazur@sev.org)
 * MegaMek - Copyright (C) 2020 - The MegaMek Team  
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.client.ui.swing;

import megamek.client.Client;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.IGame;
import megamek.common.Player;
import megamek.common.Team;
import megamek.common.event.GameListener;
import megamek.common.event.GameListenerAdapter;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.preference.IPreferenceChangeListener;
import megamek.common.preference.PreferenceChangeEvent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.MessageFormat;
import java.util.Comparator;
import java.util.List;

public class PlayerListDialog extends JDialog implements ActionListener, IPreferenceChangeListener {

    private static final long serialVersionUID = 7270469195373150106L;

    private JList<String> playerList = new JList<>(new DefaultListModel<>());

    private Client client;
    private JButton butOkay;
    private boolean modal;

    private static final String MSG_OKAY = Messages.getString("Okay");
    private static final String MSG_TITLE = Messages.getString("PlayerListDialog.title");
    private static final String MSG_NOTEAM = Messages.getString("PlayerListDialog.NoTeam");
    private static final String MSG_TEAM = Messages.getString("PlayerListDialog.Team");
    private static final String MSG_TEAMLESS = Messages.getString("PlayerListDialog.TeamLess");
    private static final String MSG_PLAYER_GM = Messages.getString("PlayerListDialog.player_gm");
    private static final String MSG_PLAYER_GHOST = Messages.getString("PlayerListDialog.player_ghost");
    private static final String MSG_PLAYER_BOT = Messages.getString("PlayerListDialog.player_bot");
    private static final String MSG_PLAYER_HUMAN = Messages.getString("PlayerListDialog.player_human");
    private static final String MSG_PLAYER_OBSERVER = Messages.getString("PlayerListDialog.player_observer");
    private static final String MSG_PLAYER_DONE = Messages.getString("PlayerListDialog.player_done");
    private static final String MSG_PLAYER_SEEALL = Messages.getString("PlayerListDialog.player_seeall");
    private static final String MSG_PLAYER_SINGLEBLIND = Messages.getString("PlayerListDialog.player_singleblind");
    private static final String MSG_PLAYER_IGNOREDOUBLEBLIND = Messages.getString("PlayerListDialog.player_ignoreDoubleBlind");

    private static final GUIPreferences GUIP = GUIPreferences.getInstance();

    public PlayerListDialog(JFrame parent, Client client, boolean modal) {
        super(parent, "", false);
        this.setTitle(MSG_TITLE);
        this.client = client;
        this.modal = modal;

        client.getGame().addGameListener(gameListener);

        add(playerList, BorderLayout.CENTER);
        add(Box.createHorizontalStrut(20), BorderLayout.LINE_START);
        add(Box.createHorizontalStrut(20), BorderLayout.LINE_END);

        butOkay = new JButton(MSG_OKAY);
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

        adaptToGUIScale();
        GUIP.addPreferenceChangeListener(this);

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
     * Refreshes the player list component with information from the game
     * object.
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
                        playerDisplay.append(MSG_NOTEAM);
                    } else {
                        playerDisplay.append(MessageFormat.format(MSG_TEAM, team.getId()));
                    }
                } else {
                    playerDisplay.append(MSG_TEAMLESS);
                }
            }

            if (player.isGameMaster()) {
                playerDisplay.append(MSG_PLAYER_GM);
            }

            if (player.isGhost()) {
                playerDisplay.append(MSG_PLAYER_GHOST);
            } else {
                if (player.isBot()) {
                    playerDisplay.append(MSG_PLAYER_BOT);
                } else {
                    playerDisplay.append(MSG_PLAYER_HUMAN);
                }
                if (player.isObserver()) {
                    playerDisplay.append(MSG_PLAYER_OBSERVER);
                } else if (player.isDone()) {
                    playerDisplay.append(MSG_PLAYER_DONE);
                }
            }

            // this may be too much detail long term, but is useful for understanding the modes
            // during testing
            if (player.getSeeAll()) {
                playerDisplay.append(MSG_PLAYER_SEEALL);
            }

            if (player.getSingleBlind()) {
                playerDisplay.append(MSG_PLAYER_SINGLEBLIND);
            }

            if (player.canIgnoreDoubleBlind()) {
                playerDisplay.append(MSG_PLAYER_IGNOREDOUBLEBLIND);
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

    private GameListener gameListener = new GameListenerAdapter() {
        @Override
        public void gamePhaseChange(GamePhaseChangeEvent e) {
            switch (e.getOldPhase()) {
                case VICTORY:
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void processWindowEvent(WindowEvent e) {
        super.processWindowEvent(e);
        if ((e.getID() == WindowEvent.WINDOW_DEACTIVATED) || (e.getID() == WindowEvent.WINDOW_CLOSING)) {
            if (!modal) {
                GUIP.setPlayerListPosX(getLocation().x);
                GUIP.setPlayerListPosY(getLocation().y);
            }
        }
    }

    private void adaptToGUIScale() {
        UIUtil.adjustDialog(this, UIUtil.FONT_SCALE1);
        setMinimumSize(new Dimension(UIUtil.scaleForGUI(300), UIUtil.scaleForGUI(260)));
    }

    @Override
    public void preferenceChange(PreferenceChangeEvent e) {
        // Update the text size when the GUI scaling changes
        if (e.getName().equals(GUIPreferences.GUI_SCALE)) {
            adaptToGUIScale();
        }
    }
}
