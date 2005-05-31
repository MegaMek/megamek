/*
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

package megamek.client;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import megamek.client.event.BoardViewEvent;
import megamek.client.event.BoardViewListener;
import megamek.common.*;
import megamek.common.actions.*;
import megamek.common.event.GameListener;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.event.GameTurnChangeEvent;
import megamek.common.util.Distractable;
import megamek.common.util.DistractableAdapter;

public class PhysicalDisplay 
    extends StatusBarPhaseDisplay
    implements GameListener, ActionListener, DoneButtoned,
               KeyListener, BoardViewListener, Distractable
{

    // Distraction implementation.
    private DistractableAdapter distracted = new DistractableAdapter();

    public static final String PHYSICAL_PUNCH = "punch"; //$NON-NLS-1$
    public static final String PHYSICAL_KICK = "kick"; //$NON-NLS-1$
    public static final String PHYSICAL_CLUB = "club"; //$NON-NLS-1$
    public static final String PHYSICAL_BRUSH_OFF = "brushOff"; //$NON-NLS-1$
    public static final String PHYSICAL_THRASH = "thrash"; //$NON-NLS-1$
    public static final String PHYSICAL_DODGE = "dodge"; //$NON-NLS-1$
    public static final String PHYSICAL_PUSH = "push"; //$NON-NLS-1$
    public static final String PHYSICAL_NEXT = "next"; //$NON-NLS-1$
    public static final String PHYSICAL_PROTO = "protoPhysical"; //$NON-NLS-1$

    private static final int    NUM_BUTTON_LAYOUTS = 2;
    // parent game
    private ClientGUI          clientgui;
    private Client            client;
        
    // buttons
    private Container         panButtons;
    
    private Button            butPunch;
    private Button            butKick;
    private Button            butPush;
    private Button            butClub;
    private Button            butBrush;
    private Button            butThrash;
    private Button            butDodge;
    private Button            butProto;
    
    private Button            butNext;
    private Button            butDone;
    private Button            butMore;
    
    private Button            butSpace;
    private Button            butSpace2;

    private int               buttonLayout;
        
    // let's keep track of what we're shooting and at what, too
    private int                cen = Entity.NONE;        // current entity number
    private Targetable         target;        // target 
      
    // stuff we want to do
    private Vector          attacks;  
    
    /**
     * Creates and lays out a new movement phase display 
     * for the specified client.
     */
    public PhysicalDisplay(ClientGUI clientgui) {
        this.clientgui = clientgui;
        this.client = clientgui.getClient();
        client.game.addGameListener(this);
        
        clientgui.getBoardView().addBoardViewListener(this);
    
        attacks = new Vector();

        setupStatusBar(Messages.getString("PhysicalDisplay.waitingForPhysicalAttackPhase")); //$NON-NLS-1$
            
        butSpace = new Button("."); //$NON-NLS-1$
        butSpace.setEnabled(false);

        butSpace2 = new Button("."); //$NON-NLS-1$
        butSpace2.setEnabled(false);

        butPunch = new Button(Messages.getString("PhysicalDisplay.Punch")); //$NON-NLS-1$
        butPunch.addActionListener(this);
        butPunch.setEnabled(false);
        butPunch.setActionCommand(PHYSICAL_PUNCH);
        
        butKick = new Button(Messages.getString("PhysicalDisplay.Kick")); //$NON-NLS-1$
        butKick.addActionListener(this);
        butKick.setEnabled(false);
        butKick.setActionCommand(PHYSICAL_KICK);
        
        butPush = new Button(Messages.getString("PhysicalDisplay.Push")); //$NON-NLS-1$
        butPush.addActionListener(this);
        butPush.setEnabled(false);
        butPush.setActionCommand(PHYSICAL_PUSH);
        
        butClub = new Button(Messages.getString("PhysicalDisplay.Clusb")); //$NON-NLS-1$
        butClub.addActionListener(this);
        butClub.setEnabled(false);
        butClub.setActionCommand(PHYSICAL_CLUB);

        butBrush = new Button(Messages.getString("PhysicalDisplay.BrushOff")); //$NON-NLS-1$
        butBrush.addActionListener(this);
        butBrush.setEnabled(false);
        butBrush.setActionCommand(PHYSICAL_BRUSH_OFF);

        butThrash = new Button(Messages.getString("PhysicalDisplay.Trash")); //$NON-NLS-1$
        butThrash.addActionListener(this);
        butThrash.setEnabled(false);
        butThrash.setActionCommand(PHYSICAL_THRASH);

        butDodge = new Button(Messages.getString("PhysicalDisplay.Dodge")); //$NON-NLS-1$
        butDodge.addActionListener(this);
        butDodge.setEnabled(false);
        butDodge.setActionCommand(PHYSICAL_DODGE);
        
        butProto = new Button(Messages.getString("PhysicalDisplay.ProtoPhysical")); //$NON-NLS-1$
        butProto.addActionListener(this);
        butProto.setEnabled(false);
        butProto.setActionCommand(PHYSICAL_PROTO);

        butDone = new Button(Messages.getString("PhysicalDisplay.Done")); //$NON-NLS-1$
        butDone.addActionListener(this);
        butDone.setEnabled(false);
        
        butNext = new Button(Messages.getString("PhysicalDisplay.NextUnit")); //$NON-NLS-1$
        butNext.addActionListener(this);
        butNext.setEnabled(false);
        butNext.setActionCommand(PHYSICAL_NEXT);
        
        butMore = new Button(Messages.getString("PhysicalDisplay.More")); //$NON-NLS-1$
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

    private void setupButtonPanel() {
        panButtons.removeAll();
        panButtons.setLayout(new GridLayout(0, 8));
        
        switch (buttonLayout) {
        case 0 :
            panButtons.add(butNext);
            panButtons.add(butPunch);
            panButtons.add(butKick);
            panButtons.add(butPush);
            panButtons.add(butClub);
            panButtons.add(butSpace);
            panButtons.add(butMore);
//             panButtons.add(butDone);
            break;
        case 1 :
            panButtons.add(butBrush);
            panButtons.add(butThrash);
            panButtons.add(butDodge);
            panButtons.add(butProto);
            panButtons.add(butSpace);
            panButtons.add(butSpace2);
            panButtons.add(butMore);
//             panButtons.add(butDone);
            break;
        }
        
        validate();
    }
    
    /**
     * Selects an entity, by number, for movement.
     */
    public void selectEntity(int en) {
        if (client.game.getEntity(en) == null) {
            System.err.println("PhysicalDisplay: tried to select non-existant entity: " + en); //$NON-NLS-1$
            return;
        }

        this.cen = en;
        clientgui.setSelectedEntityNum(en);
        
        Entity entity = ce();
        
        target(null);
        clientgui.getBoardView().highlight(ce().getPosition());
        clientgui.getBoardView().select(null);
        clientgui.getBoardView().cursor(null);

        clientgui.mechD.displayEntity(entity);
        clientgui.mechD.showPanel("movement"); //$NON-NLS-1$

        clientgui.bv.centerOnHex(entity.getPosition());

        // Update the menu bar.
        clientgui.getMenuBar().setEntity( ce() );

        // does it have a club?
        Mounted club = Compute.clubMechHas(entity);
        if (club == null || club.getName().endsWith("Club")) { //$NON-NLS-1$
            butClub.setLabel(Messages.getString("PhysicalDisplay.Club")); //$NON-NLS-1$
        } else {
            butClub.setLabel(club.getName());
        }

        if ( (entity instanceof Mech) && !entity.isProne() && entity.getCrew().getOptions().booleanOption("dodge_maneuver") ) { //$NON-NLS-1$
          setDodgeEnabled(true);
        }
    }
    
    /**
     * Does turn start stuff
     */
    private void beginMyTurn() {
        target(null);
        selectEntity(client.getFirstEntityNum());
        setNextEnabled(true);
        butDone.setEnabled(true);
        butMore.setEnabled(true);
        clientgui.setDisplayVisible(true);
        clientgui.getBoardView().select(null);
    }
    
    /**
     * Does end turn stuff.
     */
    private void endMyTurn() {
        // end my turn, then.
        Entity next = client.game.getNextEntity( client.game.getTurnIndex() );
        if ( IGame.PHASE_PHYSICAL == client.game.getPhase()
             && null != next
             && null != ce()
             && next.getOwnerId() != ce().getOwnerId() ) {
            clientgui.setDisplayVisible(false);
        };
        cen = Entity.NONE;
        target(null);
        clientgui.getBoardView().select(null);
        clientgui.getBoardView().highlight(null);
        clientgui.getBoardView().cursor(null);
        clientgui.bv.clearMovementData();
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
        setProtoEnabled(false);
        butDone.setEnabled(false);
        setNextEnabled(false);
    }
    
    /**
     * Called when the current entity is done with physical attacks.
     */
    private void ready() {
        if (attacks.isEmpty() && GUIPreferences.getInstance().getNagForNoAction()) {
            // comfirm this action
            ConfirmDialog response = clientgui.doYesNoBotherDialog(Messages.getString("PhysicalDisplay.DontPhysicalAttackDialog.title") //$NON-NLS-1$
                    , Messages.getString("PhysicalDisplay.DontPhysicalAttackDialog.message")); //$NON-NLS-1$
            if ( !response.getShowAgain() ) {
                GUIPreferences.getInstance().setNagForNoAction(false);
            }
            if ( !response.getAnswer() ) {
                return;
            }
        }

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
        clientgui.mechD.wPan.displayMech(ce());
        updateTarget();

        Entity entity = client.game.getEntity(cen);
        entity.dodging = true;
    }
    
    /**
     * Punch the target!
     */
    private void punch() {
        final ToHitData leftArm = PunchAttackAction.toHit(client.game, cen, target, PunchAttackAction.LEFT);
        final ToHitData rightArm = PunchAttackAction.toHit(client.game, cen, target, PunchAttackAction.RIGHT);
        String title = Messages.getString("PhysicalDisplay.PunchDialog.title", new Object[]{target.getDisplayName()}); //$NON-NLS-1$
        String message = Messages.getString("PhysicalDisplay.PunchDialog.message",new Object[]{ //$NON-NLS-1$
                rightArm.getValueAsString(), new Double(Compute.oddsAbove(rightArm.getValue())),rightArm.getDesc(), new Integer(PunchAttackAction.getDamageFor(ce(),PunchAttackAction.RIGHT)), rightArm.getTableDesc(),
                leftArm.getValueAsString(),  new Double(Compute.oddsAbove(leftArm.getValue())), leftArm.getDesc(), new Integer(PunchAttackAction.getDamageFor(ce(),PunchAttackAction.LEFT)), leftArm.getTableDesc()}); 
        if (clientgui.doYesNoDialog(title, message)) {
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
        }
    }
    
    /**
     * Kick the target!
     */
    private void kick() {
        ToHitData leftLeg = KickAttackAction.toHit(client.game, cen, target, KickAttackAction.LEFT);
        ToHitData rightLeg = KickAttackAction.toHit(client.game, cen, target, KickAttackAction.RIGHT);
        ToHitData rightRearLeg = null;
        ToHitData leftRearLeg = null;
        if (client.game.getEntity(cen) instanceof QuadMech &&
            client.game.getOptions().booleanOption("maxtech_mulekicks")) {
            rightRearLeg = KickAttackAction.toHit(client.game, cen, target, KickAttackAction.RIGHTMULE);
            leftRearLeg = KickAttackAction.toHit(client.game, cen, target, KickAttackAction.LEFTMULE);
        }
        ToHitData attackLeg;
        int attackSide=KickAttackAction.LEFT;
        int value = leftLeg.getValue();
        attackLeg = leftLeg;
        
        if (value>rightLeg.getValue()) {
            value = rightLeg.getValue();
            attackSide = KickAttackAction.RIGHT;
            attackLeg = rightLeg;
        }
        if (client.game.getEntity(cen) instanceof QuadMech &&
            client.game.getOptions().booleanOption("maxtech_mulekicks")) {
            if (value>rightRearLeg.getValue()) {
                value = rightRearLeg.getValue();
                attackSide = KickAttackAction.RIGHTMULE;
                attackLeg = rightRearLeg;
            }
            if (value>leftRearLeg.getValue()) {
                value = leftRearLeg.getValue();
                attackSide = KickAttackAction.LEFTMULE;
                attackLeg = leftRearLeg;
            }
        }
        String title = Messages.getString("PhysicalDisplay.KickDialog.title", new Object[]{target.getDisplayName()}); //$NON-NLS-1$
        String message = Messages.getString("PhysicalDisplay.KickDialog.message", new Object[]{ //$NON-NLS-1$
                attackLeg.getValueAsString(), new Double(Compute.oddsAbove(attackLeg.getValue())), attackLeg.getDesc()
                ,KickAttackAction.getDamageFor(ce(),attackSide)+attackLeg.getTableDesc()});
        if (clientgui.doYesNoDialog(title, message)){
            disableButtons();
            attacks.addElement(new KickAttackAction(cen, target.getTargetType(), target.getTargetId(), attackSide));
            ready();
        }
    }
    
    /**
     * Push that target!
     */
    private void push() {
        ToHitData toHit = PushAttackAction.toHit(client.game, cen, target);
        String title = Messages.getString("PhysicalDisplay.PushDialog.title", new Object[]{target.getDisplayName()}); //$NON-NLS-1$
        String message = Messages.getString("PhysicalDisplay.PushDialog.message", new Object[]{ //$NON-NLS-1$
                toHit.getValueAsString(), new Double(Compute.oddsAbove(toHit.getValue())),toHit.getDesc()});
        if (clientgui.doYesNoDialog( title, message)){
            disableButtons();
            attacks.addElement(new PushAttackAction(cen, target.getTargetType(), target.getTargetId(), target.getPosition()));
            ready();
        }
    }
    
    /**
     * Club that target!
     */
    private void club() {
        Mounted club = Compute.clubMechHas(ce());
        ToHitData toHit = ClubAttackAction.toHit(client.game, cen, target, club);
        String title = Messages.getString("PhysicalDisplay.ClubDialog.title", new Object[]{target.getDisplayName()}); //$NON-NLS-1$
        String message = Messages.getString("PhysicalDisplay.ClubDialog.message", new Object[]{ //$NON-NLS-1$
                toHit.getValueAsString(), new Double(Compute.oddsAbove(toHit.getValue())), toHit.getDesc(), 
                ClubAttackAction.getDamageFor(ce(),club)+toHit.getTableDesc()});
        if (clientgui.doYesNoDialog(title,message)){
            disableButtons();
            attacks.addElement(new ClubAttackAction(cen, target.getTargetType(), target.getTargetId(), club));
            ready();
        }
    }

    /**
     * Make a protomech physical attack on the target.
     */    
    private void proto() {
        ToHitData proto = ProtomechPhysicalAttackAction.toHit(client.game, cen, target);
        String title = Messages.getString("PhysicalDisplay.ProtoMechAttackDialog.title", new Object[]{target.getDisplayName()}); //$NON-NLS-1$
        String message = Messages.getString("PhysicalDisplay.ProtoMechAttackDialog.message", new Object[]{ //$NON-NLS-1$
                proto.getValueAsString(), new Double(Compute.oddsAbove(proto.getValue())), proto.getDesc(), ProtomechPhysicalAttackAction.getDamageFor(ce())+proto.getTableDesc()});        
        if (clientgui.doYesNoDialog(title,message)){
            disableButtons();
            attacks.addElement(new ProtomechPhysicalAttackAction(cen, target.getTargetType(), target.getTargetId()));
            ready();
          }
    }
    
    /**
     * Sweep off the target with the arms that the player selects.
     */
    private void brush() {
        ToHitData toHitLeft = BrushOffAttackAction.toHit
            ( client.game, cen, target, BrushOffAttackAction.LEFT );
        ToHitData toHitRight = BrushOffAttackAction.toHit
            ( client.game, cen, target, BrushOffAttackAction.RIGHT );
        boolean canHitLeft  = (ToHitData.IMPOSSIBLE != toHitLeft.getValue());
        boolean canHitRight = (ToHitData.IMPOSSIBLE != toHitRight.getValue());
        int damageLeft = 0;
        int damageRight = 0;
        String  title = null;
        StringBuffer    warn  = null;
        String left  = null;
        String right = null;
        String both = null;
        String[] choices = null;
        SingleChoiceDialog dlg = null;

        // If the entity can't brush off, display an error message and abort.
        if ( !canHitLeft && !canHitRight ) {
            clientgui.doAlertDialog( Messages.getString("PhysicalDisplay.AlertDialog.title"), //$NON-NLS-1$
                                  Messages.getString("PhysicalDisplay.AlertDialog.message") ); //$NON-NLS-1$
            return;
        }

        // If we can hit with both arms, the player will have to make a choice.
        // Otherwise, the player is just confirming the arm in the attack.
        if ( canHitLeft && canHitRight ) {
            both = Messages.getString("PhysicalDisplay.bothArms"); //$NON-NLS-1$
            warn = new StringBuffer(Messages.getString("PhysicalDisplay.whichArm")); //$NON-NLS-1$
            title = Messages.getString("PhysicalDisplay.chooseBrushOff"); //$NON-NLS-1$
        } else {
            warn = new StringBuffer(Messages.getString("PhysicalDisplay.confirmArm")); //$NON-NLS-1$
            title = Messages.getString("PhysicalDisplay.confirmBrushOff"); //$NON-NLS-1$
        }

        // Build the rest of the warning string.
        // Use correct text when the target is an iNarc pod.
        if (Targetable.TYPE_INARC_POD == target.getTargetType()) {
            warn.append(Messages.getString("PhysicalDisplay.brushOff1")); //$NON-NLS-1$
        }
        else {
            warn.append(Messages.getString("PhysicalDisplay.brushOff2")); //$NON-NLS-1$
        }

        // If we can hit with the left arm, get
        // the damage and construct the string.
        if ( canHitLeft ) {
            damageLeft = BrushOffAttackAction.getDamageFor(ce(), BrushOffAttackAction.LEFT);
            left = Messages.getString("PhysicalDisplay.LAHit", new Object[]{ //$NON-NLS-1$
                    toHitLeft.getValueAsString(), new Double(Compute.oddsAbove(toHitLeft.getValue())), new Integer(damageLeft)});
        }

        // If we can hit with the right arm, get
        // the damage and construct the string.
        if ( canHitRight ) {
            damageRight = BrushOffAttackAction.getDamageFor(ce(), BrushOffAttackAction.RIGHT);
            right = Messages.getString("PhysicalDisplay.RAHit", new Object[]{ //$NON-NLS-1$
                    toHitRight.getValueAsString(), new Double(Compute.oddsAbove(toHitRight.getValue())), new Integer(damageRight)});
        }

        // Allow the player to cancel or choose which arm(s) to use.
        if ( canHitLeft && canHitRight ) {
            choices = new String[3];
            choices[0] = left.toString();
            choices[1] = right.toString();
            choices[2] = both.toString();
            dlg = new SingleChoiceDialog
                ( clientgui.frame, title, warn.toString(), choices );
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
                ( clientgui.frame, title, warn.toString(), choices );
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
                ( clientgui.frame, title, warn.toString(), choices );
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
        ToHitData toHit = act.toHit(client.game);

        String title = Messages.getString("PhysicalDisplay.TrashDialog.title", new Object[]{target.getDisplayName()}); //$NON-NLS-1$
        String message = Messages.getString("PhysicalDisplay.TrashDialog.message", new Object[]{//$NON-NLS-1$
            toHit.getValueAsString(), new Double(Compute.oddsAbove(toHit.getValue())), toHit.getDesc(), 
            ThrashAttackAction.getDamageFor(ce())+toHit.getTableDesc()});

        // Give the user to cancel the attack.
        if (clientgui.doYesNoDialog(title,message)) {
            disableButtons();
            attacks.addElement( act );
            ready();
        }
    }

    /**
     * Dodge like that guy in that movie that I won't name for copywrite reasons!
     */
    private void dodge() {
      if (clientgui.doYesNoDialog(Messages.getString("PhysicalDisplay.DodgeDialog.title"), Messages.getString("PhysicalDisplay.DodgeDialog.message")) ) { //$NON-NLS-1$ //$NON-NLS-2$
        disableButtons();
        
        Entity entity = client.game.getEntity(cen);
        entity.dodging = true;
        
        DodgeAction act = new DodgeAction( cen );
        attacks.addElement( act );
        
        ready();
      }
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
        // dis/enable physical attach buttons
        if (cen != Entity.NONE && target != null) {
            if (target.getTargetType() != Targetable.TYPE_INARC_POD) {
                // punch?
                final ToHitData leftArm = PunchAttackAction.toHit
                   (client.game, cen, target, PunchAttackAction.LEFT);
                final ToHitData rightArm = PunchAttackAction.toHit
                   (client.game, cen, target, PunchAttackAction.RIGHT);
                boolean canPunch = leftArm.getValue() != ToHitData.IMPOSSIBLE 
                               || rightArm.getValue() != ToHitData.IMPOSSIBLE;
                setPunchEnabled(canPunch);
                
                // kick?
                ToHitData leftLeg = KickAttackAction.toHit
                   (client.game, cen, target, KickAttackAction.LEFT);
                ToHitData rightLeg = KickAttackAction.toHit
                   (client.game, cen, target, KickAttackAction.RIGHT);
                boolean canKick = leftLeg.getValue() != ToHitData.IMPOSSIBLE 
                              || rightLeg.getValue() != ToHitData.IMPOSSIBLE;
                if (client.game.getOptions().booleanOption("maxtech_mulekicks")) {
                    ToHitData rightRearLeg = KickAttackAction.toHit
                       (client.game, cen, target, KickAttackAction.RIGHTMULE);
                    ToHitData leftRearLeg = KickAttackAction.toHit
                       (client.game, cen, target, KickAttackAction.LEFTMULE);
                    canKick |= (leftRearLeg.getValue() != ToHitData.IMPOSSIBLE)
                            || (rightRearLeg.getValue() != ToHitData.IMPOSSIBLE);
                }
                setKickEnabled(canKick);
                
                // how about push?
                ToHitData push = PushAttackAction.toHit
                   (client.game, cen, target);
                setPushEnabled(push.getValue() != ToHitData.IMPOSSIBLE);
                
                // clubbing?
                Mounted club = Compute.clubMechHas(ce());
                if (club != null) {
                    ToHitData clubToHit = ClubAttackAction.toHit
                       (client.game, cen, target, club);
                    setClubEnabled(clubToHit.getValue() != ToHitData.IMPOSSIBLE);
                } else {
                    setClubEnabled(false);
                }
                // Thrash at infantry?
                ToHitData thrash = new ThrashAttackAction(cen, target).toHit
                   (client.game);
                setThrashEnabled( thrash.getValue() != ToHitData.IMPOSSIBLE );
                
                // make a Protomech physical attack?
                ToHitData proto = ProtomechPhysicalAttackAction.toHit
                   ( client.game, cen, target);
                setProtoEnabled(proto.getValue() != ToHitData.IMPOSSIBLE);
            }
            // Brush off swarming infantry or iNarcPods?
            ToHitData brushRight = BrushOffAttackAction.toHit
                ( client.game, cen, target, BrushOffAttackAction.RIGHT );
            ToHitData brushLeft = BrushOffAttackAction.toHit
                ( client.game, cen, target, BrushOffAttackAction.LEFT );
            boolean canBrush = (brushRight.getValue() != ToHitData.IMPOSSIBLE||
                                brushLeft.getValue() != ToHitData.IMPOSSIBLE);
            setBrushOffEnabled( canBrush );
        } else {
            setPunchEnabled(false);
            setPushEnabled(false);
            setKickEnabled(false);
            setClubEnabled(false);
            setBrushOffEnabled(false);
            setThrashEnabled(false);
            setProtoEnabled(false);
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
    public void hexMoused(BoardViewEvent b) {

        // Are we ignoring events?
        if ( this.isIgnoringEvents() ) {
            return;
        }

        // control pressed means a line of sight check.
        if ((b.getModifiers() & InputEvent.CTRL_MASK) != 0) {
            return;
        }
        if (client.isMyTurn()
            && (b.getModifiers() & MouseEvent.BUTTON1_MASK) != 0) {
            if (b.getType() == BoardViewEvent.BOARD_HEX_DRAGGED) {
                if (!b.getCoords().equals(clientgui.getBoardView().getLastCursor())) {
                    clientgui.getBoardView().cursor(b.getCoords());
                }
            } else if (b.getType() == BoardViewEvent.BOARD_HEX_CLICKED) {
                clientgui.getBoardView().select(b.getCoords());
            }
        }
    }
    public void hexSelected(BoardViewEvent b) {

        // Are we ignoring events?
        if ( this.isIgnoringEvents() ) {
            return;
        }

        if (client.isMyTurn() && b.getCoords() != null && ce() != null) {
            final Targetable targ = this.chooseTarget( b.getCoords() );
            if ( targ != null ) {
                target( targ );
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
        Building bldg = client.game.getBoard().getBuildingAt( pos );
        if ( bldg != null ) {
            targets.addElement( new BuildingTarget(pos, client.game.getBoard(), false) );
        }

        // Is the attacker targeting its own hex?
        if (ce().getPosition().equals( pos )) {
            // Add any iNarc pods attached to the entity.
            Enumeration pods = ce().getINarcPodsAttached();
            while ( pods.hasMoreElements() ) {
                choice = (Targetable) pods.nextElement();
                targets.addElement( choice );
            }
        }

        // Do we have a single choice?
        if ( targets.size() == 1 ) {

            // Return  that choice.
            choice = (Targetable) targets.elementAt( 0 );

        }

        // If we have multiple choices, display a selection dialog.
        else if ( targets.size() > 1 ) {
            String[] names = new String[ targets.size() ];
            for ( int loop = 0; loop < names.length; loop++ ) {
                names[loop] = ( (Targetable)targets.elementAt(loop) )
                    .getDisplayName();
            }
            SingleChoiceDialog choiceDialog =
                new SingleChoiceDialog( clientgui.frame,
                        Messages.getString("PhysicalDisplay.ChooseTargetDialog.title"), //$NON-NLS-1$
                        Messages.getString("PhysicalDisplay.ChooseTargetDialog.message", new Object[]{pos.getBoardNum()}), //$NON-NLS-1$
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
    public void gameTurnChange(GameTurnChangeEvent e) {

        // Are we ignoring events?
        if ( this.isIgnoringEvents() ) {
            return;
        }

        if (client.game.getPhase() == IGame.PHASE_PHYSICAL) {
            endMyTurn();

            if (client.isMyTurn()) {
                beginMyTurn();
                setStatusBarText(Messages.getString("PhysicalDisplay.its_your_turn")); //$NON-NLS-1$
            } else {
                setStatusBarText(Messages.getString("PhysicalDisplay.its_others_turn",new Object[]{e.getPlayer().getName()})); //$NON-NLS-1$
            }
        } else {
            System.err.println("PhysicalDisplay: got turnchange event when it's not the physical attacks phase"); //$NON-NLS-1$
        }
    }
    public void gamePhaseChange(GamePhaseChangeEvent e) {

        // Are we ignoring events?
        if ( this.isIgnoringEvents() ) {
            return;
        }

        if (client.isMyTurn() && client.game.getPhase() != IGame.PHASE_PHYSICAL) {
            endMyTurn();
        }
        // if we're ending the firing phase, unregister stuff.
        if (client.game.getPhase() ==  IGame.PHASE_PHYSICAL) {
            setStatusBarText(Messages.getString("PhysicalDisplay.waitingForPhysicalAttackPhase")); //$NON-NLS-1$
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
        } else if (ev.getActionCommand().equals(PHYSICAL_PROTO)) {
            proto();
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

        // Are we ignoring events?
        if ( this.isIgnoringEvents() ) {
            return;
        }

        if (ev.getKeyCode() == KeyEvent.VK_ESCAPE) {
            clearattacks();
        } else if (ev.getKeyCode() == KeyEvent.VK_ENTER && ev.isControlDown()) {
            if (client.isMyTurn()) {
                //
            }
        }
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
    
    public void unitSelected(BoardViewEvent b) {

        // Are we ignoring events?
        if ( this.isIgnoringEvents() ) {
            return;
        }

        Entity e = client.game.getEntity(b.getEntityId());
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
    
    public void setThrashEnabled(boolean enabled) {
        butThrash.setEnabled(enabled);
        clientgui.getMenuBar().setPhysicalThrashEnabled(enabled);
    }
    public void setPunchEnabled(boolean enabled) {
        butPunch.setEnabled(enabled);
        clientgui.getMenuBar().setPhysicalPunchEnabled(enabled);
    }
    public void setKickEnabled(boolean enabled) {
        butKick.setEnabled(enabled);
        clientgui.getMenuBar().setPhysicalKickEnabled(enabled);
    }
    public void setPushEnabled(boolean enabled) {
        butPush.setEnabled(enabled);
        clientgui.getMenuBar().setPhysicalPushEnabled(enabled);
    }
    public void setClubEnabled(boolean enabled) {
        butClub.setEnabled(enabled);
        clientgui.getMenuBar().setPhysicalClubEnabled(enabled);
    }
    public void setBrushOffEnabled(boolean enabled) {
        butBrush.setEnabled(enabled);
        clientgui.getMenuBar().setPhysicalBrushOffEnabled(enabled);
    }
    public void setDodgeEnabled(boolean enabled) {
        butDodge.setEnabled(enabled);
        clientgui.getMenuBar().setPhysicalDodgeEnabled(enabled);
    }
    public void setProtoEnabled(boolean enabled) {
        butProto.setEnabled(enabled);
        clientgui.getMenuBar().setPhysicalProtoEnabled(enabled);
    }
    public void setNextEnabled(boolean enabled) {
        butNext.setEnabled(enabled);
        clientgui.getMenuBar().setPhysicalNextEnabled(enabled);
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
        clientgui.getBoardView().removeBoardViewListener(this);
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

}
