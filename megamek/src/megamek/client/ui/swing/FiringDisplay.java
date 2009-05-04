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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import megamek.client.Client;
import megamek.client.event.BoardViewEvent;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.widget.IndexedCheckbox;
import megamek.common.Aero;
import megamek.common.AmmoType;
import megamek.common.BattleArmor;
import megamek.common.BipedMech;
import megamek.common.BombType;
import megamek.common.Building;
import megamek.common.BuildingTarget;
import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.GameTurn;
import megamek.common.GunEmplacement;
import megamek.common.IAimingModes;
import megamek.common.IGame;
import megamek.common.INarcPod;
import megamek.common.Infantry;
import megamek.common.LargeSupportTank;
import megamek.common.LosEffects;
import megamek.common.Mech;
import megamek.common.Mounted;
import megamek.common.Protomech;
import megamek.common.QuadMech;
import megamek.common.Tank;
import megamek.common.TargetRoll;
import megamek.common.Targetable;
import megamek.common.ToHitData;
import megamek.common.WeaponType;
import megamek.common.actions.AbstractEntityAction;
import megamek.common.actions.ArtilleryAttackAction;
import megamek.common.actions.EntityAction;
import megamek.common.actions.FindClubAction;
import megamek.common.actions.FlipArmsAction;
import megamek.common.actions.RepairWeaponMalfunctionAction;
import megamek.common.actions.SearchlightAttackAction;
import megamek.common.actions.SpotAction;
import megamek.common.actions.TorsoTwistAction;
import megamek.common.actions.TriggerAPPodAction;
import megamek.common.actions.TriggerBPodAction;
import megamek.common.actions.UnjamTurretAction;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.event.GameTurnChangeEvent;

public class FiringDisplay extends StatusBarPhaseDisplay implements
        DoneButtoned, KeyListener, ItemListener, ListSelectionListener {
    /**
     *
     */
    private static final long serialVersionUID = -5586388490027013723L;

    private static final int NUM_BUTTON_LAYOUTS = 2;

    public static final String FIRE_FIND_CLUB = "fireFindClub"; //$NON-NLS-1$
    public static final String FIRE_FIRE = "fireFire"; //$NON-NLS-1$
    public static final String FIRE_MODE = "fireMode"; //$NON-NLS-1$
    public static final String FIRE_FLIP_ARMS = "fireFlipArms"; //$NON-NLS-1$
    public static final String FIRE_MORE = "fireMore"; //$NON-NLS-1$
    public static final String FIRE_NEXT = "fireNext"; //$NON-NLS-1$
    public static final String FIRE_NEXT_TARG = "fireNextTarg"; //$NON-NLS-1$
    public static final String FIRE_SKIP = "fireSkip"; //$NON-NLS-1$
    public static final String FIRE_SPOT = "fireSpot"; //$NON-NLS-1$
    public static final String FIRE_TWIST = "fireTwist"; //$NON-NLS-1$
    public static final String FIRE_CANCEL = "fireCancel"; //$NON-NLS-1$
    public static final String FIRE_SEARCHLIGHT = "fireSearchlight"; //$NON-NLS-1$
    public static final String FIRE_CLEAR_TURRET = "fireClearTurret"; //$NON-NLS-1$
    public static final String FIRE_CLEAR_WEAPON = "fireClearWeaponJam"; //$NON-NLS-1$

    // parent game
    private Client client;

    ClientGUI clientgui;

    // buttons
    private JComponent panButtons;

    private JButton butFire;

    private JButton butTwist;

    private JButton butSkip;

    private JButton butFindClub;

    private JButton butNextTarg;

    private JButton butFlipArms;

    private JButton butSpot;

    private JButton butSearchlight;

    private JButton butSpace;

    private JButton butFireMode;

    private JButton butFireClearTurret;

    private JButton butFireClearWeaponJam;

    private JButton butNext;

    private JButton butDone;

    private JButton butMore;

    private int buttonLayout;

    // let's keep track of what we're shooting and at what, too
    private int cen = Entity.NONE; // current entity number

    Targetable target; // target

    // HACK : track when we wan to show the target choice dialog.
    private boolean showTargetChoice = true;

    // shots we have so far.
    private Vector<AbstractEntityAction> attacks;

    // is the shift key held?
    private boolean shiftheld;

    private boolean twisting;

    private Entity[] visibleTargets = null;

    private int lastTargetID = -1;

    private AimedShotHandler ash;

    /**
     * Creates and lays out a new firing phase display for the specified client.
     */
    public FiringDisplay(ClientGUI clientgui) {
        client = clientgui.getClient();
        this.clientgui = clientgui;
        client.game.addGameListener(this);

        clientgui.getBoardView().addBoardViewListener(this);

        shiftheld = false;

        // fire
        attacks = new Vector<AbstractEntityAction>();

        setupStatusBar(Messages
                .getString("FiringDisplay.waitingForFiringPhase")); //$NON-NLS-1$

        butFire = new JButton(Messages.getString("FiringDisplay.Fire")); //$NON-NLS-1$
        butFire.addActionListener(this);
        butFire.addKeyListener(this);
        butFire.setActionCommand(FIRE_FIRE);
        butFire.setEnabled(false);

        butSkip = new JButton(Messages.getString("FiringDisplay.Skip")); //$NON-NLS-1$
        butSkip.addActionListener(this);
        butSkip.addKeyListener(this);
        butSkip.setActionCommand(FIRE_SKIP);
        butSkip.setEnabled(false);

        butTwist = new JButton(Messages.getString("FiringDisplay.Twist")); //$NON-NLS-1$
        butTwist.addActionListener(this);
        butTwist.addKeyListener(this);
        butTwist.setActionCommand(FIRE_TWIST);
        butTwist.setEnabled(false);

        butFindClub = new JButton(Messages.getString("FiringDisplay.FindClub")); //$NON-NLS-1$
        butFindClub.addActionListener(this);
        butFindClub.addKeyListener(this);
        butFindClub.setActionCommand(FIRE_FIND_CLUB);
        butFindClub.setEnabled(false);

        butNextTarg = new JButton(Messages
                .getString("FiringDisplay.NextTarget")); //$NON-NLS-1$
        butNextTarg.addActionListener(this);
        butNextTarg.addKeyListener(this);
        butNextTarg.setActionCommand(FIRE_NEXT_TARG);
        butNextTarg.setEnabled(false);

        butFlipArms = new JButton(Messages.getString("FiringDisplay.FlipArms")); //$NON-NLS-1$
        butFlipArms.addActionListener(this);
        butFlipArms.addKeyListener(this);
        butFlipArms.setActionCommand(FIRE_FLIP_ARMS);
        butFlipArms.setEnabled(false);

        butSpot = new JButton(Messages.getString("FiringDisplay.Spot")); //$NON-NLS-1$
        butSpot.addActionListener(this);
        butSpot.addKeyListener(this);
        butSpot.setActionCommand(FIRE_SPOT);
        butSpot.setEnabled(false);

        butSearchlight = new JButton(Messages
                .getString("FiringDisplay.Searchlight")); //$NON-NLS-1$
        butSearchlight.addActionListener(this);
        butSearchlight.addKeyListener(this);
        butSearchlight.setActionCommand(FIRE_SEARCHLIGHT);
        butSearchlight.setEnabled(false);

        butSpace = new JButton("."); //$NON-NLS-1$
        butSpace.setEnabled(false);

        butFireMode = new JButton(Messages.getString("FiringDisplay.Mode")); //$NON-NLS-1$
        butFireMode.addActionListener(this);
        butFireMode.addKeyListener(this);
        butFireMode.setActionCommand(FIRE_MODE);
        butFireMode.setEnabled(false);

        butFireClearTurret = new JButton(Messages
                .getString("FiringDisplay.ClearTurret")); //$NON-NLS-1$
        butFireClearTurret.addActionListener(this);
        butFireClearTurret.addKeyListener(this);
        butFireClearTurret.setActionCommand(FIRE_CLEAR_TURRET);
        butFireClearTurret.setEnabled(false);

        butFireClearWeaponJam = new JButton(Messages
                .getString("FiringDisplay.ClearWeaponJam")); //$NON-NLS-1$
        butFireClearWeaponJam.addActionListener(this);
        butFireClearWeaponJam.addKeyListener(this);
        butFireClearWeaponJam.setActionCommand(FIRE_CLEAR_WEAPON);
        butFireClearWeaponJam.setEnabled(false);

        butDone = new JButton(Messages.getString("FiringDisplay.Done")); //$NON-NLS-1$
        butDone.addActionListener(this);
        butDone.addKeyListener(this);
        butDone.setEnabled(false);

        butNext = new JButton(Messages.getString("FiringDisplay.NextUnit")); //$NON-NLS-1$
        butNext.addActionListener(this);
        butNext.addKeyListener(this);
        butNext.setActionCommand(FIRE_NEXT);
        butNext.setEnabled(false);

        butMore = new JButton(Messages.getString("FiringDisplay.More")); //$NON-NLS-1$
        butMore.addActionListener(this);
        butMore.addKeyListener(this);
        butMore.setActionCommand(FIRE_MORE);
        butMore.setEnabled(false);

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

        // mech display.
        clientgui.mechD.wPan.weaponList.addListSelectionListener(this);
        clientgui.mechD.wPan.weaponList.addKeyListener(this);

        ash = new AimedShotHandler();

    }

    private void addBag(JComponent comp, GridBagLayout gridbag,
            GridBagConstraints c) {
        gridbag.setConstraints(comp, c);
        add(comp);
        comp.addKeyListener(this);
    }

    private void setupButtonPanel() {
        panButtons.removeAll();
        panButtons.setLayout(new GridLayout(0, 8));

        switch (buttonLayout) {
        case 0:
            panButtons.add(butNext);
            panButtons.add(butFire);
            panButtons.add(butSkip);
            panButtons.add(butNextTarg);
            panButtons.add(butTwist);
            panButtons.add(butFireMode);
            panButtons.add(butFireClearWeaponJam);
            panButtons.add(butMore);
            break;
        case 1:
            panButtons.add(butNext);
            panButtons.add(butFire);
            panButtons.add(butFlipArms);
            panButtons.add(butFindClub);
            panButtons.add(butSpot);
            panButtons.add(butSearchlight);
            panButtons.add(butFireClearTurret);
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
        // clear any previously considered attacks
        if (en != cen) {
            clearAttacks();
            refreshAll();
        }

        if (client.game.getEntity(en) != null) {

            cen = en;
            clientgui.setSelectedEntityNum(en);

            // If the selected entity is not on the board, use the next one.
            // ASSUMPTION: there will always be *at least one* entity on map.
            if (ce().getPosition() == null) {

                // Walk through the list of entities for this player.
                for (int nextId = client.getNextEntityNum(en); nextId != en; nextId = client
                        .getNextEntityNum(nextId)) {

                    if (client.game.getEntity(nextId).getPosition() != null) {
                        cen = nextId;
                        break;
                    }

                } // Check the player's next entity.

                // We were *supposed* to have found an on-board entity.
                if (ce().getPosition() == null) {
                    System.err
                            .println("FiringDisplay: could not find an on-board entity: " + //$NON-NLS-1$
                                    en);
                    return;
                }

            } // End ce()-not-on-board

            int lastTarget = ce().getLastTarget();
            if (ce() instanceof Mech) {
                int grapple = ((Mech) ce()).getGrappled();
                if (grapple != Entity.NONE) {
                    lastTarget = grapple;
                }
            }
            Entity t = client.game.getEntity(lastTarget);
            target(t);

            if (!ce().isOffBoard()) {
                clientgui.getBoardView().highlight(ce().getPosition());
            }
            clientgui.getBoardView().select(null);
            clientgui.getBoardView().cursor(null);

            refreshAll();
            cacheVisibleTargets();

            if (!clientgui.bv.isMovingUnits() && !ce().isOffBoard()) {
                clientgui.bv.centerOnHex(ce().getPosition());
            }

            // Update the menu bar.
            clientgui.getMenuBar().setEntity(ce());

            // only twist if crew conscious
            setTwistEnabled(ce().canChangeSecondaryFacing()
                    && ce().getCrew().isActive());

            setFindClubEnabled(FindClubAction.canMechFindClub(client.game, en));
            setFlipArmsEnabled(ce().canFlipArms());
            updateSearchlight();
            updateClearTurret();
            updateClearWeaponJam();
        } else {
            System.err
                    .println("FiringDisplay: tried to select non-existant entity: " + en); //$NON-NLS-1$
        }
    }

    /**
     * Does turn start stuff
     */
    private void beginMyTurn() {
        target = null;

        selectEntity(client.getFirstEntityNum());

        if (!clientgui.bv.isMovingUnits()) {
            clientgui.setDisplayVisible(true);
        }

        GameTurn turn = client.getMyTurn();
        // There's special processing for triggering AP Pods.
        if ((turn instanceof GameTurn.TriggerAPPodTurn) && (ce() != null)) {
            disableButtons();
            TriggerAPPodDialog dialog = new TriggerAPPodDialog(clientgui
                    .getFrame(), ce());
            dialog.setVisible(true);
            attacks.removeAllElements();
            Enumeration<TriggerAPPodAction> actions = dialog.getActions();
            while (actions.hasMoreElements()) {
                attacks.addElement(actions.nextElement());
            }
            ready();
        } else if ((turn instanceof GameTurn.TriggerBPodTurn) && (null != ce())) {
            disableButtons();
            TriggerBPodDialog dialog = new TriggerBPodDialog(clientgui, ce(),
                    ((GameTurn.TriggerBPodTurn) turn).getAttackType());
            dialog.setVisible(true);
            attacks.removeAllElements();
            Enumeration<TriggerBPodAction> actions = dialog.getActions();
            while (actions.hasMoreElements()) {
                attacks.addElement(actions.nextElement());
            }
            ready();
        } else {
            setNextEnabled(true);
            butDone.setEnabled(true);
            butMore.setEnabled(true);
            setFireModeEnabled(true);
            clientgui.getBoardView().select(null);
        }
    }

    /**
     * Does end turn stuff.
     */
    private void endMyTurn() {
        // end my turn, then.
        Entity next = client.game.getNextEntity(client.game.getTurnIndex());
        if ((client.game.getPhase() == IGame.Phase.PHASE_FIRING) && (next != null)
                && (ce() != null) && (next.getOwnerId() != ce().getOwnerId())) {
            clientgui.setDisplayVisible(false);
        }
        cen = Entity.NONE;
        target(null);
        clientgui.getBoardView().select(null);
        clientgui.getBoardView().highlight(null);
        clientgui.getBoardView().cursor(null);
        clientgui.bv.clearMovementData();
        disableButtons();

        clearVisibleTargets();
    }

    /**
     * Disables all buttons in the interface
     */
    private void disableButtons() {
        setFireEnabled(false);
        setSkipEnabled(false);
        setTwistEnabled(false);
        setSpotEnabled(false);
        setFindClubEnabled(false);
        butMore.setEnabled(false);
        setNextEnabled(false);
        butDone.setEnabled(false);
        setNextTargetEnabled(false);
        setFlipArmsEnabled(false);
        setFireModeEnabled(false);
        setFireClearTurretEnabled(false);
        setFireClearWeaponJamEnabled(false);
    }

    /**
     * Fire Mode - Adds a Fire Mode Change to the current Attack Action
     */
    private void changeMode() {
        int wn = clientgui.mechD.wPan.getSelectedWeaponNum();

        // Do nothing we have no unit selected.
        if (ce() == null) {
            return;
        }

        // If the weapon does not have modes, just exit.
        Mounted m = ce().getEquipment(wn);
        if ((m == null) || !m.getType().hasModes()) {
            return;
        }

        // Aeros cannot switch modes under standard rules
        /*
         * if (ce() instanceof Aero) { return; }
         */

        // send change to the server
        int nMode = m.switchMode();
        client.sendModeChange(cen, wn, nMode);

        // notify the player
        if (m.canInstantSwitch(nMode)) {
            clientgui
                    .systemMessage(Messages
                            .getString(
                                    "FiringDisplay.switched", new Object[] { m.getName(), m.curMode().getDisplayableName() })); //$NON-NLS-1$
        } else {
            clientgui
                    .systemMessage(Messages
                            .getString(
                                    "FiringDisplay.willSwitch", new Object[] { m.getName(), m.pendingMode().getDisplayableName() })); //$NON-NLS-1$
        }

        updateTarget();
        clientgui.mechD.wPan.displayMech(ce());
        clientgui.mechD.wPan.selectWeapon(wn);
    }

    /**
     * Cache the list of visible targets. This is used for the 'next target'
     * button.
     * <p/>
     * We'll sort it by range to us.
     */
    private void cacheVisibleTargets() {
        clearVisibleTargets();

        Vector<Entity> vec = client.game.getValidTargets(ce());
        Comparator<Entity> sortComp = new Comparator<Entity>() {
            public int compare(Entity entX, Entity entY) {
                int rangeToX = ce().getPosition().distance(entX.getPosition());
                int rangeToY = ce().getPosition().distance(entY.getPosition());

                if (rangeToX == rangeToY) {
                    return ((entX.getId() < entY.getId()) ? -1 : 1);
                }

                return ((rangeToX < rangeToY) ? -1 : 1);
            }
        };

        // put the vector in the TreeSet first to sort it.
        TreeSet<Entity> tree = new TreeSet<Entity>(sortComp);
        visibleTargets = new Entity[vec.size()];

        for (int i = 0; i < vec.size(); i++) {
            tree.add(vec.elementAt(i));
        }

        // not go through the sorted Set to cache the targets.
        Iterator<Entity> it = tree.iterator();
        int count = 0;
        while (it.hasNext()) {
            visibleTargets[count++] = it.next();
        }

        setNextTargetEnabled(visibleTargets.length > 0);
    }

    private void clearVisibleTargets() {
        visibleTargets = null;
        lastTargetID = -1;
        setNextTargetEnabled(false);
    }

    /**
     * Get the next target. Return null if we don't have any targets.
     */
    private Entity getNextTarget() {
        if (visibleTargets == null) {
            return null;
        }

        lastTargetID++;

        if (lastTargetID >= visibleTargets.length) {
            lastTargetID = 0;
        }

        return visibleTargets[lastTargetID];
    }

    /**
     * Jump to our next target. If there isn't one, well, don't do anything.
     */
    private void jumpToNextTarget() {
        Entity targ = getNextTarget();

        if (targ == null) {
            return;
        }

        // HACK : don't show the choice dialog.
        showTargetChoice = false;

        clientgui.bv.centerOnHex(targ.getPosition());
        clientgui.getBoardView().select(targ.getPosition());

        // HACK : show the choice dialog again.
        showTargetChoice = true;
        target(targ);
    }

    /**
     * Called when the current entity is done firing. Send out our attack queue
     * to the server.
     */
    private void ready() {
        if (attacks.isEmpty()
                && GUIPreferences.getInstance().getNagForNoAction()) {
            // comfirm this action
            String title = Messages
                    .getString("FiringDisplay.DontFireDialog.title"); //$NON-NLS-1$
            String body = Messages
                    .getString("FiringDisplay.DontFireDialog.message"); //$NON-NLS-1$
            ConfirmDialog response = clientgui.doYesNoBotherDialog(title, body);
            if (!response.getShowAgain()) {
                GUIPreferences.getInstance().setNagForNoAction(false);
            }
            if (!response.getAnswer()) {
                return;
            }
        }

        // stop further input (hopefully)
        disableButtons();

        // remove temporary attacks from game & board
        removeTempAttacks();

        // For bug 1002223
        // Re-compute the to-hit numbers by adding in correct order.
        Vector<EntityAction> newAttacks = new Vector<EntityAction>();
        for (EntityAction o : attacks) {
            if (o instanceof ArtilleryAttackAction) {
                newAttacks.addElement(o);
            } else if (o instanceof WeaponAttackAction) {
                WeaponAttackAction waa = (WeaponAttackAction) o;
                Entity attacker = waa.getEntity(client.game);
                Targetable target1 = waa.getTarget(client.game);
                boolean curInFrontArc = Compute.isInArc(attacker.getPosition(),
                        attacker.getSecondaryFacing(), target1.getPosition(),
                        Compute.ARC_FORWARD);
                if (curInFrontArc) {
                    WeaponAttackAction waa2 = new WeaponAttackAction(waa
                            .getEntityId(), waa.getTargetType(), waa
                            .getTargetId(), waa.getWeaponId());
                    waa2.setAimedLocation(waa.getAimedLocation());
                    waa2.setAimingMode(waa.getAimingMode());
                    waa2.setOtherAttackInfo(waa.getOtherAttackInfo());
                    waa2.setAmmoId(waa.getAmmoId());
                    waa2.setBombPayload(waa.getBombPayload());
                    newAttacks.addElement(waa2);
                }
            } else {
                newAttacks.addElement(o);
            }
        }
        // now add the attacks in rear/arm arcs
        for (EntityAction o : attacks) {
            if (o instanceof ArtilleryAttackAction) {
                // newAttacks.addElement(o);
                continue;
            } else if (o instanceof WeaponAttackAction) {
                WeaponAttackAction waa = (WeaponAttackAction) o;
                Entity attacker = waa.getEntity(client.game);
                Targetable target1 = waa.getTarget(client.game);
                boolean curInFrontArc = Compute.isInArc(attacker.getPosition(),
                        attacker.getSecondaryFacing(), target1.getPosition(),
                        Compute.ARC_FORWARD);
                if (!curInFrontArc) {
                    WeaponAttackAction waa2 = new WeaponAttackAction(waa
                            .getEntityId(), waa.getTargetType(), waa
                            .getTargetId(), waa.getWeaponId());
                    waa2.setAimedLocation(waa.getAimedLocation());
                    waa2.setAimingMode(waa.getAimingMode());
                    waa2.setOtherAttackInfo(waa.getOtherAttackInfo());
                    waa2.setAmmoId(waa.getAmmoId());
                    waa2.setBombPayload(waa.getBombPayload());
                    newAttacks.addElement(waa2);
                }
            }
        }

        // send out attacks
        client.sendAttackData(cen, newAttacks);

        // clear queue
        attacks.removeAllElements();

        // Clear the menu bar.
        clientgui.getMenuBar().setEntity(null);

        // close aimed shot display, if any
        ash.closeDialog();

        endMyTurn();
    }

    /**
     * clear turret
     */
    private void doClearTurret() {
        String title = Messages.getString("FiringDisplay.ClearTurret.title"); //$NON-NLS-1$
        String body = Messages.getString("FiringDisplay.ClearTurret.message"); //$NON-NLS-1$
        if (!clientgui.doYesNoDialog(title, body)) {
            return;
        }
        if ((attacks.size() == 0) && (ce() instanceof Tank)
                && ((Tank) ce()).isTurretJammed()) {
            UnjamTurretAction uta = new UnjamTurretAction(ce().getId());
            attacks.add(uta);
            ready();
        }
    }

    /**
     * clear weapon jam
     */
    private void doClearWeaponJam() {
        ArrayList<Mounted> weapons = ((Tank) ce()).getJammedWeapons();
        String[] names = new String[weapons.size()];
        for (int loop = 0; loop < names.length; loop++) {
            names[loop] = weapons.get(loop).getDesc();
        }
        SingleChoiceDialog choiceDialog = new SingleChoiceDialog(
                clientgui.frame, Messages
                        .getString("FiringDisplay.ClearWeaponJam.title"), //$NON-NLS-1$
                Messages.getString("FiringDisplay.ClearWeaponJam.question"), //$NON-NLS-1$
                names);
        choiceDialog.setVisible(true);
        if (choiceDialog.getAnswer() == true) {
            RepairWeaponMalfunctionAction rwma = new RepairWeaponMalfunctionAction(
                    ce().getId(), ce().getEquipmentNum(
                            weapons.get(choiceDialog.getChoice())));
            attacks.add(rwma);
            ready();
        }
    }

    /**
     * fire searchlight
     */
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

        // refresh weapon panel, as bth will have changed
        updateTarget();
    }

    private int[] doSpaceBombing() {
        int[] payload = new int[BombType.B_NUM];
        if (!(ce() instanceof Aero)) {
            return payload;
        }
        Vector<Mounted> bombs = ((Aero) ce()).getSpaceBombs();
        String[] bnames = new String[bombs.size()];
        for (int i = 0; i < bnames.length; i++) {
            bnames[i] = bombs.elementAt(i).getDesc();
        }
        ChoiceDialog bombsDialog = new ChoiceDialog(
                clientgui.frame,
                Messages.getString("FiringDisplay.SpaceBombNumberDialog.title"), //$NON-NLS-1$
                Messages
                        .getString("FiringDisplay.SpaceBombNumberDialog.message"), //$NON-NLS-1$
                bnames);
        bombsDialog.setVisible(true);
        if (bombsDialog.getAnswer()) {
            int[] choices = bombsDialog.getChoices();
            for (int choice : choices) {
                int type = ((BombType) bombs.elementAt(choice).getType())
                        .getBombType();
                payload[type] = payload[type] + 1;
            }
        }
        return payload;
    }

    /**
     * Adds a weapon attack with the currently selected weapon to the attack
     * queue.
     */
    void fire() {
        // get the selected weaponnum
        int weaponNum = clientgui.mechD.wPan.getSelectedWeaponNum();
        Mounted mounted = ce().getEquipment(weaponNum);

        // validate
        if ((ce() == null) || (target == null) || (mounted == null)
                || !(mounted.getType() instanceof WeaponType)) {
            throw new IllegalArgumentException(
                    "current fire parameters are invalid"); //$NON-NLS-1$
        }
        // check if we now shoot at a target in the front arc and previously
        // shot a target in side/rear arc that then was primary target
        // if so, ask and tell the user that to-hits will change
        if ((ce() instanceof Mech) || (ce() instanceof Tank)
                || (ce() instanceof Protomech)) {
            EntityAction lastAction = null;
            try {
                lastAction = attacks.lastElement();
            } catch (NoSuchElementException ex) {
                // ignore
            }
            if ((lastAction != null) && (lastAction instanceof WeaponAttackAction)) {
                WeaponAttackAction oldWaa = (WeaponAttackAction) lastAction;
                Targetable oldTarget = oldWaa.getTarget(client.game);
                if (!oldTarget.equals(target)) {
                    boolean oldInFront = Compute.isInArc(ce().getPosition(),
                            ce().getSecondaryFacing(), oldTarget.getPosition(),
                            Compute.ARC_FORWARD);
                    boolean curInFront = Compute.isInArc(ce().getPosition(),
                            ce().getSecondaryFacing(), target.getPosition(),
                            Compute.ARC_FORWARD);
                    if (!oldInFront && curInFront) {
                        String title = Messages
                                .getString("FiringDisplay.SecondaryTargetToHitChange.title"); //$NON-NLS-1$
                        String body = Messages
                                .getString("FiringDisplay.SecondaryTargetToHitChange.message"); //$NON-NLS-1$
                        if (!clientgui.doYesNoDialog(title, body)) {
                            return;
                        }
                    }
                }
            }
        }

        // declare searchlight, if possible
        if (GUIPreferences.getInstance().getAutoDeclareSearchlight()) {
            doSearchlight();
        }

        WeaponAttackAction waa;
        if (!mounted.getType().hasFlag(WeaponType.F_ARTILLERY)) {
            waa = new WeaponAttackAction(cen, target.getTargetType(), target
                    .getTargetId(), weaponNum);
        } else {
            waa = new ArtilleryAttackAction(cen, target.getTargetType(), target
                    .getTargetId(), weaponNum, client.game);
        }

        // if this is a space bomb attack, then bring up the payload dialog
        if (mounted.getType().hasFlag(WeaponType.F_SPACE_BOMB)) {
            // if the user cancels, then return
            int[] payload = doSpaceBombing();
            waa.setBombPayload(payload);
        }

        if ((mounted.getLinked() != null)
                && (((WeaponType) mounted.getType()).getAmmoType() != AmmoType.T_NA)) {
            Mounted ammoMount = mounted.getLinked();
            AmmoType ammoType = (AmmoType) ammoMount.getType();
            waa.setAmmoId(ce().getEquipmentNum(ammoMount));
            if (((ammoType.getMunitionType() == AmmoType.M_THUNDER_VIBRABOMB) && ((ammoType
                    .getAmmoType() == AmmoType.T_LRM) || (ammoType.getAmmoType() == AmmoType.T_MML)))
                    || (ammoType.getMunitionType() == AmmoType.M_VIBRABOMB_IV)) {
                VibrabombSettingDialog vsd = new VibrabombSettingDialog(
                        clientgui.frame);
                vsd.setVisible(true);
                waa.setOtherAttackInfo(vsd.getSetting());
            }
        }

        if (ash.allowAimedShotWith(mounted) && ash.inAimingMode()
                && ash.isAimingAtLocation()) {
            waa.setAimedLocation(ash.getAimingAt());
            waa.setAimingMode(ash.getAimingMode());
        } else {
            waa.setAimedLocation(Entity.LOC_NONE);
            waa.setAimingMode(IAimingModes.AIM_MODE_NONE);
        }

        // add the attack to our temporary queue
        attacks.addElement(waa);

        // and add it into the game, temporarily
        client.game.addAction(waa);
        clientgui.minimap.drawMap();

        // set the weapon as used
        mounted.setUsedThisRound(true);

        // find the next available weapon
        int nextWeapon = ce().getNextWeapon(weaponNum);

        // we fired a weapon, can't clear turret jams or weapon jams anymore
        updateClearTurret();
        updateClearWeaponJam();

        // check; if there are no ready weapons, you're done.
        if ((nextWeapon == -1) && GUIPreferences.getInstance().getAutoEndFiring()) {
            ready();
            return;
        }

        // otherwise, display firing info for the next weapon
        clientgui.mechD.wPan.displayMech(ce());
        clientgui.mechD.wPan.selectWeapon(nextWeapon);
        updateTarget();

    }

    /**
     * Skips to the next weapon
     */
    void nextWeapon() {
        int nextWeapon = ce().getNextWeapon(
                clientgui.mechD.wPan.getSelectedWeaponNum());
        // if there's no next weapon, forget about it
        if (nextWeapon == -1) {
            return;
        }
        clientgui.mechD.wPan.displayMech(ce());
        clientgui.mechD.wPan.selectWeapon(nextWeapon);
        updateTarget();
    }

    /**
     * The entity spends the rest of its turn finding a club
     */
    private void findClub() {
        if (ce() == null) {
            return;
        }

        // comfirm this action
        String title = Messages.getString("FiringDisplay.FindClubDialog.title"); //$NON-NLS-1$
        String body = Messages
                .getString("FiringDisplay.FindClubDialog.message"); //$NON-NLS-1$
        if (!clientgui.doYesNoDialog(title, body)) {
            return;
        }

        attacks.removeAllElements();
        attacks.addElement(new FindClubAction(cen));

        ready();
    }

    /**
     * The entity spends the rest of its turn spotting
     */
    private void doSpot() {
        if ((ce() == null) || (target == null)) {
            return;
        }
        if (ce().isINarcedWith(INarcPod.HAYWIRE)) {
            String title = Messages
                    .getString("FiringDisplay.CantSpotDialog.title"); //$NON-NLS-1$
            String body = Messages
                    .getString("FiringDisplay.CantSpotDialog.message"); //$NON-NLS-1$
            clientgui.doAlertDialog(title, body);
            return;
        }
        // comfirm this action
        String title = Messages
                .getString("FiringDisplay.SpotForInderectDialog.title"); //$NON-NLS-1$
        String body = Messages
                .getString("FiringDisplay.SpotForInderectDialog.message"); //$NON-NLS-1$
        if (!clientgui.doYesNoDialog(title, body)) {
            return;
        }
        attacks.addElement(new SpotAction(cen, target.getTargetId()));

    }

    /**
     * Removes all current fire
     */
    private void clearAttacks() {
        // We may not have an entity selected yet (race condition).
        if (ce() == null) {
            return;
        }

        // remove attacks, set weapons available again
        Enumeration<AbstractEntityAction> i = attacks.elements();
        while (i.hasMoreElements()) {
            Object o = i.nextElement();
            if (o instanceof WeaponAttackAction) {
                WeaponAttackAction waa = (WeaponAttackAction) o;
                ce().getEquipment(waa.getWeaponId()).setUsedThisRound(false);
            }
        }
        attacks.removeAllElements();

        // remove temporary attacks from game & board
        removeTempAttacks();

        // restore any other movement to default
        ce().setSecondaryFacing(ce().getFacing());
        ce().setArmsFlipped(false);
    }

    /**
     * Removes temp attacks from the game and board
     */
    private void removeTempAttacks() {
        // remove temporary attacks from game & board
        client.game.removeActionsFor(cen);
        clientgui.bv.removeAttacksFor(ce());
    }

    /**
     * removes the last action
     */
    private void removeLastFiring() {
        Object o = attacks.lastElement();
        if (o instanceof WeaponAttackAction) {
            WeaponAttackAction waa = (WeaponAttackAction) o;
            ce().getEquipment(waa.getWeaponId()).setUsedThisRound(false);
            attacks.removeElement(o);
            clientgui.mechD.wPan.displayMech(ce());
            client.game.removeAction(o);
            clientgui.bv.refreshAttacks();
            clientgui.minimap.drawMap();
        }
    }

    /**
     * Refeshes all displays.
     */
    private void refreshAll() {
        if (ce() == null) {
            return;
        }
        clientgui.bv.redrawEntity(ce());
        clientgui.mechD.displayEntity(ce());
        clientgui.mechD.showPanel("weapons"); //$NON-NLS-1$
        clientgui.mechD.wPan.selectWeapon(ce().getFirstWeapon());
        updateTarget();
    }

    /**
     * Targets something
     */
    void target(Targetable t) {
        target = t;
        ash.setAimingMode();
        updateTarget();
        ash.showDialog();
    }

    /**
     * Targets something
     */
    protected void updateTarget() {
        setFireEnabled(false);

        // make sure we're showing the current entity in the mech display
        if ((ce() != null) && !ce().equals(clientgui.mechD.getCurrentEntity())) {
            clientgui.mechD.displayEntity(ce());
        }

        // allow spotting
        if ((ce() != null) && ce().canSpot() && (target != null)
                && client.game.getOptions().booleanOption("indirect_fire")) { //$NON-NLS-1$)
            setSpotEnabled(true);
        }

        // update target panel
        final int weaponId = clientgui.mechD.wPan.getSelectedWeaponNum();
        if ((target != null) && (target.getPosition() != null) && (weaponId != -1)
                && (ce() != null)) {
            ToHitData toHit;
            if (ash.inAimingMode()) {
                Mounted weapon = ce().getEquipment(weaponId);
                boolean aiming = ash.isAimingAtLocation()
                        && ash.allowAimedShotWith(weapon);
                ash.setEnableAll(aiming);
                if (aiming) {
                    toHit = WeaponAttackAction.toHit(client.game, cen, target,
                            weaponId, ash.getAimingAt(), ash.getAimingMode());
                    clientgui.mechD.wPan.wTargetR.setText(target
                            .getDisplayName()
                            + " (" + ash.getAimingLocation() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
                } else {
                    toHit = WeaponAttackAction.toHit(client.game, cen, target,
                            weaponId, Entity.LOC_NONE,
                            IAimingModes.AIM_MODE_NONE);
                    clientgui.mechD.wPan.wTargetR.setText(target
                            .getDisplayName());
                }
                ash.setPartialCover(toHit.getCover());
            } else {
                toHit = WeaponAttackAction.toHit(client.game, cen, target,
                        weaponId, Entity.LOC_NONE, IAimingModes.AIM_MODE_NONE);
                clientgui.mechD.wPan.wTargetR.setText(target.getDisplayName());
            }
            clientgui.mechD.wPan.wRangeR
                    .setText("" + ce().getPosition().distance(target.getPosition())); //$NON-NLS-1$
            if ((ce() instanceof Aero) && (target instanceof Aero)
                    && client.game.getBoard().inAtmosphere()) {
                // add altitude difference
                int altdiff = Math.abs(ce().getElevation()
                        - target.getElevation());
                clientgui.mechD.wPan.wRangeR
                        .setText("" + ce().getPosition().distance(target.getPosition()) + " + " + altdiff + " altitude"); //$NON-NLS-1$
            }

            Mounted m = ce().getEquipment(weaponId);
            if (m.isUsedThisRound()) {
                clientgui.mechD.wPan.wToHitR.setText(Messages
                        .getString("FiringDisplay.alreadyFired")); //$NON-NLS-1$
                setFireEnabled(false);
            } else if (m.getType().hasFlag(WeaponType.F_AUTO_TARGET)) {
                clientgui.mechD.wPan.wToHitR.setText(Messages
                        .getString("FiringDisplay.autoFiringWeapon")); //$NON-NLS-1$
                setFireEnabled(false);
            } else if (toHit.getValue() == TargetRoll.IMPOSSIBLE) {
                clientgui.mechD.wPan.wToHitR.setText(toHit.getValueAsString());
                setFireEnabled(false);
            } else if (toHit.getValue() == TargetRoll.AUTOMATIC_FAIL) {
                clientgui.mechD.wPan.wToHitR.setText(toHit.getValueAsString());
                setFireEnabled(true);
            } else {
                clientgui.mechD.wPan.wToHitR.setText(toHit.getValueAsString()
                        + " (" + Compute.oddsAbove(toHit.getValue()) + "%)"); //$NON-NLS-1$ //$NON-NLS-2$
                setFireEnabled(true);
            }
            clientgui.mechD.wPan.toHitText.setText(toHit.getDesc());
            setSkipEnabled(true);
        } else {
            clientgui.mechD.wPan.wTargetR.setText("---"); //$NON-NLS-1$
            clientgui.mechD.wPan.wRangeR.setText("---"); //$NON-NLS-1$
            clientgui.mechD.wPan.wToHitR.setText("---"); //$NON-NLS-1$
            clientgui.mechD.wPan.toHitText.setText(""); //$NON-NLS-1$
        }
        updateSearchlight();
    }

    /**
     * Torso twist in the proper direction.
     */
    void torsoTwist(Coords twistTarget) {
        int direction = ce().getFacing();

        if (twistTarget != null) {
            direction = ce().clipSecondaryFacing(
                    ce().getPosition().direction(twistTarget));
        }

        if (direction != ce().getSecondaryFacing()) {
            clearAttacks();
            attacks.addElement(new TorsoTwistAction(cen, direction));
            ce().setSecondaryFacing(direction);
            refreshAll();
        }
    }

    /**
     * Torso twist to the left or right
     *
     * @param twistDir
     *            An <code>int</code> specifying wether we're twisting left or
     *            right, 0 if we're twisting to the left, 1 if to the right.
     */

    void torsoTwist(int twistDir) {
        int direction = ce().getSecondaryFacing();
        if (twistDir == 0) {
            clearAttacks();
            direction = ce().clipSecondaryFacing((direction + 5) % 6);
            attacks.addElement(new TorsoTwistAction(cen, direction));
            ce().setSecondaryFacing(direction);
            refreshAll();
        } else if (twistDir == 1) {
            clearAttacks();
            direction = ce().clipSecondaryFacing((direction + 7) % 6);
            attacks.addElement(new TorsoTwistAction(cen, direction));
            ce().setSecondaryFacing(direction);
            refreshAll();
        }
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

        // ignore buttons other than 1
        if (!client.isMyTurn()
                || ((b.getModifiers() & InputEvent.BUTTON1_MASK) == 0)) {
            return;
        }
        // control pressed means a line of sight check.
        // added ALT_MASK by kenn
        if (((b.getModifiers() & InputEvent.CTRL_MASK) != 0)
                || ((b.getModifiers() & InputEvent.ALT_MASK) != 0)) {
            return;
        }
        // check for shifty goodness
        if (shiftheld != ((b.getModifiers() & InputEvent.SHIFT_MASK) != 0)) {
            shiftheld = (b.getModifiers() & InputEvent.SHIFT_MASK) != 0;
        }

        if (b.getType() == BoardViewEvent.BOARD_HEX_DRAGGED) {
            if (shiftheld || twisting) {
                updateFlipArms(false);
                torsoTwist(b.getCoords());
            }
            clientgui.getBoardView().cursor(b.getCoords());
        } else if (b.getType() == BoardViewEvent.BOARD_HEX_CLICKED) {
            twisting = false;
            clientgui.getBoardView().select(b.getCoords());
        }
    }

    @Override
    public void hexSelected(BoardViewEvent b) {

        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        if (client.isMyTurn() && (b.getCoords() != null) && (ce() != null)
                && !b.getCoords().equals(ce().getPosition())) {
            // HACK : sometimes we don't show the target choice window
            Targetable targ = null;
            if (showTargetChoice) {
                targ = chooseTarget(b.getCoords());
            }
            if (shiftheld) {
                updateFlipArms(false);
                torsoTwist(b.getCoords());
            } else if (targ != null) {
                target(targ);
            }
        }
    }

    //
    // GameListener
    //
    @Override
    public void gameTurnChange(GameTurnChangeEvent e) {

        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        if (client.game.getPhase() == IGame.Phase.PHASE_FIRING) {

            if (client.isMyTurn()) {
                if (cen == Entity.NONE) {
                    beginMyTurn();
                }
                setStatusBarText(Messages
                        .getString("FiringDisplay.its_your_turn")); //$NON-NLS-1$
            } else {
                endMyTurn();
                setStatusBarText(Messages
                        .getString(
                                "FiringDisplay.its_others_turn", new Object[] { e.getPlayer().getName() })); //$NON-NLS-1$
            }
        }
    }

    @Override
    public void gamePhaseChange(GamePhaseChangeEvent e) {

        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        if (client.isMyTurn()
                && (client.game.getPhase() != IGame.Phase.PHASE_FIRING)) {
            endMyTurn();
        }
        // if we're ending the firing phase, unregister stuff.
        if (client.game.getPhase() == IGame.Phase.PHASE_FIRING) {
            setStatusBarText(Messages
                    .getString("FiringDisplay.waitingForFiringPhase")); //$NON-NLS-1$
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
            return;
        }

        if (ev.getSource().equals(butDone)) {
            ready();
        } else if ("viewGameOptions".equalsIgnoreCase(ev.getActionCommand())) { //$NON-NLS-1$
            // Make sure the game options dialog is not editable.
            if (clientgui.getGameOptionsDialog().isEditable()) {
                clientgui.getGameOptionsDialog().setEditable(false);
            }
            // Display the game options dialog.
            clientgui.getGameOptionsDialog().update(client.game.getOptions());
            clientgui.getGameOptionsDialog().setVisible(true);
        } else if (ev.getActionCommand().equals(FIRE_FIRE)) {
            fire();
        } else if (ev.getActionCommand().equals(FIRE_SKIP)) {
            nextWeapon();
        } else if (ev.getActionCommand().equals(FIRE_TWIST)) {
            twisting = true;
        } else if (ev.getActionCommand().equals(FIRE_NEXT)) {
            selectEntity(client.getNextEntityNum(cen));
        } else if (ev.getActionCommand().equals(FIRE_MORE)) {
            buttonLayout++;
            buttonLayout %= NUM_BUTTON_LAYOUTS;
            setupButtonPanel();
        } else if (ev.getActionCommand().equals(FIRE_FIND_CLUB)) {
            findClub();
        } else if (ev.getActionCommand().equals(FIRE_SPOT)) {
            doSpot();
        } else if (ev.getActionCommand().equals(FIRE_NEXT_TARG)) {
            jumpToNextTarget();
        } else if (ev.getActionCommand().equals(FIRE_FLIP_ARMS)) {
            updateFlipArms(!ce().getArmsFlipped());
            // Fire Mode - More Fire Mode button handling - Rasia
        } else if (ev.getActionCommand().equals(FIRE_MODE)) {
            changeMode();
        } else if (("changeSinks".equalsIgnoreCase(ev.getActionCommand()))
                || (ev.getActionCommand().equals(FIRE_CANCEL))) {
            clearAttacks();
            clientgui.getBoardView().select(null);
            clientgui.getBoardView().cursor(null);
            refreshAll();
        } else if (ev.getActionCommand().equals(FIRE_SEARCHLIGHT)) {
            doSearchlight();
        } else if (ev.getActionCommand().equals(FIRE_CLEAR_TURRET)) {
            doClearTurret();
        } else if (ev.getActionCommand().equals(FIRE_CLEAR_WEAPON)) {
            doClearWeaponJam();
        }
    }

    /**
     * update for change of arms-flipping status
     *
     * @param armsFlipped
     */
    void updateFlipArms(boolean armsFlipped) {
        if (ce() == null) {
            return;
        }
        if (armsFlipped == ce().getArmsFlipped()) {
            return;
        }

        twisting = false;

        torsoTwist(null);

        clearAttacks();
        ce().setArmsFlipped(armsFlipped);
        attacks.addElement(new FlipArmsAction(cen, armsFlipped));
        updateTarget();
        refreshAll();
    }

    private void updateSearchlight() {
        setSearchlightEnabled((ce() != null)
                && (target != null)
                && ce().isUsingSpotlight()
                && ce().getCrew().isActive()
                && SearchlightAttackAction.isPossible(client.game, cen, target,
                        null) && !(((Tank) ce()).getStunnedTurns() > 0));
    }

    private void updateClearTurret() {
        setFireClearTurretEnabled((ce() instanceof Tank)
                && ((Tank) ce()).isTurretJammed() && (attacks.size() == 0)
                && !(((Tank) ce()).getStunnedTurns() > 0));
    }

    private void updateClearWeaponJam() {
        setFireClearWeaponJamEnabled((ce() instanceof Tank)
                && (((Tank) ce()).getJammedWeapons().size() != 0)
                && (attacks.size() == 0)
                && !(((Tank) ce()).getStunnedTurns() > 0));
    }

    private void setFireEnabled(boolean enabled) {
        butFire.setEnabled(enabled);
        clientgui.getMenuBar().setFireFireEnabled(enabled);
    }

    private void setTwistEnabled(boolean enabled) {
        butTwist.setEnabled(enabled);
        clientgui.getMenuBar().setFireTwistEnabled(enabled);
    }

    private void setSkipEnabled(boolean enabled) {
        butSkip.setEnabled(enabled);
        clientgui.getMenuBar().setFireSkipEnabled(enabled);
    }

    private void setFindClubEnabled(boolean enabled) {
        butFindClub.setEnabled(enabled);
        clientgui.getMenuBar().setFireFindClubEnabled(enabled);
    }

    private void setNextTargetEnabled(boolean enabled) {
        butNextTarg.setEnabled(enabled);
        clientgui.getMenuBar().setFireNextTargetEnabled(enabled);
    }

    private void setFlipArmsEnabled(boolean enabled) {
        butFlipArms.setEnabled(enabled);
        clientgui.getMenuBar().setFireFlipArmsEnabled(enabled);
    }

    private void setSpotEnabled(boolean enabled) {
        butSpot.setEnabled(enabled);
        clientgui.getMenuBar().setFireSpotEnabled(enabled);
    }

    private void setSearchlightEnabled(boolean enabled) {
        butSearchlight.setEnabled(enabled);
        clientgui.getMenuBar().setFireSearchlightEnabled(enabled);
    }

    private void setFireModeEnabled(boolean enabled) {
        butFireMode.setEnabled(enabled);
        clientgui.getMenuBar().setFireModeEnabled(enabled);
    }

    private void setFireClearTurretEnabled(boolean enabled) {
        butFireClearTurret.setEnabled(enabled);
        clientgui.getMenuBar().setFireClearTurretEnabled(enabled);
    }

    private void setFireClearWeaponJamEnabled(boolean enabled) {
        butFireClearWeaponJam.setEnabled(enabled);
        clientgui.getMenuBar().setFireClearWeaponJamEnabled(enabled);
    }

    private void setNextEnabled(boolean enabled) {
        butNext.setEnabled(enabled);
        clientgui.getMenuBar().setFireNextEnabled(enabled);
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
            clearAttacks();
            clientgui.getBoardView().select(null);
            clientgui.getBoardView().cursor(null);
            refreshAll();
        }
        if ((ev.getKeyCode() == KeyEvent.VK_ENTER) && ev.isControlDown()) {
            if (client.isMyTurn()) {
                //
            }
        }
        if (ev.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
            if (client.isMyTurn()) {
                removeLastFiring();
            }
        }
        if ((ev.getKeyCode() == KeyEvent.VK_SHIFT) && !shiftheld) {
            shiftheld = true;
            if (client.isMyTurn()
                    && (clientgui.getBoardView().getLastCursor() != null)) {
                updateFlipArms(false);
                torsoTwist(clientgui.getBoardView().getLastCursor());
            }
        }
        if ((ev.getKeyCode() == KeyEvent.VK_LEFT) && shiftheld) {
            updateFlipArms(false);
            torsoTwist(0);
        }
        if ((ev.getKeyCode() == KeyEvent.VK_RIGHT) && shiftheld) {
            updateFlipArms(false);
            torsoTwist(1);
        }
    }

    public void keyReleased(KeyEvent ev) {

        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        if ((ev.getKeyCode() == KeyEvent.VK_SHIFT) && shiftheld) {
            shiftheld = false;
        }
    }

    public void keyTyped(KeyEvent ev) {
        // ignore
    }

    //
    // ItemListener
    //
    public void itemStateChanged(ItemEvent ev) {

        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }
    }

    // board view listener
    @Override
    public void finishedMovingUnits(BoardViewEvent b) {

        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        if (client.isMyTurn() && (ce() != null)) {
            clientgui.setDisplayVisible(true);
            clientgui.bv.centerOnHex(ce().getPosition());
        }
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

    public void valueChanged(ListSelectionEvent event) {
        if (event.getSource().equals(clientgui.mechD.wPan.weaponList)) {
            // update target data in weapon display
            updateTarget();
        }
    }

    private class AimedShotHandler implements ActionListener, ItemListener {
        private int aimingAt = Entity.LOC_NONE;

        private int aimingMode = IAimingModes.AIM_MODE_NONE;

        private int partialCover = LosEffects.COVER_NONE;

        private AimedShotDialog asd;

        public AimedShotHandler() {
            // ignore
        }

        public void showDialog() {
            if (asd != null) {
                int oldAimingMode = aimingMode;
                closeDialog();
                aimingMode = oldAimingMode;
            }

            if (inAimingMode()) {
                String[] options;
                boolean[] enabled;

                if (target instanceof Mech) {
                    if (target instanceof BipedMech) {
                        options = BipedMech.LOCATION_NAMES;
                        enabled = createEnabledMask(options.length);
                    } else {
                        options = QuadMech.LOCATION_NAMES;
                        enabled = createEnabledMask(options.length);
                    }
                    if (aimingMode == IAimingModes.AIM_MODE_IMMOBILE) {
                        aimingAt = Mech.LOC_HEAD;
                    } else if (aimingMode == IAimingModes.AIM_MODE_TARG_COMP) {
                        aimingAt = Mech.LOC_CT;
                    }
                } else if (target instanceof Tank) {
                    if (target instanceof LargeSupportTank) {
                        options = LargeSupportTank.LOCATION_NAMES;
                        aimingAt = LargeSupportTank.LOC_FRONT;
                    } else {
                        options = Tank.LOCATION_NAMES;
                        aimingAt = Tank.LOC_FRONT;
                    }
                    enabled = createEnabledMask(options.length);
                } else if (target instanceof GunEmplacement) {
                    options = GunEmplacement.HIT_LOCATION_NAMES;
                    enabled = new boolean[] { true,
                            ((GunEmplacement) target).hasTurret() };
                    aimingAt = GunEmplacement.LOC_BUILDING;
                } else if (target instanceof Protomech) {
                    options = Protomech.LOCATION_NAMES;
                    enabled = createEnabledMask(options.length);
                    aimingAt = Protomech.LOC_TORSO;
                } else if (target instanceof BattleArmor) {
                    options = BattleArmor.IS_LOCATION_NAMES;
                    enabled = createEnabledMask(options.length);
                    aimingAt = BattleArmor.LOC_TROOPER_1;
                } else {
                    return;
                }
                asd = new AimedShotDialog(
                        clientgui.frame,
                        Messages
                                .getString("FiringDisplay.AimedShotDialog.title"), //$NON-NLS-1$
                        Messages
                                .getString("FiringDisplay.AimedShotDialog.message"), //$NON-NLS-1$
                        options, enabled, aimingAt, this, this);

                asd.setVisible(true);
                updateTarget();
            }
        }

        private boolean[] createEnabledMask(int length) {
            boolean[] mask = new boolean[length];

            for (int i = 0; i < length; i++) {
                mask[i] = true;
            }

            int side = Compute.targetSideTable(ce(), target);

            // on a tank, remove turret if its missing
            // also, remove body
            if (target instanceof Tank) {
                mask[Tank.LOC_BODY] = false;
                Tank tank = (Tank) target;
                if (tank.hasNoTurret()) {
                    mask[Tank.LOC_TURRET] = false;
                }
                // remove non-visible sides
                if (target instanceof LargeSupportTank) {
                    if (side == ToHitData.SIDE_FRONTLEFT) {
                        mask[LargeSupportTank.LOC_FRONTRIGHT] = false;
                        mask[LargeSupportTank.LOC_REARRIGHT] = false;
                        mask[LargeSupportTank.LOC_REAR] = false;
                    }
                    if (side == ToHitData.SIDE_FRONTRIGHT) {
                        mask[LargeSupportTank.LOC_FRONTLEFT] = false;
                        mask[LargeSupportTank.LOC_REARLEFT] = false;
                        mask[LargeSupportTank.LOC_REAR] = false;
                    }
                    if (side == ToHitData.SIDE_REARRIGHT) {
                        mask[LargeSupportTank.LOC_FRONTLEFT] = false;
                        mask[LargeSupportTank.LOC_REARLEFT] = false;
                        mask[LargeSupportTank.LOC_FRONT] = false;
                    }
                    if (side == ToHitData.SIDE_REARLEFT) {
                        mask[LargeSupportTank.LOC_REARRIGHT] = false;
                        mask[LargeSupportTank.LOC_FRONTRIGHT] = false;
                        mask[LargeSupportTank.LOC_FRONT] = false;
                    }
                } else {
                    if (side == ToHitData.SIDE_LEFT) {
                        mask[Tank.LOC_RIGHT] = false;
                    }
                    if (side == ToHitData.SIDE_RIGHT) {
                        mask[Tank.LOC_LEFT] = false;
                    }
                    if (side == ToHitData.SIDE_REAR) {
                        mask[Tank.LOC_FRONT] = false;
                    }
                    if (side == ToHitData.SIDE_FRONT) {
                        mask[Tank.LOC_REAR] = false;
                    }
                }
            }

            // remove main gun on protos that don't have one
            if (target instanceof Protomech) {
                if (!((Protomech) target).hasMainGun()) {
                    mask[Protomech.LOC_MAINGUN] = false;
                }
            }

            // remove squad location on BAs
            // also remove dead troopers
            if (target instanceof BattleArmor) {
                mask[BattleArmor.LOC_SQUAD] = false;
            }

            // remove locations hidden by partial cover
            if ((partialCover & LosEffects.COVER_HORIZONTAL) != 0) {
                mask[Mech.LOC_LLEG] = false;
                mask[Mech.LOC_RLEG] = false;
            }
            if (side == ToHitData.SIDE_FRONT) {
                if ((partialCover & LosEffects.COVER_LOWLEFT) != 0) {
                    mask[Mech.LOC_RLEG] = false;
                }
                if ((partialCover & LosEffects.COVER_LOWRIGHT) != 0) {
                    mask[Mech.LOC_LLEG] = false;
                }
                if ((partialCover & LosEffects.COVER_LEFT) != 0) {
                    mask[Mech.LOC_RARM] = false;
                    mask[Mech.LOC_RT] = false;
                }
                if ((partialCover & LosEffects.COVER_RIGHT) != 0) {
                    mask[Mech.LOC_LARM] = false;
                    mask[Mech.LOC_LT] = false;
                }
            } else {
                if ((partialCover & LosEffects.COVER_LOWLEFT) != 0) {
                    mask[Mech.LOC_LLEG] = false;
                }
                if ((partialCover & LosEffects.COVER_LOWRIGHT) != 0) {
                    mask[Mech.LOC_RLEG] = false;
                }
                if ((partialCover & LosEffects.COVER_LEFT) != 0) {
                    mask[Mech.LOC_LARM] = false;
                    mask[Mech.LOC_LT] = false;
                }
                if ((partialCover & LosEffects.COVER_RIGHT) != 0) {
                    mask[Mech.LOC_RARM] = false;
                    mask[Mech.LOC_RT] = false;
                }
            }

            if (aimingMode == IAimingModes.AIM_MODE_TARG_COMP) {
                // Can't target head with targeting computer.
                mask[Mech.LOC_HEAD] = false;
            }
            return mask;
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

        /**
         * Enables the radiobuttons in the dialog.
         */
        public void setEnableAll(boolean enableAll) {
            if (asd != null) {
                asd.setEnableAll(enableAll);
            }
        }

        public void setPartialCover(int partialCover) {
            this.partialCover = partialCover;
        }

        public int getAimingAt() {
            return aimingAt;
        }

        public int getAimingMode() {
            return aimingMode;
        }

        /**
         * Returns the name of aimed location.
         */
        public String getAimingLocation() {
            if ((target != null) && (aimingAt != Entity.LOC_NONE)
                    && (aimingMode != IAimingModes.AIM_MODE_NONE)) {
                if (target instanceof BipedMech) {
                    return BipedMech.LOCATION_NAMES[aimingAt];
                } else if (target instanceof BipedMech) {
                    return QuadMech.LOCATION_NAMES[aimingAt];
                } else if (target instanceof GunEmplacement) {
                    return GunEmplacement.HIT_LOCATION_NAMES[aimingAt];
                }
            }
            return null;
        }

        /**
         * Sets the aiming mode, depending on the target and the attacker.
         * Against immobile mechs, targeting computer aiming mode will be used
         * if turned on. (This is a hack, but it's the resolution suggested by
         * the bug submitter, and I don't think it's half bad.
         */

        public void setAimingMode() {
            boolean allowAim;

            // TC against a mech
            allowAim = ((target != null) && ce().hasAimModeTargComp() && (target instanceof Mech));
            if (allowAim) {
                aimingMode = IAimingModes.AIM_MODE_TARG_COMP;
                return;
            }
            // immobile mech or gun emplacement
            allowAim = ((target != null) && ((target.isImmobile() && ((target instanceof Mech) || (target instanceof Tank))) || (target instanceof GunEmplacement)));
            if (allowAim) {
                aimingMode = IAimingModes.AIM_MODE_IMMOBILE;
                return;
            }
            aimingMode = IAimingModes.AIM_MODE_NONE;
        }

        /**
         * @return if are we in aiming mode
         */
        public boolean inAimingMode() {
            return aimingMode != IAimingModes.AIM_MODE_NONE;
        }

        /**
         * @return if a hit location currently selected.
         */
        public boolean isAimingAtLocation() {
            return aimingAt != Entity.LOC_NONE;
        }

        /**
         * should aimned shoots be allowed with the passed weapon
         *
         * @param weapon
         * @return
         */
        public boolean allowAimedShotWith(Mounted weapon) {
            WeaponType wtype = (WeaponType) weapon.getType();
            boolean isWeaponInfantry = wtype.hasFlag(WeaponType.F_INFANTRY);
            boolean usesAmmo = (wtype.getAmmoType() != AmmoType.T_NA)
                    && !isWeaponInfantry;
            Mounted ammo = usesAmmo ? weapon.getLinked() : null;
            AmmoType atype = ammo == null ? null : (AmmoType) ammo.getType();

            // Leg and swarm attacks can't be aimed.
            if (wtype.getInternalName().equals(Infantry.LEG_ATTACK)
                    || wtype.getInternalName().equals(Infantry.SWARM_MEK)) {
                return false;
            }
            switch (aimingMode) {
            case (IAimingModes.AIM_MODE_NONE):
                return false;
            case (IAimingModes.AIM_MODE_IMMOBILE):
                if (atype != null) {
                    switch (atype.getAmmoType()) {
                    case (AmmoType.T_SRM_STREAK):
                    case (AmmoType.T_LRM_STREAK):
                    case (AmmoType.T_LRM):
                    case (AmmoType.T_LRM_TORPEDO):
                    case (AmmoType.T_SRM):
                    case (AmmoType.T_SRM_TORPEDO):
                    case (AmmoType.T_MRM):
                    case (AmmoType.T_NARC):
                    case (AmmoType.T_AMS):
                    case (AmmoType.T_ARROW_IV):
                    case (AmmoType.T_LONG_TOM):
                    case (AmmoType.T_SNIPER):
                    case (AmmoType.T_THUMPER):
                    case (AmmoType.T_SRM_ADVANCED):
                    case (AmmoType.T_LRM_TORPEDO_COMBO):
                    case (AmmoType.T_ATM):
                    case (AmmoType.T_MML):
                    case (AmmoType.T_EXLRM):
                    case AmmoType.T_TBOLT_5:
                    case AmmoType.T_TBOLT_10:
                    case AmmoType.T_TBOLT_15:
                    case AmmoType.T_TBOLT_20:
                    case AmmoType.T_PXLRM:
                    case AmmoType.T_HSRM:
                    case AmmoType.T_MRM_STREAK:
                        return false;
                    }
                    if (((atype.getAmmoType() == AmmoType.T_AC_LBX) || (atype
                            .getAmmoType() == AmmoType.T_AC_LBX_THB))
                            && (atype.getMunitionType() == AmmoType.M_CLUSTER)) {
                        return false;
                    }
                }
                break;
            case (IAimingModes.AIM_MODE_TARG_COMP):
                if (!wtype.hasFlag(WeaponType.F_DIRECT_FIRE)
                        || wtype.hasFlag(WeaponType.F_PULSE)) {
                    return false;
                }
                if (weapon.getCurrentShots() > 1) {
                    return false;
                }
                if ((atype != null)
                        && ((atype.getAmmoType() == AmmoType.T_AC_LBX) || (atype
                                .getAmmoType() == AmmoType.T_AC_LBX_THB))
                        && (atype.getMunitionType() == AmmoType.M_CLUSTER)) {
                    return false;
                }
                break;
            }
            return true;
        }

        /**
         * ActionListener, listens to the button in the dialog.
         */
        public void actionPerformed(ActionEvent ev) {
            closeDialog();
        }

        /**
         * ItemListener, listens to the radiobuttons in the dialog.
         */
        public void itemStateChanged(ItemEvent ev) {
            IndexedCheckbox icb = (IndexedCheckbox) ev.getSource();
            aimingAt = icb.getIndex();
            updateTarget();
        }
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

    /**
     * Stop just ignoring events and actually stop listening to them.
     */
    public void removeAllListeners() {
        client.game.removeGameListener(this);
        clientgui.getBoardView().removeBoardViewListener(this);
        clientgui.mechD.wPan.weaponList.removeListSelectionListener(this);
    }

    /**
     * Have the player select a target from the entities at the given coords.
     *
     * @param pos
     *            - the <code>Coords</code> containing targets.
     */
    private Targetable chooseTarget(Coords pos) {

        boolean friendlyFire = client.game.getOptions().booleanOption(
                "friendly_fire"); //$NON-NLS-1$
        // Assume that we have *no* choice.
        Targetable choice = null;
        Enumeration<Entity> choices;

        // Get the available choices, depending on friendly fire
        if (friendlyFire) {
            choices = client.game.getEntities(pos);
        } else {
            choices = client.game.getEnemyEntities(pos, ce());
        }

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
                            .getString("FiringDisplay.ChooseTargetDialog.title"), //$NON-NLS-1$
                    Messages
                            .getString(
                                    "FiringDisplay.ChooseTargetDialog.message", new Object[] { pos.getBoardNum() }), //$NON-NLS-1$
                    names);
            choiceDialog.setVisible(true);
            if (choiceDialog.getAnswer()) {
                choice = targets.elementAt(choiceDialog.getChoice());
            }
        } // End have-choices

        // Return the chosen unit.
        return choice;

    } // End private Targetable chooseTarget( Coords )

}
