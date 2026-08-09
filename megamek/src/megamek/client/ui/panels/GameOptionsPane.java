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
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import megamek.client.ui.clientGUI.GUIPreferences;
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
import megamek.common.MMRandom;
import megamek.common.options.IOption;
import megamek.common.options.OptionsConstants;
import megamek.common.weapons.bayWeapons.capital.CapitalMissileBayWeapon;

/** Searchable settings-tree presentation for metadata-backed game option groups. */
public class GameOptionsPane extends JPanel {
    private static final int START_HEIGHT = 800;
    private static final int HEADER_IMAGE_SIZE = 80;
    private static final int MAX_VICTORY_CONDITIONS = 100;
    private static final int MAX_VICTORY_PERCENT = 100;
    private static final int MAX_VICTORY_RATIO_PERCENT = 10_000;
    private static final int MAX_VICTORY_ROUNDS = 10_000;
    private static final int MAX_VICTORY_KILLS = 10_000;
    private static final int MAX_BRIDGE_CF = 1_000;
    private static final int MAX_GHOST_TARGET_PENALTY = 10;
    private static final int MAX_WOODS_BURN_DOWN_CF = 100;
    private static final int MAX_ARTILLERY_AUTOHIT_HEXES = 100;
    private static final int MAX_ARTILLERY_MAP_AREA_HEXES = 100_000;
    private static final int MAX_EXTERNAL_HEAT = 100;
    private static final int MAX_LANCE_SIZE = 100;
    private static final int IMPORTANT_ICON = 0xE002;
    private static final int ADVANCED_ICON = 0xE8B8;
    private static final int UNOFFICIAL_ICON = 0xEA4B;
    private static final int LEGACY_ICON = 0xE889;
    private static final Color UNOFFICIAL_COLOR = new Color(0xE6, 0x9F, 0x00);
    private static final SettingsTextProvider TEXT = SettingsTextProvider.megaMek();
    private static final SettingsBadge IMPORTANT_BADGE = new SettingsBadge(IMPORTANT_ICON, null,
          getString("GameOptionsDialog.legend.important"));
    private static final SettingsBadge ADVANCED_BADGE = new SettingsBadge(ADVANCED_ICON, null,
          getString("GameOptionsDialog.legend.advanced"));
    private static final SettingsBadge UNOFFICIAL_BADGE = new SettingsBadge(UNOFFICIAL_ICON, UNOFFICIAL_COLOR,
          getString("GameOptionsDialog.legend.unofficial"));
        private static final SettingsBadge LEGACY_BADGE = new SettingsBadge(LEGACY_ICON, null,
            getString("GameOptionsDialog.legend.legacy"));
        private static final Set<String> LEGACY_OPTIONS = Set.of(
            OptionsConstants.ADVANCED_GHOST_TARGET_MODE,
            OptionsConstants.ADVANCED_GHOST_TARGET_MAX,
            OptionsConstants.ADVANCED_ASSAULT_DROP,
            OptionsConstants.ADVANCED_PARATROOPERS,
            OptionsConstants.ADVANCED_MAX_TECH_MOVEMENT_MODS,
            OptionsConstants.ADVANCED_GROUND_MOVEMENT_AUTO_ABANDON_UNIT,
            OptionsConstants.RPG_RPG_GUNNERY);
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
        this(groups, optionVisibility, Set.of());
    }

    public GameOptionsPane(List<OptionGroup> groups, Predicate<IOption> optionVisibility,
          Set<String> excludedOptionNames) {
        super(new BorderLayout());
        setName("gameOptionsPane");
        Objects.requireNonNull(optionVisibility);
        Set<String> excludedNames = Set.copyOf(excludedOptionNames);

        Map<GameOptionsPresentation.PageDefinition, PageSeed> pageSeeds = new LinkedHashMap<>();
        Map<String, DialogOptionComponentYPanel> componentsByName = new LinkedHashMap<>();
        for (OptionGroup group : groups) {
            for (DialogOptionComponentYPanel component : group.components()) {
                    if ((component.getOption().getName().equals(OptionsConstants.BASE_HIDE_UNOFFICIAL)
                        || component.getOption().getName().equals(OptionsConstants.BASE_HIDE_LEGACY))
                      || excludedNames.contains(component.getOption().getName())) {
                    continue;
                }
                componentsByName.put(component.getOption().getName(), component);
                GameOptionsPresentation.Location location = GameOptionsPresentation.location(
                      group.id(), component.getOption().getName());
                pageSeeds.computeIfAbsent(location.page(), PageSeed::new).add(group, component, location);
            }
        }
        configureSettingsDependencies(componentsByName);
        List<PageSeed> orderedPageSeeds = new ArrayList<>(pageSeeds.values());
        orderedPageSeeds.sort(Comparator.comparingInt(page -> page.definition().order()));

        List<SettingsRoute> routes = new ArrayList<>();
        Map<String, Supplier<Component>> pageFactories = new LinkedHashMap<>();
        for (PageSeed pageSeed : orderedPageSeeds) {
                        String pageTitle = pageSeed.title();
            GroupPage page = new GroupPage(pageSeed, pageTitle, optionVisibility);
            SettingsRoute route = new SettingsRoute(pageSeed.definition().routeId(),
                                    pageSeed.path(),
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
          settingsPane = new SettingsPane(routes, pageFactories, navigationText);
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

        private static void configureSettingsDependencies(
            Map<String, DialogOptionComponentYPanel> componentsByName) {
          configureEditableDependency(componentsByName, OptionsConstants.ADVANCED_WOODS_BURN_DOWN_AMOUNT,
              OptionsConstants.ADVANCED_WOODS_BURN_DOWN);
          configureEditableDependency(componentsByName,
              OptionsConstants.ADVANCED_GROUND_MOVEMENT_MEK_LANCE_MOVEMENT_NUMBER,
              OptionsConstants.ADVANCED_GROUND_MOVEMENT_MEK_LANCE_MOVEMENT);
          configureEditableDependency(componentsByName,
              OptionsConstants.ADVANCED_GROUND_MOVEMENT_VEHICLE_LANCE_MOVEMENT_NUMBER,
              OptionsConstants.ADVANCED_GROUND_MOVEMENT_VEHICLE_LANCE_MOVEMENT);

          DialogOptionComponentYPanel infantryAndProtoMekGroupSize = componentsByName.get(
              OptionsConstants.INIT_INF_PROTO_MOVE_MULTI);
          DialogOptionComponentYPanel moveMultipleInfantry = componentsByName.get(
              OptionsConstants.INIT_INF_MOVE_MULTI);
          DialogOptionComponentYPanel moveMultipleProtoMeks = componentsByName.get(
              OptionsConstants.INIT_PROTOMEKS_MOVE_MULTI);
          if (infantryAndProtoMekGroupSize != null && moveMultipleInfantry != null
              && moveMultipleProtoMeks != null) {
            infantryAndProtoMekGroupSize.setEditableWhenAnySelected(
                moveMultipleInfantry, moveMultipleProtoMeks);
          }

          DialogOptionComponentYPanel assaultDrop = componentsByName.get(OptionsConstants.ADVANCED_ASSAULT_DROP);
          DialogOptionComponentYPanel paratroopers = componentsByName.get(OptionsConstants.ADVANCED_PARATROOPERS);
          if (assaultDrop != null && paratroopers != null) {
            paratroopers.setSelectableWhenSelected(assaultDrop);
          }
        }

        private static void configureEditableDependency(
            Map<String, DialogOptionComponentYPanel> componentsByName,
            String dependentOptionName, String controllingOptionName) {
          DialogOptionComponentYPanel dependent = componentsByName.get(dependentOptionName);
          DialogOptionComponentYPanel controlling = componentsByName.get(controllingOptionName);
          if (dependent != null && controlling != null) {
            dependent.setEditableWhenSelected(controlling);
          }
        }

    public static List<SettingsBadge> legendEntries() {
        return List.of(IMPORTANT_BADGE, ADVANCED_BADGE, UNOFFICIAL_BADGE, LEGACY_BADGE);
    }

    public static SettingsBadge unofficialBadge() {
        return UNOFFICIAL_BADGE;
    }

    public static SettingsBadge legacyBadge() {
        return LEGACY_BADGE;
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
            placements.add(new OptionPlacement(component, location.sectionId(), location.sectionOrder(),
                location.optionOrder()));
        }

        private GameOptionsPresentation.PageDefinition definition() {
            return definition;
        }

        private String title() {
            return definition.title(sourceGroups);
        }

        private List<String> path() {
            return definition.path(sourceGroups);
        }

        private String effectiveIconGroupId() {
            return definition.effectiveIconGroupId(sourceGroups);
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

        private record OptionPlacement(DialogOptionComponentYPanel component, String sectionId, int sectionOrder,
                    int optionOrder) {
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

            rows = pageSeed.placements.stream()
                  .map(placement -> OptionRow.create(placement.component(), placement.optionOrder()))
                  .toList();
            Map<String, SectionRows> sectionRows = new LinkedHashMap<>();
            for (int index = 0; index < rows.size(); index++) {
                OptionPlacement placement = pageSeed.placements.get(index);
                SectionRows section = sectionRows.computeIfAbsent(placement.sectionId(),
                    ignored -> new SectionRows(placement.sectionOrder()));
                section.rows.add(rows.get(index));
            }
            sectionRows.values().forEach(section ->
                  section.rows.sort(Comparator.comparingInt(row -> row.optionOrder())));

            Icon icon = pageIcon(pageSeed.effectiveIconGroupId());
            SettingsPagePanel.Builder builder = SettingsPagePanel.builder(pageSeed.definition().id(), TEXT,
                        "GameOptionsDialog.title", icon)
                .header(new SettingsHeaderPanel(pageSeed.definition().id(), pageTitle, icon))
                  .showDetailsPanel(true)
                  .sectionsExpandedByDefault(sectionRows.size() == 1)
                  .standardContentWidth();
            List<Map.Entry<String, SectionRows>> orderedSections = new ArrayList<>(sectionRows.entrySet());
            orderedSections.sort(Comparator.comparingInt(entry -> entry.getValue().order));
            int labelWidth = sharedLabelWidth(rows);
            for (Map.Entry<String, SectionRows> entry : orderedSections) {
                SettingsFormPanel content = createSectionContent(
                      pageSeed.definition().id() + entry.getKey(), entry.getValue().rows, labelWidth);
                List<String> aliases = new ArrayList<>();
                aliases.addAll(pageSeed.searchAliases());
                entry.getValue().rows.forEach(row -> aliases.add(row.searchableText()));
                builder.literalSection(sectionTitle(entry.getKey()), sectionSummary(entry.getKey()), content,
                    sectionBadges(pageSeed.definition().advanced()), aliases);
            }
            pagePanel = builder.build();
            add(pagePanel, BorderLayout.CENTER);
        }

        private static int sharedLabelWidth(List<OptionRow> rows) {
            int width = UIUtil.scaleForGUI(SettingsFormPanel.DEFAULT_LABEL_WIDTH);
            for (OptionRow row : rows) {
                Component label = row.option().getType() == IOption.BOOLEAN
                      ? row.component().settingsCheckBox()
                      : row.component().settingsLabel();
                width = Math.max(width, label.getPreferredSize().width);
            }
            return (int) Math.ceil(width / GUIPreferences.getInstance().getGUIScale());
        }

        private static SettingsFormPanel createSectionContent(String name, List<OptionRow> rows, int labelWidth) {
            SettingsFormPanel content = new SettingsFormPanel(name,
                  labelWidth, SettingsFormPanel.DEFAULT_CONTROL_WIDTH);
            List<JCheckBox> checkBoxes = new ArrayList<>();
            for (OptionRow row : rows) {
                DialogOptionComponentYPanel component = row.component();
                if (row.option().getType() == IOption.BOOLEAN) {
                    checkBoxes.add(component.settingsCheckBox());
                    continue;
                }
                addCheckBoxes(content, checkBoxes);
                configureSettingsControl(component);
                content.addRow(component.settingsLabel(), component.settingsControl());
            }
            addCheckBoxes(content, checkBoxes);
            return content;
        }

        private static void configureSettingsControl(DialogOptionComponentYPanel component) {
            switch (component.getOption().getName()) {
                case OptionsConstants.BASE_TURN_TIMER_TARGETING,
                     OptionsConstants.BASE_TURN_TIMER_MOVEMENT,
                     OptionsConstants.BASE_TURN_TIMER_FIRING,
                     OptionsConstants.BASE_TURN_TIMER_PHYSICAL -> component.useIntegerSpinner(0);
                case OptionsConstants.BASE_DUMPING_FROM_ROUND,
                     OptionsConstants.BASE_MAX_NUMBER_ROUND_SAVES -> component.useIntegerSpinner(1);
                case OptionsConstants.BASE_RNG_TYPE -> component.useIntegerChoice(rngTypeChoices());
                case OptionsConstants.BASE_BRIDGE_CF -> component.useIntegerSpinner(0, MAX_BRIDGE_CF);
                case OptionsConstants.ADVANCED_GHOST_TARGET_MAX ->
                    component.useIntegerSpinner(0, MAX_GHOST_TARGET_PENALTY);
                case OptionsConstants.ADVANCED_WOODS_BURN_DOWN_AMOUNT ->
                    component.useIntegerSpinner(1, MAX_WOODS_BURN_DOWN_CF);
                case OptionsConstants.ADVANCED_COMBAT_NUM_HEXES_PREDESIGNATE ->
                    component.useIntegerSpinner(0, MAX_ARTILLERY_AUTOHIT_HEXES);
                case OptionsConstants.ADVANCED_COMBAT_MAP_AREA_PREDESIGNATE ->
                    component.useIntegerSpinner(1, MAX_ARTILLERY_MAP_AREA_HEXES);
                case OptionsConstants.ADVANCED_COMBAT_MAX_EXTERNAL_HEAT ->
                    component.useIntegerSpinner(0, MAX_EXTERNAL_HEAT);
                case OptionsConstants.ADVANCED_GROUND_MOVEMENT_MEK_LANCE_MOVEMENT_NUMBER ->
                    component.useIntegerSpinner(1, MAX_LANCE_SIZE);
                case OptionsConstants.ADVANCED_GROUND_MOVEMENT_VEHICLE_LANCE_MOVEMENT_NUMBER ->
                    component.useIntegerSpinner(1, MAX_LANCE_SIZE);
                case OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_BEARINGS_ONLY_VELOCITY ->
                    component.useIntegerSpinner(CapitalMissileBayWeapon.CAPITAL_MISSILE_MIN_VELOCITY,
                          CapitalMissileBayWeapon.CAPITAL_MISSILE_MAX_VELOCITY);
                case OptionsConstants.INIT_INF_PROTO_MOVE_MULTI ->
                    component.useIntegerSpinner(1, MAX_LANCE_SIZE);
                case OptionsConstants.VICTORY_ACHIEVE_CONDITIONS ->
                    component.useIntegerSpinner(1, MAX_VICTORY_CONDITIONS);
                case OptionsConstants.VICTORY_BV_DESTROYED_PERCENT ->
                    component.useIntegerSpinner(1, MAX_VICTORY_PERCENT);
                case OptionsConstants.VICTORY_BV_RATIO_PERCENT ->
                    component.useIntegerSpinner(1, MAX_VICTORY_RATIO_PERCENT);
                case OptionsConstants.VICTORY_GAME_TURN_LIMIT ->
                    component.useIntegerSpinner(1, MAX_VICTORY_ROUNDS);
                case OptionsConstants.VICTORY_GAME_KILL_COUNT ->
                    component.useIntegerSpinner(1, MAX_VICTORY_KILLS);
                default -> {
                }
            }
        }

        private static Map<Integer, String> rngTypeChoices() {
            Map<Integer, String> choices = new LinkedHashMap<>();
            choices.put(MMRandom.R_SUN, getString("GameOptionsDialog.rngType.sunRandom"));
            choices.put(MMRandom.R_CRYPTO, getString("GameOptionsDialog.rngType.cryptoRandom"));
            choices.put(MMRandom.R_POOL36, getString("GameOptionsDialog.rngType.pool36Random"));
            return choices;
        }

        private static void addCheckBoxes(SettingsFormPanel content, List<JCheckBox> checkBoxes) {
            if (checkBoxes.isEmpty()) {
                return;
            }
            if (checkBoxes.size() == 1) {
                content.addCheckBox(checkBoxes.getFirst());
            } else {
                content.addCheckBoxGrid(2, checkBoxes.toArray(JCheckBox[]::new));
            }
            checkBoxes.clear();
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

    private static List<SettingsBadge> sectionBadges(boolean advanced) {
        return advanced ? List.of(ADVANCED_BADGE) : List.of();
    }

    private static List<SettingsBadge> optionBadges(IOption option) {
        List<SettingsBadge> badges = new ArrayList<>();
        if (hasShortLabel(option)) {
            badges.add(IMPORTANT_BADGE);
        }
        if (isUnofficialOption(option)) {
            badges.add(UNOFFICIAL_BADGE);
        }
        if (isLegacyOption(option)) {
            badges.add(LEGACY_BADGE);
        }
        return badges;
    }

    private static boolean hasShortLabel(IOption option) {
        return TEXT.containsKey(shortLabelKey(option));
    }

    private static String optionDisplayName(IOption option) {
        String displayName = hasShortLabel(option) ? TEXT.getText(shortLabelKey(option)) : option.getDisplayableName();
        return displayName.replaceFirst("(?i)^\\(Unofficial\\)\\s*", "")
              .replaceFirst("(?i)^Unofficial:\\s*", "")
              .replaceFirst("(?i)\\s*\\(Unofficial\\)\\s*", " ")
              .replaceFirst("(?i)^\\(Legacy(?:\\s+MaxTech)?\\)\\s*", "")
              .replaceFirst("(?i)\\s*\\(Legacy(?:\\s+only)?\\)\\s*", " ")
              .replaceFirst("(?i)\\s*\\[Legacy]\\s*", " ")
              .trim();
    }

    private static String shortLabelKey(IOption option) {
        return "GameOptionsDialog.option." + option.getName() + ".shortName";
    }

    public static boolean isUnofficialOption(IOption option) {
        return option.getName().startsWith("unoff") || option.getDisplayableName().contains("Unofficial");
    }

    public static boolean isLegacyOption(IOption option) {
        return LEGACY_OPTIONS.contains(option.getName());
    }

    private static String sectionTitle(String sectionId) {
        return getString("GameOptionsDialog.section." + sectionId + ".title");
    }

    private static String sectionSummary(String sectionId) {
        return getString("GameOptionsDialog.section." + sectionId + ".summary");
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

    private record OptionRow(DialogOptionComponentYPanel component, IOption option, String searchableText,
          int optionOrder) {
        private static OptionRow create(DialogOptionComponentYPanel component, int optionOrder) {
            IOption option = component.getOption();
            String displayName = optionDisplayName(option);
            component.setSettingsPresentation(displayName, optionBadges(option));
            String detailsKey = "GameOptionsDialog.option." + option.getName() + ".details";
            if (TEXT.containsKey(detailsKey)) {
                component.setSettingsHelpText(TEXT.getText(detailsKey));
            }
            return new OptionRow(component, option, SettingsRoute.normalizeSearchText(
                option.getName() + ' ' + option.getDisplayableName() + ' ' + displayName), optionOrder);
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
