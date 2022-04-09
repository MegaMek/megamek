/*
 * Copyright (c) 2020 - The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.swing.gameConnectionDialogs;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.Vector;

import javax.swing.*;

import megamek.client.ui.Messages;
import megamek.client.ui.swing.util.UIUtil;
import megamek.server.Connectivity.PasswordValidator;

/**
 * The host game dialog shown when hosting a new game and when loading a game
 */
public class HostDialog extends AbstractGameConnectionDialog {
    private static final long serialVersionUID = -103094006944170081L;
    
    // Some fields are accessed for the results of the dialog
    private String serverPass;
    private boolean register;
    private String metaserver;

    private JTextField serverPassField;
    private JCheckBox chkRegister;
    private JTextField metaserverField;

    /** Constructs a host game dialog for hosting or loading a game. */
    public HostDialog(JFrame frame) {
        this(frame, "", null);
    }

    public HostDialog(JFrame frame, String playerName) {
        this(frame, playerName, null);
    }

    public HostDialog(JFrame frame, Vector<String> playerNames) {
        this(frame, "", playerNames);
    }

    public HostDialog(JFrame frame, String playerName, Vector<String> playerNames) {
        super(frame, Messages.getString("MegaMek.HostDialog.title"), true, playerName, playerNames);
    }

    //region Initialization
    @Override
    protected JPanel createMiddlePanel() {
        JLabel playerNameLabel = new JLabel(Messages.getString("MegaMek.yourNameL"), SwingConstants.RIGHT);
        JLabel serverPassLabel = new JLabel(Messages.getString("MegaMek.serverPassL"), SwingConstants.RIGHT);
        JLabel portLabel = new JLabel(Messages.getString("MegaMek.portL"), SwingConstants.RIGHT);
        JLabel metaserverLabel = new JLabel(Messages.getString("MegaMek.metaserverL"), SwingConstants.RIGHT);

        setPlayerName(getClientPreferences().getLastPlayerName());
        playerNameLabel.setLabelFor(getPlayerNameField());
        addPlayerNameActionListener(this);

        serverPassField = new JTextField(getClientPreferences().getLastServerPass(), 16);
        serverPassLabel.setLabelFor(serverPassField);
        serverPassField.addActionListener(this);

        setPortField(new JTextField(getClientPreferences().getLastServerPort() + "", 4));
        portLabel.setLabelFor(getPortField());
        getPortField().addActionListener(this);

        setMetaserver(getClientPreferences().getMetaServerName());
        metaserverField = new JTextField(getMetaserver());
        metaserverLabel.setEnabled(false);
        metaserverLabel.setLabelFor(metaserverField);
        metaserverField.setEnabled(false);

        chkRegister = new JCheckBox(Messages.getString("MegaMek.registerC"));
        setRegister(false);
        chkRegister.setSelected(isRegister());
        metaserverLabel.setEnabled(chkRegister.isSelected());
        chkRegister.addItemListener(event -> {
            metaserverLabel.setEnabled(chkRegister.isSelected());
            metaserverField.setEnabled(chkRegister.isSelected());
        });

        JPanel middlePanel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.insets = new Insets(5, 5, 5, 5);
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.EAST;

        addOptionRow(middlePanel, c, playerNameLabel, getPlayerNameField());
        addOptionRow(middlePanel, c, serverPassLabel, serverPassField);
        addOptionRow(middlePanel, c, portLabel, getPortField());

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        middlePanel.add(chkRegister, c);

        addOptionRow(middlePanel, c, metaserverLabel, metaserverField);

        return middlePanel;
    }
    //endregion Initialization

    //region Getters and Setters
    public String getServerPass() {
        return serverPass;
    }

    public void setServerPass(String serverPass) {
        this.serverPass = serverPass == null ? null : serverPass.trim();
    }

    public boolean isRegister() {
        return register;
    }

    public void setRegister(boolean register) {
        this.register = register;
    }

    public String getMetaserver() {
        return metaserver;
    }

    public void setMetaserver(String metaserver) {
        this.metaserver = metaserver;
    }
    //endregion Getters and Setters

    //region Validation
    @Override
    public boolean dataValidation(String errorTitleKey) {
        try {
            setServerPass(PasswordValidator.validatePassword(getServerPass()));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(getOwner(), Messages.getString("MegaMek.ServerPasswordError"),
                    Messages.getString(errorTitleKey), JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return super.dataValidation(errorTitleKey);
    }
    //endregion Validation

    @Override
    public void actionPerformed(ActionEvent e) {
        // reached from the Okay button or pressing Enter in the text fields
        super.actionPerformed(e);

        setServerPass(serverPassField.getText());
        setRegister(chkRegister.isSelected());
        setMetaserver(metaserverField.getText());

        // update settings
        getClientPreferences().setLastServerPass(getServerPass());
        getClientPreferences().setValue("megamek.megamek.metaservername", getMetaserver());
        setVisible(false);
    }
    
    @Override
    public void setVisible(boolean b) {
        if (b) {
            UIUtil.adjustDialog(getContentPane());
            pack();
        }
        super.setVisible(b);
    }
}
