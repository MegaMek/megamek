package megamek.client.ui.swing;

import megamek.client.Client;
import megamek.client.ui.Messages;
import megamek.common.Entity;
import megamek.common.event.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.LinkedList;

public class AccessibilityWindow extends JDialog implements KeyListener {

    public static final int MAX_HISTORY = 10;
    Client client;

    JTextArea chatArea;
    private JTextField inputField;

    public LinkedList<String> history;
    public int historyBookmark = -1;

    public AccessibilityWindow(ChatterBox cb, ClientGUI clientgui) {
        super(clientgui.getFrame(), Messages.getString("ClientGUI.ChatWindow"));
        client = clientgui.getClient();
        client.getGame().addGameListener(new GameListenerAdapter() {
            @Override
            public void gamePlayerConnected(GamePlayerConnectedEvent e) {
                systemEvent("New player has connected. Their name is " + e.getPlayer().getName() + ".");
            }

            @Override
            public void gamePlayerDisconnected(GamePlayerDisconnectedEvent e) {
                systemEvent("The player " + e.getPlayer().getName() + " has disconnected.");
            }


            @Override
            public void gameEnd(GameEndEvent e) {
                systemEvent("The game ended. Goodbye.");
            }

            @Override
            public void gameEntityNew(GameEntityNewEvent e) {
                systemEvent("Added " + e.getNumberOfEntities() +  " new entities;" );
                for(Entity ent: e.GetEntities()) {
                    systemEvent(ent.getOwner().getName() + " adds " + ent.getDisplayName());
                }
            }

            @Override
            public void gameEntityNewOffboard(GameEntityNewOffboardEvent e) {
                systemEvent("Out of game event. (unneeded)" );
            }

            @Override
            public void gameEntityRemove(GameEntityRemoveEvent e) {
                final Entity ent = e.getEntity();
                systemEvent("Removed " + ent.getDisplayName() + " from player " + ent.getOwner().getName() + ".");
            }

            @Override
            public void gameEntityChange(GameEntityChangeEvent e) {
                final Entity ent = e.getEntity();
                systemEvent(e.toString());
            }

            @Override
            public void gameNewAction(GameNewActionEvent e) {
                final Entity ent = client.getEntity(e.getAction().getEntityId());
                systemEvent( ent.getDisplayName() + " from player " + ent.getOwner().getName() + " is doing " + e.getAction().toString() + ".");
            }

            @Override
            public void gameClientFeedbackRquest(GameCFREvent e) {
                systemEvent("New feedback event.");
            }

            @Override
            public void gameVictory(GameVictoryEvent e) {
                systemEvent("Game Victory! (unneeded.)");
            }
        });

        history = new LinkedList<String>();

        chatArea = new JTextArea(
                " \n", GUIPreferences.getInstance().getInt("AdvancedChatboxSize"), 40); //$NON-NLS-1$
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setFont(new Font("Sans Serif", Font.PLAIN, 12));
        inputField = new JTextField();

        Container cp = this.getContentPane();
        cp.setLayout(new BorderLayout());
        cp.add(chatArea, BorderLayout.CENTER);
        cp.add(inputField, BorderLayout.SOUTH);
        inputField.addKeyListener(this);

        this.setVisible(true);
    }

    private void systemEvent(String s) {
        chatArea.append(s + "\n");
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

    //
    // KeyListener
    //
    public void keyPressed(KeyEvent ev) {
        if (ev.getKeyCode() == KeyEvent.VK_ENTER) {
            history.addFirst(inputField.getText());
            historyBookmark = -1;

            if (!inputField.getText().startsWith(Client.CLIENT_COMMAND)) {
                client.sendChat(inputField.getText());
            } else {
                systemEvent(client.runCommand(inputField.getText()));
            }
            inputField.setText(""); //$NON-NLS-1$

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

    public void fetchHistory() {
        try {
            inputField.setText(history.get(historyBookmark));
        } catch (IndexOutOfBoundsException ioobe) {
            inputField.setText(""); //$NON-NLS-1$
            historyBookmark = -1;
        }
    }

    public void keyReleased(KeyEvent ev) {
        //ignored
    }

    public void keyTyped(KeyEvent ev) {
        //ignored
    }

}
