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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreePath;

import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.DialogOptionListener;
import megamek.client.ui.settings.CollapsibleSectionPanel;
import megamek.client.ui.settings.SettingsFormPanel;
import megamek.client.ui.settings.SettingsNavigationPanel;
import megamek.client.ui.settings.SettingsPagePanel;
import megamek.client.ui.util.UIUtil;
import megamek.common.Configuration;
import megamek.common.options.GameOptions;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
import megamek.common.options.OptionsConstants;
import org.junit.jupiter.api.Test;

class GameOptionsPaneTest {
    private static final String IMPORTANT_SYMBOL = Character.toString(0xE002);
    private static final String ADVANCED_SYMBOL = Character.toString(0xE8B8);
    private static final String UNOFFICIAL_SYMBOL = Character.toString(0xEA4B);

    @Test
    void searchFiltersRowsByOptionNameAndDescription() throws Exception {
        runOnEdt(() -> {
            GameOptions options = new GameOptions();
            DialogOptionComponentYPanel searchlights = component(options.getOption(OptionsConstants.SEARCHLIGHTS_ON));
            DialogOptionComponentYPanel pushOffBoard = component(
                  options.getOption(OptionsConstants.BASE_PUSH_OFF_BOARD));
            GameOptionsPane pane = pane(List.of(searchlights, pushOffBoard), option -> true);

            pane.setFilterText(searchlights.getOption().getDisplayableName());

            assertTrue(searchlights.isVisible());
            assertFalse(pushOffBoard.isVisible());
        });
    }

    @Test
    void groupNameSearchKeepsGroupRowsVisible() throws Exception {
        runOnEdt(() -> {
            GameOptions options = new GameOptions();
            DialogOptionComponentYPanel searchlights = component(options.getOption(OptionsConstants.SEARCHLIGHTS_ON));
            GameOptionsPane pane = pane(List.of(searchlights), option -> true);

            pane.setFilterText("basic");

            assertTrue(searchlights.isVisible());
        });
    }

    @Test
    void refreshingVisibilityHidesExcludedOption() throws Exception {
        runOnEdt(() -> {
            GameOptions options = new GameOptions();
            DialogOptionComponentYPanel searchlights = component(options.getOption(OptionsConstants.SEARCHLIGHTS_ON));
            AtomicBoolean showSearchlights = new AtomicBoolean(true);
            GameOptionsPane pane = pane(List.of(searchlights),
                  option -> !option.getName().equals(OptionsConstants.SEARCHLIGHTS_ON) || showSearchlights.get());

            showSearchlights.set(false);
            pane.refreshVisibility();

            assertFalse(searchlights.isVisible());
        });
    }

    @Test
    void matchSetupUsesClassifiedCollapsedSectionsAndStandardSize() throws Exception {
        runOnEdt(() -> {
            GameOptions options = new GameOptions();
            DialogOptionComponentYPanel deployment = component(
                  options.getOption(OptionsConstants.BASE_EXCLUSIVE_DB_DEPLOYMENT));
            DialogOptionComponentYPanel lobby = component(options.getOption(OptionsConstants.BASE_LOBBY_AMMO_DUMP));
            GameOptionsPane pane = pane(List.of(deployment, lobby), option -> true);

            List<CollapsibleSectionPanel> sections = findSections(pane);
            assertEquals(2, sections.size());
            assertFalse(sections.get(0).isExpanded());
            assertFalse(sections.get(1).isExpanded());
            assertEquals(3, GameOptionsPane.legendEntries().size());
            assertEquals("Tooltip contains important information.",
                  GameOptionsPane.legendEntries().getFirst().description());
            assertTrue(pane.getPreferredSize().width >= UIUtil.scaleForGUI(
                  SettingsNavigationPanel.DEFAULT_NAVIGATION_WIDTH + SettingsPagePanel.DEFAULT_MAXIMUM_PAGE_WIDTH));
            assertTrue(pane.getPreferredSize().height >= UIUtil.scaleForGUI(800));
        });
    }

    @Test
        void matchSetupSectionsShareTwoColumnAlignment() throws Exception {
        runOnEdt(() -> {
            GameOptions options = new GameOptions();
            DialogOptionComponentYPanel wideCoreOption = component(
                  options.getOption(OptionsConstants.BASE_SET_PLAYER_DEPLOYMENT_TO_PLAYER_0));
            DialogOptionComponentYPanel coreFirst = component(
                options.getOption(OptionsConstants.BASE_EXCLUSIVE_DB_DEPLOYMENT));
            DialogOptionComponentYPanel testingFirst = component(
                options.getOption(OptionsConstants.BASE_TEAM_INITIATIVE));
            DialogOptionComponentYPanel testingSecond = component(
                options.getOption(OptionsConstants.BASE_SET_DEFAULT_TEAM_1));
            DialogOptionComponentYPanel displayFirst = component(
                  options.getOption(OptionsConstants.BASE_LOBBY_AMMO_DUMP));
            DialogOptionComponentYPanel displaySecond = component(
                  options.getOption(OptionsConstants.BASE_SHOW_BAY_DETAIL));

            pane(List.of(wideCoreOption, coreFirst, testingFirst, testingSecond, displayFirst, displaySecond),
                  option -> true);

            assertSectionAlignment(wideCoreOption, coreFirst, testingFirst, testingSecond, displayFirst, displaySecond);
            GridBagConstraints wideLayout = ((GridBagLayout) wideCoreOption.getParent().getLayout())
                  .getConstraints(wideCoreOption);
            assertEquals(1, wideLayout.gridwidth);
            assertEquals(GridBagConstraints.HORIZONTAL, wideLayout.fill);
            assertEquals(UIUtil.scaleForGUI(DialogOptionComponentYPanel.SETTINGS_GRID_CELL_WIDTH),
                wideCoreOption.getPreferredSize().width);
        });
    }

    @Test
    void gameMasterUsesCheckboxThenLabeledChoiceRow() throws Exception {
        runOnEdt(() -> {
            GameOptions options = new GameOptions();
            DialogOptionComponentYPanel allowGameMaster = component(
                  options.getOption(OptionsConstants.GAME_MASTER_ALLOW));
            DialogOptionComponentYPanel voteThreshold = component(
                  options.getOption(OptionsConstants.GAME_MASTER_VOTE_THRESHOLD));

            pane("gameMaster", List.of(allowGameMaster, voteThreshold), option -> true);

            assertSame(allowGameMaster.getParent(), voteThreshold.getParent());
            GridBagLayout sectionLayout = (GridBagLayout) allowGameMaster.getParent().getLayout();
            assertCell(sectionLayout, allowGameMaster, 0, 0, 2);
            assertCell(sectionLayout, voteThreshold, 0, 1, 2);

            SettingsFormPanel choiceRow = findComponent(voteThreshold, SettingsFormPanel.class);
            JLabel label = findComponent(choiceRow, JLabel.class);
            JComboBox<?> choice = findComponent(choiceRow, JComboBox.class);
            GridBagLayout choiceLayout = (GridBagLayout) choiceRow.getLayout();
            assertEquals(0, choiceLayout.getConstraints(label).gridx);
            assertEquals(1, choiceLayout.getConstraints(choice).gridx);
            assertSame(choice, label.getLabelFor());

            Container section = allowGameMaster.getParent();
            section.setSize(UIUtil.scaleForGUI(900), section.getPreferredSize().height);
            section.doLayout();
            allowGameMaster.doLayout();
            voteThreshold.doLayout();
            choiceRow.setSize(voteThreshold.getSize());
            choiceRow.doLayout();
            JCheckBox checkBox = findComponent(allowGameMaster, JCheckBox.class);
            int checkBoxX = SwingUtilities.convertPoint(checkBox.getParent(), checkBox.getX(), 0, section).x;
            int labelX = SwingUtilities.convertPoint(label.getParent(), label.getX(), 0, section).x;
            assertEquals(labelX, checkBoxX);
        });
    }

    @Test
    void standardOptionCellsUseConsistentControlOrderAndInsets() throws Exception {
        runOnEdt(() -> {
            GameOptions options = new GameOptions();
            DialogOptionComponentYPanel booleanOption = component(
                  options.getOption(OptionsConstants.SEARCHLIGHTS_ON));
            DialogOptionComponentYPanel numericOption = component(
                  options.getOption(OptionsConstants.BASE_TURN_TIMER_MOVEMENT));
            DialogOptionComponentYPanel choiceOption = component(
                  options.getOption(OptionsConstants.ALLOWED_TECH_LEVEL));

            GameOptionsPane pane = new GameOptionsPane(List.of(
                new GameOptionsPane.OptionGroup("basic", "basic", List.of(booleanOption, numericOption)),
                new GameOptionsPane.OptionGroup("allowedUnits", "allowedUnits", List.of(choiceOption))),
                option -> true);

            assertFirstComponentStartsAtCellEdge(booleanOption, JCheckBox.class);
            int numericControlX = assertLabelControlCell(numericOption, JTextField.class);
            int choiceControlX = assertLabelControlCell(choiceOption, JComboBox.class);
            assertEquals(numericControlX, choiceControlX,
                "numeric control x=" + numericControlX + ", choice control x=" + choiceControlX);
        });
    }

    @Test
    void standardChoiceControlsFitTheirPopulatedValues() throws Exception {
        runOnEdt(() -> {
            GameOptions options = new GameOptions();
            DialogOptionComponentYPanel neuralInterface = component(
                  options.getOption(OptionsConstants.ADVANCED_NEURAL_INTERFACE_MODE));
            neuralInterface.addValue(OptionsConstants.NEURAL_INTERFACE_MODE_OFF);
            neuralInterface.addValue(OptionsConstants.NEURAL_INTERFACE_MODE_PILOT_ONLY);
            neuralInterface.addValue(OptionsConstants.NEURAL_INTERFACE_MODE_FULL_TRACKING);
            DialogOptionComponentYPanel ghostTarget = component(
                  options.getOption(OptionsConstants.ADVANCED_GHOST_TARGET_MODE));
            ghostTarget.addValue(OptionsConstants.GHOST_TARGET_MODE_LEGACY);
            ghostTarget.addValue(OptionsConstants.GHOST_TARGET_MODE_STANDARD);

            List<DialogOptionComponentYPanel> components = List.of(neuralInterface, ghostTarget);
            Map<DialogOptionComponentYPanel, Integer> populatedWidths = new LinkedHashMap<>();
            for (DialogOptionComponentYPanel component : components) {
                populatedWidths.put(component, findDirectComponent(component, JComboBox.class).getPreferredSize().width);
            }

            pane("advancedRules", components, option -> true);

            for (DialogOptionComponentYPanel component : components) {
                int controlWidth = findDirectComponent(component, JComboBox.class).getPreferredSize().width;
                assertTrue(controlWidth >= populatedWidths.get(component),
                      component.getOption().getName() + ": populated width=" + populatedWidths.get(component)
                            + ", settings width=" + controlWidth);
            }
        });
    }

    @Test
    void everyStandardNonBooleanGameOptionUsesTheSharedLabelControlOrder() throws Exception {
        runOnEdt(() -> {
            GameOptions options = new GameOptions();
            List<GameOptionsPane.OptionGroup> groups = new ArrayList<>();
            List<DialogOptionComponentYPanel> nonBooleanComponents = new ArrayList<>();
            for (Enumeration<IOptionGroup> optionGroups = options.getGroups(); optionGroups.hasMoreElements(); ) {
                IOptionGroup optionGroup = optionGroups.nextElement();
                List<DialogOptionComponentYPanel> components = new ArrayList<>();
                for (Enumeration<IOption> groupOptions = optionGroup.getOptions(); groupOptions.hasMoreElements(); ) {
                    IOption option = groupOptions.nextElement();
                    DialogOptionComponentYPanel component = component(option);
                    components.add(component);
                    if (option.getType() != IOption.BOOLEAN
                          && !option.getName().equals(OptionsConstants.GAME_MASTER_VOTE_THRESHOLD)) {
                        nonBooleanComponents.add(component);
                    }
                }
                groups.add(new GameOptionsPane.OptionGroup(optionGroup.getName(), optionGroup.getDisplayableName(),
                      components));
            }

            new GameOptionsPane(groups, option -> true);

            Integer sharedControlX = null;
            for (DialogOptionComponentYPanel component : nonBooleanComponents) {
                Class<? extends Component> controlType = component.getOption().getType() == IOption.CHOICE
                      ? JComboBox.class
                      : JTextField.class;
                int controlX = assertLabelControlCell(component, controlType);
                if (sharedControlX == null) {
                    sharedControlX = controlX;
                } else {
                    assertEquals(sharedControlX, controlX, component.getOption().getName());
                }
            }
            assertTrue(nonBooleanComponents.size() > 20);
        });
    }

    @Test
    void coreRulesUseSelectiveConciseLabelsAndDetailsBadges() throws Exception {
        runOnEdt(() -> {
            GameOptions options = new GameOptions();
            Map<String, String> conciseLabels = Map.ofEntries(
                  Map.entry(OptionsConstants.BASE_DISABLE_LOCAL_SAVE, "Disable Double Blind Local Saves"),
                  Map.entry(OptionsConstants.BASE_EXCLUSIVE_DB_DEPLOYMENT, "Exclusive Double Blind Deployment"),
                  Map.entry(OptionsConstants.BASE_GM_CONTROLS_DONE_REPORT_PHASE, "GM Controls Report Completion"),
                  Map.entry(OptionsConstants.BASE_INFANTRY_DAMAGE_HEAT, "Infantry Flame Weapons Deal Heat"),
                  Map.entry(OptionsConstants.BASE_RESTRICT_GAME_COMMANDS, "Restrict Observer Commands"),
                  Map.entry(OptionsConstants.BASE_SET_ARTY_PLAYER_HOME_EDGE, "Set Artillery Home Edge"),
                  Map.entry(OptionsConstants.BASE_SET_DEFAULT_TEAM_1, "Default Players to Team 1"),
                  Map.entry(OptionsConstants.BASE_SET_PLAYER_DEPLOYMENT_TO_PLAYER_0,
                        "Use Player 0 Deployment Settings"),
                  Map.entry(OptionsConstants.BASE_SUPPRESS_UNIT_TOOLTIP_IN_REPORT_LOG, "Hide Report Log Tooltips"),
                  Map.entry(OptionsConstants.BASE_TURN_TIMER_FIRING, "Firing Phase Turn Timer"),
                  Map.entry(OptionsConstants.BASE_TURN_TIMER_MOVEMENT, "Movement Phase Turn Timer"),
                  Map.entry(OptionsConstants.BASE_TURN_TIMER_PHYSICAL, "Physical Phase Turn Timer"),
                  Map.entry(OptionsConstants.BASE_TURN_TIMER_TARGETING, "Targeting Phase Turn Timer"));
            Map<String, DialogOptionComponentYPanel> conciseComponents = new LinkedHashMap<>();
            conciseLabels.keySet().forEach(optionName ->
                  conciseComponents.put(optionName, component(options.getOption(optionName))));
            DialogOptionComponentYPanel searchlights = component(
                  options.getOption(OptionsConstants.SEARCHLIGHTS_ON));
            List<DialogOptionComponentYPanel> components = new ArrayList<>(conciseComponents.values());
            components.add(searchlights);
            GameOptionsPane pane = pane(components, option -> true);

            conciseLabels.forEach((optionName, label) -> {
                DialogOptionComponentYPanel optionComponent = conciseComponents.get(optionName);
                assertOptionPresentation(optionComponent, label, true,
                      optionName.equals(OptionsConstants.BASE_INFANTRY_DAMAGE_HEAT));
                assertFalse(optionLabel(optionComponent).getText().contains("<div width="),
                      optionLabel(optionComponent).getText());
            });
            assertOptionPresentation(searchlights, searchlights.getOption().getDisplayableName(), false, false);

            DialogOptionComponentYPanel deployment = conciseComponents.get(
                  OptionsConstants.BASE_SET_PLAYER_DEPLOYMENT_TO_PLAYER_0);
            pane.setFilterText(deployment.getOption().getDisplayableName());
            assertTrue(deployment.isVisible());
            assertFalse(searchlights.isVisible());
        });
    }

    @Test
    void coreRulesDetailsUseNaturalParagraphsWithoutDefaultStateNarration() {
        GameOptions options = new GameOptions();
        for (Enumeration<IOptionGroup> groups = options.getGroups(); groups.hasMoreElements(); ) {
            IOptionGroup group = groups.nextElement();
            if (!group.getName().equals("basic")) {
                continue;
            }
            for (Enumeration<IOption> groupOptions = group.getOptions(); groupOptions.hasMoreElements(); ) {
                IOption option = groupOptions.nextElement();
                if (Set.of(OptionsConstants.TWRULES, OptionsConstants.PLAYTEST_1, OptionsConstants.PLAYTEST_2,
                      OptionsConstants.PLAYTEST_3, OptionsConstants.BASE_LOBBY_AMMO_DUMP,
                      OptionsConstants.BASE_SHOW_BAY_DETAIL).contains(option.getName())) {
                    continue;
                }
                String description = option.getDescription();
                String lowerCaseDescription = description.toLowerCase(Locale.ROOT);
                assertFalse(description.contains("\n"), option.getName());
                assertFalse(lowerCaseDescription.contains("checked by default"), option.getName());
                assertFalse(lowerCaseDescription.contains("unchecked by default"), option.getName());
                assertFalse(lowerCaseDescription.contains("defaults to"), option.getName());
            }
        }
    }

    @Test
    void searchExpandsOnlySectionContainingMatchingOption() throws Exception {
        runOnEdt(() -> {
            GameOptions options = new GameOptions();
            DialogOptionComponentYPanel deployment = component(
                  options.getOption(OptionsConstants.BASE_EXCLUSIVE_DB_DEPLOYMENT));
            DialogOptionComponentYPanel lobby = component(options.getOption(OptionsConstants.BASE_LOBBY_AMMO_DUMP));
            GameOptionsPane pane = pane(List.of(deployment, lobby), option -> true);

            pane.setFilterText(deployment.getOption().getDisplayableName());

            List<CollapsibleSectionPanel> sections = findSections(pane);
            assertTrue(sections.get(0).isExpanded());
            assertFalse(sections.get(1).isExpanded());
            assertTrue(deployment.isVisible());
            assertFalse(lobby.isVisible());
        });
    }

    @Test
    void everyGameOptionResolvesToLocalizedSectionText() {
        GameOptions options = new GameOptions();
        int optionCount = 0;
        for (Enumeration<IOptionGroup> groups = options.getGroups(); groups.hasMoreElements(); ) {
            IOptionGroup group = groups.nextElement();
            for (Enumeration<IOption> groupOptions = group.getOptions(); groupOptions.hasMoreElements(); ) {
                IOption option = groupOptions.nextElement();
                String sectionId = GameOptionsPane.sectionId(group.getName(), option.getName());
                assertTrue(Messages.keyExists("GameOptionsDialog.section." + sectionId + ".title"), sectionId);
                assertTrue(Messages.keyExists("GameOptionsDialog.section." + sectionId + ".summary"), sectionId);
                optionCount++;
            }
        }
        assertTrue(optionCount > 200, "Expected all game options to be classified");
    }

    @Test
    void everyRegisteredGameOptionHasExactlyOneExplicitPresentationLocation() {
        GameOptions options = new GameOptions();
        Set<String> registeredOptionNames = new LinkedHashSet<>();
        Map<String, Integer> sectionSizes = new LinkedHashMap<>();

        for (Enumeration<IOptionGroup> groups = options.getGroups(); groups.hasMoreElements(); ) {
            IOptionGroup group = groups.nextElement();
            for (Enumeration<IOption> groupOptions = group.getOptions(); groupOptions.hasMoreElements(); ) {
                IOption option = groupOptions.nextElement();
                assertTrue(registeredOptionNames.add(option.getName()), "Duplicate registered option " + option.getName());
                GameOptionsPresentation.Location location = GameOptionsPresentation.location(
                      group.getName(), option.getName());
                    if (location.page().categoryId() != null) {
                      assertTrue(Messages.keyExists(
                          "GameOptionsDialog.category." + location.page().categoryId()), location.page().categoryId());
                      assertTrue(Messages.keyExists(
                          "GameOptionsDialog.page." + location.page().id() + ".title"), location.page().id());
                    }
                    assertTrue(Messages.keyExists(
                        "GameOptionsDialog.section." + location.sectionId() + ".title"), location.sectionId());
                    assertTrue(Messages.keyExists(
                        "GameOptionsDialog.section." + location.sectionId() + ".summary"), location.sectionId());
                String sectionKey = location.page().id() + ':' + location.sectionId();
                sectionSizes.merge(sectionKey, 1, Integer::sum);
            }
        }

        assertEquals(registeredOptionNames, GameOptionsPresentation.mappedOptionNames());
        sectionSizes.forEach((section, count) -> assertTrue(count <= 12, section + " contains " + count + " options"));
    }

    @Test
    void overloadedGroupsUseTaskBasedNestedNavigationPaths() throws Exception {
        runOnEdt(() -> {
            GameOptions options = new GameOptions();
            GameOptionsPane pane = new GameOptionsPane(allOptionGroups(options), option -> true);
            JTree tree = findComponent(pane, JTree.class);

            assertTreePathExists(tree, "General", "Match Setup");
            assertTreePathExists(tree, "General", "Match Flow, Timers, and Saves");
            assertTreePathExists(tree, "Rules", "Core Rules");
            assertTreePathExists(tree, "Rules", "Sensors and Visibility");
            assertTreePathExists(tree, "Combat", "Targeting, LOS, and Artillery");
            assertTreePathExists(tree, "Movement", "Vehicle Movement");
            assertTreePathExists(tree, "Aerospace", "Vessels, Fuel, and Ordnance");
        });
    }

    @Test
    void victorySectionsUseEqualTwoColumnGrids() throws Exception {
        runOnEdt(() -> {
            GameOptions options = new GameOptions();
            DialogOptionComponentYPanel checkVictory = component(
                  options.getOption(OptionsConstants.VICTORY_CHECK_VICTORY));
            DialogOptionComponentYPanel skipForcedVictory = component(
                  options.getOption(OptionsConstants.VICTORY_SKIP_FORCED_VICTORY));
            DialogOptionComponentYPanel achieveConditions = component(
                  options.getOption(OptionsConstants.VICTORY_ACHIEVE_CONDITIONS));
            DialogOptionComponentYPanel useBvDestroyed = component(
                  options.getOption(OptionsConstants.VICTORY_USE_BV_DESTROYED));
            DialogOptionComponentYPanel bvDestroyedPercent = component(
                  options.getOption(OptionsConstants.VICTORY_BV_DESTROYED_PERCENT));
            DialogOptionComponentYPanel useBvRatio = component(
                  options.getOption(OptionsConstants.VICTORY_USE_BV_RATIO));
            DialogOptionComponentYPanel bvRatioPercent = component(
                  options.getOption(OptionsConstants.VICTORY_BV_RATIO_PERCENT));

            pane("victory", List.of(checkVictory, skipForcedVictory, achieveConditions, useBvDestroyed,
                  bvDestroyedPercent, useBvRatio, bvRatioPercent), option -> true);

            assertTwoColumnGrid(checkVictory, skipForcedVictory, achieveConditions);
            assertTwoColumnGrid(useBvDestroyed, bvDestroyedPercent, useBvRatio, bvRatioPercent);
        });
    }

    @Test
    void factionLogoMappingsResolveToSharedAssets() {
        File factionsDir = new File(Configuration.universeImagesDir(), "factions");

        assertEquals(10, GameOptionsPane.factionLogos().size());
        GameOptionsPane.factionLogos().forEach((page, logo) ->
              assertTrue(new File(factionsDir, logo).isFile(), page + " logo does not exist: " + logo));
        assertTrue(new File(factionsDir, "logo_star_league.png").isFile());
    }

    @Test
    void badgesUseSectionAndOptionMetadataOwnership() throws Exception {
        runOnEdt(() -> {
            GameOptions options = new GameOptions();
            DialogOptionComponentYPanel normalOption = component(
                  options.getOption(OptionsConstants.SEARCHLIGHTS_ON));
            GameOptionsPane basicPane = pane("basic", List.of(normalOption), option -> true);
            DialogOptionComponentYPanel unofficialOption = component(
                  options.getOption(OptionsConstants.UNOFFICIAL_BRIDGE_REPAIR_ENGINEERS));
            GameOptionsPane advancedPane = pane("advancedRules", List.of(unofficialOption), option -> true);

            String normalOptionText = optionLabel(normalOption).getText();
            assertFalse(normalOptionText.contains(IMPORTANT_SYMBOL));
            assertFalse(normalOptionText.contains(UNOFFICIAL_SYMBOL));
            String unofficialOptionText = optionLabel(unofficialOption).getText();
            assertFalse(unofficialOptionText.contains(IMPORTANT_SYMBOL));
            assertTrue(unofficialOptionText.contains(UNOFFICIAL_SYMBOL));

            String basicTitle = sectionTitle(basicPane, "Battlefield Rules");
            assertFalse(basicTitle.contains(IMPORTANT_SYMBOL));
            assertFalse(basicTitle.contains(ADVANCED_SYMBOL));
            assertFalse(basicTitle.contains(UNOFFICIAL_SYMBOL));

            String advancedTitle = sectionTitle(advancedPane, "Battlefield Engineering");
            assertFalse(advancedTitle.contains(IMPORTANT_SYMBOL));
            assertTrue(advancedTitle.contains(ADVANCED_SYMBOL));
            assertFalse(advancedTitle.contains(UNOFFICIAL_SYMBOL));
        });
    }

    private static GameOptionsPane pane(List<DialogOptionComponentYPanel> components,
          java.util.function.Predicate<IOption> visibility) {
        return pane("basic", components, visibility);
    }

    private static GameOptionsPane pane(String groupId, List<DialogOptionComponentYPanel> components,
          java.util.function.Predicate<IOption> visibility) {
        return new GameOptionsPane(List.of(new GameOptionsPane.OptionGroup(groupId, groupId, components)),
              visibility);
    }

    private static List<GameOptionsPane.OptionGroup> allOptionGroups(GameOptions options) {
        List<GameOptionsPane.OptionGroup> groups = new ArrayList<>();
        for (Enumeration<IOptionGroup> optionGroups = options.getGroups(); optionGroups.hasMoreElements(); ) {
            IOptionGroup group = optionGroups.nextElement();
            List<DialogOptionComponentYPanel> components = new ArrayList<>();
            for (Enumeration<IOption> groupOptions = group.getOptions(); groupOptions.hasMoreElements(); ) {
                components.add(component(groupOptions.nextElement()));
            }
            groups.add(new GameOptionsPane.OptionGroup(group.getName(), group.getDisplayableName(), components));
        }
        return groups;
    }

    private static void assertTreePathExists(JTree tree, String... expectedPath) {
        for (int row = 0; row < tree.getRowCount(); row++) {
            TreePath treePath = tree.getPathForRow(row);
            if (treePath.getPathCount() != expectedPath.length + 1) {
                continue;
            }
            boolean matches = true;
            for (int index = 0; index < expectedPath.length; index++) {
                matches &= expectedPath[index].equals(treePath.getPathComponent(index + 1).toString());
            }
            if (matches) {
                return;
            }
        }
        throw new AssertionError("No navigation path " + String.join(" > ", expectedPath));
    }

    private static DialogOptionComponentYPanel component(IOption option) {
        return new DialogOptionComponentYPanel(new DialogOptionListener() {
            @Override
            public void optionClicked(DialogOptionComponentYPanel component, IOption changedOption, boolean state) {
            }

            @Override
            public void optionSwitched(DialogOptionComponentYPanel component, IOption changedOption, int index) {
            }
        }, option, true, true);
    }

    private static void assertCell(GridBagLayout layout, Component component, int column, int row, int width) {
        GridBagConstraints constraints = layout.getConstraints(component);
        assertEquals(column, constraints.gridx);
        assertEquals(row, constraints.gridy);
        assertEquals(width, constraints.gridwidth);
    }

    private static void assertTwoColumnGrid(DialogOptionComponentYPanel... components) {
        Container parent = components[0].getParent();
        GridBagLayout layout = (GridBagLayout) parent.getLayout();
        int expectedWidth = components[0].getPreferredSize().width;
        parent.setSize(parent.getPreferredSize());
        parent.doLayout();
        assertTrue(expectedWidth >= UIUtil.scaleForGUI(SettingsFormPanel.DEFAULT_LABEL_WIDTH));
        for (int index = 0; index < components.length; index++) {
            assertSame(parent, components[index].getParent());
            assertCell(layout, components[index], index % 2, index / 2, 1);
            assertEquals(expectedWidth, components[index].getPreferredSize().width);
            assertEquals(expectedWidth, components[index].getWidth());
        }
    }

    private static void assertSectionAlignment(DialogOptionComponentYPanel... components) {
        List<Container> sections = List.of(components[0].getParent(), components[2].getParent(),
              components[4].getParent());
        int sharedWidth = UIUtil.scaleForGUI(900);
        for (Container section : sections) {
            section.setSize(sharedWidth, section.getPreferredSize().height);
            section.doLayout();
        }

        assertSame(sections.get(0), components[1].getParent());
        assertSame(sections.get(1), components[3].getParent());
        assertSame(sections.get(2), components[5].getParent());
        assertEquals(sections.get(0).getPreferredSize().width, sections.get(1).getPreferredSize().width);
        assertEquals(sections.get(0).getPreferredSize().width, sections.get(2).getPreferredSize().width);
        assertEquals(components[1].getX(), components[3].getX());
        assertEquals(components[1].getX(), components[5].getX());
    }

    private static JLabel optionLabel(DialogOptionComponentYPanel component) {
        for (Component child : component.getComponents()) {
            if (child instanceof JLabel label) {
                return label;
            }
        }
        throw new AssertionError("Option has no label: " + component.getOption().getName());
    }

    private static <T extends Component> T findComponent(Container root, Class<T> type) {
        for (Component child : root.getComponents()) {
            if (type.isInstance(child)) {
                return type.cast(child);
            }
            if (child instanceof Container container) {
                T result = findComponentOrNull(container, type);
                if (result != null) {
                    return result;
                }
            }
        }
        throw new AssertionError("No " + type.getSimpleName() + " found");
    }

    private static <T extends Component> T findComponentOrNull(Container root, Class<T> type) {
        for (Component child : root.getComponents()) {
            if (type.isInstance(child)) {
                return type.cast(child);
            }
            if (child instanceof Container container) {
                T result = findComponentOrNull(container, type);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    private static void assertFirstComponentStartsAtCellEdge(DialogOptionComponentYPanel option,
          Class<? extends Component> expectedType) {
        option.setSize(option.getPreferredSize());
        option.doLayout();
        Component firstComponent = option.getComponent(0);
        assertTrue(expectedType.isInstance(firstComponent), firstComponent.getClass().getSimpleName());
        assertEquals(0, firstComponent.getX());
    }

    private static int assertLabelControlCell(DialogOptionComponentYPanel option,
          Class<? extends Component> controlType) {
        option.setSize(option.getPreferredSize());
        option.doLayout();
        Component label = option.getComponent(0);
        Component control = findDirectComponent(option, controlType);
        assertTrue(label instanceof JLabel, label.getClass().getSimpleName());
        assertTrue(controlType.isInstance(control), control.getClass().getSimpleName());
        assertEquals(0, label.getX());
        assertTrue(control.getX() > label.getX());
        assertSame(control, ((JLabel) label).getLabelFor());
        return control.getX();
    }

    private static Component findDirectComponent(Container root, Class<? extends Component> type) {
        for (Component component : root.getComponents()) {
            if (type.isInstance(component)) {
                return component;
            }
        }
        throw new AssertionError("No direct " + type.getSimpleName() + " found");
    }

    private static void assertOptionPresentation(DialogOptionComponentYPanel component, String label,
            boolean important, boolean unofficial) {
        String text = optionLabel(component).getText();
        assertTrue(text.contains(label), text);
                assertEquals(important, text.contains(IMPORTANT_SYMBOL), text);
        assertEquals(unofficial, text.contains(UNOFFICIAL_SYMBOL), text);
    }

    private static String sectionTitle(Container root, String title) {
        return findLabelContaining(root, title)
              .map(JLabel::getText)
              .orElseThrow(() -> new AssertionError("No section title containing: " + title));
    }

    private static Optional<JLabel> findLabelContaining(Container root, String text) {
        for (Component child : root.getComponents()) {
            if (child instanceof JLabel label && label.getText() != null && label.getText().contains(text)) {
                return Optional.of(label);
            }
            if (child instanceof Container container) {
                Optional<JLabel> result = findLabelContaining(container, text);
                if (result.isPresent()) {
                    return result;
                }
            }
        }
        return Optional.empty();
    }

    private static List<CollapsibleSectionPanel> findSections(Container root) {
        List<CollapsibleSectionPanel> sections = new ArrayList<>();
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

    private static void runOnEdt(Runnable test) throws Exception {
        try {
            SwingUtilities.invokeAndWait(test);
        } catch (InvocationTargetException exception) {
            if (exception.getCause() instanceof Error error) {
                throw error;
            }
            if (exception.getCause() instanceof Exception cause) {
                throw cause;
            }
            throw exception;
        }
    }
}
