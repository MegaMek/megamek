/*
 * MegaMek
 * Copyright (c) 2003-2005 - Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2021-2023 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client.ui.swing;

import megamek.MMConstants;
import megamek.client.ui.Messages;
import megamek.client.ui.baseComponents.AbstractButtonDialog;
import megamek.client.ui.baseComponents.MMButton;
import megamek.client.ui.baseComponents.MMComboBox;
import megamek.client.ui.swing.StatusBarPhaseDisplay.PhaseCommand;
import megamek.client.ui.swing.unitDisplay.UnitDisplay;
import megamek.client.ui.swing.util.KeyCommandBind;
import megamek.client.ui.swing.util.UIUtil;
import megamek.client.ui.swing.widget.SkinXMLHandler;
import megamek.common.Configuration;
import megamek.common.KeyBindParser;
import megamek.common.enums.GamePhase;
import megamek.common.enums.WeaponSortOrder;
import megamek.common.preference.ClientPreferences;
import megamek.common.preference.PreferenceManager;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.border.EmptyBorder;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.List;
import java.util.*;

/** The Client Settings Dialog offering GUI options concerning tooltips, map display, keybinds etc. */
public class CommonSettingsDialog extends AbstractButtonDialog implements ItemListener,
        FocusListener, ListSelectionListener, ChangeListener {

    /**
     * A class for storing information about an GUIPreferences advanced option.
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
            if (mouseDragging && (src instanceof JList)) {
                JList<?> srcList = (JList<?>) src;
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
    private final JCheckBox autoDeclareSearchlight = new JCheckBox(Messages.getString("CommonSettingsDialog.autoDeclareSearchlight"));
    private final JCheckBox nagForMASC = new JCheckBox(Messages.getString("CommonSettingsDialog.nagForMASC"));
    private final JCheckBox nagForPSR = new JCheckBox(Messages.getString("CommonSettingsDialog.nagForPSR"));
    private final JCheckBox nagForWiGELanding = new JCheckBox(Messages.getString("CommonSettingsDialog.nagForWiGELanding"));
    private final JCheckBox nagForNoAction = new JCheckBox(Messages.getString("CommonSettingsDialog.nagForNoAction"));
    private final JCheckBox nagForNoUnJamRAC = new JCheckBox(Messages.getString("CommonSettingsDialog.nagForUnJamRAC"));
    private final JCheckBox animateMove = new JCheckBox(Messages.getString("CommonSettingsDialog.animateMove"));
    private final JCheckBox showWrecks = new JCheckBox(Messages.getString("CommonSettingsDialog.showWrecks"));
    private final JCheckBox soundMuteChat = new JCheckBox(Messages.getString("CommonSettingsDialog.soundMuteChat"));
    private JTextField tfSoundMuteChatFileName;
    private final JCheckBox soundMuteMyTurn = new JCheckBox(Messages.getString("CommonSettingsDialog.soundMuteMyTurn"));
    private JTextField tfSoundMuteMyTurntFileName;
    private final JCheckBox soundMuteOthersTurn = new JCheckBox(Messages.getString("CommonSettingsDialog.soundMuteOthersTurn"));
    private JTextField tfSoundMuteOthersFileName;
    private final JCheckBox showWpsinTT = new JCheckBox(Messages.getString("CommonSettingsDialog.showWpsinTT"));
    private final JCheckBox showArmorMiniVisTT = new JCheckBox(Messages.getString("CommonSettingsDialog.showArmorMiniVisTT"));
    private final JCheckBox showPilotPortraitTT = new JCheckBox(Messages.getString("CommonSettingsDialog.showPilotPortraitTT"));
    private MMComboBox<WeaponSortOrder> comboDefaultWeaponSortOrder;
    private JTextField tooltipDelay;
    private JTextField tooltipDismissDelay;
    private JTextField tooltipDistSupression;
    private JComboBox<String> unitStartChar;
    private JTextField maxPathfinderTime;
    private final JCheckBox getFocus = new JCheckBox(Messages.getString("CommonSettingsDialog.getFocus"));
    private JSlider guiScale;

    private final JCheckBox keepGameLog = new JCheckBox(Messages.getString("CommonSettingsDialog.keepGameLog"));
    private JTextField gameLogFilename;
    // private JTextField gameLogMaxSize;
    private final JCheckBox stampFilenames = new JCheckBox(Messages.getString("CommonSettingsDialog.stampFilenames"));
    private JTextField stampFormat;
    private final JCheckBox defaultAutoejectDisabled = new JCheckBox(Messages.getString("CommonSettingsDialog.defaultAutoejectDisabled"));
    private final JCheckBox useAverageSkills = new JCheckBox(Messages.getString("CommonSettingsDialog.useAverageSkills"));
    private final JCheckBox generateNames = new JCheckBox(Messages.getString("CommonSettingsDialog.generateNames"));
    private final JCheckBox showUnitId = new JCheckBox(Messages.getString("CommonSettingsDialog.showUnitId"));
    private JComboBox<String> displayLocale;
    private final JCheckBox showIPAddressesInChat = new JCheckBox(Messages.getString("CommonSettingsDialog.showIPAddressesInChat"));
    private final JCheckBox showDamageLevel = new JCheckBox(Messages.getString("CommonSettingsDialog.showDamageLevel"));
    private final JCheckBox showDamageDecal = new JCheckBox(Messages.getString("CommonSettingsDialog.showDamageDecal"));
    private final JCheckBox showMapsheets = new JCheckBox(Messages.getString("CommonSettingsDialog.showMapsheets"));
    private final JCheckBox aOHexShadows = new JCheckBox(Messages.getString("CommonSettingsDialog.aOHexSHadows"));
    private final JCheckBox floatingIso = new JCheckBox(Messages.getString("CommonSettingsDialog.floatingIso"));
    private final JCheckBox mmSymbol = new JCheckBox(Messages.getString("CommonSettingsDialog.mmSymbol"));
    private final JCheckBox entityOwnerColor = new JCheckBox(Messages.getString("CommonSettingsDialog.entityOwnerColor"));
    private final JCheckBox teamColoring = new JCheckBox(Messages.getString("CommonSettingsDialog.teamColoring"));
    private final JCheckBox dockOnLeft = new JCheckBox(Messages.getString("CommonSettingsDialog.dockOnLeft"));
    private final JCheckBox dockMultipleOnYAxis = new JCheckBox(Messages.getString("CommonSettingsDialog.dockMultipleOnYAxis"));
    private final JCheckBox useCamoOverlay = new JCheckBox(Messages.getString("CommonSettingsDialog.useCamoOverlay"));
    private final JCheckBox useSoftCenter = new JCheckBox(Messages.getString("CommonSettingsDialog.useSoftCenter"));
    private final JCheckBox levelhighlight = new JCheckBox(Messages.getString("CommonSettingsDialog.levelHighlight"));
    private final JCheckBox shadowMap = new JCheckBox(Messages.getString("CommonSettingsDialog.useShadowMap"));
    private final JCheckBox hexInclines = new JCheckBox(Messages.getString("CommonSettingsDialog.useInclines"));
    private final JCheckBox mouseWheelZoom = new JCheckBox(Messages.getString("CommonSettingsDialog.mouseWheelZoom"));
    private final JCheckBox mouseWheelZoomFlip = new JCheckBox(Messages.getString("CommonSettingsDialog.mouseWheelZoomFlip"));


    private final JCheckBox moveDefaultClimbMode = new JCheckBox(Messages.getString("CommonSettingsDialog.moveDefaultClimbMode"));
    private ColourSelectorButton csbMoveDefaultColor;
    private ColourSelectorButton csbMoveIllegalColor;
    private ColourSelectorButton csbMoveJumpColor;
    private ColourSelectorButton csbMoveMASCColor;
    private ColourSelectorButton csbMoveRunColor;
    private ColourSelectorButton csbMoveBackColor;
    private ColourSelectorButton csbMoveSprintColor;
    private JTextField moveFontType;
    private JTextField moveFontSize;
    private JTextField moveFontStyle;

    private ColourSelectorButton csbFireSolnCanSeeColor;
    private ColourSelectorButton csbFireSolnNoSeeColor;
    private ColourSelectorButton csbBuildingTextColor;
    private ColourSelectorButton csbLowFoliageColor;
    private ColourSelectorButton csbBoardTextColor;
    private ColourSelectorButton csbBoardSpaceTextColor;
    private ColourSelectorButton csbMapsheetColor;
    private JTextField attackArrowTransparency;;
    private JTextField ecmTransparency;
    private JTextField buttonsPerRow;
    private JTextField playersRemainingToShow;

    private JComboBox<String> tmmPipModeCbo;
    private final JCheckBox darkenMapAtNight = new JCheckBox(Messages.getString("CommonSettingsDialog.darkenMapAtNight"));
    private final JCheckBox translucentHiddenUnits = new JCheckBox(Messages.getString("CommonSettingsDialog.translucentHiddenUnits"));

    // Tactical Overlay Options
    private final JCheckBox fovInsideEnabled = new JCheckBox(Messages.getString("TacticalOverlaySettingsDialog.FovInsideEnabled"));
    private JSlider fovHighlightAlpha;
    private final JCheckBox fovOutsideEnabled = new JCheckBox(Messages.getString("TacticalOverlaySettingsDialog.FovOutsideEnabled"));
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

    private final JCheckBox gameSummaryBV = new JCheckBox(Messages.getString("CommonSettingsDialog.gameSummaryBV.name"));
    private final JCheckBox gameSummaryMM = new JCheckBox(Messages.getString("CommonSettingsDialog.gameSummaryMM.name"));

    private JComboBox<String> skinFiles;
    private JComboBox<UITheme> uiThemes;

    // Avanced Settings
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
    private DefaultListModel<String> unitDisplayNonTabbed = new DefaultListModel<>();
    private final StatusBarPhaseDisplay.CommandComparator cmdComp = new StatusBarPhaseDisplay.CommandComparator();
    private final PhaseCommandListMouseAdapter cmdMouseAdaptor = new PhaseCommandListMouseAdapter();
    
    private JComboBox<String> tileSetChoice;
    private List<String> tileSets;
    private final MMToggleButton choiceToggle = new MMToggleButton(Messages.getString("CommonSettingsDialog.keyBinds.buttoneTabbing"));
    private final MMButton defaultKeyBindButton = new MMButton("default", Messages.getString("CommonSettingsDialog.keyBinds.buttonDefault"));
    private ColourSelectorButton csbWarningColor;

    private JComboBox unitDisplayAutoDisplayReportCombo;
    private JComboBox unitDisplayAutoDisplayNonReportCombo;
    private JComboBox miniMapAutoDisplayReportCombo;
    private JComboBox miniMapAutoDisplayNonReportCombo;
    private JComboBox miniReportAutoDisplayReportCombo;
    private JComboBox miniReportAutoDisplayNonReportCombo;
    private JComboBox playerListAutoDisplayReportCombo;
    private JComboBox playerListAutoDisplayNonReportCombo;

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
    private JComboBox<String> unitTooltipSeenbyCbo;
    private JTextField unitDisplayWeaponListHeightText;

    private ColourSelectorButton csbUnitTooltipArmorMiniIntact;
    private ColourSelectorButton csbUnitTooltipArmorMiniPartial;
    private ColourSelectorButton csbUnitTooltipArmorMiniDamaged;
    private JTextField unitTooltipArmorMiniArmorCharText;
    private JTextField unitTooltipArmorMiniInternalStructureCharText;
    private JTextField unitTooltipArmorMiniCriticalCharText;
    private JTextField unitTooltipArmorMiniDestroyedCharText;
    private JTextField unitTooltipArmorMiniCapArmorCharText;
    private JTextField unitTooltipArmorMiniFontSizeModText;
    private JTextField unitTooltipArmorMiniUnitsPerBlockText;
    private JTextField unitDisplayMechArmorLargeFontSizeText;
    private JTextField unitDisplayMechArmorMediumFontSizeText;
    private JTextField unitDisplayMechArmorSmallFontSizeText;
    private JTextField unitDisplayMechLargeFontSizeText;
    private JTextField unitDisplayMechMeduimFontSizeText;

    // Report
    private JTextPane reportKeywordsTextPane;
    private ColourSelectorButton csbReportLinkColor;
    private final JCheckBox showReportSprites = new JCheckBox(Messages.getString("CommonSettingsDialog.showReportSprites"));

    private ColourSelectorButton csbUnitOverviewValidColor;
    private ColourSelectorButton csbUnitOverviewSelectedColor;

    private ColourSelectorButton csbPlanetaryConditionsColorTitle;
    private ColourSelectorButton csbPlanetaryConditionsColorText;
    private ColourSelectorButton csbPlanetaryConditionsColorCold;
    private ColourSelectorButton csbPlanetaryConditionsColorHot;
    private ColourSelectorButton csbPlanetaryConditionsColorBackground;
    private final JCheckBox planetaryConditionsShowDefaults = new JCheckBox(Messages.getString("CommonSettingsDialog.planetaryConditionsShowDefaults"));
    private final JCheckBox planetaryConditionsShowHeader = new JCheckBox(Messages.getString("CommonSettingsDialog.planetaryConditionsShowHeader"));
    private final JCheckBox planetaryConditionsShowLabels = new JCheckBox(Messages.getString("CommonSettingsDialog.planetaryConditionsShowLabels"));
    private final JCheckBox planetaryConditionsShowValues = new JCheckBox(Messages.getString("CommonSettingsDialog.planetaryConditionsShowValues"));
    private final JCheckBox planetaryConditionsShowIndicators = new JCheckBox(Messages.getString("CommonSettingsDialog.planetaryConditionsShowIndicators"));

    /** Maps command strings to a JTextField for updating the modifier for the command. */
    private Map<String, JTextField> cmdModifierMap;

    /** Maps command strings to a JTextField for updating the key for the command. */
    private Map<String, JTextField> cmdKeyMap;
    
    /** Maps command strings to a Integer for updating the key for the command. */
    private Map<String, Integer> cmdKeyCodeMap; 

    private ClientGUI clientgui = null;

    private static final GUIPreferences GUIP = GUIPreferences.getInstance();
    private static final ClientPreferences CP = PreferenceManager.getClientPreferences();
    private static final UnitDisplayOrderPreferences UDOP = UnitDisplayOrderPreferences.getInstance();
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
    private boolean savedLevelhighlight;
    private boolean savedFloatingIso;
    private boolean savedMmSymbol;
    private boolean savedTeamColoring;
    private boolean savedDockOnLeft;
    private boolean savedDockMultipleOnYAxis;
    private boolean savedUseCamoOverlay;
    private boolean savedUnitLabelBorder;
    private boolean savedShowDamageDecal;
    private boolean savedShowDamageLabel;
    private String savedFovHighlightRingsRadii;
    private String savedFovHighlightRingsColors;
    private int savedFovHighlightAlpha;
    private int savedFovDarkenAlpha;
    private int savedNumStripesSlider;

    HashMap<String, String> savedAdvancedOpt = new HashMap<>();

    /** Constructs the Client Settings Dialog with a clientgui (used within the client, i.e. in lobby and game). */
    public CommonSettingsDialog(JFrame owner, ClientGUI cg) {
        this(owner);
        clientgui = cg;
    }
    
    /** Constructs the Client Settings Dialog without a clientgui (used in the main menu and board editor). */
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

        panTabs.add(Messages.getString("CommonSettingsDialog.main"), settingsPane);
        panTabs.add(Messages.getString("CommonSettingsDialog.keyBinds"), keyBindPane);
        panTabs.add(Messages.getString("CommonSettingsDialog.gameBoard"), gameBoardPane);
        panTabs.add(Messages.getString("CommonSettingsDialog.unitDisplay"), unitDisplayPane);
        panTabs.add(Messages.getString("CommonSettingsDialog.miniMap"), miniMapPane);
        panTabs.add(Messages.getString("CommonSettingsDialog.report"), reportPane);
        panTabs.add(Messages.getString("CommonSettingsDialog.overlays"), overlaysPane);
        panTabs.add(Messages.getString("CommonSettingsDialog.buttonOrder"), getButtonOrderPanel());
        panTabs.add(Messages.getString("CommonSettingsDialog.autoDisplay"), autoDisplayPane);
        panTabs.add(Messages.getString("CommonSettingsDialog.advanced"), advancedSettingsPane);

        adaptToGUIScale();

        return panTabs;
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

        comps.add(checkboxEntry(nagForMASC, null));
        comps.add(checkboxEntry(nagForPSR, null));
        comps.add(checkboxEntry(nagForWiGELanding, null));
        comps.add(checkboxEntry(nagForNoAction, null));
        comps.add(checkboxEntry(nagForNoUnJamRAC, null));
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
        JLabel playersRemainingToShowLabel = new JLabel(Messages.getString("CommonSettingsDialog.playersRemainingToShow"));
        row = new ArrayList<>();
        row.add(playersRemainingToShowLabel);
        row.add(playersRemainingToShow);
        playersRemainingToShow.setText(String.format("%d", GUIP.getPlayersRemainingToShow()));
        playersRemainingToShow.setToolTipText(Messages.getString("CommonSettingsDialog.playersRemainingToShow.tooltip"));
        comps.add(row);

        comps.add(checkboxEntry(mouseWheelZoom, null));
        comps.add(checkboxEntry(mouseWheelZoomFlip, null));
        String msg_tooltip = Messages.getString("CommonSettingsDialog.gameSummaryBV.tooltip", Configuration.gameSummaryImagesBVDir());
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

        addLineSpacer(comps);

        comps.add(checkboxEntry(animateMove, null));
        comps.add(checkboxEntry(showWrecks, null));

        showMapsheets.addItemListener(this);
        row = new ArrayList<>();
        row.add(showMapsheets);
        csbMapsheetColor = new ColourSelectorButton(Messages.getString("CommonSettingsDialog.colors.MapSheetColor"));
        csbMapsheetColor.setColour(GUIP.getMapsheetColor());
        row.add(csbMapsheetColor);
        comps.add(row);

        comps.add(checkboxEntry(aOHexShadows, null));
        comps.add(checkboxEntry(shadowMap, null));
        comps.add(checkboxEntry(hexInclines, null));
        comps.add(checkboxEntry(levelhighlight, null));
        comps.add(checkboxEntry(floatingIso, null));
        comps.add(checkboxEntry(darkenMapAtNight, null));
        darkenMapAtNight.setSelected(GUIP.getDarkenMapAtNight());
        comps.add(checkboxEntry(translucentHiddenUnits, null));
        translucentHiddenUnits.setSelected(GUIP.getTranslucentHiddenUnits());

        addLineSpacer(comps);

        attackArrowTransparency = new JTextField(4);
        attackArrowTransparency.setMaximumSize(new Dimension(150, 40));
        JLabel attackArrowTransparencyLabel = new JLabel(Messages.getString("CommonSettingsDialog.attackArrowTransparency"));
        row = new ArrayList<>();
        row.add(attackArrowTransparencyLabel);
        row.add(attackArrowTransparency);
        attackArrowTransparency.setText(String.format("%d", GUIP.getAttachArrowTransparency()));
        attackArrowTransparency.setToolTipText(Messages.getString("CommonSettingsDialog.attackArrowTransparency.tooltip"));
        comps.add(row);

        ecmTransparency = new JTextField(4);
        ecmTransparency.setMaximumSize(new Dimension(150, 40));
        JLabel ecmTransparencyLabel = new JLabel(Messages.getString("CommonSettingsDialog.ecmTransparency"));
        row = new ArrayList<>();
        row.add(ecmTransparencyLabel);
        row.add(ecmTransparency);
        ecmTransparency.setText(String.format("%d", GUIP.getECMTransparency()));
        ecmTransparency.setToolTipText(Messages.getString("CommonSettingsDialog.ecmTransparency.tooltip"));
        comps.add(row);

        tmmPipModeCbo = new JComboBox<>();
        tmmPipModeCbo.addItem(Messages.getString("CommonSettingsDialog.tmmPipMode.NoPips"));
        tmmPipModeCbo.addItem(Messages.getString("CommonSettingsDialog.tmmPipMode.WhitePips"));
        tmmPipModeCbo.addItem(Messages.getString("CommonSettingsDialog.tmmPipMode.ColoredPips"));
        tmmPipModeCbo.setSelectedIndex(GUIP.getTMMPipMode());
        JLabel tmmPipModeLabel = new JLabel(Messages.getString("CommonSettingsDialog.tmmPipMode"));
        row = new ArrayList<>();
        row.add(tmmPipModeLabel);
        row.add(tmmPipModeCbo);
        comps.add(row);

        addLineSpacer(comps);

        moveFontType = new JTextField(4);
        moveFontType.setMaximumSize(new Dimension(150, 40));
        JLabel moveFontTypeLabel = new JLabel(Messages.getString("CommonSettingsDialog.moveFontType"));
        row = new ArrayList<>();
        row.add(moveFontTypeLabel);
        row.add(moveFontType);
        moveFontType.setText(GUIP.getMoveFontType());
        comps.add(row);

        moveFontSize = new JTextField(4);
        moveFontSize.setMaximumSize(new Dimension(150, 40));
        JLabel moveFontSizeLabel = new JLabel(Messages.getString("CommonSettingsDialog.moveFontSize"));
        row = new ArrayList<>();
        row.add(moveFontSizeLabel);
        row.add(moveFontSize);
        moveFontSize.setText(String.format("%d", GUIP.getMoveFontSize()));
        comps.add(row);

        moveFontStyle = new JTextField(4);
        moveFontStyle.setMaximumSize(new Dimension(150, 40));
        JLabel moveFontStyleLabel = new JLabel(Messages.getString("CommonSettingsDialog.moveFontStyle"));
        row = new ArrayList<>();
        row.add(moveFontStyleLabel);
        row.add(moveFontStyle);
        moveFontStyle.setText(String.format("%d", GUIP.getMoveFontStyle()));
        comps.add(row);

        row = new ArrayList<>();
        csbMoveDefaultColor = new ColourSelectorButton(Messages.getString("CommonSettingsDialog.colors.MoveDefaultColor"));
        csbMoveDefaultColor.setColour(GUIP.getMoveDefaultColor());
        row.add(csbMoveDefaultColor);

        csbMoveIllegalColor = new ColourSelectorButton(Messages.getString("CommonSettingsDialog.colors.MoveIllegalColor"));
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

        csbMoveSprintColor = new ColourSelectorButton(Messages.getString("CommonSettingsDialog.colors.MoveSprintColor"));
        csbMoveSprintColor.setColour(GUIP.getMoveSprintColor());
        row.add(csbMoveSprintColor);
        comps.add(row);

        addLineSpacer(comps);

        row = new ArrayList<>();
        csbFireSolnCanSeeColor = new ColourSelectorButton(Messages.getString("CommonSettingsDialog.colors.FireSolnCanSeeColor"));
        csbFireSolnCanSeeColor.setColour(GUIP.getFireSolnCanSeeColor());
        row.add(csbFireSolnCanSeeColor);

        csbFireSolnNoSeeColor = new ColourSelectorButton(Messages.getString("CommonSettingsDialog.colors.FireSolnNoSeeColor"));
        csbFireSolnNoSeeColor.setColour(GUIP.getFireSolnNoSeeColor());
        row.add(csbFireSolnNoSeeColor);
        comps.add(row);

        addLineSpacer(comps);

        row = new ArrayList<>();

        csbBoardTextColor = new ColourSelectorButton(Messages.getString("CommonSettingsDialog.colors.BoardTextColor"));
        csbBoardTextColor.setColour(GUIP.getBoardTextColor());
        row.add(csbBoardTextColor);

        csbBoardSpaceTextColor = new ColourSelectorButton(Messages.getString("CommonSettingsDialog.colors.BoardSpaceTextColor"));
        csbBoardSpaceTextColor.setColour(GUIP.getBoardSpaceTextColor());
        row.add(csbBoardSpaceTextColor);

        csbBuildingTextColor = new ColourSelectorButton(Messages.getString("CommonSettingsDialog.colors.BuildingTextColor"));
        csbBuildingTextColor.setColour(GUIP.getBuildingTextColor());
        row.add(csbBuildingTextColor);

        csbLowFoliageColor = new ColourSelectorButton(Messages.getString("CommonSettingsDialog.colors.LowFoliageColor"));
        csbLowFoliageColor.setColour(GUIP.getLowFoliageColor());
        row.add(csbLowFoliageColor);
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
        fovHighlightRingsRadiiLabel = new JLabel(Messages.getString("TacticalOverlaySettingsDialog.FovHighlightRingsRadii"));
        row.add(Box.createRigidArea(DEPENDENT_INSET));
        row.add(fovHighlightRingsRadiiLabel);
        comps.add(row);

        addSpacer(comps, 2);

        row = new ArrayList<>();
        fovHighlightRingsRadii= new JTextField((2+1)*7);
        fovHighlightRingsRadii.addFocusListener(this);
        fovHighlightRingsRadii.setMaximumSize(new Dimension(240, 40));
        row.add(Box.createRigidArea(DEPENDENT_INSET));
        row.add(fovHighlightRingsRadii);
        comps.add(row);

        addSpacer(comps, 2);

        row= new ArrayList<>();
        fovHighlightRingsColorsLabel = new JLabel(Messages.getString("TacticalOverlaySettingsDialog.FovHighlightRingsColors"));
        row.add(Box.createRigidArea(DEPENDENT_INSET));
        row.add(fovHighlightRingsColorsLabel);
        comps.add(row);

        addSpacer(comps, 2);

        row = new ArrayList<>();
        fovHighlightRingsColors= new JTextField(50);//      ((3+1)*3+1)*7);
        fovHighlightRingsColors.addFocusListener(this);
        fovHighlightRingsColors.setMaximumSize(new Dimension(200, 40 ));
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
        numStripesLabel = new JLabel(
                Messages.getString("TacticalOverlaySettingsDialog.FovStripes"));
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
        fovGrayscaleEnabled = new JCheckBox(
                Messages.getString("TacticalOverlaySettingsDialog.FovGrayscale"));
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

        tooltipDistSupression = new JTextField(4);
        tooltipDistSupression.setMaximumSize(new Dimension(150, 40));
        tooltipDistSupression.setToolTipText(Messages.getString("CommonSettingsDialog.tooltipDistSuppression.tooltip"));
        JLabel tooltipDistSupressionLabel = new JLabel(Messages.getString("CommonSettingsDialog.tooltipDistSuppression"));
        tooltipDistSupressionLabel.setToolTipText(Messages.getString("CommonSettingsDialog.tooltipDistSuppression.tooltip"));
        row = new ArrayList<>();
        row.add(tooltipDistSupressionLabel);
        row.add(tooltipDistSupression);
        comps.add(row);

        comps.add(checkboxEntry(showWpsinTT, null));
        comps.add(checkboxEntry(showPilotPortraitTT, null));

        addLineSpacer(comps);

        comps.add(checkboxEntry(showArmorMiniVisTT, null));

        row = new ArrayList<>();
        csbUnitTooltipArmorMiniIntact = new ColourSelectorButton(Messages.getString("CommonSettingsDialog.colors.UnitTooltipArmorMiniIntact"));
        csbUnitTooltipArmorMiniIntact.setColour(GUIP.getUnitTooltipArmorMiniColorIntact());
        row.add(csbUnitTooltipArmorMiniIntact);
        csbUnitTooltipArmorMiniPartial = new ColourSelectorButton(Messages.getString("CommonSettingsDialog.colors.UnitTooltipArmorMiniPartialDamage"));
        csbUnitTooltipArmorMiniPartial.setColour(GUIP.getUnitTooltipArmorMiniColorPartialDamage());
        row.add(csbUnitTooltipArmorMiniPartial);
        csbUnitTooltipArmorMiniDamaged = new ColourSelectorButton(Messages.getString("CommonSettingsDialog.colors.UnitTooltipArmorMiniDamaged"));
        csbUnitTooltipArmorMiniDamaged.setColour(GUIP.getUnitTooltipArmorMiniColorDamaged());
        row.add(csbUnitTooltipArmorMiniDamaged);
        comps.add(row);

        JLabel unitTooltipArmorLabel = new JLabel(Messages.getString("CommonSettingsDialog.armorMiniArmorChar"));
        unitTooltipArmorMiniArmorCharText = new JTextField(5);
        unitTooltipArmorMiniArmorCharText.setText(GUIP.getUnitToolTipArmorMiniArmorChar());
        unitTooltipArmorMiniArmorCharText.setMaximumSize(new Dimension(150, 40));
        unitTooltipArmorMiniArmorCharText.setToolTipText(Messages.getString("CommonSettingsDialog.armorMiniArmorChar.tooltip"));
        row = new ArrayList<>();
        row.add(unitTooltipArmorLabel);
        row.add(unitTooltipArmorMiniArmorCharText);
        comps.add(row);

        JLabel unitTooltipInternalStructureLabel = new JLabel(Messages.getString("CommonSettingsDialog.armorMiniInternalStructureChar"));
        unitTooltipArmorMiniInternalStructureCharText = new JTextField(5);
        unitTooltipArmorMiniInternalStructureCharText.setText(GUIP.getUnitToolTipArmorMiniISChar());
        unitTooltipArmorMiniInternalStructureCharText.setMaximumSize(new Dimension(150, 40));
        unitTooltipArmorMiniInternalStructureCharText.setToolTipText(Messages.getString("CommonSettingsDialog.armorMiniInternalStructureChar.tooltip"));
        row = new ArrayList<>();
        row.add(unitTooltipInternalStructureLabel);
        row.add(unitTooltipArmorMiniInternalStructureCharText);
        comps.add(row);

        JLabel unitTooltipCriticalLabel = new JLabel(Messages.getString("CommonSettingsDialog.armorMiniCriticalChar"));
        unitTooltipArmorMiniCriticalCharText = new JTextField(5);
        unitTooltipArmorMiniCriticalCharText.setText(GUIP.getUnitToolTipArmorMiniCriticalChar());
        unitTooltipArmorMiniCriticalCharText.setMaximumSize(new Dimension(150, 40));
        unitTooltipArmorMiniCriticalCharText.setToolTipText(Messages.getString("CommonSettingsDialog.armorMiniCriticalChar.tooltip"));
        row = new ArrayList<>();
        row.add(unitTooltipCriticalLabel);
        row.add(unitTooltipArmorMiniCriticalCharText);
        comps.add(row);

        JLabel unitTooltipDestroyedLabel = new JLabel(Messages.getString("CommonSettingsDialog.armorMiniDestroyedChar"));
        unitTooltipArmorMiniDestroyedCharText = new JTextField(5);
        unitTooltipArmorMiniDestroyedCharText.setText(GUIP.getUnitToolTipArmorMiniDestoryedChar());
        unitTooltipArmorMiniDestroyedCharText.setMaximumSize(new Dimension(150, 40));
        unitTooltipArmorMiniDestroyedCharText.setToolTipText(Messages.getString("CommonSettingsDialog.armorMiniDestroyedChar.tooltip"));
        row = new ArrayList<>();
        row.add(unitTooltipDestroyedLabel);
        row.add(unitTooltipArmorMiniDestroyedCharText);
        comps.add(row);

        JLabel unitTooltipCapArmorLabel = new JLabel(Messages.getString("CommonSettingsDialog.armorMiniCapArmorChar"));
        unitTooltipArmorMiniCapArmorCharText = new JTextField(5);
        unitTooltipArmorMiniCapArmorCharText.setText(GUIP.getUnitToolTipArmorMiniCapArmorChar());
        unitTooltipArmorMiniCapArmorCharText.setMaximumSize(new Dimension(150, 40));
        unitTooltipArmorMiniCapArmorCharText.setToolTipText(Messages.getString("CommonSettingsDialog.armorMiniCapArmorChar.tooltip"));
        row = new ArrayList<>();
        row.add(unitTooltipCapArmorLabel);
        row.add(unitTooltipArmorMiniCapArmorCharText);
        comps.add(row);

        JLabel unitTooltipUnitsPerBlockLabel = new JLabel(Messages.getString("CommonSettingsDialog.armorMiniUnitsPerBlock"));
        unitTooltipArmorMiniUnitsPerBlockText = new JTextField(5);
        unitTooltipArmorMiniUnitsPerBlockText.setText(String.format("%d", GUIP.getUnitToolTipArmorMiniUnitsPerBlock()));
        unitTooltipArmorMiniUnitsPerBlockText.setMaximumSize(new Dimension(150, 40));
        unitTooltipArmorMiniUnitsPerBlockText.setToolTipText(Messages.getString("CommonSettingsDialog.armorMiniUnitsPerBlock.tooltip"));
        row = new ArrayList<>();
        row.add(unitTooltipUnitsPerBlockLabel);
        row.add(unitTooltipArmorMiniUnitsPerBlockText);
        comps.add(row);

        JLabel unitTooltipFontSizeModLabel = new JLabel(Messages.getString("CommonSettingsDialog.armorMiniFontSizeMod"));
        unitTooltipArmorMiniFontSizeModText = new JTextField(5);
        unitTooltipArmorMiniFontSizeModText.setText(String.format("%d", GUIP.getUnitToolTipArmorMiniFontSizeMod()));
        unitTooltipArmorMiniFontSizeModText.setMaximumSize(new Dimension(150, 40));
        unitTooltipArmorMiniFontSizeModText.setToolTipText(Messages.getString("CommonSettingsDialog.armorMiniFontSizeMod.tooltip"));
        row = new ArrayList<>();
        row.add(unitTooltipFontSizeModLabel);
        row.add(unitTooltipArmorMiniFontSizeModText);
        comps.add(row);

        addLineSpacer(comps);

        JLabel unitTooltipSeenbyLabel = new JLabel(Messages.getString("CommonSettingsDialog.seenby.label"));
        unitTooltipSeenbyCbo = new JComboBox<>();
        unitTooltipSeenbyCbo.addItem(Messages.getString("CommonSettingsDialog.seenby.Someone"));
        unitTooltipSeenbyCbo.addItem(Messages.getString("CommonSettingsDialog.seenby.Team"));
        unitTooltipSeenbyCbo.addItem(Messages.getString("CommonSettingsDialog.seenby.Player"));
        unitTooltipSeenbyCbo.addItem(Messages.getString("CommonSettingsDialog.seenby.PlayerDetailed"));
        unitTooltipSeenbyCbo.setSelectedIndex(GUIP.getUnitToolTipSeenByResolution());
        unitTooltipSeenbyCbo.setMaximumSize(new Dimension(300, 60));
        row = new ArrayList<>();
        row.add(unitTooltipSeenbyLabel);
        row.add(unitTooltipSeenbyCbo);
        comps.add(row);

        JLabel phaseLabel = new JLabel(Messages.getString("CommonSettingsDialog.colors.UnitDisplayHeatLevels"));
        row = new ArrayList<>();
        row.add(phaseLabel);
        comps.add(row);

        row = new ArrayList<>();
        csbUnitDisplayHeatLevel1 = new ColourSelectorButton(Messages.getString("CommonSettingsDialog.colors.UnitDisplayHeatLevel1"));
        csbUnitDisplayHeatLevel1.setColour(GUIP.getUnitDisplayHeatLevel1());
        row.add(csbUnitDisplayHeatLevel1);
        unitDisplayHeatLevel1Text = new JTextField(5);
        unitDisplayHeatLevel1Text.setText(String.format("%d", GUIP.getUnitDisplayHeatValue1()));
        unitDisplayHeatLevel1Text.setMaximumSize(new Dimension(150, 40));
        unitDisplayHeatLevel1Text.setToolTipText(Messages.getString("CommonSettingsDialog.unitDisplayHeatToolTip"));
        row.add(unitDisplayHeatLevel1Text);
        csbUnitDisplayHeatLevel2 = new ColourSelectorButton(Messages.getString("CommonSettingsDialog.colors.UnitDisplayHeatLevel2"));
        csbUnitDisplayHeatLevel2.setColour(GUIP.getUnitDisplayHeatLevel2());
        row.add(csbUnitDisplayHeatLevel2);
        unitDisplayHeatLevel2Text = new JTextField(5);
        unitDisplayHeatLevel2Text.setText(String.format("%d", GUIP.getUnitDisplayHeatValue2()));
        unitDisplayHeatLevel2Text.setMaximumSize(new Dimension(150, 40));
        unitDisplayHeatLevel2Text.setToolTipText(Messages.getString("CommonSettingsDialog.unitDisplayHeatToolTip"));
        row.add(unitDisplayHeatLevel2Text);
        csbUnitDisplayHeatLevel3 = new ColourSelectorButton(Messages.getString("CommonSettingsDialog.colors.UnitDisplayHeatLevel3"));
        csbUnitDisplayHeatLevel3.setColour(GUIP.getUnitDisplayHeatLevel3());
        row.add(csbUnitDisplayHeatLevel3);
        unitDisplayHeatLevel3Text = new JTextField(5);
        unitDisplayHeatLevel3Text.setText(String.format("%d", GUIP.getUnitDisplayHeatValue3()));
        unitDisplayHeatLevel3Text.setMaximumSize(new Dimension(150, 40));
        unitDisplayHeatLevel3Text.setToolTipText(Messages.getString("CommonSettingsDialog.unitDisplayHeatToolTip"));
        row.add(unitDisplayHeatLevel3Text);
        comps.add(row);

        row = new ArrayList<>();
        csbUnitDisplayHeatLevel4 = new ColourSelectorButton(Messages.getString("CommonSettingsDialog.colors.UnitDisplayHeatLevel4"));
        csbUnitDisplayHeatLevel4.setColour(GUIP.getUnitDisplayHeatLevel4());
        row.add(csbUnitDisplayHeatLevel4);
        unitDisplayHeatLevel4Text = new JTextField(5);
        unitDisplayHeatLevel4Text.setText(String.format("%d", GUIP.getUnitDisplayHeatValue4()));
        unitDisplayHeatLevel4Text.setMaximumSize(new Dimension(150, 40));
        unitDisplayHeatLevel4Text.setToolTipText(Messages.getString("CommonSettingsDialog.unitDisplayHeatToolTip"));
        row.add(unitDisplayHeatLevel4Text);
        csbUnitDisplayHeatLevel5 = new ColourSelectorButton(Messages.getString("CommonSettingsDialog.colors.UnitDisplayHeatLevel5"));
        csbUnitDisplayHeatLevel5.setColour(GUIP.getUnitDisplayHeatLevel5());
        row.add(csbUnitDisplayHeatLevel5);
        unitDisplayHeatLevel5Text = new JTextField(5);
        unitDisplayHeatLevel5Text.setText(String.format("%d", GUIP.getUnitDisplayHeatValue5()));
        unitDisplayHeatLevel5Text.setMaximumSize(new Dimension(150, 40));
        unitDisplayHeatLevel5Text.setToolTipText(Messages.getString("CommonSettingsDialog.unitDisplayHeatToolTip"));
        row.add(unitDisplayHeatLevel5Text);
        csbUnitDisplayHeatLevel6 = new ColourSelectorButton(Messages.getString("CommonSettingsDialog.colors.UnitDisplayHeatLevel6"));
        csbUnitDisplayHeatLevel6.setColour(GUIP.getUnitDisplayHeatLevel6());
        row.add(csbUnitDisplayHeatLevel6);
        unitDisplayHeatLevel6Text = new JTextField(5);
        unitDisplayHeatLevel6Text.setText(String.format("%d", GUIP.getUnitDisplayHeatValue6()));
        unitDisplayHeatLevel6Text.setMaximumSize(new Dimension(150, 40));
        unitDisplayHeatLevel6Text.setToolTipText(Messages.getString("CommonSettingsDialog.unitDisplayHeatToolTip"));
        row.add(unitDisplayHeatLevel6Text);
        comps.add(row);

        row = new ArrayList<>();
        csbUnitDisplayHeatLevelOverheat = new ColourSelectorButton(Messages.getString("CommonSettingsDialog.colors.UnitDisplayHeatLevelOverheat"));
        csbUnitDisplayHeatLevelOverheat.setColour(GUIP.getUnitDisplayHeatLevelOverheat());
        row.add(csbUnitDisplayHeatLevelOverheat);
        comps.add(row);

        addLineSpacer(comps);

        JLabel defaultSortOrderLabel = new JLabel(Messages.getString("CommonSettingsDialog.defaultWeaponSortOrder"));
        String toolTip = Messages.getString("CommonSettingsDialog.defaultWeaponSortOrder.tooltip");
        defaultSortOrderLabel.setToolTipText(toolTip);

        final DefaultComboBoxModel<WeaponSortOrder> defaultWeaponSortOrderModel = new DefaultComboBoxModel<>(WeaponSortOrder.values());
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

        row = new ArrayList<>();
        JLabel orderLabel = new JLabel(Messages.getString("CommonSettingsDialog.orderLabel") + ": ");
        row.add(orderLabel);

        unitDisplayNonTabbed.addElement(UDOP.getString(UnitDisplay.NON_TABBED_A1));
        unitDisplayNonTabbed.addElement(UDOP.getString(UnitDisplay.NON_TABBED_B1));
        unitDisplayNonTabbed.addElement(UDOP.getString(UnitDisplay.NON_TABBED_C1));
        unitDisplayNonTabbed.addElement(UDOP.getString(UnitDisplay.NON_TABBED_A2));
        unitDisplayNonTabbed.addElement(UDOP.getString(UnitDisplay.NON_TABBED_B2));
        unitDisplayNonTabbed.addElement(UDOP.getString(UnitDisplay.NON_TABBED_C2));

        JList<String> listUnitDisplayNonTabbed = new JList<>(unitDisplayNonTabbed);
        listUnitDisplayNonTabbed.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        listUnitDisplayNonTabbed.setVisibleRowCount(2);
        listUnitDisplayNonTabbed.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listUnitDisplayNonTabbed.addMouseListener(cmdMouseAdaptor);
        listUnitDisplayNonTabbed.addMouseMotionListener(cmdMouseAdaptor);
        row.add(listUnitDisplayNonTabbed);
        comps.add(row);

        addLineSpacer(comps);

        JLabel unitDisplayMechArmorLargeFontSizeLabel = new JLabel(Messages.getString("CommonSettingsDialog.unitDisplayMechArmorLargeFontSize"));
        unitDisplayMechArmorLargeFontSizeText = new JTextField(5);
        unitDisplayMechArmorLargeFontSizeText.setText(String.format("%d", GUIP.getUnitDisplayMechArmorLargeFontSize()));
        unitDisplayMechArmorLargeFontSizeText.setMaximumSize(new Dimension(150, 40));
        row = new ArrayList<>();
        row.add(unitDisplayMechArmorLargeFontSizeLabel);
        row.add(unitDisplayMechArmorLargeFontSizeText);
        comps.add(row);

        JLabel unitDisplayMechArmorMediumFontSizeLabel = new JLabel(Messages.getString("CommonSettingsDialog.unitDisplayMechArmorMediumFontSize"));
        unitDisplayMechArmorMediumFontSizeText = new JTextField(5);
        unitDisplayMechArmorMediumFontSizeText.setText(String.format("%d", GUIP.getUnitDisplayMechArmorMediumFontSize()));
        unitDisplayMechArmorMediumFontSizeText.setMaximumSize(new Dimension(150, 40));
        row = new ArrayList<>();
        row.add(unitDisplayMechArmorMediumFontSizeLabel);
        row.add(unitDisplayMechArmorMediumFontSizeText);
        comps.add(row);

        JLabel unitDisplayMechArmorSmallFontSizeLabel = new JLabel(Messages.getString("CommonSettingsDialog.unitDisplayMechArmorSmallFontSize"));
        unitDisplayMechArmorSmallFontSizeText = new JTextField(5);
        unitDisplayMechArmorSmallFontSizeText.setText(String.format("%d", GUIP.getUnitDisplayMechArmorSmallFontSize()));
        unitDisplayMechArmorSmallFontSizeText.setMaximumSize(new Dimension(150, 40));
        row = new ArrayList<>();
        row.add(unitDisplayMechArmorSmallFontSizeLabel);
        row.add(unitDisplayMechArmorSmallFontSizeText);
        comps.add(row);

        JLabel unitDisplayMechLargeFontSizeLabel = new JLabel(Messages.getString("CommonSettingsDialog.unitDisplayMechLargeFontSize"));
        unitDisplayMechLargeFontSizeText = new JTextField(5);
        unitDisplayMechLargeFontSizeText.setText(String.format("%d", GUIP.getUnitDisplayMechLargeFontSize()));
        unitDisplayMechLargeFontSizeText.setMaximumSize(new Dimension(150, 40));
        row = new ArrayList<>();
        row.add(unitDisplayMechLargeFontSizeLabel);
        row.add(unitDisplayMechLargeFontSizeText);
        comps.add(row);

        JLabel unitDisplayMechMediumFontSizeLabel = new JLabel(Messages.getString("CommonSettingsDialog.unitDisplayMechMediumFontSize"));
        unitDisplayMechMeduimFontSizeText = new JTextField(5);
        unitDisplayMechMeduimFontSizeText.setText(String.format("%d", GUIP.getUnitDisplayMechMediumFontSize()));
        unitDisplayMechMeduimFontSizeText.setMaximumSize(new Dimension(150, 40));
        row = new ArrayList<>();
        row.add(unitDisplayMechMediumFontSizeLabel);
        row.add(unitDisplayMechMeduimFontSizeText);
        comps.add(row);


        return createSettingsPanel(comps);
    }

    private JPanel getReportPanel() {
        List<List<Component>> comps = new ArrayList<>();
        ArrayList<Component> row;

        row = new ArrayList<>();
        csbReportLinkColor = new ColourSelectorButton(Messages.getString("CommonSettingsDialog.colors.ReportLinkColor"));
        csbReportLinkColor.setColour(GUIP.getReportLinkColor());
        row.add(csbReportLinkColor);
        comps.add(row);

        comps.add(checkboxEntry(showReportSprites, null));
        showReportSprites.setSelected(GUIP.getMiniReportShowSprites());

        addLineSpacer(comps);

        JLabel reportKeywordsLabel = new JLabel(Messages.getString("CommonSettingsDialog.reportKeywords") + ": ");
        reportKeywordsTextPane = new JTextPane();
        row = new ArrayList<>();
        row.add(reportKeywordsLabel);
        row.add(reportKeywordsTextPane);
        comps.add(row);

        return createSettingsPanel(comps);
    }

    private JPanel getOverlaysPanel() {
        List<List<Component>> comps = new ArrayList<>();
        ArrayList<Component> row;

        addLineSpacer(comps);

        row = new ArrayList<>();
        csbUnitOverviewValidColor = new ColourSelectorButton(Messages.getString("CommonSettingsDialog.colors.UnitOverviewValidColor"));
        csbUnitOverviewValidColor.setColour(GUIP.getUnitOverviewValidColor());
        row.add(csbUnitOverviewValidColor);

        csbUnitOverviewSelectedColor = new ColourSelectorButton(Messages.getString("CommonSettingsDialog.colors.UnitOverviewSelectedColor"));
        csbUnitOverviewSelectedColor.setColour(GUIP.getUnitOverviewSelectedColor());
        row.add(csbUnitOverviewSelectedColor);
        comps.add(row);

        addLineSpacer(comps);

        row = new ArrayList<>();
        csbPlanetaryConditionsColorTitle = new ColourSelectorButton(Messages.getString("CommonSettingsDialog.colors.PlanetaryConditionsColorTitle"));
        csbPlanetaryConditionsColorTitle.setColour(GUIP.getPlanetaryConditionsColorTitle());
        row.add(csbPlanetaryConditionsColorTitle);

        csbPlanetaryConditionsColorText = new ColourSelectorButton(Messages.getString("CommonSettingsDialog.colors.PlanetaryConditionsColorText"));
        csbPlanetaryConditionsColorText.setColour(GUIP.getPlanetaryConditionsColorText());
        row.add(csbPlanetaryConditionsColorText);

        csbPlanetaryConditionsColorBackground = new ColourSelectorButton(Messages.getString("CommonSettingsDialog.colors.PlanetaryConditionsColorBackground"));
        csbPlanetaryConditionsColorBackground.setColour(GUIP.getPlanetaryConditionsColorBackground());
        row.add(csbPlanetaryConditionsColorBackground);
        comps.add(row);

        row = new ArrayList<>();
        csbPlanetaryConditionsColorCold = new ColourSelectorButton(Messages.getString("CommonSettingsDialog.colors.PlanetaryConditionsColorCold"));
        csbPlanetaryConditionsColorCold.setColour(GUIP.getPlanetaryConditionsColorCold());
        row.add(csbPlanetaryConditionsColorCold);

        csbPlanetaryConditionsColorHot = new ColourSelectorButton(Messages.getString("CommonSettingsDialog.colors.PlanetaryConditionsColorHot"));
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

        addLineSpacer(comps);

        return createSettingsPanel(comps);
    }

    private JPanel getMiniMapPanel() {
        List<List<Component>> comps = new ArrayList<>();

        comps.add(checkboxEntry(mmSymbol, null));
        comps.add(checkboxEntry(gameSummaryMM,
                Messages.getString("CommonSettingsDialog.gameSummaryMM.tooltip", Configuration.gameSummaryImagesMMDir())));

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

        comps.add(checkboxEntry(soundMuteChat, null));
        tfSoundMuteChatFileName = new JTextField(5);
        tfSoundMuteChatFileName.setMaximumSize(new Dimension(450, 40));
        row = new ArrayList<>();
        row.add(tfSoundMuteChatFileName);
        comps.add(row);
        comps.add(checkboxEntry(soundMuteMyTurn, null));
        tfSoundMuteMyTurntFileName = new JTextField(5);
        tfSoundMuteMyTurntFileName.setMaximumSize(new Dimension(450, 40));
        row = new ArrayList<>();
        row.add(tfSoundMuteMyTurntFileName);
        comps.add(row);
        comps.add(checkboxEntry(soundMuteOthersTurn, null));
        tfSoundMuteOthersFileName = new JTextField(5);
        tfSoundMuteOthersFileName.setMaximumSize(new Dimension(450, 40));
        row = new ArrayList<>();
        row.add(tfSoundMuteOthersFileName);
        comps.add(row);

        addLineSpacer(comps);

        JLabel unitStartCharLabel = new JLabel(Messages.getString("CommonSettingsDialog.protoMechUnitCodes")); 
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

        comps.add(checkboxEntry(defaultAutoejectDisabled, null));
        comps.add(checkboxEntry(useAverageSkills, null));
        comps.add(checkboxEntry(generateNames, null));
        addLineSpacer(comps);
        comps.add(checkboxEntry(keepGameLog, null));

        gameLogFilenameLabel = new JLabel(Messages.getString("CommonSettingsDialog.logFileName")); 
        gameLogFilename = new JTextField(15);
        gameLogFilename.setMaximumSize(new Dimension(250, 40));
        row = new ArrayList<>();
        row.add(Box.createRigidArea(DEPENDENT_INSET));
        row.add(gameLogFilenameLabel);
        row.add(gameLogFilename);
        comps.add(row);

        addSpacer(comps, 5);
        comps.add(checkboxEntry(stampFilenames, null));

        stampFormatLabel = new JLabel(Messages.getString("CommonSettingsDialog.stampFormat")); 
        stampFormat = new JTextField(15);
        stampFormat.setMaximumSize(new Dimension(15*13, 40));
        row = new ArrayList<>();
        row.add(Box.createRigidArea(DEPENDENT_INSET));
        row.add(stampFormatLabel);
        row.add(stampFormat);
        comps.add(row);

        addLineSpacer(comps);

        comps.add(checkboxEntry(showIPAddressesInChat, Messages.getString("CommonSettingsDialog.showIPAddressesInChat.tooltip")));
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
     * Display the current settings in this dialog. <p> Overrides
     * <code>Dialog#setVisible(boolean)</code>.
     */
    @Override
    public void setVisible(boolean visible) {
        // Initialize the dialog when it's being shown
        if (visible) {
            guiScale.setValue((int) (GUIP.getGUIScale() * 10));
            autoEndFiring.setSelected(GUIP.getAutoEndFiring());
            autoDeclareSearchlight.setSelected(GUIP.getAutoDeclareSearchlight());
            nagForMASC.setSelected(GUIP.getNagForMASC());
            nagForPSR.setSelected(GUIP.getNagForPSR());
            nagForWiGELanding.setSelected(GUIP.getNagForWiGELanding());
            nagForNoAction.setSelected(GUIP.getNagForNoAction());          
            nagForNoUnJamRAC.setSelected(GUIP.getNagForNoUnJamRAC());
            animateMove.setSelected(GUIP.getShowMoveStep());
            showWrecks.setSelected(GUIP.getShowWrecks());
            soundMuteChat.setSelected(GUIP.getSoundMuteChat());
            soundMuteMyTurn.setSelected(GUIP.getSoundMuteMyTurn());
            soundMuteOthersTurn.setSelected(GUIP.getSoundMuteOthersTurn());
            tooltipDelay.setText(Integer.toString(GUIP.getTooltipDelay()));
            tooltipDismissDelay.setText(Integer.toString(GUIP.getTooltipDismissDelay()));
            tooltipDistSupression.setText(Integer.toString(GUIP.getTooltipDistSuppression()));
            showWpsinTT.setSelected(GUIP.getShowWpsinTT());
            showArmorMiniVisTT.setSelected(GUIP.getshowArmorMiniVisTT());
            showPilotPortraitTT.setSelected(GUIP.getshowPilotPortraitTT());
            comboDefaultWeaponSortOrder.setSelectedItem(GUIP.getDefaultWeaponSortOrder());
            mouseWheelZoom.setSelected(GUIP.getMouseWheelZoom());
            mouseWheelZoomFlip.setSelected(GUIP.getMouseWheelZoomFlip());

            moveDefaultClimbMode.setSelected(GUIP.getMoveDefaultClimbMode());

            // Select the correct char set (give a nice default to start).
            unitStartChar.setSelectedIndex(0);
            for (int loop = 0; loop < unitStartChar.getItemCount(); loop++) {
                if (unitStartChar.getItemAt(loop).charAt(0) == CP.getUnitStartChar()) {
                    unitStartChar.setSelectedIndex(loop);
                    break;
                }
            }

            tfSoundMuteChatFileName.setText(GUIP.getSoundBingFilenameChat());
            tfSoundMuteMyTurntFileName.setText(GUIP.getSoundBingFilenameMyTurn());
            tfSoundMuteOthersFileName.setText(GUIP.getSoundBingFilenameOthersTurn());

            maxPathfinderTime.setText(Integer.toString(CP.getMaxPathfinderTime()));

            keepGameLog.setSelected(CP.keepGameLog());
            gameLogFilename.setEnabled(keepGameLog.isSelected());
            gameLogFilename.setText(CP.getGameLogFilename());
            stampFilenames.setSelected(CP.stampFilenames());
            stampFormat.setEnabled(stampFilenames.isSelected());
            stampFormat.setText(CP.getStampFormat());
            reportKeywordsTextPane.setText(CP.getReportKeywords());
            showIPAddressesInChat.setSelected(CP.getShowIPAddressesInChat());

            defaultAutoejectDisabled.setSelected(CP.defaultAutoejectDisabled());
            useAverageSkills.setSelected(CP.useAverageSkills());
            generateNames.setSelected(CP.generateNames());
            showUnitId.setSelected(CP.getShowUnitId());

            int index = 0;
            if (CP.getLocaleString().startsWith("de")) {
                index = 1;
            }
            if (CP.getLocaleString().startsWith("ru")) {
                index = 2;
            }
            displayLocale.setSelectedIndex(index);

            showMapsheets.setSelected(GUIP.getShowMapsheets());
            showDamageLevel.setSelected(GUIP.getShowDamageLevel());
            showDamageDecal.setSelected(GUIP.getShowDamageDecal());
            aOHexShadows.setSelected(GUIP.getAOHexShadows());
            floatingIso.setSelected(GUIP.getFloatingIso());
            mmSymbol.setSelected(GUIP.getMmSymbol());
            levelhighlight.setSelected(GUIP.getLevelHighlight());
            shadowMap.setSelected(GUIP.getShadowMap());
            hexInclines.setSelected(GUIP.getHexInclines());
            useSoftCenter.setSelected(GUIP.getSoftCenter());
            entityOwnerColor.setSelected(GUIP.getUnitLabelBorder());
            teamColoring.setSelected(GUIP.getTeamColoring());

            File dir = Configuration.hexesDir();
            tileSets = new ArrayList<>(Arrays.asList(dir.list((direc, name) -> name.endsWith(".tileset"))));
            tileSets.addAll(userDataFiles(Configuration.hexesDir(), ".tileset"));
            tileSetChoice.removeAllItems();
            for (int i = 0; i < tileSets.size(); i++) {
                String name = tileSets.get(i);
                tileSetChoice.addItem(name.substring(0, name.length() - 8));
                if (name.equals(CP.getMapTileset())) {
                    tileSetChoice.setSelectedIndex(i);
                }
            }

            gameSummaryBV.setSelected(GUIP.getGameSummaryBoardView());
            gameSummaryMM.setSelected(GUIP.getGameSummaryMinimap());

            skinFiles.removeAllItems();
            List<String> xmlFiles = new ArrayList<>(Arrays
                    .asList(Configuration.skinsDir().list((directory, fileName) -> fileName.endsWith(".xml"))));
            xmlFiles.addAll(userDataFiles(Configuration.skinsDir(), ".xml"));
            Collections.sort(xmlFiles);
            for (String file : xmlFiles) {
                if (SkinXMLHandler.validSkinSpecFile(file)) {
                    skinFiles.addItem(file);
                }
            }
            // Select the default file first
            skinFiles.setSelectedItem(SkinXMLHandler.defaultSkinXML);
            // If this select fails, the default skin will be selected
            skinFiles.setSelectedItem(GUIP.getSkinFile());

            uiThemes.removeAllItems();
            for (LookAndFeelInfo lafInfo : UIManager.getInstalledLookAndFeels()) {
                uiThemes.addItem(new UITheme(lafInfo.getClassName(), lafInfo.getName()));
            }
            uiThemes.setSelectedItem(new UITheme(GUIP.getUITheme()));

            fovInsideEnabled.setSelected(GUIP.getFovHighlight());
            fovHighlightAlpha.setValue(GUIP.getFovHighlightAlpha());
            fovHighlightRingsRadii.setText(GUIP.getFovHighlightRingsRadii());
            fovHighlightRingsColors.setText(GUIP.getFovHighlightRingsColorsHsb() );
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
            savedLevelhighlight = GUIP.getLevelHighlight();
            savedFloatingIso = GUIP.getFloatingIso();
            savedMmSymbol = GUIP.getMmSymbol();
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
            savedAdvancedOpt.clear();
            
            advancedKeys.clearSelection();
            
            for (KeyCommandBind kcb : KeyCommandBind.values()) {
                cmdModifierMap.get(kcb.cmd).setText(KeyEvent.getModifiersExText(kcb.modifiers));
                cmdKeyMap.get(kcb.cmd).setText(KeyEvent.getKeyText(kcb.key));
            }
            markDuplicateBinds();

            adaptToGUIScale();
        }

        super.setVisible(visible);
    }

    /** Cancels any updates made in this dialog and closes it.  */
    @Override
    protected void cancelAction() {
        GUIP.setFovHighlight(savedFovHighlight);
        GUIP.setFovDarken(savedFovDarken);
        GUIP.setFovGrayscale(savedFovGrayscale);
        GUIP.setAOHexShadows(savedAOHexShadows);
        GUIP.setShadowMap(savedShadowMap);
        GUIP.setHexInclines(savedHexInclines);
        GUIP.setLevelHighlight(savedLevelhighlight);
        GUIP.setFloatingIso(savedFloatingIso);
        GUIP.setMmSymbol(savedMmSymbol);
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
        csbWarningColor.setColour(GUIP.getWarningColor());

        csbMoveDefaultColor.setColour(GUIP.getMoveDefaultColor());
        csbMoveIllegalColor.setColour(GUIP.getMoveIllegalColor());
        csbMoveJumpColor.setColour(GUIP.getMoveJumpColor());
        csbMoveMASCColor.setColour(GUIP.getMoveMASCColor());
        csbMoveRunColor.setColour(GUIP.getMoveRunColor());
        csbMoveBackColor.setColour(GUIP.getMoveBackColor());
        csbMoveSprintColor.setColour(GUIP.getMoveSprintColor());

        csbFireSolnCanSeeColor.setColour(GUIP.getFireSolnCanSeeColor());
        csbFireSolnNoSeeColor.setColour(GUIP.getFireSolnNoSeeColor());
        csbBuildingTextColor.setColour(GUIP.getBuildingTextColor());
        csbBoardTextColor.setColour(GUIP.getBoardTextColor());
        csbBoardSpaceTextColor.setColour(GUIP.getBoardSpaceTextColor());
        csbLowFoliageColor.setColour(GUIP.getLowFoliageColor());
        csbMapsheetColor.setColour(GUIP.getMapsheetColor());

        attackArrowTransparency.setText(String.format("%d", GUIP.getAttachArrowTransparency()));
        ecmTransparency.setText(String.format("%d", GUIP.getECMTransparency()));
        buttonsPerRow.setText(String.format("%d", GUIP.getButtonsPerRow()));
        playersRemainingToShow.setText(String.format("%d", GUIP.getPlayersRemainingToShow()));
        tmmPipModeCbo.setSelectedIndex(GUIP.getTMMPipMode());
        moveFontType.setText(GUIP.getMoveFontType());
        moveFontSize.setText(String.format("%d", GUIP.getMoveFontSize()));
        moveFontStyle.setText(String.format("%d", GUIP.getMoveFontStyle()));
        darkenMapAtNight.setSelected(GUIP.getDarkenMapAtNight());
        translucentHiddenUnits.setSelected(GUIP.getTranslucentHiddenUnits());

        for (String option: savedAdvancedOpt.keySet()) {
            GUIP.setValue(option, savedAdvancedOpt.get(option));
        }

        unitDisplayNonTabbed.clear();
        unitDisplayNonTabbed.addElement(UDOP.getString(UnitDisplay.NON_TABBED_A1));
        unitDisplayNonTabbed.addElement(UDOP.getString(UnitDisplay.NON_TABBED_B1));
        unitDisplayNonTabbed.addElement(UDOP.getString(UnitDisplay.NON_TABBED_C1));
        unitDisplayNonTabbed.addElement(UDOP.getString(UnitDisplay.NON_TABBED_A2));
        unitDisplayNonTabbed.addElement(UDOP.getString(UnitDisplay.NON_TABBED_B2));
        unitDisplayNonTabbed.addElement(UDOP.getString(UnitDisplay.NON_TABBED_C2));

        unitDisplayAutoDisplayReportCombo.setSelectedItem(GUIP.getUnitDisplayAutoDisplayReportPhase());
        unitDisplayAutoDisplayNonReportCombo.setSelectedItem(GUIP.getUnitDisplayAutoDisplayNonReportPhase());
        miniMapAutoDisplayReportCombo.setSelectedItem(GUIP.getMinimapAutoDisplayReportPhase());
        miniMapAutoDisplayNonReportCombo.setSelectedItem(GUIP.getMinimapAutoDisplayNonReportPhase());
        miniReportAutoDisplayReportCombo.setSelectedItem(GUIP.getMiniReportAutoDisplayReportPhase());
        miniReportAutoDisplayNonReportCombo.setSelectedItem(GUIP.getMiniReportAutoDisplayNonReportPhase());
        playerListAutoDisplayReportCombo.setSelectedItem(GUIP.getPlayerListAutoDisplayReportPhase());
        playerListAutoDisplayNonReportCombo.setSelectedItem(GUIP.getPlayerListAutoDisplayNonReportPhase());

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

        unitTooltipSeenbyCbo.setSelectedIndex(GUIP.getUnitToolTipSeenByResolution());
        unitDisplayWeaponListHeightText.setText(String.format("%d", GUIP.getUnitDisplayWeaponListHeight()));

        unitDisplayMechArmorLargeFontSizeText.setText(String.format("%d", GUIP.getUnitDisplayMechArmorLargeFontSize()));
        unitDisplayMechArmorMediumFontSizeText.setText(String.format("%d", GUIP.getUnitDisplayMechArmorMediumFontSize()));
        unitDisplayMechArmorSmallFontSizeText.setText(String.format("%d", GUIP.getUnitDisplayMechArmorSmallFontSize()));
        unitDisplayMechLargeFontSizeText.setText(String.format("%d", GUIP.getUnitDisplayMechLargeFontSize()));
        unitDisplayMechMeduimFontSizeText.setText(String.format("%d", GUIP.getUnitDisplayMechMediumFontSize()));

        csbUnitTooltipArmorMiniIntact.setColour(GUIP.getUnitTooltipArmorMiniColorIntact());
        csbUnitTooltipArmorMiniPartial.setColour(GUIP.getUnitTooltipArmorMiniColorPartialDamage());
        csbUnitTooltipArmorMiniDamaged.setColour(GUIP.getUnitTooltipArmorMiniColorDamaged());
        unitTooltipArmorMiniArmorCharText.setText(GUIP.getUnitToolTipArmorMiniArmorChar());
        unitTooltipArmorMiniInternalStructureCharText.setText(GUIP.getUnitToolTipArmorMiniISChar());
        unitTooltipArmorMiniCriticalCharText.setText(GUIP.getUnitToolTipArmorMiniCriticalChar());
        unitTooltipArmorMiniDestroyedCharText.setText(GUIP.getUnitToolTipArmorMiniDestoryedChar());
        unitTooltipArmorMiniCapArmorCharText.setText(GUIP.getUnitToolTipArmorMiniCapArmorChar());
        unitTooltipArmorMiniUnitsPerBlockText.setText(String.format("%d", GUIP.getUnitToolTipArmorMiniUnitsPerBlock()));
        unitTooltipArmorMiniFontSizeModText.setText(String.format("%d", GUIP.getUnitToolTipArmorMiniFontSizeMod()));

        csbReportLinkColor.setColour(GUIP.getReportLinkColor());
        showReportSprites.setSelected(GUIP.getMiniReportShowSprites());

        csbUnitOverviewValidColor.setColour(GUIP.getUnitOverviewValidColor());
        csbUnitOverviewSelectedColor.setColour(GUIP.getUnitOverviewSelectedColor());

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
        GUIP.setShowMoveStep(animateMove.isSelected());
        GUIP.setShowWrecks(showWrecks.isSelected());
        GUIP.setSoundMuteChat(soundMuteChat.isSelected());
        GUIP.setSoundMuteMyTurn(soundMuteMyTurn.isSelected());
        GUIP.setSoundMuteOthersTurn(soundMuteOthersTurn.isSelected());
        GUIP.setShowWpsinTT(showWpsinTT.isSelected());
        GUIP.setshowArmorMiniVisTT(showArmorMiniVisTT.isSelected());
        GUIP.setshowPilotPortraitTT(showPilotPortraitTT.isSelected());
        GUIP.setWarningColor(csbWarningColor.getColour());

        GUIP.setMoveDefaultColor(csbMoveDefaultColor.getColour());
        GUIP.setMoveIllegalColor(csbMoveIllegalColor.getColour());
        GUIP.setMoveJumpColor(csbMoveJumpColor.getColour());
        GUIP.setMoveMASCColor(csbMoveMASCColor.getColour());
        GUIP.setMoveRunColor(csbMoveRunColor.getColour());
        GUIP.setMoveBackColor(csbMoveBackColor.getColour());
        GUIP.setMoveSprintColor(csbMoveSprintColor.getColour());

        GUIP.setFireSolnCanSeeColor(csbFireSolnCanSeeColor.getColour());
        GUIP.setFireSolnNoSeeColor(csbFireSolnNoSeeColor.getColour());
        GUIP.setBuildingTextColor(csbBuildingTextColor.getColour());
        GUIP.setBoardTextColor(csbBoardTextColor.getColour());
        GUIP.setBoardSpaceTextColor(csbBoardSpaceTextColor.getColour());
        GUIP.setLowFoliageColor(csbLowFoliageColor.getColour());
        GUIP.setMapsheetColor(csbMapsheetColor.getColour());

        try {
            GUIP.setAttachArrowTransparency(Integer.parseInt(attackArrowTransparency.getText()));
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
        }
        try {
            GUIP.setECMTransparency(Integer.parseInt(ecmTransparency.getText()));
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
        }
        try {
            GUIP.setButtonsPerRow(Integer.parseInt(buttonsPerRow.getText()));
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
        }
        try {
            GUIP.setPlayersRemainingToShow(Integer.parseInt(playersRemainingToShow.getText()));
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
        }

        GUIP.setTMMPipMode(tmmPipModeCbo.getSelectedIndex());
        GUIP.setDarkenMapAtNight(darkenMapAtNight.isSelected());
        GUIP.setTranslucentHiddenUnits(translucentHiddenUnits.isSelected());


        GUIP.setMoveFontType(moveFontType.getText());
        try {
            GUIP.setMoveFontSize(Integer.parseInt(moveFontSize.getText()));
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
        }
        try {
            GUIP.setMoveFontStyle(Integer.parseInt(moveFontStyle.getText()));
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
        }

        try {
            GUIP.setTooltipDelay(Integer.parseInt(tooltipDelay.getText()));
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
        }
        try {
            GUIP.setTooltipDismissDelay(Integer.parseInt(tooltipDismissDelay.getText()));
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
        }
        try {
            GUIP.setTooltipDistSuppression(Integer.parseInt(tooltipDistSupression.getText()));
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
        }
        GUIP.setValue(GUIPreferences.GUI_SCALE, (float) (guiScale.getValue()) / 10);
        CP.setUnitStartChar(((String) unitStartChar.getSelectedItem()).charAt(0));

        GUIP.setMouseWheelZoom(mouseWheelZoom.isSelected());
        GUIP.setMouseWheelZoomFlip(mouseWheelZoomFlip.isSelected());

        GUIP.setMoveDefaultClimbMode(moveDefaultClimbMode.isSelected());

        GUIP.setSoundBingFilenameChat(tfSoundMuteChatFileName.getText());
        GUIP.setSoundBingFilenameMyTurn(tfSoundMuteMyTurntFileName.getText());
        GUIP.setSoundBingFilenameOthersTurn(tfSoundMuteOthersFileName.getText());

        try {
            CP.setMaxPathfinderTime(Integer.parseInt(maxPathfinderTime.getText()));
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
        }

        GUIP.setGetFocus(getFocus.isSelected());

        CP.setKeepGameLog(keepGameLog.isSelected());
        CP.setGameLogFilename(gameLogFilename.getText());
        CP.setStampFilenames(stampFilenames.isSelected());
        CP.setStampFormat(stampFormat.getText());
        CP.setReportKeywords(reportKeywordsTextPane.getText());
        CP.setShowIPAddressesInChat(showIPAddressesInChat.isSelected());

        CP.setDefaultAutoejectDisabled(defaultAutoejectDisabled.isSelected());
        CP.setUseAverageSkills(useAverageSkills.isSelected());
        CP.setGenerateNames(generateNames.isSelected());
        CP.setShowUnitId(showUnitId.isSelected());
        if ((clientgui != null) && (clientgui.getBoardView() != null)) {
            clientgui.getBoardView().updateEntityLabels();
        }

        CP.setLocale(CommonSettingsDialog.LOCALE_CHOICES[displayLocale.getSelectedIndex()]);

        GUIP.setShowMapsheets(showMapsheets.isSelected());
        GUIP.setAOHexShadows(aOHexShadows.isSelected());
        GUIP.setFloatingIso(floatingIso.isSelected());
        GUIP.setMmSymbol(mmSymbol.isSelected());
        GUIP.setLevelHighlight(levelhighlight.isSelected());
        GUIP.setShadowMap(shadowMap.isSelected());
        GUIP.setHexInclines(hexInclines.isSelected());
        GUIP.setSoftcenter(useSoftCenter.isSelected());
        GUIP.setGameSummaryBoardView(gameSummaryBV.isSelected());
        GUIP.setGameSummaryMinimap(gameSummaryMM.isSelected());

        UITheme newUITheme = (UITheme) uiThemes.getSelectedItem();
        String oldUITheme = GUIP.getUITheme();
        if (!oldUITheme.equals(newUITheme.getClassName())) {
            GUIP.setUITheme(newUITheme.getClassName());
        }

        String newSkinFile = (String) skinFiles.getSelectedItem();
        String oldSkinFile = GUIP.getSkinFile();
        if (!oldSkinFile.equals(newSkinFile)) {
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
            if (!CP.getMapTileset().equals(tileSetFileName) &&
                    (clientgui != null) && (clientgui.getBoardView() != null))  {
                clientgui.getBoardView().clearShadowMap();
            }
            CP.setMapTileset(tileSetFileName);
        }

        ToolTipManager.sharedInstance().setInitialDelay(GUIP.getTooltipDelay());
        if (GUIP.getTooltipDismissDelay() > 0) {
            ToolTipManager.sharedInstance().setDismissDelay(GUIP.getTooltipDismissDelay());
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
        for (int i = 0; i < movePhaseCommands.getSize(); i++) {
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
        for (int i = 0; i < deployPhaseCommands.getSize(); i++) {
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
        for (int i = 0; i < firingPhaseCommands.getSize(); i++) {
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
        for (int i = 0; i < physicalPhaseCommands.getSize(); i++) {
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
        for (int i = 0; i < targetingPhaseCommands.getSize(); i++) {
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

            if ((s > UnitDisplay.NON_TABBED_ZERO_INDEX) && (!unitDisplayNonTabbed.get(UnitDisplay.NON_TABBED_ZERO_INDEX).equals(UDOP.getString(UnitDisplay.NON_TABBED_A1)))) {
                unitDisplayNonTabbedChanged = true;
                UDOP.setValue(UnitDisplay.NON_TABBED_A1,  unitDisplayNonTabbed.get(UnitDisplay.NON_TABBED_ZERO_INDEX));
            }
            if ((s > UnitDisplay.NON_TABBED_ONE_INDEX) && (!unitDisplayNonTabbed.get(UnitDisplay.NON_TABBED_ONE_INDEX).equals(UDOP.getString(UnitDisplay.NON_TABBED_B1)))) {
                unitDisplayNonTabbedChanged = true;
                UDOP.setValue(UnitDisplay.NON_TABBED_B1,  unitDisplayNonTabbed.get(UnitDisplay.NON_TABBED_ONE_INDEX));
            }
            if ((s > UnitDisplay.NON_TABBED_TWO_INDEX) && (!unitDisplayNonTabbed.get(UnitDisplay.NON_TABBED_TWO_INDEX).equals( UDOP.getString(UnitDisplay.NON_TABBED_C1)))) {
                unitDisplayNonTabbedChanged = true;
                UDOP.setValue(UnitDisplay.NON_TABBED_C1,  unitDisplayNonTabbed.get(UnitDisplay.NON_TABBED_TWO_INDEX));
            }
            if ((s > UnitDisplay.NON_TABBED_THREE_INDEX) && (!unitDisplayNonTabbed.get(UnitDisplay.NON_TABBED_THREE_INDEX).equals(UDOP.getString(UnitDisplay.NON_TABBED_A2)))) {
                unitDisplayNonTabbedChanged = true;
                UDOP.setValue(UnitDisplay.NON_TABBED_A2,  unitDisplayNonTabbed.get(UnitDisplay.NON_TABBED_THREE_INDEX));
            }
            if ((s > UnitDisplay.NON_TABBED_FOUR_INDEX) && (!unitDisplayNonTabbed.get(UnitDisplay.NON_TABBED_FOUR_INDEX).equals(UDOP.getString(UnitDisplay.NON_TABBED_B2)))) {
                unitDisplayNonTabbedChanged = true;
                UDOP.setValue(UnitDisplay.NON_TABBED_B2,  unitDisplayNonTabbed.get(UnitDisplay.NON_TABBED_FOUR_INDEX));
            }
            if ((s > UnitDisplay.NON_TABBED_FIVE_INDEX) && (!unitDisplayNonTabbed.get(UnitDisplay.NON_TABBED_FIVE_INDEX).equals( UDOP.getString(UnitDisplay.NON_TABBED_C2)))) {
                unitDisplayNonTabbedChanged = true;
                UDOP.setValue(UnitDisplay.NON_TABBED_C2,  unitDisplayNonTabbed.get(UnitDisplay.NON_TABBED_FIVE_INDEX));
            }

            if ((unitDisplayNonTabbedChanged) && (clientgui != null)) {
                clientgui.unitDisplay.setDisplayNonTabbed();
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

        GUIP.setUnitDisplayHeatColorLevel1(csbUnitDisplayHeatLevel1.getColour());
        GUIP.setUnitDisplayHeatColorLevel2(csbUnitDisplayHeatLevel2.getColour());
        GUIP.setUnitDisplayHeatColorLevel3(csbUnitDisplayHeatLevel3.getColour());
        GUIP.setUnitDisplayHeatColorLevel4(csbUnitDisplayHeatLevel4.getColour());
        GUIP.setUnitDisplayHeatColorLevel5(csbUnitDisplayHeatLevel5.getColour());
        GUIP.setUnitDisplayHeatColorLevel6(csbUnitDisplayHeatLevel6 .getColour());
        GUIP.setUnitDisplayHeatColorLevelOverHeat(csbUnitDisplayHeatLevelOverheat.getColour());

        try {
            GUIP.setUnitDisplayHeatColorValue1(Integer.parseInt(unitDisplayHeatLevel1Text.getText()));
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
        }
        try {
            GUIP.setUnitDisplayHeatColorValue2(Integer.parseInt(unitDisplayHeatLevel2Text.getText()));
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
        }
        try {
            GUIP.setUnitDisplayHeatColorValue3(Integer.parseInt(unitDisplayHeatLevel3Text.getText()));
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
        }
        try {
            GUIP.setUnitDisplayHeatColorValue4(Integer.parseInt(unitDisplayHeatLevel4Text.getText()));
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
        }
        try {
            GUIP.setUnitDisplayHeatColorValue5(Integer.parseInt(unitDisplayHeatLevel5Text.getText()));
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
        }
        try {
            GUIP.setUnitDisplayHeatColorValue6(Integer.parseInt(unitDisplayHeatLevel6Text.getText()));
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
        }

        GUIP.setUnitToolTipSeenByResolution(unitTooltipSeenbyCbo.getSelectedIndex());
        try {
            GUIP.setUnitDisplayWeaponListHeight(Integer.parseInt(unitDisplayWeaponListHeightText.getText()));
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
        }

        try {
            GUIP.setUnitDisplayMechArmorLargeFontSize(Integer.parseInt(unitDisplayMechArmorLargeFontSizeText.getText()));
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
        }
        try {
            GUIP.setUnitDisplayMechArmorMediumFontSize(Integer.parseInt(unitDisplayMechArmorMediumFontSizeText.getText()));
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
        }
        try {
            GUIP.setUnitDisplayMechArmorSmallFontSize(Integer.parseInt(unitDisplayMechArmorSmallFontSizeText.getText()));
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
        }
        try {
            GUIP.setUnitDisplayMechLargeFontSize(Integer.parseInt(unitDisplayMechLargeFontSizeText.getText()));
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
        }
        try {
            GUIP.setUnitDisplayMechMediumFontSize(Integer.parseInt(unitDisplayMechMeduimFontSizeText.getText()));
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
        }

        GUIP.setUnitTooltipArmorminiColorIntact(csbUnitTooltipArmorMiniIntact.getColour());
        GUIP.setUnitTooltipArmorminiColorPartialDamage(csbUnitTooltipArmorMiniPartial.getColour());
        GUIP.setUnitTooltipArmorminiColorDamaged(csbUnitTooltipArmorMiniDamaged.getColour());
        GUIP.setUnitToolTipArmorMiniArmorChar(unitTooltipArmorMiniArmorCharText.getText());
        GUIP.setUnitToolTipArmorMiniISChar(unitTooltipArmorMiniInternalStructureCharText.getText());
        GUIP.setUnitToolTipArmorMiniCriticalChar(unitTooltipArmorMiniCriticalCharText.getText());
        GUIP.setUnitTooltipArmorminiDestroyedChar(unitTooltipArmorMiniDestroyedCharText.getText());
        GUIP.setUnitTooltipArmorMiniCapArmorChar(unitTooltipArmorMiniCapArmorCharText.getText());
        try {
            GUIP.setUnitTooltipArmorMiniUnitsPerBlock(Integer.parseInt(unitTooltipArmorMiniUnitsPerBlockText.getText()));
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
        }
        try {
            GUIP.setUnitToolTipArmorMiniFontSize(Integer.parseInt(unitTooltipArmorMiniFontSizeModText.getText()));
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
        }

        GUIP.setReportLinkColor(csbReportLinkColor.getColour());
        GUIP.setMiniReportShowSprites(showReportSprites.isSelected());

        GUIP.setUnitOverviewValidColor(csbUnitOverviewValidColor.getColour());
        GUIP.setUnitOverviewSelectedColor(csbUnitOverviewSelectedColor.getColour());

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
        } else if (source.equals(levelhighlight)) {
            GUIP.setLevelHighlight(levelhighlight.isSelected());
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
        }
    }

    @Override
    public void focusGained(FocusEvent e) { }

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
    
    /** Creates a panel with a box for all of the commands that can be bound to keys. */
    private JPanel getKeyBindPanel() {
        // The first column is for labels, the second column for modifiers, the third column for keys
        JPanel outer = new JPanel();
        outer.setLayout(new BoxLayout(outer, BoxLayout.PAGE_AXIS));
        
        var tabChoice = new JPanel();
        tabChoice.setLayout(new BoxLayout(tabChoice, BoxLayout.PAGE_AXIS));
        tabChoice.setBorder(new EmptyBorder(15, 15, 15, 15));
        var buttonPanel = new JPanel();
        var labelPanel = new JPanel();
        choiceToggle.addActionListener(e -> updateKeybindsFocusTraversal());
        defaultKeyBindButton.addActionListener(e -> updateKeybindsDefault());

        String msg_line1 = Messages.getString("CommonSettingsDialog.keyBinds.tabMessageLine1");
        String msg_line2 = Messages.getString("CommonSettingsDialog.keyBinds.tabMessageLine2");
        String msg_line3 = Messages.getString("CommonSettingsDialog.keyBinds.tabMessageLine3");
        var choiceLabel = new JLabel(
                "<HTML><CENTER>" + msg_line1 +
                        "<BR>" + msg_line2 +
                        "<BR>" + msg_line3 + "</HTML>");

        buttonPanel.add(choiceToggle);
        buttonPanel.add(defaultKeyBindButton);
        labelPanel.add(choiceLabel);
        tabChoice.add(buttonPanel);
        tabChoice.add(labelPanel);
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
                    // keyPressed.  We've already done what we want with the
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
            key.setText(KeyEvent.getKeyText(kcb.key));
            // Update how typing in the text field works
            final String cmd = kcb.cmd;
            cmdKeyMap.put(cmd, key);
            cmdKeyCodeMap.put(cmd, kcb.key);
            key.addKeyListener(new KeyListener() {

                @Override
                public void keyPressed(KeyEvent evt) {
                    // Don't consume this event if modifiers are held (-> enable button mnemonics)
                    if (evt.getModifiersEx() != 0) {
                        return;
                    }
                    key.setText(KeyEvent.getKeyText(evt.getKeyCode()));
                    cmdKeyCodeMap.put(kcb.cmd, evt.getKeyCode());
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
                    // keyPressed.  We've already done what we want with the
                    // typed key, and we don't want anything else acting upon
                    // the key typed event, so we consume it here.
                    evt.consume();
                }

            });
            keyBinds.add(key, gbc);
            gbc.gridx = 0;
            gbc.gridy++;
            
            // deactivate TABbing through fields here so TAB can be caught as a keybind
            modifiers.setFocusTraversalKeysEnabled(false);
            key.setFocusTraversalKeysEnabled(false);
        }
        markDuplicateBinds();
        return outer;
    }

    private JPanel getPhasePanel() {
        List<List<Component>> comps = new ArrayList<>();
        ArrayList<Component> row;

        JLabel unitDisplayLabel = new JLabel(Messages.getString("CommonMenuBar.viewMekDisplay"));
        row = new ArrayList<>();
        row.add(unitDisplayLabel);
        comps.add(row);

        JLabel phaseLabel = new JLabel(Messages.getString("CommonSettingsDialog.reportPhases") + ": ");
        unitDisplayAutoDisplayReportCombo = new JComboBox<>();
        unitDisplayAutoDisplayReportCombo.addItem(Messages.getString("ClientGUI.Hide"));
        unitDisplayAutoDisplayReportCombo.addItem(Messages.getString("ClientGUI.Show"));
        unitDisplayAutoDisplayReportCombo.addItem(Messages.getString("ClientGUI.Manual"));
        unitDisplayAutoDisplayReportCombo.setMaximumSize(new Dimension(150, 40));
        row = new ArrayList<>();
        unitDisplayAutoDisplayReportCombo.setSelectedIndex(GUIP.getUnitDisplayAutoDisplayReportPhase());
        row.add(phaseLabel);
        row.add(unitDisplayAutoDisplayReportCombo);
        comps.add(row);

        phaseLabel = new JLabel(Messages.getString("CommonSettingsDialog.nonReportPhases") + ": ");
        unitDisplayAutoDisplayNonReportCombo = new JComboBox<>();
        unitDisplayAutoDisplayNonReportCombo.addItem(Messages.getString("ClientGUI.Hide"));
        unitDisplayAutoDisplayNonReportCombo.addItem(Messages.getString("ClientGUI.Show"));
        unitDisplayAutoDisplayNonReportCombo.addItem(Messages.getString("ClientGUI.Manual"));
        unitDisplayAutoDisplayNonReportCombo.setMaximumSize(new Dimension(150, 40));
        row = new ArrayList<>();
        unitDisplayAutoDisplayNonReportCombo.setSelectedIndex(GUIP.getUnitDisplayAutoDisplayNonReportPhase());
        row.add(phaseLabel);
        row.add(unitDisplayAutoDisplayNonReportCombo);
        comps.add(row);

        addLineSpacer(comps);

        JLabel miniMapLabel = new JLabel(Messages.getString("CommonMenuBar.viewMinimap"));
        row = new ArrayList<>();
        row.add(miniMapLabel);
        comps.add(row);

        phaseLabel = new JLabel(Messages.getString("CommonSettingsDialog.reportPhases") + ": ");
        miniMapAutoDisplayReportCombo = new JComboBox<>();
        miniMapAutoDisplayReportCombo.addItem(Messages.getString("ClientGUI.Hide"));
        miniMapAutoDisplayReportCombo.addItem(Messages.getString("ClientGUI.Show"));
        miniMapAutoDisplayReportCombo.addItem(Messages.getString("ClientGUI.Manual"));
        miniMapAutoDisplayReportCombo.setMaximumSize(new Dimension(150, 40));
        row = new ArrayList<>();
        miniMapAutoDisplayReportCombo.setSelectedIndex(GUIP.getMinimapAutoDisplayReportPhase());
        row.add(phaseLabel);
        row.add(miniMapAutoDisplayReportCombo);
        comps.add(row);

        phaseLabel = new JLabel(Messages.getString("CommonSettingsDialog.nonReportPhases") + ": ");
        miniMapAutoDisplayNonReportCombo = new JComboBox<>();
        miniMapAutoDisplayNonReportCombo.addItem(Messages.getString("ClientGUI.Hide"));
        miniMapAutoDisplayNonReportCombo.addItem(Messages.getString("ClientGUI.Show"));
        miniMapAutoDisplayNonReportCombo.addItem(Messages.getString("ClientGUI.Manual"));
        miniMapAutoDisplayNonReportCombo.setMaximumSize(new Dimension(150, 40));
        row = new ArrayList<>();
        miniMapAutoDisplayNonReportCombo.setSelectedIndex(GUIP.getMinimapAutoDisplayNonReportPhase());
        row.add(phaseLabel);
        row.add(miniMapAutoDisplayNonReportCombo);
        comps.add(row);

        addLineSpacer(comps);

        JLabel miniReportLabel = new JLabel(Messages.getString("CommonMenuBar.viewRoundReport"));
        row = new ArrayList<>();
        row.add(miniReportLabel);
        comps.add(row);

        phaseLabel = new JLabel(Messages.getString("CommonSettingsDialog.reportPhases") + ": ");
        miniReportAutoDisplayReportCombo = new JComboBox<>();
        miniReportAutoDisplayReportCombo.addItem(Messages.getString("ClientGUI.Hide"));
        miniReportAutoDisplayReportCombo.addItem(Messages.getString("ClientGUI.Show"));
        miniReportAutoDisplayReportCombo.addItem(Messages.getString("ClientGUI.Manual"));
        miniReportAutoDisplayReportCombo.setMaximumSize(new Dimension(150, 40));
        row = new ArrayList<>();
        miniReportAutoDisplayReportCombo.setSelectedIndex(GUIP.getMiniReportAutoDisplayReportPhase());
        row.add(phaseLabel);
        row.add(miniReportAutoDisplayReportCombo);
        comps.add(row);

        phaseLabel = new JLabel(Messages.getString("CommonSettingsDialog.nonReportPhases") + ": ");
        miniReportAutoDisplayNonReportCombo = new JComboBox<>();
        miniReportAutoDisplayNonReportCombo.addItem(Messages.getString("ClientGUI.Hide"));
        miniReportAutoDisplayNonReportCombo.addItem(Messages.getString("ClientGUI.Show"));
        miniReportAutoDisplayNonReportCombo.addItem(Messages.getString("ClientGUI.Manual"));
        miniReportAutoDisplayNonReportCombo.setMaximumSize(new Dimension(150, 40));
        row = new ArrayList<>();
        miniReportAutoDisplayNonReportCombo.setSelectedIndex(GUIP.getMiniReportAutoDisplayNonReportPhase());
        row.add(phaseLabel);
        row.add(miniReportAutoDisplayNonReportCombo);
        comps.add(row);

        addLineSpacer(comps);

        JLabel playerListLabel = new JLabel(Messages.getString("CommonMenuBar.viewPlayerList"));
        row = new ArrayList<>();
        row.add(playerListLabel);
        comps.add(row);

        phaseLabel = new JLabel(Messages.getString("CommonSettingsDialog.reportPhases") + ": ");
        playerListAutoDisplayReportCombo = new JComboBox<>();
        playerListAutoDisplayReportCombo.addItem(Messages.getString("ClientGUI.Hide"));
        playerListAutoDisplayReportCombo.addItem(Messages.getString("ClientGUI.Show"));
        playerListAutoDisplayReportCombo.addItem(Messages.getString("ClientGUI.Manual"));
        playerListAutoDisplayReportCombo.setMaximumSize(new Dimension(150, 40));
        row = new ArrayList<>();
        playerListAutoDisplayReportCombo.setSelectedIndex(GUIP.getPlayerListAutoDisplayReportPhase());
        row.add(phaseLabel);
        row.add(playerListAutoDisplayReportCombo);
        comps.add(row);

        phaseLabel = new JLabel(Messages.getString("CommonSettingsDialog.nonReportPhases") + ": ");
        playerListAutoDisplayNonReportCombo = new JComboBox<>();
        playerListAutoDisplayNonReportCombo.addItem(Messages.getString("ClientGUI.Hide"));
        playerListAutoDisplayNonReportCombo.addItem(Messages.getString("ClientGUI.Show"));
        playerListAutoDisplayNonReportCombo.addItem(Messages.getString("ClientGUI.Manual"));
        playerListAutoDisplayNonReportCombo.setMaximumSize(new Dimension(150, 40));
        row = new ArrayList<>();
        playerListAutoDisplayNonReportCombo.setSelectedIndex(GUIP.getPlayerListAutoDisplayNonReportPhase());
        row.add(phaseLabel);
        row.add(playerListAutoDisplayNonReportCombo);
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
     * Marks the text fields when duplicate keybinds occur. Two commands may share a keybind if none
     * of them is a Menubar or exclusive keybind (although that only works well if they're used in different
     * phases such as turn and twist). 
     * Also checks for Ctrl-C and Ctrl-V. These are coded into JTables and JTrees and making them
     * configurable would be unproportional effort to the gain. 
     */
    private void markDuplicateBinds() {
        Map<KeyStroke, KeyCommandBind> duplicates = new HashMap<>();
        Set<KeyStroke> allKeys = new HashSet<>();
        // Assemble all keybinds that are used twice into the duplicates map
        for (KeyCommandBind kcb : KeyCommandBind.values()) {
            KeyStroke keyStroke = KeyStroke.getKeyStroke(keyCode(kcb), modifierCode(kcb));
            if (!allKeys.add(keyStroke)) {
                duplicates.put(keyStroke, kcb);
            }
        }
                    
        // Now traverse the commands again. When a duplicate keybind is found and this KeyCommandBind is exclusive or Menubar
        // or the other one (the first one found with the same keybind) is exclusive or Menubar, both are marked.
        // Also, Ctrl-C and Ctrl-V are marked as these are hard-mapped to Copy/Paste and cannot be used otherwise.
        for (KeyCommandBind kcb : KeyCommandBind.values()) {
            boolean isCorrect = true;
            KeyStroke keyStroke = KeyStroke.getKeyStroke(keyCode(kcb), modifierCode(kcb));
            if (duplicates.containsKey(keyStroke) && 
                    (kcb.isMenuBar || kcb.isExclusive || duplicates.get(keyStroke).isExclusive || duplicates.get(keyStroke).isMenuBar)) {
                // Mark the current kcb and the one that was already in the keyMap as duplicate
                markTextfield(cmdModifierMap.get(kcb.cmd), "This keybind is a duplicate and will not work correctly.");
                markTextfield(cmdKeyMap.get(kcb.cmd), "This keybind is a duplicate and will not work correctly.");
                markTextfield(cmdModifierMap.get(duplicates.get(keyStroke).cmd), "This keybind is a duplicate and will not work correctly.");
                markTextfield(cmdKeyMap.get(duplicates.get(keyStroke).cmd), "This keybind is a duplicate and will not work correctly.");
                isCorrect = false;
            }
            // Check for standard copy/paste keys
            if (keyStroke.equals(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK))
                    || keyStroke.equals(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.CTRL_DOWN_MASK))) {
                markTextfield(cmdModifierMap.get(kcb.cmd), "Ctrl-C / Ctrl-V cannot be used");
                markTextfield(cmdKeyMap.get(kcb.cmd), "Ctrl-C / Ctrl-V cannot be used");
                isCorrect = false;
            }
            if (isCorrect) {
                markTextfield(cmdModifierMap.get(kcb.cmd), null);
                markTextfield(cmdKeyMap.get(kcb.cmd), null);
            }
        }
    }
    
    private void markTextfield(JTextField field, String errorMsg) {
        field.setForeground(errorMsg != null ? GUIP.getWarningColor() : null);
        field.setToolTipText(errorMsg);
    }
    
    /** Returns the keycode for the character part of a user-entered keybind (The "V" in CTRL-V). */
    private int keyCode(KeyCommandBind kcb) {
        return cmdKeyCodeMap.get(kcb.cmd);
    }
    
    /** Returns the keycode for the modifier part of a user-entered keybind (The "CTRL" in CTRL-V). */
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
    
    /** Creates a panel with a list boxes that allow the button order to be changed. */
    private JPanel getButtonOrderPanel() {
        JPanel buttonOrderPanel = new JPanel();
        buttonOrderPanel.setLayout(new BoxLayout(buttonOrderPanel, BoxLayout.Y_AXIS));
        JTabbedPane phasePane = new JTabbedPane();
        buttonOrderPanel.add(phasePane);
        
        // MovementPhaseDisplay        
        movePhaseCommands = new DefaultListModel<>();
        phasePane.add("Movement", getButtonOrderPane(movePhaseCommands,
                MovementDisplay.MoveCommand.values()));
        
        // DeploymentPhaseDisplay
        deployPhaseCommands = new DefaultListModel<>();
        phasePane.add("Deployment", getButtonOrderPane(deployPhaseCommands,
                DeploymentDisplay.DeployCommand.values()));
        
        // FiringPhaseDisplay
        firingPhaseCommands = new DefaultListModel<>();
        phasePane.add("Firing", getButtonOrderPane(firingPhaseCommands,
                FiringDisplay.FiringCommand.values()));
        
        // PhysicalPhaseDisplay
        physicalPhaseCommands = new DefaultListModel<>();
        phasePane.add("Physical", getButtonOrderPane(physicalPhaseCommands,
                PhysicalDisplay.PhysicalCommand.values()));          
        
        // TargetingPhaseDisplay
        targetingPhaseCommands = new DefaultListModel<>();
        phasePane.add("Targeting", getButtonOrderPane(targetingPhaseCommands,
                TargetingPhaseDisplay.TargetingCommand.values()));
        
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
        Box innerpanel = new Box(BoxLayout.PAGE_AXIS);
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
            innerpanel.add(subPanel);
        }
        innerpanel.add(Box.createVerticalGlue());
        innerpanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panel.add(innerpanel,BorderLayout.PAGE_START);
        return panel;
    }

    private JPanel getAdvancedSettingsPanel() {
        JPanel p = new JPanel();

        String[] s = GUIP.getAdvancedProperties();
        AdvancedOptionData[] opts = new AdvancedOptionData[s.length];
        for (int i = 0; i < s.length; i++) {
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
            advancedValue.setText(GUIP.getString(
                    "Advanced" + advancedKeys.getSelectedValue().option));
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
        }
    }

    /**
     *  Returns the files in the directory given as relativePath (e.g. Configuration.hexesDir())
     *  under the userData directory ending with fileEnding (such as ".xml")
     */
    public static List<String> userDataFiles(File relativePath, String fileEnding) {
        List<String> result = new ArrayList<>();
        File dir = new File(Configuration.userdataDir(), relativePath.toString());
        String[] userDataFiles = dir.list((direc, name) -> name.endsWith(fileEnding));
        if (userDataFiles != null) {
            result.addAll(Arrays.asList(userDataFiles));
        }
        return result;
    }

    private void adaptToGUIScale() {
        UIUtil.adjustDialog(this, UIUtil.FONT_SCALE1);
    }
}
