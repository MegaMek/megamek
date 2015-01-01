/*
 * MegaMek - Copyright (C) 2000,2001,2002,2003,2004 Ben Mazur (bmazur@sev.org)
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

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Component;
import java.awt.List;
import java.awt.Panel;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

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
    private TextField           inputField;
    private Button              butDone;
    
    public ChatterBox(ClientGUI clientgui) {
        this.client = clientgui.getClient();
        client.addGameListener(this);
        
        chatArea = new TextArea(" \n", 5, 40, TextArea.SCROLLBARS_VERTICAL_ONLY); //$NON-NLS-1$
        chatArea.setEditable(false);
        playerList = new List();
        inputField = new TextField();
        inputField.addKeyListener(this);
        butDone = new Button( Messages.getString("ChatterBox.ImDone") ); //$NON-NLS-1$
        butDone.setEnabled( false );

        chatPanel = new Panel(new BorderLayout());

        Panel subPanel = new Panel( new BorderLayout() );        
        subPanel.add(chatArea, BorderLayout.CENTER);
        subPanel.add(playerList, BorderLayout.WEST);
        subPanel.add(inputField, BorderLayout.SOUTH);
        chatPanel.add(subPanel, BorderLayout.CENTER);
        chatPanel.add(butDone, BorderLayout.EAST );
        
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
     * Returns the "box" component with all teh stuff
     */
    public Component getComponent() {
        return chatPanel;
    }

    /**
     * Display a system message in the chat box.
     *
     * @param   message the <code>String</code> message to be shown.
     */
    public void systemMessage( String message ) {
        chatArea.append("\nMegaMek: " + message); //$NON-NLS-1$
    }

    /**
     * Replace the "Done" button in the chat box.
     *
     * @param   button the <code>Button</code> that should be used for "Done".
     */
    public void setDoneButton( Button button ) {
        chatPanel.remove( butDone );
        butDone = button;
        chatPanel.add( butDone, BorderLayout.EAST );
    }
    
    //
    // GameListener
    //
    public void gamePlayerChat(GameEvent ev) {
        chatArea.append("\n" + ev.getMessage()); //$NON-NLS-1$
        PlayerListDialog.refreshPlayerList(playerList, client);
    }
    public void gamePlayerStatusChange(GameEvent ev) {
        PlayerListDialog.refreshPlayerList(playerList, client);
    }
    public void gameTurnChange(GameEvent ev) {
        PlayerListDialog.refreshPlayerList(playerList, client);
    }
    public void gamePhaseChange(GameEvent ev) {
        PlayerListDialog.refreshPlayerList(playerList, client);
    }
    public void gameNewEntities(GameEvent ev) {
        PlayerListDialog.refreshPlayerList(playerList, client);
    }
    public void gameNewSettings(GameEvent ev) {
        ;
    }
    
    
    //
    // KeyListener
    //
    public void keyPressed(KeyEvent ev) {
        if(ev.getKeyCode() == KeyEvent.VK_ENTER) {
            client.sendChat(inputField.getText());
            inputField.setText(""); //$NON-NLS-1$
        }
    }
    public void keyReleased(KeyEvent ev) {
        ;
    }
    public void keyTyped(KeyEvent ev) {
        ;
    }
    
    public void gameBoardChanged(GameEvent e) {
        ;
    }

    public void gameDisconnected(GameEvent e) {
        ;
    }

    public void gameEnd(GameEvent e) {
        ;
    }

    public void gameReport(GameEvent e) {
        ;
    }
    
    public void gameMapQuery(GameEvent e) {
        ;
    }

}
