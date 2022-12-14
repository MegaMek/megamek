/*
 * MegaMek
 * Copyright (c) 2003-2005 - Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2021-2022 - The MegaMek Team. All Rights Reserved.
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
public class CommonSettingsDialog extends AbstractButtonDialog implements
        ItemListener, FocusListener, ListSelectionListener,
        ChangeListener {

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
    private final JCheckBox animateMove = new JCheckBox(Messages.getString("CommonSettingsDialog.animateMove"));
    private final JCheckBox showWrecks = new JCheckBox(Messages.getString("CommonSettingsDialog.showWrecks"));
    private final JCheckBox soundMute = new JCheckBox(Messages.getString("CommonSettingsDialog.soundMute"));
    private final JCheckBox showWpsinTT = new JCheckBox(Messages.getString("CommonSettingsDialog.showWpsinTT"));
    private final JCheckBox showArmorMiniVisTT = new JCheckBox(Messages.getString("CommonSettingsDialog.showArmorMiniVisTT"));
    private final JCheckBox showPilotPortraitTT = new JCheckBox(Messages.getString("CommonSettingsDialog.showPilotPortraitTT"));
    private final JCheckBox chkAntiAliasing = new JCheckBox(Messages.getString("CommonSettingsDialog.antiAliasing"));
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
    private final JCheckBox aOHexShadows = new JCheckBox(Messages.getString("CommonSettingsDialog.AOHexSHadows"));
    private final JCheckBox floatingIso = new JCheckBox(Messages.getString("CommonSettingsDialog.floatingIso"));
    private final JCheckBox mmSymbol = new JCheckBox(Messages.getString("CommonSettingsDialog.mmSymbol"));
    private final JCheckBox entityOwnerColor = new JCheckBox(Messages.getString("CommonSettingsDialog.entityOwnerColor"));
    private final JCheckBox teamColoring = new JCheckBox(Messages.getString("CommonSettingsDialog.teamColoring"));
    private final JCheckBox useSoftCenter = new JCheckBox(Messages.getString("CommonSettingsDialog.useSoftCenter"));
    private final JCheckBox levelhighlight = new JCheckBox(Messages.getString("CommonSettingsDialog.levelHighlight"));
    private final JCheckBox shadowMap = new JCheckBox(Messages.getString("CommonSettingsDialog.useShadowMap"));
    private final JCheckBox hexInclines = new JCheckBox(Messages.getString("CommonSettingsDialog.useInclines"));
    private final JCheckBox mouseWheelZoom = new JCheckBox(Messages.getString("CommonSettingsDialog.mouseWheelZoom"));
    private final JCheckBox mouseWheelZoomFlip = new JCheckBox(Messages.getString("CommonSettingsDialog.mouseWheelZoomFlip"));

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
    private final MMToggleButton choiceToggle = new MMToggleButton("Enable tabbing through this dialog page");

    private JComboBox unitDisplayAutoDisplayReportCombo;
    private JComboBox unitDisplayAutoDisplayNonReportCombo;
    private JComboBox miniMapAutoDisplayReportCombo;
    private JComboBox miniMapAutoDisplayNonReportCombo;
    private JComboBox miniReportAutoDisplayReportCombo;
    private JComboBox miniReportAutoDisplayNonReportCombo;
    private JComboBox playerListAutoDisplayReportCombo;
    private JComboBox playerListAutoDisplayNonReportCombo;

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
    private boolean savedUnitLabelBorder;
    private boolean savedShowDamageDecal;
    private boolean savedShowDamageLabel;
    private boolean savedAntiAlias;
    private String savedFovHighlightRingsRadii;
    private String savedFovHighlightRingsColors;
    private int savedFovHighlightAlpha;
    private int savedFovDarkenAlpha;
    private int savedNumStripesSlider;
    HashMap<String, String> savedAdvancedOpt = new HashMap<>();

    private static final String MSG_UNITDISPLAY = Messages.getString("CommonMenuBar.viewMekDisplay");
    private static final String MSG_MINIMAP = Messages.getString("CommonMenuBar.viewMinimap");
    private static final String MSG_MINIREPORT = Messages.getString("CommonMenuBar.viewRoundReport");
    private static final String MSG_PLAYERLIST = Messages.getString("CommonMenuBar.viewPlayerList");
    private static final String MSG_SHOW = Messages.getString("ClientGUI.Show");
    private static final String MSG_HIDE = Messages.getString("ClientGUI.Hide");
    private static final String MSG_MANUAL = Messages.getString("ClientGUI.Manual");
    private static final String MSG_REPORTPHASES = Messages.getString("CommonSettingsDialog.ReportPhases");
    private static final String MSG_NONREPORTPHASES = Messages.getString("CommonSettingsDialog.NonReportPhases");
    private static final String MSG_MAIN = Messages.getString("CommonSettingsDialog.Main");
    private static final String MSG_GRAPHICS = Messages.getString("CommonSettingsDialog.Graphics");
    private static final String MSG_KEYBINDS = Messages.getString("CommonSettingsDialog.KeyBinds");
    private static final String MSG_BUTTONORDER = Messages.getString("CommonSettingsDialog.ButtonOrder");
    private static final String MSG_UNITDISPLAYORDER = Messages.getString("CommonSettingsDialog.UnitDisplayOrder");
    private static final String MSG_AUTODISPLAY = Messages.getString("CommonSettingsDialog.AutoDisplay");
    private static final String MSG_ADVANCED = Messages.getString("CommonSettingsDialog.Advanced");

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
        JScrollPane graphicsPane = new JScrollPane(getGraphicsPanel());
        graphicsPane.getVerticalScrollBar().setUnitIncrement(16);
        JScrollPane keyBindPane = new JScrollPane(getKeyBindPanel());
        keyBindPane.getVerticalScrollBar().setUnitIncrement(16);
        JScrollPane advancedSettingsPane = new JScrollPane(getAdvancedSettingsPanel());
        advancedSettingsPane.getVerticalScrollBar().setUnitIncrement(16);
        JScrollPane unitDisplayPane = new JScrollPane(getUnitDisplayPanel());
        unitDisplayPane.getVerticalScrollBar().setUnitIncrement(16);
        JScrollPane autoDisplayPane = new JScrollPane(getPhasePanel());
        autoDisplayPane.getVerticalScrollBar().setUnitIncrement(16);

        panTabs.add(MSG_MAIN, settingsPane);
        panTabs.add(MSG_GRAPHICS, graphicsPane);
        panTabs.add(MSG_KEYBINDS, keyBindPane);
        panTabs.add(MSG_BUTTONORDER, getButtonOrderPanel());
        panTabs.add(MSG_UNITDISPLAYORDER, unitDisplayPane);
        panTabs.add(MSG_AUTODISPLAY, autoDisplayPane);
        panTabs.add(MSG_ADVANCED, advancedSettingsPane);

        adaptToGUIScale();

        return panTabs;
    }

    private JPanel getUnitDisplayPanel() {
        JPanel unitDisplayPane = new JPanel();
        unitDisplayPane.setLayout(new BoxLayout(unitDisplayPane, BoxLayout.Y_AXIS));
        JTabbedPane phasePane = new JTabbedPane();
        unitDisplayPane.add(phasePane);

        phasePane.add("Non Tabbed", getUnitDisplayPane());

        return unitDisplayPane;
    }

    private JScrollPane getUnitDisplayPane() {
        JPanel panel = new JPanel();

        unitDisplayNonTabbed.addElement(UDOP.getString(UnitDisplay.NON_TABBED_A1));
        unitDisplayNonTabbed.addElement(UDOP.getString(UnitDisplay.NON_TABBED_B1));
        unitDisplayNonTabbed.addElement(UDOP.getString(UnitDisplay.NON_TABBED_C1));
        unitDisplayNonTabbed.addElement(UDOP.getString(UnitDisplay.NON_TABBED_A2));
        unitDisplayNonTabbed.addElement(UDOP.getString(UnitDisplay.NON_TABBED_B2));
        unitDisplayNonTabbed.addElement(UDOP.getString(UnitDisplay.NON_TABBED_C2));

        JList<String> listUnitDisplayNonTabbed = new JList<>(unitDisplayNonTabbed);
        listUnitDisplayNonTabbed.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listUnitDisplayNonTabbed.addMouseListener(cmdMouseAdaptor);
        listUnitDisplayNonTabbed.addMouseMotionListener(cmdMouseAdaptor);
        panel.add(listUnitDisplayNonTabbed);
        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        return scrollPane;
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

        comps.add(checkboxEntry(showDamageLevel, null));
        comps.add(checkboxEntry(showDamageDecal, null));
        comps.add(checkboxEntry(showUnitId, null));
        comps.add(checkboxEntry(entityOwnerColor, null));
        comps.add(checkboxEntry(teamColoring, null));
        comps.add(checkboxEntry(useSoftCenter, Messages.getString("CommonSettingsDialog.useSoftCenterTip")));
        addLineSpacer(comps);

        tooltipDelay = new JTextField(4);
        tooltipDelay.setMaximumSize(new Dimension(150, 40));
        JLabel tooltipDelayLabel = new JLabel(Messages.getString("CommonSettingsDialog.tooltipDelay")); 
        row = new ArrayList<>();
        row.add(tooltipDelayLabel);
        row.add(tooltipDelay);
        comps.add(row);

        tooltipDismissDelay = new JTextField(4);
        tooltipDismissDelay.setMaximumSize(new Dimension(150, 40));
        tooltipDismissDelay.setToolTipText(Messages.getString("CommonSettingsDialog.tooltipDismissDelayTooltip"));
        JLabel tooltipDismissDelayLabel = new JLabel(Messages.getString("CommonSettingsDialog.tooltipDismissDelay")); 
        tooltipDismissDelayLabel.setToolTipText(Messages.getString("CommonSettingsDialog.tooltipDismissDelayTooltip"));
        row = new ArrayList<>();
        row.add(tooltipDismissDelayLabel);
        row.add(tooltipDismissDelay);
        comps.add(row);
        
        tooltipDistSupression = new JTextField(4);
        tooltipDistSupression.setMaximumSize(new Dimension(150, 40));
        tooltipDistSupression.setToolTipText(Messages.getString("CommonSettingsDialog.tooltipDistSuppressionTooltip"));
        JLabel tooltipDistSupressionLabel = new JLabel(Messages.getString("CommonSettingsDialog.tooltipDistSuppression")); 
        tooltipDistSupressionLabel.setToolTipText(Messages.getString("CommonSettingsDialog.tooltipDistSuppressionTooltip"));
        row = new ArrayList<>();
        row.add(tooltipDistSupressionLabel);
        row.add(tooltipDistSupression);
        comps.add(row);

        comps.add(checkboxEntry(showWpsinTT, null));
        comps.add(checkboxEntry(showArmorMiniVisTT, null));
        comps.add(checkboxEntry(showPilotPortraitTT, null));
        addLineSpacer(comps);
        comps.add(checkboxEntry(soundMute, null));
        
        JLabel maxPathfinderTimeLabel = new JLabel(Messages.getString("CommonSettingsDialog.pathFiderTimeLimit"));
        maxPathfinderTime = new JTextField(5);
        maxPathfinderTime.setMaximumSize(new Dimension(150, 40));
        row = new ArrayList<>();
        row.add(maxPathfinderTimeLabel);
        row.add(maxPathfinderTime);
        comps.add(row);

        addLineSpacer(comps);
        comps.add(checkboxEntry(nagForMASC, null));
        comps.add(checkboxEntry(nagForPSR, null));
        comps.add(checkboxEntry(nagForWiGELanding, null));
        comps.add(checkboxEntry(nagForNoAction, null));
        comps.add(checkboxEntry(getFocus, null));
        comps.add(checkboxEntry(mouseWheelZoom, null));
        comps.add(checkboxEntry(mouseWheelZoomFlip, null));
        comps.add(checkboxEntry(autoEndFiring, null));
        comps.add(checkboxEntry(autoDeclareSearchlight, null));

        JLabel defaultSortOrderLabel = new JLabel(Messages.getString("CommonSettingsDialog.defaultWeaponSortOrder"));
        String toolTip = Messages.getString("CommonSettingsDialog.defaultWeaponSortOrderTooltip");
        defaultSortOrderLabel.setToolTipText(toolTip);

        final DefaultComboBoxModel<WeaponSortOrder> defaultWeaponSortOrderModel = new DefaultComboBoxModel<>(WeaponSortOrder.values());
        defaultWeaponSortOrderModel.removeElement(WeaponSortOrder.CUSTOM); // Custom makes no sense as a default
        comboDefaultWeaponSortOrder = new MMComboBox<>("comboDefaultWeaponSortOrder", defaultWeaponSortOrderModel);
        comboDefaultWeaponSortOrder.setToolTipText(toolTip);
        row = new ArrayList<>();
        row.add(defaultSortOrderLabel);
        row.add(comboDefaultWeaponSortOrder);
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
            animateMove.setSelected(GUIP.getShowMoveStep());
            showWrecks.setSelected(GUIP.getShowWrecks());
            soundMute.setSelected(GUIP.getSoundMute());
            tooltipDelay.setText(Integer.toString(GUIP.getTooltipDelay()));
            tooltipDismissDelay.setText(Integer.toString(GUIP.getTooltipDismissDelay()));
            tooltipDistSupression.setText(Integer.toString(GUIP.getTooltipDistSuppression()));
            showWpsinTT.setSelected(GUIP.getShowWpsinTT());
            showArmorMiniVisTT.setSelected(GUIP.getshowArmorMiniVisTT());
            showPilotPortraitTT.setSelected(GUIP.getshowPilotPortraitTT());
            comboDefaultWeaponSortOrder.setSelectedItem(GUIP.getDefaultWeaponSortOrder());
            mouseWheelZoom.setSelected(GUIP.getMouseWheelZoom());
            mouseWheelZoomFlip.setSelected(GUIP.getMouseWheelZoomFlip());

            // Select the correct char set (give a nice default to start).
            unitStartChar.setSelectedIndex(0);
            for (int loop = 0; loop < unitStartChar.getItemCount(); loop++) {
                if (unitStartChar.getItemAt(loop).charAt(0) == CP.getUnitStartChar()) {
                    unitStartChar.setSelectedIndex(loop);
                    break;
                }
            }

            maxPathfinderTime.setText(Integer.toString(CP.getMaxPathfinderTime()));

            keepGameLog.setSelected(CP.keepGameLog());
            gameLogFilename.setEnabled(keepGameLog.isSelected());
            gameLogFilename.setText(CP.getGameLogFilename());
            stampFilenames.setSelected(CP.stampFilenames());
            stampFormat.setEnabled(stampFilenames.isSelected());
            stampFormat.setText(CP.getStampFormat());
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
            chkAntiAliasing.setSelected(GUIP.getAntiAliasing());
            showDamageLevel.setSelected(GUIP.getShowDamageLevel());
            showDamageDecal.setSelected(GUIP.getShowDamageDecal());
            aOHexShadows.setSelected(GUIP.getAOHexShadows());
            floatingIso.setSelected(GUIP.getFloatingIso());
            mmSymbol.setSelected(GUIP.getMmSymbol());
            levelhighlight.setSelected(GUIP.getLevelHighlight());
            shadowMap.setSelected(GUIP.getShadowMap());
            hexInclines.setSelected(GUIP.getHexInclines());
            useSoftCenter.setSelected(GUIP.getBoolean("SOFTCENTER"));
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
            savedUnitLabelBorder = GUIP.getUnitLabelBorder();
            savedShowDamageDecal = GUIP.getShowDamageDecal();
            savedShowDamageLabel = GUIP.getShowDamageLevel();
            savedFovHighlightRingsRadii = GUIP.getFovHighlightRingsRadii();
            savedFovHighlightRingsColors = GUIP.getFovHighlightRingsColorsHsb();
            savedFovHighlightAlpha = GUIP.getFovHighlightAlpha();
            savedFovDarkenAlpha = GUIP.getFovDarkenAlpha();
            savedNumStripesSlider = GUIP.getFovStripes();
            savedAntiAlias = GUIP.getAntiAliasing();
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
        GUIP.setUnitLabelBorder(savedUnitLabelBorder);
        GUIP.setShowDamageDecal(savedShowDamageDecal);
        GUIP.setShowDamageLevel(savedShowDamageLabel);
        GUIP.setFovHighlightRingsRadii(savedFovHighlightRingsRadii);
        GUIP.setFovHighlightRingsColorsHsb(savedFovHighlightRingsColors);
        GUIP.setFovHighlightAlpha(savedFovHighlightAlpha);
        GUIP.setFovDarkenAlpha(savedFovDarkenAlpha);
        GUIP.setFovStripes(savedNumStripesSlider);
        GUIP.setAntiAliasing(savedAntiAlias);
         
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

        setVisible(false);
    }

    /** Update the settings from this dialog's values, then close it. */
    @Override
    protected void okAction() {
        GUIP.setShowDamageLevel(showDamageLevel.isSelected());
        GUIP.setShowDamageDecal(showDamageDecal.isSelected());
        GUIP.setUnitLabelBorder(entityOwnerColor.isSelected());
        GUIP.setTeamColoring(teamColoring.isSelected());
        GUIP.setAutoEndFiring(autoEndFiring.isSelected());
        GUIP.setAutoDeclareSearchlight(autoDeclareSearchlight.isSelected());
        GUIP.setDefaultWeaponSortOrder(Objects.requireNonNull(comboDefaultWeaponSortOrder.getSelectedItem()));
        GUIP.setNagForMASC(nagForMASC.isSelected());
        GUIP.setNagForPSR(nagForPSR.isSelected());
        GUIP.setNagForWiGELanding(nagForWiGELanding.isSelected());
        GUIP.setNagForNoAction(nagForNoAction.isSelected());
        GUIP.setShowMoveStep(animateMove.isSelected());
        GUIP.setShowWrecks(showWrecks.isSelected());
        GUIP.setSoundMute(soundMute.isSelected());
        GUIP.setShowWpsinTT(showWpsinTT.isSelected());
        GUIP.setshowArmorMiniVisTT(showArmorMiniVisTT.isSelected());
        GUIP.setshowPilotPortraitTT(showPilotPortraitTT.isSelected());
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
        GUIP.setValue("SOFTCENTER", useSoftCenter.isSelected());
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

        GUIP.setUnitDisplayAutoDisplayReportPhase((String) unitDisplayAutoDisplayReportCombo.getSelectedItem());
        GUIP.setUnitDisplayAutoDisplayNonReportPhase((String) unitDisplayAutoDisplayNonReportCombo.getSelectedItem());
        GUIP.setMinimapAutoDisplayReportPhase((String) miniMapAutoDisplayReportCombo.getSelectedItem());
        GUIP.setMinimapAutoDisplayNonReportPhase((String) miniMapAutoDisplayNonReportCombo.getSelectedItem());
        GUIP.setMiniReportAutoDisplayReportPhase((String) miniReportAutoDisplayReportCombo.getSelectedItem());
        GUIP.setMiniReportAutoDisplayNonReportPhase((String) miniReportAutoDisplayNonReportCombo.getSelectedItem());
        GUIP.setPlayerListAutoDisplayReportPhase((String) playerListAutoDisplayReportCombo.getSelectedItem());
        GUIP.setPlayerListAutoDisplayNonReportPhase((String) playerListAutoDisplayNonReportCombo.getSelectedItem());

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
        } else if (source.equals(chkAntiAliasing)) {
            GUIP.setAntiAliasing(chkAntiAliasing.isSelected());
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

    /** The Graphics Tab */
    private JPanel getGraphicsPanel() {

        List<List<Component>> comps = new ArrayList<>();
        ArrayList<Component> row;

        comps.add(checkboxEntry(chkAntiAliasing, Messages.getString("CommonSettingsDialog.antiAliasingToolTip")));
        comps.add(checkboxEntry(animateMove, null));
        comps.add(checkboxEntry(showWrecks, null));
        comps.add(checkboxEntry(showMapsheets, null));
        comps.add(checkboxEntry(aOHexShadows, null));
        comps.add(checkboxEntry(shadowMap, null));
        comps.add(checkboxEntry(hexInclines, null));
        comps.add(checkboxEntry(levelhighlight, null));
        comps.add(checkboxEntry(floatingIso, null));
        comps.add(checkboxEntry(mmSymbol, null));
        comps.add(checkboxEntry(gameSummaryBV,
                Messages.getString("CommonSettingsDialog.gameSummaryBV.tooltip", Configuration.gameSummaryImagesBVDir())));
        comps.add(checkboxEntry(gameSummaryMM,
                Messages.getString("CommonSettingsDialog.gameSummaryMM.tooltip", Configuration.gameSummaryImagesMMDir())));

        addLineSpacer(comps);
        addSpacer(comps, 3);

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

        addSpacer(comps, 5);

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
        
        // Highlighting Radius inside FoV
        comps.add(checkboxEntry(fovInsideEnabled, null));

        // Add some vertical spacing
        row = new ArrayList<>();
        row.add(Box.createVerticalStrut(2));
        comps.add(row);

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
        fovHighlightRingsRadii.setMaximumSize(new Dimension(100, 40 ));
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
        var choiceLabel = new JLabel(
                "<HTML><CENTER>This will enable stepping through the entry fields on this page using the TAB key." +
                        "<BR> It will prevent TAB from being used as a keybind and " +
                        "<BR> remove any existing TAB keybinds.");
        buttonPanel.add(choiceToggle);
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
        headers.setToolTipText("The name of the action");
        keyBinds.add(headers, gbc);
        gbc.gridx++;
        headers = new JLabel("Modifier");
        headers.setToolTipText("The modifier key, like shift, ctrl, alt");
        keyBinds.add(headers, gbc);
        gbc.gridx++;
        headers = new JLabel("Key");
        headers.setToolTipText("The key");
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
        JPanel outer = new JPanel();
        outer.setLayout(new BoxLayout(outer, BoxLayout.PAGE_AXIS));

        JLabel unitDisplayLabel = new JLabel(MSG_UNITDISPLAY);
        row = new ArrayList<>();
        row.add(unitDisplayLabel);
        comps.add(row);

        JLabel phaseLabel = new JLabel(MSG_REPORTPHASES + ": ");
        unitDisplayAutoDisplayReportCombo = new JComboBox<>();
        unitDisplayAutoDisplayReportCombo.addItem(MSG_SHOW);
        unitDisplayAutoDisplayReportCombo.addItem(MSG_HIDE);
        unitDisplayAutoDisplayReportCombo.addItem(MSG_MANUAL);
        unitDisplayAutoDisplayReportCombo.setMaximumSize(new Dimension(150, 40));
        row = new ArrayList<>();
        unitDisplayAutoDisplayReportCombo.setSelectedItem(GUIP.getUnitDisplayAutoDisplayReportPhase());
        row.add(phaseLabel);
        row.add(unitDisplayAutoDisplayReportCombo);
        comps.add(row);

        phaseLabel = new JLabel(MSG_NONREPORTPHASES + ": ");
        unitDisplayAutoDisplayNonReportCombo = new JComboBox<>();
        unitDisplayAutoDisplayNonReportCombo.addItem(MSG_SHOW);
        unitDisplayAutoDisplayNonReportCombo.addItem(MSG_HIDE);
        unitDisplayAutoDisplayNonReportCombo.addItem(MSG_MANUAL);
        unitDisplayAutoDisplayNonReportCombo.setMaximumSize(new Dimension(150, 40));
        row = new ArrayList<>();
        unitDisplayAutoDisplayNonReportCombo.setSelectedItem(GUIP.getUnitDisplayAutoDisplayNonReportPhase());
        row.add(phaseLabel);
        row.add(unitDisplayAutoDisplayNonReportCombo);
        comps.add(row);

        addLineSpacer(comps);

        JLabel miniMapLabel = new JLabel(MSG_MINIMAP);
        row = new ArrayList<>();
        row.add(miniMapLabel);
        comps.add(row);

        phaseLabel = new JLabel(MSG_REPORTPHASES + ": ");
        miniMapAutoDisplayReportCombo = new JComboBox<>();
        miniMapAutoDisplayReportCombo.addItem(MSG_SHOW);
        miniMapAutoDisplayReportCombo.addItem(MSG_HIDE);
        miniMapAutoDisplayReportCombo.addItem(MSG_MANUAL);
        miniMapAutoDisplayReportCombo.setMaximumSize(new Dimension(150, 40));
        row = new ArrayList<>();
        miniMapAutoDisplayReportCombo.setSelectedItem(GUIP.getMinimapAutoDisplayReportPhase());
        row.add(phaseLabel);
        row.add(miniMapAutoDisplayReportCombo);
        comps.add(row);

        phaseLabel = new JLabel(MSG_NONREPORTPHASES + ": ");
        miniMapAutoDisplayNonReportCombo = new JComboBox<>();
        miniMapAutoDisplayNonReportCombo.addItem(MSG_SHOW);
        miniMapAutoDisplayNonReportCombo.addItem(MSG_HIDE);
        miniMapAutoDisplayNonReportCombo.addItem(MSG_MANUAL);
        miniMapAutoDisplayNonReportCombo.setMaximumSize(new Dimension(150, 40));
        row = new ArrayList<>();
        miniMapAutoDisplayNonReportCombo.setSelectedItem(GUIP.getMinimapAutoDisplayNonReportPhase());
        row.add(phaseLabel);
        row.add(miniMapAutoDisplayNonReportCombo);
        comps.add(row);

        addLineSpacer(comps);

        JLabel miniReportLabel = new JLabel(MSG_MINIREPORT);
        row = new ArrayList<>();
        row.add(miniReportLabel);
        comps.add(row);

        phaseLabel = new JLabel(MSG_REPORTPHASES + ": ");
        miniReportAutoDisplayReportCombo = new JComboBox<>();
        miniReportAutoDisplayReportCombo.addItem(MSG_SHOW);
        miniReportAutoDisplayReportCombo.addItem(MSG_HIDE);
        miniReportAutoDisplayReportCombo.addItem(MSG_MANUAL);
        miniReportAutoDisplayReportCombo.setMaximumSize(new Dimension(150, 40));
        row = new ArrayList<>();
        miniReportAutoDisplayReportCombo.setSelectedItem(GUIP.getMiniReportAutoDisplayReportPhase());
        row.add(phaseLabel);
        row.add(miniReportAutoDisplayReportCombo);
        comps.add(row);

        phaseLabel = new JLabel(MSG_NONREPORTPHASES + ": ");
        miniReportAutoDisplayNonReportCombo = new JComboBox<>();
        miniReportAutoDisplayNonReportCombo.addItem(MSG_SHOW);
        miniReportAutoDisplayNonReportCombo.addItem(MSG_HIDE);
        miniReportAutoDisplayNonReportCombo.addItem(MSG_MANUAL);
        miniReportAutoDisplayNonReportCombo.setMaximumSize(new Dimension(150, 40));
        row = new ArrayList<>();
        miniReportAutoDisplayNonReportCombo.setSelectedItem(GUIP.getMiniReportAutoDisplayNonReportPhase());
        row.add(phaseLabel);
        row.add(miniReportAutoDisplayNonReportCombo);
        comps.add(row);

        addLineSpacer(comps);

        JLabel playerListLabel = new JLabel(MSG_PLAYERLIST);
        row = new ArrayList<>();
        row.add(playerListLabel);
        comps.add(row);

        phaseLabel = new JLabel(MSG_REPORTPHASES + ": ");
        playerListAutoDisplayReportCombo = new JComboBox<>();
        playerListAutoDisplayReportCombo.addItem(MSG_SHOW);
        playerListAutoDisplayReportCombo.addItem(MSG_HIDE);
        playerListAutoDisplayReportCombo.addItem(MSG_MANUAL);
        playerListAutoDisplayReportCombo.setMaximumSize(new Dimension(150, 40));
        row = new ArrayList<>();
        playerListAutoDisplayReportCombo.setSelectedItem(GUIP.getPlayerListAutoDisplayReportPhase());
        row.add(phaseLabel);
        row.add(playerListAutoDisplayReportCombo);
        comps.add(row);

        phaseLabel = new JLabel(MSG_NONREPORTPHASES + ": ");
        playerListAutoDisplayNonReportCombo = new JComboBox<>();
        playerListAutoDisplayNonReportCombo.addItem(MSG_SHOW);
        playerListAutoDisplayNonReportCombo.addItem(MSG_HIDE);
        playerListAutoDisplayNonReportCombo.addItem(MSG_MANUAL);
        playerListAutoDisplayNonReportCombo.setMaximumSize(new Dimension(150, 40));
        row = new ArrayList<>();
        playerListAutoDisplayNonReportCombo.setSelectedItem(GUIP.getPlayerListAutoDisplayNonReportPhase());
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
        JList<StatusBarPhaseDisplay.PhaseCommand> jlist = new JList<>(list);
        jlist.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jlist.addMouseListener(cmdMouseAdaptor);
        jlist.addMouseMotionListener(cmdMouseAdaptor);
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
