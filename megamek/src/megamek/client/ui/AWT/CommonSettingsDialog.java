/*
 * MegaMek - Copyright (C) 2003, 2004 Ben Mazur (bmazur@sev.org)
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
import java.util.*;
import java.io.*;

import megamek.common.Settings;

public class CommonSettingsDialog extends Dialog implements ActionListener {

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
    private TextField   shiftScrollSensitivity;
    private Choice      unitStartChar;

    private static final String CANCEL = "CANCEL";
    private static final String UPDATE = "UPDATE";

    /**
     * Standard constructor.  There is no default constructor for this class.
     *
     * @param   owner - the <code>Frame</code> that owns this dialog.
     */
    public CommonSettingsDialog( Frame owner) {
        // Initialize our superclass with a title.
        super( owner, "Client Settings" );

        // Lay out this dialog.
        this.setLayout( new GridLayout(0, 1) );

        // Add the setting controls.
        Panel panSetting;
        minimapEnabled
            = new Checkbox( "The minimap can be shown." );
        this.add( minimapEnabled );
        autoEndFiring
            = new Checkbox( "Skip 'Done' when firing all weapons." );
        this.add( autoEndFiring );
        nagForMASC
            = new Checkbox( "Confirm all movement that uses MASC." );
        this.add( nagForMASC );
        nagForPSR
            = new Checkbox( "Confirm all movement that requires a PSR." );
        this.add( nagForPSR );
        nagForNoAction
            = new Checkbox( "Confirm done when no movement/firing/physicals declared." );
        this.add( nagForNoAction );
        animateMove
            = new Checkbox( "Animate movement." );
        this.add( animateMove );
        showWrecks
            = new Checkbox( "Show wrecks." );
        this.add( showWrecks );
        soundMute
            = new Checkbox( "Mute sound." );
        this.add( soundMute );
        showMapHexPopup
            = new Checkbox( "Show map hex popup." );
        this.add( showMapHexPopup );

        panSetting = new Panel();
        tooltipDelay
            = new TextField(4);
        panSetting.add( tooltipDelay );
        panSetting.add( new Label("Tooltip popup delay.") );
        this.add( panSetting );

        panSetting = new Panel();
        unitStartChar
            = new Choice();
        // Add option for "A, B, C, D..."
        unitStartChar.addItem( "\u0041, \u0042, \u0043, \u0044..." );
        // Add option for "ALPHA, BETA, GAMMA, DELTA..."
        unitStartChar.addItem( "\u0391, \u0392, \u0393, \u0394..." );
        // Add option for "alpha, beta, gamma, delta..."
        unitStartChar.addItem( "\u03B1, \u03B2, \u03B3, \u03B4..." );
        panSetting.add( unitStartChar );
        panSetting.add( new Label("ProtoMech unit codes.") );
        this.add( panSetting );

        panSetting = new Panel();
        shiftScrollSensitivity
            = new TextField(4);
        panSetting.add( shiftScrollSensitivity );
        panSetting.add( new Label("Shift-Scroll sensitivity.") );
        this.add( panSetting );

        // Add the dialog controls.
        Panel buttons = new Panel();
        buttons.setLayout( new GridLayout(1, 0) );
        buttons.add( new Label() );
        Button update = new Button( "Update" );
        update.setActionCommand( CommonSettingsDialog.UPDATE );
        update.addActionListener( this );
        buttons.add( update );
        buttons.add( new Label() );
        Button cancel = new Button( "Cancel" );
        cancel.setActionCommand( CommonSettingsDialog.CANCEL );
        cancel.addActionListener( this );
        buttons.add( cancel );
        buttons.add( new Label() );
        this.add( buttons );

        // Close this dialog when the window manager says to.
        addWindowListener(new WindowAdapter() {
	    public void windowClosing(WindowEvent e) { cancel(); }
	});

        // Center this dialog.
        pack();
        setResizable(false);
        setLocation(owner.getLocation().x + owner.getSize().width/2 - getSize().width/2,
                    owner.getLocation().y + owner.getSize().height/2 - getSize().height/2);

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

        shiftScrollSensitivity.setText( Integer.toString(Settings.shiftScrollSensitivity ) );

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
        Settings.shiftScrollSensitivity =   Integer.parseInt(shiftScrollSensitivity.getText());
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

}
