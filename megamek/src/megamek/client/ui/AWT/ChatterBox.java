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

package megamek.client;

import java.awt.*;
import java.awt.event.*;
import java.util.Enumeration;

import megamek.common.*;

/**
 * ChatterBox keeps track of a player list and a (chat) message
 * buffer.  Although it is not an AWT component, it keeps
 * one that it will gladly supply.
 */
public class ChatterBox
implements GameListener, KeyListener {
    public Client client;
    
    public String[]            chatBuffer;
    
    // AWT components
    public Panel chatPanel;
    private TextArea            chatArea;
    private List                playerList;
    private TextField        inputField;
    
    public ChatterBox(Client client) {
        this.client = client;
        client.addGameListener(this);
        
        chatArea = new TextArea(" \n", 5, 40, TextArea.SCROLLBARS_VERTICAL_ONLY);
        chatArea.setEditable(false);
        playerList = new List();
        inputField = new TextField();
        inputField.addKeyListener(this);
        
        chatPanel = new Panel(new BorderLayout());
        
        chatPanel.add(chatArea, BorderLayout.CENTER);
        chatPanel.add(playerList, BorderLayout.WEST);
        chatPanel.add(inputField, BorderLayout.SOUTH);
        
    }
    
    /**
     * Tries to scroll down to the end of the box
     */
    public void moveToEnd() {
        if (chatArea.isShowing()) {
            int last = chatArea.getText().length() - 1;
            chatArea.select(last, last);
            chatArea.setCaretPosition(last);
        }
    }
    
    /**
     * Refreshes the player list component with information
     * from the game object.
     */
    public void refreshPlayerList() {
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
    
    /**
     * Returns the "box" component with all teh stuff
     */
    public Component getComponent() {
        return chatPanel;
    }
    
    public void systemMessage(String s) {
        chatArea.append("\nMegaMek: " + s);
    }
    
    //
    // GameListener
    //
    public void gamePlayerChat(GameEvent ev) {
        chatArea.append("\n" + ev.getMessage());
        refreshPlayerList();
    }
    public void gamePlayerStatusChange(GameEvent ev) {
        refreshPlayerList();
    }
    public void gameTurnChange(GameEvent ev) {
        refreshPlayerList();
    }
    public void gamePhaseChange(GameEvent ev) {
        refreshPlayerList();
    }
    public void gameNewEntities(GameEvent ev) {
        refreshPlayerList();
    }
    public void gameNewSettings(GameEvent ev) {
        ;
    }
    
    
    //
    // KeyListener
    //
    public void keyPressed(KeyEvent ev) {
        if(ev.getKeyCode() == ev.VK_ENTER) {
            client.sendChat(inputField.getText());
            inputField.setText("");
        }
    }
    public void keyReleased(KeyEvent ev) {
        ;
    }
    public void keyTyped(KeyEvent ev) {
        ;
    }
    
}
