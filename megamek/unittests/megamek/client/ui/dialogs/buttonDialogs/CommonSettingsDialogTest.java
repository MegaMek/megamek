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
package megamek.client.ui.dialogs.buttonDialogs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListSelectionModel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import megamek.client.ui.Messages;
import megamek.client.ui.buttons.ColourSelectorButton;
import megamek.client.ui.dialogs.unitDisplay.UnitDisplayPanel;
import megamek.client.ui.panels.CommonSettingsPane;
import megamek.client.ui.settings.SettingsBadge;
import megamek.client.ui.settings.SettingsButton;
import megamek.client.ui.settings.SettingsCheckBox;
import megamek.client.ui.settings.SettingsFormPanel;
import megamek.client.ui.settings.SettingsPagePanel;
import megamek.client.ui.util.PlayerColour;
import megamek.client.ui.util.UIUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CommonSettingsDialogTest {

    @Test
    void createsPlayerColourButtonsWithLocalizedLabels() {
        for (PlayerColour playerColour : PlayerColour.values()) {
            ColourSelectorButton button = CommonSettingsDialog.createPlayerColourButton(playerColour);
            String localizedName = playerColour.toString();

            button.setColour(Color.BLACK);

            assertFalse(button.getText().isBlank());
            assertEquals(localizedName, button.getText());
            assertEquals(localizedName, button.getAccessibleContext().getAccessibleName());
        }
    }

    @Test
    void resolvesBrownPlayerColourLocalization() {
        assertEquals("PlayerColour.BROWN.text", PlayerColour.PLAYER_COLOUR_BROWN);
        assertEquals("Brown", PlayerColour.BROWN.toString());
    }

    @Test
    void laysOutGenericCheckBoxesAndOddFillerInBalancedProductionGrid() {
        JCheckBox firstCheckBox = new JCheckBox("First");
        JCheckBox secondCheckBox = new JCheckBox(
            Messages.getString("CommonSettingsDialog.useAverageSkills"));
        JCheckBox thirdCheckBox = new JCheckBox("Third");
        int longCheckBoxWidth = secondCheckBox.getPreferredSize().width;

        JPanel form = settingsGroup(List.of(
          row(firstCheckBox), row(secondCheckBox), row(thirdCheckBox)));
        Component finalCellFiller = form.getComponent(3);
        GridBagConstraints fillerConstraints = ((GridBagLayout) form.getLayout()).getConstraints(finalCellFiller);
        CommonSettingsPane pane = createSettingsPane("generic", form);

        assertCell(form, firstCheckBox, 0, 0);
        assertCell(form, secondCheckBox, 1, 0);
        assertCell(form, thirdCheckBox, 0, 1);
        assertCell(form, finalCellFiller, 1, 1);
        assertEquals(thirdCheckBox.getPreferredSize().width, finalCellFiller.getPreferredSize().width);
        assertEquals(GridBagConstraints.HORIZONTAL, fillerConstraints.fill);
        assertTrue(fillerConstraints.weightx > 0.0);

        assertAtStandardAndWideWidths(pane, laidOutPane -> {
        assertAlignedBalancedRows(laidOutPane, List.of(
                    List.of(firstCheckBox, secondCheckBox),
                    List.of(thirdCheckBox, finalCellFiller)));
            assertTrue(secondCheckBox.getWidth() >= longCheckBoxWidth);
        });
    }

    @Test
    void keepsCustomRowsFullWidth() {
        JButton customButton = new JButton("Custom");

        JPanel form = settingsGroup(List.of(row(customButton)));
        GridBagConstraints constraints = ((GridBagLayout) form.getLayout()).getConstraints(customButton);
        CommonSettingsPane pane = createSettingsPane("custom", form);

        assertSame(form, customButton.getParent());
        assertEquals(0, constraints.gridx);
        assertEquals(2, constraints.gridwidth);
        assertEquals(GridBagConstraints.HORIZONTAL, constraints.fill);

        assertAtStandardAndWideWidths(pane, ignored -> {
            assertTrue(customButton.getWidth() > 0);
            assertEquals(0, customButton.getX());
            assertEquals(form.getWidth(), customButton.getWidth());
        });
    }

    @Test
    void laysOutApplicationSectionsOnSharedBalancedProductionTracks() {
        JLabel localeLabel = new JLabel(Messages.getString("CommonSettingsDialog.locale"));
        JComboBox<String> localeControl = new JComboBox<>(new String[] { "English", "Deutsch" });
        JLabel scaleLabel = new JLabel(Messages.getString("CommonSettingsDialog.guiScale"));
        JSlider scaleControl = CommonSettingsDialog.createGuiScaleSlider();
        JLabel userFilesLabel = new JLabel(Messages.getString("CommonSettingsDialog.userDir"));
        JTextField userFilesField = new JTextField("C:/Users/test/MegaMek", 20);
        JPanel userFilesControl = CommonSettingsDialog.applicationPathControl(
              userFilesField, new JButton("Choose"), new JButton("Help"));
        JLabel mmlLabel = new JLabel(Messages.getString("CommonSettingsDialog.mmlPath"));
        JTextField mmlField = new JTextField("C:/Games/MegaMekLab", 20);
        JPanel mmlControl = CommonSettingsDialog.applicationPathControl(mmlField, new JButton("Choose"));
        JLabel themeLabel = new JLabel(Messages.getString("CommonSettingsDialog.uiTheme"));
        JComboBox<String> themeControl = new JComboBox<>(new String[] { "Flat Light" });
        JLabel skinLabel = new JLabel(Messages.getString("CommonSettingsDialog.skinFile"));
        JComboBox<String> skinControl = new JComboBox<>(new String[] { "defaultSkin.xml" });
        List<JLabel> labels = List.of(localeLabel, scaleLabel, userFilesLabel, mmlLabel, themeLabel, skinLabel);
        List<JComponent> controls = List.of(
              localeControl, scaleControl, userFilesControl, mmlControl, themeControl, skinControl);
        List<Integer> naturalLabelWidths = labels.stream().map(label -> label.getPreferredSize().width).toList();

        JPanel localeSection = settingsGroup(List.of(row(localeLabel, localeControl)));
        JPanel scaleSection = settingsGroup(List.of(row(scaleLabel, scaleControl)));
        JPanel userFilesSection = settingsGroup(List.of(row(userFilesLabel, userFilesControl)));
        JPanel mmlSection = settingsGroup(List.of(row(mmlLabel, mmlControl)));
        JPanel themeSection = settingsGroup(List.of(
              row(themeLabel, themeControl), row(skinLabel, skinControl)));
        CommonSettingsPane pane = createSettingsPane("application",
              localeSection, scaleSection, userFilesSection, mmlSection, themeSection);

        assertSame(userFilesControl, userFilesLabel.getLabelFor());
        assertSame(mmlControl, mmlLabel.getLabelFor());
        assertAtStandardAndWideWidths(pane, laidOutPane -> {
            List<List<Component>> rows = new ArrayList<>();
            for (int index = 0; index < labels.size(); index++) {
                rows.add(List.of(labels.get(index), controls.get(index)));
                assertSame(controls.get(index), labels.get(index).getLabelFor());
                assertTrue(labels.get(index).getWidth() >= naturalLabelWidths.get(index));
            }
            assertAlignedBalancedRows(laidOutPane, rows);
            assertComponentWithinParent(userFilesField);
            assertComponentWithinParent(mmlField);
        });
    }

    @Test
    void laysOutColourSectionsAndNestedPlayerColoursOnSharedBalancedTracks() {
        ColourSelectorButton warning = new ColourSelectorButton("Warning");
        ColourSelectorButton caution = new ColourSelectorButton("Caution");
        ColourSelectorButton precaution = new ColourSelectorButton("Precaution");
        ColourSelectorButton okay = new ColourSelectorButton("Okay");
        ColourSelectorButton player = new ColourSelectorButton("Player unit");
        ColourSelectorButton ally = new ColourSelectorButton("Allied unit");
        ColourSelectorButton enemy = new ColourSelectorButton("Enemy unit");
        ColourSelectorButton playerRed = new ColourSelectorButton("Red");
        ColourSelectorButton playerBlue = new ColourSelectorButton("Blue");
        ColourSelectorButton playerGreen = new ColourSelectorButton("Green");
        List<ColourSelectorButton> colourButtons = List.of(
              warning, caution, precaution, okay, player, ally, enemy, playerRed, playerBlue, playerGreen);
        List<Integer> naturalWidths = colourButtons.stream().map(button -> button.getPreferredSize().width).toList();

        JPanel statusSection = settingsGroup(List.of(row(warning, caution, precaution, okay)));
        JPanel unitSection = settingsGroup(List.of(row(player, ally, enemy)));
        JPanel nestedPlayerContent = CommonSettingsDialog.createSettingsPanel(List.of(
              row(new JLabel("Player colours")), row(playerRed, playerBlue, playerGreen)));
        JPanel playerSection = settingsGroup(List.of(row(nestedPlayerContent)));
        JPanel statusGrid = findNamedPanel(statusSection, "pnlCommonSettingsColourGrid");
        JPanel unitGrid = findNamedPanel(unitSection, "pnlCommonSettingsColourGrid");
        JPanel playerGrid = findNamedPanel(playerSection, "pnlCommonSettingsColourGrid");
        Component unitFiller = unitGrid.getComponent(3);
        Component playerFiller = playerGrid.getComponent(3);
        CommonSettingsPane pane = createSettingsPane("colours", statusSection, unitSection, playerSection);

        assertSame(statusGrid, warning.getParent());
        assertCell(unitGrid, unitFiller, 1, 1);
        assertCell(playerGrid, playerFiller, 1, 1);
        assertAtStandardAndWideWidths(pane, laidOutPane -> {
            assertAlignedBalancedRows(laidOutPane, List.of(
                  List.of(warning, caution), List.of(precaution, okay),
                  List.of(player, ally), List.of(enemy, unitFiller),
                  List.of(playerRed, playerBlue), List.of(playerGreen, playerFiller)));
            for (int index = 0; index < colourButtons.size(); index++) {
                ColourSelectorButton button = colourButtons.get(index);
                assertTrue(button.getWidth() >= naturalWidths.get(index));
                assertEquals(SwingConstants.LEFT, button.getHorizontalAlignment());
            }
        });
    }

    @Test
    void laysOutAudioAsTwoSectionsWithAlignedControlColumns() {
        assertAudioSectionsHaveAlignedControlColumns();
    }

    @Test
    void laysOutGameBoardGeneralSectionsOnSharedBalancedTracks() {
        JLabel tilesetLabel = new JLabel(Messages.getString("CommonSettingsDialog.tileset"));
        JComboBox<String> tileset = new JComboBox<>(new String[] { "saxarba" });
        JCheckBox noAction = new JCheckBox(Messages.getString("CommonSettingsDialog.nagForNoAction"));
        JCheckBox psr = new JCheckBox(Messages.getString("CommonSettingsDialog.nagForPSR"));
        JCheckBox masc = new JCheckBox(Messages.getString("CommonSettingsDialog.nagForMASC"));
        JCheckBox sprint = new JCheckBox(Messages.getString("CommonSettingsDialog.nagForSprint"));
        JCheckBox focus = new JCheckBox("Focus window when a phase begins");
        JCheckBox endFiring = new JCheckBox("Skip Done when firing all weapons");
        JLabel buttonsLabel = new JLabel(Messages.getString("CommonSettingsDialog.buttonsPerRow"));
        JSpinner buttons = CommonSettingsDialog.createIntegerSpinner(12, 1, 1);
        JLabel playersLabel = new JLabel(Messages.getString("CommonSettingsDialog.playersRemainingToShow"));
        JSpinner players = CommonSettingsDialog.createIntegerSpinner(3, 0, 1);
        JCheckBox zoom = new JCheckBox(Messages.getString("CommonSettingsDialog.mouseWheelZoom"));
        JCheckBox flip = new JCheckBox(Messages.getString("CommonSettingsDialog.mouseWheelZoomFlip"));
        JCheckBox summary = new JCheckBox("Save game summary image of board");
        JLabel pathfinderLabel = new JLabel(Messages.getString("CommonSettingsDialog.pathFiderTimeLimit"));
        JSpinner pathfinder = CommonSettingsDialog.createIntegerSpinner(500, 1, 1);
        JCheckBox damageLabel = new JCheckBox(Messages.getString("CommonSettingsDialog.showDamageLevel"));
        JCheckBox damageIcon = new JCheckBox(Messages.getString("CommonSettingsDialog.showDamageDecal"));
        JCheckBox unitId = new JCheckBox(Messages.getString("CommonSettingsDialog.showUnitId"));
        ColourSelectorButton textColour = new ColourSelectorButton("Unit Text Color");
        ColourSelectorButton validColour = new ColourSelectorButton("Unit Valid Color");
        ColourSelectorButton selectedColour = new ColourSelectorButton("Unit Selected Color");

        JPanel tilesetGrid = CommonSettingsDialog.createGameBoardFieldGrid(
              "TestGameBoardTilesetGrid", tilesetLabel, tileset);
        JPanel confirmationGrid = CommonSettingsDialog.createGameBoardOptionGrid(
              "TestGameBoardConfirmationsGrid", noAction, psr, masc, sprint);
        JPanel actionGrid = CommonSettingsDialog.createGameBoardOptionGrid(
              "TestGameBoardActionsGrid", focus, endFiring);
        JPanel controlsGrid = CommonSettingsDialog.createGameBoardControlsGrid(
              "TestGameBoardControlsGrid", buttonsLabel, buttons, playersLabel, players, zoom, flip, summary);
        JPanel pathfinderGrid = CommonSettingsDialog.createGameBoardFieldGrid(
              "TestGameBoardPathfinderGrid", pathfinderLabel, pathfinder);
        JPanel unitsGrid = CommonSettingsDialog.createGameBoardGroupedOptionGrid(
              "TestGameBoardUnitsGrid", List.of(damageLabel, damageIcon, unitId),
            List.of(textColour, validColour, selectedColour));

        JPanel tilesetSection = settingsGroup(List.of(row(tilesetGrid)));
        JPanel confirmationSection = settingsGroup(List.of(row(confirmationGrid)));
        JPanel actionSection = settingsGroup(List.of(row(actionGrid)));
        JPanel controlsSection = settingsGroup(List.of(row(controlsGrid)));
        JPanel pathfinderSection = settingsGroup(List.of(row(pathfinderGrid)));
        JPanel unitsSection = settingsGroup(List.of(row(unitsGrid)));
        CommonSettingsPane pane = createSettingsPane("gameBoardGeneral",
              tilesetSection, confirmationSection, actionSection, controlsSection, pathfinderSection, unitsSection);
        Component controlsFiller = controlsGrid.getComponent(7);
        Component unitOptionFiller = unitsGrid.getComponent(3);
        Component unitColourFiller = unitsGrid.getComponent(7);

        assertSame(tileset, tilesetLabel.getLabelFor());
        assertSame(buttons, buttonsLabel.getLabelFor());
        assertSame(players, playersLabel.getLabelFor());
        assertSame(pathfinder, pathfinderLabel.getLabelFor());
        assertAtConstrainedStandardAndWideWidths(pane,
              List.of(tilesetSection, confirmationSection, actionSection,
                    controlsSection, pathfinderSection, unitsSection),
              laidOutPane -> assertAlignedBalancedRows(laidOutPane, List.of(
                    List.of(tilesetLabel, tileset),
                    List.of(noAction, psr), List.of(masc, sprint),
                    List.of(focus, endFiring),
                    List.of(buttonsLabel, buttons), List.of(playersLabel, players),
                    List.of(zoom, flip), List.of(summary, controlsFiller),
                    List.of(pathfinderLabel, pathfinder),
                    List.of(damageLabel, damageIcon), List.of(unitId, unitOptionFiller),
                    List.of(textColour, validColour), List.of(selectedColour, unitColourFiller))));
    }

    @Test
    void constrainsGameBoardGeneralNumericSpinners() {
        JSpinner buttons = CommonSettingsDialog.createIntegerSpinner(0, 1, 1);
        JSpinner players = CommonSettingsDialog.createIntegerSpinner(-1, 0, 1);
        JSpinner pathfinder = CommonSettingsDialog.createIntegerSpinner(500, 1, 1);

        assertEquals(1, buttons.getValue());
        assertEquals(1, ((javax.swing.SpinnerNumberModel) buttons.getModel()).getMinimum());
        assertEquals(0, players.getValue());
        assertEquals(0, ((javax.swing.SpinnerNumberModel) players.getModel()).getMinimum());
        assertEquals(500, pathfinder.getValue());
        assertEquals(1, ((javax.swing.SpinnerNumberModel) pathfinder.getModel()).getMinimum());
        assertEquals(1, ((javax.swing.SpinnerNumberModel) pathfinder.getModel()).getStepSize());
    }

    @Test
    void gameBoardGeneralLabelsFitStandardBalancedCells() {
        int cellWidth = UIUtil.scaleForGUI(SettingsFormPanel.DEFAULT_LABEL_WIDTH);
        List<String> keys = List.of(
            "CommonSettingsDialog.nagForNoAction",
            "CommonSettingsDialog.nagForCrushingBuildings",
            "CommonSettingsDialog.nagForMechanicalJumpFallDamage",
            "CommonSettingsDialog.nagForWiGELanding",
            "CommonSettingsDialog.nagForUnJamRAC",
            "CommonSettingsDialog.nagForLaunchDoors",
            "CommonSettingsDialog.nagForOverheat",
            "CommonSettingsDialog.nagForDishonor",
            "CommonSettingsDialog.nagForOddSizedBoard",
            "CommonSettingsDialog.autoDeclareSearchlight",
            "CommonSettingsDialog.useSoftCenter");

        for (String key : keys) {
            JCheckBox checkBox = new JCheckBox(Messages.getString(key));
            assertTrue(checkBox.getPreferredSize().width <= cellWidth,
                key + " requires " + checkBox.getPreferredSize().width + "px");
        }
        JLabel playersLabel = new JLabel(Messages.getString("CommonSettingsDialog.playersRemainingToShow"));
        assertTrue(playersLabel.getPreferredSize().width <= cellWidth);
    }

    @Test
    void laysOutGameBoardAppearanceSectionsOnSharedBalancedTracks() {
        JCheckBox animate = new JCheckBox(Messages.getString("CommonSettingsDialog.animateMove"));
        JCheckBox wrecks = new JCheckBox(Messages.getString("CommonSettingsDialog.showWrecks"));
        JCheckBox quality = new JCheckBox(Messages.getString("CommonSettingsDialog.highQualityGraphics"));
        JCheckBox performance = new JCheckBox(Messages.getString("CommonSettingsDialog.highPerformanceGraphics"));
        JCheckBox mapSheets = new JCheckBox(Messages.getString("CommonSettingsDialog.showMapsheets"));
        ColourSelectorButton mapSheetColour = new ColourSelectorButton("Map Sheet Color");
        JCheckBox ambientShadows = new JCheckBox(Messages.getString("CommonSettingsDialog.aOHexSHadows"));
        JCheckBox spriteShadows = new JCheckBox(Messages.getString("CommonSettingsDialog.useShadowMap"));
        JCheckBox inclines = new JCheckBox(Messages.getString("CommonSettingsDialog.useInclines"));
        JCheckBox level = new JCheckBox(Messages.getString("CommonSettingsDialog.levelHighlight"));
        ColourSelectorButton boardText = new ColourSelectorButton("Board Text Color");
        ColourSelectorButton spaceText = new ColourSelectorButton("Board Space Text Color");
        ColourSelectorButton buildingText = new ColourSelectorButton("Building Text Color");
        JCheckBox demolitionOutline = new JCheckBox(
            Messages.getString("CommonSettingsDialog.demolitionChargeHazardOutline"));

        JPanel renderingGrid = CommonSettingsDialog.createGameBoardGroupedOptionGrid(
            "TestGameBoardRenderingGrid",
            List.of(animate, wrecks, quality, performance),
            List.of(mapSheets, ambientShadows, spriteShadows, inclines, level),
            List.of(mapSheetColour, boardText, spaceText, buildingText),
            List.of(demolitionOutline));

        JLabel attackLabel = new JLabel(Messages.getString("CommonSettingsDialog.attackArrowTransparency"));
        JSpinner attack = new JSpinner();
        JLabel ecmLabel = new JLabel(Messages.getString("CommonSettingsDialog.ecmTransparency"));
        JSpinner ecm = new JSpinner();
        JLabel pipLabel = new JLabel(Messages.getString("CommonSettingsDialog.tmmPipMode"));
        JComboBox<String> pips = new JComboBox<>(new String[] { "No Pips" });
        JPanel indicatorsGrid = CommonSettingsDialog.createGameBoardFieldGrid(
                  "TestGameBoardIndicatorsGrid", attackLabel, attack, ecmLabel, ecm, pipLabel, pips);

        JLabel fontLabel = new JLabel(Messages.getString("CommonSettingsDialog.moveFontType"));
        JComboBox<String> font = new JComboBox<>(new String[] { "Noto Sans" });
        JLabel sizeLabel = new JLabel(Messages.getString("CommonSettingsDialog.moveFontSize"));
        JSpinner size = CommonSettingsDialog.createIntegerSpinner(26, 1, 1);
        JLabel styleLabel = new JLabel(Messages.getString("CommonSettingsDialog.moveFontStyle"));
        JComboBox<String> style = new JComboBox<>(new String[] { "Plain" });
        ColourSelectorButton defaultMove = new ColourSelectorButton("Move Default Color");
        ColourSelectorButton illegalMove = new ColourSelectorButton("Move Illegal Color");
        ColourSelectorButton jumpMove = new ColourSelectorButton("Move Jump Color");
        SettingsFormPanel movementGrid = CommonSettingsDialog.createGameBoardFieldGrid(
                  "TestGameBoardMovementGrid", fontLabel, font, sizeLabel, size, styleLabel, style);
        movementGrid.addEqualWidthComponentGrid(2, defaultMove, illegalMove, jumpMove);

        ColourSelectorButton visibleFire = new ColourSelectorButton("Firing Solutions Color - Can See");
        ColourSelectorButton hiddenFire = new ColourSelectorButton("Firing Solutions Color - Can't See");
        ColourSelectorButton sensorRange = new ColourSelectorButton("Sensor Range Color");
        JPanel fireGrid = CommonSettingsDialog.createGameBoardOptionGrid(
                  "TestGameBoardFireGrid", visibleFire, hiddenFire, sensorRange);

        JPanel renderingSection = settingsGroup(List.of(row(renderingGrid)));
        JPanel indicatorsSection = settingsGroup(List.of(row(indicatorsGrid)));
        JPanel movementSection = settingsGroup(List.of(row(movementGrid)));
        JPanel fireSection = settingsGroup(List.of(row(fireGrid)));
        CommonSettingsPane pane = createSettingsPane("gameBoardAppearance",
                  renderingSection, indicatorsSection, movementSection, fireSection);

        Component renderingOptionFiller = renderingGrid.getComponent(9);
        Component demolitionFiller = renderingGrid.getComponent(15);
        Component movementFiller = movementGrid.getComponent(9);
        Component fireFiller = fireGrid.getComponent(3);

        assertSame(attack, attackLabel.getLabelFor());
        assertSame(ecm, ecmLabel.getLabelFor());
        assertSame(pips, pipLabel.getLabelFor());
        assertSame(font, fontLabel.getLabelFor());
        assertSame(size, sizeLabel.getLabelFor());
        assertSame(style, styleLabel.getLabelFor());
        assertEquals(26, size.getValue());
        assertEquals(1, ((javax.swing.SpinnerNumberModel) size.getModel()).getMinimum());
        assertEquals(1, ((javax.swing.SpinnerNumberModel) size.getModel()).getStepSize());
        assertAtConstrainedStandardAndWideWidths(pane,
            List.of(renderingSection, indicatorsSection, movementSection, fireSection),
                  laidOutPane -> assertAlignedBalancedRows(laidOutPane, List.of(
                      List.of(animate, wrecks), List.of(quality, performance),
                      List.of(mapSheets, ambientShadows), List.of(spriteShadows, inclines),
                    List.of(level, renderingOptionFiller),
                      List.of(mapSheetColour, boardText), List.of(spaceText, buildingText),
                    List.of(demolitionOutline, demolitionFiller),
                      List.of(attackLabel, attack), List.of(ecmLabel, ecm), List.of(pipLabel, pips),
                      List.of(fontLabel, font), List.of(sizeLabel, size), List.of(styleLabel, style),
                      List.of(defaultMove, illegalMove), List.of(jumpMove, movementFiller),
                      List.of(visibleFire, hiddenFire), List.of(sensorRange, fireFiller))));
    }

    @Test
    void gameBoardAppearanceLabelsFitStandardBalancedCells() {
        int cellWidth = UIUtil.scaleForGUI(SettingsFormPanel.DEFAULT_LABEL_WIDTH);
        List<String> keys = List.of(
            "CommonSettingsDialog.aOHexSHadows",
            "CommonSettingsDialog.useShadowMap",
            "CommonSettingsDialog.useInclines",
            "CommonSettingsDialog.levelHighlight",
            "CommonSettingsDialog.floatingIso",
            "CommonSettingsDialog.demolitionChargeHazardOutline");

        for (String key : keys) {
            JCheckBox checkBox = new JCheckBox(Messages.getString(key));
            assertTrue(checkBox.getPreferredSize().width <= cellWidth,
                key + " requires " + checkBox.getPreferredSize().width + "px");
        }
    }

    @Test
    void laysOutGameBoardFieldOfViewAsBalancedHybridForm() {
        JCheckBox insideEnabled = new JCheckBox(
            Messages.getString("TacticalOverlaySettingsDialog.FovInsideEnabled"));
        JLabel insideOpacityLabel = new JLabel(
            Messages.getString("TacticalOverlaySettingsDialog.FovHighlightAlpha"));
        JSlider insideOpacity = new JSlider(0, 255, 40);
        JSpinner insidePercent = new JSpinner(new javax.swing.SpinnerNumberModel(0, 0, 100, 1));
        JPanel insideOpacityControl = CommonSettingsDialog.createGameBoardFovOpacityControl(
                  insideOpacity, insidePercent);
        JLabel rangesLabel = new JLabel(
            Messages.getString("TacticalOverlaySettingsDialog.FovHighlightRanges"));
        FovHighlightRingsPanel ranges = new FovHighlightRingsPanel(
                  "5 10", "0.3 1 1 ; 0.6 1 1", () -> { });
        SettingsFormPanel insideGrid = CommonSettingsDialog.createGameBoardFovInsideGrid(
                  insideEnabled, insideOpacityLabel, insideOpacityControl, rangesLabel, ranges);

        JCheckBox outsideEnabled = new JCheckBox(
            Messages.getString("TacticalOverlaySettingsDialog.FovOutsideEnabled"));
        JCheckBox grayscale = new JCheckBox(
            Messages.getString("TacticalOverlaySettingsDialog.FovGrayscale"));
        JLabel outsideOpacityLabel = new JLabel(
            Messages.getString("TacticalOverlaySettingsDialog.FovDarkenAlpha"));
        JSlider outsideOpacity = new JSlider(0, 255, 100);
        JSpinner outsidePercent = new JSpinner(new javax.swing.SpinnerNumberModel(0, 0, 100, 1));
        JPanel outsideOpacityControl = CommonSettingsDialog.createGameBoardFovOpacityControl(
                  outsideOpacity, outsidePercent);
        JLabel stripesLabel = new JLabel(Messages.getString("TacticalOverlaySettingsDialog.FovStripes"));
        JSpinner stripes = new JSpinner(new javax.swing.SpinnerNumberModel(35, 0, 50, 1));
        SettingsFormPanel outsideGrid = CommonSettingsDialog.createGameBoardFovOutsideGrid(
                  outsideEnabled, grayscale, outsideOpacityLabel, outsideOpacityControl, stripesLabel, stripes);

        JPanel insideSection = settingsGroup(List.of(row(insideGrid)));
        JPanel outsideSection = settingsGroup(List.of(row(outsideGrid)));
        CommonSettingsPane pane = createSettingsPane("gameBoardFieldOfView", insideSection, outsideSection);
        SettingsFormPanel insideFields = (SettingsFormPanel) insideGrid.getComponent(2);
        JPanel insideOpacityField = (JPanel) insideFields.getComponent(0);
        Component insideFieldFiller = insideFields.getComponent(1);
        JPanel rangesField = (JPanel) insideGrid.getComponent(3);
        SettingsFormPanel outsideOptions = (SettingsFormPanel) outsideGrid.getComponent(0);
        SettingsFormPanel outsideFields = (SettingsFormPanel) outsideGrid.getComponent(1);
        JPanel outsideOpacityField = (JPanel) outsideFields.getComponent(0);
        JPanel stripesField = (JPanel) outsideFields.getComponent(1);

        assertSame(insideOpacity, insideOpacityLabel.getLabelFor());
        assertSame(ranges, rangesLabel.getLabelFor());
        assertSame(outsideOpacity, outsideOpacityLabel.getLabelFor());
        assertSame(stripes, stripesLabel.getLabelFor());
        assertEquals(16, insidePercent.getValue());
        assertEquals(39, outsidePercent.getValue());
        assertFalse(insideOpacity.getPaintTicks());
        assertFalse(insideOpacity.getPaintLabels());
        assertAtConstrainedStandardAndWideWidths(pane, List.of(insideSection, outsideSection), laidOutPane -> {
                assertAlignedBalancedRows(insideFields, List.of(
                    List.of(insideOpacityField, insideFieldFiller)));
                assertAlignedBalancedRows(outsideGrid, List.of(
                    List.of(outsideEnabled, grayscale),
                    List.of(outsideOpacityField, stripesField)));
            assertSame(outsideEnabled.getParent(), outsideOptions);
            assertEquals(insideFields.getX(), rangesField.getX());
            assertEquals(insideFields.getWidth(), rangesField.getWidth());
            assertTrue(insideOpacity.getWidth() >= UIUtil.scaleForGUI(80));
            assertTrue(outsideOpacity.getWidth() >= UIUtil.scaleForGUI(80));
        });
    }

    @Test
    void synchronizesGameBoardFieldOfViewOpacityPercentages() {
        JSlider opacity = new JSlider(0, 255, 40);
        JSpinner percentage = new JSpinner(new javax.swing.SpinnerNumberModel(0, 0, 100, 1));
        CommonSettingsDialog.createGameBoardFovOpacityControl(opacity, percentage);

        assertEquals(16, percentage.getValue());
        percentage.setValue(50);
        assertEquals(128, opacity.getValue());
        opacity.setValue(255);
        assertEquals(100, percentage.getValue());
        assertEquals(0, CommonSettingsDialog.fovPercentToAlpha(0));
        assertEquals(0, CommonSettingsDialog.fovAlphaToPercent(0));
    }

    @Test
    void gameBoardFieldOfViewLabelsFitStandardBalancedCells() {
        int cellWidth = UIUtil.scaleForGUI(SettingsFormPanel.DEFAULT_LABEL_WIDTH);
        List<String> checkBoxKeys = List.of(
            "TacticalOverlaySettingsDialog.FovInsideEnabled",
            "TacticalOverlaySettingsDialog.FovOutsideEnabled",
            "TacticalOverlaySettingsDialog.FovGrayscale");
        List<String> labelKeys = List.of(
            "TacticalOverlaySettingsDialog.FovHighlightAlpha",
            "TacticalOverlaySettingsDialog.FovHighlightRanges",
            "TacticalOverlaySettingsDialog.FovDarkenAlpha",
            "TacticalOverlaySettingsDialog.FovStripes");

        for (String key : checkBoxKeys) {
            JCheckBox checkBox = new JCheckBox(Messages.getString(key));
            assertTrue(checkBox.getPreferredSize().width <= cellWidth,
                key + " requires " + checkBox.getPreferredSize().width + "px");
        }
        for (String key : labelKeys) {
            JLabel label = new JLabel(Messages.getString(key));
            assertTrue(label.getPreferredSize().width <= cellWidth,
                key + " requires " + label.getPreferredSize().width + "px");
        }
    }

    @Test
    void laysOutTooltipSectionsOnBalancedTracks() {
        JLabel popupLabel = new JLabel(Messages.getString("CommonSettingsDialog.tooltipDelay"));
        JSpinner popup = CommonSettingsDialog.createTooltipIntegerSpinner(1000, 0, 100);
        JLabel dismissLabel = new JLabel(Messages.getString("CommonSettingsDialog.tooltipDismissDelay"));
        JSpinner dismiss = CommonSettingsDialog.createTooltipIntegerSpinner(-1, -1, 1);
        JLabel suppressionLabel = new JLabel(Messages.getString("CommonSettingsDialog.tooltipDistSuppression"));
        JSpinner suppression = CommonSettingsDialog.createTooltipIntegerSpinner(60, 0, 1);
        JLabel fontLabel = new JLabel(Messages.getString("CommonSettingsDialog.unitTooltipFontSizeMod"));
        JComboBox<String> font = new JComboBox<>(new String[] { "small", "medium", "large" });
        JCheckBox weapons = new JCheckBox(Messages.getString("CommonSettingsDialog.showWpsinTT"));
        JCheckBox locations = new JCheckBox(Messages.getString("CommonSettingsDialog.showWpsLocinTT"));
        JCheckBox portrait = new JCheckBox(Messages.getString("CommonSettingsDialog.showPilotPortraitTT"));
        ColourSelectorButton foreground = new ColourSelectorButton("Foreground Color");
        ColourSelectorButton deEmphasized = new ColourSelectorButton("De-emphasized Color");
        ColourSelectorButton building = new ColourSelectorButton("Building Color");
        SettingsFormPanel contentGrid = CommonSettingsDialog.createTooltipContentGrid(
            new JComponent[] {
                               popupLabel, popup, dismissLabel, dismiss,
                               suppressionLabel, suppression, fontLabel, font },
            new JComponent[] { weapons, locations, portrait },
            new JComponent[] { foreground, deEmphasized, building });

        JCheckBox enabled = new JCheckBox(Messages.getString("CommonSettingsDialog.showArmorMiniVisTT"));
        ColourSelectorButton intact = new ColourSelectorButton("Intact Color");
        ColourSelectorButton partial = new ColourSelectorButton("Partially Damaged Color");
        ColourSelectorButton damaged = new ColourSelectorButton("Damaged Color");
        JLabel armorLabel = new JLabel(Messages.getString("CommonSettingsDialog.armorMiniArmorChar"));
        JComboBox<CommonSettingsDialog.TooltipSymbolOption> armor = CommonSettingsDialog
            .createTooltipSymbolSelector("\u2B1B");
        JLabel internalLabel = new JLabel(Messages.getString(
            "CommonSettingsDialog.armorMiniInternalStructureChar"));
        JComboBox<CommonSettingsDialog.TooltipSymbolOption> internal = CommonSettingsDialog
            .createTooltipSymbolSelector("\u26CA");
        JLabel unitsLabel = new JLabel(Messages.getString("CommonSettingsDialog.armorMiniUnitsPerBlock"));
        JSpinner units = CommonSettingsDialog.createTooltipIntegerSpinner(10, 1, 1);
        SettingsFormPanel armorGrid = CommonSettingsDialog.createTooltipArmorGrid(enabled,
            new JComponent[] { intact, partial, damaged },
            new JComponent[] {
                               armorLabel, armor, internalLabel, internal, unitsLabel, units });

        JPanel contentSection = settingsGroup(List.of(row(contentGrid)));
        JPanel armorSection = settingsGroup(List.of(row(armorGrid)));
        CommonSettingsPane pane = createSettingsPane("tooltips", contentSection, armorSection);
        Component contentOptionFiller = contentGrid.getComponent(11);
        Component contentColourFiller = contentGrid.getComponent(15);
        Component armorEnabledFiller = armorGrid.getComponent(1);
        Component armorColourFiller = armorGrid.getComponent(5);

        assertSame(popup, popupLabel.getLabelFor());
        assertSame(dismiss, dismissLabel.getLabelFor());
        assertSame(suppression, suppressionLabel.getLabelFor());
        assertSame(font, fontLabel.getLabelFor());
        assertSame(armor, armorLabel.getLabelFor());
        assertSame(internal, internalLabel.getLabelFor());
        assertSame(units, unitsLabel.getLabelFor());
        assertAtConstrainedStandardAndWideWidths(pane, List.of(contentSection, armorSection), laidOutPane -> {
            assertAlignedBalancedRows(contentGrid,
                List.of(
                    List.of(popupLabel, popup),
                    List.of(dismissLabel, dismiss),
                    List.of(suppressionLabel, suppression),
                    List.of(fontLabel, font),
                    List.of(weapons, locations),
                    List.of(portrait, contentOptionFiller),
                    List.of(foreground, deEmphasized),
                    List.of(building, contentColourFiller)));
            assertAlignedBalancedRows(armorGrid,
                List.of(
                    List.of(enabled, armorEnabledFiller),
                    List.of(intact, partial),
                    List.of(damaged, armorColourFiller),
                    List.of(armorLabel, armor),
                    List.of(internalLabel, internal),
                    List.of(unitsLabel, units)));
        });
    }

    @Test
    void constrainsTooltipNumericSpinnersToValidMinimums() {
        JSpinner popup = CommonSettingsDialog.createTooltipIntegerSpinner(-5, 0, 100);
        JSpinner dismiss = CommonSettingsDialog.createTooltipIntegerSpinner(-1, -1, 1);
        JSpinner units = CommonSettingsDialog.createTooltipIntegerSpinner(10, 1, 1);

        assertEquals(0, popup.getValue());
        assertEquals(0, ((javax.swing.SpinnerNumberModel) popup.getModel()).getMinimum());
        assertEquals(100, ((javax.swing.SpinnerNumberModel) popup.getModel()).getStepSize());
        assertEquals(-1, dismiss.getValue());
        assertEquals(-1, ((javax.swing.SpinnerNumberModel) dismiss.getModel()).getMinimum());
        assertEquals(1, ((javax.swing.SpinnerNumberModel) dismiss.getModel()).getStepSize());
        assertEquals(10, units.getValue());
        assertEquals(1, ((javax.swing.SpinnerNumberModel) units.getModel()).getMinimum());
        assertEquals(1, ((javax.swing.SpinnerNumberModel) units.getModel()).getStepSize());
    }

    @Test
    void offersDescriptiveTooltipSymbolsAndPreservesCustomValues() {
        JComboBox<CommonSettingsDialog.TooltipSymbolOption> shipped = CommonSettingsDialog
            .createTooltipSymbolSelector("\u26CA");
        JComboBox<CommonSettingsDialog.TooltipSymbolOption> custom = CommonSettingsDialog
            .createTooltipSymbolSelector("custom-symbol");

        assertFalse(shipped.isEditable());
        assertEquals("\u26CA", CommonSettingsDialog.selectedTooltipSymbol(shipped));
        assertTrue(shipped.getSelectedItem().toString().contains("Shield"));
        assertEquals("custom-symbol", CommonSettingsDialog.selectedTooltipSymbol(custom));
        assertTrue(custom.getSelectedItem().toString().contains("Custom"));
        assertEquals(shipped.getItemCount() + 1, custom.getItemCount());
    }

    @Test
    void tooltipLabelsFitStandardBalancedCells() {
        int cellWidth = UIUtil.scaleForGUI(SettingsFormPanel.DEFAULT_LABEL_WIDTH);
        List<String> labelKeys = List.of(
            "CommonSettingsDialog.tooltipDelay",
            "CommonSettingsDialog.tooltipDismissDelay",
            "CommonSettingsDialog.tooltipDistSuppression",
            "CommonSettingsDialog.unitTooltipFontSizeMod",
            "CommonSettingsDialog.armorMiniArmorChar",
            "CommonSettingsDialog.armorMiniCapArmorChar",
            "CommonSettingsDialog.armorMiniCriticalChar",
            "CommonSettingsDialog.armorMiniDestroyedChar",
            "CommonSettingsDialog.armorMiniInternalStructureChar",
            "CommonSettingsDialog.armorMiniUnitsPerBlock");

        for (String key : labelKeys) {
            JLabel label = new JLabel(Messages.getString(key));
            assertTrue(label.getPreferredSize().width <= cellWidth,
                key + " requires " + label.getPreferredSize().width + "px");
        }
    }

    @Test
    void laysOutUnitDisplaySectionsWithoutWideningThePage() {
        JLabel seenByLabel = new JLabel(Messages.getString("CommonSettingsDialog.seenby.label"));
        JComboBox<String> seenBy = new JComboBox<>(new String[] { "Someone", "Team", "Player" });
        JLabel[] heatLabels = new JLabel[6];
        JSpinner[] heatSpinners = new JSpinner[6];
        ColourSelectorButton[] heatColours = new ColourSelectorButton[6];
        for (int index = 0; index < heatLabels.length; index++) {
            heatLabels[index] = new JLabel(Messages.getString(
                    "CommonSettingsDialog.unitDisplayHeatMaximum", "Level " + (index + 1)));
            heatSpinners[index] = CommonSettingsDialog.createIntegerSpinner(index + 4, 0, 1);
            heatColours[index] = new ColourSelectorButton("Level " + (index + 1) + " colour");
        }
        JLabel overheatLabel = new JLabel(Messages.getString(
            "CommonSettingsDialog.unitDisplayHeatAboveMaximum"));
        ColourSelectorButton overheatColour = new ColourSelectorButton("Overheat colour");
        SettingsFormPanel heatGrid = CommonSettingsDialog.createUnitDisplayHeatGrid(
                  seenByLabel, seenBy, heatLabels, heatSpinners, heatColours, overheatLabel, overheatColour);

        DefaultListModel<String> order = unitDisplayOrderModel();
        JPanel orderGrid = CommonSettingsDialog.createUnitDisplayOrderGrid(order);

        JLabel sortLabel = new JLabel(Messages.getString("CommonSettingsDialog.defaultWeaponSortOrder"));
        JComboBox<String> sort = new JComboBox<>(new String[] { "Name", "Range" });
        JLabel heightLabel = new JLabel(Messages.getString("CommonSettingsDialog.weaponListHeight"));
        JSpinner height = CommonSettingsDialog.createIntegerSpinner(200, 1, 1);
        SettingsFormPanel weaponsGrid = CommonSettingsDialog.createGameBoardFieldGrid(
                  "TestUnitDisplayWeaponsGrid", sortLabel, sort, heightLabel, height);

        JLabel armorLargeLabel = new JLabel(Messages.getString(
            "CommonSettingsDialog.unitDisplayMekArmorLargeFontSize"));
        JSpinner armorLarge = CommonSettingsDialog.createIntegerSpinner(12, 1, 1);
        JLabel armorMediumLabel = new JLabel(Messages.getString(
            "CommonSettingsDialog.unitDisplayMekArmorMediumFontSize"));
        JSpinner armorMedium = CommonSettingsDialog.createIntegerSpinner(10, 1, 1);
        JLabel armorSmallLabel = new JLabel(Messages.getString(
            "CommonSettingsDialog.unitDisplayMekArmorSmallFontSize"));
        JSpinner armorSmall = CommonSettingsDialog.createIntegerSpinner(9, 1, 1);
        JLabel informationLargeLabel = new JLabel(Messages.getString(
            "CommonSettingsDialog.unitDisplayMekLargeFontSize"));
        JSpinner informationLarge = CommonSettingsDialog.createIntegerSpinner(12, 1, 1);
        JLabel informationMediumLabel = new JLabel(Messages.getString(
            "CommonSettingsDialog.unitDisplayMekMediumFontSize"));
        JSpinner informationMedium = CommonSettingsDialog.createIntegerSpinner(10, 1, 1);
        SettingsFormPanel fontsGrid = CommonSettingsDialog.createGameBoardFieldGrid(
            "TestUnitDisplayFontsGrid",
                  armorLargeLabel, armorLarge, armorMediumLabel, armorMedium, armorSmallLabel, armorSmall,
                  informationLargeLabel, informationLarge, informationMediumLabel, informationMedium);

        JPanel heatSection = settingsGroup(List.of(row(heatGrid)));
        JPanel orderSection = settingsGroup(List.of(row(orderGrid)));
        JPanel weaponsSection = settingsGroup(List.of(row(weaponsGrid)));
        JPanel fontsSection = settingsGroup(List.of(row(fontsGrid)));
        CommonSettingsPane pane = createSettingsPane("unitDisplay",
                  heatSection, orderSection, weaponsSection, fontsSection);

        assertSame(seenBy, seenByLabel.getLabelFor());
        for (int index = 0; index < heatLabels.length; index++) {
            assertSame(heatSpinners[index], heatLabels[index].getLabelFor());
            assertCell(heatGrid, heatGrid.getComponent(2 + (index * 2)), 0, index + 1);
            assertCell(heatGrid, heatColours[index], 1, index + 1);
        }
        assertSame(overheatColour, overheatLabel.getLabelFor());
        assertSame(sort, sortLabel.getLabelFor());
        assertSame(height, heightLabel.getLabelFor());
        assertSame(armorLarge, armorLargeLabel.getLabelFor());
        assertSame(informationMedium, informationMediumLabel.getLabelFor());

        assertAtConstrainedStandardAndWideWidths(pane,
                  List.of(heatGrid, orderGrid, weaponsGrid, fontsGrid), laidOutPane -> {
                List<List<Component>> heatRows = new ArrayList<>();
                heatRows.add(List.of(seenByLabel, seenBy));
                for (int index = 0; index < heatLabels.length; index++) {
                    heatRows.add(List.of(heatGrid.getComponent(2 + (index * 2)), heatColours[index]));
                }
                heatRows.add(List.of(overheatLabel, overheatColour));
                assertAlignedBalancedRows(heatGrid, heatRows);
                    assertAlignedBalancedRows(weaponsGrid, List.of(
                        List.of(sortLabel, sort), List.of(heightLabel, height)));
                    assertAlignedBalancedRows(fontsGrid, List.of(
                        List.of(armorLargeLabel, armorLarge), List.of(armorMediumLabel, armorMedium),
                        List.of(armorSmallLabel, armorSmall),
                        List.of(informationLargeLabel, informationLarge),
                        List.of(informationMediumLabel, informationMedium)));
                assertThreeColumnUnitDisplayOrderGrid(orderGrid);
                assertEquals(heatGrid.getWidth(), orderGrid.getWidth());
                assertEquals(orderGrid.getWidth(), weaponsGrid.getWidth());
                assertEquals(weaponsGrid.getWidth(), fontsGrid.getWidth());
            });
    }

    @Test
    void reordersNonTabbedUnitDisplayPanelsWithSharedDragListControl() {
        DefaultListModel<String> order = unitDisplayOrderModel();
        JPanel orderGrid = CommonSettingsDialog.createUnitDisplayOrderGrid(order);
        JList<?> orderList = findComponent(orderGrid, JList.class);

        CommonSettingsDialog.moveListElement(order, 0, 3);

        assertSame(order, orderList.getModel());
        assertEquals(UnitDisplayPanel.NON_TABBED_PILOT, order.get(0));
        assertEquals(UnitDisplayPanel.NON_TABBED_GENERAL, order.get(3));
        assertEquals(JList.HORIZONTAL_WRAP, orderList.getLayoutOrientation());
        assertEquals(2, orderList.getVisibleRowCount());
        assertEquals(ListSelectionModel.SINGLE_SELECTION, orderList.getSelectionMode());
        assertTrue(orderList.getMouseMotionListeners().length > 0);
        orderGrid.setSize(orderGrid.getPreferredSize());
        layoutRecursively(orderGrid);
        assertThreeColumnUnitDisplayOrderGrid(orderGrid);
    }

    @Test
    void localizesMovementTraitorCommand() {
        assertTrue(Messages.keyExists("MovementDisplay.Traitor"));
        assertEquals("Traitor", Messages.getString("MovementDisplay.Traitor"));
    }

    @Test
    void loadsUnitDisplayOrderWithoutRecursion() {
        DefaultListModel<String> order = new DefaultListModel<>();
        order.addElement("stale");
        List<String> savedOrder = List.of(
                                    UnitDisplayPanel.NON_TABBED_GENERAL, UnitDisplayPanel.NON_TABBED_WEAPON,
                                    UnitDisplayPanel.NON_TABBED_EXTRA, UnitDisplayPanel.NON_TABBED_PILOT,
                                    UnitDisplayPanel.NON_TABBED_SYSTEM, UnitDisplayPanel.NON_TABBED_ARMOR);

        CommonSettingsDialog.loadUnitDisplayOrder(order, savedOrder);

                            assertEquals(savedOrder, java.util.stream.IntStream.range(0, order.size())
                .mapToObj(order::get)
                .toList());
    }

    @Test
    void constrainsUnitDisplayNumericSpinners() {
        JSpinner heat = CommonSettingsDialog.createIntegerSpinner(-1, 0, 1);
        JSpinner weaponHeight = CommonSettingsDialog.createIntegerSpinner(0, 1, 1);
        JSpinner fontSize = CommonSettingsDialog.createIntegerSpinner(0, 1, 1);

        assertEquals(0, heat.getValue());
        assertEquals(0, ((javax.swing.SpinnerNumberModel) heat.getModel()).getMinimum());
        assertEquals(1, weaponHeight.getValue());
        assertEquals(1, ((javax.swing.SpinnerNumberModel) weaponHeight.getModel()).getMinimum());
        assertEquals(1, fontSize.getValue());
        assertEquals(1, ((javax.swing.SpinnerNumberModel) fontSize.getModel()).getMinimum());
    }

    @Test
    void laysOutReportSectionsOnBalancedTracksWithMultilinePresetEditors() {
        ColourSelectorButton link = new ColourSelectorButton("Report Link Color");
        ColourSelectorButton success = new ColourSelectorButton("Report Success Color");
        ColourSelectorButton miss = new ColourSelectorButton("Report Miss Color");
        ColourSelectorButton info = new ColourSelectorButton("Report Info Color");
        JLabel fontLabel = new JLabel(Messages.getString("CommonSettingsDialog.reportFontType"));
        JComboBox<String> font = new JComboBox<>(new String[] { "Noto Sans" });
        JCheckBox sprites = new JCheckBox(Messages.getString("CommonSettingsDialog.showReportSprites"));
        SettingsFormPanel appearanceGrid = CommonSettingsDialog.createReportAppearanceGrid(
                  link, success, miss, info, fontLabel, font, sprites);

        JCheckBox players = new JCheckBox(Messages.getString("CommonSettingsDialog.showReportPlayerList"));
        JCheckBox units = new JCheckBox(Messages.getString("CommonSettingsDialog.showReportUnitList"));
        JCheckBox search = new JCheckBox(Messages.getString("CommonSettingsDialog.showReportKeywordsList"));
        JLabel searchLabel = new JLabel(Messages.getString("CommonSettingsDialog.reportKeywords"));
        String searchHelp = Messages.getString("CommonSettingsDialog.reportKeywords.tooltip");
        JTextArea searchEditor = new JTextArea(6, 20);
        searchEditor.setText("Needs\nRolls\nDamage");
        JScrollPane searchControl = CommonSettingsDialog.createReportKeywordEditor(searchEditor, searchHelp);
        SettingsFormPanel searchGrid = CommonSettingsDialog.createReportSearchGrid(
                  players, units, search, searchLabel, searchEditor, searchControl);

        JCheckBox filter = new JCheckBox(Messages.getString("CommonSettingsDialog.showReportFilterList"));
        JLabel filterLabel = new JLabel(Messages.getString("CommonSettingsDialog.reportFilterKeywords"));
        String filterHelp = Messages.getString("CommonSettingsDialog.reportFilterKeywords.tooltip");
        JTextArea filterEditor = new JTextArea(4, 20);
        filterEditor.setText("Fire Hit Damage\nHit Damage");
        JScrollPane filterControl = CommonSettingsDialog.createReportKeywordEditor(filterEditor, filterHelp);
        SettingsFormPanel filterGrid = CommonSettingsDialog.createReportFilterGrid(
                  filter, filterLabel, filterEditor, filterControl);

        assertEquals(searchHelp, searchControl.getToolTipText());
        assertEquals(filterHelp, filterControl.getToolTipText());

        JPanel appearanceSection = settingsGroup(List.of(row(appearanceGrid)));
        JPanel searchSection = settingsGroup(List.of(row(searchGrid)));
        JPanel filterSection = settingsGroup(List.of(row(filterGrid)));
        CommonSettingsPane pane = createSettingsPane(
                  "report", appearanceSection, searchSection, filterSection);
        Component appearanceFiller = appearanceGrid.getComponent(7);
        Component searchFiller = searchGrid.getComponent(3);
        Component filterFiller = filterGrid.getComponent(1);

        assertSame(font, fontLabel.getLabelFor());
        assertSame(searchEditor, searchLabel.getLabelFor());
        assertSame(filterEditor, filterLabel.getLabelFor());
        assertFalse(searchEditor.getLineWrap());
        assertFalse(filterEditor.getLineWrap());
        assertEquals(6, searchEditor.getRows());
        assertEquals(4, filterEditor.getRows());
        assertEquals("Needs\nRolls\nDamage", searchEditor.getText());
        assertEquals("Fire Hit Damage\nHit Damage", filterEditor.getText());

        assertAtConstrainedStandardAndWideWidths(pane,
                  List.of(appearanceGrid, searchGrid, filterGrid), laidOutPane -> {
                    assertAlignedBalancedRows(appearanceGrid, List.of(
                        List.of(link, success), List.of(miss, info), List.of(fontLabel, font),
                        List.of(sprites, appearanceFiller)));
                    assertAlignedBalancedRows(searchGrid, List.of(
                        List.of(players, units), List.of(search, searchFiller),
                        List.of(searchLabel, searchControl)));
                    assertAlignedBalancedRows(filterGrid, List.of(
                        List.of(filter, filterFiller), List.of(filterLabel, filterControl)));
                assertEquals(appearanceGrid.getWidth(), searchGrid.getWidth());
                assertEquals(searchGrid.getWidth(), filterGrid.getWidth());
            });
    }

    @Test
    void laysOutOverlaySectionsOnBalancedTracksWithoutSliderLabels() {
        ColourSelectorButton unitShadow = new ColourSelectorButton("Unit name shadow color");
        ColourSelectorButton conditionShadow = new ColourSelectorButton("Condition shadow color");
        SettingsFormPanel overviewGrid = CommonSettingsDialog.createOverlayOverviewGrid(
                  unitShadow, conditionShadow);

        ColourSelectorButton title = new ColourSelectorButton("Title color");
        ColourSelectorButton text = new ColourSelectorButton("Text color");
        ColourSelectorButton background = new ColourSelectorButton("Background color");
        ColourSelectorButton cold = new ColourSelectorButton("Cold indicator color");
        ColourSelectorButton hot = new ColourSelectorButton("Hot indicator color");
        JCheckBox defaults = new JCheckBox("Show default conditions");
        JCheckBox header = new JCheckBox("Show header");
        JCheckBox labels = new JCheckBox("Show labels");
        JCheckBox values = new JCheckBox("Show values");
        JCheckBox indicators = new JCheckBox("Show indicators");
        JLabel backgroundOpacityLabel = new JLabel("Background opacity");
        JSlider backgroundOpacity = new JSlider(0, 255, 128);
        JSpinner backgroundPercentage = new JSpinner(
            new javax.swing.SpinnerNumberModel(0, 0, 100, 1));
        JPanel backgroundOpacityControl = CommonSettingsDialog.createGameBoardFovOpacityControl(
                  backgroundOpacity, backgroundPercentage);
        SettingsFormPanel planetaryGrid = CommonSettingsDialog.createOverlayPlanetaryGrid(
                  title, text, background, cold, hot, defaults, header, labels, values, indicators,
                  backgroundOpacityLabel, backgroundOpacity, backgroundOpacityControl);

        JCheckBox toasts = new JCheckBox("Show board toast notifications");
        JCheckBox reportEvents = new JCheckBox("Show round report events as toasts");
        JLabel durationLabel = new JLabel("Toast display time (seconds)");
        JSpinner duration = new JSpinner(new javax.swing.SpinnerNumberModel(3, 1, 10, 1));
        JLabel gapLabel = new JLabel("Gap between report toasts (seconds)");
        JSpinner gap = new JSpinner(new javax.swing.SpinnerNumberModel(2, 1, 10, 1));
        SettingsFormPanel toastGrid = CommonSettingsDialog.createOverlayToastGrid(
                  toasts, reportEvents, durationLabel, duration, gapLabel, gap);

        JLabel opacityLabel = new JLabel("Opacity");
        JSlider opacity = new JSlider(0, 255, 64);
        JSpinner opacityPercentage = new JSpinner(
            new javax.swing.SpinnerNumberModel(0, 0, 100, 1));
        JPanel opacityControl = CommonSettingsDialog.createGameBoardFovOpacityControl(
                  opacity, opacityPercentage);
        JLabel scaleLabel = new JLabel("Scale");
        JSlider scale = new JSlider(30, 150, 100);
        JSpinner scalePercentage = new JSpinner(
            new javax.swing.SpinnerNumberModel(100, 30, 150, 1));
        JPanel scaleControl = CommonSettingsDialog.createOverlayValueControl(
                  scale, scalePercentage, "%");
        JLabel horizontalLabel = new JLabel("Horizontal position");
        JSlider horizontal = new JSlider(-1000, 2000, -250);
        JSpinner horizontalPixels = new JSpinner(
            new javax.swing.SpinnerNumberModel(-250, -1000, 2000, 10));
        JPanel horizontalControl = CommonSettingsDialog.createOverlayValueControl(
                  horizontal, horizontalPixels, "px");
        JLabel verticalLabel = new JLabel("Vertical position");
        JSlider vertical = new JSlider(-1000, 2000, 500);
        JSpinner verticalPixels = new JSpinner(
            new javax.swing.SpinnerNumberModel(500, -1000, 2000, 10));
        JPanel verticalControl = CommonSettingsDialog.createOverlayValueControl(
                  vertical, verticalPixels, "px");
        JLabel imageLabel = new JLabel("Image file");
        JTextField image = new JTextField("trace.png", 20);
        JPanel imageControl = CommonSettingsDialog.applicationPathControl(image, new JButton());
        SettingsFormPanel traceGrid = CommonSettingsDialog.createOverlayTraceGrid(
                  opacityLabel, opacity, opacityControl, scaleLabel, scale, scaleControl,
                  horizontalLabel, horizontal, horizontalControl,
                  verticalLabel, vertical, verticalControl, imageLabel, image, imageControl);

        JPanel overviewSection = settingsGroup(List.of(row(overviewGrid)));
        JPanel planetarySection = settingsGroup(List.of(row(planetaryGrid)));
        JPanel toastSection = settingsGroup(List.of(row(toastGrid)));
        JPanel traceSection = settingsGroup(List.of(row(traceGrid)));
        CommonSettingsPane pane = createSettingsPane(
                  "overlays", overviewSection, planetarySection, toastSection, traceSection);
        Component planetaryColorFiller = planetaryGrid.getComponent(5);
        Component planetaryOptionFiller = planetaryGrid.getComponent(11);
        JPanel opacityField = (JPanel) traceGrid.getComponent(0);
        JPanel scaleField = (JPanel) traceGrid.getComponent(1);
        JPanel horizontalField = (JPanel) traceGrid.getComponent(2);
        JPanel verticalField = (JPanel) traceGrid.getComponent(3);

        assertSame(backgroundOpacity, backgroundOpacityLabel.getLabelFor());
        assertSame(duration, durationLabel.getLabelFor());
        assertSame(gap, gapLabel.getLabelFor());
        assertSame(opacity, opacityLabel.getLabelFor());
        assertSame(scale, scaleLabel.getLabelFor());
        assertSame(horizontal, horizontalLabel.getLabelFor());
        assertSame(vertical, verticalLabel.getLabelFor());
        assertSame(image, imageLabel.getLabelFor());
        assertEquals(50, backgroundPercentage.getValue());
        assertEquals(25, opacityPercentage.getValue());
        int sliderValueWidth = backgroundPercentage.getPreferredSize().width;
        for (JSpinner spinner : List.of(
                                    opacityPercentage, scalePercentage, horizontalPixels, verticalPixels)) {
            assertEquals(sliderValueWidth, spinner.getPreferredSize().width);
        }
        for (JSlider slider : List.of(backgroundOpacity, opacity, scale, horizontal, vertical)) {
            assertFalse(slider.getPaintTicks());
            assertFalse(slider.getPaintLabels());
        }

        assertAtConstrainedStandardAndWideWidths(pane,
                  List.of(overviewSection, planetarySection, toastSection, traceSection), laidOutPane -> {
                    assertAlignedBalancedRows(overviewGrid, List.of(
                        List.of(unitShadow, conditionShadow)));
                    assertAlignedBalancedRows(planetaryGrid, List.of(
                        List.of(title, text), List.of(background, cold), List.of(hot, planetaryColorFiller),
                        List.of(defaults, header), List.of(labels, values),
                        List.of(indicators, planetaryOptionFiller),
                        List.of(backgroundOpacityLabel, backgroundOpacityControl)));
                    assertAlignedBalancedRows(toastGrid, List.of(
                        List.of(toasts, reportEvents), List.of(durationLabel, duration),
                        List.of(gapLabel, gap)));
                    assertAlignedBalancedRows(traceGrid, List.of(
                        List.of(opacityField, scaleField), List.of(horizontalField, verticalField),
                        List.of(imageLabel, imageControl)));
                assertEquals(overviewGrid.getWidth(), planetaryGrid.getWidth());
                assertEquals(planetaryGrid.getWidth(), toastGrid.getWidth());
                assertEquals(toastGrid.getWidth(), traceGrid.getWidth());
                assertTrue(scale.getWidth() >= UIUtil.scaleForGUI(80));
                assertTrue(horizontal.getWidth() >= UIUtil.scaleForGUI(80));
                for (JSpinner spinner : List.of(
                                                opacityPercentage, scalePercentage, horizontalPixels, verticalPixels)) {
                    assertEquals(backgroundPercentage.getWidth(), spinner.getWidth());
                }
            });
    }

    @Test
    void synchronizesOverlayScaleAndOriginValues() {
        JSlider scale = new JSlider(30, 150, 100);
        JSpinner scalePercentage = new JSpinner(
            new javax.swing.SpinnerNumberModel(100, 30, 150, 1));
        CommonSettingsDialog.createOverlayValueControl(scale, scalePercentage, "%");

        scalePercentage.setValue(125);
        assertEquals(125, scale.getValue());
        scale.setValue(75);
        assertEquals(75, scalePercentage.getValue());

        JSlider origin = new JSlider(-1000, 2000, 0);
        JSpinner pixels = new JSpinner(
            new javax.swing.SpinnerNumberModel(0, -1000, 2000, 10));
        CommonSettingsDialog.createOverlayValueControl(origin, pixels, "px");

        pixels.setValue(-400);
        assertEquals(-400, origin.getValue());
        origin.setValue(1200);
        assertEquals(1200, pixels.getValue());
    }

    @Test
    void laysOutAutoDisplayAsBalancedPhaseRowsAndTabCheckboxes() {
        List<SettingsFormPanel> phaseGrids = new ArrayList<>();
        List<List<Component>> phaseControls = new ArrayList<>();
        List<JPanel> sections = new ArrayList<>();
        for (String name : List.of(
                                    "UnitDisplay", "MiniMap", "MiniReport", "PlayerList", "ForceDisplay", "BotCommands")) {
            JLabel reportLabel = new JLabel(Messages.getString("CommonSettingsDialog.reportPhases"));
            JComboBox<String> reportControl = new JComboBox<>(new String[] { "Hide", "Show", "Manual" });
            JLabel nonReportLabel = new JLabel(Messages.getString("CommonSettingsDialog.nonReportPhases"));
            JComboBox<String> nonReportControl = new JComboBox<>(new String[] { "Hide", "Show", "Manual" });
            SettingsFormPanel grid = CommonSettingsDialog.createAutoDisplayPhaseGrid(
                                        name, reportLabel, reportControl, nonReportLabel, nonReportControl);

            assertSame(reportControl, reportLabel.getLabelFor());
            assertSame(nonReportControl, nonReportLabel.getLabelFor());
            assertEquals(4, grid.getComponentCount());
            phaseGrids.add(grid);
            phaseControls.add(List.of(reportLabel, reportControl, nonReportLabel, nonReportControl));
            sections.add(settingsGroup(List.of(row(grid))));
        }

        JCheckBox movement = new JCheckBox(Messages.getString("CommonSettingsDialog.tabsMove"));
        JCheckBox firing = new JCheckBox(Messages.getString("CommonSettingsDialog.tabsFire"));
        SettingsFormPanel tabGrid = CommonSettingsDialog.createAutoDisplayTabGrid(movement, firing);
        JPanel tabSection = settingsGroup(List.of(row(tabGrid)));
        sections.add(tabSection);
        CommonSettingsPane pane = createSettingsPane(
                                    "autoDisplay", sections.toArray(JComponent[]::new));

        assertEquals(2, tabGrid.getComponentCount());
        assertTrue(movement.getText().startsWith("Show "));
        assertTrue(firing.getText().startsWith("Show "));
        assertAtConstrainedStandardAndWideWidths(pane, sections, laidOutPane -> {
            int expectedWidth = phaseGrids.getFirst().getWidth();
            for (int index = 0; index < phaseGrids.size(); index++) {
                SettingsFormPanel grid = phaseGrids.get(index);
                List<Component> controls = phaseControls.get(index);
                                    assertAlignedBalancedRows(grid, List.of(
                                            controls.subList(0, 2), controls.subList(2, 4)));
                assertEquals(expectedWidth, grid.getWidth());
            }
            assertAlignedBalancedRows(tabGrid, List.of(List.of(movement, firing)));
            assertEquals(expectedWidth, tabGrid.getWidth());
        });
    }

    @Test
    void laysOutAiSettingsAsOneBalancedGridWithoutRedundantLabels() {
        JCheckBox autoResolve = new JCheckBox(
            Messages.getString("CommonSettingsDialog.showAutoResolvePanel"));
        JCheckBox experimental = new JCheckBox(
            Messages.getString("CommonSettingsDialog.enableExperimentalBotFeatures"));
        JLabel favoriteLabel = new JLabel(
            Messages.getString("CommonSettingsDialog.favoritePrincessBehaviorSetting"));
        JComboBox<String> favoriteControl = new JComboBox<>(new String[] { "DEFAULT", "AGGRESSIVE" });
        SettingsFormPanel grid = CommonSettingsDialog.createAiDisplayGrid(
                  autoResolve, experimental, favoriteLabel, favoriteControl);
        JPanel section = settingsGroup(List.of(row(grid)));
        CommonSettingsPane pane = createSettingsPane("aiDisplay", section);

        assertEquals(4, grid.getComponentCount());
        assertSame(favoriteControl, favoriteLabel.getLabelFor());
        assertTrue(autoResolve.getText().startsWith("Show "));
        assertTrue(experimental.getText().startsWith("Enable "));
              assertAtConstrainedStandardAndWideWidths(pane, List.of(section), laidOutPane ->
                  assertAlignedBalancedRows(grid, List.of(
                      List.of(autoResolve, experimental), List.of(favoriteLabel, favoriteControl))));
    }

    @Test
    void laysOutAdvancedOptionsAsThreeSemanticBalancedSections() {
        ColourSelectorButton backgroundColor = new ColourSelectorButton("Background color");
        backgroundColor.setColour(Color.WHITE);
        int initializedColorButtonHeight = backgroundColor.getPreferredSize().height;
        JCheckBox autoSlide = new JCheckBox("Hide chat when inactive");
        JLabel opacityLabel = new JLabel("Background opacity");
        JSlider opacity = new JSlider(0, 255, 128);
        JSpinner opacityPercentage = new JSpinner(
            new javax.swing.SpinnerNumberModel(0, 0, 100, 1));
        JPanel opacityControl = CommonSettingsDialog.createGameBoardFovOpacityControl(
              opacity, opacityPercentage);
        SettingsFormPanel chatGrid = CommonSettingsDialog.createAdvancedChatGrid(
              backgroundColor, autoSlide, opacityLabel, opacity, opacityControl);

        JLabel repeatDelayLabel = new JLabel("Repeat delay (milliseconds)");
        JSpinner repeatDelay = CommonSettingsDialog.createBoundedIntegerSpinner(0, 0, Integer.MAX_VALUE, 10);
        JLabel repeatRateLabel = new JLabel("Repeat rate (per second)");
        JSpinner repeatRate = CommonSettingsDialog.createBoundedIntegerSpinner(20, 0, 100, 1);
        JLabel moveDelayLabel = new JLabel("Movement step delay (milliseconds)");
        JSpinner moveDelay = CommonSettingsDialog.createBoundedIntegerSpinner(50, 0, Integer.MAX_VALUE, 10);
        SettingsFormPanel timingGrid = CommonSettingsDialog.createAdvancedTimingGrid(
              repeatDelayLabel, repeatDelay, repeatRateLabel, repeatRate, moveDelayLabel, moveDelay);

        JCheckBox drawTime = new JCheckBox("Show board draw time");
        JCheckBox skipSavePrompt = new JCheckBox("Skip save reminder when leaving");
        JCheckBox saveLobby = new JCheckBox("Save lobby backup when game starts");
        SettingsFormPanel safetyGrid = CommonSettingsDialog.createAdvancedSafetyGrid(
              drawTime, skipSavePrompt, saveLobby);
        Component safetyFiller = safetyGrid.getComponent(3);

        JPanel chatSection = settingsGroup(List.of(row(chatGrid)));
        JPanel timingSection = settingsGroup(List.of(row(timingGrid)));
        JPanel safetySection = settingsGroup(List.of(row(safetyGrid)));
        CommonSettingsPane pane = createSettingsPane(
              "advanced", chatSection, timingSection, safetySection);

        assertSame(opacity, opacityLabel.getLabelFor());
        assertSame(repeatDelay, repeatDelayLabel.getLabelFor());
        assertSame(repeatRate, repeatRateLabel.getLabelFor());
        assertSame(moveDelay, moveDelayLabel.getLabelFor());
        assertEquals(initializedColorButtonHeight, backgroundColor.getPreferredSize().height);
        assertTrue(backgroundColor.getPreferredSize().height >= backgroundColor.getIcon().getIconHeight());
        assertEquals(50, opacityPercentage.getValue());
        assertFalse(opacity.getPaintTicks());
        assertFalse(opacity.getPaintLabels());
        assertAtConstrainedStandardAndWideWidths(pane,
              List.of(chatSection, timingSection, safetySection), laidOutPane -> {
                assertAlignedBalancedRows(chatGrid, List.of(
                    List.of(backgroundColor, autoSlide), List.of(opacityLabel, opacityControl)));
                assertAlignedBalancedRows(timingGrid, List.of(
                    List.of(repeatDelayLabel, repeatDelay), List.of(repeatRateLabel, repeatRate),
                        List.of(moveDelayLabel, moveDelay)));
                assertAlignedBalancedRows(safetyGrid, List.of(
                    List.of(drawTime, skipSavePrompt), List.of(saveLobby, safetyFiller)));
                assertEquals(chatGrid.getWidth(), timingGrid.getWidth());
                assertEquals(timingGrid.getWidth(), safetyGrid.getWidth());
            });
    }

    @Test
    void constrainsAdvancedNumericControlsToRuntimeRanges() {
        JSpinner repeatDelay = CommonSettingsDialog.createBoundedIntegerSpinner(-10, 0, Integer.MAX_VALUE, 10);
        JSpinner repeatRate = CommonSettingsDialog.createBoundedIntegerSpinner(150, 0, 100, 1);
          javax.swing.SpinnerNumberModel delayModel =
              (javax.swing.SpinnerNumberModel) repeatDelay.getModel();
          javax.swing.SpinnerNumberModel rateModel =
              (javax.swing.SpinnerNumberModel) repeatRate.getModel();

        assertEquals(0, repeatDelay.getValue());
        assertEquals(100, repeatRate.getValue());
        assertEquals(0, delayModel.getMinimum());
        assertEquals(10, delayModel.getStepSize());
        assertEquals(0, rateModel.getMinimum());
        assertEquals(100, rateModel.getMaximum());
        assertEquals(1, rateModel.getStepSize());
    }

    @Test
    void providesNamesAndOptionDetailsForEveryAdvancedControl() {
        for (String keyBase : List.of(
            "AdvancedOptions.Chatbox2BackColor",
            "AdvancedOptions.Chatbox2Transparency",
            "AdvancedOptions.Chatbox2AutoSlideDown",
            "AdvancedOptions.KeyRepeatDelay",
            "AdvancedOptions.KeyRepeatRate",
            "AdvancedOptions.MoveStepDelay",
            "AdvancedOptions.ShowFPS",
            "AdvancedOptions.NoSaveNag",
            "AdvancedOptions.SaveLobbyOnStart")) {
            assertTrue(Messages.keyExists(keyBase + ".name"), keyBase);
            assertTrue(Messages.keyExists(keyBase + ".tooltip"), keyBase);
            assertFalse(Messages.getString(keyBase + ".name").isBlank(), keyBase);
            assertFalse(Messages.getString(keyBase + ".tooltip").isBlank(), keyBase);
        }
    }

    @Test
    void alignsButtonOrderPhasesAsDirectFourColumnRowMajorGrids() {
        DefaultListModel<String> firstPhase = new DefaultListModel<>();
        for (int index = 0; index < 10; index++) {
            firstPhase.addElement("Command " + index);
        }
        DefaultListModel<String> secondPhase = new DefaultListModel<>();
        for (int index = 0; index < 8; index++) {
            secondPhase.addElement(index == 7 ? "Longest command across phases" : "Action " + index);
        }
        JList<String> firstList = new JList<>(firstPhase);
        JList<String> secondList = new JList<>(secondPhase);

        JPanel firstPanel = CommonSettingsDialog.createButtonOrderListPanel(firstList);
        JPanel secondPanel = CommonSettingsDialog.createButtonOrderListPanel(secondList);
        CommonSettingsDialog.setUniformButtonOrderCellSize(List.of(firstList, secondList));
        for (JPanel panel : List.of(firstPanel, secondPanel)) {
            Dimension listSize = panel.getComponent(0).getPreferredSize();
            panel.setSize(listSize.width + UIUtil.scaleForGUI(200), listSize.height);
            layoutRecursively(panel);
        }

        assertEquals(JList.HORIZONTAL_WRAP, firstList.getLayoutOrientation());
        assertEquals(3, firstList.getVisibleRowCount());
        assertEquals(2, secondList.getVisibleRowCount());
        assertEquals(firstList.getFixedCellWidth(), secondList.getFixedCellWidth());
        assertEquals(firstList.getFixedCellHeight(), secondList.getFixedCellHeight());
        assertTrue(firstList.getFixedCellWidth() > 0);
        assertTrue(firstList.getFixedCellHeight() > 0);
        assertSame(firstList, firstPanel.getComponent(0));
        assertSame(secondList, secondPanel.getComponent(0));
        assertEquals(firstPanel.getInsets().left, firstList.getX());
        assertEquals(secondPanel.getInsets().left, secondList.getX());
        assertTrue(firstList.getWidth() < firstPanel.getWidth());
        assertTrue(secondList.getWidth() < secondPanel.getWidth());

        var firstCell = firstList.getCellBounds(0, 0);
        var fourthCell = firstList.getCellBounds(3, 3);
        var fifthCell = firstList.getCellBounds(4, 4);
        assertEquals(firstCell.y, fourthCell.y);
        assertEquals(firstCell.x, fifthCell.x);
        assertTrue(fifthCell.y > firstCell.y);
        for (int column = 0; column < 4; column++) {
            assertEquals(firstList.getCellBounds(column, column).x,
                secondList.getCellBounds(column, column).x);
        }
        for (int index = 0; index < firstPhase.getSize(); index++) {
            assertEquals(firstList.getFixedCellWidth(), firstList.getCellBounds(index, index).width);
        }
        for (int index = 0; index < secondPhase.getSize(); index++) {
            assertEquals(secondList.getFixedCellWidth(), secondList.getCellBounds(index, index).width);
        }
    }

    @Test
    void presentsKeyBindingsAsOneWorkflowWithContextualNavigationHelp() {
        SettingsCheckBox navigation = CommonSettingsDialog.createKeyBindTabNavigationControl();
        SettingsButton reset = CommonSettingsDialog.createKeyBindResetButton();
        JLabel commandHeader = new JLabel("Command");
        JLabel modifierHeader = new JLabel("Modifier");
        JLabel keyHeader = new JLabel("Key");
        JLabel command = new JLabel("Activate Chatbox Command", SwingConstants.RIGHT);
        JTextField modifier = new JTextField(10);
        JTextField key = new JTextField(10);
        JPanel bindingsGrid = CommonSettingsDialog.createKeyBindGrid(
              commandHeader, modifierHeader, keyHeader);
        CommonSettingsDialog.addKeyBindGridRow(bindingsGrid, 2, command, modifier, key);
        JPanel content = CommonSettingsDialog.createKeyBindSectionContent(navigation, reset, bindingsGrid);
        List<CommonSettingsPane.OptionSection> sections = CommonSettingsDialog.createKeyBindSections(content);

        assertEquals(1, sections.size());
        assertEquals("keyBinds.commands", sections.getFirst().id());
        assertSame(content, sections.getFirst().content());
        BorderLayout contentLayout = (BorderLayout) content.getLayout();
        assertSame(bindingsGrid, contentLayout.getLayoutComponent(BorderLayout.CENTER));

        JPanel actions = (JPanel) contentLayout.getLayoutComponent(BorderLayout.NORTH);
        assertCell(actions, navigation, 0, 0);
        assertCell(actions, reset, 1, 0);
        assertEquals(Messages.getString("CommonSettingsDialog.keyBinds.tabNavigation.tooltip"),
            navigation.getSettingsHelpText());
        assertEquals(Messages.getString("CommonSettingsDialog.keyBinds.buttonDefault.tooltip"),
            reset.getSettingsHelpText());
        assertTrue(navigation.getText().contains(SettingsBadge.formatHtml(
                List.of(CommonSettingsPane.legendEntries().getFirst()))));

        CommonSettingsPane pane = new CommonSettingsPane(List.of(new CommonSettingsPane.OptionPage(
              "keyBinds", List.of("Key Binds"), "keyBinds", sections)));
        assertTrue(findComponent(pane, SettingsPagePanel.class).shouldShowDetailsPanel());

        content.setSize(UIUtil.scaleForGUI(SettingsPagePanel.DEFAULT_SECTION_STACK_WIDTH),
            content.getPreferredSize().height);
        layoutRecursively(content);

        assertEquals(navigation.getWidth(), reset.getWidth());
        assertEquals(command.getPreferredSize().width, command.getWidth());
        assertEquals(modifier.getPreferredSize().width, modifier.getWidth());
        assertEquals(key.getPreferredSize().width, key.getWidth());
        assertTrue(command.getX() > 0);
        assertTrue(key.getX() + key.getWidth() < bindingsGrid.getWidth());
    }

    @Test
    void enablingTabNavigationRemovesTabBinding() {
        JTextField modifier = new JTextField("Ctrl");
        JTextField key = new JTextField("Tab");

        int keyCode = CommonSettingsDialog.configureKeyBindFieldsForTabNavigation(
              true, java.awt.event.KeyEvent.VK_TAB, modifier, key);

        assertEquals(0, keyCode);
        assertEquals("", modifier.getText());
        assertEquals("", key.getText());
        assertTrue(modifier.getFocusTraversalKeysEnabled());
        assertTrue(key.getFocusTraversalKeysEnabled());
    }

    private static void assertAudioSectionsHaveAlignedControlColumns() {
        JLabel volumeLabel = new JLabel(Messages.getString("CommonSettingsDialog.masterVolume"));
        JSlider volumeSlider = new JSlider(0, 100);
        volumeSlider.setToolTipText("Master volume help");
        JCheckBox chatMute = new JCheckBox(Messages.getString("CommonSettingsDialog.soundMuteChat"));
        JTextField chatSoundFile = new JTextField(5);
        JCheckBox myTurnMute = new JCheckBox(Messages.getString("CommonSettingsDialog.soundMuteMyTurn"));
        JTextField myTurnSoundFile = new JTextField(5);
        JCheckBox otherTurnsMute = new JCheckBox(
            Messages.getString("CommonSettingsDialog.soundMuteOthersTurn"));
        JTextField otherTurnsSoundFile = new JTextField(5);
        int otherTurnsMuteWidth = otherTurnsMute.getPreferredSize().width;

        CommonSettingsPane.SectionedContent content = CommonSettingsDialog.createAudioSettingsPanel(
              volumeLabel, volumeSlider,
              chatMute, chatSoundFile,
              myTurnMute, myTurnSoundFile,
              otherTurnsMute, otherTurnsSoundFile);

        assertEquals(2, content.getComponentCount());
        JPanel volumeSection = (JPanel) content.getComponent(0);
        JPanel notificationsSection = (JPanel) content.getComponent(1);
        CommonSettingsPane pane = new CommonSettingsPane(List.of(new CommonSettingsPane.OptionPage(
              "audio", List.of("Audio"), "audio", List.of(
                new CommonSettingsPane.OptionSection("audio.volume",
                    Messages.getString("CommonSettingsDialog.section.audio.volume.title"),
                    Messages.getString("CommonSettingsDialog.section.audio.volume.summary"),
                          volumeSection, false),
                new CommonSettingsPane.OptionSection("audio.notifications",
                    Messages.getString("CommonSettingsDialog.section.audio.notifications.title"),
                    Messages.getString("CommonSettingsDialog.section.audio.notifications.summary"),
                          notificationsSection, false)))));

        JPanel volumeGrid = findNamedPanel(volumeSection, "pnlCommonSettingsAudioVolumeGrid");
        assertSame(volumeSection, volumeGrid);
        assertEquals(2, volumeGrid.getComponentCount());
        assertCell(volumeGrid, volumeLabel, 0, 0);
        JPanel volumeControl = (JPanel) volumeGrid.getComponent(1);
        assertCell(volumeGrid, volumeControl, 1, 0);
        assertSame(volumeSlider, volumeLabel.getLabelFor());
        assertSame(volumeControl, volumeSlider.getParent());
        JSpinner volumeSpinner = findComponent(volumeControl, JSpinner.class);
        assertEquals("masterVolumeSpinner", volumeSpinner.getName());
        assertFalse(volumeSlider.getPaintTicks());
        assertFalse(volumeSlider.getPaintLabels());
        volumeSlider.setValue(73);
        assertEquals(73, volumeSpinner.getValue());
        volumeSpinner.setValue(42);
        assertEquals(42, volumeSlider.getValue());

        JPanel notificationGrid = findNamedPanel(notificationsSection,
            "pnlCommonSettingsAudioNotificationGrid");
        assertSame(notificationsSection, notificationGrid);
        assertEquals(6, notificationGrid.getComponentCount());
        JPanel chatSoundControl = (JPanel) notificationGrid.getComponent(1);
        JPanel myTurnSoundControl = (JPanel) notificationGrid.getComponent(3);
        JPanel otherTurnsSoundControl = (JPanel) notificationGrid.getComponent(5);
        assertCell(notificationGrid, chatMute, 0, 0);
        assertCell(notificationGrid, chatSoundControl, 1, 0);
        assertCell(notificationGrid, myTurnMute, 0, 1);
        assertCell(notificationGrid, myTurnSoundControl, 1, 1);
        assertCell(notificationGrid, otherTurnsMute, 0, 2);
        assertCell(notificationGrid, otherTurnsSoundControl, 1, 2);
          assertAudioFileControl(chatSoundControl, chatSoundFile, "btnChatSoundChooser",
            "CommonSettingsDialog.soundMuteChat.chooser.title");
          assertAudioFileControl(myTurnSoundControl, myTurnSoundFile, "btnMyTurnSoundChooser",
            "CommonSettingsDialog.soundMuteMyTurn.chooser.title");
          assertAudioFileControl(otherTurnsSoundControl, otherTurnsSoundFile, "btnOtherTurnsSoundChooser",
            "CommonSettingsDialog.soundMuteOthersTurn.chooser.title");

        assertAtConstrainedStandardAndWideWidths(pane, List.of(volumeGrid, notificationGrid), laidOutPane -> {
            assertEquals(xRelativeTo(volumeGrid, laidOutPane), xRelativeTo(notificationGrid, laidOutPane));
            assertEquals(volumeGrid.getWidth(), notificationGrid.getWidth());
            assertAlignedBalancedRows(laidOutPane, List.of(
                    List.of(volumeLabel, volumeControl),
                    List.of(chatMute, chatSoundControl),
                    List.of(myTurnMute, myTurnSoundControl),
                    List.of(otherTurnsMute, otherTurnsSoundControl)));
            GridBagConstraints volumeLabelConstraints =
                  ((GridBagLayout) volumeGrid.getLayout()).getConstraints(volumeLabel);
            int columnGap = xRelativeTo(volumeControl, laidOutPane)
                  - xRelativeTo(volumeLabel, laidOutPane) - volumeLabel.getWidth();
            assertEquals(volumeLabelConstraints.insets.right, columnGap);
            assertEquals(xRelativeTo(volumeGrid, laidOutPane) + volumeGrid.getWidth(),
                xRelativeTo(volumeControl, laidOutPane) + volumeControl.getWidth());
            assertTrue(otherTurnsMute.getWidth() >= otherTurnsMuteWidth);
        });

        assertEquals("Notifications",
            Messages.getString("CommonSettingsDialog.section.audio.notifications.title"));
        assertEquals("Configure notification muting and sound files.",
            Messages.getString("CommonSettingsDialog.section.audio.notifications.summary"));
    }

    private static void assertAudioFileControl(JPanel control, JTextField field, String buttonName,
        String chooserTitleKey) {
        assertSame(control, field.getParent());
        JButton chooser = findComponent(control, JButton.class);
        String chooserTitle = Messages.getString(chooserTitleKey);
        assertEquals(buttonName, chooser.getName());
        assertEquals(chooserTitle, chooser.getToolTipText());
        assertEquals(chooserTitle, chooser.getAccessibleContext().getAccessibleName());
        assertTrue(chooser.getIcon() != null);
        assertEquals(field.getPreferredSize().height, chooser.getPreferredSize().width);
        assertEquals(field.getPreferredSize().height, chooser.getPreferredSize().height);
        assertEquals(1, chooser.getActionListeners().length);
    }

    @Test
    void rendersUnitDefaultsAsCompleteOptionsOnSharedTwoColumnTracks() {
        JLabel protoMekLabel = new JLabel("ProtoMek unit codes");
        JTextField protoMekControl = new JTextField("A, B, C, D...");
        JCheckBox autoEject = new JCheckBox("Disable automatic ejection");
        JCheckBox randomSkills = new JCheckBox("Use current random skills");
        JCheckBox randomNames = new JCheckBox("Generate random pilot names");

        JPanel grid = CommonSettingsDialog.createUnitDefaultsGrid(protoMekLabel, protoMekControl,
              autoEject, randomSkills, randomNames);
        Component finalCellFiller = grid.getComponent(5);

        assertCell(grid, protoMekLabel, 0, 0);
        assertCell(grid, protoMekControl, 1, 0);
        assertCell(grid, autoEject, 0, 1);
        assertCell(grid, randomSkills, 1, 1);
        assertCell(grid, randomNames, 0, 2);
        assertCell(grid, finalCellFiller, 1, 2);
        assertSame(protoMekControl, protoMekLabel.getLabelFor());
        assertEquals(protoMekLabel.getPreferredSize().width, protoMekControl.getPreferredSize().width);
        assertEquals(protoMekControl.getPreferredSize().width, autoEject.getPreferredSize().width);
        assertEquals(autoEject.getPreferredSize().width, randomSkills.getPreferredSize().width);
        assertEquals(randomSkills.getPreferredSize().width, randomNames.getPreferredSize().width);
        assertEquals(randomNames.getPreferredSize().width, finalCellFiller.getPreferredSize().width);
    }

    @Test
    void alignsBehaviorSectionRightOptionsAtSameActualXCoordinate() {
        JCheckBox windowLeftTop = new JCheckBox("Team coloring");
        JCheckBox windowRightTop = new JCheckBox("Dock on left");
        JCheckBox windowLeftBottom = new JCheckBox("Stack vertically");
        JCheckBox windowRightBottom = new JCheckBox("Use camouflage overlay");
        JPanel windowGrid = CommonSettingsDialog.createBehaviorOptionsGrid("TestWindowLayoutGrid",
              windowLeftTop, windowRightTop, windowLeftBottom, windowRightBottom);

        JLabel protoMekLabel = new JLabel("ProtoMek unit codes");
        JTextField protoMekControl = new JTextField("A, B, C, D...");
        JCheckBox unitRightTop = new JCheckBox("Disable automatic ejection");
        JCheckBox unitLeftBottom = new JCheckBox(
            Messages.getString("CommonSettingsDialog.useAverageSkills"));
        JCheckBox unitRightBottom = new JCheckBox("Generate random pilot names");
        int longUnitOptionWidth = unitLeftBottom.getPreferredSize().width;
        JPanel unitGrid = CommonSettingsDialog.createUnitDefaultsGrid(protoMekLabel, protoMekControl,
              unitRightTop, unitLeftBottom, unitRightBottom);
        Component unitFiller = unitGrid.getComponent(5);

        JLabel gameLogLabel = new JLabel("Game log filename");
        JTextField gameLogField = new JTextField("game.log");
        JLabel autoResolveLogLabel = new JLabel("Auto Resolve log filename");
        JTextField autoResolveLogField = new JTextField("simulation.log");
        JCheckBox timestampOption = new JCheckBox("Add a timestamp");
        String defaultStampFormat = "_yyyy-MM-dd_HH-mm-ss";
        JLabel dateFormatLabel = new JLabel("Date format");
        JTextField dateFormatField = new JTextField(defaultStampFormat, 20);
        JCheckBox loggingLeftTop = new JCheckBox("Log data");
        JCheckBox loggingRightTop = new JCheckBox("Keep game log");
        JPanel loggingGrid = CommonSettingsDialog.createBehaviorLoggingGrid("TestLoggingGrid",
              loggingLeftTop, loggingRightTop,
              gameLogLabel, gameLogField,
              autoResolveLogLabel, autoResolveLogField,
            timestampOption,
              dateFormatLabel, dateFormatField);
        Component timestampFiller = loggingGrid.getComponent(7);

        JCheckBox privacyLeft = new JCheckBox("Show IP addresses in chat");
        JCheckBox privacyRight = new JCheckBox("Use sprites only");
        JPanel privacyGrid = CommonSettingsDialog.createBehaviorOptionsGrid("TestPrivacyGrid",
              privacyLeft, privacyRight);

        List<JPanel> sections = List.of(
            settingsGroup(List.of(row(windowGrid))),
            settingsGroup(List.of(row(unitGrid))),
            settingsGroup(List.of(row(loggingGrid))),
            settingsGroup(List.of(row(privacyGrid))));
        CommonSettingsPane pane = createSettingsPane("behavior", sections.toArray(new JComponent[0]));

        GridBagConstraints dateFormatFieldConstraints =
              ((GridBagLayout) loggingGrid.getLayout()).getConstraints(dateFormatField);
        assertCell(unitGrid, protoMekLabel, 0, 0);
        assertCell(unitGrid, protoMekControl, 1, 0);
        assertCell(unitGrid, unitRightTop, 0, 1);
        assertCell(unitGrid, unitLeftBottom, 1, 1);
        assertCell(unitGrid, unitRightBottom, 0, 2);
        assertCell(unitGrid, unitFiller, 1, 2);
        assertCell(loggingGrid, gameLogLabel, 0, 1);
        assertCell(loggingGrid, gameLogField, 1, 1);
        assertCell(loggingGrid, autoResolveLogLabel, 0, 2);
        assertCell(loggingGrid, autoResolveLogField, 1, 2);
        assertCell(loggingGrid, timestampOption, 0, 3);
        assertCell(loggingGrid, timestampFiller, 1, 3);
        assertCell(loggingGrid, dateFormatLabel, 0, 4);
        assertCell(loggingGrid, dateFormatField, 1, 4);
        assertSame(protoMekControl, protoMekLabel.getLabelFor());
        assertSame(gameLogField, gameLogLabel.getLabelFor());
        assertSame(autoResolveLogField, autoResolveLogLabel.getLabelFor());
        assertSame(dateFormatField, dateFormatLabel.getLabelFor());
        assertEquals(GridBagConstraints.HORIZONTAL, dateFormatFieldConstraints.fill);
        assertTrue(dateFormatFieldConstraints.weightx > 0.0);
        int requiredDateFormatWidth = dateFormatField.getFontMetrics(dateFormatField.getFont())
              .stringWidth(defaultStampFormat)
              + dateFormatField.getInsets().left
              + dateFormatField.getInsets().right;

        assertAtStandardAndWideWidths(pane, laidOutPane -> {
            assertAlignedBalancedRows(laidOutPane, List.of(
                    List.of(windowLeftTop, windowRightTop),
                    List.of(windowLeftBottom, windowRightBottom),
                    List.of(protoMekLabel, protoMekControl),
                    List.of(unitRightTop, unitLeftBottom),
                    List.of(unitRightBottom, unitFiller),
                    List.of(loggingLeftTop, loggingRightTop),
                    List.of(gameLogLabel, gameLogField),
                    List.of(autoResolveLogLabel, autoResolveLogField),
                    List.of(timestampOption, timestampFiller),
                    List.of(dateFormatLabel, dateFormatField),
                    List.of(privacyLeft, privacyRight)));
            int expectedGridX = xRelativeTo(windowGrid, laidOutPane);
            int expectedGridWidth = windowGrid.getWidth();
            for (JPanel grid : List.of(windowGrid, unitGrid, loggingGrid, privacyGrid)) {
                assertEquals(expectedGridX, xRelativeTo(grid, laidOutPane));
                assertEquals(expectedGridWidth, grid.getWidth());
            }
            assertTrue(unitLeftBottom.getWidth() >= longUnitOptionWidth);
            assertTrue(dateFormatField.getWidth() >= requiredDateFormatWidth);
            assertHorizontallyFilled(protoMekControl);
            assertHorizontallyFilled(gameLogField);
            assertHorizontallyFilled(autoResolveLogField);
        });
    }

    @Test
    void usesConciseUnitDefaultLabels() {
        assertEquals("Disable automatic ejection for lobby units",
            Messages.getString("CommonSettingsDialog.defaultAutoejectDisabled"));
        assertEquals("Use current random skill settings for lobby units",
            Messages.getString("CommonSettingsDialog.useAverageSkills"));
        assertEquals("Generate random pilot names",
            Messages.getString("CommonSettingsDialog.generateNames"));
    }

    @Test
    void presentsShowIpAddressesAsImportantMetadataWithDetails() {
        SettingsCheckBox checkBox = CommonSettingsDialog.createShowIpAddressesInChatCheckBox();
        SettingsBadge importantBadge = CommonSettingsPane.legendEntries().stream()
            .filter(badge -> badge.codePoint() == 0xE002)
            .findFirst()
            .orElseThrow();
        String helpText = Messages.getString("CommonSettingsDialog.showIPAddressesInChat.tooltip");

        assertTrue(checkBox.getText().contains("Show IP Addresses in Chat"));
        assertFalse(checkBox.getText().contains("WARNING"));
        assertTrue(checkBox.getText().contains(importantBadge.toHtml()));
        assertEquals(helpText, checkBox.getSettingsHelpText());
        assertTrue(checkBox.getToolTipText().contains("information disclosure"));
    }

    @Test
    void prefersMegaMekUserFilesHelpDocument(@TempDir Path docsDirectory) throws IOException {
        Path megaMekDocument = createHelpDocument(docsDirectory, "UserDir");
        createHelpDocument(docsDirectory, "MekHQ");
        JButton button = new JButton();
        AtomicReference<URL> openedDocument = new AtomicReference<>();

        CommonSettingsDialog.configureUserFilesHelpButton(button, docsDirectory.toFile(), openedDocument::set);
        button.doClick();

        assertTrue(button.isEnabled());
        assertEquals(megaMekDocument.toUri().toURL(), openedDocument.get());
    }

    @Test
    void fallsBackToMekHqUserFilesHelpDocument(@TempDir Path docsDirectory) throws IOException {
        Path mekHqDocument = createHelpDocument(docsDirectory, "MekHQ");
        JButton button = new JButton();
        AtomicReference<URL> openedDocument = new AtomicReference<>();

        CommonSettingsDialog.configureUserFilesHelpButton(button, docsDirectory.toFile(), openedDocument::set);
        button.doClick();

        assertTrue(button.isEnabled());
        assertEquals(mekHqDocument.toUri().toURL(), openedDocument.get());
    }

    @Test
    void disablesUserFilesHelpWhenDocumentIsUnavailable(@TempDir Path docsDirectory) {
        JButton button = new JButton();
        String unavailableMessage = "User Files help is unavailable because its documentation file is missing.";

        CommonSettingsDialog.configureUserFilesHelpButton(button, docsDirectory.toFile(), ignored -> { });

        assertFalse(button.isEnabled());
        assertEquals(unavailableMessage, button.getToolTipText());
        assertEquals(unavailableMessage, button.getAccessibleContext().getAccessibleDescription());
    }

    private static Path createHelpDocument(Path docsDirectory, String suiteDirectory) throws IOException {
        Path document = docsDirectory.resolve(Path.of("Customization", suiteDirectory, "UserDirHelp.html"));
        Files.createDirectories(document.getParent());
        return Files.createFile(document);
    }

    @Test
    void leavesNestedColourButtonWidthsIndependent() {
        ColourSelectorButton nestedButton = new ColourSelectorButton("");
        JPanel nestedPanel = CommonSettingsDialog.createSettingsPanel(List.of(row(nestedButton)));
        int nestedWidth = nestedButton.getPreferredSize().width;

        ColourSelectorButton wideButton = new ColourSelectorButton("Wide");
        Dimension wideSize = wideButton.getPreferredSize();
        wideButton.setPreferredSize(new Dimension(nestedWidth + 100, wideSize.height));

        CommonSettingsDialog.createSettingsPanel(List.of(row(wideButton), row(nestedPanel)));

        assertEquals(nestedWidth, nestedButton.getPreferredSize().width);
    }

    @Test
    void rejectsUnmappedSettingsGroups() {
        JPanel firstGroup = new JPanel();
        JPanel omittedGroup = new JPanel();
        CommonSettingsPane.SectionedContent content = new CommonSettingsPane.SectionedContent(
            List.of(firstGroup, omittedGroup));
        CommonSettingsPane.OptionPage incompletePage = new CommonSettingsPane.OptionPage(
              "test", List.of("Test"), "Test", List.of(new CommonSettingsPane.OptionSection(
                    "first", "First", "First group", firstGroup, false)));

        assertThrows(IllegalArgumentException.class,
            () -> CommonSettingsDialog.addMappedPages(new ArrayList<>(), "test", content, incompletePage));
    }

    private static List<Component> row(Component... components) {
        return List.of(components);
    }

    private static DefaultListModel<String> unitDisplayOrderModel() {
        DefaultListModel<String> order = new DefaultListModel<>();
        order.addElement(UnitDisplayPanel.NON_TABBED_GENERAL);
        order.addElement(UnitDisplayPanel.NON_TABBED_PILOT);
        order.addElement(UnitDisplayPanel.NON_TABBED_ARMOR);
        order.addElement(UnitDisplayPanel.NON_TABBED_WEAPON);
        order.addElement(UnitDisplayPanel.NON_TABBED_SYSTEM);
        order.addElement(UnitDisplayPanel.NON_TABBED_EXTRA);
        return order;
    }

    private static void assertThreeColumnUnitDisplayOrderGrid(JPanel orderGrid) {
        JList<?> orderList = findComponent(orderGrid, JList.class);
        assertEquals(6, orderList.getModel().getSize());
        assertEquals(2, orderList.getVisibleRowCount());
        int preferredCellWidth = UIUtil.scaleForGUI((SettingsFormPanel.DEFAULT_LABEL_WIDTH * 2) / 3);
        Insets insets = orderGrid.getInsets();
        int availableWidth = orderGrid.getWidth() - insets.left - insets.right;
        assertEquals(Math.min(preferredCellWidth, availableWidth / 3), orderList.getFixedCellWidth());
        assertTrue(orderList.getFixedCellHeight() > 0);
        assertComponentWithinParent(orderList);
    }

    private static JPanel settingsGroup(List<List<Component>> rows) {
        CommonSettingsPane.SectionedContent content =
              (CommonSettingsPane.SectionedContent) CommonSettingsDialog.createSettingsPanel(rows);
        assertEquals(1, content.groups().size());
        return (JPanel) content.groups().getFirst();
    }

    private static CommonSettingsPane createSettingsPane(String id, JComponent... sections) {
        List<CommonSettingsPane.OptionSection> optionSections = new ArrayList<>();
        for (int index = 0; index < sections.length; index++) {
            optionSections.add(new CommonSettingsPane.OptionSection(
                  id + "." + index, "Section " + index, "Summary " + index, sections[index], false));
        }
        return new CommonSettingsPane(List.of(new CommonSettingsPane.OptionPage(
              id, List.of(id), id, optionSections)));
    }

    private static void assertAtStandardAndWideWidths(CommonSettingsPane pane,
        Consumer<CommonSettingsPane> assertion) {
        SettingsPagePanel page = findComponent(pane, SettingsPagePanel.class);
        page.expandAllSections();
        Dimension standardSize = pane.getPreferredSize();
        for (int extraWidth : new int[] { 0, UIUtil.scaleForGUI(320) }) {
            pane.setSize(standardSize.width + extraWidth, standardSize.height);
            layoutRecursively(pane);
            assertion.accept(pane);
        }
    }

    private static void assertAtConstrainedStandardAndWideWidths(CommonSettingsPane pane,
        List<JPanel> sectionContents, Consumer<CommonSettingsPane> assertion) {
        SettingsPagePanel page = findComponent(pane, SettingsPagePanel.class);
        page.expandAllSections();
        Dimension standardSize = pane.getPreferredSize();

        pane.setSize(standardSize.width, standardSize.height);
        layoutRecursively(pane);
        assertion.accept(pane);

        pane.setSize(standardSize.width - UIUtil.scaleForGUI(320), standardSize.height);
        layoutRecursively(pane);
        int constrainedContentWidth = UIUtil.scaleForGUI(500);
        for (JPanel sectionContent : sectionContents) {
            sectionContent.setSize(constrainedContentWidth, sectionContent.getPreferredSize().height);
            layoutRecursively(sectionContent);
        }
        assertion.accept(pane);

        pane.setSize(standardSize.width + UIUtil.scaleForGUI(320), standardSize.height);
        layoutRecursively(pane);
        assertion.accept(pane);
    }

    private static void assertAlignedBalancedRows(Container ancestor, List<List<Component>> rows) {
        Component firstLeft = rows.getFirst().getFirst();
        Component firstRight = rows.getFirst().get(1);
        int expectedLeftX = xRelativeTo(firstLeft, ancestor);
        int expectedRightX = xRelativeTo(firstRight, ancestor);
        int expectedRightEdge = expectedRightX + firstRight.getWidth();

        for (List<Component> row : rows) {
            Component left = row.getFirst();
            Component right = row.get(1);
            String description = componentDescription(left) + " | " + componentDescription(right);
            assertTrue(left.getWidth() > 0, description);
            assertEquals(left.getWidth(), right.getWidth(), description);
            assertEquals(expectedLeftX, xRelativeTo(left, ancestor), description);
            assertEquals(expectedRightX, xRelativeTo(right, ancestor), description);
            assertEquals(expectedRightEdge, xRelativeTo(right, ancestor) + right.getWidth(), description);
            assertHorizontallyFilled(left);
            assertHorizontallyFilled(right);
        }
    }

    private static String componentDescription(Component component) {
        if (component instanceof javax.swing.AbstractButton button) {
            return button.getText();
        }
        return component.getClass().getSimpleName();
    }

    private static void assertHorizontallyFilled(Component component) {
        JPanel parent = (JPanel) component.getParent();
        GridBagConstraints constraints = ((GridBagLayout) parent.getLayout()).getConstraints(component);
        assertEquals(GridBagConstraints.HORIZONTAL, constraints.fill);
        assertTrue(constraints.weightx > 0.0);
        assertComponentWithinParent(component);
    }

    private static void assertComponentWithinParent(Component component) {
        assertTrue(component.getWidth() > 0);
        assertTrue(component.getX() >= 0);
        assertTrue(component.getX() + component.getWidth() <= component.getParent().getWidth());
    }

    private static void assertCell(JPanel panel, Component component, int column, int row) {
        GridBagConstraints constraints = ((GridBagLayout) panel.getLayout()).getConstraints(component);
        assertEquals(column, constraints.gridx);
        assertEquals(row, constraints.gridy);
    }

    private static JPanel findNamedPanel(Container root, String name) {
        if (root instanceof JPanel panel && name.equals(panel.getName())) {
            return panel;
        }
        for (Component child : root.getComponents()) {
            if (child instanceof Container container) {
                try {
                    return findNamedPanel(container, name);
                } catch (AssertionError ignored) {
                    // Continue through the remaining branches.
                }
            }
        }
        throw new AssertionError("No panel named " + name);
    }

    private static <T extends Component> T findComponent(Container root, Class<T> type) {
        List<Container> pending = new ArrayList<>(List.of(root));
        while (!pending.isEmpty()) {
            Container container = pending.removeLast();
            for (Component child : container.getComponents()) {
                if (type.isInstance(child)) {
                    return type.cast(child);
                }
                if (child instanceof Container childContainer) {
                    pending.add(childContainer);
                }
            }
        }
        throw new AssertionError("No " + type.getSimpleName());
    }

    private static void layoutRecursively(Container container) {
        container.doLayout();
        for (Component child : container.getComponents()) {
            if (child instanceof Container nestedContainer) {
                layoutRecursively(nestedContainer);
            }
        }
    }

    private static int xRelativeTo(Component component, Container ancestor) {
        int x = 0;
        Component current = component;
        while (current != ancestor) {
            x += current.getX();
            current = current.getParent();
            if (current == null) {
                throw new AssertionError("Component is not contained by the expected ancestor");
            }
        }
        return x;
    }
}
