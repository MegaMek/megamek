/*
 * Copyright (C) 2000-2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2016-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package megamek.client.ui.panels.phaseDisplay;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import javax.swing.AbstractAction;
import javax.swing.event.ListSelectionEvent;

import megamek.client.event.BoardViewEvent;
import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.dialogs.phaseDisplay.VibrabombSettingDialog;
import megamek.client.ui.util.KeyCommandBind;
import megamek.client.ui.util.MegaMekController;
import megamek.client.ui.widget.MegaMekButton;
import megamek.common.HexTarget;
import megamek.common.ToHitData;
import megamek.common.actions.ArtilleryAttackAction;
import megamek.common.actions.EntityAction;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.compute.ComputeArc;
import megamek.common.enums.AimingMode;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.event.GameTurnChangeEvent;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.Entity;
import megamek.common.units.Targetable;
import megamek.common.weapons.Weapon;
import megamek.common.weapons.capitalWeapons.CapitalMissileWeapon;
import megamek.logging.MMLogger;

/**
 * This display is used for when hidden units are taking pointblank shots.
 *
 * @author arlith
 */
public class PointblankShotDisplay extends FiringDisplay {
    @Serial
    private static final long serialVersionUID = -58785096133753153L;

    private static final MMLogger logger = MMLogger.create(PointblankShotDisplay.class);

    /**
     * This enumeration lists all the possible ActionCommands that can be carried out during the pointblank phase. Each
     * command has a string for the command plus a flag that determines what unit type it is appropriate for.
     *
     * @author arlith
     */
    public enum FiringCommand implements PhaseCommand {
        FIRE_TWIST("fireTwist"),
        FIRE_FIRE("fireFire"),
        FIRE_SKIP("fireSkip"),
        FIRE_MODE("fireMode"),
        FIRE_FLIP_ARMS("fireFlipArms"),
        FIRE_SEARCHLIGHT("fireSearchlight"),
        FIRE_CALLED("fireCalled"),
        FIRE_CANCEL("fireCancel"),
        FIRE_MORE("fireMore");

        final String cmd;

        /**
         * Priority that determines this buttons order
         */
        public int priority;

        FiringCommand(String c) {
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

        public String getHotKeyDesc() {
            String result = "";

            String msg_left = Messages.getString("Left");
            String msg_right = Messages.getString("Right");
            String msg_next = Messages.getString("Next");
            String msg_previous = Messages.getString("Previous");

            switch (this) {
                case FIRE_TWIST:
                    result = "<BR>";
                    result += "&nbsp;&nbsp;" + msg_left + ": " + KeyCommandBind.getDesc(KeyCommandBind.TWIST_LEFT);
                    result += "&nbsp;&nbsp;" + msg_right + ": " + KeyCommandBind.getDesc(KeyCommandBind.TWIST_RIGHT);
                    break;
                case FIRE_FIRE:
                    result = "<BR>";
                    result += "&nbsp;&nbsp;" + KeyCommandBind.getDesc(KeyCommandBind.FIRE);
                    break;
                case FIRE_SKIP:
                    result = "<BR>";
                    result += "&nbsp;&nbsp;" + msg_next + ": " + KeyCommandBind.getDesc(KeyCommandBind.NEXT_WEAPON);
                    result += "&nbsp;&nbsp;" + msg_previous + ": " + KeyCommandBind.getDesc(KeyCommandBind.PREV_WEAPON);
                    break;
                case FIRE_MODE:
                    result = "<BR>";
                    result += "&nbsp;&nbsp;" + msg_next + ": " + KeyCommandBind.getDesc(KeyCommandBind.NEXT_MODE);
                    result += "&nbsp;&nbsp;" + msg_previous + ": " + KeyCommandBind.getDesc(KeyCommandBind.PREV_MODE);
                    break;
                case FIRE_CANCEL:
                    result = "<BR>";
                    result += "&nbsp;&nbsp;" + KeyCommandBind.getDesc(KeyCommandBind.CANCEL);
                    break;
                default:
                    break;
            }

            return result;
        }
    }

    // buttons
    protected Map<FiringCommand, MegaMekButton> buttons;

    /**
     * Creates and lays out a new pointblank phase display for the specified clientGUI.getClient().
     */
    public PointblankShotDisplay(final ClientGUI clientGUI) {
        super(clientGUI);
        setButtons();
        setButtonsTooltips();
        setupButtonPanel();

        AbstractAction doneHandler = new AbstractAction() {
            @Serial
            private static final long serialVersionUID = -5034474968902280850L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (isIgnoringEvents()) {
                    return;
                }
                if (clientGUI.isProcessingPointblankShot()) {
                    if (e.getSource().equals(butSkipTurn)) {
                        // Undo any turret turns, arm flips, etc.
                        attacks.clear();
                        performDoneNoAction();
                    } else {
                        ready();
                    }
                    // When the turn is ended, we could miss a key release event
                    // This will ensure no repeating keys are stuck down
                    clientGUI.controller.stopAllRepeating();
                }
            }
        };
        // All turn-end handling goes through ready()
        butDone.addActionListener(doneHandler);
        butSkipTurn.addActionListener(doneHandler);
    }

    @Override
    protected void setButtons() {
        buttons = new HashMap<>((int) (FiringCommand.values().length * 1.25 + 0.5));
        for (FiringCommand cmd : FiringCommand.values()) {
            buttons.put(cmd, createButton(cmd.getCmd(), "FiringDisplay."));
        }
        numButtonGroups = (int) Math.ceil((buttons.size() + 0.0) / buttonsPerGroup);
    }

    @Override
    protected void setButtonsTooltips() {
        for (FiringCommand cmd : FiringCommand.values()) {
            String tt = createToolTip(cmd.getCmd(), "FiringDisplay.", cmd.getHotKeyDesc());
            buttons.get(cmd).setToolTipText(tt);
        }
    }

    private boolean shouldPerformPointBlankKeyCommands() {
        return clientgui.isProcessingPointblankShot()
              && !clientgui.isChatBoxActive()
              && isVisible()
              && !isIgnoringEvents();
    }

    private boolean shouldPerformFireKeyCommand() {
        return shouldPerformPointBlankKeyCommands() && buttons.get(FiringCommand.FIRE_FIRE).isEnabled();
    }

    private boolean shouldPerformDoneKeyCommand() {
        return shouldPerformPointBlankKeyCommands() && butDone.isEnabled();
    }

    /**
     * Register all of the <code>CommandAction</code>s for this panel display.
     */
    @Override
    protected void registerKeyCommands() {
        MegaMekController controller = clientgui.controller;
        controller.registerCommandAction(KeyCommandBind.UNDO_LAST_STEP, this::shouldPerformPointBlankKeyCommands,
              this::removeLastFiring);

        controller.registerCommandAction(KeyCommandBind.TWIST_LEFT, this::shouldPerformPointBlankKeyCommands,
              this::twistLeft);
        controller.registerCommandAction(KeyCommandBind.TWIST_RIGHT, this::shouldPerformPointBlankKeyCommands,
              this::twistRight);

        controller.registerCommandAction(KeyCommandBind.NEXT_WEAPON, this::shouldPerformPointBlankKeyCommands,
              this::nextWeapon);
        controller.registerCommandAction(KeyCommandBind.PREV_WEAPON, this::shouldPerformPointBlankKeyCommands,
              this::prevWeapon);

        controller.registerCommandAction(KeyCommandBind.NEXT_MODE, this, () -> changeMode(true));
        controller.registerCommandAction(KeyCommandBind.PREV_MODE, this, () -> changeMode(false));

        controller.registerCommandAction(KeyCommandBind.FIRE, this::shouldPerformFireKeyCommand, this::fire);
        controller.registerCommandAction(KeyCommandBind.CANCEL, this::shouldPerformClearKeyCommand, this::clear);
        controller.registerCommandAction(KeyCommandBind.DONE, this::shouldPerformDoneKeyCommand, this::ready);
        controller.registerCommandAction(KeyCommandBind.DONE_NO_ACTION,
              this::shouldPerformDoneKeyCommand,
              this::performDoneNoAction);
    }

    @Override
    protected ArrayList<MegaMekButton> getButtonList() {
        if (buttons == null) {
            return new ArrayList<>();
        }
        ArrayList<MegaMekButton> buttonList = new ArrayList<>();
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

        if (game.getEntity(en) != null) {
            currentEntity = en;
            clientgui.setSelectedEntityNum(en);
            clientgui.getUnitDisplay().displayEntity(currentEntity());

            clearMarkedHexes();
            if (!currentEntity().isOffBoard()) {
                clientgui.getBoardView(currentEntity()).highlight(currentEntity().getPosition());
            }

            refreshAll();

            clientgui.centerOnUnit(currentEntity());

            // only twist if crew conscious
            setTwistEnabled(currentEntity().canChangeSecondaryFacing()
                  && currentEntity().getCrew().isActive());

            setFlipArmsEnabled(currentEntity().canFlipArms());
            updateSearchlight();
        } else {
            logger.error("Tried to select non-existent entity {}", en);
        }

        clientgui.clearTemporarySprites();
    }

    /**
     * Does turn start stuff
     */
    @Override
    public void beginMyTurn() {
        clientgui.maybeShowUnitDisplay();
        clientgui.clearFieldOfFire();
        clientgui.clearTemporarySprites();

        butDone.setEnabled(true);
        if (numButtonGroups > 1) {
            buttons.get(FiringCommand.FIRE_MORE).setEnabled(true);
        }
        setFireCalledEnabled(game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_CALLED_SHOTS));
        setStatusBarText(Messages.getString("StatusBarPhaseDisplay.pointblankShot"));
    }

    /**
     * Does end turn stuff.
     */
    @Override
    protected void endMyTurn() {
        // end my turn, then.
        Entity next = game.getNextEntity(game.getTurnIndex());
        if (game.getPhase().isFiring() && (next != null) && (currentEntity() != null)
              && (next.getOwnerId() != currentEntity().getOwnerId())) {
            clientgui.maybeShowUnitDisplay();
        }
        currentEntity = Entity.NONE;
        target(null);
        clearMarkedHexes();
        clearMovementSprites();
        clientgui.getBoardView().clearStrafingCoords();
        clientgui.clearFieldOfFire();
        clientgui.clearTemporarySprites();
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

    private boolean checkNags() {
        if (needNagForNoAction()) {
            if (attacks.isEmpty()) {
                // confirm this action
                String title = Messages.getString("FiringDisplay.DontFireDialog.title");
                String body = Messages.getString("FiringDisplay.DontFireDialog.message");
                if (checkNagForNoAction(title, body)) {
                    return true;
                }
            }
        }

        // We need to nag for overheat on capital fighters
        if (needNagForOverheat()) {
            if ((currentEntity() != null)
                  && currentEntity().isCapitalFighter()) {
                int totalHeat = 0;
                for (EntityAction action : attacks) {
                    if (action instanceof WeaponAttackAction) {
                        Mounted<?> weapon = currentEntity().getEquipment(((WeaponAttackAction) action).getWeaponId());
                        totalHeat += weapon.getCurrentHeat();
                    }
                }

                if (totalHeat > currentEntity().getHeatCapacity()) {
                    // confirm this action
                    String title = Messages.getString("FiringDisplay.OverheatNag.title");
                    String body = Messages.getString("FiringDisplay.OverheatNag.message");
                    if (checkNagForOverheat(title, body)) {
                        return true;
                    }
                }
            }
        }

        return currentEntity() == null;
    }

    @Override
    public void ready() {
        if (checkNags()) {
            return;
        }

        // stop further input (hopefully)
        disableButtons();

        // remove temporary attacks from game & board
        removeTempAttacks();

        if (attacks.isEmpty()) {
            // Send signal that we are not taking a PBS shot if there are no attacks
            clientgui.getClient().sendHiddenPBSCFRResponse(null);
        } else {
            // For bug 1002223
            // Re-compute the to-hit numbers by adding in correct order.
            Vector<EntityAction> newAttacks = new Vector<>();
            for (EntityAction o : attacks) {
                if (o instanceof ArtilleryAttackAction) {
                    newAttacks.addElement(o);
                } else if (o instanceof WeaponAttackAction waa) {
                    Entity weaponEntity = waa.getEntity(game);
                    Entity attacker = weaponEntity.getAttackingEntity();
                    Targetable target1 = waa.getTarget(clientgui.getClient()
                          .getGame());
                    boolean curInFrontArc = ComputeArc.isInArc(attacker.getPosition(),
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
                        waa2.setAmmoMunitionType(waa.getAmmoMunitionType());
                        waa2.setAmmoCarrier(waa.getAmmoCarrier());
                        waa2.setBombPayloads(waa.getBombPayloads());
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
                if (!(o instanceof ArtilleryAttackAction) && (o instanceof WeaponAttackAction waa)) {
                    Entity weaponEntity = waa.getEntity(game);
                    Entity attacker = weaponEntity.getAttackingEntity();
                    Targetable target1 = waa.getTarget(clientgui.getClient().getGame());
                    boolean curInFrontArc = ComputeArc.isInArc(attacker.getPosition(),
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
                        waa2.setAmmoMunitionType(waa.getAmmoMunitionType());
                        waa2.setAmmoCarrier(waa.getAmmoCarrier());
                        waa2.setBombPayloads(waa.getBombPayloads());
                        waa2.setStrafing(waa.isStrafing());
                        waa2.setStrafingFirstShot(waa.isStrafingFirstShot());
                        waa2.setPointblankShot(waa.isPointblankShot());
                        newAttacks.addElement(waa2);
                    }
                }
            }

            // If the user picked a hex along the flight path, server needs to know
            if ((target instanceof Entity) && Compute.isGroundToAir(currentEntity(), target)) {
                Coords targetPos = ((Entity) target).getPlayerPickedPassThrough(currentEntity);
                if (targetPos != null) {
                    clientgui.getClient().sendPlayerPickedPassThrough(
                          target.getId(), currentEntity, targetPos);
                }
            }

            // send out attacks
            clientgui.getClient().sendHiddenPBSCFRResponse(newAttacks);
        }

        clientgui.setPointblankEID(Entity.NONE);

        // clear queue
        removeAllAttacks();

        // close aimed shot display, if any
        ash.closeDialog();

        if ((currentEntity() != null) && currentEntity().isWeaponOrderChanged()) {
            clientgui.getClient().sendEntityWeaponOrderUpdate(currentEntity());
        }
        endMyTurn();
    }

    /**
     * Adds a weapon attack with the currently selected weapon to the attack queue.
     */
    @Override
    public void fire() {
        // get the selected weapon num
        final int weaponNum = clientgui.getUnitDisplay().wPan.getSelectedWeaponNum();
        WeaponMounted mounted = (WeaponMounted) currentEntity().getEquipment(weaponNum);

        // validate
        if ((currentEntity() == null) || (mounted == null)) {
            throw new IllegalArgumentException("current fire parameters are invalid");
        }

        // declare searchlight, if possible
        if (GUIP.getAutoDeclareSearchlight()
              && currentEntity().isUsingSearchlight()) {
            doSearchlight();
        }

        WeaponAttackAction waa;
        if (!(mounted.getType().hasFlag(WeaponType.F_ARTILLERY)
              || (mounted.getType() instanceof CapitalMissileWeapon
              && Compute.isGroundToGround(currentEntity(), target)))) {
            waa = new WeaponAttackAction(currentEntity, target.getTargetType(),
                  target.getId(), weaponNum);
        } else {
            waa = new ArtilleryAttackAction(currentEntity, target.getTargetType(),
                  target.getId(), weaponNum, game);
        }

        if ((mounted.getLinked() != null)
              && (mounted.getType().getAmmoType() != AmmoType.AmmoTypeEnum.NA)
              && (mounted.getLinked().getType() instanceof AmmoType)) {
            Mounted<?> ammoMount = mounted.getLinked();
            AmmoType ammoType = (AmmoType) ammoMount.getType();
            waa.setAmmoId(ammoMount.getEntity().getEquipmentNum(ammoMount));
            EnumSet<AmmoType.Munitions> ammoMunitionType = ammoType.getMunitionType();
            waa.setAmmoMunitionType(ammoMunitionType);
            waa.setAmmoCarrier(ammoMount.getEntity().getId());
            if (((ammoMunitionType.contains(AmmoType.Munitions.M_THUNDER_VIBRABOMB))
                  && ((ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.LRM)
                  || (ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.LRM_IMP)
                  || (ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.MML)))
                  || (ammoType.getMunitionType().contains(AmmoType.Munitions.M_VIBRABOMB_IV))) {
                VibrabombSettingDialog vsd = new VibrabombSettingDialog(clientgui.getFrame());
                vsd.setVisible(true);
                waa.setOtherAttackInfo(vsd.getSetting());
                waa.setHomingShot(ammoType.getMunitionType().contains(AmmoType.Munitions.M_HOMING)
                      && ammoMount.curMode().equals("Homing"));
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
        addAttack(waa);

        // and add it into the game, temporarily
        game.addAction(waa);

        // set the weapon as used
        mounted.setUsedThisRound(true);

        // find the next available weapon
        int nextWeapon = clientgui.getUnitDisplay().wPan.getNextWeaponNum();

        // check; if there are no ready weapons, you're done.
        if ((nextWeapon == -1)
              && GUIP.getAutoEndFiring()) {
            ready();
            return;
        }

        // otherwise, display firing info for the next weapon
        clientgui.getUnitDisplay().wPan.displayMek(currentEntity());
        Mounted<?> nextMounted = currentEntity().getEquipment(nextWeapon);
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
        if (currentEntity() == null) {
            return;
        }
        final int weaponId = clientgui.getUnitDisplay().wPan.getSelectedWeaponNum();
        Mounted<?> weapon = currentEntity().getEquipment(weaponId);
        // Some weapons pick an automatic target
        if ((weapon != null) && weapon.getType().hasFlag(WeaponType.F_VGL)) {
            int facing;
            if (currentEntity().isSecondaryArcWeapon(weaponId)) {
                facing = currentEntity().getSecondaryFacing();
            } else {
                facing = currentEntity().getFacing();
            }
            facing = (facing + weapon.getFacing()) % 6;
            Coords c = currentEntity().getPosition().translated(facing);
            Targetable hexTarget = new HexTarget(c, currentEntity().getBoardId(), Targetable.TYPE_HEX_CLEAR);

            // Ignore events that will be generated by the select/cursor calls
            setIgnoringEvents(true);
            clientgui.getBoardView(currentEntity()).select(c);
            setIgnoringEvents(false);
            target = hexTarget;
        } else {
            target = t;
        }
        if ((target instanceof Entity) && Compute.isGroundToAir(currentEntity(), target)) {
            Coords targetPos = Compute.getClosestFlightPath(currentEntity, currentEntity().getPosition(), (Entity) target);
            clientgui.getBoardView(target).cursor(targetPos);
        }
        ash.setAimingMode();
        updateTarget();
        ash.showDialog();
    }

    @Override
    public void updateTarget() {
        setFireEnabled(false);

        // update target panel
        final int weaponId = clientgui.getUnitDisplay().wPan.getSelectedWeaponNum();
        if ((currentEntity() != null) && currentEntity().equals(clientgui.getUnitDisplay().getCurrentEntity())
              && (target != null) && (target.getPosition() != null) && (weaponId != -1)) {
            ToHitData toHit;
            if (!ash.getAimingMode().isNone()) {
                WeaponMounted weapon = (WeaponMounted) currentEntity().getEquipment(weaponId);
                boolean aiming = ash.isAimingAtLocation() && ash.allowAimedShotWith(weapon);
                ash.setEnableAll(aiming);
                if (aiming) {
                    toHit = WeaponAttackAction.toHit(game, currentEntity, target,
                          weaponId, ash.getAimingAt(), ash.getAimingMode(),
                          false, false, null, null, false, true,
                          WeaponAttackAction.UNASSIGNED, WeaponAttackAction.UNASSIGNED);
                    clientgui.getUnitDisplay().wPan.setTarget(target,
                          Messages.getFormattedString("MekDisplay.AimingAt", ash.getAimingLocation()));

                } else {
                    toHit = WeaponAttackAction.toHit(game, currentEntity, target, weaponId, Entity.LOC_NONE,
                          AimingMode.NONE, false, false,
                          null, null, false, true,
                          WeaponAttackAction.UNASSIGNED, WeaponAttackAction.UNASSIGNED);
                    clientgui.getUnitDisplay().wPan.setTarget(target, null);
                }
                ash.setPartialCover(toHit.getCover());
            } else {
                toHit = WeaponAttackAction.toHit(game, currentEntity, target, weaponId, Entity.LOC_NONE,
                      AimingMode.NONE, false, false, null,
                      null, false, true,
                      WeaponAttackAction.UNASSIGNED, WeaponAttackAction.UNASSIGNED);
                clientgui.getUnitDisplay().wPan.setTarget(target, null);
            }
            int effectiveDistance = Compute.effectiveDistance(game, currentEntity(), target);
            clientgui.getUnitDisplay().wPan.wRangeR.setText("" + effectiveDistance);
            WeaponMounted m = currentEntity().getWeapon(weaponId);
            // If we have a Centurion Weapon System selected, we may need to
            // update ranges.
            if (m.getType().hasFlag(WeaponType.F_CWS)) {
                clientgui.getUnitDisplay().wPan.selectWeapon(weaponId);
            }

            if (m.isUsedThisRound()) {
                clientgui.getUnitDisplay().wPan.setToHit(Messages.getString("FiringDisplay.alreadyFired"));
                setFireEnabled(false);
            } else if ((m.getType().hasFlag(WeaponType.F_AUTO_TARGET) && !m.curMode().equals(Weapon.MODE_AMS_MANUAL))) {
                clientgui.getUnitDisplay().wPan.setToHit(Messages.getString("FiringDisplay.autoFiringWeapon"));
                setFireEnabled(false);
            } else if (toHit.getValue() == TargetRoll.IMPOSSIBLE) {
                clientgui.getUnitDisplay().wPan.setToHit(toHit);
                setFireEnabled(false);
            } else if (toHit.getValue() == TargetRoll.AUTOMATIC_FAIL) {
                clientgui.getUnitDisplay().wPan.setToHit(toHit);
                setFireEnabled(true);
            } else {
                boolean natAptGunnery = currentEntity().hasAbility(OptionsConstants.PILOT_APTITUDE_GUNNERY);
                clientgui.getUnitDisplay().wPan.setToHit(toHit, natAptGunnery);
                setFireEnabled(true);
            }
            setSkipEnabled(true);
        } else {
            clientgui.getUnitDisplay().wPan.setTarget(null, null);
            clientgui.getUnitDisplay().wPan.wRangeR.setText("---");
            clientgui.getUnitDisplay().wPan.clearToHit();
        }

        if ((weaponId != -1) && (currentEntity() != null)) {
            Mounted<?> m = currentEntity().getEquipment(weaponId);
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

        if (b.getType() == BoardViewEvent.BOARD_HEX_DRAGGED) {
            if (b.isShiftHeld() || twisting) {
                updateFlipArms(false);
                torsoTwist(b.getCoords());
            }
            b.getBoardView().cursor(b.getCoords());
        } else if (b.getType() == BoardViewEvent.BOARD_HEX_CLICKED) {
            twisting = false;
            if (!b.isShiftHeld()) {
                b.getBoardView().select(b.getCoords());
            }
        }
    }

    @Override
    public void hexSelected(BoardViewEvent event) {
        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        Coords evtCoords = event.getCoords();
        if (clientgui.isProcessingPointblankShot() && (evtCoords != null)
              && (currentEntity() != null)) {
            if (!evtCoords.equals(currentEntity().getPosition())) {
                if (event.isShiftHeld()) {
                    updateFlipArms(false);
                    torsoTwist(event.getCoords());
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
            updateFlipArms(!currentEntity().getArmsFlipped());
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
        if ((target instanceof Entity) && Compute.isGroundToAir(currentEntity(), target)) {
            ((Entity) target).setPlayerPickedPassThrough(currentEntity, null);
        }
        clearAttacks();
        clearMarkedHexes();
        refreshAll();
    }

    // board view listener
    @Override
    public void finishedMovingUnits(BoardViewEvent b) {
        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        if (clientgui.isProcessingPointblankShot() && (currentEntity() != null)) {
            clientgui.maybeShowUnitDisplay();
            clientgui.centerOnUnit(currentEntity());
        }
    }

    @Override
    public void unitSelected(BoardViewEvent event) {
        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        Entity entity = game.getEntity(event.getEntityId());
        if (entity != null) {
            if (clientgui.getPointblankEID() == entity.getId()) {
                selectEntity(entity.getId());
            } else {
                clientgui.maybeShowUnitDisplay();
                clientgui.getUnitDisplay().displayEntity(entity);
                if (entity.isDeployed()) {
                    clientgui.centerOnUnit(entity);
                }
            }
        }
    }

    @Override
    public void valueChanged(ListSelectionEvent event) {
        if (event.getValueIsAdjusting()) {
            return;
        }

        if (event.getSource() == clientgui.getUnitDisplay().wPan.weaponList) {
            updateTarget();
        }
    }
}
