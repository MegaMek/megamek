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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.ListResourceBundle;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

class SettingsPaneTest {
    private static final SettingsNavigationText NAVIGATION_TEXT = new SettingsNavigationText(
          "Filter", "Filter settings", "No matches", "%d matches", "Expand", "Collapse");
    private static final SettingsTextProvider PAGE_TEXT = SettingsTextProvider.fromResourceBundle(
          new ListResourceBundle() {
              @Override
              protected Object[][] getContents() {
                  return new Object[][] { { "header", "Header" } };
              }
          });

    @Test
    void pagesAreCreatedLazilyAndCached() throws Exception {
        AtomicInteger firstBuilds = new AtomicInteger();
        AtomicInteger secondBuilds = new AtomicInteger();
        runOnEdt(() -> {
            SettingsRoute first = new SettingsRoute("first", List.of("First"));
            SettingsRoute second = new SettingsRoute("second", List.of("Second"));
            Map<String, Supplier<Component>> factories = new HashMap<>();
            factories.put("first", () -> {
                firstBuilds.incrementAndGet();
                return page("First section", "first summary");
            });
            factories.put("second", () -> {
                secondBuilds.incrementAndGet();
                return page("Second section", "second summary");
            });
            SettingsPane pane = new SettingsPane(List.of(first, second), factories, NAVIGATION_TEXT);

            pane.selectRoute(second);
            pane.selectRoute(second);

            assertEquals(1, firstBuilds.get());
            assertEquals(1, secondBuilds.get());
        });
    }

    @Test
    void activeFilterExpandsOnlyMatchingSection() throws Exception {
        runOnEdt(() -> {
            SettingsRoute route = new SettingsRoute("page", List.of("Page"));
            SettingsPagePanel page = SettingsPagePanel.builder("Test", PAGE_TEXT, "header", null)
                .sectionsExpandedByDefault(false)
                .literalSection("Alpha", "alpha summary", new JLabel())
                .literalSection("Beta", "beta summary", new JLabel())
                .build();
            SettingsPane pane = new SettingsPane(List.of(route), Map.of("page", () -> page),
                NAVIGATION_TEXT);

            pane.setFilterText("beta");
            pane.selectRoute(route);

            List<CollapsibleSectionPanel> sections = findSections(page);
            assertEquals(2, sections.size());
            assertFalse(sections.get(0).isExpanded());
            assertTrue(sections.get(1).isExpanded());
        });
        flushEventQueue();
    }

    @Test
    void typingFilterExpandsMatchingSectionOnCurrentPage() throws Exception {
        runOnEdt(() -> {
            SettingsRoute route = new SettingsRoute("page", List.of("Page"));
            SettingsPagePanel page = SettingsPagePanel.builder("Test", PAGE_TEXT, "header", null)
                  .sectionsExpandedByDefault(false)
                  .literalSection("Alpha", null, new JLabel("First option"))
                  .literalSection("Beta", null, new JLabel("Needle option"))
                  .build();
            SettingsPane pane = new SettingsPane(List.of(route), Map.of("page", () -> page),
                NAVIGATION_TEXT);

            pane.setFilterText("needle");

            List<CollapsibleSectionPanel> sections = findSections(page);
            assertFalse(sections.get(0).isExpanded());
            assertTrue(sections.get(1).isExpanded());
        });
    }

    @Test
    void filterMatchingAnotherRouteLeavesCurrentPageExpansionUnchanged() throws Exception {
        runOnEdt(() -> {
            SettingsRoute first = new SettingsRoute("first", List.of("First"));
            SettingsRoute second = new SettingsRoute("second", List.of("Second"));
            SettingsPagePanel firstPage = SettingsPagePanel.builder("First", PAGE_TEXT, "header", null)
                  .sectionsExpandedByDefault(false)
                  .literalSection("Alpha", null, new JLabel("Alpha option"))
                  .literalSection("Beta", null, new JLabel("Beta option"))
                  .build();
            SettingsPane pane = new SettingsPane(List.of(first, second), Map.of(
                  "first", () -> firstPage,
                "second", () -> page("Needle section", null)), NAVIGATION_TEXT);

            pane.setFilterText("needle");

            List<CollapsibleSectionPanel> sections = findSections(firstPage);
            assertFalse(sections.get(0).isExpanded());
            assertFalse(sections.get(1).isExpanded());
        });
        finishSearchIndexing();
    }

    @Test
    void clearingFilterRestoresExpansionState() throws Exception {
        runOnEdt(() -> {
            SettingsRoute route = new SettingsRoute("page", List.of("Page"));
            SettingsPagePanel page = SettingsPagePanel.builder("Test", PAGE_TEXT, "header", null)
                  .sectionsExpandedByDefault(false)
                  .literalSection("Alpha", null, new JLabel("Alpha option"))
                  .literalSection("Beta", null, new JLabel("Needle option"))
                  .build();
            SettingsPane pane = new SettingsPane(List.of(route), Map.of("page", () -> page),
                NAVIGATION_TEXT);

            pane.setFilterText("needle");
            List<CollapsibleSectionPanel> sections = findSections(page);
            assertFalse(sections.get(0).isExpanded());
            assertTrue(sections.get(1).isExpanded());

            pane.setFilterText("");
            assertFalse(sections.get(0).isExpanded());
            assertFalse(sections.get(1).isExpanded());
        });
    }

    @Test
    void activeFilterHighlightsCurrentAndNewlySelectedPages() throws Exception {
        runOnEdt(() -> {
            SettingsRoute first = new SettingsRoute("first", List.of("First"));
            SettingsRoute second = new SettingsRoute("second", List.of("Second"));
            SettingsPane pane = new SettingsPane(List.of(first, second), Map.of(
                  "first", () -> componentPage("Search Needle"),
                "second", () -> componentPage("Another Needle")), NAVIGATION_TEXT);
            pane.setSize(800, 500);
            layoutTree(pane);

            pane.setFilterText("needle");
            SettingsContentHost host = findComponent(pane, "settingsContentHost", SettingsContentHost.class);
            assertFalse(host.getSearchHighlightBounds().isEmpty());

            assertTrue(pane.selectRoute(second));
            layoutTree(pane);
            assertFalse(host.getSearchHighlightBounds().isEmpty());
        });
    }

    @Test
    void navigatingWithActiveFilterRebindsHelpWhenHiddenControlReturns() throws Exception {
        runOnEdt(() -> {
            SettingsRoute first = new SettingsRoute("first", List.of("First"));
            SettingsRoute second = new SettingsRoute("second", List.of("Second"));
            FilterableHelpPage secondPage = new FilterableHelpPage();
            SettingsPane pane = new SettingsPane(List.of(first, second), Map.of(
                  "first", () -> componentPage("First option"),
                  "second", () -> secondPage), NAVIGATION_TEXT);

            pane.selectRoute(second);
            pane.selectRoute(first);
            pane.setFilterText("needle");
            pane.selectRoute(second);

            assertEquals("Other help", secondPage.other.getToolTipText());

            pane.setFilterText("");

            assertNull(secondPage.other.getToolTipText());
        });
    }

    @Test
    void parentRouteFallsBackToFirstDescendantPage() throws Exception {
        AtomicInteger childBuilds = new AtomicInteger();
        runOnEdt(() -> {
            SettingsRoute initial = new SettingsRoute("initial", List.of("Initial"));
            SettingsRoute parent = new SettingsRoute("group", List.of("Group"));
            SettingsRoute child = new SettingsRoute("child", List.of("Group", "Child"),
                  List.of("group", "group.child"), List.of(), true);
            Map<String, Supplier<Component>> factories = new HashMap<>();
            factories.put("initial", () -> page("Initial", null));
            factories.put("child", () -> {
                childBuilds.incrementAndGet();
                return page("Child", null);
            });
            SettingsPane pane = new SettingsPane(List.of(initial, parent, child), factories,
                NAVIGATION_TEXT);

            assertTrue(pane.selectRoute(parent));
            assertEquals(1, childBuilds.get());
        });
    }

    @Test
    void searchIndexRefreshesResultsAfterEachPage() throws Exception {
        AtomicInteger secondBuilds = new AtomicInteger();
        AtomicReference<SettingsPane> pane = new AtomicReference<>();
        runOnEdt(() -> {
            SettingsRoute first = new SettingsRoute("first", List.of("First"));
            SettingsRoute second = new SettingsRoute("second", List.of("Second"));
            pane.set(new SettingsPane(List.of(first, second), Map.of(
                  "first", () -> page("Initial", null),
                  "second", () -> {
                      secondBuilds.incrementAndGet();
                      return page("Needle section", null);
                  }), NAVIGATION_TEXT));
            pane.get().setFilterText("needle");
        });
        finishSearchIndexing();

        runOnEdt(() -> {
            JLabel status = findComponent(pane.get(), "lblSettingsFilterStatus", JLabel.class);
            assertEquals(1, secondBuilds.get());
            assertEquals("1 matches", status.getText());
        });
    }

    @Test
    void pageBuildClearsStaleRouteSearchText() throws Exception {
        runOnEdt(() -> {
            SettingsRoute route = new SettingsRoute("page", List.of("Page"));
            route.setSectionSearchText("Stale Search Token");

            new SettingsPane(List.of(route), Map.of("page", () -> SettingsPagePanel.builder(
                "Test", PAGE_TEXT, "header", null).build()), NAVIGATION_TEXT);

            assertFalse(route.matches(SettingsRoute.normalizeSearchText("stale")));
            assertTrue(route.matches(SettingsRoute.normalizeSearchText("header")));
        });
    }

    @Test
    void clearingFilterCancelsQueuedIndexingAndLaterSearchRestartsIt() throws Exception {
        AtomicInteger secondBuilds = new AtomicInteger();
        AtomicReference<SettingsPane> pane = new AtomicReference<>();

        runOnEdt(() -> {
            SettingsRoute first = new SettingsRoute("first", List.of("First"));
            SettingsRoute second = new SettingsRoute("second", List.of("Second"));
            pane.set(new SettingsPane(List.of(first, second), Map.of(
                  "first", () -> page("Initial", null),
                  "second", () -> {
                      secondBuilds.incrementAndGet();
                      return page("Needle section", null);
                  }), NAVIGATION_TEXT));
            pane.get().setFilterText("needle");
            pane.get().setFilterText("");
        });
        flushEventQueue();
        assertEquals(0, secondBuilds.get());

        runOnEdt(() -> pane.get().setFilterText("needle"));
        finishSearchIndexing();
        assertEquals(1, secondBuilds.get());
    }

    @Test
    void failedSearchIndexFactoryCanBeRetried() throws Exception {
        AtomicInteger attempts = new AtomicInteger();
        AtomicReference<SettingsPane> pane = new AtomicReference<>();
        runOnEdt(() -> {
            SettingsRoute first = new SettingsRoute("first", List.of("First"));
            SettingsRoute retry = new SettingsRoute("retry", List.of("Retry"));
            pane.set(new SettingsPane(List.of(first, retry), Map.of(
                  "first", () -> page("Initial", null),
                  "retry", () -> {
                      if (attempts.getAndIncrement() == 0) {
                          throw new IllegalStateException("First indexing attempt fails");
                      }
                      return page("Needle section", null);
                  }), NAVIGATION_TEXT));
            pane.get().setFilterText("needle");
        });
        flushEventQueue();
        assertEquals(1, attempts.get());

        runOnEdt(() -> pane.get().setFilterText("needle "));
        finishSearchIndexing();

        runOnEdt(() -> {
            JLabel status = findComponent(pane.get(), "lblSettingsFilterStatus", JLabel.class);
            assertEquals(2, attempts.get());
            assertEquals("1 matches", status.getText());
        });
    }

    private static SettingsPagePanel page(String title, String summary) {
        return SettingsPagePanel.builder("Test", PAGE_TEXT, "header", null)
              .literalSection(title, summary, new JLabel())
              .build();
    }

    private static SettingsPagePanel componentPage(String text) {
        return SettingsPagePanel.builder("Test", PAGE_TEXT, "header", null)
              .component(new JLabel(text))
              .build();
    }

    private static final class FilterableHelpPage extends JPanel implements SettingsFilterable {
        private final JLabel needle = helpLabel("Needle option", "Needle help");
        private final JLabel other = helpLabel("Other option", "Other help");

        private FilterableHelpPage() {
            applySettingsFilter("");
        }

        @Override
        public void applySettingsFilter(String normalizedFilter) {
            removeAll();
            if (normalizedFilter.isBlank() || "needle option".contains(normalizedFilter)) {
                add(needle);
            }
            if (normalizedFilter.isBlank() || "other option".contains(normalizedFilter)) {
                add(other);
            }
            revalidate();
            repaint();
        }

        private static JLabel helpLabel(String text, String help) {
            JLabel label = new JLabel(text);
            label.setToolTipText(help);
            return label;
        }
    }

    private static List<CollapsibleSectionPanel> findSections(Container root) {
        java.util.ArrayList<CollapsibleSectionPanel> sections = new java.util.ArrayList<>();
        for (Component child : root.getComponents()) {
            if (child instanceof CollapsibleSectionPanel section) {
                sections.add(section);
            }
            if (child instanceof Container container) {
                sections.addAll(findSections(container));
            }
        }
        return sections;
    }

    private static void layoutTree(Container root) {
        root.doLayout();
        for (Component child : root.getComponents()) {
            if (child instanceof Container container) {
                layoutTree(container);
            }
        }
    }

    private static <T extends Component> T findComponent(Container root, String name, Class<T> type) {
        for (Component child : root.getComponents()) {
            if (type.isInstance(child) && name.equals(child.getName())) {
                return type.cast(child);
            }
            if (child instanceof Container container) {
                T result = findComponentOrNull(container, name, type);
                if (result != null) {
                    return result;
                }
            }
        }
        throw new AssertionError("No " + type.getSimpleName() + " named " + name);
    }

    private static <T extends Component> T findComponentOrNull(Container root, String name, Class<T> type) {
        for (Component child : root.getComponents()) {
            if (type.isInstance(child) && name.equals(child.getName())) {
                return type.cast(child);
            }
            if (child instanceof Container container) {
                T result = findComponentOrNull(container, name, type);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    private static void flushEventQueue() throws Exception {
        runOnEdt(() -> { });
    }

    private static void finishSearchIndexing() throws Exception {
        flushEventQueue();
        flushEventQueue();
    }

    private static void runOnEdt(CheckedRunnable runnable) throws Exception {
        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    runnable.run();
                } catch (Throwable throwable) {
                    throw new RuntimeException(throwable);
                }
            });
        } catch (InvocationTargetException exception) {
            Throwable failure = exception.getCause();
            if (failure instanceof RuntimeException && failure.getCause() != null) {
                failure = failure.getCause();
            }
            if (failure instanceof Error error) {
                throw error;
            }
            if (failure instanceof Exception checkedException) {
                throw checkedException;
            }
            throw exception;
        }
    }

    @FunctionalInterface
    private interface CheckedRunnable {
        void run() throws Exception;
    }
}
