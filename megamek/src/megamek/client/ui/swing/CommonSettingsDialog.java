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

import megamek.client.ui.Messages;
import megamek.client.ui.baseComponents.AbstractButtonDialog;
import megamek.client.ui.baseComponents.MMComboBox;
import megamek.client.ui.swing.StatusBarPhaseDisplay.PhaseCommand;
import megamek.client.ui.swing.unitDisplay.UnitDisplay;
import megamek.client.ui.swing.util.KeyCommandBind;
import megamek.client.ui.swing.widget.SkinXMLHandler;
import megamek.common.Configuration;
import megamek.common.KeyBindParser;
import megamek.common.enums.GamePhase;
import megamek.common.enums.WeaponSortOrder;
import megamek.common.preference.ClientPreferences;
import megamek.common.preference.PreferenceManager;

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

    /** Maps command strings to a JTextField for updating the modifier for the command. */
    private Map<String, JTextField> cmdModifierMap;

    /** Maps command strings to a JTextField for updating the key for the command. */
    private Map<String, JTextField> cmdKeyMap;
    
    /** Maps command strings to a Integer for updating the key for the command. */
    private Map<String, Integer> cmdKeyCodeMap; 

    private ClientGUI clientgui = null;

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


        panTabs.add("Main", settingsPane);
        panTabs.add("Graphics", graphicsPane);
        panTabs.add("Key Binds", keyBindPane);
        panTabs.add("Button Order", getButtonOrderPanel());
        panTabs.add("Advanced", advancedSettingsPane);
        panTabs.add("Unit Display Order", unitDisplayPane);

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

        unitDisplayNonTabbed.addElement(UnitDisplayOrderPreferences.getInstance().getString(UnitDisplay.NON_TABBED_A1));
        unitDisplayNonTabbed.addElement(UnitDisplayOrderPreferences.getInstance().getString(UnitDisplay.NON_TABBED_B1));
        unitDisplayNonTabbed.addElement(UnitDisplayOrderPreferences.getInstance().getString(UnitDisplay.NON_TABBED_C1));
        unitDisplayNonTabbed.addElement(UnitDisplayOrderPreferences.getInstance().getString(UnitDisplay.NON_TABBED_A2));
        unitDisplayNonTabbed.addElement(UnitDisplayOrderPreferences.getInstance().getString(UnitDisplay.NON_TABBED_B2));
        unitDisplayNonTabbed.addElement(UnitDisplayOrderPreferences.getInstance().getString(UnitDisplay.NON_TABBED_C2));

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
            GUIPreferences gs = GUIPreferences.getInstance();
            ClientPreferences cs = PreferenceManager.getClientPreferences();

            guiScale.setValue((int) (gs.getGUIScale() * 10));
            autoEndFiring.setSelected(gs.getAutoEndFiring());
            autoDeclareSearchlight.setSelected(gs.getAutoDeclareSearchlight());
            nagForMASC.setSelected(gs.getNagForMASC());
            nagForPSR.setSelected(gs.getNagForPSR());
            nagForWiGELanding.setSelected(gs.getNagForWiGELanding());
            nagForNoAction.setSelected(gs.getNagForNoAction());
            animateMove.setSelected(gs.getShowMoveStep());
            showWrecks.setSelected(gs.getShowWrecks());
            soundMute.setSelected(gs.getSoundMute());
            tooltipDelay.setText(Integer.toString(gs.getTooltipDelay()));
            tooltipDismissDelay.setText(Integer.toString(gs.getTooltipDismissDelay()));
            tooltipDistSupression.setText(Integer.toString(gs.getTooltipDistSuppression()));
            showWpsinTT.setSelected(gs.getShowWpsinTT());
            showArmorMiniVisTT.setSelected(gs.getshowArmorMiniVisTT());
            showPilotPortraitTT.setSelected(gs.getshowPilotPortraitTT());
            comboDefaultWeaponSortOrder.setSelectedItem(gs.getDefaultWeaponSortOrder());
            mouseWheelZoom.setSelected(gs.getMouseWheelZoom());
            mouseWheelZoomFlip.setSelected(gs.getMouseWheelZoomFlip());

            // Select the correct char set (give a nice default to start).
            unitStartChar.setSelectedIndex(0);
            for (int loop = 0; loop < unitStartChar.getItemCount(); loop++) {
                if (unitStartChar.getItemAt(loop).charAt(0) == PreferenceManager
                        .getClientPreferences().getUnitStartChar()) {
                    unitStartChar.setSelectedIndex(loop);
                    break;
                }
            }

            maxPathfinderTime.setText(Integer.toString(cs.getMaxPathfinderTime()));

            keepGameLog.setSelected(cs.keepGameLog());
            gameLogFilename.setEnabled(keepGameLog.isSelected());
            gameLogFilename.setText(cs.getGameLogFilename());
            // gameLogMaxSize.setEnabled(keepGameLog.isSelected());
            // gameLogMaxSize.setText( Integer.toString(cs.getGameLogMaxSize()) );
            stampFilenames.setSelected(cs.stampFilenames());
            stampFormat.setEnabled(stampFilenames.isSelected());
            stampFormat.setText(cs.getStampFormat());
            showIPAddressesInChat.setSelected(cs.getShowIPAddressesInChat());

            defaultAutoejectDisabled.setSelected(cs.defaultAutoejectDisabled());
            useAverageSkills.setSelected(cs.useAverageSkills());
            generateNames.setSelected(cs.generateNames());
            showUnitId.setSelected(cs.getShowUnitId());

            int index = 0;
            if (cs.getLocaleString().startsWith("de")) {
                index = 1;
            }
            if (cs.getLocaleString().startsWith("ru")) {
                index = 2;
            }
            displayLocale.setSelectedIndex(index);

            showMapsheets.setSelected(gs.getShowMapsheets());
            chkAntiAliasing.setSelected(gs.getAntiAliasing());
            showDamageLevel.setSelected(gs.getShowDamageLevel());
            showDamageDecal.setSelected(gs.getShowDamageDecal());
            aOHexShadows.setSelected(gs.getAOHexShadows());
            floatingIso.setSelected(gs.getFloatingIso());
            mmSymbol.setSelected(gs.getMmSymbol());
            levelhighlight.setSelected(gs.getLevelHighlight());
            shadowMap.setSelected(gs.getShadowMap());
            hexInclines.setSelected(gs.getHexInclines());
            useSoftCenter.setSelected(gs.getBoolean("SOFTCENTER"));
            entityOwnerColor.setSelected(gs.getUnitLabelBorder());
            teamColoring.setSelected(gs.getTeamColoring());

            File dir = Configuration.hexesDir();
            tileSets = new ArrayList<>(Arrays.asList(dir.list((direc, name) -> name.endsWith(".tileset"))));
            tileSets.addAll(userDataFiles(Configuration.hexesDir(), ".tileset"));
            tileSetChoice.removeAllItems();
            for (int i = 0; i < tileSets.size(); i++) {
                String name = tileSets.get(i);
                tileSetChoice.addItem(name.substring(0, name.length() - 8));
                if (name.equals(cs.getMapTileset())) {
                    tileSetChoice.setSelectedIndex(i);
                }
            }

            gameSummaryBV.setSelected(gs.getGameSummaryBoardView());
            gameSummaryMM.setSelected(gs.getGameSummaryMinimap());

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
            skinFiles.setSelectedItem(GUIPreferences.getInstance().getSkinFile());

            uiThemes.removeAllItems();
            for (LookAndFeelInfo lafInfo : UIManager.getInstalledLookAndFeels()) {
                uiThemes.addItem(new UITheme(lafInfo.getClassName(), lafInfo.getName()));
            }
            uiThemes.setSelectedItem(new UITheme(GUIPreferences.getInstance().getUITheme()));

            fovInsideEnabled.setSelected(gs.getFovHighlight());
            fovHighlightAlpha.setValue(gs.getFovHighlightAlpha());
            fovHighlightRingsRadii.setText( gs.getFovHighlightRingsRadii());
            fovHighlightRingsColors.setText( gs.getFovHighlightRingsColorsHsb() );
            fovOutsideEnabled.setSelected(gs.getFovDarken());
            fovDarkenAlpha.setValue(gs.getFovDarkenAlpha());
            numStripesSlider.setValue(gs.getFovStripes());
            fovGrayscaleEnabled.setSelected(gs.getFovGrayscale());

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

            getFocus.setSelected(gs.getFocus());
            
            savedFovHighlight = gs.getFovHighlight();
            savedFovDarken = gs.getFovDarken();
            savedFovGrayscale = gs.getFovGrayscale();
            savedAOHexShadows = gs.getAOHexShadows();
            savedShadowMap = gs.getShadowMap();
            savedHexInclines = gs.getHexInclines();
            savedLevelhighlight = gs.getLevelHighlight();
            savedFloatingIso = gs.getFloatingIso();
            savedMmSymbol = gs.getMmSymbol();
            savedTeamColoring = gs.getTeamColoring();
            savedUnitLabelBorder = gs.getUnitLabelBorder();
            savedShowDamageDecal = gs.getShowDamageDecal();
            savedShowDamageLabel = gs.getShowDamageLevel();
            savedFovHighlightRingsRadii = gs.getFovHighlightRingsRadii();
            savedFovHighlightRingsColors = gs.getFovHighlightRingsColorsHsb();
            savedFovHighlightAlpha = gs.getFovHighlightAlpha();
            savedFovDarkenAlpha = gs.getFovDarkenAlpha();
            savedNumStripesSlider = gs.getFovStripes();
            savedAntiAlias = gs.getAntiAliasing();
            savedAdvancedOpt.clear();
            
            advancedKeys.clearSelection();
            
            for (KeyCommandBind kcb : KeyCommandBind.values()) {
                cmdModifierMap.get(kcb.cmd).setText(KeyEvent.getModifiersExText(kcb.modifiers));
                cmdKeyMap.get(kcb.cmd).setText(KeyEvent.getKeyText(kcb.key));
            }
            markDuplicateBinds();
            
        }   
        super.setVisible(visible);
    }

    /** Cancels any updates made in this dialog and closes it.  */
    @Override
    protected void cancelAction() {
        GUIPreferences guip = GUIPreferences.getInstance();
        guip.setFovHighlight(savedFovHighlight);
        guip.setFovDarken(savedFovDarken);
        guip.setFovGrayscale(savedFovGrayscale);
        guip.setAOHexShadows(savedAOHexShadows);
        guip.setShadowMap(savedShadowMap);
        guip.setHexInclines(savedHexInclines);
        guip.setLevelHighlight(savedLevelhighlight);
        guip.setFloatingIso(savedFloatingIso);
        guip.setMmSymbol(savedMmSymbol);
        guip.setTeamColoring(savedTeamColoring);
        guip.setUnitLabelBorder(savedUnitLabelBorder);
        guip.setShowDamageDecal(savedShowDamageDecal);
        guip.setShowDamageLevel(savedShowDamageLabel);
        guip.setFovHighlightRingsRadii(savedFovHighlightRingsRadii);
        guip.setFovHighlightRingsColorsHsb(savedFovHighlightRingsColors);
        guip.setFovHighlightAlpha(savedFovHighlightAlpha);
        guip.setFovDarkenAlpha(savedFovDarkenAlpha);
        guip.setFovStripes(savedNumStripesSlider);
        guip.setAntiAliasing(savedAntiAlias);
         
        for (String option: savedAdvancedOpt.keySet()) {
            GUIPreferences.getInstance().setValue(option, savedAdvancedOpt.get(option));
        }

        unitDisplayNonTabbed.clear();
        unitDisplayNonTabbed.addElement(UnitDisplayOrderPreferences.getInstance().getString(UnitDisplay.NON_TABBED_A1));
        unitDisplayNonTabbed.addElement(UnitDisplayOrderPreferences.getInstance().getString(UnitDisplay.NON_TABBED_B1));
        unitDisplayNonTabbed.addElement(UnitDisplayOrderPreferences.getInstance().getString(UnitDisplay.NON_TABBED_C1));
        unitDisplayNonTabbed.addElement(UnitDisplayOrderPreferences.getInstance().getString(UnitDisplay.NON_TABBED_A2));
        unitDisplayNonTabbed.addElement(UnitDisplayOrderPreferences.getInstance().getString(UnitDisplay.NON_TABBED_B2));
        unitDisplayNonTabbed.addElement(UnitDisplayOrderPreferences.getInstance().getString(UnitDisplay.NON_TABBED_C2));

        setVisible(false);
    }

    /** Update the settings from this dialog's values, then close it. */
    @Override
    protected void okAction() {
        GUIPreferences gs = GUIPreferences.getInstance();
        ClientPreferences cs = PreferenceManager.getClientPreferences();

        gs.setShowDamageLevel(showDamageLevel.isSelected());
        gs.setShowDamageDecal(showDamageDecal.isSelected());
        gs.setUnitLabelBorder(entityOwnerColor.isSelected());
        gs.setTeamColoring(teamColoring.isSelected());
        gs.setAutoEndFiring(autoEndFiring.isSelected());
        gs.setAutoDeclareSearchlight(autoDeclareSearchlight.isSelected());
        gs.setDefaultWeaponSortOrder(Objects.requireNonNull(comboDefaultWeaponSortOrder.getSelectedItem()));
        gs.setNagForMASC(nagForMASC.isSelected());
        gs.setNagForPSR(nagForPSR.isSelected());
        gs.setNagForWiGELanding(nagForWiGELanding.isSelected());
        gs.setNagForNoAction(nagForNoAction.isSelected());
        gs.setShowMoveStep(animateMove.isSelected());
        gs.setShowWrecks(showWrecks.isSelected());
        gs.setSoundMute(soundMute.isSelected());
        gs.setShowWpsinTT(showWpsinTT.isSelected());
        gs.setshowArmorMiniVisTT(showArmorMiniVisTT.isSelected());
        gs.setshowPilotPortraitTT(showPilotPortraitTT.isSelected());
        gs.setTooltipDelay(Integer.parseInt(tooltipDelay.getText()));
        gs.setTooltipDismissDelay(Integer.parseInt(tooltipDismissDelay.getText()));
        gs.setTooltipDistSuppression(Integer.parseInt(tooltipDistSupression.getText()));
        gs.setValue(GUIPreferences.GUI_SCALE, (float) (guiScale.getValue()) / 10);
        cs.setUnitStartChar(((String) unitStartChar.getSelectedItem()).charAt(0));
        
        gs.setMouseWheelZoom(mouseWheelZoom.isSelected());
        gs.setMouseWheelZoomFlip(mouseWheelZoomFlip.isSelected());

        cs.setMaxPathfinderTime(Integer.parseInt(maxPathfinderTime.getText()));

        gs.setGetFocus(getFocus.isSelected());

        cs.setKeepGameLog(keepGameLog.isSelected());
        cs.setGameLogFilename(gameLogFilename.getText());
        // cs.setGameLogMaxSize(Integer.parseInt(gameLogMaxSize.getText()));
        cs.setStampFilenames(stampFilenames.isSelected());
        cs.setStampFormat(stampFormat.getText());
        cs.setShowIPAddressesInChat(showIPAddressesInChat.isSelected());

        cs.setDefaultAutoejectDisabled(defaultAutoejectDisabled.isSelected());
        cs.setUseAverageSkills(useAverageSkills.isSelected());
        cs.setGenerateNames(generateNames.isSelected());
        cs.setShowUnitId(showUnitId.isSelected());
        if ((clientgui != null) && (clientgui.getBoardView() != null)) {
            clientgui.getBoardView().updateEntityLabels();
        }

        cs.setLocale(CommonSettingsDialog.LOCALE_CHOICES[displayLocale.getSelectedIndex()]);

        gs.setShowMapsheets(showMapsheets.isSelected());
        gs.setAOHexShadows(aOHexShadows.isSelected());
        gs.setFloatingIso(floatingIso.isSelected());
        gs.setMmSymbol(mmSymbol.isSelected());
        gs.setLevelHighlight(levelhighlight.isSelected());
        gs.setShadowMap(shadowMap.isSelected());
        gs.setHexInclines(hexInclines.isSelected());
        gs.setValue("SOFTCENTER", useSoftCenter.isSelected());
        gs.setGameSummaryBoardView(gameSummaryBV.isSelected());
        gs.setGameSummaryMinimap(gameSummaryMM.isSelected());

        UITheme newUITheme = (UITheme) uiThemes.getSelectedItem();
        String oldUITheme = gs.getUITheme();
        if (!oldUITheme.equals(newUITheme.getClassName())) {
            gs.setUITheme(newUITheme.getClassName());
        }

        String newSkinFile = (String) skinFiles.getSelectedItem();
        String oldSkinFile = gs.getSkinFile();
        if (!oldSkinFile.equals(newSkinFile)) {
            boolean success = SkinXMLHandler.initSkinXMLHandler(newSkinFile);
            if (!success) {
                SkinXMLHandler.initSkinXMLHandler(oldSkinFile);
                String title = Messages.getString("CommonSettingsDialog.skinFileFail.title");
                String msg = Messages.getString("CommonSettingsDialog.skinFileFail.msg");
                JOptionPane.showMessageDialog(getFrame(), msg, title, JOptionPane.ERROR_MESSAGE);
            } else {
                gs.setSkinFile(newSkinFile);
            }            
        }

        if (tileSetChoice.getSelectedIndex() >= 0) {
            String tileSetFileName = tileSets.get(tileSetChoice.getSelectedIndex());
            if (!cs.getMapTileset().equals(tileSetFileName) &&
                    (clientgui != null) && (clientgui.getBoardView() != null))  {
                clientgui.getBoardView().clearShadowMap();
            }
            cs.setMapTileset(tileSetFileName);
        }

        ToolTipManager.sharedInstance().setInitialDelay(gs.getTooltipDelay());
        if (gs.getTooltipDismissDelay() > 0) {
            ToolTipManager.sharedInstance().setDismissDelay(gs.getTooltipDismissDelay());
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
        ButtonOrderPreferences bop = ButtonOrderPreferences.getInstance();
        boolean buttonOrderChanged = false;
        for (int i = 0; i < movePhaseCommands.getSize(); i++) {
            StatusBarPhaseDisplay.PhaseCommand cmd = movePhaseCommands.get(i);
            if (cmd.getPriority() != i) {
                cmd.setPriority(i);
                bop.setValue(cmd.getCmd(), i);
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
                bop.setValue(cmd.getCmd(), i);
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
                bop.setValue(cmd.getCmd(), i);
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
                bop.setValue(cmd.getCmd(), i);
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
                bop.setValue(cmd.getCmd(), i);
                buttonOrderChanged = true;
            }
        }
        
        // Need to do stuff if the order changes.
        if (buttonOrderChanged && (clientgui != null)) {
            clientgui.updateButtonPanel(GamePhase.TARGETING);
        }

        // unit display non tabbed
        if (!GUIPreferences.getInstance().getDisplayStartTabbed()) {
            boolean unitDisplayNonTabbedChanged = false;
            int s = unitDisplayNonTabbed.getSize();

            if ((s > UnitDisplay.NON_TABBED_ZERO_INDEX) && (!unitDisplayNonTabbed.get(UnitDisplay.NON_TABBED_ZERO_INDEX).equals(UnitDisplayOrderPreferences.getInstance().getString(UnitDisplay.NON_TABBED_A1)))) {
                unitDisplayNonTabbedChanged = true;
                UnitDisplayOrderPreferences.getInstance().setValue(UnitDisplay.NON_TABBED_A1,  unitDisplayNonTabbed.get(UnitDisplay.NON_TABBED_ZERO_INDEX));
            }
            if ((s > UnitDisplay.NON_TABBED_ONE_INDEX) && (!unitDisplayNonTabbed.get(UnitDisplay.NON_TABBED_ONE_INDEX).equals(UnitDisplayOrderPreferences.getInstance().getString(UnitDisplay.NON_TABBED_B1)))) {
                unitDisplayNonTabbedChanged = true;
                UnitDisplayOrderPreferences.getInstance().setValue(UnitDisplay.NON_TABBED_B1,  unitDisplayNonTabbed.get(UnitDisplay.NON_TABBED_ONE_INDEX));
            }
            if ((s > UnitDisplay.NON_TABBED_TWO_INDEX) && (!unitDisplayNonTabbed.get(UnitDisplay.NON_TABBED_TWO_INDEX).equals( UnitDisplayOrderPreferences.getInstance().getString(UnitDisplay.NON_TABBED_C1)))) {
                unitDisplayNonTabbedChanged = true;
                UnitDisplayOrderPreferences.getInstance().setValue(UnitDisplay.NON_TABBED_C1,  unitDisplayNonTabbed.get(UnitDisplay.NON_TABBED_TWO_INDEX));
            }
            if ((s > UnitDisplay.NON_TABBED_THREE_INDEX) && (!unitDisplayNonTabbed.get(UnitDisplay.NON_TABBED_THREE_INDEX).equals(UnitDisplayOrderPreferences.getInstance().getString(UnitDisplay.NON_TABBED_A2)))) {
                unitDisplayNonTabbedChanged = true;
                UnitDisplayOrderPreferences.getInstance().setValue(UnitDisplay.NON_TABBED_A2,  unitDisplayNonTabbed.get(UnitDisplay.NON_TABBED_THREE_INDEX));
            }
            if ((s > UnitDisplay.NON_TABBED_FOUR_INDEX) && (!unitDisplayNonTabbed.get(UnitDisplay.NON_TABBED_FOUR_INDEX).equals(UnitDisplayOrderPreferences.getInstance().getString(UnitDisplay.NON_TABBED_B2)))) {
                unitDisplayNonTabbedChanged = true;
                UnitDisplayOrderPreferences.getInstance().setValue(UnitDisplay.NON_TABBED_B2,  unitDisplayNonTabbed.get(UnitDisplay.NON_TABBED_FOUR_INDEX));
            }
            if ((s > UnitDisplay.NON_TABBED_FIVE_INDEX) && (!unitDisplayNonTabbed.get(UnitDisplay.NON_TABBED_FIVE_INDEX).equals( UnitDisplayOrderPreferences.getInstance().getString(UnitDisplay.NON_TABBED_C2)))) {
                unitDisplayNonTabbedChanged = true;
                UnitDisplayOrderPreferences.getInstance().setValue(UnitDisplay.NON_TABBED_C2,  unitDisplayNonTabbed.get(UnitDisplay.NON_TABBED_FIVE_INDEX));
            }

            if ((unitDisplayNonTabbedChanged) && (clientgui != null)) {
                clientgui.unitDisplay.setDisplayNonTabbed();
            }
        }

        setVisible(false);
    }

    /** Handle some setting changes that directly update e.g. the board. */
    @Override
    public void itemStateChanged(ItemEvent event) {
        Object source = event.getItemSelectable();
        GUIPreferences guip = GUIPreferences.getInstance();
        if (source.equals(keepGameLog)) {
            gameLogFilename.setEnabled(keepGameLog.isSelected());
            stampFormatLabel.setEnabled(stampFilenames.isSelected());
            gameLogFilenameLabel.setEnabled(keepGameLog.isSelected());
            // gameLogMaxSize.setEnabled(keepGameLog.isSelected());
        } else if (source.equals(stampFilenames)) {
            stampFormat.setEnabled(stampFilenames.isSelected());
            stampFormatLabel.setEnabled(stampFilenames.isSelected());
        } else if (source.equals(fovInsideEnabled)) {
            guip.setFovHighlight(fovInsideEnabled.isSelected());
            fovHighlightAlpha.setEnabled(fovInsideEnabled.isSelected());
            fovHighlightRingsRadii.setEnabled(fovInsideEnabled.isSelected());
            fovHighlightRingsColors.setEnabled(fovInsideEnabled.isSelected());
            fovHighlightRingsColorsLabel.setEnabled(fovInsideEnabled.isSelected());
            fovHighlightRingsRadiiLabel.setEnabled(fovInsideEnabled.isSelected());
            highlightAlphaLabel.setEnabled(fovInsideEnabled.isSelected());
        } else if (source.equals(fovOutsideEnabled)) {
            guip.setFovDarken(fovOutsideEnabled.isSelected());
            fovDarkenAlpha.setEnabled(fovOutsideEnabled.isSelected());
            numStripesSlider.setEnabled(fovOutsideEnabled.isSelected());
            darkenAlphaLabel.setEnabled(fovOutsideEnabled.isSelected());
            numStripesLabel.setEnabled(fovOutsideEnabled.isSelected());
            fovGrayscaleEnabled.setEnabled(fovOutsideEnabled.isSelected());
        } else if (source.equals(fovGrayscaleEnabled)) {
            guip.setFovGrayscale(fovGrayscaleEnabled.isSelected());
        } else if (source.equals(aOHexShadows)) {
            guip.setAOHexShadows(aOHexShadows.isSelected());
        } else if (source.equals(shadowMap)) {
            guip.setShadowMap(shadowMap.isSelected());
        } else if (source.equals(hexInclines)) {
            guip.setHexInclines(hexInclines.isSelected());
        } else if (source.equals(levelhighlight)) {
            guip.setLevelHighlight(levelhighlight.isSelected());
        } else if (source.equals(floatingIso)) {
            guip.setFloatingIso(floatingIso.isSelected());
        } else if (source.equals(mmSymbol)) {
            guip.setMmSymbol(mmSymbol.isSelected());
        } else if (source.equals(teamColoring)) {
            guip.setTeamColoring(teamColoring.isSelected());
        } else if (source.equals(entityOwnerColor)) {
            guip.setUnitLabelBorder(entityOwnerColor.isSelected());
        } else if (source.equals(showDamageDecal)) {
            guip.setShowDamageDecal(showDamageDecal.isSelected());
        } else if (source.equals(showDamageLevel)) {
            guip.setShowDamageLevel(showDamageLevel.isSelected());
        } else if (source.equals(chkAntiAliasing)) {
            guip.setAntiAliasing(chkAntiAliasing.isSelected());
        }
    }

    @Override
    public void focusGained(FocusEvent e) { }

    @Override
    public void focusLost(FocusEvent e) {
        Object src = e.getSource();
        GUIPreferences guip = GUIPreferences.getInstance();          
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
        fovHighlightAlpha.setMaximumSize(new Dimension(400, 100));
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
        fovHighlightRingsRadii.setMaximumSize(new Dimension(100, fovHighlightRingsRadii.getPreferredSize().height) );
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
        fovHighlightRingsColors.setMaximumSize(new Dimension(200, fovHighlightRingsColors.getPreferredSize().height) );
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
        fovDarkenAlpha.setMaximumSize(new Dimension(400, 100));
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
        numStripesSlider.setMaximumSize(new Dimension(250, 100));
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
        field.setForeground(errorMsg != null ? GUIPreferences.getInstance().getWarningColor() : null);
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

        String[] s = GUIPreferences.getInstance().getAdvancedProperties();
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
        advancedValue.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
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
            advancedValue.setText(GUIPreferences.getInstance().getString(
                    "Advanced" + advancedKeys.getSelectedValue().option));
            advancedKeyIndex = advancedKeys.getSelectedIndex();
        }
    }

    @Override
    public void stateChanged(ChangeEvent evt) {
        GUIPreferences guip = GUIPreferences.getInstance();
        if (evt.getSource().equals(fovHighlightAlpha)) {
            guip.setFovHighlightAlpha(Math.max(0, Math.min(255, fovHighlightAlpha.getValue())));
        } else if (evt.getSource().equals(fovDarkenAlpha)) {
            guip.setFovDarkenAlpha(Math.max(0, Math.min(255, fovDarkenAlpha.getValue())));
        } else if (evt.getSource().equals(numStripesSlider)) {
            guip.setFovStripes(numStripesSlider.getValue());
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
}
