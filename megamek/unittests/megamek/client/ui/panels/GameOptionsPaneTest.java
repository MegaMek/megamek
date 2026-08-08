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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseEvent;
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
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.JTree;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.tree.TreePath;

import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.DialogOptionListener;
import megamek.client.ui.settings.CollapsibleSectionPanel;
import megamek.client.ui.settings.SettingsHelpPanel;
import megamek.client.ui.settings.SettingsNavigationPanel;
import megamek.client.ui.settings.SettingsPagePanel;
import megamek.client.ui.util.UIUtil;
import megamek.common.Configuration;
import megamek.common.MMRandom;
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
            assertTrue(searchlights.settingsCheckBox().isVisible());
            assertFalse(pushOffBoard.settingsCheckBox().isVisible());
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
            assertFalse(searchlights.settingsCheckBox().isVisible());
        });
    }

    @Test
    void directLabeledRowPreservesOwnerBehaviorAndHelp() throws Exception {
        runOnEdt(() -> {
            GameOptions options = new GameOptions();
            DialogOptionComponentYPanel movementTimer = component(
                  options.getOption(OptionsConstants.BASE_TURN_TIMER_MOVEMENT));
            GameOptionsPane pane = pane(List.of(movementTimer), option -> true);
            JLabel label = movementTimer.settingsLabel();
            JComponent control = movementTimer.settingsControl();

            movementTimer.setValue(12);
            assertEquals(12, movementTimer.getValue());
            movementTimer.setEditable(false);
            assertFalse(control.isEnabled());
            movementTimer.setEditable(true);
            assertTrue(control.isEnabled());

            MouseEvent enter = new MouseEvent(label, MouseEvent.MOUSE_ENTERED,
                  System.currentTimeMillis(), 0, 0, 0, 0, false);
            for (var listener : label.getMouseListeners()) {
                listener.mouseEntered(enter);
            }
            JEditorPane helpText = findComponent(pane, JEditorPane.class);
            assertTrue(helpText.getText().contains("seconds"), helpText.getText());
            assertFalse(helpText.getText().contains("width=\"500\""), helpText.getText());
            assertFalse(helpText.getText().contains("<br>"), helpText.getText());
            SettingsHelpPanel helpPanel = findComponent(pane, SettingsHelpPanel.class);
            assertEquals("Option Details", ((TitledBorder) helpPanel.getBorder()).getTitle());

            pane.setFilterText("no matching option text");
            assertFalse(label.isVisible());
            assertFalse(control.isVisible());
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

            JCheckBox wideCoreCheckBox = wideCoreOption.settingsCheckBox();
            JCheckBox coreFirstCheckBox = coreFirst.settingsCheckBox();
            JCheckBox testingFirstCheckBox = testingFirst.settingsCheckBox();
            JCheckBox testingSecondCheckBox = testingSecond.settingsCheckBox();
            JCheckBox displayFirstCheckBox = displayFirst.settingsCheckBox();
            JCheckBox displaySecondCheckBox = displaySecond.settingsCheckBox();
            assertSectionAlignment(wideCoreCheckBox, coreFirstCheckBox,
                testingFirstCheckBox, testingSecondCheckBox, displayFirstCheckBox, displaySecondCheckBox);
            GridBagConstraints wideLayout = ((GridBagLayout) wideCoreCheckBox.getParent().getLayout())
                .getConstraints(wideCoreCheckBox);
            assertEquals(1, wideLayout.gridwidth);
            assertEquals(GridBagConstraints.NONE, wideLayout.fill);
        });
    }

    @Test
    void ammoDumpingRoundUsesFullWidthLabeledSpinner() throws Exception {
        runOnEdt(() -> {
            GameOptions options = new GameOptions();
            DialogOptionComponentYPanel lobbyAmmo = component(
                  options.getOption(OptionsConstants.BASE_LOBBY_AMMO_DUMP));
            DialogOptionComponentYPanel dumpingRound = component(
                  options.getOption(OptionsConstants.BASE_DUMPING_FROM_ROUND));
            DialogOptionComponentYPanel bayDetail = component(
                  options.getOption(OptionsConstants.BASE_SHOW_BAY_DETAIL));
            DialogOptionComponentYPanel reportTooltips = component(
                options.getOption(OptionsConstants.BASE_SUPPRESS_UNIT_TOOLTIP_IN_REPORT_LOG));

            pane(List.of(lobbyAmmo, dumpingRound, bayDetail, reportTooltips), option -> true);

            JCheckBox lobbyCheckBox = lobbyAmmo.settingsCheckBox();
            JLabel label = dumpingRound.settingsLabel();
            JSpinner spinner = (JSpinner) dumpingRound.settingsControl();
            JCheckBox bayDetailCheckBox = bayDetail.settingsCheckBox();
            JCheckBox reportTooltipsCheckBox = reportTooltips.settingsCheckBox();
            Container content = label.getParent();
            GridBagLayout sectionLayout = (GridBagLayout) content.getLayout();
            assertCell(sectionLayout, lobbyCheckBox, 0, 0, 2);
            assertCell(sectionLayout, label, 0, 1, 1);
            assertCell(sectionLayout, spinner, 1, 1, GridBagConstraints.REMAINDER);
            assertCell(sectionLayout, bayDetailCheckBox, 0, 2, 1);
            assertCell(sectionLayout, reportTooltipsCheckBox, 1, 2, 1);
            assertTrue(content.getPreferredSize().width < UIUtil.scaleForGUI(
                SettingsPagePanel.DEFAULT_SECTION_STACK_WIDTH));
            CollapsibleSectionPanel section = (CollapsibleSectionPanel) SwingUtilities.getAncestorOfClass(
                CollapsibleSectionPanel.class, label);
            assertNotNull(section);
            assertSame(spinner, label.getLabelFor());
            assertEquals(1, ((SpinnerNumberModel) spinner.getModel()).getMinimum());
            JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor) spinner.getEditor();
            assertEquals(JTextField.LEFT, editor.getTextField().getHorizontalAlignment());
            assertEquals(1, dumpingRound.getValue());
        });
    }

    @Test
    void matchFlowUsesNumericSpinnersAndNamedRngDropdown() throws Exception {
        runOnEdt(() -> {
            GameOptions options = new GameOptions();
            List<DialogOptionComponentYPanel> timers = List.of(
                  component(options.getOption(OptionsConstants.BASE_TURN_TIMER_TARGETING)),
                  component(options.getOption(OptionsConstants.BASE_TURN_TIMER_MOVEMENT)),
                  component(options.getOption(OptionsConstants.BASE_TURN_TIMER_FIRING)),
                  component(options.getOption(OptionsConstants.BASE_TURN_TIMER_PHYSICAL)));
            DialogOptionComponentYPanel rotatingSaves = component(
                  options.getOption(OptionsConstants.BASE_MAX_NUMBER_ROUND_SAVES));
            DialogOptionComponentYPanel rngType = component(options.getOption(OptionsConstants.BASE_RNG_TYPE));
            List<DialogOptionComponentYPanel> components = new ArrayList<>(timers);
            components.add(rotatingSaves);
            components.add(rngType);

            pane(components, option -> true);

            for (DialogOptionComponentYPanel timer : timers) {
                JSpinner spinner = (JSpinner) timer.settingsControl();
                assertEquals(0, ((SpinnerNumberModel) spinner.getModel()).getMinimum());
                assertEquals(0, timer.getValue());
            }
            JSpinner savesSpinner = (JSpinner) rotatingSaves.settingsControl();
            assertEquals(1, ((SpinnerNumberModel) savesSpinner.getModel()).getMinimum());
            assertEquals(3, rotatingSaves.getValue());

            JComboBox<?> rngTypes = (JComboBox<?>) rngType.settingsControl();
            assertEquals(3, rngTypes.getItemCount());
            assertEquals("SunRandom", rngTypes.getItemAt(0));
            assertEquals("Java CryptoRandom", rngTypes.getItemAt(1));
            assertEquals("Pool36Random (Unofficial)", rngTypes.getItemAt(2));
            assertEquals(MMRandom.R_DEFAULT, rngType.getValue());
            rngType.setValue(MMRandom.R_POOL36);
            assertEquals(MMRandom.R_POOL36, rngType.getValue());
            assertEquals(2, rngTypes.getSelectedIndex());
        });
    }

    @Test
    void hiddenUnofficialBackingOptionDoesNotLeaveAnEmptyGridCell() throws Exception {
        runOnEdt(() -> {
            GameOptions options = new GameOptions();
            DialogOptionComponentYPanel reportTooltips = component(
                  options.getOption(OptionsConstants.BASE_SUPPRESS_UNIT_TOOLTIP_IN_REPORT_LOG));
            DialogOptionComponentYPanel hideUnofficial = component(
                  options.getOption(OptionsConstants.BASE_HIDE_UNOFFICIAL));
            DialogOptionComponentYPanel hideLegacy = component(
                  options.getOption(OptionsConstants.BASE_HIDE_LEGACY));

            pane(List.of(reportTooltips, hideUnofficial, hideLegacy), option -> true);

            JCheckBox reportTooltipsCheckBox = reportTooltips.settingsCheckBox();
            JCheckBox hideLegacyCheckBox = hideLegacy.settingsCheckBox();
            assertSame(reportTooltipsCheckBox.getParent(), hideLegacyCheckBox.getParent());
            GridBagLayout layout = (GridBagLayout) reportTooltipsCheckBox.getParent().getLayout();
            assertCell(layout, reportTooltipsCheckBox, 0, 0, 1);
            assertCell(layout, hideLegacyCheckBox, 1, 0, 1);
            assertNull(hideUnofficial.getParent());
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

            JCheckBox checkBox = allowGameMaster.settingsCheckBox();
            JLabel label = voteThreshold.settingsLabel();
            JComboBox<?> choice = (JComboBox<?>) voteThreshold.settingsControl();
            assertSame(checkBox.getParent(), label.getParent());
            assertSame(label.getParent(), choice.getParent());
            GridBagLayout sectionLayout = (GridBagLayout) checkBox.getParent().getLayout();
            assertCell(sectionLayout, checkBox, 0, 0, 2);
            assertCell(sectionLayout, label, 0, 1, 1);
            assertCell(sectionLayout, choice, 1, 1, GridBagConstraints.REMAINDER);
            assertSame(choice, label.getLabelFor());
        });
    }

    @Test
    void victoryAndGameMasterShareOneLeafPage() throws Exception {
        runOnEdt(() -> {
            GameOptions options = new GameOptions();
            DialogOptionComponentYPanel allowGameMaster = component(
                  options.getOption(OptionsConstants.GAME_MASTER_ALLOW));
            DialogOptionComponentYPanel checkVictory = component(
                  options.getOption(OptionsConstants.VICTORY_CHECK_VICTORY));

            new GameOptionsPane(List.of(
                  new GameOptionsPane.OptionGroup("gameMaster", "Game Master", List.of(allowGameMaster)),
                  new GameOptionsPane.OptionGroup("victory", "Victory Conditions", List.of(checkVictory))),
                  option -> true);

            assertSame(SwingUtilities.getAncestorOfClass(SettingsPagePanel.class, allowGameMaster),
                  SwingUtilities.getAncestorOfClass(SettingsPagePanel.class, checkVictory));
        });
    }

    @Test
    void gameMasterOnlyPageUsesLobbyFallbackTitleAndIcon() throws Exception {
        runOnEdt(() -> {
            GameOptions options = new GameOptions();
            DialogOptionComponentYPanel allowGameMaster = component(
                  options.getOption(OptionsConstants.GAME_MASTER_ALLOW));
            GameOptionsPane pane = new GameOptionsPane(List.of(new GameOptionsPane.OptionGroup(
                  "gameMaster", "Game Master", List.of(allowGameMaster))), option -> true);
            JTree tree = findComponent(pane, JTree.class);

            assertTreePathExists(tree, "General", "Game Master");
            assertTreePathDoesNotExist(tree, "General", "Victory and Game Master");
            GameOptionsPresentation.PageDefinition page = GameOptionsPresentation.location(
                  "gameMaster", OptionsConstants.GAME_MASTER_ALLOW).page();
            assertEquals("gameMaster", page.effectiveIconGroupId(Map.of("gameMaster", "Game Master")));
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

            new GameOptionsPane(List.of(
                                    new GameOptionsPane.OptionGroup("basic", "basic", List.of(booleanOption, numericOption)),
                                    new GameOptionsPane.OptionGroup("allowedUnits", "allowedUnits", List.of(choiceOption))),
                option -> true);

            JCheckBox checkBox = booleanOption.settingsCheckBox();
            assertEquals(0, ((GridBagLayout) checkBox.getParent().getLayout()).getConstraints(checkBox).gridx);
            int numericControlX = assertLabelControlRow(numericOption);
            int choiceControlX = assertLabelControlRow(choiceOption);
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
                populatedWidths.put(component, component.settingsControl().getPreferredSize().width);
            }

            pane("advancedRules", components, option -> true);

            for (DialogOptionComponentYPanel component : components) {
                int controlWidth = component.settingsControl().getPreferredSize().width;
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
                                        if (option.getType() != IOption.BOOLEAN) {
                        nonBooleanComponents.add(component);
                    }
                }
                groups.add(new GameOptionsPane.OptionGroup(optionGroup.getName(), optionGroup.getDisplayableName(),
                      components));
            }

            new GameOptionsPane(groups, option -> true);

            for (DialogOptionComponentYPanel component : nonBooleanComponents) {
                assertLabelControlRow(component);
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
                    assertFalse(optionPresentationText(optionComponent).contains("<div width="),
                        optionPresentationText(optionComponent));
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

            pane.setFilterText("");
            assertFalse(sections.get(0).isExpanded());
            assertFalse(sections.get(1).isExpanded());
            assertTrue(deployment.isVisible());
            assertTrue(lobby.isVisible());
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
        Map<String, Set<String>> pageSections = new LinkedHashMap<>();

        for (Enumeration<IOptionGroup> groups = options.getGroups(); groups.hasMoreElements(); ) {
            IOptionGroup group = groups.nextElement();
            for (Enumeration<IOption> groupOptions = group.getOptions(); groupOptions.hasMoreElements(); ) {
                IOption option = groupOptions.nextElement();
                assertTrue(registeredOptionNames.add(option.getName()), "Duplicate registered option " + option.getName());
                GameOptionsPresentation.Location location = GameOptionsPresentation.location(
                      group.getName(), option.getName());
                    assertTrue(Messages.keyExists(
                        "GameOptionsDialog.category." + location.page().categoryId()), location.page().categoryId());
                    assertTrue(Messages.keyExists(
                        "GameOptionsDialog.page." + location.page().id() + ".title"), location.page().id());
                    assertTrue(Messages.keyExists(
                        "GameOptionsDialog.section." + location.sectionId() + ".title"), location.sectionId());
                    assertTrue(Messages.keyExists(
                        "GameOptionsDialog.section." + location.sectionId() + ".summary"), location.sectionId());
                String sectionKey = location.page().id() + ':' + location.sectionId();
                sectionSizes.merge(sectionKey, 1, Integer::sum);
                    pageSections.computeIfAbsent(location.page().id(), ignored -> new LinkedHashSet<>())
                        .add(location.sectionId());
            }
        }

        assertEquals(registeredOptionNames, GameOptionsPresentation.mappedOptionNames());
        sectionSizes.forEach((section, count) -> assertTrue(count <= 12, section + " contains " + count + " options"));
                pageSections.forEach((page, sections) -> assertTrue(sections.size() >= 2 && sections.size() <= 4,
              page + " contains " + sections.size() + " sections"));
    }

    @Test
    void gameOptionsUseTaskBasedNestedNavigationPaths() throws Exception {
        runOnEdt(() -> {
            GameOptions options = new GameOptions();
            GameOptionsPane pane = new GameOptionsPane(allOptionGroups(options), option -> true);
            JTree tree = findComponent(pane, JTree.class);

            assertTreePathExists(tree, "General", "Match Setup");
            assertTreePathExists(tree, "General", "Match Flow, Timers, and Saves");
            assertTreePathExists(tree, "General", "Victory and Game Master");
            assertTreePathExists(tree, "General", "Units and Technology");
            assertTreePathExists(tree, "Rules", "Core Rules");
            assertTreePathExists(tree, "Rules", "Sensors and Visibility");
            assertTreePathExists(tree, "Combat", "Targeting, LOS, and Artillery");
            assertTreePathExists(tree, "Movement", "Vehicle Movement");
            assertTreePathExists(tree, "Aerospace", "Vessels, Fuel, and Ordnance");
            assertTreePathExists(tree, "Initiative and Pilots", "Initiative and Simultaneous Phases");
            assertTreePathExists(tree, "Initiative and Pilots", "Pilot Abilities, Edge, and Skills");
            assertTreePathDoesNotExist(tree, "Game Master");
            assertTreePathDoesNotExist(tree, "Victory Conditions");
            assertTreePathDoesNotExist(tree, "Allowed Units and Equipment");
            assertTreePathDoesNotExist(tree, "Initiative Rules");
            assertTreePathDoesNotExist(tree, "RPG Related");
        });
    }

    @Test
    void victorySectionsUseCheckboxGridsAndLabeledRows() throws Exception {
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

            assertTwoColumnCheckBoxGrid(checkVictory, skipForcedVictory);
            assertLabelControlRow(achieveConditions);
            assertStandaloneCheckBox(useBvDestroyed);
            assertLabelControlRow(bvDestroyedPercent);
            assertStandaloneCheckBox(useBvRatio);
            assertLabelControlRow(bvRatioPercent);
        });
    }

        @Test
        void victoryNumericOptionsUseBoundedSpinnersAndKeepDefaults() throws Exception {
          runOnEdt(() -> {
            GameOptions options = new GameOptions();
            DialogOptionComponentYPanel conditions = component(
                options.getOption(OptionsConstants.VICTORY_ACHIEVE_CONDITIONS));
            DialogOptionComponentYPanel destroyedPercent = component(
                options.getOption(OptionsConstants.VICTORY_BV_DESTROYED_PERCENT));
            DialogOptionComponentYPanel ratioPercent = component(
                options.getOption(OptionsConstants.VICTORY_BV_RATIO_PERCENT));
            DialogOptionComponentYPanel turnLimit = component(
                options.getOption(OptionsConstants.VICTORY_GAME_TURN_LIMIT));
            DialogOptionComponentYPanel killCount = component(
                options.getOption(OptionsConstants.VICTORY_GAME_KILL_COUNT));

            pane("victory", List.of(conditions, destroyedPercent, ratioPercent, turnLimit, killCount),
                option -> true);

            assertIntegerSpinner(conditions, 1, 100, 1);
            assertIntegerSpinner(destroyedPercent, 1, 100, 100);
            assertIntegerSpinner(ratioPercent, 1, 10_000, 300);
            assertIntegerSpinner(turnLimit, 1, 10_000, 10);
            assertIntegerSpinner(killCount, 1, 10_000, 4);
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

            String normalOptionText = optionPresentationText(normalOption);
            assertFalse(normalOptionText.contains(IMPORTANT_SYMBOL));
            assertFalse(normalOptionText.contains(UNOFFICIAL_SYMBOL));
            String unofficialOptionText = optionPresentationText(unofficialOption);
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
        if (!treePathExists(tree, expectedPath)) {
            throw new AssertionError("No navigation path " + String.join(" > ", expectedPath));
        }
    }

    private static void assertTreePathDoesNotExist(JTree tree, String... unexpectedPath) {
        if (treePathExists(tree, unexpectedPath)) {
            throw new AssertionError("Unexpected navigation path " + String.join(" > ", unexpectedPath));
        }
    }

    private static boolean treePathExists(JTree tree, String... expectedPath) {
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
                return true;
            }
        }
        return false;
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

    private static void assertTwoColumnCheckBoxGrid(DialogOptionComponentYPanel... components) {
        JCheckBox[] checkBoxes = new JCheckBox[components.length];
        for (int index = 0; index < components.length; index++) {
            checkBoxes[index] = components[index].settingsCheckBox();
        }
        Container parent = checkBoxes[0].getParent();
        GridBagLayout layout = (GridBagLayout) parent.getLayout();
        for (int index = 0; index < checkBoxes.length; index++) {
            assertSame(parent, checkBoxes[index].getParent());
            assertCell(layout, checkBoxes[index], index % 2, index / 2, 1);
        }
    }

    private static void assertStandaloneCheckBox(DialogOptionComponentYPanel component) {
        JCheckBox checkBox = component.settingsCheckBox();
        assertCell((GridBagLayout) checkBox.getParent().getLayout(), checkBox, 0,
              ((GridBagLayout) checkBox.getParent().getLayout()).getConstraints(checkBox).gridy, 2);
    }

    private static void assertSectionAlignment(Component... components) {
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
        assertEquals(components[1].getX(), components[3].getX());
        assertEquals(components[1].getX(), components[5].getX());
    }

    private static String optionPresentationText(DialogOptionComponentYPanel component) {
        return component.getOption().getType() == IOption.BOOLEAN
              ? component.settingsCheckBox().getText()
              : component.settingsLabel().getText();
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

    private static int assertLabelControlRow(DialogOptionComponentYPanel option) {
        JLabel label = option.settingsLabel();
        JComponent control = option.settingsControl();
        assertSame(label.getParent(), control.getParent());
        GridBagLayout layout = (GridBagLayout) label.getParent().getLayout();
        assertEquals(0, layout.getConstraints(label).gridx);
        assertEquals(1, layout.getConstraints(control).gridx);
        assertSame(control, label.getLabelFor());
        label.getParent().setSize(label.getParent().getPreferredSize());
        label.getParent().doLayout();
        return control.getX();
    }

    private static void assertIntegerSpinner(DialogOptionComponentYPanel option, int minimum, int maximum,
          int expectedValue) {
        JSpinner spinner = (JSpinner) option.settingsControl();
        SpinnerNumberModel model = (SpinnerNumberModel) spinner.getModel();
        assertEquals(minimum, model.getMinimum());
        assertEquals(maximum, model.getMaximum());
        assertEquals(expectedValue, option.getValue());
    }

    private static void assertOptionPresentation(DialogOptionComponentYPanel component, String label,
            boolean important, boolean unofficial) {
        String text = optionPresentationText(component);
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
