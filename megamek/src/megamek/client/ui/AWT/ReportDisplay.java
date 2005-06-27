/**
 * MegaMek - Copyright (C) 2000,2001,2002,2003,2004 Ben Mazur (bmazur@sev.org)
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

import java.awt.*;
import java.awt.event.*;

import megamek.client.Client;
import megamek.common.*;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.util.Distractable;
import megamek.common.util.DistractableAdapter;

public class ReportDisplay 
    extends StatusBarPhaseDisplay
    implements ActionListener, KeyListener, DoneButtoned, Distractable
{
    // Distraction implementation.
    private DistractableAdapter distracted = new DistractableAdapter();

    // parent game
    public Client client;
    
//     // chatterbox keeps track of chatting and other messages
//     private ChatterBox        cb;
    
    // displays
    private TextArea        rta;
    
    private Window            mechw;
    private MechDisplay        mechd;
    private    boolean            mechdOn;
    
    // buttons
    private Button            readyB;
    private Button            rerollInitiativeB;
    
    // let's keep track of what we're moving, too
    private int                cen;    // current entity number
    private MovePath    md;        // movement data
    private MovePath    cmd;    // considering movement data
    
    private boolean rerolled; //have we rerolled an init?
    
    /**
     * Creates and lays out a new movement phase display 
     * for the specified client.
     */
    public ReportDisplay(Client client) {
        this.client = client;

        client.game.addGameListener( this );

//         cb = client.cb;
        
        rta = new TextArea(client.eotr, 40, 25, TextArea.SCROLLBARS_VERTICAL_ONLY);
        rta.setEditable(false);
        
        setupStatusBar( "" ); //$NON-NLS-1$
        
        readyB = new Button(Messages.getString("ReportDisplay.Done")); //$NON-NLS-1$
        readyB.setActionCommand("ready"); //$NON-NLS-1$
        readyB.addActionListener(this);
        
        rerollInitiativeB = new Button(Messages.getString("ReportDisplay.Reroll")); //$NON-NLS-1$
        rerollInitiativeB.setActionCommand("reroll_initiative"); //$NON-NLS-1$
        rerollInitiativeB.addActionListener(this);
        
        // layout screen
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        setLayout(gridbag);
        
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;    c.weighty = 1.0;
        c.insets = new Insets(1, 1, 1, 1);
        c.gridwidth = GridBagConstraints.REMAINDER;
        addBag(rta, gridbag, c);

//         c.gridwidth = 1;
//         c.weightx = 1.0;    c.weighty = 0.0;
//         addBag(cb.getComponent(), gridbag, c);

        c.gridwidth = 1;
        c.weightx = 0.0;    c.weighty = 0.0;
        Panel panButtons = new Panel();
        panButtons.setLayout( new GridLayout(1, 8) );
        panButtons.add(rerollInitiativeB);
        for ( int padding = 0; padding < 6; padding++ ) {
            panButtons.add( new Label( "" ) ); //$NON-NLS-1$
        }
        addBag( panButtons, gridbag, c );

//         c.weightx = 1.0;    c.weighty = 0.0;
//         c.gridwidth = GridBagConstraints.REMAINDER;
//         addBag(panStatus, gridbag, c);

//         c.gridwidth = GridBagConstraints.REMAINDER;
//         c.weightx = 0.0;    c.weighty = 0.0;
//         addBag(readyB, gridbag, c);

        addKeyListener(this);
        
    }

    private void addBag(Component comp, GridBagLayout gridbag, GridBagConstraints c) {
        gridbag.setConstraints(comp, c);
        add(comp);
        comp.addKeyListener(this);
    }

    /**
     * Show or hide the "reroll inititiative" button in this report display.
     *
     * @param   show a <code>boolean</code> that indicates that the button
     *          should be shown in this report display.
     */
    public void showRerollButton( boolean show ) {
        rerollInitiativeB.setVisible( show );
    }

    /**
     * Sets you as ready and disables the ready button.
     */
    public void ready() {
        rerollInitiativeB.setEnabled(false);
        readyB.setEnabled(false);
        client.sendDone(true);
    }

    /**
     * Requests an initiative reroll and disables the ready button.
     */
    public void rerollInitiative() {
        rerolled=true;
        rerollInitiativeB.setEnabled(false);
        readyB.setEnabled(false);
        client.sendRerollInitiativeRequest();
    }
    /**
     * have we rerolled init this round?
     */
    public boolean hasRerolled() {
        return rerolled;
    }
    
    public void resetButtons() {
        resetReadyButton();
        if (client.game.getPhase() == IGame.PHASE_INITIATIVE
            && client.game.hasTacticalGenius(client.getLocalPlayer())) {
            showRerollButton(true);
        } else {
            showRerollButton(false);
        }
        rerollInitiativeB.setEnabled(true);
    }

    public void resetReadyButton() {
        readyB.setEnabled(true);
    }
    
    public void resetRerollButton() {
        rerollInitiativeB.setEnabled(true);
    }

    /**
     * Refreshes the report
     */
    public void refresh() {
        rta.setText(client.eotr);
    }
    
    //
    // ActionListener
    //
    public void actionPerformed(ActionEvent ev) {
        if(ev.getActionCommand().equalsIgnoreCase("ready")) { //$NON-NLS-1$
            ready();
        }
        if(ev.getActionCommand().equalsIgnoreCase("reroll_initiative")) { //$NON-NLS-1$
            rerollInitiative();
        }
    }
    

    //
    // KeyListener
    //
    public void keyPressed(KeyEvent ev) {
        if(ev.getKeyCode() == KeyEvent.VK_ESCAPE) {
        }
        if(ev.getKeyCode() == KeyEvent.VK_ENTER && ev.isControlDown()) {
            ready();
        }
    }
    public void keyReleased(KeyEvent ev) {
        ;
    }
    public void keyTyped(KeyEvent ev) {
        ;
    }

    public void gamePhaseChange(GamePhaseChangeEvent e){

        // Are we ignoring events?
        if ( this.isIgnoringEvents() ) {
            return;
        }

        refresh();
        resetButtons();
        rerolled=false;
    }

    /**
     * Determine if the listener is currently distracted.
     *
     * @return  <code>true</code> if the listener is ignoring events.
     */
    public boolean isIgnoringEvents() {
        return this.distracted.isIgnoringEvents();
    }

    /**
     * Specify if the listener should be distracted.
     *
     * @param   distract <code>true</code> if the listener should ignore events
     *          <code>false</code> if the listener should pay attention again.
     *          Events that occured while the listener was distracted NOT
     *          going to be processed.
     */
    public void setIgnoringEvents( boolean distracted ) {
        this.distracted.setIgnoringEvents( distracted );
    }

    /**
     * Stop just ignoring events and actually stop listening to them.
     */
    public void removeAllListeners() {
        client.game.removeGameListener(this);
    }

    /**
     * Retrieve the "Done" button of this object.
     *
     * @return  the <code>java.awt.Button</code> that activates this
     *          object's "Done" action.
     */
    public Button getDoneButton() {
        return readyB;
    }

    /**
     * Get the secondary display section of this phase.
     *
     * @return  the <code>Component</code> which is displayed in the
     *          secondary section during this phase.
     */
    public Component getSecondaryDisplay() {
        return panStatus;
    }

}
