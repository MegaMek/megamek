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
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
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
package megamek.client.ui.dialogs.buttonDialogs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Dimension;
import java.lang.reflect.InvocationTargetException;
import java.io.File;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import megamek.client.ui.clientGUI.DialogOptionListener;
import megamek.client.ui.panels.DialogOptionComponentYPanel;
import megamek.client.ui.panels.GameOptionsPane;
import megamek.common.options.GameOptions;
import megamek.common.options.IBasicOption;
import megamek.common.options.IOption;
import megamek.common.options.OptionsConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GameOptionsDialogTest {
    @TempDir
    private Path tempDirectory;

    @Test
    void ruleToggleLabelsUseBadgeIconsAndOnOffText() {
        String unofficialOn = GameOptionsDialog.ruleToggleText(
              GameOptionsPane.unofficialBadge(), "Reglas no oficiales", "Activado");
        String legacyOff = GameOptionsDialog.ruleToggleText(
              GameOptionsPane.legacyBadge(), "Reglas heredadas", "Desactivado");

        assertTrue(unofficialOn.contains(Character.toString(0xEA4B)), unofficialOn);
        assertTrue(unofficialOn.contains("color=\"#e69f00\""), unofficialOn);
        assertTrue(unofficialOn.contains("Reglas no oficiales: <b>Activado</b>"), unofficialOn);
        assertTrue(legacyOff.contains(Character.toString(0xE889)), legacyOff);
        assertTrue(legacyOff.contains("Reglas heredadas: <b>Desactivado</b>"), legacyOff);
        assertFalse(unofficialOn.contains("\u2713"), unofficialOn);
        assertFalse(legacyOff.contains("\u2717"), legacyOff);
    }

    @Test
    void responsiveFooterKeepsGroupsIntactAndWrapsWhenNeeded() {
        JPanel ruleControls = fixedSizePanel(760, 40);
        JPanel actionButtons = fixedSizePanel(720, 40);
        JPanel footer = GameOptionsDialog.responsiveFooter(ruleControls, actionButtons, 8);

        footer.setSize(1_600, 100);
        footer.doLayout();

        assertEquals(8, ruleControls.getX());
        assertEquals(ruleControls.getY(), actionButtons.getY());
        assertTrue(actionButtons.getX() + actionButtons.getWidth() <= footer.getWidth());

        footer.setSize(1_200, footer.getPreferredSize().height);
        footer.doLayout();

        assertEquals(8, ruleControls.getX());
        assertTrue(actionButtons.getY() > ruleControls.getY());
        assertEquals((footer.getWidth() - actionButtons.getWidth()) / 2, actionButtons.getX());
        assertTrue(ruleControls.getX() + ruleControls.getWidth() <= footer.getWidth());
        assertTrue(actionButtons.getX() + actionButtons.getWidth() <= footer.getWidth());
    }

    @Test
    void loadingOptionsPreservesCallerExcludedValues() {
        GameOptions loadedOptions = new GameOptions();
        loadedOptions.getOption(OptionsConstants.ALLOWED_YEAR).setValue(3150);
        loadedOptions.getOption(OptionsConstants.SEARCHLIGHTS_ON).setValue(true);
        Vector<IBasicOption> fileOptions = new Vector<>();
        fileOptions.add(loadedOptions.getOption(OptionsConstants.ALLOWED_YEAR));
        fileOptions.add(loadedOptions.getOption(OptionsConstants.SEARCHLIGHTS_ON));
        File file = tempDirectory.resolve("excluded-options.xml").toFile();
        GameOptions.saveOptions(fileOptions, file.getAbsolutePath());

        GameOptions targetOptions = new GameOptions();
        targetOptions.getOption(OptionsConstants.ALLOWED_YEAR).setValue(3025);
        targetOptions.getOption(OptionsConstants.SEARCHLIGHTS_ON).setValue(false);

        GameOptionsDialog.loadOptionsPreservingExcluded(targetOptions, file,
              Set.of(OptionsConstants.ALLOWED_YEAR));

        assertEquals(3025, targetOptions.intOption(OptionsConstants.ALLOWED_YEAR));
        assertTrue(targetOptions.booleanOption(OptionsConstants.SEARCHLIGHTS_ON));
    }

    @Test
    void savingOptionsIncludesCallerExcludedValues() {
        GameOptions options = new GameOptions();
        options.getOption(OptionsConstants.ALLOWED_YEAR).setValue(3025);
        Vector<IBasicOption> output = new Vector<>();
        output.add(options.getOption(OptionsConstants.SEARCHLIGHTS_ON));

        GameOptionsDialog.appendUnrepresentedOptions(output, options, Set.of(OptionsConstants.SEARCHLIGHTS_ON));
        File file = tempDirectory.resolve("saved-excluded-options.xml").toFile();
        GameOptions.saveOptions(output, file.getAbsolutePath());
        GameOptions loaded = new GameOptions();
        loaded.getOption(OptionsConstants.ALLOWED_YEAR).setValue(3150);
        loaded.loadOptions(file, false);

        assertEquals(3025, loaded.intOption(OptionsConstants.ALLOWED_YEAR));
    }

    @Test
    void deactivatingCategoryChangesOnlyMatchingOptions() {
        GameOptions options = new GameOptions();
        DialogOptionComponentYPanel assaultDrop = component(
              options.getOption(OptionsConstants.ADVANCED_ASSAULT_DROP));
        DialogOptionComponentYPanel ghostTargetMode = component(
              options.getOption(OptionsConstants.ADVANCED_GHOST_TARGET_MODE));
        DialogOptionComponentYPanel searchlights = component(options.getOption(OptionsConstants.SEARCHLIGHTS_ON));
        assaultDrop.setSelected(true);
        ghostTargetMode.addValue(OptionsConstants.GHOST_TARGET_MODE_LEGACY);
        ghostTargetMode.addValue(OptionsConstants.GHOST_TARGET_MODE_STANDARD);
        ghostTargetMode.setValue(OptionsConstants.GHOST_TARGET_MODE_LEGACY);
        searchlights.setSelected(true);

        GameOptionsDialog.deactivateOptions(Map.of(
              OptionsConstants.ADVANCED_ASSAULT_DROP, List.of(assaultDrop),
              OptionsConstants.ADVANCED_GHOST_TARGET_MODE, List.of(ghostTargetMode),
              OptionsConstants.SEARCHLIGHTS_ON, List.of(searchlights)),
              GameOptionsPane::isLegacyOption);

        assertFalse((Boolean) assaultDrop.getValue());
        assertEquals(OptionsConstants.GHOST_TARGET_MODE_STANDARD, ghostTargetMode.getValue());
        assertTrue((Boolean) searchlights.getValue());
    }

    @Test
    void coreRulesDisableExactlyTheRulesSpecificOptions() {
        Set<String> expectedDisabledOptions = Set.of(
              OptionsConstants.BASE_FLAMER_HEAT,
              OptionsConstants.ADVANCED_COMBAT_TAC_OPS_AMS,
              OptionsConstants.ADVANCED_COMBAT_TAC_OPS_CHARGE_DAMAGE,
              OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_WALK_BACKWARDS,
              OptionsConstants.ADVANCED_COMBAT_TAC_OPS_RETRACTABLE_BLADES,
              OptionsConstants.ADVANCED_COMBAT_UNJAM_UAC,
              OptionsConstants.INIT_FRONT_LOAD_INITIATIVE,
              OptionsConstants.ADVANCED_MINEFIELDS,
              OptionsConstants.ADVANCED_ALTERNATE_MASC,
              OptionsConstants.ADVANCED_ALTERNATE_MASC_ENHANCED);
        GameOptions options = new GameOptions();
        Map<String, List<DialogOptionComponentYPanel>> optionComponents = new LinkedHashMap<>();

        for (Enumeration<IOption> gameOptions = options.getOptions(); gameOptions.hasMoreElements(); ) {
            IOption option = gameOptions.nextElement();
            optionComponents.put(option.getName(), List.of(component(option)));
        }

        GameOptionsDialog.applyRulesSystemEditability(optionComponents, true, OptionsConstants.RULES_CORE);

        Set<String> disabledOptions = new LinkedHashSet<>();
        optionComponents.forEach((optionName, components) -> {
            if (!components.getFirst().getEditable()) {
                disabledOptions.add(optionName);
            }
        });
        assertEquals(expectedDisabledOptions, disabledOptions);
        assertTrue(optionComponents.get(OptionsConstants.ADVANCED_COMBAT_CASE_PILOT_DAMAGE)
              .getFirst().getEditable());

        GameOptionsDialog.applyRulesSystemEditability(optionComponents, true, OptionsConstants.RULES_TW);
        expectedDisabledOptions.forEach(optionName ->
              assertTrue(optionComponents.get(optionName).getFirst().getEditable(), optionName));

        GameOptionsDialog.applyRulesSystemEditability(optionComponents, false, OptionsConstants.RULES_TW);
        expectedDisabledOptions.forEach(optionName ->
              assertFalse(optionComponents.get(optionName).getFirst().getEditable(), optionName));
    }

    @Test
    void unknownRulesSystemFallsBackToCore() {
        assertEquals(OptionsConstants.RULES_CORE, GameOptionsDialog.normalizeRulesSystem(null));
        assertEquals(OptionsConstants.RULES_CORE, GameOptionsDialog.normalizeRulesSystem(42));
        assertEquals(OptionsConstants.RULES_CORE, GameOptionsDialog.normalizeRulesSystem("future rules"));
        assertEquals(OptionsConstants.RULES_TW, GameOptionsDialog.normalizeRulesSystem(OptionsConstants.RULES_TW));
    }

    @Test
    void openingDialogPresentationDoesNotChangeHiddenRuleValues() throws Exception {
        runOnEdt(() -> {
            GameOptions options = new GameOptions();
            options.getOption(OptionsConstants.UNOFFICIAL_BRIDGE_REPAIR_ENGINEERS).setValue(true);
            options.getOption(OptionsConstants.ADVANCED_ASSAULT_DROP).setValue(true);
            options.getOption(OptionsConstants.BASE_FLAMER_HEAT).setValue(true);
            DialogOptionComponentYPanel bridgeRepair = component(
                  options.getOption(OptionsConstants.UNOFFICIAL_BRIDGE_REPAIR_ENGINEERS));
            DialogOptionComponentYPanel assaultDrop = component(
                  options.getOption(OptionsConstants.ADVANCED_ASSAULT_DROP));
            DialogOptionComponentYPanel flamerHeat = component(
                  options.getOption(OptionsConstants.BASE_FLAMER_HEAT));
            List<GameOptionsPane.OptionGroup> groups = List.of(
                  new GameOptionsPane.OptionGroup("advancedRules", "Advanced Rules",
                        List.of(bridgeRepair, assaultDrop)),
                  new GameOptionsPane.OptionGroup("basic", "Basic", List.of(flamerHeat)));
            GameOptionsPane pane = new GameOptionsPane(groups,
                  option -> !GameOptionsPane.isUnofficialOption(option) && !GameOptionsPane.isLegacyOption(option));
            Map<String, List<DialogOptionComponentYPanel>> optionComponents = Map.of(
                  OptionsConstants.UNOFFICIAL_BRIDGE_REPAIR_ENGINEERS, List.of(bridgeRepair),
                  OptionsConstants.ADVANCED_ASSAULT_DROP, List.of(assaultDrop),
                  OptionsConstants.BASE_FLAMER_HEAT, List.of(flamerHeat));

            GameOptionsDialog.refreshOptionPresentation(
                  pane, optionComponents, true, OptionsConstants.RULES_CORE);

            for (DialogOptionComponentYPanel component : List.of(bridgeRepair, assaultDrop, flamerHeat)) {
                assertTrue((Boolean) component.getValue());
                assertFalse(component.hasChanged());
            }
            assertFalse(bridgeRepair.isVisible());
            assertFalse(assaultDrop.isVisible());
            assertFalse(flamerHeat.getEditable());
        });
    }

    @Test
    void switchingRulesSystemsPreservesVisibleStagedValues() throws Exception {
        runOnEdt(() -> {
            GameOptions options = new GameOptions();
            DialogOptionComponentYPanel flamerHeat = component(
                  options.getOption(OptionsConstants.BASE_FLAMER_HEAT));
            flamerHeat.setSelected(true);
            Map<String, List<DialogOptionComponentYPanel>> optionComponents = Map.of(
                  OptionsConstants.BASE_FLAMER_HEAT, List.of(flamerHeat));

            GameOptionsDialog.applyRulesSystemEditability(
                  optionComponents, true, OptionsConstants.RULES_CORE);

            assertTrue(flamerHeat.isVisible());
            assertFalse(flamerHeat.getEditable());
            assertTrue((Boolean) flamerHeat.getValue());

            GameOptionsDialog.applyRulesSystemEditability(
                  optionComponents, true, OptionsConstants.RULES_TW);

            assertTrue(flamerHeat.isVisible());
            assertTrue(flamerHeat.getEditable());
            assertTrue((Boolean) flamerHeat.getValue());

            GameOptionsDialog.applyRulesSystemEditability(
                  optionComponents, true, OptionsConstants.RULES_CORE);

            assertTrue(flamerHeat.isVisible());
            assertFalse(flamerHeat.getEditable());
            assertTrue((Boolean) flamerHeat.getValue());
        });
    }

    @Test
    void totalWarfareRestoresEnhancedMascDependency() throws Exception {
        runOnEdt(() -> {
            GameOptions options = new GameOptions();
            DialogOptionComponentYPanel alternateMasc = component(
                  options.getOption(OptionsConstants.ADVANCED_ALTERNATE_MASC));
            DialogOptionComponentYPanel enhancedMasc = component(
                  options.getOption(OptionsConstants.ADVANCED_ALTERNATE_MASC_ENHANCED));
            new GameOptionsPane(List.of(new GameOptionsPane.OptionGroup(
                  "advancedRules", "Advanced Rules", List.of(alternateMasc, enhancedMasc))), option -> true);
            Map<String, List<DialogOptionComponentYPanel>> optionComponents = Map.of(
                  OptionsConstants.ADVANCED_ALTERNATE_MASC, List.of(alternateMasc),
                  OptionsConstants.ADVANCED_ALTERNATE_MASC_ENHANCED, List.of(enhancedMasc));

            GameOptionsDialog.applyRulesSystemEditability(
                  optionComponents, true, OptionsConstants.RULES_CORE);
            assertFalse(alternateMasc.getEditable());
            assertFalse(enhancedMasc.getEditable());

            GameOptionsDialog.applyRulesSystemEditability(
                  optionComponents, true, OptionsConstants.RULES_TW);
            assertTrue(alternateMasc.getEditable());
            assertFalse(enhancedMasc.getEditable());

            alternateMasc.setSelected(true);
            assertTrue(enhancedMasc.getEditable());
        });
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

    private static JPanel fixedSizePanel(int width, int height) {
        JPanel panel = new JPanel();
        panel.setPreferredSize(new Dimension(width, height));
        return panel;
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
