/*  
* MegaMek - Copyright (C) 2020 - The MegaMek Team. All Rights Reserved.
*  
* This program is free software; you can redistribute it and/or modify it under  
* the terms of the GNU General Public License as published by the Free Software  
* Foundation; either version 2 of the License, or (at your option) any later  
* version.  
*  
* This program is distributed in the hope that it will be useful, but WITHOUT  
* ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS  
* FOR A PARTICULAR PURPOSE. See the GNU General Public License for more  
* details.  
*/
package megamek.client.ui.swing;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import megamek.MegaMek;
import megamek.client.ui.Messages;
import megamek.common.preference.IClientPreferences;
import megamek.common.preference.PreferenceManager;

/**
 * The host game dialog shown when hosting a new game and when loading a game
 */
public class HostDialog extends ClientDialog implements ActionListener {
    
    private static final long serialVersionUID = -103094006944170081L;
    
    // Some fields are accessed for the results of the dialog
    private String playerName;
    private String serverPass;
    private int port;
    private boolean register;
    private String metaserver;

    private JTextField yourNameF;
    private JTextField serverPassF;
    private JTextField portF;
    private JCheckBox registerC;
    private JTextField metaserverF;
    
    private IClientPreferences cPrefs = PreferenceManager.getClientPreferences();

    /** Constructs a host game dialog for hosting or loading a game. */
    public HostDialog(JFrame frame) {
        super(frame, Messages.getString("MegaMek.HostDialog.title"), true);
        JLabel yourNameL = new JLabel(Messages.getString("MegaMek.yourNameL"), SwingConstants.RIGHT);
        JLabel serverPassL = new JLabel(Messages.getString("MegaMek.serverPassL"), SwingConstants.RIGHT);
        JLabel portL = new JLabel(Messages.getString("MegaMek.portL"), SwingConstants.RIGHT);
        yourNameF = new JTextField(cPrefs.getLastPlayerName(), 16);
        yourNameL.setLabelFor(yourNameF);
        yourNameF.addActionListener(this);
        serverPassF = new JTextField(cPrefs.getLastServerPass(), 16);
        serverPassL.setLabelFor(serverPassF);
        serverPassF.addActionListener(this);
        portF = new JTextField(cPrefs.getLastServerPort() + "", 4);
        portL.setLabelFor(portF);
        portF.addActionListener(this);
        setMetaserver(cPrefs.getMetaServerName());
        JLabel metaserverL = new JLabel(Messages.getString("MegaMek.metaserverL"), SwingConstants.RIGHT);
        metaserverF = new JTextField(getMetaserver());
        metaserverL.setEnabled(isRegister());
        metaserverL.setLabelFor(metaserverF);
        metaserverF.setEnabled(isRegister());
        registerC = new JCheckBox(Messages.getString("MegaMek.registerC"));
        setRegister(false);
        registerC.setSelected(isRegister());
        metaserverL.setEnabled(registerC.isSelected());
        metaserverF.setEnabled(registerC.isSelected());
        registerC.addItemListener(event -> {
            metaserverL.setEnabled(registerC.isSelected());
            metaserverF.setEnabled(registerC.isSelected());
        });
        
        JPanel middlePanel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.insets = new Insets(5, 5, 5, 5);
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.EAST;
        
        addOptionRow(middlePanel, c, yourNameL, yourNameF);
        addOptionRow(middlePanel, c, serverPassL, serverPassF);
        addOptionRow(middlePanel, c, portL, portF);
        
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        middlePanel.add(registerC, c);
        
        addOptionRow(middlePanel, c, metaserverL, metaserverF);
        
        add(middlePanel, BorderLayout.CENTER);  
        
        // The buttons
        JButton okayB = new JButton(new OkayAction(this));
        JButton cancelB = new ButtonEsc(new CloseAction(this));

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(okayB);
        buttonPanel.add(cancelB);
        add(buttonPanel, BorderLayout.PAGE_END);
        
        pack();
        setResizable(false);
        center();
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public String getServerPass() {
        return serverPass;
    }

    public void setServerPass(String serverPass) {
        this.serverPass = serverPass;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
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

    @Override
    public void actionPerformed(ActionEvent e) {
        // reached from the Okay button or pressing Enter in
        // the text fields
        try {
            setPlayerName(yourNameF.getText());
            setServerPass(serverPassF.getText());
            setRegister(registerC.isSelected());
            setMetaserver(metaserverF.getText());
            setPort(Integer.parseInt(portF.getText()));
        } catch (NumberFormatException ex) {
            MegaMek.getLogger().error(this, ex.getMessage());
            setPort(2346);
        }

        // update settings
        cPrefs.setLastPlayerName(getPlayerName());
        cPrefs.setLastServerPass(getServerPass());
        cPrefs.setLastServerPort(getPort());
        cPrefs.setValue("megamek.megamek.metaservername", getMetaserver());
        setVisible(false);
    }
}
