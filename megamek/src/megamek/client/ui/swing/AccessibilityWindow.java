/*
 * Copyright (c) 2019-2021 - The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.swing;

import megamek.client.Client;
import megamek.client.ui.swing.boardview.BoardView1;
import megamek.client.ui.Messages;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.event.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.util.LinkedList;

public class AccessibilityWindow extends JDialog implements KeyListener {
    private static final String cleanHtmlRegex = "<[^>]*>";
    public static final int MAX_HISTORY = 10;
    public static final String ACCESSIBLE_GUI_SHORTCUT = ".";

    Client client;
    ClientGUI gui;
    JTextArea chatArea;

    private Coords selectedTarget;
    private JTextField inputField;
    private LinkedList<String> history;
    private int historyBookmark = -1;

    public AccessibilityWindow(ClientGUI clientGUI) {
        super(clientGUI.getFrame(), Messages.getString("ClientGUI.ChatWindow"));
        client = clientGUI.getClient();
        gui = clientGUI;
        client.getGame().addGameListener(new GameListenerAdapter() {
            @Override
            public void gamePlayerConnected(GamePlayerConnectedEvent e) {
                String name = (e != null) && (e.getPlayer() != null)
                            ? e.getPlayer().getName()
                            : "UNNAMED";
                systemEvent("New player has connected. Their name is " + name + ".");
            }

            @Override
            public void gamePlayerDisconnected(GamePlayerDisconnectedEvent e) {
                String name = (e != null) && (e.getPlayer() != null)
                            ? e.getPlayer().getName()
                            : "UNNAMED";
                systemEvent("The player " + name + " has disconnected.");
            }

            @Override
            public void gamePhaseChange(GamePhaseChangeEvent e) {
                systemEvent("Phase changed it is now " + IGame.Phase.getDisplayableName(e.getNewPhase()) + ".");
                if (client.phaseReport != null) {
                    systemEvent(cleanHtml(client.phaseReport));
                }
            }

            @Override
            public void gameTurnChange(GameTurnChangeEvent e) {
                systemEvent("Turn changed it is now " + e.getPlayer().getName() + "'s turn.");
                //systemEvent(cleanHtml(client.roundReport));
            }

            @Override
            public void gameReport(GameReportEvent e) {
                systemEvent(e.getReport());
            }

            @Override
            public void gameEnd(GameEndEvent e) {
                systemEvent("The game ended. Goodbye.");
            }

            @Override
            public void gameBoardChanged(GameBoardChangeEvent e) {
            }

            @Override
            public void gameEntityNew(GameEntityNewEvent e) {
                if (e != null) {
                    systemEvent("Added " + e.getNumberOfEntities() +  " new entities;" );
                    for (Entity ent : e.GetEntities()) {
                        String name = ent.getOwner() != null ? ent.getOwner().getName() : "UNNAMED";
                        systemEvent(name + " adds " + ent.getDisplayName());
                    }
                }
            }

            @Override
            public void gameEntityNewOffboard(GameEntityNewOffboardEvent e) {
                //systemEvent("Out of game event. (unneeded)" );
            }

            @Override
            public void gameEntityRemove(GameEntityRemoveEvent e) {
                if ((e != null) && (e.getEntity() != null)) {
                    final Entity ent = e.getEntity();
                    String name = ent.getOwner() != null ? ent.getOwner().getName() : "UNNAMED";
                    systemEvent("Removed " + ent.getDisplayName() + " from player " + name + ".");
                }
            }

            @Override
            public void gameEntityChange(GameEntityChangeEvent e) {
                if ((e != null) && (e.getEntity() != null)) {
                    systemEvent(e.toString() );
                }
            }

            @Override
            public void gameNewAction(GameNewActionEvent e) {
                if ((e != null) && (e.getAction() != null)) {
                    final Entity ent = client.getEntity(e.getAction().getEntityId());
                    if (ent != null) {
                        String name = ent.getOwner() != null 
                                    ? ent.getOwner().getName() 
                                    : "UNNAMED";
                        systemEvent(ent.getDisplayName() + " from player " + name + " is doing " + e.getAction().toDisplayableString(client) + ".");
                    }
                }
            }

            @Override
            public void gameClientFeedbackRequest(GameCFREvent e) {
                systemEvent("New feedback event.");
            }

            @Override
            public void gameVictory(GameVictoryEvent e) {
                systemEvent("Game Victory! (unneeded.)");
            }
        });

        history = new LinkedList<>();

        setLayout(new BorderLayout());

        chatArea = new JTextArea(
                " \n", GUIPreferences.getInstance().getInt("AdvancedChatboxSize"), 40);
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setFont(new Font("Sans Serif", Font.PLAIN, 12));
        add(new JScrollPane(chatArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);

        inputField = new JTextField();
        inputField.addKeyListener(this);
        add(inputField, BorderLayout.SOUTH);
    }

    // Stolen in principle from the MapMenu.
    private void processAccessibleGUI() {
        final String[] args = inputField.getText().split(" ");
        if (args.length == 3) {
            selectedTarget = new Coords(Integer.parseInt(args[1]) - 1,
                    Integer.parseInt(args[2]) - 1);
            // Why don't constants work here?
            // Cursor over the hex.
            gui.bv.mouseAction(selectedTarget, 3, InputEvent.BUTTON1_DOWN_MASK, MouseEvent.BUTTON1);
            // Click.
            ((BoardView1) gui.getBoardView()).mouseAction(selectedTarget, 1, InputEvent.BUTTON1_DOWN_MASK, MouseEvent.BUTTON1);
        }
    }

    private void systemEvent(String s) {
        if (s != null) {
            chatArea.append(s + "\n");
        }
    }

    private String cleanHtml(String str) {
        str = str.replaceAll(cleanHtmlRegex, "");
        //replace &nbsp; with space
        str = str.replace("&nbsp;", " ");
        //replace &amp; with &
        str = str.replace("&amp;", "&");

        return str;
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

    //region Key Listener
    @Override
    public void keyPressed(KeyEvent ev) {
        if (ev.getKeyCode() == KeyEvent.VK_ENTER) {
            history.addFirst(inputField.getText());
            historyBookmark = -1;

            if (inputField.getText().startsWith(Client.CLIENT_COMMAND)) {
                systemEvent(client.runCommand(inputField.getText()));
            } else if (inputField.getText().startsWith(ACCESSIBLE_GUI_SHORTCUT)) {
                processAccessibleGUI();
                systemEvent("Selected " + selectedTarget.toFriendlyString() + " in the GUI.");
            } else {
                //default to running commands in the accesibility window, added a say command for chat.
                systemEvent(client.runCommand(Client.CLIENT_COMMAND + inputField.getText()));
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
        moveToEnd();
    }

    /**
     * Pull a bookmarked item from the history.
     */
    private void fetchHistory() {
        try {
            inputField.setText(history.get(historyBookmark));
        } catch (IndexOutOfBoundsException ignored) {
            inputField.setText("");
            historyBookmark = -1;
        }
    }

    @Override
    public void keyReleased(KeyEvent ev) {
        //ignored
    }

    @Override
    public void keyTyped(KeyEvent ev) {
        //ignored
    }
    //endregion Key Listener
}
