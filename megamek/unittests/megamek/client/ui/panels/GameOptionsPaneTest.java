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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.Color;
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
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.stream.Collectors;
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
    private static final String LEGACY_SYMBOL = Character.toString(0xE889);

    @Test
    void searchFiltersRowsByOptionName() throws Exception {
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
    void filterPreservesRawDisplayTextWhileMatchingNormalizedText() throws Exception {
        runOnEdt(() -> {
            GameOptions options = new GameOptions();
            DialogOptionComponentYPanel searchlights = component(options.getOption(OptionsConstants.SEARCHLIGHTS_ON));
            GameOptionsPane pane = pane(List.of(searchlights), option -> true);

            pane.setFilterText("  SEARCHLIGHTS!!!  ");

            assertEquals("  SEARCHLIGHTS!!!  ", pane.getFilterText());
            assertEquals("searchlights", pane.getActiveFilter());
            assertTrue(searchlights.isVisible());
        });
    }

    @Test
    void searchDoesNotMatchOptionDetailsText() throws Exception {
        runOnEdt(() -> {
            GameOptions options = new GameOptions();
            DialogOptionComponentYPanel disableLocalSave = component(
                  options.getOption(OptionsConstants.BASE_DISABLE_LOCAL_SAVE));
            GameOptionsPane pane = pane(List.of(disableLocalSave), option -> true);

            assertTrue(disableLocalSave.getOption().getDescription().contains("cheat"));

            pane.setFilterText("heat");

            assertFalse(disableLocalSave.isVisible());
            assertFalse(disableLocalSave.settingsCheckBox().isVisible());
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
    void sectionTitleSearchKeepsRowsInMatchingSectionVisible() throws Exception {
        runOnEdt(() -> {
            GameOptions options = new GameOptions();
            DialogOptionComponentYPanel minefields = component(
                  options.getOption(OptionsConstants.ADVANCED_MINEFIELDS));
            DialogOptionComponentYPanel bridgeBuilding = component(
                  options.getOption(OptionsConstants.ADVANCED_BRIDGE_BUILDING_ENGINEERS));
            GameOptionsPane pane = pane("advancedRules", List.of(minefields, bridgeBuilding), option -> true);

            pane.setFilterText("Battlefield Engineering");

            assertFalse(minefields.isVisible());
            assertTrue(bridgeBuilding.isVisible());
        });
    }

    @Test
    void visibilityRefreshKeepsSectionSearchAndReplacesVisibleOptionIndex() throws Exception {
        runOnEdt(() -> {
            GameOptions options = new GameOptions();
            DialogOptionComponentYPanel bridgeBuilding = component(
                  options.getOption(OptionsConstants.ADVANCED_BRIDGE_BUILDING_ENGINEERS));
            DialogOptionComponentYPanel bridgeRepair = component(
                  options.getOption(OptionsConstants.UNOFFICIAL_BRIDGE_REPAIR_ENGINEERS));
            AtomicBoolean showBridgeRepair = new AtomicBoolean(false);
            GameOptionsPane pane = pane("advancedRules", List.of(bridgeBuilding, bridgeRepair),
                  option -> !option.getName().equals(OptionsConstants.UNOFFICIAL_BRIDGE_REPAIR_ENGINEERS)
                        || showBridgeRepair.get());
            JTree tree = findComponent(pane, JTree.class);

            pane.setFilterText(OptionsConstants.UNOFFICIAL_BRIDGE_REPAIR_ENGINEERS);
            assertTreePathDoesNotExist(tree, "Rules", "Terrain and Environment");

            pane.setFilterText("Battlefield Engineering");
            assertTreePathExists(tree, "Rules", "Terrain and Environment");

            showBridgeRepair.set(true);
            pane.refreshVisibility();
            pane.setFilterText(OptionsConstants.UNOFFICIAL_BRIDGE_REPAIR_ENGINEERS);
            assertTreePathExists(tree, "Rules", "Terrain and Environment");
        });
    }

    @Test
    void pageTitleSearchKeepsPageRowsVisible() throws Exception {
        runOnEdt(() -> {
            GameOptions options = new GameOptions();
            DialogOptionComponentYPanel extremeRange = component(
                  options.getOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_RANGE));
            DialogOptionComponentYPanel calledShots = component(
                  options.getOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_CALLED_SHOTS));
            GameOptionsPane pane = pane("advancedCombat", List.of(extremeRange, calledShots), option -> true);

            pane.setFilterText("Targeting LOS Artillery");

            assertTrue(extremeRange.isVisible());
            assertTrue(calledShots.isVisible());
        });
    }

    @Test
    void unchangedVisibilityRefreshDoesNotRebuildSectionForm() throws Exception {
        runOnEdt(() -> {
            GameOptions options = new GameOptions();
            DialogOptionComponentYPanel extremeRange = component(
                  options.getOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_RANGE));
            DialogOptionComponentYPanel losRange = component(
                  options.getOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_LOS_RANGE));
            GameOptionsPane pane = pane("advancedCombat", List.of(extremeRange, losRange), option -> true);
            Container sectionForm = extremeRange.settingsCheckBox().getParent();
            Component[] originalChildren = sectionForm.getComponents();

            pane.refreshVisibility();

            assertEquals(List.of(originalChildren), List.of(sectionForm.getComponents()));
        });
    }

    @Test
    void filteringReflowsSurvivingRightColumnCheckbox() throws Exception {
        runOnEdt(() -> {
            GameOptions options = new GameOptions();
            DialogOptionComponentYPanel extremeRange = component(
                  options.getOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_RANGE));
            DialogOptionComponentYPanel diagrammingLos = component(
                options.getOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_LOS1));
            GameOptionsPane pane = pane("advancedCombat", List.of(extremeRange, diagrammingLos), option -> true);

            pane.setFilterText("Diagramming");

            JCheckBox checkBox = diagrammingLos.settingsCheckBox();
            assertTrue(checkBox.isVisible());
            assertCell((GridBagLayout) checkBox.getParent().getLayout(), checkBox, 0, 0, 2);
        });
    }

    @Test
    void filteringDoesNotRestoreTooltipsSuppressedByDetailsPanel() throws Exception {
        runOnEdt(() -> {
            GameOptions options = new GameOptions();
            DialogOptionComponentYPanel doubleBlind = component(
                  options.getOption(OptionsConstants.ADVANCED_DOUBLE_BLIND));
            DialogOptionComponentYPanel teamVision = component(
                  options.getOption(OptionsConstants.ADVANCED_TEAM_VISION));
            GameOptionsPane pane = pane("advancedRules", List.of(doubleBlind, teamVision), option -> true);
            JCheckBox doubleBlindCheckBox = doubleBlind.settingsCheckBox();
            JCheckBox teamVisionCheckBox = teamVision.settingsCheckBox();

            assertNull(doubleBlindCheckBox.getToolTipText());
            assertNull(teamVisionCheckBox.getToolTipText());

            pane.setFilterText("vision");
            assertNull(doubleBlindCheckBox.getToolTipText());
            assertNull(teamVisionCheckBox.getToolTipText());

            pane.setFilterText("");
            assertNull(doubleBlindCheckBox.getToolTipText());
            assertNull(teamVisionCheckBox.getToolTipText());
        });
    }

    @Test
    void refreshingVisibilityBindsHelpToRevealedOption() throws Exception {
        runOnEdt(() -> {
            GameOptions options = new GameOptions();
            DialogOptionComponentYPanel bridgeBuilding = component(
                  options.getOption(OptionsConstants.ADVANCED_BRIDGE_BUILDING_ENGINEERS));
            DialogOptionComponentYPanel bridgeRepair = component(
                  options.getOption(OptionsConstants.UNOFFICIAL_BRIDGE_REPAIR_ENGINEERS));
            AtomicBoolean showBridgeRepair = new AtomicBoolean(false);
            GameOptionsPane pane = pane("advancedRules", List.of(bridgeBuilding, bridgeRepair),
                  option -> !option.getName().equals(OptionsConstants.UNOFFICIAL_BRIDGE_REPAIR_ENGINEERS)
                        || showBridgeRepair.get());
            JCheckBox bridgeRepairCheckBox = bridgeRepair.settingsCheckBox();

            assertNotNull(bridgeRepairCheckBox.getToolTipText());
            showBridgeRepair.set(true);
            pane.refreshVisibility();

            assertTrue(bridgeRepairCheckBox.isVisible());
            assertNull(bridgeRepairCheckBox.getToolTipText());
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
    void refreshingVisibilityHidesSectionWithNoVisibleRows() throws Exception {
        runOnEdt(() -> {
            GameOptions options = new GameOptions();
            DialogOptionComponentYPanel bridgeRepair = component(
                  options.getOption(OptionsConstants.UNOFFICIAL_BRIDGE_REPAIR_ENGINEERS));
            AtomicBoolean showOption = new AtomicBoolean(true);
            GameOptionsPane pane = pane("advancedRules", List.of(bridgeRepair), option -> showOption.get());
            CollapsibleSectionPanel section = (CollapsibleSectionPanel) SwingUtilities.getAncestorOfClass(
                  CollapsibleSectionPanel.class, bridgeRepair.settingsCheckBox());
            assertNotNull(section);

            showOption.set(false);
            pane.refreshVisibility();

            assertFalse(section.isVisible());
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
            assertEquals(4, GameOptionsPane.legendEntries().size());
            assertEquals("Tooltip contains important information.",
                  GameOptionsPane.legendEntries().getFirst().description());
            assertEquals(new Color(0xE6, 0x9F, 0x00), GameOptionsPane.legendEntries().get(2).color());
            assertEquals("This option is retained for compatibility with superseded rules.",
                GameOptionsPane.legendEntries().getLast().description());
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
            assertSectionAlignment(coreFirstCheckBox, wideCoreCheckBox,
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
    void hiddenRuleBackingOptionsDoNotLeaveEmptyGridCells() throws Exception {
        runOnEdt(() -> {
            GameOptions options = new GameOptions();
            DialogOptionComponentYPanel reportTooltips = component(
                  options.getOption(OptionsConstants.BASE_SUPPRESS_UNIT_TOOLTIP_IN_REPORT_LOG));
            DialogOptionComponentYPanel hideUnofficial = component(
                  options.getOption(OptionsConstants.BASE_HIDE_UNOFFICIAL));
            DialogOptionComponentYPanel hideLegacy = component(
                  options.getOption(OptionsConstants.BASE_HIDE_LEGACY));

            pane(List.of(reportTooltips, hideUnofficial, hideLegacy), option -> true);

            assertStandaloneCheckBox(reportTooltips);
            assertNull(hideUnofficial.getParent());
            assertNull(hideLegacy.getParent());
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

            SettingsPagePanel gameMasterPage = (SettingsPagePanel) SwingUtilities.getAncestorOfClass(
                SettingsPagePanel.class, allowGameMaster.settingsCheckBox());
            SettingsPagePanel victoryPage = (SettingsPagePanel) SwingUtilities.getAncestorOfClass(
                SettingsPagePanel.class, checkVictory.settingsCheckBox());
            assertNotNull(gameMasterPage);
            assertSame(gameMasterPage, victoryPage);
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
                    if (Set.of(OptionsConstants.RULES_SYSTEM, OptionsConstants.BASE_LOBBY_AMMO_DUMP,
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
    void everyLocalizedSectionBelongsToThePresentationCatalog() {
        Set<String> catalogKeys = new LinkedHashSet<>();
        GameOptions options = new GameOptions();
        for (Enumeration<IOptionGroup> groups = options.getGroups(); groups.hasMoreElements(); ) {
            IOptionGroup group = groups.nextElement();
            for (Enumeration<IOption> groupOptions = group.getOptions(); groupOptions.hasMoreElements(); ) {
                String sectionId = GameOptionsPane.sectionId(group.getName(), groupOptions.nextElement().getName());
                catalogKeys.add("GameOptionsDialog.section." + sectionId + ".title");
                catalogKeys.add("GameOptionsDialog.section." + sectionId + ".summary");
            }
        }
        catalogKeys.add("GameOptionsDialog.section.allowedUnits.uncategorized.title");
        catalogKeys.add("GameOptionsDialog.section.allowedUnits.uncategorized.summary");

        ResourceBundle bundle = ResourceBundle.getBundle("megamek.client.messages", Locale.ROOT);
        Set<String> localizedKeys = bundle.keySet().stream()
              .filter(key -> key.startsWith("GameOptionsDialog.section."))
              .collect(Collectors.toCollection(LinkedHashSet::new));

        assertEquals(catalogKeys, localizedKeys);
    }

    @Test
    void missingPresentationLocationUsesFallbackWithoutWeakeningStrictLookup() {
        GameOptionsPresentation.Location fallback = GameOptionsPresentation.locationOrFallback(
              "newGroup", "new_option");

        assertEquals("general.unitsAndTechnology", fallback.page().id());
        assertEquals("allowedUnits.uncategorized", fallback.sectionId());
        assertThrows(IllegalArgumentException.class,
              () -> GameOptionsPresentation.location("newGroup", "new_option"));
    }

        @Test
        void battlefieldOptionsFollowCatalogOrder() {
          List<String> optionNames = List.of(
              OptionsConstants.SEARCHLIGHTS_ON,
              OptionsConstants.BASE_PUSH_OFF_BOARD,
              OptionsConstants.BASE_BRIDGE_CF,
              OptionsConstants.BASE_RANDOM_BASEMENTS,
              OptionsConstants.BASE_BREEZE);

          assertEquals(List.of(0, 1, 2, 3, 4), optionNames.stream()
              .map(optionName -> GameOptionsPresentation.location("basic", optionName).optionOrder())
              .toList());
        }

    @Test
    void everyRegisteredGameOptionHasExactlyOneExplicitPresentationLocation() {
        GameOptions options = new GameOptions();
        Set<String> registeredOptionNames = new LinkedHashSet<>();
        Map<String, Integer> sectionSizes = new LinkedHashMap<>();
        Map<String, Set<String>> pageSections = new LinkedHashMap<>();
        Map<String, Set<Integer>> sectionOptionOrders = new LinkedHashMap<>();

        for (Enumeration<IOptionGroup> groups = options.getGroups(); groups.hasMoreElements(); ) {
            IOptionGroup group = groups.nextElement();
            for (Enumeration<IOption> groupOptions = group.getOptions(); groupOptions.hasMoreElements(); ) {
                IOption option = groupOptions.nextElement();
                assertTrue(registeredOptionNames.add(option.getName()),
                      "Duplicate registered option " + option.getName());
                GameOptionsPresentation.Location location = GameOptionsPresentation.location(
                      group.getName(), option.getName());
                    if (!location.page().isDirect()) {
                      assertTrue(Messages.keyExists(
                          "GameOptionsDialog.category." + location.page().categoryId()),
                          location.page().categoryId());
                    }
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
                    assertTrue(location.optionOrder() < Integer.MAX_VALUE, option.getName());
                    assertTrue(sectionOptionOrders.computeIfAbsent(sectionKey, ignored -> new LinkedHashSet<>())
                        .add(location.optionOrder()), "Duplicate option order for " + sectionKey);
            }
        }

        assertEquals(registeredOptionNames, GameOptionsPresentation.mappedOptionNames());
        sectionSizes.forEach((section, count) -> assertTrue(count <= 12, section + " contains " + count + " options"));
          pageSections.forEach((page, sections) -> {
            int minimumSections = page.equals("landing") ? 1 : 2;
            assertTrue(sections.size() >= minimumSections && sections.size() <= 4,
                page + " contains " + sections.size() + " sections");
          });
    }

    @Test
    void gameOptionsUseTaskBasedNestedNavigationPaths() throws Exception {
        runOnEdt(() -> {
            GameOptions options = new GameOptions();
            GameOptionsPane pane = new GameOptionsPane(allOptionGroups(options), option -> true);
            JTree tree = findComponent(pane, JTree.class);

            assertTreePathExists(tree, "Game Options");
            assertTreePathExists(tree, "General", "Match Setup");
            assertTreePathExists(tree, "General", "Match Flow, Timers, and Saves");
            assertTreePathExists(tree, "General", "Victory and Game Master");
            assertTreePathExists(tree, "General", "Units and Technology");
            assertTreePathExists(tree, "Rules", "General Rules");
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
    void gameOptionsLandingPageOwnsRulesSystemAndIsSelectedFirst() throws Exception {
        runOnEdt(() -> {
            GameOptions options = new GameOptions();
            List<GameOptionsPane.OptionGroup> groups = allOptionGroups(options);
            DialogOptionComponentYPanel rulesSystem = groups.stream()
                  .flatMap(group -> group.components().stream())
                  .filter(component -> component.getOption().getName().equals(OptionsConstants.RULES_SYSTEM))
                  .findFirst()
                  .orElseThrow();
            rulesSystem.addValue(OptionsConstants.RULES_CORE);
            rulesSystem.addValue(OptionsConstants.RULES_TW);
            rulesSystem.setSelected(options.getOption(OptionsConstants.RULES_SYSTEM).stringValue());

            GameOptionsPane pane = new GameOptionsPane(groups, option -> true);
            JTree tree = findComponent(pane, JTree.class);
            TreePath selectedPath = tree.getSelectionPath();
            SettingsPagePanel landingPage = findComponent(pane, SettingsPagePanel.class);
            JComboBox<?> rulesSystemControl = (JComboBox<?>) rulesSystem.settingsControl();
            JLabel rulesSystemDescription = findComponentByName(
                  landingPage, "lblRulesSystemDescription", JLabel.class);
            JLabel logo = findIconLabel(landingPage);
            GameOptionsPresentation.Location location = GameOptionsPresentation.location(
                  "basic", OptionsConstants.RULES_SYSTEM);

            assertNotNull(selectedPath);
            assertEquals("Game Options", selectedPath.getLastPathComponent().toString());
            assertEquals("landing", location.page().id());
            assertTrue(location.page().isDirect());
            assertEquals("landing.rulesSystem", location.sectionId());
            assertFalse(landingPage.shouldShowDetailsPanel());
            List<CollapsibleSectionPanel> sections = findSections(landingPage);
            assertEquals(1, sections.size());
            assertTrue(sections.getFirst().isExpanded());
            assertNotNull(rulesSystem.settingsLabel().getParent());
            assertNotNull(rulesSystemControl.getParent());
            assertEquals(2, rulesSystemControl.getItemCount());
            assertEquals(OptionsConstants.RULES_CORE, rulesSystemControl.getItemAt(0));
            assertEquals(OptionsConstants.RULES_TW, rulesSystemControl.getItemAt(1));
            assertEquals(OptionsConstants.RULES_CORE, rulesSystemControl.getSelectedItem());
            assertTrue(rulesSystemDescription.getText().contains("Core rules (2026)"));
            assertTrue(rulesSystemDescription.getText().contains("greyed out and disabled"));
            rulesSystemControl.setSelectedItem(OptionsConstants.RULES_TW);
            assertTrue(rulesSystemDescription.getText().contains("Total Warfare (2006)"));
            assertTrue(rulesSystemDescription.getText().contains("Bombast Lasers"));
            assertTrue(rulesSystemDescription.getText().contains("Extended LRMs"));
            assertEquals(UIUtil.scaleForGUI(200), logo.getIcon().getIconWidth());
            assertEquals(UIUtil.scaleForGUI(200), logo.getIcon().getIconHeight());
            assertTrue(landingPage.getPageSearchText().contains("Across the Inner Sphere"));
            assertTrue(landingPage.getPageSearchText().contains("clarity at the planning table"));
            assertEquals("GameOptionsDialog.page.landing.rulesSystem.core",
                  GameOptionsPane.rulesSystemDescriptionKey("future rules"));
        });
    }

    @Test
    void excludedOptionsDoNotCreateBlankSectionsOrRemoveRemainingPage() throws Exception {
        runOnEdt(() -> {
            GameOptions options = new GameOptions();
            DialogOptionComponentYPanel canonOnly = component(
                  options.getOption(OptionsConstants.ALLOWED_CANON_ONLY));
            DialogOptionComponentYPanel year = component(options.getOption(OptionsConstants.ALLOWED_YEAR));
            DialogOptionComponentYPanel techLevel = component(
                  options.getOption(OptionsConstants.ALLOWED_TECH_LEVEL));
            DialogOptionComponentYPanel variableTechLevel = component(
                  options.getOption(OptionsConstants.ALLOWED_ERA_BASED));
            DialogOptionComponentYPanel showExtinct = component(
                  options.getOption(OptionsConstants.ALLOWED_SHOW_EXTINCT));
            DialogOptionComponentYPanel allowInvalid = component(
                  options.getOption(OptionsConstants.ALLOWED_ALLOW_ILLEGAL_UNITS));
            Set<String> excluded = Set.of(
                  OptionsConstants.ALLOWED_CANON_ONLY,
                  OptionsConstants.ALLOWED_YEAR,
                  OptionsConstants.ALLOWED_TECH_LEVEL,
                  OptionsConstants.ALLOWED_ERA_BASED,
                  OptionsConstants.ALLOWED_SHOW_EXTINCT);

            GameOptionsPane pane = new GameOptionsPane(List.of(new GameOptionsPane.OptionGroup(
                  "allowedUnits", "Allowed Units and Equipment",
                  List.of(canonOnly, year, techLevel, variableTechLevel, showExtinct, allowInvalid))),
                  option -> true, excluded);

            assertNull(SwingUtilities.getAncestorOfClass(GameOptionsPane.class, canonOnly.settingsCheckBox()));
            assertNull(SwingUtilities.getAncestorOfClass(GameOptionsPane.class, year.settingsLabel()));
            assertNull(SwingUtilities.getAncestorOfClass(GameOptionsPane.class, techLevel.settingsLabel()));
            assertNull(SwingUtilities.getAncestorOfClass(GameOptionsPane.class, variableTechLevel.settingsCheckBox()));
            assertNull(SwingUtilities.getAncestorOfClass(GameOptionsPane.class, showExtinct.settingsCheckBox()));
            assertSame(pane,
                  SwingUtilities.getAncestorOfClass(GameOptionsPane.class, allowInvalid.settingsCheckBox()));
            assertEquals(1, findSections(pane).size());
            assertTrue(sectionTitle(pane, "Special Restrictions").contains("Special Restrictions"));
            assertTreePathExists(findComponent(pane, JTree.class), "General", "Units and Technology");
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
        File factionsDir = factionLogoDirectory();

        assertTrue(factionsDir.isDirectory(), "Shared faction logo directory does not exist: " + factionsDir);
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
            assertTrue(unofficialOptionText.contains("color=\"#e69f00\""), unofficialOptionText);

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

    @Test
    void unofficialClassificationDoesNotDependOnLocalizedDisplayText() {
        IOption option = mock(IOption.class);
        when(option.getName()).thenReturn(OptionsConstants.ADVANCED_ALTERNATE_MASC);
        String spanishDisplayName = "(No oficial) Alternativo MASC/Supercharger";
        when(option.getDisplayableName()).thenReturn(spanishDisplayName);

        assertTrue(GameOptionsPane.isUnofficialOption(option));
        assertEquals("Alternativo MASC/Supercharger", GameOptionsPane.stripCategoryMarker(spanishDisplayName));
    }

    @Test
    void legacyClassificationStripsLocalizedLeadingMarkers() {
        IOption option = mock(IOption.class);
        when(option.getName()).thenReturn(OptionsConstants.ADVANCED_ASSAULT_DROP);

        assertTrue(GameOptionsPane.isLegacyOption(option));
        assertEquals("Descenso de asalto", GameOptionsPane.stripCategoryMarker("(Legado) Descenso de asalto"));
                assertEquals("\u0428\u0442\u0443\u0440\u043C\u043E\u0432\u043E\u0439 \u0441\u0431\u0440\u043E\u0441",
              GameOptionsPane.stripCategoryMarker("(\u0423\u0441\u0442\u0430\u0440\u0435\u0432\u0448\u0435\u0435) "
                  + "\u0428\u0442\u0443\u0440\u043C\u043E\u0432\u043E\u0439 \u0441\u0431\u0440\u043E\u0441"));
    }

    @Test
    void unofficialMetadataMatchesRegisteredEnglishOptionMarkers() {
        ResourceBundle englishOptions = ResourceBundle.getBundle("megamek.common.options.messages", Locale.ROOT);
        GameOptions options = new GameOptions();
        Set<String> unofficialOptions = new LinkedHashSet<>();

        for (Enumeration<IOptionGroup> groups = options.getGroups(); groups.hasMoreElements(); ) {
            IOptionGroup group = groups.nextElement();
            for (Enumeration<IOption> groupOptions = group.getOptions(); groupOptions.hasMoreElements(); ) {
                IOption option = groupOptions.nextElement();
                String displayName = englishOptions.getString(
                      "GameOptionsInfo.option." + option.getName() + ".displayableName");
                boolean hasUnofficialMarker = displayName.startsWith("(Unofficial)")
                      || displayName.startsWith("Unofficial:");
                assertEquals(hasUnofficialMarker, GameOptionsPane.isUnofficialOption(option), option.getName());
                if (hasUnofficialMarker) {
                    unofficialOptions.add(option.getName());
                }
            }
        }

        assertEquals(unofficialOptions, GameOptionsPane.unofficialOptionNames());
    }

    @Test
    void rngDescriptionUsesDropdownHelpInAllSupportedLocales() {
        String key = "GameOptionsInfo.option.rng_type.description";
        String englishDescription = ResourceBundle.getBundle(
              "megamek.common.options.messages", Locale.ROOT).getString(key);

        for (Locale locale : List.of(Locale.GERMAN, Locale.forLanguageTag("es"), Locale.forLanguageTag("ru"))) {
            assertEquals(englishDescription,
                  ResourceBundle.getBundle("megamek.common.options.messages", locale).getString(key),
                  locale.toLanguageTag());
        }
    }

    @Test
    void sensorsRemoveUnofficialTextAndUseBadges() throws Exception {
        runOnEdt(() -> {
            GameOptions options = new GameOptions();
            List<DialogOptionComponentYPanel> unofficialOptions = List.of(
                  component(options.getOption(OptionsConstants.ADVANCED_SINGLE_BLIND_BOTS)),
                  component(options.getOption(OptionsConstants.ADVANCED_PILOTS_VISUAL_RANGE_ONE)),
                  component(options.getOption(OptionsConstants.ADVANCED_PILOTS_CANNOT_SPOT)),
                  component(options.getOption(OptionsConstants.ADVANCED_INCLUSIVE_SENSOR_RANGE)),
                  component(options.getOption(OptionsConstants.ADVANCED_SENSORS_DETECT_ALL)),
                  component(options.getOption(OptionsConstants.ADVANCED_MAG_SCAN_NO_HILLS)),
                  component(options.getOption(OptionsConstants.ADVANCED_METAL_CONTENT)));

            pane("advancedRules", unofficialOptions, option -> true);

            for (DialogOptionComponentYPanel option : unofficialOptions) {
                String text = optionPresentationText(option);
                assertFalse(text.contains("(Unofficial)"), text);
                assertFalse(text.startsWith("Unofficial:"), text);
                assertTrue(text.contains(UNOFFICIAL_SYMBOL), text);
                assertTrue(text.contains("color=\"#e69f00\""), text);
            }
        });
    }

    @Test
    void legacyOptionsUseBadgeInsteadOfLabelText() throws Exception {
        runOnEdt(() -> {
            GameOptions options = new GameOptions();
            DialogOptionComponentYPanel ghostTargetMode = component(
                options.getOption(OptionsConstants.ADVANCED_GHOST_TARGET_MODE));
            ghostTargetMode.addValue(OptionsConstants.GHOST_TARGET_MODE_LEGACY);
            ghostTargetMode.addValue(OptionsConstants.GHOST_TARGET_MODE_STANDARD);
            List<DialogOptionComponentYPanel> advancedRules = List.of(
                ghostTargetMode,
                  component(options.getOption(OptionsConstants.ADVANCED_GHOST_TARGET_MAX)),
                  component(options.getOption(OptionsConstants.ADVANCED_ASSAULT_DROP)),
                  component(options.getOption(OptionsConstants.ADVANCED_PARATROOPERS)),
                  component(options.getOption(OptionsConstants.ADVANCED_MAX_TECH_MOVEMENT_MODS)));
            DialogOptionComponentYPanel autoAbandon = component(
                  options.getOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_AUTO_ABANDON_UNIT));
            DialogOptionComponentYPanel rpgGunnery = component(
                  options.getOption(OptionsConstants.RPG_RPG_GUNNERY));
            List<DialogOptionComponentYPanel> registeredAdvancedRules = new ArrayList<>(advancedRules);
            registeredAdvancedRules.add(autoAbandon);

            GameOptionsPane pane = new GameOptionsPane(List.of(
                new GameOptionsPane.OptionGroup("advancedRules", "Advanced Rules", registeredAdvancedRules),
                  new GameOptionsPane.OptionGroup("rpg", "RPG", List.of(rpgGunnery))), option -> true);

            List<DialogOptionComponentYPanel> legacyOptions = new ArrayList<>(advancedRules);
            legacyOptions.add(autoAbandon);
            legacyOptions.add(rpgGunnery);
            for (DialogOptionComponentYPanel option : legacyOptions) {
                String text = optionPresentationText(option);
                assertFalse(text.toLowerCase(Locale.ROOT).contains("legacy"), text);
                assertTrue(text.contains(LEGACY_SYMBOL), text);
            }
            assertIntegerSpinner(advancedRules.get(1), 0, 10, 5);

            JLabel ghostTargetModeLabel = ghostTargetMode.settingsLabel();
            assertTrue(ghostTargetModeLabel.getText().contains(IMPORTANT_SYMBOL), ghostTargetModeLabel.getText());
            MouseEvent enter = new MouseEvent(ghostTargetModeLabel, MouseEvent.MOUSE_ENTERED,
                  System.currentTimeMillis(), 0, 0, 0, 0, false);
            for (var listener : ghostTargetModeLabel.getMouseListeners()) {
                listener.mouseEntered(enter);
            }
            String detailsText = findComponent(pane, JEditorPane.class).getText();
            assertTrue(detailsText.contains("Area Effect uses the original MegaMek ECM bubble behavior"), detailsText);
            assertFalse(detailsText.contains("Legacy:"), detailsText);
        });
    }

    @Test
    void bridgeCfUsesBoundedSpinnerAndKeepsBoardDefaultValue() throws Exception {
        runOnEdt(() -> {
            GameOptions options = new GameOptions();
            DialogOptionComponentYPanel bridgeCf = component(
                  options.getOption(OptionsConstants.BASE_BRIDGE_CF));

            pane(List.of(bridgeCf), option -> true);

            assertIntegerSpinner(bridgeCf, 0, 1_000, 0);
        });
    }

    @Test
    void woodsBurnDownAmountUsesDependentBoundedSpinner() throws Exception {
        runOnEdt(() -> {
            GameOptions options = new GameOptions();
            DialogOptionComponentYPanel woodsBurnDown = component(
                  options.getOption(OptionsConstants.ADVANCED_WOODS_BURN_DOWN));
            DialogOptionComponentYPanel woodsBurnDownAmount = component(
                  options.getOption(OptionsConstants.ADVANCED_WOODS_BURN_DOWN_AMOUNT));

            pane("advancedRules", List.of(woodsBurnDown, woodsBurnDownAmount), option -> true);

            assertIntegerSpinner(woodsBurnDownAmount, 1, 100, 5);
            assertEquals("Woods CF burned per turn", woodsBurnDownAmount.settingsLabel().getText());
            assertFalse(woodsBurnDownAmount.settingsControl().isEnabled());
            woodsBurnDown.setSelected(true);
            assertTrue(woodsBurnDownAmount.settingsControl().isEnabled());

            woodsBurnDownAmount.setEditable(false);
            woodsBurnDown.setSelected(false);
            woodsBurnDown.setSelected(true);
            assertFalse(woodsBurnDownAmount.settingsControl().isEnabled());
        });
    }

    @Test
    void terrainSectionsAlignRightColumnsAndFitUnofficialBadge() throws Exception {
        runOnEdt(() -> {
            GameOptions options = new GameOptions();
            DialogOptionComponentYPanel minefields = component(
                  options.getOption(OptionsConstants.ADVANCED_MINEFIELDS));
            DialogOptionComponentYPanel blackIce = component(
                  options.getOption(OptionsConstants.ADVANCED_BLACK_ICE));
            DialogOptionComponentYPanel woodsBurnDown = component(
                  options.getOption(OptionsConstants.ADVANCED_WOODS_BURN_DOWN));
            DialogOptionComponentYPanel woodsBurnDownAmount = component(
                  options.getOption(OptionsConstants.ADVANCED_WOODS_BURN_DOWN_AMOUNT));
            DialogOptionComponentYPanel battleWreck = component(
                  options.getOption(OptionsConstants.ADVANCED_TAC_OPS_BATTLE_WRECK));
            DialogOptionComponentYPanel bridgeBuilding = component(
                  options.getOption(OptionsConstants.ADVANCED_BRIDGE_BUILDING_ENGINEERS));
            DialogOptionComponentYPanel bridgeRepair = component(
                  options.getOption(OptionsConstants.UNOFFICIAL_BRIDGE_REPAIR_ENGINEERS));

            pane("advancedRules", List.of(minefields, blackIce, woodsBurnDown, woodsBurnDownAmount,
                  battleWreck, bridgeBuilding, bridgeRepair), option -> true);

            JCheckBox minefieldsCheckBox = minefields.settingsCheckBox();
            JCheckBox blackIceCheckBox = blackIce.settingsCheckBox();
            JLabel woodsAmountLabel = woodsBurnDownAmount.settingsLabel();
            JComponent woodsAmountControl = woodsBurnDownAmount.settingsControl();
            JCheckBox battleWreckCheckBox = battleWreck.settingsCheckBox();
            JCheckBox bridgeBuildingCheckBox = bridgeBuilding.settingsCheckBox();
            JCheckBox bridgeRepairCheckBox = bridgeRepair.settingsCheckBox();
            int bridgeRepairPreferredWidth = bridgeRepairCheckBox.getPreferredSize().width;

            assertSectionAlignment(minefieldsCheckBox, blackIceCheckBox,
                  woodsAmountLabel, woodsAmountControl, battleWreckCheckBox, bridgeBuildingCheckBox);
            assertTrue(bridgeRepairCheckBox.getText().contains("&nbsp;"), bridgeRepairCheckBox.getText());
            bridgeRepairCheckBox.getParent().doLayout();
            assertTrue(bridgeRepairCheckBox.getWidth() >= bridgeRepairPreferredWidth);
        });
    }

    @Test
    void targetingArtilleryUsesBoundedSpinnersAndPageWideColumnAlignment() throws Exception {
        runOnEdt(() -> {
            GameOptions options = new GameOptions();
            DialogOptionComponentYPanel extremeRange = component(
                  options.getOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_RANGE));
            DialogOptionComponentYPanel losRange = component(
                  options.getOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_LOS_RANGE));
            DialogOptionComponentYPanel calledShots = component(
                  options.getOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_CALLED_SHOTS));
            DialogOptionComponentYPanel glancingBlows = component(
                  options.getOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_GLANCING_BLOWS));
            DialogOptionComponentYPanel autohitHexes = component(
                  options.getOption(OptionsConstants.ADVANCED_COMBAT_NUM_HEXES_PREDESIGNATE));
            DialogOptionComponentYPanel mapAreaHexes = component(
                  options.getOption(OptionsConstants.ADVANCED_COMBAT_MAP_AREA_PREDESIGNATE));

            pane("advancedCombat", List.of(extremeRange, losRange, calledShots, glancingBlows,
                  autohitHexes, mapAreaHexes), option -> true);

            assertIntegerSpinner(autohitHexes, 0, 100, 5);
            assertIntegerSpinner(mapAreaHexes, 1, 100_000, 1_088);
            assertTrue(autohitHexes.settingsLabel().getText().contains("Autohit hexes per map area"));
            assertTrue(mapAreaHexes.settingsLabel().getText().contains("Map area size in hexes"));
            assertFalse(autohitHexes.settingsLabel().getText().contains("Specify"));
            assertFalse(mapAreaHexes.settingsLabel().getText().contains("Specify"));
            assertSectionAlignment(extremeRange.settingsCheckBox(), losRange.settingsCheckBox(),
                  calledShots.settingsCheckBox(), glancingBlows.settingsCheckBox(),
                  autohitHexes.settingsLabel(), autohitHexes.settingsControl());
            assertEquals(autohitHexes.settingsControl().getX(), mapAreaHexes.settingsControl().getX());
        });
    }

    @Test
    void heatAndFireGroupsFireOptionsBeforeHeatOptions() throws Exception {
        runOnEdt(() -> {
            GameOptions options = new GameOptions();
            DialogOptionComponentYPanel startFire = component(
                  options.getOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_START_FIRE));
            DialogOptionComponentYPanel expandedHeat = component(
                  options.getOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_HEAT));
            DialogOptionComponentYPanel coolantFailure = component(
                  options.getOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_COOLANT_FAILURE));
            DialogOptionComponentYPanel maximumExternalHeat = component(
                  options.getOption(OptionsConstants.ADVANCED_COMBAT_MAX_EXTERNAL_HEAT));
            DialogOptionComponentYPanel forestFiresNoSmoke = component(
                  options.getOption(OptionsConstants.ADVANCED_COMBAT_FOREST_FIRES_NO_SMOKE));

            pane("advancedCombat", List.of(startFire, expandedHeat, coolantFailure, maximumExternalHeat,
                  forestFiresNoSmoke), option -> true);

            assertTwoColumnCheckBoxGrid(startFire, forestFiresNoSmoke, expandedHeat, coolantFailure);
            assertLabelControlRow(maximumExternalHeat);
            assertIntegerSpinner(maximumExternalHeat, 0, 100, 15);
        });
    }

    @Test
    void mekLanceMovementUsesBoundedSpinnerAndKeepsDefault() throws Exception {
        runOnEdt(() -> {
            GameOptions options = new GameOptions();
            DialogOptionComponentYPanel mekLanceMovement = component(
                  options.getOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_MEK_LANCE_MOVEMENT));
            DialogOptionComponentYPanel mekLanceSize = component(
                  options.getOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_MEK_LANCE_MOVEMENT_NUMBER));

            pane("advancedGroundMovement", List.of(mekLanceMovement, mekLanceSize), option -> true);

            assertIntegerSpinner(mekLanceSize, 1, 100, 4);
            assertLabelControlRow(mekLanceSize);
            assertFalse(mekLanceSize.settingsControl().isEnabled());
            mekLanceMovement.setSelected(true);
            assertTrue(mekLanceSize.settingsControl().isEnabled());
        });
    }

    @Test
    void vehicleLanceMovementUsesDependentBoundedSpinner() throws Exception {
        runOnEdt(() -> {
            GameOptions options = new GameOptions();
            DialogOptionComponentYPanel vehicleLanceMovement = component(
                  options.getOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_VEHICLE_LANCE_MOVEMENT));
            DialogOptionComponentYPanel vehicleLanceSize = component(
                  options.getOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_VEHICLE_LANCE_MOVEMENT_NUMBER));

            pane("advancedGroundMovement", List.of(vehicleLanceMovement, vehicleLanceSize), option -> true);

            assertIntegerSpinner(vehicleLanceSize, 1, 100, 4);
            assertFalse(vehicleLanceSize.settingsControl().isEnabled());
            vehicleLanceMovement.setSelected(true);
            assertTrue(vehicleLanceSize.settingsControl().isEnabled());
        });
    }

    @Test
    void paratroopersRequireAssaultDropWithoutClearingSavedValue() throws Exception {
        runOnEdt(() -> {
            GameOptions options = new GameOptions();
            options.getOption(OptionsConstants.ADVANCED_PARATROOPERS).setValue(true);
            DialogOptionComponentYPanel assaultDrop = component(
                  options.getOption(OptionsConstants.ADVANCED_ASSAULT_DROP));
            DialogOptionComponentYPanel paratroopers = component(
                  options.getOption(OptionsConstants.ADVANCED_PARATROOPERS));

            pane("advancedRules", List.of(assaultDrop, paratroopers), option -> true);

            assertFalse(paratroopers.settingsCheckBox().isEnabled());
            assertTrue(paratroopers.settingsCheckBox().isSelected());
            assertFalse(paratroopers.hasChanged());
            assaultDrop.setSelected(true);
            assertTrue(paratroopers.settingsCheckBox().isEnabled());
            assertTrue(paratroopers.settingsCheckBox().isSelected());
            assaultDrop.setSelected(false);
            assertFalse(paratroopers.settingsCheckBox().isEnabled());
            assertTrue(paratroopers.settingsCheckBox().isSelected());
            assertFalse(paratroopers.hasChanged());
        });
    }

    @Test
    void climbOutRequiresReturnFlyoverWithoutClearingSavedValue() throws Exception {
        runOnEdt(() -> {
            GameOptions options = new GameOptions();
            options.getOption(OptionsConstants.ADVANCED_AERO_RULES_RETURN_FLYOVER).setValue(false);
            options.getOption(OptionsConstants.ADVANCED_AERO_RULES_CLIMB_OUT).setValue(true);
            DialogOptionComponentYPanel returnFlyover = component(
                  options.getOption(OptionsConstants.ADVANCED_AERO_RULES_RETURN_FLYOVER));
            DialogOptionComponentYPanel climbOut = component(
                  options.getOption(OptionsConstants.ADVANCED_AERO_RULES_CLIMB_OUT));

            pane("advancedAeroRules", List.of(returnFlyover, climbOut), option -> true);

            assertFalse(climbOut.settingsCheckBox().isEnabled());
            assertTrue(climbOut.settingsCheckBox().isSelected());
            assertFalse(climbOut.hasChanged());
            returnFlyover.setSelected(true);
            assertTrue(climbOut.settingsCheckBox().isEnabled());
            returnFlyover.setSelected(false);
            assertFalse(climbOut.settingsCheckBox().isEnabled());
            assertTrue(climbOut.settingsCheckBox().isSelected());
            assertFalse(climbOut.hasChanged());
        });
    }

    @Test
    void vibrabombProtectionAppearsInMekMovementWithShorterLabel() {
        GameOptionsPresentation.Location location = GameOptionsPresentation.location(
              "advancedGroundMovement", OptionsConstants.ADVANCED_GROUND_MOVEMENT_NO_PRE_MOVE_VIBRA);
        GameOptions options = new GameOptions();

        assertEquals("movement.meks", location.page().id());
        assertEquals("movement.meks.formations", location.sectionId());
        assertEquals("(Unofficial) Vibrabombs do not damage unmoved Meks",
              options.getOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_NO_PRE_MOVE_VIBRA).getDisplayableName());
    }

    @Test
    void aerospaceLaunchVelocityUsesRulesBoundedSpinner() throws Exception {
        runOnEdt(() -> {
            GameOptions options = new GameOptions();
            DialogOptionComponentYPanel launchVelocity = component(options.getOption(
                  OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_BEARINGS_ONLY_VELOCITY));

            pane("advancedAeroRules", List.of(launchVelocity), option -> true);

            assertIntegerSpinner(launchVelocity, 1, 500, 50);
        });
    }

    @Test
    void infantryAndProtoMekGroupSizeUsesBoundedSpinner() throws Exception {
        runOnEdt(() -> {
            GameOptions options = new GameOptions();
            DialogOptionComponentYPanel moveMultipleInfantry = component(
                options.getOption(OptionsConstants.INIT_INF_MOVE_MULTI));
            DialogOptionComponentYPanel moveMultipleProtoMeks = component(
                options.getOption(OptionsConstants.INIT_PROTOMEKS_MOVE_MULTI));
            DialogOptionComponentYPanel groupSize = component(
                  options.getOption(OptionsConstants.INIT_INF_PROTO_MOVE_MULTI));

            pane("initiative", List.of(moveMultipleInfantry, moveMultipleProtoMeks, groupSize), option -> true);

            assertIntegerSpinner(groupSize, 1, 100, 3);
            assertFalse(groupSize.settingsControl().isEnabled());
            moveMultipleInfantry.setSelected(true);
            assertTrue(groupSize.settingsControl().isEnabled());
            moveMultipleInfantry.setSelected(false);
            assertFalse(groupSize.settingsControl().isEnabled());
            moveMultipleProtoMeks.setSelected(true);
            assertTrue(groupSize.settingsControl().isEnabled());
            moveMultipleInfantry.setSelected(true);
            moveMultipleProtoMeks.setSelected(false);
            assertTrue(groupSize.settingsControl().isEnabled());
            moveMultipleInfantry.setSelected(false);
            assertFalse(groupSize.settingsControl().isEnabled());
        });
    }

    @Test
    void multiMovementControlsUseInfantryMovementPage() throws Exception {
        runOnEdt(() -> {
            GameOptions options = new GameOptions();
            DialogOptionComponentYPanel moveMultipleInfantry = component(
                  options.getOption(OptionsConstants.INIT_INF_MOVE_MULTI));
            DialogOptionComponentYPanel moveMultipleProtoMeks = component(
                  options.getOption(OptionsConstants.INIT_PROTOMEKS_MOVE_MULTI));
            DialogOptionComponentYPanel groupSize = component(
                  options.getOption(OptionsConstants.INIT_INF_PROTO_MOVE_MULTI));

            pane("initiative", List.of(moveMultipleInfantry, moveMultipleProtoMeks, groupSize), option -> true);

            assertTwoColumnCheckBoxGrid(moveMultipleInfantry, moveMultipleProtoMeks);
            assertLabelControlRow(groupSize);
            assertEquals("movement.infantry", GameOptionsPresentation.location(
                  "initiative", OptionsConstants.INIT_INF_MOVE_MULTI).page().id());
            assertEquals("movement.infantry", GameOptionsPresentation.location(
                  "initiative", OptionsConstants.INIT_PROTOMEKS_MOVE_MULTI).page().id());
            assertEquals("movement.infantry", GameOptionsPresentation.location(
                  "initiative", OptionsConstants.INIT_INF_PROTO_MOVE_MULTI).page().id());
        });
    }

    @Test
    void protoMekMultiMovementDetailsReferenceSharedInfantryCount() {
        GameOptions options = new GameOptions();
        String description = options.getOption(OptionsConstants.INIT_PROTOMEKS_MOVE_MULTI).getDescription();

        assertTrue(description.contains("Number of Infantry/ProtoMeks to move per Mek"), description);
        assertTrue(description.contains("Infantry Movement"), description);
    }

    @Test
    void rpgGunneryMovesSkillListToImportantDetails() throws Exception {
        runOnEdt(() -> {
            GameOptions options = new GameOptions();
            DialogOptionComponentYPanel rpgGunnery = component(
                  options.getOption(OptionsConstants.RPG_RPG_GUNNERY));
            GameOptionsPane pane = pane("rpg", List.of(rpgGunnery), option -> true);
            JCheckBox checkBox = rpgGunnery.settingsCheckBox();

            assertTrue(checkBox.getText().contains("RPG Gunnery Skills"));
            assertFalse(checkBox.getText().contains("Gunnery Laser/"));
            assertTrue(checkBox.getText().contains(IMPORTANT_SYMBOL));
            assertTrue(checkBox.getText().contains(LEGACY_SYMBOL));

            MouseEvent enter = new MouseEvent(checkBox, MouseEvent.MOUSE_ENTERED,
                  System.currentTimeMillis(), 0, 0, 0, 0, false);
            for (var listener : checkBox.getMouseListeners()) {
                listener.mouseEntered(enter);
            }
            String detailsText = findComponent(pane, JEditorPane.class).getText();
            assertTrue(detailsText.contains("Gunnery Laser"), detailsText);
            assertTrue(detailsText.contains("Gunnery Missile"), detailsText);
            assertTrue(detailsText.contains("Gunnery Ballistic"), detailsText);
            assertTrue(detailsText.contains("Piloting"), detailsText);
        });
    }

    private static GameOptionsPane pane(List<DialogOptionComponentYPanel> components,
          Predicate<IOption> visibility) {
        return pane("basic", components, visibility);
    }

    private static GameOptionsPane pane(String groupId, List<DialogOptionComponentYPanel> components,
          Predicate<IOption> visibility) {
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

    private static File factionLogoDirectory() {
        File configuredDirectory = new File(Configuration.universeImagesDir(), "factions");
        File directory = Configuration.dataDir().getAbsoluteFile();
        for (int level = 0; (level < 6) && (directory != null); level++) {
            File candidate = new File(directory, "mm-data" + File.separator + "data" + File.separator + "images"
                  + File.separator + "universe" + File.separator + "factions");
            if (candidate.isDirectory()) {
                return candidate;
            }
            directory = directory.getParentFile();
        }
        return configuredDirectory;
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

    private static <T extends Component> T findComponentByName(Container root, String name, Class<T> type) {
        return findComponentByNameOptional(root, name, type)
              .orElseThrow(() -> new AssertionError("No " + type.getSimpleName() + " named " + name + " found"));
    }

    private static <T extends Component> Optional<T> findComponentByNameOptional(
          Container root, String name, Class<T> type) {
        for (Component child : root.getComponents()) {
            if (type.isInstance(child) && name.equals(child.getName())) {
                return Optional.of(type.cast(child));
            }
            if (child instanceof Container container) {
                Optional<T> result = findComponentByNameOptional(container, name, type);
                if (result.isPresent()) {
                    return result;
                }
            }
        }
        return Optional.empty();
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

    private static JLabel findIconLabel(Container root) {
        for (Component child : root.getComponents()) {
            if (child instanceof JLabel label && label.getIcon() != null) {
                return label;
            }
            if (child instanceof Container container) {
                Optional<JLabel> result = findIconLabelOptional(container);
                if (result.isPresent()) {
                    return result.get();
                }
            }
        }
        throw new AssertionError("No icon label found");
    }

    private static Optional<JLabel> findIconLabelOptional(Container root) {
        for (Component child : root.getComponents()) {
            if (child instanceof JLabel label && label.getIcon() != null) {
                return Optional.of(label);
            }
            if (child instanceof Container container) {
                Optional<JLabel> result = findIconLabelOptional(container);
                if (result.isPresent()) {
                    return result;
                }
            }
        }
        return Optional.empty();
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
