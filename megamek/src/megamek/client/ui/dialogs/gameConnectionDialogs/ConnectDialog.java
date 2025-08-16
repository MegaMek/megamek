/*
 * Copyright (C) 2006-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.dialogs.gameConnectionDialogs;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.Serial;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import megamek.client.ui.Messages;
import megamek.server.Server;

/** The Connect to game (as Bot or Player) dialog */
public class ConnectDialog extends AbstractGameConnectionDialog {
    @Serial
    private static final long serialVersionUID = 5895056240077042429L;

    private String serverAddress;
    private JTextField serverAddressField;

    public ConnectDialog(JFrame frame) {
        this(frame, "");
    }

    public ConnectDialog(JFrame frame, String playerName) {
        super(frame, Messages.getString("MegaMek.ConnectDialog.title"), true, playerName);
    }

    //region Initialization
    @Override
    protected JPanel createMiddlePanel() {
        JLabel yourNameL = new JLabel(Messages.getString("MegaMek.yourNameL"), SwingConstants.RIGHT);
        JLabel serverAddrL = new JLabel(Messages.getString("MegaMek.serverAddrL"), SwingConstants.RIGHT);
        JLabel portL = new JLabel(Messages.getString("MegaMek.portL"), SwingConstants.RIGHT);
        setPlayerName(getClientPreferences().getLastPlayerName());
        addPlayerNameActionListener(this);
        serverAddressField = new JTextField(getClientPreferences().getLastConnectAddr(), 16);
        serverAddressField.addActionListener(this);
        setPortField(new JTextField(getClientPreferences().getLastConnectPort() + "", 4));
        getPortField().addActionListener(this);

        JPanel middlePanel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.insets = new Insets(5, 5, 5, 5);
        c.gridwidth = 1;

        addOptionRow(middlePanel, c, yourNameL, getPlayerNameField());
        addOptionRow(middlePanel, c, serverAddrL, serverAddressField);
        addOptionRow(middlePanel, c, portL, getPortField());

        return middlePanel;
    }
    //endregion Initialization

    //region Getters and Setters
    public String getServerAddress() {
        return serverAddress;
    }

    public void setServerAddress(String serverAddress) {
        this.serverAddress = serverAddress;
    }
    //endregion Getters and Setters

    //region Validation
    @Override
    public boolean dataValidation(String errorTitleKey) {
        try {
            setServerAddress(Server.validateServerAddress(getServerAddress()));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(getOwner(), Messages.getString("MegaMek.ServerAddressError"),
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
        setServerAddress(serverAddressField.getText().trim());

        // update settings
        getClientPreferences().setLastConnectAddr(getServerAddress());
        setVisible(false);
    }
}
