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

package megamek.client;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import megamek.common.*;
import megamek.common.util.Distractable;
import megamek.common.util.DistractableAdapter;

public class SelectArtyAutoHitHexDisplay 
    extends StatusBarPhaseDisplay
    implements BoardListener,  ActionListener, DoneButtoned,
               KeyListener, GameListener, Distractable
{
    // Distraction implementation.
    private DistractableAdapter distracted = new DistractableAdapter();

    // parent game
    public ClientGUI clientgui;
    private Client client;
    
    public static final String SET_HIT_HEX = "setAutoHitHex";
    
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
        client.addGameListener(this);

        client.game.board.addBoardListener(this);

        setupStatusBar("Waiting to begin Artillery autohit hex setup phase...");

        p = client.getLocalPlayer();
        
        artyAutoHitHexes.insertElementAt(new Integer(p.getId()), 0);
        
        butA = new Button("Artillery Autohit Hexes");
        butA.addActionListener(this);
        butA.setActionCommand(SET_HIT_HEX);
        butA.setEnabled(false);

        butDone = new Button("Done");
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
        client.game.board.select(null);
        client.game.board.highlight(null);
        client.game.board.cursor(null);

    }

    /**
     * Disables all buttons in the interface
     */
    private void disableButtons() {
        setArtyEnabled(0);
        
        butDone.setEnabled(false);
    }

    private void addArtyAutoHitHex(Coords coords) {
        if (!client.game.board.contains(coords)) {
            return;
        }
        if (!artyAutoHitHexes.contains(coords) && artyAutoHitHexes.size() < 6
             && clientgui.doYesNoDialog("Set designated artillery target",
                    "Do you want to set hex " + coords.getBoardNum() + " as a designated artillery target?\nAny artillery fire targeting this hex will automatically hit.")) {
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
    public void boardHexMoused(BoardEvent b) {

        // Are we ignoring events?
        if ( this.isIgnoringEvents() ) {
            return;
        }

        if (b.getType() != BoardEvent.BOARD_HEX_DRAGGED) {
            return;
        }
        
        // ignore buttons other than 1
        if (!client.isMyTurn() || (b.getModifiers() & MouseEvent.BUTTON1_MASK) == 0) {
            return;
        }

        // check for shifty goodness
        boolean shiftheld = (b.getModifiers() & MouseEvent.SHIFT_MASK) != 0;
        
        // check for a deployment
        client.game.board.select(b.getCoords());
        addArtyAutoHitHex(b.getCoords());
    }

    //
    // GameListener
    //
    public void gameTurnChange(GameEvent ev) {

        // Are we ignoring events?
        if ( this.isIgnoringEvents() ) {
            return;
        }

        endMyTurn();

        if (client.isMyTurn()) {
            beginMyTurn();
            setStatusBarText("It's your turn to select designated Artillery targets.");
        } else {
            setStatusBarText("It's " + ev.getPlayer().getName() + 
                    "'s turn to select designated Artillery targets.");
        }
    }

    public void gamePhaseChange(GameEvent ev) {

        // Are we ignoring events?
        if ( this.isIgnoringEvents() ) {
            return;
        }

        if ( client.isMyTurn() &&
             client.game.getPhase() != Game.PHASE_SET_ARTYAUTOHITHEXES ) {
            endMyTurn();
        }
        if (client.game.getPhase() == Game.PHASE_SET_ARTYAUTOHITHEXES) {
            setStatusBarText("Waiting to begin Minefield Deployment phase...");
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
        butA.setLabel("Designated Artillery targets(" + nbr + ")");
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
        client.removeGameListener(this);
        client.game.board.removeBoardListener(this);
    }

}
