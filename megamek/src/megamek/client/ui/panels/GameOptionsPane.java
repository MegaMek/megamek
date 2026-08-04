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
import static megamek.utilities.ImageUtilities.addTintToImageIcon;
import static megamek.utilities.ImageUtilities.scaleImageIcon;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JPanel;

import megamek.client.ui.settings.SettingsBadge;
import megamek.client.ui.settings.SettingsFilterable;
import megamek.client.ui.settings.SettingsFormPanel;
import megamek.client.ui.settings.SettingsHeaderPanel;
import megamek.client.ui.settings.SettingsNavigationPanel;
import megamek.client.ui.settings.SettingsNavigationText;
import megamek.client.ui.settings.SettingsPagePanel;
import megamek.client.ui.settings.SettingsPane;
import megamek.client.ui.settings.SettingsRoute;
import megamek.client.ui.settings.SettingsTextProvider;
import megamek.client.ui.util.UIUtil;
import megamek.common.Configuration;
import megamek.common.options.IOption;
import megamek.common.options.OptionsConstants;

/** Searchable settings-tree presentation for metadata-backed game option groups. */
public class GameOptionsPane extends JPanel {
    private static final int START_HEIGHT = 800;
    private static final int HEADER_IMAGE_SIZE = 80;
    private static final int IMPORTANT_ICON = 0xE002;
    private static final int ADVANCED_ICON = 0xE8B8;
    private static final int UNOFFICIAL_ICON = 0xEA4B;
    private static final SettingsTextProvider TEXT = SettingsTextProvider.megaMek();
    private static final SettingsBadge IMPORTANT_BADGE = new SettingsBadge(IMPORTANT_ICON, null,
          getString("GameOptionsDialog.legend.important"));
    private static final SettingsBadge ADVANCED_BADGE = new SettingsBadge(ADVANCED_ICON, null,
          getString("GameOptionsDialog.legend.advanced"));
    private static final SettingsBadge UNOFFICIAL_BADGE = new SettingsBadge(UNOFFICIAL_ICON, null,
          getString("GameOptionsDialog.legend.unofficial"));
    private static final Map<String, String> PAGE_FACTION_LOGOS = Map.ofEntries(
          Map.entry("basic", "logo_federated_suns.png"),
          Map.entry("gameMaster", "logo_star_league.png"),
          Map.entry("victory", "logo_clan_wolf.png"),
          Map.entry("allowedUnits", "logo_comstar.png"),
          Map.entry("advancedRules", "logo_republic_of_the_sphere.png"),
          Map.entry("advancedCombat", "logo_draconis_combine.png"),
          Map.entry("advancedGroundMovement", "logo_free_worlds_league.png"),
          Map.entry("advancedAeroRules", "logo_clan_smoke_jaguar.png"),
          Map.entry("initiative", "logo_clan_ghost_bear.png"),
          Map.entry("rpg", "logo_outworld_alliance.png"));
    private static final Map<String, Icon> PAGE_HEADER_ICONS = new LinkedHashMap<>();

    private final List<GroupPage> pages = new ArrayList<>();
    private final SettingsPane settingsPane;

    public GameOptionsPane(List<OptionGroup> groups, Predicate<IOption> optionVisibility) {
        super(new BorderLayout());
        setName("gameOptionsPane");
        Objects.requireNonNull(optionVisibility);

        Map<GameOptionsPresentation.PageDefinition, PageSeed> pageSeeds = new LinkedHashMap<>();
        for (OptionGroup group : groups) {
            for (DialogOptionComponentYPanel component : group.components()) {
                GameOptionsPresentation.Location location = GameOptionsPresentation.location(
                      group.id(), component.getOption().getName());
                pageSeeds.computeIfAbsent(location.page(), PageSeed::new).add(group, component, location);
            }
        }
        List<PageSeed> orderedPageSeeds = new ArrayList<>(pageSeeds.values());
        orderedPageSeeds.sort(Comparator.comparingInt(page -> page.definition().order()));

        List<SettingsRoute> routes = new ArrayList<>();
        Map<String, Supplier<Component>> pageFactories = new LinkedHashMap<>();
        for (PageSeed pageSeed : orderedPageSeeds) {
            String pageTitle = pageSeed.definition().title(pageSeed.primarySourceGroupDisplayName());
            GroupPage page = new GroupPage(pageSeed, pageTitle, optionVisibility);
            SettingsRoute route = new SettingsRoute(pageSeed.definition().routeId(),
                  pageSeed.definition().path(pageSeed.primarySourceGroupDisplayName()),
                  pageSeed.definition().pathIds(), pageSeed.searchAliases(), true);
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
        String normalizedFilter = SettingsRoute.normalizeSearchText(Objects.requireNonNullElse(filterText, ""));
        for (GroupPage page : pages) {
            page.refreshVisibility(normalizedFilter);
        }
    }

    /** Re-evaluates option visibility after the unofficial-options setting changes. */
    public void refreshVisibility() {
        String filter = settingsPane.getActiveFilter();
        for (GroupPage page : pages) {
            page.refreshVisibility(filter);
        }
        settingsPane.refreshFilter();
    }

    public static List<SettingsBadge> legendEntries() {
        return List.of(IMPORTANT_BADGE, ADVANCED_BADGE, UNOFFICIAL_BADGE);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension preferred = super.getPreferredSize();
        int floorWidth = UIUtil.scaleForGUI(SettingsNavigationPanel.DEFAULT_NAVIGATION_WIDTH)
              + UIUtil.scaleForGUI(SettingsPagePanel.DEFAULT_MAXIMUM_PAGE_WIDTH);
        int floorHeight = UIUtil.scaleForGUI(START_HEIGHT);
        return new Dimension(Math.max(preferred.width, floorWidth), Math.max(preferred.height, floorHeight));
    }

    /** One game-options navigation route and its staged editor components. */
    public record OptionGroup(String id, String displayName, List<DialogOptionComponentYPanel> components) {
        public OptionGroup {
            Objects.requireNonNull(id);
            Objects.requireNonNull(displayName);
            components = List.copyOf(components);
        }
    }

    private static final class PageSeed {
        private final GameOptionsPresentation.PageDefinition definition;
        private final Map<String, String> sourceGroups = new LinkedHashMap<>();
        private final List<OptionPlacement> placements = new ArrayList<>();

        private PageSeed(GameOptionsPresentation.PageDefinition definition) {
            this.definition = definition;
        }

        private void add(OptionGroup group, DialogOptionComponentYPanel component,
              GameOptionsPresentation.Location location) {
            sourceGroups.put(group.id(), group.displayName());
            placements.add(new OptionPlacement(component, location.sectionId(), location.sectionOrder()));
        }

        private GameOptionsPresentation.PageDefinition definition() {
            return definition;
        }

        private String primarySourceGroupDisplayName() {
            return sourceGroups.values().iterator().next();
        }

        private List<String> searchAliases() {
            List<String> aliases = new ArrayList<>();
            sourceGroups.forEach((id, displayName) -> {
                aliases.add(id);
                aliases.add(displayName);
            });
            return aliases;
        }
    }

    private record OptionPlacement(DialogOptionComponentYPanel component, String sectionId, int sectionOrder) {
    }

    private static final class SectionRows {
        private final int order;
        private final List<OptionRow> rows = new ArrayList<>();

        private SectionRows(int order) {
            this.order = order;
        }
    }

    private static class GroupPage extends JPanel implements SettingsFilterable {
        private final List<OptionRow> rows;
        private final Predicate<IOption> optionVisibility;
        private final String groupSearchableText;
        private final SettingsPagePanel pagePanel;
        private SettingsRoute route;

          private GroupPage(PageSeed pageSeed, String pageTitle, Predicate<IOption> optionVisibility) {
            super(new BorderLayout());
            this.optionVisibility = optionVisibility;
            groupSearchableText = SettingsRoute.normalizeSearchText(String.join(" ", pageSeed.searchAliases()));
            setName("gameOptions" + pageSeed.definition().id() + "Page");

            rows = pageSeed.placements.stream().map(placement -> OptionRow.create(placement.component())).toList();
            Map<String, SectionRows> sectionRows = new LinkedHashMap<>();
            for (int index = 0; index < rows.size(); index++) {
                OptionPlacement placement = pageSeed.placements.get(index);
                SectionRows section = sectionRows.computeIfAbsent(placement.sectionId(),
                    ignored -> new SectionRows(placement.sectionOrder()));
                section.rows.add(rows.get(index));
            }

            Icon icon = pageIcon(pageSeed.definition().iconGroupId());
            SettingsPagePanel.Builder builder = SettingsPagePanel.builder(pageSeed.definition().id(), TEXT,
                        "GameOptionsDialog.title", icon)
                .header(new SettingsHeaderPanel(pageSeed.definition().id(), pageTitle, icon))
                  .showDetailsPanel(true)
                  .sectionsExpandedByDefault(sectionRows.size() == 1)
                  .standardContentWidth();
            List<Map.Entry<String, SectionRows>> orderedSections = new ArrayList<>(sectionRows.entrySet());
            orderedSections.sort(Comparator.comparingInt(entry -> entry.getValue().order));
            for (Map.Entry<String, SectionRows> entry : orderedSections) {
                SettingsFormPanel content = createSectionContent(pageSeed.definition().id(),
                    pageSeed.definition().id() + entry.getKey(), entry.getValue().rows);
                List<String> aliases = new ArrayList<>();
                aliases.addAll(pageSeed.searchAliases());
                entry.getValue().rows.forEach(row -> aliases.add(row.searchableText()));
                builder.literalSection(sectionTitle(entry.getKey()), sectionSummary(entry.getKey()), content,
                    sectionBadges(pageSeed.definition().advanced()), aliases);
            }
            pagePanel = builder.build();
            add(pagePanel, BorderLayout.CENTER);
        }

        private static SettingsFormPanel createSectionContent(String pageId, String name, List<OptionRow> rows) {
            SettingsFormPanel content = new SettingsFormPanel(name,
                  DialogOptionComponentYPanel.SETTINGS_GRID_CELL_WIDTH);
            if (pageId.equals("gameMaster")) {
                for (OptionRow row : rows) {
                    if (row.option().getName().equals(OptionsConstants.GAME_MASTER_ALLOW)) {
                        row.component().useStandaloneCheckBoxRowLayout();
                    } else if (row.option().getName().equals(OptionsConstants.GAME_MASTER_VOTE_THRESHOLD)) {
                        row.component().useLabeledControlRowLayout();
                    }
                    content.addFullWidthComponent(row.component());
                }
                return content;
            }

            int cellWidth = UIUtil.scaleForGUI(DialogOptionComponentYPanel.SETTINGS_GRID_CELL_WIDTH);
            DialogOptionComponentYPanel[] components = rows.stream()
                  .map(OptionRow::component)
                  .toArray(DialogOptionComponentYPanel[]::new);
            for (DialogOptionComponentYPanel component : components) {
                component.useSettingsGridCellLayout();
                int contentWidth = component.getOption().getType() == IOption.BOOLEAN
                      ? UIUtil.scaleForGUI(SettingsFormPanel.DEFAULT_LABEL_WIDTH)
                      : cellWidth;
                component.fitToWidth(contentWidth);
            }
            content.addEqualWidthComponentGrid(2, components);
            return content;
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
            if (normalizedFilter.isBlank()) {
                pagePanel.collapseAllSections();
            } else {
                boolean matched = pagePanel.expandSectionsMatching(text -> sectionMatches(text, normalizedFilter));
                if (!matched) {
                    pagePanel.expandAllSections();
                }
            }
            revalidate();
            repaint();
        }

        @Override
        public void applySettingsFilter(String normalizedFilter) {
            refreshVisibility(normalizedFilter);
        }
    }

    private static List<SettingsBadge> sectionBadges(boolean advanced) {
        return advanced ? List.of(ADVANCED_BADGE) : List.of();
    }

    private static List<SettingsBadge> optionBadges(IOption option) {
        List<SettingsBadge> badges = new ArrayList<>();
        if (hasShortLabel(option)) {
            badges.add(IMPORTANT_BADGE);
        }
        if (isUnofficial(option)) {
            badges.add(UNOFFICIAL_BADGE);
        }
        return badges;
    }

    private static boolean hasShortLabel(IOption option) {
        return TEXT.containsKey(shortLabelKey(option));
    }

    private static String optionDisplayName(IOption option) {
        return hasShortLabel(option) ? TEXT.getText(shortLabelKey(option)) : option.getDisplayableName();
    }

    private static String shortLabelKey(IOption option) {
        return "GameOptionsDialog.option." + option.getName() + ".shortName";
    }

    private static boolean isUnofficial(IOption option) {
        return option.getName().startsWith("unoff") || option.getDisplayableName().contains("Unofficial");
    }

    private static String sectionTitle(String sectionId) {
        return getString("GameOptionsDialog.section." + sectionId + ".title");
    }

    private static String sectionSummary(String sectionId) {
        return getString("GameOptionsDialog.section." + sectionId + ".summary");
    }

    private static boolean sectionMatches(String text, String normalizedFilter) {
        String normalizedText = SettingsRoute.normalizeSearchText(text);
        for (String token : normalizedFilter.split("\\s+")) {
            if (!normalizedText.contains(token)) {
                return false;
            }
        }
        return true;
    }

    static String sectionId(String groupId, String optionName) {
        return GameOptionsPresentation.location(groupId, optionName).sectionId();
    }

    private static Icon pageIcon(String groupId) {
        return PAGE_HEADER_ICONS.computeIfAbsent(groupId, ignored -> {
            File factionsDir = new File(Configuration.universeImagesDir(), "factions");
            File logo = new File(factionsDir, PAGE_FACTION_LOGOS.getOrDefault(groupId, "logo_star_league.png"));
            ImageIcon scaled = scaleImageIcon(new ImageIcon(logo.getAbsolutePath()), HEADER_IMAGE_SIZE, true);
            return addTintToImageIcon(scaled.getImage(), Color.BLACK);
        });
    }

    static Map<String, String> factionLogos() {
        return PAGE_FACTION_LOGOS;
    }

    private record OptionRow(DialogOptionComponentYPanel component, IOption option, String searchableText) {
        private static OptionRow create(DialogOptionComponentYPanel component) {
            IOption option = component.getOption();
            String displayName = optionDisplayName(option);
            component.setSettingsPresentation(displayName, optionBadges(option));
            return new OptionRow(component, option, SettingsRoute.normalizeSearchText(
                  option.getName() + ' ' + option.getDisplayableName() + ' ' + displayName + ' '
                        + option.getDescription()));
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
