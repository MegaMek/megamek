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

import megamek.client.ui.Messages;
import megamek.client.ui.swing.ButtonEsc;
import megamek.client.ui.swing.ClientDialog;
import megamek.client.ui.swing.CloseAction;
import megamek.client.ui.swing.OkayAction;
import megamek.client.ui.swing.dialog.DialogButton;
import megamek.common.preference.IClientPreferences;
import megamek.common.preference.PreferenceManager;
import megamek.common.util.StringUtil;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Vector;

public abstract class AbstractGameConnectionDialog extends ClientDialog implements ActionListener {

    /**
     * We need a way to access the action map for a JComboBox editor, so that we can
     * have it fire an action when wenter is pressed. This simple class allows this.
     */
    public class SimpleComboBoxEditor extends JTextField implements ComboBoxEditor {

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

    private IClientPreferences clientPreferences = PreferenceManager.getClientPreferences();

    protected AbstractGameConnectionDialog(JFrame owner, String title, boolean modal, String playerName) {
        this(owner, title, modal, playerName, null);
    }

    protected AbstractGameConnectionDialog(JFrame owner, String title, boolean modal, String playerName, Vector<String> playerNames) {
        super(owner, title, modal);

        this.playerNames = playerNames;

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
        JButton okayB = new DialogButton(new OkayAction(this));
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
                preferredSize.setSize(180, preferredSize.getHeight());
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
            //  but that shouldn't be a problem for these dialogs
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
        return !getPlayerName().trim().isBlank();
    }
    //endregion Validation

    @Override
    public void actionPerformed(ActionEvent e) {
        // reached from the Okay button or pressing Enter in the text fields
        setPlayerName(getPlayerNameFromUI());
        try {
            setPort(Integer.parseInt(getPortField().getText()));
        } catch (NumberFormatException ex) {
            LogManager.getLogger().error("", ex);
        }

        setConfirmed(true);
        getClientPreferences().setLastPlayerName(getPlayerName());
        getClientPreferences().setLastConnectPort(getPort());
    }
}
