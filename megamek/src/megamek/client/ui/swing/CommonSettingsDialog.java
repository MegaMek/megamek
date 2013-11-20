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
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ToolTipManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import megamek.client.ui.Messages;
import megamek.common.preference.IClientPreferences;
import megamek.common.preference.PreferenceManager;

public class CommonSettingsDialog extends ClientDialog implements
        ActionListener, ItemListener, FocusListener, ListSelectionListener {
    /**
     *
     */
    private static final long serialVersionUID = 1535370193846895473L;

    private JTabbedPane panTabs;

    private JCheckBox minimapEnabled;
    private JCheckBox autoEndFiring;
    private JCheckBox autoDeclareSearchlight;
    private JCheckBox nagForMASC;
    private JCheckBox nagForPSR;
    private JCheckBox nagForNoAction;
    private JCheckBox animateMove;
    private JCheckBox showWrecks;
    private JCheckBox soundMute;
    private JCheckBox showMapHexPopup;
    private JTextField tooltipDelay;
    private JTextField tooltipDismissDelay;
    private JComboBox<String> unitStartChar;
    private JTextField maxPathfinderTime;
    private JCheckBox getFocus;

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

    private JCheckBox showMapsheets;
    private JCheckBox mouseWheelZoom;
    private JCheckBox mouseWheelZoomFlip;

    // Tactical Overlay Options
    private JCheckBox fovInsideEnabled;
    private JSlider fovHighlightAlpha;
    private JCheckBox fovOutsideEnabled;
    private JSlider fovDarkenAlpha;

    private JList<String> keys;
    private int keysIndex = 0;
    private JTextField value;

    private JComboBox<String> tileSetChoice;
    private File[] tileSets;

    private static final String CANCEL = "CANCEL"; //$NON-NLS-1$
    private static final String UPDATE = "UPDATE"; //$NON-NLS-1$

    private static final String[] LOCALE_CHOICES = { "en", "de", "ru" }; //$NON-NLS-1$

    /**
     * Standard constructor. There is no default constructor for this class.
     *
     * @param owner - the <code>Frame</code> that owns this dialog.
     */
    public CommonSettingsDialog(JFrame owner) {
        // Initialize our superclass with a title.
        super(owner, Messages.getString("CommonSettingsDialog.title")); //$NON-NLS-1$

        panTabs = new JTabbedPane();

        JPanel settingsPanel = getSettingsPanel();
        JScrollPane settingsPane = new JScrollPane(settingsPanel);
        panTabs.add("Main", settingsPane);

        JPanel tacticalOverlaySettingsPanel = getTacticalOverlaySettingsPanel();
        JScrollPane tacticalOverlaySettingsPane = new JScrollPane(tacticalOverlaySettingsPanel);
        panTabs.add("Tactical Overlay", tacticalOverlaySettingsPane);

        JPanel advancedSettingsPanel = getAdvancedSettingsPanel();
        JScrollPane advancedSettingsPane = new JScrollPane(advancedSettingsPanel);
        panTabs.add("Advanced", advancedSettingsPane);

        setLayout(new BorderLayout());
        getContentPane().add(panTabs, BorderLayout.CENTER);
        getContentPane().add(getButtonsPanel(), BorderLayout.SOUTH);

        // Close this dialog when the window manager says to.
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cancel();
            }
        });

        // Center this dialog.
        pack();

        // Make the thing wide enough so a horizontal scrollbar isn't
        // necessary. I'm not sure why the extra hardcoded 10 pixels
        // is needed, maybe it's a ms windows thing.
        setLocationAndSize(settingsPanel.getPreferredSize().width
                + settingsPane.getInsets().right + 20, settingsPanel
                .getPreferredSize().height);
    }

    private JPanel getButtonsPanel() {
        // Add the dialog controls.
        JPanel buttons = new JPanel();
        buttons.setLayout(new GridLayout(1, 0, 20, 5));
        JButton update = new JButton(Messages.getString("CommonSettingsDialog.Update")); //$NON-NLS-1$
        update.setActionCommand(CommonSettingsDialog.UPDATE);
        update.addActionListener(this);
        buttons.add(update);
        JButton cancel = new JButton(Messages.getString("Cancel")); //$NON-NLS-1$
        cancel.setActionCommand(CommonSettingsDialog.CANCEL);
        cancel.addActionListener(this);
        buttons.add(cancel);

        return buttons;
    }

    private JPanel getSettingsPanel() {

        ArrayList<ArrayList<JComponent>> comps = new ArrayList<ArrayList<JComponent>>();
        ArrayList<JComponent> row;

        // Add the setting controls.
        minimapEnabled = new JCheckBox(Messages.getString("CommonSettingsDialog.minimapEnabled")); //$NON-NLS-1$
        row = new ArrayList<JComponent>();
        row.add(minimapEnabled);
        comps.add(row);

        autoEndFiring = new JCheckBox(Messages.getString("CommonSettingsDialog.autoEndFiring")); //$NON-NLS-1$
        row = new ArrayList<JComponent>();
        row.add(autoEndFiring);
        comps.add(row);

        autoDeclareSearchlight = new JCheckBox(Messages.getString("CommonSettingsDialog.autoDeclareSearchlight")); //$NON-NLS-1$
        row = new ArrayList<JComponent>();
        row.add(autoDeclareSearchlight);
        comps.add(row);

        nagForMASC = new JCheckBox(Messages.getString("CommonSettingsDialog.nagForMASC")); //$NON-NLS-1$
        row = new ArrayList<JComponent>();
        row.add(nagForMASC);
        comps.add(row);

        mouseWheelZoom = new JCheckBox(Messages.getString("CommonSettingsDialog.mouseWheelZoom")); //$NON-NLS-1$
        row = new ArrayList<JComponent>();
        row.add(mouseWheelZoom);
        comps.add(row);

        mouseWheelZoomFlip = new JCheckBox(Messages.getString("CommonSettingsDialog.mouseWheelZoomFlip")); //$NON-NLS-1$
        row = new ArrayList<JComponent>();
        row.add(mouseWheelZoomFlip);
        comps.add(row);

        nagForPSR = new JCheckBox(Messages.getString("CommonSettingsDialog.nagForPSR")); //$NON-NLS-1$
        row = new ArrayList<JComponent>();
        row.add(nagForPSR);
        comps.add(row);

        nagForNoAction = new JCheckBox(Messages.getString("CommonSettingsDialog.nagForNoAction")); //$NON-NLS-1$
        row = new ArrayList<JComponent>();
        row.add(nagForNoAction);
        comps.add(row);

        animateMove = new JCheckBox(Messages.getString("CommonSettingsDialog.animateMove")); //$NON-NLS-1$
        row = new ArrayList<JComponent>();
        row.add(animateMove);
        comps.add(row);

        showWrecks = new JCheckBox(Messages.getString("CommonSettingsDialog.showWrecks")); //$NON-NLS-1$
        row = new ArrayList<JComponent>();
        row.add(showWrecks);
        comps.add(row);

        soundMute = new JCheckBox(Messages.getString("CommonSettingsDialog.soundMute")); //$NON-NLS-1$
        row = new ArrayList<JComponent>();
        row.add(soundMute);
        comps.add(row);

        showMapHexPopup = new JCheckBox(Messages.getString("CommonSettingsDialog.showMapHexPopup")); //$NON-NLS-1$
        row = new ArrayList<JComponent>();
        row.add(showMapHexPopup);
        comps.add(row);

        tooltipDelay = new JTextField(4);
        JLabel tooltipDelayLabel = new JLabel(Messages.getString("CommonSettingsDialog.tooltipDelay")); //$NON-NLS-1$
        row = new ArrayList<JComponent>();
        row.add(tooltipDelayLabel);
        row.add(tooltipDelay);
        comps.add(row);

        tooltipDismissDelay = new JTextField(4);
        tooltipDismissDelay.setToolTipText(Messages.getString("CommonSettingsDialog.tooltipDismissDelayTooltip"));
        JLabel tooltipDismissDelayLabel = new JLabel(Messages.getString("CommonSettingsDialog.tooltipDismissDelay")); //$NON-NLS-1$
        tooltipDismissDelayLabel.setToolTipText(Messages.getString("CommonSettingsDialog.tooltipDismissDelayTooltip"));
        row = new ArrayList<JComponent>();
        row.add(tooltipDismissDelayLabel);
        row.add(tooltipDismissDelay);
        comps.add(row);

        JLabel unitStartCharLabel = new JLabel(Messages.getString("CommonSettingsDialog.protoMechUnitCodes")); //$NON-NLS-1$
        unitStartChar = new JComboBox<String>();
        // Add option for "A, B, C, D..."
        unitStartChar.addItem("\u0041, \u0042, \u0043, \u0044..."); //$NON-NLS-1$
        // Add option for "ALPHA, BETA, GAMMA, DELTA..."
        unitStartChar.addItem("\u0391, \u0392, \u0393, \u0394..."); //$NON-NLS-1$
        // Add option for "alpha, beta, gamma, delta..."
        unitStartChar.addItem("\u03B1, \u03B2, \u03B3, \u03B4..."); //$NON-NLS-1$
        row = new ArrayList<JComponent>();
        row.add(unitStartCharLabel);
        row.add(unitStartChar);
        comps.add(row);

        JLabel maxPathfinderTimeLabel = new JLabel(Messages.getString("CommonSettingsDialog.pathFiderTimeLimit"));
        maxPathfinderTime = new JTextField(5);
        row = new ArrayList<JComponent>();
        row.add(maxPathfinderTimeLabel);
        row.add(maxPathfinderTime);
        comps.add(row);

        getFocus = new JCheckBox(Messages.getString("CommonSettingsDialog.getFocus")); //$NON-NLS-1$
        row = new ArrayList<JComponent>();
        row.add(getFocus);
        comps.add(row);

        // player-specific settings
        defaultAutoejectDisabled = new JCheckBox(Messages.getString("CommonSettingsDialog.defaultAutoejectDisabled")); //$NON-NLS-1$
        defaultAutoejectDisabled.addItemListener(this);
        row = new ArrayList<JComponent>();
        row.add(defaultAutoejectDisabled);
        comps.add(row);

        useAverageSkills = new JCheckBox(Messages.getString("CommonSettingsDialog.useAverageSkills")); //$NON-NLS-1$
        useAverageSkills.addItemListener(this);
        row = new ArrayList<JComponent>();
        row.add(useAverageSkills);
        comps.add(row);

        generateNames = new JCheckBox(Messages.getString("CommonSettingsDialog.generateNames")); //$NON-NLS-1$
        generateNames.addItemListener(this);
        row = new ArrayList<JComponent>();
        row.add(generateNames);
        comps.add(row);

        showUnitId = new JCheckBox(Messages.getString("CommonSettingsDialog.showUnitId")); //$NON-NLS-1$
        showUnitId.addItemListener(this);
        row = new ArrayList<JComponent>();
        row.add(showUnitId);
        comps.add(row);

        // client-side gameLog settings
        keepGameLog = new JCheckBox(Messages.getString("CommonSettingsDialog.keepGameLog")); //$NON-NLS-1$
        keepGameLog.addItemListener(this);
        row = new ArrayList<JComponent>();
        row.add(keepGameLog);
        comps.add(row);

        JLabel gameLogFilenameLabel = new JLabel(Messages.getString("CommonSettingsDialog.logFileName")); //$NON-NLS-1$
        gameLogFilename = new JTextField(15);
        row = new ArrayList<JComponent>();
        row.add(gameLogFilenameLabel);
        row.add(gameLogFilename);
        comps.add(row);

        JLabel tileSetChoiceLabel = new JLabel(Messages.getString("CommonSettingsDialog.tileset")); //$NON-NLS-1$
        tileSetChoice = new JComboBox<String>(); //$NON-NLS-1$
        row = new ArrayList<JComponent>();
        row.add(tileSetChoiceLabel);
        row.add(tileSetChoice);
        comps.add(row);

        stampFilenames = new JCheckBox(Messages.getString("CommonSettingsDialog.stampFilenames")); //$NON-NLS-1$
        stampFilenames.addItemListener(this);
        row = new ArrayList<JComponent>();
        row.add(stampFilenames);
        comps.add(row);

        JLabel stampFormatLabel = new JLabel(Messages.getString("CommonSettingsDialog.stampFormat")); //$NON-NLS-1$
        stampFormat = new JTextField(15);
        stampFormat.setMaximumSize(new Dimension(15*13,40));
        row = new ArrayList<JComponent>();
        row.add(stampFormatLabel);
        row.add(stampFormat);
        comps.add(row);

        // displayLocale settings
        JLabel displayLocaleLabel = new JLabel(Messages.getString("CommonSettingsDialog.locale")); //$NON-NLS-1$
        // displayLocale = new JTextField(8);
        displayLocale = new JComboBox<String>();
        displayLocale.addItem(Messages.getString("CommonSettingsDialog.locale.English")); //$NON-NLS-1$
        displayLocale.addItem(Messages.getString("CommonSettingsDialog.locale.Deutsch")); //$NON-NLS-1$
        displayLocale.addItem(Messages.getString("CommonSettingsDialog.locale.Russian")); //$NON-NLS-1$
        row = new ArrayList<JComponent>();
        row.add(displayLocaleLabel);
        row.add(displayLocale);
        comps.add(row);

        // showMapsheets setting
        showMapsheets = new JCheckBox(Messages.getString("CommonSettingsDialog.showMapsheets")); //$NON-NLS-1$
        row = new ArrayList<JComponent>();
        row.add(showMapsheets);
        comps.add(row);

        return createSettingsPanel(comps);
    }

    /**
     * Display the current settings in this dialog. <p/> Overrides
     * <code>Dialog#setVisible(boolean)</code>.
     */
    @Override
    public void setVisible(boolean visible) {
        GUIPreferences gs = GUIPreferences.getInstance();
        IClientPreferences cs = PreferenceManager.getClientPreferences();

        minimapEnabled.setSelected(gs.getMinimapEnabled());
        autoEndFiring.setSelected(gs.getAutoEndFiring());
        autoDeclareSearchlight.setSelected(gs.getAutoDeclareSearchlight());
        nagForMASC.setSelected(gs.getNagForMASC());
        nagForPSR.setSelected(gs.getNagForPSR());
        nagForNoAction.setSelected(gs.getNagForNoAction());
        animateMove.setSelected(gs.getShowMoveStep());
        showWrecks.setSelected(gs.getShowWrecks());
        soundMute.setSelected(gs.getSoundMute());
        showMapHexPopup.setSelected(gs.getShowMapHexPopup());
        tooltipDelay.setText(Integer.toString(gs.getTooltipDelay()));
        tooltipDismissDelay.setText(Integer.toString(gs.getTooltipDismissDelay()));

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

        File dir = new File("data" + File.separator + "images" + File.separator
                + "hexes" + File.separator);
        tileSets = dir.listFiles(new FilenameFilter() {
            public boolean accept(File direc, String name) {
                if (name.endsWith(".tileset")) {
                    return true;
                }
                return false;
            }
        });
        tileSetChoice.removeAllItems();
        for (int i = 0; i < tileSets.length; i++) {
            String name = tileSets[i].getName();
            tileSetChoice.addItem(name.substring(0, name.length() - 8));
            if (name.equals(cs.getMapTileset())) {
                tileSetChoice.setSelectedIndex(i);
            }
        }

        fovInsideEnabled.setSelected(gs.getFovHighlight());
        fovHighlightAlpha.setValue((int) ((100./255.) * gs.getFovHighlightAlpha()));
        fovOutsideEnabled.setSelected(gs.getFovDarken());
        fovDarkenAlpha.setValue((int) ((100./255.) * gs.getFovDarkenAlpha()));

        getFocus.setSelected(gs.getFocus());
        super.setVisible(visible);
    }

    /**
     * Cancel any updates made in this dialog, and closes it.
     */
    void cancel() {
        setVisible(false);
    }

    /**
     * Update the settings from this dialog's values, then closes it.
     */
    private void update() {
        GUIPreferences gs = GUIPreferences.getInstance();
        IClientPreferences cs = PreferenceManager.getClientPreferences();

        gs.setMinimapEnabled(minimapEnabled.isSelected());
        gs.setAutoEndFiring(autoEndFiring.isSelected());
        gs.setAutoDeclareSearchlight(autoDeclareSearchlight.isSelected());
        gs.setNagForMASC(nagForMASC.isSelected());
        gs.setNagForPSR(nagForPSR.isSelected());
        gs.setNagForNoAction(nagForNoAction.isSelected());
        gs.setShowMoveStep(animateMove.isSelected());
        gs.setShowWrecks(showWrecks.isSelected());
        gs.setSoundMute(soundMute.isSelected());
        gs.setShowMapHexPopup(showMapHexPopup.isSelected());
        gs.setTooltipDelay(Integer.parseInt(tooltipDelay.getText()));
        gs.setTooltipDismissDelay(Integer.parseInt(tooltipDismissDelay.getText()));
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

        cs.setDefaultAutoejectDisabled(defaultAutoejectDisabled.isSelected());
        cs.setUseAverageSkills(useAverageSkills.isSelected());
        cs.setGenerateNames(generateNames.isSelected());
        cs.setShowUnitId(showUnitId.isSelected());

        cs.setLocale(CommonSettingsDialog.LOCALE_CHOICES[displayLocale
                .getSelectedIndex()]);

        gs.setShowMapsheets(showMapsheets.isSelected());

        if (tileSetChoice.getSelectedIndex() >= 0) {
            cs.setMapTileset(tileSets[tileSetChoice.getSelectedIndex()]
                    .getName());
        }

        // Tactical Overlay Settings
        gs.setFovHighlight(fovInsideEnabled.isSelected());
        gs.setFovHighlightAlpha((int) (fovHighlightAlpha.getValue() * 2.55)); // convert from 0-100 to 0-255
        gs.setFovDarken(fovOutsideEnabled.isSelected());
        gs.setFovDarkenAlpha((int) (fovDarkenAlpha.getValue() * 2.55)); // convert from 0-100 to 0-255

        ToolTipManager.sharedInstance().setInitialDelay(
                GUIPreferences.getInstance().getTooltipDelay());
        if (GUIPreferences.getInstance().getTooltipDismissDelay() > 0)
        {
            ToolTipManager.sharedInstance().setDismissDelay(
                    GUIPreferences.getInstance().getTooltipDismissDelay());
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
        if (CommonSettingsDialog.UPDATE.equalsIgnoreCase(command)) {
            update();
        } else if (CommonSettingsDialog.CANCEL.equalsIgnoreCase(command)) {
            cancel();
        }
    }

    /**
     * Handle the player clicking checkboxes. <p/> Implements the
     * <code>ItemListener</code> interface.
     *
     * @param event - the <code>ItemEvent</code> that initiated this call.
     */
    public void itemStateChanged(ItemEvent event) {
        Object source = event.getItemSelectable();
        if (source.equals(keepGameLog)) {
            gameLogFilename.setEnabled(keepGameLog.isSelected());
            // gameLogMaxSize.setEnabled(keepGameLog.isSelected());
        } else if (source.equals(stampFilenames)) {
            stampFormat.setEnabled(stampFilenames.isSelected());
        }
    }

    public void focusGained(FocusEvent e) {
    }

    public void focusLost(FocusEvent e) {
        GUIPreferences.getInstance().setValue(
                "Advanced" + keys.getModel().getElementAt(keysIndex),
                value.getText());
    }

    private JPanel getTacticalOverlaySettingsPanel() {

        ArrayList<ArrayList<JComponent>> comps = new ArrayList<ArrayList<JComponent>>();
        ArrayList<JComponent> row;

        fovInsideEnabled = new JCheckBox(Messages.getString("TacticalOverlaySettingsDialog.FovInsideEnabled")); //$NON-NLS-1$
        row = new ArrayList<JComponent>();
        row.add(fovInsideEnabled);
        comps.add(row);

        fovHighlightAlpha = new JSlider();
        fovHighlightAlpha.setMajorTickSpacing(20);
        fovHighlightAlpha.setMinorTickSpacing(5);
        fovHighlightAlpha.setPaintTicks(true);
        fovHighlightAlpha.setPaintLabels(true);
        fovHighlightAlpha.setMaximumSize(new Dimension(700, 100));
        JLabel highlightAlphaLabel = new JLabel(Messages.getString("TacticalOverlaySettingsDialog.FovHighlightAlpha")); //$NON-NLS-1$
        row = new ArrayList<JComponent>();
        row.add(fovHighlightAlpha);
        row.add(highlightAlphaLabel);
        comps.add(row);

        fovOutsideEnabled = new JCheckBox(Messages.getString("TacticalOverlaySettingsDialog.FovOutsideEnabled")); //$NON-NLS-1$
        row = new ArrayList<JComponent>();
        row.add(fovOutsideEnabled);
        comps.add(row);

        fovDarkenAlpha = new JSlider();
        fovDarkenAlpha.setMajorTickSpacing(20);
        fovDarkenAlpha.setMinorTickSpacing(5);
        fovDarkenAlpha.setPaintTicks(true);
        fovDarkenAlpha.setPaintLabels(true);
        fovDarkenAlpha.setMaximumSize(new Dimension(700, 100));
        JLabel darkenAlphaLabel = new JLabel(Messages.getString("TacticalOverlaySettingsDialog.FovDarkenAlpha")); //$NON-NLS-1$
        row = new ArrayList<JComponent>();
        row.add(fovDarkenAlpha);
        row.add(darkenAlphaLabel);
        comps.add(row);

        return createSettingsPanel(comps);
    }

    private JPanel createSettingsPanel(ArrayList<ArrayList<JComponent>> comps) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        for (ArrayList<JComponent> cs : comps) {
        	JPanel subPanel = new JPanel();
            subPanel.setLayout(new BoxLayout(subPanel, BoxLayout.X_AXIS));
        	for (JComponent c : cs) {
        		subPanel.add(c);
        	}
        	subPanel.add(Box.createHorizontalGlue());
        	panel.add(subPanel);
        }
        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private JPanel getAdvancedSettingsPanel() {
        JPanel p = new JPanel();

        String[] s = GUIPreferences.getInstance().getAdvancedProperties();
        Arrays.sort(s);
        for (int i = 0; i < s.length; i++) {
            s[i] = s[i].substring(s[i].indexOf("Advanced") + 8, s[i].length());
        }
        keys = new JList<String>(s);
        keys.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        keys.addListSelectionListener(this);
        p.add(keys);

        value = new JTextField(10);
        value.addFocusListener(this);
        p.add(value);

        return p;
    }

    public void valueChanged(ListSelectionEvent event) {
        if (event.getValueIsAdjusting()) {
            return;
        }
        if (event.getSource().equals(keys)) {
            value.setText(GUIPreferences.getInstance().getString(
                    "Advanced" + keys.getSelectedValue()));
            keysIndex = keys.getSelectedIndex();
        }
    }
}
