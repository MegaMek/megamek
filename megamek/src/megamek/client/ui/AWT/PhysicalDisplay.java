/**
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
import megamek.common.actions.*;

public class PhysicalDisplay 
    extends StatusBarPhaseDisplay
    implements BoardListener, GameListener, ActionListener,
    KeyListener, BoardViewListener
{
	public static final String PHYSICAL_PUNCH = "punch";
	public static final String PHYSICAL_KICK = "kick";
	public static final String PHYSICAL_CLUB = "club";
	public static final String PHYSICAL_BRUSH_OFF = "brushOff";
	public static final String PHYSICAL_THRASH = "thrash";
	public static final String PHYSICAL_DODGE = "dodge";
	public static final String PHYSICAL_PUSH = "push";
	public static final String PHYSICAL_NEXT = "next";

    private static final int    NUM_BUTTON_LAYOUTS = 2;
    // parent game
    private Client          client;
        
    // buttons
    private Container         panButtons;
    
    private Button            butPunch;
    private Button            butKick;
    private Button            butPush;
    private Button            butClub;
    private Button            butBrush;
    private Button            butThrash;
    private Button            butDodge;
    
    private Button            butNext;
    private Button            butDone;
    private Button            butMore;
    
    private Button            butSpace;
    private Button            butSpace2;
    private Button            butSpace3;

    private int               buttonLayout;
        
    // let's keep track of what we're shooting and at what, too
    private int                cen;        // current entity number
    private Targetable         target;        // target 
      
    // stuff we want to do
    private Vector          attacks;  
    
    /**
     * Creates and lays out a new movement phase display 
     * for the specified client.
     */
    public PhysicalDisplay(Client client) {
        this.client = client;
        client.addGameListener(this);
        
        client.game.board.addBoardListener(this);
    
        attacks = new Vector();

        setupStatusBar("Waiting to begin Physical Attack phase...");
            
        butSpace = new Button(".");
        butSpace.setEnabled(false);

        butSpace2 = new Button(".");
        butSpace2.setEnabled(false);

        butSpace3 = new Button(".");
        butSpace3.setEnabled(false);

        butPunch = new Button("Punch");
        butPunch.addActionListener(this);
        butPunch.setEnabled(false);
        butPunch.setActionCommand(PHYSICAL_PUNCH);
        
        butKick = new Button("Kick");
        butKick.addActionListener(this);
        butKick.setEnabled(false);
        butKick.setActionCommand(PHYSICAL_KICK);
        
        butPush = new Button("Push");
        butPush.addActionListener(this);
        butPush.setEnabled(false);
        butPush.setActionCommand(PHYSICAL_PUSH);
        
        butClub = new Button("Club");
        butClub.addActionListener(this);
        butClub.setEnabled(false);
        butClub.setActionCommand(PHYSICAL_CLUB);

        butBrush = new Button("Brush Off");
        butBrush.addActionListener(this);
        butBrush.setEnabled(false);
        butBrush.setActionCommand(PHYSICAL_BRUSH_OFF);

        butThrash = new Button("Thrash");
        butThrash.addActionListener(this);
        butThrash.setEnabled(false);
        butThrash.setActionCommand(PHYSICAL_THRASH);

        butDodge = new Button("Dodge");
        butDodge.addActionListener(this);
        butDodge.setEnabled(false);
        butDodge.setActionCommand(PHYSICAL_DODGE);

        butDone = new Button("Done");
        butDone.addActionListener(this);
        butDone.setEnabled(false);
        
        butNext = new Button(" Next Unit ");
        butNext.addActionListener(this);
        butNext.setEnabled(false);
        butNext.setActionCommand(PHYSICAL_NEXT);
        
        butMore = new Button("More...");
        butMore.addActionListener(this);
        butMore.setEnabled(false);
        
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

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 0.0;    c.weighty = 0.0;
        c.ipady = 20;
        addBag(panButtons, gridbag, c);
        
        addKeyListener(this);
        
    }
    
    private void addBag(Component comp, GridBagLayout gridbag, GridBagConstraints c) {
        gridbag.setConstraints(comp, c);
        add(comp);
        comp.addKeyListener(this);
    }

    private void setupButtonPanel() {
        panButtons.removeAll();
        panButtons.setLayout(new GridLayout(1, 6));
        
        switch (buttonLayout) {
        case 0 :
            panButtons.add(butNext);
            panButtons.add(butPunch);
            panButtons.add(butKick);
            panButtons.add(butPush);
            panButtons.add(butClub);
            panButtons.add(butSpace);
            panButtons.add(butMore);
            panButtons.add(butDone);
            break;
        case 1 :
            panButtons.add(butBrush);
            panButtons.add(butThrash);
            panButtons.add(butDodge);
            panButtons.add(butSpace);
            panButtons.add(butSpace2);
            panButtons.add(butSpace3);
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
        if (client.game.getEntity(en) == null) {
            System.err.println("PhysicalDisplay: tried to select non-existant entity: " + en);
            return;
        }
        if (ce() != null) {
        	ce().setSelected(false);
        }
        this.cen = en;
        ce().setSelected(true);
        
        Entity entity = ce();
        
        target(null);
        client.game.board.highlight(ce().getPosition());
        client.game.board.select(null);
        client.game.board.cursor(null);

        client.mechD.displayEntity(entity);
        client.mechD.showPanel("movement");

        client.bv.centerOnHex(entity.getPosition());

        // Update the menu bar.
        client.getMenuBar().setEntity( ce() );

        // does it have a club?
        Mounted club = Compute.clubMechHas(entity);
        if (club == null || club.getName().endsWith("Club")) {
            butClub.setLabel("Club");
        } else {
            butClub.setLabel(club.getName());
        }

        if ( (entity instanceof Mech) && !entity.isProne() && entity.getCrew().getOptions().booleanOption("dodge_maneuver") ) {
          setDodgeEnabled(true);
        }
    }
    
    /**
     * Does turn start stuff
     */
    private void beginMyTurn() {
        target(null);
        setNextEnabled(true);
        butDone.setEnabled(true);
        butMore.setEnabled(true);
        client.setDisplayVisible(true);
        client.game.board.select(null);
        client.game.board.highlight(null);
        selectEntity(client.getFirstEntityNum());
    }
    
    /**
     * Does end turn stuff.
     */
    private void endMyTurn() {
        // end my turn, then.
        cen = Entity.NONE;
        target(null);
        client.game.board.select(null);
        client.game.board.highlight(null);
        client.game.board.cursor(null);
        client.setDisplayVisible(false);
        client.bv.clearMovementData();
        disableButtons();
    }
    
    /**
     * Disables all buttons in the interface
     */
    private void disableButtons() {
        setKickEnabled(false);
        setPunchEnabled(false);
        setPushEnabled(false);
        setClubEnabled(false);
        setBrushOffEnabled(false);
        setThrashEnabled(false);
        setDodgeEnabled(false);
        butDone.setEnabled(false);
        setNextEnabled(false);
    }
    
    /**
     * Called when the current entity is done with physical attacks.
     */
    private void ready() {
        disableButtons();
        client.sendAttackData(cen, attacks);
        attacks.removeAllElements();
    }
    
    
    /**
     * Clears all current actions
     */
    private void clearattacks() {
        if (attacks.size() > 0) {
          attacks.removeAllElements();
        }
        client.mechD.wPan.displayMech(ce());
        updateTarget();

        Entity entity = client.game.getEntity(cen);
        entity.dodging = true;
    }
    
    /**
     * Punch the target!
     */
    private void punch() {
  final ToHitData leftArm = Compute.toHitPunch(client.game, cen, target, PunchAttackAction.LEFT);
  final ToHitData rightArm = Compute.toHitPunch(client.game, cen, target, PunchAttackAction.RIGHT);

  if (client.doYesNoDialog( "Punch " + target.getDisplayName() + "?",
    "To Hit [RA]: " + rightArm.getValueAsString() + " (" + Compute.oddsAbove(rightArm.getValue()) + "%)   (" + rightArm.getDesc() + ")"
    + "\nDamage [RA]: "+Compute.getPunchDamageFor(ce(),PunchAttackAction.RIGHT)+rightArm.getTableDesc()
    + "\n   and/or"
    +"\nTo Hit [LA]: " + leftArm.getValueAsString() + " (" + Compute.oddsAbove(leftArm.getValue()) + "%)   (" + leftArm.getDesc() + ")"
    + "\nDamage [LA]: "+Compute.getPunchDamageFor(ce(),PunchAttackAction.LEFT)+leftArm.getTableDesc()
  ) ) {
          disableButtons();
          if (leftArm.getValue() != ToHitData.IMPOSSIBLE 
              && rightArm.getValue() != ToHitData.IMPOSSIBLE) {
              attacks.addElement(new PunchAttackAction(cen, target.getTargetType(), target.getTargetId(), PunchAttackAction.BOTH));
          } else if (leftArm.getValue() < rightArm.getValue()) {
              attacks.addElement(new PunchAttackAction(cen, target.getTargetType(), target.getTargetId(), PunchAttackAction.LEFT));
          } else {
              attacks.addElement(new PunchAttackAction(cen, target.getTargetType(), target.getTargetId(), PunchAttackAction.RIGHT));
          }

          ready();
  };
    }
    
    /**
     * Kick the target!
     */
    private void kick() {
  ToHitData leftLeg = Compute.toHitKick(client.game, cen, target, KickAttackAction.LEFT);
  ToHitData rightLeg = Compute.toHitKick(client.game, cen, target, KickAttackAction.RIGHT);
  ToHitData attackLeg;
  int attackSide;

  if (leftLeg.getValue() < rightLeg.getValue()) {
    attackLeg = leftLeg;
    attackSide = KickAttackAction.LEFT;
  } else {
    attackLeg = rightLeg;
    attackSide = KickAttackAction.RIGHT;
  };

  if (client.doYesNoDialog( "Kick " + target.getDisplayName() + "?",
    "To Hit: " + attackLeg.getValueAsString() + " (" + Compute.oddsAbove(attackLeg.getValue()) + "%)   (" + attackLeg.getDesc() + ")"
    + "\nDamage: "+Compute.getKickDamageFor(ce(),attackSide)+attackLeg.getTableDesc()
  ) ) {
    disableButtons();
    attacks.addElement(new KickAttackAction(cen, target.getTargetType(), target.getTargetId(), attackSide));
    ready();
  };
    }
    
    /**
     * Push that target!
     */
    private void push() {
  ToHitData toHit = Compute.toHitPush(client.game, cen, target);
  if (client.doYesNoDialog( "Push " + target.getDisplayName() + "?",
    "To Hit: " + toHit.getValueAsString() + " (" + Compute.oddsAbove(toHit.getValue()) + "%)   (" + toHit.getDesc() + ")"
  ) ) {
    disableButtons();
    attacks.addElement(new PushAttackAction(cen, target.getTargetType(), target.getTargetId(), target.getPosition()));
    ready();
  };
    }
    
    /**
     * Club that target!
     */
    private void club() {
  Mounted club = Compute.clubMechHas(ce());
  ToHitData toHit = Compute.toHitClub(client.game, cen, target, club);
  if (client.doYesNoDialog( "Club " + target.getDisplayName() + "?",
    "To Hit: " + toHit.getValueAsString() + " (" + Compute.oddsAbove(toHit.getValue()) + "%)   (" + toHit.getDesc() + ")"
    + "\nDamage: "+Compute.getClubDamageFor(ce(),club)+toHit.getTableDesc()
  ) ) {
    disableButtons();
    attacks.addElement(new ClubAttackAction(cen, target.getTargetType(), target.getTargetId(), club));
    ready();
  };
    }

    /**
     * Sweep off the target with the arms that the player selects.
     */
    private void brush() {
        ToHitData toHitLeft = Compute.toHitBrushOff
            ( client.game, cen, target, BrushOffAttackAction.LEFT );
        ToHitData toHitRight = Compute.toHitBrushOff
            ( client.game, cen, target, BrushOffAttackAction.RIGHT );
        boolean canHitLeft  = (ToHitData.IMPOSSIBLE != toHitLeft.getValue());
        boolean canHitRight = (ToHitData.IMPOSSIBLE != toHitRight.getValue());
        int     damageLeft = 0;
        int     damageRight = 0;
        String  title = null;
        StringBuffer    warn  = null;
        StringBuffer    left  = null;
        StringBuffer    right = null;
        StringBuffer    both  = null;
        String[]        choices = null;
        SingleChoiceDialog dlg = null;

        // If the entity can't brush off, display an error message and abort.
        if ( !canHitLeft && !canHitRight ) {
            client.doAlertDialog( "Code shouldn't get here!",
                                  "You've selected a 'brush off' attack that automatically fails!  Try again." );
            return;
        }

        // If we can hit with both arms, the player will have to make a choice.
        // Otherwise, the player is just confirming the arm in the attack.
        if ( canHitLeft && canHitRight ) {
            both = new StringBuffer( "Both Arms" );
            warn = new StringBuffer( "Which arm(s) do you want to use to" );
            title = "Choose Brush Off Attacks";
        } else {
            warn = new StringBuffer( "Confirm you want to use this arm to" );
            title = "Confrim Brush Off Attack";
        }

        // Build the rest of the warning string.
        warn.append( "\nbrush off the swarming infantry?" )
            .append( "\nWARNING: any arm that misses the infantry" )
            .append( "\n\thits the Mek on the Punch table!" );

        // If we can hit with the left arm, get
        // the damage and construct the string.
        if ( canHitLeft ) {
            damageLeft = Compute.getBrushOffDamageFor
                ( ce(), BrushOffAttackAction.LEFT );
            left = new StringBuffer( "Left Arm to-hit: " );
            left.append( toHitLeft.getValueAsString() )
                .append( " (" )
                .append( Compute.oddsAbove(toHitLeft.getValue()) )
                .append( "%) Damage: " )
                .append( damageLeft );
        }

        // If we can hit with the right arm, get
        // the damage and construct the string.
        if ( canHitRight ) {
            damageRight = Compute.getBrushOffDamageFor
                ( ce(), BrushOffAttackAction.RIGHT );
            right = new StringBuffer( "Right Arm to-hit: " );
            right.append( toHitRight.getValueAsString() )
                .append( " (" )
                .append( Compute.oddsAbove(toHitRight.getValue()) )
                .append( "%) Damage: " )
                .append( damageRight );
        }

        // Allow the player to cancel or choose which arm(s) to use.
        if ( canHitLeft && canHitRight ) {
            choices = new String[3];
            choices[0] = left.toString();
            choices[1] = right.toString();
            choices[2] = both.toString();
            dlg = new SingleChoiceDialog
                ( client.frame, title, warn.toString(), choices );
            dlg.show();
            if ( dlg.getAnswer() ) {
                disableButtons();
                switch ( dlg.getChoice() ) {
                case 0:
                    attacks.addElement( new BrushOffAttackAction
                        (cen, target.getTargetType(), target.getTargetId(), BrushOffAttackAction.LEFT) );
                    break;
                case 1:
                    attacks.addElement( new BrushOffAttackAction
                        (cen, target.getTargetType(), target.getTargetId(), BrushOffAttackAction.RIGHT) );
                    break;
                case 2: 
                    attacks.addElement( new BrushOffAttackAction
                        (cen, target.getTargetType(), target.getTargetId(), BrushOffAttackAction.BOTH) );
                    break;
                }
                ready();

            } // End not-cancel

        } // End choose-attack(s)

        // If only the left arm is available, confirm that choice.
        else if ( canHitLeft ) {
            choices = new String[1];
            choices[0] = left.toString();
            dlg = new SingleChoiceDialog
                ( client.frame, title, warn.toString(), choices );
            dlg.show();
            if ( dlg.getAnswer() ) {
                disableButtons();
                attacks.addElement( new BrushOffAttackAction
                    (cen, target.getTargetType(), target.getTargetId(), BrushOffAttackAction.LEFT) );
                ready();

            } // End not-cancel

        } // End confirm-left

        // If only the right arm is available, confirm that choice.
        else if ( canHitRight ) {
            choices = new String[1];
            choices[0] = right.toString();
            dlg = new SingleChoiceDialog
                ( client.frame, title, warn.toString(), choices );
            dlg.show();
            if ( dlg.getAnswer() ) {
                disableButtons();
                attacks.addElement( new BrushOffAttackAction
                    (cen, target.getTargetType(), target.getTargetId(), BrushOffAttackAction.RIGHT) );
                ready();

            } // End not-cancel

        } // End confirm-right

    } // End private void brush()

    /**
     * Thrash at the target, unless the player cancels the action.
     */
    private void thrash() {
        ThrashAttackAction act = new ThrashAttackAction( cen, target.getTargetType(), target.getTargetId() );
        ToHitData toHit = Compute.toHitThrash( client.game, act );
        StringBuffer at = new StringBuffer();
        StringBuffer damage = new StringBuffer();

        // Build the dialog's strings.
        at.append( "Thrash at " )
            .append( target.getDisplayName() )
            .append( " (warning: this causes a Piloting roll to avoid fall damage)?" );
        damage.append( "To Hit: " )
            .append( toHit.getValueAsString() )
            .append( " (" )
            .append( Compute.oddsAbove(toHit.getValue()) )
            .append( "%)   (" )
            .append( toHit.getDesc() )
            .append( ")" )
            .append( "\nDamage: " )
            .append( Compute.getThrashDamageFor(ce()) )
            .append( toHit.getTableDesc() );

        // Give the user to cancel the attack.
        if ( client.doYesNoDialog(at.toString(), damage.toString()) ) {
            disableButtons();
            attacks.addElement( act );
            ready();
        }
    }

    /**
     * Dodge like that guy in that movie that I won't name for copywrite reasons!
     */
    private void dodge() {
      if (client.doYesNoDialog("Dodge?", "Enable dodge? You will not be able to do other physical attacks this round.") ) {
        disableButtons();
        
        Entity entity = client.game.getEntity(cen);
        entity.dodging = true;
        
        DodgeAction act = new DodgeAction( cen );
        attacks.addElement( act );
        
        ready();
      };
    }

    /**
     * Targets something
     */
    void target(Targetable t) {
        this.target = t;
        updateTarget();
    }

    /**
     * Targets an entity
     */
    private void updateTarget() {
        // dis/enable punch button
        if (cen != Entity.NONE && target != null) {
            // punch?
            final ToHitData leftArm = Compute.toHitPunch(client.game, cen, target, PunchAttackAction.LEFT);
            final ToHitData rightArm = Compute.toHitPunch(client.game, cen, target, PunchAttackAction.RIGHT);
            boolean canPunch = leftArm.getValue() != ToHitData.IMPOSSIBLE 
                              || rightArm.getValue() != ToHitData.IMPOSSIBLE;
            setPunchEnabled(canPunch);
            
            // kick?
            ToHitData leftLeg = Compute.toHitKick(client.game, cen, target, KickAttackAction.LEFT);
            ToHitData rightLeg = Compute.toHitKick(client.game, cen, target, KickAttackAction.RIGHT);
            boolean canKick = leftLeg.getValue() != ToHitData.IMPOSSIBLE 
                              || rightLeg.getValue() != ToHitData.IMPOSSIBLE;
            setKickEnabled(canKick);
            
            // how about push?
            ToHitData push = Compute.toHitPush(client.game, cen, target);
            setPushEnabled(push.getValue() != ToHitData.IMPOSSIBLE);
            
            // clubbing?
            Mounted club = Compute.clubMechHas(ce());
            if (club != null) {
                ToHitData clubToHit = Compute.toHitClub(client.game, cen, target, club);
                setClubEnabled(clubToHit.getValue() != ToHitData.IMPOSSIBLE);
            } else {
                setClubEnabled(false);
            }

            // Brush off swarming infantry?
            ToHitData brushRight = Compute.toHitBrushOff
                ( client.game, cen, target, BrushOffAttackAction.RIGHT );
            ToHitData brushLeft = Compute.toHitBrushOff
                ( client.game, cen, target, BrushOffAttackAction.LEFT );
            boolean canBrush = (brushRight.getValue() != ToHitData.IMPOSSIBLE||
                                brushLeft.getValue() != ToHitData.IMPOSSIBLE);
            setBrushOffEnabled( canBrush );

            // Thrash at infantry?
            ToHitData thrash = Compute.toHitThrash( client.game, cen, target );
            setThrashEnabled( thrash.getValue() != ToHitData.IMPOSSIBLE );
        } else {
            setPunchEnabled(false);
            setPushEnabled(false);
            setKickEnabled(false);
            setClubEnabled(false);
            setBrushOffEnabled(false);
            setThrashEnabled(false);
        }
    }
    
    /**
     * Returns the current entity.
     */
    private Entity ce() {
        return client.game.getEntity(cen);
    }

    //
    // BoardListener
    //
    public void boardHexMoused(BoardEvent b) {
        // control pressed means a line of sight check.
        if ((b.getModifiers() & InputEvent.CTRL_MASK) != 0) {
            return;
        }
        if (client.isMyTurn()
            && (b.getModifiers() & MouseEvent.BUTTON1_MASK) != 0) {
            if (b.getType() == BoardEvent.BOARD_HEX_DRAGGED) {
                if (!b.getCoords().equals(client.game.board.lastCursor)) {
                    client.game.board.cursor(b.getCoords());
                }
            } else if (b.getType() == BoardEvent.BOARD_HEX_CLICKED) {
                client.game.board.select(b.getCoords());
            }
        }
    }
    public void boardHexSelected(BoardEvent b) {
        if (client.isMyTurn() && b.getCoords() != null && ce() != null) {
            final Targetable target = this.chooseTarget( b.getCoords() );
            if ( target != null ) {
                target( target );
            } else {
                target(null);
            }
        }
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
                names[loop] = ( (Targetable)targets.elementAt(loop) )
                    .getDisplayName();
            }
            SingleChoiceDialog choiceDialog =
                new SingleChoiceDialog( client.frame,
                                        "Choose Target",
                                        question.toString(),
                                        names );
            choiceDialog.show();
            if ( choiceDialog.getAnswer() == true ) {
                choice = (Targetable) targets.elementAt
                    ( choiceDialog.getChoice() );
            }
        } // End have-choices

        // Return the chosen unit.
        return choice;

    } // End private Targetable chooseTarget( Coords )

    //
    // GameListener
    //
    public void gameTurnChange(GameEvent ev) {
        if (client.game.getPhase() == Game.PHASE_PHYSICAL) {
            endMyTurn();

            if (client.isMyTurn()) {
                beginMyTurn();
                setStatusBarText("It's your turn to declare physical attacks.");
            } else {
                setStatusBarText("It's " + ev.getPlayer().getName() + "'s turn to declare physical attacks.");
            }
        } else {
            System.err.println("PhysicalDisplay: got turnchange event when it's not the physical attacks phase");
        }
    }
    public void gamePhaseChange(GameEvent ev) {
        if (client.isMyTurn() && client.game.getPhase() != Game.PHASE_PHYSICAL) {
            endMyTurn();
        }
        // if we're ending the firing phase, unregister stuff.
        if (client.game.getPhase() !=  Game.PHASE_PHYSICAL) {
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
        if ( statusBarActionPerformed(ev, client) )
          return;
          
        if (!client.isMyTurn()) {
            // odd...
            return;
        }
        if (ev.getSource() == butDone) {
            ready();
        } else if (ev.getActionCommand().equals(PHYSICAL_PUNCH)) {
            punch();
        } else if (ev.getActionCommand().equals(PHYSICAL_KICK)) {
            kick();
        } else if (ev.getActionCommand().equals(PHYSICAL_PUSH)) {
            push();
        } else if (ev.getActionCommand().equals(PHYSICAL_CLUB)) {
            club();
        } else if (ev.getActionCommand().equals(PHYSICAL_BRUSH_OFF)) {
            brush();
        } else if (ev.getActionCommand().equals(PHYSICAL_THRASH)) {
            thrash();
        } else if (ev.getActionCommand().equals(PHYSICAL_DODGE)) {
            dodge();
        } else if (ev.getActionCommand().equals(PHYSICAL_NEXT)) {
            selectEntity(client.getNextEntityNum(cen));
        } else if (ev.getSource() == butMore) {
            buttonLayout++;
            
            if ( buttonLayout >= NUM_BUTTON_LAYOUTS )
              buttonLayout = 0;

            setupButtonPanel();
        }
    }
    

    //
    // KeyListener
    //
    public void keyPressed(KeyEvent ev) {
        if (ev.getKeyCode() == KeyEvent.VK_ESCAPE) {
            clearattacks();
        } else if (ev.getKeyCode() == KeyEvent.VK_ENTER && ev.isControlDown()) {
            if (client.isMyTurn()) {
                //
            }
        }
    }
    public void keyReleased(KeyEvent ev) {
        ;
    }
    public void keyTyped(KeyEvent ev) {
        ;
    }

	//
	// BoardViewListener
	//
    public void finishedMovingUnits(BoardViewEvent b) {
    }
    
    public void selectUnit(BoardViewEvent b) {
    	Entity e = client.game.getEntity(b.getEntityId());
    	if (client.isMyTurn()) {
    		if (!e.isSelectableThisTurn(client.game)) {
            	client.setDisplayVisible(true);
            	client.mechD.displayEntity(e);
            	client.bv.centerOnHex(e.getPosition());
            } else {
	            selectEntity(e.getId());
    		}
    	} else {
        	client.setDisplayVisible(true);
        	client.mechD.displayEntity(e);
    		if (e.isDeployed()) {
            	client.bv.centerOnHex(e.getPosition());
    		}
    	}
    }
	
	public void setThrashEnabled(boolean enabled) {
		butThrash.setEnabled(enabled);
        client.getMenuBar().setPhysicalThrashEnabled(enabled);
	}
	public void setPunchEnabled(boolean enabled) {
		butPunch.setEnabled(enabled);
        client.getMenuBar().setPhysicalPunchEnabled(enabled);
	}
	public void setKickEnabled(boolean enabled) {
		butKick.setEnabled(enabled);
        client.getMenuBar().setPhysicalKickEnabled(enabled);
	}
	public void setPushEnabled(boolean enabled) {
		butPush.setEnabled(enabled);
        client.getMenuBar().setPhysicalPushEnabled(enabled);
	}
	public void setClubEnabled(boolean enabled) {
		butClub.setEnabled(enabled);
        client.getMenuBar().setPhysicalClubEnabled(enabled);
	}
	public void setBrushOffEnabled(boolean enabled) {
		butBrush.setEnabled(enabled);
        client.getMenuBar().setPhysicalBrushOffEnabled(enabled);
	}
	public void setDodgeEnabled(boolean enabled) {
		butDodge.setEnabled(enabled);
        client.getMenuBar().setPhysicalDodgeEnabled(enabled);
	}
	public void setNextEnabled(boolean enabled) {
		butNext.setEnabled(enabled);
        client.getMenuBar().setPhysicalNextEnabled(enabled);
	}

}
