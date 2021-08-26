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

package megamek.client.ui.swing.widget;

import java.awt.BorderLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import megamek.client.ui.swing.util.UIUtil;

/**
 * Holds a component that can be collapsed and detached into a
 * separate window.
 *
 * When constructed, the content component will be added to this
 * pane. When detached, it will be re-parented to a pop-up
 * window. When re-attached, it will be re-parented back to this pane.
 *
 * When attached, a header pane will be displayed that contains the
 * given human-readable title and a detach button. When detached, the
 * window will use the header in place of the normal window controls,
 * with an attach button.
 */
public class DetachablePane extends JComponent {


    private class ActionButton extends JButton {

        private ActionButton(Action action) {
            super(action);
            setMargin(new Insets(2, 2, 2, 2));
            setHideActionText(true);
        }

        @Override
        public void setEnabled(boolean enabled) {
            this.setVisible(enabled);
            super.setEnabled(enabled);
        }

    }

    private final static String FILENAME_ATTACH = "step-in-symbolic.png"; //$NON-NLS-1$
    private final static String FILENAME_DETACH = "detach-symbolic.png"; //$NON-NLS-1$

    /** Defines the different modes the pane may be in. */
    public enum Mode {
        EXPANDED,
        // XXX Not yet implemented
        // COLLAPSED,
        DETACHED;
    }

    // Default to detached so that when explicitly updating the state
    // in the ctor, the effects are applied.
    private Mode state = Mode.DETACHED;

    private Action detach;
    private Action attach;

    // The component that gets moved between this base pane and the
    // window
    private Box root;

    private JLabel title;
    private JPanel header;
    private JComponent content;
    private JFrame window;

    private JButton detachButton;
    private JButton attachButton;


    public DetachablePane(String title, JComponent content) {
        this.setLayout(new BorderLayout());

        this.title = new JLabel();
        this.title.setAlignmentX(0.0f);

        this.detach = new AbstractAction("Detach") {
                public void actionPerformed(ActionEvent e) {
                    detachPane();
                }
            };
        this.detach.putValue(Action.SHORT_DESCRIPTION, "Detach this pane");
        this.detach.putValue(Action.SMALL_ICON, UIUtil.loadWidgetIcon(FILENAME_DETACH, 16));

        this.attach = new AbstractAction("Attach") {
                public void actionPerformed(ActionEvent e) {
                    attachPane();
                }
            };
        this.attach.putValue(Action.SHORT_DESCRIPTION, "Attach this pane");
        this.attach.putValue(Action.SMALL_ICON, UIUtil.loadWidgetIcon(FILENAME_ATTACH, 16));

        var buttons = Box.createHorizontalBox();
        buttons.add(new ActionButton(this.attach));
        buttons.add(new ActionButton(this.detach));

        this.header = new JPanel(new BorderLayout(0, 8));
        this.header.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        this.header.add(this.title, BorderLayout.CENTER);
        this.header.add(buttons, BorderLayout.EAST);

        this.content = content;

        this.root = Box.createVerticalBox();
        this.root.add(this.header);
        this.root.add(this.content);

        this.window = new JFrame();
        this.window.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing​(WindowEvent e) {
                    attachPane();
                }
            });

        setTitle(title);
        setState(Mode.EXPANDED);
    }

    /**
     * Detaches the pane, with animation.
     */
    public void detachPane() {
        setState(Mode.DETACHED);
    }

    /**
     * Attaches the pane, with animation.
     */
    public void attachPane() {
        setState(Mode.EXPANDED);
    }

    /**
     * Returns the current state of the pane.
     */
    public Mode getState() {
        return this.state;
    }

    /**
     * Sets the current state of the pane, with no animation.
     */
    public void setState(Mode newState) {
        if (this.state != newState) {
            switch (newState) {
                case EXPANDED:
                    this.attach.setEnabled(false);
                    this.detach.setEnabled(true);

                    this.window.setVisible(false);
                    this.window.remove(this.root);

                    add(this.root, BorderLayout.CENTER);
                    revalidate();
                    super.setVisible(true);

                    break;

                case DETACHED:
                    this.attach.setEnabled(true);
                    this.detach.setEnabled(false);

                    remove(this.root);
                    revalidate();
                    super.setVisible(false);

                    this.window.add(this.root, BorderLayout.CENTER);
                    this.window.pack();
                    this.window.setVisible(true);

                    break;
            }
            this.state = newState;
        }
    }

    /**
     * Returns the human-readable title of the pane.
     */
    public String getTitle() {
        return this.title.getText();
    }

    /**
     * Returns the human-readable title of the pane.
     */
    public void setTitle(String title) {
        this.title.setText(title);
        this.title.setToolTipText(title);
        this.window.setTitle(title);
    }

    /**
     * Returns the content component of the pane.
     */
    public JComponent getContent() {
        return this.content;
    }

    /**
     * Returns the window that displays the content pane when detached.
     */
    public JFrame getWindow() {
        return this.window;
    }

    @Override
    public void setVisible(boolean visible) {
        if (this.state == Mode.DETACHED) {
            this.window.setVisible(visible);
        } else {
            super.setVisible(visible);
        }
    }

}
