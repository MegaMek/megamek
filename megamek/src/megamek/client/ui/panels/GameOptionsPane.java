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

/** Searchable settings-tree presentation for metadata-backed game option groups. */
public class GameOptionsPane extends JPanel {
    private static final int START_HEIGHT = 800;
    private static final int HEADER_IMAGE_SIZE = 80;
    private static final int ADVANCED_ICON = 0xE8B8;
    private static final int UNOFFICIAL_ICON = 0xE002;
    private static final SettingsTextProvider TEXT = SettingsTextProvider.megaMek();
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

    public static List<SettingsBadge> legendEntries() {
        return List.of(ADVANCED_BADGE, UNOFFICIAL_BADGE);
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

    private static class GroupPage extends JPanel implements SettingsFilterable {
        private final List<OptionRow> rows;
        private final Predicate<IOption> optionVisibility;
        private final String groupSearchableText;
        private final SettingsPagePanel pagePanel;
        private SettingsRoute route;

        private GroupPage(OptionGroup group, Predicate<IOption> optionVisibility) {
            super(new BorderLayout());
            this.optionVisibility = optionVisibility;
            groupSearchableText = SettingsRoute.normalizeSearchText(group.id() + ' ' + group.displayName());
            setName("gameOptions" + group.id() + "Page");

            rows = group.components().stream().map(OptionRow::create).toList();
            Map<String, List<OptionRow>> sectionRows = new LinkedHashMap<>();
            for (OptionRow row : rows) {
                sectionRows.computeIfAbsent(sectionId(group.id(), row.option().getName()), ignored -> new ArrayList<>())
                      .add(row);
            }

            Icon icon = pageIcon(group.id());
            SettingsPagePanel.Builder builder = SettingsPagePanel.builder(group.id(), TEXT,
                        "GameOptionsDialog.title", icon)
                  .header(new SettingsHeaderPanel(group.id(), group.displayName(), icon))
                  .showDetailsPanel(true)
                  .sectionsExpandedByDefault(sectionRows.size() == 1)
                  .standardContentWidth();
            for (Map.Entry<String, List<OptionRow>> entry : sectionRows.entrySet()) {
                SettingsFormPanel content = createSectionContent(group.id() + entry.getKey(), entry.getValue());
                List<String> aliases = new ArrayList<>();
                aliases.add(group.id());
                aliases.add(group.displayName());
                entry.getValue().forEach(row -> aliases.add(row.searchableText()));
                builder.literalSection(sectionTitle(entry.getKey()), sectionSummary(entry.getKey()), content,
                        sectionBadges(group.id()), aliases);
            }
            pagePanel = builder.build();
            add(pagePanel, BorderLayout.CENTER);
        }

        private static SettingsFormPanel createSectionContent(String name, List<OptionRow> rows) {
            SettingsFormPanel content = new SettingsFormPanel(name, SettingsFormPanel.DEFAULT_LABEL_WIDTH);
            int cellWidth = UIUtil.scaleForGUI(SettingsFormPanel.DEFAULT_LABEL_WIDTH);
            DialogOptionComponentYPanel[] components = rows.stream()
                  .map(OptionRow::component)
                  .toArray(DialogOptionComponentYPanel[]::new);
            for (DialogOptionComponentYPanel component : components) {
                component.fitToWidth(cellWidth);
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

    private static List<SettingsBadge> sectionBadges(String groupId) {
        return groupId.startsWith("advanced") ? List.of(ADVANCED_BADGE) : List.of();
    }

    private static List<SettingsBadge> optionBadges(IOption option) {
        return isUnofficial(option) ? List.of(UNOFFICIAL_BADGE) : List.of();
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
        return switch (groupId) {
            case "basic" -> classifyBasic(optionName);
            case "gameMaster" -> "gameMaster.control";
            case "victory" -> classifyVictory(optionName);
            case "allowedUnits" -> classifyAllowedUnits(optionName);
            case "advancedRules" -> classifyAdvancedRules(optionName);
            case "advancedCombat" -> classifyAdvancedCombat(optionName);
            case "advancedGroundMovement" -> classifyGroundMovement(optionName);
            case "advancedAeroRules" -> classifyAero(optionName);
            case "initiative" -> classifyInitiative(optionName);
            case "rpg" -> classifyRpg(optionName);
            default -> groupId + ".general";
        };
    }

    private static String classifyBasic(String name) {
        if (containsAny(name, "playtest", "twrules")) {
            return "basic.testing";
        }
        if (containsAny(name, "show_", "lobby_")) {
            return "basic.display";
        }
        return "basic.rules";
    }

    private static String classifyVictory(String name) {
        if (name.contains("bv_")) {
            return "victory.battleValue";
        }
        if (containsAny(name, "turn_limit", "kill_count", "commander_killed")) {
            return "victory.alternate";
        }
        return "victory.conditions";
    }

    private static String classifyAllowedUnits(String name) {
        if (containsAny(name, "canon", "year", "techlevel", "era_based")) {
            return "allowedUnits.availability";
        }
        return "allowedUnits.restrictions";
    }

    private static String classifyAdvancedRules(String name) {
        if (containsAny(name, "sensor", "blind", "vision", "ecm", "ghost", "bap", "magscan")) {
            return "advancedRules.sensors";
        }
        if (containsAny(name, "mine", "ice", "woods", "ignite", "lightning", "bridge")) {
            return "advancedRules.terrain";
        }
        if (containsAny(name, "eject", "abandon")) {
            return "advancedRules.ejection";
        }
        if (containsAny(name, "infantry", "ba_", "paratrooper", "assault_drop")) {
            return "advancedRules.specialUnits";
        }
        return "advancedRules.general";
    }

    private static String classifyAdvancedCombat(String name) {
        if (containsAny(name, "ammo", "ams", "rapid_ac", "uac", "ppc", "gauss", "energy_weapon", "hotload")) {
            return "advancedCombat.weapons";
        }
        if (containsAny(name, "crit", "damage", "heat", "explosion", "rotor", "cluster")) {
            return "advancedCombat.damage";
        }
        if (containsAny(name, "los", "range", "dead_zone", "cover", "predesignate", "scatter")) {
            return "advancedCombat.targeting";
        }
        if (containsAny(name, "vehicle", "vtol", "proto", "ba_")) {
            return "advancedCombat.units";
        }
        return "advancedCombat.attacks";
    }

    private static String classifyGroundMovement(String name) {
        if (containsAny(name, "vehicle", "reverse", "hover", "premove", "immobile")) {
            return "advancedGroundMovement.vehicles";
        }
        if (containsAny(name, "infantry", "physical", "leg_damage", "walk_backwards")) {
            return "advancedGroundMovement.infantry";
        }
        return "advancedGroundMovement.meks";
    }

    private static String classifyAero(String name) {
        if (containsAny(name, "sensor", "ecm", "aaa", "pointdef", "bracket", "over_penetrate", "damage_thresh")) {
            return "advancedAeroRules.targeting";
        }
        if (containsAny(name, "bomb", "nuke", "dropship", "fuel", "kf_", "ammo_explosion")) {
            return "advancedAeroRules.vessels";
        }
        return "advancedAeroRules.flight";
    }

    private static String classifyInitiative(String name) {
        if (name.startsWith("inf_")) {
            return "initiative.infantry";
        }
        if (name.startsWith("proto")) {
            return "initiative.protomeks";
        }
        return "initiative.simultaneous";
    }

    private static String classifyRpg(String name) {
        if (containsAny(name, "initiative", "command_init")) {
            return "rpg.initiative";
        }
        if (containsAny(name, "gunnery", "artillery", "toughness")) {
            return "rpg.skills";
        }
        if (containsAny(name, "shutdown", "ejection")) {
            return "rpg.conditions";
        }
        return "rpg.core";
    }

    private static boolean containsAny(String value, String... fragments) {
        for (String fragment : fragments) {
            if (value.contains(fragment)) {
                return true;
            }
        }
        return false;
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
            component.setSettingsBadges(optionBadges(option));
            return new OptionRow(component, option, SettingsRoute.normalizeSearchText(
                  option.getName() + ' ' + option.getDisplayableName() + ' ' + option.getDescription()));
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
