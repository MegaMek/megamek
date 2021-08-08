/*
 * Copyright (C) 2021 - The MegaMek Team. All Rights Reserved.
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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.LinkedList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import megamek.client.Client;
import megamek.common.event.GameEntityChangeEvent;
import megamek.common.event.GameEntityNewEvent;
import megamek.common.event.GameListenerAdapter;
import megamek.common.event.GamePlayerChatEvent;
import megamek.common.preference.PreferenceManager;
import megamek.client.ui.swing.util.ColorAnimator;
import megamek.client.ui.swing.util.HUD;
import megamek.client.ui.swing.util.InputForwarder;
import megamek.client.ui.swing.util.UIUtil;


/**
 * A chat interface and game status report display.
 */
public class ChatPane extends JPanel {


    /** Listener interfaces for classes interested in chat events. */
    public interface ChatListener {

        void chatEntryAdded(Entry added);

    }

    public class Entry extends Box {


        private JTextArea message;
        private InputForwarder messageForwarder;
        private ColorAnimator animator = null;
        private boolean isTransient = false;


        public Entry(String text) {
            super(BoxLayout.PAGE_AXIS);
            setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

            this.messageForwarder = new InputForwarder(ChatPane.this.entryScroller);

            this.message = new JTextArea(text);
            this.message.setAlignmentX(0.0f);
            this.message.setEditable(false);
            this.message.setLineWrap(true);
            this.message.setWrapStyleWord(true);
            this.message.setForeground(getForeground());
            this.message.setOpaque(false);
            add(this.message);
        }

        public void setTransient(boolean isTransient, int intervalMillis) {
            if (this.isTransient != isTransient) {
                this.isTransient = isTransient;
                float targetAlpha;

                if (isTransient) {
                    this.message.setFocusable(false);
                    this.messageForwarder.forwardAllFrom(this.message);
                    targetAlpha = 0.0f;
                } else {
                    this.message.setFocusable(true);
                    this.messageForwarder.unforwardAllFrom(this.message);
                    targetAlpha = 1.0f;
                }

                if (this.animator != null) {
                    this.animator.cancel();
                    this.animator = null;
                    ChatPane.this.animationsRunning--;
                }

                var initial = message.getForeground();
                var comps = initial.getComponents(null);
                var clock = ChatPane.this.animationClock;
                this.animator = new ColorAnimator(
                    intervalMillis,
                    clock,
                    initial,
                    new Color(comps[0], comps[1], comps[2], targetAlpha),
                    (c) -> tick(c)
                );
                ChatPane.this.animationsRunning++;

                if (!clock.isRunning()) {
                    clock.start();
                }
            }
        }

        @Override
        public void setForeground(Color color) {
            if (this.message != null) {
                this.message.setForeground(color);
            }
            super.setForeground(color);
        }

        private void tick(Color color) {
            if (this.message != null) {
                this.message.setForeground(color);
            }
            if (this.animator.isComplete()) {
                this.animator = null;
                ChatPane.this.animationsRunning--;
            }
        }

    }


    private final static String FILENAME_BUTTON_SEND = "paper-plane-symbolic.png"; //$NON-NLS-1$


    private Client client;

    private Action sendAction;
    private List<ChatListener> chatListeners = new LinkedList<>();

    private Box entryList;
    private JScrollPane entryScroller;

    private JPanel chatControls;
    private JTextArea chatInput;
    private JButton chatSend;

    private Timer animationClock;
    private int animationsRunning = 0;


    public ChatPane(Client client) {
        super(new BorderLayout());
        this.client = client;
        this.client.getGame().addGameListener(new GameListenerAdapter() {
                @Override
                public void gamePlayerChat(GamePlayerChatEvent e) {
                    receivedChat(e);
                }
                @Override
                public void gameEntityNew(GameEntityNewEvent e) {
                    entityAdded(e);
                    if (PreferenceManager.getClientPreferences()
                        .getPrintEntityChange()) {
                        addMessage("MegaMek: " + e.getNumberOfEntities() +
                                   " Entities added.");
                    }
                }
                @Override
                public void gameEntityChange(GameEntityChangeEvent e) {
                    entityChanged(e);
                    if (PreferenceManager.getClientPreferences()
                        .getPrintEntityChange()) {
                        addMessage("Megamek: " + e.toString());
                    }
                }
            });

        this.animationClock = new Timer(100, (e) -> tick());

        this.sendAction = new AbstractAction("Send") {
                public void actionPerformed(ActionEvent e) {
                    sendChat();
                }
            };
        this.sendAction.putValue(Action.SHORT_DESCRIPTION, "Send chat message");
        this.sendAction.putValue(Action.SMALL_ICON, UIUtil.loadWidgetIcon(FILENAME_BUTTON_SEND, 16));

        // Entry widgets

        this.entryList = new Box(BoxLayout.PAGE_AXIS);

        this.entryScroller = new JScrollPane(
            this.entryList,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        );

        // Chat widgets

        // This uses a single line JTextArea rather than a JTextField
        // since the latter can't always be themed with some PLAF's.
        // See: https://bugs-stage.openjdk.java.net/browse/JDK-6527149
        this.chatInput = new JTextArea();
        this.chatInput.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        this.chatInput.putClientProperty(HUD.BORDER_PROPERTY, SwingConstants.LEADING);
        this.chatInput.setToolTipText("Enter a chat message");
        this.chatInput.setLineWrap(true);
        this.chatInput.setWrapStyleWord(true);
        this.chatInput.getDocument().addDocumentListener(new DocumentListener() {
                public void changedUpdate​(DocumentEvent e) {
                    // noop
                }
                public void insertUpdate​(DocumentEvent e) {
                    updateSendState();
                }
                public void removeUpdate​(DocumentEvent e) {
                    updateSendState();
                }
            });
        // Restore standard <Enter> behaviour of JTextFields
        this.chatInput.getInputMap(WHEN_FOCUSED).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "send"
        );
        this.chatInput.getActionMap().put("send", this.sendAction);
        // Restore standard <Tab> behaviour of JTextFields
        this.chatInput.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERS‌​AL_KEYS, null);
        this.chatInput.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERS‌​AL_KEYS, null);
        this.chatInput.setFocusTraversalKeys(KeyboardFocusManager.UP_CYCLE_TRAVERSAL_KEYS, null);

        this.chatSend = new JButton(this.sendAction);
        this.chatSend.setHideActionText(true);
        this.chatSend.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        this.chatControls = new JPanel(new BorderLayout());
        this.chatControls.add(this.chatInput, BorderLayout.CENTER);
        this.chatControls.add(chatSend, BorderLayout.LINE_END);

        add(this.entryScroller, BorderLayout.CENTER);
        add(this.chatControls, BorderLayout.SOUTH);

        updateSendState();
    }

    public JPanel getChatControls() {
        return this.chatControls;
    }

    public JTextArea getChatInput() {
        return this.chatInput;
    }

    public JScrollPane getEntryScroller() {
        return this.entryScroller;
    }

    public void addMessage(String text) {
        addEntry(new Entry(text));
    }

    public List<Entry> getEntries() {
        var entries = new LinkedList<Entry>();
        var count = this.entryList.getComponentCount();
        for (int i = 0; i < count; i++) {
            entries.add((Entry) this.entryList.getComponent(i));
        }
        return entries;
    }

    public void focusChatInput() {
        this.chatInput.grabFocus();
        this.chatInput.selectAll();
    }

    public boolean isChatInputFocused() {
        return this.chatInput.isFocusOwner();
    }

    public void addChatListener(ChatListener listener) {
        this.chatListeners.add(listener);
    }

    public void removeChatListener(ChatListener listener) {
        this.chatListeners.remove(listener);
    }

    @Override
    public void setForeground(Color color) {
        if (this.chatSend != null) {
            this.chatSend.setForeground(color);
        }
        super.setForeground(color);
    }

    private void addEntry(Entry entry) {
        entry.setForeground(getForeground());
        this.entryList.add(entry);
        for (var listener: this.chatListeners) {
            listener.chatEntryAdded(entry);
        }

        // Revalidate is needed to get scrolled pane to show its
        // scrollbars when first needed, but the update is queued on
        // the EDT. So need to also dispatch the scroll-to on the EDT
        // so it is executed after the scrolled pane is updated.
        revalidate();
        SwingUtilities.invokeLater(
            () -> entry.scrollRectToVisible(entry.getBounds())
        );
    }

    private void entityAdded(GameEntityNewEvent e) {
        if (PreferenceManager.getClientPreferences().getPrintEntityChange()) {
            addMessage("MegaMek: " + e.getNumberOfEntities() +
                       " Entities added.");
        }
    }

    private void entityChanged(GameEntityChangeEvent e) {
        if (PreferenceManager.getClientPreferences().getPrintEntityChange()) {
            addMessage("Megamek: " + e.toString());
        }
    }

    private void receivedChat(GamePlayerChatEvent e) {
        addMessage(e.getMessage());
    }

    private void sendChat() {
        var text = this.chatInput.getText().trim();
        if (text.length() > 0) {
            this.client.sendChat(text);
            this.chatInput.setText(""); //$NON-NLS-1$
        }
        focusChatInput();
    }

    private void updateSendState() {
        this.sendAction.setEnabled(!this.chatInput.getText().trim().isEmpty());
    }

    private void tick() {
        if (this.animationsRunning == 0) {
            this.animationClock.stop();
        }
    }

}
