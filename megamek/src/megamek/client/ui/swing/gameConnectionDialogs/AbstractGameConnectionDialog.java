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

import megamek.MegaMek;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.ButtonEsc;
import megamek.client.ui.swing.ClientDialog;
import megamek.client.ui.swing.CloseAction;
import megamek.client.ui.swing.OkayAction;
import megamek.common.preference.IClientPreferences;
import megamek.common.preference.PreferenceManager;
import megamek.common.util.StringUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public abstract class AbstractGameConnectionDialog extends ClientDialog implements ActionListener {
    private static final long serialVersionUID = -5114410402284987181L;
    private String playerName;
    private int port;

    private boolean confirmed;

    private JTextField playerNameField;
    private JTextField portField;

    private IClientPreferences clientPreferences = PreferenceManager.getClientPreferences();

    protected AbstractGameConnectionDialog(JFrame owner, String title, boolean modal, String playerName) {
        super(owner, title, modal);

        setPlayerName(""); // initialize player name
        setPort(2346);
        setConfirmed(false);

        initComponents();

        // if the player name is specified, overwrite the preference with it
        if (!StringUtil.isNullOrEmpty(playerName)) {
            setPlayerName(playerName);
        }
    }

    //region Initialization
    private void initComponents() {
        add(createMiddlePanel(), BorderLayout.CENTER);

        createButtons();

        pack();
        setResizable(false);
        center();
    }

    protected abstract JPanel createMiddlePanel();

    protected void createButtons() {
        JButton okayB = new JButton(new OkayAction(this));
        JButton cancelB = new ButtonEsc(new CloseAction(this));

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(okayB);
        buttonPanel.add(cancelB);
        add(buttonPanel, BorderLayout.PAGE_END);
    }
    //endregion Initialization

    //region Getters and Setters
    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public void setConfirmed(boolean confirmed) {
        this.confirmed = confirmed;
    }

    public JTextField getPlayerNameField() {
        return playerNameField;
    }

    public void setPlayerNameField(JTextField playerNameField) {
        this.playerNameField = playerNameField;
    }

    public JTextField getPortField() {
        return portField;
    }

    public void setPortField(JTextField portField) {
        this.portField = portField;
    }

    protected IClientPreferences getClientPreferences() {
        return clientPreferences;
    }
    //endregion Getters and Setters

    //region Validation
    public boolean dataValidation(String errorTitleKey) {
        if (!isConfirmed() || StringUtil.isNullOrEmpty(getPlayerName()) || (getPort() == 0)) {
            return false;
        } else if (!validatePlayerName()) {
            JOptionPane.showMessageDialog(getOwner(), Messages.getString("MegaMek.PlayerNameError"),
                    Messages.getString(errorTitleKey), JOptionPane.ERROR_MESSAGE);
            return false;
        }

        return true;
    }

    private boolean validatePlayerName() {
        // Players should have to enter a non-blank, non-whitespace name.
        return !getPlayerName().trim().equals("");
    }
    //endregion Validation

    @Override
    public void actionPerformed(ActionEvent e) {
        // reached from the Okay button or pressing Enter in the text fields
        setPlayerName(getPlayerNameField().getText());
        try {
            setPort(Integer.parseInt(getPortField().getText()));
        } catch (NumberFormatException ex) {
            MegaMek.getLogger().error(this, ex.getMessage());
        }

        setConfirmed(true);
        getClientPreferences().setLastPlayerName(getPlayerName());
        getClientPreferences().setLastConnectPort(getPort());
    }
}
