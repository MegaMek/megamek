/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import megamek.common.Messages;

/**
 * Custom component to represent a tab with a title and a close button
 */
@SuppressWarnings("UnnecessaryUnicodeEscape") // It's necessary or the encoding breaks on some systems
public class CloseableTab extends JPanel {
    private final Component component;
    private EnhancedTabbedPane parentPane;
    private final JLabel titleLabel;
    private final JButton closeButton;
    private boolean isDirty = false;
    final private String closeButtonText = "\u00D7"; // X

    /**
     * Creates a new closeable tab with the specified title
     *
     * @param parentPane The parent tabbed pane
     * @param title      The title to display
     * @param component  The component associated with this tab
     */
    public CloseableTab(EnhancedTabbedPane parentPane, String title, Component component) {
        super(new BorderLayout(0, 0));
        this.parentPane = parentPane;
        this.component = component;
        setOpaque(false);

        // Create the title label
        titleLabel = new JLabel(title);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 6));

        // Add components to panel
        add(titleLabel, BorderLayout.WEST);
        closeButton = createCloseButton();
        add(closeButton, BorderLayout.EAST);

        MouseAdapter tabEventForwarder = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                forwardEventToTabbedPane(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                forwardEventToTabbedPane(e);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                forwardEventToTabbedPane(e);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                forwardEventToTabbedPane(e);
            }

            /**
             * Forwards a mouse event to the parent tabbed pane
             */
            private void forwardEventToTabbedPane(MouseEvent e) {
                // Find the parent tabbed pane
                Container parent = CloseableTab.this.getParent();
                while (parent != null && !(parent instanceof JTabbedPane)) {
                    parent = parent.getParent();
                }

                if (parent instanceof JTabbedPane tabbedPane) {

                    // Convert to tabbed pane coordinates
                    Point tabPanePoint = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), tabbedPane);

                    // Create a new event at the tabbed pane location
                    MouseEvent convertedEvent = new MouseEvent(tabbedPane,
                          e.getID(),
                          e.getWhen(),
                          e.getModifiersEx(),
                          tabPanePoint.x,
                          tabPanePoint.y,
                          e.getClickCount(),
                          e.isPopupTrigger(),
                          e.getButton());

                    tabbedPane.dispatchEvent(convertedEvent);
                }
            }
        };
        titleLabel.addMouseListener(tabEventForwarder);
        titleLabel.addMouseMotionListener(tabEventForwarder);
    }

    private JButton createCloseButton() {
        JButton closeButton = new JButton(closeButtonText);
        closeButton.setFont(closeButton.getFont().deriveFont(Font.BOLD, 18f));
        closeButton.setPreferredSize(new Dimension(18, 18));
        closeButton.setToolTipText(Messages.getString("EnhancedTabbedPane.closeButton.tooltip"));
        closeButton.setContentAreaFilled(false);
        closeButton.setBorder(BorderFactory.createEmptyBorder(-3, 0, 0, 2));
        closeButton.setBorderPainted(false);
        closeButton.setFocusable(false);

        final Color originalColor = closeButton.getForeground();
        closeButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                closeButton.setForeground(Color.RED);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                closeButton.setForeground(originalColor);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    close(e);
                    e.consume();
                }
            }
        });

        return closeButton;
    }

    /**
     * Perform the close action
     *
     * @param e The mouse event that triggered the close action
     */
    public void close(MouseEvent e) {
        int tabIndex = findTabIndex();
        if (tabIndex < 0) {
            return;
        }
        for (EnhancedTabbedPane.TabStateListener listener : parentPane.tabStateListeners) {
            if (listener != null) {
                listener.onTabCloseRequest(tabIndex, component, e);
            }
        }
    }

    /**
     * Finds the index of this tab in the parent pane
     *
     * @return The index of this tab in the parent pane, or -1 if not found
     */
    int findTabIndex() {
        // Find this tab's index in the parent pane
        for (int i = 0; i < parentPane.getTabCount(); i++) {
            if (parentPane.getTabComponentAt(i) == this) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Sets the title of this tab
     *
     * @param title The new title to set
     */
    public void setTitle(String title) {
        titleLabel.setText(title);
        if (parentPane != null) {
            parentPane.positionActionButtons();
        }
    }

    /**
     * Sets the font style of the title label to bold or plain
     *
     */
    public void setBold(boolean bold) {
        Font font = titleLabel.getFont();
        if (font != null) {
            titleLabel.setFont(font.deriveFont(bold ? Font.BOLD : Font.PLAIN));
        }
    }

    /**
     * Sets the visual dirty state of this tab and updates the icon accordingly
     *
     */
    public void setDirty(boolean dirty) {
        if (this.isDirty != dirty) {
            this.isDirty = dirty;
            if (dirty) {
                // Big bullet point
                String closeButtonDirtyText = "\u25CF";
                closeButton.setText(closeButtonDirtyText);
            } else {
                closeButton.setText(closeButtonText);

            }
            revalidate();
            repaint();
        }
    }

    /**
     * Gets the title of this tab
     *
     * @return The title of this tab
     */
    public String getTitle() {
        return titleLabel.getText();
    }

    /**
     * Checks if this tab is selected
     *
     * @return True if this tab is selected, false otherwise
     */
    public boolean isTabSelected() {
        return (parentPane.getSelectedIndex() == findTabIndex());
    }

    /**
     * Gets the component associated with this tab
     *
     * @return The component associated with this tab
     */
    public Component getComponent() {
        return component;
    }

    /**
     * Sets the parent pane of this tab
     *
     * @param parentPane The parent pane to set
     */
    public void setParentPane(EnhancedTabbedPane parentPane) {
        this.parentPane = parentPane;
    }

    /**
     * Gets the parent pane of this tab
     *
     * @return The parent pane of this tab
     */
    public EnhancedTabbedPane getParentPane() {
        return parentPane;
    }

}
