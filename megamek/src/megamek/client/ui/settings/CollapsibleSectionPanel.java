/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.settings;

import static megamek.codeUtilities.StringUtility.isNullOrBlank;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;

import megamek.client.ui.util.UIUtil;
import megamek.common.annotations.Nullable;

/** A lightweight, theme-friendly collapsible section for reusable settings screens. */
public class CollapsibleSectionPanel extends JPanel {
    public static final String EXPANDED_PROPERTY = "expanded";
    public static final String TOGGLE_ACTION = "toggle";

    private static final String COLLAPSE_DESCRIPTION_KEY =
          "CollapsibleSectionPanel.collapse.accessibleDescription";
    private static final String EXPAND_DESCRIPTION_KEY =
          "CollapsibleSectionPanel.expand.accessibleDescription";
    private static final int HEADER_VERTICAL_PADDING = 6;
    private static final int HEADER_HORIZONTAL_PADDING = 8;
    private static final int CONTENT_LEFT_PADDING = 24;
    private static final int CONTENT_VERTICAL_PADDING = 6;
    private static final int CHEVRON_ICON_SIZE = 12;
    private static final int CHEVRON_STROKE_WIDTH = 2;
    private static final int CHEVRON_HORIZONTAL_PADDING = 3;
    private static final int CHEVRON_VERTICAL_PADDING = 4;

    private final SettingsTextProvider textProvider;
    private final JPanel headerPanel;
    private final JLabel iconLabel = new JLabel();
    private final JLabel titleLabel = new JLabel();
    private final JLabel summaryLabel = new JLabel() {
        @Override
        public String getToolTipText(MouseEvent event) {
            return truncatedSummaryToolTip(this);
        }
    };
    private final JPanel trailingPanel = new JPanel(new BorderLayout());
    private final JPanel contentPanel = new JPanel(new BorderLayout());
    private final Action toggleAction = new AbstractAction(TOGGLE_ACTION) {
        @Override
        public void actionPerformed(java.awt.event.ActionEvent event) {
            toggleExpanded();
        }
    };

    private String title;
    private boolean expanded;
    private boolean headerHovered;
    private boolean titleMuted;

    public CollapsibleSectionPanel(String title) {
        this(title, null);
    }

    public CollapsibleSectionPanel(String title, @Nullable JComponent content) {
        this(title, content, SettingsTextProvider.megaMek());
    }

    public CollapsibleSectionPanel(String title, @Nullable JComponent content, SettingsTextProvider textProvider) {
        this.title = Objects.requireNonNull(title);
        this.textProvider = Objects.requireNonNull(textProvider);

        setLayout(new BorderLayout());
        setOpaque(false);
        getActionMap().put(TOGGLE_ACTION, toggleAction);

        headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);
        contentPanel.setOpaque(false);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(UIUtil.scaleForGUI(CONTENT_VERTICAL_PADDING),
              UIUtil.scaleForGUI(CONTENT_LEFT_PADDING),
              UIUtil.scaleForGUI(CONTENT_VERTICAL_PADDING),
              0));
        add(contentPanel, BorderLayout.CENTER);

        setSummary("");
        setContent(content);
        setExpanded(true);
    }

    public void setTitle(String title) {
        this.title = Objects.requireNonNull(title);
        updateHeader();
    }

    public void setSummary(String summary) {
        // Keep the empty summary label visible because its weighted GridBag cell keeps the icon and title left-aligned.
        summaryLabel.setText(isNullOrBlank(summary) ? "" : summary);
        summaryLabel.setVisible(true);
    }

    private static String truncatedSummaryToolTip(JLabel label) {
        return label.getPreferredSize().width > label.getWidth() ? label.getText() : null;
    }

    /**
     * Mutes or restores the section title color.
     *
     * @param muted {@code true} to draw the title in the disabled foreground color
     */
    public void setTitleMuted(boolean muted) {
        titleMuted = muted;
        updateTitleForeground();
    }

    private void updateTitleForeground() {
        titleLabel.setForeground(titleMuted ? UIManager.getColor("Label.disabledForeground") : null);
    }

    public void setTrailingComponent(@Nullable JComponent component) {
        trailingPanel.removeAll();
        if (component != null) {
            trailingPanel.add(component, BorderLayout.CENTER);
        }
        trailingPanel.setVisible(component != null);
        revalidate();
        repaint();
    }

    public void setContent(@Nullable JComponent content) {
        contentPanel.removeAll();
        if (content != null) {
            contentPanel.add(content, BorderLayout.CENTER);
        }
        revalidate();
        repaint();
    }

    public boolean isExpanded() {
        return expanded;
    }

    public int getContentPreferredWidth() {
        return contentPanel.getPreferredSize().width;
    }

    public void setExpanded(boolean expanded) {
        if (this.expanded == expanded) {
            return;
        }

        boolean previousValue = this.expanded;
        this.expanded = expanded;
        contentPanel.setVisible(expanded);
        updateHeader();
        firePropertyChange(EXPANDED_PROPERTY, previousValue, expanded);
        revalidate();
        repaint();
    }

    private JPanel createHeaderPanel() {
        JPanel sectionHeader = new JPanel(new GridBagLayout());
        sectionHeader.setOpaque(false);
        int verticalPadding = UIUtil.scaleForGUI(HEADER_VERTICAL_PADDING);
        int horizontalPadding = UIUtil.scaleForGUI(HEADER_HORIZONTAL_PADDING);
        sectionHeader.setBorder(BorderFactory.createEmptyBorder(verticalPadding,
              horizontalPadding,
              verticalPadding,
              horizontalPadding));
        Cursor handCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
        sectionHeader.setCursor(handCursor);
        iconLabel.setCursor(handCursor);
        titleLabel.setCursor(handCursor);
        summaryLabel.setCursor(handCursor);
        sectionHeader.setFocusable(true);

        titleLabel.putClientProperty("FlatLaf.styleClass", "h4");
        summaryLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        ToolTipManager.sharedInstance().registerComponent(summaryLabel);

        trailingPanel.setOpaque(false);
        trailingPanel.setVisible(false);

        GridBagConstraints layout = new GridBagConstraints();
        layout.gridx = 0;
        layout.gridy = 0;
        layout.weightx = 0.0;
        layout.fill = GridBagConstraints.NONE;
        layout.anchor = GridBagConstraints.WEST;
        sectionHeader.add(iconLabel, layout);

        layout.gridx++;
        layout.insets = new Insets(0, horizontalPadding, 0, 0);
        sectionHeader.add(titleLabel, layout);

        layout.gridx++;
        layout.weightx = 1.0;
        layout.insets = new Insets(0, horizontalPadding, 0, 0);
        layout.fill = GridBagConstraints.HORIZONTAL;
        sectionHeader.add(summaryLabel, layout);

        layout.gridx++;
        layout.weightx = 0.0;
        layout.fill = GridBagConstraints.NONE;
        sectionHeader.add(trailingPanel, layout);

        MouseAdapter headerInteractionListener = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                if (SwingUtilities.isLeftMouseButton(event)) {
                    sectionHeader.requestFocusInWindow();
                }
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                if (!SwingUtilities.isLeftMouseButton(event)) {
                    return;
                }
                Point pointInHeader = SwingUtilities.convertPoint((java.awt.Component) event.getSource(),
                      event.getPoint(), sectionHeader);
                if (sectionHeader.contains(pointInHeader)) {
                    toggleExpanded();
                }
            }

            @Override
            public void mouseEntered(MouseEvent event) {
                headerHovered = true;
                updateHeaderBackground();
            }

            @Override
            public void mouseExited(MouseEvent event) {
                Point pointInHeader = SwingUtilities.convertPoint((java.awt.Component) event.getSource(),
                      event.getPoint(), sectionHeader);
                headerHovered = sectionHeader.contains(pointInHeader);
                updateHeaderBackground();
            }
        };
        sectionHeader.addMouseListener(headerInteractionListener);
        iconLabel.addMouseListener(headerInteractionListener);
        titleLabel.addMouseListener(headerInteractionListener);
        summaryLabel.addMouseListener(headerInteractionListener);
        sectionHeader.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent event) {
                updateHeaderBackground();
            }

            @Override
            public void focusLost(FocusEvent event) {
                updateHeaderBackground();
            }
        });
        sectionHeader.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0),
              TOGGLE_ACTION);
        sectionHeader.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
              TOGGLE_ACTION);
        sectionHeader.getActionMap().put(TOGGLE_ACTION, toggleAction);

        return sectionHeader;
    }

    private void toggleExpanded() {
        setExpanded(!isExpanded());
    }

    private void updateHeader() {
        titleLabel.setText(title);
        updateTitleForeground();
        iconLabel.setIcon(getDisclosureIcon());
        headerPanel.getAccessibleContext().setAccessibleName(title);
        String descriptionKey = isExpanded() ? COLLAPSE_DESCRIPTION_KEY : EXPAND_DESCRIPTION_KEY;
        headerPanel.getAccessibleContext().setAccessibleDescription(textProvider.getText(descriptionKey));
        updateHeaderBackground();
    }

    private void updateHeaderBackground() {
        boolean active = headerHovered || headerPanel.isFocusOwner();
        headerPanel.setOpaque(active);
        headerPanel.setBackground(active ? getHeaderBackgroundColor() : null);
        headerPanel.repaint();
    }

    private Icon getDisclosureIcon() {
        Icon disclosureIcon = UIManager.getIcon(isExpanded() ? "Tree.expandedIcon" : "Tree.collapsedIcon");
        if (disclosureIcon != null) {
            return disclosureIcon;
        }
        return new ChevronIcon(isExpanded(), getForeground());
    }

    private Color getHeaderBackgroundColor() {
        Color backgroundColor = UIManager.getColor("List.hoverBackground");
        if (backgroundColor == null) {
            backgroundColor = UIManager.getColor("Button.hoverBackground");
        }
        return backgroundColor == null ? UIManager.getColor("Panel.background") : backgroundColor;
    }

    @Override
    public Dimension getPreferredSize() {
        return getStableSize(getHeaderBaselineSize(), contentPanel.getPreferredSize());
    }

    @Override
    public Dimension getMinimumSize() {
        return getStableSize(getHeaderBaselineSize(), contentPanel.getMinimumSize());
    }

    private Dimension getHeaderBaselineSize() {
        Insets insets = headerPanel.getInsets();
        int width = insets.left + insets.right
              + iconLabel.getPreferredSize().width
              + UIUtil.scaleForGUI(HEADER_HORIZONTAL_PADDING)
              + titleLabel.getPreferredSize().width;
        if (trailingPanel.isVisible()) {
            width += UIUtil.scaleForGUI(HEADER_HORIZONTAL_PADDING)
                  + trailingPanel.getPreferredSize().width;
        }
        return new Dimension(width, headerPanel.getPreferredSize().height);
    }

    private Dimension getStableSize(Dimension headerSize, Dimension contentSize) {
        int width = Math.max(headerSize.width, contentSize.width);
        int height = headerSize.height + (isExpanded() ? contentSize.height : 0);
        return new Dimension(width, height);
    }

    private static final class ChevronIcon implements Icon {
        private final boolean expanded;
        private final Color color;

        private ChevronIcon(boolean expanded, Color color) {
            this.expanded = expanded;
            this.color = color;
        }

        @Override
        public void paintIcon(java.awt.Component component, Graphics graphics, int x, int y) {
            Graphics2D graphics2D = (Graphics2D) graphics.create();
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics2D.setColor(color == null ? component.getForeground() : color);
            graphics2D.setStroke(new BasicStroke(UIUtil.scaleForGUI((float) CHEVRON_STROKE_WIDTH),
                  BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            int left = x + UIUtil.scaleForGUI(CHEVRON_HORIZONTAL_PADDING);
            int right = x + getIconWidth() - UIUtil.scaleForGUI(CHEVRON_HORIZONTAL_PADDING);
            int top = y + UIUtil.scaleForGUI(CHEVRON_VERTICAL_PADDING);
            int centerX = x + getIconWidth() / 2;
            int centerY = y + getIconHeight() / 2;
            int bottom = y + getIconHeight() - UIUtil.scaleForGUI(CHEVRON_VERTICAL_PADDING);

            if (expanded) {
                graphics2D.drawLine(left, top, centerX, bottom);
                graphics2D.drawLine(centerX, bottom, right, top);
            } else {
                graphics2D.drawLine(left, top, right, centerY);
                graphics2D.drawLine(right, centerY, left, bottom);
            }
            graphics2D.dispose();
        }

        @Override
        public int getIconWidth() {
            return UIUtil.scaleForGUI(CHEVRON_ICON_SIZE);
        }

        @Override
        public int getIconHeight() {
            return UIUtil.scaleForGUI(CHEVRON_ICON_SIZE);
        }
    }
}
