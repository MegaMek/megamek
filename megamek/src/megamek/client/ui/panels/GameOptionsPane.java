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
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
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
package megamek.client.ui.panels;

import static megamek.client.ui.Messages.getString;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

import megamek.client.ui.settings.SettingsFilterable;
import megamek.client.ui.settings.SettingsHeaderPanel;
import megamek.client.ui.settings.SettingsNavigationText;
import megamek.client.ui.settings.SettingsPagePanel;
import megamek.client.ui.settings.SettingsPane;
import megamek.client.ui.settings.SettingsRoute;
import megamek.client.ui.settings.SettingsTextProvider;
import megamek.common.options.IOption;

/** Searchable settings-tree presentation for metadata-backed game option groups. */
public class GameOptionsPane extends JPanel {
    private static final SettingsTextProvider TEXT = SettingsTextProvider.megaMek();

    private final List<GroupPage> pages = new ArrayList<>();
    private final SettingsPane settingsPane;

    public GameOptionsPane(List<OptionGroup> groups, Predicate<IOption> optionVisibility) {
        super(new BorderLayout());
        setName("gameOptionsPane");
        Objects.requireNonNull(optionVisibility);

        List<SettingsRoute> routes = new ArrayList<>();
        Map<String, Supplier<Component>> pageFactories = new LinkedHashMap<>();
        for (OptionGroup group : groups) {
            GroupPage page = new GroupPage(group, optionVisibility);
            SettingsRoute route = new SettingsRoute("gameOptions." + group.id(), List.of(group.displayName()),
                  List.of(group.id()), true);
            page.setRoute(route);
            pages.add(page);
            routes.add(route);
            pageFactories.put(route.getId(), () -> page);
        }

        SettingsNavigationText navigationText = new SettingsNavigationText(
              getString("GameOptionsDialog.Search"),
              getString("GameOptionsDialog.SearchToolTip"),
              getString("GameOptionsDialog.SearchNoMatches"),
              getString("GameOptionsDialog.SearchMatches"),
              getString("SettingsPagePanel.expandAll.text"),
              getString("SettingsPagePanel.collapseAll.text"));
        settingsPane = new SettingsPane(routes, pageFactories, navigationText,
              getString("GameOptionsDialog.optionDescriptionHint"));
        add(settingsPane, BorderLayout.CENTER);
    }

    public String getActiveFilter() {
        return settingsPane.getActiveFilter();
    }

    public void setFilterText(String filterText) {
        settingsPane.setFilterText(filterText);
    }

    /** Re-evaluates option visibility after the unofficial-options setting changes. */
    public void refreshVisibility() {
        String filter = settingsPane.getActiveFilter();
        for (GroupPage page : pages) {
            page.refreshVisibility(filter);
        }
        settingsPane.refreshFilter();
    }

    /** One game-options navigation route and its staged editor components. */
    public record OptionGroup(String id, String displayName, List<DialogOptionComponentYPanel> components) {
        public OptionGroup {
            Objects.requireNonNull(id);
            Objects.requireNonNull(displayName);
            components = List.copyOf(components);
        }
    }

    private static class GroupPage extends JPanel implements SettingsFilterable {
        private final List<OptionRow> rows;
        private final Predicate<IOption> optionVisibility;
        private final String groupSearchableText;
        private SettingsRoute route;

        private GroupPage(OptionGroup group, Predicate<IOption> optionVisibility) {
            super(new BorderLayout());
            this.optionVisibility = optionVisibility;
            groupSearchableText = SettingsRoute.normalizeSearchText(group.id() + ' ' + group.displayName());
            setName("gameOptions" + group.id() + "Page");

            JPanel optionList = new JPanel();
            optionList.setName("gameOptions" + group.id() + "List");
            optionList.setLayout(new BoxLayout(optionList, BoxLayout.PAGE_AXIS));
            rows = group.components().stream().map(OptionRow::new).toList();
            rows.forEach(row -> optionList.add(row.component()));

            SettingsPagePanel page = SettingsPagePanel.builder(group.id(), TEXT, "GameOptionsDialog.title", null)
                  .header(new SettingsHeaderPanel(group.id(), group.displayName(), null))
                  .component(optionList)
                  .showDetailsPanel(true)
                  .build();
            add(page, BorderLayout.CENTER);
        }

        private void setRoute(SettingsRoute route) {
            this.route = route;
            refreshVisibility("");
        }

        private void refreshVisibility(String normalizedFilter) {
            StringBuilder searchableText = new StringBuilder();
            for (OptionRow row : rows) {
                boolean generallyVisible = optionVisibility.test(row.option());
                row.component().setVisible(generallyVisible && row.matches(normalizedFilter, groupSearchableText));
                if (generallyVisible) {
                    searchableText.append(' ').append(row.searchableText());
                }
            }
            if (route != null) {
                route.setSectionSearchText(searchableText.toString());
            }
            revalidate();
            repaint();
        }

        @Override
        public void applySettingsFilter(String normalizedFilter) {
            refreshVisibility(normalizedFilter);
        }
    }

    private record OptionRow(DialogOptionComponentYPanel component, IOption option, String searchableText) {
        private OptionRow(DialogOptionComponentYPanel component) {
            this(component, component.getOption(), SettingsRoute.normalizeSearchText(
                  component.getOption().getName() + ' '
                        + component.getOption().getDisplayableName() + ' '
                        + component.getOption().getDescription()));
        }

        private boolean matches(String normalizedFilter, String groupSearchableText) {
            if (normalizedFilter.isBlank()) {
                return true;
            }
            String combinedSearchableText = groupSearchableText + ' ' + searchableText;
            for (String token : normalizedFilter.split("\\s+")) {
                if (!combinedSearchableText.contains(token)) {
                    return false;
                }
            }
            return true;
        }
    }
}