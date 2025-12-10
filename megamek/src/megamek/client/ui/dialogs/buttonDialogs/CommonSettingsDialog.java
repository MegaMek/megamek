/*
 * Copyright (c) 2003-2005 - Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2003-2025 The MegaMek Team. All Rights Reserved.
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

import static java.util.stream.Collectors.toList;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;
import javax.swing.*;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.MouseInputAdapter;

import com.formdev.flatlaf.icons.FlatHelpButtonIcon;
import megamek.MMConstants;
import megamek.client.bot.princess.BehaviorSettingsFactory;
import megamek.client.ui.Messages;
import megamek.client.ui.buttons.ColourSelectorButton;
import megamek.client.ui.buttons.MMButton;
import megamek.client.ui.buttons.MMToggleButton;
import megamek.client.ui.clientGUI.ButtonOrderPreferences;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.clientGUI.UITheme;
import megamek.client.ui.clientGUI.UnitDisplayOrderPreferences;
import megamek.client.ui.comboBoxes.MMComboBox;
import megamek.client.ui.dialogs.helpDialogs.HelpDialog;
import megamek.client.ui.dialogs.minimap.MinimapPanel;
import megamek.client.ui.dialogs.unitDisplay.UnitDisplayPanel;
import megamek.client.ui.models.FileNameComboBoxModel;
import megamek.client.ui.panels.phaseDisplay.DeploymentDisplay;
import megamek.client.ui.panels.phaseDisplay.FiringDisplay;
import megamek.client.ui.panels.phaseDisplay.PhysicalDisplay;
import megamek.client.ui.panels.phaseDisplay.StatusBarPhaseDisplay;
import megamek.client.ui.panels.phaseDisplay.StatusBarPhaseDisplay.PhaseCommand;
import megamek.client.ui.panels.phaseDisplay.TargetingPhaseDisplay;
import megamek.client.ui.panels.phaseDisplay.commands.MoveCommand;
import megamek.client.ui.util.FontHandler;
import megamek.client.ui.util.KeyCommandBind;
import megamek.client.ui.util.PlayerColour;
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
 */
public class CommonSettingsDialog extends AbstractButtonDialog
      implements ItemListener, FocusListener, ListSelectionListener, ChangeListener {
    private final static MMLogger logger = MMLogger.create(CommonSettingsDialog.class);

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
    private JTextField tooltipDelay;
    private JTextField tooltipDismissDelay;
    private JTextField tooltipDistSuppression;
    private JComboBox<String> unitStartChar;
    private JTextField maxPathfinderTime;
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
    private final JCheckBox useGPinUnitSelection = new JCheckBox(Messages.getString(
          "CommonSettingsDialog.useGPinUnitSelection"));
    private final JCheckBox generateNames = new JCheckBox(Messages.getString("CommonSettingsDialog.generateNames"));
    private final JCheckBox showUnitId = new JCheckBox(Messages.getString("CommonSettingsDialog.showUnitId"));
    private final JCheckBox showAutoResolvePanel = new JCheckBox(Messages.getString(
          "CommonSettingsDialog.showAutoResolvePanel"));
    private JComboBox<String> favoritePrincessBehaviorSetting;
    private JComboBox<String> displayLocale;
    private final JCheckBox showIPAddressesInChat = new JCheckBox(Messages.getString(
          "CommonSettingsDialog.showIPAddressesInChat"));
    private final JCheckBox startSearchlightsOn = new JCheckBox(Messages.getString(
          "CommonSettingsDialog.startSearchlightsOn"));
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
    private JTextField moveFontSize;
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
    private ColourSelectorButton csbBoardTextColor;
    private ColourSelectorButton csbBoardSpaceTextColor;
    private ColourSelectorButton csbMapSheetColor;
    private JSpinner attackArrowTransparency;
    private JSpinner ecmTransparency;
    private JSpinner movePathPersistenceOnMiniMap;
    private JTextField buttonsPerRow;
    private JTextField playersRemainingToShow;

    private JComboBox<String> tmmPipModeCbo;
    private final JCheckBox darkenMapAtNight =
          new JCheckBox(Messages.getString("CommonSettingsDialog.darkenMapAtNight"));
    private final JCheckBox translucentHiddenUnits = new JCheckBox(Messages.getString(
          "CommonSettingsDialog.translucentHiddenUnits"));

    // Tactical Overlay Options
    private final JCheckBox fovInsideEnabled = new JCheckBox(Messages.getString(
          "TacticalOverlaySettingsDialog.FovInsideEnabled"));
    private JSlider fovHighlightAlpha;
    private final JCheckBox fovOutsideEnabled = new JCheckBox(Messages.getString(
          "TacticalOverlaySettingsDialog.FovOutsideEnabled"));
    private JSlider fovDarkenAlpha;
    private JSlider numStripesSlider;
    private JCheckBox fovGrayscaleEnabled;
    private JTextField fovHighlightRingsRadii;
    private JTextField fovHighlightRingsColors;

    // Labels (there to make it possible to disable them)
    private JLabel darkenAlphaLabel;
    private JLabel numStripesLabel;
    private JLabel fovHighlightRingsColorsLabel;
    private JLabel fovHighlightRingsRadiiLabel;
    private JLabel highlightAlphaLabel;

    private JLabel stampFormatLabel;
    private JLabel gameLogFilenameLabel;

    private final JCheckBox gameSummaryBV =
          new JCheckBox(Messages.getString("CommonSettingsDialog.gameSummaryBV.name"));
    private final JCheckBox gameSummaryMM =
          new JCheckBox(Messages.getString("CommonSettingsDialog.gameSummaryMM.name"));
    private final JCheckBox gifGameSummaryMM = new JCheckBox(Messages.getString(
          "CommonSettingsDialog.gifGameSummaryMM.name"));
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

    private final MMToggleButton choiceToggle = new MMToggleButton(Messages.getString(
          "CommonSettingsDialog.keyBinds.buttoneTabbing"));
    private final MMButton defaultKeyBindButton = new MMButton("default",
          Messages.getString("CommonSettingsDialog.keyBinds.buttonDefault"));

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

    private JTextField unitDisplayHeatLevel1Text;
    private JTextField unitDisplayHeatLevel2Text;
    private JTextField unitDisplayHeatLevel3Text;
    private JTextField unitDisplayHeatLevel4Text;
    private JTextField unitDisplayHeatLevel5Text;
    private JTextField unitDisplayHeatLevel6Text;
    private JComboBox<String> unitTooltipSeenByCbo;
    private JTextField unitDisplayWeaponListHeightText;

    private ColourSelectorButton csbUnitTooltipArmorMiniIntact;
    private ColourSelectorButton csbUnitTooltipArmorMiniPartial;
    private ColourSelectorButton csbUnitTooltipArmorMiniDamaged;
    private JTextField unitTooltipArmorMiniArmorCharText;
    private JTextField unitTooltipArmorMiniInternalStructureCharText;
    private JTextField unitTooltipArmorMiniCriticalCharText;
    private JTextField unitTooltipArmorMiniDestroyedCharText;
    private JTextField unitTooltipArmorMiniCapArmorCharText;
    private JComboBox<String> unitTooltipFontSizeModCbo;
    private JTextField unitTooltipArmorMiniUnitsPerBlockText;
    private JTextField unitDisplayMekArmorLargeFontSizeText;
    private JTextField unitDisplayMekArmorMediumFontSizeText;
    private JTextField unitDisplayMekArmorSmallFontSizeText;
    private JTextField unitDisplayMekLargeFontSizeText;
    private JTextField unitDisplayMekMediumFontSizeText;

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
    private JTextPane reportKeywordsTextPane;
    private JTextPane reportFilterKeywordsTextPane;
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

    private static final Dimension LABEL_SPACER = new Dimension(5, 0);
    private static final Dimension DEPENDENT_INSET = new Dimension(25, 0);

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
        JTabbedPane panTabs = new JTabbedPane();
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cancelAction();
            }
        });

        JScrollPane settingsPane = new JScrollPane(getSettingsPanel());
        settingsPane.getVerticalScrollBar().setUnitIncrement(16);
        JScrollPane audioPane = new JScrollPane(getAudioPanel());
        audioPane.getVerticalScrollBar().setUnitIncrement(16);
        JScrollPane keyBindPane = new JScrollPane(getKeyBindPanel());
        keyBindPane.getVerticalScrollBar().setUnitIncrement(16);
        JScrollPane advancedSettingsPane = new JScrollPane(getAdvancedSettingsPanel());
        advancedSettingsPane.getVerticalScrollBar().setUnitIncrement(16);
        JScrollPane gameBoardPane = new JScrollPane(getGameBoardPanel());
        gameBoardPane.getVerticalScrollBar().setUnitIncrement(16);
        JScrollPane unitDisplayPane = new JScrollPane(getUnitDisplayPanel());
        unitDisplayPane.getVerticalScrollBar().setUnitIncrement(16);
        JScrollPane miniMapPane = new JScrollPane(getMiniMapPanel());
        miniMapPane.getVerticalScrollBar().setUnitIncrement(16);
        JScrollPane reportPane = new JScrollPane(getReportPanel());
        reportPane.getVerticalScrollBar().setUnitIncrement(16);
        JScrollPane overlaysPane = new JScrollPane(getOverlaysPanel());
        overlaysPane.getVerticalScrollBar().setUnitIncrement(16);
        JScrollPane autoDisplayPane = new JScrollPane(getPhasePanel());
        autoDisplayPane.getVerticalScrollBar().setUnitIncrement(16);
        JScrollPane aiDisplayPane = new JScrollPane(aiDisplayPanel());
        aiDisplayPane.getVerticalScrollBar().setUnitIncrement(16);

        panTabs.add(Messages.getString("CommonSettingsDialog.main"), settingsPane);
        panTabs.add(Messages.getString("CommonSettingsDialog.audio"), audioPane);
        panTabs.add(Messages.getString("CommonSettingsDialog.keyBinds"), keyBindPane);
        panTabs.add(Messages.getString("CommonSettingsDialog.gameBoard"), gameBoardPane);
        panTabs.add(Messages.getString("CommonSettingsDialog.unitDisplay"), unitDisplayPane);
        panTabs.add(Messages.getString("CommonSettingsDialog.miniMap"), miniMapPane);
        panTabs.add(Messages.getString("CommonSettingsDialog.report"), reportPane);
        panTabs.add(Messages.getString("CommonSettingsDialog.overlays"), overlaysPane);
        panTabs.add(Messages.getString("CommonSettingsDialog.buttonOrder"), getButtonOrderPanel());
        panTabs.add(Messages.getString("CommonSettingsDialog.autoDisplay"), autoDisplayPane);
        panTabs.add(Messages.getString("CommonSettingsDialog.aiDisplay"), aiDisplayPane);
        panTabs.add(Messages.getString("CommonSettingsDialog.advanced"), advancedSettingsPane);

        return panTabs;
    }

    private JPanel getAudioPanel() {
        List<List<Component>> comps = new ArrayList<>();
        ArrayList<Component> row;

        row = new ArrayList<>();
        row.add(masterVolumeLabel);
        comps.add(row);

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
        masterVolumeSlider.setMaximumSize(new Dimension(250, 100));
        masterVolumeSlider.setToolTipText(Messages.getString("CommonSettingsDialog.masterVolumeTT"));
        row = new ArrayList<>();
        row.add(masterVolumeSlider);
        comps.add(row);

        addLineSpacer(comps);

        comps.add(checkboxEntry(soundMuteChat, null));

        tfSoundMuteChatFileName = new JTextField(5);
        tfSoundMuteChatFileName.setMaximumSize(new Dimension(450, 40));
        row = new ArrayList<>();
        row.add(tfSoundMuteChatFileName);
        comps.add(row);

        addLineSpacer(comps);

        comps.add(checkboxEntry(soundMuteMyTurn, null));

        tfSoundMuteMyTurnFileName = new JTextField(5);
        tfSoundMuteMyTurnFileName.setMaximumSize(new Dimension(450, 40));
        row = new ArrayList<>();
        row.add(tfSoundMuteMyTurnFileName);
        comps.add(row);

        addLineSpacer(comps);

        comps.add(checkboxEntry(soundMuteOthersTurn, null));

        tfSoundMuteOthersFileName = new JTextField(5);
        tfSoundMuteOthersFileName.setMaximumSize(new Dimension(450, 40));
        row = new ArrayList<>();
        row.add(tfSoundMuteOthersFileName);
        comps.add(row);

        return createSettingsPanel(comps);
    }

    private JPanel getGameBoardPanel() {
        List<List<Component>> comps = new ArrayList<>();
        ArrayList<Component> row;

        // Tileset
        JLabel tileSetChoiceLabel = new JLabel(Messages.getString("CommonSettingsDialog.tileset"));
        tileSetChoice = new JComboBox<>();
        tileSetChoice.setMaximumSize(new Dimension(400, tileSetChoice.getMaximumSize().height));
        row = new ArrayList<>();
        row.add(tileSetChoiceLabel);
        row.add(Box.createHorizontalStrut(15));
        row.add(tileSetChoice);
        comps.add(row);
        addLineSpacer(comps);

        comps.add(checkboxEntry(nagForNoAction, null));
        comps.add(checkboxEntry(nagForPSR, null));
        comps.add(checkboxEntry(nagForMASC, null));
        comps.add(checkboxEntry(nagForSprint, null));
        comps.add(checkboxEntry(nagForCrushingBuildings, null));
        comps.add(checkboxEntry(nagForMechanicalJumpFallDamage, null));
        comps.add(checkboxEntry(nagForWiGELanding, null));
        comps.add(checkboxEntry(nagForNoUnJamRAC, null));
        comps.add(checkboxEntry(nagForLaunchDoors, null));
        comps.add(checkboxEntry(nagForOverheat, null));
        comps.add(checkboxEntry(nagForOddSizedBoard, null));

        addLineSpacer(comps);

        comps.add(checkboxEntry(getFocus, null));
        comps.add(checkboxEntry(autoEndFiring, null));
        comps.add(checkboxEntry(autoDeclareSearchlight, null));
        comps.add(checkboxEntry(moveDefaultClimbMode, null));
        moveDefaultClimbMode.setToolTipText(Messages.getString("CommonSettingsDialog.moveDefaultClimbMode.tooltip"));

        addLineSpacer(comps);

        buttonsPerRow = new JTextField(4);
        buttonsPerRow.setMaximumSize(new Dimension(150, 40));
        JLabel buttonsPerRowLabel = new JLabel(Messages.getString("CommonSettingsDialog.buttonsPerRow"));
        row = new ArrayList<>();
        row.add(buttonsPerRowLabel);
        row.add(buttonsPerRow);
        buttonsPerRow.setText(String.format("%d", GUIP.getButtonsPerRow()));
        buttonsPerRow.setToolTipText(Messages.getString("CommonSettingsDialog.buttonsPerRow.tooltip"));
        comps.add(row);

        playersRemainingToShow = new JTextField(4);
        playersRemainingToShow.setMaximumSize(new Dimension(150, 40));
        JLabel playersRemainingToShowLabel =
              new JLabel(Messages.getString("CommonSettingsDialog.playersRemainingToShow"));
        row = new ArrayList<>();
        row.add(playersRemainingToShowLabel);
        row.add(playersRemainingToShow);
        playersRemainingToShow.setText(String.format("%d", GUIP.getPlayersRemainingToShow()));
        playersRemainingToShow.setToolTipText(
              Messages.getString("CommonSettingsDialog.playersRemainingToShow.tooltip"));
        comps.add(row);

        comps.add(checkboxEntry(mouseWheelZoom, null));
        comps.add(checkboxEntry(mouseWheelZoomFlip, null));
        String msg_tooltip = Messages.getString("CommonSettingsDialog.gameSummaryBV.tooltip",
              Configuration.gameSummaryImagesBVDir());
        comps.add(checkboxEntry(gameSummaryBV, msg_tooltip));

        addLineSpacer(comps);

        JLabel maxPathfinderTimeLabel = new JLabel(Messages.getString("CommonSettingsDialog.pathFiderTimeLimit"));
        maxPathfinderTime = new JTextField(5);
        maxPathfinderTime.setMaximumSize(new Dimension(150, 40));
        row = new ArrayList<>();
        row.add(maxPathfinderTimeLabel);
        row.add(maxPathfinderTime);
        comps.add(row);

        addLineSpacer(comps);

        comps.add(checkboxEntry(showDamageLevel, null));
        comps.add(checkboxEntry(showDamageDecal, null));
        comps.add(checkboxEntry(showUnitId, null));
        comps.add(checkboxEntry(entityOwnerColor, Messages.getString("CommonSettingsDialog.entityOwnerColor.tooltip")));
        comps.add(checkboxEntry(useSoftCenter, Messages.getString("CommonSettingsDialog.useSoftCenter.tooltip")));
        comps.add(checkboxEntry(useAutoCenter, Messages.getString("CommonSettingsDialog.useAutoCenter.tooltip")));
        comps.add(checkboxEntry(useAutoSelectNext,
              Messages.getString("CommonSettingsDialog.useAutoSelectNext.tooltip")));

        row = new ArrayList<>();
        csbUnitTextColor = new ColourSelectorButton(Messages.getString("CommonSettingsDialog.colors.UnitTextColor"));
        csbUnitTextColor.setColour(GUIP.getUnitTextColor());
        row.add(csbUnitTextColor);
        csbUnitValidColor = new ColourSelectorButton(Messages.getString("CommonSettingsDialog.colors.UnitValidColor"));
        csbUnitValidColor.setColour(GUIP.getUnitValidColor());
        row.add(csbUnitValidColor);
        csbUnitSelectedColor = new ColourSelectorButton(Messages.getString(
              "CommonSettingsDialog.colors.UnitSelectedColor"));
        csbUnitSelectedColor.setColour(GUIP.getUnitSelectedColor());
        row.add(csbUnitSelectedColor);
        comps.add(row);

        addLineSpacer(comps);

        comps.add(checkboxEntry(animateMove, null));
        comps.add(checkboxEntry(showWrecks, null));
        comps.add(checkboxEntry(chkHighQualityGraphics,
              Messages.getString("CommonSettingsDialog.highQualityGraphics.tooltip")));
        comps.add(checkboxEntry(chkHighPerformanceGraphics,
              Messages.getString("CommonSettingsDialog.highPerformanceGraphics.tooltip")));
        showMapSheets.addItemListener(this);
        row = new ArrayList<>();
        row.add(showMapSheets);
        csbMapSheetColor = new ColourSelectorButton(Messages.getString("CommonSettingsDialog.colors.MapSheetColor"));
        csbMapSheetColor.setColour(GUIP.getMapsheetColor());
        row.add(csbMapSheetColor);
        comps.add(row);

        comps.add(checkboxEntry(aOHexShadows, null));
        comps.add(checkboxEntry(shadowMap, null));
        comps.add(checkboxEntry(hexInclines, null));
        comps.add(checkboxEntry(levelHighlight, null));
        comps.add(checkboxEntry(floatingIso, null));
        comps.add(checkboxEntry(darkenMapAtNight, null));
        darkenMapAtNight.setSelected(GUIP.getDarkenMapAtNight());
        comps.add(checkboxEntry(translucentHiddenUnits, null));
        translucentHiddenUnits.setSelected(GUIP.getTranslucentHiddenUnits());
        comps.add(checkboxEntry(artilleryDisplayMisses,
              Messages.getString("CommonSettingsDialog.hexes.ShowArtilleryMisses.tooltip")));

        // Artillery and bomb display choices
        artilleryDisplayMisses.setSelected(GUIP.getShowArtilleryMisses());
        comps.add(checkboxEntry(artilleryDisplayDriftedHits,
              Messages.getString("CommonSettingsDialog.hexes.ShowArtilleryDriftedHits.tooltip")));
        artilleryDisplayDriftedHits.setSelected(GUIP.getShowArtilleryDrifts());
        comps.add(checkboxEntry(bombsDisplayMisses,
              Messages.getString("CommonSettingsDialog.hexes.ShowBombMisses.tooltip")));
        bombsDisplayMisses.setSelected(GUIP.getShowBombMisses());
        comps.add(checkboxEntry(bombsDisplayDrifts,
              Messages.getString("CommonSettingsDialog.hexes.ShowBombDrifts.tooltip")));
        bombsDisplayDrifts.setSelected(GUIP.getShowBombDrifts());

        row = new ArrayList<>();

        csbBoardTextColor = new ColourSelectorButton(Messages.getString("CommonSettingsDialog.colors.BoardTextColor"));
        csbBoardTextColor.setColour(GUIP.getBoardTextColor());
        row.add(csbBoardTextColor);

        csbBoardSpaceTextColor = new ColourSelectorButton(Messages.getString(
              "CommonSettingsDialog.colors.BoardSpaceTextColor"));
        csbBoardSpaceTextColor.setColour(GUIP.getBoardSpaceTextColor());
        row.add(csbBoardSpaceTextColor);

        csbBuildingTextColor = new ColourSelectorButton(Messages.getString(
              "CommonSettingsDialog.colors.BuildingTextColor"));
        csbBuildingTextColor.setColour(GUIP.getBuildingTextColor());
        row.add(csbBuildingTextColor);

        csbLowFoliageColor =
              new ColourSelectorButton(Messages.getString("CommonSettingsDialog.colors.LowFoliageColor"));
        csbLowFoliageColor.setColour(GUIP.getLowFoliageColor());
        row.add(csbLowFoliageColor);
        comps.add(row);

        addLineSpacer(comps);

        SpinnerNumberModel mAttackArrowTransparency = new SpinnerNumberModel(GUIP.getAttackArrowTransparency(),
              0,
              256,
              1);
        attackArrowTransparency = new JSpinner(mAttackArrowTransparency);
        attackArrowTransparency.setMaximumSize(new Dimension(150, 40));
        JLabel attackArrowTransparencyLabel = new JLabel(Messages.getString(
              "CommonSettingsDialog.attackArrowTransparency"));
        row = new ArrayList<>();
        row.add(attackArrowTransparencyLabel);
        row.add(attackArrowTransparency);
        attackArrowTransparency.setToolTipText(
              Messages.getString("CommonSettingsDialog.attackArrowTransparency.tooltip"));
        comps.add(row);

        SpinnerNumberModel mECMTransparency = new SpinnerNumberModel(GUIP.getECMTransparency(), 0, 256, 1);
        ecmTransparency = new JSpinner(mECMTransparency);
        ecmTransparency.setMaximumSize(new Dimension(150, 40));
        JLabel ecmTransparencyLabel = new JLabel(Messages.getString("CommonSettingsDialog.ecmTransparency"));
        row = new ArrayList<>();
        row.add(ecmTransparencyLabel);
        row.add(ecmTransparency);
        ecmTransparency.setToolTipText(Messages.getString("CommonSettingsDialog.ecmTransparency.tooltip"));
        comps.add(row);

        tmmPipModeCbo = new JComboBox<>();
        tmmPipModeCbo.addItem(Messages.getString("CommonSettingsDialog.tmmPipMode.NoPips"));
        tmmPipModeCbo.addItem(Messages.getString("CommonSettingsDialog.tmmPipMode.WhitePips"));
        tmmPipModeCbo.addItem(Messages.getString("CommonSettingsDialog.tmmPipMode.ColoredPips"));
        tmmPipModeCbo.addItem(Messages.getString("CommonSettingsDialog.tmmPipMode.WhitePipsBigger"));
        tmmPipModeCbo.addItem(Messages.getString("CommonSettingsDialog.tmmPipMode.ColoredPipsBigger"));
        tmmPipModeCbo.setSelectedIndex(GUIP.getTMMPipMode());
        JLabel tmmPipModeLabel = new JLabel(Messages.getString("CommonSettingsDialog.tmmPipMode"));
        row = new ArrayList<>();
        row.add(tmmPipModeLabel);
        row.add(tmmPipModeCbo);
        comps.add(row);

        addLineSpacer(comps);

        fontTypeChooserMoveFont = new JComboBox<>(new Vector<>(FontHandler.getAvailableNonSymbolFonts()));
        fontTypeChooserMoveFont.setSelectedItem(GUIP.getMoveFontType());

        JLabel moveFontTypeLabel = new JLabel(Messages.getString("CommonSettingsDialog.moveFontType"));
        row = new ArrayList<>();
        row.add(moveFontTypeLabel);
        row.add(fontTypeChooserMoveFont);
        comps.add(row);

        moveFontSize = new JTextField(4);
        moveFontSize.setMaximumSize(new Dimension(150, 40));
        JLabel moveFontSizeLabel = new JLabel(Messages.getString("CommonSettingsDialog.moveFontSize"));
        row = new ArrayList<>();
        row.add(moveFontSizeLabel);
        row.add(moveFontSize);
        moveFontSize.setText(String.format("%d", GUIP.getMoveFontSize()));
        comps.add(row);

        fontStyleChooserMoveFont.addItem(Messages.getString("Plain"));
        fontStyleChooserMoveFont.addItem(Messages.getString("Bold"));
        fontStyleChooserMoveFont.addItem(Messages.getString("Italic"));
        JLabel moveFontStyleLabel = new JLabel(Messages.getString("CommonSettingsDialog.moveFontStyle"));
        row = new ArrayList<>();
        row.add(moveFontStyleLabel);
        row.add(fontStyleChooserMoveFont);
        fontStyleChooserMoveFont.setSelectedIndex(GUIP.getMoveFontStyle());
        comps.add(row);

        row = new ArrayList<>();
        csbMoveDefaultColor =
              new ColourSelectorButton(Messages.getString("CommonSettingsDialog.colors.MoveDefaultColor"));
        csbMoveDefaultColor.setColour(GUIP.getMoveDefaultColor());
        row.add(csbMoveDefaultColor);

        csbMoveIllegalColor =
              new ColourSelectorButton(Messages.getString("CommonSettingsDialog.colors.MoveIllegalColor"));
        csbMoveIllegalColor.setColour(GUIP.getMoveIllegalColor());
        row.add(csbMoveIllegalColor);

        csbMoveJumpColor = new ColourSelectorButton(Messages.getString("CommonSettingsDialog.colors.MoveJumpColor"));
        csbMoveJumpColor.setColour(GUIP.getMoveJumpColor());
        row.add(csbMoveJumpColor);

        csbMoveMASCColor = new ColourSelectorButton(Messages.getString("CommonSettingsDialog.colors.MoveMASCColor"));
        csbMoveMASCColor.setColour(GUIP.getMoveMASCColor());
        row.add(csbMoveMASCColor);
        comps.add(row);

        row = new ArrayList<>();
        csbMoveRunColor = new ColourSelectorButton(Messages.getString("CommonSettingsDialog.colors.MoveRunColor"));
        csbMoveRunColor.setColour(GUIP.getMoveRunColor());
        row.add(csbMoveRunColor);

        csbMoveBackColor = new ColourSelectorButton(Messages.getString("CommonSettingsDialog.colors.MoveBackColor"));
        csbMoveBackColor.setColour(GUIP.getMoveBackColor());
        row.add(csbMoveBackColor);

        csbMoveSprintColor =
              new ColourSelectorButton(Messages.getString("CommonSettingsDialog.colors.MoveSprintColor"));
        csbMoveSprintColor.setColour(GUIP.getMoveSprintColor());
        row.add(csbMoveSprintColor);
        comps.add(row);

        addLineSpacer(comps);

        row = new ArrayList<>();
        csbFireSolutionCanSeeColor = new ColourSelectorButton(Messages.getString(
              "CommonSettingsDialog.colors.FireSolnCanSeeColor"));
        csbFireSolutionCanSeeColor.setColour(GUIP.getFireSolnCanSeeColor());
        row.add(csbFireSolutionCanSeeColor);
        csbFireSolutionNoSeeColor = new ColourSelectorButton(Messages.getString(
              "CommonSettingsDialog.colors.FireSolnNoSeeColor"));
        csbFireSolutionNoSeeColor.setColour(GUIP.getFireSolnNoSeeColor());
        row.add(csbFireSolutionNoSeeColor);
        comps.add(row);

        row = new ArrayList<>();
        csbFieldOfFireMinColor = new ColourSelectorButton(Messages.getString(
              "CommonSettingsDialog.colors.FieldOfFireMinColor"));
        csbFieldOfFireMinColor.setColour(GUIP.getFieldOfFireMinColor());
        row.add(csbFieldOfFireMinColor);
        csbFieldOfFireShortColor = new ColourSelectorButton(Messages.getString(
              "CommonSettingsDialog.colors.FieldOfFireShortColor"));
        csbFieldOfFireShortColor.setColour(GUIP.getFieldOfFireShortColor());
        row.add(csbFieldOfFireShortColor);
        csbFieldOfFireMediumColor = new ColourSelectorButton(Messages.getString(
              "CommonSettingsDialog.colors.FieldOfFireMediumColor"));
        csbFieldOfFireMediumColor.setColour(GUIP.getFieldOfFireMediumColor());
        row.add(csbFieldOfFireMediumColor);
        csbFieldOfFireLongColor = new ColourSelectorButton(Messages.getString(
              "CommonSettingsDialog.colors.FieldOfFireLongColor"));
        csbFieldOfFireLongColor.setColour(GUIP.getFieldOfFireLongColor());
        row.add(csbFieldOfFireLongColor);
        csbFieldOfFireExtremeColor = new ColourSelectorButton(Messages.getString(
              "CommonSettingsDialog.colors.FieldOfFireExtremeColor"));
        csbFieldOfFireExtremeColor.setColour(GUIP.getFieldOfFireExtremeColor());
        row.add(csbFieldOfFireExtremeColor);
        comps.add(row);

        row = new ArrayList<>();
        csbSensorRangeColor =
              new ColourSelectorButton(Messages.getString("CommonSettingsDialog.colors.SensorRangeColor"));
        csbSensorRangeColor.setColour(GUIP.getSensorRangeColor());
        row.add(csbSensorRangeColor);
        csbVisualRangeColor =
              new ColourSelectorButton(Messages.getString("CommonSettingsDialog.colors.VisualRangeColor"));
        csbVisualRangeColor.setColour(GUIP.getVisualRangeColor());
        row.add(csbVisualRangeColor);
        comps.add(row);

        addLineSpacer(comps);

        addSpacer(comps, 3);

        // Highlighting Radius inside FoV
        comps.add(checkboxEntry(fovInsideEnabled, null));

        addSpacer(comps, 2);

        // Inside Opaqueness slider
        fovHighlightAlpha = new JSlider(0, 255);
        fovHighlightAlpha.setMajorTickSpacing(25);
        fovHighlightAlpha.setMinorTickSpacing(5);
        fovHighlightAlpha.setPaintTicks(true);
        fovHighlightAlpha.setPaintLabels(true);
        fovHighlightAlpha.setMaximumSize(new Dimension(800, 100));
        fovHighlightAlpha.addChangeListener(this);
        fovHighlightAlpha.setToolTipText(Messages.getString("TacticalOverlaySettingsDialog.AlphaTooltip"));
        // Label
        highlightAlphaLabel = new JLabel(Messages.getString("TacticalOverlaySettingsDialog.FovHighlightAlpha"));
        row = new ArrayList<>();
        row.add(Box.createRigidArea(DEPENDENT_INSET));
        row.add(highlightAlphaLabel);
        comps.add(row);

        addSpacer(comps, 1);

        row = new ArrayList<>();
        row.add(Box.createRigidArea(DEPENDENT_INSET));
        row.add(fovHighlightAlpha);
        comps.add(row);

        addSpacer(comps, 3);

        row = new ArrayList<>();
        fovHighlightRingsRadiiLabel = new JLabel(Messages.getString(
              "TacticalOverlaySettingsDialog.FovHighlightRingsRadii"));
        row.add(Box.createRigidArea(DEPENDENT_INSET));
        row.add(fovHighlightRingsRadiiLabel);
        comps.add(row);

        addSpacer(comps, 2);

        row = new ArrayList<>();
        fovHighlightRingsRadii = new JTextField((2 + 1) * 7);
        fovHighlightRingsRadii.addFocusListener(this);
        fovHighlightRingsRadii.setMaximumSize(new Dimension(240, 40));
        row.add(Box.createRigidArea(DEPENDENT_INSET));
        row.add(fovHighlightRingsRadii);
        comps.add(row);

        addSpacer(comps, 2);

        row = new ArrayList<>();
        fovHighlightRingsColorsLabel = new JLabel(Messages.getString(
              "TacticalOverlaySettingsDialog.FovHighlightRingsColors"));
        row.add(Box.createRigidArea(DEPENDENT_INSET));
        row.add(fovHighlightRingsColorsLabel);
        comps.add(row);

        addSpacer(comps, 2);

        row = new ArrayList<>();
        fovHighlightRingsColors = new JTextField(50);// ((3+1)*3+1)*7);
        fovHighlightRingsColors.addFocusListener(this);
        fovHighlightRingsColors.setMaximumSize(new Dimension(200, 40));
        row.add(Box.createRigidArea(DEPENDENT_INSET));
        row.add(fovHighlightRingsColors);
        row.add(Box.createHorizontalGlue());
        comps.add(row);

        addLineSpacer(comps);

        // Outside FoV Darkening
        comps.add(checkboxEntry(fovOutsideEnabled, null));

        addSpacer(comps, 1);

        fovDarkenAlpha = new JSlider(0, 255);
        fovDarkenAlpha.setMajorTickSpacing(25);
        fovDarkenAlpha.setMinorTickSpacing(5);
        fovDarkenAlpha.setPaintTicks(true);
        fovDarkenAlpha.setPaintLabels(true);
        fovDarkenAlpha.setMaximumSize(new Dimension(800, 100));
        fovDarkenAlpha.addChangeListener(this);
        fovDarkenAlpha.setToolTipText(Messages.getString("TacticalOverlaySettingsDialog.AlphaTooltip"));
        darkenAlphaLabel = new JLabel(Messages.getString("TacticalOverlaySettingsDialog.FovDarkenAlpha"));
        darkenAlphaLabel.setToolTipText(Messages.getString("TacticalOverlaySettingsDialog.AlphaTooltip"));
        row = new ArrayList<>();
        row.add(Box.createRigidArea(new Dimension(4, 0)));
        row.add(Box.createRigidArea(DEPENDENT_INSET));
        row.add(darkenAlphaLabel);
        comps.add(row);

        addSpacer(comps, 2);

        row = new ArrayList<>();
        row.add(Box.createRigidArea(DEPENDENT_INSET));
        row.add(fovDarkenAlpha);
        comps.add(row);

        addSpacer(comps, 4);

        numStripesSlider = new JSlider(0, 50);
        numStripesSlider.setMajorTickSpacing(10);
        numStripesSlider.setMinorTickSpacing(5);
        numStripesSlider.setPaintTicks(true);
        numStripesSlider.setPaintLabels(true);
        numStripesSlider.setMaximumSize(new Dimension(450, 100));
        numStripesSlider.addChangeListener(this);
        numStripesSlider.setToolTipText(Messages.getString("TacticalOverlaySettingsDialog.FovStripesTooltip"));
        numStripesLabel = new JLabel(Messages.getString("TacticalOverlaySettingsDialog.FovStripes"));
        numStripesLabel.setToolTipText(Messages.getString("TacticalOverlaySettingsDialog.FovStripesTooltip"));
        row = new ArrayList<>();
        row.add(Box.createRigidArea(new Dimension(4, 0)));
        row.add(Box.createRigidArea(DEPENDENT_INSET));
        row.add(numStripesLabel);
        comps.add(row);

        addSpacer(comps, 1);

        row = new ArrayList<>();
        row.add(Box.createRigidArea(new Dimension(4, 0)));
        row.add(Box.createRigidArea(DEPENDENT_INSET));
        row.add(numStripesSlider);
        comps.add(row);

        addSpacer(comps, 3);

        row = new ArrayList<>();
        fovGrayscaleEnabled = new JCheckBox(Messages.getString("TacticalOverlaySettingsDialog.FovGrayscale"));
        fovGrayscaleEnabled.addItemListener(this);
        row.add(Box.createRigidArea(new Dimension(4, 0)));
        row.add(Box.createRigidArea(DEPENDENT_INSET));
        row.add(fovGrayscaleEnabled);
        comps.add(row);

        return createSettingsPanel(comps);
    }

    private JPanel getUnitDisplayPanel() {
        List<List<Component>> comps = new ArrayList<>();
        ArrayList<Component> row;

        tooltipDelay = new JTextField(4);
        tooltipDelay.setMaximumSize(new Dimension(150, 40));
        JLabel tooltipDelayLabel = new JLabel(Messages.getString("CommonSettingsDialog.tooltipDelay"));
        row = new ArrayList<>();
        row.add(tooltipDelayLabel);
        row.add(tooltipDelay);
        comps.add(row);

        tooltipDismissDelay = new JTextField(4);
        tooltipDismissDelay.setMaximumSize(new Dimension(150, 40));
        tooltipDismissDelay.setToolTipText(Messages.getString("CommonSettingsDialog.tooltipDismissDelay.tooltip"));
        JLabel tooltipDismissDelayLabel = new JLabel(Messages.getString("CommonSettingsDialog.tooltipDismissDelay"));
        tooltipDismissDelayLabel.setToolTipText(Messages.getString("CommonSettingsDialog.tooltipDismissDelay.tooltip"));
        row = new ArrayList<>();
        row.add(tooltipDismissDelayLabel);
        row.add(tooltipDismissDelay);
        comps.add(row);

        tooltipDistSuppression = new JTextField(4);
        tooltipDistSuppression.setMaximumSize(new Dimension(150, 40));
        tooltipDistSuppression.setToolTipText(Messages.getString("CommonSettingsDialog.tooltipDistSuppression.tooltip"));
        JLabel tooltipDistSuppressionLabel =
              new JLabel(Messages.getString("CommonSettingsDialog.tooltipDistSuppression"));
        tooltipDistSuppressionLabel.setToolTipText(Messages.getString(
              "CommonSettingsDialog.tooltipDistSuppression.tooltip"));
        row = new ArrayList<>();
        row.add(tooltipDistSuppressionLabel);
        row.add(tooltipDistSuppression);
        comps.add(row);

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
        row = new ArrayList<>();
        row.add(unitTooltipFontSizeModLabel);
        row.add(unitTooltipFontSizeModCbo);
        comps.add(row);

        comps.add(checkboxEntry(showWpsInTT, null));
        comps.add(checkboxEntry(showWpsLocinTT, null));
        comps.add(checkboxEntry(showPilotPortraitTT, null));

        row = new ArrayList<>();
        csbUnitTooltipFGColor = new ColourSelectorButton(Messages.getString(
              "CommonSettingsDialog.colors.UnitTooltipFGColor"));
        csbUnitTooltipFGColor.setColour(GUIP.getUnitToolTipFGColor());
        row.add(csbUnitTooltipFGColor);
        csbUnitTooltipLightFGColor = new ColourSelectorButton(Messages.getString(
              "CommonSettingsDialog.colors.UnitTooltipLightFGColor"));
        csbUnitTooltipLightFGColor.setColour(GUIP.getToolTipLightFGColor());
        row.add(csbUnitTooltipLightFGColor);
        csbUnitTooltipBuildingFGColor = new ColourSelectorButton(Messages.getString(
              "CommonSettingsDialog.colors.UnitTooltipBuildingFGColor"));
        csbUnitTooltipBuildingFGColor.setColour(GUIP.getUnitToolTipBuildingFGColor());
        row.add(csbUnitTooltipBuildingFGColor);
        csbUnitTooltipAltFGColor = new ColourSelectorButton(Messages.getString(
              "CommonSettingsDialog.colors.UnitTooltipAltFGColor"));
        csbUnitTooltipAltFGColor.setColour(GUIP.getUnitToolTipAltFGColor());
        row.add(csbUnitTooltipAltFGColor);
        csbUnitTooltipBlockFGColor = new ColourSelectorButton(Messages.getString(
              "CommonSettingsDialog.colors.UnitTooltipBlockFGColor"));
        csbUnitTooltipBlockFGColor.setColour(GUIP.getUnitToolTipBlockFGColor());
        row.add(csbUnitTooltipBlockFGColor);
        csbUnitTooltipTerrainFGColor = new ColourSelectorButton(Messages.getString(
              "CommonSettingsDialog.colors.UnitTooltipTerrainFGColor"));
        csbUnitTooltipTerrainFGColor.setColour(GUIP.getUnitToolTipTerrainFGColor());
        row.add(csbUnitTooltipTerrainFGColor);
        comps.add(row);

        row = new ArrayList<>();
        csbUnitTooltipBGColor = new ColourSelectorButton(Messages.getString(
              "CommonSettingsDialog.colors.UnitTooltipBGColor"));
        csbUnitTooltipBGColor.setColour(GUIP.getUnitToolTipBGColor());
        row.add(csbUnitTooltipBGColor);
        csbUnitTooltipBuildingBGColor = new ColourSelectorButton(Messages.getString(
              "CommonSettingsDialog.colors.UnitTooltipBuildingBGColor"));
        csbUnitTooltipBuildingBGColor.setColour(GUIP.getUnitToolTipBuildingBGColor());
        row.add(csbUnitTooltipBuildingBGColor);
        csbUnitTooltipAltBGColor = new ColourSelectorButton(Messages.getString(
              "CommonSettingsDialog.colors.UnitTooltipAltBGColor"));
        csbUnitTooltipAltBGColor.setColour(GUIP.getUnitToolTipAltBGColor());
        row.add(csbUnitTooltipAltBGColor);
        csbUnitTooltipBlockBGColor = new ColourSelectorButton(Messages.getString(
              "CommonSettingsDialog.colors.UnitTooltipBlockBGColor"));
        csbUnitTooltipBlockBGColor.setColour(GUIP.getUnitToolTipBlockBGColor());
        row.add(csbUnitTooltipBlockBGColor);
        csbUnitTooltipTerrainBGColor = new ColourSelectorButton(Messages.getString(
              "CommonSettingsDialog.colors.UnitTooltipTerrainBGColor"));
        csbUnitTooltipTerrainBGColor.setColour(GUIP.getUnitToolTipTerrainBGColor());
        row.add(csbUnitTooltipTerrainBGColor);
        comps.add(row);

        row = new ArrayList<>();
        csbUnitTooltipHighlightColor = new ColourSelectorButton(Messages.getString(
              "CommonSettingsDialog.colors.UnitTooltipHighlightColor"));
        csbUnitTooltipHighlightColor.setColour(GUIP.getUnitToolTipHighlightColor());
        row.add(csbUnitTooltipHighlightColor);
        csbUnitTooltipWeaponColor = new ColourSelectorButton(Messages.getString(
              "CommonSettingsDialog.colors.UnitTooltipWeaponColor"));
        csbUnitTooltipWeaponColor.setColour(GUIP.getUnitToolTipWeaponColor());
        row.add(csbUnitTooltipWeaponColor);
        csbUnitTooltipQuirkColor = new ColourSelectorButton(Messages.getString(
              "CommonSettingsDialog.colors.UnitTooltipQuirkColor"));
        csbUnitTooltipQuirkColor.setColour(GUIP.getUnitToolTipQuirkColor());
        row.add(csbUnitTooltipQuirkColor);
        comps.add(row);

        addLineSpacer(comps);

        comps.add(checkboxEntry(showArmorMiniVisTT, null));

        row = new ArrayList<>();
        csbUnitTooltipArmorMiniIntact = new ColourSelectorButton(Messages.getString(
              "CommonSettingsDialog.colors.UnitTooltipArmorMiniIntact"));
        csbUnitTooltipArmorMiniIntact.setColour(GUIP.getUnitTooltipArmorMiniColorIntact());
        row.add(csbUnitTooltipArmorMiniIntact);
        csbUnitTooltipArmorMiniPartial = new ColourSelectorButton(Messages.getString(
              "CommonSettingsDialog.colors.UnitTooltipArmorMiniPartialDamage"));
        csbUnitTooltipArmorMiniPartial.setColour(GUIP.getUnitTooltipArmorMiniColorPartialDamage());
        row.add(csbUnitTooltipArmorMiniPartial);
        csbUnitTooltipArmorMiniDamaged = new ColourSelectorButton(Messages.getString(
              "CommonSettingsDialog.colors.UnitTooltipArmorMiniDamaged"));
        csbUnitTooltipArmorMiniDamaged.setColour(GUIP.getUnitTooltipArmorMiniColorDamaged());
        row.add(csbUnitTooltipArmorMiniDamaged);
        comps.add(row);

        JLabel unitTooltipArmorLabel = new JLabel(Messages.getString("CommonSettingsDialog.armorMiniArmorChar"));
        unitTooltipArmorMiniArmorCharText = new JTextField(5);
        unitTooltipArmorMiniArmorCharText.setText(GUIP.getUnitToolTipArmorMiniArmorChar());
        unitTooltipArmorMiniArmorCharText.setMaximumSize(new Dimension(150, 40));
        unitTooltipArmorMiniArmorCharText.setToolTipText(Messages.getString(
              "CommonSettingsDialog.armorMiniArmorChar.tooltip"));
        row = new ArrayList<>();
        row.add(unitTooltipArmorLabel);
        row.add(unitTooltipArmorMiniArmorCharText);
        comps.add(row);

        JLabel unitTooltipInternalStructureLabel = new JLabel(Messages.getString(
              "CommonSettingsDialog.armorMiniInternalStructureChar"));
        unitTooltipArmorMiniInternalStructureCharText = new JTextField(5);
        unitTooltipArmorMiniInternalStructureCharText.setText(GUIP.getUnitToolTipArmorMiniISChar());
        unitTooltipArmorMiniInternalStructureCharText.setMaximumSize(new Dimension(150, 40));
        unitTooltipArmorMiniInternalStructureCharText.setToolTipText(Messages.getString(
              "CommonSettingsDialog.armorMiniInternalStructureChar.tooltip"));
        row = new ArrayList<>();
        row.add(unitTooltipInternalStructureLabel);
        row.add(unitTooltipArmorMiniInternalStructureCharText);
        comps.add(row);

        JLabel unitTooltipCriticalLabel = new JLabel(Messages.getString("CommonSettingsDialog.armorMiniCriticalChar"));
        unitTooltipArmorMiniCriticalCharText = new JTextField(5);
        unitTooltipArmorMiniCriticalCharText.setText(GUIP.getUnitToolTipArmorMiniCriticalChar());
        unitTooltipArmorMiniCriticalCharText.setMaximumSize(new Dimension(150, 40));
        unitTooltipArmorMiniCriticalCharText.setToolTipText(Messages.getString(
              "CommonSettingsDialog.armorMiniCriticalChar.tooltip"));
        row = new ArrayList<>();
        row.add(unitTooltipCriticalLabel);
        row.add(unitTooltipArmorMiniCriticalCharText);
        comps.add(row);

        JLabel unitTooltipDestroyedLabel =
              new JLabel(Messages.getString("CommonSettingsDialog.armorMiniDestroyedChar"));
        unitTooltipArmorMiniDestroyedCharText = new JTextField(5);
        unitTooltipArmorMiniDestroyedCharText.setText(GUIP.getUnitToolTipArmorMiniDestroyedChar());
        unitTooltipArmorMiniDestroyedCharText.setMaximumSize(new Dimension(150, 40));
        unitTooltipArmorMiniDestroyedCharText.setToolTipText(Messages.getString(
              "CommonSettingsDialog.armorMiniDestroyedChar.tooltip"));
        row = new ArrayList<>();
        row.add(unitTooltipDestroyedLabel);
        row.add(unitTooltipArmorMiniDestroyedCharText);
        comps.add(row);

        JLabel unitTooltipCapArmorLabel = new JLabel(Messages.getString("CommonSettingsDialog.armorMiniCapArmorChar"));
        unitTooltipArmorMiniCapArmorCharText = new JTextField(5);
        unitTooltipArmorMiniCapArmorCharText.setText(GUIP.getUnitToolTipArmorMiniCapArmorChar());
        unitTooltipArmorMiniCapArmorCharText.setMaximumSize(new Dimension(150, 40));
        unitTooltipArmorMiniCapArmorCharText.setToolTipText(Messages.getString(
              "CommonSettingsDialog.armorMiniCapArmorChar.tooltip"));
        row = new ArrayList<>();
        row.add(unitTooltipCapArmorLabel);
        row.add(unitTooltipArmorMiniCapArmorCharText);
        comps.add(row);

        JLabel unitTooltipUnitsPerBlockLabel = new JLabel(Messages.getString(
              "CommonSettingsDialog.armorMiniUnitsPerBlock"));
        unitTooltipArmorMiniUnitsPerBlockText = new JTextField(5);
        unitTooltipArmorMiniUnitsPerBlockText.setText(String.format("%d", GUIP.getUnitToolTipArmorMiniUnitsPerBlock()));
        unitTooltipArmorMiniUnitsPerBlockText.setMaximumSize(new Dimension(150, 40));
        unitTooltipArmorMiniUnitsPerBlockText.setToolTipText(Messages.getString(
              "CommonSettingsDialog.armorMiniUnitsPerBlock.tooltip"));
        row = new ArrayList<>();
        row.add(unitTooltipUnitsPerBlockLabel);
        row.add(unitTooltipArmorMiniUnitsPerBlockText);
        comps.add(row);

        addLineSpacer(comps);

        JLabel unitTooltipSeenByLabel = new JLabel(Messages.getString("CommonSettingsDialog.seenby.label"));
        unitTooltipSeenByCbo = new JComboBox<>();
        unitTooltipSeenByCbo.addItem(Messages.getString("CommonSettingsDialog.seenby.Someone"));
        unitTooltipSeenByCbo.addItem(Messages.getString("CommonSettingsDialog.seenby.Team"));
        unitTooltipSeenByCbo.addItem(Messages.getString("CommonSettingsDialog.seenby.Player"));
        unitTooltipSeenByCbo.addItem(Messages.getString("CommonSettingsDialog.seenby.PlayerDetailed"));
        unitTooltipSeenByCbo.setSelectedIndex(GUIP.getUnitToolTipSeenByResolution());
        unitTooltipSeenByCbo.setMaximumSize(new Dimension(300, 60));
        row = new ArrayList<>();
        row.add(unitTooltipSeenByLabel);
        row.add(unitTooltipSeenByCbo);
        comps.add(row);

        JLabel phaseLabel = new JLabel(Messages.getString("CommonSettingsDialog.colors.UnitDisplayHeatLevels"));
        row = new ArrayList<>();
        row.add(phaseLabel);
        comps.add(row);

        row = new ArrayList<>();
        csbUnitDisplayHeatLevel1 = new ColourSelectorButton(Messages.getString(
              "CommonSettingsDialog.colors.UnitDisplayHeatLevel1"));
        csbUnitDisplayHeatLevel1.setColour(GUIP.getUnitDisplayHeatLevel1());
        row.add(csbUnitDisplayHeatLevel1);
        unitDisplayHeatLevel1Text = new JTextField(5);
        unitDisplayHeatLevel1Text.setText(String.format("%d", GUIP.getUnitDisplayHeatValue1()));
        unitDisplayHeatLevel1Text.setMaximumSize(new Dimension(150, 40));
        unitDisplayHeatLevel1Text.setToolTipText(Messages.getString("CommonSettingsDialog.unitDisplayHeatToolTip"));
        row.add(unitDisplayHeatLevel1Text);
        csbUnitDisplayHeatLevel2 = new ColourSelectorButton(Messages.getString(
              "CommonSettingsDialog.colors.UnitDisplayHeatLevel2"));
        csbUnitDisplayHeatLevel2.setColour(GUIP.getUnitDisplayHeatLevel2());
        row.add(csbUnitDisplayHeatLevel2);
        unitDisplayHeatLevel2Text = new JTextField(5);
        unitDisplayHeatLevel2Text.setText(String.format("%d", GUIP.getUnitDisplayHeatValue2()));
        unitDisplayHeatLevel2Text.setMaximumSize(new Dimension(150, 40));
        unitDisplayHeatLevel2Text.setToolTipText(Messages.getString("CommonSettingsDialog.unitDisplayHeatToolTip"));
        row.add(unitDisplayHeatLevel2Text);
        csbUnitDisplayHeatLevel3 = new ColourSelectorButton(Messages.getString(
              "CommonSettingsDialog.colors.UnitDisplayHeatLevel3"));
        csbUnitDisplayHeatLevel3.setColour(GUIP.getUnitDisplayHeatLevel3());
        row.add(csbUnitDisplayHeatLevel3);
        unitDisplayHeatLevel3Text = new JTextField(5);
        unitDisplayHeatLevel3Text.setText(String.format("%d", GUIP.getUnitDisplayHeatValue3()));
        unitDisplayHeatLevel3Text.setMaximumSize(new Dimension(150, 40));
        unitDisplayHeatLevel3Text.setToolTipText(Messages.getString("CommonSettingsDialog.unitDisplayHeatToolTip"));
        row.add(unitDisplayHeatLevel3Text);
        comps.add(row);

        row = new ArrayList<>();
        csbUnitDisplayHeatLevel4 = new ColourSelectorButton(Messages.getString(
              "CommonSettingsDialog.colors.UnitDisplayHeatLevel4"));
        csbUnitDisplayHeatLevel4.setColour(GUIP.getUnitDisplayHeatLevel4());
        row.add(csbUnitDisplayHeatLevel4);
        unitDisplayHeatLevel4Text = new JTextField(5);
        unitDisplayHeatLevel4Text.setText(String.format("%d", GUIP.getUnitDisplayHeatValue4()));
        unitDisplayHeatLevel4Text.setMaximumSize(new Dimension(150, 40));
        unitDisplayHeatLevel4Text.setToolTipText(Messages.getString("CommonSettingsDialog.unitDisplayHeatToolTip"));
        row.add(unitDisplayHeatLevel4Text);
        csbUnitDisplayHeatLevel5 = new ColourSelectorButton(Messages.getString(
              "CommonSettingsDialog.colors.UnitDisplayHeatLevel5"));
        csbUnitDisplayHeatLevel5.setColour(GUIP.getUnitDisplayHeatLevel5());
        row.add(csbUnitDisplayHeatLevel5);
        unitDisplayHeatLevel5Text = new JTextField(5);
        unitDisplayHeatLevel5Text.setText(String.format("%d", GUIP.getUnitDisplayHeatValue5()));
        unitDisplayHeatLevel5Text.setMaximumSize(new Dimension(150, 40));
        unitDisplayHeatLevel5Text.setToolTipText(Messages.getString("CommonSettingsDialog.unitDisplayHeatToolTip"));
        row.add(unitDisplayHeatLevel5Text);
        csbUnitDisplayHeatLevel6 = new ColourSelectorButton(Messages.getString(
              "CommonSettingsDialog.colors.UnitDisplayHeatLevel6"));
        csbUnitDisplayHeatLevel6.setColour(GUIP.getUnitDisplayHeatLevel6());
        row.add(csbUnitDisplayHeatLevel6);
        unitDisplayHeatLevel6Text = new JTextField(5);
        unitDisplayHeatLevel6Text.setText(String.format("%d", GUIP.getUnitDisplayHeatValue6()));
        unitDisplayHeatLevel6Text.setMaximumSize(new Dimension(150, 40));
        unitDisplayHeatLevel6Text.setToolTipText(Messages.getString("CommonSettingsDialog.unitDisplayHeatToolTip"));
        row.add(unitDisplayHeatLevel6Text);
        comps.add(row);

        row = new ArrayList<>();
        csbUnitDisplayHeatLevelOverheat = new ColourSelectorButton(Messages.getString(
              "CommonSettingsDialog.colors.UnitDisplayHeatLevelOverheat"));
        csbUnitDisplayHeatLevelOverheat.setColour(GUIP.getUnitDisplayHeatLevelOverheat());
        row.add(csbUnitDisplayHeatLevelOverheat);
        comps.add(row);

        addLineSpacer(comps);

        row = new ArrayList<>();
        JLabel orderLabel = new JLabel(Messages.getString("CommonSettingsDialog.orderLabel") + ": ");
        row.add(orderLabel);

        unitDisplayNonTabbed.addElement(UNIT_DISPLAY_ORDER_PREFERENCES.getString(UnitDisplayPanel.NON_TABBED_A1));
        unitDisplayNonTabbed.addElement(UNIT_DISPLAY_ORDER_PREFERENCES.getString(UnitDisplayPanel.NON_TABBED_B1));
        unitDisplayNonTabbed.addElement(UNIT_DISPLAY_ORDER_PREFERENCES.getString(UnitDisplayPanel.NON_TABBED_C1));
        unitDisplayNonTabbed.addElement(UNIT_DISPLAY_ORDER_PREFERENCES.getString(UnitDisplayPanel.NON_TABBED_A2));
        unitDisplayNonTabbed.addElement(UNIT_DISPLAY_ORDER_PREFERENCES.getString(UnitDisplayPanel.NON_TABBED_B2));
        unitDisplayNonTabbed.addElement(UNIT_DISPLAY_ORDER_PREFERENCES.getString(UnitDisplayPanel.NON_TABBED_C2));

        JList<String> listUnitDisplayNonTabbed = new JList<>(unitDisplayNonTabbed);
        listUnitDisplayNonTabbed.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        listUnitDisplayNonTabbed.setVisibleRowCount(2);
        listUnitDisplayNonTabbed.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listUnitDisplayNonTabbed.addMouseListener(cmdMouseAdaptor);
        listUnitDisplayNonTabbed.addMouseMotionListener(cmdMouseAdaptor);
        row.add(listUnitDisplayNonTabbed);
        comps.add(row);

        addLineSpacer(comps);

        JLabel defaultSortOrderLabel = new JLabel(Messages.getString("CommonSettingsDialog.defaultWeaponSortOrder"));
        String toolTip = Messages.getString("CommonSettingsDialog.defaultWeaponSortOrder.tooltip");
        defaultSortOrderLabel.setToolTipText(toolTip);

        final DefaultComboBoxModel<WeaponSortOrder> defaultWeaponSortOrderModel = new DefaultComboBoxModel<>(
              WeaponSortOrder.values());
        defaultWeaponSortOrderModel.removeElement(WeaponSortOrder.CUSTOM); // Custom makes no sense as a default
        comboDefaultWeaponSortOrder = new MMComboBox<>("comboDefaultWeaponSortOrder", defaultWeaponSortOrderModel);
        comboDefaultWeaponSortOrder.setToolTipText(toolTip);
        row = new ArrayList<>();
        row.add(defaultSortOrderLabel);
        row.add(comboDefaultWeaponSortOrder);
        comps.add(row);

        JLabel weaponListHeightLabel = new JLabel(Messages.getString("CommonSettingsDialog.weaponListHeight"));
        unitDisplayWeaponListHeightText = new JTextField(5);
        unitDisplayWeaponListHeightText.setText(String.format("%d", GUIP.getUnitDisplayWeaponListHeight()));
        unitDisplayWeaponListHeightText.setMaximumSize(new Dimension(150, 40));
        row = new ArrayList<>();
        row.add(weaponListHeightLabel);
        row.add(unitDisplayWeaponListHeightText);
        comps.add(row);

        addLineSpacer(comps);

        JLabel unitDisplayMekArmorLargeFontSizeLabel = new JLabel(Messages.getString(
              "CommonSettingsDialog.unitDisplayMekArmorLargeFontSize"));
        unitDisplayMekArmorLargeFontSizeText = new JTextField(5);
        unitDisplayMekArmorLargeFontSizeText.setText(String.format("%d", GUIP.getUnitDisplayMekArmorLargeFontSize()));
        unitDisplayMekArmorLargeFontSizeText.setMaximumSize(new Dimension(150, 40));
        row = new ArrayList<>();
        row.add(unitDisplayMekArmorLargeFontSizeLabel);
        row.add(unitDisplayMekArmorLargeFontSizeText);
        comps.add(row);

        JLabel unitDisplayMekArmorMediumFontSizeLabel = new JLabel(Messages.getString(
              "CommonSettingsDialog.unitDisplayMekArmorMediumFontSize"));
        unitDisplayMekArmorMediumFontSizeText = new JTextField(5);
        unitDisplayMekArmorMediumFontSizeText.setText(String.format("%d", GUIP.getUnitDisplayMekArmorMediumFontSize()));
        unitDisplayMekArmorMediumFontSizeText.setMaximumSize(new Dimension(150, 40));
        row = new ArrayList<>();
        row.add(unitDisplayMekArmorMediumFontSizeLabel);
        row.add(unitDisplayMekArmorMediumFontSizeText);
        comps.add(row);

        JLabel unitDisplayMekArmorSmallFontSizeLabel = new JLabel(Messages.getString(
              "CommonSettingsDialog.unitDisplayMekArmorSmallFontSize"));
        unitDisplayMekArmorSmallFontSizeText = new JTextField(5);
        unitDisplayMekArmorSmallFontSizeText.setText(String.format("%d", GUIP.getUnitDisplayMekArmorSmallFontSize()));
        unitDisplayMekArmorSmallFontSizeText.setMaximumSize(new Dimension(150, 40));
        row = new ArrayList<>();
        row.add(unitDisplayMekArmorSmallFontSizeLabel);
        row.add(unitDisplayMekArmorSmallFontSizeText);
        comps.add(row);

        JLabel unitDisplayMekLargeFontSizeLabel = new JLabel(Messages.getString(
              "CommonSettingsDialog.unitDisplayMekLargeFontSize"));
        unitDisplayMekLargeFontSizeText = new JTextField(5);
        unitDisplayMekLargeFontSizeText.setText(String.format("%d", GUIP.getUnitDisplayMekLargeFontSize()));
        unitDisplayMekLargeFontSizeText.setMaximumSize(new Dimension(150, 40));
        row = new ArrayList<>();
        row.add(unitDisplayMekLargeFontSizeLabel);
        row.add(unitDisplayMekLargeFontSizeText);
        comps.add(row);

        JLabel unitDisplayMekMediumFontSizeLabel = new JLabel(Messages.getString(
              "CommonSettingsDialog.unitDisplayMekMediumFontSize"));
        unitDisplayMekMediumFontSizeText = new JTextField(5);
        unitDisplayMekMediumFontSizeText.setText(String.format("%d", GUIP.getUnitDisplayMekMediumFontSize()));
        unitDisplayMekMediumFontSizeText.setMaximumSize(new Dimension(150, 40));
        row = new ArrayList<>();
        row.add(unitDisplayMekMediumFontSizeLabel);
        row.add(unitDisplayMekMediumFontSizeText);
        comps.add(row);

        return createSettingsPanel(comps);
    }

    private JPanel getReportPanel() {
        List<List<Component>> comps = new ArrayList<>();
        ArrayList<Component> row;

        row = new ArrayList<>();
        csbReportLinkColor =
              new ColourSelectorButton(Messages.getString("CommonSettingsDialog.colors.ReportLinkColor"));
        csbReportLinkColor.setColour(GUIP.getReportLinkColor());
        row.add(csbReportLinkColor);

        csbReportSuccessColor = new ColourSelectorButton(Messages.getString(
              "CommonSettingsDialog.colors.ReportSuccessColor"));
        csbReportSuccessColor.setColour(GUIP.getReportSuccessColor());
        row.add(csbReportSuccessColor);

        csbReportMissColor =
              new ColourSelectorButton(Messages.getString("CommonSettingsDialog.colors.ReportMissColor"));
        csbReportMissColor.setColour(GUIP.getReportMissColor());
        row.add(csbReportMissColor);
        comps.add(row);

        csbReportInfoColor =
              new ColourSelectorButton(Messages.getString("CommonSettingsDialog.colors.ReportInfoColor"));
        csbReportInfoColor.setColour(GUIP.getReportInfoColor());
        row.add(csbReportInfoColor);
        comps.add(row);

        fontTypeChooserReportFont = new JComboBox<>(new Vector<>(FontHandler.getAvailableNonSymbolFonts()));
        fontTypeChooserReportFont.setSelectedItem(GUIP.getReportFontType());

        JLabel moveFontTypeLabel = new JLabel(Messages.getString("CommonSettingsDialog.reportFontType"));
        row = new ArrayList<>();
        row.add(moveFontTypeLabel);
        row.add(fontTypeChooserReportFont);
        comps.add(row);

        comps.add(checkboxEntry(showReportSprites, null));
        showReportSprites.setSelected(GUIP.getMiniReportShowSprites());

        addLineSpacer(comps);

        row = new ArrayList<>();
        row.add(chkReportShowPlayers);
        chkReportShowPlayers.setToolTipText(Messages.getString(
              "CommonSettingsDialog.showReportPlayerList.tooltip"));
        chkReportShowPlayers.setSelected(GUIP.getMiniReportShowPlayers());
        comps.add(row);

        row = new ArrayList<>();
        row.add(chkReportShowUnits);
        chkReportShowUnits.setToolTipText(Messages.getString(
              "CommonSettingsDialog.showReportUnitList.tooltip"));
        chkReportShowUnits.setSelected(GUIP.getMiniReportShowUnits());
        comps.add(row);

        row = new ArrayList<>();
        row.add(chkReportShowKeywords);
        chkReportShowKeywords.setToolTipText(Messages.getString(
              "CommonSettingsDialog.showReportKeywordsList.tooltip"));
        chkReportShowKeywords.setSelected(GUIP.getMiniReportShowKeywords());
        comps.add(row);
        JLabel reportKeywordsLabel = new JLabel(Messages.getString("CommonSettingsDialog.reportKeywords") + ": ");
        reportKeywordsTextPane = new JTextPane();
        row = new ArrayList<>();
        row.add(reportKeywordsLabel);
        row.add(reportKeywordsTextPane);
        comps.add(row);

        row = new ArrayList<>();
        row.add(chkReportShowFilter);
        chkReportShowFilter.setToolTipText(Messages.getString(
              "CommonSettingsDialog.showReportFilterList.tooltip"));
        chkReportShowFilter.setSelected(GUIP.getMiniReportShowFilter());
        comps.add(row);
        addLineSpacer(comps);

        JLabel reportFilterKeywordsLabel = new JLabel(Messages.getString("CommonSettingsDialog.reportFilterKeywords")
              + ":"
              + " ");
        reportFilterKeywordsTextPane = new JTextPane();
        row = new ArrayList<>();
        row.add(reportFilterKeywordsLabel);
        row.add(reportFilterKeywordsTextPane);
        comps.add(row);

        return createSettingsPanel(comps);
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
        JLabel minimapThemeLabel = new JLabel(Messages.getString("CommonSettingsDialog.minimapTheme"));
        minimapTheme = new MMComboBox<>("minimapTheme", new FileNameComboBoxModel(GUIP.getMinimapThemes()));
        minimapTheme.setMaximumSize(new Dimension(200, 25));
        minimapTheme.setSelectedItem(CLIENT_PREFERENCES.getMinimapTheme().getName());

        List<Component> row = new ArrayList<>();
        row.add(minimapThemeLabel);
        row.add(Box.createHorizontalStrut(15));
        row.add(minimapTheme);

        MapSettings mapSettings = MapSettings.getInstance();
        var board = BoardUtilities.generateRandom(mapSettings);

        boardImage = MinimapPanel.getMinimapImageMaxZoom(board, CLIENT_PREFERENCES.getMinimapTheme());

        boardImageLabel = new JLabel(new ImageIcon(boardImage));
        boardImageLabel.setPreferredSize(new Dimension(250, 250));

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
        row.add(boardImageLabel);
        comps.add(row);
        addLineSpacer(comps);
        comps.add(checkboxEntry(mmSymbol, null));
        comps.add(checkboxEntry(gameSummaryMM,
              Messages.getString("CommonSettingsDialog.gameSummaryMM.tooltip",
                    Configuration.gameSummaryImagesMMDir())));
        comps.add(checkboxEntry(gifGameSummaryMM,
              Messages.getString("CommonSettingsDialog.gifGameSummaryMM.tooltip",
                    Configuration.gameSummaryImagesMMDir())));
        comps.add(checkboxEntry(drawFacingArrowsOnMiniMap, null));
        comps.add(checkboxEntry(drawSensorRangeOnMiniMap, null));
        comps.add(checkboxEntry(paintBordersOnMiniMap, null));
        comps.add(checkboxEntry(showUnitDisplayNamesOnMinimap,
              Messages.getString("CommonSettingsDialog.showUnitDisplayNamesOnMinimap.tooltip")));

        SpinnerNumberModel movePathPersistenceModel = new SpinnerNumberModel(GUIP.getMovePathPersistenceOnMiniMap(),
              0,
              100,
              1);
        movePathPersistenceOnMiniMap = new JSpinner(movePathPersistenceModel);
        movePathPersistenceOnMiniMap.setMaximumSize(new Dimension(150, 40));
        movePathPersistenceOnMiniMap.setToolTipText(Messages.getString(
              "CommonSettingsDialog.movePathPersistence.tooltip"));
        JLabel movePathPersistenceOnMiniMapLabel = new JLabel(Messages.getString(
              "CommonSettingsDialog.movePathPersistence"));
        movePathPersistenceOnMiniMapLabel.setLabelFor(movePathPersistenceOnMiniMap);
        row = new ArrayList<>();
        row.add(movePathPersistenceOnMiniMapLabel);
        row.add(movePathPersistenceOnMiniMap);
        comps.add(row);

        return createSettingsPanel(comps);
    }

    private static class PlayerColourHelper {
        PlayerColour pc;
        ColourSelectorButton csb;

        public PlayerColourHelper(PlayerColour pc, ColourSelectorButton csb) {
            this.pc = pc;
            this.csb = csb;
        }
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
            ColourSelectorButton csb = new ColourSelectorButton("");
            csb.setToolTipText(Messages.getString("CommonSettingsDialog.playerColour.tooltip"));
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
        displayLocale.setMaximumSize(new Dimension(150, 40));
        row = new ArrayList<>();
        row.add(displayLocaleLabel);
        row.add(displayLocale);
        comps.add(row);

        addLineSpacer(comps);

        guiScale = new JSlider();
        guiScale.setMajorTickSpacing(3);
        guiScale.setMinimum(7);
        guiScale.setMaximum(24);
        Hashtable<Integer, JComponent> table = new Hashtable<>();
        table.put(7, new JLabel("70%"));
        table.put(10, new JLabel("100%"));
        table.put(16, new JLabel("160%"));
        table.put(22, new JLabel("220%"));
        guiScale.setLabelTable(table);
        guiScale.setPaintTicks(true);
        guiScale.setPaintLabels(true);
        guiScale.setMaximumSize(new Dimension(250, 100));
        guiScale.setToolTipText(Messages.getString("CommonSettingsDialog.guiScaleTT"));
        JLabel guiScaleLabel = new JLabel(Messages.getString("CommonSettingsDialog.guiScale"));
        row = new ArrayList<>();
        row.add(guiScaleLabel);
        row.add(guiScale);
        comps.add(row);

        addLineSpacer(comps);

        JLabel userDirLabel = new JLabel(Messages.getString("CommonSettingsDialog.userDir"));
        userDirLabel.setToolTipText(Messages.getString("CommonSettingsDialog.userDir.tooltip"));
        userDir = new JTextField(20);
        userDir.setMaximumSize(new Dimension(250, 40));
        userDir.setToolTipText(Messages.getString("CommonSettingsDialog.userDir.tooltip"));
        JButton userDirChooser = new JButton("...");
        userDirChooser.addActionListener(e -> fileChooseUserDir(userDir, getFrame()));
        userDirChooser.setToolTipText(Messages.getString("CommonSettingsDialog.userDir.chooser.title"));
        JButton userDirHelp = new JButton(new FlatHelpButtonIcon());
        userDirHelp.putClientProperty("JButton.buttonType", "help");
        try {
            String helpTitle = Messages.getString("UserDirHelpDialog.title");
            URL helpFile = new File(MMConstants.USER_DIR_README_FILE).toURI().toURL();
            userDirHelp.addActionListener(e -> new HelpDialog(helpTitle, helpFile, getFrame()).setVisible(true));
        } catch (MalformedURLException e) {
            logger.error(e,
                  "Could not find the user data directory readme file at {}", MMConstants.USER_DIR_README_FILE);
        }
        row = new ArrayList<>();
        row.add(userDirLabel);
        row.add(userDir);
        row.add(Box.createHorizontalStrut(10));
        row.add(userDirChooser);
        row.add(Box.createHorizontalStrut(10));
        row.add(userDirHelp);
        comps.add(row);

        addLineSpacer(comps);

        JLabel mmlPathLabel = new JLabel(Messages.getString("CommonSettingsDialog.mmlPath"));
        mmlPathLabel.setToolTipText(Messages.getString("CommonSettingsDialog.mmlPath.tooltip"));
        mmlPath = new JTextField(20);
        mmlPath.setMaximumSize(new Dimension(250, 40));
        mmlPath.setToolTipText(Messages.getString("CommonSettingsDialog.mmlPath.tooltip"));
        JButton mmlPathChooser = new JButton("...");
        mmlPathChooser.addActionListener(e -> fileChoose(mmlPath,
              getFrame(),
              Messages.getString("CommonSettingsDialog.mmlPath.chooser.title"),
              false));
        row = new ArrayList<>();
        row.add(mmlPathLabel);
        row.add(mmlPath);
        row.add(Box.createHorizontalStrut(10));
        row.add(mmlPathChooser);
        comps.add(row);

        addLineSpacer(comps);

        // UI Theme
        uiThemes = new JComboBox<>();
        uiThemes.setMaximumSize(new Dimension(400, uiThemes.getMaximumSize().height));
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
        skinFiles.setMaximumSize(new Dimension(400, skinFiles.getMaximumSize().height));
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

        comps.add(checkboxEntry(teamColoring, Messages.getString("CommonSettingsDialog.teamColoring.tooltip")));
        comps.add(checkboxEntry(dockOnLeft, null));
        dockOnLeft.setSelected(GUIP.getDockOnLeft());
        comps.add(checkboxEntry(dockMultipleOnYAxis, null));
        dockMultipleOnYAxis.setSelected(GUIP.getDockMultipleOnYAxis());
        comps.add(checkboxEntry(useCamoOverlay, null));
        useCamoOverlay.setSelected(GUIP.getUseCamoOverlay());

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
        row = new ArrayList<>();
        row.add(unitStartCharLabel);
        row.add(unitStartChar);
        comps.add(row);

        comps.add(checkboxEntry(defaultAutoEjectDisabled, null));
        comps.add(checkboxEntry(useAverageSkills, null));
        comps.add(checkboxEntry(useGPinUnitSelection,
              "This changes the BV/PV displayed in the unit selection list. It does not change the pilot/gunnery of the mek once selected. Request restart of Megamek."));
        comps.add(checkboxEntry(generateNames, null));

        addLineSpacer(comps);
        comps.add(checkboxEntry(datasetLogging, null));
        comps.add(checkboxEntry(keepGameLog, null));

        gameLogFilenameLabel = new JLabel(Messages.getString("CommonSettingsDialog.logFileName"));
        gameLogFilename = new JTextField(15);
        gameLogFilename.setMaximumSize(new Dimension(250, 40));
        row = new ArrayList<>();
        row.add(Box.createRigidArea(DEPENDENT_INSET));
        row.add(gameLogFilenameLabel);
        row.add(gameLogFilename);
        comps.add(row);

        JLabel autoResolveLogFilenameLabel = new JLabel(Messages.getString("CommonSettingsDialog.autoResolveLogFileName"));
        autoResolveLogFilename = new JTextField(15);
        autoResolveLogFilename.setMaximumSize(new Dimension(250, 40));
        row = new ArrayList<>();
        row.add(Box.createRigidArea(DEPENDENT_INSET));
        row.add(autoResolveLogFilenameLabel);
        row.add(autoResolveLogFilename);
        comps.add(row);

        addSpacer(comps, 5);
        comps.add(checkboxEntry(stampFilenames, null));

        stampFormatLabel = new JLabel(Messages.getString("CommonSettingsDialog.stampFormat"));
        stampFormat = new JTextField(15);
        stampFormat.setMaximumSize(new Dimension(15 * 13, 40));
        row = new ArrayList<>();
        row.add(Box.createRigidArea(DEPENDENT_INSET));
        row.add(stampFormatLabel);
        row.add(stampFormat);
        comps.add(row);

        addLineSpacer(comps);

        comps.add(checkboxEntry(showIPAddressesInChat,
              Messages.getString("CommonSettingsDialog.showIPAddressesInChat.tooltip")));
        comps.add(checkboxEntry(startSearchlightsOn,
              Messages.getString("CommonSettingsDialog.startSearchlightsOn.tooltip")));
        comps.add(checkboxEntry(spritesOnly,
              Messages.getString("CommonSettingsDialog.spritesOnly.tooltip")));
        return createSettingsPanel(comps);
    }

    private List<Component> checkboxEntry(JCheckBox checkbox, String toolTip) {
        checkbox.setToolTipText(toolTip);
        checkbox.addItemListener(this);
        List<Component> row = new ArrayList<>();
        row.add(checkbox);
        return row;
    }

    private void addLineSpacer(List<List<Component>> comps) {
        List<Component> row = new ArrayList<>();
        row.add(Box.createRigidArea(new Dimension(0, 10)));
        comps.add(row);

        JSeparator Sep = new JSeparator(SwingConstants.HORIZONTAL);
        row = new ArrayList<>();
        row.add(Sep);
        comps.add(row);

        row = new ArrayList<>();
        row.add(Box.createRigidArea(new Dimension(0, 5)));
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
            nagForMechanicalJumpFallDamage.setSelected(GUIP.getNagForMechanicalJumpFallDamage());
            nagForCrushingBuildings.setSelected(GUIP.getNagForCrushingBuildings());
            nagForLaunchDoors.setSelected(GUIP.getNagForLaunchDoors());
            nagForSprint.setSelected(GUIP.getNagForSprint());
            nagForOddSizedBoard.setSelected(GUIP.getNagForOddSizedBoard());
            animateMove.setSelected(GUIP.getShowMoveStep());
            showWrecks.setSelected(GUIP.getShowWrecks());
            tooltipDelay.setText(Integer.toString(GUIP.getTooltipDelay()));
            tooltipDismissDelay.setText(Integer.toString(GUIP.getTooltipDismissDelay()));
            tooltipDistSuppression.setText(Integer.toString(GUIP.getTooltipDistSuppression()));
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

            maxPathfinderTime.setText(Integer.toString(CLIENT_PREFERENCES.getMaxPathfinderTime()));

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
            reportKeywordsTextPane.setText(CLIENT_PREFERENCES.getReportKeywords());
            reportFilterKeywordsTextPane.setText(CLIENT_PREFERENCES.getReportFilterKeywords());
            showIPAddressesInChat.setSelected(CLIENT_PREFERENCES.getShowIPAddressesInChat());
            startSearchlightsOn.setSelected(CLIENT_PREFERENCES.getStartSearchlightsOn());
            spritesOnly.setSelected(CLIENT_PREFERENCES.getSpritesOnly());

            defaultAutoEjectDisabled.setSelected(CLIENT_PREFERENCES.defaultAutoEjectDisabled());
            useAverageSkills.setSelected(CLIENT_PREFERENCES.useAverageSkills());
            useGPinUnitSelection.setSelected(CLIENT_PREFERENCES.useGPinUnitSelection());
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
            gifGameSummaryMM.setSelected(GUIP.getGifGameSummaryMinimap());
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
            fovHighlightRingsRadii.setText(GUIP.getFovHighlightRingsRadii());
            fovHighlightRingsColors.setText(GUIP.getFovHighlightRingsColorsHsb());
            fovOutsideEnabled.setSelected(GUIP.getFovDarken());
            fovDarkenAlpha.setValue(GUIP.getFovDarkenAlpha());
            numStripesSlider.setValue(GUIP.getFovStripes());
            fovGrayscaleEnabled.setSelected(GUIP.getFovGrayscale());

            fovHighlightAlpha.setEnabled(fovInsideEnabled.isSelected());
            fovHighlightRingsRadii.setEnabled(fovInsideEnabled.isSelected());
            fovHighlightRingsColors.setEnabled(fovInsideEnabled.isSelected());
            fovDarkenAlpha.setEnabled(fovOutsideEnabled.isSelected());
            numStripesSlider.setEnabled(fovOutsideEnabled.isSelected());
            fovGrayscaleEnabled.setEnabled(fovOutsideEnabled.isSelected());

            darkenAlphaLabel.setEnabled(fovOutsideEnabled.isSelected());
            numStripesLabel.setEnabled(fovOutsideEnabled.isSelected());
            fovHighlightRingsColorsLabel.setEnabled(fovInsideEnabled.isSelected());
            fovHighlightRingsRadiiLabel.setEnabled(fovInsideEnabled.isSelected());
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
        csbMapSheetColor.setColour(GUIP.getMapsheetColor());

        attackArrowTransparency.setValue(GUIP.getAttackArrowTransparency());
        ecmTransparency.setValue(GUIP.getECMTransparency());
        buttonsPerRow.setText(String.format("%d", GUIP.getButtonsPerRow()));
        playersRemainingToShow.setText(String.format("%d", GUIP.getPlayersRemainingToShow()));
        tmmPipModeCbo.setSelectedIndex(GUIP.getTMMPipMode());
        fontTypeChooserMoveFont.setSelectedItem(GUIP.getMoveFontType());
        moveFontSize.setText(String.format("%d", GUIP.getMoveFontSize()));
        fontStyleChooserMoveFont.setSelectedIndex(GUIP.getMoveFontStyle());
        darkenMapAtNight.setSelected(GUIP.getDarkenMapAtNight());
        translucentHiddenUnits.setSelected(GUIP.getTranslucentHiddenUnits());

        artilleryDisplayMisses.setSelected(GUIP.getShowArtilleryMisses());
        artilleryDisplayDriftedHits.setSelected(GUIP.getShowArtilleryDrifts());
        bombsDisplayMisses.setSelected(GUIP.getShowBombMisses());
        bombsDisplayDrifts.setSelected(GUIP.getShowBombDrifts());

        for (String option : savedAdvancedOpt.keySet()) {
            GUIP.setValue(option, savedAdvancedOpt.get(option));
        }

        unitDisplayNonTabbed.clear();
        unitDisplayNonTabbed.addElement(UNIT_DISPLAY_ORDER_PREFERENCES.getString(UnitDisplayPanel.NON_TABBED_A1));
        unitDisplayNonTabbed.addElement(UNIT_DISPLAY_ORDER_PREFERENCES.getString(UnitDisplayPanel.NON_TABBED_B1));
        unitDisplayNonTabbed.addElement(UNIT_DISPLAY_ORDER_PREFERENCES.getString(UnitDisplayPanel.NON_TABBED_C1));
        unitDisplayNonTabbed.addElement(UNIT_DISPLAY_ORDER_PREFERENCES.getString(UnitDisplayPanel.NON_TABBED_A2));
        unitDisplayNonTabbed.addElement(UNIT_DISPLAY_ORDER_PREFERENCES.getString(UnitDisplayPanel.NON_TABBED_B2));
        unitDisplayNonTabbed.addElement(UNIT_DISPLAY_ORDER_PREFERENCES.getString(UnitDisplayPanel.NON_TABBED_C2));

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

        unitDisplayHeatLevel1Text.setText(String.format("%d", GUIP.getUnitDisplayHeatValue1()));
        unitDisplayHeatLevel1Text.setText(String.format("%d", GUIP.getUnitDisplayHeatValue2()));
        unitDisplayHeatLevel1Text.setText(String.format("%d", GUIP.getUnitDisplayHeatValue3()));
        unitDisplayHeatLevel1Text.setText(String.format("%d", GUIP.getUnitDisplayHeatValue4()));
        unitDisplayHeatLevel1Text.setText(String.format("%d", GUIP.getUnitDisplayHeatValue5()));
        unitDisplayHeatLevel1Text.setText(String.format("%d", GUIP.getUnitDisplayHeatValue6()));

        unitTooltipSeenByCbo.setSelectedIndex(GUIP.getUnitToolTipSeenByResolution());
        unitDisplayWeaponListHeightText.setText(String.format("%d", GUIP.getUnitDisplayWeaponListHeight()));

        unitDisplayMekArmorLargeFontSizeText.setText(String.format("%d", GUIP.getUnitDisplayMekArmorLargeFontSize()));
        unitDisplayMekArmorMediumFontSizeText.setText(String.format("%d", GUIP.getUnitDisplayMekArmorMediumFontSize()));
        unitDisplayMekArmorSmallFontSizeText.setText(String.format("%d", GUIP.getUnitDisplayMekArmorSmallFontSize()));
        unitDisplayMekLargeFontSizeText.setText(String.format("%d", GUIP.getUnitDisplayMekLargeFontSize()));
        unitDisplayMekMediumFontSizeText.setText(String.format("%d", GUIP.getUnitDisplayMekMediumFontSize()));

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
        unitTooltipArmorMiniArmorCharText.setText(GUIP.getUnitToolTipArmorMiniArmorChar());
        unitTooltipArmorMiniInternalStructureCharText.setText(GUIP.getUnitToolTipArmorMiniISChar());
        unitTooltipArmorMiniCriticalCharText.setText(GUIP.getUnitToolTipArmorMiniCriticalChar());
        unitTooltipArmorMiniDestroyedCharText.setText(GUIP.getUnitToolTipArmorMiniDestroyedChar());
        unitTooltipArmorMiniCapArmorCharText.setText(GUIP.getUnitToolTipArmorMiniCapArmorChar());
        unitTooltipArmorMiniUnitsPerBlockText.setText(String.format("%d", GUIP.getUnitToolTipArmorMiniUnitsPerBlock()));
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
        GUIP.setMapSheetColor(csbMapSheetColor.getColour());

        GUIP.setAttackArrowTransparency((Integer) attackArrowTransparency.getValue());
        GUIP.setECMTransparency((Integer) ecmTransparency.getValue());
        GUIP.setDrawFacingArrowsOnMiniMap(drawFacingArrowsOnMiniMap.isSelected());
        GUIP.setDrawSensorRangeOnMiniMap(drawSensorRangeOnMiniMap.isSelected());
        GUIP.setPaintBorders(paintBordersOnMiniMap.isSelected());
        GUIP.setShowUnitDisplayNamesOnMinimap(showUnitDisplayNamesOnMinimap.isSelected());
        GUIP.setButtonsPerRow(MathUtility.parseInt(buttonsPerRow.getText(), 5));
        GUIP.setPlayersRemainingToShow(MathUtility.parseInt(playersRemainingToShow.getText(), 2));

        GUIP.setTMMPipMode(tmmPipModeCbo.getSelectedIndex());
        GUIP.setDarkenMapAtNight(darkenMapAtNight.isSelected());
        GUIP.setTranslucentHiddenUnits(translucentHiddenUnits.isSelected());

        GUIP.setShowArtilleryMisses(artilleryDisplayMisses.isSelected());
        GUIP.setShowArtilleryDrifts(artilleryDisplayDriftedHits.isSelected());
        GUIP.setShowBombMisses(bombsDisplayMisses.isSelected());
        GUIP.setShowBombDrifts(bombsDisplayDrifts.isSelected());

        Object selectedChooserMoveFond = fontTypeChooserMoveFont.getSelectedItem();
        if (selectedChooserMoveFond != null) {
            GUIP.setMoveFontType(fontTypeChooserMoveFont.getSelectedItem().toString());
            GUIP.setMoveFontSize(MathUtility.parseInt(moveFontSize.getText(), 12));
        }

        GUIP.setMoveFontStyle(fontStyleChooserMoveFont.getSelectedIndex());
        GUIP.setTooltipDelay(MathUtility.parseInt(tooltipDelay.getText(), 250));
        GUIP.setTooltipDismissDelay(MathUtility.parseInt(tooltipDismissDelay.getText(), 250));
        GUIP.setTooltipDistSuppression(MathUtility.parseInt(tooltipDistSuppression.getText(), 250));

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

        CLIENT_PREFERENCES.setMaxPathfinderTime(MathUtility.parseInt(maxPathfinderTime.getText(), 500));

        GUIP.setGetFocus(getFocus.isSelected());

        CLIENT_PREFERENCES.setKeepGameLog(keepGameLog.isSelected());
        CLIENT_PREFERENCES.setDataLogging(datasetLogging.isSelected());
        CLIENT_PREFERENCES.setGameLogFilename(gameLogFilename.getText());
        CLIENT_PREFERENCES.setAutoResolveGameLogFilename(autoResolveLogFilename.getText());
        CLIENT_PREFERENCES.setUserDir(userDir.getText());
        CLIENT_PREFERENCES.setMmlPath(mmlPath.getText());
        CLIENT_PREFERENCES.setStampFilenames(stampFilenames.isSelected());
        CLIENT_PREFERENCES.setStampFormat(stampFormat.getText());
        CLIENT_PREFERENCES.setReportKeywords(reportKeywordsTextPane.getText());
        CLIENT_PREFERENCES.setReportFilterKeywords(reportFilterKeywordsTextPane.getText());
        CLIENT_PREFERENCES.setShowIPAddressesInChat(showIPAddressesInChat.isSelected());
        CLIENT_PREFERENCES.setStartSearchlightsOn(startSearchlightsOn.isSelected());
        CLIENT_PREFERENCES.setSpritesOnly(spritesOnly.isSelected());
        CLIENT_PREFERENCES.setEnableExperimentalBotFeatures(enableExperimentalBotFeatures.isSelected());
        CLIENT_PREFERENCES.setDefaultAutoEjectDisabled(defaultAutoEjectDisabled.isSelected());
        CLIENT_PREFERENCES.setUseAverageSkills(useAverageSkills.isSelected());
        CLIENT_PREFERENCES.setUseGpInUnitSelection(useGPinUnitSelection.isSelected());
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
        GUIP.setGifGameSummaryMinimap(gifGameSummaryMM.isSelected());
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

        // unit display non tabbed
        if (!GUIP.getUnitDisplayStartTabbed()) {
            boolean unitDisplayNonTabbedChanged = false;
            int s = unitDisplayNonTabbed.getSize();

            if ((s > UnitDisplayPanel.NON_TABBED_ZERO_INDEX)
                  && (!unitDisplayNonTabbed.get(UnitDisplayPanel.NON_TABBED_ZERO_INDEX)
                  .equals(UNIT_DISPLAY_ORDER_PREFERENCES.getString(UnitDisplayPanel.NON_TABBED_A1)))) {
                unitDisplayNonTabbedChanged = true;
                UNIT_DISPLAY_ORDER_PREFERENCES.setValue(UnitDisplayPanel.NON_TABBED_A1,
                      unitDisplayNonTabbed.get(UnitDisplayPanel.NON_TABBED_ZERO_INDEX));
            }
            if ((s > UnitDisplayPanel.NON_TABBED_ONE_INDEX)
                  && (!unitDisplayNonTabbed.get(UnitDisplayPanel.NON_TABBED_ONE_INDEX)
                  .equals(UNIT_DISPLAY_ORDER_PREFERENCES.getString(UnitDisplayPanel.NON_TABBED_B1)))) {
                unitDisplayNonTabbedChanged = true;
                UNIT_DISPLAY_ORDER_PREFERENCES.setValue(UnitDisplayPanel.NON_TABBED_B1,
                      unitDisplayNonTabbed.get(UnitDisplayPanel.NON_TABBED_ONE_INDEX));
            }
            if ((s > UnitDisplayPanel.NON_TABBED_TWO_INDEX)
                  && (!unitDisplayNonTabbed.get(UnitDisplayPanel.NON_TABBED_TWO_INDEX)
                  .equals(UNIT_DISPLAY_ORDER_PREFERENCES.getString(UnitDisplayPanel.NON_TABBED_C1)))) {
                unitDisplayNonTabbedChanged = true;
                UNIT_DISPLAY_ORDER_PREFERENCES.setValue(UnitDisplayPanel.NON_TABBED_C1,
                      unitDisplayNonTabbed.get(UnitDisplayPanel.NON_TABBED_TWO_INDEX));
            }
            if ((s > UnitDisplayPanel.NON_TABBED_THREE_INDEX)
                  && (!unitDisplayNonTabbed.get(UnitDisplayPanel.NON_TABBED_THREE_INDEX)
                  .equals(UNIT_DISPLAY_ORDER_PREFERENCES.getString(UnitDisplayPanel.NON_TABBED_A2)))) {
                unitDisplayNonTabbedChanged = true;
                UNIT_DISPLAY_ORDER_PREFERENCES.setValue(UnitDisplayPanel.NON_TABBED_A2,
                      unitDisplayNonTabbed.get(UnitDisplayPanel.NON_TABBED_THREE_INDEX));
            }
            if ((s > UnitDisplayPanel.NON_TABBED_FOUR_INDEX)
                  && (!unitDisplayNonTabbed.get(UnitDisplayPanel.NON_TABBED_FOUR_INDEX)
                  .equals(UNIT_DISPLAY_ORDER_PREFERENCES.getString(UnitDisplayPanel.NON_TABBED_B2)))) {
                unitDisplayNonTabbedChanged = true;
                UNIT_DISPLAY_ORDER_PREFERENCES.setValue(UnitDisplayPanel.NON_TABBED_B2,
                      unitDisplayNonTabbed.get(UnitDisplayPanel.NON_TABBED_FOUR_INDEX));
            }
            if ((s > UnitDisplayPanel.NON_TABBED_FIVE_INDEX)
                  && (!unitDisplayNonTabbed.get(UnitDisplayPanel.NON_TABBED_FIVE_INDEX)
                  .equals(UNIT_DISPLAY_ORDER_PREFERENCES.getString(UnitDisplayPanel.NON_TABBED_C2)))) {
                unitDisplayNonTabbedChanged = true;
                UNIT_DISPLAY_ORDER_PREFERENCES.setValue(UnitDisplayPanel.NON_TABBED_C2,
                      unitDisplayNonTabbed.get(UnitDisplayPanel.NON_TABBED_FIVE_INDEX));
            }

            if ((unitDisplayNonTabbedChanged) && (clientgui != null)) {
                clientgui.getUnitDisplay().setDisplayNonTabbed();
            }
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
        GUIP.setUnitDisplayHeatColorValue1(MathUtility.parseInt(unitDisplayHeatLevel1Text.getText()));
        GUIP.setUnitDisplayHeatColorValue2(MathUtility.parseInt(unitDisplayHeatLevel2Text.getText()));
        GUIP.setUnitDisplayHeatColorValue3(MathUtility.parseInt(unitDisplayHeatLevel3Text.getText()));
        GUIP.setUnitDisplayHeatColorValue4(MathUtility.parseInt(unitDisplayHeatLevel4Text.getText()));
        GUIP.setUnitDisplayHeatColorValue5(MathUtility.parseInt(unitDisplayHeatLevel5Text.getText()));
        GUIP.setUnitDisplayHeatColorValue6(MathUtility.parseInt(unitDisplayHeatLevel6Text.getText()));

        GUIP.setUnitToolTipSeenByResolution(unitTooltipSeenByCbo.getSelectedIndex());
        GUIP.setUnitDisplayWeaponListHeight(MathUtility.parseInt(unitDisplayWeaponListHeightText.getText()));

        GUIP.setUnitDisplayMekArmorLargeFontSize(MathUtility.parseInt(unitDisplayMekArmorLargeFontSizeText.getText()));
        GUIP.setUnitDisplayMekArmorMediumFontSize(MathUtility.parseInt(unitDisplayMekArmorMediumFontSizeText.getText()));
        GUIP.setUnitDisplayMekArmorSmallFontSize(MathUtility.parseInt(unitDisplayMekArmorSmallFontSizeText.getText()));
        GUIP.setUnitDisplayMekLargeFontSize(MathUtility.parseInt(unitDisplayMekLargeFontSizeText.getText()));
        GUIP.setUnitDisplayMekMediumFontSize(MathUtility.parseInt(unitDisplayMekMediumFontSizeText.getText()));

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
        GUIP.setUnitToolTipArmorMiniArmorChar(unitTooltipArmorMiniArmorCharText.getText());
        GUIP.setUnitToolTipArmorMiniISChar(unitTooltipArmorMiniInternalStructureCharText.getText());
        GUIP.setUnitToolTipArmorMiniCriticalChar(unitTooltipArmorMiniCriticalCharText.getText());
        GUIP.setUnitTooltipArmorMiniDestroyedChar(unitTooltipArmorMiniDestroyedCharText.getText());
        GUIP.setUnitTooltipArmorMiniCapArmorChar(unitTooltipArmorMiniCapArmorCharText.getText());

        GUIP.setUnitTooltipArmorMiniUnitsPerBlock
              (MathUtility.parseInt(unitTooltipArmorMiniUnitsPerBlockText.getText()));
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
        } else if (source.equals(fovInsideEnabled)) {
            GUIP.setFovHighlight(fovInsideEnabled.isSelected());
            fovHighlightAlpha.setEnabled(fovInsideEnabled.isSelected());
            fovHighlightRingsRadii.setEnabled(fovInsideEnabled.isSelected());
            fovHighlightRingsColors.setEnabled(fovInsideEnabled.isSelected());
            fovHighlightRingsColorsLabel.setEnabled(fovInsideEnabled.isSelected());
            fovHighlightRingsRadiiLabel.setEnabled(fovInsideEnabled.isSelected());
            highlightAlphaLabel.setEnabled(fovInsideEnabled.isSelected());
        } else if (source.equals(fovOutsideEnabled)) {
            GUIP.setFovDarken(fovOutsideEnabled.isSelected());
            fovDarkenAlpha.setEnabled(fovOutsideEnabled.isSelected());
            numStripesSlider.setEnabled(fovOutsideEnabled.isSelected());
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
        Object src = e.getSource();
        GUIPreferences guip = GUIP;
        if (src.equals(fovHighlightRingsRadii)) {
            guip.setFovHighlightRingsRadii(fovHighlightRingsRadii.getText());
            return;
        } else if (src.equals(fovHighlightRingsColors)) {
            guip.setFovHighlightRingsColorsHsb(fovHighlightRingsColors.getText());
            return;
        }
        // For Advanced options
        String option = "Advanced" + advancedKeys.getModel().getElementAt(advancedKeyIndex).option;
        savedAdvancedOpt.put(option, guip.getString(option));
        guip.setValue(option, advancedValue.getText());
    }

    /**
     * Creates a panel with a box for all the commands that can be bound to keys.
     */
    private JPanel getKeyBindPanel() {
        JPanel outer = new JPanel();
        outer.setLayout(new BoxLayout(outer, BoxLayout.PAGE_AXIS));

        var topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        defaultKeyBindButton.addActionListener(e -> updateKeybindsDefault());
        defaultKeyBindButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        String msg_esc = Messages.getString("CommonSettingsDialog.keyBinds.escMessage");
        var escInfoLabel = new JLabel("<HTML><CENTER>" + msg_esc + "</HTML>");
        escInfoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        escInfoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        topPanel.add(defaultKeyBindButton);
        topPanel.add(escInfoLabel);
        topPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        var tabChoice = new JPanel();
        tabChoice.setLayout(new BoxLayout(tabChoice, BoxLayout.Y_AXIS));
        tabChoice.setBorder(new EmptyBorder(15, 15, 15, 15));
        choiceToggle.addActionListener(e -> updateKeybindsFocusTraversal());
        choiceToggle.setMaximumSize(new Dimension(600, 40));
        choiceToggle.setAlignmentX(Component.CENTER_ALIGNMENT);
        String msg_line1 = Messages.getString("CommonSettingsDialog.keyBinds.tabMessageLine1");
        String msg_line2 = Messages.getString("CommonSettingsDialog.keyBinds.tabMessageLine2");
        String msg_line3 = Messages.getString("CommonSettingsDialog.keyBinds.tabMessageLine3");
        var tabChoiceLabel = new JLabel("<HTML><CENTER>"
              + msg_line1
              + "<BR>"
              + msg_line2
              + "<BR>"
              + msg_line3
              + "</HTML>");
        tabChoiceLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        tabChoiceLabel.setHorizontalAlignment(SwingConstants.CENTER);

        tabChoice.add(choiceToggle);
        tabChoice.add(tabChoiceLabel);
        tabChoice.setAlignmentX(Component.CENTER_ALIGNMENT);

        outer.add(topPanel);
        outer.add(tabChoice);

        JPanel keyBinds = new JPanel(new GridBagLayout());
        outer.add(keyBinds);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = gbc.gridy = 0;
        gbc.insets = new Insets(0, 10, 5, 10);

        // Create header: labels for describing what each column does
        JLabel headers = new JLabel("Name");
        String msg_tooltipName = Messages.getString("CommonSettingsDialog.keyBinds.tooltipName");
        headers.setToolTipText(msg_tooltipName);
        keyBinds.add(headers, gbc);
        gbc.gridx++;
        headers = new JLabel("Modifier");
        String msg_tooltipModifier = Messages.getString("CommonSettingsDialog.keyBinds.tooltipModifier");
        headers.setToolTipText(msg_tooltipModifier);
        keyBinds.add(headers, gbc);
        gbc.gridx++;
        headers = new JLabel("Key");
        String msg_tooltipKey = Messages.getString("CommonSettingsDialog.keyBinds.tooltipKey");
        headers.setToolTipText(msg_tooltipKey);
        keyBinds.add(headers, gbc);
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 4;

        gbc.fill = GridBagConstraints.BOTH;
        keyBinds.add(new JSeparator(SwingConstants.HORIZONTAL), gbc);

        gbc.fill = GridBagConstraints.NONE;
        gbc.gridy++;
        gbc.gridwidth = 1;

        // Create maps to retrieve the text fields for saving
        int numBinds = KeyCommandBind.values().length;
        cmdModifierMap = new HashMap<>((int) (numBinds * 1.26));
        cmdKeyMap = new HashMap<>((int) (numBinds * 1.26));
        cmdKeyCodeMap = new HashMap<>((int) (numBinds * 1.26));

        // For each keyCommandBind, create a label and two text fields
        for (KeyCommandBind kcb : KeyCommandBind.values()) {
            JLabel name = new JLabel(Messages.getString("KeyBinds.cmdNames." + kcb.cmd));
            name.setToolTipText(Messages.getString("KeyBinds.cmdDesc." + kcb.cmd));
            gbc.anchor = GridBagConstraints.EAST;
            keyBinds.add(name, gbc);
            gbc.gridx++;
            gbc.anchor = GridBagConstraints.CENTER;

            final JTextField modifiers = new JTextField(10);
            modifiers.setText(KeyEvent.getModifiersExText(kcb.modifiers));
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

            keyBinds.add(modifiers, gbc);
            gbc.gridx++;
            cmdModifierMap.put(kcb.cmd, modifiers);
            final JTextField key = new JTextField(10);
            key.setName(kcb.cmd);
            if (kcb.key == 0) {
                key.setText("");
            } else {
                key.setText(KeyEvent.getKeyText(kcb.key));
            }

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

            keyBinds.add(key, gbc);
            gbc.gridx = 0;
            gbc.gridy++;

            // deactivate TAB-bing through fields here so TAB can be caught as a keybind
            modifiers.setFocusTraversalKeysEnabled(false);
            key.setFocusTraversalKeysEnabled(false);
        }
        markDuplicateBinds();
        return outer;
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
            cmdModifierMap.get(kcb.cmd).setFocusTraversalKeysEnabled(choiceToggle.isSelected());
            cmdKeyMap.get(kcb.cmd).setFocusTraversalKeysEnabled(choiceToggle.isSelected());
            if ((keyCode(kcb) == KeyEvent.VK_TAB) && choiceToggle.isEnabled()) {
                cmdKeyMap.get(kcb.cmd).setText("");
            }
        }
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
    private JPanel getButtonOrderPanel() {
        JPanel buttonOrderPanel = new JPanel();
        buttonOrderPanel.setLayout(new BoxLayout(buttonOrderPanel, BoxLayout.Y_AXIS));
        JTabbedPane phasePane = new JTabbedPane();
        buttonOrderPanel.add(phasePane);

        // MovementPhaseDisplay
        movePhaseCommands = new DefaultListModel<>();
        phasePane.add("Movement", getButtonOrderPane(movePhaseCommands, MoveCommand.values()));

        // DeploymentPhaseDisplay
        deployPhaseCommands = new DefaultListModel<>();
        phasePane.add("Deployment", getButtonOrderPane(deployPhaseCommands, DeploymentDisplay.DeployCommand.values()));

        // FiringPhaseDisplay
        firingPhaseCommands = new DefaultListModel<>();
        phasePane.add("Firing", getButtonOrderPane(firingPhaseCommands, FiringDisplay.FiringCommand.values()));

        // PhysicalPhaseDisplay
        physicalPhaseCommands = new DefaultListModel<>();
        phasePane.add("Physical", getButtonOrderPane(physicalPhaseCommands, PhysicalDisplay.PhysicalCommand.values()));

        // TargetingPhaseDisplay
        targetingPhaseCommands = new DefaultListModel<>();
        phasePane.add("Targeting",
              getButtonOrderPane(targetingPhaseCommands, TargetingPhaseDisplay.TargetingCommand.values()));

        return buttonOrderPanel;
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

    private JPanel createSettingsPanel(List<List<Component>> comps) {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        Box innerPanel = new Box(BoxLayout.PAGE_AXIS);
        for (List<Component> cs : comps) {
            Box subPanel = new Box(BoxLayout.LINE_AXIS);
            for (Component c : cs) {
                if (c instanceof JLabel) {
                    subPanel.add(Box.createRigidArea(LABEL_SPACER));
                    subPanel.add(c);
                    subPanel.add(Box.createRigidArea(LABEL_SPACER));
                } else {
                    subPanel.add(c);
                }
            }
            subPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            innerPanel.add(subPanel);
        }
        innerPanel.add(Box.createVerticalGlue());
        innerPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panel.add(innerPanel, BorderLayout.PAGE_START);
        return panel;
    }

    private JPanel getAdvancedSettingsPanel() {
        JPanel p = new JPanel();

        String[] s = GUIP.getAdvancedProperties();
        AdvancedOptionData[] opts = new AdvancedOptionData[s.length];
        for (int i = 0;
              i < s.length;
              i++) {
            s[i] = s[i].substring(s[i].indexOf("Advanced") + 8);
            opts[i] = new AdvancedOptionData(s[i]);
        }
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
            GUIP.setFovHighlightAlpha(Math.max(0, Math.min(255, fovHighlightAlpha.getValue())));
        } else if (evt.getSource().equals(fovDarkenAlpha)) {
            GUIP.setFovDarkenAlpha(Math.max(0, Math.min(255, fovDarkenAlpha.getValue())));
        } else if (evt.getSource().equals(numStripesSlider)) {
            GUIP.setFovStripes(numStripesSlider.getValue());
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
        try (Stream<Path> entries = Files.walk(path.toPath())) {
            return entries.map(Objects::toString).filter(name -> name.endsWith(fileEnding)).collect(toList());
        } catch (IOException e) {
            logger.warn("Error while reading {} files from {}", fileEnding, path);
            return new ArrayList<>();
        }
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

    private static void fileChoose(JTextField textField, JFrame parent, String title, boolean directories) {
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
