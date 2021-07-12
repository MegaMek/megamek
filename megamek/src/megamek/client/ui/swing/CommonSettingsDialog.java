/*
 * MegaMek - Copyright (C) 2003, 2004, 2005 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */

package megamek.client.ui.swing;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.MouseInputAdapter;

import megamek.client.ui.Messages;
import megamek.client.ui.swing.StatusBarPhaseDisplay.PhaseCommand;
import megamek.client.ui.swing.util.KeyCommandBind;
import megamek.client.ui.swing.widget.SkinXMLHandler;
import megamek.common.Configuration;
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.KeyBindParser;
import megamek.common.preference.IClientPreferences;
import megamek.common.preference.PreferenceManager;

public class CommonSettingsDialog extends ClientDialog implements
        ActionListener, ItemListener, FocusListener, ListSelectionListener,
        ChangeListener {

    /**
     * A class for storing information about an GUIPreferences advanced option.
     *
     * @author arlith
     *
     */
    private class AdvancedOptionData implements Comparable<AdvancedOptionData> {

        public String option;

        public AdvancedOptionData(String option) {
            this.option = option;
        }

        /**
         * Returns true if this option has tooltip text.
         *
         * @return
         */
        public boolean hasTooltipText() {
            return Messages.keyExists("AdvancedOptions." + option + ".tooltip");
        }

        /**
         * Returns the tooltip text for this option.
         *
         * @return
         */
        public String getTooltipText() {
            return Messages.getString("AdvancedOptions." + option + ".tooltip");
        }

        /**
         * Returns a human-readable name for this advanced option.
         *
         */
        public String toString() {
            if (Messages.keyExists("AdvancedOptions." + option + ".name")) {
                return Messages.getString("AdvancedOptions." + option + ".name");
            } else {
                return option;
            }
        }

        @Override
        public int compareTo(AdvancedOptionData other) {
            return this.toString().compareTo(other.toString());
        }
    }

    private class PhaseCommandListMouseAdapter extends MouseInputAdapter {
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
    
    /**
     *
     */
    private static final long serialVersionUID = 1535370193846895473L;

    private JCheckBox autoEndFiring;
    private JCheckBox autoDeclareSearchlight;
    private JCheckBox nagForMASC;
    private JCheckBox nagForPSR;
    private JCheckBox nagForWiGELanding;
    private JCheckBox nagForNoAction;
    private JCheckBox animateMove;
    private JCheckBox showWrecks;
    private JCheckBox soundMute;
    private JCheckBox showWpsinTT;
    private JCheckBox showArmorMiniVisTT;
    private JCheckBox showPilotPortraitTT;
    private JCheckBox chkAntiAliasing;
    private JComboBox<String> defaultWeaponSortOrder;
    private JTextField tooltipDelay;
    private JTextField tooltipDismissDelay;
    private JTextField tooltipDistSupression;
    private JComboBox<String> unitStartChar;
    private JTextField maxPathfinderTime;
    private JCheckBox getFocus;
    private JSlider guiScale;

    private JCheckBox keepGameLog;
    private JTextField gameLogFilename;
    // private JTextField gameLogMaxSize;
    private JCheckBox stampFilenames;
    private JTextField stampFormat;
    private JCheckBox defaultAutoejectDisabled;
    private JCheckBox useAverageSkills;
    private JCheckBox generateNames;
    private JCheckBox showUnitId;
    private JComboBox<String> displayLocale;
    private JCheckBox showIPAddressesInChat;

    private JCheckBox showDamageLevel;
    private JCheckBox showDamageDecal;
    private JCheckBox showMapsheets;
    private JCheckBox aOHexShadows;
    private JCheckBox floatingIso;
    private JCheckBox mmSymbol;
    private JCheckBox entityOwnerColor;
    private JCheckBox teamColoring;
    private JCheckBox useSoftCenter;
    private JCheckBox levelhighlight;
    private JCheckBox shadowMap;
    private JCheckBox hexInclines;
    private JCheckBox mouseWheelZoom;
    private JCheckBox mouseWheelZoomFlip;

    // Tactical Overlay Options
    private JCheckBox fovInsideEnabled;
    private JSlider fovHighlightAlpha;
    private JCheckBox fovOutsideEnabled;
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

    private JCheckBox gameSummaryBV;
    private JCheckBox gameSummaryMM;

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
    private StatusBarPhaseDisplay.CommandComparator cmdComp = new StatusBarPhaseDisplay.CommandComparator(); 
    private PhaseCommandListMouseAdapter cmdMouseAdaptor = new PhaseCommandListMouseAdapter();
    
    private JComboBox<String> tileSetChoice;
    private List<File> tileSets;

    /**
     * A Map that maps command strings to a JTextField for updating the modifier
     * for the command.
     */
    private Map<String, JTextField> cmdModifierMap;

    /**
     * A Map that maps command strings to a Integer for updating the key
     * for the command.
     */
    private Map<String, Integer> cmdKeyMap;

    /**
     * A Map that maps command strings to a JCheckBox for updating the
     * isRepeatable flag.
     */
    private Map<String, JCheckBox> cmdRepeatableMap;
    
    private ClientGUI clientgui = null;

    private static final String CANCEL = "CANCEL"; //$NON-NLS-1$
    private static final String UPDATE = "UPDATE"; //$NON-NLS-1$

    private static final String[] LOCALE_CHOICES = { "en", "de", "ru" }; //$NON-NLS-1$
    
    private static final Dimension LABEL_SPACER = new Dimension(5,0);
    private static final Dimension DEPENDENT_INSET = new Dimension(25,0);
    
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
    private String savedFovHighlightRingsRadii;
    private String savedFovHighlightRingsColors;
    private int savedFovHighlightAlpha;
    private int savedFovDarkenAlpha;
    private int savedNumStripesSlider;
    HashMap<String, String> savedAdvancedOpt = new HashMap<>();

    /**
     * Standard constructor. There is no default constructor for this class.
     *
     * @param owner - the <code>Frame</code> that owns this dialog.
     */
    public CommonSettingsDialog(JFrame owner, ClientGUI cg) {
        this(owner);
        clientgui = cg;
    }
    
    /**
     * Standard constructor. There is no default constructor for this class.
     *
     * @param owner - the <code>Frame</code> that owns this dialog.
     */
    public CommonSettingsDialog(JFrame owner) {
        super(owner, Messages.getString("CommonSettingsDialog.title"), true);

        JTabbedPane panTabs = new JTabbedPane();
        setLayout(new BorderLayout());
        getContentPane().add(panTabs, BorderLayout.CENTER);
        getContentPane().add(getButtonsPanel(), BorderLayout.PAGE_END);
        // Close this dialog when the window manager says to.
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cancel();
            }
        });

        // Add the tabs
        JPanel settingsPanel = getSettingsPanel();
        JScrollPane settingsPane = new JScrollPane(getSettingsPanel());
        settingsPane.getVerticalScrollBar().setUnitIncrement(16);
        panTabs.add("Main", settingsPane);
        
        JScrollPane graphicsPane = new JScrollPane(getGraphicsPanel());
        graphicsPane.getVerticalScrollBar().setUnitIncrement(16);
        panTabs.add("Graphics", graphicsPane);

        JScrollPane keyBindPane = new JScrollPane(getKeyBindPanel());
        keyBindPane.getVerticalScrollBar().setUnitIncrement(16);
        panTabs.add("Key Binds", keyBindPane);

        panTabs.add("Button Order", getButtonOrderPanel());
        
        JScrollPane advancedSettingsPane = new JScrollPane(getAdvancedSettingsPanel());
        advancedSettingsPane.getVerticalScrollBar().setUnitIncrement(16);
        panTabs.add("Advanced", advancedSettingsPane);

        pack();
        setLocationAndSize(getPreferredSize().width, settingsPanel.getPreferredSize().height);
    }

    private JPanel getButtonsPanel() {
        // Add the dialog controls.
        JPanel buttons = new JPanel();
        buttons.setLayout(new GridLayout(1, 0, 20, 5));
        JButton update = new JButton(Messages.getString("CommonSettingsDialog.Update")); //$NON-NLS-1$
        update.setActionCommand(UPDATE);
        update.addActionListener(this);
        buttons.add(update);
        JButton cancel = new JButton(Messages.getString("Cancel")); //$NON-NLS-1$
        cancel.setActionCommand(CANCEL);
        cancel.addActionListener(this);
        buttons.add(cancel);

        return buttons;
    }

    private JPanel getSettingsPanel() {

        ArrayList<ArrayList<Component>> comps = new ArrayList<ArrayList<Component>>();
        ArrayList<Component> row;

        // displayLocale settings
        JLabel displayLocaleLabel = new JLabel(Messages.getString("CommonSettingsDialog.locale")); //$NON-NLS-1$
        displayLocale = new JComboBox<String>();
        displayLocale.addItem(Messages.getString("CommonSettingsDialog.locale.English")); //$NON-NLS-1$
        displayLocale.addItem(Messages.getString("CommonSettingsDialog.locale.Deutsch")); //$NON-NLS-1$
        displayLocale.addItem(Messages.getString("CommonSettingsDialog.locale.Russian")); //$NON-NLS-1$
        displayLocale.setMaximumSize(new Dimension(150,40));
        row = new ArrayList<>();
        row.add(displayLocaleLabel);
        row.add(displayLocale);
        comps.add(row);
        
        // Horizontal Line and Spacer
        row = new ArrayList<>();
        row.add(Box.createRigidArea(new Dimension(0, 10)));
        comps.add(row);

        JSeparator Sep = new JSeparator(SwingConstants.HORIZONTAL);
        row = new ArrayList<>();
        row.add(Sep);
        comps.add(row);
        
        row = new ArrayList<>();
        row.add(Box.createRigidArea(new Dimension(0, 5)));
        comps.add(row);
        // --------------   
        guiScale = new JSlider();
        guiScale.setMajorTickSpacing(3);
        guiScale.setMinimum(7);
        guiScale.setMaximum(24);
        Hashtable<Integer, JComponent> table = new Hashtable<Integer, JComponent>();
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

        showDamageLevel = new JCheckBox(Messages.getString("CommonSettingsDialog.showDamageLevel")); //$NON-NLS-1$
        showDamageLevel.addItemListener(this);
        row = new ArrayList<>();
        row.add(showDamageLevel);
        comps.add(row);
        
        showDamageDecal = new JCheckBox(Messages.getString("CommonSettingsDialog.showDamageDecal")); //$NON-NLS-1$
        showDamageDecal.addItemListener(this);
        row = new ArrayList<>();
        row.add(showDamageDecal);
        comps.add(row);
        
        showUnitId = new JCheckBox(Messages.getString("CommonSettingsDialog.showUnitId")); //$NON-NLS-1$
        showUnitId.addItemListener(this);
        row = new ArrayList<>();
        row.add(showUnitId);
        comps.add(row);

        entityOwnerColor = new JCheckBox(Messages.getString("CommonSettingsDialog.entityOwnerColor")); //$NON-NLS-1$
        entityOwnerColor.setToolTipText(Messages.getString("CommonSettingsDialog.entityOwnerColorTip"));
        entityOwnerColor.addItemListener(this);
        row = new ArrayList<>();
        row.add(entityOwnerColor);
        comps.add(row);
        
        teamColoring = new JCheckBox(Messages.getString("CommonSettingsDialog.teamColoring"));
        teamColoring.setToolTipText(Messages.getString("CommonSettingsDialog.teamColoringTip"));
        teamColoring.addItemListener(this);
        row = new ArrayList<>();
        row.add(teamColoring);
        comps.add(row);
        
        useSoftCenter = new JCheckBox(Messages.getString("CommonSettingsDialog.useSoftCenter")); //$NON-NLS-1$
        useSoftCenter.setToolTipText(Messages.getString("CommonSettingsDialog.useSoftCenterTip"));
        useSoftCenter.addItemListener(this);
        row = new ArrayList<>();
        row.add(useSoftCenter);
        comps.add(row);
        
        // Horizontal Line and Spacer
        row = new ArrayList<>();
        row.add(Box.createRigidArea(new Dimension(0, 10)));
        comps.add(row);

        Sep = new JSeparator(SwingConstants.HORIZONTAL);
        row = new ArrayList<>();
        row.add(Sep);
        comps.add(row);
        
        row = new ArrayList<>();
        row.add(Box.createRigidArea(new Dimension(0, 5)));
        comps.add(row);
        // --------------        

        // Tooltip Stuff
        //
        // Popup Delay and Dismiss Delay
        tooltipDelay = new JTextField(4);
        tooltipDelay.setMaximumSize(new Dimension(150,40));
        JLabel tooltipDelayLabel = new JLabel(Messages.getString("CommonSettingsDialog.tooltipDelay")); //$NON-NLS-1$
        row = new ArrayList<>();
        row.add(tooltipDelayLabel);
        row.add(tooltipDelay);
        comps.add(row);

        tooltipDismissDelay = new JTextField(4);
        tooltipDismissDelay.setMaximumSize(new Dimension(150,40));
        tooltipDismissDelay.setToolTipText(Messages.getString("CommonSettingsDialog.tooltipDismissDelayTooltip"));
        JLabel tooltipDismissDelayLabel = new JLabel(Messages.getString("CommonSettingsDialog.tooltipDismissDelay")); //$NON-NLS-1$
        tooltipDismissDelayLabel.setToolTipText(Messages.getString("CommonSettingsDialog.tooltipDismissDelayTooltip"));
        row = new ArrayList<>();
        row.add(tooltipDismissDelayLabel);
        row.add(tooltipDismissDelay);
        comps.add(row);
        
        tooltipDistSupression = new JTextField(4);
        tooltipDistSupression.setMaximumSize(new Dimension(150,40));
        tooltipDistSupression.setToolTipText(Messages.getString("CommonSettingsDialog.tooltipDistSuppressionTooltip"));
        JLabel tooltipDistSupressionLabel = new JLabel(Messages.getString("CommonSettingsDialog.tooltipDistSuppression")); //$NON-NLS-1$
        tooltipDistSupressionLabel.setToolTipText(Messages.getString("CommonSettingsDialog.tooltipDistSuppressionTooltip"));
        row = new ArrayList<>();
        row.add(tooltipDistSupressionLabel);
        row.add(tooltipDistSupression);
        comps.add(row);

        showWpsinTT = new JCheckBox(Messages.getString("CommonSettingsDialog.showWpsinTT")); //$NON-NLS-1$
        row = new ArrayList<>();
        row.add(showWpsinTT);
        comps.add(row);

        // copied from showWpsinTT, kept comment as it looks like a relevant compiler/editor flag?
        showArmorMiniVisTT = new JCheckBox(Messages.getString("CommonSettingsDialog.showArmorMiniVisTT")); //$NON-NLS-1$
        row = new ArrayList<>();
        row.add(showArmorMiniVisTT);
        comps.add(row);
        
        showPilotPortraitTT = new JCheckBox(Messages.getString("CommonSettingsDialog.showPilotPortraitTT")); //$NON-NLS-1$
        row = new ArrayList<>();
        row.add(showPilotPortraitTT);
        comps.add(row);

        // Horizontal Line and Spacer
        row = new ArrayList<>();
        row.add(Box.createRigidArea(new Dimension(0, 10)));
        comps.add(row);

        Sep = new JSeparator(SwingConstants.HORIZONTAL);
        row = new ArrayList<>();
        row.add(Sep);
        comps.add(row);
        
        row = new ArrayList<>();
        row.add(Box.createRigidArea(new Dimension(0, 5)));
        comps.add(row);
        // --------------        
        
        soundMute = new JCheckBox(Messages.getString("CommonSettingsDialog.soundMute")); //$NON-NLS-1$
        row = new ArrayList<>();
        row.add(soundMute);
        comps.add(row);
        
        JLabel maxPathfinderTimeLabel = new JLabel(Messages.getString("CommonSettingsDialog.pathFiderTimeLimit"));
        maxPathfinderTime = new JTextField(5);
        maxPathfinderTime.setMaximumSize(new Dimension(150,40));
        row = new ArrayList<>();
        row.add(maxPathfinderTimeLabel);
        row.add(maxPathfinderTime);
        comps.add(row);

        // Horizontal Line and Spacer
        row = new ArrayList<>();
        row.add(Box.createRigidArea(new Dimension(0, 10)));
        comps.add(row);

        Sep = new JSeparator(SwingConstants.HORIZONTAL);
        row = new ArrayList<>();
        row.add(Sep);
        comps.add(row);
        
        row = new ArrayList<>();
        row.add(Box.createRigidArea(new Dimension(0, 5)));
        comps.add(row);
        // --------------
        
        nagForMASC = new JCheckBox(Messages.getString("CommonSettingsDialog.nagForMASC")); //$NON-NLS-1$
        row = new ArrayList<>();
        row.add(nagForMASC);
        comps.add(row);

        nagForPSR = new JCheckBox(Messages.getString("CommonSettingsDialog.nagForPSR")); //$NON-NLS-1$
        row = new ArrayList<>();
        row.add(nagForPSR);
        comps.add(row);

        nagForWiGELanding = new JCheckBox(Messages.getString("CommonSettingsDialog.nagForWiGELanding")); //$NON-NLS-1$
        row = new ArrayList<>();
        row.add(nagForWiGELanding);
        comps.add(row);

        nagForNoAction = new JCheckBox(Messages.getString("CommonSettingsDialog.nagForNoAction")); //$NON-NLS-1$
        row = new ArrayList<>();
        row.add(nagForNoAction);
        comps.add(row);
        
        getFocus = new JCheckBox(Messages.getString("CommonSettingsDialog.getFocus")); //$NON-NLS-1$
        row = new ArrayList<>();
        row.add(getFocus);
        comps.add(row);
        mouseWheelZoom = new JCheckBox(Messages.getString("CommonSettingsDialog.mouseWheelZoom")); //$NON-NLS-1$
        row = new ArrayList<>();
        row.add(mouseWheelZoom);
        comps.add(row);

        mouseWheelZoomFlip = new JCheckBox(Messages.getString("CommonSettingsDialog.mouseWheelZoomFlip")); //$NON-NLS-1$
        row = new ArrayList<>();
        row.add(mouseWheelZoomFlip);
        comps.add(row);

        autoEndFiring = new JCheckBox(Messages.getString("CommonSettingsDialog.autoEndFiring")); //$NON-NLS-1$
        row = new ArrayList<>();
        row.add(autoEndFiring);
        comps.add(row);

        autoDeclareSearchlight = new JCheckBox(Messages.getString("CommonSettingsDialog.autoDeclareSearchlight")); //$NON-NLS-1$
        row = new ArrayList<>();
        row.add(autoDeclareSearchlight);
        comps.add(row);
        
        JLabel defaultSortOrderLabel = new JLabel(Messages.getString("CommonSettingsDialog.defaultWeaponSortOrder")); //$NON-NLS-1$
        String toolTip = Messages
                .getString("CommonSettingsDialog.defaultWeaponSortOrderTooltip");
        defaultSortOrderLabel.setToolTipText(toolTip);
        defaultWeaponSortOrder = new JComboBox<>();
        defaultWeaponSortOrder.setToolTipText(toolTip);
        for (Entity.WeaponSortOrder s : Entity.WeaponSortOrder.values()) {
            // Skip custom: it doesn't make sense as a default.
            if (s.equals(Entity.WeaponSortOrder.CUSTOM)) {
                continue;
            }
            String entry = "MechDisplay.WeaponSortOrder." + s.i18nEntry;
            defaultWeaponSortOrder.addItem(Messages.getString(entry));
        }
        row = new ArrayList<>();
        row.add(defaultSortOrderLabel);
        row.add(defaultWeaponSortOrder);
        comps.add(row);

        // Horizontal Line and Spacer
        row = new ArrayList<>();
        row.add(Box.createRigidArea(new Dimension(0, 10)));
        comps.add(row);

        Sep = new JSeparator(SwingConstants.HORIZONTAL);
        row = new ArrayList<>();
        row.add(Sep);
        comps.add(row);
        
        row = new ArrayList<>();
        row.add(Box.createRigidArea(new Dimension(0, 5)));
        comps.add(row);
        // --------------        

        JLabel unitStartCharLabel = new JLabel(Messages.getString("CommonSettingsDialog.protoMechUnitCodes")); //$NON-NLS-1$
        unitStartChar = new JComboBox<String>();
        // Add option for "A, B, C, D..."
        unitStartChar.addItem("\u0041, \u0042, \u0043, \u0044..."); //$NON-NLS-1$
        // Add option for "ALPHA, BETA, GAMMA, DELTA..."
        unitStartChar.addItem("\u0391, \u0392, \u0393, \u0394..."); //$NON-NLS-1$
        // Add option for "alpha, beta, gamma, delta..."
        unitStartChar.addItem("\u03B1, \u03B2, \u03B3, \u03B4..."); //$NON-NLS-1$
        unitStartChar.setMaximumSize(new Dimension(150,40));
        row = new ArrayList<>();
        row.add(unitStartCharLabel);
        row.add(unitStartChar);
        comps.add(row);

        // player-specific settings
        defaultAutoejectDisabled = new JCheckBox(Messages.getString("CommonSettingsDialog.defaultAutoejectDisabled")); //$NON-NLS-1$
        defaultAutoejectDisabled.addItemListener(this);
        row = new ArrayList<>();
        row.add(defaultAutoejectDisabled);
        comps.add(row);

        useAverageSkills = new JCheckBox(Messages.getString("CommonSettingsDialog.useAverageSkills")); //$NON-NLS-1$
        useAverageSkills.addItemListener(this);
        row = new ArrayList<>();
        row.add(useAverageSkills);
        comps.add(row);

        generateNames = new JCheckBox(Messages.getString("CommonSettingsDialog.generateNames")); //$NON-NLS-1$
        generateNames.addItemListener(this);
        row = new ArrayList<>();
        row.add(generateNames);
        comps.add(row);
        
        // Horizontal Line and Spacer
        row = new ArrayList<>();
        row.add(Box.createRigidArea(new Dimension(0, 10)));
        comps.add(row);

        Sep = new JSeparator(SwingConstants.HORIZONTAL);
        row = new ArrayList<>();
        row.add(Sep);
        comps.add(row);
        
        row = new ArrayList<>();
        row.add(Box.createRigidArea(new Dimension(0, 5)));
        comps.add(row);
        // --------------  

        // client-side gameLog settings
        keepGameLog = new JCheckBox(Messages.getString("CommonSettingsDialog.keepGameLog")); //$NON-NLS-1$
        keepGameLog.addItemListener(this);
        row = new ArrayList<>();
        row.add(keepGameLog);
        comps.add(row);

        gameLogFilenameLabel = new JLabel(Messages.getString("CommonSettingsDialog.logFileName")); //$NON-NLS-1$
        gameLogFilename = new JTextField(15);
        gameLogFilename.setMaximumSize(new Dimension(250,40));
        row = new ArrayList<>();
        row.add(Box.createRigidArea(DEPENDENT_INSET));
        row.add(gameLogFilenameLabel);
        row.add(gameLogFilename);
        comps.add(row);
        
        row = new ArrayList<>();
        row.add(Box.createRigidArea(new Dimension(0, 5)));
        comps.add(row);

        stampFilenames = new JCheckBox(Messages.getString("CommonSettingsDialog.stampFilenames")); //$NON-NLS-1$
        stampFilenames.addItemListener(this);
        row = new ArrayList<>();
        row.add(stampFilenames);
        comps.add(row);

        stampFormatLabel = new JLabel(Messages.getString("CommonSettingsDialog.stampFormat")); //$NON-NLS-1$
        stampFormat = new JTextField(15);
        stampFormat.setMaximumSize(new Dimension(15*13,40));
        row = new ArrayList<>();
        row.add(Box.createRigidArea(DEPENDENT_INSET));
        row.add(stampFormatLabel);
        row.add(stampFormat);
        comps.add(row);

        // Horizontal Line and Spacer
        row = new ArrayList<>();
        row.add(Box.createRigidArea(new Dimension(0, 10)));
        comps.add(row);

        Sep = new JSeparator(SwingConstants.HORIZONTAL);
        row = new ArrayList<>();
        row.add(Sep);
        comps.add(row);
        
        row = new ArrayList<>();
        row.add(Box.createRigidArea(new Dimension(0, 5)));
        comps.add(row);
        // -------------- 

        showIPAddressesInChat = new JCheckBox(Messages.getString("CommonSettingsDialog.showIPAddressesInChat"));
        showIPAddressesInChat.setToolTipText(Messages.getString("CommonSettingsDialog.showIPAddressesInChat.tooltip"));
        row = new ArrayList<>();
        row.add(showIPAddressesInChat);
        comps.add(row);

        return createSettingsPanel(comps);
    }

    /**
     * Display the current settings in this dialog. <p/> Overrides
     * <code>Dialog#setVisible(boolean)</code>.
     */
    @Override
    public void setVisible(boolean visible) {
        // Initialize the dialog when it's being shown
        if (visible) {
            GUIPreferences gs = GUIPreferences.getInstance();
            IClientPreferences cs = PreferenceManager.getClientPreferences();

            guiScale.setValue((int)(gs.getGUIScale() * 10));
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
            defaultWeaponSortOrder.setSelectedIndex(gs.getDefaultWeaponSortOrder());
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
            tileSets = new ArrayList<>(Arrays.asList(dir.listFiles(new FilenameFilter() {
                public boolean accept(File direc, String name) {
                    return name.endsWith(".tileset");
                }
            })));
            dir = new File(Configuration.userdataDir(),
                    Configuration.hexesDir().toString());
            File[] userDataTilesets = dir.listFiles(new FilenameFilter() {
                public boolean accept(File direc, String name) {
                    return name.endsWith(".tileset");
                }
            });
            if (userDataTilesets != null) {
                tileSets.addAll(Arrays.asList(userDataTilesets));
            }
            tileSetChoice.removeAllItems();
            for (int i = 0; (tileSets != null) && i < tileSets.size(); i++) {
                String name = tileSets.get(i).getName();
                tileSetChoice.addItem(name.substring(0, name.length() - 8));
                if (name.equals(cs.getMapTileset())) {
                    tileSetChoice.setSelectedIndex(i);
                }
            }

	        gameSummaryBV.setSelected(gs.getGameSummaryBoardView());
	        gameSummaryMM.setSelected(gs.getGameSummaryMiniMap());

            skinFiles.removeAllItems();
            List<String> xmlFiles = new ArrayList<>(Arrays
                    .asList(Configuration.skinsDir().list(new FilenameFilter() {
                        public boolean accept(File directory, String fileName) {
                            return fileName.endsWith(".xml");
                        }
                    })));
            String[] files = new File(Configuration.userdataDir(), Configuration.skinsDir().toString())
                    .list(new FilenameFilter() {
                        public boolean accept(File directory, String fileName) {
                            return fileName.endsWith(".xml");
                        }
                    });
            if (files != null) {
                xmlFiles.addAll(Arrays.asList(files));

            }
            Collections.sort(xmlFiles);
            for (String file : xmlFiles) {
                if (SkinXMLHandler.validSkinSpecFile(file)) {
                    skinFiles.addItem(file);
                }
            }
            // Select the default file first
            skinFiles.setSelectedItem(SkinXMLHandler.defaultSkinXML);
            // If this select fials, the default skin will be selected
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
            savedAdvancedOpt.clear();
            
            advancedKeys.clearSelection();
            
        }   
        super.setVisible(visible);
    }       
            
    /** Cancel any updates made in this dialog and close it.  */     
    void cancel() {
        // Restore values that are immediately updated by player clicks
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
         
        for (String option: savedAdvancedOpt.keySet()) {
            GUIPreferences.getInstance().setValue(option, savedAdvancedOpt.get(option));
        }

        setVisible(false);
    }

    /**
     * Update the settings from this dialog's values, then closes it.
     */
    private void update() {
        GUIPreferences gs = GUIPreferences.getInstance();
        IClientPreferences cs = PreferenceManager.getClientPreferences();

        gs.setShowDamageLevel(showDamageLevel.isSelected());
        gs.setShowDamageDecal(showDamageDecal.isSelected());
        gs.setUnitLabelBorder(entityOwnerColor.isSelected());
        gs.setTeamColoring(teamColoring.isSelected());
        gs.setAutoEndFiring(autoEndFiring.isSelected());
        gs.setAutoDeclareSearchlight(autoDeclareSearchlight.isSelected());
        gs.setDefaultWeaponSortOrder(defaultWeaponSortOrder.getSelectedIndex());
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
        gs.setValue(GUIPreferences.GUI_SCALE, (float)(guiScale.getValue()) / 10);
        cs.setUnitStartChar(((String) unitStartChar.getSelectedItem())
                .charAt(0));
        
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
        if ((clientgui != null) && (clientgui.bv != null)) {
            clientgui.bv.updateEntityLabels();
        }

        cs.setLocale(CommonSettingsDialog.LOCALE_CHOICES[displayLocale
                .getSelectedIndex()]);

        gs.setShowMapsheets(showMapsheets.isSelected());
        gs.setAOHexShadows(aOHexShadows.isSelected());
        gs.setFloatingIso(floatingIso.isSelected());
        gs.setMmSymbol(mmSymbol.isSelected());
        gs.setLevelHighlight(levelhighlight.isSelected());
        gs.setShadowMap(shadowMap.isSelected());
        gs.setHexInclines(hexInclines.isSelected());
        gs.setValue("SOFTCENTER", useSoftCenter.isSelected());

        if ((gs.getAntiAliasing() != chkAntiAliasing.isSelected()) &&
                ((clientgui != null) && (clientgui.bv != null))) {            
            clientgui.bv.clearHexImageCache();
            clientgui.bv.repaint();
        }

        gs.setAntiAliasing(chkAntiAliasing.isSelected());

        gs.setGameSummaryBoardView(gameSummaryBV.isSelected());
        gs.setGameSummaryMiniMap(gameSummaryMM.isSelected());

        UITheme newUITheme = (UITheme)uiThemes.getSelectedItem();
        String oldUITheme = gs.getUITheme();
        if (!oldUITheme.equals(newUITheme.getClassName())) {
            gs.setUITheme(newUITheme.getClassName());
        }

        String newSkinFile = (String)skinFiles.getSelectedItem();
        String oldSkinFile = gs.getSkinFile();
        if (!oldSkinFile.equals(newSkinFile)) {
            boolean success = SkinXMLHandler.initSkinXMLHandler(newSkinFile);
            if (!success) {
                SkinXMLHandler.initSkinXMLHandler(oldSkinFile);
                String title = Messages
                        .getString("CommonSettingsDialog.skinFileFail.title");
                String msg = Messages
                        .getString("CommonSettingsDialog.skinFileFail.msg");
                JOptionPane.showMessageDialog(owner, msg, title,
                        JOptionPane.ERROR_MESSAGE);
            } else {
                gs.setSkinFile(newSkinFile);
            }            
        }

        if (tileSetChoice.getSelectedIndex() >= 0) {
            String tileSetFileName = tileSets.get(tileSetChoice.getSelectedIndex()).getName();
            if (!cs.getMapTileset().equals(tileSetFileName) &&
                    (clientgui != null) && (clientgui.bv != null))  {
                clientgui.bv.clearShadowMap();
            }
            cs.setMapTileset(tileSetFileName);
        }

        ToolTipManager.sharedInstance().setInitialDelay(gs.getTooltipDelay());
        if (gs.getTooltipDismissDelay() > 0) {
            ToolTipManager.sharedInstance().setDismissDelay(gs.getTooltipDismissDelay());
        }

        // Lets iterate through all of the KeyCommandBinds and see if they've
        //  changed
        boolean bindsChanged = false;
        for (KeyCommandBind kcb : KeyCommandBind.values()){
            JTextField txtModifiers = cmdModifierMap.get(kcb.cmd);
            JCheckBox repeatable = cmdRepeatableMap.get(kcb.cmd);
            Integer keyCode = cmdKeyMap.get(kcb.cmd);
            // This shouldn't happen, but just to be safe...
            if (txtModifiers == null || keyCode == null || repeatable == null){
                continue;
            }
            int modifiers = 0;
            if (txtModifiers.getText().contains(
                    KeyEvent.getModifiersExText(KeyEvent.SHIFT_DOWN_MASK))){
                modifiers |= KeyEvent.SHIFT_DOWN_MASK;
            }
            if (txtModifiers.getText().contains(
                    KeyEvent.getModifiersExText(KeyEvent.ALT_DOWN_MASK))){
                modifiers |= KeyEvent.ALT_DOWN_MASK;
            }
            if (txtModifiers.getText().contains(
                    KeyEvent.getModifiersExText(KeyEvent.CTRL_DOWN_MASK))){
                modifiers |= KeyEvent.CTRL_DOWN_MASK;
            }

            if (kcb.modifiers != modifiers){
                bindsChanged = true;
                kcb.modifiers = modifiers;
            }

            if (kcb.key != keyCode){
                bindsChanged = true;
                kcb.key = keyCode;
            }

            if (kcb.isRepeatable != repeatable.isSelected()){
                bindsChanged = true;
                kcb.isRepeatable = repeatable.isSelected();
            }
        }

        if (bindsChanged){
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
            clientgui.updateButtonPanel(IGame.Phase.PHASE_MOVEMENT);
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
            clientgui.updateButtonPanel(IGame.Phase.PHASE_DEPLOYMENT);
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
            clientgui.updateButtonPanel(IGame.Phase.PHASE_FIRING);
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
            clientgui.updateButtonPanel(IGame.Phase.PHASE_PHYSICAL);
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
            clientgui.updateButtonPanel(IGame.Phase.PHASE_TARGETING);
        }

        setVisible(false);
    }

    /**
     * Handle the player pressing the action buttons. <p/> Implements the
     * <code>ActionListener</code> interface.
     *
     * @param event - the <code>ActionEvent</code> that initiated this call.
     */
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        if (command.equals(UPDATE)) {
            update();
        } else if (command.equals(CANCEL)) {
            cancel();
        }
    }

    /** Handle some setting changes that directly update e.g. the board. */
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
        }
    }

    public void focusGained(FocusEvent e) { }

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

    /** 
     * The Graphics Tab
     */
    private JPanel getGraphicsPanel() {

        ArrayList<ArrayList<Component>> comps = new ArrayList<ArrayList<Component>>();
        ArrayList<Component> row;
        
        // Anti-Aliasing
        chkAntiAliasing = new JCheckBox(Messages.getString(
                "CommonSettingsDialog.antiAliasing")); //$NON-NLS-1$
        chkAntiAliasing.setToolTipText(Messages.getString(
                "CommonSettingsDialog.antiAliasingToolTip"));
        row = new ArrayList<>();
        row.add(chkAntiAliasing);
        comps.add(row);
        
        // Animate Moves
        animateMove = new JCheckBox(Messages.getString("CommonSettingsDialog.animateMove")); //$NON-NLS-1$
        row = new ArrayList<>();
        row.add(animateMove);
        comps.add(row);

        // Show Wrecks
        showWrecks = new JCheckBox(Messages.getString("CommonSettingsDialog.showWrecks")); //$NON-NLS-1$
        row = new ArrayList<>();
        row.add(showWrecks);
        comps.add(row);     
        
        // Show Mapsheet borders
        showMapsheets = new JCheckBox(Messages.getString("CommonSettingsDialog.showMapsheets")); //$NON-NLS-1$
        row = new ArrayList<>();
        row.add(showMapsheets);
        comps.add(row);

        // Hill Base AO Shadows
        aOHexShadows = new JCheckBox(Messages.getString("CommonSettingsDialog.AOHexSHadows")); //$NON-NLS-1$
        row = new ArrayList<>();
        aOHexShadows.addItemListener(this);
        row.add(aOHexShadows);
        comps.add(row);
        
        // Shadow Map = Terrain and Building shadows
        shadowMap = new JCheckBox(Messages.getString("CommonSettingsDialog.useShadowMap")); //$NON-NLS-1$
        row = new ArrayList<>();
        shadowMap.addItemListener(this);
        row.add(shadowMap);
        comps.add(row);
        
        // Use Incline graphics (hex border highlights/shadows)
        hexInclines = new JCheckBox(Messages.getString("CommonSettingsDialog.useInclines")); //$NON-NLS-1$
        row = new ArrayList<>();
        hexInclines.addItemListener(this);
        row.add(hexInclines);
        comps.add(row);
        
        // Level Highlight = borders around level changes
        levelhighlight = new JCheckBox(Messages.getString("CommonSettingsDialog.levelHighlight")); //$NON-NLS-1$
        row = new ArrayList<>();
        levelhighlight.addItemListener(this);
        row.add(levelhighlight);
        comps.add(row);
        
        // Floating Isometric = do not draw hex sides
        floatingIso = new JCheckBox(Messages.getString("CommonSettingsDialog.floatingIso")); //$NON-NLS-1$
        row = new ArrayList<>();
        floatingIso.addItemListener(this);
        row.add(floatingIso);
        comps.add(row);

        // Type of symbol used on the minimap 
        mmSymbol = new JCheckBox(Messages.getString("CommonSettingsDialog.mmSymbol")); //$NON-NLS-1$
        row = new ArrayList<>();
        mmSymbol.addItemListener(this);
        row.add(mmSymbol);
        comps.add(row);

        // Game Summary - BoardView
        gameSummaryBV = new JCheckBox(Messages.getString("CommonSettingsDialog.gameSummaryBV.name")); //$NON-NLS-1$
        gameSummaryBV.setToolTipText(Messages.getString("CommonSettingsDialog.gameSummaryBV.tooltip", //$NON-NLS-1$
                new Object[] { Configuration.gameSummaryImagesBVDir() }));
        row = new ArrayList<>();
        gameSummaryBV.addItemListener(this);
        row.add(gameSummaryBV);
        comps.add(row);

        // Game Summary - Mini-map
        gameSummaryMM = new JCheckBox(Messages.getString("CommonSettingsDialog.gameSummaryMM.name")); //$NON-NLS-1$
        gameSummaryMM.setToolTipText(Messages.getString("CommonSettingsDialog.gameSummaryMM.tooltip", //$NON-NLS-1$
                new Object[] { Configuration.gameSummaryImagesMMDir() }));
        row = new ArrayList<>();
        gameSummaryMM.addItemListener(this);
        row.add(gameSummaryMM);
        comps.add(row);

        // UI Theme
        uiThemes = new JComboBox<UITheme>();
        uiThemes.setMaximumSize(new Dimension(400,uiThemes.getMaximumSize().height));
        JLabel uiThemesLabel = new JLabel(Messages.getString("CommonSettingsDialog.uiTheme")); //$NON-NLS-1$
        row = new ArrayList<>();
        row.add(uiThemesLabel);
        row.add(uiThemes);
        comps.add(row);   
        
        // Spacer
        row = new ArrayList<>();
        row.add(Box.createRigidArea(new Dimension(0, 5)));
        comps.add(row);

        // Skin
        skinFiles = new JComboBox<String>();
        skinFiles.setMaximumSize(new Dimension(400,skinFiles.getMaximumSize().height));
        JLabel skinFileLabel = new JLabel(Messages.getString("CommonSettingsDialog.skinFile")); //$NON-NLS-1$
        row = new ArrayList<>();
        row.add(skinFileLabel);
        row.add(skinFiles);
        comps.add(row);   
        
        // Spacer
        row = new ArrayList<>();
        row.add(Box.createRigidArea(new Dimension(0, 5)));
        comps.add(row);

        // Tileset
        JLabel tileSetChoiceLabel = new JLabel(Messages.getString("CommonSettingsDialog.tileset")); //$NON-NLS-1$
        tileSetChoice = new JComboBox<String>(); //$NON-NLS-1$
        tileSetChoice.setMaximumSize(new Dimension(400,tileSetChoice.getMaximumSize().height));
        row = new ArrayList<>();
        row.add(tileSetChoiceLabel);
        row.add(Box.createHorizontalStrut(15));
        row.add(tileSetChoice);
        comps.add(row);
        
        // Horizontal Line and Spacer
        row = new ArrayList<>();
        row.add(Box.createRigidArea(new Dimension(0, 10)));
        comps.add(row);

        JSeparator highlightSep = new JSeparator(SwingConstants.HORIZONTAL);
        row = new ArrayList<>();
        row.add(highlightSep);
        comps.add(row);
        
        row = new ArrayList<>();
        row.add(Box.createRigidArea(new Dimension(0, 5)));
        comps.add(row);
        
        // Highlighting Radius inside FoV
        //
        // Highlight inside Check box
        fovInsideEnabled = new JCheckBox(Messages.getString("TacticalOverlaySettingsDialog.FovInsideEnabled")); //$NON-NLS-1$
        fovInsideEnabled.addItemListener(this);
        row = new ArrayList<>();
        row.add(fovInsideEnabled);
        comps.add(row);

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
        highlightAlphaLabel = new JLabel(Messages.getString("TacticalOverlaySettingsDialog.FovHighlightAlpha")); //$NON-NLS-1$
        row = new ArrayList<>();
        row.add(Box.createRigidArea(DEPENDENT_INSET));
        row.add(highlightAlphaLabel);
        comps.add(row);
        
        // Add some vertical spacing
        row = new ArrayList<>();
        row.add(Box.createVerticalStrut(1));
        comps.add(row);
        
        row = new ArrayList<>();
        row.add(Box.createRigidArea(DEPENDENT_INSET));
        row.add(fovHighlightAlpha);
        comps.add(row);
        
        // Add some vertical spacing
        row = new ArrayList<>();
        row.add(Box.createVerticalStrut(3));
        comps.add(row);

        row = new ArrayList<>();
        fovHighlightRingsRadiiLabel = new JLabel(Messages.getString("TacticalOverlaySettingsDialog.FovHighlightRingsRadii")); //$NON-NLS-1$
        row.add(Box.createRigidArea(DEPENDENT_INSET));
        row.add(fovHighlightRingsRadiiLabel);
        comps.add(row);
        
        // Add some vertical spacing
        row = new ArrayList<>();
        row.add(Box.createVerticalStrut(2));
        comps.add(row);
        
        row = new ArrayList<>();
        fovHighlightRingsRadii= new JTextField((2+1)*7);
        fovHighlightRingsRadii.addFocusListener(this);
        fovHighlightRingsRadii.setMaximumSize(new Dimension(100,fovHighlightRingsRadii.getPreferredSize().height) );
        row.add(Box.createRigidArea(DEPENDENT_INSET));
        row.add(fovHighlightRingsRadii);
        comps.add(row);

        // Add some vertical spacing
        row = new ArrayList<>();
        row.add(Box.createVerticalStrut(2));
        comps.add(row);
        
        row= new ArrayList<>();
        fovHighlightRingsColorsLabel = new JLabel(Messages.getString("TacticalOverlaySettingsDialog.FovHighlightRingsColors")); //$NON-NLS-1$
        row.add(Box.createRigidArea(DEPENDENT_INSET));
        row.add(fovHighlightRingsColorsLabel);
        comps.add(row);
                
        // Add some vertical spacing
        row = new ArrayList<>();
        row.add(Box.createVerticalStrut(2));
        comps.add(row);
        
        row = new ArrayList<>();
        fovHighlightRingsColors= new JTextField(50);//      ((3+1)*3+1)*7);
        fovHighlightRingsColors.addFocusListener(this);
        fovHighlightRingsColors.setMaximumSize(new Dimension(200,fovHighlightRingsColors.getPreferredSize().height) );
        row.add(Box.createRigidArea(DEPENDENT_INSET));
        row.add(fovHighlightRingsColors);
        row.add(Box.createHorizontalGlue());
        comps.add(row);

        // Horizontal Line and Spacer
        row = new ArrayList<>();
        row.add(Box.createRigidArea(new Dimension(0, 10)));
        comps.add(row);

        JSeparator OutSideSep = new JSeparator(SwingConstants.HORIZONTAL);
        row = new ArrayList<>();
        row.add(OutSideSep);
        comps.add(row);
        
        row = new ArrayList<>();
        row.add(Box.createRigidArea(new Dimension(0, 5)));
        comps.add(row);
        
        // Outside FoV Darkening
        //
        // Activation Checkbox
        fovOutsideEnabled = new JCheckBox(Messages.getString("TacticalOverlaySettingsDialog.FovOutsideEnabled")); //$NON-NLS-1$
        fovOutsideEnabled.addItemListener(this);
        row = new ArrayList<>();
        row.add(fovOutsideEnabled);
        comps.add(row);
        
        // Add some vertical spacing
        row = new ArrayList<>();
        row.add(Box.createVerticalStrut(1));
        comps.add(row);

        fovDarkenAlpha = new JSlider(0, 255);
        fovDarkenAlpha.setMajorTickSpacing(25);
        fovDarkenAlpha.setMinorTickSpacing(5);
        fovDarkenAlpha.setPaintTicks(true);
        fovDarkenAlpha.setPaintLabels(true);
        fovDarkenAlpha.setMaximumSize(new Dimension(400, 100));
        fovDarkenAlpha.addChangeListener(this);
        fovDarkenAlpha.setToolTipText(Messages.getString("TacticalOverlaySettingsDialog.AlphaTooltip"));
        darkenAlphaLabel = new JLabel(Messages.getString("TacticalOverlaySettingsDialog.FovDarkenAlpha")); //$NON-NLS-1$
        darkenAlphaLabel.setToolTipText(Messages.getString("TacticalOverlaySettingsDialog.AlphaTooltip"));
        row = new ArrayList<>();
        row.add(Box.createRigidArea(new Dimension(4,0)));
        row.add(Box.createRigidArea(DEPENDENT_INSET));
        row.add(darkenAlphaLabel);
        comps.add(row);
        
        // Add some vertical spacing
        row = new ArrayList<>();
        row.add(Box.createVerticalStrut(2));
        comps.add(row);
        
        row = new ArrayList<>();
        row.add(Box.createRigidArea(DEPENDENT_INSET));
        row.add(fovDarkenAlpha);
        comps.add(row);
        
        // Add some vertical spacing
        row = new ArrayList<>();
        row.add(Box.createVerticalStrut(4));
        comps.add(row);
        
        numStripesSlider = new JSlider(0, 50);
        numStripesSlider.setMajorTickSpacing(10);
        numStripesSlider.setMinorTickSpacing(5);
        numStripesSlider.setPaintTicks(true);
        numStripesSlider.setPaintLabels(true);
        numStripesSlider.setMaximumSize(new Dimension(250, 100));
        numStripesSlider.addChangeListener(this);
        numStripesSlider.setToolTipText(Messages.getString("TacticalOverlaySettingsDialog.FovStripesTooltip"));
        numStripesLabel = new JLabel(
                Messages.getString("TacticalOverlaySettingsDialog.FovStripes")); //$NON-NLS-1$
        numStripesLabel.setToolTipText(Messages.getString("TacticalOverlaySettingsDialog.FovStripesTooltip"));
        row = new ArrayList<>();
        row.add(Box.createRigidArea(new Dimension(4,0)));
        row.add(Box.createRigidArea(DEPENDENT_INSET));
        row.add(numStripesLabel);
        comps.add(row);
        
        row = new ArrayList<>();
        row.add(Box.createVerticalStrut(1));
        comps.add(row);
        
        row = new ArrayList<>();
        row.add(Box.createRigidArea(new Dimension(4,0)));
        row.add(Box.createRigidArea(DEPENDENT_INSET));
        row.add(numStripesSlider);
        comps.add(row);
        
        row = new ArrayList<>();
        row.add(Box.createVerticalStrut(3));
        comps.add(row);
        
        row = new ArrayList<>();
        fovGrayscaleEnabled = new JCheckBox(
                Messages.getString("TacticalOverlaySettingsDialog.FovGrayscale")); //$NON-NLS-1$
        fovGrayscaleEnabled.addItemListener(this);
        row.add(Box.createRigidArea(new Dimension(4,0)));
        row.add(Box.createRigidArea(DEPENDENT_INSET));
        row.add(fovGrayscaleEnabled);
        comps.add(row);   
        
        return createSettingsPanel(comps);
    }
    
    /**
     * Creates a panel with a box for all of the commands that can be bound to
     * keys.
     *
     * @return
     */
    private JPanel getKeyBindPanel() {
        // Create the panel to hold all the components
        // We will have an N x 43 grid, the first column is for labels, the
        //  second column will hold text fields for modifiers, the third
        //  column holds text fields for keys, and the fourth has a checkbox for
        //  isRepeatable.
        JPanel outer = new JPanel();
        outer.setLayout(new FlowLayout(FlowLayout.LEFT));
        
        JPanel keyBinds = new JPanel(new GridBagLayout());
        outer.add(keyBinds);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = gbc.gridy = 0;
        gbc.insets = new Insets(0,10,5,10);
        
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
        gbc.gridx++;
        headers = new JLabel("Repeatable?");
        headers.setToolTipText("Should this action repeat rapidly " +
                "when the key is held down?");
        keyBinds.add(headers, gbc);
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 4;
        
        gbc.fill = GridBagConstraints.BOTH;
        JSeparator sep = new JSeparator(SwingConstants.HORIZONTAL);
        keyBinds.add(sep,gbc);
        
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridy++;
        gbc.gridwidth = 1;
        

        // Create maps to retrieve the text fields for saving
        int numBinds = KeyCommandBind.values().length;
        cmdModifierMap = new HashMap<String,JTextField>((int)(numBinds*1.26));
        cmdKeyMap = new HashMap<String,Integer>((int)(numBinds*1.26));
        cmdRepeatableMap = new HashMap<String,JCheckBox>((int)(numBinds*1.26));

        // For each keyCommandBind, create a label and two text fields
        for (KeyCommandBind kcb : KeyCommandBind.values()){
            JLabel name = new JLabel(
                    Messages.getString("KeyBinds.cmdNames." + kcb.cmd));
            name.setToolTipText(
                    Messages.getString("KeyBinds.cmdDesc." + kcb.cmd));
            gbc.anchor = GridBagConstraints.EAST;
            keyBinds.add(name, gbc);
            gbc.gridx++;
            gbc.anchor = GridBagConstraints.CENTER;

            final JTextField modifiers = new JTextField(10);
            modifiers.setText(KeyEvent.getModifiersExText(kcb.modifiers));
            for (KeyListener kl : modifiers.getKeyListeners()){
                modifiers.removeKeyListener(kl);
            }
            // Update how typing in the text field works
            modifiers.addKeyListener(new KeyListener(){

                @Override
                public void keyPressed(KeyEvent evt) {
                    modifiers.setText(
                            KeyEvent.getModifiersExText(evt.getModifiersEx()));
                    evt.consume();
                }

                @Override
                public void keyReleased(KeyEvent evt) {
                }

                @Override
                public void keyTyped(KeyEvent evt) {
                    // This might be a bit hackish, but we want to deal with
                    //  the key code, so the code to update the text is in
                    //  keyPressed.  We've already done what we want with the
                    //  typed key, and we don't want anything else acting upon
                    //  the key typed event, so we consume it here.
                    evt.consume();
                }

            });
            keyBinds.add(modifiers, gbc);
            gbc.gridx++;
            cmdModifierMap.put(kcb.cmd, modifiers);
            final JTextField key  = new JTextField(10);
            key.setName(kcb.cmd);
            key.setText(KeyEvent.getKeyText(kcb.key));
            // Update how typing in the text field works
            final String cmd = kcb.cmd;
            cmdKeyMap.put(cmd, kcb.key);
            key.addKeyListener(new KeyListener(){

                @Override
                public void keyPressed(KeyEvent evt) {
                    key.setText(KeyEvent.getKeyText(evt.getKeyCode()));
                    cmdKeyMap.put(cmd, evt.getKeyCode());
                    evt.consume();
                }

                @Override
                public void keyReleased(KeyEvent evt) {
                }

                @Override
                public void keyTyped(KeyEvent evt) {
                    // This might be a bit hackish, but we want to deal with
                    //  the key code, so the code to update the text is in
                    //  keyPressed.  We've already done what we want with the
                    //  typed key, and we don't want anything else acting upon
                    //  the key typed event, so we consume it here.
                    evt.consume();
                }

            });
            keyBinds.add(key, gbc);
            gbc.gridx++;

            JCheckBox repeatable = new JCheckBox("Repeatable?");
            repeatable.setSelected(kcb.isRepeatable);
            cmdRepeatableMap.put(kcb.cmd,repeatable);
            keyBinds.add(repeatable, gbc);
            gbc.gridx = 0;
            gbc.gridy++;
            
            // deactivate TABbing through fields here so TAB can be caught as a keybind
            modifiers.setFocusTraversalKeysEnabled(false);
            key.setFocusTraversalKeysEnabled(false);
            repeatable.setFocusTraversalKeysEnabled(false);
        }
        return outer;
    }
    
    /**
     * Creates a panel with a list boxes that allow the button order to be 
     * changed.
     *
     * @return
     */
    private JPanel getButtonOrderPanel(){
        JPanel buttonOrderPanel = new JPanel();
        buttonOrderPanel.setLayout(new BoxLayout(buttonOrderPanel, BoxLayout.Y_AXIS));
        JTabbedPane phasePane = new JTabbedPane();
        buttonOrderPanel.add(phasePane);
        
        // MovementPhaseDisplay        
        movePhaseCommands = new DefaultListModel<StatusBarPhaseDisplay.PhaseCommand>();
        phasePane.add("Movement", getButtonOrderPane(movePhaseCommands,
                MovementDisplay.MoveCommand.values()));
        
        // DeploymentPhaseDisplay
        deployPhaseCommands = new DefaultListModel<StatusBarPhaseDisplay.PhaseCommand>();
        phasePane.add("Deployment", getButtonOrderPane(deployPhaseCommands,
                DeploymentDisplay.DeployCommand.values()));
        
        // FiringPhaseDisplay
        firingPhaseCommands = new DefaultListModel<StatusBarPhaseDisplay.PhaseCommand>();
        phasePane.add("Firing", getButtonOrderPane(firingPhaseCommands,
                FiringDisplay.FiringCommand.values()));
        
        // PhysicalPhaseDisplay
        physicalPhaseCommands = new DefaultListModel<StatusBarPhaseDisplay.PhaseCommand>();
        phasePane.add("Physical", getButtonOrderPane(physicalPhaseCommands,
                PhysicalDisplay.PhysicalCommand.values()));          
        
        // TargetingPhaseDisplay
        targetingPhaseCommands = new DefaultListModel<StatusBarPhaseDisplay.PhaseCommand>();
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

    private JPanel createSettingsPanel(ArrayList<ArrayList<Component>> comps) {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        Box innerpanel = new Box(BoxLayout.PAGE_AXIS);
        for (ArrayList<Component> cs : comps) {
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
        innerpanel.setBorder(new EmptyBorder(10,10,10,10));
        panel.add(innerpanel,BorderLayout.PAGE_START);
        return panel;
    }

    private JPanel getAdvancedSettingsPanel() {
        JPanel p = new JPanel();

        String[] s = GUIPreferences.getInstance().getAdvancedProperties();
        AdvancedOptionData[] opts = new AdvancedOptionData[s.length];
        for (int i = 0; i < s.length; i++) {
            s[i] = s[i].substring(s[i].indexOf("Advanced") + 8, s[i].length());
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
                    if (dat.hasTooltipText()) {
                        advancedKeys.setToolTipText(dat.getTooltipText());
                    } else {
                        advancedKeys.setToolTipText(null);
                    }
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
            guip.setFovHighlightAlpha(Math.max(0, Math.min(255, (int) fovHighlightAlpha.getValue())));
        } else if (evt.getSource().equals(fovDarkenAlpha)) {
            guip.setFovDarkenAlpha(Math.max(0, Math.min(255, (int) fovDarkenAlpha.getValue())));
        } else if (evt.getSource().equals(numStripesSlider)) {
            guip.setFovStripes(numStripesSlider.getValue());
        }
    }
}
