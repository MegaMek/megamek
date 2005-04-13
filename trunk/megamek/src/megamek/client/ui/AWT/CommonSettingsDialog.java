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

package megamek.client;

import java.awt.*;
import java.awt.event.*;

import megamek.common.Settings;

public class CommonSettingsDialog extends ClientDialog
    implements ActionListener, ItemListener
{
    private ScrollPane  scrOptions = new ScrollPane();

    private Checkbox    minimapEnabled;
    private Checkbox    autoEndFiring;
    private Checkbox    nagForMASC;
    private Checkbox    nagForPSR;
    private Checkbox    nagForNoAction;
    private Checkbox    animateMove;
    private Checkbox    showWrecks;
    private Checkbox    soundMute;
    private Checkbox    showMapHexPopup;
    private TextField   tooltipDelay;
    private Choice      unitStartChar;
    private TextField   maxPathfinderTime;
    private Checkbox    getFocus;
    
    private Checkbox    rightDragScroll;
    private Checkbox    ctlScroll;
    private Checkbox    clickEdgeScroll;
    private Checkbox    alwaysRightClickScroll;
    private Checkbox    autoEdgeScroll;
    private TextField   scrollSensitivity;

    private Checkbox    keepServerlog;
    private TextField   serverlogFilename;
    private TextField   serverlogMaxSize;
    private Checkbox    defaultAutoejectDisabled;
    private Checkbox    showUnitId;

    private static final String CANCEL = "CANCEL"; //$NON-NLS-1$
    private static final String UPDATE = "UPDATE"; //$NON-NLS-1$
    

    /**
     * Standard constructor.  There is no default constructor for this class.
     *
     * @param   owner - the <code>Frame</code> that owns this dialog.
     */
    public CommonSettingsDialog( Frame owner) {
        // Initialize our superclass with a title.
        super( owner, Messages.getString("CommonSettingsDialog.title") ); //$NON-NLS-1$

        // Lay out this dialog.
        Panel tempPanel = new Panel();
        tempPanel.setLayout( new GridLayout(0, 1) );

        // Add the setting controls.
        Panel panSetting;
        minimapEnabled
            = new Checkbox( Messages.getString("CommonSettingsDialog.minimapEnabled") ); //$NON-NLS-1$
        tempPanel.add( minimapEnabled );
        
        autoEndFiring
            = new Checkbox( Messages.getString("CommonSettingsDialog.autoEndFiring") ); //$NON-NLS-1$
        tempPanel.add( autoEndFiring );
        
        nagForMASC
            = new Checkbox( Messages.getString("CommonSettingsDialog.nagForMASC") ); //$NON-NLS-1$
        tempPanel.add( nagForMASC );
        
        nagForPSR
            = new Checkbox( Messages.getString("CommonSettingsDialog.nagForPSR") ); //$NON-NLS-1$
        tempPanel.add( nagForPSR );
        
        nagForNoAction
            = new Checkbox( Messages.getString("CommonSettingsDialog.nagForNoAction") ); //$NON-NLS-1$
        tempPanel.add( nagForNoAction );
        
        animateMove
            = new Checkbox( Messages.getString("CommonSettingsDialog.animateMove") ); //$NON-NLS-1$
        tempPanel.add( animateMove );
        
        showWrecks
            = new Checkbox( Messages.getString("CommonSettingsDialog.showWrecks") ); //$NON-NLS-1$
        tempPanel.add( showWrecks );
        
        soundMute
            = new Checkbox( Messages.getString("CommonSettingsDialog.soundMute") ); //$NON-NLS-1$
        tempPanel.add( soundMute );
        
        showMapHexPopup
            = new Checkbox( Messages.getString("CommonSettingsDialog.showMapHexPopup") ); //$NON-NLS-1$
        tempPanel.add( showMapHexPopup );

        panSetting = new Panel();
        tooltipDelay
            = new TextField(4);
        panSetting.add( tooltipDelay );
        panSetting.add( new Label(Messages.getString("CommonSettingsDialog.tooltipDelay")) ); //$NON-NLS-1$
        tempPanel.add( panSetting );

        panSetting = new Panel();
        unitStartChar
            = new Choice();
        // Add option for "A, B, C, D..."
        unitStartChar.addItem( "\u0041, \u0042, \u0043, \u0044..." ); //$NON-NLS-1$
        // Add option for "ALPHA, BETA, GAMMA, DELTA..."
        unitStartChar.addItem( "\u0391, \u0392, \u0393, \u0394..." ); //$NON-NLS-1$
        // Add option for "alpha, beta, gamma, delta..."
        unitStartChar.addItem( "\u03B1, \u03B2, \u03B3, \u03B4..." ); //$NON-NLS-1$
        panSetting.add( unitStartChar );
        panSetting.add( new Label(Messages.getString("CommonSettingsDialog.protoMechUnitCodes")) ); //$NON-NLS-1$

        panSetting = new Panel();
        maxPathfinderTime
            = new TextField(5);
        panSetting.add( maxPathfinderTime );
        panSetting.add( new Label(Messages.getString("CommonSettingsDialog.pathFiderTimeLimit")) ); //$NON-NLS-1$
        tempPanel.add( panSetting );
        getFocus
            = new Checkbox( Messages.getString("CommonSettingsDialog.getFocus")); //$NON-NLS-1$
        tempPanel.add( getFocus );
        tempPanel.add( panSetting );

        // player-specific settings
        defaultAutoejectDisabled
            = new Checkbox( Messages.getString("CommonSettingsDialog.defaultAutoejectDisabled") ); //$NON-NLS-1$
        defaultAutoejectDisabled.addItemListener(this);
        tempPanel.add( defaultAutoejectDisabled );

        showUnitId
            = new Checkbox ( Messages.getString("CommonSettingsDialog.showUnitId")); //$NON-NLS-1$
        showUnitId.addItemListener(this);
        tempPanel.add( showUnitId );
        
        // client-side gamelog settings
        keepServerlog
            = new Checkbox( Messages.getString("CommonSettingsDialog.keepServerlog") ); //$NON-NLS-1$
        keepServerlog.addItemListener(this);
        tempPanel.add( keepServerlog );
        panSetting = new Panel();
        serverlogFilename
            = new TextField(20);
        panSetting.add( serverlogFilename );
        panSetting.add( new Label(Messages.getString("CommonSettingsDialog.logFileName")) ); //$NON-NLS-1$
        tempPanel.add( panSetting );
        panSetting = new Panel();
        serverlogMaxSize
            = new TextField(5);
        panSetting.add( serverlogMaxSize );
        panSetting.add( new Label(Messages.getString("CommonSettingsDialog.logFileMaxSize")) ); //$NON-NLS-1$
        tempPanel.add( panSetting );

        // scrolling options
        tempPanel.add( new Label(Messages.getString("CommonSettingsDialog.minimapScroll")) ); //$NON-NLS-1$
        tempPanel.add( new Label(Messages.getString("CommonSettingsDialog.additionalScroll")) ); //$NON-NLS-1$

        rightDragScroll
            = new Checkbox( Messages.getString("CommonSettingsDialog.rightDragScroll") ); //$NON-NLS-1$
        tempPanel.add( rightDragScroll );
        
        ctlScroll
            = new Checkbox( Messages.getString("CommonSettingsDialog.ctlScroll") ); //$NON-NLS-1$
            tempPanel.add( ctlScroll );

        clickEdgeScroll
            = new Checkbox( Messages.getString("CommonSettingsDialog.clickEdgeScroll") ); //$NON-NLS-1$
            tempPanel.add( clickEdgeScroll );

        alwaysRightClickScroll
            = new Checkbox( Messages.getString("CommonSettingsDialog.alwaysRightClickScroll") ); //$NON-NLS-1$
            tempPanel.add( alwaysRightClickScroll );
        
        autoEdgeScroll
            = new Checkbox( Messages.getString("CommonSettingsDialog.autoEdgeScroll") ); //$NON-NLS-1$
            tempPanel.add( autoEdgeScroll );

        panSetting = new Panel();
        scrollSensitivity
            = new TextField(4);
        panSetting.add( scrollSensitivity );
        panSetting.add( new Label(Messages.getString("CommonSettingsDialog.scrollSesitivity")) ); //$NON-NLS-1$
        tempPanel.add( panSetting );

        scrOptions.add(tempPanel);

        // add the scrollable panel
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        setLayout(gridbag);
            
        c.insets = new Insets(1, 1, 1, 1);
        c.weightx = 1.0;    c.weighty = 1.0;
        c.fill = GridBagConstraints.BOTH;
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(scrOptions, c);
        this.add(scrOptions);

        // Add the dialog controls.
        Panel buttons = new Panel();
        buttons.setLayout( new GridLayout(1, 0) );
        buttons.add( new Label() );
        Button update = new Button( Messages.getString("CommonSettingsDialog.Update") ); //$NON-NLS-1$
        update.setActionCommand( CommonSettingsDialog.UPDATE );
        update.addActionListener( this );
        buttons.add( update );
        buttons.add( new Label() );
        Button cancel = new Button( Messages.getString("Cancel") ); //$NON-NLS-1$
        cancel.setActionCommand( CommonSettingsDialog.CANCEL );
        cancel.addActionListener( this );
        buttons.add( cancel );
        buttons.add( new Label() );

        c.weightx = 1.0;    c.weighty = 0.0;
        gridbag.setConstraints(buttons, c);
        this.add( buttons );

        // Close this dialog when the window manager says to.
        addWindowListener(new WindowAdapter() {
	    public void windowClosing(WindowEvent e) { cancel(); }
        });

        // Center this dialog.
        pack();

        setLocationAndSize(tempPanel.getSize());
    }

    /**
     * Display the current settings in this dialog.
     * <p/>
     * Overrides <code>Dialog#show()</code>.
     */
    public void show() {
        minimapEnabled.setState( Settings.minimapEnabled );
        autoEndFiring.setState( Settings.autoEndFiring );
        nagForMASC.setState( Settings.nagForMASC );
        nagForPSR.setState( Settings.nagForPSR );
        nagForNoAction.setState( Settings.nagForNoAction );
        animateMove.setState( Settings.showMoveStep );
        showWrecks.setState( Settings.showWrecks );
        soundMute.setState( Settings.soundMute );
        showMapHexPopup.setState( Settings.showMapHexPopup );
        tooltipDelay.setText( Integer.toString(Settings.tooltipDelay ) );

        // Select the correct char set (give a nice default to start).
        unitStartChar.select(0);
        for ( int loop = 0; loop < unitStartChar.getItemCount(); loop++ ) {
            if ( unitStartChar.getItem(loop).charAt(0) ==
                 Settings.unitStartChar ) {
                unitStartChar.select(loop);
                break;
            }
        }

        maxPathfinderTime.setText( Integer.toString(Settings.maxPathfinderTime ) );

        rightDragScroll.setState( Settings.rightDragScroll );
        ctlScroll.setState( Settings.ctlScroll );
        clickEdgeScroll.setState( Settings.clickEdgeScroll );
        alwaysRightClickScroll.setState( Settings.alwaysRightClickScroll );
        autoEdgeScroll.setState( Settings.autoEdgeScroll );
        scrollSensitivity.setText( Integer.toString(Settings.getScrollSensitivity() ) );

        keepServerlog.setState( Settings.keepServerlog );
        serverlogFilename.setEnabled(keepServerlog.getState());
        serverlogFilename.setText( Settings.serverlogFilename );
        serverlogMaxSize.setEnabled(keepServerlog.getState());
        serverlogMaxSize.setText( Integer.toString(Settings.serverlogMaxSize) );

        defaultAutoejectDisabled.setState( Settings.defaultAutoejectDisabled );
        showUnitId.setState( Settings.showUnitId );

        getFocus.setState( Settings.getFocus );
        super.show();
    }

    /**
     * Cancel any updates made in this dialog, and closes it.
     */
    private void cancel() {
        this.setVisible( false );
    }

    /**
     * Update the settings from this dialog's values, then closes it.
     */
    private void update() {
        Settings.minimapEnabled = minimapEnabled.getState();
        Settings.autoEndFiring =  autoEndFiring.getState();
        Settings.nagForMASC =     nagForMASC.getState();
        Settings.nagForPSR =      nagForPSR.getState();
        Settings.nagForNoAction = nagForNoAction.getState();
        Settings.showMoveStep =   animateMove.getState();
        Settings.showWrecks =     showWrecks.getState();
        Settings.soundMute =      soundMute.getState();
        Settings.showMapHexPopup= showMapHexPopup.getState();
        Settings.tooltipDelay =   Integer.parseInt(tooltipDelay.getText());
        Settings.unitStartChar=   unitStartChar.getSelectedItem().charAt(0);

        Settings.rightDragScroll        = rightDragScroll.getState();
        Settings.ctlScroll              = ctlScroll.getState();
        Settings.clickEdgeScroll        = clickEdgeScroll.getState();
        Settings.alwaysRightClickScroll = alwaysRightClickScroll.getState();
        Settings.autoEdgeScroll         = autoEdgeScroll.getState();
        Settings.setScrollSensitivity( Integer.parseInt(scrollSensitivity.getText()) );

        Settings.maxPathfinderTime      = Integer.parseInt(maxPathfinderTime.getText());

        Settings.getFocus               = getFocus.getState();

        Settings.keepServerlog          = keepServerlog.getState();
        Settings.serverlogFilename      = serverlogFilename.getText();
        Settings.serverlogMaxSize       = Integer.parseInt(serverlogMaxSize.getText());

        Settings.defaultAutoejectDisabled = defaultAutoejectDisabled.getState();
        Settings.showUnitId             = showUnitId.getState();

        Settings.save();
        this.setVisible( false );
    }

    /**
     * Handle the player pressing the action buttons.
     * <p/>
     * Implements the <code>ActionListener</code> interface.
     *
     * @param   event - the <code>ActionEvent</code> that initiated this call.
     */
    public void actionPerformed( ActionEvent event ) {
        String command = event.getActionCommand();
        if ( CommonSettingsDialog.UPDATE.equalsIgnoreCase( command ) ) {
            update();
        }
        else if ( CommonSettingsDialog.CANCEL.equalsIgnoreCase( command ) ) {
            cancel();
        }
    }

    /**
     * Handle the player clicking checkboxes.
     * <p/>
     * Implements the <code>ItemListener</code> interface.
     *
     * @param   event - the <code>ItemEvent</code> that initiated this call.
     */
    public void itemStateChanged( ItemEvent event ) {
        Object source = event.getItemSelectable();
        if ( source.equals(keepServerlog) ) {
            serverlogFilename.setEnabled(keepServerlog.getState());
            serverlogMaxSize.setEnabled(keepServerlog.getState());
        }
    }
}
