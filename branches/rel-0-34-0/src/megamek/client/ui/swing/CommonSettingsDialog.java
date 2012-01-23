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
import java.awt.FlowLayout;
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
import java.util.Arrays;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
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
    private JComboBox unitStartChar;
    private JTextField maxPathfinderTime;
    private JCheckBox getFocus;

    private JCheckBox rightDragScroll;
    private JCheckBox ctlScroll;
    private JCheckBox clickEdgeScroll;
    private JCheckBox alwaysRightClickScroll;
    private JCheckBox autoEdgeScroll;
    private JTextField scrollSensitivity;

    private JCheckBox keepGameLog;
    private JTextField gameLogFilename;
    // private JTextField gameLogMaxSize;
    private JCheckBox stampFilenames;
    private JTextField stampFormat;
    private JCheckBox defaultAutoejectDisabled;
    private JCheckBox useAverageSkills;
    private JCheckBox showUnitId;
    private JComboBox displayLocale;
    private JCheckBox chatloungeTabs;

    private JCheckBox showMapsheets;
    private JCheckBox mouseWheelZoom;

    private JList keys;
    private int keysIndex = 0;
    private JTextField value;

    private JComboBox tileSetChoice;
    private File[] tileSets;

    private static final String CANCEL = "CANCEL"; //$NON-NLS-1$
    private static final String UPDATE = "UPDATE"; //$NON-NLS-1$

    private static final String[] LOCALE_CHOICES = { "en", "de", "ru" ,"es"};

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
        JScrollPane scroll = new JScrollPane(settingsPanel);
        panTabs.add("Main", scroll);
        panTabs.add("Advanced", getAdvancedSettingsPanel());
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
                + scroll.getInsets().right + 20, settingsPanel
                .getPreferredSize().height);
    }

    private JPanel getButtonsPanel() {
        // Add the dialog controls.
        JPanel buttons = new JPanel();
        buttons.setLayout(new GridLayout(1, 0, 20, 5));
        JButton update = new JButton(Messages
                .getString("CommonSettingsDialog.Update")); //$NON-NLS-1$
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
        // Lay out this dialog.
        JPanel tempPanel = new JPanel();
        tempPanel.setLayout(new GridLayout(0, 1));

        // Add the setting controls.
        minimapEnabled = new JCheckBox(Messages
                .getString("CommonSettingsDialog.minimapEnabled")); //$NON-NLS-1$
        tempPanel.add(minimapEnabled);

        autoEndFiring = new JCheckBox(Messages
                .getString("CommonSettingsDialog.autoEndFiring")); //$NON-NLS-1$
        tempPanel.add(autoEndFiring);

        autoDeclareSearchlight = new JCheckBox(Messages
                .getString("CommonSettingsDialog.autoDeclareSearchlight")); //$NON-NLS-1$
        tempPanel.add(autoDeclareSearchlight);

        nagForMASC = new JCheckBox(Messages
                .getString("CommonSettingsDialog.nagForMASC")); //$NON-NLS-1$
        tempPanel.add(nagForMASC);

        mouseWheelZoom = new JCheckBox(Messages
                .getString("CommonSettingsDialog.mouseWheelZoom")); //$NON-NLS-1$
        tempPanel.add(mouseWheelZoom);

        nagForPSR = new JCheckBox(Messages
                .getString("CommonSettingsDialog.nagForPSR")); //$NON-NLS-1$
        tempPanel.add(nagForPSR);

        nagForNoAction = new JCheckBox(Messages
                .getString("CommonSettingsDialog.nagForNoAction")); //$NON-NLS-1$
        tempPanel.add(nagForNoAction);

        animateMove = new JCheckBox(Messages
                .getString("CommonSettingsDialog.animateMove")); //$NON-NLS-1$
        tempPanel.add(animateMove);

        showWrecks = new JCheckBox(Messages
                .getString("CommonSettingsDialog.showWrecks")); //$NON-NLS-1$
        tempPanel.add(showWrecks);

        soundMute = new JCheckBox(Messages
                .getString("CommonSettingsDialog.soundMute")); //$NON-NLS-1$
        tempPanel.add(soundMute);

        showMapHexPopup = new JCheckBox(Messages
                .getString("CommonSettingsDialog.showMapHexPopup")); //$NON-NLS-1$
        tempPanel.add(showMapHexPopup);

        JPanel panSetting;
        panSetting = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panSetting.add(new JLabel(Messages
                .getString("CommonSettingsDialog.tooltipDelay"))); //$NON-NLS-1$
        tooltipDelay = new JTextField(4);
        panSetting.add(tooltipDelay);

        tempPanel.add(panSetting);

        panSetting = new JPanel(new FlowLayout(FlowLayout.LEFT));
        unitStartChar = new JComboBox();
        // Add option for "A, B, C, D..."
        unitStartChar.addItem("\u0041, \u0042, \u0043, \u0044..."); //$NON-NLS-1$
        // Add option for "ALPHA, BETA, GAMMA, DELTA..."
        unitStartChar.addItem("\u0391, \u0392, \u0393, \u0394..."); //$NON-NLS-1$
        // Add option for "alpha, beta, gamma, delta..."
        unitStartChar.addItem("\u03B1, \u03B2, \u03B3, \u03B4..."); //$NON-NLS-1$
        panSetting.add(unitStartChar);
        panSetting.add(new JLabel(Messages
                .getString("CommonSettingsDialog.protoMechUnitCodes"))); //$NON-NLS-1$

        panSetting = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panSetting.add(new JLabel(Messages
                .getString("CommonSettingsDialog.pathFiderTimeLimit"))); //$NON-NLS-1$
        maxPathfinderTime = new JTextField(5);
        panSetting.add(maxPathfinderTime);
        tempPanel.add(panSetting);

        getFocus = new JCheckBox(Messages
                .getString("CommonSettingsDialog.getFocus")); //$NON-NLS-1$
        tempPanel.add(getFocus);
        tempPanel.add(panSetting);

        // player-specific settings
        defaultAutoejectDisabled = new JCheckBox(Messages
                .getString("CommonSettingsDialog.defaultAutoejectDisabled")); //$NON-NLS-1$
        defaultAutoejectDisabled.addItemListener(this);
        tempPanel.add(defaultAutoejectDisabled);

        useAverageSkills = new JCheckBox(Messages
                .getString("CommonSettingsDialog.useAverageSkills")); //$NON-NLS-1$
        useAverageSkills.addItemListener(this);
        tempPanel.add(useAverageSkills);

        showUnitId = new JCheckBox(Messages
                .getString("CommonSettingsDialog.showUnitId")); //$NON-NLS-1$
        showUnitId.addItemListener(this);
        tempPanel.add(showUnitId);

        // client-side gameLog settings
        keepGameLog = new JCheckBox(Messages
                .getString("CommonSettingsDialog.keepGameLog")); //$NON-NLS-1$
        keepGameLog.addItemListener(this);
        tempPanel.add(keepGameLog);

        panSetting = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panSetting.add(new JLabel(Messages
                .getString("CommonSettingsDialog.logFileName"))); //$NON-NLS-1$
        gameLogFilename = new JTextField(15);
        panSetting.add(gameLogFilename);
        tempPanel.add(panSetting);

        panSetting = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panSetting.add(new JLabel(Messages
                .getString("CommonSettingsDialog.tileset"))); //$NON-NLS-1$
        tileSetChoice = new JComboBox();
        panSetting.add(tileSetChoice);
        tempPanel.add(panSetting);

        /*
         * panSetting = new JPanel(new FlowLayout(FlowLayout.LEFT));
         * panSetting.add( new
         * JLabel(Messages.getString("CommonSettingsDialog.logFileMaxSize")) );
         * //$NON-NLS-1$ gameLogMaxSize = new JTextField(5); panSetting.add(
         * gameLogMaxSize ); tempPanel.add( panSetting );
         */

        stampFilenames = new JCheckBox(Messages
                .getString("CommonSettingsDialog.stampFilenames")); //$NON-NLS-1$
        stampFilenames.addItemListener(this);
        tempPanel.add(stampFilenames);

        panSetting = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panSetting.add(new JLabel(Messages
                .getString("CommonSettingsDialog.stampFormat"))); //$NON-NLS-1$
        stampFormat = new JTextField(15);
        panSetting.add(stampFormat);
        tempPanel.add(panSetting);

        // scrolling options
        JTextArea ta = new JTextArea(Messages
                .getString("CommonSettingsDialog.mapScrollText"));
        ta.setEditable(false);
        ta.setOpaque(false);
        tempPanel.add(ta); 

        rightDragScroll = new JCheckBox(Messages
                .getString("CommonSettingsDialog.rightDragScroll")); //$NON-NLS-1$
        tempPanel.add(rightDragScroll);

        ctlScroll = new JCheckBox(Messages
                .getString("CommonSettingsDialog.ctlScroll")); //$NON-NLS-1$
        tempPanel.add(ctlScroll);

        clickEdgeScroll = new JCheckBox(Messages
                .getString("CommonSettingsDialog.clickEdgeScroll")); //$NON-NLS-1$
        tempPanel.add(clickEdgeScroll);

        alwaysRightClickScroll = new JCheckBox(Messages
                .getString("CommonSettingsDialog.alwaysRightClickScroll")); //$NON-NLS-1$
        tempPanel.add(alwaysRightClickScroll);

        autoEdgeScroll = new JCheckBox(Messages
                .getString("CommonSettingsDialog.autoEdgeScroll")); //$NON-NLS-1$
        tempPanel.add(autoEdgeScroll);

        panSetting = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panSetting.add(new JLabel(Messages
                .getString("CommonSettingsDialog.scrollSesitivity"))); //$NON-NLS-1$
        scrollSensitivity = new JTextField(4);
        panSetting.add(scrollSensitivity);
        tempPanel.add(panSetting);

        // displayLocale settings
        panSetting = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panSetting.add(new JLabel(Messages
                .getString("CommonSettingsDialog.locale"))); //$NON-NLS-1$
        // displayLocale = new JTextField(8);
        displayLocale = new JComboBox();
        displayLocale.addItem(Messages
                .getString("CommonSettingsDialog.locale.English")); //$NON-NLS-1$
        displayLocale.addItem(Messages
                .getString("CommonSettingsDialog.locale.Deutsch")); //$NON-NLS-1$
        displayLocale.addItem(Messages
                .getString("CommonSettingsDialog.locale.Russian")); //$NON-NLS-1$
        panSetting.add(displayLocale);
        displayLocale.addItem(Messages
                .getString("CommonSettingsDialog.locale.Spanish")); //$NON-NLS-1$
        panSetting.add(displayLocale);
        tempPanel.add(panSetting);

        // chatloungtab setting
        chatloungeTabs = new JCheckBox(Messages
                .getString("CommonSettingsDialog.chatloungeTabs")); //$NON-NLS-1$
        tempPanel.add(chatloungeTabs);

        // showMapsheets setting
        showMapsheets = new JCheckBox(Messages
                .getString("CommonSettingsDialog.showMapsheets")); //$NON-NLS-1$
        tempPanel.add(showMapsheets);
        return tempPanel;
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

        // Select the correct char set (give a nice default to start).
        unitStartChar.setSelectedIndex(0);
        for (int loop = 0; loop < unitStartChar.getItemCount(); loop++) {
            if (((String) unitStartChar.getItemAt(loop)).charAt(0) == PreferenceManager
                    .getClientPreferences().getUnitStartChar()) {
                unitStartChar.setSelectedIndex(loop);
                break;
            }
        }

        maxPathfinderTime.setText(Integer.toString(cs.getMaxPathfinderTime()));

        rightDragScroll.setSelected(gs.getRightDragScroll());
        ctlScroll.setSelected(gs.getCtlScroll());
        clickEdgeScroll.setSelected(gs.getClickEdgeScroll());
        alwaysRightClickScroll.setSelected(gs.getAlwaysRightClickScroll());
        autoEdgeScroll.setSelected(gs.getAutoEdgeScroll());
        scrollSensitivity.setText(Integer.toString(gs.getScrollSensitivity()));

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
        showUnitId.setSelected(cs.getShowUnitId());

        int index = 0;
        if (cs.getLocaleString().startsWith("de")) {
            index = 1;
        }
        if (cs.getLocaleString().startsWith("ru")) {
            index = 2;
        }
        displayLocale.setSelectedIndex(index);

        chatloungeTabs.setSelected(gs.getChatLoungeTabs());

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
        cs.setUnitStartChar(((String) unitStartChar.getSelectedItem())
                .charAt(0));

        gs.setRightDragScroll(rightDragScroll.isSelected());
        gs.setCtlScroll(ctlScroll.isSelected());
        gs.setClickEdgeScroll(clickEdgeScroll.isSelected());
        gs.setAlwaysRightClickScroll(alwaysRightClickScroll.isSelected());
        gs.setAutoEdgeScroll(autoEdgeScroll.isSelected());
        gs.setScrollSensitivity(Integer.parseInt(scrollSensitivity.getText()));
        gs.setMouseWheelZoom(mouseWheelZoom.isSelected());

        cs.setMaxPathfinderTime(Integer.parseInt(maxPathfinderTime.getText()));

        gs.setGetFocus(getFocus.isSelected());

        cs.setKeepGameLog(keepGameLog.isSelected());
        cs.setGameLogFilename(gameLogFilename.getText());
        // cs.setGameLogMaxSize(Integer.parseInt(gameLogMaxSize.getText()));
        cs.setStampFilenames(stampFilenames.isSelected());
        cs.setStampFormat(stampFormat.getText());

        cs.setDefaultAutoejectDisabled(defaultAutoejectDisabled.isSelected());
        cs.setUseAverageSkills(useAverageSkills.isSelected());
        cs.setShowUnitId(showUnitId.isSelected());

        cs.setLocale(CommonSettingsDialog.LOCALE_CHOICES[displayLocale
                .getSelectedIndex()]);

        gs.setChatloungeTabs(chatloungeTabs.isSelected());
        gs.setShowMapsheets(showMapsheets.isSelected());

        if (tileSetChoice.getSelectedIndex() >= 0) {
            cs.setMapTileset(tileSets[tileSetChoice.getSelectedIndex()]
                    .getName());
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

    private JPanel getAdvancedSettingsPanel() {
        JPanel p = new JPanel();

        String[] s = GUIPreferences.getInstance().getAdvancedProperties();
        Arrays.sort(s);
        for (int i = 0; i < s.length; i++) {
            s[i] = s[i].substring(s[i].indexOf("Advanced") + 8, s[i].length());
        }
        keys = new JList(s);
        keys.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        keys.addListSelectionListener(this);
        p.add(keys);

        value = new JTextField(10);
        value.addFocusListener(this);
        p.add(value);

        return p;
    }

    public void valueChanged(ListSelectionEvent event) {
        if (event.getSource().equals(keys)) {
            value.setText(GUIPreferences.getInstance().getString(
                    "Advanced" + keys.getSelectedValue()));
            keysIndex = keys.getSelectedIndex();
        }
    }
}
