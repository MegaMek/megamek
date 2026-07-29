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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;

import megamek.client.ui.util.UIUtil;
import megamek.logging.MMLogger;

/**
 * Generic settings coordinator that combines navigation and content hosting, creates pages lazily, caches them,
 * indexes section text on first search, and reveals matching sections when a filtered result is opened.
 */
public class SettingsPane extends JPanel {
    private static final MMLogger LOGGER = MMLogger.create(SettingsPane.class);
    private static final int CONTENT_MARGIN = 4;

    private final List<SettingsRoute> routes;
    private final Map<String, Supplier<Component>> pageFactories;
    private final Map<String, Component> pageCache = new HashMap<>();
    private final SettingsContentHost contentHost;
    private final SettingsNavigationPanel navigationPanel;
    private boolean searchIndexInProgress;
    private boolean searchIndexComplete;
    private int searchIndexGeneration;

    public SettingsPane(List<SettingsRoute> routes, Map<String, Supplier<Component>> pageFactories,
          SettingsNavigationText navigationText, String helpTitle) {
        super(new BorderLayout());
        setName("settingsPane");
        this.routes = List.copyOf(routes);
        this.pageFactories = Map.copyOf(pageFactories);
        validateConfiguration();

        SettingsRoute initialRoute = firstPageRoute();
        Component initialContent = getPage(initialRoute);
        contentHost = new SettingsContentHost(initialContent, helpTitle, initialRoute.shouldShowDetailsPanel());
        navigationPanel = new SettingsNavigationPanel(this.routes, this::selectedNavigationTarget, navigationText);
        navigationPanel.setSearchIndexInitializer(this::ensureSearchIndexBuilt);

        int margin = UIUtil.scaleForGUI(CONTENT_MARGIN);
        setBorder(BorderFactory.createEmptyBorder(margin, margin, 0, margin));
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, navigationPanel, contentHost);
        splitPane.setName("settingsSplitPane");
        splitPane.setResizeWeight(0.0);
        splitPane.setDividerLocation(UIUtil.scaleForGUI(SettingsNavigationPanel.DEFAULT_NAVIGATION_WIDTH));
        add(splitPane, BorderLayout.CENTER);
        navigationPanel.selectRoute(initialRoute);
    }

    /** Selects and displays a route programmatically. Parent routes without pages fall back to their first page. */
    public boolean selectRoute(SettingsRoute route) {
        navigationPanel.selectRoute(route);
        return showRoute(route);
    }

    public void focusSearchField() {
        navigationPanel.focusSearchField();
    }

    public void setFilterText(String filterText) {
        navigationPanel.setFilterText(filterText);
    }

    public String getActiveFilter() {
        return navigationPanel.getActiveFilter();
    }

    private void selectedNavigationTarget(SettingsRoute route) {
        showRoute(route);
        contentHost.resetScrollPosition();
    }

    private boolean showRoute(SettingsRoute route) {
        SettingsRoute effectiveRoute = defaultPageRoute(route);
        Component page = getPage(effectiveRoute);
        if (page == null) {
            return false;
        }
        contentHost.setContent(page, effectiveRoute.shouldShowDetailsPanel());
        expandSectionsForActiveFilter(effectiveRoute, page);
        return true;
    }

    private void expandSectionsForActiveFilter(SettingsRoute route, Component page) {
        String activeFilter = navigationPanel.getActiveFilter();
        if (activeFilter.isBlank()) {
            return;
        }
        SettingsPagePanel pagePanel = SettingsContentHost.findPagePanel(page);
        if (pagePanel == null) {
            return;
        }
        boolean expandedMatchingSection = pagePanel.expandSectionsMatching(
              sectionText -> route.sectionMatches(sectionText, activeFilter));
        if (!expandedMatchingSection) {
            pagePanel.expandAllSections();
        }
    }

    private Component getPage(SettingsRoute route) {
        Supplier<Component> factory = pageFactories.get(route.getId());
        if (factory == null) {
            return null;
        }
        Component page = pageCache.computeIfAbsent(route.getId(), id -> Objects.requireNonNull(factory.get()));
        SettingsPagePanel pagePanel = SettingsContentHost.findPagePanel(page);
        if (pagePanel != null && !pagePanel.getSectionSearchText().isBlank()) {
            route.setSectionSearchText(pagePanel.getSectionSearchText());
        }
        return page;
    }

    private void ensureSearchIndexBuilt() {
        if (navigationPanel.getActiveFilter().isBlank()) {
            cancelSearchIndex();
            return;
        }
        if (searchIndexInProgress || searchIndexComplete) {
            return;
        }
        searchIndexInProgress = true;
        int generation = ++searchIndexGeneration;
        List<SettingsRoute> pageRoutes = routes.stream()
              .filter(route -> pageFactories.containsKey(route.getId()))
              .toList();
        buildSearchIndexStep(pageRoutes, 0, generation);
    }

    private void cancelSearchIndex() {
        if (searchIndexInProgress) {
            searchIndexInProgress = false;
            searchIndexGeneration++;
        }
    }

    private void buildSearchIndexStep(List<SettingsRoute> pageRoutes, int index, int generation) {
        if (generation != searchIndexGeneration) {
            return;
        }
        if (navigationPanel.getActiveFilter().isBlank()) {
            cancelSearchIndex();
            return;
        }
        if (index >= pageRoutes.size()) {
            searchIndexInProgress = false;
            searchIndexComplete = true;
            navigationPanel.refreshFilter();
            return;
        }
        SettingsRoute route = pageRoutes.get(index);
        try {
            getPage(route);
            navigationPanel.refreshFilter();
        } catch (RuntimeException exception) {
            searchIndexInProgress = false;
            searchIndexComplete = false;
            searchIndexGeneration++;
            LOGGER.error(exception, "Unable to index settings route " + route.getId());
            return;
        }
        SwingUtilities.invokeLater(() -> buildSearchIndexStep(pageRoutes, index + 1, generation));
    }

    private SettingsRoute firstPageRoute() {
        for (SettingsRoute route : routes) {
            if (pageFactories.containsKey(route.getId())) {
                return route;
            }
        }
        throw new IllegalArgumentException("A settings pane requires at least one page factory");
    }

    private SettingsRoute defaultPageRoute(SettingsRoute route) {
        if (pageFactories.containsKey(route.getId())) {
            return route;
        }
        for (SettingsRoute candidate : routes) {
            if (pageFactories.containsKey(candidate.getId()) && isDescendant(route, candidate)) {
                return candidate;
            }
        }
        return route;
    }

    private static boolean isDescendant(SettingsRoute parent, SettingsRoute candidate) {
        List<String> parentPath = parent.getPathIds();
        List<String> candidatePath = candidate.getPathIds();
        return candidatePath.size() > parentPath.size()
              && candidatePath.subList(0, parentPath.size()).equals(parentPath);
    }

    private void validateConfiguration() {
        if (routes.isEmpty()) {
            throw new IllegalArgumentException("A settings pane requires at least one route");
        }
        Set<String> routeIds = new HashSet<>();
        for (SettingsRoute route : routes) {
            if (!routeIds.add(route.getId())) {
                throw new IllegalArgumentException("Duplicate settings route id: " + route.getId());
            }
        }
        for (String factoryId : pageFactories.keySet()) {
            if (!routeIds.contains(factoryId)) {
                throw new IllegalArgumentException("No settings route exists for page factory: " + factoryId);
            }
        }
    }
}
