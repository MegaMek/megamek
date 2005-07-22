/*
 * MegaMek - Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
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

public class SelectArtyAutoHitHexDisplay 
    extends StatusBarPhaseDisplay
    implements BoardViewListener,  ActionListener, DoneButtoned,
               KeyListener, GameListener, Distractable
{
    // Distraction implementation.
    private DistractableAdapter distracted = new DistractableAdapter();

    // parent game
    public ClientGUI clientgui;
    private Client client;
    
    public static final String SET_HIT_HEX = "setAutoHitHex"; //$NON-NLS-1$
    
    // buttons
    private Panel             panButtons;
    
    private Button            butA;
    private Button            butDone;

    private boolean           artyEnabled;
    private Player            p;
    private Vector            artyAutoHitHexes = new Vector();

    /**
     * Creates and lays out a new deployment phase display 
     * for the specified client.
     */
    public SelectArtyAutoHitHexDisplay(ClientGUI clientgui) {
        this.clientgui = clientgui;
        this.client = clientgui.getClient();
        client.game.addGameListener(this);

        clientgui.getBoardView().addBoardViewListener(this);

        setupStatusBar(Messages.getString("SelectArtyAutoHitHexDisplay.waitingArtillery")); //$NON-NLS-1$

        p = client.getLocalPlayer();
        
        artyAutoHitHexes.insertElementAt(new Integer(p.getId()), 0);
        
        butA = new Button(Messages.getString("SelectArtyAutoHitHexDisplay.artilleryAutohithexes")); //$NON-NLS-1$
        butA.addActionListener(this);
        butA.setActionCommand(SET_HIT_HEX);
        butA.setEnabled(false);

        butDone = new Button(Messages.getString("SelectArtyAutoHitHexDisplay.Done")); //$NON-NLS-1$
        butDone.addActionListener(this);
        butDone.setEnabled(false);

        // layout button grid
        panButtons = new Panel();
        panButtons.setLayout(new GridLayout(0, 2));
        panButtons.add(butA);
        
        // layout screen
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        setLayout(gridbag);

        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;    c.weighty = 1.0;
        c.insets = new Insets(1, 1, 1, 1);

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
     * Enables relevant buttons and sets up for your turn.
     */
    private void beginMyTurn() {
        setArtyEnabled(5);
        butDone.setEnabled(true);
    }

    /**
     * Clears out old deployment data and disables relevant buttons.
     */
    private void endMyTurn() {
        // end my turn, then.
        disableButtons();
        clientgui.getBoardView().select(null);
        clientgui.getBoardView().highlight(null);
        clientgui.getBoardView().cursor(null);

    }

    /**
     * Disables all buttons in the interface
     */
    private void disableButtons() {
        setArtyEnabled(0);
        
        butDone.setEnabled(false);
    }

    private void addArtyAutoHitHex(Coords coords) {
        if (!client.game.getBoard().contains(coords)) {
            return;
        }
        if (!artyAutoHitHexes.contains(coords) && artyAutoHitHexes.size() < 6
             && clientgui.doYesNoDialog(Messages.getString("SelectArtyAutoHitHexDisplay.setArtilleryTargetDialog.title"), //$NON-NLS-1$
                     Messages.getString("SelectArtyAutoHitHexDisplay.setArtilleryTargetDialog.message", new Object[]{coords.getBoardNum()}))) { //$NON-NLS-1$
            artyAutoHitHexes.addElement(coords);
            setArtyEnabled( 6 - artyAutoHitHexes.size());
            if (artyAutoHitHexes.size() == 6) {
                setArtyEnabled(0);
            }
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
        if (!client.isMyTurn() || (b.getModifiers() & MouseEvent.BUTTON1_MASK) == 0) {
            return;
        }
        
        // check for a deployment
        clientgui.getBoardView().select(b.getCoords());
        addArtyAutoHitHex(b.getCoords());
    }

    //
    // GameListener
    //
    public void gameTurnChange(GameTurnChangeEvent e) {

        // Are we ignoring events?
        if ( this.isIgnoringEvents() ) {
            return;
        }

        endMyTurn();

        if (client.isMyTurn()) {
            beginMyTurn();
            setStatusBarText(Messages.getString("SelectArtyAutoHitHexDisplay.its_your_turn")); //$NON-NLS-1$
        } else {            
            setStatusBarText(Messages.getString("SelectArtyAutoHitHexDisplay.its_others_turn", new Object[]{e.getPlayer().getName()})); //$NON-NLS-1$
        }
    }

    public void gamePhaseChange(GamePhaseChangeEvent e) {
        // Are we ignoring events?
        if ( this.isIgnoringEvents() ) {
            return;
        }

        if ( client.isMyTurn() &&
             client.game.getPhase() != IGame.PHASE_SET_ARTYAUTOHITHEXES ) {
            endMyTurn();
        }
        if (client.game.getPhase() == IGame.PHASE_SET_ARTYAUTOHITHEXES) {
            setStatusBarText(Messages.getString("SelectArtyAutoHitHexDisplay.waitingMinefieldPhase")); //$NON-NLS-1$
        }
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
        
        if (ev.getSource().equals(butDone)) {
            endMyTurn();
            client.sendArtyAutoHitHexes(artyAutoHitHexes);
            client.sendPlayerInfo();
        }
        if (ev.getActionCommand().equals(SET_HIT_HEX)) {
            artyEnabled = true;          
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

    private void setArtyEnabled(int nbr) {
        butA.setLabel(Messages.getString("SelectArtyAutoHitHexDisplay.designatedTargets", new Object[]{new Integer(nbr)})); //$NON-NLS-1$
        butA.setEnabled(nbr > 0);
//        clientgui.getMenuBar().setSelectArtyAutoHitHexEnabled(nbr);
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
        client.game.removeGameListener(this);
        clientgui.getBoardView().removeBoardViewListener(this);
    }

}
