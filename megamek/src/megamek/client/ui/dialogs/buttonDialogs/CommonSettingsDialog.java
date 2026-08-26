/*
 * Copyright (c) 2003-2005 - Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2003-2026 The MegaMek Team. All Rights Reserved.
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
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.MouseInputAdapter;

import megamek.MMConstants;
import megamek.client.bot.princess.BehaviorSettingsFactory;
import megamek.client.ui.Messages;
import megamek.client.ui.buttons.ColourSelectorButton;
import megamek.client.ui.clientGUI.ButtonOrderPreferences;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.clientGUI.GifRecordingMode;
import megamek.client.ui.clientGUI.UITheme;
import megamek.client.ui.clientGUI.UnitDisplayOrderPreferences;
import megamek.client.ui.comboBoxes.MMComboBox;
import megamek.client.ui.dialogs.helpDialogs.HelpDialog;
import megamek.client.ui.dialogs.minimap.MinimapPanel;
import megamek.client.ui.dialogs.unitDisplay.UnitDisplayPanel;
import megamek.client.ui.models.FileNameComboBoxModel;
import megamek.client.ui.panels.CommonSettingsPane;
import megamek.client.ui.panels.phaseDisplay.DeploymentDisplay;
import megamek.client.ui.panels.phaseDisplay.FiringDisplay;
import megamek.client.ui.panels.phaseDisplay.PhysicalDisplay;
import megamek.client.ui.panels.phaseDisplay.StatusBarPhaseDisplay;
import megamek.client.ui.panels.phaseDisplay.StatusBarPhaseDisplay.PhaseCommand;
import megamek.client.ui.panels.phaseDisplay.TargetingPhaseDisplay;
import megamek.client.ui.panels.phaseDisplay.commands.MoveCommand;
import megamek.client.ui.settings.SettingsBadge;
import megamek.client.ui.settings.SettingsButton;
import megamek.client.ui.settings.SettingsCheckBox;
import megamek.client.ui.settings.SettingsFormPanel;
import megamek.client.ui.settings.SettingsIconLegend;
import megamek.client.ui.settings.SettingsTextField;
import megamek.client.ui.settings.SettingsTextProvider;
import megamek.client.ui.util.FontHandler;
import megamek.client.ui.util.KeyCommandBind;
import megamek.client.ui.util.PlayerColour;
import megamek.client.ui.util.UIUtil;
import megamek.client.ui.widget.SkinXMLHandler;
import megamek.codeUtilities.MathUtility;
import megamek.common.Configuration;
import megamek.common.KeyBindParser;
import megamek.common.enums.GamePhase;
import megamek.common.enums.WeaponSortOrder;
import megamek.common.loaders.MapSettings;
import megamek.common.preference.ClientPreferences;
import megamek.common.preference.PreferenceManager;
import megamek.common.util.BoardUtilities;
import megamek.common.util.fileUtils.MegaMekFile;
import megamek.logging.MMLogger;

/**
 * The Client Settings Dialog offering GUI options concerning tooltips, map display, keybinds etc.
 *
 * <p>Hidden testing/debug preferences are intentionally NOT shown in the Advanced list (they are filtered out via
 * {@link #HIDDEN_ADVANCED_OPTIONS}) and can only be enabled by manually adding them to {@code clientsettings.xml}:
 * <pre>{@code
 * <preference name="RevealAllArtilleryRounds" value="true"/>          reveal BOTH teams' in-flight artillery target hexes
 * <preference name="AdvancedShowBotArtilleryHeatMap" value="true"/>   draw Princess's predicted/firing artillery heat map
 * <preference name="AdvancedRevealObscuredArtillery" value="true"/>   reveal otherwise-obscured artillery hex markers
 * }</pre>
 * Each defaults to {@code false}; remove the line (or set {@code false}) to disable.
 */
public class CommonSettingsDialog extends AbstractButtonDialog
      implements ItemListener, FocusListener, ListSelectionListener, ChangeListener {
    private final static MMLogger logger = MMLogger.create(CommonSettingsDialog.class);
    private static final SettingsTextProvider SETTINGS_TEXT = SettingsTextProvider.megaMek();
    private static final SettingsBadge IMPORTANT_BADGE = new SettingsBadge(0xE002, null,
          Messages.getString("CommonSettingsDialog.legend.important"));
    private static final int BEHAVIOR_OPTION_COLUMNS = 2;

    /**
     * A class for storing information about an GUIPreferences advanced option.
     *
     * @author arlith
     */
    private static class AdvancedOptionData implements Comparable<AdvancedOptionData> {
        public String option;

        public AdvancedOptionData(String option) {
            this.option = option;
        }

        /** Returns true if this option has tooltip text. */
        public boolean hasTooltipText() {
            return Messages.keyExists("AdvancedOptions." + option + ".tooltip");
        }

        /** Returns the tooltip text for this option. */
        public String getTooltipText() {
            return Messages.getString("AdvancedOptions." + option + ".tooltip");
        }

        /** Returns a human-readable name for this advanced option. */
        @Override
        public String toString() {
            String key = "AdvancedOptions." + option + ".name";
            return Messages.keyExists(key) ? Messages.getString(key) : option;
        }

        @Override
        public int compareTo(AdvancedOptionData other) {
            return toString().compareTo(other.toString());
        }
    }

    private static class PhaseCommandListMouseAdapter extends MouseInputAdapter {
        private boolean mouseDragging = false;
        private int dragSourceIndex;

        @Override
        public void mousePressed(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e)) {
                Object src = e.getSource();
                if (src instanceof JList) {
                    dragSourceIndex = ((JList<?>) src).getSelectedIndex();
                    mouseDragging = true;
                }
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            mouseDragging = false;
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            Object src = e.getSource();
            if (mouseDragging && (src instanceof JList<?> srcList)) {
                DefaultListModel<?> srcModel = (DefaultListModel<?>) srcList.getModel();
                int currentIndex = srcList.locationToIndex(e.getPoint());
                if (currentIndex != dragSourceIndex) {
                    int dragTargetIndex = srcList.getSelectedIndex();
                    moveElement(srcModel, dragSourceIndex, dragTargetIndex);
                    dragSourceIndex = currentIndex;
                }
            }
        }

        private <T> void moveElement(DefaultListModel<T> srcModel, int srcIndex, int trgIndex) {
            T dragElement = srcModel.get(srcIndex);
            srcModel.remove(srcIndex);
            srcModel.add(trgIndex, dragElement);
        }
    }

    private final JCheckBox autoEndFiring = new JCheckBox(Messages.getString("CommonSettingsDialog.autoEndFiring"));
    private final JCheckBox autoDeclareSearchlight = new JCheckBox(Messages.getString(
          "CommonSettingsDialog.autoDeclareSearchlight"));
    private final JCheckBox nagForMASC = new JCheckBox(Messages.getString("CommonSettingsDialog.nagForMASC"));
    private final JCheckBox nagForPSR = new JCheckBox(Messages.getString("CommonSettingsDialog.nagForPSR"));
    private final JCheckBox nagForWiGELanding = new JCheckBox(Messages.getString(
          "CommonSettingsDialog.nagForWiGELanding"));
    private final JCheckBox nagForNoAction = new JCheckBox(Messages.getString("CommonSettingsDialog.nagForNoAction"));
    private final JCheckBox nagForNoUnJamRAC = new JCheckBox(Messages.getString("CommonSettingsDialog.nagForUnJamRAC"));
    private final JCheckBox nagForOverheat = new JCheckBox(Messages.getString("CommonSettingsDialog.nagForOverheat"));
    private final JCheckBox nagForDishonor = new JCheckBox(Messages.getString("CommonSettingsDialog.nagForDishonor"));
    private final JCheckBox nagForMechanicalJumpFallDamage = new JCheckBox(Messages.getString(
          "CommonSettingsDialog.nagForMechanicalJumpFallDamage"));
    private final JCheckBox nagForCrushingBuildings = new JCheckBox(Messages.getString(
          "CommonSettingsDialog.nagForCrushingBuildings"));
    private final JCheckBox nagForLaunchDoors = new JCheckBox(Messages.getString(
          "CommonSettingsDialog.nagForLaunchDoors"));
    private final JCheckBox nagForSprint = new JCheckBox(Messages.getString("CommonSettingsDialog.nagForSprint"));
    private final JCheckBox nagForOddSizedBoard =
          new JCheckBox(Messages.getString("CommonSettingsDialog.nagForOddSizedBoard"));
    private final JCheckBox animateMove = new JCheckBox(Messages.getString("CommonSettingsDialog.animateMove"));
    private final JCheckBox showWrecks = new JCheckBox(Messages.getString("CommonSettingsDialog.showWrecks"));
    private final JCheckBox chkHighQualityGraphics = new JCheckBox(Messages.getString(
          "CommonSettingsDialog.highQualityGraphics"));
    private final JCheckBox chkHighPerformanceGraphics = new JCheckBox(Messages.getString(
          "CommonSettingsDialog.highPerformanceGraphics"));
    private final JCheckBox showWpsInTT = new JCheckBox(Messages.getString("CommonSettingsDialog.showWpsinTT"));
    private final JCheckBox showWpsLocinTT = new JCheckBox(Messages.getString("CommonSettingsDialog.showWpsLocinTT"));
    private final JCheckBox showArmorMiniVisTT = new JCheckBox(Messages.getString(
          "CommonSettingsDialog.showArmorMiniVisTT"));
    private final JCheckBox showPilotPortraitTT = new JCheckBox(Messages.getString(
          "CommonSettingsDialog.showPilotPortraitTT"));
    private MMComboBox<WeaponSortOrder> comboDefaultWeaponSortOrder;
    private JSpinner tooltipDelay;
    private JSpinner tooltipDismissDelay;
    private JSpinner tooltipDistSuppression;
    private JComboBox<String> unitStartChar;
    private JSpinner maxPathfinderTime;
    private final JCheckBox getFocus = new JCheckBox(Messages.getString("CommonSettingsDialog.getFocus"));
    private JSlider guiScale;
    private ColourSelectorButton csbWarningColor;
    private ColourSelectorButton csbCautionColor;
    private ColourSelectorButton csbPrecautionColor;
    private ColourSelectorButton csbOkColor;
    private ColourSelectorButton csbMyUnitColor;
    private ColourSelectorButton csbAllyUnitColor;
    private ColourSelectorButton csbEnemyColor;

    ArrayList<PlayerColourHelper> playerColours;

    // Audio Tab
    private final JLabel masterVolumeLabel = new JLabel(Messages.getString("CommonSettingsDialog.masterVolume"));
    private JSlider masterVolumeSlider;
    private final JCheckBox soundMuteChat = new JCheckBox(Messages.getString("CommonSettingsDialog.soundMuteChat"));
    private JTextField tfSoundMuteChatFileName;
    private final JCheckBox soundMuteMyTurn = new JCheckBox(Messages.getString("CommonSettingsDialog.soundMuteMyTurn"));
    private JTextField tfSoundMuteMyTurnFileName;
    private final JCheckBox soundMuteOthersTurn = new JCheckBox(Messages.getString(
          "CommonSettingsDialog.soundMuteOthersTurn"));
    private JTextField tfSoundMuteOthersFileName;

    private JTextField userDir;
    private JTextField mmlPath;
    private final JCheckBox keepGameLog = new JCheckBox(Messages.getString("CommonSettingsDialog.keepGameLog"));
    private final JCheckBox datasetLogging = new JCheckBox(Messages.getString("CommonSettingsDialog.datasetLogging"));

    private JTextField gameLogFilename;
    private JTextField autoResolveLogFilename;
    private final JCheckBox stampFilenames = new JCheckBox(Messages.getString("CommonSettingsDialog.stampFilenames"));
    private JTextField stampFormat;
    private final JCheckBox enableExperimentalBotFeatures = new JCheckBox(Messages.getString(
          "CommonSettingsDialog.enableExperimentalBotFeatures"));
    private final JCheckBox defaultAutoEjectDisabled = new JCheckBox(Messages.getString(
          "CommonSettingsDialog.defaultAutoejectDisabled"));
    private final JCheckBox useAverageSkills =
          new JCheckBox(Messages.getString("CommonSettingsDialog.useAverageSkills"));
    private final JCheckBox generateNames = new JCheckBox(Messages.getString("CommonSettingsDialog.generateNames"));
    private final JCheckBox showUnitId = new JCheckBox(Messages.getString("CommonSettingsDialog.showUnitId"));
    private final JCheckBox showAutoResolvePanel = new JCheckBox(Messages.getString(
          "CommonSettingsDialog.showAutoResolvePanel"));
    private JComboBox<String> favoritePrincessBehaviorSetting;
    private JComboBox<String> displayLocale;
    private final SettingsCheckBox showIPAddressesInChat = createShowIpAddressesInChatCheckBox();
    private final JCheckBox spritesOnly = new JCheckBox(Messages.getString(
          "CommonSettingsDialog.spritesOnly"));
    private final JCheckBox showDamageLevel = new JCheckBox(Messages.getString("CommonSettingsDialog.showDamageLevel"));
    private final JCheckBox showDamageDecal = new JCheckBox(Messages.getString("CommonSettingsDialog.showDamageDecal"));
    private final JCheckBox showMapSheets = new JCheckBox(Messages.getString("CommonSettingsDialog.showMapsheets"));
    private final JCheckBox aOHexShadows = new JCheckBox(Messages.getString("CommonSettingsDialog.aOHexSHadows"));
    private final JCheckBox floatingIso = new JCheckBox(Messages.getString("CommonSettingsDialog.floatingIso"));
    private final JCheckBox mmSymbol = new JCheckBox(Messages.getString("CommonSettingsDialog.mmSymbol"));
    private final JCheckBox drawFacingArrowsOnMiniMap = new JCheckBox(Messages.getString(
          "CommonSettingsDialog.drawFacingArrowsOnMiniMap"));
    private final JCheckBox drawSensorRangeOnMiniMap = new JCheckBox(Messages.getString(
          "CommonSettingsDialog.drawSensorRangeOnMiniMap"));
    private final JCheckBox paintBordersOnMiniMap = new JCheckBox(Messages.getString(
          "CommonSettingsDialog.paintBordersOnMiniMap"));
    private final JCheckBox entityOwnerColor =
          new JCheckBox(Messages.getString("CommonSettingsDialog.entityOwnerColor"));
    private final JCheckBox teamColoring = new JCheckBox(Messages.getString("CommonSettingsDialog.teamColoring"));
    private final JCheckBox dockOnLeft = new JCheckBox(Messages.getString("CommonSettingsDialog.dockOnLeft"));
    private final JCheckBox dockMultipleOnYAxis = new JCheckBox(Messages.getString(
          "CommonSettingsDialog.dockMultipleOnYAxis"));
    private final JCheckBox useCamoOverlay = new JCheckBox(Messages.getString("CommonSettingsDialog.useCamoOverlay"));
    private final JCheckBox useSoftCenter = new JCheckBox(Messages.getString("CommonSettingsDialog.useSoftCenter"));
    private final JCheckBox useAutoCenter = new JCheckBox(Messages.getString("CommonSettingsDialog.useAutoCenter"));
    private final JCheckBox useAutoSelectNext = new JCheckBox(Messages.getString(
          "CommonSettingsDialog.useAutoSelectNext"));
    private final JCheckBox levelHighlight = new JCheckBox(Messages.getString("CommonSettingsDialog.levelHighlight"));
    private final JCheckBox shadowMap = new JCheckBox(Messages.getString("CommonSettingsDialog.useShadowMap"));
    private final JCheckBox hexInclines = new JCheckBox(Messages.getString("CommonSettingsDialog.useInclines"));
    private final JCheckBox mouseWheelZoom = new JCheckBox(Messages.getString("CommonSettingsDialog.mouseWheelZoom"));
    private final JCheckBox mouseWheelZoomFlip = new JCheckBox(Messages.getString(
          "CommonSettingsDialog.mouseWheelZoomFlip"));

    // Bomb and Artillery displays
    private final JCheckBox artilleryDisplayMisses = new JCheckBox(Messages.getString(
          "CommonSettingsDialog.hexes.ShowArtilleryMisses"));
    private final JCheckBox artilleryDisplayDriftedHits = new JCheckBox(Messages.getString(
          "CommonSettingsDialog.hexes.ShowArtilleryDriftedHits"));
    private final JCheckBox artilleryDisplayDriftArrows = new JCheckBox(Messages.getString(
          "CommonSettingsDialog.hexes.ShowArtilleryDriftArrows"));
    private final JCheckBox bombsDisplayMisses = new JCheckBox(Messages.getString(
          "CommonSettingsDialog.hexes.ShowBombMisses"));
    private final JCheckBox bombsDisplayDrifts = new JCheckBox(Messages.getString(
          "CommonSettingsDialog.hexes.ShowBombDrifts"));

    private final JCheckBox moveDefaultClimbMode = new JCheckBox(Messages.getString(
          "CommonSettingsDialog.moveDefaultClimbMode"));
    private ColourSelectorButton csbMoveDefaultColor;
    private ColourSelectorButton csbMoveIllegalColor;
    private ColourSelectorButton csbMoveJumpColor;
    private ColourSelectorButton csbMoveMASCColor;
    private ColourSelectorButton csbMoveRunColor;
    private ColourSelectorButton csbMoveBackColor;
    private ColourSelectorButton csbMoveSprintColor;

    private JComboBox<String> fontTypeChooserMoveFont = new JComboBox<>();
    private JSpinner moveFontSize;
    private final JComboBox<String> fontStyleChooserMoveFont = new JComboBox<>();

    private ColourSelectorButton csbFireSolutionCanSeeColor;
    private ColourSelectorButton csbFireSolutionNoSeeColor;
    private ColourSelectorButton csbFieldOfFireMinColor;
    private ColourSelectorButton csbFieldOfFireShortColor;
    private ColourSelectorButton csbFieldOfFireMediumColor;
    private ColourSelectorButton csbFieldOfFireLongColor;
    private ColourSelectorButton csbFieldOfFireExtremeColor;
    private ColourSelectorButton csbSensorRangeColor;
    private ColourSelectorButton csbVisualRangeColor;
    private ColourSelectorButton csbUnitValidColor;
    private ColourSelectorButton csbUnitSelectedColor;
    private ColourSelectorButton csbUnitTextColor;
    private ColourSelectorButton csbBuildingTextColor;
    private ColourSelectorButton csbLowFoliageColor;
    private ColourSelectorButton csbDemolitionChargeColor;
    private final JCheckBox demolitionChargeHazardOutline = new JCheckBox(
          Messages.getString("CommonSettingsDialog.demolitionChargeHazardOutline"));
    private ColourSelectorButton csbBoardTextColor;
    private ColourSelectorButton csbBoardSpaceTextColor;
    private ColourSelectorButton csbMapSheetColor;
    private JSpinner attackArrowTransparency;
    private JSpinner ecmTransparency;
    private JSpinner movePathPersistenceOnMiniMap;
    private JSpinner buttonsPerRow;
    private JSpinner playersRemainingToShow;

    private JComboBox<String> tmmPipModeCbo;
    private final JCheckBox darkenMapAtNight =
          new JCheckBox(Messages.getString("CommonSettingsDialog.darkenMapAtNight"));
    private final JCheckBox translucentHiddenUnits = new JCheckBox(Messages.getString(
          "CommonSettingsDialog.translucentHiddenUnits"));

    // Tactical Overlay Options
    private final JCheckBox fovInsideEnabled = new JCheckBox(Messages.getString(
          "TacticalOverlaySettingsDialog.FovInsideEnabled"));
    private JSlider fovHighlightAlpha;
        private JSpinner fovHighlightOpacityPercent;
        private FovHighlightRingsPanel fovHighlightRingsEditor;
    private final JCheckBox fovOutsideEnabled = new JCheckBox(Messages.getString(
          "TacticalOverlaySettingsDialog.FovOutsideEnabled"));
    private JSlider fovDarkenAlpha;
        private JSpinner fovDarkenOpacityPercent;
        private JSpinner fovStripesSpinner;
    private JCheckBox fovGrayscaleEnabled;

    // Labels (there to make it possible to disable them)
    private JLabel darkenAlphaLabel;
    private JLabel numStripesLabel;
        private JLabel fovHighlightRangesLabel;
    private JLabel highlightAlphaLabel;

    private JLabel stampFormatLabel;
    private JLabel gameLogFilenameLabel;

    private final JCheckBox gameSummaryBV =
          new JCheckBox(Messages.getString("CommonSettingsDialog.gameSummaryBV.name"));
    private final JCheckBox gameSummaryMM =
          new JCheckBox(Messages.getString("CommonSettingsDialog.gameSummaryMM.name"));
    private final JComboBox<String> gifGameSummaryRecording = new JComboBox<>(new String[] {
          Messages.getString("CommonSettingsDialog.gifGameSummaryRecording.always"),
          Messages.getString("CommonSettingsDialog.gifGameSummaryRecording.ask"),
          Messages.getString("CommonSettingsDialog.gifGameSummaryRecording.never") });
    private final JCheckBox showUnitDisplayNamesOnMinimap = new JCheckBox(Messages.getString(
          "CommonSettingsDialog.showUnitDisplayNamesOnMinimap.name"));
    private JComboBox<String> skinFiles;
    private JComboBox<UITheme> uiThemes;

    // Advanced Settings
    private JList<AdvancedOptionData> advancedKeys;
    private int advancedKeyIndex = 0;
    private JTextField advancedValue;

    // Button order
    private DefaultListModel<StatusBarPhaseDisplay.PhaseCommand> movePhaseCommands;
    private DefaultListModel<StatusBarPhaseDisplay.PhaseCommand> deployPhaseCommands;
    private DefaultListModel<StatusBarPhaseDisplay.PhaseCommand> firingPhaseCommands;
    private DefaultListModel<StatusBarPhaseDisplay.PhaseCommand> physicalPhaseCommands;
    private DefaultListModel<StatusBarPhaseDisplay.PhaseCommand> targetingPhaseCommands;

    // Unit Display order
    private final DefaultListModel<String> unitDisplayNonTabbed = new DefaultListModel<>();
    private final StatusBarPhaseDisplay.CommandComparator cmdComp = new StatusBarPhaseDisplay.CommandComparator();
    private final PhaseCommandListMouseAdapter cmdMouseAdaptor = new PhaseCommandListMouseAdapter();

    private JComboBox<String> tileSetChoice;
    private List<String> tileSets;
    private MMComboBox<String> minimapTheme;

        private final SettingsCheckBox keyBindTabNavigation = createKeyBindTabNavigationControl();
    private final SettingsButton defaultKeyBindButton = createKeyBindResetButton();

    private ColourSelectorButton csbUnitTooltipFGColor;
    private ColourSelectorButton csbUnitTooltipLightFGColor;
    private ColourSelectorButton csbUnitTooltipBuildingFGColor;
    private ColourSelectorButton csbUnitTooltipAltFGColor;
    private ColourSelectorButton csbUnitTooltipBlockFGColor;
    private ColourSelectorButton csbUnitTooltipTerrainFGColor;
    private ColourSelectorButton csbUnitTooltipBGColor;
    private ColourSelectorButton csbUnitTooltipBuildingBGColor;
    private ColourSelectorButton csbUnitTooltipAltBGColor;
    private ColourSelectorButton csbUnitTooltipBlockBGColor;
    private ColourSelectorButton csbUnitTooltipTerrainBGColor;
    private ColourSelectorButton csbUnitTooltipHighlightColor;
    private ColourSelectorButton csbUnitTooltipWeaponColor;
    private ColourSelectorButton csbUnitTooltipQuirkColor;

    private ColourSelectorButton csbUnitDisplayHeatLevel1;
    private ColourSelectorButton csbUnitDisplayHeatLevel2;
    private ColourSelectorButton csbUnitDisplayHeatLevel3;
    private ColourSelectorButton csbUnitDisplayHeatLevel4;
    private ColourSelectorButton csbUnitDisplayHeatLevel5;
    private ColourSelectorButton csbUnitDisplayHeatLevel6;
    private ColourSelectorButton csbUnitDisplayHeatLevelOverheat;

    private JSpinner unitDisplayHeatLevel1Spinner;
    private JSpinner unitDisplayHeatLevel2Spinner;
    private JSpinner unitDisplayHeatLevel3Spinner;
    private JSpinner unitDisplayHeatLevel4Spinner;
    private JSpinner unitDisplayHeatLevel5Spinner;
    private JSpinner unitDisplayHeatLevel6Spinner;
    private JComboBox<String> unitTooltipSeenByCbo;
    private JSpinner unitDisplayWeaponListHeightSpinner;

    private ColourSelectorButton csbUnitTooltipArmorMiniIntact;
    private ColourSelectorButton csbUnitTooltipArmorMiniPartial;
    private ColourSelectorButton csbUnitTooltipArmorMiniDamaged;
    private JComboBox<TooltipSymbolOption> unitTooltipArmorMiniArmorCharCbo;
    private JComboBox<TooltipSymbolOption> unitTooltipArmorMiniInternalStructureCharCbo;
    private JComboBox<TooltipSymbolOption> unitTooltipArmorMiniCriticalCharCbo;
    private JComboBox<TooltipSymbolOption> unitTooltipArmorMiniDestroyedCharCbo;
    private JComboBox<TooltipSymbolOption> unitTooltipArmorMiniCapArmorCharCbo;
    private JComboBox<String> unitTooltipFontSizeModCbo;
    private JSpinner unitTooltipArmorMiniUnitsPerBlockSpinner;
    private JSpinner unitDisplayMekArmorLargeFontSizeSpinner;
    private JSpinner unitDisplayMekArmorMediumFontSizeSpinner;
    private JSpinner unitDisplayMekArmorSmallFontSizeSpinner;
    private JSpinner unitDisplayMekLargeFontSizeSpinner;
    private JSpinner unitDisplayMekMediumFontSizeSpinner;

    // Auto Display
    private JComboBox<String> unitDisplayAutoDisplayReportCombo;
    private JComboBox<String> unitDisplayAutoDisplayNonReportCombo;
    private JComboBox<String> miniMapAutoDisplayReportCombo;
    private JComboBox<String> miniMapAutoDisplayNonReportCombo;
    private JComboBox<String> miniReportAutoDisplayReportCombo;
    private JComboBox<String> miniReportAutoDisplayNonReportCombo;
    private JComboBox<String> playerListAutoDisplayReportCombo;
    private JComboBox<String> playerListAutoDisplayNonReportCombo;
    private JComboBox<String> forceDisplayAutoDisplayReportCombo;
    private JComboBox<String> forceDisplayAutoDisplayNonReportCombo;
    private JComboBox<String> botCommandsAutoDisplayReportCombo;
    private JComboBox<String> botCommandsAutoDisplayNonReportCombo;
    private JCheckBox displayMoveDisplayDuringMovePhases;
    private JCheckBox displayFireDisplayDuringFirePhases;

    // Report
    private final JCheckBox chkReportShowPlayers = new JCheckBox(Messages.getString(
          "CommonSettingsDialog.showReportPlayerList"));
    private final JCheckBox chkReportShowUnits = new JCheckBox(Messages.getString(
          "CommonSettingsDialog.showReportUnitList"));
    private final JCheckBox chkReportShowKeywords = new JCheckBox(Messages.getString(
          "CommonSettingsDialog.showReportKeywordsList"));
    private JTextArea reportKeywordsTextArea;
    private JTextArea reportFilterKeywordsTextArea;
    private final JCheckBox chkReportShowFilter = new JCheckBox(Messages.getString(
          "CommonSettingsDialog.showReportFilterList"));
    private ColourSelectorButton csbReportLinkColor;
    private ColourSelectorButton csbReportSuccessColor;
    private ColourSelectorButton csbReportMissColor;
    private ColourSelectorButton csbReportInfoColor;
    private JComboBox<String> fontTypeChooserReportFont = new JComboBox<>();
    private final JCheckBox showReportSprites = new JCheckBox(Messages.getString(
          "CommonSettingsDialog.showReportSprites"));

    private ColourSelectorButton csbUnitOverviewTextShadowColor;
    private ColourSelectorButton csbUnitOverviewConditionShadowColor;

    private ColourSelectorButton csbPlanetaryConditionsColorTitle;
    private ColourSelectorButton csbPlanetaryConditionsColorText;
    private ColourSelectorButton csbPlanetaryConditionsColorCold;
    private ColourSelectorButton csbPlanetaryConditionsColorHot;
    private ColourSelectorButton csbPlanetaryConditionsColorBackground;
    private final JCheckBox planetaryConditionsShowDefaults = new JCheckBox(Messages.getString(
          "CommonSettingsDialog.planetaryConditionsShowDefaults"));
    private final JCheckBox planetaryConditionsShowHeader = new JCheckBox(Messages.getString(
          "CommonSettingsDialog.planetaryConditionsShowHeader"));
    private final JCheckBox planetaryConditionsShowLabels = new JCheckBox(Messages.getString(
          "CommonSettingsDialog.planetaryConditionsShowLabels"));
    private final JCheckBox planetaryConditionsShowValues = new JCheckBox(Messages.getString(
          "CommonSettingsDialog.planetaryConditionsShowValues"));
    private final JCheckBox planetaryConditionsShowIndicators = new JCheckBox(Messages.getString(
          "CommonSettingsDialog.planetaryConditionsShowIndicators"));
    private JSpinner planetaryConditionsBackgroundTransparency;

    private final JCheckBox toastEnabled = new JCheckBox(Messages.getString("CommonSettingsDialog.toastEnabled"));
    private final JCheckBox toastReportEvents = new JCheckBox(Messages.getString(
          "CommonSettingsDialog.toastReportEvents"));
    private JSpinner toastDurationSpinner;
    private JLabel toastDurationLabel;
    private JSpinner toastDripSpinner;
    private JLabel toastDripLabel;

    private JSlider traceOverlayTransparencySlider;
    private JSlider traceOverlayScaleSlider;
    private JSlider traceOverlayOriginXSlider;
    private JSlider traceOverlayOriginYSlider;
    private JTextField traceOverlayImageFile;

    /**
     * Maps command strings to a JTextField for updating the modifier for the command.
     */
    private Map<String, JTextField> cmdModifierMap;

    /**
     * Maps command strings to a JTextField for updating the key for the command.
     */
    private Map<String, JTextField> cmdKeyMap;

    /** Maps command strings to an Integer for updating the key for the command. */
    private Map<String, Integer> cmdKeyCodeMap;

    private ClientGUI clientgui = null;

    private static final GUIPreferences GUIP = GUIPreferences.getInstance();
    private static final ClientPreferences CLIENT_PREFERENCES = PreferenceManager.getClientPreferences();
    private static final UnitDisplayOrderPreferences UNIT_DISPLAY_ORDER_PREFERENCES = UnitDisplayOrderPreferences.getInstance();
    private static final ButtonOrderPreferences BOP = ButtonOrderPreferences.getInstance();

    private static final String[] LOCALE_CHOICES = { "en", "de", "ru", "es" };

    private static final Dimension DEPENDENT_INSET = new Dimension(25, 0);

    /** Shortest and longest display/spacing time the toast spinners allow, in seconds. */
    private static final int MIN_TOAST_SECONDS = 1;
    private static final int MAX_TOAST_SECONDS = 10;
    private static final int BUTTON_GAP = 8;
    /** Wrap width for the multi-line warning under the toast on/off checkbox, before GUI scaling. */
    private static final int TOAST_WARNING_WIDTH_PX = 480;

    /**
     * Clamps a persisted toast-timing value into the range the toast spinners allow. Guards against a
     * hand-edited or corrupted preferences file: {@link SpinnerNumberModel}'s constructor throws an
     * {@link IllegalArgumentException} when its initial value falls outside {@code [minimum, maximum]},
     * which would crash the Overlays tab as it is built.
     *
     * @param seconds the stored timing value in seconds, possibly out of range
     *
     * @return the value clamped to {@code [MIN_TOAST_SECONDS, MAX_TOAST_SECONDS]}
     */
    private static int clampToastSeconds(int seconds) {
        return Math.min(MAX_TOAST_SECONDS, Math.max(MIN_TOAST_SECONDS, seconds));
    }

    // Save some values to restore them when the dialog is canceled
    private boolean savedFovHighlight;
    private boolean savedFovDarken;
    private boolean savedFovGrayscale;
    private boolean savedAOHexShadows;
    private boolean savedShadowMap;
    private boolean savedHexInclines;
    private boolean savedLevelHighlight;
    private boolean savedFloatingIso;
    private boolean savedMmSymbol;
    private boolean savedDrawFacingArrowsOnMiniMap;
    private boolean savedDrawSensorRangeOnMiniMap;
    private boolean savedPaintBorders;
    private boolean savedTeamColoring;
    private boolean savedDockOnLeft;
    private boolean savedDockMultipleOnYAxis;
    private boolean savedUseCamoOverlay;
    private boolean savedUnitLabelBorder;
    private boolean savedShowDamageDecal;
    private boolean savedShowDamageLabel;
    private boolean savedHighQualityGraphics;
    private String savedFovHighlightRingsRadii;
    private String savedFovHighlightRingsColors;
    private int savedFovHighlightAlpha;
    private int savedFovDarkenAlpha;
    private int savedNumStripesSlider;
    private int savedMovePathPersistenceOnMiniMap;

    HashMap<String, String> savedAdvancedOpt = new HashMap<>();

    /**
     * Constructs the Client Settings Dialog with a {@link ClientGUI} (used within the client, i.e. in lobby and game).
     */
    public CommonSettingsDialog(JFrame owner, ClientGUI cg) {
        this(owner);
        clientgui = cg;
    }

    /**
     * Constructs the Client Settings Dialog without a {@link ClientGUI} (used in the main menu and board editor).
     */
    public CommonSettingsDialog(JFrame owner) {
        super(owner, true, "ClientSettings", "CommonSettingsDialog.title");
        initialize();
    }

    @Override
    protected Container createCenterPane() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cancelAction();
            }
        });

        List<CommonSettingsPane.OptionPage> pages = new ArrayList<>();

        CommonSettingsPane.SectionedContent main = sectionedContent(getSettingsPanel(), "main");
        addMappedPages(pages, "main", main,
              optionPage("main.application", path("main", "main.application"), main,
                    section("main.locale", 0), section("main.scale", 1), section("main.userFiles", 2),
                    section("main.mml", 3), section("main.theme", 4)),
              optionPage("main.colours", path("main", "main.colours"), main,
                    section("main.statusColours", 5), section("main.unitColours", 6),
                    section("main.playerColours", 7)),
              optionPage("main.behavior", path("main", "main.behavior"), main,
                    section("main.interface", 8), section("main.units", 9), section("main.logging", 10),
                    section("main.privacy", 11)));

        CommonSettingsPane.SectionedContent audio = sectionedContent(getAudioPanel(), "audio");
        addMappedPages(pages, "audio", audio,
              optionPage("audio", path("audio"), audio,
                    section("audio.volume", 0), section("audio.notifications", 1)));
        pages.add(optionPage("keyBinds", path("keyBinds"), getKeyBindSections()));

        CommonSettingsPane.SectionedContent gameBoard = sectionedContent(getGameBoardPanel(), "gameBoard");
        addMappedPages(pages, "gameBoard", gameBoard,
              optionPage("gameBoard.general", path("gameBoard", "gameBoard.general"), gameBoard,
                    section("gameBoard.tileset", 0), section("gameBoard.nags", 1), section("gameBoard.actions", 2),
                    section("gameBoard.controls", 3), section("gameBoard.pathfinder", 4),
                    section("gameBoard.units", 5)),
              optionPage("gameBoard.appearance", path("gameBoard", "gameBoard.appearance"), gameBoard,
                    section("gameBoard.rendering", 6), section("gameBoard.indicators", 7),
                    section("gameBoard.movement", 8), section("gameBoard.fire", 9)),
              optionPage("gameBoard.fov", path("gameBoard", "gameBoard.fov"), gameBoard,
                    section("gameBoard.fovInside", 10), section("gameBoard.fovOutside", 11)));

        CommonSettingsPane.SectionedContent unitDisplay = sectionedContent(getUnitDisplayPanel(), "unitDisplay");
        addMappedPages(pages, "unitDisplay", unitDisplay,
              optionPage("unitDisplay.tooltips", path("unitDisplay", "unitDisplay.tooltips"), unitDisplay,
                    section("unitDisplay.tooltip", 0), section("unitDisplay.armor", 1)),
              optionPage("unitDisplay.interface", path("unitDisplay", "unitDisplay.interface"), unitDisplay,
                    section("unitDisplay.heat", 2), section("unitDisplay.order", 3),
                    section("unitDisplay.weapons", 4), section("unitDisplay.fonts", 5)));

        CommonSettingsPane.SectionedContent miniMap = sectionedContent(getMiniMapPanel(), "miniMap");
        addMappedPages(pages, "miniMap", miniMap,
              optionPage("miniMap", path("miniMap"), miniMap,
                    section("miniMap.theme", 0), section("miniMap.display", 1)));

        CommonSettingsPane.SectionedContent report = sectionedContent(getReportPanel(), "report");
        addMappedPages(pages, "report", report,
              optionPage("report", path("report"), report,
                    section("report.appearance", 0), section("report.content", 1), section("report.filter", 2)));

        CommonSettingsPane.SectionedContent overlays = sectionedContent(getOverlaysPanel(), "overlays");
        addMappedPages(pages, "overlays", overlays,
              optionPage("overlays", path("overlays"), overlays,
                    section("overlays.overview", 0), section("overlays.planetary", 1),
                    section("overlays.toasts", 2), section("overlays.trace", 3)));
        pages.add(optionPage("buttonOrder", path("buttonOrder"), getButtonOrderSections()));

        CommonSettingsPane.SectionedContent autoDisplay = sectionedContent(getPhasePanel(), "autoDisplay");
        addMappedPages(pages, "autoDisplay", autoDisplay,
              optionPage("autoDisplay", path("autoDisplay"), autoDisplay,
                    section("autoDisplay.unit", 0), section("autoDisplay.minimap", 1),
                    section("autoDisplay.report", 2), section("autoDisplay.players", 3),
                    section("autoDisplay.force", 4), section("autoDisplay.bots", 5),
                    section("autoDisplay.tabs", 6)));

        CommonSettingsPane.SectionedContent aiDisplay = sectionedContent(aiDisplayPanel(), "aiDisplay");
        addMappedPages(pages, "aiDisplay", aiDisplay,
              optionPage("aiDisplay", path("aiDisplay"), aiDisplay,
                    section("aiDisplay.resolve", 0), section("aiDisplay.bot", 1)));
        pages.add(optionPage("advanced", path("advanced"), List.of(
              optionSection("advanced.settings", getAdvancedSettingsPanel(), true))));

        return new CommonSettingsPane(pages);
    }

    private List<String> path(String... ids) {
        return Arrays.stream(ids).map(id -> Messages.getString("CommonSettingsDialog.page." + id)).toList();
    }

    private CommonSettingsPane.OptionPage optionPage(String id, List<String> path,
          CommonSettingsPane.SectionedContent content, SectionReference... references) {
        List<CommonSettingsPane.OptionSection> sections = new ArrayList<>();
        for (SectionReference reference : references) {
            if (reference.index() >= content.groups().size()) {
                throw new IllegalArgumentException("No section " + reference.index() + " for " + id);
            }
            sections.add(optionSection(reference.id(), content.groups().get(reference.index()), false));
        }
        return optionPage(id, path, sections);
    }

    private CommonSettingsPane.OptionPage optionPage(String id, List<String> path,
          List<CommonSettingsPane.OptionSection> sections) {
        return new CommonSettingsPane.OptionPage(id, path, id.replace(".", ""), sections);
    }

    static void addMappedPages(List<CommonSettingsPane.OptionPage> pages, String contentId,
          CommonSettingsPane.SectionedContent content, CommonSettingsPane.OptionPage... mappedPages) {
        List<JComponent> mappedGroups = Arrays.stream(mappedPages)
              .flatMap(page -> Objects.requireNonNull(page).sections().stream())
              .map(section -> Objects.requireNonNull(section).content())
              .toList();
        boolean mappedExactlyOnce = mappedGroups.size() == content.groups().size()
              && content.groups().stream().allMatch(group -> mappedGroups.stream()
                    .filter(mappedGroup -> mappedGroup == group)
                    .count() == 1);
        if (!mappedExactlyOnce) {
            throw new IllegalArgumentException("Every section for " + contentId + " must be mapped exactly once");
        }
        pages.addAll(List.of(mappedPages));
    }

    private CommonSettingsPane.OptionSection optionSection(String id, JComponent content, boolean advanced) {
        return new CommonSettingsPane.OptionSection(id,
              Messages.getString("CommonSettingsDialog.section." + id + ".title"),
              Messages.getString("CommonSettingsDialog.section." + id + ".summary"), content, advanced);
    }

    private CommonSettingsPane.SectionedContent sectionedContent(JComponent content, String id) {
        if (content instanceof CommonSettingsPane.SectionedContent sectionedContent) {
            return sectionedContent;
        }
        throw new IllegalArgumentException("Sectioned page content required for " + id);
    }

    private static SectionReference section(String id, int index) {
        return new SectionReference(id, index);
    }

    private record SectionReference(String id, int index) {
    }

    @Override
    protected JPanel createButtonPanel() {
        int gap = UIUtil.scaleForGUI(BUTTON_GAP);
        JButton okButton = new JButton(resources.getString("Ok.text"));
        okButton.setName("okButton");
        okButton.setToolTipText(resources.getString("Ok.toolTipText"));
        okButton.putClientProperty("FlatLaf.style",
              "background: $Button.default.background; foreground: $Button.default.foreground");
        okButton.addActionListener(this::okButtonActionPerformed);

        JButton cancelButton = new JButton(resources.getString("Cancel.text"));
        cancelButton.setName("cancelButton");
        cancelButton.setToolTipText(resources.getString("Cancel.toolTipText"));
        cancelButton.addActionListener(this::cancelActionPerformed);
        setUniformButtonSize(okButton, cancelButton);

        JPanel actionButtons = new JPanel(new FlowLayout(FlowLayout.CENTER, gap, gap));
        actionButtons.add(okButton);
        actionButtons.add(cancelButton);

        JButton legendButton = SettingsIconLegend.createLegendButton(
              Messages.getString("CommonSettingsDialog.legend.button"),
              Messages.getString("CommonSettingsDialog.legend.tooltip"),
              CommonSettingsPane.legendEntries());
        JPanel legendPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, gap, gap));
        legendPanel.add(legendButton);

        JPanel rightSpacer = new JPanel();
        rightSpacer.setOpaque(false);
        rightSpacer.setPreferredSize(new Dimension(legendPanel.getPreferredSize().width, 0));

        JPanel footer = new JPanel(new BorderLayout());
        footer.add(legendPanel, BorderLayout.WEST);
        footer.add(actionButtons, BorderLayout.CENTER);
        footer.add(rightSpacer, BorderLayout.EAST);
        return footer;
    }

    private static void setUniformButtonSize(JButton... buttons) {
        int width = Arrays.stream(buttons).mapToInt(button -> button.getPreferredSize().width).max().orElse(0);
        int height = Arrays.stream(buttons).mapToInt(button -> button.getPreferredSize().height).max().orElse(0);
        Dimension size = new Dimension(width, height);
        for (JButton button : buttons) {
            button.setPreferredSize(size);
            button.setMinimumSize(size);
        }
    }

    private JPanel getAudioPanel() {
        masterVolumeSlider = new JSlider();
        masterVolumeSlider.setMinorTickSpacing(5);
        masterVolumeSlider.setMajorTickSpacing(25);
        masterVolumeSlider.setMinimum(0);
        masterVolumeSlider.setMaximum(100);
        Hashtable<Integer, JComponent> table = new Hashtable<>();
        table.put(0, new JLabel("0%"));
        table.put(25, new JLabel("25%"));
        table.put(50, new JLabel("50%"));
        table.put(75, new JLabel("75%"));
        table.put(100, new JLabel("100%"));
        masterVolumeSlider.setLabelTable(table);
        masterVolumeSlider.setPaintTicks(true);
        masterVolumeSlider.setPaintLabels(true);
        masterVolumeSlider.setToolTipText(Messages.getString("CommonSettingsDialog.masterVolumeTT"));

        tfSoundMuteChatFileName = new JTextField(5);
        tfSoundMuteMyTurnFileName = new JTextField(5);
        tfSoundMuteOthersFileName = new JTextField(5);

        return createAudioSettingsPanel(masterVolumeLabel, masterVolumeSlider,
              soundMuteChat, tfSoundMuteChatFileName,
              soundMuteMyTurn, tfSoundMuteMyTurnFileName,
              soundMuteOthersTurn, tfSoundMuteOthersFileName);
    }

    static CommonSettingsPane.SectionedContent createAudioSettingsPanel(JLabel volumeLabel, JSlider volumeSlider,
          JCheckBox chatMute, JTextField chatSoundFile,
          JCheckBox myTurnMute, JTextField myTurnSoundFile,
          JCheckBox otherTurnsMute, JTextField otherTurnsSoundFile) {
        volumeLabel.setLabelFor(volumeSlider);
        SettingsFormPanel volumeGrid = createAudioControlGrid(
              "CommonSettingsAudioVolumeGrid", volumeLabel, volumeSlider);
          JPanel chatSoundControl = createAudioFileControl(chatSoundFile, "btnChatSoundChooser",
              Messages.getString("CommonSettingsDialog.soundMuteChat.chooser.title"));
          JPanel myTurnSoundControl = createAudioFileControl(myTurnSoundFile, "btnMyTurnSoundChooser",
              Messages.getString("CommonSettingsDialog.soundMuteMyTurn.chooser.title"));
          JPanel otherTurnsSoundControl = createAudioFileControl(otherTurnsSoundFile, "btnOtherTurnsSoundChooser",
              Messages.getString("CommonSettingsDialog.soundMuteOthersTurn.chooser.title"));
        SettingsFormPanel notificationGrid = createAudioControlGrid(
              "CommonSettingsAudioNotificationGrid",
              chatMute, chatSoundControl,
              myTurnMute, myTurnSoundControl,
              otherTurnsMute, otherTurnsSoundControl);
        return new CommonSettingsPane.SectionedContent(List.of(volumeGrid, notificationGrid));
    }

        static JPanel createAudioFileControl(JTextField field, String buttonName, String chooserTitle) {
          JButton chooser = applicationIconButton(buttonName, 0xE2C8, chooserTitle);
          chooser.addActionListener(event -> fileChoose(field, chooser, chooserTitle, false));
          return applicationPathControl(field, chooser);
        }

    static SettingsFormPanel createAudioControlGrid(String name, JComponent... controls) {
        SettingsFormPanel grid = new SettingsFormPanel(name,
              SettingsFormPanel.DEFAULT_LABEL_WIDTH, 0);
        grid.addEqualWidthComponentGrid(2, controls);
        return grid;
    }

    static SettingsFormPanel createGameBoardFieldGrid(String name, JComponent... labelsAndControls) {
        if ((labelsAndControls.length % 2) != 0) {
            throw new IllegalArgumentException("Game Board field grids require label/control pairs");
        }
        SettingsFormPanel grid = new SettingsFormPanel(name, SettingsFormPanel.DEFAULT_LABEL_WIDTH, 0);
        for (int index = 0; index < labelsAndControls.length; index += 2) {
            JComponent label = labelsAndControls[index];
            JComponent control = labelsAndControls[index + 1];
            if (label instanceof JLabel swingLabel) {
                swingLabel.setLabelFor(control);
            }
            grid.addEqualWidthComponentGrid(2, label, control);
        }
        return grid;
    }

    static SettingsFormPanel createGameBoardOptionGrid(String name, JComponent... options) {
        SettingsFormPanel grid = new SettingsFormPanel(name, SettingsFormPanel.DEFAULT_LABEL_WIDTH, 0);
        grid.addEqualWidthComponentGrid(2, options);
        return grid;
    }

    static SettingsFormPanel createGameBoardControlsGrid(String name,
          JLabel buttonsPerRowLabel, JComponent buttonsPerRowControl,
          JLabel playersRemainingLabel, JComponent playersRemainingControl,
          JComponent... options) {
        SettingsFormPanel grid = new SettingsFormPanel(name, SettingsFormPanel.DEFAULT_LABEL_WIDTH, 0);
        buttonsPerRowLabel.setLabelFor(buttonsPerRowControl);
        grid.addEqualWidthComponentGrid(2, buttonsPerRowLabel, buttonsPerRowControl);
        playersRemainingLabel.setLabelFor(playersRemainingControl);
        grid.addEqualWidthComponentGrid(2, playersRemainingLabel, playersRemainingControl);
        grid.addEqualWidthComponentGrid(2, options);
        return grid;
    }

    @SafeVarargs
    static SettingsFormPanel createGameBoardGroupedOptionGrid(String name,
          List<? extends JComponent>... optionGroups) {
        SettingsFormPanel grid = new SettingsFormPanel(name, SettingsFormPanel.DEFAULT_LABEL_WIDTH, 0);
        for (List<? extends JComponent> optionGroup : optionGroups) {
            grid.addEqualWidthComponentGrid(2, optionGroup.toArray(JComponent[]::new));
        }
        return grid;
    }

        static JPanel createGameBoardFovField(String name, JLabel label, JComponent control) {
          JPanel field = new JPanel(new BorderLayout(0, UIUtil.scaleForGUI(4)));
          field.setName("pnl" + name);
          field.setOpaque(false);
                    Object labelTarget = control.getClientProperty("CommonSettingsDialog.labelFor");
                    label.setLabelFor(labelTarget instanceof Component component ? component : control);
          field.add(label, BorderLayout.NORTH);
          field.add(control, BorderLayout.CENTER);
          return field;
        }

                static JPanel createGameBoardFovOpacityControl(JSlider slider, JSpinner percentSpinner) {
                    JPanel control = new JPanel(new BorderLayout(UIUtil.scaleForGUI(8), 0));
                    control.setOpaque(false);
                    slider.setPaintTicks(false);
                    slider.setPaintLabels(false);
                    control.add(slider, BorderLayout.CENTER);

                    JPanel value = new JPanel(new BorderLayout(UIUtil.scaleForGUI(3), 0));
                    value.setOpaque(false);
                    JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor) percentSpinner.getEditor();
                    editor.getTextField().setColumns(3);
                    editor.getTextField().setHorizontalAlignment(SwingConstants.RIGHT);
                    value.add(percentSpinner, BorderLayout.CENTER);
                        JLabel percentLabel = new JLabel("%");
                        percentSpinner.addPropertyChangeListener("enabled",
                            event -> percentLabel.setEnabled(percentSpinner.isEnabled()));
                        value.add(percentLabel, BorderLayout.EAST);
                    control.add(value, BorderLayout.EAST);
                    control.putClientProperty("CommonSettingsDialog.labelFor", slider);

                    boolean[] synchronizing = { false };
                    slider.addChangeListener(event -> {
                            if (!synchronizing[0]) {
                                    synchronizing[0] = true;
                                    percentSpinner.setValue(fovAlphaToPercent(slider.getValue()));
                                    synchronizing[0] = false;
                            }
                    });
                    percentSpinner.addChangeListener(event -> {
                            if (!synchronizing[0]) {
                                    synchronizing[0] = true;
                                    slider.setValue(fovPercentToAlpha((int) percentSpinner.getValue()));
                                    synchronizing[0] = false;
                            }
                    });
                    percentSpinner.setValue(fovAlphaToPercent(slider.getValue()));
                    return control;
                }

                static int fovAlphaToPercent(int alpha) {
                    return Math.round(Math.clamp(alpha, 0, 255) * 100.0f / 255.0f);
                }

                static int fovPercentToAlpha(int percent) {
                    return Math.round(Math.clamp(percent, 0, 100) * 255.0f / 100.0f);
                }

        static SettingsFormPanel createGameBoardFovInsideGrid(JCheckBox enabled,
                        JLabel opacityLabel, JComponent opacity,
                        JLabel rangesLabel, JComponent ranges) {
              SettingsFormPanel fields = new SettingsFormPanel("CommonSettingsGameBoardFovInsideFields",
              SettingsFormPanel.DEFAULT_LABEL_WIDTH, 0);
              fields.addEqualWidthComponentGrid(2,
                                    createGameBoardFovField("FovInsideOpacity", opacityLabel, opacity));
                            JPanel rangesField = createGameBoardFovField("FovInsideRanges", rangesLabel, ranges);
                            Dimension rangesSize = rangesField.getPreferredSize();
                            rangesField.setPreferredSize(new Dimension(fields.getPreferredSize().width, rangesSize.height));

              SettingsFormPanel grid = new SettingsFormPanel("CommonSettingsGameBoardFovInsideGrid", 0, 0);
          grid.addCheckBox(enabled);
              grid.addFullWidthComponent(fields);
                            grid.addFullWidthComponent(rangesField);
          return grid;
        }

        static SettingsFormPanel createGameBoardFovOutsideGrid(JCheckBox enabled, JCheckBox grayscale,
                        JLabel opacityLabel, JComponent opacity,
                        JLabel stripesLabel, JComponent stripes) {
              SettingsFormPanel options = new SettingsFormPanel("CommonSettingsGameBoardFovOutsideOptions",
              SettingsFormPanel.DEFAULT_LABEL_WIDTH, 0);
              options.addEqualWidthComponentGrid(2, enabled, grayscale);

              SettingsFormPanel fields = new SettingsFormPanel("CommonSettingsGameBoardFovOutsideFields",
                  SettingsFormPanel.DEFAULT_LABEL_WIDTH, 0);
              fields.addEqualWidthComponentGrid(2,
              createGameBoardFovField("FovOutsideOpacity", opacityLabel, opacity),
              createGameBoardFovField("FovOutsideStripes", stripesLabel, stripes));

              SettingsFormPanel grid = new SettingsFormPanel("CommonSettingsGameBoardFovOutsideGrid", 0, 0);
              grid.addFullWidthComponent(options);
              grid.addFullWidthComponent(fields);
          return grid;
        }

        private JPanel getGameBoardPanel() {
        List<List<Component>> comps = new ArrayList<>();
        ArrayList<Component> row;

        // Tileset
        JLabel tileSetChoiceLabel = new JLabel(Messages.getString("CommonSettingsDialog.tileset"));
        tileSetChoice = new JComboBox<>();
        tileSetChoice.setMaximumSize(new Dimension(400, tileSetChoice.getMaximumSize().height));
          comps.add(List.of(createGameBoardFieldGrid("CommonSettingsGameBoardTilesetGrid",
              tileSetChoiceLabel, tileSetChoice)));
        addLineSpacer(comps);

          configureCheckBox(nagForNoAction, Messages.getString("CommonSettingsDialog.nagForNoAction.tooltip"));
          List.of(nagForPSR, nagForMASC, nagForSprint).forEach(checkBox -> configureCheckBox(checkBox, null));
          configureCheckBox(nagForCrushingBuildings,
              Messages.getString("CommonSettingsDialog.nagForCrushingBuildings.tooltip"));
          configureCheckBox(nagForMechanicalJumpFallDamage,
              Messages.getString("CommonSettingsDialog.nagForMechanicalJumpFallDamage.tooltip"));
          configureCheckBox(nagForWiGELanding,
              Messages.getString("CommonSettingsDialog.nagForWiGELanding.tooltip"));
          configureCheckBox(nagForNoUnJamRAC,
              Messages.getString("CommonSettingsDialog.nagForUnJamRAC.tooltip"));
          configureCheckBox(nagForLaunchDoors,
              Messages.getString("CommonSettingsDialog.nagForLaunchDoors.tooltip"));
          configureCheckBox(nagForOverheat,
              Messages.getString("CommonSettingsDialog.nagForOverheat.tooltip"));
          configureCheckBox(nagForDishonor,
              Messages.getString("CommonSettingsDialog.nagForDishonor.tooltip"));
          configureCheckBox(nagForOddSizedBoard,
              Messages.getString("CommonSettingsDialog.nagForOddSizedBoard.tooltip"));
          comps.add(List.of(createGameBoardOptionGrid("CommonSettingsGameBoardConfirmationsGrid",
              nagForNoAction, nagForPSR, nagForMASC, nagForSprint,
              nagForCrushingBuildings, nagForMechanicalJumpFallDamage, nagForWiGELanding,
              nagForNoUnJamRAC, nagForLaunchDoors, nagForOverheat, nagForDishonor, nagForOddSizedBoard)));

        addLineSpacer(comps);

          List.of(getFocus, autoEndFiring).forEach(checkBox -> configureCheckBox(checkBox, null));
          configureCheckBox(autoDeclareSearchlight,
              Messages.getString("CommonSettingsDialog.autoDeclareSearchlight.tooltip"));
          configureCheckBox(moveDefaultClimbMode,
              Messages.getString("CommonSettingsDialog.moveDefaultClimbMode.tooltip"));
          comps.add(List.of(createGameBoardOptionGrid("CommonSettingsGameBoardTurnActionsGrid",
              getFocus, autoEndFiring, autoDeclareSearchlight, moveDefaultClimbMode)));

        addLineSpacer(comps);

          buttonsPerRow = createIntegerSpinner(GUIP.getButtonsPerRow(), 1, 1);
        JLabel buttonsPerRowLabel = new JLabel(Messages.getString("CommonSettingsDialog.buttonsPerRow"));
        buttonsPerRow.setToolTipText(Messages.getString("CommonSettingsDialog.buttonsPerRow.tooltip"));

          playersRemainingToShow = createIntegerSpinner(GUIP.getPlayersRemainingToShow(), 0, 1);
        JLabel playersRemainingToShowLabel =
              new JLabel(Messages.getString("CommonSettingsDialog.playersRemainingToShow"));
        playersRemainingToShow.setToolTipText(
              Messages.getString("CommonSettingsDialog.playersRemainingToShow.tooltip"));

                List.of(mouseWheelZoom, mouseWheelZoomFlip).forEach(checkBox -> configureCheckBox(checkBox, null));
        String msg_tooltip = Messages.getString("CommonSettingsDialog.gameSummaryBV.tooltip",
              Configuration.gameSummaryImagesBVDir());
                configureCheckBox(gameSummaryBV, msg_tooltip);
                comps.add(List.of(createGameBoardControlsGrid("CommonSettingsGameBoardControlsGrid",
              buttonsPerRowLabel, buttonsPerRow,
              playersRemainingToShowLabel, playersRemainingToShow,
              mouseWheelZoom, mouseWheelZoomFlip, gameSummaryBV)));

        addLineSpacer(comps);

        JLabel maxPathfinderTimeLabel = new JLabel(Messages.getString("CommonSettingsDialog.pathFiderTimeLimit"));
        maxPathfinderTime = createIntegerSpinner(CLIENT_PREFERENCES.getMaxPathfinderTime(), 1, 1);
                comps.add(List.of(createGameBoardFieldGrid("CommonSettingsGameBoardPathfinderGrid",
              maxPathfinderTimeLabel, maxPathfinderTime)));

        addLineSpacer(comps);

                List.of(showDamageLevel, showDamageDecal, showUnitId)
              .forEach(checkBox -> configureCheckBox(checkBox, null));
                configureCheckBox(entityOwnerColor, Messages.getString("CommonSettingsDialog.entityOwnerColor.tooltip"));
                configureCheckBox(useSoftCenter, Messages.getString("CommonSettingsDialog.useSoftCenter.tooltip"));
                configureCheckBox(useAutoCenter, Messages.getString("CommonSettingsDialog.useAutoCenter.tooltip"));
                configureCheckBox(useAutoSelectNext, Messages.getString("CommonSettingsDialog.useAutoSelectNext.tooltip"));

        csbUnitTextColor = new ColourSelectorButton(Messages.getString("CommonSettingsDialog.colors.UnitTextColor"));
        csbUnitTextColor.setColour(GUIP.getUnitTextColor());
        csbUnitValidColor = new ColourSelectorButton(Messages.getString("CommonSettingsDialog.colors.UnitValidColor"));
        csbUnitValidColor.setColour(GUIP.getUnitValidColor());
        csbUnitSelectedColor = new ColourSelectorButton(Messages.getString(
              "CommonSettingsDialog.colors.UnitSelectedColor"));
        csbUnitSelectedColor.setColour(GUIP.getUnitSelectedColor());
        comps.add(List.of(createGameBoardGroupedOptionGrid("CommonSettingsGameBoardUnitsGrid",
              List.of(showDamageLevel, showDamageDecal, showUnitId, entityOwnerColor,
                    useSoftCenter, useAutoCenter, useAutoSelectNext),
              List.of(csbUnitTextColor, csbUnitValidColor, csbUnitSelectedColor))));

        addLineSpacer(comps);

          List.of(animateMove, showWrecks).forEach(checkBox -> configureCheckBox(checkBox, null));
          configureCheckBox(chkHighQualityGraphics,
              Messages.getString("CommonSettingsDialog.highQualityGraphics.tooltip"));
          configureCheckBox(chkHighPerformanceGraphics,
              Messages.getString("CommonSettingsDialog.highPerformanceGraphics.tooltip"));
          configureCheckBox(showMapSheets, null);
        csbMapSheetColor = new ColourSelectorButton(Messages.getString("CommonSettingsDialog.colors.MapSheetColor"));
        csbMapSheetColor.setColour(GUIP.getMapsheetColor());

          configureCheckBox(aOHexShadows, Messages.getString("CommonSettingsDialog.aOHexSHadows.tooltip"));
          configureCheckBox(shadowMap, Messages.getString("CommonSettingsDialog.useShadowMap.tooltip"));
          configureCheckBox(hexInclines, Messages.getString("CommonSettingsDialog.useInclines.tooltip"));
          configureCheckBox(levelHighlight, Messages.getString("CommonSettingsDialog.levelHighlight.tooltip"));
          configureCheckBox(floatingIso, Messages.getString("CommonSettingsDialog.floatingIso.tooltip"));
          configureCheckBox(darkenMapAtNight, null);
        darkenMapAtNight.setSelected(GUIP.getDarkenMapAtNight());
          configureCheckBox(translucentHiddenUnits, null);
        translucentHiddenUnits.setSelected(GUIP.getTranslucentHiddenUnits());
          configureCheckBox(artilleryDisplayMisses,
              Messages.getString("CommonSettingsDialog.hexes.ShowArtilleryMisses.tooltip"));

        // Artillery and bomb display choices
        artilleryDisplayMisses.setSelected(GUIP.getShowArtilleryMisses());
          configureCheckBox(artilleryDisplayDriftedHits,
              Messages.getString("CommonSettingsDialog.hexes.ShowArtilleryDriftedHits.tooltip"));
        artilleryDisplayDriftedHits.setSelected(GUIP.getShowArtilleryDrifts());
          configureCheckBox(artilleryDisplayDriftArrows,
              Messages.getString("CommonSettingsDialog.hexes.ShowArtilleryDriftArrows.tooltip"));
        artilleryDisplayDriftArrows.setSelected(GUIP.getShowArtilleryDriftArrows());
          configureCheckBox(bombsDisplayMisses,
              Messages.getString("CommonSettingsDialog.hexes.ShowBombMisses.tooltip"));
        bombsDisplayMisses.setSelected(GUIP.getShowBombMisses());
          configureCheckBox(bombsDisplayDrifts,
              Messages.getString("CommonSettingsDialog.hexes.ShowBombDrifts.tooltip"));
        bombsDisplayDrifts.setSelected(GUIP.getShowBombDrifts());

        csbBoardTextColor = new ColourSelectorButton(Messages.getString("CommonSettingsDialog.colors.BoardTextColor"));
        csbBoardTextColor.setColour(GUIP.getBoardTextColor());

        csbBoardSpaceTextColor = new ColourSelectorButton(Messages.getString(
              "CommonSettingsDialog.colors.BoardSpaceTextColor"));
        csbBoardSpaceTextColor.setColour(GUIP.getBoardSpaceTextColor());

        csbBuildingTextColor = new ColourSelectorButton(Messages.getString(
              "CommonSettingsDialog.colors.BuildingTextColor"));
        csbBuildingTextColor.setColour(GUIP.getBuildingTextColor());

        csbLowFoliageColor =
              new ColourSelectorButton(Messages.getString("CommonSettingsDialog.colors.LowFoliageColor"));
        csbLowFoliageColor.setColour(GUIP.getLowFoliageColor());

        csbDemolitionChargeColor = new ColourSelectorButton(Messages.getString(
              "CommonSettingsDialog.colors.DemolitionChargeColor"));
        csbDemolitionChargeColor.setColour(GUIP.getDemolitionChargeColor());
          configureCheckBox(demolitionChargeHazardOutline,
              Messages.getString("CommonSettingsDialog.demolitionChargeHazardOutline.tooltip"));

          comps.add(List.of(createGameBoardGroupedOptionGrid("CommonSettingsGameBoardRenderingGrid",
              List.of(animateMove, showWrecks, chkHighQualityGraphics, chkHighPerformanceGraphics),
              List.of(showMapSheets, aOHexShadows, shadowMap, hexInclines, levelHighlight, floatingIso,
                  darkenMapAtNight, translucentHiddenUnits,
                  artilleryDisplayMisses, artilleryDisplayDriftedHits, artilleryDisplayDriftArrows,
                  bombsDisplayMisses, bombsDisplayDrifts),
              List.of(csbMapSheetColor, csbBoardTextColor, csbBoardSpaceTextColor, csbBuildingTextColor,
                  csbLowFoliageColor, csbDemolitionChargeColor),
              List.of(demolitionChargeHazardOutline))));

        addLineSpacer(comps);

        SpinnerNumberModel mAttackArrowTransparency = new SpinnerNumberModel(GUIP.getAttackArrowTransparency(),
              0,
              256,
              1);
        attackArrowTransparency = new JSpinner(mAttackArrowTransparency);
        attackArrowTransparency.setMaximumSize(new Dimension(150, 40));
        JLabel attackArrowTransparencyLabel = new JLabel(Messages.getString(
              "CommonSettingsDialog.attackArrowTransparency"));
        attackArrowTransparency.setToolTipText(
              Messages.getString("CommonSettingsDialog.attackArrowTransparency.tooltip"));

        SpinnerNumberModel mECMTransparency = new SpinnerNumberModel(GUIP.getECMTransparency(), 0, 256, 1);
        ecmTransparency = new JSpinner(mECMTransparency);
        ecmTransparency.setMaximumSize(new Dimension(150, 40));
        JLabel ecmTransparencyLabel = new JLabel(Messages.getString("CommonSettingsDialog.ecmTransparency"));
        ecmTransparency.setToolTipText(Messages.getString("CommonSettingsDialog.ecmTransparency.tooltip"));

        tmmPipModeCbo = new JComboBox<>();
        tmmPipModeCbo.addItem(Messages.getString("CommonSettingsDialog.tmmPipMode.NoPips"));
        tmmPipModeCbo.addItem(Messages.getString("CommonSettingsDialog.tmmPipMode.WhitePips"));
        tmmPipModeCbo.addItem(Messages.getString("CommonSettingsDialog.tmmPipMode.ColoredPips"));
        tmmPipModeCbo.addItem(Messages.getString("CommonSettingsDialog.tmmPipMode.WhitePipsBigger"));
        tmmPipModeCbo.addItem(Messages.getString("CommonSettingsDialog.tmmPipMode.ColoredPipsBigger"));
        tmmPipModeCbo.setSelectedIndex(GUIP.getTMMPipMode());
        JLabel tmmPipModeLabel = new JLabel(Messages.getString("CommonSettingsDialog.tmmPipMode"));
          comps.add(List.of(createGameBoardFieldGrid("CommonSettingsGameBoardIndicatorsGrid",
              attackArrowTransparencyLabel, attackArrowTransparency,
              ecmTransparencyLabel, ecmTransparency,
              tmmPipModeLabel, tmmPipModeCbo)));

        addLineSpacer(comps);

        fontTypeChooserMoveFont = new JComboBox<>(new Vector<>(FontHandler.getAvailableNonSymbolFonts()));
        fontTypeChooserMoveFont.setSelectedItem(GUIP.getMoveFontType());

        JLabel moveFontTypeLabel = new JLabel(Messages.getString("CommonSettingsDialog.moveFontType"));

        moveFontSize = createIntegerSpinner(GUIP.getMoveFontSize(), 1, 1);
        JLabel moveFontSizeLabel = new JLabel(Messages.getString("CommonSettingsDialog.moveFontSize"));

        fontStyleChooserMoveFont.addItem(Messages.getString("Plain"));
        fontStyleChooserMoveFont.addItem(Messages.getString("Bold"));
        fontStyleChooserMoveFont.addItem(Messages.getString("Italic"));
        JLabel moveFontStyleLabel = new JLabel(Messages.getString("CommonSettingsDialog.moveFontStyle"));
        fontStyleChooserMoveFont.setSelectedIndex(GUIP.getMoveFontStyle());
        csbMoveDefaultColor =
              new ColourSelectorButton(Messages.getString("CommonSettingsDialog.colors.MoveDefaultColor"));
        csbMoveDefaultColor.setColour(GUIP.getMoveDefaultColor());

        csbMoveIllegalColor =
              new ColourSelectorButton(Messages.getString("CommonSettingsDialog.colors.MoveIllegalColor"));
        csbMoveIllegalColor.setColour(GUIP.getMoveIllegalColor());

        csbMoveJumpColor = new ColourSelectorButton(Messages.getString("CommonSettingsDialog.colors.MoveJumpColor"));
        csbMoveJumpColor.setColour(GUIP.getMoveJumpColor());

        csbMoveMASCColor = new ColourSelectorButton(Messages.getString("CommonSettingsDialog.colors.MoveMASCColor"));
        csbMoveMASCColor.setColour(GUIP.getMoveMASCColor());
        csbMoveRunColor = new ColourSelectorButton(Messages.getString("CommonSettingsDialog.colors.MoveRunColor"));
        csbMoveRunColor.setColour(GUIP.getMoveRunColor());

        csbMoveBackColor = new ColourSelectorButton(Messages.getString("CommonSettingsDialog.colors.MoveBackColor"));
        csbMoveBackColor.setColour(GUIP.getMoveBackColor());

        csbMoveSprintColor =
              new ColourSelectorButton(Messages.getString("CommonSettingsDialog.colors.MoveSprintColor"));
        csbMoveSprintColor.setColour(GUIP.getMoveSprintColor());
      SettingsFormPanel movementGrid = createGameBoardFieldGrid("CommonSettingsGameBoardMovementGrid",
          moveFontTypeLabel, fontTypeChooserMoveFont,
          moveFontSizeLabel, moveFontSize,
          moveFontStyleLabel, fontStyleChooserMoveFont);
      movementGrid.addEqualWidthComponentGrid(2,
          csbMoveDefaultColor, csbMoveIllegalColor, csbMoveJumpColor, csbMoveMASCColor,
          csbMoveRunColor, csbMoveBackColor, csbMoveSprintColor);
      comps.add(List.of(movementGrid));

        addLineSpacer(comps);

        csbFireSolutionCanSeeColor = new ColourSelectorButton(Messages.getString(
              "CommonSettingsDialog.colors.FireSolnCanSeeColor"));
        csbFireSolutionCanSeeColor.setColour(GUIP.getFireSolnCanSeeColor());
        csbFireSolutionNoSeeColor = new ColourSelectorButton(Messages.getString(
              "CommonSettingsDialog.colors.FireSolnNoSeeColor"));
        csbFireSolutionNoSeeColor.setColour(GUIP.getFireSolnNoSeeColor());

        csbFieldOfFireMinColor = new ColourSelectorButton(Messages.getString(
              "CommonSettingsDialog.colors.FieldOfFireMinColor"));
        csbFieldOfFireMinColor.setColour(GUIP.getFieldOfFireMinColor());
        csbFieldOfFireShortColor = new ColourSelectorButton(Messages.getString(
              "CommonSettingsDialog.colors.FieldOfFireShortColor"));
        csbFieldOfFireShortColor.setColour(GUIP.getFieldOfFireShortColor());
        csbFieldOfFireMediumColor = new ColourSelectorButton(Messages.getString(
              "CommonSettingsDialog.colors.FieldOfFireMediumColor"));
        csbFieldOfFireMediumColor.setColour(GUIP.getFieldOfFireMediumColor());
        csbFieldOfFireLongColor = new ColourSelectorButton(Messages.getString(
              "CommonSettingsDialog.colors.FieldOfFireLongColor"));
        csbFieldOfFireLongColor.setColour(GUIP.getFieldOfFireLongColor());
        csbFieldOfFireExtremeColor = new ColourSelectorButton(Messages.getString(
              "CommonSettingsDialog.colors.FieldOfFireExtremeColor"));
        csbFieldOfFireExtremeColor.setColour(GUIP.getFieldOfFireExtremeColor());

        csbSensorRangeColor =
              new ColourSelectorButton(Messages.getString("CommonSettingsDialog.colors.SensorRangeColor"));
        csbSensorRangeColor.setColour(GUIP.getSensorRangeColor());
        csbVisualRangeColor =
              new ColourSelectorButton(Messages.getString("CommonSettingsDialog.colors.VisualRangeColor"));
        csbVisualRangeColor.setColour(GUIP.getVisualRangeColor());
      comps.add(List.of(createGameBoardOptionGrid("CommonSettingsGameBoardFireRangesGrid",
          csbFireSolutionCanSeeColor, csbFireSolutionNoSeeColor,
          csbFieldOfFireMinColor, csbFieldOfFireShortColor,
          csbFieldOfFireMediumColor, csbFieldOfFireLongColor, csbFieldOfFireExtremeColor,
          csbSensorRangeColor, csbVisualRangeColor)));

        addLineSpacer(comps);

        // Highlighting Radius inside FoV
          configureCheckBox(fovInsideEnabled,
              Messages.getString("TacticalOverlaySettingsDialog.FovInsideEnabled.tooltip"));

        // Inside Opaqueness slider
        fovHighlightAlpha = new JSlider(0, 255);
        fovHighlightAlpha.addChangeListener(this);
        fovHighlightAlpha.setToolTipText(Messages.getString("TacticalOverlaySettingsDialog.AlphaTooltip"));
          fovHighlightOpacityPercent = new JSpinner(new SpinnerNumberModel(0, 0, 100, 1));
          fovHighlightOpacityPercent.setToolTipText(Messages.getString("TacticalOverlaySettingsDialog.AlphaTooltip"));
          JPanel fovHighlightOpacityControl = createGameBoardFovOpacityControl(
              fovHighlightAlpha, fovHighlightOpacityPercent);
        highlightAlphaLabel = new JLabel(Messages.getString("TacticalOverlaySettingsDialog.FovHighlightAlpha"));
          highlightAlphaLabel.setToolTipText(Messages.getString("TacticalOverlaySettingsDialog.AlphaTooltip"));

          fovHighlightRangesLabel = new JLabel(Messages.getString(
              "TacticalOverlaySettingsDialog.FovHighlightRanges"));
          fovHighlightRangesLabel.setToolTipText(Messages.getString(
              "TacticalOverlaySettingsDialog.FovHighlightRanges.tooltip"));
          fovHighlightRingsEditor = new FovHighlightRingsPanel(
              GUIP.getFovHighlightRingsRadii(), GUIP.getFovHighlightRingsColorsHsb(),
              this::saveFovHighlightRanges);
          fovHighlightRingsEditor.setToolTipText(Messages.getString(
              "TacticalOverlaySettingsDialog.FovHighlightRanges.tooltip"));

          comps.add(List.of(createGameBoardFovInsideGrid(fovInsideEnabled,
              highlightAlphaLabel, fovHighlightOpacityControl,
              fovHighlightRangesLabel, fovHighlightRingsEditor)));

        addLineSpacer(comps);

        // Outside FoV Darkening
          configureCheckBox(fovOutsideEnabled,
              Messages.getString("TacticalOverlaySettingsDialog.FovOutsideEnabled.tooltip"));

        fovDarkenAlpha = new JSlider(0, 255);
        fovDarkenAlpha.addChangeListener(this);
        fovDarkenAlpha.setToolTipText(Messages.getString("TacticalOverlaySettingsDialog.AlphaTooltip"));
          fovDarkenOpacityPercent = new JSpinner(new SpinnerNumberModel(0, 0, 100, 1));
          fovDarkenOpacityPercent.setToolTipText(Messages.getString("TacticalOverlaySettingsDialog.AlphaTooltip"));
          JPanel fovDarkenOpacityControl = createGameBoardFovOpacityControl(
              fovDarkenAlpha, fovDarkenOpacityPercent);
        darkenAlphaLabel = new JLabel(Messages.getString("TacticalOverlaySettingsDialog.FovDarkenAlpha"));
        darkenAlphaLabel.setToolTipText(Messages.getString("TacticalOverlaySettingsDialog.AlphaTooltip"));

          fovStripesSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 50, 1));
          fovStripesSpinner.addChangeListener(this);
          fovStripesSpinner.setToolTipText(Messages.getString("TacticalOverlaySettingsDialog.FovStripesTooltip"));
        numStripesLabel = new JLabel(Messages.getString("TacticalOverlaySettingsDialog.FovStripes"));
        numStripesLabel.setToolTipText(Messages.getString("TacticalOverlaySettingsDialog.FovStripesTooltip"));

        fovGrayscaleEnabled = new JCheckBox(Messages.getString("TacticalOverlaySettingsDialog.FovGrayscale"));
          configureCheckBox(fovGrayscaleEnabled,
              Messages.getString("TacticalOverlaySettingsDialog.FovGrayscale.tooltip"));

          comps.add(List.of(createGameBoardFovOutsideGrid(fovOutsideEnabled, fovGrayscaleEnabled,
              darkenAlphaLabel, fovDarkenOpacityControl, numStripesLabel, fovStripesSpinner)));

        return createSettingsPanel(comps);
    }

    record TooltipSymbolOption(String symbol, String description) {
        @Override
        public String toString() {
            return symbol + "  " + description;
        }
    }

    static JSpinner createIntegerSpinner(int value, int minimum, int stepSize) {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(
            Math.max(minimum, value),
            minimum,
            Integer.MAX_VALUE,
            stepSize));
        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(spinner, "#");
        editor.getTextField().setColumns(8);
        spinner.setEditor(editor);
        return spinner;
    }

    static JSpinner createTooltipIntegerSpinner(int value, int minimum, int stepSize) {
        return createIntegerSpinner(value, minimum, stepSize);
    }

    static JComboBox<TooltipSymbolOption> createTooltipSymbolSelector(String selectedSymbol) {
        JComboBox<TooltipSymbolOption> selector = new JComboBox<>();
        selector.addItem(new TooltipSymbolOption("\u2B1B",
            Messages.getString("CommonSettingsDialog.tooltipSymbol.filledSquare")));
        selector.addItem(new TooltipSymbolOption("\u25A3",
            Messages.getString("CommonSettingsDialog.tooltipSymbol.centeredSquare")));
        selector.addItem(new TooltipSymbolOption("\u27D0",
            Messages.getString("CommonSettingsDialog.tooltipSymbol.centeredDiamond")));
        selector.addItem(new TooltipSymbolOption("\u2715",
            Messages.getString("CommonSettingsDialog.tooltipSymbol.cross")));
        selector.addItem(new TooltipSymbolOption("\u26CA",
            Messages.getString("CommonSettingsDialog.tooltipSymbol.shield")));
        selector.addItem(new TooltipSymbolOption("\u25C6",
            Messages.getString("CommonSettingsDialog.tooltipSymbol.filledDiamond")));
        selector.addItem(new TooltipSymbolOption("\u25C7",
            Messages.getString("CommonSettingsDialog.tooltipSymbol.hollowDiamond")));
        selector.addItem(new TooltipSymbolOption("\u25CF",
            Messages.getString("CommonSettingsDialog.tooltipSymbol.filledCircle")));
        selector.addItem(new TooltipSymbolOption("\u25CB",
            Messages.getString("CommonSettingsDialog.tooltipSymbol.hollowCircle")));
        selector.addItem(new TooltipSymbolOption("\u25AC",
            Messages.getString("CommonSettingsDialog.tooltipSymbol.bar")));
        selectTooltipSymbol(selector, selectedSymbol);
        return selector;
    }

    static void selectTooltipSymbol(JComboBox<TooltipSymbolOption> selector, String symbol) {
        for (int index = 0; index < selector.getItemCount(); index++) {
            TooltipSymbolOption option = selector.getItemAt(index);
            if (option.symbol().equals(symbol)) {
                selector.setSelectedIndex(index);
                return;
            }
        }
        TooltipSymbolOption custom = new TooltipSymbolOption(symbol,
            Messages.getString("CommonSettingsDialog.tooltipSymbol.custom"));
        selector.addItem(custom);
        selector.setSelectedItem(custom);
    }

    static String selectedTooltipSymbol(JComboBox<TooltipSymbolOption> selector) {
        Object selected = selector.getSelectedItem();
        return selected instanceof TooltipSymbolOption option ? option.symbol() : "";
    }

    static SettingsFormPanel createTooltipContentGrid(JComponent[] labelsAndControls,
        JComponent[] options, JComponent[] colours) {
        SettingsFormPanel grid = new SettingsFormPanel("CommonSettingsTooltipContentGrid",
            SettingsFormPanel.DEFAULT_LABEL_WIDTH,
            0);
        associateAlternatingLabels(labelsAndControls);
        grid.addEqualWidthComponentGrid(2, labelsAndControls);
        grid.addEqualWidthComponentGrid(2, options);
        grid.addEqualWidthComponentGrid(2, colours);
        return grid;
    }

    static SettingsFormPanel createTooltipArmorGrid(JCheckBox enabled, JComponent[] colours,
        JComponent[] labelsAndControls) {
        SettingsFormPanel grid = new SettingsFormPanel("CommonSettingsTooltipArmorGrid",
            SettingsFormPanel.DEFAULT_LABEL_WIDTH,
            0);
        grid.addEqualWidthComponentGrid(2, enabled);
        grid.addEqualWidthComponentGrid(2, colours);
        associateAlternatingLabels(labelsAndControls);
        grid.addEqualWidthComponentGrid(2, labelsAndControls);
        return grid;
    }

    private static void associateAlternatingLabels(JComponent[] labelsAndControls) {
        for (int index = 0; index + 1 < labelsAndControls.length; index += 2) {
            if (labelsAndControls[index] instanceof JLabel label) {
                label.setLabelFor(labelsAndControls[index + 1]);
            }
        }
    }

    private JPanel getUnitDisplayPanel() {
        List<List<Component>> comps = new ArrayList<>();
        ArrayList<Component> row;

        tooltipDelay = createTooltipIntegerSpinner(GUIP.getTooltipDelay(), 0, 100);
        tooltipDelay.setToolTipText(Messages.getString("CommonSettingsDialog.tooltipDelay.tooltip"));
        JLabel tooltipDelayLabel = new JLabel(Messages.getString("CommonSettingsDialog.tooltipDelay"));
        tooltipDelayLabel.setToolTipText(Messages.getString("CommonSettingsDialog.tooltipDelay.tooltip"));

        tooltipDismissDelay = createTooltipIntegerSpinner(GUIP.getTooltipDismissDelay(), -1, 1);
        tooltipDismissDelay.setToolTipText(Messages.getString("CommonSettingsDialog.tooltipDismissDelay.tooltip"));
        JLabel tooltipDismissDelayLabel = new JLabel(Messages.getString("CommonSettingsDialog.tooltipDismissDelay"));
        tooltipDismissDelayLabel.setToolTipText(Messages.getString("CommonSettingsDialog.tooltipDismissDelay.tooltip"));

        tooltipDistSuppression = createTooltipIntegerSpinner(GUIP.getTooltipDistSuppression(), 0, 1);
        tooltipDistSuppression.setToolTipText(Messages.getString(
            "CommonSettingsDialog.tooltipDistSuppression.tooltip"));
        JLabel tooltipDistSuppressionLabel = new JLabel(Messages.getString(
            "CommonSettingsDialog.tooltipDistSuppression"));
        tooltipDistSuppressionLabel.setToolTipText(Messages.getString(
            "CommonSettingsDialog.tooltipDistSuppression.tooltip"));

        JLabel unitTooltipFontSizeModLabel =
              new JLabel(Messages.getString("CommonSettingsDialog.unitTooltipFontSizeMod"));

        unitTooltipFontSizeModCbo = new JComboBox<>();
        unitTooltipFontSizeModCbo.addItem("large");
        unitTooltipFontSizeModCbo.addItem("medium");
        unitTooltipFontSizeModCbo.addItem("small");
        unitTooltipFontSizeModCbo.addItem("x-small");
        unitTooltipFontSizeModCbo.addItem("xx-small");
        unitTooltipFontSizeModCbo.setSelectedItem(GUIP.getUnitToolTipFontSizeMod());
        unitTooltipFontSizeModCbo.setMaximumSize(new Dimension(300, 60));

        unitTooltipFontSizeModCbo.setToolTipText(Messages.getString(
              "CommonSettingsDialog.unitTooltipFontSizeMod.tooltip"));

        csbUnitTooltipFGColor = new ColourSelectorButton(Messages.getString(
              "CommonSettingsDialog.colors.UnitTooltipFGColor"));
        csbUnitTooltipFGColor.setColour(GUIP.getUnitToolTipFGColor());
        csbUnitTooltipLightFGColor = new ColourSelectorButton(Messages.getString(
              "CommonSettingsDialog.colors.UnitTooltipLightFGColor"));
        csbUnitTooltipLightFGColor.setColour(GUIP.getToolTipLightFGColor());
        csbUnitTooltipBuildingFGColor = new ColourSelectorButton(Messages.getString(
              "CommonSettingsDialog.colors.UnitTooltipBuildingFGColor"));
        csbUnitTooltipBuildingFGColor.setColour(GUIP.getUnitToolTipBuildingFGColor());
        csbUnitTooltipAltFGColor = new ColourSelectorButton(Messages.getString(
              "CommonSettingsDialog.colors.UnitTooltipAltFGColor"));
        csbUnitTooltipAltFGColor.setColour(GUIP.getUnitToolTipAltFGColor());
        csbUnitTooltipBlockFGColor = new ColourSelectorButton(Messages.getString(
              "CommonSettingsDialog.colors.UnitTooltipBlockFGColor"));
        csbUnitTooltipBlockFGColor.setColour(GUIP.getUnitToolTipBlockFGColor());
        csbUnitTooltipTerrainFGColor = new ColourSelectorButton(Messages.getString(
              "CommonSettingsDialog.colors.UnitTooltipTerrainFGColor"));
        csbUnitTooltipTerrainFGColor.setColour(GUIP.getUnitToolTipTerrainFGColor());

        csbUnitTooltipBGColor = new ColourSelectorButton(Messages.getString(
              "CommonSettingsDialog.colors.UnitTooltipBGColor"));
        csbUnitTooltipBGColor.setColour(GUIP.getUnitToolTipBGColor());
        csbUnitTooltipBuildingBGColor = new ColourSelectorButton(Messages.getString(
              "CommonSettingsDialog.colors.UnitTooltipBuildingBGColor"));
        csbUnitTooltipBuildingBGColor.setColour(GUIP.getUnitToolTipBuildingBGColor());
        csbUnitTooltipAltBGColor = new ColourSelectorButton(Messages.getString(
              "CommonSettingsDialog.colors.UnitTooltipAltBGColor"));
        csbUnitTooltipAltBGColor.setColour(GUIP.getUnitToolTipAltBGColor());
        csbUnitTooltipBlockBGColor = new ColourSelectorButton(Messages.getString(
              "CommonSettingsDialog.colors.UnitTooltipBlockBGColor"));
        csbUnitTooltipBlockBGColor.setColour(GUIP.getUnitToolTipBlockBGColor());
        csbUnitTooltipTerrainBGColor = new ColourSelectorButton(Messages.getString(
              "CommonSettingsDialog.colors.UnitTooltipTerrainBGColor"));
        csbUnitTooltipTerrainBGColor.setColour(GUIP.getUnitToolTipTerrainBGColor());

        csbUnitTooltipHighlightColor = new ColourSelectorButton(Messages.getString(
              "CommonSettingsDialog.colors.UnitTooltipHighlightColor"));
        csbUnitTooltipHighlightColor.setColour(GUIP.getUnitToolTipHighlightColor());
        csbUnitTooltipWeaponColor = new ColourSelectorButton(Messages.getString(
              "CommonSettingsDialog.colors.UnitTooltipWeaponColor"));
        csbUnitTooltipWeaponColor.setColour(GUIP.getUnitToolTipWeaponColor());
        csbUnitTooltipQuirkColor = new ColourSelectorButton(Messages.getString(
              "CommonSettingsDialog.colors.UnitTooltipQuirkColor"));
        csbUnitTooltipQuirkColor.setColour(GUIP.getUnitToolTipQuirkColor());

        comps.add(List.of(createTooltipContentGrid(
            new JComponent[] {
                               tooltipDelayLabel, tooltipDelay,
                               tooltipDismissDelayLabel, tooltipDismissDelay,
                               tooltipDistSuppressionLabel, tooltipDistSuppression,
                               unitTooltipFontSizeModLabel, unitTooltipFontSizeModCbo },
            new JComponent[] { showWpsInTT, showWpsLocinTT, showPilotPortraitTT },
            new JComponent[] {
                               csbUnitTooltipFGColor, csbUnitTooltipBGColor,
                               csbUnitTooltipLightFGColor, csbUnitTooltipHighlightColor,
                               csbUnitTooltipBuildingFGColor, csbUnitTooltipBuildingBGColor,
                               csbUnitTooltipAltFGColor, csbUnitTooltipAltBGColor,
                               csbUnitTooltipBlockFGColor, csbUnitTooltipBlockBGColor,
                               csbUnitTooltipTerrainFGColor, csbUnitTooltipTerrainBGColor,
                               csbUnitTooltipWeaponColor, csbUnitTooltipQuirkColor })));

        addLineSpacer(comps);

        csbUnitTooltipArmorMiniIntact = new ColourSelectorButton(Messages.getString(
              "CommonSettingsDialog.colors.UnitTooltipArmorMiniIntact"));
        csbUnitTooltipArmorMiniIntact.setColour(GUIP.getUnitTooltipArmorMiniColorIntact());
        csbUnitTooltipArmorMiniPartial = new ColourSelectorButton(Messages.getString(
              "CommonSettingsDialog.colors.UnitTooltipArmorMiniPartialDamage"));
        csbUnitTooltipArmorMiniPartial.setColour(GUIP.getUnitTooltipArmorMiniColorPartialDamage());
        csbUnitTooltipArmorMiniDamaged = new ColourSelectorButton(Messages.getString(
              "CommonSettingsDialog.colors.UnitTooltipArmorMiniDamaged"));
        csbUnitTooltipArmorMiniDamaged.setColour(GUIP.getUnitTooltipArmorMiniColorDamaged());

        JLabel unitTooltipArmorLabel = new JLabel(Messages.getString("CommonSettingsDialog.armorMiniArmorChar"));
        unitTooltipArmorMiniArmorCharCbo = createTooltipSymbolSelector(GUIP.getUnitToolTipArmorMiniArmorChar());
        unitTooltipArmorMiniArmorCharCbo.setToolTipText(Messages.getString(
            "CommonSettingsDialog.armorMiniArmorChar.tooltip"));

        JLabel unitTooltipInternalStructureLabel = new JLabel(Messages.getString(
            "CommonSettingsDialog.armorMiniInternalStructureChar"));
        unitTooltipArmorMiniInternalStructureCharCbo = createTooltipSymbolSelector(GUIP
            .getUnitToolTipArmorMiniISChar());
        unitTooltipArmorMiniInternalStructureCharCbo.setToolTipText(Messages.getString(
            "CommonSettingsDialog.armorMiniInternalStructureChar.tooltip"));

        JLabel unitTooltipCriticalLabel = new JLabel(Messages.getString("CommonSettingsDialog.armorMiniCriticalChar"));
        unitTooltipArmorMiniCriticalCharCbo = createTooltipSymbolSelector(GUIP.getUnitToolTipArmorMiniCriticalChar());
        unitTooltipArmorMiniCriticalCharCbo.setToolTipText(Messages.getString(
            "CommonSettingsDialog.armorMiniCriticalChar.tooltip"));

        JLabel unitTooltipDestroyedLabel = new JLabel(Messages.getString(
            "CommonSettingsDialog.armorMiniDestroyedChar"));
        unitTooltipArmorMiniDestroyedCharCbo = createTooltipSymbolSelector(GUIP.getUnitToolTipArmorMiniDestroyedChar());
        unitTooltipArmorMiniDestroyedCharCbo.setToolTipText(Messages.getString(
            "CommonSettingsDialog.armorMiniDestroyedChar.tooltip"));

        JLabel unitTooltipCapArmorLabel = new JLabel(Messages.getString("CommonSettingsDialog.armorMiniCapArmorChar"));
        unitTooltipArmorMiniCapArmorCharCbo = createTooltipSymbolSelector(GUIP.getUnitToolTipArmorMiniCapArmorChar());
        unitTooltipArmorMiniCapArmorCharCbo.setToolTipText(Messages.getString(
            "CommonSettingsDialog.armorMiniCapArmorChar.tooltip"));

        JLabel unitTooltipUnitsPerBlockLabel = new JLabel(Messages.getString(
            "CommonSettingsDialog.armorMiniUnitsPerBlock"));
        unitTooltipArmorMiniUnitsPerBlockSpinner = createTooltipIntegerSpinner(
            GUIP.getUnitToolTipArmorMiniUnitsPerBlock(),
            1,
            1);
        unitTooltipArmorMiniUnitsPerBlockSpinner.setToolTipText(Messages.getString(
            "CommonSettingsDialog.armorMiniUnitsPerBlock.tooltip"));

        comps.add(List.of(createTooltipArmorGrid(showArmorMiniVisTT,
            new JComponent[] {
                               csbUnitTooltipArmorMiniIntact, csbUnitTooltipArmorMiniPartial,
                               csbUnitTooltipArmorMiniDamaged },
            new JComponent[] {
                               unitTooltipArmorLabel, unitTooltipArmorMiniArmorCharCbo,
                               unitTooltipInternalStructureLabel, unitTooltipArmorMiniInternalStructureCharCbo,
                               unitTooltipCriticalLabel, unitTooltipArmorMiniCriticalCharCbo,
                               unitTooltipDestroyedLabel, unitTooltipArmorMiniDestroyedCharCbo,
                               unitTooltipCapArmorLabel, unitTooltipArmorMiniCapArmorCharCbo,
                               unitTooltipUnitsPerBlockLabel, unitTooltipArmorMiniUnitsPerBlockSpinner })));

        addLineSpacer(comps);

        JLabel unitTooltipSeenByLabel = new JLabel(Messages.getString("CommonSettingsDialog.seenby.label"));
        unitTooltipSeenByCbo = new JComboBox<>();
        unitTooltipSeenByCbo.addItem(Messages.getString("CommonSettingsDialog.seenby.Someone"));
        unitTooltipSeenByCbo.addItem(Messages.getString("CommonSettingsDialog.seenby.Team"));
        unitTooltipSeenByCbo.addItem(Messages.getString("CommonSettingsDialog.seenby.Player"));
        unitTooltipSeenByCbo.addItem(Messages.getString("CommonSettingsDialog.seenby.PlayerDetailed"));
        unitTooltipSeenByCbo.setSelectedIndex(GUIP.getUnitToolTipSeenByResolution());
          String seenByToolTip = Messages.getString("CommonSettingsDialog.seenby.tooltip");
          unitTooltipSeenByLabel.setToolTipText(seenByToolTip);
          unitTooltipSeenByCbo.setToolTipText(seenByToolTip);

        csbUnitDisplayHeatLevel1 = new ColourSelectorButton(Messages.getString(
              "CommonSettingsDialog.unitDisplayHeatColor",
              Messages.getString("CommonSettingsDialog.colors.UnitDisplayHeatLevel1")));
        csbUnitDisplayHeatLevel1.setColour(GUIP.getUnitDisplayHeatLevel1());
          unitDisplayHeatLevel1Spinner = createIntegerSpinner(GUIP.getUnitDisplayHeatValue1(), 0, 1);
        csbUnitDisplayHeatLevel2 = new ColourSelectorButton(Messages.getString(
              "CommonSettingsDialog.unitDisplayHeatColor",
              Messages.getString("CommonSettingsDialog.colors.UnitDisplayHeatLevel2")));
        csbUnitDisplayHeatLevel2.setColour(GUIP.getUnitDisplayHeatLevel2());
          unitDisplayHeatLevel2Spinner = createIntegerSpinner(GUIP.getUnitDisplayHeatValue2(), 0, 1);
        csbUnitDisplayHeatLevel3 = new ColourSelectorButton(Messages.getString(
              "CommonSettingsDialog.unitDisplayHeatColor",
              Messages.getString("CommonSettingsDialog.colors.UnitDisplayHeatLevel3")));
        csbUnitDisplayHeatLevel3.setColour(GUIP.getUnitDisplayHeatLevel3());
          unitDisplayHeatLevel3Spinner = createIntegerSpinner(GUIP.getUnitDisplayHeatValue3(), 0, 1);
        csbUnitDisplayHeatLevel4 = new ColourSelectorButton(Messages.getString(
              "CommonSettingsDialog.unitDisplayHeatColor",
              Messages.getString("CommonSettingsDialog.colors.UnitDisplayHeatLevel4")));
        csbUnitDisplayHeatLevel4.setColour(GUIP.getUnitDisplayHeatLevel4());
          unitDisplayHeatLevel4Spinner = createIntegerSpinner(GUIP.getUnitDisplayHeatValue4(), 0, 1);
        csbUnitDisplayHeatLevel5 = new ColourSelectorButton(Messages.getString(
              "CommonSettingsDialog.unitDisplayHeatColor",
              Messages.getString("CommonSettingsDialog.colors.UnitDisplayHeatLevel5")));
        csbUnitDisplayHeatLevel5.setColour(GUIP.getUnitDisplayHeatLevel5());
          unitDisplayHeatLevel5Spinner = createIntegerSpinner(GUIP.getUnitDisplayHeatValue5(), 0, 1);
        csbUnitDisplayHeatLevel6 = new ColourSelectorButton(Messages.getString(
              "CommonSettingsDialog.unitDisplayHeatColor",
              Messages.getString("CommonSettingsDialog.colors.UnitDisplayHeatLevel6")));
        csbUnitDisplayHeatLevel6.setColour(GUIP.getUnitDisplayHeatLevel6());
          unitDisplayHeatLevel6Spinner = createIntegerSpinner(GUIP.getUnitDisplayHeatValue6(), 0, 1);
        csbUnitDisplayHeatLevelOverheat = new ColourSelectorButton(Messages.getString(
              "CommonSettingsDialog.unitDisplayHeatColor",
              Messages.getString("CommonSettingsDialog.colors.UnitDisplayHeatLevelOverheat")));
        csbUnitDisplayHeatLevelOverheat.setColour(GUIP.getUnitDisplayHeatLevelOverheat());

          String heatToolTip = Messages.getString("CommonSettingsDialog.unitDisplayHeatToolTip");
          JSpinner[] heatSpinners = {
            unitDisplayHeatLevel1Spinner, unitDisplayHeatLevel2Spinner, unitDisplayHeatLevel3Spinner,
            unitDisplayHeatLevel4Spinner, unitDisplayHeatLevel5Spinner, unitDisplayHeatLevel6Spinner
          };
          ColourSelectorButton[] heatColours = {
            csbUnitDisplayHeatLevel1, csbUnitDisplayHeatLevel2, csbUnitDisplayHeatLevel3,
            csbUnitDisplayHeatLevel4, csbUnitDisplayHeatLevel5, csbUnitDisplayHeatLevel6
          };
          JLabel[] heatLabels = new JLabel[heatSpinners.length];
          for (int index = 0; index < heatSpinners.length; index++) {
            String levelName = Messages.getString("CommonSettingsDialog.colors.UnitDisplayHeatLevel" + (index + 1));
            heatLabels[index] = new JLabel(Messages.getString(
                "CommonSettingsDialog.unitDisplayHeatMaximum", levelName));
            heatLabels[index].setToolTipText(heatToolTip);
            heatSpinners[index].setToolTipText(heatToolTip);
          }
          JLabel overheatLabel = new JLabel(Messages.getString("CommonSettingsDialog.unitDisplayHeatAboveMaximum"));
          overheatLabel.setToolTipText(heatToolTip);
          comps.add(List.of(createUnitDisplayHeatGrid(unitTooltipSeenByLabel, unitTooltipSeenByCbo,
              heatLabels, heatSpinners, heatColours, overheatLabel, csbUnitDisplayHeatLevelOverheat)));

        addLineSpacer(comps);

          loadUnitDisplayOrder(unitDisplayNonTabbed, savedUnitDisplayOrder());
          comps.add(List.of(createUnitDisplayOrderGrid(unitDisplayNonTabbed)));

        addLineSpacer(comps);

        JLabel defaultSortOrderLabel = new JLabel(Messages.getString("CommonSettingsDialog.defaultWeaponSortOrder"));
        String toolTip = Messages.getString("CommonSettingsDialog.defaultWeaponSortOrder.tooltip");
        defaultSortOrderLabel.setToolTipText(toolTip);

        final DefaultComboBoxModel<WeaponSortOrder> defaultWeaponSortOrderModel = new DefaultComboBoxModel<>(
              WeaponSortOrder.values());
        defaultWeaponSortOrderModel.removeElement(WeaponSortOrder.CUSTOM); // Custom makes no sense as a default
        comboDefaultWeaponSortOrder = new MMComboBox<>("comboDefaultWeaponSortOrder", defaultWeaponSortOrderModel);
        comboDefaultWeaponSortOrder.setToolTipText(toolTip);

        JLabel weaponListHeightLabel = new JLabel(Messages.getString("CommonSettingsDialog.weaponListHeight"));
        unitDisplayWeaponListHeightSpinner = createIntegerSpinner(GUIP.getUnitDisplayWeaponListHeight(), 1, 1);
          String weaponListHeightToolTip = Messages.getString("CommonSettingsDialog.weaponListHeight.tooltip");
          weaponListHeightLabel.setToolTipText(weaponListHeightToolTip);
          unitDisplayWeaponListHeightSpinner.setToolTipText(weaponListHeightToolTip);
          comps.add(List.of(createGameBoardFieldGrid("CommonSettingsUnitDisplayWeaponsGrid",
              defaultSortOrderLabel, comboDefaultWeaponSortOrder,
              weaponListHeightLabel, unitDisplayWeaponListHeightSpinner)));

        addLineSpacer(comps);

        JLabel unitDisplayMekArmorLargeFontSizeLabel = new JLabel(Messages.getString(
              "CommonSettingsDialog.unitDisplayMekArmorLargeFontSize"));
        unitDisplayMekArmorLargeFontSizeSpinner = createIntegerSpinner(
              GUIP.getUnitDisplayMekArmorLargeFontSize(), 1, 1);

        JLabel unitDisplayMekArmorMediumFontSizeLabel = new JLabel(Messages.getString(
              "CommonSettingsDialog.unitDisplayMekArmorMediumFontSize"));
        unitDisplayMekArmorMediumFontSizeSpinner = createIntegerSpinner(
              GUIP.getUnitDisplayMekArmorMediumFontSize(), 1, 1);

        JLabel unitDisplayMekArmorSmallFontSizeLabel = new JLabel(Messages.getString(
              "CommonSettingsDialog.unitDisplayMekArmorSmallFontSize"));
        unitDisplayMekArmorSmallFontSizeSpinner = createIntegerSpinner(
              GUIP.getUnitDisplayMekArmorSmallFontSize(), 1, 1);

        JLabel unitDisplayMekLargeFontSizeLabel = new JLabel(Messages.getString(
              "CommonSettingsDialog.unitDisplayMekLargeFontSize"));
        unitDisplayMekLargeFontSizeSpinner = createIntegerSpinner(GUIP.getUnitDisplayMekLargeFontSize(), 1, 1);

        JLabel unitDisplayMekMediumFontSizeLabel = new JLabel(Messages.getString(
              "CommonSettingsDialog.unitDisplayMekMediumFontSize"));
        unitDisplayMekMediumFontSizeSpinner = createIntegerSpinner(GUIP.getUnitDisplayMekMediumFontSize(), 1, 1);

        JLabel[] fontLabels = {
            unitDisplayMekArmorLargeFontSizeLabel, unitDisplayMekArmorMediumFontSizeLabel,
            unitDisplayMekArmorSmallFontSizeLabel, unitDisplayMekLargeFontSizeLabel,
            unitDisplayMekMediumFontSizeLabel
        };
        JSpinner[] fontSpinners = {
            unitDisplayMekArmorLargeFontSizeSpinner, unitDisplayMekArmorMediumFontSizeSpinner,
            unitDisplayMekArmorSmallFontSizeSpinner, unitDisplayMekLargeFontSizeSpinner,
            unitDisplayMekMediumFontSizeSpinner
        };
        String[] fontToolTipKeys = {
            "CommonSettingsDialog.unitDisplayMekArmorLargeFontSize.tooltip",
            "CommonSettingsDialog.unitDisplayMekArmorMediumFontSize.tooltip",
            "CommonSettingsDialog.unitDisplayMekArmorSmallFontSize.tooltip",
            "CommonSettingsDialog.unitDisplayMekLargeFontSize.tooltip",
            "CommonSettingsDialog.unitDisplayMekMediumFontSize.tooltip"
        };
        for (int index = 0; index < fontLabels.length; index++) {
            String fontToolTip = Messages.getString(fontToolTipKeys[index]);
            fontLabels[index].setToolTipText(fontToolTip);
            fontSpinners[index].setToolTipText(fontToolTip);
        }
        comps.add(List.of(createGameBoardFieldGrid("CommonSettingsUnitDisplayFontsGrid",
              unitDisplayMekArmorLargeFontSizeLabel, unitDisplayMekArmorLargeFontSizeSpinner,
              unitDisplayMekArmorMediumFontSizeLabel, unitDisplayMekArmorMediumFontSizeSpinner,
              unitDisplayMekArmorSmallFontSizeLabel, unitDisplayMekArmorSmallFontSizeSpinner,
              unitDisplayMekLargeFontSizeLabel, unitDisplayMekLargeFontSizeSpinner,
              unitDisplayMekMediumFontSizeLabel, unitDisplayMekMediumFontSizeSpinner)));

        return createSettingsPanel(comps);
    }

    static SettingsFormPanel createUnitDisplayHeatGrid(JLabel seenByLabel, JComponent seenByControl,
          JLabel[] heatLabels, JSpinner[] heatSpinners, ColourSelectorButton[] heatColours,
          JLabel overheatLabel, ColourSelectorButton overheatColour) {
        if ((heatLabels.length != heatSpinners.length) || (heatLabels.length != heatColours.length)) {
            throw new IllegalArgumentException("Unit Display heat controls must have matching levels");
        }

        SettingsFormPanel grid = new SettingsFormPanel("CommonSettingsUnitDisplayHeatGrid",
              SettingsFormPanel.DEFAULT_LABEL_WIDTH, 0);
        seenByLabel.setLabelFor(seenByControl);
        grid.addEqualWidthComponentGrid(2, seenByLabel, seenByControl);
        for (int index = 0; index < heatLabels.length; index++) {
            JPanel threshold = new JPanel(new BorderLayout(UIUtil.scaleForGUI(12), 0));
            threshold.setOpaque(false);
            heatLabels[index].setLabelFor(heatSpinners[index]);
            threshold.add(heatLabels[index], BorderLayout.CENTER);
            threshold.add(heatSpinners[index], BorderLayout.EAST);
            grid.addEqualWidthComponentGrid(2, threshold, heatColours[index]);
        }
        overheatLabel.setLabelFor(overheatColour);
        grid.addEqualWidthComponentGrid(2, overheatLabel, overheatColour);
        return grid;
    }

    static SettingsFormPanel createUnitDisplayOrderGrid(DefaultListModel<String> orderModel) {
        List<String> panelNames = List.of(
              UnitDisplayPanel.NON_TABBED_GENERAL, UnitDisplayPanel.NON_TABBED_PILOT,
              UnitDisplayPanel.NON_TABBED_ARMOR, UnitDisplayPanel.NON_TABBED_WEAPON,
              UnitDisplayPanel.NON_TABBED_SYSTEM, UnitDisplayPanel.NON_TABBED_EXTRA);
        if (orderModel.size() != panelNames.size()) {
            throw new IllegalArgumentException("Unit Display panel order must contain six positions");
        }

        String[] slotKeys = {
            "topLeft", "topCenter", "topRight", "bottomLeft", "bottomCenter", "bottomRight"
        };
        @SuppressWarnings("unchecked")
        JComboBox<String>[] selectors = new JComboBox[slotKeys.length];
        JPanel[] slots = new JPanel[slotKeys.length];
        boolean[] synchronizing = { false };
        String toolTip = Messages.getString("CommonSettingsDialog.unitDisplayOrder.tooltip");

        Runnable synchronizeSelectors = () -> {
            if (synchronizing[0]) {
                return;
            }
            synchronizing[0] = true;
            for (int index = 0; index < selectors.length; index++) {
                selectors[index].setSelectedItem(index < orderModel.size() ? orderModel.get(index) : null);
            }
            synchronizing[0] = false;
        };

        for (int index = 0; index < selectors.length; index++) {
            int slotIndex = index;
            JLabel slotLabel = new JLabel(Messages.getString(
                  "CommonSettingsDialog.unitDisplayOrder." + slotKeys[index]));
            JComboBox<String> selector = new JComboBox<>(panelNames.toArray(String[]::new));
            selector.setName("unitDisplayOrder" + slotKeys[index]);
            selector.setToolTipText(toolTip);
            slotLabel.setToolTipText(toolTip);
            slotLabel.setLabelFor(selector);
            selector.addActionListener(event -> {
                if (synchronizing[0] || (slotIndex >= orderModel.size())) {
                    return;
                }
                Object selection = selector.getSelectedItem();
                if (!(selection instanceof String selectedPanel)) {
                    return;
                }
                String previousPanel = orderModel.get(slotIndex);
                if (selectedPanel.equals(previousPanel)) {
                    return;
                }
                int previousSlot = orderModel.indexOf(selectedPanel);
                if (previousSlot < 0) {
                    synchronizeSelectors.run();
                    return;
                }

                synchronizing[0] = true;
                orderModel.set(slotIndex, selectedPanel);
                orderModel.set(previousSlot, previousPanel);
                synchronizing[0] = false;
                synchronizeSelectors.run();
            });
            selectors[index] = selector;

            JPanel slot = new JPanel(new BorderLayout(0, UIUtil.scaleForGUI(4)));
            slot.setName("pnlUnitDisplayOrder" + slotKeys[index]);
            slot.setOpaque(false);
            slot.add(slotLabel, BorderLayout.NORTH);
            slot.add(selector, BorderLayout.CENTER);
            slots[index] = slot;
        }

        orderModel.addListDataListener(new ListDataListener() {
            @Override
            public void intervalAdded(ListDataEvent event) {
                synchronizeSelectors.run();
            }

            @Override
            public void intervalRemoved(ListDataEvent event) {
                synchronizeSelectors.run();
            }

            @Override
            public void contentsChanged(ListDataEvent event) {
                synchronizeSelectors.run();
            }
        });
        synchronizeSelectors.run();

        SettingsFormPanel grid = new SettingsFormPanel("CommonSettingsUnitDisplayOrderGrid", 180, 0);
        grid.addEqualWidthComponentGrid(3, slots);
        return grid;
    }

    static void loadUnitDisplayOrder(DefaultListModel<String> orderModel, List<String> panelOrder) {
        orderModel.clear();
        panelOrder.forEach(orderModel::addElement);
    }

    private static List<String> savedUnitDisplayOrder() {
        return List.of(
              UNIT_DISPLAY_ORDER_PREFERENCES.getString(UnitDisplayPanel.NON_TABBED_A1),
              UNIT_DISPLAY_ORDER_PREFERENCES.getString(UnitDisplayPanel.NON_TABBED_B1),
              UNIT_DISPLAY_ORDER_PREFERENCES.getString(UnitDisplayPanel.NON_TABBED_C1),
              UNIT_DISPLAY_ORDER_PREFERENCES.getString(UnitDisplayPanel.NON_TABBED_A2),
              UNIT_DISPLAY_ORDER_PREFERENCES.getString(UnitDisplayPanel.NON_TABBED_B2),
              UNIT_DISPLAY_ORDER_PREFERENCES.getString(UnitDisplayPanel.NON_TABBED_C2));
    }

    private JPanel getReportPanel() {
        List<List<Component>> comps = new ArrayList<>();
        csbReportLinkColor =
              new ColourSelectorButton(Messages.getString("CommonSettingsDialog.colors.ReportLinkColor"));
        csbReportLinkColor.setColour(GUIP.getReportLinkColor());

        csbReportSuccessColor = new ColourSelectorButton(Messages.getString(
              "CommonSettingsDialog.colors.ReportSuccessColor"));
        csbReportSuccessColor.setColour(GUIP.getReportSuccessColor());

        csbReportMissColor =
              new ColourSelectorButton(Messages.getString("CommonSettingsDialog.colors.ReportMissColor"));
        csbReportMissColor.setColour(GUIP.getReportMissColor());

        csbReportInfoColor =
              new ColourSelectorButton(Messages.getString("CommonSettingsDialog.colors.ReportInfoColor"));
        csbReportInfoColor.setColour(GUIP.getReportInfoColor());

        fontTypeChooserReportFont = new JComboBox<>(new Vector<>(FontHandler.getAvailableNonSymbolFonts()));
        fontTypeChooserReportFont.setSelectedItem(GUIP.getReportFontType());

          JLabel reportFontTypeLabel = new JLabel(Messages.getString("CommonSettingsDialog.reportFontType"));

          configureCheckBox(showReportSprites, null);
        showReportSprites.setSelected(GUIP.getMiniReportShowSprites());
          comps.add(List.of(createReportAppearanceGrid(
              csbReportLinkColor, csbReportSuccessColor, csbReportMissColor, csbReportInfoColor,
              reportFontTypeLabel, fontTypeChooserReportFont, showReportSprites)));

        addLineSpacer(comps);

        chkReportShowPlayers.setToolTipText(Messages.getString(
              "CommonSettingsDialog.showReportPlayerList.tooltip"));
        chkReportShowPlayers.setSelected(GUIP.getMiniReportShowPlayers());

        chkReportShowUnits.setToolTipText(Messages.getString(
              "CommonSettingsDialog.showReportUnitList.tooltip"));
        chkReportShowUnits.setSelected(GUIP.getMiniReportShowUnits());

        chkReportShowKeywords.setToolTipText(Messages.getString(
              "CommonSettingsDialog.showReportKeywordsList.tooltip"));
        chkReportShowKeywords.setSelected(GUIP.getMiniReportShowKeywords());
          JLabel reportKeywordsLabel = new JLabel(Messages.getString("CommonSettingsDialog.reportKeywords"));
          String reportKeywordsHelp = Messages.getString("CommonSettingsDialog.reportKeywords.tooltip");
          reportKeywordsLabel.setToolTipText(reportKeywordsHelp);
          reportKeywordsTextArea = new JTextArea(6, 20);
          JScrollPane reportKeywordsEditor = createReportKeywordEditor(reportKeywordsTextArea, reportKeywordsHelp);
          comps.add(List.of(createReportSearchGrid(
              chkReportShowPlayers, chkReportShowUnits, chkReportShowKeywords,
              reportKeywordsLabel, reportKeywordsTextArea, reportKeywordsEditor)));

          addLineSpacer(comps);

        chkReportShowFilter.setToolTipText(Messages.getString(
              "CommonSettingsDialog.showReportFilterList.tooltip"));
        chkReportShowFilter.setSelected(GUIP.getMiniReportShowFilter());

          JLabel reportFilterKeywordsLabel = new JLabel(Messages.getString(
              "CommonSettingsDialog.reportFilterKeywords"));
          String reportFilterKeywordsHelp = Messages.getString("CommonSettingsDialog.reportFilterKeywords.tooltip");
          reportFilterKeywordsLabel.setToolTipText(reportFilterKeywordsHelp);
          reportFilterKeywordsTextArea = new JTextArea(4, 20);
          JScrollPane reportFilterKeywordsEditor = createReportKeywordEditor(
              reportFilterKeywordsTextArea, reportFilterKeywordsHelp);
          comps.add(List.of(createReportFilterGrid(
              chkReportShowFilter, reportFilterKeywordsLabel,
              reportFilterKeywordsTextArea, reportFilterKeywordsEditor)));

        return createSettingsPanel(comps);
    }

        static SettingsFormPanel createReportAppearanceGrid(
            JComponent linkColour, JComponent successColour, JComponent missColour, JComponent infoColour,
            JLabel fontLabel, JComponent fontControl, JCheckBox showSprites) {
          SettingsFormPanel grid = new SettingsFormPanel("CommonSettingsReportAppearanceGrid",
              SettingsFormPanel.DEFAULT_LABEL_WIDTH, 0);
          fontLabel.setLabelFor(fontControl);
          grid.addEqualWidthComponentGrid(2, linkColour, successColour, missColour, infoColour);
          grid.addEqualWidthComponentGrid(2, fontLabel, fontControl);
          grid.addEqualWidthComponentGrid(2, showSprites);
          return grid;
        }

        static SettingsFormPanel createReportSearchGrid(
            JCheckBox showPlayers, JCheckBox showUnits, JCheckBox showKeywords,
            JLabel keywordsLabel, JTextArea keywordsEditor, JComponent keywordsControl) {
          SettingsFormPanel grid = new SettingsFormPanel("CommonSettingsReportSearchGrid",
              SettingsFormPanel.DEFAULT_LABEL_WIDTH, 0);
          keywordsLabel.setLabelFor(keywordsEditor);
          grid.addEqualWidthComponentGrid(2, showPlayers, showUnits);
          grid.addEqualWidthComponentGrid(2, showKeywords);
          grid.addEqualWidthComponentGrid(2, keywordsLabel, keywordsControl);
          return grid;
        }

        static SettingsFormPanel createReportFilterGrid(
            JCheckBox showFilter, JLabel keywordsLabel, JTextArea keywordsEditor, JComponent keywordsControl) {
          SettingsFormPanel grid = new SettingsFormPanel("CommonSettingsReportFilterGrid",
              SettingsFormPanel.DEFAULT_LABEL_WIDTH, 0);
          keywordsLabel.setLabelFor(keywordsEditor);
          grid.addEqualWidthComponentGrid(2, showFilter);
          grid.addEqualWidthComponentGrid(2, keywordsLabel, keywordsControl);
          return grid;
        }

        static JScrollPane createReportKeywordEditor(JTextArea editor, String helpText) {
          editor.setLineWrap(false);
          editor.setToolTipText(helpText);
          JScrollPane scrollPane = new JScrollPane(editor,
              ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
              ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
          scrollPane.setToolTipText(helpText);
          return scrollPane;
        }

    private JPanel getOverlaysPanel() {
        List<List<Component>> comps = new ArrayList<>();
        ArrayList<Component> row;

        addLineSpacer(comps);

        row = new ArrayList<>();
        csbUnitOverviewTextShadowColor = new ColourSelectorButton(Messages.getString(
              "CommonSettingsDialog.colors.UnitOverviewTextShadowColor"));
        csbUnitOverviewTextShadowColor.setColour(GUIP.getUnitOverviewTextShadowColor());
        csbUnitOverviewTextShadowColor.setToolTipText(Messages.getString(
              "CommonSettingsDialog.colors.UnitOverviewTextShadowColor.tooltip"));
        row.add(csbUnitOverviewTextShadowColor);
        csbUnitOverviewConditionShadowColor = new ColourSelectorButton(Messages.getString(
              "CommonSettingsDialog.colors.UnitOverviewConditionShadowColor"));
        csbUnitOverviewConditionShadowColor.setColour(GUIP.getUnitOverviewConditionShadowColor());
        csbUnitOverviewConditionShadowColor.setToolTipText(Messages.getString(
              "CommonSettingsDialog.colors.UnitOverviewConditionShadowColor.tooltip"));
        row.add(csbUnitOverviewConditionShadowColor);
        comps.add(row);

        addLineSpacer(comps);

        row = new ArrayList<>();
        csbPlanetaryConditionsColorTitle = new ColourSelectorButton(Messages.getString(
              "CommonSettingsDialog.colors.PlanetaryConditionsColorTitle"));
        csbPlanetaryConditionsColorTitle.setColour(GUIP.getPlanetaryConditionsColorTitle());
        row.add(csbPlanetaryConditionsColorTitle);
        csbPlanetaryConditionsColorText = new ColourSelectorButton(Messages.getString(
              "CommonSettingsDialog.colors.PlanetaryConditionsColorText"));
        csbPlanetaryConditionsColorText.setColour(GUIP.getPlanetaryConditionsColorText());
        row.add(csbPlanetaryConditionsColorText);
        csbPlanetaryConditionsColorBackground = new ColourSelectorButton(Messages.getString(
              "CommonSettingsDialog.colors.PlanetaryConditionsColorBackground"));
        csbPlanetaryConditionsColorBackground.setColour(GUIP.getPlanetaryConditionsColorBackground());
        row.add(csbPlanetaryConditionsColorBackground);
        comps.add(row);

        row = new ArrayList<>();
        csbPlanetaryConditionsColorCold = new ColourSelectorButton(Messages.getString(
              "CommonSettingsDialog.colors.PlanetaryConditionsColorCold"));
        csbPlanetaryConditionsColorCold.setColour(GUIP.getPlanetaryConditionsColorCold());
        row.add(csbPlanetaryConditionsColorCold);
        csbPlanetaryConditionsColorHot = new ColourSelectorButton(Messages.getString(
              "CommonSettingsDialog.colors.PlanetaryConditionsColorHot"));
        csbPlanetaryConditionsColorHot.setColour(GUIP.getPlanetaryConditionsColorHot());
        row.add(csbPlanetaryConditionsColorHot);
        comps.add(row);

        comps.add(checkboxEntry(planetaryConditionsShowDefaults, null));
        planetaryConditionsShowDefaults.setSelected(GUIP.getPlanetaryConditionsShowDefaults());
        comps.add(checkboxEntry(planetaryConditionsShowHeader, null));
        planetaryConditionsShowHeader.setSelected(GUIP.getPlanetaryConditionsShowHeader());
        comps.add(checkboxEntry(planetaryConditionsShowLabels, null));
        planetaryConditionsShowLabels.setSelected(GUIP.getPlanetaryConditionsShowLabels());
        comps.add(checkboxEntry(planetaryConditionsShowValues, null));
        planetaryConditionsShowValues.setSelected(GUIP.getPlanetaryConditionsShowValues());
        comps.add(checkboxEntry(planetaryConditionsShowIndicators, null));
        planetaryConditionsShowIndicators.setSelected(GUIP.getPlanetaryConditionsShowIndicators());

        SpinnerNumberModel mPlanetaryConditionsBackgroundTransparency =
              new SpinnerNumberModel(GUIP.getPlanetaryConditionsBackgroundTransparency(),
                    0,
                    256,
                    1);
        planetaryConditionsBackgroundTransparency = new JSpinner(mPlanetaryConditionsBackgroundTransparency);
        planetaryConditionsBackgroundTransparency.setMaximumSize(new Dimension(150, 40));
        JLabel planetaryConditionsBackgroundTransparencyLabel = new JLabel(Messages.getString(
              "CommonSettingsDialog.colors.PlanetaryConditionsBackgroundTransparency"));
        row = new ArrayList<>();
        row.add(planetaryConditionsBackgroundTransparency);
        row.add(planetaryConditionsBackgroundTransparencyLabel);
        planetaryConditionsBackgroundTransparency.setToolTipText(Messages.getString(
              "CommonSettingsDialog.colors.PlanetaryConditionsBackgroundTransparency.tooltip"));
        comps.add(row);

        addLineSpacer(comps);

        // Set the state before checkboxEntry() attaches the item listener: the listener drives the dependent
        // controls, which are not built until further down this method.
        toastEnabled.setSelected(GUIP.getToastEnabled());
        comps.add(checkboxEntry(toastEnabled, Messages.getString("CommonSettingsDialog.toastEnabled.tooltip")));

        // The warning stays visible whether or not toasts are on: a player deciding whether to switch them off needs
        // to read it before clicking, not after.
        JLabel toastDisabledWarning = new JLabel("<html><body style='width: "
              + UIUtil.scaleForGUI(TOAST_WARNING_WIDTH_PX) + "px'>"
              + Messages.getString("CommonSettingsDialog.toastEnabled.warning") + "</body></html>");
        toastDisabledWarning.setToolTipText(Messages.getString("CommonSettingsDialog.toastEnabled.tooltip"));
        row = new ArrayList<>();
        row.add(Box.createRigidArea(DEPENDENT_INSET));
        row.add(toastDisabledWarning);
        comps.add(row);

        addSpacer(comps, 3);

        SpinnerNumberModel toastDurationModel = new SpinnerNumberModel(
              clampToastSeconds(GUIP.getToastDurationSeconds()),
              MIN_TOAST_SECONDS,
              MAX_TOAST_SECONDS,
              1);
        toastDurationSpinner = new JSpinner(toastDurationModel);
        toastDurationSpinner.setMaximumSize(new Dimension(150, 40));
        toastDurationSpinner.setToolTipText(Messages.getString("CommonSettingsDialog.toastDurationSeconds.tooltip"));
        toastDurationLabel = new JLabel(Messages.getString("CommonSettingsDialog.toastDurationSeconds"));
        toastDurationLabel.setToolTipText(Messages.getString("CommonSettingsDialog.toastDurationSeconds.tooltip"));
        row = new ArrayList<>();
        row.add(Box.createRigidArea(DEPENDENT_INSET));
        row.add(toastDurationSpinner);
        row.add(toastDurationLabel);
        comps.add(row);

        SpinnerNumberModel toastDripModel = new SpinnerNumberModel(
              clampToastSeconds(GUIP.getToastDripSeconds()),
              MIN_TOAST_SECONDS,
              MAX_TOAST_SECONDS,
              1);
        toastDripSpinner = new JSpinner(toastDripModel);
        toastDripSpinner.setMaximumSize(new Dimension(150, 40));
        toastDripSpinner.setToolTipText(Messages.getString("CommonSettingsDialog.toastDripSeconds.tooltip"));
        toastDripLabel = new JLabel(Messages.getString("CommonSettingsDialog.toastDripSeconds"));
        toastDripLabel.setToolTipText(Messages.getString("CommonSettingsDialog.toastDripSeconds.tooltip"));
        row = new ArrayList<>();
        row.add(Box.createRigidArea(DEPENDENT_INSET));
        row.add(toastDripSpinner);
        row.add(toastDripLabel);
        comps.add(row);

        toastReportEvents.setSelected(GUIP.getToastReportEvents());
        row = new ArrayList<>();
        row.add(Box.createRigidArea(DEPENDENT_INSET));
        row.addAll(checkboxEntry(toastReportEvents,
              Messages.getString("CommonSettingsDialog.toastReportEvents.tooltip")));
        comps.add(row);

        setToastControlsEnabled(toastEnabled.isSelected());

        addLineSpacer(comps);

        addSpacer(comps, 1);

        JLabel traceOverlayTransparencyLabel = new JLabel(Messages.getString(
              "CommonSettingsDialog.TraceOverlayTransparency"));
        traceOverlayTransparencyLabel.setToolTipText(
              Messages.getString(
                    "CommonSettingsDialog.TraceOverlayTransparency.tooltip"));

        row = new ArrayList<>();
        row.add(Box.createRigidArea(new Dimension(4, 0)));
        row.add(traceOverlayTransparencyLabel);
        comps.add(row);

        traceOverlayTransparencySlider = new JSlider(0, 255);
        traceOverlayTransparencySlider.setMajorTickSpacing(32);
        traceOverlayTransparencySlider.setMinorTickSpacing(4);
        traceOverlayTransparencySlider.setPaintTicks(true);
        traceOverlayTransparencySlider.setPaintLabels(true);
        traceOverlayTransparencySlider.setMaximumSize(new Dimension(1000, 100));
        traceOverlayTransparencySlider.addChangeListener(this);
        traceOverlayTransparencySlider.setToolTipText(
              Messages.getString("CommonSettingsDialog.TraceOverlayTransparency.tooltip"));
        traceOverlayTransparencySlider.setValue(GUIP.getTraceOverlayTransparency());

        row = new ArrayList<>();
        row.add(Box.createRigidArea(new Dimension(4, 0)));
        row.add(Box.createRigidArea(DEPENDENT_INSET));
        row.add(traceOverlayTransparencySlider);
        comps.add(row);

        addSpacer(comps, 1);

        JLabel traceOverlayScaleLabel = new JLabel(Messages.getString("CommonSettingsDialog.TraceOverlayScale"));
        traceOverlayScaleLabel.setToolTipText(Messages.getString("CommonSettingsDialog.TraceOverlayScale.tooltip"));

        row = new ArrayList<>();
        row.add(Box.createRigidArea(new Dimension(4, 0)));
        row.add(traceOverlayScaleLabel);
        comps.add(row);

        traceOverlayScaleSlider = new JSlider(30, 150);
        traceOverlayScaleSlider.setMajorTickSpacing(5);
        traceOverlayScaleSlider.setMinorTickSpacing(1);
        traceOverlayScaleSlider.setPaintTicks(true);
        traceOverlayScaleSlider.setPaintLabels(true);
        Hashtable<Integer, JComponent> labelTable = new Hashtable<>();
        labelTable.put(50, new JLabel("0.5"));
        labelTable.put(75, new JLabel("0.75"));
        labelTable.put(100, new JLabel("1"));
        labelTable.put(125, new JLabel("1.25"));
        labelTable.put(150, new JLabel("1.5"));
        traceOverlayScaleSlider.setLabelTable(labelTable);
        traceOverlayScaleSlider.setMaximumSize(new Dimension(1000, 100));
        traceOverlayScaleSlider.addChangeListener(this);
        traceOverlayScaleSlider.setToolTipText(Messages.getString("CommonSettingsDialog.TraceOverlayScale.tooltip"));
        traceOverlayScaleSlider.setValue(GUIP.getTraceOverlayScale());

        row = new ArrayList<>();
        row.add(Box.createRigidArea(new Dimension(4, 0)));
        row.add(Box.createRigidArea(DEPENDENT_INSET));
        row.add(traceOverlayScaleSlider);
        comps.add(row);

        JLabel traceOverlayOriginLabel = new JLabel(Messages.getString("CommonSettingsDialog.TraceOverlayOrigin"));

        row = new ArrayList<>();
        row.add(traceOverlayOriginLabel);
        comps.add(row);

        traceOverlayOriginXSlider = new JSlider(-1000, 2000);
        traceOverlayOriginXSlider.setMajorTickSpacing(200);
        traceOverlayOriginXSlider.setMinorTickSpacing(10);
        traceOverlayOriginXSlider.setPaintTicks(true);
        traceOverlayOriginXSlider.setPaintLabels(true);
        traceOverlayOriginXSlider.setMaximumSize(new Dimension(1000, 100));
        traceOverlayOriginXSlider.addChangeListener(this);
        traceOverlayOriginXSlider.setToolTipText(Messages.getString("CommonSettingsDialog.TraceOverlayOrigin.tooltip"));
        traceOverlayOriginXSlider.setValue(GUIP.getTraceOverlayOriginX());

        row = new ArrayList<>();
        row.add(Box.createRigidArea(new Dimension(4, 0)));
        row.add(Box.createRigidArea(DEPENDENT_INSET));
        row.add(traceOverlayOriginXSlider);
        comps.add(row);

        traceOverlayOriginYSlider = new JSlider(-1000, 2000);
        traceOverlayOriginYSlider.setMajorTickSpacing(200);
        traceOverlayOriginYSlider.setMinorTickSpacing(10);
        traceOverlayOriginYSlider.setPaintTicks(true);
        traceOverlayOriginYSlider.setPaintLabels(true);
        traceOverlayOriginYSlider.setMaximumSize(new Dimension(1000, 100));
        traceOverlayOriginYSlider.addChangeListener(this);
        traceOverlayOriginYSlider.setToolTipText(Messages.getString("CommonSettingsDialog.TraceOverlayOrigin.tooltip"));
        traceOverlayOriginYSlider.setValue(GUIP.getTraceOverlayOriginY());

        row = new ArrayList<>();
        row.add(Box.createRigidArea(new Dimension(4, 0)));
        row.add(Box.createRigidArea(DEPENDENT_INSET));
        row.add(traceOverlayOriginYSlider);
        comps.add(row);

        JLabel traceOverlayImageFileLabel =
              new JLabel(Messages.getString("CommonSettingsDialog.TraceOverlayImageFile"));
        traceOverlayImageFile = new JTextField(20);
        traceOverlayImageFile.setMaximumSize(new Dimension(250, 40));
        traceOverlayImageFile.setText(GUIP.getTraceOverlayImageFile());
        JButton traceOverlayImageFileChooser = new JButton("...");
        traceOverlayImageFileChooser.addActionListener(
              e -> selectTraceOverlayImageFile(traceOverlayImageFile, getFrame()));

        row = new ArrayList<>();
        row.add(traceOverlayImageFileLabel);
        row.add(traceOverlayImageFile);
        row.add(Box.createHorizontalStrut(10));
        row.add(traceOverlayImageFileChooser);
        row.add(Box.createHorizontalStrut(10));
        comps.add(row);

        addLineSpacer(comps);

        return createSettingsPanel(comps);
    }

    private static void selectTraceOverlayImageFile(JTextField textField, JFrame frame) {
        fileChoose(textField, frame, Messages.getString("CommonSettingsDialog.TraceOverlayImageFile"), false);
        GUIP.setTraceOverlayImageFile(textField.getText());
    }

    private BufferedImage boardImage;
    private JLabel boardImageLabel;

    private JPanel getMiniMapPanel() {
        List<List<Component>> comps = new ArrayList<>();
        minimapTheme = new MMComboBox<>("minimapTheme", new FileNameComboBoxModel(GUIP.getMinimapThemes()));
        minimapTheme.setSelectedItem(CLIENT_PREFERENCES.getMinimapTheme().getName());

        MapSettings mapSettings = MapSettings.getInstance();
        var board = BoardUtilities.generateRandom(mapSettings);

        boardImage = MinimapPanel.getMinimapImageMaxZoom(board, CLIENT_PREFERENCES.getMinimapTheme());

        boardImageLabel = new JLabel(new ImageIcon(boardImage));
        boardImageLabel.setPreferredSize(new Dimension(250, 250));
        boardImageLabel.setHorizontalAlignment(SwingConstants.CENTER);

        minimapTheme.addActionListener(e -> {
            String theme = minimapTheme.getSelectedItem();
            if (theme != null) {
                var newTheme = new MegaMekFile(Configuration.minimapThemesDir(), theme).getFile();
                SwingUtilities.invokeLater(() -> {
                    boardImage = MinimapPanel.getMinimapImageMaxZoom(board, newTheme);
                    boardImageLabel.setIcon(new ImageIcon(boardImage));
                    boardImageLabel.revalidate();
                    boardImageLabel.repaint();
                });
            }
        });
              comps.add(List.of(createMiniMapThemeGrid(minimapTheme, boardImageLabel)));

        addLineSpacer(comps);

              configureCheckBox(mmSymbol, null);
              configureCheckBox(gameSummaryMM, Messages.getString("CommonSettingsDialog.gameSummaryMM.tooltip",
                  Configuration.gameSummaryImagesMMDir()));
        JLabel gifGameSummaryRecordingLabel =
              new JLabel(Messages.getString("CommonSettingsDialog.gifGameSummaryRecording.name"));
        String gifRecordingTooltip = Messages.getString("CommonSettingsDialog.gifGameSummaryRecording.tooltip",
              Configuration.gameSummaryImagesMMDir());
        gifGameSummaryRecordingLabel.setToolTipText(gifRecordingTooltip);
        gifGameSummaryRecording.setToolTipText(gifRecordingTooltip);
              configureCheckBox(drawFacingArrowsOnMiniMap, null);
              configureCheckBox(drawSensorRangeOnMiniMap, null);
              configureCheckBox(paintBordersOnMiniMap, null);
              configureCheckBox(showUnitDisplayNamesOnMinimap,
                  Messages.getString("CommonSettingsDialog.showUnitDisplayNamesOnMinimap.tooltip"));

        SpinnerNumberModel movePathPersistenceModel = new SpinnerNumberModel(GUIP.getMovePathPersistenceOnMiniMap(),
              0,
              100,
              1);
        movePathPersistenceOnMiniMap = new JSpinner(movePathPersistenceModel);
        movePathPersistenceOnMiniMap.setMaximumSize(UIUtil.scaleForGUI(150, 40));
        movePathPersistenceOnMiniMap.setToolTipText(Messages.getString(
              "CommonSettingsDialog.movePathPersistence.tooltip"));
        JLabel movePathPersistenceOnMiniMapLabel = new JLabel(Messages.getString(
              "CommonSettingsDialog.movePathPersistence"));
        movePathPersistenceOnMiniMapLabel.setLabelFor(movePathPersistenceOnMiniMap);
          comps.add(List.of(createMiniMapDisplayGrid(
              mmSymbol, gameSummaryMM,
              gifGameSummaryRecordingLabel, gifGameSummaryRecording,
              drawFacingArrowsOnMiniMap, drawSensorRangeOnMiniMap,
              paintBordersOnMiniMap, showUnitDisplayNamesOnMinimap,
              movePathPersistenceOnMiniMapLabel, movePathPersistenceOnMiniMap)));

        return createSettingsPanel(comps);
    }

        static SettingsFormPanel createMiniMapThemeGrid(JComponent themeSelector, JComponent preview) {
          SettingsFormPanel grid = new SettingsFormPanel("CommonSettingsMiniMapThemeGrid",
              SettingsFormPanel.DEFAULT_LABEL_WIDTH, 0);
          grid.addEqualWidthComponentGrid(2, themeSelector, preview);
          return grid;
        }

        static SettingsFormPanel createMiniMapDisplayGrid(
            JCheckBox symbols, JCheckBox summaryImage,
            JLabel recordingLabel, JComponent recordingControl,
            JCheckBox facingArrows, JCheckBox sensorRange,
            JCheckBox borders, JCheckBox unitNames,
            JLabel persistenceLabel, JComponent persistenceControl) {
          SettingsFormPanel grid = new SettingsFormPanel("CommonSettingsMiniMapDisplayGrid",
              SettingsFormPanel.DEFAULT_LABEL_WIDTH, 0);
          recordingLabel.setLabelFor(recordingControl);
          persistenceLabel.setLabelFor(persistenceControl);
          grid.addEqualWidthComponentGrid(2, symbols, summaryImage);
          grid.addEqualWidthComponentGrid(2, recordingLabel, recordingControl);
          grid.addEqualWidthComponentGrid(2, facingArrows, sensorRange);
          grid.addEqualWidthComponentGrid(2, borders, unitNames);
          grid.addEqualWidthComponentGrid(2, persistenceLabel, persistenceControl);
          return grid;
        }

    private static class PlayerColourHelper {
        PlayerColour pc;
        ColourSelectorButton csb;

        public PlayerColourHelper(PlayerColour pc, ColourSelectorButton csb) {
            this.pc = pc;
            this.csb = csb;
        }
    }

    static ColourSelectorButton createPlayerColourButton(PlayerColour playerColour) {
        ColourSelectorButton button = new ColourSelectorButton(playerColour.toString());
        button.setToolTipText(Messages.getString("CommonSettingsDialog.playerColour.tooltip"));
        return button;
    }

    private JPanel getPlayerColourPanel() {
        List<List<Component>> comps = new ArrayList<>();
        ArrayList<Component> row;

        playerColours = new ArrayList<>();

        row = new ArrayList<>();
        JLabel playerColourLabel = new JLabel(Messages.getString("CommonSettingsDialog.playerColour"));
        playerColourLabel.setToolTipText(Messages.getString("CommonSettingsDialog.playerColour.tooltip"));
        row.add(playerColourLabel);
        comps.add(row);

        for (PlayerColour pc : PlayerColour.values()) {
            ColourSelectorButton csb = createPlayerColourButton(pc);
            playerColours.add(new PlayerColourHelper(pc, csb));
        }

        row = new ArrayList<>();

        for (PlayerColourHelper pch : playerColours) {
            pch.csb.setColour(GUIP.getColor(pch.pc.getText()));
            row.add(pch.csb);
        }

        comps.add(row);

        return createSettingsPanel(comps);
    }

    private JPanel getSettingsPanel() {
        List<List<Component>> comps = new ArrayList<>();
        ArrayList<Component> row;

        JLabel displayLocaleLabel = new JLabel(Messages.getString("CommonSettingsDialog.locale"));
        displayLocale = new JComboBox<>();
        displayLocale.addItem(Messages.getString("CommonSettingsDialog.locale.English"));
        displayLocale.addItem(Messages.getString("CommonSettingsDialog.locale.Deutsch"));
        displayLocale.addItem(Messages.getString("CommonSettingsDialog.locale.Russian"));
        displayLocale.addItem(Messages.getString("CommonSettingsDialog.locale.Spanish"));
        row = new ArrayList<>();
        row.add(displayLocaleLabel);
        row.add(displayLocale);
        comps.add(row);

        addLineSpacer(comps);

        JLabel guiScaleLabel = new JLabel(Messages.getString("CommonSettingsDialog.guiScale"));
        row = new ArrayList<>();
        row.add(guiScaleLabel);
        row.add(createGuiScaleControl());
        comps.add(row);

        addLineSpacer(comps);

        JLabel userDirLabel = new JLabel(Messages.getString("CommonSettingsDialog.userDir"));
        userDirLabel.setToolTipText(Messages.getString("CommonSettingsDialog.userDir.tooltip"));
        userDir = new JTextField(20);
        userDir.setToolTipText(Messages.getString("CommonSettingsDialog.userDir.tooltip"));
        String userFilesChooserTitle = Messages.getString("CommonSettingsDialog.userDir.chooser.title");
        JButton userFilesChooser = applicationIconButton("btnUserDirChooser", 0xE2C8, userFilesChooserTitle);
        userFilesChooser.addActionListener(e -> fileChooseUserDir(userDir, getFrame()));
        JButton userFilesHelp = createUserFilesHelpButton();
        row = new ArrayList<>();
        row.add(userDirLabel);
        row.add(applicationPathControl(userDir, userFilesChooser, userFilesHelp));
        comps.add(row);

        addLineSpacer(comps);

        JLabel mmlPathLabel = new JLabel(Messages.getString("CommonSettingsDialog.mmlPath"));
        mmlPathLabel.setToolTipText(Messages.getString("CommonSettingsDialog.mmlPath.tooltip"));
        mmlPath = new JTextField(20);
        mmlPath.setToolTipText(Messages.getString("CommonSettingsDialog.mmlPath.tooltip"));
        String mmlPathChooserTitle = Messages.getString("CommonSettingsDialog.mmlPath.chooser.title");
        JButton mmlPathChooser = applicationIconButton("btnMmlPathChooser", 0xE2C8, mmlPathChooserTitle);
        mmlPathChooser.addActionListener(e -> fileChoose(mmlPath,
              getFrame(),
              mmlPathChooserTitle,
              false));
        row = new ArrayList<>();
        row.add(mmlPathLabel);
        row.add(applicationPathControl(mmlPath, mmlPathChooser));
        comps.add(row);

        addLineSpacer(comps);

        // UI Theme
        uiThemes = new JComboBox<>();
        JLabel uiThemesLabel = new JLabel(Messages.getString("CommonSettingsDialog.uiTheme"));
        row = new ArrayList<>();
        row.add(uiThemesLabel);
        row.add(uiThemes);
        comps.add(row);

        addSpacer(comps, 5);

        // Skin
        skinFiles = new JComboBox<>();
        skinFiles.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                  boolean cellHasFocus) {
                return super.getListCellRendererComponent(list,
                      new File((String) value).getName(),
                      index,
                      isSelected,
                      cellHasFocus);
            }
        });
        JLabel skinFileLabel = new JLabel(Messages.getString("CommonSettingsDialog.skinFile"));
        row = new ArrayList<>();
        row.add(skinFileLabel);
        row.add(skinFiles);
        comps.add(row);

        addLineSpacer(comps);

        row = new ArrayList<>();
        csbWarningColor = new ColourSelectorButton(Messages.getString("CommonSettingsDialog.colors.warningColor"));
        csbWarningColor.setColour(GUIP.getWarningColor());
        row.add(csbWarningColor);
        csbCautionColor = new ColourSelectorButton(Messages.getString("CommonSettingsDialog.colors.CautionColor"));
        csbCautionColor.setColour(GUIP.getCautionColor());
        row.add(csbCautionColor);
        csbPrecautionColor =
              new ColourSelectorButton(Messages.getString("CommonSettingsDialog.colors.PrecautionColor"));
        csbPrecautionColor.setColour(GUIP.getPrecautionColor());
        row.add(csbPrecautionColor);
        csbOkColor = new ColourSelectorButton(Messages.getString("CommonSettingsDialog.colors.OkColor"));
        csbOkColor.setColour(GUIP.getOkColor());
        row.add(csbOkColor);
        comps.add(row);

        addLineSpacer(comps);

        row = new ArrayList<>();
        csbMyUnitColor = new ColourSelectorButton(Messages.getString("CommonSettingsDialog.colors.myUnitColor"));
        csbMyUnitColor.setColour(GUIP.getMyUnitColor());
        row.add(csbMyUnitColor);
        csbAllyUnitColor = new ColourSelectorButton(Messages.getString("CommonSettingsDialog.colors.allyUnitColor"));
        csbAllyUnitColor.setColour(GUIP.getAllyUnitColor());
        row.add(csbAllyUnitColor);
        csbEnemyColor = new ColourSelectorButton(Messages.getString("CommonSettingsDialog.colors.enemyUnitColor"));
        csbEnemyColor.setColour(GUIP.getEnemyUnitColor());
        row.add(csbEnemyColor);
        comps.add(row);

        addLineSpacer(comps);

        row = new ArrayList<>();
        row.add(getPlayerColourPanel());
        comps.add(row);

        addLineSpacer(comps);

        configureCheckBox(teamColoring, Messages.getString("CommonSettingsDialog.teamColoring.tooltip"));
        configureCheckBox(dockOnLeft, null);
        dockOnLeft.setSelected(GUIP.getDockOnLeft());
        configureCheckBox(dockMultipleOnYAxis, null);
        dockMultipleOnYAxis.setSelected(GUIP.getDockMultipleOnYAxis());
        configureCheckBox(useCamoOverlay, null);
        useCamoOverlay.setSelected(GUIP.getUseCamoOverlay());
        row = new ArrayList<>();
        row.add(createBehaviorOptionsGrid("CommonSettingsWindowLayoutGrid",
              teamColoring, dockOnLeft, dockMultipleOnYAxis, useCamoOverlay));
        comps.add(row);

        addLineSpacer(comps);

        JLabel unitStartCharLabel = new JLabel(Messages.getString("CommonSettingsDialog.protoMekUnitCodes"));
        unitStartChar = new JComboBox<>();
        // Add option for "A, B, C, D..."
        unitStartChar.addItem("\u0041, \u0042, \u0043, \u0044...");
        // Add option for "ALPHA, BETA, GAMMA, DELTA..."
        unitStartChar.addItem("\u0391, \u0392, \u0393, \u0394...");
        // Add option for "alpha, beta, gamma, delta..."
        unitStartChar.addItem("\u03B1, \u03B2, \u03B3, \u03B4...");
        unitStartChar.setMaximumSize(new Dimension(150, 40));
        configureCheckBox(defaultAutoEjectDisabled, null);
        configureCheckBox(useAverageSkills, null);
        configureCheckBox(generateNames, null);
        row = new ArrayList<>();
        row.add(createUnitDefaultsGrid(unitStartCharLabel, unitStartChar,
              defaultAutoEjectDisabled, useAverageSkills, generateNames));
        comps.add(row);

        addLineSpacer(comps);
        configureCheckBox(datasetLogging, null);
        configureCheckBox(keepGameLog, null);

        gameLogFilenameLabel = new JLabel(Messages.getString("CommonSettingsDialog.logFileName"));
        gameLogFilename = new JTextField(15);
        JLabel autoResolveLogFilenameLabel = new JLabel(
              Messages.getString("CommonSettingsDialog.autoResolveLogFileName"));
        autoResolveLogFilename = new JTextField(15);
        configureCheckBox(stampFilenames, null);

        stampFormatLabel = new JLabel(Messages.getString("CommonSettingsDialog.stampFormat"));
        stampFormat = new JTextField(20);
        row = new ArrayList<>();
        row.add(createBehaviorLoggingGrid("CommonSettingsLoggingGrid",
              datasetLogging, keepGameLog,
              gameLogFilenameLabel, gameLogFilename,
              autoResolveLogFilenameLabel, autoResolveLogFilename,
              stampFilenames,
              stampFormatLabel, stampFormat));
        comps.add(row);

        addLineSpacer(comps);

        showIPAddressesInChat.addItemListener(this);
        configureCheckBox(spritesOnly, Messages.getString("CommonSettingsDialog.spritesOnly.tooltip"));
        row = new ArrayList<>();
        row.add(createBehaviorOptionsGrid("CommonSettingsPrivacyRenderingGrid",
              showIPAddressesInChat, spritesOnly));
        comps.add(row);
        return createSettingsPanel(comps);
    }

    private List<Component> checkboxEntry(JCheckBox checkbox, String toolTip) {
        configureCheckBox(checkbox, toolTip);
        return List.of(checkbox);
    }

    private void configureCheckBox(JCheckBox checkbox, String toolTip) {
        checkbox.setToolTipText(toolTip);
        checkbox.addItemListener(this);
    }

    /**
     * Greys out the toast timing and report-echo controls when toasts are switched off entirely, since none of them
     * have any effect in that state.
     *
     * @param enabled {@code true} when board toasts are switched on
     */
    private void setToastControlsEnabled(boolean enabled) {
        toastDurationSpinner.setEnabled(enabled);
        toastDurationLabel.setEnabled(enabled);
        toastDripSpinner.setEnabled(enabled);
        toastDripLabel.setEnabled(enabled);
        toastReportEvents.setEnabled(enabled);
    }

    private void addLineSpacer(List<List<Component>> comps) {
        List<Component> row = new ArrayList<>();
        row.add(new SettingsSectionBreak());
        comps.add(row);
    }

    private void addSpacer(List<List<Component>> comps, int height) {
        List<Component> row = new ArrayList<>();
        row.add(Box.createVerticalStrut(height));
        comps.add(row);
    }

    /**
     * Display the current settings in this dialog.
     * <p>
     * Overrides
     * <code>Dialog#setVisible(boolean)</code>.
     */
    @Override
    public void setVisible(boolean visible) {
        // Initialize the dialog when it's being shown
        if (visible) {
            guiScale.setValue((int) (GUIP.getGUIScale() * 10));
            autoEndFiring.setSelected(GUIP.getAutoEndFiring());
            autoDeclareSearchlight.setSelected(GUIP.getAutoDeclareSearchlight());
            enableExperimentalBotFeatures.setSelected(CLIENT_PREFERENCES.getEnableExperimentalBotFeatures());
            nagForMASC.setSelected(GUIP.getNagForMASC());
            nagForPSR.setSelected(GUIP.getNagForPSR());
            nagForWiGELanding.setSelected(GUIP.getNagForWiGELanding());
            nagForNoAction.setSelected(GUIP.getNagForNoAction());
            nagForNoUnJamRAC.setSelected(GUIP.getNagForNoUnJamRAC());
            nagForOverheat.setSelected(GUIP.getNagForOverheat());
            nagForDishonor.setSelected(GUIP.getNagForDishonor());
            nagForMechanicalJumpFallDamage.setSelected(GUIP.getNagForMechanicalJumpFallDamage());
            nagForCrushingBuildings.setSelected(GUIP.getNagForCrushingBuildings());
            nagForLaunchDoors.setSelected(GUIP.getNagForLaunchDoors());
            nagForSprint.setSelected(GUIP.getNagForSprint());
            nagForOddSizedBoard.setSelected(GUIP.getNagForOddSizedBoard());
            animateMove.setSelected(GUIP.getShowMoveStep());
            showWrecks.setSelected(GUIP.getShowWrecks());
            tooltipDelay.setValue(Math.max(0, GUIP.getTooltipDelay()));
            tooltipDismissDelay.setValue(Math.max(-1, GUIP.getTooltipDismissDelay()));
            tooltipDistSuppression.setValue(Math.max(0, GUIP.getTooltipDistSuppression()));
            showWpsInTT.setSelected(GUIP.getShowWpsInTT());
            showWpsLocinTT.setSelected(GUIP.getShowWpsLocinTT());
            showArmorMiniVisTT.setSelected(GUIP.getShowArmorMiniVisTT());
            showPilotPortraitTT.setSelected(GUIP.getShowPilotPortraitTT());
            comboDefaultWeaponSortOrder.setSelectedItem(GUIP.getDefaultWeaponSortOrder());
            mouseWheelZoom.setSelected(GUIP.getMouseWheelZoom());
            mouseWheelZoomFlip.setSelected(GUIP.getMouseWheelZoomFlip());

            moveDefaultClimbMode.setSelected(GUIP.getMoveDefaultClimbMode());

            // Select the correct char set (give a nice default to start).
            unitStartChar.setSelectedIndex(0);
            for (int loop = 0;
                  loop < unitStartChar.getItemCount();
                  loop++) {
                if (unitStartChar.getItemAt(loop).charAt(0) == CLIENT_PREFERENCES.getUnitStartChar()) {
                    unitStartChar.setSelectedIndex(loop);
                    break;
                }
            }

            masterVolumeSlider.setValue(GUIP.getMasterVolume());
            soundMuteChat.setSelected(GUIP.getSoundMuteChat());
            soundMuteMyTurn.setSelected(GUIP.getSoundMuteMyTurn());
            soundMuteOthersTurn.setSelected(GUIP.getSoundMuteOthersTurn());
            tfSoundMuteChatFileName.setText(GUIP.getSoundBingFilenameChat());
            tfSoundMuteMyTurnFileName.setText(GUIP.getSoundBingFilenameMyTurn());
            tfSoundMuteOthersFileName.setText(GUIP.getSoundBingFilenameOthersTurn());

            maxPathfinderTime.setValue(Math.max(1, CLIENT_PREFERENCES.getMaxPathfinderTime()));

            keepGameLog.setSelected(CLIENT_PREFERENCES.keepGameLog());
            datasetLogging.setSelected(CLIENT_PREFERENCES.dataLoggingEnabled());
            gameLogFilename.setEnabled(keepGameLog.isSelected());
            gameLogFilename.setText(CLIENT_PREFERENCES.getGameLogFilename());
            autoResolveLogFilename.setEnabled(keepGameLog.isSelected());
            autoResolveLogFilename.setText(CLIENT_PREFERENCES.getAutoResolveGameLogFilename());
            userDir.setText(CLIENT_PREFERENCES.getUserDir());
            mmlPath.setText(CLIENT_PREFERENCES.getMmlPath());
            stampFilenames.setSelected(CLIENT_PREFERENCES.stampFilenames());
            stampFormat.setEnabled(stampFilenames.isSelected());
            stampFormat.setText(CLIENT_PREFERENCES.getStampFormat());
            reportKeywordsTextArea.setText(CLIENT_PREFERENCES.getReportKeywords());
            reportFilterKeywordsTextArea.setText(CLIENT_PREFERENCES.getReportFilterKeywords());
            showIPAddressesInChat.setSelected(CLIENT_PREFERENCES.getShowIPAddressesInChat());
            spritesOnly.setSelected(CLIENT_PREFERENCES.getSpritesOnly());

            defaultAutoEjectDisabled.setSelected(CLIENT_PREFERENCES.defaultAutoEjectDisabled());
            useAverageSkills.setSelected(CLIENT_PREFERENCES.useAverageSkills());
            generateNames.setSelected(CLIENT_PREFERENCES.generateNames());
            showUnitId.setSelected(CLIENT_PREFERENCES.getShowUnitId());
            showAutoResolvePanel.setSelected(CLIENT_PREFERENCES.getShowAutoResolvePanel());
            //            favoritePrincessBehaviorSetting.setSelectedItem(CLIENT_PREFERENCES.getFavoritePrincessBehaviorSetting());

            int index = 0;
            if (CLIENT_PREFERENCES.getLocaleString().startsWith("de")) {
                index = 1;
            }
            if (CLIENT_PREFERENCES.getLocaleString().startsWith("ru")) {
                index = 2;
            }
            displayLocale.setSelectedIndex(index);

            showMapSheets.setSelected(GUIP.getShowMapSheets());
            chkHighQualityGraphics.setSelected(GUIP.getHighQualityGraphics());
            chkHighPerformanceGraphics.setSelected(GUIP.getHighPerformanceGraphics());
            showDamageLevel.setSelected(GUIP.getShowDamageLevel());
            showDamageDecal.setSelected(GUIP.getShowDamageDecal());
            aOHexShadows.setSelected(GUIP.getAOHexShadows());
            floatingIso.setSelected(GUIP.getFloatingIso());
            mmSymbol.setSelected(GUIP.getMmSymbol());
            drawFacingArrowsOnMiniMap.setSelected(GUIP.getDrawFacingArrowsOnMiniMap());
            drawSensorRangeOnMiniMap.setSelected(GUIP.getDrawSensorRangeOnMiniMap());
            paintBordersOnMiniMap.setSelected(GUIP.paintBorders());
            showUnitDisplayNamesOnMinimap.setSelected(GUIP.showUnitDisplayNamesOnMinimap());
            levelHighlight.setSelected(GUIP.getLevelHighlight());
            shadowMap.setSelected(GUIP.getShadowMap());
            hexInclines.setSelected(GUIP.getHexInclines());
            useSoftCenter.setSelected(GUIP.getSoftCenter());
            useAutoCenter.setSelected(GUIP.getAutoCenter());
            useAutoSelectNext.setSelected(GUIP.getAutoSelectNextUnit());
            entityOwnerColor.setSelected(GUIP.getUnitLabelBorder());
            teamColoring.setSelected(GUIP.getTeamColoring());

            File dir = Configuration.hexesDir();
            tileSets = new ArrayList<>(Arrays.asList(Objects.requireNonNull(dir.list((directory, name) -> name.endsWith(
                  ".tileset")))));
            tileSets.addAll(userDataFiles(Configuration.hexesDir(), ".tileset"));
            tileSetChoice.removeAllItems();
            for (int i = 0;
                  i < tileSets.size();
                  i++) {
                String name = tileSets.get(i);
                tileSetChoice.addItem(name.substring(0, name.length() - 8));
                if (name.equals(CLIENT_PREFERENCES.getMapTileset())) {
                    tileSetChoice.setSelectedIndex(i);
                }
            }

            minimapTheme.setSelectedItem(CLIENT_PREFERENCES.getMinimapTheme().getName());

            gameSummaryBV.setSelected(GUIP.getGameSummaryBoardView());
            gameSummaryMM.setSelected(GUIP.getGameSummaryMinimap());
            gifGameSummaryRecording.setSelectedIndex(GUIP.getGifGameSummaryRecording().ordinal());
            skinFiles.removeAllItems();
            ArrayList<String> xmlFiles = new ArrayList<>(filteredFiles(Configuration.skinsDir(), ".xml"));

            String userDirName = PreferenceManager.getClientPreferences().getUserDir();
            File userDir = new File(userDirName);
            if (!userDirName.isBlank() && userDir.isDirectory()) {
                xmlFiles.addAll(filteredFilesWithSubDirs(userDir, ".xml"));
            }

            File internalUserDataDir = new File(Configuration.userDataDir(), Configuration.skinsDir().toString());
            xmlFiles.addAll(filteredFiles(internalUserDataDir, ".xml"));
            xmlFiles.removeIf(file -> !SkinXMLHandler.validSkinSpecFile(file));
            Collections.sort(xmlFiles);
            ComboBoxModel<String> model = new DefaultComboBoxModel<>(xmlFiles.toArray(new String[0]));
            model.setSelectedItem(GUIP.getSkinFile());
            skinFiles.setModel(model);

            uiThemes.removeAllItems();
            for (LookAndFeelInfo lafInfo : UIManager.getInstalledLookAndFeels()) {
                if (GUIPreferences.isSupportedLookAndFeel(lafInfo)) {
                    uiThemes.addItem(new UITheme(lafInfo.getClassName(), lafInfo.getName()));
                }
            }
            uiThemes.setSelectedItem(new UITheme(GUIP.getUITheme()));

            fovInsideEnabled.setSelected(GUIP.getFovHighlight());
            fovHighlightAlpha.setValue(GUIP.getFovHighlightAlpha());
            fovHighlightRingsEditor.setValues(
                GUIP.getFovHighlightRingsRadii(), GUIP.getFovHighlightRingsColorsHsb());
            fovOutsideEnabled.setSelected(GUIP.getFovDarken());
            fovDarkenAlpha.setValue(GUIP.getFovDarkenAlpha());
            fovStripesSpinner.setValue(GUIP.getFovStripes());
            fovGrayscaleEnabled.setSelected(GUIP.getFovGrayscale());

            fovHighlightAlpha.setEnabled(fovInsideEnabled.isSelected());
            fovHighlightOpacityPercent.setEnabled(fovInsideEnabled.isSelected());
            fovHighlightRingsEditor.setEditorEnabled(fovInsideEnabled.isSelected());
            fovDarkenAlpha.setEnabled(fovOutsideEnabled.isSelected());
            fovDarkenOpacityPercent.setEnabled(fovOutsideEnabled.isSelected());
            fovStripesSpinner.setEnabled(fovOutsideEnabled.isSelected());
            fovGrayscaleEnabled.setEnabled(fovOutsideEnabled.isSelected());

            darkenAlphaLabel.setEnabled(fovOutsideEnabled.isSelected());
            numStripesLabel.setEnabled(fovOutsideEnabled.isSelected());
            fovHighlightRangesLabel.setEnabled(fovInsideEnabled.isSelected());
            highlightAlphaLabel.setEnabled(fovInsideEnabled.isSelected());

            stampFormatLabel.setEnabled(stampFilenames.isSelected());
            gameLogFilenameLabel.setEnabled(keepGameLog.isSelected());

            getFocus.setSelected(GUIP.getFocus());

            savedFovHighlight = GUIP.getFovHighlight();
            savedFovDarken = GUIP.getFovDarken();
            savedFovGrayscale = GUIP.getFovGrayscale();
            savedAOHexShadows = GUIP.getAOHexShadows();
            savedShadowMap = GUIP.getShadowMap();
            savedHexInclines = GUIP.getHexInclines();
            savedLevelHighlight = GUIP.getLevelHighlight();
            savedFloatingIso = GUIP.getFloatingIso();
            savedMmSymbol = GUIP.getMmSymbol();
            savedDrawFacingArrowsOnMiniMap = GUIP.getDrawFacingArrowsOnMiniMap();
            savedDrawSensorRangeOnMiniMap = GUIP.getDrawSensorRangeOnMiniMap();
            savedPaintBorders = GUIP.paintBorders();
            savedTeamColoring = GUIP.getTeamColoring();
            savedDockOnLeft = GUIP.getDockOnLeft();
            savedDockMultipleOnYAxis = GUIP.getDockMultipleOnYAxis();
            savedUseCamoOverlay = GUIP.getUseCamoOverlay();
            savedUnitLabelBorder = GUIP.getUnitLabelBorder();
            savedShowDamageDecal = GUIP.getShowDamageDecal();
            savedShowDamageLabel = GUIP.getShowDamageLevel();
            savedFovHighlightRingsRadii = GUIP.getFovHighlightRingsRadii();
            savedFovHighlightRingsColors = GUIP.getFovHighlightRingsColorsHsb();
            savedFovHighlightAlpha = GUIP.getFovHighlightAlpha();
            savedFovDarkenAlpha = GUIP.getFovDarkenAlpha();
            savedNumStripesSlider = GUIP.getFovStripes();
            savedHighQualityGraphics = GUIP.getHighQualityGraphics();
            savedMovePathPersistenceOnMiniMap = GUIP.getMovePathPersistenceOnMiniMap();
            savedAdvancedOpt.clear();

            advancedKeys.clearSelection();

            for (KeyCommandBind kcb : KeyCommandBind.values()) {
                cmdModifierMap.get(kcb.cmd).setText(KeyEvent.getModifiersExText(kcb.modifiers));
                if (kcb.key == 0) {
                    cmdKeyMap.get(kcb.cmd).setText("");
                } else {
                    cmdKeyMap.get(kcb.cmd).setText(KeyEvent.getKeyText(kcb.key));
                }

            }

            markDuplicateBinds();
        }

        super.setVisible(visible);
    }

    /** Cancels any updates made in this dialog and closes it. */
    @Override
    protected void cancelAction() {
        GUIP.setFovHighlight(savedFovHighlight);
        GUIP.setFovDarken(savedFovDarken);
        GUIP.setFovGrayscale(savedFovGrayscale);
        GUIP.setAOHexShadows(savedAOHexShadows);
        GUIP.setShadowMap(savedShadowMap);
        GUIP.setHexInclines(savedHexInclines);
        GUIP.setLevelHighlight(savedLevelHighlight);
        GUIP.setFloatingIso(savedFloatingIso);
        GUIP.setMmSymbol(savedMmSymbol);
        GUIP.setDrawSensorRangeOnMiniMap(savedDrawSensorRangeOnMiniMap);
        GUIP.setDrawFacingArrowsOnMiniMap(savedDrawFacingArrowsOnMiniMap);
        GUIP.setPaintBorders(savedPaintBorders);
        GUIP.setMovePathPersistenceOnMiniMap(savedMovePathPersistenceOnMiniMap);
        GUIP.setTeamColoring(savedTeamColoring);
        GUIP.setDockOnLeft(savedDockOnLeft);
        GUIP.setDockMultipleOnYAxis(savedDockMultipleOnYAxis);
        GUIP.setUseCamoOverlay(savedUseCamoOverlay);
        GUIP.setUnitLabelBorder(savedUnitLabelBorder);
        GUIP.setShowDamageDecal(savedShowDamageDecal);
        GUIP.setShowDamageLevel(savedShowDamageLabel);
        GUIP.setFovHighlightRingsRadii(savedFovHighlightRingsRadii);
        GUIP.setFovHighlightRingsColorsHsb(savedFovHighlightRingsColors);
        GUIP.setFovHighlightAlpha(savedFovHighlightAlpha);
        GUIP.setFovDarkenAlpha(savedFovDarkenAlpha);
        GUIP.setFovStripes(savedNumStripesSlider);
        GUIP.setHighQualityGraphics(savedHighQualityGraphics);

        csbWarningColor.setColour(GUIP.getWarningColor());
        csbCautionColor.setColour(GUIP.getCautionColor());
        csbPrecautionColor.setColour(GUIP.getPrecautionColor());
        csbOkColor.setColour(GUIP.getOkColor());

        csbMyUnitColor.setColour(GUIP.getMyUnitColor());
        csbAllyUnitColor.setColour(GUIP.getAllyUnitColor());
        csbEnemyColor.setColour(GUIP.getEnemyUnitColor());

        for (PlayerColourHelper pch : playerColours) {
            pch.csb.setColour(GUIP.getColor(pch.pc.getText()));
        }

        csbMoveDefaultColor.setColour(GUIP.getMoveDefaultColor());
        csbMoveIllegalColor.setColour(GUIP.getMoveIllegalColor());
        csbMoveJumpColor.setColour(GUIP.getMoveJumpColor());
        csbMoveMASCColor.setColour(GUIP.getMoveMASCColor());
        csbMoveRunColor.setColour(GUIP.getMoveRunColor());
        csbMoveBackColor.setColour(GUIP.getMoveBackColor());
        csbMoveSprintColor.setColour(GUIP.getMoveSprintColor());

        csbFireSolutionCanSeeColor.setColour(GUIP.getFireSolnCanSeeColor());
        csbFireSolutionNoSeeColor.setColour(GUIP.getFireSolnNoSeeColor());
        csbFieldOfFireMinColor.setColour(GUIP.getFieldOfFireMinColor());
        csbFieldOfFireShortColor.setColour(GUIP.getFieldOfFireShortColor());
        csbFieldOfFireMediumColor.setColour(GUIP.getFieldOfFireMediumColor());
        csbFieldOfFireLongColor.setColour(GUIP.getFieldOfFireLongColor());
        csbFieldOfFireExtremeColor.setColour(GUIP.getFieldOfFireExtremeColor());

        csbSensorRangeColor.setColour(GUIP.getSensorRangeColor());
        csbVisualRangeColor.setColour(GUIP.getVisualRangeColor());

        csbUnitValidColor.setColour(GUIP.getUnitValidColor());
        csbUnitSelectedColor.setColour(GUIP.getUnitSelectedColor());
        csbUnitTextColor.setColour(GUIP.getUnitTextColor());

        csbBuildingTextColor.setColour(GUIP.getBuildingTextColor());
        csbBoardTextColor.setColour(GUIP.getBoardTextColor());
        csbBoardSpaceTextColor.setColour(GUIP.getBoardSpaceTextColor());
        csbLowFoliageColor.setColour(GUIP.getLowFoliageColor());
        csbDemolitionChargeColor.setColour(GUIP.getDemolitionChargeColor());
        demolitionChargeHazardOutline.setSelected(GUIP.getDemolitionChargeHazardOutline());
        csbMapSheetColor.setColour(GUIP.getMapsheetColor());

        attackArrowTransparency.setValue(GUIP.getAttackArrowTransparency());
        ecmTransparency.setValue(GUIP.getECMTransparency());
        buttonsPerRow.setValue(Math.max(1, GUIP.getButtonsPerRow()));
        playersRemainingToShow.setValue(Math.max(0, GUIP.getPlayersRemainingToShow()));
        tmmPipModeCbo.setSelectedIndex(GUIP.getTMMPipMode());
        fontTypeChooserMoveFont.setSelectedItem(GUIP.getMoveFontType());
        moveFontSize.setValue(Math.max(1, GUIP.getMoveFontSize()));
        fontStyleChooserMoveFont.setSelectedIndex(GUIP.getMoveFontStyle());
        darkenMapAtNight.setSelected(GUIP.getDarkenMapAtNight());
        translucentHiddenUnits.setSelected(GUIP.getTranslucentHiddenUnits());

        artilleryDisplayMisses.setSelected(GUIP.getShowArtilleryMisses());
        artilleryDisplayDriftedHits.setSelected(GUIP.getShowArtilleryDrifts());
        artilleryDisplayDriftArrows.setSelected(GUIP.getShowArtilleryDriftArrows());
        bombsDisplayMisses.setSelected(GUIP.getShowBombMisses());
        bombsDisplayDrifts.setSelected(GUIP.getShowBombDrifts());

        for (String option : savedAdvancedOpt.keySet()) {
            GUIP.setValue(option, savedAdvancedOpt.get(option));
        }

        loadUnitDisplayOrder(unitDisplayNonTabbed, savedUnitDisplayOrder());

        unitDisplayAutoDisplayReportCombo.setSelectedItem(GUIP.getUnitDisplayAutoDisplayReportPhase());
        unitDisplayAutoDisplayNonReportCombo.setSelectedItem(GUIP.getUnitDisplayAutoDisplayNonReportPhase());
        miniMapAutoDisplayReportCombo.setSelectedItem(GUIP.getMinimapAutoDisplayReportPhase());
        miniMapAutoDisplayNonReportCombo.setSelectedItem(GUIP.getMinimapAutoDisplayNonReportPhase());
        miniReportAutoDisplayReportCombo.setSelectedItem(GUIP.getMiniReportAutoDisplayReportPhase());
        miniReportAutoDisplayNonReportCombo.setSelectedItem(GUIP.getMiniReportAutoDisplayNonReportPhase());
        playerListAutoDisplayReportCombo.setSelectedItem(GUIP.getPlayerListAutoDisplayReportPhase());
        playerListAutoDisplayNonReportCombo.setSelectedItem(GUIP.getPlayerListAutoDisplayNonReportPhase());
        forceDisplayAutoDisplayReportCombo.setSelectedItem(GUIP.getForceDisplayAutoDisplayReportPhase());
        forceDisplayAutoDisplayNonReportCombo.setSelectedItem(GUIP.getForceDisplayAutoDisplayNonReportPhase());
        botCommandsAutoDisplayReportCombo.setSelectedItem(GUIP.getBotCommandsAutoDisplayReportPhase());
        botCommandsAutoDisplayNonReportCombo.setSelectedItem(GUIP.getBotCommandsAutoDisplayNonReportPhase());
        displayMoveDisplayDuringMovePhases.setSelected(GUIP.getMoveDisplayTabDuringMovePhases());
        displayFireDisplayDuringFirePhases.setSelected(GUIP.getFireDisplayTabDuringFiringPhases());

        csbUnitDisplayHeatLevel1.setColour(GUIP.getUnitDisplayHeatLevel1());
        csbUnitDisplayHeatLevel2.setColour(GUIP.getUnitDisplayHeatLevel2());
        csbUnitDisplayHeatLevel3.setColour(GUIP.getUnitDisplayHeatLevel3());
        csbUnitDisplayHeatLevel4.setColour(GUIP.getUnitDisplayHeatLevel4());
        csbUnitDisplayHeatLevel5.setColour(GUIP.getUnitDisplayHeatLevel5());
        csbUnitDisplayHeatLevel6.setColour(GUIP.getUnitDisplayHeatLevel6());
        csbUnitDisplayHeatLevelOverheat.setColour(GUIP.getUnitDisplayHeatLevelOverheat());

        unitDisplayHeatLevel1Spinner.setValue(GUIP.getUnitDisplayHeatValue1());
        unitDisplayHeatLevel2Spinner.setValue(GUIP.getUnitDisplayHeatValue2());
        unitDisplayHeatLevel3Spinner.setValue(GUIP.getUnitDisplayHeatValue3());
        unitDisplayHeatLevel4Spinner.setValue(GUIP.getUnitDisplayHeatValue4());
        unitDisplayHeatLevel5Spinner.setValue(GUIP.getUnitDisplayHeatValue5());
        unitDisplayHeatLevel6Spinner.setValue(GUIP.getUnitDisplayHeatValue6());

        unitTooltipSeenByCbo.setSelectedIndex(GUIP.getUnitToolTipSeenByResolution());
        unitDisplayWeaponListHeightSpinner.setValue(GUIP.getUnitDisplayWeaponListHeight());

        unitDisplayMekArmorLargeFontSizeSpinner.setValue(GUIP.getUnitDisplayMekArmorLargeFontSize());
        unitDisplayMekArmorMediumFontSizeSpinner.setValue(GUIP.getUnitDisplayMekArmorMediumFontSize());
        unitDisplayMekArmorSmallFontSizeSpinner.setValue(GUIP.getUnitDisplayMekArmorSmallFontSize());
        unitDisplayMekLargeFontSizeSpinner.setValue(GUIP.getUnitDisplayMekLargeFontSize());
        unitDisplayMekMediumFontSizeSpinner.setValue(GUIP.getUnitDisplayMekMediumFontSize());

        csbUnitTooltipFGColor.setColour(GUIP.getUnitToolTipFGColor());
        csbUnitTooltipLightFGColor.setColour(GUIP.getToolTipLightFGColor());
        csbUnitTooltipBuildingFGColor.setColour(GUIP.getUnitToolTipBuildingFGColor());
        csbUnitTooltipAltFGColor.setColour(GUIP.getUnitToolTipAltFGColor());
        csbUnitTooltipBlockFGColor.setColour(GUIP.getUnitToolTipBlockFGColor());
        csbUnitTooltipTerrainFGColor.setColour(GUIP.getUnitToolTipTerrainFGColor());
        csbUnitTooltipBGColor.setColour(GUIP.getUnitToolTipBGColor());
        csbUnitTooltipBuildingBGColor.setColour(GUIP.getUnitToolTipBuildingBGColor());
        csbUnitTooltipAltBGColor.setColour(GUIP.getUnitToolTipAltBGColor());
        csbUnitTooltipBlockBGColor.setColour(GUIP.getUnitToolTipBlockBGColor());
        csbUnitTooltipTerrainBGColor.setColour(GUIP.getUnitToolTipTerrainBGColor());

        csbUnitTooltipHighlightColor.setColour(GUIP.getUnitToolTipHighlightColor());
        csbUnitTooltipWeaponColor.setColour(GUIP.getUnitToolTipWeaponColor());
        csbUnitTooltipQuirkColor.setColour(GUIP.getUnitToolTipQuirkColor());

        csbUnitTooltipArmorMiniIntact.setColour(GUIP.getUnitTooltipArmorMiniColorIntact());
        csbUnitTooltipArmorMiniPartial.setColour(GUIP.getUnitTooltipArmorMiniColorPartialDamage());
        csbUnitTooltipArmorMiniDamaged.setColour(GUIP.getUnitTooltipArmorMiniColorDamaged());
          selectTooltipSymbol(unitTooltipArmorMiniArmorCharCbo, GUIP.getUnitToolTipArmorMiniArmorChar());
          selectTooltipSymbol(unitTooltipArmorMiniInternalStructureCharCbo, GUIP.getUnitToolTipArmorMiniISChar());
          selectTooltipSymbol(unitTooltipArmorMiniCriticalCharCbo, GUIP.getUnitToolTipArmorMiniCriticalChar());
          selectTooltipSymbol(unitTooltipArmorMiniDestroyedCharCbo, GUIP.getUnitToolTipArmorMiniDestroyedChar());
          selectTooltipSymbol(unitTooltipArmorMiniCapArmorCharCbo, GUIP.getUnitToolTipArmorMiniCapArmorChar());
          unitTooltipArmorMiniUnitsPerBlockSpinner.setValue(
              Math.max(1, GUIP.getUnitToolTipArmorMiniUnitsPerBlock()));
        unitTooltipFontSizeModCbo.setSelectedItem(GUIP.getUnitToolTipFontSizeMod());

        csbReportLinkColor.setColour(GUIP.getReportLinkColor());
        csbReportSuccessColor.setColour(GUIP.getReportSuccessColor());
        csbReportMissColor.setColour(GUIP.getReportMissColor());
        csbReportInfoColor.setColour(GUIP.getReportInfoColor());
        fontTypeChooserReportFont.setSelectedItem(GUIP.getReportFontType());
        showReportSprites.setSelected(GUIP.getMiniReportShowSprites());

        csbUnitOverviewTextShadowColor.setColour(GUIP.getUnitOverviewTextShadowColor());
        csbUnitOverviewConditionShadowColor.setColour(GUIP.getUnitOverviewConditionShadowColor());

        csbPlanetaryConditionsColorTitle.setColour(GUIP.getPlanetaryConditionsColorTitle());
        csbPlanetaryConditionsColorText.setColour(GUIP.getPlanetaryConditionsColorText());
        csbPlanetaryConditionsColorBackground.setColour(GUIP.getPlanetaryConditionsColorBackground());
        csbPlanetaryConditionsColorCold.setColour(GUIP.getPlanetaryConditionsColorCold());
        csbPlanetaryConditionsColorHot.setColour(GUIP.getPlanetaryConditionsColorHot());

        planetaryConditionsShowDefaults.setSelected(GUIP.getPlanetaryConditionsShowDefaults());
        planetaryConditionsShowHeader.setSelected(GUIP.getPlanetaryConditionsShowHeader());
        planetaryConditionsShowLabels.setSelected(GUIP.getPlanetaryConditionsShowLabels());
        planetaryConditionsShowValues.setSelected(GUIP.getPlanetaryConditionsShowValues());
        planetaryConditionsShowIndicators.setSelected(GUIP.getPlanetaryConditionsShowIndicators());
        planetaryConditionsBackgroundTransparency.setValue(GUIP.getPlanetaryConditionsBackgroundTransparency());

        toastEnabled.setSelected(GUIP.getToastEnabled());
        toastDurationSpinner.setValue(clampToastSeconds(GUIP.getToastDurationSeconds()));
        toastDripSpinner.setValue(clampToastSeconds(GUIP.getToastDripSeconds()));
        toastReportEvents.setSelected(GUIP.getToastReportEvents());
        setToastControlsEnabled(toastEnabled.isSelected());

        traceOverlayTransparencySlider.setValue(GUIP.getTraceOverlayTransparency());
        traceOverlayScaleSlider.setValue(GUIP.getTraceOverlayScale());
        traceOverlayOriginXSlider.setValue(GUIP.getTraceOverlayOriginX());
        traceOverlayOriginYSlider.setValue(GUIP.getTraceOverlayOriginY());
        traceOverlayImageFile.setText(GUIP.getTraceOverlayImageFile());

        setVisible(false);
    }

    /** Update the settings from this dialog's values, then close it. */
    @Override
    protected void okAction() {
        GUIP.setShowDamageLevel(showDamageLevel.isSelected());
        GUIP.setShowDamageDecal(showDamageDecal.isSelected());
        GUIP.setUnitLabelBorder(entityOwnerColor.isSelected());
        GUIP.setTeamColoring(teamColoring.isSelected());
        GUIP.setDockOnLeft(dockOnLeft.isSelected());
        GUIP.setDockMultipleOnYAxis(dockMultipleOnYAxis.isSelected());
        GUIP.setUseCamoOverlay(useCamoOverlay.isSelected());
        GUIP.setAutoEndFiring(autoEndFiring.isSelected());
        GUIP.setAutoDeclareSearchlight(autoDeclareSearchlight.isSelected());
        GUIP.setDefaultWeaponSortOrder(Objects.requireNonNull(comboDefaultWeaponSortOrder.getSelectedItem()));
        GUIP.setNagForMASC(nagForMASC.isSelected());
        GUIP.setNagForPSR(nagForPSR.isSelected());
        GUIP.setNagForWiGELanding(nagForWiGELanding.isSelected());
        GUIP.setNagForNoAction(nagForNoAction.isSelected());
        GUIP.setNagForNoUnJamRAC(nagForNoUnJamRAC.isSelected());
        GUIP.setNagForOverheat(nagForOverheat.isSelected());
        GUIP.setNagForDishonor(nagForDishonor.isSelected());
        GUIP.setNagForMechanicalJumpFallDamage(nagForMechanicalJumpFallDamage.isSelected());
        GUIP.setNagForCrushingBuildings(nagForCrushingBuildings.isSelected());
        GUIP.setNagForLaunchDoors(nagForLaunchDoors.isSelected());
        GUIP.setNagForSprint(nagForSprint.isSelected());
        GUIP.setNagForOddSizedBoard(nagForOddSizedBoard.isSelected());
        GUIP.setShowMoveStep(animateMove.isSelected());
        GUIP.setShowWrecks(showWrecks.isSelected());
        GUIP.setShowWpsInTT(showWpsInTT.isSelected());
        GUIP.setShowWpsLocinTT(showWpsLocinTT.isSelected());
        GUIP.setShowArmorMiniVisTT(showArmorMiniVisTT.isSelected());
        GUIP.setShowPilotPortraitTT(showPilotPortraitTT.isSelected());

        GUIP.setWarningColor(csbWarningColor.getColour());
        GUIP.setCautionColor(csbCautionColor.getColour());
        GUIP.setPrecautionColor(csbPrecautionColor.getColour());
        GUIP.setOkColor(csbOkColor.getColour());

        GUIP.setMyUnitColor(csbMyUnitColor.getColour());
        GUIP.setAllyUnitColor(csbAllyUnitColor.getColour());
        GUIP.setEnemyUnitColor(csbEnemyColor.getColour());

        for (PlayerColourHelper pch : playerColours) {
            GUIP.setColor(pch.pc.getText(), pch.csb.getColour());
        }

        GUIP.setMoveDefaultColor(csbMoveDefaultColor.getColour());
        GUIP.setMoveIllegalColor(csbMoveIllegalColor.getColour());
        GUIP.setMoveJumpColor(csbMoveJumpColor.getColour());
        GUIP.setMoveMASCColor(csbMoveMASCColor.getColour());
        GUIP.setMoveRunColor(csbMoveRunColor.getColour());
        GUIP.setMoveBackColor(csbMoveBackColor.getColour());
        GUIP.setMoveSprintColor(csbMoveSprintColor.getColour());

        GUIP.setFireSolutionCanSeeColor(csbFireSolutionCanSeeColor.getColour());
        GUIP.setFireSolutionNoSeeColor(csbFireSolutionNoSeeColor.getColour());
        GUIP.setFieldOfFireMinColor(csbFieldOfFireMinColor.getColour());
        GUIP.setFieldOfFireShortColor(csbFieldOfFireShortColor.getColour());
        GUIP.setBoardFieldOfFireMediumColor(csbFieldOfFireMediumColor.getColour());
        GUIP.setFieldOfFireLongColor(csbFieldOfFireLongColor.getColour());
        GUIP.setFieldOfFireExtremeColor(csbFieldOfFireExtremeColor.getColour());

        GUIP.setSensorRangeColor(csbSensorRangeColor.getColour());
        GUIP.setVisualRangeColor(csbVisualRangeColor.getColour());

        GUIP.setUnitValidColor(csbUnitValidColor.getColour());
        GUIP.setUnitSelectedColor(csbUnitSelectedColor.getColour());
        GUIP.setUnitOverviewTextColor(csbUnitTextColor.getColour());

        GUIP.setBuildingTextColor(csbBuildingTextColor.getColour());
        GUIP.setBoardTextColor(csbBoardTextColor.getColour());
        GUIP.setBoardSpaceTextColor(csbBoardSpaceTextColor.getColour());
        GUIP.setLowFoliageColor(csbLowFoliageColor.getColour());
        GUIP.setDemolitionChargeColor(csbDemolitionChargeColor.getColour());
        GUIP.setDemolitionChargeHazardOutline(demolitionChargeHazardOutline.isSelected());
        GUIP.setMapSheetColor(csbMapSheetColor.getColour());

        GUIP.setAttackArrowTransparency((Integer) attackArrowTransparency.getValue());
        GUIP.setECMTransparency((Integer) ecmTransparency.getValue());
        GUIP.setDrawFacingArrowsOnMiniMap(drawFacingArrowsOnMiniMap.isSelected());
        GUIP.setDrawSensorRangeOnMiniMap(drawSensorRangeOnMiniMap.isSelected());
        GUIP.setPaintBorders(paintBordersOnMiniMap.isSelected());
        GUIP.setShowUnitDisplayNamesOnMinimap(showUnitDisplayNamesOnMinimap.isSelected());
        GUIP.setButtonsPerRow((int) buttonsPerRow.getValue());
        GUIP.setPlayersRemainingToShow((int) playersRemainingToShow.getValue());

        GUIP.setTMMPipMode(tmmPipModeCbo.getSelectedIndex());
        GUIP.setDarkenMapAtNight(darkenMapAtNight.isSelected());
        GUIP.setTranslucentHiddenUnits(translucentHiddenUnits.isSelected());

        GUIP.setShowArtilleryMisses(artilleryDisplayMisses.isSelected());
        GUIP.setShowArtilleryDrifts(artilleryDisplayDriftedHits.isSelected());
        GUIP.setShowArtilleryDriftArrows(artilleryDisplayDriftArrows.isSelected());
        GUIP.setShowBombMisses(bombsDisplayMisses.isSelected());
        GUIP.setShowBombDrifts(bombsDisplayDrifts.isSelected());

        Object selectedChooserMoveFond = fontTypeChooserMoveFont.getSelectedItem();
        if (selectedChooserMoveFond != null) {
            GUIP.setMoveFontType(fontTypeChooserMoveFont.getSelectedItem().toString());
            GUIP.setMoveFontSize((int) moveFontSize.getValue());
        }

        GUIP.setMoveFontStyle(fontStyleChooserMoveFont.getSelectedIndex());
        GUIP.setTooltipDelay((int) tooltipDelay.getValue());
        GUIP.setTooltipDismissDelay((int) tooltipDismissDelay.getValue());
        GUIP.setTooltipDistSuppression((int) tooltipDistSuppression.getValue());

        GUIP.setValue(GUIPreferences.GUI_SCALE, (float) (guiScale.getValue()) / 10);

        Object unitSelected = unitStartChar.getSelectedItem();
        if (unitSelected instanceof String unitStart) {
            CLIENT_PREFERENCES.setUnitStartChar(unitStart.charAt(0));
        }

        GUIP.setMouseWheelZoom(mouseWheelZoom.isSelected());
        GUIP.setMouseWheelZoomFlip(mouseWheelZoomFlip.isSelected());

        GUIP.setMoveDefaultClimbMode(moveDefaultClimbMode.isSelected());

        GUIP.setMasterVolume(masterVolumeSlider.getValue());
        GUIP.setSoundMuteChat(soundMuteChat.isSelected());
        GUIP.setSoundMuteMyTurn(soundMuteMyTurn.isSelected());
        GUIP.setSoundMuteOthersTurn(soundMuteOthersTurn.isSelected());

        GUIP.setSoundBingFilenameChat(tfSoundMuteChatFileName.getText());
        GUIP.setSoundBingFilenameMyTurn(tfSoundMuteMyTurnFileName.getText());
        GUIP.setSoundBingFilenameOthersTurn(tfSoundMuteOthersFileName.getText());

        CLIENT_PREFERENCES.setMaxPathfinderTime((int) maxPathfinderTime.getValue());

        GUIP.setGetFocus(getFocus.isSelected());

        CLIENT_PREFERENCES.setKeepGameLog(keepGameLog.isSelected());
        CLIENT_PREFERENCES.setDataLogging(datasetLogging.isSelected());
        CLIENT_PREFERENCES.setGameLogFilename(gameLogFilename.getText());
        CLIENT_PREFERENCES.setAutoResolveGameLogFilename(autoResolveLogFilename.getText());
        CLIENT_PREFERENCES.setUserDir(userDir.getText());
        CLIENT_PREFERENCES.setMmlPath(mmlPath.getText());
        CLIENT_PREFERENCES.setStampFilenames(stampFilenames.isSelected());
        CLIENT_PREFERENCES.setStampFormat(stampFormat.getText());
        CLIENT_PREFERENCES.setReportKeywords(reportKeywordsTextArea.getText());
        CLIENT_PREFERENCES.setReportFilterKeywords(reportFilterKeywordsTextArea.getText());
        CLIENT_PREFERENCES.setShowIPAddressesInChat(showIPAddressesInChat.isSelected());
        CLIENT_PREFERENCES.setSpritesOnly(spritesOnly.isSelected());
        CLIENT_PREFERENCES.setEnableExperimentalBotFeatures(enableExperimentalBotFeatures.isSelected());
        CLIENT_PREFERENCES.setDefaultAutoEjectDisabled(defaultAutoEjectDisabled.isSelected());
        CLIENT_PREFERENCES.setUseAverageSkills(useAverageSkills.isSelected());
        CLIENT_PREFERENCES.setGenerateNames(generateNames.isSelected());
        CLIENT_PREFERENCES.setShowUnitId(showUnitId.isSelected());
        CLIENT_PREFERENCES.setShowAutoResolvePanel(showAutoResolvePanel.isSelected());
        CLIENT_PREFERENCES.setFavoritePrincessBehaviorSetting(
              (String) favoritePrincessBehaviorSetting.getSelectedItem());
        if ((clientgui != null) && (clientgui.getBoardView() != null)) {
            clientgui.getBoardView().updateEntityLabels();
        }

        CLIENT_PREFERENCES.setLocale(CommonSettingsDialog.LOCALE_CHOICES[displayLocale.getSelectedIndex()]);
        GUIP.setShowMapSheets(showMapSheets.isSelected());
        GUIP.setAOHexShadows(aOHexShadows.isSelected());
        GUIP.setFloatingIso(floatingIso.isSelected());
        GUIP.setMmSymbol(mmSymbol.isSelected());
        GUIP.setLevelHighlight(levelHighlight.isSelected());
        GUIP.setShadowMap(shadowMap.isSelected());
        GUIP.setHexInclines(hexInclines.isSelected());
        GUIP.setSoftCenter(useSoftCenter.isSelected());
        GUIP.setAutoCenter(useAutoCenter.isSelected());
        GUIP.setAutoSelectNextUnit(useAutoSelectNext.isSelected());
        GUIP.setGameSummaryBoardView(gameSummaryBV.isSelected());
        GUIP.setGameSummaryMinimap(gameSummaryMM.isSelected());
        int selectedRecordingIndex = gifGameSummaryRecording.getSelectedIndex();
        GifRecordingMode[] recordingModes = GifRecordingMode.values();
        boolean isValidRecordingIndex = (selectedRecordingIndex >= 0)
              && (selectedRecordingIndex < recordingModes.length);
        GUIP.setGifGameSummaryRecording(isValidRecordingIndex
              ? recordingModes[selectedRecordingIndex]
              : GifRecordingMode.ASK);
        GUIP.setShowUnitDisplayNamesOnMinimap(showUnitDisplayNamesOnMinimap.isSelected());
        UITheme newUITheme = (UITheme) uiThemes.getSelectedItem();
        String oldUITheme = GUIP.getUITheme();
        if (newUITheme != null && !oldUITheme.equals(newUITheme.getClassName())) {
            GUIP.setUITheme(newUITheme.getClassName());
        }

        String newSkinFile = (String) skinFiles.getSelectedItem();
        String oldSkinFile = GUIP.getSkinFile();
        if ((oldSkinFile == null) || !(oldSkinFile.equals(newSkinFile))) {
            boolean success = SkinXMLHandler.initSkinXMLHandler(newSkinFile);
            if (!success) {
                SkinXMLHandler.initSkinXMLHandler(oldSkinFile);
                String title = Messages.getString("CommonSettingsDialog.skinFileFail.title");
                String msg = Messages.getString("CommonSettingsDialog.skinFileFail.msg");
                JOptionPane.showMessageDialog(getFrame(), msg, title, JOptionPane.ERROR_MESSAGE);
            } else {
                GUIP.setSkinFile(newSkinFile);
            }
        }

        if (tileSetChoice.getSelectedIndex() >= 0) {
            String tileSetFileName = tileSets.get(tileSetChoice.getSelectedIndex());
            if (!CLIENT_PREFERENCES.getMapTileset().equals(tileSetFileName)
                  && (clientgui != null)
                  && (clientgui.getBoardView() != null)) {
                clientgui.getBoardView().clearShadowMap();
            }
            CLIENT_PREFERENCES.setMapTileset(tileSetFileName);
        }

        CLIENT_PREFERENCES.setMinimapTheme(minimapTheme.getSelectedItem());

        ToolTipManager.sharedInstance().setInitialDelay(GUIP.getTooltipDelay());
        if (GUIP.getTooltipDismissDelay() > 0) {
            ToolTipManager.sharedInstance().setDismissDelay(GUIP.getTooltipDismissDelay());
        } else {
            ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);
        }

        // Check if any keybinds have changed and, if so, save them
        boolean bindsChanged = false;
        for (KeyCommandBind kcb : KeyCommandBind.values()) {
            int modifiers = modifierCode(kcb);
            int keyCode = keyCode(kcb);
            bindsChanged |= (kcb.modifiers != modifiers) || (kcb.key != keyCode);
            kcb.modifiers = modifiers;
            kcb.key = keyCode;
        }

        if (bindsChanged) {
            KeyBindParser.writeKeyBindings();
        }

        // Button Order
        // Movement
        boolean buttonOrderChanged = false;
        for (int i = 0;
              i < movePhaseCommands.getSize();
              i++) {
            StatusBarPhaseDisplay.PhaseCommand cmd = movePhaseCommands.get(i);
            if (cmd.getPriority() != i) {
                cmd.setPriority(i);
                BOP.setValue(cmd.getCmd(), i);
                buttonOrderChanged = true;
            }
        }

        // Need to do stuff if the order changes.
        if (buttonOrderChanged && (clientgui != null)) {
            clientgui.updateButtonPanel(GamePhase.MOVEMENT);
        }

        // Deploy
        buttonOrderChanged = false;
        for (int i = 0;
              i < deployPhaseCommands.getSize();
              i++) {
            StatusBarPhaseDisplay.PhaseCommand cmd = deployPhaseCommands.get(i);
            if (cmd.getPriority() != i) {
                cmd.setPriority(i);
                BOP.setValue(cmd.getCmd(), i);
                buttonOrderChanged = true;
            }
        }

        // Need to do stuff if the order changes.
        if (buttonOrderChanged && (clientgui != null)) {
            clientgui.updateButtonPanel(GamePhase.DEPLOYMENT);
        }

        // Firing
        buttonOrderChanged = false;
        for (int i = 0;
              i < firingPhaseCommands.getSize();
              i++) {
            StatusBarPhaseDisplay.PhaseCommand cmd = firingPhaseCommands.get(i);
            if (cmd.getPriority() != i) {
                cmd.setPriority(i);
                BOP.setValue(cmd.getCmd(), i);
                buttonOrderChanged = true;
            }
        }

        // Need to do stuff if the order changes.
        if (buttonOrderChanged && (clientgui != null)) {
            clientgui.updateButtonPanel(GamePhase.FIRING);
        }

        // Physical
        buttonOrderChanged = false;
        for (int i = 0;
              i < physicalPhaseCommands.getSize();
              i++) {
            StatusBarPhaseDisplay.PhaseCommand cmd = physicalPhaseCommands.get(i);
            if (cmd.getPriority() != i) {
                cmd.setPriority(i);
                BOP.setValue(cmd.getCmd(), i);
                buttonOrderChanged = true;
            }
        }

        // Need to do stuff if the order changes.
        if (buttonOrderChanged && (clientgui != null)) {
            clientgui.updateButtonPanel(GamePhase.PHYSICAL);
        }

        // Targeting
        buttonOrderChanged = false;
        for (int i = 0;
              i < targetingPhaseCommands.getSize();
              i++) {
            StatusBarPhaseDisplay.PhaseCommand cmd = targetingPhaseCommands.get(i);
            if (cmd.getPriority() != i) {
                cmd.setPriority(i);
                BOP.setValue(cmd.getCmd(), i);
                buttonOrderChanged = true;
            }
        }

        // Need to do stuff if the order changes.
        if (buttonOrderChanged && (clientgui != null)) {
            clientgui.updateButtonPanel(GamePhase.TARGETING);
        }

                String[] unitDisplayPositions = {
                        UnitDisplayPanel.NON_TABBED_A1, UnitDisplayPanel.NON_TABBED_B1, UnitDisplayPanel.NON_TABBED_C1,
                        UnitDisplayPanel.NON_TABBED_A2, UnitDisplayPanel.NON_TABBED_B2, UnitDisplayPanel.NON_TABBED_C2
                };
                boolean unitDisplayNonTabbedChanged = false;
                int savedPositions = Math.min(unitDisplayNonTabbed.getSize(), unitDisplayPositions.length);
                for (int index = 0; index < savedPositions; index++) {
                        String position = unitDisplayPositions[index];
                        String panelName = unitDisplayNonTabbed.get(index);
                        if (!panelName.equals(UNIT_DISPLAY_ORDER_PREFERENCES.getString(position))) {
                                UNIT_DISPLAY_ORDER_PREFERENCES.setValue(position, panelName);
                                unitDisplayNonTabbedChanged = true;
                        }
                }
                if (unitDisplayNonTabbedChanged && (clientgui != null) && !GUIP.getUnitDisplayStartTabbed()) {
                        clientgui.getUnitDisplay().setDisplayNonTabbed();
        }

        GUIP.setUnitDisplayAutoDisplayReportPhase(unitDisplayAutoDisplayReportCombo.getSelectedIndex());
        GUIP.setUnitDisplayAutoDisplayNonReportPhase(unitDisplayAutoDisplayNonReportCombo.getSelectedIndex());
        GUIP.setMinimapAutoDisplayReportPhase(miniMapAutoDisplayReportCombo.getSelectedIndex());
        GUIP.setMinimapAutoDisplayNonReportPhase(miniMapAutoDisplayNonReportCombo.getSelectedIndex());
        GUIP.setMiniReportAutoDisplayReportPhase(miniReportAutoDisplayReportCombo.getSelectedIndex());
        GUIP.setMiniReportAutoDisplayNonReportPhase(miniReportAutoDisplayNonReportCombo.getSelectedIndex());
        GUIP.setPlayerListAutoDisplayReportPhase(playerListAutoDisplayReportCombo.getSelectedIndex());
        GUIP.setPlayerListAutoDisplayNonReportPhase(playerListAutoDisplayNonReportCombo.getSelectedIndex());
        GUIP.setForceDisplayAutoDisplayReportPhase(forceDisplayAutoDisplayReportCombo.getSelectedIndex());
        GUIP.setForceDisplayAutoDisplayNonReportPhase(forceDisplayAutoDisplayNonReportCombo.getSelectedIndex());
        GUIP.setBotCommandAutoDisplayReportPhase(botCommandsAutoDisplayReportCombo.getSelectedIndex());
        GUIP.setBotCommandAutoDisplayNonReportPhase(botCommandsAutoDisplayNonReportCombo.getSelectedIndex());
        GUIP.setMoveDisplayTabDuringMovePhases(displayMoveDisplayDuringMovePhases.isSelected());
        GUIP.setFireDisplayTabDuringFiringPhases(displayFireDisplayDuringFirePhases.isSelected());

        GUIP.setUnitDisplayHeatColorLevel1(csbUnitDisplayHeatLevel1.getColour());
        GUIP.setUnitDisplayHeatColorLevel2(csbUnitDisplayHeatLevel2.getColour());
        GUIP.setUnitDisplayHeatColorLevel3(csbUnitDisplayHeatLevel3.getColour());
        GUIP.setUnitDisplayHeatColorLevel4(csbUnitDisplayHeatLevel4.getColour());
        GUIP.setUnitDisplayHeatColorLevel5(csbUnitDisplayHeatLevel5.getColour());
        GUIP.setUnitDisplayHeatColorLevel6(csbUnitDisplayHeatLevel6.getColour());
        GUIP.setUnitDisplayHeatColorLevelOverHeat(csbUnitDisplayHeatLevelOverheat.getColour());
        GUIP.setUnitDisplayHeatColorValue1((Integer) unitDisplayHeatLevel1Spinner.getValue());
        GUIP.setUnitDisplayHeatColorValue2((Integer) unitDisplayHeatLevel2Spinner.getValue());
        GUIP.setUnitDisplayHeatColorValue3((Integer) unitDisplayHeatLevel3Spinner.getValue());
        GUIP.setUnitDisplayHeatColorValue4((Integer) unitDisplayHeatLevel4Spinner.getValue());
        GUIP.setUnitDisplayHeatColorValue5((Integer) unitDisplayHeatLevel5Spinner.getValue());
        GUIP.setUnitDisplayHeatColorValue6((Integer) unitDisplayHeatLevel6Spinner.getValue());

        GUIP.setUnitToolTipSeenByResolution(unitTooltipSeenByCbo.getSelectedIndex());
        GUIP.setUnitDisplayWeaponListHeight((Integer) unitDisplayWeaponListHeightSpinner.getValue());

        GUIP.setUnitDisplayMekArmorLargeFontSize((Integer) unitDisplayMekArmorLargeFontSizeSpinner.getValue());
        GUIP.setUnitDisplayMekArmorMediumFontSize((Integer) unitDisplayMekArmorMediumFontSizeSpinner.getValue());
        GUIP.setUnitDisplayMekArmorSmallFontSize((Integer) unitDisplayMekArmorSmallFontSizeSpinner.getValue());
        GUIP.setUnitDisplayMekLargeFontSize((Integer) unitDisplayMekLargeFontSizeSpinner.getValue());
        GUIP.setUnitDisplayMekMediumFontSize((Integer) unitDisplayMekMediumFontSizeSpinner.getValue());

        GUIP.setUnitToolTipFGColor(csbUnitTooltipFGColor.getColour());
        GUIP.setUnitTooltipLightFGColor(csbUnitTooltipLightFGColor.getColour());
        GUIP.setUnitTooltipBuildingFGColor(csbUnitTooltipBuildingFGColor.getColour());
        GUIP.setUnitTooltipAltFGColor(csbUnitTooltipAltFGColor.getColour());
        GUIP.setUnitTooltipBlockFGColor(csbUnitTooltipBlockFGColor.getColour());
        GUIP.setUnitTooltipTerrainFGColor(csbUnitTooltipTerrainFGColor.getColour());
        GUIP.setUnitToolTipBGColor(csbUnitTooltipBGColor.getColour());
        GUIP.setUnitTooltipBuildingBGColor(csbUnitTooltipBuildingBGColor.getColour());
        GUIP.setUnitTooltipAltBGColor(csbUnitTooltipAltBGColor.getColour());
        GUIP.setUnitTooltipBlockBGColor(csbUnitTooltipBlockBGColor.getColour());
        GUIP.setUnitTooltipTerrainBGColor(csbUnitTooltipTerrainBGColor.getColour());

        GUIP.setUnitTooltipHighlightColor(csbUnitTooltipHighlightColor.getColour());
        GUIP.setUnitTooltipWeaponColor(csbUnitTooltipQuirkColor.getColour());
        GUIP.setUnitTooltipQuirkColor(csbUnitTooltipWeaponColor.getColour());

        GUIP.setUnitTooltipArmorMiniColorIntact(csbUnitTooltipArmorMiniIntact.getColour());
        GUIP.setUnitTooltipArmorMiniColorPartialDamage(csbUnitTooltipArmorMiniPartial.getColour());
        GUIP.setUnitTooltipArmorMiniColorDamaged(csbUnitTooltipArmorMiniDamaged.getColour());
          GUIP.setUnitToolTipArmorMiniArmorChar(selectedTooltipSymbol(unitTooltipArmorMiniArmorCharCbo));
          GUIP.setUnitToolTipArmorMiniISChar(selectedTooltipSymbol(unitTooltipArmorMiniInternalStructureCharCbo));
          GUIP.setUnitToolTipArmorMiniCriticalChar(selectedTooltipSymbol(unitTooltipArmorMiniCriticalCharCbo));
          GUIP.setUnitTooltipArmorMiniDestroyedChar(selectedTooltipSymbol(unitTooltipArmorMiniDestroyedCharCbo));
          GUIP.setUnitTooltipArmorMiniCapArmorChar(selectedTooltipSymbol(unitTooltipArmorMiniCapArmorCharCbo));

          GUIP.setUnitTooltipArmorMiniUnitsPerBlock((int) unitTooltipArmorMiniUnitsPerBlockSpinner.getValue());
        GUIP.setUnitToolTipFontSize((String) unitTooltipFontSizeModCbo.getSelectedItem());

        Object unitToolTipFontSize = unitTooltipFontSizeModCbo.getSelectedItem();

        if (unitToolTipFontSize instanceof String fontSize) {
            GUIP.setUnitToolTipFontSize(fontSize);
        }

        GUIP.setReportLinkColor(csbReportLinkColor.getColour());
        GUIP.setReportSuccessColor(csbReportSuccessColor.getColour());
        GUIP.setReportMissColor(csbReportMissColor.getColour());
        GUIP.setReportInfoColo(csbReportInfoColor.getColour());

        Object fontTypeChooserReport = fontTypeChooserReportFont.getSelectedItem();
        if (fontTypeChooserReport instanceof String fontTypeChosen) {
            GUIP.setReportFontType(fontTypeChosen);
        }

        GUIP.setMiniReportShowSprites(showReportSprites.isSelected());
        GUIP.setMiniReportShowPlayers(chkReportShowPlayers.isSelected());
        GUIP.setMiniReportShowUnits(chkReportShowUnits.isSelected());
        GUIP.setMiniReportShowKeywords(chkReportShowKeywords.isSelected());
        GUIP.setMiniReportShowFilter(chkReportShowFilter.isSelected());
        if ((clientgui != null) && (clientgui.getMiniReportDisplay() != null)) {
            clientgui.getMiniReportDisplay().refreshSearchPanel();
        }

        GUIP.setUnitOverviewTextShadowColor(csbUnitOverviewTextShadowColor.getColour());
        GUIP.setUnitOverviewConditionShadowColor(csbUnitOverviewConditionShadowColor.getColour());

        GUIP.setPlanetaryConditionsColorTitle(csbPlanetaryConditionsColorTitle.getColour());
        GUIP.setPlanetaryConditionsColorText(csbPlanetaryConditionsColorText.getColour());
        GUIP.setPlanetaryConditionsColorBackground(csbPlanetaryConditionsColorBackground.getColour());
        GUIP.setPlanetaryConditionsColorCold(csbPlanetaryConditionsColorCold.getColour());
        GUIP.setPlanetaryConditionsColorHot(csbPlanetaryConditionsColorHot.getColour());

        GUIP.setPlanetaryConditionsShowDefaults(planetaryConditionsShowDefaults.isSelected());
        GUIP.setPlanetaryConditionsShowHeader(planetaryConditionsShowHeader.isSelected());
        GUIP.setPlanetaryConditionsShowLabels(planetaryConditionsShowLabels.isSelected());
        GUIP.setPlanetaryConditionsShowValues(planetaryConditionsShowValues.isSelected());
        GUIP.setPlanetaryConditionsShowIndicators(planetaryConditionsShowIndicators.isSelected());
        GUIP.setPlanetaryConditionsBackgroundTransparency(
              (Integer) planetaryConditionsBackgroundTransparency.getValue());

        GUIP.setToastEnabled(toastEnabled.isSelected());
        GUIP.setToastDurationSeconds((Integer) toastDurationSpinner.getValue());
        GUIP.setToastDripSeconds((Integer) toastDripSpinner.getValue());
        GUIP.setToastReportEvents(toastReportEvents.isSelected());

        GUIP.setTraceOverlayTransparency(traceOverlayTransparencySlider.getValue());
        GUIP.setTraceOverlayScale(traceOverlayScaleSlider.getValue());
        GUIP.setTraceOverlayOriginX(traceOverlayOriginXSlider.getValue());
        GUIP.setTraceOverlayOriginY(traceOverlayOriginYSlider.getValue());
        GUIP.setTraceOverlayImageFile(traceOverlayImageFile.getText());

        setVisible(false);
    }

    /** Handle some setting changes that directly update e.g. the board. */
    @Override
    public void itemStateChanged(ItemEvent event) {
        Object source = event.getItemSelectable();
        if (source.equals(keepGameLog)) {
            gameLogFilename.setEnabled(keepGameLog.isSelected());
            stampFormatLabel.setEnabled(stampFilenames.isSelected());
            gameLogFilenameLabel.setEnabled(keepGameLog.isSelected());
            // gameLogMaxSize.setEnabled(keepGameLog.isSelected());
        } else if (source.equals(stampFilenames)) {
            stampFormat.setEnabled(stampFilenames.isSelected());
            stampFormatLabel.setEnabled(stampFilenames.isSelected());
        } else if (source.equals(toastEnabled)) {
            setToastControlsEnabled(toastEnabled.isSelected());
        } else if (source.equals(fovInsideEnabled)) {
            GUIP.setFovHighlight(fovInsideEnabled.isSelected());
            fovHighlightAlpha.setEnabled(fovInsideEnabled.isSelected());
            fovHighlightOpacityPercent.setEnabled(fovInsideEnabled.isSelected());
            fovHighlightRingsEditor.setEditorEnabled(fovInsideEnabled.isSelected());
            fovHighlightRangesLabel.setEnabled(fovInsideEnabled.isSelected());
            highlightAlphaLabel.setEnabled(fovInsideEnabled.isSelected());
        } else if (source.equals(fovOutsideEnabled)) {
            GUIP.setFovDarken(fovOutsideEnabled.isSelected());
            fovDarkenAlpha.setEnabled(fovOutsideEnabled.isSelected());
            fovDarkenOpacityPercent.setEnabled(fovOutsideEnabled.isSelected());
            fovStripesSpinner.setEnabled(fovOutsideEnabled.isSelected());
            darkenAlphaLabel.setEnabled(fovOutsideEnabled.isSelected());
            numStripesLabel.setEnabled(fovOutsideEnabled.isSelected());
            fovGrayscaleEnabled.setEnabled(fovOutsideEnabled.isSelected());
        } else if (source.equals(fovGrayscaleEnabled)) {
            GUIP.setFovGrayscale(fovGrayscaleEnabled.isSelected());
        } else if (source.equals(aOHexShadows)) {
            GUIP.setAOHexShadows(aOHexShadows.isSelected());
        } else if (source.equals(shadowMap)) {
            GUIP.setShadowMap(shadowMap.isSelected());
        } else if (source.equals(hexInclines)) {
            GUIP.setHexInclines(hexInclines.isSelected());
        } else if (source.equals(levelHighlight)) {
            GUIP.setLevelHighlight(levelHighlight.isSelected());
        } else if (source.equals(floatingIso)) {
            GUIP.setFloatingIso(floatingIso.isSelected());
        } else if (source.equals(mmSymbol)) {
            GUIP.setMmSymbol(mmSymbol.isSelected());
        } else if (source.equals(teamColoring)) {
            GUIP.setTeamColoring(teamColoring.isSelected());
        } else if (source.equals(entityOwnerColor)) {
            GUIP.setUnitLabelBorder(entityOwnerColor.isSelected());
        } else if (source.equals(showDamageDecal)) {
            GUIP.setShowDamageDecal(showDamageDecal.isSelected());
        } else if (source.equals(showDamageLevel)) {
            GUIP.setShowDamageLevel(showDamageLevel.isSelected());
        } else if (source.equals(chkHighQualityGraphics)) {
            GUIP.setHighQualityGraphics(chkHighQualityGraphics.isSelected());
        } else if (source.equals(chkHighPerformanceGraphics)) {
            GUIP.setHighPerformanceGraphics(chkHighPerformanceGraphics.isSelected());
        } else if (source.equals(drawFacingArrowsOnMiniMap)) {
            GUIP.setDrawFacingArrowsOnMiniMap(drawFacingArrowsOnMiniMap.isSelected());
        } else if (source.equals(drawSensorRangeOnMiniMap)) {
            GUIP.setDrawFacingArrowsOnMiniMap(drawSensorRangeOnMiniMap.isSelected());
        } else if (source.equals(paintBordersOnMiniMap)) {
            GUIP.setPaintBorders(paintBordersOnMiniMap.isSelected());
        } else if (source.equals(movePathPersistenceOnMiniMap)) {
            GUIP.setMovePathPersistenceOnMiniMap((int) movePathPersistenceOnMiniMap.getValue());
        } else if (source.equals(showUnitDisplayNamesOnMinimap)) {
            GUIP.setShowUnitDisplayNamesOnMinimap(showUnitDisplayNamesOnMinimap.isSelected());
        }
    }

    @Override
    public void focusGained(FocusEvent e) {

    }

    @Override
    public void focusLost(FocusEvent e) {
        GUIPreferences guip = GUIP;
        // For Advanced options
        String option = "Advanced" + advancedKeys.getModel().getElementAt(advancedKeyIndex).option;
        savedAdvancedOpt.put(option, guip.getString(option));
        guip.setValue(option, advancedValue.getText());
            }

    private void saveFovHighlightRanges() {
        GUIP.setFovHighlightRingsRadii(fovHighlightRingsEditor.getRadiiValue());
        GUIP.setFovHighlightRingsColorsHsb(fovHighlightRingsEditor.getColoursValue());
    }

    private List<CommonSettingsPane.OptionSection> getKeyBindSections() {
        return createKeyBindSections(getKeyBindSectionContent());
    }

    static List<CommonSettingsPane.OptionSection> createKeyBindSections(JComponent content) {
        return List.of(new CommonSettingsPane.OptionSection("keyBinds.commands",
              Messages.getString("CommonSettingsDialog.section.keyBinds.commands.title"),
              Messages.getString("CommonSettingsDialog.section.keyBinds.commands.summary"),
              content, false));
    }

    static SettingsCheckBox createKeyBindTabNavigationControl() {
        return new SettingsCheckBox(SETTINGS_TEXT, "CommonSettingsDialog.keyBinds.tabNavigation",
              List.of(IMPORTANT_BADGE));
    }

    static SettingsButton createKeyBindResetButton() {
        return new SettingsButton("default", SETTINGS_TEXT, "CommonSettingsDialog.keyBinds.buttonDefault");
    }

    private JComponent getKeyBindSectionContent() {
        defaultKeyBindButton.addActionListener(e -> updateKeybindsDefault());
        String msg_esc = Messages.getString("CommonSettingsDialog.keyBinds.escMessage");
        keyBindTabNavigation.addActionListener(e -> updateKeybindsFocusTraversal());

        // Create header: labels for describing what each column does
          JLabel commandHeader = new JLabel(Messages.getString("CommonSettingsDialog.keyBinds.column.command"),
              SwingConstants.CENTER);
        String msg_tooltipName = Messages.getString("CommonSettingsDialog.keyBinds.tooltipName");
          commandHeader.setToolTipText(msg_tooltipName);
        String modifierColumn = Messages.getString("CommonSettingsDialog.keyBinds.column.modifier");
          JLabel modifierHeader = new JLabel(modifierColumn, SwingConstants.CENTER);
        String msg_tooltipModifier = Messages.getString("CommonSettingsDialog.keyBinds.tooltipModifier");
          modifierHeader.setToolTipText(msg_tooltipModifier);
        String keyColumn = Messages.getString("CommonSettingsDialog.keyBinds.column.key");
          JLabel keyHeader = new JLabel(keyColumn, SwingConstants.CENTER);
        String msg_tooltipKey = Messages.getString("CommonSettingsDialog.keyBinds.tooltipKey");
        String keyHelp = msg_tooltipKey + " " + msg_esc;
          keyHeader.setToolTipText(keyHelp);
          JPanel keyBinds = createKeyBindGrid(commandHeader, modifierHeader, keyHeader);
          int keyBindRow = 2;

        // Create maps to retrieve the text fields for saving
        int numBinds = KeyCommandBind.values().length;
        cmdModifierMap = new HashMap<>((int) (numBinds * 1.26));
        cmdKeyMap = new HashMap<>((int) (numBinds * 1.26));
        cmdKeyCodeMap = new HashMap<>((int) (numBinds * 1.26));

        // For each keyCommandBind, create a label and two text fields
        for (KeyCommandBind kcb : KeyCommandBind.values()) {
            String commandName = Messages.getString("KeyBinds.cmdNames." + kcb.cmd);
            JLabel name = new JLabel(commandName, SwingConstants.RIGHT);
            name.setToolTipText(Messages.getString("KeyBinds.cmdDesc." + kcb.cmd));

            final SettingsTextField modifiers = new SettingsTextField(SETTINGS_TEXT,
                "CommonSettingsDialog.keyBinds.modifierField");
            modifiers.setColumns(10);
            modifiers.setText(KeyEvent.getModifiersExText(kcb.modifiers));
            modifiers.getAccessibleContext().setAccessibleName(commandName + " " + modifierColumn);
            for (KeyListener kl : modifiers.getKeyListeners()) {
                modifiers.removeKeyListener(kl);
            }

            // Update how typing in the text field works
            modifiers.addKeyListener(new KeyListener() {

                @Override
                public void keyPressed(KeyEvent evt) {
                    modifiers.setText(KeyEvent.getModifiersExText(evt.getModifiersEx()));
                    markDuplicateBinds();
                    evt.consume();
                }

                @Override
                public void keyReleased(KeyEvent evt) {
                }

                @Override
                public void keyTyped(KeyEvent evt) {
                    // This might be a bit hackish, but we want to deal with
                    // the key code, so the code to update the text is in
                    // keyPressed. We've already done what we want with the
                    // typed key, and we don't want anything else acting upon
                    // the key typed event, so we consume it here.
                    evt.consume();
                }

            });
            cmdModifierMap.put(kcb.cmd, modifiers);
            final SettingsTextField key = new SettingsTextField(SETTINGS_TEXT,
                "CommonSettingsDialog.keyBinds.keyField");
            key.setColumns(10);
            key.setName(kcb.cmd);
            if (kcb.key == 0) {
                key.setText("");
            } else {
                key.setText(KeyEvent.getKeyText(kcb.key));
            }
            key.getAccessibleContext().setAccessibleName(commandName + " " + keyColumn);

            // Update how typing in the text field works
            final String cmd = kcb.cmd;
            cmdKeyMap.put(cmd, key);
            cmdKeyCodeMap.put(cmd, kcb.key);

            key.addKeyListener(new KeyListener() {

                @Override
                public void keyPressed(KeyEvent evt) {
                    if (evt.getKeyCode() == KeyEvent.VK_ESCAPE) { // Unbind command with Esc
                        key.setText("");
                        modifiers.setText("");
                        cmdKeyCodeMap.put(kcb.cmd, 0);
                    } else {
                        // Don't consume this event if modifiers are held (-> enable button mnemonics)
                        if (evt.getModifiersEx() != 0) {
                            return;
                        }
                        key.setText(KeyEvent.getKeyText(evt.getKeyCode()));
                        cmdKeyCodeMap.put(kcb.cmd, evt.getKeyCode());
                    }
                    markDuplicateBinds();
                    evt.consume();
                }

                @Override
                public void keyReleased(KeyEvent evt) {
                }

                @Override
                public void keyTyped(KeyEvent evt) {
                    // This might be a bit hackish, but we want to deal with
                    // the key code, so the code to update the text is in
                    // keyPressed. We've already done what we want with the
                    // typed key, and we don't want anything else acting upon
                    // the key typed event, so we consume it here.
                    evt.consume();
                }

            });
            addKeyBindGridRow(keyBinds, keyBindRow++, name, modifiers, key);

            // deactivate TAB-bing through fields here so TAB can be caught as a keybind
            modifiers.setFocusTraversalKeysEnabled(false);
            key.setFocusTraversalKeysEnabled(false);
        }
        markDuplicateBinds();
        return createKeyBindSectionContent(keyBindTabNavigation, defaultKeyBindButton, keyBinds);
        }

    static JPanel createKeyBindGrid(JComponent commandHeader, JComponent modifierHeader, JComponent keyHeader) {
        JPanel grid = new JPanel(new GridBagLayout());
        grid.setName("pnlCommonSettingsKeyBindingsGrid");
        grid.setOpaque(false);
        addKeyBindGridRow(grid, 0, commandHeader, modifierHeader, keyHeader);

        GridBagConstraints separatorLayout = new GridBagConstraints();
        separatorLayout.gridx = 0;
        separatorLayout.gridy = 1;
        separatorLayout.gridwidth = 3;
        separatorLayout.fill = GridBagConstraints.HORIZONTAL;
        separatorLayout.insets = new Insets(0, 10, 5, 10);
        grid.add(new JSeparator(SwingConstants.HORIZONTAL), separatorLayout);
        return grid;
    }

    static void addKeyBindGridRow(JPanel grid, int row, JComponent command, JComponent modifier, JComponent key) {
        List<JComponent> cells = List.of(command, modifier, key);
        for (int column = 0; column < cells.size(); column++) {
            GridBagConstraints layout = new GridBagConstraints();
            layout.gridx = column;
            layout.gridy = row;
            layout.fill = GridBagConstraints.HORIZONTAL;
            layout.insets = new Insets(0, 10, 5, 10);
            grid.add(cells.get(column), layout);
        }
    }

        static JPanel createKeyBindSectionContent(JComponent navigationControl, JComponent resetControl,
          JComponent bindingsGrid) {
        int gap = UIUtil.scaleForGUI(12);
        SettingsFormPanel actions = new SettingsFormPanel("CommonSettingsKeyBindingsActions", 0, 0);
        actions.addEqualWidthComponentGrid(2, navigationControl, resetControl);

        JPanel content = new JPanel(new BorderLayout(0, gap));
        content.setName("pnlCommonSettingsKeyBindings");
        content.setOpaque(false);
        content.add(actions, BorderLayout.NORTH);
        content.add(bindingsGrid, BorderLayout.CENTER);
        return content;
    }

    private JComboBox<String> createHideShowComboBox(int i) {
        JComboBox<String> cb = new JComboBox<>();
        cb.addItem(Messages.getString("ClientGUI.Hide"));
        cb.addItem(Messages.getString("ClientGUI.Show"));
        cb.addItem(Messages.getString("ClientGUI.Manual"));
        cb.setMaximumSize(new Dimension(150, 40));
        cb.setSelectedIndex(i);

        return cb;
    }

    private JCheckBox createOnOffCheckBox(boolean b) {
        JCheckBox checkBox = new JCheckBox();
        checkBox.setEnabled(true);
        checkBox.setSelected(b);

        return checkBox;
    }

    private JPanel aiDisplayPanel() {

        List<List<Component>> comps = new ArrayList<>();
        List<Component> row = new ArrayList<>();

        // Label ACAR
        row.add(new JLabel(Messages.getString("CommonSettingsDialog.acarSettingsLabel")));
        comps.add(row);
        comps.add(checkboxEntry(showAutoResolvePanel, null));

        addLineSpacer(comps);
        // Label BOT & PACAR
        favoritePrincessBehaviorSetting = new MMComboBox<>("favoritePrincessBehaviorSetting",
              BehaviorSettingsFactory.getInstance().getBehaviorNameList());
        favoritePrincessBehaviorSetting.setMaximumSize(new Dimension(200, 25));
        favoritePrincessBehaviorSetting.setToolTipText(Messages.getString(
              "CommonSettingsDialog.favoritePrincessBehaviorSettingTooltip"));
        favoritePrincessBehaviorSetting.setSelectedItem(CLIENT_PREFERENCES.getFavoritePrincessBehaviorSetting());

        row = new ArrayList<>();
        row.add(new JLabel(Messages.getString("CommonSettingsDialog.pacarSettingsLabel")));
        comps.add(row);
        row = new ArrayList<>();
        row.add(new JLabel(Messages.getString("CommonSettingsDialog.favoritePrincessBehaviorSetting")));
        row.add(favoritePrincessBehaviorSetting);
        comps.add(row);

        comps.add(checkboxEntry(enableExperimentalBotFeatures,
              Messages.getString("CommonSettingsDialog.enableExperimentalBotFeatures.tooltip")));

        return createSettingsPanel(comps);
    }

    private JPanel getPhasePanel() {
        List<List<Component>> comps = new ArrayList<>();
        ArrayList<Component> row;

        row = new ArrayList<>();
        JLabel unitDisplayLabel = new JLabel(Messages.getString("CommonMenuBar.viewMekDisplay"));
        row.add(unitDisplayLabel);
        comps.add(row);
        row = new ArrayList<>();
        JLabel phaseLabel = new JLabel(Messages.getString("CommonSettingsDialog.reportPhases") + ": ");
        row.add(phaseLabel);
        unitDisplayAutoDisplayReportCombo = createHideShowComboBox(GUIP.getUnitDisplayAutoDisplayReportPhase());
        row.add(unitDisplayAutoDisplayReportCombo);
        comps.add(row);
        row = new ArrayList<>();
        phaseLabel = new JLabel(Messages.getString("CommonSettingsDialog.nonReportPhases") + ": ");
        row.add(phaseLabel);
        unitDisplayAutoDisplayNonReportCombo = createHideShowComboBox(GUIP.getUnitDisplayAutoDisplayNonReportPhase());
        row.add(unitDisplayAutoDisplayNonReportCombo);
        comps.add(row);

        addLineSpacer(comps);

        row = new ArrayList<>();
        JLabel miniMapLabel = new JLabel(Messages.getString("CommonMenuBar.viewMinimap"));
        row.add(miniMapLabel);
        comps.add(row);
        row = new ArrayList<>();
        phaseLabel = new JLabel(Messages.getString("CommonSettingsDialog.reportPhases") + ": ");
        row.add(phaseLabel);
        miniMapAutoDisplayReportCombo = createHideShowComboBox(GUIP.getMinimapAutoDisplayReportPhase());
        row.add(miniMapAutoDisplayReportCombo);
        comps.add(row);
        row = new ArrayList<>();
        phaseLabel = new JLabel(Messages.getString("CommonSettingsDialog.nonReportPhases") + ": ");
        row.add(phaseLabel);
        miniMapAutoDisplayNonReportCombo = createHideShowComboBox(GUIP.getMinimapAutoDisplayNonReportPhase());
        row.add(miniMapAutoDisplayNonReportCombo);
        comps.add(row);

        addLineSpacer(comps);

        row = new ArrayList<>();
        JLabel miniReportLabel = new JLabel(Messages.getString("CommonMenuBar.viewRoundReport"));
        row.add(miniReportLabel);
        comps.add(row);
        row = new ArrayList<>();
        phaseLabel = new JLabel(Messages.getString("CommonSettingsDialog.reportPhases") + ": ");
        row.add(phaseLabel);
        miniReportAutoDisplayReportCombo = createHideShowComboBox(GUIP.getMiniReportAutoDisplayReportPhase());
        row.add(miniReportAutoDisplayReportCombo);
        comps.add(row);
        row = new ArrayList<>();
        phaseLabel = new JLabel(Messages.getString("CommonSettingsDialog.nonReportPhases") + ": ");
        row.add(phaseLabel);
        miniReportAutoDisplayNonReportCombo = createHideShowComboBox(GUIP.getMiniReportAutoDisplayNonReportPhase());
        row.add(miniReportAutoDisplayNonReportCombo);
        comps.add(row);

        addLineSpacer(comps);

        row = new ArrayList<>();
        JLabel playerListLabel = new JLabel(Messages.getString("CommonMenuBar.viewPlayerList"));
        row.add(playerListLabel);
        comps.add(row);
        row = new ArrayList<>();
        phaseLabel = new JLabel(Messages.getString("CommonSettingsDialog.reportPhases") + ": ");
        row.add(phaseLabel);
        playerListAutoDisplayReportCombo = createHideShowComboBox(GUIP.getPlayerListAutoDisplayReportPhase());
        row.add(playerListAutoDisplayReportCombo);
        comps.add(row);
        row = new ArrayList<>();
        phaseLabel = new JLabel(Messages.getString("CommonSettingsDialog.nonReportPhases") + ": ");
        row.add(phaseLabel);
        playerListAutoDisplayNonReportCombo = createHideShowComboBox(GUIP.getPlayerListAutoDisplayNonReportPhase());
        row.add(playerListAutoDisplayNonReportCombo);
        comps.add(row);

        addLineSpacer(comps);

        row = new ArrayList<>();
        JLabel forceDisplayLabel = new JLabel(Messages.getString("CommonMenuBar.viewForceDisplay"));
        row.add(forceDisplayLabel);
        comps.add(row);
        row = new ArrayList<>();
        phaseLabel = new JLabel(Messages.getString("CommonSettingsDialog.reportPhases") + ": ");
        row.add(phaseLabel);
        forceDisplayAutoDisplayReportCombo = createHideShowComboBox(GUIP.getForceDisplayAutoDisplayReportPhase());
        row.add(forceDisplayAutoDisplayReportCombo);
        comps.add(row);
        row = new ArrayList<>();
        phaseLabel = new JLabel(Messages.getString("CommonSettingsDialog.nonReportPhases") + ": ");
        row.add(phaseLabel);
        forceDisplayAutoDisplayNonReportCombo = createHideShowComboBox(GUIP.getForceDisplayAutoDisplayNonReportPhase());
        row.add(forceDisplayAutoDisplayNonReportCombo);
        comps.add(row);

        addLineSpacer(comps);

        row = new ArrayList<>();
        JLabel botCommandsLabel = new JLabel(Messages.getString("CommonMenuBar.viewBotCommands"));
        row.add(botCommandsLabel);
        comps.add(row);
        row = new ArrayList<>();
        phaseLabel = new JLabel(Messages.getString("CommonSettingsDialog.reportPhases") + ": ");
        row.add(phaseLabel);
        botCommandsAutoDisplayReportCombo = createHideShowComboBox(GUIP.getBotCommandsAutoDisplayReportPhase());
        row.add(botCommandsAutoDisplayReportCombo);
        comps.add(row);
        row = new ArrayList<>();
        phaseLabel = new JLabel(Messages.getString("CommonSettingsDialog.nonReportPhases") + ": ");
        row.add(phaseLabel);
        botCommandsAutoDisplayNonReportCombo = createHideShowComboBox(GUIP.getBotCommandsAutoDisplayNonReportPhase());
        row.add(botCommandsAutoDisplayNonReportCombo);
        comps.add(row);

        addLineSpacer(comps);


        // Firing/Movement Display changes
        row = new ArrayList<>();
        JLabel tabsDisplayLabel = new JLabel(Messages.getString("CommonMenuBar.viewFiringMovingTabs"));
        row.add(tabsDisplayLabel);
        comps.add(row);
        row = new ArrayList<>();
        phaseLabel = new JLabel(Messages.getString("CommonSettingsDialog.tabsMove") + ": ");
        row.add(phaseLabel);
        displayMoveDisplayDuringMovePhases = createOnOffCheckBox(GUIP.getMoveDisplayTabDuringMovePhases());
        row.add(displayMoveDisplayDuringMovePhases);
        comps.add(row);
        row = new ArrayList<>();
        phaseLabel = new JLabel(Messages.getString("CommonSettingsDialog.tabsFire") + ": ");
        row.add(phaseLabel);
        displayFireDisplayDuringFirePhases = createOnOffCheckBox(GUIP.getFireDisplayTabDuringFiringPhases());
        row.add(displayFireDisplayDuringFirePhases);
        comps.add(row);

        return createSettingsPanel(comps);
    }

    private void updateKeybindsFocusTraversal() {
        for (KeyCommandBind kcb : KeyCommandBind.values()) {
            int keyCode = configureKeyBindFieldsForTabNavigation(keyBindTabNavigation.isSelected(), keyCode(kcb),
                  cmdModifierMap.get(kcb.cmd), cmdKeyMap.get(kcb.cmd));
            cmdKeyCodeMap.put(kcb.cmd, keyCode);
        }
        markDuplicateBinds();
    }

    static int configureKeyBindFieldsForTabNavigation(boolean enabled, int keyCode,
          JTextField modifierField, JTextField keyField) {
        modifierField.setFocusTraversalKeysEnabled(enabled);
        keyField.setFocusTraversalKeysEnabled(enabled);
        if (enabled && (keyCode == KeyEvent.VK_TAB)) {
            modifierField.setText("");
            keyField.setText("");
            return 0;
        }
        return keyCode;
    }

    private void updateKeybindsDefault() {
        for (KeyCommandBind kcb : KeyCommandBind.values()) {
            cmdKeyMap.get(kcb.cmd).setText(KeyEvent.getKeyText(kcb.keyDefault));
            cmdModifierMap.get(kcb.cmd).setText(KeyEvent.getModifiersExText(kcb.modifiersDefault));
            cmdKeyCodeMap.put(kcb.cmd, kcb.keyDefault);
        }

        markDuplicateBinds();
    }

    /**
     * Marks the text fields when duplicate keybinds occur. Two commands may share a keybind if none of them is a
     * Menubar or exclusive keybind (although that only works well if they're used in different phases such as turn and
     * twist). Also checks for Ctrl-C and Ctrl-V. These are coded into JTables and JTrees and making them configurable
     * would be unproportional effort to the gain.
     */
    private void markDuplicateBinds() {
        Map<KeyStroke, KeyCommandBind> duplicates = new HashMap<>();
        Set<KeyStroke> allKeys = new HashSet<>();
        // Assemble all keybinds that are used twice into the duplicates map
        for (KeyCommandBind kcb : KeyCommandBind.values()) {
            KeyStroke keyStroke = KeyStroke.getKeyStroke(keyCode(kcb), modifierCode(kcb));
            if (!allKeys.add(keyStroke) && keyStroke.getKeyCode() != 0) { // Disregard unbound keys (keycode 0)
                duplicates.put(keyStroke, kcb);
            }
        }

        // Now traverse the commands again. When a duplicate keybind is found and this
        // KeyCommandBind is exclusive or Menubar
        // or the other one (the first one found with the same keybind) is exclusive or
        // Menubar, both are marked.
        // Also, Ctrl-C and Ctrl-V are marked as these are hard-mapped to Copy/Paste and
        // cannot be used otherwise.
        for (KeyCommandBind kcb : KeyCommandBind.values()) {
            boolean isCorrect = true;
            KeyStroke keyStroke = KeyStroke.getKeyStroke(keyCode(kcb), modifierCode(kcb));
            if (duplicates.containsKey(keyStroke) &&
                  (kcb.isMenuBar
                        || kcb.isExclusive
                        || duplicates.get(keyStroke).isExclusive
                        || duplicates.get(keyStroke).isMenuBar)) {
                // Mark the current kcb and the one that was already in the keyMap as duplicate
                markTextField(cmdModifierMap.get(kcb.cmd), "This keybind is a duplicate and will not work correctly.");
                markTextField(cmdKeyMap.get(kcb.cmd), "This keybind is a duplicate and will not work correctly.");
                markTextField(cmdModifierMap.get(duplicates.get(keyStroke).cmd),
                      "This keybind is a duplicate and will not work correctly.");
                markTextField(cmdKeyMap.get(duplicates.get(keyStroke).cmd),
                      "This keybind is a duplicate and will not work correctly.");
                isCorrect = false;
            }
            // Check for standard copy/paste keys
            if (keyStroke.equals(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK)) ||
                  keyStroke.equals(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.CTRL_DOWN_MASK))) {
                markTextField(cmdModifierMap.get(kcb.cmd), "Ctrl-C / Ctrl-V cannot be used");
                markTextField(cmdKeyMap.get(kcb.cmd), "Ctrl-C / Ctrl-V cannot be used");
                isCorrect = false;
            }
            if (isCorrect) {
                markTextField(cmdModifierMap.get(kcb.cmd), null);
                markTextField(cmdKeyMap.get(kcb.cmd), null);
            }
        }
    }

    private void markTextField(JTextField field, String errorMsg) {
        field.setForeground(errorMsg != null ? GUIP.getWarningColor() : null);
        field.setToolTipText(errorMsg);
    }

    /**
     * Returns the keycode for the character part of a user-entered keybind (The "V" in CTRL-V).
     */
    private int keyCode(KeyCommandBind kcb) {
        return cmdKeyCodeMap.get(kcb.cmd);
    }

    /**
     * Returns the keycode for the modifier part of a user-entered keybind (The "CTRL" in CTRL-V).
     */
    private int modifierCode(KeyCommandBind kcb) {
        int modifiers = 0;
        String modText = cmdModifierMap.get(kcb.cmd).getText();
        if (modText.contains(KeyEvent.getModifiersExText(KeyEvent.SHIFT_DOWN_MASK))) {
            modifiers |= KeyEvent.SHIFT_DOWN_MASK;
        }
        if (modText.contains(KeyEvent.getModifiersExText(KeyEvent.ALT_DOWN_MASK))) {
            modifiers |= KeyEvent.ALT_DOWN_MASK;
        }
        if (modText.contains(KeyEvent.getModifiersExText(KeyEvent.CTRL_DOWN_MASK))) {
            modifiers |= KeyEvent.CTRL_DOWN_MASK;
        }
        return modifiers;
    }

    /**
     * Creates a panel with a list boxes that allow the button order to be changed.
     */
    private List<CommonSettingsPane.OptionSection> getButtonOrderSections() {
        movePhaseCommands = new DefaultListModel<>();
        deployPhaseCommands = new DefaultListModel<>();
        firingPhaseCommands = new DefaultListModel<>();
        physicalPhaseCommands = new DefaultListModel<>();
        targetingPhaseCommands = new DefaultListModel<>();
        return List.of(
              optionSection("buttonOrder.movement", getButtonOrderPane(movePhaseCommands, MoveCommand.values()), false),
              optionSection("buttonOrder.deployment",
                    getButtonOrderPane(deployPhaseCommands, DeploymentDisplay.DeployCommand.values()), false),
              optionSection("buttonOrder.firing",
                    getButtonOrderPane(firingPhaseCommands, FiringDisplay.FiringCommand.values()), false),
              optionSection("buttonOrder.physical",
                    getButtonOrderPane(physicalPhaseCommands, PhysicalDisplay.PhysicalCommand.values()), false),
              optionSection("buttonOrder.targeting",
                    getButtonOrderPane(targetingPhaseCommands, TargetingPhaseDisplay.TargetingCommand.values()), false));
    }

    /** Constructs the button ordering panel for one phase. */
    private JScrollPane getButtonOrderPane(DefaultListModel<PhaseCommand> list,
          StatusBarPhaseDisplay.PhaseCommand[] commands) {
        JPanel panel = new JPanel();
        Arrays.sort(commands, cmdComp);

        for (StatusBarPhaseDisplay.PhaseCommand cmd : commands) {
            list.addElement(cmd);
        }

        int rowCount = list.getSize() / 7;

        if (list.getSize() % 7 != 0) {
            rowCount++;
        }

        JList<StatusBarPhaseDisplay.PhaseCommand> jlist = new JList<>(list);
        jlist.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jlist.addMouseListener(cmdMouseAdaptor);
        jlist.addMouseMotionListener(cmdMouseAdaptor);
        jlist.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        jlist.setVisibleRowCount(rowCount);
        panel.add(jlist);
        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        return scrollPane;
    }

    static JPanel createSettingsPanel(List<List<Component>> comps) {
        List<ColourSelectorButton> colourButtons = new ArrayList<>();
        for (List<Component> row : comps) {
            row.forEach(component -> collectColourButtons(component, colourButtons));
        }
        setUniformWidth(colourButtons);

        List<JComponent> groups = new ArrayList<>();
        List<List<Component>> rows = new ArrayList<>();
        for (List<Component> row : comps) {
            if (row.size() == 1 && row.getFirst() instanceof SettingsSectionBreak) {
                addSettingsGroup(groups, rows);
                rows = new ArrayList<>();
            } else {
                rows.add(row);
            }
        }
        addSettingsGroup(groups, rows);
        return new CommonSettingsPane.SectionedContent(groups);
    }

    private static void addSettingsGroup(List<JComponent> groups, List<List<Component>> rows) {
        if (!rows.isEmpty()) {
            groups.add(createSettingsGroup(rows));
        }
    }

    private static JPanel createSettingsGroup(List<List<Component>> rows) {
        SettingsFormPanel panel = new SettingsFormPanel("CommonSettingsGroup",
              SettingsFormPanel.DEFAULT_LABEL_WIDTH, SettingsFormPanel.DEFAULT_CONTROL_WIDTH);
        List<JCheckBox> checkBoxes = new ArrayList<>();
        List<JComponent> colourButtons = new ArrayList<>();
        for (List<Component> row : rows) {
            List<JComponent> components = swingComponents(row);
            if (isSingleCheckBox(components)) {
                flushComponentGrid(panel, colourButtons);
                checkBoxes.add((JCheckBox) components.getFirst());
            } else if (isColourButtonRow(components)) {
                flushCheckBoxGrid(panel, checkBoxes);
                colourButtons.addAll(components);
            } else {
                flushCheckBoxGrid(panel, checkBoxes);
                flushComponentGrid(panel, colourButtons);
                addSettingsRow(panel, components);
            }
        }
        flushCheckBoxGrid(panel, checkBoxes);
        flushComponentGrid(panel, colourButtons);
        return panel;
    }

    private static List<JComponent> swingComponents(List<Component> row) {
        return row.stream()
              .filter(JComponent.class::isInstance)
              .map(JComponent.class::cast)
              .toList();
    }

    private static boolean isSingleCheckBox(List<JComponent> components) {
        return components.size() == 1 && components.getFirst() instanceof JCheckBox;
    }

    private static boolean isColourButtonRow(List<JComponent> components) {
        return !components.isEmpty() && components.stream().allMatch(ColourSelectorButton.class::isInstance);
    }

    private static void flushCheckBoxGrid(SettingsFormPanel panel, List<JCheckBox> checkBoxes) {
        if (!checkBoxes.isEmpty()) {
            panel.addEqualWidthComponentGrid(2, checkBoxes.toArray(new JComponent[0]));
            checkBoxes.clear();
        }
    }

    private static void flushComponentGrid(SettingsFormPanel panel, List<JComponent> components) {
        if (!components.isEmpty()) {
            SettingsFormPanel colourGrid = new SettingsFormPanel("CommonSettingsColourGrid");
            colourGrid.addEqualWidthComponentGrid(2, components.toArray(new JComponent[0]));
            panel.addFullWidthComponent(colourGrid);
            components.clear();
        }
    }

    private static void addSettingsRow(SettingsFormPanel panel, List<JComponent> components) {
        if (components.isEmpty()) {
            return;
        }
        if (components.size() == 1 && components.getFirst() instanceof JCheckBox checkBox) {
            panel.addCheckBox(checkBox);
            return;
        }
        if (components.size() == 1) {
            panel.addFullWidthComponent(components.getFirst());
            return;
        }
        if (components.getFirst() instanceof JLabel label && components.size() > 1) {
            JComponent control = components.size() == 2
                  ? components.get(1)
                  : rowPanel(components.subList(1, components.size()));
            label.setLabelFor(control);
            panel.addEqualWidthComponentGrid(2, label, control);
            return;
        }
        panel.addFullWidthComponent(rowPanel(components));
    }

    static JPanel createUnitDefaultsGrid(JLabel protoMekLabel, JComponent protoMekControl,
          JCheckBox... checkBoxes) {
          SettingsFormPanel grid = new SettingsFormPanel("CommonSettingsUnitDefaultsGrid",
              SettingsFormPanel.DEFAULT_LABEL_WIDTH, 0);
          protoMekLabel.setLabelFor(protoMekControl);
          grid.addEqualWidthComponentGrid(BEHAVIOR_OPTION_COLUMNS, protoMekLabel, protoMekControl);
          grid.addEqualWidthComponentGrid(BEHAVIOR_OPTION_COLUMNS, checkBoxes);
          return grid;
    }

    static SettingsFormPanel createBehaviorOptionsGrid(String name, JComponent... options) {
        SettingsFormPanel grid = new SettingsFormPanel(name, SettingsFormPanel.DEFAULT_LABEL_WIDTH, 0);
        grid.addEqualWidthComponentGrid(BEHAVIOR_OPTION_COLUMNS, options);
        return grid;
    }

    static SettingsFormPanel createBehaviorLoggingGrid(String name,
          JCheckBox datasetLogging, JCheckBox keepGameLog,
          JLabel gameLogLabel, JComponent gameLogControl,
          JLabel autoResolveLogLabel, JComponent autoResolveLogControl,
          JCheckBox stampFilenames,
          JLabel dateFormatLabel, JComponent dateFormatControl) {
    SettingsFormPanel grid = new SettingsFormPanel(name, SettingsFormPanel.DEFAULT_LABEL_WIDTH, 0);
        grid.addEqualWidthComponentGrid(BEHAVIOR_OPTION_COLUMNS, datasetLogging, keepGameLog);
        gameLogLabel.setLabelFor(gameLogControl);
        grid.addEqualWidthComponentGrid(BEHAVIOR_OPTION_COLUMNS, gameLogLabel, gameLogControl);
        autoResolveLogLabel.setLabelFor(autoResolveLogControl);
        grid.addEqualWidthComponentGrid(BEHAVIOR_OPTION_COLUMNS, autoResolveLogLabel, autoResolveLogControl);
        grid.addEqualWidthComponentGrid(BEHAVIOR_OPTION_COLUMNS, stampFilenames);
    dateFormatLabel.setLabelFor(dateFormatControl);
    grid.addEqualWidthComponentGrid(BEHAVIOR_OPTION_COLUMNS, dateFormatLabel, dateFormatControl);
    return grid;
}

    static SettingsCheckBox createShowIpAddressesInChatCheckBox() {
        return new SettingsCheckBox(SETTINGS_TEXT, "CommonSettingsDialog.showIPAddressesInChat",
              List.of(IMPORTANT_BADGE));
    }

    private static JButton applicationIconButton(String name, int codePoint, String accessibleName) {
        JButton button = new JButton();
        button.setName(name);
        button.setToolTipText(accessibleName);
        button.getAccessibleContext().setAccessibleName(accessibleName);
        button.setIcon(FontHandler.symbolIcon(codePoint,
              button.getFont().getSize() + UIUtil.scaleForGUI(2), button.getForeground()));
        return button;
    }

    private JSlider createGuiScaleControl() {
        guiScale = createGuiScaleSlider();
        return guiScale;
    }

    static JSlider createGuiScaleSlider() {
        JSlider slider = new JSlider(7, 24);
        slider.setMinorTickSpacing(1);
        Hashtable<Integer, JComponent> labels = new Hashtable<>();
        labels.put(7, new JLabel("70%"));
        labels.put(10, new JLabel("100%"));
        labels.put(14, new JLabel("140%"));
        labels.put(17, new JLabel("170%"));
        labels.put(21, new JLabel("210%"));
        labels.put(24, new JLabel("240%"));
          slider.setLabelTable(labels);
          slider.setPaintTicks(true);
          slider.setPaintLabels(true);
          slider.setToolTipText(Messages.getString("CommonSettingsDialog.guiScaleTT"));
        return slider;
    }

    private JButton createUserFilesHelpButton() {
        String title = Messages.getString("UserDirHelpDialog.title");
        JButton button = applicationIconButton("btnUserDirHelp", 0xE887, title);
        configureUserFilesHelpButton(button, Configuration.docsDir(),
              documentUrl -> new HelpDialog(title, documentUrl, this).setVisible(true));
        return button;
    }

    static void configureUserFilesHelpButton(JButton button, File docsDirectory, Consumer<URL> helpAction) {
        Optional<File> document = resolveUserFilesHelpDocument(docsDirectory);
        if (document.isEmpty()) {
            disableUserFilesHelpButton(button);
            logger.error("Could not find the user data directory help file under {}", docsDirectory);
            return;
        }

        File resolvedDocument = document.get();
        try {
            URL documentUrl = resolvedDocument.toURI().toURL();
            button.addActionListener(e -> helpAction.accept(documentUrl));
        } catch (MalformedURLException exception) {
            disableUserFilesHelpButton(button);
            logger.error(exception, "Could not open the user data directory help file at {}", resolvedDocument);
        }
    }

    private static Optional<File> resolveUserFilesHelpDocument(File docsDirectory) {
        File megaMekDocument = new File(docsDirectory, "Customization/UserDir/UserDirHelp.html");
        if (megaMekDocument.isFile()) {
            return Optional.of(megaMekDocument);
        }

        File mekHqDocument = new File(docsDirectory, "Customization/MekHQ/UserDirHelp.html");
        return mekHqDocument.isFile() ? Optional.of(mekHqDocument) : Optional.empty();
    }

    private static void disableUserFilesHelpButton(JButton button) {
        String unavailableMessage = Messages.getString("CommonSettingsDialog.userDir.helpUnavailable");
        button.setEnabled(false);
        button.setToolTipText(unavailableMessage);
        button.getAccessibleContext().setAccessibleDescription(unavailableMessage);
    }

    static JPanel applicationPathControl(JTextField field, JButton... buttons) {
        int gap = UIUtil.scaleForGUI(6);
        Dimension buttonSize = new Dimension(field.getPreferredSize().height, field.getPreferredSize().height);
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
        buttonPanel.setOpaque(false);
        for (int index = 0; index < buttons.length; index++) {
            JButton button = buttons[index];
            button.setPreferredSize(buttonSize);
            button.setMinimumSize(buttonSize);
            button.setMaximumSize(buttonSize);
            if (index > 0) {
                buttonPanel.add(Box.createHorizontalStrut(gap));
            }
            buttonPanel.add(button);
        }

        JPanel control = new JPanel(new BorderLayout(gap, 0));
        control.setOpaque(false);
        control.add(field, BorderLayout.CENTER);
        control.add(buttonPanel, BorderLayout.LINE_END);
        return control;
    }

    private static void collectColourButtons(Component component, List<ColourSelectorButton> colourButtons) {
        if (component instanceof ColourSelectorButton button) {
            button.setHorizontalAlignment(SwingConstants.LEFT);
            colourButtons.add(button);
        }
        if (component instanceof CommonSettingsPane.SectionedContent) {
            return;
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                collectColourButtons(child, colourButtons);
            }
        }
    }

    private static void setUniformWidth(List<? extends JComponent> components) {
        int width = components.stream()
              .map(component -> Objects.requireNonNull(component).getPreferredSize())
              .mapToInt(dimension -> dimension.width)
              .max()
              .orElse(0);
        for (JComponent component : components) {
            JComponent nonNullComponent = Objects.requireNonNull(component);
            Dimension preferred = nonNullComponent.getPreferredSize();
            nonNullComponent.setPreferredSize(new Dimension(width, preferred.height));
        }
    }

    private static JPanel rowPanel(List<JComponent> components) {
        JPanel row = new JPanel();
        row.setOpaque(false);
        row.setLayout(new BoxLayout(row, BoxLayout.LINE_AXIS));
        components.forEach(row::add);
        row.add(Box.createHorizontalGlue());
        return row;
    }

    private static final class SettingsSectionBreak extends JComponent {
    }

    // Advanced-preference keys deliberately hidden from the Advanced settings list: testing/debug gates that must only
    // be enabled by manually editing clientsettings.xml, never via the UI. They keep their "Advanced" prefix so the
    // value still loads from clientsettings.xml; they are simply filtered out of the displayed list here.
    // (AdvancedRevealAllArtilleryRounds is the legacy key for the now-renamed RevealAllArtilleryRounds gate.)
    private static final Set<String> HIDDEN_ADVANCED_OPTIONS = Set.of(
          "AdvancedRevealAllArtilleryRounds",
          "AdvancedShowBotArtilleryHeatMap",
          "AdvancedRevealObscuredArtillery");

    private JPanel getAdvancedSettingsPanel() {
        JPanel p = new JPanel();

        String[] advancedProperties = GUIP.getAdvancedProperties();
        List<AdvancedOptionData> visibleOptions = new ArrayList<>();
        for (String advancedProperty : advancedProperties) {
            // Skip deliberately-hidden testing gates so they never appear in the Advanced list - they are controlled
            // only by manually editing clientsettings.xml.
            if (HIDDEN_ADVANCED_OPTIONS.contains(advancedProperty)) {
                continue;
            }
            visibleOptions.add(new AdvancedOptionData(
                  advancedProperty.substring(advancedProperty.indexOf("Advanced") + 8)));
        }
        AdvancedOptionData[] opts = visibleOptions.toArray(new AdvancedOptionData[0]);
        Arrays.sort(opts);
        advancedKeys = new JList<>(opts);
        advancedKeys.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        advancedKeys.addListSelectionListener(this);

        advancedKeys.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int index = advancedKeys.locationToIndex(e.getPoint());
                if (index > -1) {
                    AdvancedOptionData dat = advancedKeys.getModel().getElementAt(index);
                    advancedKeys.setToolTipText(dat.hasTooltipText() ? dat.getTooltipText() : null);
                }
            }
        });

        p.add(advancedKeys);

        advancedValue = new JTextField(10);
        advancedValue.setFont(new Font(MMConstants.FONT_SANS_SERIF, Font.PLAIN, 16));
        advancedValue.addFocusListener(this);
        p.add(advancedValue);

        return p;
    }

    /** Used to note which advanced setting is currently clicked. */
    @Override
    public void valueChanged(ListSelectionEvent event) {
        if (event.getValueIsAdjusting()) {
            return;
        }
        if (event.getSource().equals(advancedKeys) && !advancedKeys.isSelectionEmpty()) {
            advancedValue.setText(GUIP.getString("Advanced" + advancedKeys.getSelectedValue().option));
            advancedKeyIndex = advancedKeys.getSelectedIndex();
        }
    }

    @Override
    public void stateChanged(ChangeEvent evt) {
        if (evt.getSource().equals(fovHighlightAlpha)) {
            GUIP.setFovHighlightAlpha(Math.clamp(fovHighlightAlpha.getValue(), 0, 255));
        } else if (evt.getSource().equals(fovDarkenAlpha)) {
            GUIP.setFovDarkenAlpha(Math.clamp(fovDarkenAlpha.getValue(), 0, 255));
        } else if (evt.getSource().equals(fovStripesSpinner)) {
            GUIP.setFovStripes((int) fovStripesSpinner.getValue());
        } else if (evt.getSource().equals(traceOverlayTransparencySlider)) {
            GUIP.setTraceOverlayTransparency(traceOverlayTransparencySlider.getValue());
        } else if (evt.getSource().equals(traceOverlayScaleSlider)) {
            GUIP.setTraceOverlayScale(traceOverlayScaleSlider.getValue());
        } else if (evt.getSource().equals(traceOverlayOriginXSlider)) {
            GUIP.setTraceOverlayOriginX(traceOverlayOriginXSlider.getValue());
        } else if (evt.getSource().equals(traceOverlayOriginYSlider)) {
            GUIP.setTraceOverlayOriginY(traceOverlayOriginYSlider.getValue());
        }
    }

    /**
     * Returns the files in the directory given as relativePath (e.g. Configuration.hexesDir()) under the userData
     * directory ending with fileEnding (such as ".xml")
     */
    public static List<String> userDataFiles(File relativePath, String fileEnding) {
        List<String> result = new ArrayList<>();
        File dir = new File(Configuration.userDataDir(), relativePath.toString());
        String[] userDataFiles = dir.list((directory, name) -> name.endsWith(fileEnding));
        if (userDataFiles != null) {
            result.addAll(Arrays.asList(userDataFiles));
        }
        return result;
    }

    public static List<String> filteredFiles(File path, String fileEnding) {
        List<String> result = new ArrayList<>();
        String[] userDataFiles = path.list((directory, name) -> name.endsWith(fileEnding));
        if (userDataFiles != null) {
            Arrays.stream(userDataFiles).map(file -> path + "/" + file).forEach(result::add);
        }
        return result;
    }

    public static List<String> filteredFilesWithSubDirs(File path, String fileEnding) {
        if (!path.exists()) {
            logger.warn("Path {} does not exist.", path);
            return new ArrayList<>();
        }
            List<String> result = new ArrayList<>();
            try {
                  Files.walkFileTree(path.toPath(), filteredFileVisitor(fileEnding, result));
        } catch (IOException e) {
                  logger.warn(e, "Error while reading {} files from {}", fileEnding, path);
        }
            return result;
      }

      static SimpleFileVisitor<Path> filteredFileVisitor(String fileEnding, List<String> result) {
            return new SimpleFileVisitor<>() {
                  @Override
                  public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
                        if (file.toString().endsWith(fileEnding)) {
                              result.add(file.toString());
                        }
                        return FileVisitResult.CONTINUE;
                  }

                  @Override
                  public FileVisitResult visitFileFailed(Path file, IOException exception) {
                        logger.warn(exception, "Unable to access {} while searching for {} files; skipping it.", file,
                                fileEnding);
                        return FileVisitResult.CONTINUE;
                  }

                  @Override
                  public FileVisitResult postVisitDirectory(Path directory, IOException exception) {
                        if (exception != null) {
                              logger.warn(exception,
                                      "Unable to finish reading {} while searching for {} files; skipping it.", directory,
                                      fileEnding);
                        }
                        return FileVisitResult.CONTINUE;
                  }
            };
      }

    /**
     * Shows a file chooser for selecting a user directory and sets the given text field to the result if one was
     * chosen. This is for use with settings dialogs (also used in MML and MHQ)
     *
     * @param userDirTextField The {@link JTextField} showing the user dir for manual change
     * @param parent           The parent JFrame of the settings dialog
     */
    public static void fileChooseUserDir(JTextField userDirTextField, JFrame parent) {
        fileChoose(userDirTextField, parent, Messages.getString("CommonSettingsDialog.userDir.chooser.title"), true);
    }

    private static void fileChoose(JTextField textField, Component parent, String title, boolean directories) {
        JFileChooser userDirChooser = new JFileChooser(textField.getText());
        userDirChooser.setDialogTitle(title);
        if (directories) {
            userDirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        }
        int returnVal = userDirChooser.showOpenDialog(parent);
        if ((returnVal == JFileChooser.APPROVE_OPTION) &&
              (userDirChooser.getSelectedFile() != null) &&
              (directories ?
                    userDirChooser.getSelectedFile().isDirectory() :
                    userDirChooser.getSelectedFile().isFile())) {
            textField.setText(userDirChooser.getSelectedFile().toString());
        }
    }
}
