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

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
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
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import megamek.client.ui.Messages;
import megamek.client.ui.buttons.ColourSelectorButton;
import megamek.client.ui.panels.CommonSettingsPane;
import megamek.client.ui.settings.SettingsBadge;
import megamek.client.ui.settings.SettingsCheckBox;
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
        JSlider scaleControl = new JSlider(7, 24);
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

    private static void assertAudioSectionsHaveAlignedControlColumns() {
        JLabel volumeLabel = new JLabel(Messages.getString("CommonSettingsDialog.masterVolume"));
        JSlider volumeSlider = new JSlider(0, 100);
        volumeSlider.setMinorTickSpacing(5);
        volumeSlider.setMajorTickSpacing(25);
        Hashtable<Integer, JComponent> volumeLabels = new Hashtable<>();
        volumeLabels.put(0, new JLabel("0%"));
        volumeLabels.put(25, new JLabel("25%"));
        volumeLabels.put(50, new JLabel("50%"));
        volumeLabels.put(75, new JLabel("75%"));
        volumeLabels.put(100, new JLabel("100%"));
        volumeSlider.setLabelTable(volumeLabels);
        volumeSlider.setPaintTicks(true);
        volumeSlider.setPaintLabels(true);
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
        assertCell(volumeGrid, volumeSlider, 1, 0);
        assertSame(volumeSlider, volumeLabel.getLabelFor());

        JPanel notificationGrid = findNamedPanel(notificationsSection,
              "pnlCommonSettingsAudioNotificationGrid");
                assertSame(notificationsSection, notificationGrid);
        assertEquals(6, notificationGrid.getComponentCount());
        assertCell(notificationGrid, chatMute, 0, 0);
        assertCell(notificationGrid, chatSoundFile, 1, 0);
        assertCell(notificationGrid, myTurnMute, 0, 1);
        assertCell(notificationGrid, myTurnSoundFile, 1, 1);
        assertCell(notificationGrid, otherTurnsMute, 0, 2);
        assertCell(notificationGrid, otherTurnsSoundFile, 1, 2);

        assertAtConstrainedStandardAndWideWidths(pane, List.of(volumeGrid, notificationGrid), laidOutPane -> {
            assertEquals(xRelativeTo(volumeGrid, laidOutPane), xRelativeTo(notificationGrid, laidOutPane));
            assertEquals(volumeGrid.getWidth(), notificationGrid.getWidth());
            assertAlignedBalancedRows(laidOutPane, List.of(
                  List.of(volumeLabel, volumeSlider),
                  List.of(chatMute, chatSoundFile),
                  List.of(myTurnMute, myTurnSoundFile),
                  List.of(otherTurnsMute, otherTurnsSoundFile)));
            GridBagConstraints volumeLabelConstraints =
                  ((GridBagLayout) volumeGrid.getLayout()).getConstraints(volumeLabel);
            int columnGap = xRelativeTo(volumeSlider, laidOutPane)
                  - xRelativeTo(volumeLabel, laidOutPane) - volumeLabel.getWidth();
            assertEquals(volumeLabelConstraints.insets.right, columnGap);
            assertEquals(xRelativeTo(volumeGrid, laidOutPane) + volumeGrid.getWidth(),
                  xRelativeTo(volumeSlider, laidOutPane) + volumeSlider.getWidth());
            assertTrue(otherTurnsMute.getWidth() >= otherTurnsMuteWidth);
        });

        assertEquals("Notifications",
              Messages.getString("CommonSettingsDialog.section.audio.notifications.title"));
        assertEquals("Configure notification muting and sound files.",
              Messages.getString("CommonSettingsDialog.section.audio.notifications.summary"));
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
        JPanel protoMekOption = findNamedPanel(grid, "pnlCommonSettingsProtoMekOption");

        assertCell(grid, protoMekOption, 0, 0);
        assertCell(grid, autoEject, 1, 0);
        assertCell(grid, randomSkills, 0, 1);
        assertCell(grid, randomNames, 1, 1);
        assertCell(protoMekOption, protoMekLabel, 0, 0);
        assertCell(protoMekOption, protoMekControl, 1, 0);
        assertSame(protoMekControl, protoMekLabel.getLabelFor());
        assertEquals(protoMekOption.getPreferredSize().width, autoEject.getPreferredSize().width);
        assertEquals(autoEject.getPreferredSize().width, randomSkills.getPreferredSize().width);
        assertEquals(randomSkills.getPreferredSize().width, randomNames.getPreferredSize().width);
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
          JPanel protoMekOption = findNamedPanel(unitGrid, "pnlCommonSettingsProtoMekOption");

          JLabel gameLogLabel = new JLabel("Game log filename");
          JTextField gameLogField = new JTextField("game.log");
        JPanel gameLogOption = CommonSettingsDialog.createBehaviorFieldOption("TestGameLogOption",
              gameLogLabel, gameLogField);
          JLabel autoResolveLogLabel = new JLabel("Auto Resolve log filename");
          JTextField autoResolveLogField = new JTextField("simulation.log");
        JPanel autoResolveLogOption = CommonSettingsDialog.createBehaviorFieldOption("TestAutoResolveLogOption",
              autoResolveLogLabel, autoResolveLogField);
        JCheckBox timestampOption = new JCheckBox("Add a timestamp");
        String defaultStampFormat = "_yyyy-MM-dd_HH-mm-ss";
        JLabel dateFormatLabel = new JLabel("Date format");
        JTextField dateFormatField = new JTextField(defaultStampFormat, 20);
        JCheckBox loggingLeftTop = new JCheckBox("Log data");
        JCheckBox loggingRightTop = new JCheckBox("Keep game log");
        JPanel loggingGrid = CommonSettingsDialog.createBehaviorLoggingGrid("TestLoggingGrid",
              List.of(loggingLeftTop, loggingRightTop, gameLogOption, autoResolveLogOption, timestampOption),
              dateFormatLabel, dateFormatField);
        Component timestampFiller = loggingGrid.getComponent(5);

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
          assertCell(unitGrid, protoMekOption, 0, 0);
          assertCell(unitGrid, unitRightTop, 1, 0);
          assertCell(loggingGrid, gameLogOption, 0, 1);
          assertCell(loggingGrid, autoResolveLogOption, 1, 1);
        assertCell(loggingGrid, timestampOption, 0, 2);
        assertCell(loggingGrid, timestampFiller, 1, 2);
        assertCell(loggingGrid, dateFormatLabel, 0, 3);
        assertCell(loggingGrid, dateFormatField, 1, 3);
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
                  List.of(protoMekOption, unitRightTop),
                  List.of(unitLeftBottom, unitRightBottom),
                  List.of(loggingLeftTop, loggingRightTop),
                  List.of(gameLogOption, autoResolveLogOption),
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
