/*
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
package megamek.client.ui.dialogs.gameConnectionDialogs;

import static megamek.codeUtilities.MathUtility.clamp;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Vector;
import javax.swing.*;

import megamek.MMConstants;
import megamek.client.ui.Messages;
import megamek.client.ui.buttons.ButtonEsc;
import megamek.client.ui.buttons.DialogButton;
import megamek.client.ui.clientGUI.CloseAction;
import megamek.client.ui.clientGUI.OkayAction;
import megamek.client.ui.dialogs.clientDialogs.ClientDialog;
import megamek.client.ui.util.UIUtil;
import megamek.codeUtilities.MathUtility;
import megamek.codeUtilities.StringUtility;
import megamek.common.preference.ClientPreferences;
import megamek.common.preference.PreferenceManager;
import megamek.logging.MMLogger;
import megamek.server.Server;

public abstract class AbstractGameConnectionDialog extends ClientDialog implements ActionListener {
    private static final MMLogger logger = MMLogger.create(AbstractGameConnectionDialog.class);

    /**
     * We need a way to access the action map for a JComboBox editor, so that we can have it fire an action when wenter
     * is pressed. This simple class allows this.
     */
    public static class SimpleComboBoxEditor extends JTextField implements ComboBoxEditor {

        private static final long serialVersionUID = 4496820410417436582L;

        @Override
        public Component getEditorComponent() {
            return this;
        }

        @Override
        public void setItem(Object anObject) {
            if (anObject != null) {
                setText(anObject.toString());
            } else {
                setText(null);
            }
        }

        @Override
        public Object getItem() {
            return getText();
        }

    }

    private static final long serialVersionUID = -5114410402284987181L;
    private String playerName;
    private int port;

    private boolean confirmed;

    private JTextField playerNameField = null;
    private JComboBox<String> playerNameCombo = null;
    private JTextField portField;

    private Vector<String> playerNames = null;

    private ClientPreferences clientPreferences = PreferenceManager.getClientPreferences();

    protected AbstractGameConnectionDialog(JFrame owner, String title, boolean modal, String playerName) {
        this(owner, title, modal, playerName, null);
    }

    protected AbstractGameConnectionDialog(JFrame owner, String title, boolean modal, String playerName,
          Vector<String> playerNames) {
        super(owner, title, modal);

        this.playerNames = playerNames;

        setPlayerName(""); // initialize player name
        setPort(MMConstants.DEFAULT_PORT);
        setConfirmed(false);

        initComponents();

        // if the player name is specified, overwrite the preference with it
        if (!StringUtility.isNullOrBlank(playerName)) {
            setPlayerName(playerName);
        }
    }

    // region Initialization
    private void initComponents() {
        add(createMiddlePanel(), BorderLayout.CENTER);

        createButtons();

        pack();
        setResizable(false);
        center();
    }

    protected abstract JPanel createMiddlePanel();

    protected void createButtons() {
        JButton okayB = new DialogButton(new OkayAction(this));
        JButton cancelB = new ButtonEsc(new CloseAction(this));

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(okayB);
        buttonPanel.add(cancelB);
        add(buttonPanel, BorderLayout.PAGE_END);
    }
    // endregion Initialization

    // region Getters and Setters
    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName.trim();
        if (playerNames == null) {
            if (playerNameField == null) {
                playerNameField = new JTextField(playerName, 16);
            } else {
                playerNameField.setText(playerName);
            }
        } else {
            if (playerNameCombo == null) {
                playerNameCombo = new JComboBox<>(playerNames);
                Dimension preferredSize = playerNameCombo.getPreferredSize();
                preferredSize.setSize(UIUtil.scaleForGUI(180), UIUtil.scaleForGUI(25));
                playerNameCombo.setPreferredSize(preferredSize);
                playerNameCombo.setEditable(true);
            }
            playerNameCombo.setSelectedItem(playerName);
        }
    }

    public String getPlayerNameFromUI() {
        if (playerNames == null) {
            return playerNameField.getText();
        } else {
            return (String) playerNameCombo.getSelectedItem();
        }
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

    public JComponent getPlayerNameField() {
        if (playerNames == null) {
            return playerNameField;
        } else {
            return playerNameCombo;
        }
    }

    public void addPlayerNameActionListener(ActionListener listener) {
        if (playerNames == null) {
            playerNameField.addActionListener(listener);
        } else {
            // Make it so an action is fired when enter is pressed
            // This is necessary because the default JComboBox ActionEven
            // can't distinguish between typing and hitting enter
            // Note, this won't work with multiple action listeners
            // but that shouldn't be a problem for these dialogs
            SimpleComboBoxEditor cbe = new SimpleComboBoxEditor();
            InputMap im = cbe.getInputMap();
            ActionMap am = cbe.getActionMap();
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "enter");
            am.put("enter", new AbstractAction() {
                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(ActionEvent e) {
                    listener.actionPerformed(e);
                }
            });
            playerNameCombo.setEditor(cbe);
        }
    }

    public JTextField getPortField() {
        return portField;
    }

    public void setPortField(JTextField portField) {
        this.portField = portField;
    }

    protected ClientPreferences getClientPreferences() {
        return clientPreferences;
    }
    // endregion Getters and Setters

    // region Validation
    public boolean dataValidation(String errorTitleKey) {

        if (!isConfirmed()) {
            return false;
        }

        try {
            setPlayerName(Server.validatePlayerName(playerName));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(getOwner(),
                  Messages.getString("MegaMek.PlayerNameError"),
                  Messages.getString(errorTitleKey),
                  JOptionPane.ERROR_MESSAGE);
            return false;
        }

        try {
            setPort(Server.validatePort(port));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(getOwner(),
                  Messages.getFormattedString("MegaMek.PortError", MMConstants.MIN_PORT, MMConstants.MAX_PORT),
                  Messages.getString(errorTitleKey),
                  JOptionPane.ERROR_MESSAGE);
            return false;
        }

        return true;
    }
    // endregion Validation

    @Override
    public void actionPerformed(ActionEvent e) {
        // reached from the Okay button or pressing Enter in the text fields
        setPlayerName(getPlayerNameFromUI());
        int port = MathUtility.parseInt(getPortField().getText(), MMConstants.DEFAULT_PORT);
        setPort(clamp(port, MMConstants.MIN_PORT, MMConstants.MAX_PORT));
        setConfirmed(true);
        getClientPreferences().setLastPlayerName(getPlayerName());
        getClientPreferences().setLastConnectPort(getPort());
    }
}
