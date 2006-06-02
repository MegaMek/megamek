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

package megamek.client.ui.swing;

import megamek.client.Client;
import megamek.common.event.GameEntityNewEvent;
import megamek.common.event.GameEntityRemoveEvent;
import megamek.common.event.GameListenerAdapter;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.event.GamePlayerChangeEvent;
import megamek.common.event.GamePlayerChatEvent;
import megamek.common.event.GameTurnChangeEvent;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/**
 * ChatterBox keeps track of a player list and a (chat) message
 * buffer.  Although it is not an AWT component, it keeps
 * one that it will gladly supply.
 */
public class ChatterBox implements KeyListener {
    private Client client;

    private JPanel chatPanel;
    private JTextArea chatArea;
    private JList playerList;
    private JTextField inputField;
    private JButton butDone;

    public ChatterBox(ClientGUI clientgui) {
        client = clientgui.getClient();
        client.game.addGameListener(new GameListenerAdapter() {
            public void gamePlayerChat(GamePlayerChatEvent e) {
                chatArea.append('\n' + e.getMessage()); //$NON-NLS-1$
                PlayerListDialog.refreshPlayerList(playerList, client);
            }

            public void gamePlayerChange(GamePlayerChangeEvent e) {
                PlayerListDialog.refreshPlayerList(playerList, client);
            }

            public void gameTurnChange(GameTurnChangeEvent e) {
                PlayerListDialog.refreshPlayerList(playerList, client);
            }

            public void gamePhaseChange(GamePhaseChangeEvent e) {
                PlayerListDialog.refreshPlayerList(playerList, client);
            }

            public void gameEntityNew(GameEntityNewEvent e) {
                PlayerListDialog.refreshPlayerList(playerList, client);
            }

            public void gameEntityRemove(GameEntityRemoveEvent e) {
                PlayerListDialog.refreshPlayerList(playerList, client);
            }
        });

        chatArea = new JTextArea(" \n", GUIPreferences.getInstance().getInt("AdvancedChatboxSize"), 40); //$NON-NLS-1$
        chatArea.setEditable(false);
        playerList = new JList(new DefaultListModel());
        playerList.setVisibleRowCount(GUIPreferences.getInstance().getInt("AdvancedChatboxSize"));
        inputField = new JTextField();
        inputField.addKeyListener(this);
        butDone = new JButton(Messages.getString("ChatterBox.ImDone")); //$NON-NLS-1$
        butDone.setEnabled(false);

        chatPanel = new JPanel(new BorderLayout());

        JPanel subPanel = new JPanel(new BorderLayout());
        subPanel.add(new JScrollPane(chatArea), BorderLayout.CENTER);
        subPanel.add(playerList, BorderLayout.WEST);
        subPanel.add(inputField, BorderLayout.SOUTH);
        chatPanel.add(subPanel, BorderLayout.CENTER);
        chatPanel.add(butDone, BorderLayout.EAST);

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
    public JComponent getComponent() {
        return chatPanel;
    }

    /**
     * Display a system message in the chat box.
     *
     * @param message the <code>String</code> message to be shown.
     */
    public void systemMessage(String message) {
        chatArea.append("\nMegaMek: " + message); //$NON-NLS-1$
    }

    /**
     * Replace the "Done" button in the chat box.
     *
     * @param button the <code>JButton</code> that should be used for "Done".
     */
    public void setDoneButton(JButton button) {
        chatPanel.remove(butDone);
        butDone = button;
        chatPanel.add(butDone, BorderLayout.EAST);
    }

    //
    // KeyListener
    //
    public void keyPressed(KeyEvent ev) {
        if (ev.getKeyCode() == KeyEvent.VK_ENTER) {
            client.sendChat(inputField.getText());
            inputField.setText(""); //$NON-NLS-1$
        }
    }

    public void keyReleased(KeyEvent ev) {
    }

    public void keyTyped(KeyEvent ev) {
    }

}
