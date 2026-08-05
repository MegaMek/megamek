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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLayer;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.text.html.HTML;

import org.apache.commons.text.StringEscapeUtils;

import megamek.client.ui.util.UIUtil;
import megamek.common.annotations.Nullable;
import megamek.common.ui.FastJScrollPane;

/** Owns the central scrollable settings content and an optional sticky help panel. */
public class SettingsContentHost extends JPanel {
    private static final int SCROLL_SPEED = 16;
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("</?([A-Za-z][A-Za-z0-9]*)\\b[^<>]*>");

    private final JPanel contentPanel = new SettingsContentPanel();
    private final JScrollPane contentScrollPane;
    private final SettingsSearchHighlightLayerUI searchHighlightLayerUI = new SettingsSearchHighlightLayerUI();
    private final JLayer<JScrollPane> searchHighlightLayer;
    private final SettingsHelpPanel helpPanel;
    private final List<HelpBinding> activeHelpBindings = new ArrayList<>();
    private Component currentContent;

    public SettingsContentHost(Component content, String helpTitle, boolean showHelpPanel) {
        super(new BorderLayout());
        setName("settingsContentHost");
        contentPanel.setName("settingsContentPanel");
        contentScrollPane = new FastJScrollPane(contentPanel,
              ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
              ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        contentScrollPane.setName("settingsContentScrollPane");
        contentScrollPane.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
        searchHighlightLayer = new JLayer<>(contentScrollPane, searchHighlightLayerUI);
        searchHighlightLayer.setName("settingsSearchHighlightLayer");
        helpPanel = new SettingsHelpPanel(helpTitle);
        add(searchHighlightLayer, BorderLayout.CENTER);
        add(helpPanel, BorderLayout.SOUTH);
        setContent(content, showHelpPanel);
    }

    public void setContent(Component content) {
        setContent(content, true);
    }

    /**
     * Replaces the hosted content. A contained {@link SettingsPagePanel} supplies the definitive help-panel policy;
     * {@code defaultShowHelpPanel} is used only when the content does not contain one.
     */
    public void setContent(Component content, boolean defaultShowHelpPanel) {
        unbindHelp();
        helpPanel.clearHelpText();
        boolean showHelp = shouldShowHelpPanel(content, defaultShowHelpPanel);
        helpPanel.setVisible(showHelp);
        contentPanel.removeAll();
        currentContent = content;
        contentPanel.add(content, BorderLayout.CENTER);
        if (showHelp) {
            bindHelp(content, null);
        }
        contentPanel.revalidate();
        contentPanel.repaint();
        revalidate();
        repaint();
        SwingUtilities.invokeLater(this::resetScrollPosition);
    }

    public void resetScrollPosition() {
        contentScrollPane.getVerticalScrollBar().setValue(0);
    }

    /** Updates the non-mutating text highlights painted over the current settings content. */
    public void setSearchFilter(String normalizedFilter) {
        searchHighlightLayerUI.setFilter(normalizedFilter, searchHighlightLayer);
    }

    /** Exposes painted bounds for headless overlay tests. */
    List<Rectangle> getSearchHighlightBounds() {
        return searchHighlightLayerUI.highlightBounds(searchHighlightLayer);
    }

    /**
     * Updates this host's sticky help surface. Plain text is wrapped as HTML so it soft-wraps to the panel width;
     * caller-supplied HTML is preserved.
     */
    public void setHelpText(String helpText) {
        if (helpText == null || helpText.isBlank()) {
            return;
        }
        boolean isHtmlDocument = helpText.regionMatches(true, 0, "<html>", 0, "<html>".length());
        if (isHtmlDocument) {
            helpPanel.setHelpText(helpText);
            return;
        }
        String body = containsHtmlTag(helpText)
              ? helpText
              : StringEscapeUtils.escapeHtml4(helpText);
        helpPanel.setHelpText("<html>" + body + "</html>");
    }

    private static boolean containsHtmlTag(String text) {
        var matcher = HTML_TAG_PATTERN.matcher(text);
        while (matcher.find()) {
            if (HTML.getTag(matcher.group(1).toLowerCase(Locale.ROOT)) != null) {
                return true;
            }
        }
        return false;
    }

    /** Finds the nearest settings content host containing {@code component}. */
    public static @Nullable SettingsContentHost findHost(Component component) {
        if (component instanceof SettingsContentHost host) {
            return host;
        }
        return (SettingsContentHost) SwingUtilities.getAncestorOfClass(SettingsContentHost.class, component);
    }

    public static @Nullable SettingsPagePanel findPagePanel(Component content) {
        if (content instanceof SettingsPagePanel pagePanel) {
            return pagePanel;
        }
        if (content instanceof Container container) {
            for (Component child : container.getComponents()) {
                SettingsPagePanel result = findPagePanel(child);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    private boolean shouldShowHelpPanel(Component content, boolean defaultShowHelpPanel) {
        SettingsPagePanel pagePanel = findPagePanel(content);
        return pagePanel == null ? defaultShowHelpPanel : pagePanel.shouldShowDetailsPanel();
    }

    private void bindHelp(Component component, @Nullable String inheritedHelpText) {
        String descendantHelpText = inheritedHelpText;
        if (component instanceof JComponent swingComponent && !(component instanceof JButton)) {
            String ownHelpText = component instanceof SettingsHelpProvider provider
                  ? provider.getSettingsHelpText()
                  : swingComponent.getToolTipText();
            if (ownHelpText != null && !ownHelpText.isBlank()) {
                descendantHelpText = ownHelpText;
            }
            if (descendantHelpText != null && !descendantHelpText.isBlank()) {
                String helpText = descendantHelpText;
                MouseAdapter mouseListener = new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent event) {
                        setHelpText(helpText);
                    }
                };
                FocusAdapter focusListener = new FocusAdapter() {
                    @Override
                    public void focusGained(FocusEvent event) {
                        setHelpText(helpText);
                    }
                };
                boolean suppressTooltip = ownHelpText != null && !ownHelpText.isBlank()
                      && swingComponent.getToolTipText() != null;
                HelpBinding binding = new HelpBinding(swingComponent, swingComponent.getToolTipText(),
                      suppressTooltip, mouseListener, focusListener);
                swingComponent.addMouseListener(binding.mouseListener());
                swingComponent.addFocusListener(binding.focusListener());
                if (suppressTooltip) {
                    swingComponent.setToolTipText(null);
                }
                activeHelpBindings.add(binding);
            }
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                bindHelp(child, descendantHelpText);
            }
        }
    }

    private void unbindHelp() {
        for (HelpBinding binding : activeHelpBindings) {
            binding.component().removeMouseListener(binding.mouseListener());
            binding.component().removeFocusListener(binding.focusListener());
            if (binding.suppressTooltip()) {
                binding.component().setToolTipText(binding.originalTooltip());
            }
        }
        activeHelpBindings.clear();
    }

    @Override
    public void removeNotify() {
        unbindHelp();
        super.removeNotify();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (helpPanel.isVisible() && activeHelpBindings.isEmpty() && currentContent != null) {
            bindHelp(currentContent, null);
        }
    }

    private record HelpBinding(JComponent component, String originalTooltip, boolean suppressTooltip,
                               MouseAdapter mouseListener, FocusAdapter focusListener) {
    }

    private static class SettingsContentPanel extends JPanel implements Scrollable {
        private SettingsContentPanel() {
            super(new BorderLayout());
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension preferredSize = super.getPreferredSize();
            SettingsPagePanel pagePanel = findPagePanel(this);
            if (pagePanel == null) {
                return preferredSize;
            }
            Dimension contentSize = pagePanel.getContentPreferredSize();
            return new Dimension(Math.max(preferredSize.width, contentSize.width),
                  Math.max(preferredSize.height, contentSize.height));
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return super.getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return UIUtil.scaleForGUI(SCROLL_SPEED);
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return orientation == SwingConstants.VERTICAL ? visibleRect.height : visibleRect.width;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            Container parent = getParent();
            return !(parent instanceof JViewport viewport) || viewport.getWidth() >= getPreferredSize().width;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }
}
