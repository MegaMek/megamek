/*
 * MegaMek -
 * Copyright (C) 2000-2005 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.client.ui.swing;

import megamek.client.event.BoardViewEvent;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.unitDisplay.WeaponPanel;
import megamek.client.ui.swing.util.CommandAction;
import megamek.client.ui.swing.util.KeyCommandBind;
import megamek.client.ui.swing.util.MegaMekController;
import megamek.client.ui.swing.widget.MegamekButton;
import megamek.common.*;
import megamek.common.actions.ArtilleryAttackAction;
import megamek.common.actions.EntityAction;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.enums.AimingMode;
import megamek.common.enums.GamePhase;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.event.GameTurnChangeEvent;
import megamek.common.options.OptionsConstants;
import megamek.common.weapons.Weapon;
import megamek.common.weapons.capitalweapons.CapitalMissileWeapon;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.event.*;
import java.util.*;

/**
 * This display is used for when hidden units are taking pointblank shots.
 * 
 * @author arlith
 *
 */
public class PointblankShotDisplay extends FiringDisplay implements ItemListener, ListSelectionListener {
    private static final long serialVersionUID = -58785096133753153L;

    /**
     * This enumeration lists all of the possible ActionCommands that can be
     * carried out during the pointblank phase.  Each command has a string for
     * the command plus a flag that determines what unit type it is
     * appropriate for.
     *
     * @author arlith
     */
    public static enum FiringCommand implements PhaseCommand {
        FIRE_TWIST("fireTwist"),
        FIRE_FIRE("fireFire"),
        FIRE_SKIP("fireSkip"),
        FIRE_MODE("fireMode"),
        FIRE_FLIP_ARMS("fireFlipArms"),
        FIRE_SEARCHLIGHT("fireSearchlight"),
        FIRE_CALLED("fireCalled"),
        FIRE_CANCEL("fireCancel"),
        FIRE_MORE("fireMore");

        String cmd;

        /**
         * Priority that determines this buttons order
         */
        public int priority;

        private FiringCommand(String c) {
            cmd = c;
        }

        @Override
        public String getCmd() {
            return cmd;
        }

        @Override
        public int getPriority() {
            return priority;
        }

        @Override
        public void setPriority(int p) {
            priority = p;
        }

        @Override
        public String toString() {
            return Messages.getString("FiringDisplay." + getCmd());
        }
    }

    // buttons
    protected Map<FiringCommand, MegamekButton> buttons;

    /**
     * Creates and lays out a new pointblank phase display for the specified
     * clientgui.getClient().
     */
    public PointblankShotDisplay(final ClientGUI clientgui) {
        super(clientgui);

        buttons = new HashMap<>((int) (FiringCommand.values().length * 1.25 + 0.5));
        for (FiringCommand cmd : FiringCommand.values()) {
            String title = Messages.getString("FiringDisplay." + cmd.getCmd());
            MegamekButton newButton = new MegamekButton(title, "PhaseDisplayButton");
            String ttKey = "FiringDisplay." + cmd.getCmd() + ".tooltip";
            if (Messages.keyExists(ttKey)) {
                newButton.setToolTipText(Messages.getString(ttKey));
            }
            newButton.addActionListener(this);
            newButton.setActionCommand(cmd.getCmd());
            newButton.setEnabled(false);
            buttons.put(cmd, newButton);
        }
        numButtonGroups =
                (int) Math.ceil((buttons.size() + 0.0) / buttonsPerGroup);

        setupButtonPanel();

        butDone.addActionListener(new AbstractAction() {
            private static final long serialVersionUID = -5034474968902280850L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (isIgnoringEvents()) {
                    return;
                }
                if (clientgui.isProcessingPointblankShot()) {
                    ready();
                    // When the turn is ended, we could miss a key release event
                    // This will ensure no repeating keys are stuck down
                    clientgui.controller.stopAllRepeating();
                }
            }
        });

    }

    /**
     * Register all of the <code>CommandAction</code>s for this panel display.
     */
    @Override
    protected void registerKeyCommands() {
        MegaMekController controller = clientgui.controller;
        final StatusBarPhaseDisplay display = this;

        // Register the action for DONE
        clientgui.controller.registerCommandAction(KeyCommandBind.DONE.cmd,
                new CommandAction() {

                    @Override
                    public boolean shouldPerformAction() {
                        if (!clientgui.isProcessingPointblankShot()
                                || clientgui.getBoardView().getChatterBoxActive()
                                || display.isIgnoringEvents()
                                || !display.isVisible()
                                || !butDone.isEnabled()) {
                            return false;
                        } else {
                            return true;
                        }
                    }

                    @Override
                    public void performAction() {
                        ready();
                    }
                });

        // Register the action for UNDO
        controller.registerCommandAction(KeyCommandBind.UNDO_LAST_STEP.cmd,
                new CommandAction() {

                    @Override
                    public boolean shouldPerformAction() {
                        if (!clientgui.isProcessingPointblankShot()
                                || clientgui.getBoardView().getChatterBoxActive()
                                || display.isIgnoringEvents()
                                || !display.isVisible()) {
                            return false;
                        } else {
                            return true;
                        }
                    }

                    @Override
                    public void performAction() {
                        removeLastFiring();
                    }
                });

        // Register the action for TWIST_LEFT
        controller.registerCommandAction(KeyCommandBind.TWIST_LEFT.cmd,
                new CommandAction() {

                    @Override
                    public boolean shouldPerformAction() {
                        if (!clientgui.isProcessingPointblankShot()
                                || clientgui.getBoardView().getChatterBoxActive()
                                || !display.isVisible()
                                || display.isIgnoringEvents()) {
                            return false;
                        } else {
                            return true;
                        }
                    }

                    @Override
                    public void performAction() {
                        updateFlipArms(false);
                        torsoTwist(0);
                    }
                });

        // Register the action for TWIST_RIGHT
        controller.registerCommandAction(KeyCommandBind.TWIST_RIGHT.cmd,
                new CommandAction() {

                    @Override
                    public boolean shouldPerformAction() {
                        if (!clientgui.isProcessingPointblankShot()
                                || clientgui.getBoardView().getChatterBoxActive()
                                || !display.isVisible()
                                || display.isIgnoringEvents()) {
                            return false;
                        } else {
                            return true;
                        }
                    }

                    @Override
                    public void performAction() {
                        updateFlipArms(false);
                        torsoTwist(1);
                    }
                });

        // Register the action for FIRE
        controller.registerCommandAction(KeyCommandBind.FIRE.cmd,
                new CommandAction() {

                    @Override
                    public boolean shouldPerformAction() {
                        if (!clientgui.isProcessingPointblankShot()
                                || clientgui.getBoardView().getChatterBoxActive()
                                || !display.isVisible()
                                || display.isIgnoringEvents()
                                || !buttons.get(FiringCommand.FIRE_FIRE)
                                        .isEnabled()) {
                            return false;
                        } else {
                            return true;
                        }
                    }

                    @Override
                    public void performAction() {
                        fire();
                    }
                });

        // Register the action for NEXT_WEAPON
        controller.registerCommandAction(KeyCommandBind.NEXT_WEAPON.cmd,
                new CommandAction() {

                    @Override
                    public boolean shouldPerformAction() {
                        if (!clientgui.isProcessingPointblankShot()
                                || clientgui.getBoardView().getChatterBoxActive()
                                || !display.isVisible()
                                || display.isIgnoringEvents()) {
                            return false;
                        } else {
                            return true;
                        }
                    }

                    @Override
                    public void performAction() {
                        nextWeapon();
                    }
                });

        // Register the action for PREV_WEAPON
        controller.registerCommandAction(KeyCommandBind.PREV_WEAPON.cmd,
                new CommandAction() {

                    @Override
                    public boolean shouldPerformAction() {
                        if (!clientgui.isProcessingPointblankShot()
                                || clientgui.getBoardView().getChatterBoxActive()
                                || !display.isVisible()
                                || display.isIgnoringEvents()) {
                            return false;
                        } else {
                            return true;
                        }
                    }

                    @Override
                    public void performAction() {
                        prevWeapon();
                    }
                });

        // Register the action for NEXT_MODE
        controller.registerCommandAction(KeyCommandBind.NEXT_MODE.cmd,
                new CommandAction() {

                    @Override
                    public boolean shouldPerformAction() {
                        if (!clientgui.getClient().isMyTurn()
                                || clientgui.getBoardView().getChatterBoxActive()
                                || display.isIgnoringEvents()
                                || !display.isVisible()) {
                            return false;
                        } else {
                            return true;
                        }
                    }

                    @Override
                    public void performAction() {
                        changeMode(true);
                    }
                });

        // Register the action for PREV_MODE
        controller.registerCommandAction(KeyCommandBind.PREV_MODE.cmd,
                new CommandAction() {

                    @Override
                    public boolean shouldPerformAction() {
                        if (!clientgui.getClient().isMyTurn()
                                || clientgui.getBoardView().getChatterBoxActive()
                                || display.isIgnoringEvents()
                                || !display.isVisible()) {
                            return false;
                        } else {
                            return true;
                        }
                    }

                    @Override
                    public void performAction() {
                        changeMode(false);
                    }
                });

        // Register the action for CLEAR
        controller.registerCommandAction(KeyCommandBind.CANCEL.cmd,
                new CommandAction() {

                    @Override
                    public boolean shouldPerformAction() {
                        if (clientgui.getBoardView().getChatterBoxActive()
                                || !display.isVisible()
                                || display.isIgnoringEvents()) {
                            return false;
                        } else {
                            return true;
                        }
                    }

                    @Override
                    public void performAction() {
                        clear();
                    }
                });

    }

    @Override
    protected ArrayList<MegamekButton> getButtonList() {
        if (buttons == null) {
            return new ArrayList<>();
        }
        ArrayList<MegamekButton> buttonList = new ArrayList<>();
        int i = 0;
        FiringCommand[] commands = FiringCommand.values();
        CommandComparator comparator = new CommandComparator();
        Arrays.sort(commands, comparator);
        for (FiringCommand cmd : commands) {
            if (cmd == FiringCommand.FIRE_MORE
                    || cmd == FiringCommand.FIRE_CANCEL) {
                continue;
            }

            buttonList.add(buttons.get(cmd));
            i++;

            if ((i + 1) % buttonsPerGroup == 0) {
                buttonList.add(buttons.get(FiringCommand.FIRE_MORE));
                i++;
            }
        }
        if (!buttonList.get(i - 1).getActionCommand()
                .equals(FiringCommand.FIRE_MORE.getCmd())) {
            while ((i + 1) % buttonsPerGroup != 0) {
                buttonList.add(null);
                i++;
            }
            buttonList.add(buttons.get(FiringCommand.FIRE_MORE));
        }
        return buttonList;
    }


    /**
     * Selects an entity, by number, for firing.
     */
    @Override
    public void selectEntity(int en) {
        // clear any previously considered attacks
        clearAttacks();
        refreshAll();

        if (clientgui.getClient().getGame().getEntity(en) != null) {
            cen = en;
            clientgui.setSelectedEntityNum(en);
            clientgui.getUnitDisplay().displayEntity(ce());

            if (!ce().isOffBoard()) {
                clientgui.getBoardView().highlight(ce().getPosition());
            }
            clientgui.getBoardView().select(null);
            clientgui.getBoardView().cursor(null);

            refreshAll();

            clientgui.getBoardView().centerOnHex(ce().getPosition());

            // only twist if crew conscious
            setTwistEnabled(ce().canChangeSecondaryFacing()
                            && ce().getCrew().isActive());

            setFlipArmsEnabled(ce().canFlipArms());
            updateSearchlight();
        } else {
            LogManager.getLogger().error("Tried to select non-existent entity " + en);
        }

        clientgui.getBoardView().clearFiringSolutionData();
    }

    /**
     * Does turn start stuff
     */
    @Override
    public void beginMyTurn() {
        clientgui.maybeShowUnitDisplay();
        clientgui.getBoardView().clearFieldofF();

        butDone.setEnabled(true);
        if (numButtonGroups > 1) {
            buttons.get(FiringCommand.FIRE_MORE).setEnabled(true);
        }
        setFireCalledEnabled(clientgui.getClient().getGame().getOptions()
                .booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_CALLED_SHOTS));
        setStatusBarText(Messages.getString("StatusBarPhaseDisplay.pointblankShot"));
    }

    /**
     * Does end turn stuff.
     */
    @Override
    protected void endMyTurn() {
        // end my turn, then.
        Game game = clientgui.getClient().getGame();
        Entity next = game.getNextEntity(game.getTurnIndex());
        if ((game.getPhase() == GamePhase.FIRING)
            && (next != null) && (ce() != null)
            && (next.getOwnerId() != ce().getOwnerId())) {
            clientgui.setUnitDisplayVisible(false);
        }
        cen = Entity.NONE;
        target(null);
        clientgui.getBoardView().select(null);
        clientgui.getBoardView().highlight(null);
        clientgui.getBoardView().cursor(null);
        clientgui.getBoardView().clearMovementData();
        clientgui.getBoardView().clearFiringSolutionData();
        clientgui.getBoardView().clearStrafingCoords();
        clientgui.getBoardView().clearFieldofF();
        clientgui.setSelectedEntityNum(Entity.NONE);
        disableButtons();
        // Return back to the movement phase display
        clientgui.switchPanel(GamePhase.MOVEMENT);
    }

    /**
     * Disables all buttons in the interface
     */
    @Override
    protected void disableButtons() {
        setFireEnabled(false);
        setSkipEnabled(false);
        setTwistEnabled(false);
        buttons.get(FiringCommand.FIRE_MORE).setEnabled(false);
        butDone.setEnabled(false);
        setFlipArmsEnabled(false);
        setFireModeEnabled(false);
        setFireCalledEnabled(false);
    }

    /**
     * Called when the current entity is done firing. Send out our attack queue
     * to the server.
     */
    @Override
    public void ready() {
        if (attacks.isEmpty() && GUIPreferences.getInstance().getNagForNoAction()) {
            // confirm this action
            String title = Messages.getString("FiringDisplay.DontFireDialog.title");
            String body = Messages.getString("FiringDisplay.DontFireDialog.message");
            ConfirmDialog response = clientgui.doYesNoBotherDialog(title, body);
            if (!response.getShowAgain()) {
                GUIPreferences.getInstance().setNagForNoAction(false);
            }

            if (!response.getAnswer()) {
                return;
            }
        }

        // We need to nag for overheat on capital fighters
        if ((ce() != null) && ce().isCapitalFighter()
            && GUIPreferences.getInstance().getNagForOverheat()) {
            int totalheat = 0;
            for (EntityAction action : attacks) {
                if (action instanceof WeaponAttackAction) {
                    Mounted weapon = ce().getEquipment(((WeaponAttackAction) action).getWeaponId());
                    totalheat += weapon.getCurrentHeat();
                }
            }

            if (totalheat > ce().getHeatCapacity()) {
                // confirm this action
                String title = Messages.getString("FiringDisplay.OverheatNag.title");
                String body = Messages.getString("FiringDisplay.OverheatNag.message");
                ConfirmDialog response = clientgui.doYesNoBotherDialog(title, body);
                if (!response.getShowAgain()) {
                    GUIPreferences.getInstance().setNagForOverheat(false);
                }

                if (!response.getAnswer()) {
                    return;
                }
            }
        }

        // stop further input (hopefully)
        disableButtons();

        // remove temporary attacks from game & board
        removeTempAttacks();

        // For bug 1002223
        // Re-compute the to-hit numbers by adding in correct order.
        Vector<EntityAction> newAttacks = new Vector<>();
        for (EntityAction o : attacks) {
            if (o instanceof ArtilleryAttackAction) {
                newAttacks.addElement(o);
            } else if (o instanceof WeaponAttackAction) {
                WeaponAttackAction waa = (WeaponAttackAction) o;
                Entity attacker = waa
                        .getEntity(clientgui.getClient().getGame());
                Targetable target1 = waa.getTarget(clientgui.getClient()
                        .getGame());
                boolean curInFrontArc = Compute.isInArc(attacker.getPosition(),
                        attacker.getSecondaryFacing(), target1,
                        attacker.getForwardArc());
                if (curInFrontArc) {
                    WeaponAttackAction waa2 = new WeaponAttackAction(
                            waa.getEntityId(), waa.getTargetType(),
                            waa.getTargetId(), waa.getWeaponId());
                    waa2.setAimedLocation(waa.getAimedLocation());
                    waa2.setAimingMode(waa.getAimingMode());
                    waa2.setOtherAttackInfo(waa.getOtherAttackInfo());
                    waa2.setAmmoId(waa.getAmmoId());
                    waa2.setAmmoCarrier(waa.getAmmoCarrier());
                    waa2.setBombPayload(waa.getBombPayload());
                    waa2.setStrafing(waa.isStrafing());
                    waa2.setStrafingFirstShot(waa.isStrafingFirstShot());
                    waa2.setPointblankShot(waa.isPointblankShot());
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
                Entity attacker = waa
                        .getEntity(clientgui.getClient().getGame());
                Targetable target1 = waa.getTarget(clientgui.getClient()
                        .getGame());
                boolean curInFrontArc = Compute.isInArc(attacker.getPosition(),
                        attacker.getSecondaryFacing(), target1,
                        attacker.getForwardArc());
                if (!curInFrontArc) {
                    WeaponAttackAction waa2 = new WeaponAttackAction(
                            waa.getEntityId(), waa.getTargetType(),
                            waa.getTargetId(), waa.getWeaponId());
                    waa2.setAimedLocation(waa.getAimedLocation());
                    waa2.setAimingMode(waa.getAimingMode());
                    waa2.setOtherAttackInfo(waa.getOtherAttackInfo());
                    waa2.setAmmoId(waa.getAmmoId());
                    waa2.setAmmoCarrier(waa.getAmmoCarrier());
                    waa2.setBombPayload(waa.getBombPayload());
                    waa2.setStrafing(waa.isStrafing());
                    waa2.setStrafingFirstShot(waa.isStrafingFirstShot());
                    waa2.setPointblankShot(waa.isPointblankShot());
                    newAttacks.addElement(waa2);
                }
            }
        }
        
        // If the user picked a hex along the flight path, server needs to know
        if ((target instanceof Entity) && Compute.isGroundToAir(ce(), target)) {
            Coords targetPos = ((Entity) target).getPlayerPickedPassThrough(cen);
            if (targetPos != null) {
                clientgui.getClient().sendPlayerPickedPassThrough(
                        ((Entity) target).getId(), cen, targetPos);
            }
        }

        // send out attacks
        clientgui.getClient().sendHiddenPBSCFRResponse(newAttacks);
        clientgui.setPointblankEID(Entity.NONE);

        // clear queue
        attacks.removeAllElements();

        // close aimed shot display, if any
        ash.closeDialog();

        if ((ce() != null) && ce().isWeapOrderChanged()) {
            clientgui.getClient().sendEntityWeaponOrderUpdate(ce());
        }
        endMyTurn();
    }

    /**
     * Adds a weapon attack with the currently selected weapon to the attack
     * queue.
     */
    @Override
    void fire() {
        final Game game = clientgui.getClient().getGame();
        // get the selected weaponnum
        final int weaponNum = clientgui.getUnitDisplay().wPan.getSelectedWeaponNum();
        Mounted mounted = ce().getEquipment(weaponNum);

        // validate
        if ((ce() == null)
                || (mounted == null)
                || !(mounted.getType() instanceof WeaponType)) {
            throw new IllegalArgumentException("current fire parameters are invalid");
        }

        // declare searchlight, if possible
        if (GUIPreferences.getInstance().getAutoDeclareSearchlight()
            && ce().isUsingSearchlight()) {
            doSearchlight();
        }

        WeaponAttackAction waa;
        if (!(mounted.getType().hasFlag(WeaponType.F_ARTILLERY)
                || (mounted.getType() instanceof CapitalMissileWeapon
                        && Compute.isGroundToGround(ce(), target)))) {
            waa = new WeaponAttackAction(cen, target.getTargetType(),
                    target.getId(), weaponNum);
        } else {
            waa = new ArtilleryAttackAction(cen, target.getTargetType(),
                    target.getId(), weaponNum, game);
        }

        if ((mounted.getLinked() != null)
                && (((WeaponType) mounted.getType()).getAmmoType() != AmmoType.T_NA)
                && (mounted.getLinked().getType() instanceof AmmoType)) {
            Mounted ammoMount = mounted.getLinked();
            AmmoType ammoType = (AmmoType) ammoMount.getType();
            waa.setAmmoId(ammoMount.getEntity().getEquipmentNum(ammoMount));
            waa.setAmmoCarrier(ammoMount.getEntity().getId());
            if (((ammoType.getMunitionType() == AmmoType.M_THUNDER_VIBRABOMB) 
                    && ((ammoType.getAmmoType() == AmmoType.T_LRM)
                            || (ammoType.getAmmoType() == AmmoType.T_LRM_IMP)
                            || (ammoType.getAmmoType() == AmmoType.T_MML)))
                    || (ammoType.getMunitionType() == AmmoType.M_VIBRABOMB_IV)) {
                VibrabombSettingDialog vsd = new VibrabombSettingDialog(clientgui.frame);
                vsd.setVisible(true);
                waa.setOtherAttackInfo(vsd.getSetting());
                waa.setHomingShot(ammoType.getMunitionType() == AmmoType.M_HOMING && ammoMount.curMode().equals("Homing"));
            }
        }

        if (ash.allowAimedShotWith(mounted) && !ash.getAimingMode().isNone() && ash.isAimingAtLocation()) {
            waa.setAimedLocation(ash.getAimingAt());
            waa.setAimingMode(ash.getAimingMode());
        } else {
            waa.setAimedLocation(Entity.LOC_NONE);
            waa.setAimingMode(AimingMode.NONE);
        }
        waa.setPointblankShot(true);

        // add the attack to our temporary queue
        attacks.addElement(waa);

        // and add it into the game, temporarily
        game.addAction(waa);

        // set the weapon as used
        mounted.setUsedThisRound(true);

        // find the next available weapon
        int nextWeapon = clientgui.getUnitDisplay().wPan.getNextWeaponNum();

        // check; if there are no ready weapons, you're done.
        if ((nextWeapon == -1)
            && GUIPreferences.getInstance().getAutoEndFiring()) {
            ready();
            return;
        }

        // otherwise, display firing info for the next weapon
        clientgui.getUnitDisplay().wPan.displayMech(ce());
        Mounted nextMounted = ce().getEquipment(nextWeapon);
        if (!mounted.getType().hasFlag(WeaponType.F_VGL) && (nextMounted != null)
                && nextMounted.getType().hasFlag(WeaponType.F_VGL)) {
            clientgui.getUnitDisplay().wPan.setPrevTarget(target);
        }
        clientgui.getUnitDisplay().wPan.selectWeapon(nextWeapon);
        updateTarget();

    }

    /**
     * Targets something
     */
    @Override
    public void target(Targetable t) {
        if (ce() == null) {
            return;
        }
        final int weaponId = clientgui.getUnitDisplay().wPan.getSelectedWeaponNum();
        Mounted weapon = ce().getEquipment(weaponId); 
        // Some weapons pick an automatic target
        if ((weapon != null) && weapon.getType().hasFlag(WeaponType.F_VGL)) {
            int facing;
            if (ce().isSecondaryArcWeapon(weaponId)) {
                facing = ce().getSecondaryFacing();
            } else {
                facing = ce().getFacing();
            }
            facing = (facing + weapon.getFacing()) % 6;
            Coords c = ce().getPosition().translated(facing);
            Targetable hexTarget = new HexTarget(c, Targetable.TYPE_HEX_CLEAR);
            
            // Ignore events that will be generated by the select/cursor calls
            setIgnoringEvents(true);
            clientgui.getBoardView().select(c);
            setIgnoringEvents(false);
            target = hexTarget;
        } else {
            target = t;
        }
        if ((target instanceof Entity) && Compute.isGroundToAir(ce(), target)) {
            Coords targetPos = Compute.getClosestFlightPath(cen, ce().getPosition(), (Entity) target);
            clientgui.getBoardView().cursor(targetPos);
        }
        ash.setAimingMode();
        updateTarget();
        ash.showDialog();
    }

    /**
     * Targets something
     */
    @Override
    public void updateTarget() {
        setFireEnabled(false);
        Game game = clientgui.getClient().getGame();

        // update target panel
        final int weaponId = clientgui.getUnitDisplay().wPan.getSelectedWeaponNum();
        if ((target != null) && (target.getPosition() != null) && (weaponId != -1) && (ce() != null)) {
            ToHitData toHit;
            if (!ash.getAimingMode().isNone()) {
                Mounted weapon = ce().getEquipment(weaponId);
                boolean aiming = ash.isAimingAtLocation()
                        && ash.allowAimedShotWith(weapon);
                ash.setEnableAll(aiming);
                if (aiming) {
                    toHit = WeaponAttackAction.toHit(game, cen, target,
                            weaponId, ash.getAimingAt(), ash.getAimingMode(),
                            false, false, null, null, false, true);
                    String t =  String.format("<html><div WIDTH=%d>%s</div></html>", WeaponPanel.TARGET_DISPLAY_WIDTH, target.getDisplayName() + " (" + ash.getAimingLocation() + ")");
                    clientgui.getUnitDisplay().wPan.wTargetR.setText(t);
                } else {
                    toHit = WeaponAttackAction.toHit(game, cen, target, weaponId, Entity.LOC_NONE,
                            AimingMode.NONE, false, false,
                            null, null, false, true);

                    String t =  String.format("<html><div WIDTH=%d>%s</div></html>", WeaponPanel.TARGET_DISPLAY_WIDTH, target.getDisplayName());
                    clientgui.getUnitDisplay().wPan.wTargetR.setText(t);
                }
                ash.setPartialCover(toHit.getCover());
            } else {
                toHit = WeaponAttackAction.toHit(game, cen, target, weaponId, Entity.LOC_NONE,
                        AimingMode.NONE, false, false, null,
                        null, false, true);

                String t =  String.format("<html><div WIDTH=%d>%s</div></html>", WeaponPanel.TARGET_DISPLAY_WIDTH, target.getDisplayName());
                clientgui.getUnitDisplay().wPan.wTargetR.setText(t);
            }
            int effectiveDistance = Compute.effectiveDistance(game, ce(), target);
            clientgui.getUnitDisplay().wPan.wRangeR.setText("" + effectiveDistance);
            Mounted m = ce().getEquipment(weaponId);
            // If we have a Centurion Weapon System selected, we may need to
            //  update ranges.
            if (m.getType().hasFlag(WeaponType.F_CWS)) {
                clientgui.getUnitDisplay().wPan.selectWeapon(weaponId);
            }

            if (m.isUsedThisRound()) {
                clientgui.getUnitDisplay().wPan.wToHitR.setText(Messages.getString("FiringDisplay.alreadyFired"));
                setFireEnabled(false);
            } else if ((m.getType().hasFlag(WeaponType.F_AUTO_TARGET) && !m.curMode().equals(Weapon.MODE_AMS_MANUAL))) {
                clientgui.getUnitDisplay().wPan.wToHitR.setText(Messages.getString("FiringDisplay.autoFiringWeapon"));
                setFireEnabled(false);
            } else if (toHit.getValue() == TargetRoll.IMPOSSIBLE) {
                clientgui.getUnitDisplay().wPan.wToHitR.setText(toHit.getValueAsString());
                setFireEnabled(false);
            } else if (toHit.getValue() == TargetRoll.AUTOMATIC_FAIL) {
                clientgui.getUnitDisplay().wPan.wToHitR.setText(toHit.getValueAsString());
                setFireEnabled(true);
            } else {
                boolean natAptGunnery = ce().hasAbility(OptionsConstants.PILOT_APTITUDE_GUNNERY);
                clientgui.getUnitDisplay().wPan.wToHitR.setText(toHit.getValueAsString()
                        + " (" + Compute.oddsAbove(toHit.getValue(), natAptGunnery) + "%)");
                setFireEnabled(true);
            }
            clientgui.getUnitDisplay().wPan.toHitText.setText(toHit.getDesc());
            setSkipEnabled(true);
        } else {
            clientgui.getUnitDisplay().wPan.wTargetR.setText("---");
            clientgui.getUnitDisplay().wPan.wRangeR.setText("---");
            clientgui.getUnitDisplay().wPan.wToHitR.setText("---");
            clientgui.getUnitDisplay().wPan.toHitText.setText("");
        }

        if ((weaponId != -1) && (ce() != null)) {
            Mounted m = ce().getEquipment(weaponId);
            setFireModeEnabled(m.isModeSwitchable());
        }

        updateSearchlight();
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
        if (!clientgui.isProcessingPointblankShot()
            || ((b.getButton() != MouseEvent.BUTTON1))) {
            return;
        }
        // control pressed means a line of sight check.
        // added ALT_MASK by kenn
        if (((b.getModifiers() & InputEvent.CTRL_DOWN_MASK) != 0)
            || ((b.getModifiers() & InputEvent.ALT_DOWN_MASK) != 0)) {
            return;
        }
        // check for shifty goodness
        if (shiftheld != ((b.getModifiers() & InputEvent.SHIFT_DOWN_MASK) != 0)) {
            shiftheld = (b.getModifiers() & InputEvent.SHIFT_DOWN_MASK) != 0;
        }

        if (b.getType() == BoardViewEvent.BOARD_HEX_DRAGGED) {
            if (shiftheld || twisting) {
                updateFlipArms(false);
                torsoTwist(b.getCoords());
            }
            clientgui.getBoardView().cursor(b.getCoords());
        } else if (b.getType() == BoardViewEvent.BOARD_HEX_CLICKED) {
            twisting = false;
            if (!shiftheld) {
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

        Coords evtCoords = b.getCoords();
        if (clientgui.isProcessingPointblankShot() && (evtCoords != null)
            && (ce() != null)) {
            if (!evtCoords.equals(ce().getPosition())) {
                if (shiftheld) {
                    updateFlipArms(false);
                    torsoTwist(b.getCoords());
                }
            }
        }
    }

    //
    // GameListener
    //
    @Override
    public void gameTurnChange(GameTurnChangeEvent e) {

    }

    @Override
    public void gamePhaseChange(GamePhaseChangeEvent e) {

    }

    //
    // ActionListener
    //
    @Override
    public void actionPerformed(ActionEvent ev) {
        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        if (!clientgui.isProcessingPointblankShot()) {
            return;
        }

        if (ev.getActionCommand().equals(FiringCommand.FIRE_FIRE.getCmd())) {
            fire();
        } else if (ev.getActionCommand().equals(FiringCommand.FIRE_SKIP.getCmd())) {
            nextWeapon();
        } else if (ev.getActionCommand().equals(FiringCommand.FIRE_TWIST.getCmd())) {
            twisting = true;
        } else if (ev.getActionCommand().equals(FiringCommand.FIRE_MORE.getCmd())) {
            currentButtonGroup++;
            currentButtonGroup %= numButtonGroups;
            setupButtonPanel();
        } else if (ev.getActionCommand().equals(FiringCommand.FIRE_FLIP_ARMS.getCmd())) {
            updateFlipArms(!ce().getArmsFlipped());
        } else if (ev.getActionCommand().equals(FiringCommand.FIRE_MODE.getCmd())) {
            changeMode(true);
        } else if (ev.getActionCommand().equals(FiringCommand.FIRE_CALLED.getCmd())) {
            changeCalled();
        } else if (("changeSinks".equalsIgnoreCase(ev.getActionCommand()))
                   || (ev.getActionCommand().equals(FiringCommand.FIRE_CANCEL.getCmd()))) {
            clear();
        }
    }

    @Override
    protected void setFireEnabled(boolean enabled) {
        buttons.get(FiringCommand.FIRE_FIRE).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(FiringCommand.FIRE_FIRE.getCmd(), enabled);
    }

    @Override
    protected void setTwistEnabled(boolean enabled) {
        buttons.get(FiringCommand.FIRE_TWIST).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(FiringCommand.FIRE_TWIST.getCmd(), enabled);
    }

    @Override
    protected void setSkipEnabled(boolean enabled) {
        buttons.get(FiringCommand.FIRE_SKIP).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(FiringCommand.FIRE_SKIP.getCmd(), enabled);
    }

    @Override
    protected void setFlipArmsEnabled(boolean enabled) {
        buttons.get(FiringCommand.FIRE_FLIP_ARMS).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(FiringCommand.FIRE_FLIP_ARMS.getCmd(), enabled);
    }

    @Override
    protected void setSearchlightEnabled(boolean enabled) {
        buttons.get(FiringCommand.FIRE_SEARCHLIGHT).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(FiringCommand.FIRE_SEARCHLIGHT.getCmd(), enabled);
    }

    @Override
    protected void setFireModeEnabled(boolean enabled) {
        buttons.get(FiringCommand.FIRE_MODE).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(FiringCommand.FIRE_MODE.getCmd(), enabled);
    }

    @Override
    protected void setFireCalledEnabled(boolean enabled) {
        buttons.get(FiringCommand.FIRE_CALLED).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(FiringCommand.FIRE_CALLED.getCmd(), enabled);
    }

    @Override
    public void clear() {
        if ((target instanceof Entity) && Compute.isGroundToAir(ce(), target)) {
            ((Entity) target).setPlayerPickedPassThrough(cen, null);
        }
        clearAttacks();        
        clientgui.getBoardView().select(null);
        clientgui.getBoardView().cursor(null);
        refreshAll();
    }

    //
    // ItemListener
    //
    @Override
    public void itemStateChanged(ItemEvent ev) {

    }

    // board view listener
    @Override
    public void finishedMovingUnits(BoardViewEvent b) {
        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        if (clientgui.isProcessingPointblankShot() && (ce() != null)) {
            clientgui.maybeShowUnitDisplay();
            clientgui.getBoardView().centerOnHex(ce().getPosition());
        }
    }

    @Override
    public void unitSelected(BoardViewEvent b) {
        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        Entity e = clientgui.getClient().getGame().getEntity(b.getEntityId());
        if (clientgui.getPointblankEID() == e.getId()) {
            selectEntity(e.getId());
        } else {
            clientgui.maybeShowUnitDisplay();
            clientgui.getUnitDisplay().displayEntity(e);
            if (e.isDeployed()) {
                clientgui.getBoardView().centerOnHex(e.getPosition());
            }
        }
    }

    @Override
    public void valueChanged(ListSelectionEvent event) {
        if (event.getValueIsAdjusting()) {
            return;
        }

        if (event.getSource().equals(clientgui.getUnitDisplay().wPan.weaponList)) {
            // update target data in weapon display
            updateTarget();
        }
    }
}
