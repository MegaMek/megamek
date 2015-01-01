/*
 * MegaMek - Copyright (C) 2000,2001,2002,2003,2004,2005,2006 Ben Mazur (bmazur@sev.org)
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
import java.util.*;

import megamek.client.Client;
import megamek.client.event.BoardViewEvent;
import megamek.client.event.BoardViewListener;
import megamek.common.*;
import megamek.common.event.GameListener;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.event.GameTurnChangeEvent;
import megamek.common.util.Distractable;
import megamek.common.util.DistractableAdapter;

public class DeploymentDisplay 
    extends StatusBarPhaseDisplay
    implements BoardViewListener,  ActionListener, DoneButtoned,
               KeyListener, GameListener, Distractable
{
    // Distraction implementation.
    private DistractableAdapter distracted = new DistractableAdapter();

    // Action command names
    public static final String DEPLOY_TURN        = "deployTurn"; //$NON-NLS-1$
    public static final String DEPLOY_NEXT        = "deployNext"; //$NON-NLS-1$
    public static final String DEPLOY_LOAD        = "deployLoad"; //$NON-NLS-1$
    public static final String DEPLOY_UNLOAD      = "deployUnload"; //$NON-NLS-1$
    public static final String DEPLOY_REMOVE      = "deployRemove"; //$NON-NLS-1$
    public static final String DEPLOY_ASSAULTDROP = "assaultDrop"; //$NON-NLS-1$

    // parent game
    public Client client;
    private ClientGUI clientgui;
    
    // buttons
    private Panel             panButtons;
    
    private Button            butNext;
    private Button            butTurn;
//     private Button            butSpace;
//     private Button            butSpace2;
//     private Button            butSpace3;
    private Button            butLoad;
    private Button            butUnload;
    private Button            butRemove;
    private Button            butAssaultDrop;
    private Button            butDone;

    private int               cen = Entity.NONE;    // current entity number

    // is the shift key held?
    private boolean            turnMode = false;
    
    private boolean            assaultDropPreference = false;

    /**
     * Creates and lays out a new deployment phase display 
     * for the specified client.
     */
    public DeploymentDisplay(ClientGUI clientgui) {
        this.clientgui = clientgui;
        this.client = clientgui.getClient();
        client.game.addGameListener(this);
        clientgui.getBoardView().addBoardViewListener(this);

        setupStatusBar(Messages.getString("DeploymentDisplay.waitingForDeploymentPhase")); //$NON-NLS-1$


        butTurn = new Button(Messages.getString("DeploymentDisplay.Turn")); //$NON-NLS-1$
        butTurn.addActionListener(this);
        butTurn.setActionCommand(DEPLOY_TURN);
        butTurn.setEnabled(false);
                        
//         butSpace = new Button(".");
//         butSpace.setEnabled(false);

//         butSpace2 = new Button(".");
//         butSpace2.setEnabled(false);

//         butSpace3 = new Button(".");
//         butSpace3.setEnabled(false);

        butLoad = new Button(Messages.getString("DeploymentDisplay.Load")); //$NON-NLS-1$
        butLoad.addActionListener(this);
        butLoad.setActionCommand(DEPLOY_LOAD);
        butLoad.setEnabled(false);

        butUnload = new Button(Messages.getString("DeploymentDisplay.Unload")); //$NON-NLS-1$
        butUnload.addActionListener(this);
        butUnload.setActionCommand(DEPLOY_UNLOAD);
        butUnload.setEnabled(false);

        butNext = new Button(Messages.getString("DeploymentDisplay.NextUnit")); //$NON-NLS-1$
        butNext.addActionListener(this);
        butNext.setActionCommand(DEPLOY_NEXT);
        butNext.setEnabled(true);
        
        butRemove = new Button(Messages.getString("DeploymentDisplay.Remove")); //$NON-NLS-1$
        butRemove.addActionListener(this);
        butRemove.setActionCommand(DEPLOY_REMOVE);
        setRemoveEnabled(true);

        butAssaultDrop = new Button(Messages.getString("DeploymentDisplay.AssaultDropOn")); //$NON-NLS-1$
        butAssaultDrop.addActionListener(this);
        butAssaultDrop.setActionCommand(DEPLOY_ASSAULTDROP);
        butAssaultDrop.setEnabled(false);

        butDone = new Button(Messages.getString("DeploymentDisplay.Deploy")); //$NON-NLS-1$
        butDone.addActionListener(this);
        butDone.setEnabled(false);

        // layout button grid
        panButtons = new Panel();
        panButtons.setLayout(new GridLayout(0, 8));
        panButtons.add(butNext);
        panButtons.add(butTurn);
        panButtons.add(butLoad);
        panButtons.add(butUnload);
        panButtons.add(butRemove);
        panButtons.add(butAssaultDrop);
//         panButtons.add(butSpace);
//         panButtons.add(butSpace2);
//         panButtons.add(butSpace3);
//         panButtons.add(butDone);

        // layout screen
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        setLayout(gridbag);

        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;    c.weighty = 1.0;
        c.insets = new Insets(1, 1, 1, 1);
//         c.gridwidth = GridBagConstraints.REMAINDER;
//         addBag(clientgui.bv, gridbag, c);

//         c.weightx = 1.0;    c.weighty = 0;
//         c.gridwidth = 1;
//         addBag(client.cb.getComponent(), gridbag, c);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 0.0;    c.weighty = 0.0;
        addBag(panButtons, gridbag, c);

        c.weightx = 1.0;    c.weighty = 0.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        addBag(panStatus, gridbag, c);

        clientgui.bv.addKeyListener( this );
        addKeyListener(this);

    }
    
    private void addBag(Component comp, GridBagLayout gridbag, GridBagConstraints c) {
        gridbag.setConstraints(comp, c);
        add(comp);
        comp.addKeyListener(this);
    }
        
    /**
     * Selects an entity for deployment
     */
    public void selectEntity(int en) {
        
        // hmm, sometimes this gets called when there's no ready entities?
        if (client.game.getEntity(en) == null) {
            System.err.println("DeploymentDisplay: tried to select non-existant entity: " + en); //$NON-NLS-1$
            return;
        }
        
        // remove C3 sprites from earlier here, or we might crash when
        // trying to draw a c3 sprite belonging to the previously selected,
        // but not deployed entity
        clientgui.bv.clearC3Networks();        

        this.cen = en;
        clientgui.setSelectedEntityNum(en);

        setTurnEnabled(true);
        butDone.setEnabled(false);
        setLoadEnabled(true);
        setUnloadEnabled(true);
        
        clientgui.getBoardView().select(null);
        clientgui.getBoardView().cursor(null);
        // RACE : if player clicks fast enough, ce() is null.
        if ( null != ce() ) {
            // set facing according to starting position
            switch (client.getLocalPlayer().getStartingPos()) {
            case 8:
                ce().setFacing(1);
                ce().setSecondaryFacing(1);
                break;
            case 7:
                ce().setFacing(1);
                ce().setSecondaryFacing(1);
                break;
            case 6:
                ce().setFacing(0);
                ce().setSecondaryFacing(0);
                break;
            case 5:
                ce().setFacing(5);
                ce().setSecondaryFacing(5);
                break;
            case 4:
                ce().setFacing(5);
                ce().setSecondaryFacing(5);
                break;
            case 3:
                ce().setFacing(4);
                ce().setSecondaryFacing(4);
                break;
            case 2:
                ce().setFacing(3);
                ce().setSecondaryFacing(3);
                break;
            case 1:
                ce().setFacing(2);
                ce().setSecondaryFacing(2);
                break;
            case 0:
                ce().setFacing(0);
                ce().setSecondaryFacing(0);
                break;
            }
            
            setAssaultDropEnabled(ce().canAssaultDrop() 
                    && ce().getGame().getOptions().booleanOption("assault_drop"));
            
            clientgui.mechD.displayEntity(ce());
            clientgui.mechD.showPanel("movement"); //$NON-NLS-1$
        
            // Update the menu bar.
            clientgui.getMenuBar().setEntity( ce() );
        }
    }

    /**
     * Enables relevant buttons and sets up for your turn.
     */
    private void beginMyTurn() {
        clientgui.setDisplayVisible(true);
        selectEntity(client.getFirstDeployableEntityNum());
        setNextEnabled(true);
        Player p = client.getLocalPlayer();
        // mark deployment hexes
        clientgui.bv.markDeploymentHexesFor(p);
        clientgui.bv.repaint(100);
    }

    /**
     * Clears out old deployment data and disables relevant buttons.
     */
    private void endMyTurn() {
        // end my turn, then.
        disableButtons();
        Entity next = client.game.getNextEntity( client.game.getTurnIndex() );
        if ( IGame.PHASE_DEPLOYMENT == client.game.getPhase()
             && null != next
             && null != ce()
             && next.getOwnerId() != ce().getOwnerId() ) {
            clientgui.setDisplayVisible(false);
        }
        cen = Entity.NONE;
        clientgui.getBoardView().select(null);
        clientgui.getBoardView().highlight(null);
        clientgui.getBoardView().cursor(null);
        clientgui.bv.markDeploymentHexesFor(null);
        clientgui.bv.repaint(100);
    }

    /**
     * Disables all buttons in the interface
     */
    private void disableButtons() {
        setTurnEnabled(false);
        setNextEnabled(false);
        butDone.setEnabled(false);
        setLoadEnabled(false);
        setUnloadEnabled(false);
    }

    /**
     * Sends a deployment to the server
     */
    private void deploy() {
        disableButtons();
        
        Entity en = ce();
        client.deploy(cen, en.getPosition(), en.getFacing(), en.getLoadedUnits(), assaultDropPreference);
        en.setDeployed(true);
    }
    
    /**
     * Sends an entity removal to the server
     */
    private void remove() {
        disableButtons();
        client.sendDeleteEntity(cen);
        selectEntity(client.getNextDeployableEntityNum(cen));
        if (client.getNextDeployableEntityNum(cen) == -1) {
            butNext.setEnabled(false);
            this.endMyTurn();
        }
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
        clientgui.bv.markDeploymentHexesFor(null);
        client.game.removeGameListener(this);
        clientgui.getBoardView().removeBoardViewListener(this);
        
        this.removeAll();
    }

    public void gameTurnChange(GameTurnChangeEvent e) {                
        // Are we ignoring events?
        if (isIgnoringEvents() ) {
            return;
        }
        if (client.isMyTurn()) {
            beginMyTurn();
            setStatusBarText(Messages.getString("DeploymentDisplay.its_your_turn")); //$NON-NLS-1$
        } else {
            endMyTurn();
            setStatusBarText(Messages.getString("DeploymentDisplay.its_others_turn", new Object[]{e.getPlayer().getName()})); //$NON-NLS-1$
        }
    }
    
    public void gamePhaseChange(GamePhaseChangeEvent e) {
        DeploymentDisplay.this.clientgui.bv.markDeploymentHexesFor(null);
        // Are we ignoring events?
        if ( this.isIgnoringEvents() ) {
            return;
        }

        if (client.game.getPhase() == IGame.PHASE_DEPLOYMENT) {
            setStatusBarText(Messages.getString("DeploymentDisplay.waitingForDeploymentPhase")); //$NON-NLS-1$
        }
    }

    //
    // BoardListener
    //
    public void hexMoused(BoardViewEvent b) {

        // Are we ignoring events?
        if ( this.isIgnoringEvents() ) {
            return;
        }

        if (b.getType() != BoardViewEvent.BOARD_HEX_DRAGGED) {
            return;
        }
        
        // ignore buttons other than 1
        if (!client.isMyTurn() || ce() == null || (b.getModifiers() & MouseEvent.BUTTON1_MASK) == 0) {
            return;
        }

        // control pressed means a line of sight check.
        // added ALT_MASK by kenn
        if ((b.getModifiers() & InputEvent.CTRL_MASK) != 0 || (b.getModifiers() & InputEvent.ALT_MASK) != 0) {
            return;
        }

        // check for shifty goodness
        boolean shiftheld = (b.getModifiers() & MouseEvent.SHIFT_MASK) != 0;
        
        // check for a deployment
        Coords moveto = b.getCoords(); 
        if (ce().getPosition() != null && (shiftheld || turnMode)) { // turn
            ce().setFacing(ce().getPosition().direction(moveto));
            ce().setSecondaryFacing(ce().getFacing());
            clientgui.bv.redrawEntity(ce());
            turnMode = false;
        } else if (!(client.game.getBoard().isLegalDeployment(moveto, ce().getOwner())
                || assaultDropPreference)
                || ce().isHexProhibited(client.game.getBoard().getHex(moveto))) {
            AlertDialog dlg = new AlertDialog( clientgui.frame,
                                               Messages.getString("DeploymentDisplay.alertDialog.title"), //$NON-NLS-1$
                                               Messages.getString("DeploymentDisplay.cantDeployInto", new Object[]{ce().getShortName(), moveto.getBoardNum()})); //$NON-NLS-1$
            dlg.setVisible(true);
            return;
        } else if (Compute.stackingViolation(client.game, ce().getId(), moveto) != null) {
            // check if deployed unit violates stacking
            return;
        } else {    
            ce().setPosition(moveto);
            clientgui.bv.redrawEntity(ce());
            butDone.setEnabled(true);
        }
        clientgui.getBoardView().select( moveto );
    }

    
    //
    // ActionListener
    //
    public void actionPerformed(ActionEvent ev) {

        // Are we ignoring events?
        if ( this.isIgnoringEvents() ) {
            return;
        }

        if ( statusBarActionPerformed(ev, client) )
            return;
          
        if (!client.isMyTurn()) {
            // odd...
            return;
        }
        
        if (ev.getSource() == butDone) {
            deploy();
        } else if (ev.getActionCommand().equals(DEPLOY_NEXT)) { 
            ce().setPosition(null);
            clientgui.bv.redrawEntity(ce());
            // Unload any loaded units.
            Enumeration iter =  ce().getLoadedUnits().elements();
            while ( iter.hasMoreElements() ) {
                Entity other = (Entity) iter.nextElement();
                // Please note, the Server never got this unit's load orders.
                ce().unload( other );
                other.setTransportId( Entity.NONE );
                other.newRound(client.game.getRoundCount());
            }
            
            selectEntity(client.getNextDeployableEntityNum(cen));
        } else if (ev.getActionCommand().equals(DEPLOY_TURN)) {
            turnMode = true;
        } 
        else if (ev.getActionCommand().equals(DEPLOY_LOAD)) {

            // What undeployed units can we load?
            Vector choices = new Vector();
            Enumeration entities = client.game.getEntities();
            Entity other;
            while ( entities.hasMoreElements() ) {
                other = (Entity) entities.nextElement();
                if ( other.isSelectableThisTurn()
                     && ce().canLoad(other) ) {
                    choices.addElement( other );
                }
            }

            // Do we have anyone to load?
            if ( choices.size() > 0 ) {
                String[] names = new String[ choices.size() ];
                for ( int loop = 0; loop < names.length; loop++ ) {
                    names[loop] = ( (Entity)choices.elementAt(loop) ).getShortName();
                }
                SingleChoiceDialog choiceDialog =
                    new SingleChoiceDialog( clientgui.frame,
                                            Messages.getString("DeploymentDisplay.loadUnitDialog.title"), //$NON-NLS-1$
                                            Messages.getString("DeploymentDisplay.loadUnitDialog.message", new Object[]{ce().getShortName(), ce().getUnusedString()}), //$NON-NLS-1$
                                            names );
                choiceDialog.setVisible(true);
                if ( choiceDialog.getAnswer() == true ) {
                    other = (Entity) choices.elementAt( choiceDialog.getChoice() );
                    // Please note, the Server may never get this load order.
                    ce().load( other );
                    other.setTransportId( cen );
                    clientgui.mechD.displayEntity(ce());
                }
            } // End have-choices
            else {
                AlertDialog alert = new AlertDialog( clientgui.frame,
                                                     Messages.getString("DeploymentDisplay.allertDialog1.title"), //$NON-NLS-1$
                                                     Messages.getString("DeploymentDisplay.allertDialog1.message", new Object[]{ce().getShortName()})); //$NON-NLS-1$
                alert.setVisible(true);
            }

        } // End load-unit

        else if (ev.getActionCommand().equals(DEPLOY_UNLOAD)) {

            // Do we have anyone to unload?
            Vector choices = ce().getLoadedUnits();
            if ( choices.size() > 0 ) {
                Entity other = null;
                String[] names = new String[ choices.size() ];
                for ( int loop = 0; loop < names.length; loop++ ) {
                    names[loop] = ( (Entity)choices.elementAt(loop) ).getShortName();
                }
                SingleChoiceDialog choiceDialog =
                    new SingleChoiceDialog( clientgui.frame,
                                            Messages.getString("DeploymentDisplay.unloadUnitDialog.title"), //$NON-NLS-1$
                                            Messages.getString("DeploymentDisplay.unloadUnitDialog.message", new Object[]{ce().getShortName(), ce().getUnusedString()}), //$NON-NLS-1$
                                            names );
                choiceDialog.setVisible(true);
                if ( choiceDialog.getAnswer() == true ) {
                    other = (Entity) choices.elementAt( choiceDialog.getChoice() );
                    // Please note, the Server never got this load order.
                    if ( ce().unload( other ) ) {
                        other.setTransportId( Entity.NONE );
                        other.newRound(client.game.getRoundCount());
                        clientgui.mechD.displayEntity(ce());
                    }
                    else {
                        System.out.println( "Could not unload " + //$NON-NLS-1$
                                            other.getShortName() +
                                            " from " + ce().getShortName() ); //$NON-NLS-1$
                    }
                }
            } // End have-choices
            else {
                AlertDialog alert = new AlertDialog( clientgui.frame,
                                                     Messages.getString("DeploymentDisplay.allertDialog2.title"), //$NON-NLS-1$
                                                     Messages.getString("DeploymentDisplay.allertDialog2.message",new Object[]{ce().getShortName()})); //$NON-NLS-1$
                alert.setVisible(true);
            }

        } // End unload-unit
        
        else if (ev.getActionCommand().equals(DEPLOY_REMOVE)) {
            remove();
        }
        
        else if (ev.getActionCommand().equals(DEPLOY_ASSAULTDROP)) {
            assaultDropPreference = !assaultDropPreference;
            if(assaultDropPreference) {
                butAssaultDrop.setLabel(Messages.getString("DeploymentDisplay.AssaultDropOff"));
            } else {
                butAssaultDrop.setLabel(Messages.getString("DeploymentDisplay.AssaultDropOn"));
            }
        }

    } // End public void actionPerformed(ActionEvent ev)
    

    //
    // KeyListener
    //
    public void keyPressed(KeyEvent ev) {
    }

    public void keyReleased(KeyEvent ev) {
    }

    public void keyTyped(KeyEvent ev) {
    }

    //
    // BoardViewListener
    //
    public void finishedMovingUnits(BoardViewEvent b) {
    }
    
    // Selected a unit in the unit overview.
    public void unitSelected(BoardViewEvent b) {

        // Are we ignoring events?
        if ( this.isIgnoringEvents() ) {
            return;
        }

        Entity e = client.game.getEntity(b.getEntityId());
        if ( null == e ) {
            return;
        }
        if (client.isMyTurn()) {
            if ( client.game.getTurn().isValidEntity(e,client.game) ) {
                if (ce() != null) {
                    ce().setPosition(null);
                    clientgui.bv.redrawEntity(ce());
                    // Unload any loaded units.
                    Enumeration iter =  ce().getLoadedUnits().elements();
                    while ( iter.hasMoreElements() ) {
                        Entity other = (Entity) iter.nextElement();
                        // Please note, the Server never got this unit's load orders.
                        ce().unload( other );
                        other.setTransportId( Entity.NONE );
                        other.newRound(client.game.getRoundCount());
                    }
                }
                
                selectEntity(e.getId());
                if ( null != e.getPosition() ) {
                    clientgui.bv.centerOnHex(e.getPosition());
                }
            }
        } else {
            clientgui.setDisplayVisible(true);
            clientgui.mechD.displayEntity(e);
            if (e.isDeployed()) {
                clientgui.bv.centerOnHex(e.getPosition());
            }
        }
    }

    private void setNextEnabled(boolean enabled) {
        butNext.setEnabled(enabled);
        clientgui.getMenuBar().setDeployNextEnabled(enabled);
    }
    private void setTurnEnabled(boolean enabled) {
        butTurn.setEnabled(enabled);
        clientgui.getMenuBar().setDeployTurnEnabled(enabled);
    }
    private void setLoadEnabled(boolean enabled) {
        butLoad.setEnabled(enabled);
        clientgui.getMenuBar().setDeployLoadEnabled(enabled);
    }
    private void setUnloadEnabled(boolean enabled) {
        butUnload.setEnabled(enabled);
        clientgui.getMenuBar().setDeployUnloadEnabled(enabled);
    }
    private void setRemoveEnabled(boolean enabled) {
        butRemove.setEnabled(enabled);
        clientgui.getMenuBar().setDeployNextEnabled(enabled);
    }
    private void setAssaultDropEnabled(boolean enabled) {
        butAssaultDrop.setEnabled(enabled);
        clientgui.getMenuBar().setDeployAssaultDropEnabled(enabled);
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
     * Retrieve the "Done" button of this object.
     *
     * @return  the <code>java.awt.Button</code> that activates this
     *          object's "Done" action.
     */
    public Button getDoneButton() {
        return butDone;
    }
    
    /**
     * Stop just ignoring events and actually stop listening to them.
     */
    public void removeAllListeners() {
        die();
    }

}
