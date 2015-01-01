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

package megamek.client.ui.AWT;

import gov.nist.gui.TabPanel;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Choice;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.ScrollPane;
import java.awt.TextField;
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

import megamek.client.ui.Messages;
import megamek.client.ui.AWT.widget.AdvancedLabel;
import megamek.common.preference.IClientPreferences;
import megamek.common.preference.PreferenceManager;

public class CommonSettingsDialog extends ClientDialog implements
        ActionListener, ItemListener, FocusListener {
    /**
     *
     */
    private static final long serialVersionUID = 5270441082873687662L;
    private ScrollPane scrolledPane = new ScrollPane();
    private TabPanel panTabs;

    private Checkbox minimapEnabled;
    private Checkbox autoEndFiring;
    private Checkbox autoDeclareSearchlight;
    private Checkbox nagForMASC;
    private Checkbox nagForPSR;
    private Checkbox nagForNoAction;
    private Checkbox animateMove;
    private Checkbox showWrecks;
    private Checkbox soundMute;
    private Checkbox showMapHexPopup;
    private TextField tooltipDelay;
    private Choice unitStartChar;
    private TextField maxPathfinderTime;
    private Checkbox getFocus;

    private Checkbox rightDragScroll;
    private Checkbox ctlScroll;
    private Checkbox clickEdgeScroll;
    private Checkbox alwaysRightClickScroll;
    private Checkbox autoEdgeScroll;
    private TextField scrollSensitivity;

    private Checkbox keepGameLog;
    private TextField gameLogFilename;
    // private TextField gameLogMaxSize;
    private Checkbox stampFilenames;
    private TextField stampFormat;
    private Checkbox defaultAutoejectDisabled;
    private Checkbox useAverageSkills;
    private Checkbox showUnitId;
    private Choice locale;
    private Checkbox chatloungeTabs;

    private Checkbox showMapsheets;
    private Checkbox mouseWheelZoom;

    private List keys;
    private int keysIndex = 0;
    private TextField value;

    private Choice tileSetChoice;
    private File[] tileSets;

    private static final String CANCEL = "CANCEL"; //$NON-NLS-1$
    private static final String UPDATE = "UPDATE"; //$NON-NLS-1$

    private static final String[] LOCALE_CHOICES = { "en", "de", "ru", "es" };

    /**
     * Standard constructor. There is no default constructor for this class.
     *
     * @param owner - the <code>Frame</code> that owns this dialog.
     */
    public CommonSettingsDialog(Frame owner) {
        // Initialize our superclass with a title.
        super(owner, Messages.getString("CommonSettingsDialog.title")); //$NON-NLS-1$

        panTabs = new TabPanel();
        Panel settingsPanel = getSettingsPanel();
        scrolledPane.add(settingsPanel);
        panTabs.add("Main", scrolledPane);
        panTabs.add("Advanced", getAdvancedSettingsPanel());
        setLayout(new BorderLayout());
        this.add(panTabs, BorderLayout.CENTER);
        this.add(getButtonsPanel(), BorderLayout.SOUTH);

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
                + scrolledPane.getInsets().right + 10, settingsPanel
                .getPreferredSize().height);
    }

    private Panel getButtonsPanel() {
        // Add the dialog controls.
        Panel buttons = new Panel();
        buttons.setLayout(new GridLayout(1, 0, 20, 5));
        Button update = new Button(Messages
                .getString("CommonSettingsDialog.Update")); //$NON-NLS-1$
        update.setActionCommand(CommonSettingsDialog.UPDATE);
        update.addActionListener(this);
        buttons.add(update);
        Button cancel = new Button(Messages.getString("Cancel")); //$NON-NLS-1$
        cancel.setActionCommand(CommonSettingsDialog.CANCEL);
        cancel.addActionListener(this);
        buttons.add(cancel);

        return buttons;
    }

    private Panel getSettingsPanel() {
        // Lay out this dialog.
        Panel tempPanel = new Panel();
        tempPanel.setLayout(new GridLayout(0, 1));

        // Add the setting controls.
        minimapEnabled = new Checkbox(Messages
                .getString("CommonSettingsDialog.minimapEnabled")); //$NON-NLS-1$
        tempPanel.add(minimapEnabled);

        autoEndFiring = new Checkbox(Messages
                .getString("CommonSettingsDialog.autoEndFiring")); //$NON-NLS-1$
        tempPanel.add(autoEndFiring);

        autoDeclareSearchlight = new Checkbox(Messages
                .getString("CommonSettingsDialog.autoDeclareSearchlight")); //$NON-NLS-1$
        tempPanel.add(autoDeclareSearchlight);

        nagForMASC = new Checkbox(Messages
                .getString("CommonSettingsDialog.nagForMASC")); //$NON-NLS-1$
        tempPanel.add(nagForMASC);

        mouseWheelZoom = new Checkbox(Messages
                .getString("CommonSettingsDialog.mouseWheelZoom")); //$NON-NLS-1$
        tempPanel.add(mouseWheelZoom);

        nagForPSR = new Checkbox(Messages
                .getString("CommonSettingsDialog.nagForPSR")); //$NON-NLS-1$
        tempPanel.add(nagForPSR);

        nagForNoAction = new Checkbox(Messages
                .getString("CommonSettingsDialog.nagForNoAction")); //$NON-NLS-1$
        tempPanel.add(nagForNoAction);

        animateMove = new Checkbox(Messages
                .getString("CommonSettingsDialog.animateMove")); //$NON-NLS-1$
        tempPanel.add(animateMove);

        showWrecks = new Checkbox(Messages
                .getString("CommonSettingsDialog.showWrecks")); //$NON-NLS-1$
        tempPanel.add(showWrecks);

        soundMute = new Checkbox(Messages
                .getString("CommonSettingsDialog.soundMute")); //$NON-NLS-1$
        tempPanel.add(soundMute);

        showMapHexPopup = new Checkbox(Messages
                .getString("CommonSettingsDialog.showMapHexPopup")); //$NON-NLS-1$
        tempPanel.add(showMapHexPopup);

        Panel panSetting;
        panSetting = new Panel(new FlowLayout(FlowLayout.LEFT));
        panSetting.add(new Label(Messages
                .getString("CommonSettingsDialog.tooltipDelay"))); //$NON-NLS-1$
        tooltipDelay = new TextField(4);
        panSetting.add(tooltipDelay);

        tempPanel.add(panSetting);

        panSetting = new Panel(new FlowLayout(FlowLayout.LEFT));
        unitStartChar = new Choice();
        // Add option for "A, B, C, D..."
        unitStartChar.addItem("\u0041, \u0042, \u0043, \u0044..."); //$NON-NLS-1$
        // Add option for "ALPHA, BETA, GAMMA, DELTA..."
        unitStartChar.addItem("\u0391, \u0392, \u0393, \u0394..."); //$NON-NLS-1$
        // Add option for "alpha, beta, gamma, delta..."
        unitStartChar.addItem("\u03B1, \u03B2, \u03B3, \u03B4..."); //$NON-NLS-1$
        panSetting.add(unitStartChar);
        panSetting.add(new Label(Messages
                .getString("CommonSettingsDialog.protoMechUnitCodes"))); //$NON-NLS-1$

        panSetting = new Panel(new FlowLayout(FlowLayout.LEFT));
        panSetting.add(new Label(Messages
                .getString("CommonSettingsDialog.pathFiderTimeLimit"))); //$NON-NLS-1$
        maxPathfinderTime = new TextField(5);
        panSetting.add(maxPathfinderTime);
        tempPanel.add(panSetting);

        getFocus = new Checkbox(Messages
                .getString("CommonSettingsDialog.getFocus")); //$NON-NLS-1$
        tempPanel.add(getFocus);
        tempPanel.add(panSetting);

        // player-specific settings
        defaultAutoejectDisabled = new Checkbox(Messages
                .getString("CommonSettingsDialog.defaultAutoejectDisabled")); //$NON-NLS-1$
        defaultAutoejectDisabled.addItemListener(this);
        tempPanel.add(defaultAutoejectDisabled);

        useAverageSkills = new Checkbox(Messages
                .getString("CommonSettingsDialog.useAverageSkills")); //$NON-NLS-1$
        useAverageSkills.addItemListener(this);
        tempPanel.add(useAverageSkills);

        showUnitId = new Checkbox(Messages
                .getString("CommonSettingsDialog.showUnitId")); //$NON-NLS-1$
        showUnitId.addItemListener(this);
        tempPanel.add(showUnitId);

        // client-side gameLog settings
        keepGameLog = new Checkbox(Messages
                .getString("CommonSettingsDialog.keepGameLog")); //$NON-NLS-1$
        keepGameLog.addItemListener(this);
        tempPanel.add(keepGameLog);

        panSetting = new Panel(new FlowLayout(FlowLayout.LEFT));
        panSetting.add(new Label(Messages
                .getString("CommonSettingsDialog.logFileName"))); //$NON-NLS-1$
        gameLogFilename = new TextField(15);
        panSetting.add(gameLogFilename);
        tempPanel.add(panSetting);

        panSetting = new Panel(new FlowLayout(FlowLayout.LEFT));
        panSetting.add(new Label(Messages
                .getString("CommonSettingsDialog.tileset"))); //$NON-NLS-1$
        tileSetChoice = new Choice();
        panSetting.add(tileSetChoice);
        tempPanel.add(panSetting);

        /*
         * panSetting = new Panel(new FlowLayout(FlowLayout.LEFT));
         * panSetting.add( new
         * Label(Messages.getString("CommonSettingsDialog.logFileMaxSize")) );
         * //$NON-NLS-1$ gameLogMaxSize = new TextField(5); panSetting.add(
         * gameLogMaxSize ); tempPanel.add( panSetting );
         */

        stampFilenames = new Checkbox(Messages
                .getString("CommonSettingsDialog.stampFilenames")); //$NON-NLS-1$
        stampFilenames.addItemListener(this);
        tempPanel.add(stampFilenames);

        panSetting = new Panel(new FlowLayout(FlowLayout.LEFT));
        panSetting.add(new Label(Messages
                .getString("CommonSettingsDialog.stampFormat"))); //$NON-NLS-1$
        stampFormat = new TextField(15);
        panSetting.add(stampFormat);
        tempPanel.add(panSetting);

        // scrolling options
        tempPanel.add(new AdvancedLabel(Messages
                .getString("CommonSettingsDialog.mapScrollText"))); //$NON-NLS-1$

        rightDragScroll = new Checkbox(Messages
                .getString("CommonSettingsDialog.rightDragScroll")); //$NON-NLS-1$
        tempPanel.add(rightDragScroll);

        ctlScroll = new Checkbox(Messages
                .getString("CommonSettingsDialog.ctlScroll")); //$NON-NLS-1$
        tempPanel.add(ctlScroll);

        clickEdgeScroll = new Checkbox(Messages
                .getString("CommonSettingsDialog.clickEdgeScroll")); //$NON-NLS-1$
        tempPanel.add(clickEdgeScroll);

        alwaysRightClickScroll = new Checkbox(Messages
                .getString("CommonSettingsDialog.alwaysRightClickScroll")); //$NON-NLS-1$
        tempPanel.add(alwaysRightClickScroll);

        autoEdgeScroll = new Checkbox(Messages
                .getString("CommonSettingsDialog.autoEdgeScroll")); //$NON-NLS-1$
        tempPanel.add(autoEdgeScroll);

        panSetting = new Panel(new FlowLayout(FlowLayout.LEFT));
        panSetting.add(new Label(Messages
                .getString("CommonSettingsDialog.scrollSesitivity"))); //$NON-NLS-1$
        scrollSensitivity = new TextField(4);
        panSetting.add(scrollSensitivity);
        tempPanel.add(panSetting);

        // locale settings
        panSetting = new Panel(new FlowLayout(FlowLayout.LEFT));
        panSetting.add(new Label(Messages
                .getString("CommonSettingsDialog.locale"))); //$NON-NLS-1$
        // locale = new TextField(8);
        locale = new Choice();
        locale.add(Messages.getString("CommonSettingsDialog.locale.English")); //$NON-NLS-1$
        locale.add(Messages.getString("CommonSettingsDialog.locale.Deutsch")); //$NON-NLS-1$
        locale.add(Messages.getString("CommonSettingsDialog.locale.Russian")); //$NON-NLS-1$
        locale.add(Messages.getString("CommonSettingsDialog.locale.Spanish")); //$NON-NLS-1$
        panSetting.add(locale);
        tempPanel.add(panSetting);

        // chatloungtab setting
        chatloungeTabs = new Checkbox(Messages
                .getString("CommonSettingsDialog.chatloungeTabs")); //$NON-NLS-1$
        tempPanel.add(chatloungeTabs);

        // showMapsheets setting
        showMapsheets = new Checkbox(Messages
                .getString("CommonSettingsDialog.showMapsheets")); //$NON-NLS-1$
        tempPanel.add(showMapsheets);
        return tempPanel;
    }

    /**
     * Display the current settings in this dialog. <p/> Overrides
     * <code>Dialog#show()</code>.
     */
    @Override
    public void setVisible(boolean show) {
        if (show) {
            GUIPreferences gs = GUIPreferences.getInstance();
            IClientPreferences cs = PreferenceManager.getClientPreferences();

            minimapEnabled.setState(gs.getMinimapEnabled());
            autoEndFiring.setState(gs.getAutoEndFiring());
            autoDeclareSearchlight.setState(gs.getAutoDeclareSearchlight());
            nagForMASC.setState(gs.getNagForMASC());
            nagForPSR.setState(gs.getNagForPSR());
            nagForNoAction.setState(gs.getNagForNoAction());
            animateMove.setState(gs.getShowMoveStep());
            showWrecks.setState(gs.getShowWrecks());
            soundMute.setState(gs.getSoundMute());
            showMapHexPopup.setState(gs.getShowMapHexPopup());
            tooltipDelay.setText(Integer.toString(gs.getTooltipDelay()));
            mouseWheelZoom.setState(gs.getMouseWheelZoom());

            // Select the correct char set (give a nice default to start).
            unitStartChar.select(0);
            for (int loop = 0; loop < unitStartChar.getItemCount(); loop++) {
                if (unitStartChar.getItem(loop).charAt(0) == PreferenceManager
                        .getClientPreferences().getUnitStartChar()) {
                    unitStartChar.select(loop);
                    break;
                }
            }

            maxPathfinderTime.setText(Integer.toString(cs
                    .getMaxPathfinderTime()));

            rightDragScroll.setState(gs.getRightDragScroll());
            ctlScroll.setState(gs.getCtlScroll());
            clickEdgeScroll.setState(gs.getClickEdgeScroll());
            alwaysRightClickScroll.setState(gs.getAlwaysRightClickScroll());
            autoEdgeScroll.setState(gs.getAutoEdgeScroll());
            scrollSensitivity.setText(Integer.toString(gs
                    .getScrollSensitivity()));

            keepGameLog.setState(cs.keepGameLog());
            gameLogFilename.setEnabled(keepGameLog.getState());
            gameLogFilename.setText(cs.getGameLogFilename());
            // gameLogMaxSize.setEnabled(keepGameLog.getState());
            // gameLogMaxSize.setText( Integer.toString(cs.getGameLogMaxSize())
            // );
            stampFilenames.setState(cs.stampFilenames());
            stampFormat.setEnabled(stampFilenames.getState());
            stampFormat.setText(cs.getStampFormat());

            defaultAutoejectDisabled.setState(cs.defaultAutoejectDisabled());
            useAverageSkills.setState(cs.useAverageSkills());
            showUnitId.setState(cs.getShowUnitId());

            int index = 0;
            if (cs.getLocaleString().startsWith("de")) {
                index = 1;
            }
            if (cs.getLocaleString().startsWith("ru")) {
                index = 2;
            }
            if (cs.getLocaleString().startsWith("es")) {
                index = 3;
            }
            locale.select(index);

            chatloungeTabs.setState(gs.getChatLoungeTabs());

            showMapsheets.setState(gs.getShowMapsheets());

            File dir = new File("data" + File.separator + "images"
                    + File.separator + "hexes" + File.separator);
            tileSets = dir.listFiles(new FilenameFilter() {
                public boolean accept(File direc, String name) {
                    if (name.endsWith(".tileset")) {
                        return true;
                    }
                    return false;
                }
            });
            tileSetChoice.removeAll();
            for (int i = 0; i < tileSets.length; i++) {
                String name = tileSets[i].getName();
                tileSetChoice.add(name.substring(0, name.length() - 8));
                if (name.equals(cs.getMapTileset())) {
                    tileSetChoice.select(i);
                }
            }

            getFocus.setState(gs.getFocus());
        }
        super.setVisible(show);
    }

    /**
     * Cancel any updates made in this dialog, and closes it.
     */
    private void cancel() {
        setVisible(false);
    }

    /**
     * Update the settings from this dialog's values, then closes it.
     */
    private void update() {
        GUIPreferences gs = GUIPreferences.getInstance();
        IClientPreferences cs = PreferenceManager.getClientPreferences();

        gs.setMinimapEnabled(minimapEnabled.getState());
        gs.setAutoEndFiring(autoEndFiring.getState());
        gs.setAutoDeclareSearchlight(autoDeclareSearchlight.getState());
        gs.setNagForMASC(nagForMASC.getState());
        gs.setNagForPSR(nagForPSR.getState());
        gs.setNagForNoAction(nagForNoAction.getState());
        gs.setShowMoveStep(animateMove.getState());
        gs.setShowWrecks(showWrecks.getState());
        gs.setSoundMute(soundMute.getState());
        gs.setShowMapHexPopup(showMapHexPopup.getState());
        gs.setTooltipDelay(Integer.parseInt(tooltipDelay.getText()));
        cs.setUnitStartChar(unitStartChar.getSelectedItem().charAt(0));

        gs.setRightDragScroll(rightDragScroll.getState());
        gs.setCtlScroll(ctlScroll.getState());
        gs.setClickEdgeScroll(clickEdgeScroll.getState());
        gs.setAlwaysRightClickScroll(alwaysRightClickScroll.getState());
        gs.setAutoEdgeScroll(autoEdgeScroll.getState());
        gs.setScrollSensitivity(Integer.parseInt(scrollSensitivity.getText()));
        gs.setMouseWheelZoom(mouseWheelZoom.getState());

        cs.setMaxPathfinderTime(Integer.parseInt(maxPathfinderTime.getText()));

        gs.setGetFocus(getFocus.getState());

        cs.setKeepGameLog(keepGameLog.getState());
        cs.setGameLogFilename(gameLogFilename.getText());
        // cs.setGameLogMaxSize(Integer.parseInt(gameLogMaxSize.getText()));
        cs.setStampFilenames(stampFilenames.getState());
        cs.setStampFormat(stampFormat.getText());

        cs.setDefaultAutoejectDisabled(defaultAutoejectDisabled.getState());
        cs.setUseAverageSkills(useAverageSkills.getState());
        cs.setShowUnitId(showUnitId.getState());

        cs.setLocale(CommonSettingsDialog.LOCALE_CHOICES[locale
                .getSelectedIndex()]);

        gs.setChatloungeTabs(chatloungeTabs.getState());
        gs.setShowMapsheets(showMapsheets.getState());

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
            gameLogFilename.setEnabled(keepGameLog.getState());
            // gameLogMaxSize.setEnabled(keepGameLog.getState());
        }
        if (source.equals(stampFilenames)) {
            stampFormat.setEnabled(stampFilenames.getState());
        }
        if ((event.getSource() == keys)
                && (event.getStateChange() == ItemEvent.SELECTED)) {
            value.setText(GUIPreferences.getInstance().getString(
                    "Advanced" + keys.getSelectedItem()));
            keysIndex = keys.getSelectedIndex();
        }
    }

    public void focusGained(FocusEvent e) {
    }

    public void focusLost(FocusEvent e) {
        GUIPreferences.getInstance().setValue(
                "Advanced" + keys.getItem(keysIndex), value.getText());
    }

    private Panel getAdvancedSettingsPanel() {
        Panel p = new Panel();

        keys = new List(10, false);
        String[] s = GUIPreferences.getInstance().getAdvancedProperties();
        Arrays.sort(s);
        for (int i = 0; i < s.length; i++) {
            keys.add(s[i]
                    .substring(s[i].indexOf("Advanced") + 8, s[i].length()));
        }
        keys.addItemListener(this);
        p.add(keys);

        value = new TextField(10);
        value.addFocusListener(this);
        p.add(value);

        return p;
    }
}
