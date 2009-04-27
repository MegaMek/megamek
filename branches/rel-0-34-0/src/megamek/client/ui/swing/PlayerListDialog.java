/*
 * MegaMek - Copyright (C) 2000-2003 Ben Mazur (bmazur@sev.org)
 * 
 *  This program is free software; you can redistribute it and/or modify it 
 *  under the terms of the GNU General Public License as published by the Free 
 *  Software Foundation; either version 2 of the License, or (at your option) 
 *  any later version.
 * 
 *  This program is distributed in the hope that it will be useful, but 
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 *  for more details.
 */

package megamek.client.ui.swing;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Enumeration;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JList;

import megamek.client.Client;
import megamek.client.ui.Messages;
import megamek.common.Player;

public class PlayerListDialog extends JDialog implements ActionListener {
    /**
     * 
     */
    private static final long serialVersionUID = 7270469195373150106L;
    private JButton butClose = new JButton(Messages
            .getString("PlayerListDialog.Close")); //$NON-NLS-1$
    private JList playerList = new JList(new DefaultListModel());

    private Client client;

    public PlayerListDialog(JFrame parent, Client client) {
        super(parent, Messages.getString("PlayerListDialog.title"), false); //$NON-NLS-1$
        this.client = client;

        butClose.addActionListener(this);

        // layout
        getContentPane().setLayout(new BorderLayout());

        getContentPane().add(playerList, BorderLayout.NORTH);
        getContentPane().add(butClose, BorderLayout.SOUTH);

        refreshPlayerList();

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                setVisible(false);
            }
        });

        pack();
        setResizable(false);
        setLocation(parent.getLocation().x + parent.getSize().width / 2
                - getSize().width / 2, parent.getLocation().y
                + parent.getSize().height / 2 - getSize().height / 2);
    }

    public void actionPerformed(ActionEvent e) {
        setVisible(false);
    }

    /**
     * Refreshes the player list component with information from the game
     * object.
     */
    public static void refreshPlayerList(JList playerList, Client client) {
        ((DefaultListModel) playerList.getModel()).removeAllElements();
        for (Enumeration<Player> e = client.getPlayers(); e.hasMoreElements();) {
            final Player player = e.nextElement();
            StringBuffer playerDisplay = new StringBuffer(player.getName());
            if (player.isGhost()) {
                playerDisplay.append(" ["); //$NON-NLS-1$
                playerDisplay.append(Messages
                        .getString("PlayerListDialog.player_ghost")); //$NON-NLS-1$
                playerDisplay.append(']'); //$NON-NLS-1$
            } else if (player.isObserver()) {
                playerDisplay.append(" ["); //$NON-NLS-1$
                playerDisplay.append(Messages
                        .getString("PlayerListDialog.player_observer")); //$NON-NLS-1$
                playerDisplay.append(']'); //$NON-NLS-1$
            } else if (player.isDone()) {
                playerDisplay.append(" ("); //$NON-NLS-1$
                playerDisplay.append(Messages
                        .getString("PlayerListDialog.player_done")); //$NON-NLS-1$
                playerDisplay.append(')'); //$NON-NLS-1$
            }
            ((DefaultListModel) playerList.getModel()).addElement(playerDisplay
                    .toString());
        }
    }

    private void refreshPlayerList() {
        refreshPlayerList(playerList, client);
    }
}
