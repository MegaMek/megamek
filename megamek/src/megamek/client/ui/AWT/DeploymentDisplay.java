/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
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
import java.io.*;
import java.util.*;

import megamek.common.*;

public class DeploymentDisplay 
    extends AbstractPhaseDisplay
    implements BoardListener,  ActionListener,
    KeyListener, GameListener
{    
    // parent game
    public Client client;
    
    // displays
    private Label             labStatus;
    private Panel             panStatus;
    private Button            butDisplay;
    private Button            butMap;

    // buttons
    private Panel             panButtons;
    
    private Button            butNext;
    private Button            butTurn;
    private Button            butSpace;
    private Button              butLoad;
    private Button              butUnload;
    private Button            butDone;

    private int                cen;    // current entity number

    // is the shift key held?
    private boolean            turnMode = false;

    /**
     * Creates and lays out a new deployment phase display 
     * for the specified client.
     */
    public DeploymentDisplay(Client client) {
        this.client = client;
        client.addGameListener(this);


        client.game.board.addBoardListener(this);

        setupStatusBar();


        butTurn = new Button("Turn");
        butTurn.addActionListener(this);
        butTurn.setEnabled(false);
                        
        butSpace = new Button(".");
        butSpace.setEnabled(false);

        butLoad = new Button("Load");
        butLoad.addActionListener(this);
        butLoad.setEnabled(false);

        butUnload = new Button("Unload");
        butUnload.addActionListener(this);
        butUnload.setEnabled(false);

        butNext = new Button("Next Unit");
        butNext.addActionListener(this);
        butNext.setEnabled(true);

        butDone = new Button("Deploy");
        butDone.addActionListener(this);
        butDone.setEnabled(false);

        // layout button grid
        panButtons = new Panel();
        panButtons.setLayout(new GridLayout(2, 3));
        panButtons.add(butTurn);
        panButtons.add(butLoad);
        panButtons.add(butNext);
        panButtons.add(butSpace);
        panButtons.add(butUnload);
        panButtons.add(butDone);

        // layout screen
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        setLayout(gridbag);

        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;    c.weighty = 1.0;
        c.insets = new Insets(1, 1, 1, 1);
        c.gridwidth = GridBagConstraints.REMAINDER;
        addBag(client.bv, gridbag, c);

        c.weightx = 1.0;    c.weighty = 0.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        addBag(panStatus, gridbag, c);

        c.gridwidth = 1;
        c.weightx = 1.0;    c.weighty = 0.0;
        addBag(client.cb.getComponent(), gridbag, c);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 0.0;    c.weighty = 0.0;
        addBag(panButtons, gridbag, c);

        addKeyListener(this);

    }
    
    private void addBag(Component comp, GridBagLayout gridbag, GridBagConstraints c) {
        gridbag.setConstraints(comp, c);
        add(comp);
        comp.addKeyListener(this);
    }
        
    /**
     * Sets up the status bar with toggle buttons for the mek display and map.
     * TODO: remove copy/pastiness with deploy, move, fire & phys panels
     */
    private void setupStatusBar() {
        panStatus = new Panel();

        labStatus = new Label("Waiting to begin Deployment phase...", Label.CENTER);
        
        butDisplay = new Button("D");
        butDisplay.addActionListener(this);
        
        butMap = new Button("M");
        butMap.addActionListener(this);
        
        // layout
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        panStatus.setLayout(gridbag);
            
        c.insets = new Insets(0, 1, 0, 1);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;    c.weighty = 0.0;
        gridbag.setConstraints(labStatus, c);
        panStatus.add(labStatus);
        
        c.weightx = 0.0;    c.weighty = 0.0;
        gridbag.setConstraints(butDisplay, c);
        panStatus.add(butDisplay);
        
        c.gridwidth = GridBagConstraints.REMAINDER;
        panStatus.add(butMap);
    }

    /**
     * Selects an entity for deployment
     */
    public void selectEntity(int en) {
        
        // hmm, sometimes this gets called when there's no ready entities?
        if (client.game.getEntity(en) == null) {
            System.err.println("DeploymentDisplay: tried to select non-existant entity: " + en);
            return;
        }

        // okay.
        this.cen = en;

        butTurn.setEnabled(true);
        butDone.setEnabled(false);
        butLoad.setEnabled(true);
        butUnload.setEnabled(true);
        
        client.game.board.select(null);
        client.game.board.cursor(null);
        client.mechD.displayEntity(ce());
        client.mechD.showPanel("movement");
    }

    /**
     * Enables relevant buttons and sets up for your turn.
     */
    private void beginMyTurn() {
        client.setDisplayVisible(true);
        selectEntity(client.getFirstEntityNum());
        butNext.setEnabled(true);
        Player p = client.getLocalPlayer();
        // mark deployment hexes if not 'All'
        if (p.getStartingPos() != 0) {
            client.bv.markDeploymentHexesFor(p);
            client.bv.repaint(100);
        }
    }

    /**
     * Clears out old deployment data and disables relevant buttons.
     */
    private void endMyTurn() {
        // end my turn, then.
        disableButtons();
        cen = Entity.NONE;
        client.game.board.select(null);
        client.game.board.highlight(null);
        client.game.board.cursor(null);
        client.setDisplayVisible(false);
        client.bv.markDeploymentHexesFor(null);
        client.bv.repaint(100);
    }

    /**
     * Disables all buttons in the interface
     */
    private void disableButtons() {
        butTurn.setEnabled(false);
        butNext.setEnabled(false);
        butDone.setEnabled(false);
        butLoad.setEnabled(false);
        butUnload.setEnabled(false);
    }

    /**
     * Sends a deployment to the server
     */
    private void deploy() {
        disableButtons();
        client.deploy(cen, ce().getPosition(), ce().getFacing(), ce().getLoadedUnits());
    }

    /**
     * Returns the current entity.
     */
    private Entity ce() {
        return client.game.getEntity(cen);
    }

    public void die() {
        if (client.isMyTurn()) {
            endMyTurn();
        }
        client.bv.markDeploymentHexesFor(null);
        client.removeGameListener(this);
        client.game.board.removeBoardListener(this);
        client.bv.removeKeyListener(this);
        client.cb.getComponent().removeKeyListener(this);

        this.removeAll();
    }

    //
    // BoardListener
    //
    public void boardHexMoused(BoardEvent b) {
        
        if (b.getType() != BoardEvent.BOARD_HEX_DRAGGED) {
            return;
        }
        
        // ignore buttons other than 1
        if (!client.isMyTurn() || ce() == null || (b.getModifiers() & MouseEvent.BUTTON1_MASK) == 0) {
            return;
        }

        
        // check for shifty goodness
        boolean shiftheld = (b.getModifiers() & MouseEvent.SHIFT_MASK) != 0;
        
        // check for a deployment
        client.game.board.select(b.getCoords());
        Coords moveto = b.getCoords(); 
        if (ce().getPosition() != null && (shiftheld || turnMode)) { // turn
            ce().setFacing(ce().getPosition().direction(moveto));
            ce().setSecondaryFacing(ce().getFacing());
            client.bv.redrawEntity(ce());
            turnMode = false;
        }
        else if ( !client.game.board.isLegalDeployment
                  (moveto, ce().getOwner()) ||
                  ce().isHexProhibited(client.game.board.getHex(moveto)) ) {
            StringBuffer buff = new StringBuffer();
            buff.append( ce().getShortName() )
                .append( " can not deploy into " )
                .append( moveto.getBoardNum() )
                .append( "." );
            AlertDialog dlg = new AlertDialog( client.frame,
                                               "Invalid deployment",
                                               buff.toString() );
            dlg.show();
            return;
        }
        else if (client.game.getFirstEntity(moveto) != null) {
            return;
        }
        else {    
            ce().setPosition(moveto);
            client.bv.redrawEntity(ce());
            butDone.setEnabled(true);
        }
        
    }

    //
    // GameListener
    //
    public void gameTurnChange(GameEvent ev) {
        
        endMyTurn();

        if (client.isMyTurn()) {
            beginMyTurn();
            labStatus.setText("It's your turn to deploy.");
        } else {
            labStatus.setText("It's " + ev.getPlayer().getName() + 
                    "'s turn to deploy.");
        }
    }
    
    public void gamePhaseChange(GameEvent ev) {
        if (client.game.phase != Game.PHASE_DEPLOYMENT) {
            die();
        }
    }

    //
    // ActionListener
    //
    public void actionPerformed(ActionEvent ev) {
        if (ev.getSource() == butDisplay) {
            client.toggleDisplay();
        }
        else if (ev.getSource() == butMap) {
            client.toggleMap();
        }
        
        if (!client.isMyTurn()) {
            // odd...
            return;
        }
        
        if (ev.getSource() == butDone) {
            deploy();
        } else if (ev.getSource() == butNext) { 
            ce().setPosition(null);
            client.bv.redrawEntity(ce());
            // Unload any loaded units.
            Enumeration iter =  ce().getLoadedUnits().elements();
            while ( iter.hasMoreElements() ) {
                Entity other = (Entity) iter.nextElement();
                // Please note, the Server never got this unit's load orders.
                ce().unload( other );
                other.setTransportId( Entity.NONE );
                other.newRound();
            }
            selectEntity(client.getNextEntityNum(cen));
        } else if (ev.getSource() == butTurn) {
            turnMode = true;
        } 
        else if ( ev.getSource() == butLoad ) {

            // What undeployed units can we load?
            Vector choices = new Vector();
            int otherId = client.getNextEntityNum( cen );
            Entity other = client.getEntity( otherId );
            while ( otherId != cen ) {

                // Is the other entity deployed?
                if ( other.getPosition() == null ) {

                    // Can the current entity load the other entity?
                    if ( ce().canLoad( other ) ) {
                        choices.addElement( other );
                    }

                } // End other not yet deployed.

                // Check the next entity.
                otherId = client.getNextEntityNum( otherId );
                other = client.getEntity( otherId );

            } // End have list of choices.

            // Do we have anyone to load?
            if ( choices.size() > 0 ) {
                String[] names = new String[ choices.size() ];
                StringBuffer question = new StringBuffer();
                question.append( ce().getShortName() );
                question.append( " has the following unused space:\n" );
                question.append( ce().getUnusedString() );
                question.append( "\n\nWhich unit do you want to load?" );
                for ( int loop = 0; loop < names.length; loop++ ) {
                    names[loop] = ( (Entity)choices.elementAt(loop) ).getShortName();
                }
                SingleChoiceDialog choiceDialog =
                    new SingleChoiceDialog( client.frame,
                                            "Load Unit",
                                            question.toString(),
                                            names );
                choiceDialog.show();
                if ( choiceDialog.getAnswer() == true ) {
                    other = (Entity) choices.elementAt( choiceDialog.getChoice() );
                    // Please note, the Server may never get this load order.
                    ce().load( other );
                    other.setTransportId( cen );
                    client.mechD.displayEntity(ce());
                }
            } // End have-choices
            else {
                AlertDialog alert = new AlertDialog( client.frame,
                                                     "Load Unit",
                                                     ce().getShortName() + " can not load any of the remaining units." );
                alert.show();
            }

        } // End load-unit

        else if ( ev.getSource() == butUnload ) {

            // Do we have anyone to unload?
            Vector choices = ce().getLoadedUnits();
            if ( choices.size() > 0 ) {
                Entity other = null;
                String[] names = new String[ choices.size() ];
                StringBuffer question = new StringBuffer();
                question.append( ce().getShortName() );
                question.append( " has the following unused space:\n" );
                question.append( ce().getUnusedString() );
                question.append( "\n\nWhich unit do you want to unload?" );
                for ( int loop = 0; loop < names.length; loop++ ) {
                    names[loop] = ( (Entity)choices.elementAt(loop) ).getShortName();
                }
                SingleChoiceDialog choiceDialog =
                    new SingleChoiceDialog( client.frame,
                                            "Unload Unit",
                                            question.toString(),
                                            names );
                choiceDialog.show();
                if ( choiceDialog.getAnswer() == true ) {
                    other = (Entity) choices.elementAt( choiceDialog.getChoice() );
                    // Please note, the Server never got this load order.
                    if ( ce().unload( other ) ) {
                        other.setTransportId( Entity.NONE );
                        other.newRound();
                        client.mechD.displayEntity(ce());
                    }
                    else {
                        System.out.println( "Could not unload " +
                                            other.getShortName() +
                                            " from " + ce().getShortName() );
                    }
                }
            } // End have-choices
            else {
                AlertDialog alert = new AlertDialog( client.frame,
                                                     "Unload Unit",
                                                     ce().getShortName() + " is not transporting any units." );
                alert.show();
            }

        } // End unload-unit

    } // End public void actionPerformed(ActionEvent ev)
    

    //
    // KeyListener
    //
    public void keyPressed(KeyEvent ev) {
    }

    public void keyReleased(KeyEvent ev) {
    }

    public void keyTyped(KeyEvent ev) {
        ;
    }
}
