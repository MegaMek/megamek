/*
 * MegaMek - Copyright (C) 2002, 2003 Ben Mazur (bmazur@sev.org)
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

/*
 * StartingPositionDialog.java
 *
 * Created on December 9, 2002, 2:43 PM
 */

package megamek.client.ui.swing;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Enumeration;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import megamek.client.Client;
import megamek.client.ui.Messages;
import megamek.common.Entity;
import megamek.common.EntitySelector;
import megamek.common.IOffBoardDirections;
import megamek.common.IStartingPositions;
import megamek.common.Player;

/**
 * The starting position dialog allows the player to select a starting position.
 *
 * @author Ben
 */
public class StartingPositionDialog extends JDialog implements ActionListener {

    /**
     *
     */
    private static final long serialVersionUID = 7255701351824139329L;
    private Client client;
    private ClientGUI clientgui;

    private JPanel panButtons = new JPanel();
    private JButton butOkay = new JButton(Messages.getString("Okay")); //$NON-NLS-1$
    private JButton butCancel = new JButton(Messages.getString("Cancel")); //$NON-NLS-1$

    private JPanel panStartButtons = new JPanel();
    private JButton[] butStartPos = new JButton[11];

    private JList lisStartList = new JList(new DefaultListModel());

    /**
     * Creates a new instance of StartingPositionDialog
     */
    public StartingPositionDialog(ClientGUI clientgui) {
        super(clientgui.frame, Messages
                .getString("StartingPositionDialog.title"), true); //$NON-NLS-1$
        client = clientgui.getClient();
        this.clientgui = clientgui;

        lisStartList.setEnabled(false);

        setupStartGrid();
        setupButtons();

        // layout
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        getContentPane().setLayout(gridbag);

        c.fill = GridBagConstraints.VERTICAL;
        c.insets = new Insets(4, 4, 4, 4);
        c.gridwidth = 1;
        gridbag.setConstraints(panStartButtons, c);
        getContentPane().add(panStartButtons);

        c.weightx = 1.0;
        c.weighty = 1.0;
        c.fill = GridBagConstraints.BOTH;
        c.gridwidth = GridBagConstraints.REMAINDER;
        JScrollPane sp = new JScrollPane(lisStartList);
        gridbag.setConstraints(sp, c);
        getContentPane().add(sp);

        c.fill = GridBagConstraints.NONE;
        gridbag.setConstraints(panButtons, c);
        getContentPane().add(panButtons);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                setVisible(false);
            }
        });

        pack();
        setResizable(false);
        setLocation(clientgui.frame.getLocation().x
                + clientgui.frame.getSize().width / 2 - getSize().width / 2,
                clientgui.frame.getLocation().y
                        + clientgui.frame.getSize().height / 2
                        - getSize().height / 2);
    }

    private void setupStartGrid() {
        for (int i = 0; i < 11; i++) {
            butStartPos[i] = new JButton(
                    IStartingPositions.START_LOCATION_NAMES[i]);
            butStartPos[i].addActionListener(this);
        }
        panStartButtons.setLayout(new GridLayout(4, 3));
        panStartButtons.add(butStartPos[1]);
        panStartButtons.add(butStartPos[2]);
        panStartButtons.add(butStartPos[3]);
        panStartButtons.add(butStartPos[8]);
        panStartButtons.add(butStartPos[10]);
        panStartButtons.add(butStartPos[4]);
        panStartButtons.add(butStartPos[7]);
        panStartButtons.add(butStartPos[6]);
        panStartButtons.add(butStartPos[5]);
        panStartButtons.add(butStartPos[0]);
        panStartButtons.add(butStartPos[9]);
    }

    private void setupButtons() {
        butOkay.addActionListener(this);
        butCancel.addActionListener(this);

        // layout
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        panButtons.setLayout(gridbag);

        c.insets = new Insets(5, 5, 0, 0);
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.fill = GridBagConstraints.VERTICAL;
        c.ipadx = 20;
        c.ipady = 5;
        c.gridwidth = 1;
        gridbag.setConstraints(butOkay, c);
        panButtons.add(butOkay);

        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(butCancel, c);
        panButtons.add(butCancel);
    }

    public void update() {
        ((DefaultListModel) lisStartList.getModel()).removeAllElements();
        for (Enumeration<Player> i = client.getPlayers(); i.hasMoreElements();) {
            Player player = i.nextElement();
            if (player != null) {
                StringBuffer ssb = new StringBuffer();
                ssb.append(player.getName()).append(" : "); //$NON-NLS-1$
                ssb.append(IStartingPositions.START_LOCATION_NAMES[player
                        .getStartingPos()]);
                ((DefaultListModel) lisStartList.getModel()).addElement(ssb
                        .toString());
            }
        }
    }

    public void actionPerformed(ActionEvent ev) {
        for (int i = 0; i < 11; i++) {
            if (ev.getSource().equals(butStartPos[i])) {
                if (client.game.getOptions().booleanOption("double_blind")
                        && client.game.getOptions().booleanOption(
                                "exclusive_db_deployment")) {
                    if (i == 0) {
                        clientgui
                                .doAlertDialog("Starting Position not allowed",
                                        "In Double Blind play, you cannot choose 'Any' as starting position.");
                        return;
                    }
                    for (Enumeration<Player> e = client.game.getPlayers(); e
                            .hasMoreElements();) {
                        Player player = e.nextElement();
                        if (player.getStartingPos() == 0) {
                            continue;
                        }
                        // CTR and EDG don't overlap
                        if (((player.getStartingPos() == 9) && (i == 10))
                            || ((player.getStartingPos() == 10) && (i == 9))) {
                            continue;
                        }
                        // check for overlapping starting directions
                        if (((player.getStartingPos() == i)
                                || (player.getStartingPos() + 1 == i) || (player
                                .getStartingPos() - 1 == i))
                                && (player.getId() != client.getLocalPlayer()
                                        .getId())) {
                            clientgui
                                    .doAlertDialog(
                                            "Must choose exclusive deployment zone",
                                            "When using double blind, each player needs to have an exclusive deployment zone.");
                            return;
                        }
                    }
                }
                if (client.game.getOptions().booleanOption("deep_deployment")
                        && (i > 0) && (i <= 9)) {
                    i += 10;
                }
                client.getLocalPlayer().setStartingPos(i);
                client.sendPlayerInfo();
                // If the gameoption set_arty_player_homeedge is set,
                // set all the player's offboard arty units to be behind the
                // newly
                // selected home edge.
                if (client.game.getOptions().booleanOption(
                        "set_arty_player_homeedge")) { //$NON-NLS-1$
                    int direction = IOffBoardDirections.NONE;
                    switch (i) {
                        case 0:
                            break;
                        case 1:
                        case 2:
                        case 3:
                            direction = IOffBoardDirections.NORTH;
                            break;
                        case 4:
                            direction = IOffBoardDirections.EAST;
                            break;
                        case 5:
                        case 6:
                        case 7:
                            direction = IOffBoardDirections.SOUTH;
                            break;
                        case 8:
                            direction = IOffBoardDirections.WEST;
                            break;
                        case 11:
                        case 12:
                        case 13:
                            direction = IOffBoardDirections.NORTH;
                            break;
                        case 14:
                            direction = IOffBoardDirections.EAST;
                            break;
                        case 15:
                        case 16:
                        case 17:
                            direction = IOffBoardDirections.SOUTH;
                            break;
                        case 18:
                            direction = IOffBoardDirections.WEST;
                            break;
                        default:
                    }
                    Enumeration<Entity> thisPlayerArtyUnits = client.game
                            .getSelectedEntities(new EntitySelector() {
                                public boolean accept(Entity entity) {
                                    if (entity.getOwnerId() == client
                                            .getLocalPlayer().getId()) {
                                        return true;
                                    }
                                    return false;
                                }
                            });
                    while (thisPlayerArtyUnits.hasMoreElements()) {
                        Entity entity = thisPlayerArtyUnits.nextElement();
                        if (entity.getOffBoardDirection() != IOffBoardDirections.NONE) {
                            if (direction > IOffBoardDirections.NONE) {
                                entity.setOffBoard(entity.getOffBoardDistance(),
                                        direction);
                            }
                        }
                    }
                }
            }
        }
        setVisible(false);
    }

    public void setClient(Client client) {
        this.client = client;
    }

}
