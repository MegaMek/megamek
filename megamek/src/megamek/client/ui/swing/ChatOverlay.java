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

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.KeyboardFocusManager;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;

import megamek.client.ui.swing.util.HUD;
import megamek.client.ui.swing.util.InputForwarder;

/**
 * A panel that displays a ChatPane as an overlay of the board view.
 *
 * This class displays the chat pane with a mostly transparent,
 * "heads-up display style" that allow toggling its visibility.
 */
public class ChatOverlay extends JComponent {


    // Lays out and animates the overlay's slider
    private class SliderLayout implements LayoutManager {

        public void addLayoutComponent(String name, Component comp) {
        }

        public void removeLayoutComponent(Component comp) {
        }

        public Dimension preferredLayoutSize(Container parent) {
            var preferred = ChatOverlay.this.slider.getPreferredSize();
            var insets = parent.getInsets();
            preferred.width += insets.left + insets.right;
            preferred.height += insets.top + insets.bottom;
            return preferred;
        }

        public Dimension minimumLayoutSize(Container parent) {
            return preferredLayoutSize(parent);
        }

        public void layoutContainer(Container parent) {
            ChatOverlay.this.updatePreferredSize();

            var slider = ChatOverlay.this.slider;

            var parentSize = parent.getSize();
            var parentInsets = parent.getInsets();

            var sliderPreferred = slider.getPreferredSize();

            var x = parentInsets.left;
            var y = parentSize.height - parentInsets.bottom;

            var width = sliderPreferred.width;
            var maxWidth = parentSize.width - (parentInsets.left + parentInsets.right);
            if (width > maxWidth) {
                width = maxWidth;
            }

            var height = sliderPreferred.height;
            var maxHeight = parentSize.height - (parentInsets.top + parentInsets.bottom);
            if (height > maxHeight) {
                height = maxHeight;
            }

            slider.setBounds(x, y - height, width, height);
        }

    }

    private ChatPane chat;
    private Box slider;

    // this is initialised to the opposite of their init state
    // so the ctor can call their set methods to initialise related
    // state
    private boolean isActive = true;


    public ChatOverlay(ChatPane chat, Component board) {
        super();
        this.chat = chat;

        setLayout(new SliderLayout());
        setOpaque(false);

        HUD.applyHud(this.chat);
        add(this.chat);

        this.slider = new Box(BoxLayout.PAGE_AXIS);
        this.slider.setBackground(HUD.BACKGROUND_COLOR);
        this.slider.add(this.chat);
        add(this.slider);

        // When not active, forward input events targeted at the chat
        // entry list back to board view for processing so that
        // e.g. clicks pass through
        var scrollerRedirect = new InputForwarder(board) {
                protected void dispatch(InputEvent e) {
                    if (!ChatOverlay.this.isActive) {
                        super.dispatch(e);
                    }
                }
            };
        scrollerRedirect.forwardAllFrom(this.chat.getEntryScroller());

        this.chat.addChatListener((entry) -> chatChanged(entry));

        KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener(
            "permanentFocusOwner", (e) -> focusChanged()
        );

        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            "escape"
        );
        getActionMap().put(
            "escape",
            new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    setActive(false);
                }
            }
        );

        setActive(false);
    }

    public boolean isActive() {
        return this.isActive;
    }

    public void setActive(boolean isActive) {
        if (this.isActive != isActive) {
            this.isActive = isActive;
            this.chat.getEntryScroller().setVerticalScrollBarPolicy(
                isActive
                ? ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
                : ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER
            );
            this.slider.setOpaque(isActive);

            for (var entry: this.chat.getEntries()) {
                entry.setTransient(!isActive, 300);
            }

            if (isActive && !this.chat.isChatInputFocused()) {
                this.chat.focusChatInput();
            } else if (!isActive && this.chat.isChatInputFocused()) {
                transferFocusUpCycle();
            }
        }
    }

    private void chatChanged(ChatPane.Entry added) {
        if (!this.isActive) {
            added.setTransient(true, 5000);
        }
        revalidate();
    }

    private void updatePreferredSize() {
        var sliderPreferred = this.slider.getPreferredSize();
        sliderPreferred.width = 450;
        sliderPreferred.height = this.chat.getPreferredSize().height;

        this.slider.setPreferredSize(sliderPreferred);
    }

    private void focusChanged() {
        var manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        if (manager.getFocusedWindow() == getTopLevelAncestor()) {
            var focused = manager.getPermanentFocusOwner();
            if (focused != null) {
                setActive(isAncestorOf(focused));
            }
        }
    }

}
