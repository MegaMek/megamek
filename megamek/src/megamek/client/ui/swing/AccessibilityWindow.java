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

import megamek.MMConstants;
import megamek.client.Client;
import megamek.client.commands.ClientCommand;
import megamek.client.ui.Messages;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.event.*;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.LinkedList;
import java.util.Objects;

public class AccessibilityWindow extends JDialog {
    private static final String CLEAN_HTML_REGEX = "<[^>]*>";
    public static final int MAX_HISTORY = 10;
    public static final String ACCESSIBLE_GUI_SHORTCUT = ".";

    private final Client client;
    private final ClientGUI gui;
    private final JTextArea chatArea = new JTextArea(" \n", 5, 40);
    private final JTextField inputField = new JTextField();
    private final LinkedList<String> history = new LinkedList<>();

    private Coords selectedTarget;
    private int historyBookmark = -1;

    public AccessibilityWindow(ClientGUI clientGUI) {
        super(clientGUI.getFrame(), Messages.getString("ClientGUI.ChatWindow"));
        client = Objects.requireNonNull(clientGUI.getClient());
        gui = clientGUI;
        client.getGame().addGameListener(gameListener);
        setLayout(new BorderLayout());

        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setFont(new Font(MMConstants.FONT_SANS_SERIF, Font.PLAIN, 12));
        var scrollPane = new JScrollPane(chatArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane, BorderLayout.CENTER);

        inputField.addKeyListener(keyListener);
        add(inputField, BorderLayout.SOUTH);
    }

    private void processAccessibleGUI() {
        final String[] args = inputField.getText().split(" ");
        if (args.length == 3) {
            try {
                selectedTarget = new Coords(Integer.parseInt(args[1]) - 1,
                        Integer.parseInt(args[2]) - 1);
            } catch (NumberFormatException e) {
                systemEvent("Couldn't parse coordinates.");
                return;
            }
            // Why don't constants work here?
            // Cursor over the hex.
            gui.getBoardView().mouseAction(selectedTarget, 3, InputEvent.BUTTON1_DOWN_MASK, MouseEvent.BUTTON1);
            // Click.
            gui.getBoardView().mouseAction(selectedTarget, 1, InputEvent.BUTTON1_DOWN_MASK, MouseEvent.BUTTON1);
        }
    }

    private void systemEvent(String s) {
        if (s != null) {
            chatArea.append(s + "\n");
        }
    }

    private String cleanHtml(String str) {
        return str.replaceAll(CLEAN_HTML_REGEX, "")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&");
    }

    /**
     * Tries to scroll down to the end of the box
     */
    private void moveToEnd() {
        if (chatArea.isShowing()) {
            int last = chatArea.getText().length() - 1;
            chatArea.select(last, last);
            chatArea.setCaretPosition(last);
        }
    }

    //region key listener
    private final KeyListener keyListener = new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent ev) {
            if (ev.getKeyCode() == KeyEvent.VK_ENTER) {
                history.addFirst(inputField.getText());
                historyBookmark = -1;

                if (inputField.getText().startsWith(ClientCommand.CLIENT_COMMAND)) {
                    systemEvent(gui.runCommand(inputField.getText()));
                } else if (inputField.getText().startsWith(ACCESSIBLE_GUI_SHORTCUT)) {
                    processAccessibleGUI();
                    systemEvent("Selected " + selectedTarget.toFriendlyString() + " in the GUI.");
                } else {
                    // default to running commands in the accesibility window, added a say command for chat.
                    systemEvent(gui.runCommand(ClientCommand.CLIENT_COMMAND + inputField.getText()));
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
    };

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
    //endregion

    //region game listener
    private final GameListener gameListener = new GameListenerAdapter() {

        @Override
        public void gamePlayerConnected(GamePlayerConnectedEvent e) {
            String name = (e != null) && (e.getPlayer() != null)
                    ? e.getPlayer().getName()
                    : "[Unknown]";
            systemEvent("New player has connected. Their name is " + name + ".");
        }

        @Override
        public void gamePlayerDisconnected(GamePlayerDisconnectedEvent e) {
            String name = (e != null) && (e.getPlayer() != null)
                    ? e.getPlayer().getName()
                    : "[Unknown]";
            systemEvent("The player " + name + " has disconnected.");
        }

        @Override
        public void gamePhaseChange(GamePhaseChangeEvent e) {
            systemEvent("Phase changed; it is now " + e.getNewPhase() + ".");
            if (client.phaseReport != null) {
                systemEvent(cleanHtml(client.phaseReport));
            }
        }

        @Override
        public void gameTurnChange(GameTurnChangeEvent e) {
            if ((e != null) && (e.getPlayer() != null)) {
                systemEvent("Turn changed; it is now " + e.getPlayer().getName() + "'s turn.");
            }
        }

        @Override
        public void gameReport(GameReportEvent e) {
            if (e != null) {
                systemEvent(e.getReport());
            }
        }

        @Override
        public void gameEnd(GameEndEvent e) {
            systemEvent("The game ended. Goodbye.");
        }

        @Override
        public void gameEntityNew(GameEntityNewEvent e) {
            if (e != null) {
                systemEvent("Added " + e.getNumberOfEntities() + " new entities;");
                try {
                    for (Entity ent : e.GetEntities()) {
                        String name = ent.getOwner() != null ? ent.getOwner().getName() : "UNNAMED";
                        systemEvent(name + " adds " + ent.getDisplayName());
                    }
                } catch (Exception ignored) {
                    // shouldn't happen but keep it from crashing the game
                }
            }
        }

        @Override
        public void gameEntityRemove(GameEntityRemoveEvent e) {
            if ((e != null) && (e.getEntity() != null)) {
                final Entity ent = e.getEntity();
                String name = (ent.getOwner() != null) ? ent.getOwner().getName() : "UNNAMED";
                systemEvent("Removed " + ent.getDisplayName() + " from player " + name + ".");
            }
        }

        @Override
        public void gameEntityChange(GameEntityChangeEvent e) {
            if ((e != null) && (e.getEntity() != null)) {
                systemEvent(e.toString());
            }
        }

        @Override
        public void gameNewAction(GameNewActionEvent e) {
            if ((e != null) && (e.getAction() != null)) {
                final Entity ent = client.getEntity(e.getAction().getEntityId());
                if (ent != null) {
                    String name = (ent.getOwner() != null) ? ent.getOwner().getName() : "[Unknown]";
                    try {
                        String actionText = ent.getDisplayName() + " from player " + name + " is doing " +
                                e.getAction().toAccessibilityDescription(client) + ".";
                        systemEvent(actionText);
                    } catch (Exception ex) {
                        LogManager.getLogger().warn("Couldn't obtain action accessibility description", ex);
                        systemEvent("An unknown action happened");
                    }
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
    };
    //endregion
}
