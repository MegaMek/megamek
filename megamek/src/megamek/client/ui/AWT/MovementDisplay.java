/*
 * MegaMek -
 * Copyright (C) 2000,2001,2002,2003,2004,2005 Ben Mazur (bmazur@sev.org)
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
import megamek.common.actions.ChargeAttackAction;
import megamek.common.actions.DfaAttackAction;
import megamek.common.util.Distractable;
import megamek.common.util.DistractableAdapter;

public class MovementDisplay
    extends StatusBarPhaseDisplay
    implements BoardListener,  ActionListener, DoneButtoned,
               KeyListener, GameListener, BoardViewListener, Distractable
{
    // Distraction implementation.
    private DistractableAdapter distracted = new DistractableAdapter();

    private static final int    NUM_BUTTON_LAYOUTS = 3;

    public static final String    MOVE_WALK = "moveWalk";
    public static final String    MOVE_NEXT = "moveNext";
    public static final String    MOVE_JUMP = "moveJump";
    public static final String    MOVE_BACK_UP = "moveBackUp";
    public static final String    MOVE_TURN = "moveTurn";
    public static final String    MOVE_GET_UP = "moveGetUp";
    public static final String    MOVE_CHARGE = "moveCharge";
    public static final String    MOVE_DFA = "moveDFA";
    public static final String    MOVE_GO_PRONE = "moveGoProne";
    public static final String    MOVE_FLEE = "moveFlee";
    public static final String    MOVE_EJECT = "moveEject";
    public static final String    MOVE_LOAD = "moveLoad";
    public static final String    MOVE_UNLOAD = "moveUnload";
    public static final String    MOVE_UNJAM = "moveUnjam";
    public static final String    MOVE_CLEAR = "moveClear";
    public static final String    MOVE_CANCEL   = "moveCancel";

    // parent game
    public Client client;
    private ClientGUI clientgui;

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

    private Button            butRAC;
    private Button            butFlee;
    private Button            butEject;

    private Button            butLoad;
    private Button            butUnload;
    private Button            butSpace;

    private Button            butClear;

    private Button            butNext;
    private Button            butDone;
    private Button            butMore;

    private int               buttonLayout;

    // let's keep track of what we're moving, too
    private int                cen = Entity.NONE;    // current entity number
    private MovePath           cmd;    // considering movement data

    // what "gear" is our mech in?
    private int                gear;

    // is the shift key held?
    private boolean            shiftheld;

    /**
     * A local copy of the current entity's loaded units.
     */
    private Vector              loadedUnits = null;

    public static final int        GEAR_LAND        = 0;
    public static final int        GEAR_BACKUP      = 1;
    public static final int        GEAR_JUMP        = 2;
    public static final int        GEAR_CHARGE      = 3;
    public static final int        GEAR_DFA         = 4;
    public static final int        GEAR_TURN        = 5;

    /**
     * Creates and lays out a new movement phase display
     * for the specified client.
     */
    public MovementDisplay(ClientGUI clientgui) {
        this.clientgui = clientgui;
        this.client = clientgui.getClient();
        client.addGameListener(this);

        gear = MovementDisplay.GEAR_LAND;

        shiftheld = false;

        client.game.board.addBoardListener(this);

        setupStatusBar("Waiting to begin Movement phase...");

        butClear = new Button("Clear mines");
        butClear.addActionListener(this);
        butClear.setEnabled(false);
        butClear.setActionCommand(MOVE_CLEAR);

        butWalk = new Button("Walk");
        butWalk.addActionListener(this);
        butWalk.setEnabled(false);
        butWalk.setActionCommand(MOVE_WALK);

        butJump = new Button("Jump");
        butJump.addActionListener(this);
        butJump.setEnabled(false);
        butJump.setActionCommand(MOVE_JUMP);

        butBackup = new Button("Back Up");
        butBackup.addActionListener(this);
        butBackup.setEnabled(false);
        butBackup.setActionCommand(MOVE_BACK_UP);

        butTurn = new Button("Turn");
        butTurn.addActionListener(this);
        butTurn.setEnabled(false);
        butTurn.setActionCommand(MOVE_TURN);


        butUp = new Button("Get Up");
        butUp.addActionListener(this);
        butUp.setEnabled(false);
        butUp.setActionCommand(MOVE_GET_UP);

        butDown = new Button("Go Prone");
        butDown.addActionListener(this);
        butDown.setEnabled(false);
        butDown.setActionCommand(MOVE_GO_PRONE);

        butCharge = new Button("Charge");
        butCharge.addActionListener(this);
        butCharge.setEnabled(false);
        butCharge.setActionCommand(MOVE_CHARGE);

        butDfa = new Button("D.F.A.");
        butDfa.addActionListener(this);
        butDfa.setEnabled(false);
        butDfa.setActionCommand(MOVE_DFA);

        butFlee = new Button("Flee");
        butFlee.addActionListener(this);
        butFlee.setEnabled(false);
        butFlee.setActionCommand(MOVE_FLEE);

        butEject = new Button("Eject");
        butEject.addActionListener(this);
        butEject.setEnabled(false);
        butEject.setActionCommand(MOVE_EJECT);

        butRAC = new Button("Unjam RAC");
        butRAC.addActionListener(this);
        butRAC.setEnabled(false);
        butRAC.setActionCommand(MOVE_UNJAM);

        butMore = new Button("More...");
        butMore.addActionListener(this);
        butMore.setEnabled(false);

        butNext = new Button("Next Unit");
        butNext.addActionListener(this);
        butNext.setEnabled(false);
        butNext.setActionCommand(MOVE_NEXT);

        butDone = new Button("Move");
        butDone.addActionListener(this);
        butDone.setEnabled(false);

        butLoad = new Button("Load");
        butLoad.addActionListener(this);
        butLoad.setEnabled(false);
        butLoad.setActionCommand(MOVE_LOAD);

        butUnload = new Button("Unload");
        butUnload.addActionListener(this);
        butUnload.setEnabled(false);
        butUnload.setActionCommand(MOVE_UNLOAD);
        
        butSpace = new Button(".");
        butSpace.setEnabled(false);

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
        addKeyListener( this );

    }

    private void addBag(Component comp, GridBagLayout gridbag, GridBagConstraints c) {
        gridbag.setConstraints(comp, c);
        add(comp);
        comp.addKeyListener(this);
    }
    
    private void setupButtonPanel() {
        panButtons.removeAll();
        panButtons.setLayout(new GridLayout(0, 8));

        switch (buttonLayout) {
        case 0 :
            panButtons.add(butNext);
            panButtons.add(butWalk);
            panButtons.add(butJump);
            panButtons.add(butBackup);
            panButtons.add(butTurn);
            panButtons.add(butUp);
            panButtons.add(butMore);
//             panButtons.add(butDone);
            break;
        case 1 :
            panButtons.add(butNext);
            panButtons.add(butCharge);
            panButtons.add(butDfa);
            panButtons.add(butDown);
            panButtons.add(butFlee);
            panButtons.add(butEject);
            panButtons.add(butMore);
//             panButtons.add(butDone);
            break;
        case 2:
            panButtons.add(butNext);
            panButtons.add(butLoad);
            panButtons.add(butUnload);
            panButtons.add(butRAC);
            panButtons.add(butClear);
            panButtons.add(butSpace);
            panButtons.add(butMore);
//             panButtons.add(butDone);
            break;
        }

        validate();
    }

    /**
     * Selects an entity, by number, for movement.
     */
    public synchronized void selectEntity(int en) {
        final Entity ce = client.game.getEntity(en);
        
        
        // hmm, sometimes this gets called when there's no ready entities?
        if (ce == null) {
            System.err.println("MovementDisplay: tried to select non-existant entity: " + en);
            return;
        }
        // okay...
        if (ce != null) {
        	ce.setSelected(false);
        }
        this.cen = en;
        ce.setSelected(true);
        clearAllMoves();
	    updateButtons();
        // Update the menu bar.
        clientgui.getMenuBar().setEntity( ce );
        
        client.game.board.highlight(ce.getPosition());
        client.game.board.select(null);
        client.game.board.cursor(null);
        clientgui.mechD.displayEntity(ce);
        clientgui.mechD.showPanel("movement");
        if (!clientgui.bv.isMovingUnits()) {
	        clientgui.bv.centerOnHex(ce.getPosition());
	    }
    }
    
    /**
     * Sets the buttons to their proper states
     */
    private void updateButtons() {
        final Entity ce = ce();
        boolean isMech      = (ce instanceof Mech);
        boolean isInfantry  = (ce instanceof Infantry);
        boolean isProtomech = (ce instanceof Protomech);
        // ^-- I suppose these should really be methods, a-la Entity.canCharge(), Entity.canDFA()...
        
        setWalkEnabled(!ce.isImmobile() && ce.getWalkMP() > 0 && !ce.isStuck());
        setJumpEnabled(!ce.isImmobile() && ce.getJumpMP() > 0 && !ce.isStuck());
        setBackUpEnabled(butWalk.isEnabled());

        setChargeEnabled(ce.canCharge());
        setDFAEnabled(ce.canDFA());

        if ( isInfantry || isProtomech ) {
            if(client.game.containsMinefield(ce.getPosition())) {
                setClearEnabled(true);
            } else {
                setClearEnabled(false);
            }
        } else {
            setClearEnabled(false);
        }
        
        setTurnEnabled(!ce.isImmobile() && 
                !ce.isStuck() &&
                (ce.getWalkMP() > 0 || ce.getJumpMP() > 0));

        if (ce.isProne()) {
            setGetUpEnabled(!ce.isImmobile() && !ce.isStuck());
            setGoProneEnabled(false);
        } else {
            setGetUpEnabled(false);
            setGoProneEnabled(!ce.isImmobile() && isMech && !ce.isStuck());
        }

        updateProneButtons();
        updateRACButton();
        updateLoadButtons();

        setFleeEnabled(ce.canFlee());
        if (client.game.getOptions().booleanOption("vehicles_can_eject")) {
          setEjectEnabled ( (!isInfantry) && ce.isActive());
        } else {
          setEjectEnabled(isMech && ce.isActive());
        }
        
    }

    /**
     * Enables relevant buttons and sets up for your turn.
     */
    private void beginMyTurn() {
        setStatusBarText("It's your turn to move.");
        selectEntity(client.getFirstEntityNum());
        butDone.setLabel("Done");
        butDone.setEnabled(true);
        setNextEnabled(true);
        butMore.setEnabled(true);
        if (!clientgui.bv.isMovingUnits()) {
            clientgui.setDisplayVisible(true);
        }
    }

    /**
     * Clears out old movement data and disables relevant buttons.
     */
    private synchronized void endMyTurn() {
        final Entity ce = ce();
        
        
        // end my turn, then.
        disableButtons();
        Entity next = client.game.getNextEntity( client.game.getTurnIndex() );
        if ( Game.PHASE_MOVEMENT == client.game.getPhase()
             && null != next
             && null != ce
             && next.getOwnerId() != ce.getOwnerId() ) {
            clientgui.setDisplayVisible(false);
        }
        cen = Entity.NONE;
        client.game.board.select(null);
        client.game.board.highlight(null);
        client.game.board.cursor(null);
        clientgui.bv.clearMovementData();
    }

    /**
     * Disables all buttons in the interface
     */
    private void disableButtons() {
        setWalkEnabled(false);
        setJumpEnabled(false);
        setBackUpEnabled(false);
        setTurnEnabled(false);
        setFleeEnabled(false);
        setEjectEnabled(false);
        setUnjamEnabled(false);
        setGetUpEnabled(false);
        setGoProneEnabled(false);
        setChargeEnabled(false);
        setDFAEnabled(false);
        setNextEnabled(false);
        butMore.setEnabled(false);
        butDone.setEnabled(false);
        setLoadEnabled(false);
        setUnloadEnabled(false);
        setClearEnabled(false);
    }
    /**
     * Clears out the curently selected movement data and
     * resets it.
     */
    private void clearAllMoves() {
        final Entity ce = ce();
        
        
        // clear board cursors
        client.game.board.select(null);
        client.game.board.cursor(null);
        
        // create new current and considered paths
        cmd = new MovePath(client.game, ce);
        
        // set to "walk," or the equivalent
        gear = MovementDisplay.GEAR_LAND;
        
        // update some GUI elements
        clientgui.bv.clearMovementData();
        butDone.setLabel("Done");
        updateProneButtons();
        updateRACButton();

        // We may not have an entity selected yet (race condition).
        if ( ce != null ) {
            loadedUnits = ce.getLoadedUnits();
        } else {
            // The variable, loadedUnits, can not be null.
            loadedUnits = new Vector();
        }
        updateLoadButtons();
    }

    private void removeLastStep() {
        cmd.removeLastStep();
        
        if (cmd.length() == 0) {
	        clearAllMoves();
        } else {
	        clientgui.bv.drawMovementData(ce(), cmd);
        }
    }

    /**
     * Sends a data packet indicating the chosen movement.
     */
    private synchronized void moveTo(MovePath md) {
        if (md.length() == 0 && Settings.nagForNoAction) {
            //Hmm....no movement steps, comfirm this action
            String title = "Remain stationary?";
            String body = "This unit has not moved.\n\n" +
                "Are you really done?\n";
            ConfirmDialog response = clientgui.doYesNoBotherDialog(title, body);
            if ( !response.getShowAgain() ) {
                Settings.nagForNoAction = false;
                Settings.save();
            }
            if ( !response.getAnswer() ) {
                return;
            }
        }

        if ( md != null ) {
            if (md.hasActiveMASC() && Settings.nagForMASC) { //pop up are you sure dialog
                Mech m = (Mech)ce();
                ConfirmDialog nag = new ConfirmDialog(clientgui.frame,"Are you sure?", "The movement you have selected will require a roll of " + m.getMASCTarget() + " or higher\nto avoid MASC failure.  Do you wish to proceed?", true);
                nag.setVisible(true);
                if (nag.getAnswer()) {
                    // do they want to be bothered again?
                    if (!nag.getShowAgain()) {
                        Settings.nagForMASC = false;
                    }
                } else {
                    return;
                }
            }

            String check = doPSRCheck(md);
            if (check.length() > 0 && Settings.nagForPSR) {
                ConfirmDialog nag = 
                    new ConfirmDialog(clientgui.frame,
                                      "Are you sure?", 
                                      "You must make the following piloting\n" +
                                      "skill check(s) for your movement:\n" +
                                      check, true);
                nag.setVisible(true);
                if (nag.getAnswer()) {
                    // do they want to be bothered again?
                    if (!nag.getShowAgain()) {
                        Settings.nagForPSR = false;
                    }
                } else {
                    return;
                }
            }
        }

        disableButtons();
        clientgui.bv.clearMovementData();
        client.moveEntity(cen, md);
    }

    private String addNag(PilotingRollData rollTarget) {
        String desc = "Need " + rollTarget.getValueAsString() + " [" + rollTarget.getDesc() + "]\n";
        return desc;
    }

    /**
     * Checks to see if piloting skill rolls are needed for the
     *  currently selected movement.  This code is basically a
     *  simplified version of Server.processMovement(), except
     *  that it just reads information (no writing).  Note that
     *  MovePath.clipToPossible() is called though, which changes the
     *  md object.
     */
    private String doPSRCheck(MovePath md) {

        StringBuffer nagReport = new StringBuffer();

        final Entity entity = ce();

        // okay, proceed with movement calculations
        Coords lastPos = entity.getPosition();
        Coords curPos = entity.getPosition();
        int curFacing = entity.getFacing();
        int distance = 0;
        int moveType = Entity.MOVE_NONE;
        int overallMoveType = Entity.MOVE_NONE;
        boolean firstStep;
        int prevFacing = curFacing;
        Hex prevHex = null;
        final boolean isInfantry = (entity instanceof Infantry);

        PilotingRollData rollTarget;
        
        // Compile the move
        md.clipToPossible();

        overallMoveType = md.getLastStepMovementType();
        
        // iterate through steps
        firstStep = true;
        /* Bug 754610: Revert fix for bug 702735. */
        MoveStep prevStep = null;
        for (final Enumeration i = md.getSteps(); i.hasMoreElements();) {
            final MoveStep step = (MoveStep)i.nextElement();
            boolean isPavementStep = step.isPavementStep();
            
            // stop for illegal movement
            if (step.getMovementType() == Entity.MOVE_ILLEGAL) {
                break;
            }
            
            // check piloting skill for getting up
            rollTarget = entity.checkGetUp(step);
            if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                nagReport.append(addNag(rollTarget));
            }

            // set most step parameters
            moveType = step.getMovementType();
            distance = step.getDistance();
 
            // set last step parameters
            curPos = step.getPosition();
            curFacing = step.getFacing();

            final Hex curHex = client.game.board.getHex(curPos);

            // Check for skid.
            rollTarget = entity.checkSkid(moveType, prevHex, overallMoveType,
                                          prevStep, prevFacing, curFacing,
                                          lastPos, curPos, isInfantry,
                                          distance);
            if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                // Have an entity-meaningful PSR message.
                nagReport.append(addNag(rollTarget));
            }

            // check if we've moved into rubble
            rollTarget = entity.checkRubbleMove(step, curHex, lastPos, curPos);
            if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                nagReport.append(addNag(rollTarget));
            }
            
            // check if we've moved into water
            rollTarget = entity.checkWaterMove(step, curHex, lastPos, curPos,
                                               isPavementStep);
            if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                nagReport.append(addNag(rollTarget));
            }
            
            // check if we've moved into swamp
            rollTarget = entity.checkSwampMove(step, curHex, lastPos, curPos);
            if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                nagReport.append(addNag(rollTarget));
            }
            
            // check if we used more MPs than the Mech/Vehicle would have in normal gravity
            if (!i.hasMoreElements() && !firstStep) {
                if (entity instanceof Mech) {
                    if ((step.getMovementType() == Entity.MOVE_WALK) || (step.getMovementType() == Entity.MOVE_RUN)) {
                        if (step.getMpUsed() > entity.getRunMP(false)) {
                            rollTarget = entity.checkMovedTooFast(step);
                            if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                                nagReport.append(addNag(rollTarget));
                            }
                        }
                    } else if (step.getMovementType() == Entity.MOVE_JUMP) {
                        if (step.getMpUsed() > entity.getOriginalJumpMP()) {
                            rollTarget = entity.checkMovedTooFast(step);
                            if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                                nagReport.append(addNag(rollTarget));
                            }
                        }    
                      }
                } else if (entity instanceof Tank) {
                    if ((step.getMovementType() == Entity.MOVE_WALK) || (step.getMovementType() == Entity.MOVE_RUN)) {
                        // For Tanks, we need to check if the tank had more MPs because it was moving along a road
                        if (step.getMpUsed() > entity.getRunMP(false) && !step.isOnlyPavement()) {
                            rollTarget = entity.checkMovedTooFast(step);
                            if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                                nagReport.append(addNag(rollTarget));
                            }    
                        } else if (step.getMovementType() == Entity.MOVE_WALK) {
                            // If the tank was just cruising, he got a flat +1 road bonus
                            if (step.getMpUsed() > entity.getWalkMP(false) + 1) {
                                rollTarget = entity.checkMovedTooFast(step);
                                if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                                    nagReport.append(addNag(rollTarget));
                                }
                            }
                        } else if (step.getMovementType() == Entity.MOVE_RUN) {
                            // If the tank was flanking, we need a calculation to see wether we get a +1 or +2 road bonus
                            // NOTE: this continues the assumption from MoveStep.java that the +1 bonus is applied to 
                            // cruising speed, thus possibly gaining 2 flanking MPs
                            int k = entity.getWalkMP(false) % 2 == 1 ? 1 : 2;
                            if (step.getMpUsed() > entity.getRunMP(false) + k) {
                                rollTarget = entity.checkMovedTooFast(step);
                                if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                                    nagReport.append(addNag(rollTarget));
                                }
                            }
                        }
                    }   
                }
            }

            // Handle non-infantry moving into a building.
            if (entity.checkMovementInBuilding(lastPos, curPos, step,
                                               curHex, prevHex)) {
                
                // Get the building being exited.
                // TODO: allow units to climb on top of buildings.
                Building bldgExited = client.game.board.getBuildingAt( lastPos );

                // Get the building being entered.
                // TODO: allow units to climb on top of buildings.
                Building bldgEntered = client.game.board.getBuildingAt( curPos );

                if ( bldgExited != null && bldgEntered != null && 
                     !bldgExited.equals(bldgEntered) ) {
                    // Exiting one building and entering another.
                    //  Brave, aren't we?
                    rollTarget = entity.rollMovementInBuilding(bldgExited, distance, "exiting");
                    nagReport.append(addNag(rollTarget));
                    rollTarget = entity.rollMovementInBuilding(bldgEntered, distance, "entering");
                    nagReport.append(addNag(rollTarget));
                } else {
                    Building bldg;
                    if (bldgEntered == null) {
                        // Exiting a building.
                        bldg = bldgExited;
                    } else {
                        // Entering or moving within a building.
                        bldg = bldgEntered;
                    }
                    rollTarget = entity.rollMovementInBuilding(bldg, distance, "");
                    nagReport.append(addNag(rollTarget));
                }
            }

            if (step.getType() == MovePath.STEP_GO_PRONE) {
                rollTarget = entity.checkDislodgeSwarmers();
                if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                    nagReport.append(addNag(rollTarget));
                }
            }

            // update lastPos, prevStep, prevFacing & prevHex
            lastPos = new Coords(curPos);
            prevStep = step;
            /* Bug 754610: Revert fix for bug 702735.
            if (prevHex != null && !curHex.equals(prevHex)) {
            */
            if (!curHex.equals(prevHex)) {
                prevFacing = curFacing;
            }
            prevHex = curHex;
            
            firstStep = false;
        }
        
        // running with destroyed hip or gyro needs a check
        rollTarget = entity.checkRunningWithDamage(overallMoveType);
        if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
            nagReport.append(addNag(rollTarget));
        }

        // but the danger isn't over yet!  landing from a jump can be risky!
        if (overallMoveType == Entity.MOVE_JUMP && !entity.isMakingDfa()) {
            // check for damaged criticals
            rollTarget = entity.checkLandingWithDamage();
            if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                nagReport.append(addNag(rollTarget));
            }
            // jumped into water?
            int waterLevel = client.game.board.getHex(curPos).levelOf(Terrain.WATER);
            rollTarget = entity.checkWaterMove(waterLevel);
            if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                nagReport.append(addNag(rollTarget));
            }
        }
        
        return nagReport.toString();
    }

    /**
     * Returns the current entity.
     */
    private synchronized Entity ce() {
        return client.game.getEntity(cen);
    }

    /**
     * Returns new MovePath for the currently selected movement type
     */
    private void currentMove(Coords dest) {
        if (shiftheld || gear == GEAR_TURN) {
            cmd.rotatePathfinder(cmd.getFinalCoords().direction(dest));
        } else if (gear == GEAR_LAND || gear == GEAR_JUMP) {
            cmd.findPathTo(dest, MovePath.STEP_FORWARDS);
        } else if (gear == GEAR_BACKUP) {
            cmd.findPathTo(dest, MovePath.STEP_BACKWARDS);
        } else if (gear == GEAR_CHARGE) {
            cmd.findPathTo(dest, MovePath.STEP_CHARGE);
        } else if (gear == GEAR_DFA) {
            cmd.findPathTo(dest, MovePath.STEP_DFA);
        }
    }

    //
    // BoardListener
    //
    public synchronized void boardHexMoused(BoardEvent b) {
        final Entity ce = ce();
        
        
        // Are we ignoring events?
        if ( this.isIgnoringEvents() ) {
            return;
        }

        // ignore buttons other than 1
        if (!client.isMyTurn() || (b.getModifiers() & MouseEvent.BUTTON1_MASK) == 0) {
            return;
        }
        // control pressed means a line of sight check.
        // added ALT_MASK by kenn
        if ((b.getModifiers() & InputEvent.CTRL_MASK) != 0 || (b.getModifiers() & InputEvent.ALT_MASK) != 0) {
            return;
        }
        // check for shifty goodness
        if (shiftheld != ((b.getModifiers() & MouseEvent.SHIFT_MASK) != 0)) {
            shiftheld = (b.getModifiers() & MouseEvent.SHIFT_MASK) != 0;
        }

        if (b.getType() == BoardEvent.BOARD_HEX_DRAGGED) {
            if (!b.getCoords().equals(client.game.board.lastCursor) || shiftheld || gear == MovementDisplay.GEAR_TURN) {
                client.game.board.cursor(b.getCoords());

                // either turn or move
                if ( ce != null) {
                    currentMove(b.getCoords());
                    clientgui.bv.drawMovementData(ce, cmd);
                }
            }
        } else if (b.getType() == BoardEvent.BOARD_HEX_CLICKED) {

            Coords moveto = b.getCoords();
            clientgui.bv.drawMovementData(ce, cmd);

            client.game.board.select(b.getCoords());

            if (shiftheld || gear == MovementDisplay.GEAR_TURN) {
                butDone.setLabel("Move");
                return;
            }

            if (gear == MovementDisplay.GEAR_CHARGE) {
                // check if target is valid
                final Targetable target = this.chooseTarget( b.getCoords() );
                if (target == null || target.equals(ce)) {
                    clientgui.doAlertDialog("Can't perform charge", "No target!");
                    clearAllMoves();
                    return;
                }

                // check if it's a valid charge
                ToHitData toHit = new ChargeAttackAction(cen, target.getTargetType(), target.getTargetId(), target.getPosition()).toHit(client.game, cmd);
                if (toHit.getValue() != ToHitData.IMPOSSIBLE) {

                    // Determine how much damage the charger will take.
                    int toAttacker = 0;
                    if ( target.getTargetType() == Targetable.TYPE_ENTITY ) {
                        Entity te = (Entity) target;
                        toAttacker = ChargeAttackAction.getDamageTakenBy(ce,te, client.game.getOptions().booleanOption("maxtech_charge_damage"), cmd.getHexesMoved());
                    }
                    else if ( target.getTargetType() ==
                              Targetable.TYPE_BUILDING ) {
                        Building bldg = client.game.board.getBuildingAt
                            ( moveto );
                        toAttacker = ChargeAttackAction.getDamageTakenBy(ce,bldg);
                    }

                    // Ask the player if they want to charge.
                    if ( clientgui.doYesNoDialog
                         ( "Charge " + target.getDisplayName() + "?",
                           "To Hit: " + toHit.getValueAsString() +
                           " (" + Compute.oddsAbove(toHit.getValue()) +
                           "%)   (" + toHit.getDesc() + ")"
                           + "\nDamage to Target: "+
                           ChargeAttackAction.getDamageFor(ce,cmd.getHexesMoved())+
                           " (in 5pt clusters)"+ toHit.getTableDesc()
                           + "\nDamage to Self: " +
                           toAttacker +
                           " (in 5pt clusters)" ) ) {
                        // if they answer yes, charge the target.
                        cmd.getLastStep().setTarget(target);
                        moveTo(cmd);
                    } else {
                        // else clear movement
                        clearAllMoves();
                    };
                    return;
                } else {
                    // if not valid, tell why
                    clientgui.doAlertDialog( "Can't perform charge",
                                          toHit.getDesc() );
                    clearAllMoves();
                    return;
                }
            } else if (gear == MovementDisplay.GEAR_DFA) {
                // check if target is valid
                final Targetable target = this.chooseTarget( b.getCoords() );
                if (target == null || target.equals(ce)) {
                    clientgui.doAlertDialog("Can't perform D.F.A.", "No target!");
                    clearAllMoves();
                    return;
                }

                // check if it's a valid DFA
                ToHitData toHit = DfaAttackAction.toHit( client.game,
                                                    cen,
                                                    target,
                                                    cmd);
                if (toHit.getValue() != ToHitData.IMPOSSIBLE) {
                    // if yes, ask them if they want to DFA
                    if ( clientgui.doYesNoDialog
                         ( "D.F.A. " + target.getDisplayName() + "?",
                           "To Hit: " + toHit.getValueAsString() +
                           " (" + Compute.oddsAbove(toHit.getValue()) +
                           "%)   (" + toHit.getDesc() + ")"
                           + "\nDamage to Target: " +
                           DfaAttackAction.getDamageFor(ce) +
                           " (in 5pt clusters)" + toHit.getTableDesc()
                           + "\nDamage to Self: " +
                           DfaAttackAction.getDamageTakenBy(ce) +
                           " (in 5pt clusters) (using Kick table)" ) ) {
                        // if they answer yes, DFA the target
                        cmd.getLastStep().setTarget(target);
                        moveTo(cmd);
                    } else {
                        // else clear movement
                        clearAllMoves();
                    };
                    return;

                } else {
                    // if not valid, tell why
                    clientgui.doAlertDialog( "Can't perform D.F.A.",
                                          toHit.getDesc() );
                    clearAllMoves();
                    return;
                }
            }

            butDone.setLabel("Move");
            updateProneButtons();
            updateRACButton();
            updateLoadButtons();
        }
    }

    private synchronized void updateProneButtons() {
        final Entity ce = ce();
        
        
        if (ce != null && !ce.isImmobile()) {
            setGetUpEnabled(cmd.getFinalProne());
            setGoProneEnabled(!(butUp.isEnabled()) && ce instanceof Mech);
        } else {
            setGetUpEnabled(false);
            setGoProneEnabled(false);
        }
    }
    
    private void updateRACButton() {
        final Entity ce = ce();
        
        
        if ( null == ce ) {
            return;
        }
        setUnjamEnabled(ce.canUnjamRAC() && (gear == MovementDisplay.GEAR_LAND || gear == MovementDisplay.GEAR_TURN || gear == MovementDisplay.GEAR_BACKUP) && cmd.getMpUsed() <= ce.getWalkMP() );
    }

    private synchronized void updateLoadButtons() {
        final Entity ce = ce();
        
        
        // Disable the "Unload" button if we're in the wrong
        // gear or if the entity is not transporting units.
        if ( ( gear != MovementDisplay.GEAR_LAND &&
               gear != MovementDisplay.GEAR_TURN &&
               gear != MovementDisplay.GEAR_BACKUP ) ||
             loadedUnits.size() == 0 
             || cen == Entity.NONE) {
            setUnloadEnabled( false );
        }
        else {
            setUnloadEnabled( true );
        }

        // If the current entity has moved, disable "Load" button.
        if ( cmd.length() > 0 || cen == Entity.NONE ) {

            setLoadEnabled( false );

        } else {

            // Check the other entities in the current hex for friendly units.
            Entity other = null;
            Enumeration entities =
                client.game.getEntities( ce.getPosition() );
            while ( entities.hasMoreElements() ) {

                // Is the other unit friendly and not the current entity?
                other = (Entity)entities.nextElement();
                if ( ce.getOwner() == other.getOwner() &&
                     !ce.equals(other) ) {

                    // Yup. If the current entity has at least 1 MP, if it can
                    // transport the other unit, and if the other hasn't moved
                    // then enable the "Load" button.
                    if ( ce.getWalkMP() > 0 &&
                         ce.canLoad(other) &&
                         other.isSelectableThisTurn() ) {
                        setLoadEnabled( true );
                    }

                    // We can stop looking.
                    break;
                } else {
                    // Nope. Discard it.
                    other = null;
                }
            } // Check the next entity in this position.

        } // End ce-hasn't-moved

    } // private void updateLoadButtons

    /**
     * Get the unit that the player wants to unload. This method will
     * remove the unit from our local copy of loaded units.
     *
     * @return  The <code>Entity</code> that the player wants to unload.
     *          This value will not be <code>null</code>.
     */
    private Entity getUnloadedUnit() {
        Entity ce  = ce();
        Entity choice = null;
        // Handle error condition.
        if ( this.loadedUnits.size() == 0 ) {
            System.err.println( "MovementDisplay#getUnloadedUnit() called without loaded units." );

        }

        // If we have multiple choices, display a selection dialog.
        else if ( this.loadedUnits.size() > 1 ) {
            String[] names = new String[ this.loadedUnits.size() ];
            StringBuffer question = new StringBuffer();
            question.append( ce.getShortName() );
            question.append( " has the following unused space:\n" );
            question.append( ce.getUnusedString() );
            question.append( "\n\nWhich unit do you want to unload?" );
            for ( int loop = 0; loop < names.length; loop++ ) {
                names[loop] = ( (Entity)this.loadedUnits.elementAt(loop) ).getShortName();
            }
            SingleChoiceDialog choiceDialog =
                new SingleChoiceDialog( clientgui.frame,
                                        "Unload Unit",
                                        question.toString(),
                                        names );
            choiceDialog.show();
            if ( choiceDialog.getAnswer() == true ) {
                choice = (Entity) this.loadedUnits.elementAt( choiceDialog.getChoice() );
            }
        } // End have-choices

        // Only one choice.
        else {
            choice = (Entity) this.loadedUnits.elementAt( 0 );
            this.loadedUnits.removeElementAt( 0 );
        }

        // Return the chosen unit.
        return choice;
    }

    /**
     * Have the player select a target from the entities at the given coords.
     *
     * @param   pos - the <code>Coords</code> containing targets.
     */
    private Targetable chooseTarget( Coords pos ) {
        final Entity ce = ce();
        
        
        // Assume that we have *no* choice.
        Targetable choice = null;

        // Get the available choices.
        Enumeration choices = client.game.getEntities( pos );

        // Convert the choices into a List of targets.
        Vector targets = new Vector();
        while ( choices.hasMoreElements() ) {
            choice = (Targetable) choices.nextElement();
            if ( !ce.equals( choice ) ) {
                targets.addElement( choice );
            }
        }

        // Is there a building in the hex?
        Building bldg = client.game.board.getBuildingAt( pos );
        if ( bldg != null ) {
            targets.addElement( new BuildingTarget(pos, client.game.board, false) );
        }

        // Do we have a single choice?
        if ( targets.size() == 1 ) {

            // Return  that choice.
            choice = (Targetable) targets.elementAt( 0 );

        }

        // If we have multiple choices, display a selection dialog.
        else if ( targets.size() > 1 ) {
            String[] names = new String[ targets.size() ];
            StringBuffer question = new StringBuffer();
            question.append( "Hex " );
            question.append( pos.getBoardNum() );
            question.append( " contains the following targets." );
            question.append( "\n\nWhich target do you want to attack?" );
            for ( int loop = 0; loop < names.length; loop++ ) {
                names[loop] = ( (Targetable)targets.elementAt(loop) ).getDisplayName();
            }
            SingleChoiceDialog choiceDialog =
                new SingleChoiceDialog( clientgui.frame,
                                        "Choose Target",
                                        question.toString(),
                                        names );
            choiceDialog.show();
            if ( choiceDialog.getAnswer() == true ) {
                choice = (Targetable) targets.elementAt( choiceDialog.getChoice() );
            }
        } // End have-choices

        // Return the chosen unit.
        return choice;

    } // End private Targetable chooseTarget( Coords )

    //
    // GameListener
    //
    public void gameTurnChange(GameEvent ev) {

        // Are we ignoring events?
        if ( this.isIgnoringEvents() ) {
            return;
        }

        if (client.game.getPhase() != Game.PHASE_MOVEMENT) {
            // ignore
            return;
        }
        // else, change turn
        endMyTurn();

        if (client.isMyTurn()) {
            // Can the player unload entities stranded on immobile transports?
            if ( client.canUnloadStranded() ) {
                unloadStranded();
            } else {
                beginMyTurn();
            }

        } else {
            if (ev.getPlayer() == null && client.game.getTurn() instanceof GameTurn.UnloadStrandedTurn) {
                setStatusBarText("Please wait for another player to unload their stranded units...");
            } else {
                setStatusBarText("It's " + ev.getPlayer().getName() + "'s turn to move.");
            }
        }
    }
    public void gamePhaseChange(GameEvent ev) {

        // Are we ignoring events?
        if ( this.isIgnoringEvents() ) {
            return;
        }

        if (client.isMyTurn() && client.game.getPhase() != Game.PHASE_MOVEMENT) {
            endMyTurn();
        }
        if (client.game.getPhase() ==  Game.PHASE_MOVEMENT) {
            setStatusBarText("Waiting to begin Movement phase...");
        }
    }

    //
    // ActionListener
    //
    public synchronized void actionPerformed(ActionEvent ev) {
        final Entity ce = ce();
        
        
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
            moveTo(cmd);
        } else if (ev.getActionCommand().equals(MOVE_NEXT)) {
            selectEntity(client.getNextEntityNum(cen));
        } else if (ev.getActionCommand().equals(MOVE_CANCEL)) {       	
            clearAllMoves();
        } else if (ev.getSource() == butMore) {
            buttonLayout++;
            buttonLayout %= NUM_BUTTON_LAYOUTS;
            setupButtonPanel();
        } else if (ev.getActionCommand().equals(MOVE_UNJAM)) {
            if (gear == MovementDisplay.GEAR_JUMP || gear == MovementDisplay.GEAR_CHARGE || gear == MovementDisplay.GEAR_DFA || cmd.getMpUsed() > ce.getWalkMP()) { // in the wrong gear
                //clearAllMoves();
                //gear = Compute.GEAR_LAND;
                setUnjamEnabled(false);
            }
            else {
              cmd.addStep(MovePath.STEP_UNJAM_RAC);
              moveTo(cmd);
            }
        } else if (ev.getActionCommand().equals(MOVE_WALK)) {
            if (gear == MovementDisplay.GEAR_JUMP) {
                clearAllMoves();
            }
            gear = MovementDisplay.GEAR_LAND;
        } else if (ev.getActionCommand().equals(MOVE_JUMP)) {
            if (gear != MovementDisplay.GEAR_JUMP) {
                clearAllMoves();
            }
            if (!cmd.isJumping()) {
                cmd.addStep(MovePath.STEP_START_JUMP);
            }
            gear = MovementDisplay.GEAR_JUMP;
        } else if (ev.getActionCommand().equals(MOVE_TURN)) {
            gear = MovementDisplay.GEAR_TURN;
        } else if (ev.getActionCommand().equals(MOVE_BACK_UP)) {
            if (gear == MovementDisplay.GEAR_JUMP) {
                clearAllMoves();
            }
            gear = MovementDisplay.GEAR_BACKUP;
        } else if (ev.getActionCommand().equals(MOVE_CLEAR)) {       	
            clearAllMoves();
            if (!client.game.containsMinefield(ce.getPosition())) {
                clientgui.doAlertDialog( "Can't clear minefield",
                                      "No minefield in hex!" );
                return;
            }

            // Does the entity has a minesweeper?
            int clear = Minefield.CLEAR_NUMBER_INFANTRY;
            int boom = Minefield.CLEAR_NUMBER_INFANTRY_ACCIDENT;
            Enumeration equip = ce.getMisc();
            while ( equip.hasMoreElements() ) {
                Mounted mounted = (Mounted) equip.nextElement();
                if ( mounted.getType()
                     .hasFlag(MiscType.F_MINESWEEPER) ) {
                    clear = Minefield.CLEAR_NUMBER_SWEEPER;
                    boom = Minefield.CLEAR_NUMBER_SWEEPER_ACCIDENT;
                    break;
                }
            }

            StringBuffer buff = new StringBuffer();
            buff.append( "The unit successfully clears the\nminefield on " )
                .append( clear )
                .append( "+. The minefield\nwill explode on " )
                .append( boom )
                .append( " or less." );
            if ( clientgui.doYesNoDialog( "Clear the minefield?",
                                       buff.toString() ) ) {
                cmd.addStep(MovePath.STEP_CLEAR_MINEFIELD);
                moveTo(cmd);
            }
        } else if (ev.getActionCommand().equals(MOVE_CHARGE)) {
            if (gear != MovementDisplay.GEAR_LAND) {
                clearAllMoves();
            }
            gear = MovementDisplay.GEAR_CHARGE;
        } else if (ev.getActionCommand().equals(MOVE_DFA)) {
            if (gear != MovementDisplay.GEAR_JUMP) {
                clearAllMoves();
            }
            gear = MovementDisplay.GEAR_DFA;
            if (!cmd.isJumping()) {
                cmd.addStep(MovePath.STEP_START_JUMP);
            }
        } else if (ev.getActionCommand().equals(MOVE_GET_UP)) {
            clearAllMoves();
            if (cmd.getFinalProne()) {
                cmd.addStep(MovePath.STEP_GET_UP);
            }
            clientgui.bv.drawMovementData(ce, cmd);
            clientgui.bv.repaint();
            butDone.setLabel("Move");
        } else if (ev.getActionCommand().equals(MOVE_GO_PRONE)) {
            gear = MovementDisplay.GEAR_LAND;
            if (!cmd.getFinalProne()) {
                cmd.addStep(MovePath.STEP_GO_PRONE);
            }
            clientgui.bv.drawMovementData(ce, cmd);
            clientgui.bv.repaint();
            butDone.setLabel("Move");
        } else if (ev.getActionCommand().equals(MOVE_FLEE) && clientgui.doYesNoDialog("Escape?", "Do you want to flee?")) {
            clearAllMoves();
            cmd.addStep(MovePath.STEP_FLEE);
            moveTo(cmd);
        } else if (ev.getActionCommand().equals(MOVE_EJECT)) {
            if (ce instanceof Tank) {
                if (clientgui.doYesNoDialog("Abandon?", "Do you want to abandon this vehicle?")) {
                    clearAllMoves();
                    cmd.addStep(MovePath.STEP_EJECT);
                    moveTo(cmd);
                }
            } else if (clientgui.doYesNoDialog("Eject?", "Do you want to abandon this mech?")) {
                clearAllMoves();
                cmd.addStep(MovePath.STEP_EJECT);
                moveTo(cmd);
            }
        } else if ( ev.getActionCommand().equals(MOVE_LOAD) ) {
            // Find the other friendly unit in our hex, add it
            // to our local list of loaded units, and then stop.
            Entity other = null;
            Enumeration entities =
                client.game.getEntities( ce.getPosition() );
            while ( entities.hasMoreElements() ) {
                other = (Entity)entities.nextElement();
                if ( ce.getOwner() == other.getOwner() &&
                     !ce.equals(other) ) {
                    loadedUnits.addElement( other );
                    break;
                }
                other = null;
            }

            // Handle not finding a unit to load.
            if ( other != null ) {
                cmd.addStep( MovePath.STEP_LOAD );
                clientgui.bv.drawMovementData(ce, cmd);
                gear = MovementDisplay.GEAR_LAND;
            }
        }
        else if ( ev.getActionCommand().equals(MOVE_UNLOAD) ) {
            // Ask the user if we're carrying multiple units.
            Entity other = getUnloadedUnit();

            // Player can cancel the unload.
            if ( other != null ) {
                cmd.addStep( MovePath.STEP_UNLOAD, other );
                clientgui.bv.drawMovementData(ce, cmd);
            }
        }

        updateProneButtons();
        updateRACButton();
        updateLoadButtons();
    }

    /**
     * Give the player the opportunity to unload all entities that are
     * stranded on immobile transports.
     * <p/>
     * According to <a href="http://www.classicbattletech.com/w3t/showflat.php?Cat=&Board=ask&Number=555466&page=2&view=collapsed&sb=5&o=0&fpart=">
     * Randall Bills</a>, the "minimum move" rule allow stranded units to
     * dismount at the start of the turn.
     */
    private void unloadStranded() {
        Vector stranded = new Vector();
        String[] names = null;
        Entity entity = null;
        Entity transport = null;

        // Let the player know what's going on.
        setStatusBarText
            ("All players unload entities stranded on immobile transports.");

        // Collect the stranded entities into the vector.
        // TODO : get a better interface to "game" and "turn"
        Enumeration entities = client.getSelectedEntities
            ( new EntitySelector() {
                    private final Game game =
                        MovementDisplay.this.client.game;
                    private final GameTurn turn =
                        MovementDisplay.this.client.game.getTurn();
                    private final int ownerId =
                        MovementDisplay.this.client.getLocalPlayer().getId();
                    public boolean accept( Entity entity ) {
                        if ( turn.isValid( ownerId, entity, game ) )
                            return true;
                        return false;
                    }
                } );
        while ( entities.hasMoreElements() ) {
            stranded.addElement( entities.nextElement() );
        }

        // Construct an array of stranded entity names
        names = new String[ stranded.size() ];
        for ( int index = 0; index < names.length; index++ ) {
            StringBuffer buffer = new StringBuffer();
            entity = (Entity) stranded.elementAt(index);
            transport = client.getEntity( entity.getTransportId() );
            buffer.append( entity.getDisplayName() );
            if ( null != transport ) {
                buffer.append( " at " )
                    .append( transport.getPosition().getBoardNum() );
            }
            names[index] = buffer.toString();
        }

        // Show the choices to the player
        // TODO : implement this function!!!
        int[] indexes = clientgui.doChoiceDialog( "Unload Stranded Units", 
                                               "The following units are currently stranded\non immobile transports.  Select as any and\nall units that you want to unload.",
                                               names );

        // Convert the indexes into selected entity IDs and tell the server.
        int[] ids = null;
        if ( null != indexes ) {
            ids = new int[indexes.length];
            for ( int index = 0; index < indexes.length; index++ ) {
                entity = (Entity) stranded.elementAt(index);
                ids[index] = entity.getId();
            }
        }
        client.sendUnloadStranded( ids );
    }

    //
    // KeyListener
    //
    public void keyPressed(KeyEvent ev) {
        final Entity ce = ce();
        
        
        // Are we ignoring events?
        if ( this.isIgnoringEvents() ) {
            return;
        }

        if (ev.getKeyCode() == KeyEvent.VK_ESCAPE) {
            clearAllMoves();
        }
        if (ev.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
            if (client.isMyTurn()) {
	            removeLastStep();
            }
        }
        if (ev.getKeyCode() == KeyEvent.VK_ENTER && ev.isControlDown()) {
            if (client.isMyTurn()) {
                moveTo(cmd);
            }
        }
        if (ev.getKeyCode() == KeyEvent.VK_SHIFT && !shiftheld) {
            shiftheld = true;
            if (client.isMyTurn() && client.game.board.lastCursor != null && !client.game.board.lastCursor.equals(client.game.board.selected)) {
                // switch to turning
                //clientgui.bv.clearMovementData();
                currentMove(client.game.board.lastCursor);
                clientgui.bv.drawMovementData(ce, cmd);
            }
        }
        
        // arrow can also rotate when shift is down
        if (shiftheld && client.isMyTurn() && (ev.getKeyCode() == KeyEvent.VK_LEFT || ev.getKeyCode() == KeyEvent.VK_RIGHT)) {
            int curDir = cmd.getFinalFacing();
            int dir = curDir;
            if (ev.getKeyCode() == KeyEvent.VK_LEFT) {
                dir = (dir + 5) % 6;
            } else {
                dir = (dir + 7) % 6;
            }
            Coords curPos = cmd.getFinalCoords();
            Coords target = curPos.translated(dir);
            currentMove(target);
            clientgui.bv.drawMovementData(ce, cmd);
        }
    }
    
    public void keyReleased(KeyEvent ev) {

        // Are we ignoring events?
        if ( this.isIgnoringEvents() ) {
            return;
        }

        if (ev.getKeyCode() == KeyEvent.VK_SHIFT && shiftheld) {
            shiftheld = false;
            if (client.isMyTurn() && client.game.board.lastCursor != null && !client.game.board.lastCursor.equals(client.game.board.selected)) {
                // switch to movement
                clientgui.bv.clearMovementData();
                currentMove(client.game.board.lastCursor);
                clientgui.bv.drawMovementData(ce(), cmd);
            }
        }
    }
    public void keyTyped(KeyEvent ev) {
    }

    // board view listener 
    public void finishedMovingUnits(BoardViewEvent b) {
        final Entity ce = ce();
        
        
        // Are we ignoring events?
        if ( this.isIgnoringEvents() ) {
            return;
        }

        if (client.isMyTurn() && ce != null) {
            clientgui.setDisplayVisible(true);
            clientgui.bv.centerOnHex(ce.getPosition());
        }
    }
    public void selectUnit(BoardViewEvent b) {

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
                selectEntity(e.getId());
            }
    	} else {
            clientgui.setDisplayVisible(true);
            clientgui.mechD.displayEntity(e);
            if (e.isDeployed()) {
            	clientgui.bv.centerOnHex(e.getPosition());
            }
    	}
    }
    private void setWalkEnabled(boolean enabled) {
        butWalk.setEnabled(enabled);
        clientgui.getMenuBar().setMoveWalkEnabled(enabled);
    }
    private void setTurnEnabled(boolean enabled) {
        butTurn.setEnabled(enabled);
        clientgui.getMenuBar().setMoveTurnEnabled(enabled);
    }
    private void setNextEnabled(boolean enabled) {
        butNext.setEnabled(enabled);
        clientgui.getMenuBar().setMoveNextEnabled(enabled);
    }
    private void setLoadEnabled(boolean enabled) {
        butLoad.setEnabled(enabled);
        clientgui.getMenuBar().setMoveLoadEnabled(enabled);
    }
    private void setUnloadEnabled(boolean enabled) {
        butUnload.setEnabled(enabled);
        clientgui.getMenuBar().setMoveUnloadEnabled(enabled);
    }
    private void setJumpEnabled(boolean enabled) {
        butJump.setEnabled(enabled);
        clientgui.getMenuBar().setMoveJumpEnabled(enabled);
    }
    private void setBackUpEnabled(boolean enabled) {
        butBackup.setEnabled(enabled);
        clientgui.getMenuBar().setMoveBackUpEnabled(enabled);
    }
    private void setChargeEnabled(boolean enabled) {
        butCharge.setEnabled(enabled);
        clientgui.getMenuBar().setMoveChargeEnabled(enabled);
    }
    private void setDFAEnabled(boolean enabled) {
        butDfa.setEnabled(enabled);
        clientgui.getMenuBar().setMoveDFAEnabled(enabled);
    }
    private void setGoProneEnabled(boolean enabled) {
        butDown.setEnabled(enabled);
        clientgui.getMenuBar().setMoveGoProneEnabled(enabled);
    }
    private void setFleeEnabled(boolean enabled) {
        butFlee.setEnabled(enabled);
        clientgui.getMenuBar().setMoveFleeEnabled(enabled);
    }
    private void setEjectEnabled(boolean enabled) {
        butEject.setEnabled(enabled);
        clientgui.getMenuBar().setMoveEjectEnabled(enabled);
    }
    private void setUnjamEnabled(boolean enabled) {
        butRAC.setEnabled(enabled);
        clientgui.getMenuBar().setMoveUnjamEnabled(enabled);
    }
    private void setClearEnabled(boolean enabled) {
        butClear.setEnabled(enabled);
        clientgui.getMenuBar().setMoveClearEnabled(enabled);
    }
    private void setGetUpEnabled(boolean enabled) {
        butUp.setEnabled(enabled);
        clientgui.getMenuBar().setMoveGetUpEnabled(enabled);
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
