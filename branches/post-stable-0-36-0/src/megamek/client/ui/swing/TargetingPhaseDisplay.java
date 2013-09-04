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

package megamek.client.ui.swing;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import megamek.client.event.BoardViewEvent;
import megamek.client.ui.Messages;
import megamek.client.ui.SharedUtility;
import megamek.common.AmmoType;
import megamek.common.Building;
import megamek.common.BuildingTarget;
import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.Dropship;
import megamek.common.Entity;
import megamek.common.GameTurn;
import megamek.common.HexTarget;
import megamek.common.IGame;
import megamek.common.Mounted;
import megamek.common.TargetRoll;
import megamek.common.Targetable;
import megamek.common.ToHitData;
import megamek.common.WeaponType;
import megamek.common.actions.ArtilleryAttackAction;
import megamek.common.actions.EntityAction;
import megamek.common.actions.FlipArmsAction;
import megamek.common.actions.SearchlightAttackAction;
import megamek.common.actions.TorsoTwistAction;
import megamek.common.actions.TriggerAPPodAction;
import megamek.common.actions.TriggerBPodAction;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.event.GameTurnChangeEvent;
import megamek.common.weapons.ArtilleryWeapon;

/*
 * Targeting Phase Display. Breaks naming convention because TargetingDisplay is too easy to confuse
 * with something else
 */

public class TargetingPhaseDisplay extends StatusBarPhaseDisplay implements
        KeyListener, ItemListener, ListSelectionListener {
    /**
     *
     */
    private static final long serialVersionUID = 3441669419807288865L;

    // Action command names
    private static final String FIRE_FIRE = "fireFire"; //$NON-NLS-1$
    private static final String FIRE_MODE = "fireMode"; //$NON-NLS-1$
    private static final String FIRE_FLIP_ARMS = "fireFlipArms"; //$NON-NLS-1$
    private static final String FIRE_NEXT = "fireNext"; //$NON-NLS-1$
    private static final String FIRE_NEXT_TARG = "fireNextTarg"; //$NON-NLS-1$
    private static final String FIRE_SKIP = "fireSkip"; //$NON-NLS-1$
    private static final String FIRE_TWIST = "fireTwist"; //$NON-NLS-1$
    private static final String FIRE_CANCEL = "fireCancel"; //$NON-NLS-1$
    private static final String FIRE_SEARCHLIGHT = "fireSearchlight"; //$NON-NLS-1$

    // buttons
    private JComponent panButtons;

    private JButton butFire;

    private JButton butTwist;

    private JButton butSkip;

    private JButton butFlipArms;

    private JButton butFireMode;

    private JButton butSpace;

    private JButton butNext;

    private JButton butNextTarg;

    private JButton butSearchlight;

    // let's keep track of what we're shooting and at what, too
    private int cen = Entity.NONE; // current entity number

    private Targetable target; // target

    // shots we have so far.
    private Vector<EntityAction> attacks;

    // is the shift key held?
    private boolean shiftheld;

    private boolean twisting;

    private final IGame.Phase phase;

    private Entity[] visibleTargets;

    private int lastTargetID = -1;

    /**
     * Creates and lays out a new targeting phase display for the specified
     * clientgui.getClient().
     */
    public TargetingPhaseDisplay(ClientGUI clientgui, boolean offboard) {
        this.clientgui = clientgui;
        phase = offboard ? IGame.Phase.PHASE_OFFBOARD
                : IGame.Phase.PHASE_TARGETING;
        shiftheld = false;

        // fire
        attacks = new Vector<EntityAction>();

        setupStatusBar(Messages
                .getString("TargetingPhaseDisplay.waitingForTargetingPhase")); //$NON-NLS-1$

        butFire = new JButton(Messages.getString("TargetingPhaseDisplay.Fire")); //$NON-NLS-1$
        butFire.addActionListener(this);
        butFire.setActionCommand(FIRE_FIRE);
        butFire.setEnabled(false);

        butSkip = new JButton(Messages.getString("TargetingPhaseDisplay.Skip")); //$NON-NLS-1$
        butSkip.addActionListener(this);
        butSkip.setActionCommand(FIRE_SKIP);
        butSkip.setEnabled(false);

        butTwist = new JButton(Messages
                .getString("TargetingPhaseDisplay.Twist")); //$NON-NLS-1$
        butTwist.addActionListener(this);
        butTwist.setActionCommand(FIRE_TWIST);
        butTwist.setEnabled(false);

        butFlipArms = new JButton(Messages
                .getString("TargetingPhaseDisplay.FlipArms")); //$NON-NLS-1$
        butFlipArms.addActionListener(this);
        butFlipArms.setActionCommand(FIRE_FLIP_ARMS);
        butFlipArms.setEnabled(false);

        butFireMode = new JButton(Messages
                .getString("TargetingPhaseDisplay.Mode")); //$NON-NLS-1$
        butFireMode.addActionListener(this);
        butFireMode.setActionCommand(FIRE_MODE);
        butFireMode.setEnabled(false);

        butNextTarg = new JButton(Messages
                .getString("FiringDisplay.NextTarget")); //$NON-NLS-1$
        butNextTarg.addActionListener(this);
        butNextTarg.addKeyListener(this);
        butNextTarg.setActionCommand(FIRE_NEXT_TARG);
        butNextTarg.setEnabled(false);

        butSearchlight = new JButton(Messages
                .getString("FiringDisplay.Searchlight")); //$NON-NLS-1$
        butSearchlight.addActionListener(this);
        butSearchlight.addKeyListener(this);
        butSearchlight.setActionCommand(FIRE_SEARCHLIGHT);
        butSearchlight.setEnabled(false);

        butSpace = new JButton(""); //$NON-NLS-1$
        butSpace.setEnabled(false);
        butSpace.setVisible(false);

        butDone.setText(Messages.getString("TargetingPhaseDisplay.Done")); //$NON-NLS-1$
        butDone.setEnabled(false);

        butNext = new JButton(Messages
                .getString("TargetingPhaseDisplay.NextUnit")); //$NON-NLS-1$
        butNext.addActionListener(this);
        butNext.setActionCommand(FIRE_NEXT);
        butNext.setEnabled(false);

        // layout button grid
        panButtons = new JPanel();
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

    }

    /**
     * Have the panel register itself as a listener wherever it's needed.
     * <p/>
     * According to
     * http://www-106.ibm.com/developerworks/java/library/j-jtp0618.html it is a
     * major bad no-no to perform these registrations before the constructor
     * finishes, so this function has to be called after the panel is created.
     * Please note, this restriction only applies to listeners for objects that
     * aren't on the panel itself.
     */
    public void initializeListeners() {

        clientgui.getClient().game.addGameListener(this);
        clientgui.getBoardView().addBoardViewListener(this);

        clientgui.bv.addKeyListener(this);
        addKeyListener(this);

        // mech display.
        clientgui.mechD.wPan.weaponList.addListSelectionListener(this);
        clientgui.mechD.wPan.weaponList.addKeyListener(this);
    }

    private void addBag(JComponent comp, GridBagLayout gridbag,
            GridBagConstraints c) {
        gridbag.setConstraints(comp, c);
        add(comp);
        comp.addKeyListener(this);
    }

    private void setupButtonPanel() {
        panButtons.removeAll();
        panButtons.setLayout(new GridLayout(1, 5));

        panButtons.add(butNext);
        panButtons.add(butFire);
        panButtons.add(butSkip);
        panButtons.add(butNextTarg);
        panButtons.add(butFlipArms);
        panButtons.add(butTwist);
        panButtons.add(butFireMode);
        panButtons.add(butSearchlight);
        panButtons.add(butDone);

        validate();
    }

    /**
     * Selects an entity, by number, for movement.
     */
    private void selectEntity(int en) {
        // clear any previously considered attacks
        if (en != cen) {
            clearAttacks();
            refreshAll();
        }

        if (clientgui.getClient().game.getEntity(en) != null) {

            cen = en;
            clientgui.setSelectedEntityNum(en);

            // If the selected entity is not on the board, use the next one.
            // ASSUMPTION: there will always be *at least one* entity on map.
            if (null == ce().getPosition()) {

                // Walk through the list of entities for this player.
                for (int nextId = clientgui.getClient().getNextEntityNum(en); nextId != en; nextId = clientgui.getClient()
                        .getNextEntityNum(nextId)) {

                    if (null != clientgui.getClient().game.getEntity(nextId).getPosition()) {
                        cen = nextId;
                        break;
                    }

                } // Check the player's next entity.

                // We were *supposed* to have found an on-board entity.
                if (null == ce().getPosition()) {
                    System.err
                            .println("FiringDisplay: could not find an on-board entity: " + //$NON-NLS-1$
                                    en);
                    return;
                }

            } // End ce()-not-on-board

            target(null);
            clientgui.getBoardView().highlight(ce().getPosition());
            clientgui.getBoardView().select(null);
            clientgui.getBoardView().cursor(null);

            refreshAll();
            cacheVisibleTargets();

            if (!clientgui.bv.isMovingUnits()) {
                clientgui.bv.centerOnHex(ce().getPosition());
            }

            // Update the menu bar.
            clientgui.getMenuBar().setEntity(ce());

            // 2003-12-29, nemchenk -- only twist if crew conscious
            setTwistEnabled(ce().canChangeSecondaryFacing()
                    && ce().getCrew().isActive());
            setFlipArmsEnabled(ce().canFlipArms());
            updateSearchlight();

            setFireModeEnabled(true);
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


        if (!clientgui.bv.isMovingUnits()) {
            clientgui.setDisplayVisible(true);
        }

        selectEntity(clientgui.getClient().getFirstEntityNum());

        GameTurn turn = clientgui.getClient().getMyTurn();
        // There's special processing for triggering AP Pods.
        if ((turn instanceof GameTurn.TriggerAPPodTurn) && (null != ce())) {
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
            clientgui.getBoardView().select(null);
        }
    }

    /**
     * Does end turn stuff.
     */
    private void endMyTurn() {
        // end my turn, then.
        Entity next = clientgui.getClient().game.getNextEntity(clientgui.getClient().game.getTurnIndex());
        if ((phase == clientgui.getClient().game.getPhase()) && (null != next) && (null != ce())
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
        setFireEnabled(false);
        setSkipEnabled(false);
        setTwistEnabled(false);
        setNextEnabled(false);
        butDone.setEnabled(false);
        setFlipArmsEnabled(false);
        setFireModeEnabled(false);
        setNextTargetEnabled(false);
    }

    /**
     * Fire Mode - Adds a Fire Mode Change to the current Attack Action
     */
    private void changeMode() {
        int wn = clientgui.mechD.wPan.getSelectedWeaponNum();

        // Do nothing we have no unit selected.
        if (null == ce()) {
            return;
        }

        // If the weapon does not have modes, just exit.
        Mounted m = ce().getEquipment(wn);
        if ((m == null) || !m.getType().hasModes()) {
            return;
        }


        // Dropship Artillery cannot be switched to "Direct" Fire
        final WeaponType wtype = (WeaponType) m.getType();
        if ((ce() instanceof Dropship) && (wtype instanceof ArtilleryWeapon)) {
            return;
        }

        // send change to the server
        int nMode = m.switchMode();
        clientgui.getClient().sendModeChange(cen, wn, nMode);

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
     * Called when the current entity is done firing. Send out our attack queue
     * to the server.
     */
    @Override
    public void ready() {
        if (attacks.isEmpty()
                && GUIPreferences.getInstance().getNagForNoAction()) {
            // comfirm this action
            String title = Messages
                    .getString("TargetingPhaseDisplay.DontFireDialog.title"); //$NON-NLS-1$
            String body = Messages
                    .getString("TargetingPhaseDisplay.DontFireDialog.message"); //$NON-NLS-1$
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

        // send out attacks
        clientgui.getClient().sendAttackData(cen, attacks);

        // clear queue
        attacks.removeAllElements();

        // Clear the menu bar.
        clientgui.getMenuBar().setEntity(null);

        endMyTurn();
    }

    private void doSearchlight() {
        // validate
        if ((ce() == null) || (target == null)) {
            throw new IllegalArgumentException(
                    "current searchlight parameters are invalid"); //$NON-NLS-1$
        }

        if (!SearchlightAttackAction.isPossible(clientgui.getClient().game, cen, target, null)) {
            return;
        }

        // create and queue a searchlight action
        SearchlightAttackAction saa = new SearchlightAttackAction(cen, target
                .getTargetType(), target.getTargetId());
        attacks.addElement(saa);

        // and add it into the game, temporarily
        clientgui.getClient().game.addAction(saa);
        clientgui.bv.addAttack(saa);
        clientgui.minimap.drawMap();

        // refresh weapon panel, as bth will have changed
        updateTarget();
    }

    /**
     * Adds a weapon attack with the currently selected weapon to the attack
     * queue.
     */
    private void fire() {
        // get the selected weaponnum
        int weaponNum = clientgui.mechD.wPan.getSelectedWeaponNum();
        Mounted mounted = ce().getEquipment(weaponNum);

        // validate
        if ((ce() == null) || (target == null) || (mounted == null)
                || !(mounted.getType() instanceof WeaponType)) {
            throw new IllegalArgumentException(
                    "current fire parameters are invalid"); //$NON-NLS-1$
        }

        // declare searchlight, if possible
        if (GUIPreferences.getInstance().getAutoDeclareSearchlight()) {
            doSearchlight();
        }

        WeaponAttackAction waa = new WeaponAttackAction(cen, target
                .getTargetType(), target.getTargetId(), weaponNum);
        if (mounted.getType().hasFlag(WeaponType.F_ARTILLERY)) {
            waa = new ArtilleryAttackAction(cen, target.getTargetType(), target
                    .getTargetId(), weaponNum, clientgui.getClient().game);
        }
        if ((null != mounted.getLinked())
                && (((WeaponType) mounted.getType()).getAmmoType() != AmmoType.T_NA)) {
            Mounted ammoMount = mounted.getLinked();
            waa.setAmmoId(ce().getEquipmentNum(ammoMount));
            if (((AmmoType) ammoMount.getType()).getMunitionType() == AmmoType.M_VIBRABOMB_IV) {
                VibrabombSettingDialog vsd = new VibrabombSettingDialog(
                        clientgui.frame);
                vsd.setVisible(true);
                waa.setOtherAttackInfo(vsd.getSetting());
            }
        }

        // add the attack to our temporary queue
        attacks.addElement(waa);

        // and add it into the game, temporarily
        clientgui.getClient().game.addAction(waa);
        clientgui.minimap.drawMap();

        // set the weapon as used
        mounted.setUsedThisRound(true);

        // find the next available weapon
        int nextWeapon = ce().getNextWeapon(weaponNum);

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
    private void nextWeapon() {
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
     * Removes all current fire
     */
    private void clearAttacks() {
        // We may not have an entity selected yet (race condition).
        if (ce() == null) {
            return;
        }

        // remove attacks, set weapons available again
        Enumeration<EntityAction> i = attacks.elements();
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
        clientgui.getClient().game.removeActionsFor(cen);
        clientgui.bv.removeAttacksFor(ce());

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
        updateTarget();

    }

    /**
     * Targets something
     */
    protected void updateTarget() {
        setFireEnabled(false);

        // update target panel
        final int weaponId = clientgui.mechD.wPan.getSelectedWeaponNum();
        if ((target != null) && (weaponId != -1)) {
            ToHitData toHit;

            toHit = WeaponAttackAction.toHit(clientgui.getClient().game, cen, target,
                    weaponId, Entity.LOC_NONE, 0);
            clientgui.mechD.wPan.wTargetR.setText(target.getDisplayName());

            clientgui.mechD.wPan.wRangeR
                    .setText("" + ce().getPosition().distance(target.getPosition())); //$NON-NLS-1$
            Mounted m = ce().getEquipment(weaponId);
            if (m.isUsedThisRound()) {
                clientgui.mechD.wPan.wToHitR.setText(Messages
                        .getString("TargetingPhaseDisplay.alreadyFired")); //$NON-NLS-1$
                setFireEnabled(false);
            } else if (m.getType().hasFlag(WeaponType.F_AUTO_TARGET)) {
                clientgui.mechD.wPan.wToHitR.setText(Messages
                        .getString("TargetingPhaseDisplay.autoFiringWeapon")); //$NON-NLS-1$
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
    void torsoTwist(Coords cTarget) {
        int direction = ce().getFacing();

        if (null != cTarget) {
            direction = ce().clipSecondaryFacing(
                    ce().getPosition().direction(cTarget));
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
     * @param twistDirection
     *            An <code>int</code> specifying wether we're twisting left or
     *            right, 0 if we're twisting to the left, 1 if to the right.
     */

    void torsoTwist(int twistDirection) {
        int direction = ce().getSecondaryFacing();
        if (twistDirection == 0) {
            clearAttacks();
            direction = ce().clipSecondaryFacing((direction + 5) % 6);
            attacks.addElement(new TorsoTwistAction(cen, direction));
            ce().setSecondaryFacing(direction);
            refreshAll();
        } else if (twistDirection == 1) {
            clearAttacks();
            direction = ce().clipSecondaryFacing((direction + 7) % 6);
            attacks.addElement(new TorsoTwistAction(cen, direction));
            ce().setSecondaryFacing(direction);
            refreshAll();
        }
    }

    /**
     * Cache the list of visible targets. This is used for the 'next target'
     * button.
     * <p/>
     * We'll sort it by range to us.
     */
    private void cacheVisibleTargets() {
        clearVisibleTargets();

        Vector<Entity> vec = clientgui.getClient().game.getValidTargets(ce());
        Comparator<Entity> sortComp = new Comparator<Entity>() {
            public int compare(Entity x, Entity y) {

                int rangeToX = ce().getPosition().distance(x.getPosition());
                int rangeToY = ce().getPosition().distance(y.getPosition());

                if (rangeToX == rangeToY) {
                    return x.getId() < y.getId() ? -1 : 1;
                }

                return rangeToX < rangeToY ? -1 : 1;
            }
        };

        TreeSet<Entity> tree = new TreeSet<Entity>(sortComp);
        visibleTargets = new Entity[vec.size()];

        for (int i = 0; i < vec.size(); i++) {
            tree.add(vec.elementAt(i));
        }

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
        if (null == visibleTargets) {
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

        if (null == targ) {
            return;
        }

        // HACK : don't show the choice dialog.

        clientgui.bv.centerOnHex(targ.getPosition());
        clientgui.getBoardView().select(targ.getPosition());

        // HACK : show the choice dialog again.
        target(targ);
    }

    /**
     * Returns the current entity.
     */
    Entity ce() {
        return clientgui.getClient().game.getEntity(cen);
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
        if (!clientgui.getClient().isMyTurn()
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

        if (clientgui.getClient().isMyTurn() && (b.getCoords() != null) && (ce() != null)
                && !b.getCoords().equals(ce().getPosition())) {
            boolean friendlyFire = clientgui.getClient().game.getOptions().booleanOption(
                    "friendly_fire"); //$NON-NLS-1$
            if (shiftheld) {
                updateFlipArms(false);
                torsoTwist(b.getCoords());
            } else if (phase == IGame.Phase.PHASE_TARGETING) {
                target(new HexTarget(b.getCoords(), ce().getGame().getBoard(),
                        Targetable.TYPE_HEX_ARTILLERY));
            } else if (friendlyFire
                    && (clientgui.getClient().game.getFirstEntity(b.getCoords()) != null)) {
                target(clientgui.getClient().game.getFirstEntity(b.getCoords()));
            } else if (clientgui.getClient().game.getFirstEnemyEntity(b.getCoords(), ce()) != null) {
                target(clientgui.getClient().game.getFirstEnemyEntity(b.getCoords(), ce()));
            } else if (ce().hasTAG() && phase == IGame.Phase.PHASE_OFFBOARD){
                target(chooseTarget(b.getCoords()));
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

        boolean friendlyFire = clientgui.getClient().game.getOptions()
                .booleanOption("friendly_fire"); //$NON-NLS-1$
        // Assume that we have *no* choice.
        Targetable choice = null;
        Enumeration<Entity> choices;

        // Get the available choices, depending on friendly fire
        if (friendlyFire) {
            choices = clientgui.getClient().game.getEntities(pos);
        } else {
            choices = clientgui.getClient().game.getEnemyEntities(pos, ce());
        }

        // Convert the choices into a List of targets.
        List<Targetable> targets = new ArrayList<Targetable>();
        while (choices.hasMoreElements()) {
            choice = choices.nextElement();
            if (!ce().equals(choice)) {
                targets.add(choice);
            }
        }

        // Is there a building in the hex?
        Building bldg = clientgui.getClient().game.getBoard()
                .getBuildingAt(pos);
        if (bldg != null) {
            targets.add(new BuildingTarget(pos, clientgui.getClient().game
                    .getBoard(), Targetable.TYPE_BLDG_TAG));
        }
        
        targets.add(new HexTarget(pos, clientgui.getClient().game.getBoard(),
                Targetable.TYPE_HEX_TAG));

        // Do we have a single choice?
        if (targets.size() == 1) {
            // Return that choice.
            choice = targets.get(0);
        }

        // If we have multiple choices, display a selection dialog.
        else if (targets.size() > 1) {
            String input = (String) JOptionPane
                    .showInputDialog(
                            clientgui,
                            Messages.getString(
                                    "FiringDisplay.ChooseTargetDialog.message", //$NON-NLS-1$ 
                                    new Object[] { pos.getBoardNum() }),
                            Messages.getString("FiringDisplay.ChooseTargetDialog.title"), //$NON-NLS-1$
                            JOptionPane.QUESTION_MESSAGE, null, SharedUtility
                                    .getDisplayArray(targets), null);
            choice = SharedUtility.getTargetPicked(targets, input);
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

        if (clientgui.getClient().game.getPhase() == phase) {

            if (clientgui.getClient().isMyTurn()) {
                if (cen == Entity.NONE) {
                    beginMyTurn();
                }
                setStatusBarText(Messages
                        .getString("TargetingPhaseDisplay.its_your_turn")); //$NON-NLS-1$
            } else {
                endMyTurn();
                if (e.getPlayer() != null) {
                    setStatusBarText(Messages
                            .getString(
                                    "TargetingPhaseDisplay.its_others_turn", new Object[] { e.getPlayer().getName() })); //$NON-NLS-1$
                }

            }
        }
    }

    @Override
    public void gamePhaseChange(GamePhaseChangeEvent e) {

        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        if (clientgui.getClient().isMyTurn() && (clientgui.getClient().game.getPhase() != phase)) {
            endMyTurn();
        }
        // if we're ending the firing phase, unregister stuff.
        if (clientgui.getClient().game.getPhase() == phase) {
            setStatusBarText(Messages
                    .getString("TargetingPhaseDisplay.waitingForFiringPhase")); //$NON-NLS-1$
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

        if (statusBarActionPerformed(ev, clientgui.getClient())) {
            return;
        }

        if (!clientgui.getClient().isMyTurn()) {
            return;
        }

        if (ev.getActionCommand().equalsIgnoreCase("viewGameOptions")) { //$NON-NLS-1$
            // Make sure the game options dialog is not editable.
            if (clientgui.getGameOptionsDialog().isEditable()) {
                clientgui.getGameOptionsDialog().setEditable(false);
            }
            // Display the game options dialog.
            clientgui.getGameOptionsDialog().update(clientgui.getClient().game.getOptions());
            clientgui.getGameOptionsDialog().setVisible(true);
        } else if (ev.getActionCommand().equals(FIRE_FIRE)) {
            fire();
        } else if (ev.getActionCommand().equals(FIRE_SKIP)) {
            nextWeapon();
        } else if (ev.getActionCommand().equals(FIRE_TWIST)) {
            twisting = true;
        } else if (ev.getActionCommand().equals(FIRE_NEXT)) {
            selectEntity(clientgui.getClient().getNextEntityNum(cen));
        } else if (ev.getActionCommand().equals(FIRE_NEXT_TARG)) {
            jumpToNextTarget();
        } else if (ev.getActionCommand().equals(FIRE_FLIP_ARMS)) {
            updateFlipArms(!ce().getArmsFlipped());
        } else if (ev.getActionCommand().equals(FIRE_MODE)) {
            changeMode();
        } else if (ev.getActionCommand().equals(FIRE_CANCEL)) {
            clear();
        } else if (ev.getActionCommand().equals(FIRE_SEARCHLIGHT)) {
            doSearchlight();
        }
    }

    private void updateFlipArms(boolean armsFlipped) {
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
                && SearchlightAttackAction.isPossible(clientgui.getClient().game, cen, target,
                        null));
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

    private void setFlipArmsEnabled(boolean enabled) {
        butFlipArms.setEnabled(enabled);
        clientgui.getMenuBar().setFireFlipArmsEnabled(enabled);
    }

    private void setNextEnabled(boolean enabled) {
        butNext.setEnabled(enabled);
        clientgui.getMenuBar().setFireNextEnabled(enabled);
    }

    private void setSearchlightEnabled(boolean enabled) {
        butSearchlight.setEnabled(enabled);
        clientgui.getMenuBar().setFireSearchlightEnabled(enabled);
    }

    private void setFireModeEnabled(boolean enabled) {
        butFireMode.setEnabled(enabled);
        clientgui.getMenuBar().setFireModeEnabled(enabled);
    }

    private void setNextTargetEnabled(boolean enabled) {
        butNextTarg.setEnabled(enabled);
        clientgui.getMenuBar().setFireNextTargetEnabled(enabled);
    }

    //
    // KeyListener
    //
    public void keyPressed(KeyEvent ev) {

        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        if ((ev.getKeyCode() == KeyEvent.VK_SHIFT) && !shiftheld) {
            shiftheld = true;
            if (clientgui.getClient().isMyTurn()
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

    @Override
    public void clear() {
        clearAttacks();
        clientgui.getBoardView().select(null);
        clientgui.getBoardView().cursor(null);
        refreshAll();
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

        if (clientgui.getClient().isMyTurn() && (ce() != null)) {
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

        Entity e = clientgui.getClient().game.getEntity(b.getEntityId());
        if (clientgui.getClient().isMyTurn()) {
            if (clientgui.getClient().getMyTurn().isValidEntity(e, clientgui.getClient().game)) {
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

    /**
     * Stop just ignoring events and actually stop listening to them.
     */
    public void removeAllListeners() {
        clientgui.getClient().game.removeGameListener(this);
        clientgui.getBoardView().removeBoardViewListener(this);
        clientgui.mechD.wPan.weaponList.removeListSelectionListener(this);
    }

    public void valueChanged(ListSelectionEvent event) {
        if (event.getSource().equals(clientgui.mechD.wPan.weaponList)) {
            // update target data in weapon display
            updateTarget();
        }
    }
}
