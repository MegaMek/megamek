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
import java.awt.HeadlessException;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import javax.swing.*;
import javax.swing.text.html.HTML;

import jakarta.annotation.Nullable;
import megamek.client.ui.util.UIUtil;
import megamek.common.ui.FastJScrollPane;
import org.apache.commons.text.StringEscapeUtils;

/** Owns the central scrollable settings content and an optional sticky help panel. */
public class SettingsContentHost extends JPanel {
    private static final int SCROLL_SPEED = 16;
    private static final int HOVER_HELP_DELAY_MS = 200;
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("</?([A-Za-z][A-Za-z0-9]*)\\b[^<>]*>");

    private final JPanel contentPanel = new SettingsContentPanel();
    private final JScrollPane contentScrollPane;
    private final SettingsSearchHighlightLayerUI searchHighlightLayerUI = new SettingsSearchHighlightLayerUI();
    private final JLayer<JScrollPane> searchHighlightLayer;
    private final SettingsHelpPanel helpPanel;
    private final List<HelpBinding> activeHelpBindings = new ArrayList<>();
    private final Timer hoverHelpTimer = new Timer(HOVER_HELP_DELAY_MS, event -> commitPendingHoverHelp(true));
    private @Nullable Component pendingHoverSource;
    private @Nullable String pendingHoverText;
    private Component currentContent;
    private String activeSearchFilter = "";

    public SettingsContentHost(Component content, boolean showHelpPanel) {
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
        helpPanel = new SettingsHelpPanel();
        hoverHelpTimer.setRepeats(false);
        add(searchHighlightLayer, BorderLayout.CENTER);
        add(helpPanel, BorderLayout.SOUTH);
        setContent(content, showHelpPanel);
    }

    /** @deprecated settings help surfaces always use the shared localized title */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public SettingsContentHost(Component content, String ignoredHelpTitle, boolean showHelpPanel) {
        this(content, showHelpPanel);
    }

    public void setContent(Component content) {
        setContent(content, true);
    }

    /**
     * Replaces the hosted content. A contained {@link SettingsPagePanel} supplies the definitive help-panel policy;
     * {@code defaultShowHelpPanel} is used only when the content does not contain one.
     */
    public void setContent(Component content, boolean defaultShowHelpPanel) {
        cancelPendingHoverHelp();
        unbindHelp();
        helpPanel.clearHelpText();
        boolean showHelp = shouldShowHelpPanel(content, defaultShowHelpPanel);
        helpPanel.setVisible(showHelp);
        contentPanel.removeAll();
        currentContent = content;
        contentPanel.add(content, BorderLayout.CENTER);
        if (showHelp) {
            bindHelp(content, null, null);
        }
        contentPanel.revalidate();
        contentPanel.repaint();
        revalidate();
        repaint();
        SwingUtilities.invokeLater(this::resetScrollPosition);
    }

    /** Rebuilds contextual-help listeners after the current content's component hierarchy changes. */
    public void refreshHelpBindings() {
        cancelPendingHoverHelp();
        unbindHelp();
        helpPanel.clearHelpText();
        if (helpPanel.isVisible() && currentContent != null) {
            bindHelp(currentContent, null, null);
            revealMatchingHelp(activeSearchFilter);
        }
    }

    public void resetScrollPosition() {
        contentScrollPane.getVerticalScrollBar().setValue(0);
    }

    /** Updates the non-mutating text highlights painted over the current settings content. */
    public void setSearchFilter(String normalizedFilter) {
        activeSearchFilter = normalizedFilter == null ? "" : normalizedFilter;
        searchHighlightLayerUI.setFilter(activeSearchFilter, searchHighlightLayer);
        helpPanel.clearHelpText();
        helpPanel.setSearchFilter(activeSearchFilter);
        revealMatchingHelp(activeSearchFilter);
    }

    private void revealMatchingHelp(String normalizedFilter) {
        if (!helpPanel.isVisible() || normalizedFilter == null || normalizedFilter.isBlank()) {
            searchHighlightLayerUI.setHelpMatchComponents(List.of(), searchHighlightLayer);
            return;
        }
        List<String> tokens = SettingsSearchText.tokens(normalizedFilter);
        Set<JComponent> matchingOwners = Collections.newSetFromMap(new IdentityHashMap<>());
        HelpBinding firstMatch = null;
        for (HelpBinding binding : activeHelpBindings) {
            String normalizedHelp = SettingsRoute.normalizeSearchText(binding.helpText());
            if (tokens.stream().allMatch(normalizedHelp::contains)) {
                matchingOwners.add(binding.helpOwner());
                if (firstMatch == null) {
                    firstMatch = binding;
                }
            }
        }
        searchHighlightLayerUI.setHelpMatchComponents(List.copyOf(matchingOwners), searchHighlightLayer);
        if (firstMatch == null) {
            return;
        }
        showHelpTextNow(firstMatch.helpText());
        JComponent firstMatchOwner = firstMatch.helpOwner();
        SwingUtilities.invokeLater(() -> {
            if (normalizedFilter.equals(activeSearchFilter)
                  && SwingUtilities.isDescendingFrom(firstMatchOwner, contentPanel)) {
                firstMatchOwner.scrollRectToVisible(new Rectangle(
                      0, 0, firstMatchOwner.getWidth(), firstMatchOwner.getHeight()));
            }
        });
    }

    /** Exposes painted bounds for headless overlay tests. */
    List<Rectangle> getSearchHighlightBounds() {
        return searchHighlightLayerUI.highlightBounds(searchHighlightLayer);
    }

    List<Rectangle> getHelpMatchBounds() {
        return searchHighlightLayerUI.helpMatchBounds(searchHighlightLayer);
    }

    /**
     * Updates this host's sticky help surface. Plain text is wrapped as HTML so it soft-wraps to the panel width;
     * caller-supplied HTML is preserved.
     *
     * <p>When called while the pointer is over the settings content (that is, from a hover handler), the update is
     * treated as hover help: it is only shown once the pointer has rested for {@link #HOVER_HELP_DELAY_MS} and is
     * still over the same control. This keeps the text from being replaced by every option the mouse crosses on its
     * way down to the help box. Otherwise the text is shown immediately.</p>
     */
    public void setHelpText(String helpText) {
        if (helpText == null || helpText.isBlank()) {
            return;
        }
        Component hovered = findComponentUnderPointer();
        if (hovered == null) {
            showHelpTextNow(helpText);
            return;
        }
        // A page's own hover handler typically runs just after the generic binding for the same control; keep the
        // already-pending control as the source so its text simply replaces the generic text.
        boolean samePendingControl = pendingHoverSource != null
              && SwingUtilities.isDescendingFrom(hovered, pendingHoverSource);
        requestHoverHelpText(samePendingControl ? pendingHoverSource : hovered, helpText);
    }

    /** Shows help text immediately, discarding any pending hover help. Used for focus, search, and settled hovers. */
    private void showHelpTextNow(String helpText) {
        cancelPendingHoverHelp();
        boolean isHtmlDocument = helpText.regionMatches(true, 0, "<html>", 0, "<html>".length());
        if (isHtmlDocument) {
            helpPanel.setHelpText(helpText);
            return;
        }
        String body = (containsHtmlTag(helpText)
              ? helpText
              : StringEscapeUtils.escapeHtml4(helpText))
              .replace("\r\n", "\n")
              .replace('\r', '\n')
              .replace("\n", "<br>");
        helpPanel.setHelpText("<html>" + body + "</html>");
    }

    /**
     * Queues hover help. It is committed only once the pointer has rested on {@code source} for
     * {@link #HOVER_HELP_DELAY_MS} and is still over it. Each request replaces any pending one.
     */
    private void requestHoverHelpText(Component source, String helpText) {
        pendingHoverSource = source;
        pendingHoverText = helpText;
        hoverHelpTimer.restart();
    }

    /**
     * Commits the pending hover help, if any.
     *
     * @param requirePointerOverSource when {@code true}, the text is only shown if the pointer is still over the
     *                                 requesting component; tests pass {@code false} because there is no real pointer
     */
    void commitPendingHoverHelp(boolean requirePointerOverSource) {
        Component source = pendingHoverSource;
        String helpText = pendingHoverText;
        cancelPendingHoverHelp();
        if (source == null || helpText == null) {
            return;
        }
        if (requirePointerOverSource && !isPointerOver(source)) {
            return;
        }
        showHelpTextNow(helpText);
    }

    private void cancelPendingHoverHelp() {
        hoverHelpTimer.stop();
        pendingHoverSource = null;
        pendingHoverText = null;
    }

    /** Returns the deepest content component under the pointer, or {@code null} if the pointer is not over content. */
    private @Nullable Component findComponentUnderPointer() {
        JViewport viewport = contentScrollPane.getViewport();
        try {
            if (!viewport.isShowing()) {
                return null;
            }
            Point viewportPoint = viewport.getMousePosition(true);
            if (viewportPoint == null) {
                return null;
            }
            Point contentPoint = SwingUtilities.convertPoint(viewport, viewportPoint, contentPanel);
            Component deepest = SwingUtilities.getDeepestComponentAt(contentPanel, contentPoint.x, contentPoint.y);
            return deepest == null ? contentPanel : deepest;
        } catch (HeadlessException exception) {
            return null;
        }
    }

    private static boolean isPointerOver(Component component) {
        try {
            if (!component.isShowing()) {
                return false;
            }
            // Count the pointer as "over" a container while it rests on one of its children, so container-level help
            // bindings are not dropped just because a child is under the pointer.
            return component instanceof Container container
                  ? container.getMousePosition(true) != null
                  : component.getMousePosition() != null;
        } catch (HeadlessException exception) {
            return false;
        }
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

    private void bindHelp(Component component, @Nullable String inheritedHelpText,
          @Nullable JComponent inheritedHelpOwner) {
        String descendantHelpText = inheritedHelpText;
        JComponent descendantHelpOwner = inheritedHelpOwner;
        boolean supportsHelp = !(component instanceof JButton) || component instanceof SettingsHelpProvider;
        if (component instanceof JComponent swingComponent && supportsHelp) {
            String ownHelpText = component instanceof SettingsHelpProvider provider
                  ? provider.getSettingsHelpText()
                  : swingComponent.getToolTipText();
            if (ownHelpText != null && !ownHelpText.isBlank()) {
                descendantHelpText = ownHelpText;
                descendantHelpOwner = swingComponent;
            }
            if (descendantHelpText != null && !descendantHelpText.isBlank() && descendantHelpOwner != null) {
                String helpText = descendantHelpText;
                MouseAdapter mouseListener = new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent event) {
                        requestHoverHelpText(event.getComponent(), helpText);
                    }
                };
                FocusAdapter focusListener = new FocusAdapter() {
                    @Override
                    public void focusGained(FocusEvent event) {
                        showHelpTextNow(helpText);
                    }
                };
                boolean suppressTooltip = swingComponent.getToolTipText() != null;
                HelpBinding binding = new HelpBinding(swingComponent, descendantHelpOwner,
                      swingComponent.getToolTipText(), helpText, suppressTooltip, mouseListener, focusListener);
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
                bindHelp(child, descendantHelpText, descendantHelpOwner);
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
        cancelPendingHoverHelp();
        unbindHelp();
        super.removeNotify();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (helpPanel.isVisible() && activeHelpBindings.isEmpty() && currentContent != null) {
            bindHelp(currentContent, null, null);
        }
    }

    private record HelpBinding(JComponent component, JComponent helpOwner, String originalTooltip, String helpText,
                               boolean suppressTooltip, MouseAdapter mouseListener, FocusAdapter focusListener) {
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
