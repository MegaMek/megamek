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

public class MovementDisplay 
    extends AbstractPhaseDisplay
    implements BoardListener,  ActionListener,
    KeyListener, ComponentListener, MouseListener, GameListener
{
    private static final int    NUM_BUTTON_LAYOUTS = 2;
    
    // parent game
    public Client client;

    // displays
    private Label             statusL;

    // buttons
    private Panel             panButtons;
    
    private Button            butWalk;
    private Button            butJump;
    private Button            butBackup;
    private Button            butTurn;
    
    private Button            butUp;
    private Button            butDown;
    private Button            butCharge;
    private Button            butDfa;
    private Button            butFlee;
    
    private Button            butSpace;
   
    private Button            butNext;
    private Button            butDone;
    private Button            butMore;

    private int               buttonLayout;

    // let's keep track of what we're moving, too
    private int                cen;    // current entity number
    private MovementData    md;        // movement data
    private MovementData    cmd;    // considering movement data

    // what "gear" is our mech in?
    private int                gear;

    // is the shift key held?
    private boolean            mouseheld;
    private boolean            shiftheld;
    
    // stuff for the current turn
    private int			turnInfMoved = 0;

    /**
     * Creates and lays out a new movement phase display 
     * for the specified client.
     */
    public MovementDisplay(Client client) {
        this.client = client;
        client.addGameListener(this);

        gear = Compute.GEAR_LAND;

        shiftheld = false;

        client.game.board.addBoardListener(this);

        statusL = new Label("Waiting to begin Movement phase...", Label.CENTER);

        butWalk = new Button("Walk");
        butWalk.addActionListener(this);
        butWalk.setEnabled(false);

        butJump = new Button("Jump");
        butJump.addActionListener(this);
        butJump.setEnabled(false);

        butBackup = new Button("Back Up");
        butBackup.addActionListener(this);
        butBackup.setEnabled(false);

        butTurn = new Button("Turn");
        butTurn.addActionListener(this);
        butTurn.setEnabled(false);
        

        butUp = new Button("Get Up");
        butUp.addActionListener(this);
        butUp.setEnabled(false);

        butDown = new Button("Go Prone");
        butDown.addActionListener(this);
        butDown.setEnabled(false);

        butCharge = new Button("Charge");
        butCharge.addActionListener(this);
        butCharge.setEnabled(false);

        butDfa = new Button("D.F.A.");
        butDfa.addActionListener(this);
        butDfa.setEnabled(false);
        
        butFlee = new Button("Flee");
        butFlee.addActionListener(this);
        butFlee.setEnabled(false);
        
        butSpace = new Button(".");
        butSpace.setEnabled(false);

        butMore = new Button("More...");
        butMore.addActionListener(this);
        butMore.setEnabled(false);

        butNext = new Button(" Next Unit ");
        butNext.addActionListener(this);
        butNext.setEnabled(false);

        butDone = new Button("Move");
        butDone.addActionListener(this);
        butDone.setEnabled(false);

        // layout button grid
        panButtons = new Panel();
        buttonLayout = 0;
        setupButtonPanel();

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
        addBag(statusL, gridbag, c);

        c.gridwidth = 1;
        c.weightx = 1.0;    c.weighty = 0.0;
        addBag(client.cb.getComponent(), gridbag, c);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 0.0;    c.weighty = 0.0;
        addBag(panButtons, gridbag, c);

        addKeyListener(this);

        // mech display.
        client.mechD.addMouseListener(this);

        client.frame.addComponentListener(this);
    }
    
    private void addBag(Component comp, GridBagLayout gridbag, GridBagConstraints c) {
        gridbag.setConstraints(comp, c);
        add(comp);
        comp.addKeyListener(this);
    }
    
    private void setupButtonPanel() {
        panButtons.removeAll();
        panButtons.setLayout(new GridLayout(2, 4));
        
        switch (buttonLayout) {
        case 0 :
            panButtons.add(butWalk);
            panButtons.add(butJump);
            panButtons.add(butBackup);
            panButtons.add(butNext);
            panButtons.add(butTurn);
            panButtons.add(butSpace);
            panButtons.add(butMore);
            panButtons.add(butDone);
            break;
        case 1 :
            panButtons.add(butUp);
            panButtons.add(butCharge);
            panButtons.add(butDfa);
            panButtons.add(butNext);
            panButtons.add(butDown);
            panButtons.add(butFlee);
            panButtons.add(butMore);
            panButtons.add(butDone);

	    // Disable DFA and Charge for Infantry.
	    if ( ce() instanceof Infantry ) {
		butDfa.setEnabled(false);
		butCharge.setEnabled(false);
	    } else {
		butDfa.setEnabled(true);
		butCharge.setEnabled(true);
	    }

            break;
        }
        
        validate();
    }
    
    /**
     * Selects an entity, by number, for movement.
     */
    public void selectEntity(int en) {
	boolean isInfantry;
	boolean infMoveLast =
	    client.game.getOptions().booleanOption("inf_move_last");
	boolean infMoveMulti =
	    client.game.getOptions().booleanOption("inf_move_multi");

        // hmm, sometimes this gets called when there's no ready entities?
        if (client.game.getEntity(en) == null) {
            System.err.println("MovementDisplay: tried to select non-existant entity: " + en);
            System.err.println("MovementDisplay: sending ready signal...");
            client.sendReady(true);
            return;
        }
        // okay.
        this.cen = en;
	isInfantry = (ce() instanceof Infantry);

	// If the current entity is Infantry, and infantry move last, then
	// make sure that all other entities for the player have moved.
	if ( isInfantry && infMoveLast && turnInfMoved == 0 ) {

	    // Walk through the list of entities for this player.
	    for ( int nextId = client.getNextEntityNum(en);
		  nextId != en;
		  nextId = client.getNextEntityNum(nextId) ) {

		// If we find a non-Infantry entity, make the
		// player move it instead, and stop looping.
		if ( !(client.game.getEntity(nextId) instanceof Infantry) ) {
		    this.cen = nextId;
		    isInfantry = false;
		    break;
		}

	    } // Check the player's next entity.

	} // End check-inf_move_last

	// If the current entity is not infantry, and we're in a middle of an
	// infantry move block, make sure that the player has no other infantry
	else if ( !isInfantry && infMoveMulti && 
		  (turnInfMoved % Game.INF_MOVE_MULTI) > 0 ) {

	    // Walk through the list of entities for this player.
	    for ( int nextId = client.getNextEntityNum(en);
		  nextId != en;
		  nextId = client.getNextEntityNum(nextId) ) {

		// If we find an Infantry platoon, make the
		// player move it instead, and stop looping.
		if ( client.game.getEntity(nextId) instanceof Infantry ) {
		    this.cen = nextId;
		    isInfantry = true;
		    break;
		}

	    } // Check the player's next entity.

	    // If the current entity isn't infantry, all player's infantry
	    // have been moved; reset the counter so we never check again.
	    if ( !isInfantry ) {
		turnInfMoved = 0;
	    }

	} // End check-inf_move_multi

        md = new MovementData();
        cmd = new MovementData();
        gear = Compute.GEAR_LAND;
        butWalk.setEnabled(ce().getWalkMP() > 0);
        butJump.setEnabled(ce().getJumpMP() > 0);
        butBackup.setEnabled(ce().getWalkMP() > 0);
	// Infantry can't charge or DFA.
	if ( isInfantry ) {
	    butCharge.setEnabled(false);
	    butDfa.setEnabled(false);
	} else {
        butCharge.setEnabled(ce().getWalkMP() > 0);
        butDfa.setEnabled(ce().getJumpMP() > 0);
	}
        butTurn.setEnabled(ce().getWalkMP() > 0 || ce().getJumpMP() > 0);
        
        if (ce().isProne()) {
            butUp.setEnabled(true);
        } else {
            butDown.setEnabled(false);
        }
        butFlee.setEnabled(Compute.canEntityFlee(client.game, cen));
        client.game.board.highlight(ce().getPosition());
        client.game.board.select(null);
        client.game.board.cursor(null);
        client.mechD.displayEntity(ce());
        client.mechD.showPanel("movement");
        client.bv.centerOnHex(ce().getPosition());
    }

    /**
     * Enables relevant buttons and sets up for your turn.
     */
    private void beginMyTurn() {
        butDone.setLabel("Done");
        butDone.setEnabled(true);
        butNext.setEnabled(true);
        butMore.setEnabled(true);
        moveMechDisplay();
        client.mechW.setVisible(true);
        moveMechDisplay();
        selectEntity(client.getFirstEntityNum());
    }

    /**
     * Clears out old movement data and disables relevant buttons.
     */
    private void endMyTurn() {
        // end my turn, then.
        disableButtons();
        cen = Entity.NONE;
        client.game.board.select(null);
        client.game.board.highlight(null);
        client.game.board.cursor(null);
        client.mechW.setVisible(false);
        client.bv.clearMovementData();
    }

    /**
     * Disables all buttons in the interface
     */
    private void disableButtons() {
        butWalk.setEnabled(false);
        butJump.setEnabled(false);
        butBackup.setEnabled(false);
        butTurn.setEnabled(false);
        butFlee.setEnabled(false);
        butUp.setEnabled(false);
        butDown.setEnabled(false);
        butCharge.setEnabled(false);
        butDfa.setEnabled(false);
        butNext.setEnabled(false);
        butMore.setEnabled(false);
        butDone.setEnabled(false);
    }
    /**
     * Clears out the curently selected movement data and
     * resets it.
     */
    private void clearAllMoves() {
        client.game.board.select(null);
        client.game.board.cursor(null);
        md = new MovementData();
        cmd = new MovementData();
        client.bv.clearMovementData();
        butDone.setLabel("Done");;
    }

    /**
     * Sends a data packet indicating the chosen movement.
     */
    private void moveTo(MovementData md) {
        disableButtons();
        client.bv.clearMovementData();
        client.moveEntity(cen, md);
	// If we've moved an Infantry platoon, increment our turn counter.
	if ( ce() instanceof Infantry ) {
	    turnInfMoved++;
	}
        client.sendReady(true);
    }

    /**
     * Returns the current entity.
     */
    private Entity ce() {
        return client.game.getEntity(cen);
    }

    /**
     * Returns new MovementData for the currently selected movement type
     */
    private MovementData currentMove(Coords src, int facing, Coords dest) {
        if (shiftheld || gear == Compute.GEAR_TURN) {
            return Compute.rotatePathfinder(facing, src.direction(dest));
        } else if (gear == Compute.GEAR_LAND || gear == Compute.GEAR_JUMP) {
            return Compute.lazyPathfinder(src, facing, dest);
        } else if (gear == Compute.GEAR_BACKUP) {
            return Compute.backwardsLazyPathfinder(src, facing, dest);
        } else if (gear == Compute.GEAR_CHARGE) {
            return Compute.chargeLazyPathfinder(src, facing, dest);
        } else if (gear == Compute.GEAR_DFA) {
            return Compute.dfaLazyPathfinder(src, facing, dest);
        }       

        return null;
    }

    /**
     * Moves the mech display window to the proper position.
     */
    private void moveMechDisplay() {
        if (!client.bv.isShowing()) {
            return;
        }
        client.mechW.setLocation(client.bv.getLocationOnScreen().x 
                                 + client.bv.getSize().width 
                                 - client.mechD.getSize().width - 20,
                                 client.bv.getLocationOnScreen().y + 20);
    }

    //
    // BoardListener
    //
    public void boardHexMoused(BoardEvent b) {
        // ignore buttons other than 1
        if (!client.isMyTurn() || (b.getModifiers() & MouseEvent.BUTTON1_MASK) == 0) {
            return;
        }
        // check for shifty goodness
        if (shiftheld != ((b.getModifiers() & MouseEvent.SHIFT_MASK) != 0)) {
            shiftheld = (b.getModifiers() & MouseEvent.SHIFT_MASK) != 0;
        }
        
        if (b.getType() == b.BOARD_HEX_DRAGGED) {
            if (!b.getCoords().equals(client.game.board.lastCursor) || shiftheld || gear == Compute.GEAR_TURN) {
                client.game.board.cursor(b.getCoords());

                // either turn or move
                cmd = md.getAppended(currentMove(md.getFinalCoords(ce().getPosition(), ce().getFacing()), md.getFinalFacing(ce().getFacing()), b.getCoords()));
                client.bv.drawMovementData(ce(), cmd);
            }
        } else if (b.getType() == b.BOARD_HEX_CLICKED) {
            final Entity target = client.game.getFirstEntity(b.getCoords());

            Coords moveto = b.getCoords();
            client.bv.drawMovementData(ce(), cmd);
            md = new MovementData(cmd);
            
            client.game.board.select(b.getCoords());

            if (shiftheld || gear == Compute.GEAR_TURN) {
                butDone.setLabel("Move");
                return;
            }

            if (gear == Compute.GEAR_CHARGE) {
                // check if target is valid
                if (target == null || target.equals(ce())) {
                    client.doAlertDialog("Can't perform charge", "No target!");
                    clearAllMoves();
                    gear = Compute.GEAR_LAND;
                    return;
                }
                
                // check if it's a valid charge
                ToHitData toHit = Compute.toHitCharge(client.game, cen, target.getId(), md);
                if (toHit.getValue() != ToHitData.IMPOSSIBLE) {
                    // if yes, ask them if they want to charge

				if ( client.doYesNoDialog( "Charge " + target.getDisplayName() + "?",
					"To Hit: " + toHit.getValueAsString() + " (" + Compute.oddsAbove(toHit.getValue()) + "%)   (" + toHit.getDesc() + ")"
					+ "\nDamage to Target: "+Compute.getChargeDamageFor(ce(),md.getHexesMoved())+" (in 5pt clusters)"+toHit.getTableDesc()
					+ "\nDamage to Self: " + Compute.getChargeDamageTakenBy(ce(),target)+" (in 5pt clusters)"
				) ) {
	                        // if they answer yes, charge
      	                  moveTo(md);
				} else {
					// else clear movement
					clearAllMoves();
				};
				return;
                } else {
                    // if not valid, tell why
                    client.doAlertDialog("Can't perform charge", toHit.getDesc());
                    clearAllMoves();
                    gear = Compute.GEAR_LAND;
                    return;
                }
            } else if (gear == Compute.GEAR_DFA) {
                // check if target is valid
                if (target == null || target.equals(ce())) {
                    client.doAlertDialog("Can't perform D.F.A.", "No target!");
                    clearAllMoves();
                    gear = Compute.GEAR_LAND;
                    return;
                }
                
                // check if it's a valid DFA
                ToHitData toHit = Compute.toHitDfa(client.game, cen, target.getId(), md);
                if (toHit.getValue() != ToHitData.IMPOSSIBLE) {
                    // if yes, ask them if they want to DFA
				if ( client.doYesNoDialog( "D.F.A. " + target.getDisplayName() + "?",
					"To Hit: " + toHit.getValueAsString() + " (" + Compute.oddsAbove(toHit.getValue()) + "%)   (" + toHit.getDesc() + ")"
					+ "\nDamage to Target: "+Compute.getDfaDamageFor(ce())+" (in 5pt clusters)"+toHit.getTableDesc()
					+ "\nDamage to Self: " + Compute.getDfaDamageTakenBy(ce())+" (in 5pt clusters) (using Kick table)"
				) ) {
	                      	// if they answer yes, go for it
                        	moveTo(md);
				} else {
	                        // else clear movement
					clearAllMoves();
				};
                        return;
                
                } else {
                    // if not valid, tell why
                    client.doAlertDialog("Can't perform D.F.A.", toHit.getDesc());
                    clearAllMoves();
                    gear = Compute.GEAR_LAND;
                    return;
                }
            }
            
            butDone.setLabel("Move");
        }
    }

    //
    // GameListener
    //
    public void gameTurnChange(GameEvent ev) {
        if (client.game.phase != Game.PHASE_MOVEMENT) {
            // ignore
            return;
        }
        // else, change turn
        endMyTurn();

        if (client.isMyTurn()) {
            beginMyTurn();
            statusL.setText("It's your turn to move.");
        } else {
            statusL.setText("It's " + ev.getPlayer().getName() + "'s turn to move.");
        }
    }
    public void gamePhaseChange(GameEvent ev) {
        if (client.isMyTurn() && client.game.phase != Game.PHASE_MOVEMENT) {
            endMyTurn();
        }
        if (client.game.phase !=  Game.PHASE_MOVEMENT) {
            client.removeGameListener(this);
            client.game.board.removeBoardListener(this);
            client.bv.removeKeyListener(this);
            client.cb.getComponent().removeKeyListener(this);
            client.mechD.removeMouseListener(this);
            client.frame.removeComponentListener(this);
	    // Reset the infantry move counter;
	    turnInfMoved = 0;
        }
    }

    //
    // ActionListener
    //
    public void actionPerformed(ActionEvent ev) {
        if (!client.isMyTurn()) {
            // odd...
            return;
        }
        
        if (ev.getSource() == butDone) {
            moveTo(md);
        } else if (ev.getSource() == butNext) {
            clearAllMoves();
            selectEntity(client.getNextEntityNum(cen));
        } else if (ev.getSource() == butMore) {
            buttonLayout++;
            buttonLayout %= NUM_BUTTON_LAYOUTS;
            setupButtonPanel();
        } else if (ev.getSource() == butWalk) {
            if (gear == Compute.GEAR_JUMP) {
                clearAllMoves();
            }
            gear = Compute.GEAR_LAND;
        } else if (ev.getSource() == butJump) {
            if (gear != Compute.GEAR_JUMP) {
                clearAllMoves();
            }
            if (!md.contains(MovementData.STEP_START_JUMP)) {
                md.addStep(MovementData.STEP_START_JUMP);
            }
            gear = Compute.GEAR_JUMP;
        } else if (ev.getSource() == butTurn) {
            gear = Compute.GEAR_TURN;
        } else if (ev.getSource() == butBackup) {
            if (gear == Compute.GEAR_JUMP) {
                clearAllMoves();
            }
            gear = Compute.GEAR_BACKUP;
        } else if (ev.getSource() == butCharge) {
            if (gear != Compute.GEAR_LAND) {
                clearAllMoves();
            }
            gear = Compute.GEAR_CHARGE;
        } else if (ev.getSource() == butDfa) {
            if (gear != Compute.GEAR_JUMP) {
                clearAllMoves();
            }
            gear = Compute.GEAR_DFA;
            if (!md.contains(MovementData.STEP_START_JUMP)) {
                md.addStep(MovementData.STEP_START_JUMP);
            }
        } else if (ev.getSource() == butUp) {
            clearAllMoves();
            gear = Compute.GEAR_LAND;
            if (!md.contains(MovementData.STEP_GET_UP)) {
                md.addStep(MovementData.STEP_GET_UP);
            }
            client.bv.drawMovementData(ce(), cmd);
            butDone.setLabel("Move");
        } else if (ev.getSource() == butFlee && client.doYesNoDialog("Escape?", "Do you want to flee?")) {
            clearAllMoves();
            md.addStep(MovementData.STEP_FLEE);
            moveTo(md);
        }

    }
    

    //
    // KeyListener
    //
    public void keyPressed(KeyEvent ev) {
        if (ev.getKeyCode() == ev.VK_ESCAPE) {
            clearAllMoves();
        }
        if (ev.getKeyCode() == ev.VK_ENTER && ev.isControlDown()) {
            if (client.isMyTurn()) {
                moveTo(cmd);
            }
        }
        if (ev.getKeyCode() == ev.VK_SHIFT && !shiftheld) {
            shiftheld = true;
            if (client.isMyTurn() && client.game.board.lastCursor != null && !client.game.board.lastCursor.equals(client.game.board.selected)) {
                // switch to turning
                //client.bv.clearMovementData();
                cmd = md.getAppended(currentMove(md.getFinalCoords(ce().getPosition(), ce().getFacing()), md.getFinalFacing(ce().getFacing()), client.game.board.lastCursor));
                client.bv.drawMovementData(ce(), cmd);
            }
        }
    }
    public void keyReleased(KeyEvent ev) {
        if (ev.getKeyCode() == ev.VK_SHIFT && shiftheld) {
            shiftheld = false;
            if (client.isMyTurn() && client.game.board.lastCursor != null && !client.game.board.lastCursor.equals(client.game.board.selected)) {
                // switch to movement
                client.bv.clearMovementData();
                cmd = md.getAppended(currentMove(md.getFinalCoords(ce().getPosition(), ce().getFacing()), md.getFinalFacing(ce().getFacing()), client.game.board.lastCursor));
                client.bv.drawMovementData(ce(), cmd);
            }
        }
    }
    public void keyTyped(KeyEvent ev) {
        ;
    }
    
    //
    // ComponentListener
    //
    public void componentHidden(ComponentEvent ev) {
        client.mechW.setVisible(false);
    }
    public void componentMoved(ComponentEvent ev) {
        moveMechDisplay();
    }
    public void componentResized(ComponentEvent ev) {
        moveMechDisplay();
    }
    public void componentShown(ComponentEvent ev) {
        client.mechW.setVisible(false);
        moveMechDisplay();
    }
    
    //
    // MouseListener
    //
    public void mouseEntered(MouseEvent ev) {
        ;
    }
    public void mouseExited(MouseEvent ev) {
        ;
    }
    public void mousePressed(MouseEvent ev) {
        ;
    }
    public void mouseReleased(MouseEvent ev) {
        ;
    }
    public void mouseClicked(MouseEvent ev) {
        ;
    }

}
