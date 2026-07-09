/*
 * Copyright (C) 2023-2025 The MegaMek Team. All Rights Reserved.
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

import java.util.ArrayList;
import java.util.List;

import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.clientGUI.tooltip.EntityActionLog;
import megamek.client.ui.dialogs.TurretFacingDialog;
import megamek.common.actions.DirectionalMountFacingAction;
import megamek.common.actions.EntityAction;
import megamek.common.actions.FlipArmsAction;
import megamek.common.actions.TorsoTwistAction;
import megamek.common.annotations.Nullable;
import megamek.common.compute.TurretFacing;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.options.OptionsConstants;
import megamek.common.units.DirectionalTorsoMountRules;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.units.Tank;
import megamek.logging.MMLogger;

public abstract class AttackPhaseDisplay extends ActionPhaseDisplay {
    private static final MMLogger LOGGER = MMLogger.create(AttackPhaseDisplay.class);

    /**
     * Passed to {@link #pendingDirectionalMountFacings(int)} as the excluded location when the caller wants the pending
     * mount facing actions of every location preserved.
     */
    protected static final int NO_EXCLUDED_LOCATION = -1;

    // client list of attacks user has input
    protected EntityActionLog attacks;

    protected AttackPhaseDisplay(ClientGUI cg) {
        super(cg);
        attacks = new EntityActionLog(game);
    }

    /**
     * called by updateDonePanel to populate the label text of the Done button. Usually wraps a call to
     * Messages.getString(...Fire) but can be extended to add more details.
     *
     * @return text for label
     */
    abstract protected String getDoneButtonLabel();

    /**
     * called by updateDonePanel to populate the label text of the NoAction button. Usually wraps a call to
     * Messages.getString(...Skip) but can be extended to add more details.
     *
     * @return text for label
     */
    abstract protected String getSkipTurnButtonLabel();

    @Override
    protected void updateDonePanel() {
        if (attacks.isEmpty() || ((attacks.size() == 1) && (attacks.firstElement() instanceof TorsoTwistAction))) {
            // a torso twist alone should not trigger Done button
            updateDonePanelButtons(getDoneButtonLabel(), getSkipTurnButtonLabel(), false, null);
        } else {
            updateDonePanelButtons(getDoneButtonLabel(), getSkipTurnButtonLabel(), true, attacks.getDescriptions());
        }
    }

    protected void removeAttack(EntityAction o) {
        attacks.remove(o);
        updateDonePanel();
    }

    /** removes all elements from the local temporary attack list */
    protected void removeAllAttacks() {
        attacks.clear();
        updateDonePanel();
    }

    /** add an attack at the given index to the local temporary attack list */
    protected void addAttack(int index, EntityAction entityAction) {
        attacks.add(index, entityAction);
        updateDonePanel();
    }

    /** add an attack to the end of the local temporary attack list */
    protected void addAttack(EntityAction entityAction) {
        attacks.add(entityAction);
        updateDonePanel();
    }

    // --- Directional Torso Mount and turret rotation controls (BMM p.83; issues #1040, #6518) ---
    // These are shared by every attack-declaration display that offers the Flip Mount / Rotate Turret buttons
    // (the firing phase and the targeting/TAG phase), so the controls behave identically wherever a unit can
    // declare weapon attacks. Displays that do not offer the buttons inherit the no-op hooks below and are
    // unaffected.

    /**
     * Clears the pending attack queue and resets any declared facing. The default is a no-op; displays that hold a
     * pending attack list override this so {@link #flipDirectionalMount()} can rebuild the queue.
     */
    protected void clearAttacks() {}

    /**
     * Recomputes the to-hit / target information after a mount-facing change. The default is a no-op; attack displays
     * override this to refresh their target panel (they cannot share a single {@code updateTarget()} because its
     * visibility differs across the display family).
     */
    protected void refreshTargetAfterMountChange() {}

    /**
     * Enables or disables the Flip Mount button. The default is a no-op; displays that provide the button override this
     * to toggle their own button and menu item.
     *
     * @param enabled whether the Flip Mount button should be enabled
     */
    protected void setFlipMountEnabled(boolean enabled) {}

    /**
     * Enables or disables the Rotate Turret button. The default is a no-op; displays that provide the button override
     * this to toggle their own button and menu item.
     *
     * @param enabled whether the Rotate Turret button should be enabled
     */
    protected void setRotateTurretEnabled(boolean enabled) {}

    /**
     * Enables or disables the second Rotate Turret button (the rear turret of a dual-turret vehicle). The default is a
     * no-op; displays that provide the button override this to toggle their own button and menu item.
     *
     * @param enabled whether the rear-turret rotate button should be enabled
     */
    protected void setRotateRearTurretEnabled(boolean enabled) {}

    /**
     * Relabels the Rotate Turret button for the selected unit: on a dual-turret vehicle it reads "Rotate Fr. Turret"
     * (the second button covers the rear turret); on every other unit it reads the plain "Rotate Turret". The default
     * is a no-op; displays that provide the button override this.
     *
     * @param dualTurretTank {@code true} when the selected unit is a dual-turret vehicle
     */
    protected void setRotateTurretLabel(boolean dualTurretTank) {}

    /**
     * Flips the selected weapon's 2-point Directional Torso Mount between the front and rear arc (BMM p.83). A
     * Directional Torso Mount is a single mount holding every directional-mount weapon in one torso location, so the
     * whole mount flips together. The facing is queued as a {@link DirectionalMountFacingAction} (applied by the server
     * when the turn is committed) rather than sent immediately, so it composes with a torso twist or arms flip declared
     * in the same turn and can still be cleared.
     */
    public void flipDirectionalMount() {
        if (currentEntity() == null) {
            LOGGER.info("[DirTorsoMount] flip ignored - no current entity");
            return;
        }
        WeaponMounted weapon = clientgui.getUnitDisplay().wPan.getSelectedWeapon();
        int weaponNumber = clientgui.getUnitDisplay().wPan.getSelectedWeaponNum();
        if ((weapon == null) || (weaponNumber == -1)) {
            LOGGER.info("[DirTorsoMount] {}: flip ignored - no weapon selected (weaponNumber={})",
                  currentEntity().getShortName(), weaponNumber);
            return;
        }
        LOGGER.info("[DirTorsoMount] {}: flip requested - {}",
              currentEntity().getShortName(), weapon.directionalMountDebug());
        if (!weapon.hasDirectionalTorsoMount()) {
            LOGGER.info("[DirTorsoMount] {}: flip ignored - {} is not a directional mount",
                  currentEntity().getShortName(), weapon.getName());
            return;
        }
        if (weapon.hasDirectional360TorsoMount()) {
            LOGGER.info("[DirTorsoMount] {}: flip ignored - {} is a 360-degree turret (no front/rear to flip)",
                  currentEntity().getShortName(), weapon.getName());
            return;
        }
        if (weapon.isDirectionalMountLocked()) {
            LOGGER.info("[DirTorsoMount] {}: flip ignored - mount for {} is locked",
                  currentEntity().getShortName(), weapon.getName());
            return;
        }
        if (weapon.isDirectionalMountAlreadyFlipped()) {
            LOGGER.info("[DirTorsoMount] {}: flip ignored - mount for {} already changed facing in an earlier phase"
                  + " this turn (once per turn, BMM p.83)", currentEntity().getShortName(), weapon.getName());
            return;
        }
        boolean newRear = !weapon.isDirectionalMountRear();
        int newFacing = newRear ? 3 : 0;
        int flippedLocation = weapon.getLocation();

        // clearAttacks() below resets the secondary facing and drops every pending action; capture the declared
        // torso twist, arms-flip and any other-location mount facings so the flip does not undo them (BMM p.83:
        // the mount arc is declared alongside - and independently of - a torso twist).
        int twistedFacing = currentEntity().getSecondaryFacing();
        boolean wasTwisted = twistedFacing != currentEntity().getFacing();
        boolean wasArmsFlipped = currentEntity().getArmsFlipped();
        List<DirectionalMountFacingAction> otherMountFacings = pendingDirectionalMountFacings(flippedLocation);

        clearAttacks();

        if (wasTwisted) {
            currentEntity().setSecondaryFacing(twistedFacing);
            addAttack(new TorsoTwistAction(currentEntity, twistedFacing));
        }
        if (wasArmsFlipped) {
            currentEntity().setArmsFlipped(true);
            addAttack(new FlipArmsAction(currentEntity, true));
        }
        for (DirectionalMountFacingAction mountFacing : otherMountFacings) {
            addAttack(mountFacing);
        }

        // The whole mount (every directional weapon in this location) shares one facing, so flip them together.
        DirectionalTorsoMountRules.setMountFacing(currentEntity(), flippedLocation, newFacing);
        addAttack(new DirectionalMountFacingAction(currentEntity, weaponNumber, newFacing));
        LOGGER.info("[DirTorsoMount] {}: {} flipped to {} (facing offset {}); queued facing action",
              currentEntity().getShortName(), weapon.getName(), newRear ? "REAR" : "FRONT", newFacing);
        refreshTargetAfterMountChange();
        // Rebuild the weapon display and reselect the flipped weapon (mirrors the mode-change flow). A full refresh
        // is avoided here: it selects the first weapon and short-circuits the unit-display rebuild when the entity
        // object is unchanged, which is what dropped the selection and left the arc indicator stale.
        clientgui.onAllBoardViews(boardView -> boardView.redrawEntity(currentEntity()));
        clientgui.getUnitDisplay().wPan.displayMek(currentEntity());
        clientgui.getUnitDisplay().wPan.selectWeapon(weapon);
        updateDonePanel();
        clientgui.updateFiringArc(currentEntity());
    }

    /**
     * Collects the pending Directional Torso Mount facing actions (BMM p.83) so they can be re-added after a
     * {@link #clearAttacks()} that is only meant to rebuild the weapon attacks (a torso twist or another mount flip).
     * The mount arc is a persistent, separately-declared state, not a weapon attack, so it must survive the clear.
     *
     * @param excludedLocation a location to omit because the caller re-declares it itself, or
     *                         {@link #NO_EXCLUDED_LOCATION} to keep all
     *
     * @return the pending mount facing actions to preserve
     */
    protected List<DirectionalMountFacingAction> pendingDirectionalMountFacings(int excludedLocation) {
        List<DirectionalMountFacingAction> mountFacings = new ArrayList<>();
        if (currentEntity() == null) {
            return mountFacings;
        }
        for (EntityAction action : attacks) {
            if (action instanceof DirectionalMountFacingAction mountFacing) {
                Mounted<?> mount = currentEntity().getEquipment(mountFacing.getWeaponNumber());
                if ((mount != null) && (mount.getLocation() != excludedLocation)) {
                    mountFacings.add(mountFacing);
                }
            }
        }
        return mountFacings;
    }

    /**
     * Enables the flip-mount button only when the selected weapon is in a 2-point Directional Torso Mount whose arc can
     * still be changed - i.e. it is a directional mount, is not a 360-degree turret (which has no front/rear to flip),
     * has not been locked by damage, and has not already changed facing in an earlier phase this turn (once per turn,
     * like a torso twist). Logs the decision for any directional-mount unit to aid playtesting.
     */
    protected void updateFlipMount() {
        WeaponMounted weapon = clientgui.getUnitDisplay().wPan.getSelectedWeapon();
        if ((currentEntity() == null) || (weapon == null)) {
            setFlipMountEnabled(false);
            return;
        }
        boolean canFlip = weapon.hasDirectionalTorsoMount()
              && !weapon.hasDirectional360TorsoMount()
              && !weapon.isDirectionalMountLocked()
              && !weapon.isDirectionalMountAlreadyFlipped();
        boolean unitHasDirectionalMount =
              currentEntity().hasQuirk(OptionsConstants.QUIRK_POS_DIRECTIONAL_TORSO_MOUNT)
                    || currentEntity().hasQuirk(OptionsConstants.QUIRK_POS_DIRECTIONAL_TORSO_MOUNT_360)
                    || weapon.hasDirectionalTorsoMount();
        if (unitHasDirectionalMount) {
            LOGGER.info("[DirTorsoMount] {}: flip-mount button {} - {}",
                  currentEntity().getShortName(), canFlip ? "ENABLED" : "disabled",
                  weapon.directionalMountDebug());
        }
        setFlipMountEnabled(canFlip);
    }

    /**
     * Opens the facing dialog for the selected weapon's rotatable mount - a Mek shoulder/head/quad turret, a vehicle
     * dual turret, or a Directional Torso Mount (issues #1040, #6518) - so turrets can be aimed with an action button,
     * not only the right-click menu.
     */
    public void rotateSelectedMount() {
        Entity entity = currentEntity();
        if (entity == null) {
            return;
        }
        // Vehicle turrets rotate as a whole, independent of the selected weapon: on a dual-turret vehicle this
        // button covers the front turret ("Rotate Fr. Turret"; the rear turret has its own button), on a
        // single-turret vehicle the main turret.
        if (entity instanceof Tank tank) {
            if (!tank.hasNoDualTurret()) {
                new TurretFacingDialog(clientgui.getFrame(), tank, clientgui).setVisible(true);
            } else if (!tank.hasNoTurret()) {
                // The main turret follows the unit's secondary facing, so rotating it is a turret twist: the dialog
                // only picks the facing and the twist is declared through the same path as the Twist button.
                new TurretFacingDialog(clientgui.getFrame(), tank, clientgui, this::declareSecondaryFacing)
                      .setVisible(true);
            }
            return;
        }
        WeaponMounted weapon = clientgui.getUnitDisplay().wPan.getSelectedWeapon();
        if (weapon == null) {
            return;
        }
        if (weapon.hasDirectionalTorsoMount() && (entity instanceof Mek directionalMek)) {
            new TurretFacingDialog(clientgui.getFrame(), directionalMek, weapon, clientgui, true).setVisible(true);
        } else if (weapon.isMekTurretMounted() && (entity instanceof Mek turretMek)) {
            Mounted<?> turretItem = findMekTurretItem(turretMek, weapon.getLocation());
            if (turretItem != null) {
                new TurretFacingDialog(clientgui.getFrame(), turretMek, turretItem, clientgui).setVisible(true);
            }
        }
    }

    /**
     * Opens the facing dialog for a dual-turret vehicle's rear (main) turret - the "Rotate Rr. Turret" button. The rear
     * turret follows the unit's secondary facing, so the rotation is declared as a turret twist.
     */
    public void rotateRearTurret() {
        if ((currentEntity() instanceof Tank tank) && !tank.hasNoDualTurret()) {
            new TurretFacingDialog(clientgui.getFrame(), tank, clientgui, this::declareSecondaryFacing)
                  .setVisible(true);
        }
    }

    /**
     * Declares a change of the unit's secondary facing (a torso/turret twist) to the given facing, as if the player had
     * used the Twist control. The default is a no-op; attack displays override this with their twist-declaration path
     * (which clears pending attacks like any twist). Used by {@link #rotateSelectedMount()} for vehicle main turrets,
     * whose rotation is a turret twist.
     *
     * @param facing the absolute facing (0-5) to twist to
     */
    protected void declareSecondaryFacing(int facing) {}

    /**
     * @param mek      the unit
     * @param location the location holding a Mek turret
     *
     * @return the turret {@link MiscType} equipment item in the location (shoulder/head/quad turret), or {@code null}
     */
    private @Nullable Mounted<?> findMekTurretItem(Mek mek, int location) {
        for (Mounted<?> miscMounted : mek.getMisc()) {
            boolean isInRequestedLocation = miscMounted.getLocation() == location;
            boolean isMekTurret = TurretFacing.isMekTurretItem(miscMounted);
            if (isInRequestedLocation && isMekTurret) {
                return miscMounted;
            }
        }
        return null;
    }

    /**
     * Enables the rotate-turret buttons. Vehicle turrets rotate as a whole (independent of the selected weapon): a
     * dual-turret vehicle gets both buttons - front turret on the first, rear turret on the second - while a
     * single-turret vehicle gets only the first, for its main turret. On Meks the first button follows the selected
     * weapon's rotatable mount (a Mek turret or a 360-degree Directional Torso Mount) as long as its facing can still
     * be changed.
     */
    protected void updateRotateTurret() {
        Entity entity = currentEntity();
        if (entity == null) {
            setRotateTurretEnabled(false);
            setRotateRearTurretEnabled(false);
            return;
        }
        if (entity instanceof Tank tank) {
            updateTankRotateButtons(tank);
            return;
        }
        setRotateTurretLabel(false);
        setRotateRearTurretEnabled(false);
        WeaponMounted weapon = clientgui.getUnitDisplay().wPan.getSelectedWeapon();
        if (weapon == null) {
            setRotateTurretEnabled(false);
            return;
        }
        // A 2-point Directional Torso Mount is flipped front/rear with the Flip Mount button, so it does not use
        // the rotate dialog; only the 3-point 360 quad turret (and Mek turrets) use the rotate button.
        boolean isTwoPointDirectionalMount = weapon.hasDirectionalTorsoMount() && !weapon.hasDirectional360TorsoMount();
        // A directional mount locked by damage, or already refaced in an earlier phase this turn, cannot rotate.
        boolean isUnavailableDirectionalMount = weapon.hasDirectionalTorsoMount()
              && (weapon.isDirectionalMountLocked() || weapon.isDirectionalMountAlreadyFlipped());
        boolean canRotate = TurretFacing.isRotatable(entity, weapon)
              && !isTwoPointDirectionalMount
              && !isUnavailableDirectionalMount;
        setRotateTurretEnabled(canRotate);
    }

    /**
     * Enables and labels the rotate buttons for a vehicle. The front (dual) turret's facing is a freely-set offset and
     * only needs its mechanism intact; the main turret follows the unit's secondary facing, so its rotation needs the
     * turret twist to still be available (turret intact, not yet twisted this turn, crew active - matching the Twist
     * button). On a dual-turret vehicle the main turret is the rear turret and uses the second button.
     *
     * @param tank the selected vehicle
     */
    private void updateTankRotateButtons(Tank tank) {
        boolean canTwistTurret = tank.canChangeSecondaryFacing() && tank.getCrew().isActive();
        boolean hasDualTurret = !tank.hasNoDualTurret();
        setRotateTurretLabel(hasDualTurret);
        if (hasDualTurret) {
            setRotateTurretEnabled(!tank.isTurretLocked(tank.getLocTurret2()));
            setRotateRearTurretEnabled(canTwistTurret);
        } else {
            setRotateTurretEnabled(!tank.hasNoTurret() && canTwistTurret);
            setRotateRearTurretEnabled(false);
        }
    }
}
