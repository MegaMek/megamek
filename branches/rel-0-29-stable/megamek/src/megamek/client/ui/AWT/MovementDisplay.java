/*
 * MegaMek - Copyright (C) 2000-2003 Ben Mazur (bmazur@sev.org)
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

public class MovementDisplay
    extends AbstractPhaseDisplay
    implements BoardListener,  ActionListener,
    KeyListener, GameListener
{
    private static final int    NUM_BUTTON_LAYOUTS = 3;

    // parent game
    public Client client;

    // displays
    private Label             statusL;
    private Panel             panStatus;
    private Button            butDisplay;
    private Button            butMap;

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

    // Hentai - for unjamming RAC (sets to Walk only)
    private Button            butRAC;
    private Button            butFlee;
    private Button            butEject;

    private Button            butLoad;
    private Button            butUnload;
    private Button            butReport;

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
    private boolean            shiftheld;

    /**
     * A local copy of the current entity's loaded units.
     */
    private Vector              loadedUnits = null;

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

        setupStatusBar();

        butSpace = new Button("");
        butSpace.setEnabled(false);

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

        butEject = new Button("Eject");
        butEject.addActionListener(this);
        butEject.setEnabled(false);

        butRAC = new Button("Unjam");
        butRAC.addActionListener(this);
        butRAC.setEnabled(false);

        butMore = new Button("More...");
        butMore.addActionListener(this);
        butMore.setEnabled(false);

        butNext = new Button(" Next Unit ");
        butNext.addActionListener(this);
        butNext.setEnabled(false);

        butDone = new Button("Move");
        butDone.addActionListener(this);
        butDone.setEnabled(false);

        butLoad = new Button("Load");
        butLoad.addActionListener(this);
        butLoad.setEnabled(false);

        butUnload = new Button("Unload");
        butUnload.addActionListener(this);
        butUnload.setEnabled(false);
        
        butReport = new Button("Report..");
        butReport.addActionListener(this);
        butReport.setEnabled(true);

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

        statusL = new Label("Waiting to begin Movement phase...", Label.CENTER);
        
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
        gridbag.setConstraints(statusL, c);
        panStatus.add(statusL);
        
        c.weightx = 0.0;    c.weighty = 0.0;
        gridbag.setConstraints(butDisplay, c);
        panStatus.add(butDisplay);
        
        c.gridwidth = GridBagConstraints.REMAINDER;
        panStatus.add(butMap);
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
            panButtons.add(butReport);
            panButtons.add(butMore);
            panButtons.add(butDone);
            break;
        case 1 :
            panButtons.add(butUp);
            panButtons.add(butCharge);
            panButtons.add(butDfa);
            panButtons.add(butNext);
            panButtons.add(butDown);
            panButtons.add(butSpace);
            panButtons.add(butMore);
            panButtons.add(butDone);
            break;
        case 2:
            panButtons.add(butFlee);
            panButtons.add(butLoad);
            panButtons.add(butRAC);
            panButtons.add(butNext);
            panButtons.add(butEject);
            panButtons.add(butUnload);
            panButtons.add(butMore);
            panButtons.add(butDone);
            break;
        }

        validate();
    }

    /**
     * Selects an entity, by number, for movement.
     */
    public void selectEntity(int en) {
        // clear any previously considered moves
        if (en != cen) {
            clearAllMoves();
        }
        
        // hmm, sometimes this gets called when there's no ready entities?
        if (client.game.getEntity(en) == null) {
            System.err.println("MovementDisplay: tried to select non-existant entity: " + en);
            return;
        }
        // okay.
        this.cen = en;

        md = new MovementData();
        cmd = new MovementData();
        gear = Compute.GEAR_LAND;
        loadedUnits = ce().getLoadedUnits();
        
        updateButtons();
        
        client.game.board.highlight(ce().getPosition());
        client.game.board.select(null);
        client.game.board.cursor(null);
        client.mechD.displayEntity(ce());
        client.mechD.showPanel("movement");
        client.bv.centerOnHex(ce().getPosition());
    }
    
    /**
     * Sets the buttons to their proper states
     */
    private void updateButtons() {
        boolean isMech = (ce() instanceof Mech);
        boolean isInfantry = (ce() instanceof Infantry);
        
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
            butDown.setEnabled(false);
        } else {
            butUp.setEnabled(false);
            butDown.setEnabled(isMech);
        }

        updateProneButtons();
        updateRACButton();
        updateLoadButtons();

        butFlee.setEnabled(Compute.canEntityFlee(client.game, cen));
        butEject.setEnabled(isMech);
        
    }

    /**
     * Enables relevant buttons and sets up for your turn.
     */
    private void beginMyTurn() {
        selectEntity(client.getFirstEntityNum());
        butDone.setLabel("Done");
        butDone.setEnabled(true);
        butNext.setEnabled(true);
        butMore.setEnabled(true);
        client.setDisplayVisible(true);
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
        client.setDisplayVisible(false);
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
        butEject.setEnabled(false);
        butRAC.setEnabled(false);
        butUp.setEnabled(false);
        butDown.setEnabled(false);
        butCharge.setEnabled(false);
        butDfa.setEnabled(false);
        butNext.setEnabled(false);
        butMore.setEnabled(false);
        butDone.setEnabled(false);
        butLoad.setEnabled(false);
        butUnload.setEnabled(false);
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
        butDone.setLabel("Done");
        updateProneButtons();
        updateRACButton();

        // We may not have an entity selected yet (race condition).
        if ( ce() != null ) {
            loadedUnits = ce().getLoadedUnits();
        } else {
            // The variable, loadedUnits, can not be null.
            loadedUnits = new Vector();
        }
        updateLoadButtons();
    }

    /**
     * Sends a data packet indicating the chosen movement.
     */
    private void moveTo(MovementData md) {
        if (md.hasActiveMASC() && Settings.nagForMASC) { //pop up are you sure dialog
            Mech m = (Mech)ce();
            ConfirmDialog nag = new ConfirmDialog(client.frame,"Are you sure?", "The movement you have selected will require a roll of " + m.getMASCTarget() + " or higher\nto avoid MASC failure.  Do you wish to proceed?", true);
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

        disableButtons();
        client.bv.clearMovementData();
        client.moveEntity(cen, md);
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

        if (b.getType() == BoardEvent.BOARD_HEX_DRAGGED) {
            if (!b.getCoords().equals(client.game.board.lastCursor) || shiftheld || gear == Compute.GEAR_TURN) {
                client.game.board.cursor(b.getCoords());

                // either turn or move
                cmd = md.getAppended(currentMove(md.getFinalCoords(ce().getPosition(), ce().getFacing()), md.getFinalFacing(ce().getFacing()), b.getCoords()));
                client.bv.drawMovementData(ce(), cmd);
            }
        } else if (b.getType() == BoardEvent.BOARD_HEX_CLICKED) {

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
                final Targetable target = this.chooseTarget( b.getCoords() );
                if (target == null || target.equals(ce())) {
                    client.doAlertDialog("Can't perform charge", "No target!");
                    clearAllMoves();
                    gear = Compute.GEAR_LAND;
                    return;
                }

                // check if it's a valid charge
                ToHitData toHit = Compute.toHitCharge( client.game,
                                                       cen,
                                                       target,
                                                       md);
                if (toHit.getValue() != ToHitData.IMPOSSIBLE) {

                    // Determine how much damage the charger will take.
                    int toAttacker = 0;
                    if ( target.getTargetType() == Targetable.TYPE_ENTITY ) {
                        Entity te = (Entity) target;
                        toAttacker = Compute.getChargeDamageTakenBy(ce(),te);
                    }
                    else if ( target.getTargetType() ==
                              Targetable.TYPE_BUILDING ) {
                        Building bldg = client.game.board.getBuildingAt
                            ( moveto );
                        toAttacker = Compute.getChargeDamageTakenBy(ce(),bldg);
                    }

                    // Ask the player if they want to charge.
                    if ( client.doYesNoDialog
                         ( "Charge " + target.getDisplayName() + "?",
                           "To Hit: " + toHit.getValueAsString() +
                           " (" + Compute.oddsAbove(toHit.getValue()) +
                           "%)   (" + toHit.getDesc() + ")"
                           + "\nDamage to Target: "+
                           Compute.getChargeDamageFor(ce(),md.getHexesMoved())+
                           " (in 5pt clusters)"+ toHit.getTableDesc()
                           + "\nDamage to Self: " +
                           toAttacker +
                           " (in 5pt clusters)" ) ) {
                        // if they answer yes, charge the target.
                        md.getStep(md.length()-1).setTarget(target);
                        moveTo(md);
                    } else {
                        // else clear movement
                        clearAllMoves();
                    };
                    return;
                } else {
                    // if not valid, tell why
                    client.doAlertDialog( "Can't perform charge",
                                          toHit.getDesc() );
                    clearAllMoves();
                    gear = Compute.GEAR_LAND;
                    return;
                }
            } else if (gear == Compute.GEAR_DFA) {
                // check if target is valid
                final Targetable target = this.chooseTarget( b.getCoords() );
                if (target == null || target.equals(ce())) {
                    client.doAlertDialog("Can't perform D.F.A.", "No target!");
                    clearAllMoves();
                    gear = Compute.GEAR_LAND;
                    return;
                }

                // check if it's a valid DFA
                ToHitData toHit = Compute.toHitDfa( client.game,
                                                    cen,
                                                    target,
                                                    md);
                if (toHit.getValue() != ToHitData.IMPOSSIBLE) {
                    // if yes, ask them if they want to DFA
                    if ( client.doYesNoDialog
                         ( "D.F.A. " + target.getDisplayName() + "?",
                           "To Hit: " + toHit.getValueAsString() +
                           " (" + Compute.oddsAbove(toHit.getValue()) +
                           "%)   (" + toHit.getDesc() + ")"
                           + "\nDamage to Target: " +
                           Compute.getDfaDamageFor(ce()) +
                           " (in 5pt clusters)" + toHit.getTableDesc()
                           + "\nDamage to Self: " +
                           Compute.getDfaDamageTakenBy(ce()) +
                           " (in 5pt clusters) (using Kick table)" ) ) {
                        // if they answer yes, DFA the target
                        md.getStep(md.length()-1).setTarget(target);
                        moveTo(md);
                    } else {
                        // else clear movement
                        clearAllMoves();
                    };
                    return;

                } else {
                    // if not valid, tell why
                    client.doAlertDialog( "Can't perform D.F.A.",
                                          toHit.getDesc() );
                    clearAllMoves();
                    gear = Compute.GEAR_LAND;
                    return;
                }
            }

            butDone.setLabel("Move");
            updateProneButtons();
            updateRACButton();
            updateLoadButtons();
        }
    }

    private void updateProneButtons() {
        if (ce() != null && md != null) {
            butUp.setEnabled(md.getFinalProne(ce().isProne()));
            butDown.setEnabled(!(butUp.isEnabled()) && ce() instanceof Mech);
        } else {
            butUp.setEnabled(false);
            butDown.setEnabled(false);
        }
    }
    
    private void updateRACButton() {
        if ( null == ce() || null == md ) {
            return;
        }
        butRAC.setEnabled(ce().canUnjamRAC() && (gear == Compute.GEAR_LAND || gear == Compute.GEAR_TURN || gear == Compute.GEAR_BACKUP) && md.getMpUsed() <= ce().getWalkMP() );
    }

    private void updateLoadButtons() {

        // Disable the "Unload" button if we're in the wrong
        // gear or if the entity is not transporting units.
        if ( ( gear != Compute.GEAR_LAND &&
               gear != Compute.GEAR_TURN &&
               gear != Compute.GEAR_BACKUP ) ||
             loadedUnits.size() == 0 
             || cen == Entity.NONE) {
            butUnload.setEnabled( false );
        }
        else {
            butUnload.setEnabled( true );
        }

        // If the current entity has moved, disable "Load" button.
        if ( md.length() > 0 || cen == Entity.NONE ) {

            butLoad.setEnabled( false );

        } else {

            // Check the other entities in the current hex for friendly units.
            Entity other = null;
            Enumeration entities =
                client.game.getEntities( ce().getPosition() );
            while ( entities.hasMoreElements() ) {

                // Is the other unit friendly and not the current entity?
                other = (Entity)entities.nextElement();
                if ( ce().getOwner() == other.getOwner() &&
                     !ce().equals(other) ) {

                    // Yup. If the current entity has at least 1 MP, if it can
                    // transport the other unit, and if the other hasn't moved
                    // then enable the "Load" button.
                    if ( ce().getWalkMP() > 0 &&
                         ce().canLoad(other) &&
                         other.isSelectable() ) {
                        butLoad.setEnabled( true );
                    }

                    // We can stop looking.
                    break;
                } else {
                    // Nope. Discard it.
                    other = null;
                }
            } // Check the next entity in this position.

        } // End ce()-hasn't-moved

    } // private void updateLoadButtons

    /**
     * Get the unit that the player wants to unload. This method will
     * remove the unit from our local copy of loaded units.
     *
     * @return  The <code>Entity</code> that the player wants to unload.
     *          This value will not be <code>null</code>.
     */
    private Entity getUnloadedUnit() {

        Entity choice = null;

        // Handle error condition.
        if ( this.loadedUnits.size() == 0 ) {
            System.err.println( "MovementDisplay#getUnloadedUnit() called without loaded units." );

        }

        // If we have multiple choices, display a selection dialog.
        else if ( this.loadedUnits.size() > 1 ) {
            String[] names = new String[ this.loadedUnits.size() ];
            StringBuffer question = new StringBuffer();
            question.append( ce().getShortName() );
            question.append( " has the following unused space:\n" );
            question.append( ce().getUnusedString() );
            question.append( "\n\nWhich unit do you want to unload?" );
            for ( int loop = 0; loop < names.length; loop++ ) {
                names[loop] = ( (Entity)this.loadedUnits.elementAt(loop) ).getShortName();
            }
            SingleChoiceDialog choiceDialog =
                new SingleChoiceDialog( client.frame,
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

        // Assume that we have *no* choice.
        Targetable choice = null;

        // Get the available choices.
        Enumeration choices = client.game.getEntities( pos );

        // Convert the choices into a List of targets.
        Vector targets = new Vector();
        while ( choices.hasMoreElements() ) {
            choice = (Targetable) choices.nextElement();
            if ( !ce().equals( choice ) ) {
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
                new SingleChoiceDialog( client.frame,
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
            moveTo(md);
        } else if (ev.getSource() == butNext) {
            selectEntity(client.getNextEntityNum(cen));
        } else if (ev.getSource() == butMore) {
            buttonLayout++;
            buttonLayout %= NUM_BUTTON_LAYOUTS;
            setupButtonPanel();
        } else if (ev.getSource() == butReport) {
            new MiniReportDisplay(client.frame, client.eotr).show();
            return;
        } else if (ev.getSource() == butRAC) {
            if (gear == Compute.GEAR_JUMP || gear == Compute.GEAR_CHARGE || gear == Compute.GEAR_DFA || md.getMpUsed() > ce().getWalkMP()) { // in the wrong gear
                //clearAllMoves();
                //gear = Compute.GEAR_LAND;
                butRAC.setEnabled(false);
            }
            else {
              md.addStep(MovementData.STEP_UNJAM_RAC);
              moveTo(md);
            }
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
            if (md.getFinalProne(ce().isProne())) {
                md.addStep(MovementData.STEP_GET_UP);
            }
            cmd = new MovementData(md);
            client.bv.drawMovementData(ce(), cmd);
            client.bv.repaint();
            butDone.setLabel("Move");
        } else if (ev.getSource() == butDown) {
            gear = Compute.GEAR_LAND;
            if (!md.getFinalProne(ce().isProne())) {
                md.addStep(MovementData.STEP_GO_PRONE);
            }
            cmd = new MovementData(md);
            client.bv.drawMovementData(ce(), cmd);
            client.bv.repaint();
            butDone.setLabel("Move");
        } else if (ev.getSource() == butFlee && client.doYesNoDialog("Escape?", "Do you want to flee?")) {
            clearAllMoves();
            md.addStep(MovementData.STEP_FLEE);
            moveTo(md);
        } else if (ev.getSource() == butEject && client.doYesNoDialog("Eject?", "Do you want to abandon this mech?")) {
            clearAllMoves();
            md.addStep(MovementData.STEP_EJECT);
            moveTo(md);
        }
        else if ( ev.getSource() == butLoad ) {
            // Find the other friendly unit in our hex, add it
            // to our local list of loaded units, and then stop.
            Entity other = null;
            Enumeration entities =
                client.game.getEntities( ce().getPosition() );
            while ( entities.hasMoreElements() ) {
                other = (Entity)entities.nextElement();
                if ( ce().getOwner() == other.getOwner() &&
                     !ce().equals(other) ) {
                    loadedUnits.addElement( other );
                    break;
                }
                other = null;
            }

            // Handle not finding a unit to load.
            if ( other != null ) {
                md.addStep( MovementData.STEP_LOAD );
                client.bv.drawMovementData(ce(), md);
                gear = Compute.GEAR_LAND;
            }
        }
        else if ( ev.getSource() == butUnload ) {
            // Ask the user if we're carrying multiple units.
            Entity other = getUnloadedUnit();

            // Player can cancel the unload.
            if ( other != null ) {
                cmd.addStep( MovementData.STEP_UNLOAD, other );
                md = new MovementData(cmd);
                client.bv.drawMovementData(ce(), cmd);
            }
        }

        updateProneButtons();
        updateRACButton();
        updateLoadButtons();
    }


    //
    // KeyListener
    //
    public void keyPressed(KeyEvent ev) {
        if (ev.getKeyCode() == KeyEvent.VK_ESCAPE) {
            clearAllMoves();
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
                //client.bv.clearMovementData();
                cmd = md.getAppended(currentMove(md.getFinalCoords(ce().getPosition(), ce().getFacing()), md.getFinalFacing(ce().getFacing()), client.game.board.lastCursor));
                client.bv.drawMovementData(ce(), cmd);
            }
        }
        
        // arrow can also rotate when shift is down
        if (shiftheld && client.isMyTurn() && (ev.getKeyCode() == KeyEvent.VK_LEFT || ev.getKeyCode() == KeyEvent.VK_RIGHT)) {
            int curDir = md.getFinalFacing(ce().getFacing());
            int dir = curDir;
            if (ev.getKeyCode() == KeyEvent.VK_LEFT) {
                dir = (dir + 5) % 6;
            } else {
                dir = (dir + 7) % 6;
            }
            Coords curPos = md.getFinalCoords(ce().getPosition(), ce().getFacing());
            Coords target = curPos.translated(dir);
            cmd = md.getAppended(currentMove(curPos, curDir, target));
            client.bv.drawMovementData(ce(), cmd);
            md = new MovementData(cmd);
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
}
