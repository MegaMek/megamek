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

package megamek.client;

import java.awt.*;
import java.awt.event.*;
import java.util.Enumeration;

import megamek.client.Client;
import megamek.common.Player;

public class PlayerListDialog
    extends Dialog implements ActionListener
{
    private Button butClose   = new Button("Close");
    private List   playerList = new List();
    
    private Client client;
    
    public PlayerListDialog(Frame parent, Client client) {
        super(parent, "Player list", false);
        this.client = client;
        
        butClose.addActionListener(this);
        
        // layout
        setLayout(new BorderLayout());
        
        add(playerList, BorderLayout.NORTH);
        add(butClose, BorderLayout.SOUTH);
        
        refreshPlayerList();
       
        addWindowListener(new WindowAdapter() {
	    public void windowClosing(WindowEvent e) { setVisible(false); }
	});
        
        pack();
        setResizable(false);
        setLocation(parent.getLocation().x + parent.getSize().width/2 - getSize().width/2,
                    parent.getLocation().y + parent.getSize().height/2 - getSize().height/2);
    }

    public void actionPerformed(ActionEvent e) {
        this.setVisible(false);
    }
    /**
     * Refreshes the player list component with information
     * from the game object.
     */
    public static void refreshPlayerList(List playerList, Client client) {
        playerList.removeAll();
        for(Enumeration e = client.getPlayers(); e.hasMoreElements();) {
            final Player player = (Player)e.nextElement();
            StringBuffer playerDisplay = new StringBuffer(player.getName());
            if (player.isGhost()) {
                playerDisplay.append(" [ghost]");
            } else if (player.isObserver()) {
                playerDisplay.append(" [observer]");
            } else if (player.isDone()) {
                playerDisplay.append(" (done)");
            }
            playerList.add(playerDisplay.toString());
        }
    }
    
    public void refreshPlayerList() {
        refreshPlayerList(playerList, client);
    }
}
