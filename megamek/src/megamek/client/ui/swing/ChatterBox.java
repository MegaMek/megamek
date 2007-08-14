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
import megamek.client.commands.ClientCommand;
import megamek.client.commands.DeployCommand;
import megamek.client.commands.HelpCommand;
import megamek.client.commands.MoveCommand;
import megamek.client.commands.RulerCommand;
import megamek.client.commands.ShowEntityCommand;
import megamek.client.ui.IClientCommandHandler;
import megamek.common.event.GameEntityChangeEvent;
import megamek.common.event.GameEntityNewEvent;
import megamek.common.event.GameEntityRemoveEvent;
import megamek.common.event.GameListenerAdapter;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.event.GamePlayerChangeEvent;
import megamek.common.event.GamePlayerChatEvent;
import megamek.common.event.GameTurnChangeEvent;
import megamek.common.preference.PreferenceManager;

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
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;

/**
 * ChatterBox keeps track of a player list and a (chat) message
 * buffer.  Although it is not an AWT component, it keeps
 * one that it will gladly supply.
 */
public class ChatterBox implements KeyListener, IClientCommandHandler {
    private static final String CLIENT_COMMAND = "#";
    private static final int MAX_HISTORY = 10;
    private Client client;
    private Hashtable<String, ClientCommand> commandsHash = new Hashtable<String, ClientCommand>();

    private JPanel chatPanel;
    private JTextArea chatArea;
    private JList playerList;
    private JTextField inputField;
    private JButton butDone;
    
    private LinkedList<String> history;
    private int historyBookmark = -1;

    public ChatterBox(ClientGUI clientgui) {
        client = clientgui.getClient();
        client.game.addGameListener(new GameListenerAdapter() {
            public void gamePlayerChat(GamePlayerChatEvent e) {
                chatArea.append('\n' + e.getMessage()); //$NON-NLS-1$
                PlayerListDialog.refreshPlayerList(playerList, client);
                moveToEnd();
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
                if(PreferenceManager.getClientPreferences().getPrintEntityChange()) {
                    systemMessage(e.getNumberOfEntities() + " Entities added.");
                }
            }

            public void gameEntityRemove(GameEntityRemoveEvent e) {
                PlayerListDialog.refreshPlayerList(playerList, client);
            }
            
            public void gameEntityChange(GameEntityChangeEvent e) {
                if(PreferenceManager.getClientPreferences().getPrintEntityChange()) {
                    systemMessage(e.toString());
                }
            }
        });
        history = new LinkedList<String>();

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

        registerCommand(new DeployCommand(client));
        registerCommand(new HelpCommand(client, this));
        registerCommand(new MoveCommand(client));
        registerCommand(new RulerCommand(client));
        registerCommand(new ShowEntityCommand(client));
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
        moveToEnd();
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
            history.addFirst(inputField.getText());
            historyBookmark = -1;
            
            if(!inputField.getText().startsWith(CLIENT_COMMAND)) {
                client.sendChat(inputField.getText());
            } else {
                runCommand(inputField.getText());
            }
            inputField.setText(""); //$NON-NLS-1$
            
            if(history.size() > MAX_HISTORY) {
                history.removeLast();
            }
        } else if (ev.getKeyCode() == KeyEvent.VK_UP) {
            historyBookmark++;
            fetchHistory();
        } else if (ev.getKeyCode() == KeyEvent.VK_DOWN) {
            historyBookmark--;
            fetchHistory();
        }
        moveToEnd();
    }

    /**
     * 
     */
    private void fetchHistory() {
        try {
            inputField.setText(history.get(historyBookmark));
        } catch(IndexOutOfBoundsException ioobe) {
            inputField.setText(""); //$NON-NLS-1$
            historyBookmark = -1;
        }
    }

    public void keyReleased(KeyEvent ev) {
    }

    public void keyTyped(KeyEvent ev) {
    }

    /**
     * 
     * @param text a client command with CLIENT_COMMAND prepended.
     */
    public void runCommand(String cmd) {
        cmd = cmd.substring(CLIENT_COMMAND.length());
        
        runCommand(cmd.split("\\s+"));
    }
    
    /**
     * Runs the command
     * @param args the command and it's arguments with the CLIENT_COMMAND already removed, and the string tokenized.
     */
    public void runCommand(String[] args) {
        if(args != null && args.length > 0 && commandsHash.containsKey(args[0])) {
            systemMessage(commandsHash.get(args[0]).run(args));
        } else {
            systemMessage("Unknown Client Command.");
        }
    }
    
    /**
     * Registers a new command in the client command table
     */
    public void registerCommand(ClientCommand command) {
        commandsHash.put(command.getName(), command);
    }

    /**
     * Returns the command associated with the specified name
     */
    public ClientCommand getCommand(String name) {
        return commandsHash.get(name);
    }

    /* (non-Javadoc)
     * @see megamek.client.ui.IClientCommandHandler#getAllCommandNames()
     */
    public Enumeration<String> getAllCommandNames() {
        return commandsHash.keys();
    }

}
