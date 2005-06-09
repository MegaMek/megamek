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

import megamek.client.event.BoardViewEvent;
import megamek.client.event.BoardViewListener;
import megamek.common.*;
import megamek.common.actions.ChargeAttackAction;
import megamek.common.actions.DfaAttackAction;
import megamek.common.event.GameListener;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.event.GameTurnChangeEvent;
import megamek.common.util.Distractable;
import megamek.common.util.DistractableAdapter;

public class MovementDisplay
    extends StatusBarPhaseDisplay
    implements ActionListener, DoneButtoned,
               KeyListener, GameListener, BoardViewListener, Distractable
{
    // Distraction implementation.
    private DistractableAdapter distracted = new DistractableAdapter();

    private static final int    NUM_BUTTON_LAYOUTS = 4;

    public static final String    MOVE_WALK = "moveWalk"; //$NON-NLS-1$
    public static final String    MOVE_NEXT = "moveNext"; //$NON-NLS-1$
    public static final String    MOVE_JUMP = "moveJump"; //$NON-NLS-1$
    public static final String    MOVE_BACK_UP = "moveBackUp"; //$NON-NLS-1$
    public static final String    MOVE_TURN = "moveTurn"; //$NON-NLS-1$
    public static final String    MOVE_GET_UP = "moveGetUp"; //$NON-NLS-1$
    public static final String    MOVE_CHARGE = "moveCharge"; //$NON-NLS-1$
    public static final String    MOVE_DFA = "moveDFA"; //$NON-NLS-1$
    public static final String    MOVE_GO_PRONE = "moveGoProne"; //$NON-NLS-1$
    public static final String    MOVE_FLEE = "moveFlee"; //$NON-NLS-1$
    public static final String    MOVE_EJECT = "moveEject"; //$NON-NLS-1$
    public static final String    MOVE_LOAD = "moveLoad"; //$NON-NLS-1$
    public static final String    MOVE_UNLOAD = "moveUnload"; //$NON-NLS-1$
    public static final String    MOVE_UNJAM = "moveUnjam"; //$NON-NLS-1$
    public static final String    MOVE_CLEAR = "moveClear"; //$NON-NLS-1$
    public static final String    MOVE_CANCEL   = "moveCancel"; //$NON-NLS-1$
    public static final String    MOVE_RAISE_ELEVATION = "moveRaiseElevation"; //$NON-NLS-1$
    public static final String    MOVE_LOWER_ELEVATION = "moveLowerElevation"; //$NON-NLS-1$

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
    
    private Button            butRaise;
    private Button            butLower;

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
        client.game.addGameListener(this);

        gear = MovementDisplay.GEAR_LAND;

        shiftheld = false;

        clientgui.getBoardView().addBoardViewListener(this);

        setupStatusBar(Messages.getString("MovementDisplay.waitingForMovementPhase")); //$NON-NLS-1$

        butClear = new Button(Messages.getString("MovementDisplay.butClear")); //$NON-NLS-1$
        butClear.addActionListener(this);
        butClear.setEnabled(false);
        butClear.setActionCommand(MOVE_CLEAR);
        butClear.addKeyListener(this);

        butWalk = new Button(Messages.getString("MovementDisplay.butWalk")); //$NON-NLS-1$
        butWalk.addActionListener(this);
        butWalk.setEnabled(false);
        butWalk.setActionCommand(MOVE_WALK);
        butWalk.addKeyListener(this);

        butJump = new Button(Messages.getString("MovementDisplay.butJump")); //$NON-NLS-1$
        butJump.addActionListener(this);
        butJump.setEnabled(false);
        butJump.setActionCommand(MOVE_JUMP);
        butJump.addKeyListener(this);

        butBackup = new Button(Messages.getString("MovementDisplay.butBackup")); //$NON-NLS-1$
        butBackup.addActionListener(this);
        butBackup.setEnabled(false);
        butBackup.setActionCommand(MOVE_BACK_UP);
        butBackup.addKeyListener(this);

        butTurn = new Button(Messages.getString("MovementDisplay.butTurn")); //$NON-NLS-1$
        butTurn.addActionListener(this);
        butTurn.setEnabled(false);
        butTurn.setActionCommand(MOVE_TURN);
        butTurn.addKeyListener(this);

        butUp = new Button(Messages.getString("MovementDisplay.butUp")); //$NON-NLS-1$
        butUp.addActionListener(this);
        butUp.setEnabled(false);
        butUp.setActionCommand(MOVE_GET_UP);
        butUp.addKeyListener(this);

        butDown = new Button(Messages.getString("MovementDisplay.butDown")); //$NON-NLS-1$
        butDown.addActionListener(this);
        butDown.setEnabled(false);
        butDown.setActionCommand(MOVE_GO_PRONE);
        butDown.addKeyListener(this);

        butCharge = new Button(Messages.getString("MovementDisplay.butCharge")); //$NON-NLS-1$
        butCharge.addActionListener(this);
        butCharge.setEnabled(false);
        butCharge.setActionCommand(MOVE_CHARGE);
        butCharge.addKeyListener(this);

        butDfa = new Button(Messages.getString("MovementDisplay.butDfa")); //$NON-NLS-1$
        butDfa.addActionListener(this);
        butDfa.setEnabled(false);
        butDfa.setActionCommand(MOVE_DFA);
        butDfa.addKeyListener(this);

        butFlee = new Button(Messages.getString("MovementDisplay.butFlee")); //$NON-NLS-1$
        butFlee.addActionListener(this);
        butFlee.setEnabled(false);
        butFlee.setActionCommand(MOVE_FLEE);
        butFlee.addKeyListener(this);

        butEject = new Button(Messages.getString("MovementDisplay.butEject")); //$NON-NLS-1$
        butEject.addActionListener(this);
        butEject.setEnabled(false);
        butEject.setActionCommand(MOVE_EJECT);
        butEject.addKeyListener(this);

        butRAC = new Button(Messages.getString("MovementDisplay.butRAC")); //$NON-NLS-1$
        butRAC.addActionListener(this);
        butRAC.setEnabled(false);
        butRAC.setActionCommand(MOVE_UNJAM);
        butRAC.addKeyListener(this);

        butMore = new Button(Messages.getString("MovementDisplay.butMore")); //$NON-NLS-1$
        butMore.addActionListener(this);
        butMore.setEnabled(false);
        butMore.addKeyListener(this);

        butNext = new Button(Messages.getString("MovementDisplay.butNext")); //$NON-NLS-1$
        butNext.addActionListener(this);
        butNext.setEnabled(false);
        butNext.setActionCommand(MOVE_NEXT);
        butNext.addKeyListener(this);

        butDone = new Button(Messages.getString("MovementDisplay.butDone")); //$NON-NLS-1$
        butDone.addActionListener(this);
        butDone.setEnabled(false);
        butDone.addKeyListener(this);

        butLoad = new Button(Messages.getString("MovementDisplay.butLoad")); //$NON-NLS-1$
        butLoad.addActionListener(this);
        butLoad.setEnabled(false);
        butLoad.setActionCommand(MOVE_LOAD);
        butLoad.addKeyListener(this);

        butUnload = new Button(Messages.getString("MovementDisplay.butUnload")); //$NON-NLS-1$
        butUnload.addActionListener(this);
        butUnload.setEnabled(false);
        butUnload.setActionCommand(MOVE_UNLOAD);
        butUnload.addKeyListener(this);
        
        butRaise = new Button(Messages.getString("MovementDisplay.butRaise"));
        butRaise.addActionListener(this);
        butRaise.setEnabled(false);
        butRaise.setActionCommand(MOVE_RAISE_ELEVATION);
        butRaise.addKeyListener(this);
        
        butLower = new Button(Messages.getString("MovementDisplay.butLower"));
        butLower.addActionListener(this);
        butLower.setEnabled(false);
        butLower.setActionCommand(MOVE_LOWER_ELEVATION);
        butLower.addKeyListener(this);
        
        butSpace = new Button("."); //$NON-NLS-1$
        butSpace.setEnabled(false);
        butSpace.addKeyListener(this);

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
        case 3:
            panButtons.add(butRaise);
            panButtons.add(butLower);
            panButtons.add(butSpace);
            panButtons.add(butSpace);
            panButtons.add(butSpace);
            panButtons.add(butSpace);
            panButtons.add(butMore);
            
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
            System.err.println("MovementDisplay: tried to select non-existant entity: " + en); //$NON-NLS-1$
            return;
        }
        
        Entity oldSelected = client.game.getEntity(cen);

        this.cen = en;
        clientgui.setSelectedEntityNum(en);

        clearAllMoves();
        updateButtons();
        // Update the menu bar.
        clientgui.getMenuBar().setEntity( ce );
        
        clientgui.getBoardView().highlight(ce.getPosition());
        clientgui.getBoardView().select(null);
        clientgui.getBoardView().cursor(null);
        clientgui.mechD.displayEntity(ce);
        clientgui.mechD.showPanel("movement"); //$NON-NLS-1$
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
        boolean isVTOL      = (ce instanceof VTOL);
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
        updateElevationButtons();

        setFleeEnabled(ce.canFlee());
        if (client.game.getOptions().booleanOption("vehicles_can_eject")) { //$NON-NLS-1$
          setEjectEnabled ( (!isInfantry) && ce.isActive());
        } else {
          setEjectEnabled(isMech && ce.isActive());
        }
        
    }

    /**
     * Enables relevant buttons and sets up for your turn.
     */
    private void beginMyTurn() {
        setStatusBarText(Messages.getString("MovementDisplay.its_your_turn")); //$NON-NLS-1$
        selectEntity(client.getFirstEntityNum());
        butDone.setLabel(Messages.getString("MovementDisplay.Done")); //$NON-NLS-1$
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
        if ( IGame.PHASE_MOVEMENT == client.game.getPhase()
             && null != next
             && null != ce
             && next.getOwnerId() != ce.getOwnerId() ) {
            clientgui.setDisplayVisible(false);
        }
        cen = Entity.NONE;
        clientgui.getBoardView().select(null);
        clientgui.getBoardView().highlight(null);
        clientgui.getBoardView().cursor(null);
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
        clientgui.getBoardView().select(null);
        clientgui.getBoardView().cursor(null);
        
        // create new current and considered paths
        cmd = new MovePath(client.game, ce);
        
        // set to "walk," or the equivalent
        gear = MovementDisplay.GEAR_LAND;
        
        // update some GUI elements
        clientgui.bv.clearMovementData();
        butDone.setLabel(Messages.getString("MovementDisplay.Done")); //$NON-NLS-1$
        updateProneButtons();
        updateRACButton();
        updateElevationButtons();

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

            // Set the button's label to "Done"
            // if the entire move is impossible.
            MovePath possible = (MovePath) cmd.clone();
            possible.clipToPossible();
            if (possible.length() == 0) {
                butDone.setLabel( Messages.getString("MovementDisplay.Done") ); //$NON-NLS-1$
            }
        }
    }

    /**
     * Sends a data packet indicating the chosen movement.
     */
    private synchronized void moveTo(MovePath md) {
        md.clipToPossible();
        if (md.length() == 0 && GUIPreferences.getInstance().getNagForNoAction()) {
            //Hmm....no movement steps, comfirm this action
            String title = Messages.getString("MovementDisplay.ConfirmNoMoveDlg.title"); //$NON-NLS-1$
            String body = Messages.getString("MovementDisplay.ConfirmNoMoveDlg.message"); //$NON-NLS-1$
            ConfirmDialog response = clientgui.doYesNoBotherDialog(title, body);
            if ( !response.getShowAgain() ) {
                GUIPreferences.getInstance().setNagForNoAction(false);
            }
            if ( !response.getAnswer() ) {
                return;
            }
        }

        if ( md != null ) {
            if (md.hasActiveMASC() && GUIPreferences.getInstance().getNagForMASC()) { //pop up are you sure dialog
                Mech m = (Mech)ce();
                ConfirmDialog nag = new ConfirmDialog(clientgui.frame,Messages.getString("MovementDisplay.areYouSure"), //$NON-NLS-1$
                        Messages.getString("MovementDisplay.ConfirmMoveRoll", new Object[]{new Integer(m.getMASCTarget())}), //$NON-NLS-1$
                        true);
                nag.setVisible(true);
                if (nag.getAnswer()) {
                    // do they want to be bothered again?
                    if (!nag.getShowAgain()) {
                        GUIPreferences.getInstance().setNagForMASC(false);
                    }
                } else {
                    return;
                }
            }

            String check = doPSRCheck(md);
            if (check.length() > 0 && GUIPreferences.getInstance().getNagForPSR()) {
                ConfirmDialog nag = 
                    new ConfirmDialog(clientgui.frame,
                                      Messages.getString("MovementDisplay.areYouSure"),  //$NON-NLS-1$
                                      Messages.getString("MovementDisplay.ConfirmPilotingRoll")+ //$NON-NLS-1$
                                      check, true);
                nag.setVisible(true);
                if (nag.getAnswer()) {
                    // do they want to be bothered again?
                    if (!nag.getShowAgain()) {
                        GUIPreferences.getInstance().setNagForPSR(false);
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
        return Messages.getString("MovementDisplay.addNag", new Object[]{rollTarget.getValueAsString(), rollTarget.getDesc()});//$NON-NLS-1$
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
        int moveType = IEntityMovementType.MOVE_NONE;
        int overallMoveType = IEntityMovementType.MOVE_NONE;
        boolean firstStep;
        int prevFacing = curFacing;
        IHex prevHex = null;
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
            if (step.getMovementType() == IEntityMovementType.MOVE_ILLEGAL) {
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

            final IHex curHex = client.game.getBoard().getHex(curPos);

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
            
            if(entity instanceof VTOL) {
                rollTarget = ((VTOL)entity).checkSideSlip(moveType,prevHex,overallMoveType,
                                             prevStep,prevFacing,curFacing,
                                             lastPos,curPos,distance);
                if(rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                    nagReport.append(addNag(rollTarget));
                }
            }
            
            // check if we've moved into swamp
            rollTarget = entity.checkSwampMove(step, curHex, lastPos, curPos);
            if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                nagReport.append(addNag(rollTarget));
            }
            
            // check if we used more MPs than the Mech/Vehicle would have in normal gravity
            if (!i.hasMoreElements() && !firstStep) {
                if (entity instanceof Mech) {
                    if ((step.getMovementType() == IEntityMovementType.MOVE_WALK) || (step.getMovementType() == IEntityMovementType.MOVE_RUN)) {
                        if (step.getMpUsed() > entity.getRunMP(false)) {
                            rollTarget = entity.checkMovedTooFast(step);
                            if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                                nagReport.append(addNag(rollTarget));
                            }
                        }
                    } else if (step.getMovementType() == IEntityMovementType.MOVE_JUMP) {
                        if (step.getMpUsed() > entity.getOriginalJumpMP()) {
                            rollTarget = entity.checkMovedTooFast(step);
                            if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                                nagReport.append(addNag(rollTarget));
                            }
                        }    
                      }
                } else if (entity instanceof Tank) {
                    if ((step.getMovementType() == IEntityMovementType.MOVE_WALK) || (step.getMovementType() == IEntityMovementType.MOVE_RUN)) {
                        // For Tanks, we need to check if the tank had more MPs because it was moving along a road
                        if (step.getMpUsed() > entity.getRunMP(false) && !step.isOnlyPavement()) {
                            rollTarget = entity.checkMovedTooFast(step);
                            if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                                nagReport.append(addNag(rollTarget));
                            }    
                        }
                        // If the tank was moving on a road, he got a +1 bonus.
                        // N.B. The Ask Precentor Martial forum said that a 4/6
                        //      tank on a road can move 5/7, **not** 5/8.
                        else if (step.getMpUsed() > entity.getRunMP(false) + 1)
                        {
                            rollTarget = entity.checkMovedTooFast(step);
                            if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                                nagReport.append(addNag(rollTarget));
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
                Building bldgExited = client.game.getBoard().getBuildingAt( lastPos );

                // Get the building being entered.
                // TODO: allow units to climb on top of buildings.
                Building bldgEntered = client.game.getBoard().getBuildingAt( curPos );

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
        if (overallMoveType == IEntityMovementType.MOVE_JUMP && !entity.isMakingDfa()) {
            // check for damaged criticals
            rollTarget = entity.checkLandingWithDamage();
            if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                nagReport.append(addNag(rollTarget));
            }
            // jumped into water?
            int waterLevel = client.game.getBoard().getHex(curPos).terrainLevel(Terrains.WATER);
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
    public synchronized void hexMoused(BoardViewEvent b) {
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

        if (b.getType() == BoardViewEvent.BOARD_HEX_DRAGGED) {
            if (!b.getCoords().equals(clientgui.getBoardView().getLastCursor()) || shiftheld || gear == MovementDisplay.GEAR_TURN) {
                clientgui.getBoardView().cursor(b.getCoords());

                // either turn or move
                if ( ce != null) {
                    currentMove(b.getCoords());
                    clientgui.bv.drawMovementData(ce, cmd);
                }
            }
        } else if (b.getType() == BoardViewEvent.BOARD_HEX_CLICKED) {

            Coords moveto = b.getCoords();
            clientgui.bv.drawMovementData(ce, cmd);

            clientgui.getBoardView().select(b.getCoords());

            if (shiftheld || gear == MovementDisplay.GEAR_TURN) {
                butDone.setLabel(Messages.getString("MovementDisplay.Move")); //$NON-NLS-1$

                // Set the button's label to "Done"
                // if the entire move is impossible.
                MovePath possible = (MovePath) cmd.clone();
                possible.clipToPossible();
                if (possible.length() == 0) {
                    butDone.setLabel( Messages.getString("MovementDisplay.Done") ); //$NON-NLS-1$
                }
                return;
            }

            if (gear == MovementDisplay.GEAR_CHARGE) {
                // check if target is valid
                final Targetable target = this.chooseTarget( b.getCoords() );
                if (target == null || target.equals(ce)) {
                    clientgui.doAlertDialog(Messages.getString("MovementDisplay.CantCharge"), Messages.getString("MovementDisplay.NoTarget")); //$NON-NLS-1$ //$NON-NLS-2$
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
                        toAttacker = ChargeAttackAction.getDamageTakenBy(ce,te, client.game.getOptions().booleanOption("maxtech_charge_damage"), cmd.getHexesMoved()); //$NON-NLS-1$
                    }
                    else if ( target.getTargetType() ==
                              Targetable.TYPE_BUILDING ) {
                        Building bldg = client.game.getBoard().getBuildingAt
                            ( moveto );
                        toAttacker = ChargeAttackAction.getDamageTakenBy(ce,bldg);
                    }

                    // Ask the player if they want to charge.
                    if ( clientgui.doYesNoDialog
                         ( Messages.getString("MovementDisplay.ChargeDialog.title", new Object[]{target.getDisplayName()}), //$NON-NLS-1$
                           Messages.getString("MovementDisplay.ChargeDialog.message", new Object[]{ //$NON-NLS-1$      
                           toHit.getValueAsString(), new Double(Compute.oddsAbove(toHit.getValue())),toHit.getDesc(),
                           new Integer(ChargeAttackAction.getDamageFor(ce,cmd.getHexesMoved())),toHit.getTableDesc(),
                           new Integer(toAttacker)}))) {
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
                    clientgui.doAlertDialog( Messages.getString("MovementDisplay.CantCharge"), //$NON-NLS-1$
                                          toHit.getDesc() );
                    clearAllMoves();
                    return;
                }
            } else if (gear == MovementDisplay.GEAR_DFA) {
                // check if target is valid
                final Targetable target = this.chooseTarget( b.getCoords() );
                if (target == null || target.equals(ce)) {
                    clientgui.doAlertDialog(Messages.getString("MovementDisplay.CantDFA"), Messages.getString("MovementDisplay.NoTarget")); //$NON-NLS-1$ //$NON-NLS-2$
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
                         ( Messages.getString("MovementDisplay.DFADialog.title", new Object[]{target.getDisplayName()}), //$NON-NLS-1$
                           Messages.getString("MovementDisplay.DFADialog.message", new Object[]{ //$NON-NLS-1$
                                   toHit.getValueAsString(),new Double(Compute.oddsAbove(toHit.getValue())),
                                   toHit.getDesc(), new Integer(DfaAttackAction.getDamageFor(ce)), toHit.getTableDesc(),
                                   new Integer(DfaAttackAction.getDamageTakenBy(ce))}))) {
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
                    clientgui.doAlertDialog( Messages.getString("MovementDisplay.CantDFA"), //$NON-NLS-1$
                                          toHit.getDesc() );
                    clearAllMoves();
                    return;
                }
            }

            butDone.setLabel(Messages.getString("MovementDisplay.Move")); //$NON-NLS-1$
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
    
    private synchronized void updateElevationButtons() {
        final Entity ce = ce();
        
        if(null == ce) {
            return;
        }
        if(ce instanceof VTOL) {
            setRaiseEnabled(true);
            setLowerEnabled(((VTOL)ce).canGoDown(cmd.getFinalElevation(),cmd.getFinalCoords())? true : false);
        } else {
            setRaiseEnabled(false);
            setLowerEnabled(false);
        }
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
            System.err.println( "MovementDisplay#getUnloadedUnit() called without loaded units." ); //$NON-NLS-1$

        }

        // If we have multiple choices, display a selection dialog.
        else if ( this.loadedUnits.size() > 1 ) {
            String[] names = new String[ this.loadedUnits.size() ];
            String question = Messages.getString("MovementDisplay.UnloadUnitDialog.message", new Object[]{ //$NON-NLS-1$
                    ce.getShortName(),ce.getUnusedString()});
            for ( int loop = 0; loop < names.length; loop++ ) {
                names[loop] = ( (Entity)this.loadedUnits.elementAt(loop) ).getShortName();
            }
            SingleChoiceDialog choiceDialog =
                new SingleChoiceDialog( clientgui.frame,
                                        Messages.getString("MovementDisplay.UnloadUnitDialog.title"), //$NON-NLS-1$
                                        question,
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
        Building bldg = client.game.getBoard().getBuildingAt( pos );
        if ( bldg != null ) {
            targets.addElement( new BuildingTarget(pos, client.game.getBoard(), false) );
        }

        // Do we have a single choice?
        if ( targets.size() == 1 ) {

            // Return  that choice.
            choice = (Targetable) targets.elementAt( 0 );

        }

        // If we have multiple choices, display a selection dialog.
        else if ( targets.size() > 1 ) {
            String[] names = new String[ targets.size() ];
            String question = Messages.getString("MovementDisplay.ChooseTargetDialog.message", new Object[]{//$NON-NLS-1$
                    pos.getBoardNum()});
            for ( int loop = 0; loop < names.length; loop++ ) {
                names[loop] = ( (Targetable)targets.elementAt(loop) ).getDisplayName();
            }
            SingleChoiceDialog choiceDialog =
                new SingleChoiceDialog( clientgui.frame,
                                        Messages.getString("MovementDisplay.ChooseTargetDialog.title"), //$NON-NLS-1$
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
    public void gameTurnChange(GameTurnChangeEvent e) {

        // Are we ignoring events?
        if ( this.isIgnoringEvents() ) {
            return;
        }

        if (client.game.getPhase() != IGame.PHASE_MOVEMENT) {
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
            if (e.getPlayer() == null && client.game.getTurn() instanceof GameTurn.UnloadStrandedTurn) {
                setStatusBarText(Messages.getString("MovementDisplay.waitForAnother")); //$NON-NLS-1$
            } else {
                setStatusBarText(Messages.getString("MovementDisplay.its_others_turn", new Object[]{e.getPlayer().getName()})); //$NON-NLS-1$
            }
        }
    }
    public void gamePhaseChange(GamePhaseChangeEvent e) {

        // Are we ignoring events?
        if ( this.isIgnoringEvents() ) {
            return;
        }

        if (client.isMyTurn() && client.game.getPhase() != IGame.PHASE_MOVEMENT) {
            endMyTurn();
        }
        if (client.game.getPhase() ==  IGame.PHASE_MOVEMENT) {
            setStatusBarText(Messages.getString("MovementDisplay.waitingForMovementPhase")); //$NON-NLS-1$
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
                clientgui.doAlertDialog( Messages.getString("MovementDisplay.CantClearMinefield"), //$NON-NLS-1$
                                      Messages.getString("MovementDisplay.NoMinefield") ); //$NON-NLS-1$
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

            if (clientgui.doYesNoDialog( Messages.getString("MovementDisplay.ClearMinefieldDialog.title"), //$NON-NLS-1$
                    Messages.getString("MovementDisplay.ClearMinefieldDialog.message", new Object[]{ //$NON-NLS-1$
                            new Integer(clear), new Integer(boom)})
            )) {
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
            butDone.setLabel(Messages.getString("MovementDisplay.Move")); //$NON-NLS-1$
        } else if (ev.getActionCommand().equals(MOVE_GO_PRONE)) {
            gear = MovementDisplay.GEAR_LAND;
            if (!cmd.getFinalProne()) {
                cmd.addStep(MovePath.STEP_GO_PRONE);
            }
            clientgui.bv.drawMovementData(ce, cmd);
            clientgui.bv.repaint();
            butDone.setLabel(Messages.getString("MovementDisplay.Move")); //$NON-NLS-1$
        } else if (ev.getActionCommand().equals(MOVE_FLEE) && clientgui.doYesNoDialog(Messages.getString("MovementDisplay.EscapeDialog.title"), Messages.getString("MovementDisplay.EscapeDialog.message"))) { //$NON-NLS-1$ //$NON-NLS-2$
            clearAllMoves();
            cmd.addStep(MovePath.STEP_FLEE);
            moveTo(cmd);
        } else if (ev.getActionCommand().equals(MOVE_EJECT)) {
            if (ce instanceof Tank) {
                if (clientgui.doYesNoDialog(Messages.getString("MovementDisplay.AbandonDialog.title"), Messages.getString("MovementDisplay.AbandonDialog.message"))) { //$NON-NLS-1$ //$NON-NLS-2$
                    clearAllMoves();
                    cmd.addStep(MovePath.STEP_EJECT);
                    moveTo(cmd);
                }
            } else if (clientgui.doYesNoDialog(Messages.getString("MovementDisplay.AbandonDialog1.title"), Messages.getString("MovementDisplay.AbandonDialog1.message"))) { //$NON-NLS-1$ //$NON-NLS-2$
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
        else if (ev.getActionCommand().equals(MOVE_RAISE_ELEVATION) ) {
            cmd.addStep(MovePath.STEP_UP);
        }
        else if (ev.getActionCommand().equals(MOVE_LOWER_ELEVATION) ) {
            cmd.addStep(MovePath.STEP_DOWN);
        }

        updateProneButtons();
        updateRACButton();
        updateLoadButtons();
        updateElevationButtons();
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
            (Messages.getString("MovementDisplay.AllPlayersUnload")); //$NON-NLS-1$

        // Collect the stranded entities into the vector.
        // TODO : get a better interface to "game" and "turn"
        Enumeration entities = client.getSelectedEntities
            ( new EntitySelector() {
                    private final IGame game =
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
            entity = (Entity) stranded.elementAt(index);
            transport = client.getEntity( entity.getTransportId() );
            String buffer;
            if ( null == transport ) {
                buffer = entity.getDisplayName();
            }
            else {
                buffer = Messages.getString("MovementDisplay.EntityAt", new Object[]{entity.getDisplayName(),transport.getPosition().getBoardNum()}); //$NON-NLS-1$ 
            }
            names[index] = buffer.toString();
        }

        // Show the choices to the player
        // TODO : implement this function!!!
        int[] indexes = clientgui.doChoiceDialog( Messages.getString("MovementDisplay.UnloadStrandedUnitsDialog.title"),  //$NON-NLS-1$
                                               Messages.getString("MovementDisplay.UnloadStrandedUnitsDialog.message"), //$NON-NLS-1$
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

System.err.println("!!!Here I am!");
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
            if (client.isMyTurn() && clientgui.getBoardView().getLastCursor() != null && !clientgui.getBoardView().getLastCursor().equals(clientgui.getBoardView().getSelected())) {
                // switch to turning
                //clientgui.bv.clearMovementData();
                currentMove(clientgui.getBoardView().getLastCursor());
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
            if (client.isMyTurn() && clientgui.getBoardView().getLastCursor() != null && !clientgui.getBoardView().getLastCursor().equals(clientgui.getBoardView().getSelected())) {
                // switch to movement
                clientgui.bv.clearMovementData();
                currentMove(clientgui.getBoardView().getLastCursor());
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
    private void setRaiseEnabled(boolean enabled) {
        butRaise.setEnabled(enabled);
        clientgui.getMenuBar().setMoveRaiseEnabled(enabled);
    }
    private void setLowerEnabled(boolean enabled) {
        butLower.setEnabled(enabled);
        clientgui.getMenuBar().setMoveLowerEnabled(enabled);
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
