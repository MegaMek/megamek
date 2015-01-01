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

package megamek.client.ui.swing;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;

import megamek.client.Client;
import megamek.client.event.BoardViewEvent;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.widget.IndexedCheckbox;
import megamek.common.BattleArmor;
import megamek.common.Building;
import megamek.common.BuildingTarget;
import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.GameTurn;
import megamek.common.IAimingModes;
import megamek.common.IGame;
import megamek.common.INarcPod;
import megamek.common.Infantry;
import megamek.common.Mech;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.QuadMech;
import megamek.common.TargetRoll;
import megamek.common.Targetable;
import megamek.common.ToHitData;
import megamek.common.actions.BAVibroClawAttackAction;
import megamek.common.actions.BreakGrappleAttackAction;
import megamek.common.actions.BrushOffAttackAction;
import megamek.common.actions.ClubAttackAction;
import megamek.common.actions.DodgeAction;
import megamek.common.actions.EntityAction;
import megamek.common.actions.GrappleAttackAction;
import megamek.common.actions.JumpJetAttackAction;
import megamek.common.actions.KickAttackAction;
import megamek.common.actions.LayExplosivesAttackAction;
import megamek.common.actions.ProtomechPhysicalAttackAction;
import megamek.common.actions.PunchAttackAction;
import megamek.common.actions.PushAttackAction;
import megamek.common.actions.SearchlightAttackAction;
import megamek.common.actions.ThrashAttackAction;
import megamek.common.actions.TripAttackAction;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.event.GameTurnChangeEvent;

public class PhysicalDisplay extends StatusBarPhaseDisplay implements
        DoneButtoned, KeyListener {

    /**
     *
     */
    private static final long serialVersionUID = -3274750006768636001L;

    public static final String PHYSICAL_PUNCH = "punch"; //$NON-NLS-1$
    public static final String PHYSICAL_KICK = "kick"; //$NON-NLS-1$
    public static final String PHYSICAL_CLUB = "club"; //$NON-NLS-1$
    public static final String PHYSICAL_BRUSH_OFF = "brushOff"; //$NON-NLS-1$
    public static final String PHYSICAL_THRASH = "thrash"; //$NON-NLS-1$
    public static final String PHYSICAL_DODGE = "dodge"; //$NON-NLS-1$
    public static final String PHYSICAL_PUSH = "push"; //$NON-NLS-1$
    public static final String PHYSICAL_TRIP = "trip"; //$NON-NLS-1$
    public static final String PHYSICAL_GRAPPLE = "grapple"; //$NON-NLS-1$
    public static final String PHYSICAL_JUMPJET = "jumpjet"; //$NON-NLS-1$
    public static final String PHYSICAL_NEXT = "next"; //$NON-NLS-1$
    public static final String PHYSICAL_PROTO = "protoPhysical"; //$NON-NLS-1$
    public static final String PHYSICAL_VIBRO = "vibroPhysical"; //$NON-NLS-1$
    public static final String PHYSICAL_SEARCHLIGHT = "fireSearchlight"; //$NON-NLS-1$
    public static final String PHYSICAL_EXPLOSIVES = "explosives"; //$NON-NLS-1$

    private static final int NUM_BUTTON_LAYOUTS = 3;
    // parent game
    ClientGUI clientgui;
    private Client client;

    // buttons
    private JComponent panButtons;

    private JButton butPunch;
    private JButton butKick;
    private JButton butPush;
    private JButton butTrip;
    private JButton butGrapple;
    private JButton butJumpJet;
    private JButton butClub;
    private JButton butBrush;
    private JButton butThrash;
    private JButton butDodge;
    private JButton butProto;
    private JButton butExplosives;
    private JButton butVibro;

    private JButton butNext;
    private JButton butDone;
    private JButton butMore;

    private JButton butSpace;
    private JButton butSpace2;
    private JButton butSearchlight;

    private int buttonLayout;

    // let's keep track of what we're shooting and at what, too
    private int cen = Entity.NONE; // current entity number
    Targetable target; // target

    // stuff we want to do
    private Vector<EntityAction> attacks;

    private AimedShotHandler ash = new AimedShotHandler();

    /**
     * Creates and lays out a new movement phase display for the specified
     * client.
     */
    public PhysicalDisplay(ClientGUI clientgui) {
        this.clientgui = clientgui;
        client = clientgui.getClient();
        client.game.addGameListener(this);

        clientgui.getBoardView().addBoardViewListener(this);

        attacks = new Vector<EntityAction>();

        setupStatusBar(Messages
                .getString("PhysicalDisplay.waitingForPhysicalAttackPhase")); //$NON-NLS-1$

        butSpace = new JButton("."); //$NON-NLS-1$
        butSpace.setEnabled(false);

        butSpace2 = new JButton("."); //$NON-NLS-1$
        butSpace2.setEnabled(false);

        butPunch = new JButton(Messages.getString("PhysicalDisplay.Punch")); //$NON-NLS-1$
        butPunch.addActionListener(this);
        butPunch.setEnabled(false);
        butPunch.setActionCommand(PHYSICAL_PUNCH);

        butKick = new JButton(Messages.getString("PhysicalDisplay.Kick")); //$NON-NLS-1$
        butKick.addActionListener(this);
        butKick.setEnabled(false);
        butKick.setActionCommand(PHYSICAL_KICK);

        butPush = new JButton(Messages.getString("PhysicalDisplay.Push")); //$NON-NLS-1$
        butPush.addActionListener(this);
        butPush.setEnabled(false);
        butPush.setActionCommand(PHYSICAL_PUSH);

        butTrip = new JButton(Messages.getString("PhysicalDisplay.Trip")); //$NON-NLS-1$
        butTrip.addActionListener(this);
        butTrip.setEnabled(false);
        butTrip.setActionCommand(PHYSICAL_TRIP);

        butGrapple = new JButton(Messages.getString("PhysicalDisplay.Grapple")); //$NON-NLS-1$
        butGrapple.addActionListener(this);
        butGrapple.setEnabled(false);
        butGrapple.setActionCommand(PHYSICAL_GRAPPLE);

        butJumpJet = new JButton(Messages.getString("PhysicalDisplay.JumpJet")); //$NON-NLS-1$
        butJumpJet.addActionListener(this);
        butJumpJet.setEnabled(false);
        butJumpJet.setActionCommand(PHYSICAL_JUMPJET);

        butClub = new JButton(Messages.getString("PhysicalDisplay.Club")); //$NON-NLS-1$
        butClub.addActionListener(this);
        butClub.setEnabled(false);
        butClub.setActionCommand(PHYSICAL_CLUB);

        butBrush = new JButton(Messages.getString("PhysicalDisplay.BrushOff")); //$NON-NLS-1$
        butBrush.addActionListener(this);
        butBrush.setEnabled(false);
        butBrush.setActionCommand(PHYSICAL_BRUSH_OFF);

        butThrash = new JButton(Messages.getString("PhysicalDisplay.Trash")); //$NON-NLS-1$
        butThrash.addActionListener(this);
        butThrash.setEnabled(false);
        butThrash.setActionCommand(PHYSICAL_THRASH);

        butDodge = new JButton(Messages.getString("PhysicalDisplay.Dodge")); //$NON-NLS-1$
        butDodge.addActionListener(this);
        butDodge.setEnabled(false);
        butDodge.setActionCommand(PHYSICAL_DODGE);

        butProto = new JButton(Messages
                .getString("PhysicalDisplay.ProtoPhysical")); //$NON-NLS-1$
        butProto.addActionListener(this);
        butProto.setEnabled(false);
        butProto.setActionCommand(PHYSICAL_PROTO);

        butVibro = new JButton(Messages
                .getString("PhysicalDisplay.Vibro")); //$NON-NLS-1$
        butVibro.addActionListener(this);
        butVibro.setEnabled(false);
        butVibro.setActionCommand(PHYSICAL_VIBRO);

        butExplosives = new JButton(Messages
                .getString("PhysicalDisplay.Explosives")); //$NON-NLS-1$
        butExplosives.addActionListener(this);
        butExplosives.setEnabled(false);
        butExplosives.setActionCommand(PHYSICAL_EXPLOSIVES);

        butDone = new JButton(Messages.getString("PhysicalDisplay.Done")); //$NON-NLS-1$
        butDone.addActionListener(this);
        butDone.setEnabled(false);

        butNext = new JButton(Messages.getString("PhysicalDisplay.NextUnit")); //$NON-NLS-1$
        butNext.addActionListener(this);
        butNext.setEnabled(false);
        butNext.setActionCommand(PHYSICAL_NEXT);

        butMore = new JButton(Messages.getString("PhysicalDisplay.More")); //$NON-NLS-1$
        butMore.addActionListener(this);
        butMore.setEnabled(false);

        butSearchlight = new JButton(Messages
                .getString("FiringDisplay.Searchlight")); //$NON-NLS-1$
        butSearchlight.addActionListener(this);
        butSearchlight.addKeyListener(this);
        butSearchlight.setActionCommand(PHYSICAL_SEARCHLIGHT);
        butSearchlight.setEnabled(false);

        // layout button grid
        panButtons = new JPanel();
        buttonLayout = 0;
        setupButtonPanel();

        // layout screen
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        setLayout(gridbag);

        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.insets = new Insets(1, 1, 1, 1);
        // c.gridwidth = GridBagConstraints.REMAINDER;
        // addBag(clientgui.bv, gridbag, c);

        // c.weightx = 1.0; c.weighty = 0;
        // c.gridwidth = 1;
        // addBag(client.cb.getComponent(), gridbag, c);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 0.0;
        c.weighty = 0.0;
        addBag(panButtons, gridbag, c);

        c.weightx = 1.0;
        c.weighty = 0.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        addBag(panStatus, gridbag, c);

        clientgui.bv.addKeyListener(this);
        addKeyListener(this);

    }

    private void addBag(JComponent comp, GridBagLayout gridbag,
            GridBagConstraints c) {
        gridbag.setConstraints(comp, c);
        add(comp);
        comp.addKeyListener(this);
    }

    private void setupButtonPanel() {
        panButtons.removeAll();
        panButtons.setLayout(new GridLayout(0, 6));

        switch (buttonLayout) {
        case 0:
            panButtons.add(butNext);
            panButtons.add(butPunch);
            panButtons.add(butKick);
            panButtons.add(butPush);
            panButtons.add(butClub);
            panButtons.add(butMore);
            break;
        case 1:
            panButtons.add(butBrush);
            panButtons.add(butThrash);
            panButtons.add(butVibro);
            panButtons.add(butProto);
            panButtons.add(butJumpJet);
            panButtons.add(butMore);
            break;
        case 2:
            panButtons.add(butDodge);
            panButtons.add(butTrip);
            panButtons.add(butGrapple);
            panButtons.add(butSearchlight);
            panButtons.add(butExplosives);
            panButtons.add(butMore);
            break;
    }
        panButtons.validate();
        panButtons.repaint();
    }

    /**
     * Selects an entity, by number, for movement.
     */
    public void selectEntity(int en) {
        if (client.game.getEntity(en) == null) {
            System.err
                    .println("PhysicalDisplay: tried to select non-existant entity: " + en); //$NON-NLS-1$
            return;
        }

        cen = en;
        clientgui.setSelectedEntityNum(en);

        Entity entity = ce();

        target(null);
        if (entity instanceof Mech) {
            int grapple = ((Mech) entity).getGrappled();
            if (grapple != Entity.NONE) {
                Entity t = client.game.getEntity(grapple);
                if (t != null) {
                    target(t);
                }
            }
        }
        clientgui.getBoardView().highlight(ce().getPosition());
        clientgui.getBoardView().select(null);
        clientgui.getBoardView().cursor(null);

        clientgui.mechD.displayEntity(entity);
        clientgui.mechD.showPanel("movement"); //$NON-NLS-1$

        clientgui.bv.centerOnHex(entity.getPosition());

        // Update the menu bar.
        clientgui.getMenuBar().setEntity(ce());

        // does it have a club?
        String clubLabel = null;
        for (Mounted club : entity.getClubs()) {
            String thisLab;
            if (club.getName().endsWith("Club")) { //$NON-NLS-1$
                thisLab = Messages.getString("PhysicalDisplay.Club"); //$NON-NLS-1$
            } else {
                thisLab = club.getName();
            }
            if (clubLabel == null) {
                clubLabel = thisLab;
            } else {
                clubLabel = clubLabel + "/" + thisLab;
            }
        }
        if (clubLabel == null) {
            clubLabel = Messages.getString("PhysicalDisplay.Club"); //$NON-NLS-1$
        }
        butClub.setText(clubLabel);

        if ((entity instanceof Mech)
                && !entity.isProne()
                && entity.getCrew().getOptions()
                        .booleanOption("dodge_maneuver")) { //$NON-NLS-1$
            setDodgeEnabled(true);
        }
    }

    /**
     * Does turn start stuff
     */
    private void beginMyTurn() {
        GameTurn turn = client.getMyTurn();
        // There's special processing for countering break grapple.
        if (turn instanceof GameTurn.CounterGrappleTurn) {
            disableButtons();
            selectEntity(((GameTurn.CounterGrappleTurn) turn).getEntityNum());
            grapple(true);
            ready();
        } else {
            target(null);
            selectEntity(client.getFirstEntityNum());
            setNextEnabled(true);
            butDone.setEnabled(true);
            butMore.setEnabled(true);
        }
        clientgui.setDisplayVisible(true);
        clientgui.getBoardView().select(null);
    }

    /**
     * Does end turn stuff.
     */
    private void endMyTurn() {
        // end my turn, then.
        Entity next = client.game.getNextEntity(client.game.getTurnIndex());
        if ((IGame.Phase.PHASE_PHYSICAL == client.game.getPhase())
                && (null != next) && (null != ce())
                && (next.getOwnerId() != ce().getOwnerId())) {
            clientgui.setDisplayVisible(false);
        }
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
        setTripEnabled(false);
        setGrappleEnabled(false);
        setJumpJetEnabled(false);
        setClubEnabled(false);
        setBrushOffEnabled(false);
        setThrashEnabled(false);
        setDodgeEnabled(false);
        setProtoEnabled(false);
        setVibroEnabled(false);
        setExplosivesEnabled(false);
        butDone.setEnabled(false);
        setNextEnabled(false);
    }

    /**
     * Called when the current entity is done with physical attacks.
     */
    private void ready() {
        if (attacks.isEmpty()
                && GUIPreferences.getInstance().getNagForNoAction()) {
            // comfirm this action
            ConfirmDialog response = clientgui
                    .doYesNoBotherDialog(
                            Messages
                                    .getString("PhysicalDisplay.DontPhysicalAttackDialog.title") //$NON-NLS-1$
                            ,
                            Messages
                                    .getString("PhysicalDisplay.DontPhysicalAttackDialog.message")); //$NON-NLS-1$
            if (!response.getShowAgain()) {
                GUIPreferences.getInstance().setNagForNoAction(false);
            }
            if (!response.getAnswer()) {
                return;
            }
        }

        disableButtons();
        client.sendAttackData(cen, attacks);
        attacks.removeAllElements();
        // close aimed shot display, if any
        ash.closeDialog();
        endMyTurn();
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
    void punch() {
        final ToHitData leftArm = PunchAttackAction.toHit(client.game, cen,
                target, PunchAttackAction.LEFT);
        final ToHitData rightArm = PunchAttackAction.toHit(client.game, cen,
                target, PunchAttackAction.RIGHT);
        String title = Messages
                .getString(
                        "PhysicalDisplay.PunchDialog.title", new Object[] { target.getDisplayName() }); //$NON-NLS-1$
        String message = Messages.getString(
                "PhysicalDisplay.PunchDialog.message", new Object[] {//$NON-NLS-1$
                        rightArm.getValueAsString(),
                        new Double(Compute.oddsAbove(rightArm.getValue())),
                        rightArm.getDesc(),
                        new Integer(PunchAttackAction.getDamageFor(ce(),
                                PunchAttackAction.RIGHT,
                                (target instanceof Infantry)
                                        && !(target instanceof BattleArmor))),
                        rightArm.getTableDesc(),
                        leftArm.getValueAsString(),
                        new Double(Compute.oddsAbove(leftArm.getValue())),
                        leftArm.getDesc(),
                        new Integer(PunchAttackAction.getDamageFor(ce(),
                                PunchAttackAction.LEFT,
                                (target instanceof Infantry)
                                        && !(target instanceof BattleArmor))),
                        leftArm.getTableDesc() });
        if (clientgui.doYesNoDialog(title, message)) {
            // check for retractable blade that can be extended in each arm
            boolean leftBladeExtend = false;
            boolean rightBladeExtend = false;
            if ((ce() instanceof Mech)
                    && (target instanceof Entity)
                    && clientgui.client.game.getOptions().booleanOption(
                            "tacops_retractable_blades")
                    && (leftArm.getValue() != TargetRoll.IMPOSSIBLE)
                    && ((Mech) ce()).hasRetractedBlade(Mech.LOC_LARM)) {
                leftBladeExtend = clientgui.doYesNoDialog(Messages
                        .getString("PhysicalDisplay.ExtendBladeDialog.title"),
                        Messages.getString(
                                "PhysicalDisplay.ExtendBladeDialog.message",
                                new Object[] { ce().getLocationName(
                                        Mech.LOC_LARM) }));
            }
            if ((ce() instanceof Mech)
                    && (target instanceof Entity)
                    && (rightArm.getValue() != TargetRoll.IMPOSSIBLE)
                    && clientgui.client.game.getOptions().booleanOption(
                            "tacops_retractable_blades")
                    && ((Mech) ce()).hasRetractedBlade(Mech.LOC_RARM)) {
                rightBladeExtend = clientgui.doYesNoDialog(Messages
                        .getString("PhysicalDisplay.ExtendBladeDialog.title"),
                        Messages.getString(
                                "PhysicalDisplay.ExtendBladeDialog.message",
                                new Object[] { ce().getLocationName(
                                        Mech.LOC_RARM) }));
            }
            disableButtons();
            // declare searchlight, if possible
            if (GUIPreferences.getInstance().getAutoDeclareSearchlight()) {
                doSearchlight();
            }

            if ((leftArm.getValue() != TargetRoll.IMPOSSIBLE)
                    && (rightArm.getValue() != TargetRoll.IMPOSSIBLE)) {
                attacks.addElement(new PunchAttackAction(cen, target
                        .getTargetType(), target.getTargetId(),
                        PunchAttackAction.BOTH, leftBladeExtend,
                        rightBladeExtend));
            } else if (leftArm.getValue() < rightArm.getValue()) {
                attacks.addElement(new PunchAttackAction(cen, target
                        .getTargetType(), target.getTargetId(),
                        PunchAttackAction.LEFT, leftBladeExtend,
                        rightBladeExtend));
            } else {
                attacks.addElement(new PunchAttackAction(cen, target
                        .getTargetType(), target.getTargetId(),
                        PunchAttackAction.RIGHT, leftBladeExtend,
                        rightBladeExtend));
            }
            ready();
        }
    }

    private void doSearchlight() {
        // validate
        if ((ce() == null) || (target == null)) {
            throw new IllegalArgumentException(
                    "current searchlight parameters are invalid"); //$NON-NLS-1$
        }

        if (!SearchlightAttackAction.isPossible(client.game, cen, target, null)) {
            return;
        }

        // create and queue a searchlight action
        SearchlightAttackAction saa = new SearchlightAttackAction(cen, target
                .getTargetType(), target.getTargetId());
        attacks.addElement(saa);

        // and add it into the game, temporarily
        client.game.addAction(saa);
        clientgui.bv.addAttack(saa);
        clientgui.minimap.drawMap();

        // and prevent duplicates
        setSearchlightEnabled(false);

        // refresh weapon panel, as bth will have changed
        updateTarget();
    }

    /**
     * Kick the target!
     */
    void kick() {
        ToHitData leftLeg = KickAttackAction.toHit(client.game, cen, target,
                KickAttackAction.LEFT);
        ToHitData rightLeg = KickAttackAction.toHit(client.game, cen, target,
                KickAttackAction.RIGHT);
        ToHitData rightRearLeg = null;
        ToHitData leftRearLeg = null;

        ToHitData attackLeg;
        int attackSide = KickAttackAction.LEFT;
        int value = leftLeg.getValue();
        attackLeg = leftLeg;

        if (value > rightLeg.getValue()) {
            value = rightLeg.getValue();
            attackSide = KickAttackAction.RIGHT;
            attackLeg = rightLeg;
        }
        if (client.game.getEntity(cen) instanceof QuadMech) {
            rightRearLeg = KickAttackAction.toHit(client.game, cen, target,
                    KickAttackAction.RIGHTMULE);
            leftRearLeg = KickAttackAction.toHit(client.game, cen, target,
                    KickAttackAction.LEFTMULE);
            if (value > rightRearLeg.getValue()) {
                value = rightRearLeg.getValue();
                attackSide = KickAttackAction.RIGHTMULE;
                attackLeg = rightRearLeg;
            }
            if (value > leftRearLeg.getValue()) {
                value = leftRearLeg.getValue();
                attackSide = KickAttackAction.LEFTMULE;
                attackLeg = leftRearLeg;
            }
        }
        String title = Messages
                .getString(
                        "PhysicalDisplay.KickDialog.title", new Object[] { target.getDisplayName() }); //$NON-NLS-1$
        String message = Messages.getString(
                "PhysicalDisplay.KickDialog.message", new Object[] {//$NON-NLS-1$
                        attackLeg.getValueAsString(),
                        new Double(Compute.oddsAbove(attackLeg.getValue())),
                        attackLeg.getDesc(),
                        KickAttackAction.getDamageFor(ce(), attackSide,
                                (target instanceof Infantry)
                                        && !(target instanceof BattleArmor))
                                + attackLeg.getTableDesc() });
        if (clientgui.doYesNoDialog(title, message)) {
            disableButtons();
            // declare searchlight, if possible
            if (GUIPreferences.getInstance().getAutoDeclareSearchlight()) {
                doSearchlight();
            }

            attacks.addElement(new KickAttackAction(cen,
                    target.getTargetType(), target.getTargetId(), attackSide));
            ready();
        }
    }

    /**
     * Push that target!
     */
    void push() {
        ToHitData toHit = PushAttackAction.toHit(client.game, cen, target);
        String title = Messages
                .getString(
                        "PhysicalDisplay.PushDialog.title", new Object[] { target.getDisplayName() }); //$NON-NLS-1$
        String message = Messages.getString(
                "PhysicalDisplay.PushDialog.message", new Object[] {//$NON-NLS-1$
                toHit.getValueAsString(),
                        new Double(Compute.oddsAbove(toHit.getValue())),
                        toHit.getDesc() });
        if (clientgui.doYesNoDialog(title, message)) {
            disableButtons();
            // declare searchlight, if possible
            if (GUIPreferences.getInstance().getAutoDeclareSearchlight()) {
                doSearchlight();
            }

            attacks.addElement(new PushAttackAction(cen,
                    target.getTargetType(), target.getTargetId(), target
                            .getPosition()));
            ready();
        }
    }

    /**
     * Trip that target!
     */
    void trip() {
        ToHitData toHit = TripAttackAction.toHit(client.game, cen, target);
        String title = Messages
                .getString(
                        "PhysicalDisplay.TripDialog.title", new Object[] { target.getDisplayName() }); //$NON-NLS-1$
        String message = Messages.getString(
                "PhysicalDisplay.TripDialog.message", new Object[] { //$NON-NLS-1$
                toHit.getValueAsString(),
                        new Double(Compute.oddsAbove(toHit.getValue())),
                        toHit.getDesc() });
        if (clientgui.doYesNoDialog(title, message)) {
            disableButtons();
            // declare searchlight, if possible
            if (GUIPreferences.getInstance().getAutoDeclareSearchlight()) {
                doSearchlight();
            }

            attacks.addElement(new TripAttackAction(cen,
                    target.getTargetType(), target.getTargetId()));
            ready();
        }
    }

    /**
     * Grapple that target!
     */
    void doGrapple() {
        if (((Mech) ce()).getGrappled() == Entity.NONE) {
            grapple(false);
        } else {
            breakGrapple();
        }
    }

    private void grapple(boolean counter) {
        ToHitData toHit = GrappleAttackAction.toHit(client.game, cen, target);
        String title = Messages
                .getString(
                        "PhysicalDisplay.GrappleDialog.title", new Object[] { target.getDisplayName() }); //$NON-NLS-1$
        String message = Messages.getString(
                "PhysicalDisplay.GrappleDialog.message", new Object[] { //$NON-NLS-1$
                toHit.getValueAsString(),
                        new Double(Compute.oddsAbove(toHit.getValue())),
                        toHit.getDesc() });
        if (counter) {
            message = Messages
                    .getString(
                            "PhysicalDisplay.CounterGrappleDialog.message", new Object[] { //$NON-NLS-1$
                                    target.getDisplayName(),
                                    toHit.getValueAsString(),
                                    new Double(Compute.oddsAbove(toHit
                                            .getValue())), toHit.getDesc() });
        }
        if (clientgui.doYesNoDialog(title, message)) {
            disableButtons();
            // declare searchlight, if possible
            if (GUIPreferences.getInstance().getAutoDeclareSearchlight()) {
                doSearchlight();
            }

            attacks.addElement(new GrappleAttackAction(cen, target
                    .getTargetType(), target.getTargetId()));
            ready();
        }
    }

    private void breakGrapple() {
        ToHitData toHit = BreakGrappleAttackAction.toHit(client.game, cen,
                target);
        String title = Messages
                .getString(
                        "PhysicalDisplay.BreakGrappleDialog.title", new Object[] { target.getDisplayName() }); //$NON-NLS-1$
        String message = Messages.getString(
                "PhysicalDisplay.BreakGrappleDialog.message", new Object[] { //$NON-NLS-1$
                toHit.getValueAsString(),
                        new Double(Compute.oddsAbove(toHit.getValue())),
                        toHit.getDesc() });
        if (clientgui.doYesNoDialog(title, message)) {
            disableButtons();
            // declare searchlight, if possible
            if (GUIPreferences.getInstance().getAutoDeclareSearchlight()) {
                doSearchlight();
            }

            attacks.addElement(new BreakGrappleAttackAction(cen, target
                    .getTargetType(), target.getTargetId()));
            ready();
        }
    }

    /**
     * slice 'em up with your vibroclaws
     */
    public void vibroclawatt() {
        BAVibroClawAttackAction act = new BAVibroClawAttackAction(cen, target
                .getTargetType(), target.getTargetId());
        ToHitData toHit = act.toHit(client.game);

        String title = Messages
                .getString(
                        "PhysicalDisplay.BAVibroClawDialog.title", new Object[] { target.getDisplayName() }); //$NON-NLS-1$
        String message = Messages.getString(
                "PhysicalDisplay.BAVibroClawDialog.message", new Object[] {//$NON-NLS-1$
                toHit.getValueAsString(),
                        new Double(Compute.oddsAbove(toHit.getValue())),
                        toHit.getDesc(),
                        ce().getVibroClaws() + toHit.getTableDesc() });

        // Give the user to cancel the attack.
        if (clientgui.doYesNoDialog(title, message)) {
            disableButtons();
            attacks.addElement(act);
            ready();
        }
    }

    void jumpjetatt() {
        ToHitData toHit;
        int leg;
        int damage;
        if (ce().isProne()) {
            toHit = JumpJetAttackAction.toHit(client.game, cen, target,
                    JumpJetAttackAction.BOTH);
            leg = JumpJetAttackAction.BOTH;
            damage = JumpJetAttackAction.getDamageFor(ce(),
                    JumpJetAttackAction.BOTH);
        } else {
            ToHitData left = JumpJetAttackAction.toHit(client.game, cen,
                    target, JumpJetAttackAction.LEFT);
            ToHitData right = JumpJetAttackAction.toHit(client.game, cen,
                    target, JumpJetAttackAction.RIGHT);
            int d_left = JumpJetAttackAction.getDamageFor(ce(),
                    JumpJetAttackAction.LEFT);
            int d_right = JumpJetAttackAction.getDamageFor(ce(),
                    JumpJetAttackAction.RIGHT);
            if (d_left * Compute.oddsAbove(left.getValue()) > d_right
                    * Compute.oddsAbove(right.getValue())) {
                toHit = left;
                leg = JumpJetAttackAction.LEFT;
                damage = d_left;
            } else {
                toHit = right;
                leg = JumpJetAttackAction.RIGHT;
                damage = d_right;
            }
        }

        String title = Messages
                .getString(
                        "PhysicalDisplay.JumpJetDialog.title", new Object[] { target.getDisplayName() }); //$NON-NLS-1$
        String message = Messages.getString(
                "PhysicalDisplay.JumpJetDialog.message", new Object[] { //$NON-NLS-1$
                toHit.getValueAsString(),
                        new Double(Compute.oddsAbove(toHit.getValue())),
                        toHit.getDesc(), damage });
        if (clientgui.doYesNoDialog(title, message)) {
            disableButtons();
            // declare searchlight, if possible
            if (GUIPreferences.getInstance().getAutoDeclareSearchlight()) {
                doSearchlight();
            }

            attacks.addElement(new JumpJetAttackAction(cen, target
                    .getTargetType(), target.getTargetId(), leg));
            ready();
        }
    }

    private Mounted chooseClub() {
        java.util.List<Mounted> clubs = ce().getClubs();
        if (clubs.size() == 1) {
            return clubs.get(0);
        } else if (clubs.size() > 1) {
            String[] names = new String[clubs.size()];
            for (int loop = 0; loop < names.length; loop++) {
                Mounted club = clubs.get(loop);
                names[loop] = Messages
                        .getString(
                                "PhysicalDisplay.ChooseClubDialog.line",
                                new Object[] {
                                        club.getName(),
                                        ClubAttackAction.toHit(client.game,
                                                cen, target, club,
                                                ash.getAimTable())
                                                .getValueAsString(),
                                        ClubAttackAction
                                                .getDamageFor(
                                                        ce(),
                                                        club,
                                                        (target instanceof Infantry)
                                                                && !(target instanceof BattleArmor)) });
            }

            SingleChoiceDialog choiceDialog = new SingleChoiceDialog(
                    clientgui.frame,
                    Messages
                            .getString("PhysicalDisplay.ChooseClubDialog.title"), //$NON-NLS-1$
                    Messages
                            .getString("PhysicalDisplay.ChooseClubDialog.message"), //$NON-NLS-1$
                    names);
            choiceDialog.setVisible(true);
            if (choiceDialog.getAnswer() == true) {
                return clubs.get(choiceDialog.getChoice());
            }
        }
        return null;
    }

    /**
     * Club that target!
     */
    void club() {
        Mounted club = chooseClub();
        if (null == club) {
            return;
        }
        ToHitData toHit = ClubAttackAction.toHit(client.game, cen, target,
                club, ash.getAimTable());
        String title = Messages
                .getString(
                        "PhysicalDisplay.ClubDialog.title", new Object[] { target.getDisplayName() }); //$NON-NLS-1$
        String message = Messages.getString(
                "PhysicalDisplay.ClubDialog.message", new Object[] {//$NON-NLS-1$
                        toHit.getValueAsString(),
                        new Double(Compute.oddsAbove(toHit.getValue())),
                        toHit.getDesc(),
                        ClubAttackAction.getDamageFor(ce(), club,
                                (target instanceof Infantry)
                                        && !(target instanceof BattleArmor))
                                + toHit.getTableDesc() });
        if (clientgui.doYesNoDialog(title, message)) {
            disableButtons();
            // declare searchlight, if possible
            if (GUIPreferences.getInstance().getAutoDeclareSearchlight()) {
                doSearchlight();
            }

            attacks.addElement(new ClubAttackAction(cen,
                    target.getTargetType(), target.getTargetId(), club, ash
                            .getAimTable()));
            ready();
        }
    }

    /**
     * Club that target!
     */
    void club(Mounted club) {
        if (null == club) {
            return;
        }
        ToHitData toHit = ClubAttackAction.toHit(client.game, cen, target,
                club, ash.getAimTable());
        String title = Messages
                .getString(
                        "PhysicalDisplay.ClubDialog.title", new Object[] { target.getDisplayName() }); //$NON-NLS-1$
        String message = Messages.getString(
                "PhysicalDisplay.ClubDialog.message", new Object[] { //$NON-NLS-1$
                        toHit.getValueAsString(),
                        new Double(Compute.oddsAbove(toHit.getValue())),
                        toHit.getDesc(),
                        ClubAttackAction.getDamageFor(ce(), club,
                                (target instanceof Infantry)
                                        && !(target instanceof BattleArmor))
                                + toHit.getTableDesc() });
        if (clientgui.doYesNoDialog(title, message)) {
            disableButtons();
            // declare searchlight, if possible
            if (GUIPreferences.getInstance().getAutoDeclareSearchlight()) {
                doSearchlight();
            }

            attacks.addElement(new ClubAttackAction(cen,
                    target.getTargetType(), target.getTargetId(), club, ash
                            .getAimTable()));
            ready();
        }
    }

    /**
     * Make a protomech physical attack on the target.
     */
    private void proto() {
        ToHitData proto = ProtomechPhysicalAttackAction.toHit(client.game, cen,
                target);
        String title = Messages
                .getString(
                        "PhysicalDisplay.ProtoMechAttackDialog.title", new Object[] { target.getDisplayName() }); //$NON-NLS-1$
        String message = Messages.getString(
                "PhysicalDisplay.ProtoMechAttackDialog.message", new Object[] {//$NON-NLS-1$
                        proto.getValueAsString(),
                        new Double(Compute.oddsAbove(proto.getValue())),
                        proto.getDesc(),
                        ProtomechPhysicalAttackAction.getDamageFor(ce())
                                + proto.getTableDesc() });
        if (clientgui.doYesNoDialog(title, message)) {
            disableButtons();
            // declare searchlight, if possible
            if (GUIPreferences.getInstance().getAutoDeclareSearchlight()) {
                doSearchlight();
            }

            attacks.addElement(new ProtomechPhysicalAttackAction(cen, target
                    .getTargetType(), target.getTargetId()));
            ready();
        }
    }

    private void explosives() {
        ToHitData explo = LayExplosivesAttackAction.toHit(client.game, cen,
                target);
        String title = Messages
                .getString(
                        "PhysicalDisplay.LayExplosivesAttackDialog.title", new Object[] { target.getDisplayName() }); //$NON-NLS-1$
        String message = Messages
                .getString(
                        "PhysicalDisplay.LayExplosivesAttackDialog.message", new Object[] {//$NON-NLS-1$
                                explo.getValueAsString(),
                                new Double(Compute.oddsAbove(explo.getValue())),
                                explo.getDesc() });
        if (clientgui.doYesNoDialog(title, message)) {
            disableButtons();
            attacks.addElement(new LayExplosivesAttackAction(cen, target
                    .getTargetType(), target.getTargetId()));
            ready();
        }
    }

    /**
     * Sweep off the target with the arms that the player selects.
     */
    private void brush() {
        ToHitData toHitLeft = BrushOffAttackAction.toHit(client.game, cen,
                target, BrushOffAttackAction.LEFT);
        ToHitData toHitRight = BrushOffAttackAction.toHit(client.game, cen,
                target, BrushOffAttackAction.RIGHT);
        boolean canHitLeft = (TargetRoll.IMPOSSIBLE != toHitLeft.getValue());
        boolean canHitRight = (TargetRoll.IMPOSSIBLE != toHitRight.getValue());
        int damageLeft = 0;
        int damageRight = 0;
        String title = null;
        StringBuffer warn = null;
        String left = null;
        String right = null;
        String both = null;
        String[] choices = null;
        SingleChoiceDialog dlg = null;

        // If the entity can't brush off, display an error message and abort.
        if (!canHitLeft && !canHitRight) {
            clientgui.doAlertDialog(Messages
                    .getString("PhysicalDisplay.AlertDialog.title"), //$NON-NLS-1$
                    Messages.getString("PhysicalDisplay.AlertDialog.message")); //$NON-NLS-1$
            return;
        }

        // If we can hit with both arms, the player will have to make a choice.
        // Otherwise, the player is just confirming the arm in the attack.
        if (canHitLeft && canHitRight) {
            both = Messages.getString("PhysicalDisplay.bothArms"); //$NON-NLS-1$
            warn = new StringBuffer(Messages
                    .getString("PhysicalDisplay.whichArm")); //$NON-NLS-1$
            title = Messages.getString("PhysicalDisplay.chooseBrushOff"); //$NON-NLS-1$
        } else {
            warn = new StringBuffer(Messages
                    .getString("PhysicalDisplay.confirmArm")); //$NON-NLS-1$
            title = Messages.getString("PhysicalDisplay.confirmBrushOff"); //$NON-NLS-1$
        }

        // Build the rest of the warning string.
        // Use correct text when the target is an iNarc pod.
        if (Targetable.TYPE_INARC_POD == target.getTargetType()) {
            warn.append(Messages.getString(
                    "PhysicalDisplay.brushOff1", new Object[] { target })); //$NON-NLS-1$
        } else {
            warn.append(Messages.getString("PhysicalDisplay.brushOff2")); //$NON-NLS-1$
        }

        // If we can hit with the left arm, get
        // the damage and construct the string.
        if (canHitLeft) {
            damageLeft = BrushOffAttackAction.getDamageFor(ce(),
                    BrushOffAttackAction.LEFT);
            left = Messages
                    .getString("PhysicalDisplay.LAHit", new Object[] {//$NON-NLS-1$
                                    toHitLeft.getValueAsString(),
                                    new Double(Compute.oddsAbove(toHitLeft
                                            .getValue())),
                                    new Integer(damageLeft) });
        }

        // If we can hit with the right arm, get
        // the damage and construct the string.
        if (canHitRight) {
            damageRight = BrushOffAttackAction.getDamageFor(ce(),
                    BrushOffAttackAction.RIGHT);
            right = Messages
                    .getString("PhysicalDisplay.RAHit", new Object[] {//$NON-NLS-1$
                                    toHitRight.getValueAsString(),
                                    new Double(Compute.oddsAbove(toHitRight
                                            .getValue())),
                                    new Integer(damageRight) });
        }

        // Allow the player to cancel or choose which arm(s) to use.
        if (canHitLeft && canHitRight) {
            choices = new String[3];
            choices[0] = left;
            choices[1] = right;
            choices[2] = both;
            dlg = new SingleChoiceDialog(clientgui.frame, title, warn
                    .toString(), choices);
            dlg.setVisible(true);
            if (dlg.getAnswer()) {
                disableButtons();
                switch (dlg.getChoice()) {
                case 0:
                    attacks.addElement(new BrushOffAttackAction(cen, target
                            .getTargetType(), target.getTargetId(),
                            BrushOffAttackAction.LEFT));
                    break;
                case 1:
                    attacks.addElement(new BrushOffAttackAction(cen, target
                            .getTargetType(), target.getTargetId(),
                            BrushOffAttackAction.RIGHT));
                    break;
                case 2:
                    attacks.addElement(new BrushOffAttackAction(cen, target
                            .getTargetType(), target.getTargetId(),
                            BrushOffAttackAction.BOTH));
                    break;
                }
                ready();

            } // End not-cancel

        } // End choose-attack(s)

        // If only the left arm is available, confirm that choice.
        else if (canHitLeft) {
            choices = new String[1];
            choices[0] = left;
            dlg = new SingleChoiceDialog(clientgui.frame, title, warn
                    .toString(), choices);
            dlg.setVisible(true);
            if (dlg.getAnswer()) {
                disableButtons();
                attacks.addElement(new BrushOffAttackAction(cen, target
                        .getTargetType(), target.getTargetId(),
                        BrushOffAttackAction.LEFT));
                ready();

            } // End not-cancel

        } // End confirm-left

        // If only the right arm is available, confirm that choice.
        else if (canHitRight) {
            choices = new String[1];
            choices[0] = right;
            dlg = new SingleChoiceDialog(clientgui.frame, title, warn
                    .toString(), choices);
            dlg.setVisible(true);
            if (dlg.getAnswer()) {
                disableButtons();
                attacks.addElement(new BrushOffAttackAction(cen, target
                        .getTargetType(), target.getTargetId(),
                        BrushOffAttackAction.RIGHT));
                ready();

            } // End not-cancel

        } // End confirm-right

    } // End private void brush()

    /**
     * Thrash at the target, unless the player cancels the action.
     */
    void thrash() {
        ThrashAttackAction act = new ThrashAttackAction(cen, target
                .getTargetType(), target.getTargetId());
        ToHitData toHit = act.toHit(client.game);

        String title = Messages
                .getString(
                        "PhysicalDisplay.TrashDialog.title", new Object[] { target.getDisplayName() }); //$NON-NLS-1$
        String message = Messages.getString(
                "PhysicalDisplay.TrashDialog.message", new Object[] {//$NON-NLS-1$
                        toHit.getValueAsString(),
                        new Double(Compute.oddsAbove(toHit.getValue())),
                        toHit.getDesc(),
                        ThrashAttackAction.getDamageFor(ce())
                                + toHit.getTableDesc() });

        // Give the user to cancel the attack.
        if (clientgui.doYesNoDialog(title, message)) {
            disableButtons();
            attacks.addElement(act);
            ready();
        }
    }

    /**
     * Dodge like that guy in that movie that I won't name for copywrite
     * reasons!
     */
    void dodge() {
        if (clientgui
                .doYesNoDialog(
                        Messages.getString("PhysicalDisplay.DodgeDialog.title"), Messages.getString("PhysicalDisplay.DodgeDialog.message"))) { //$NON-NLS-1$ //$NON-NLS-2$
            disableButtons();

            Entity entity = client.game.getEntity(cen);
            entity.dodging = true;

            DodgeAction act = new DodgeAction(cen);
            attacks.addElement(act);

            ready();
        }
    }

    /**
     * Targets something
     */
    void target(Targetable t) {
        target = t;
        updateTarget();
        ash.showDialog();
    }

    /**
     * Targets an entity
     */
    void updateTarget() {
        // dis/enable physical attach buttons
        if ((cen != Entity.NONE) && (target != null)) {
            if (target.getTargetType() != Targetable.TYPE_INARC_POD) {
                // punch?
                final ToHitData leftArm = PunchAttackAction.toHit(client.game,
                        cen, target, PunchAttackAction.LEFT);
                final ToHitData rightArm = PunchAttackAction.toHit(client.game,
                        cen, target, PunchAttackAction.RIGHT);
                boolean canPunch = (leftArm.getValue() != TargetRoll.IMPOSSIBLE)
                        || (rightArm.getValue() != TargetRoll.IMPOSSIBLE);
                setPunchEnabled(canPunch);

                // kick?
                ToHitData leftLeg = KickAttackAction.toHit(client.game, cen,
                        target, KickAttackAction.LEFT);
                ToHitData rightLeg = KickAttackAction.toHit(client.game, cen,
                        target, KickAttackAction.RIGHT);
                boolean canKick = (leftLeg.getValue() != TargetRoll.IMPOSSIBLE)
                        || (rightLeg.getValue() != TargetRoll.IMPOSSIBLE);
                ToHitData rightRearLeg = KickAttackAction.toHit(client.game,
                        cen, target, KickAttackAction.RIGHTMULE);
                ToHitData leftRearLeg = KickAttackAction.toHit(client.game,
                        cen, target, KickAttackAction.LEFTMULE);
                canKick |= (leftRearLeg.getValue() != TargetRoll.IMPOSSIBLE)
                        || (rightRearLeg.getValue() != TargetRoll.IMPOSSIBLE);

                setKickEnabled(canKick);

                // how about push?
                ToHitData push = PushAttackAction.toHit(client.game, cen,
                        target);
                setPushEnabled(push.getValue() != TargetRoll.IMPOSSIBLE);

                // how about trip?
                ToHitData trip = TripAttackAction.toHit(client.game, cen,
                        target);
                setTripEnabled(trip.getValue() != TargetRoll.IMPOSSIBLE);

                // how about grapple?
                ToHitData grap = GrappleAttackAction.toHit(client.game, cen,
                        target);
                ToHitData bgrap = BreakGrappleAttackAction.toHit(client.game,
                        cen, target);
                setGrappleEnabled((grap.getValue() != TargetRoll.IMPOSSIBLE)
                        || (bgrap.getValue() != TargetRoll.IMPOSSIBLE));

                // how about JJ?
                ToHitData jjl = JumpJetAttackAction.toHit(client.game, cen,
                        target, JumpJetAttackAction.LEFT);
                ToHitData jjr = JumpJetAttackAction.toHit(client.game, cen,
                        target, JumpJetAttackAction.RIGHT);
                ToHitData jjb = JumpJetAttackAction.toHit(client.game, cen,
                        target, JumpJetAttackAction.BOTH);
                setJumpJetEnabled(!((jjl.getValue() == TargetRoll.IMPOSSIBLE)
                        && (jjr.getValue() == TargetRoll.IMPOSSIBLE) && (jjb
                        .getValue() == TargetRoll.IMPOSSIBLE)));

                // clubbing?
                boolean canClub = false;
                boolean canAim = false;
                for (Mounted club : ce().getClubs()) {
                    if (club != null) {
                        ToHitData clubToHit = ClubAttackAction.toHit(
                                client.game, cen, target, club, ash
                                        .getAimTable());
                        canClub |= (clubToHit.getValue() != TargetRoll.IMPOSSIBLE);
                        // assuming S7 vibroswords count as swords and maces
                        // count as hatchets
                        if (club.getType().hasSubType(MiscType.S_SWORD)
                                || club.getType()
                                        .hasSubType(MiscType.S_HATCHET)
                                || club.getType().hasSubType(
                                        MiscType.S_VIBRO_SMALL)
                                || club.getType().hasSubType(
                                        MiscType.S_VIBRO_MEDIUM)
                                || club.getType().hasSubType(
                                        MiscType.S_VIBRO_LARGE)
                                || club.getType().hasSubType(MiscType.S_MACE)
                                || club.getType().hasSubType(
                                        MiscType.S_MACE_THB)
                                || club.getType().hasSubType(MiscType.S_LANCE)
                                || club.getType().hasSubType(
                                        MiscType.S_CHAIN_WHIP)
                                || club.getType().hasSubType(
                                        MiscType.S_RETRACTABLE_BLADE)) {
                            canAim = true;
                        }
                    }
                }
                setClubEnabled(canClub);
                ash.setCanAim(canAim);

                // Thrash at infantry?
                ToHitData thrash = new ThrashAttackAction(cen, target)
                        .toHit(client.game);
                setThrashEnabled(thrash.getValue() != TargetRoll.IMPOSSIBLE);

                // make a Protomech physical attack?
                ToHitData proto = ProtomechPhysicalAttackAction.toHit(
                        client.game, cen, target);
                setProtoEnabled(proto.getValue() != TargetRoll.IMPOSSIBLE);

                ToHitData explo = LayExplosivesAttackAction.toHit(client.game,
                        cen, target);
                setExplosivesEnabled(explo.getValue() != TargetRoll.IMPOSSIBLE);
            }
            // Brush off swarming infantry or iNarcPods?
            ToHitData brushRight = BrushOffAttackAction.toHit(client.game, cen,
                    target, BrushOffAttackAction.RIGHT);
            ToHitData brushLeft = BrushOffAttackAction.toHit(client.game, cen,
                    target, BrushOffAttackAction.LEFT);
            boolean canBrush = ((brushRight.getValue() != TargetRoll.IMPOSSIBLE) || (brushLeft
                    .getValue() != TargetRoll.IMPOSSIBLE));
            setBrushOffEnabled(canBrush);
        } else {
            setPunchEnabled(false);
            setPushEnabled(false);
            setTripEnabled(false);
            setGrappleEnabled(false);
            setJumpJetEnabled(false);
            setKickEnabled(false);
            setClubEnabled(false);
            setBrushOffEnabled(false);
            setThrashEnabled(false);
            setProtoEnabled(false);
            setVibroEnabled(false);
        }
        setSearchlightEnabled((ce() != null) && (target != null)
                && ce().isUsingSpotlight());
    }

    /**
     * Returns the current entity.
     */
    Entity ce() {
        return client.game.getEntity(cen);
    }

    //
    // BoardListener
    //
    @Override
    public void hexMoused(BoardViewEvent b) {

        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        // control pressed means a line of sight check.
        if ((b.getModifiers() & InputEvent.CTRL_MASK) != 0) {
            return;
        }
        if (client.isMyTurn()
                && ((b.getModifiers() & InputEvent.BUTTON1_MASK) != 0)) {
            if (b.getType() == BoardViewEvent.BOARD_HEX_DRAGGED) {
                if (!b.getCoords().equals(
                        clientgui.getBoardView().getLastCursor())) {
                    clientgui.getBoardView().cursor(b.getCoords());
                }
            } else if (b.getType() == BoardViewEvent.BOARD_HEX_CLICKED) {
                clientgui.getBoardView().select(b.getCoords());
            }
        }
    }

    @Override
    public void hexSelected(BoardViewEvent b) {
        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        if (client.isMyTurn() && (b.getCoords() != null) && (ce() != null)) {
            final Targetable targ = chooseTarget(b.getCoords());
            if (targ != null) {
                target(targ);
            } else {
                target(null);
            }
        }
    }

    /**
     * Have the player select a target from the entities at the given coords.
     *
     * @param pos
     *            - the <code>Coords</code> containing targets.
     */
    private Targetable chooseTarget(Coords pos) {

        // Assume that we have *no* choice.
        Targetable choice = null;

        // Get the available choices.
        Enumeration<Entity> choices = client.game.getEntities(pos);

        // Convert the choices into a List of targets.
        Vector<Targetable> targets = new Vector<Targetable>();
        while (choices.hasMoreElements()) {
            choice = choices.nextElement();
            if (!ce().equals(choice)) {
                targets.addElement(choice);
            }
        }

        // Is there a building in the hex?
        Building bldg = client.game.getBoard().getBuildingAt(pos);
        if (bldg != null) {
            targets.addElement(new BuildingTarget(pos, client.game.getBoard(),
                    false));
        }

        // Is the attacker targeting its own hex?
        if (ce().getPosition().equals(pos)) {
            // Add any iNarc pods attached to the entity.
            Iterator<INarcPod> pods = ce().getINarcPodsAttached();
            while (pods.hasNext()) {
                choice = pods.next();
                targets.addElement(choice);
            }
        }

        // Do we have a single choice?
        if (targets.size() == 1) {

            // Return that choice.
            choice = targets.elementAt(0);

        }

        // If we have multiple choices, display a selection dialog.
        else if (targets.size() > 1) {
            String[] names = new String[targets.size()];
            for (int loop = 0; loop < names.length; loop++) {
                names[loop] = targets.elementAt(loop).getDisplayName();
            }
            SingleChoiceDialog choiceDialog = new SingleChoiceDialog(
                    clientgui.frame,
                    Messages
                            .getString("PhysicalDisplay.ChooseTargetDialog.title"), //$NON-NLS-1$
                    Messages
                            .getString(
                                    "PhysicalDisplay.ChooseTargetDialog.message", new Object[] { pos.getBoardNum() }), //$NON-NLS-1$
                    names);
            choiceDialog.setVisible(true);
            if (choiceDialog.getAnswer() == true) {
                choice = targets.elementAt(choiceDialog.getChoice());
            }
        } // End have-choices

        // Return the chosen unit.
        return choice;

    } // End private Targetable chooseTarget( Coords )

    //
    // GameListener
    //
    @Override
    public void gameTurnChange(GameTurnChangeEvent e) {

        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        if (client.game.getPhase() == IGame.Phase.PHASE_PHYSICAL) {

            if (client.isMyTurn()) {
                if (cen == Entity.NONE) {
                    beginMyTurn();
                }
                setStatusBarText(Messages
                        .getString("PhysicalDisplay.its_your_turn")); //$NON-NLS-1$
            } else {
                endMyTurn();
                setStatusBarText(Messages
                        .getString(
                                "PhysicalDisplay.its_others_turn", new Object[] { e.getPlayer().getName() })); //$NON-NLS-1$
            }
        } else {
            System.err
                    .println("PhysicalDisplay: got turnchange event when it's not the physical attacks phase"); //$NON-NLS-1$
        }
    }

    @Override
    public void gamePhaseChange(GamePhaseChangeEvent e) {

        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        if (client.isMyTurn()
                && (client.game.getPhase() != IGame.Phase.PHASE_PHYSICAL)) {
            endMyTurn();
        }
        // if we're ending the firing phase, unregister stuff.
        if (client.game.getPhase() == IGame.Phase.PHASE_PHYSICAL) {
            setStatusBarText(Messages
                    .getString("PhysicalDisplay.waitingForPhysicalAttackPhase")); //$NON-NLS-1$
        }
    }

    //
    // ActionListener
    //
    public void actionPerformed(ActionEvent ev) {

        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        if (statusBarActionPerformed(ev, client)) {
            return;
        }

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
        } else if (ev.getActionCommand().equals(PHYSICAL_TRIP)) {
            trip();
        } else if (ev.getActionCommand().equals(PHYSICAL_GRAPPLE)) {
            doGrapple();
        } else if (ev.getActionCommand().equals(PHYSICAL_JUMPJET)) {
            jumpjetatt();
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
        } else if (ev.getActionCommand().equals(PHYSICAL_EXPLOSIVES)) {
            explosives();
        } else if (ev.getActionCommand().equals(PHYSICAL_VIBRO)) {
            vibroclawatt();
        } else if (ev.getActionCommand().equals(PHYSICAL_NEXT)) {
            selectEntity(client.getNextEntityNum(cen));
        } else if (ev.getActionCommand().equals(PHYSICAL_SEARCHLIGHT)) {
            doSearchlight();
        } else if (ev.getSource() == butMore) {
            buttonLayout++;

            if (buttonLayout >= NUM_BUTTON_LAYOUTS) {
                buttonLayout = 0;
            }

            setupButtonPanel();
        }
    }

    //
    // KeyListener
    //
    public void keyPressed(KeyEvent ev) {

        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        if (ev.getKeyCode() == KeyEvent.VK_ESCAPE) {
            clearattacks();
        } else if ((ev.getKeyCode() == KeyEvent.VK_ENTER) && ev.isControlDown()) {
            if (client.isMyTurn()) {
                //
            }
        }
    }

    public void keyReleased(KeyEvent ev) {
        // ignore
    }

    public void keyTyped(KeyEvent ev) {
        // ignore
    }

    //
    // BoardViewListener
    //
    @Override
    public void finishedMovingUnits(BoardViewEvent b) {
        // no action
    }

    @Override
    public void unitSelected(BoardViewEvent b) {

        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        Entity e = client.game.getEntity(b.getEntityId());
        if (client.isMyTurn()) {
            if (client.getMyTurn().isValidEntity(e, client.game)) {
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

    public void setTripEnabled(boolean enabled) {
        butTrip.setEnabled(enabled);
    }

    public void setGrappleEnabled(boolean enabled) {
        butGrapple.setEnabled(enabled);
    }

    public void setJumpJetEnabled(boolean enabled) {
        butJumpJet.setEnabled(enabled);
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

    public void setVibroEnabled(boolean enabled) {
        butVibro.setEnabled(enabled);
        clientgui.getMenuBar().setPhysicalVibroEnabled(enabled);
    }

    public void setExplosivesEnabled(boolean enabled) {
        butExplosives.setEnabled(enabled);
        // clientgui.getMenuBar().setExplosivesEnabled(enabled);
    }

    public void setNextEnabled(boolean enabled) {
        butNext.setEnabled(enabled);
        clientgui.getMenuBar().setPhysicalNextEnabled(enabled);
    }

    private void setSearchlightEnabled(boolean enabled) {
        butSearchlight.setEnabled(enabled);
        clientgui.getMenuBar().setFireSearchlightEnabled(enabled);
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
     * @return the <code>javax.swing.JButton</code> that activates this object's
     *         "Done" action.
     */
    public JButton getDoneButton() {
        return butDone;
    }

    private class AimedShotHandler implements ActionListener, ItemListener {
        private int aimingAt = -1;

        private int aimingMode = IAimingModes.AIM_MODE_NONE;

        private AimedShotDialog asd;

        private boolean canAim;

        public AimedShotHandler() {
            // no action
        }

        public int getAimTable() {
            switch (aimingAt) {
            case 0:
                return ToHitData.HIT_PUNCH;
            case 1:
                return ToHitData.HIT_KICK;
            default:
                return ToHitData.HIT_NORMAL;
            }
        }

        public void setCanAim(boolean v) {
            canAim = v;
        }

        public void showDialog() {

            if ((ce() == null) || (target == null)) {
                return;
            }

            if (asd != null) {
                int oldAimingMode = aimingMode;
                closeDialog();
                aimingMode = oldAimingMode;
            }

            if (canAim) {

                final int attackerElevation = ce().getElevation()
                        + ce().getGame().getBoard().getHex(ce().getPosition())
                                .getElevation();
                final int targetElevation = target.getElevation()
                        + ce().getGame().getBoard()
                                .getHex(target.getPosition()).getElevation();

                if ((target instanceof Mech) && (ce() instanceof Mech)
                        && (attackerElevation == targetElevation)) {
                    String[] options = { "punch", "kick" };
                    boolean[] enabled = { true, true };

                    asd = new AimedShotDialog(
                            clientgui.frame,
                            Messages
                                    .getString("PhysicalDisplay.AimedShotDialog.title"), //$NON-NLS-1$
                            Messages
                                    .getString("PhysicalDisplay.AimedShotDialog.message"), //$NON-NLS-1$
                            options, enabled, aimingAt, this, this);

                    asd.setVisible(true);
                    updateTarget();
                }
            }
        }

        public void closeDialog() {
            if (asd != null) {
                aimingAt = Entity.LOC_NONE;
                aimingMode = IAimingModes.AIM_MODE_NONE;
                asd.setVisible(false);
                asd = null;
                updateTarget();
            }
        }

        // ActionListener, listens to the button in the dialog.
        public void actionPerformed(ActionEvent ev) {
            closeDialog();
        }

        // ItemListener, listens to the radiobuttons in the dialog.
        public void itemStateChanged(ItemEvent ev) {
            IndexedCheckbox icb = (IndexedCheckbox) ev.getSource();
            aimingAt = icb.getIndex();
            updateTarget();
        }

    }
}
