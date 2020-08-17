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
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import megamek.client.ui.Messages;
import megamek.common.preference.IClientPreferences;
import megamek.common.preference.PreferenceManager;

/** The Connect to game (as Bot or Player) dialog */
public class ConnectDialog extends ClientDialog implements ActionListener {
    
    private static final long serialVersionUID = 5895056240077042429L;
    
    private String playerName;
    private String serverAddr;
    private int port;
    private JTextField yourNameF;
    private JTextField serverAddrF;
    private JTextField portF;
    
    private IClientPreferences cPrefs = PreferenceManager.getClientPreferences();

    public ConnectDialog(JFrame frame) {
        super(frame, Messages.getString("MegaMek.ConnectDialog.title"), true);
        JLabel yourNameL = new JLabel(Messages.getString("MegaMek.yourNameL"), SwingConstants.RIGHT);
        JLabel serverAddrL = new JLabel(Messages.getString("MegaMek.serverAddrL"), SwingConstants.RIGHT);
        JLabel portL = new JLabel(Messages.getString("MegaMek.portL"), SwingConstants.RIGHT);
        yourNameF = new JTextField(cPrefs.getLastPlayerName(), 16);
        yourNameF.addActionListener(this);
        serverAddrF = new JTextField(cPrefs.getLastConnectAddr(), 16);
        serverAddrF.addActionListener(this);
        portF = new JTextField(cPrefs.getLastConnectPort() + "", 4);
        portF.addActionListener(this);
        
        JPanel middlePanel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.insets = new Insets(5, 5, 5, 5);
        c.gridwidth = 1;
        
        addOptionRow(middlePanel, c, yourNameL, yourNameF);
        addOptionRow(middlePanel, c, serverAddrL, serverAddrF);
        addOptionRow(middlePanel, c, portL, portF);
        
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

    public String getServerAddr() {
        return serverAddr;
    }

    public void setServerAddr(String serverAddr) {
        this.serverAddr = serverAddr;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // reached from the Okay button or pressing Enter in
        // the text fields
        try {
            setPlayerName(yourNameF.getText());
            setServerAddr(serverAddrF.getText());
            setPort(Integer.decode(portF.getText().trim()));
        } catch (NumberFormatException ex) {
            System.err.println(ex.getMessage());
        }

        // update settings
        cPrefs.setLastPlayerName(getPlayerName());
        cPrefs.setLastConnectAddr(getServerAddr());
        cPrefs.setLastConnectPort(getPort());
        setVisible(false);
    }
}
