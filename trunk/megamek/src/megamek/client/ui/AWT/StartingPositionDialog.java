/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
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

package megamek.client;

import megamek.common.*;

import java.awt.*;
import java.awt.event.*;
import java.util.Enumeration;

/**
 * The starting position dialog allows the player to select a starting position.
 *
 * @author  Ben
 */
public class StartingPositionDialog 
extends java.awt.Dialog implements ActionListener 
{
    private static final String startNames[] = {"Any", "NW", "N", "NE", "E", "SE", "S", "SW", "W"};
    
    private Client client;
    
    private Panel panButtons = new Panel();
    private Button butOkay = new Button("Okay");
    private Button butCancel = new Button("Cancel");
    
    private Panel panStartButtons = new Panel();
    private Button[] butStartPos = new Button[9];
    
    private List lisStartList = new List(5);

    /** Creates a new instance of StartingPositionDialog */
    public StartingPositionDialog(Client client) {
        super(client.frame, "Select a Starting Position...", true);
        this.client = client;
        
        lisStartList.setEnabled(false);
        
        setupStartGrid();
        setupButtons();
        
        // layout
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        this.setLayout(gridbag);
            
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(4, 4, 4, 4);
        c.weightx = 1.0;    c.weighty = 1.0;
        c.gridwidth = 1;
        gridbag.setConstraints(panStartButtons, c);
        this.add(panStartButtons);
            
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(lisStartList, c);
        this.add(lisStartList);
            
        c.fill = GridBagConstraints.VERTICAL;
        gridbag.setConstraints(panButtons, c);
        this.add(panButtons);        
        
        addWindowListener(new WindowAdapter() {
	    public void windowClosing(WindowEvent e) { setVisible(false); }
	});

        pack();
        setResizable(false);
        setLocation(client.frame.getLocation().x + client.frame.getSize().width/2 - getSize().width/2,
                    client.frame.getLocation().y + client.frame.getSize().height/2 - getSize().height/2);
    }
    
    private void setupStartGrid() {
         for (int i = 0; i < 9; i++) {
            butStartPos[i] = new Button(startNames[i]);
            butStartPos[i].addActionListener(this);
        }
        panStartButtons.setLayout(new GridLayout(3, 3));
        panStartButtons.add(butStartPos[1]);
        panStartButtons.add(butStartPos[2]);
        panStartButtons.add(butStartPos[3]);
        panStartButtons.add(butStartPos[8]);
        panStartButtons.add(butStartPos[0]);
        panStartButtons.add(butStartPos[4]);
        panStartButtons.add(butStartPos[7]);
        panStartButtons.add(butStartPos[6]);
        panStartButtons.add(butStartPos[5]);
   }
    
    private void setupButtons() {
        butOkay.addActionListener(this);
        butCancel.addActionListener(this);
        
        // layout
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        panButtons.setLayout(gridbag);
            
        c.insets = new Insets(5, 5, 0, 0);
        c.weightx = 1.0;    c.weighty = 1.0;
        c.fill = GridBagConstraints.VERTICAL;
        c.ipadx = 20;    c.ipady = 5;
        c.gridwidth = 1;
        gridbag.setConstraints(butOkay, c);
        panButtons.add(butOkay);
            
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(butCancel, c);
        panButtons.add(butCancel);
    }
    
    public void update() {
        lisStartList.removeAll();
        for (Enumeration i = client.getPlayers(); i.hasMoreElements();) {
            Player player = (Player)i.nextElement();
            if(player != null) {
                StringBuffer ssb = new StringBuffer();
                ssb.append(player.getName()).append(" : ");
                ssb.append(startNames[player.getStartingPos()]);
                lisStartList.add(ssb.toString());
            }
        }
    }
    
    public void actionPerformed(java.awt.event.ActionEvent ev) {
         for (int i = 0; i < 9; i++) {
            if (ev.getSource() == butStartPos[i]) { 
                client.getLocalPlayer().setStartingPos(i);
                client.sendPlayerInfo();
            }
         }
         setVisible(false);
    }
}
