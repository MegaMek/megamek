/*
 * MegaMek - Copyright (C) 2000-2004 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.client.ui.swing;

import megamek.client.Client;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.event.*;
import megamek.common.preference.IPreferenceChangeListener;
import megamek.common.preference.PreferenceChangeEvent;
import megamek.common.preference.PreferenceManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.LinkedList;

/**
 * ChatterBox keeps track of a player list and a (chat) message buffer. Although
 * it is not an AWT component, it keeps one that it will gladly supply.
 */
public class ChatterBox implements KeyListener, IPreferenceChangeListener {
    public static final int MAX_HISTORY = 10;
    Client client;

    private JPanel chatPanel;
    JTextArea chatArea;
    JList<String> playerList;
    JScrollPane scrPlayers;
    private JTextField inputField;
    private JButton butDone;
    private JSplitPane playerChatSplit;

    public LinkedList<String> history;
    public int historyBookmark = -1;
    protected static final GUIPreferences GUIP = GUIPreferences.getInstance();
    private ChatterBox2 cb2;

    private static final String CB_KEY_ADVANCED_CHATBOXSIZE = "AdvancedChatboxSize";

    public ChatterBox(ClientGUI clientgui) {
        client = clientgui.getClient();
        client.getGame().addGameListener(new GameListenerAdapter() {
            @Override
            public void gamePlayerChat(GamePlayerChatEvent e) {
                chatArea.append('\n' + e.getMessage());
                PlayerListDialog pld = clientgui.getPlayerListDialog();
                if (pld != null) {
                    pld.refreshPlayerList(playerList, client);
                }
                moveToEnd();
            }

            @Override
            public void gamePlayerChange(GamePlayerChangeEvent e) {
                PlayerListDialog pld = clientgui.getPlayerListDialog();
                if (pld != null) {
                    pld.refreshPlayerList(playerList, client);
                }
            }

            @Override
            public void gameTurnChange(GameTurnChangeEvent e) {
                PlayerListDialog pld = clientgui.getPlayerListDialog();
                if (pld != null) {
                    pld.refreshPlayerList(playerList, client);
                }
            }

            @Override
            public void gamePhaseChange(GamePhaseChangeEvent e) {
                PlayerListDialog pld = clientgui.getPlayerListDialog();
                if (pld != null) {
                    pld.refreshPlayerList(playerList, client);
                }
            }

            @Override
            public void gameEntityNew(GameEntityNewEvent e) {
                PlayerListDialog pld = clientgui.getPlayerListDialog();
                if (pld != null) {
                    pld.refreshPlayerList(playerList, client);
                }

                if (PreferenceManager.getClientPreferences().getPrintEntityChange()) {
                    systemMessage(e.getNumberOfEntities() + " " + Messages.getString("ChatterBox.entitiesAdded"));
                }
            }

            @Override
            public void gameEntityRemove(GameEntityRemoveEvent e) {
                PlayerListDialog pld = clientgui.getPlayerListDialog();
                if (pld != null) {
                    pld.refreshPlayerList(playerList, client);
                }
            }

            @Override
            public void gameEntityChange(GameEntityChangeEvent e) {
                if (PreferenceManager.getClientPreferences().getPrintEntityChange()) {
                    systemMessage(e.toString());
                }
            }
        });
        history = new LinkedList<>();

        chatArea = new JTextArea(" \n", GUIPreferences.getInstance().getInt(CB_KEY_ADVANCED_CHATBOXSIZE), 40);
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        playerList = new JList<>(new DefaultListModel<>());
        playerList.setVisibleRowCount(GUIPreferences.getInstance().getInt(CB_KEY_ADVANCED_CHATBOXSIZE));
        scrPlayers = new JScrollPane(playerList);
        scrPlayers.setPreferredSize(new Dimension(250, chatArea.getHeight()));
        inputField = new JTextField(Messages.getString("ChatLounge.ChatPlaceholder"));
        inputField.addKeyListener(this);
        inputField.addFocusListener(new FocusListener() {
            private final String chatPlaceholder = Messages.getString("ChatLounge.ChatPlaceholder");
            @Override
            public void focusGained(FocusEvent e) {
                if (inputField.getText().equals(chatPlaceholder)) {
                    inputField.setText("");
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (inputField.getText().equals("")) {
                    inputField.setText(chatPlaceholder);
                }
            }
        });
        butDone = new JButton(Messages.getString("ChatterBox.ImDone"));
        butDone.setEnabled(false);

        chatPanel = new JPanel(new BorderLayout());
        chatPanel.setLayout(new GridBagLayout());

        playerChatSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true,
                scrPlayers, new JScrollPane(chatArea));
        playerChatSplit.setResizeWeight(0.01);
        
        JPanel subPanel = new JPanel(new BorderLayout());
        subPanel.setPreferredSize(new Dimension(284, 80));
        subPanel.setMinimumSize(new Dimension(284, 80));
        subPanel.add(playerChatSplit, BorderLayout.CENTER);
        subPanel.add(inputField, BorderLayout.SOUTH);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = gbc.gridy = 0;
        gbc.gridheight = 3; gbc.gridwidth = 5;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = gbc.weighty = 1;

        chatPanel.add(subPanel, gbc);

        gbc.gridx = 5; gbc.gridy = 1;
        gbc.gridheight = 1; gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = .13; gbc.weighty = .05;
        chatPanel.add(butDone, gbc);
        butDone.setSize(UIUtil.scaleForGUI(AbstractPhaseDisplay.DONE_BUTTON_WIDTH), butDone.getHeight());
        butDone.setPreferredSize(butDone.getSize());
        butDone.setMinimumSize(butDone.getSize());
        chatPanel.setMinimumSize(chatPanel.getPreferredSize());

        adaptToGUIScale();
        GUIP.addPreferenceChangeListener(this);
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
     * Returns the "box" component with all the stuff
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
        chatArea.append("\n" + Messages.getString("ChatterBox.MegaMek") + " " + message);
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

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 5; gbc.gridy = 1;
        gbc.gridheight = 1; gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = .1; gbc.weighty = .05;

        chatPanel.add(butDone, gbc);
    }

    //
    // KeyListener
    //
    @Override
    public void keyPressed(KeyEvent ev) {
        if (ev.getKeyCode() == KeyEvent.VK_ENTER) {
            history.addFirst(inputField.getText());
            historyBookmark = -1;

            if (!inputField.getText().startsWith(Client.CLIENT_COMMAND)) {
                client.sendChat(inputField.getText());
            } else {
                systemMessage(client.runCommand(inputField.getText()));
            }
            inputField.setText("");

            if (history.size() > MAX_HISTORY) {
                history.removeLast();
            }
        } else if (ev.getKeyCode() == KeyEvent.VK_UP) {
            historyBookmark++;
            fetchHistory();
        } else if (ev.getKeyCode() == KeyEvent.VK_DOWN) {
            historyBookmark--;
            fetchHistory();
        }
        cb2.setMessage(inputField.getText()+ev.getKeyChar());
        moveToEnd();
    }

    public void fetchHistory() {
        try {
            inputField.setText(history.get(historyBookmark));
            cb2.setMessage(inputField.getText());
        } catch (IndexOutOfBoundsException ioobe) {
            inputField.setText("");
            cb2.setMessage("");
            historyBookmark = -1;
        }
    }

    @Override
    public void keyReleased(KeyEvent ev) {
        // ignored
    }

    @Override
    public void keyTyped(KeyEvent ev) {
        // ignored
    }

    public void setMessage(String message) {
        inputField.setText(message);
    }

    public void setChatterBox2(ChatterBox2 cb2) {
        this.cb2 = cb2;
    }

    private void adaptToGUIScale() {
        UIUtil.adjustContainer(chatPanel, UIUtil.FONT_SCALE1);
        UIUtil.adjustContainer(butDone, UIUtil.FONT_SCALE1);
    }

    @Override
    public void preferenceChange(PreferenceChangeEvent e) {
        switch (e.getName()) {
            case GUIPreferences.GUI_SCALE:
                adaptToGUIScale();
                break;

        }
    }
}
