/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.dialogs.unitDisplay;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.clientGUI.boardview.overlay.ToastLevel;
import megamek.client.ui.dialogs.phaseDisplay.EcmSuiteChoiceDialog;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.EquipmentActivation;
import megamek.common.equipment.EquipmentMode;
import megamek.common.equipment.MiscMounted;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponType;
import megamek.common.equipment.enums.MiscTypeFlag;
import megamek.common.game.Game;
import megamek.common.units.Entity;

/**
 * What a player does to a piece of equipment during a game: switch its mode, or dump its ammo. These are the rules
 * the Systems tab has always applied - which phase a booby trap can be armed in, that a shield changes mode in the
 * firing phase, that only one ECM suite runs at a time - shared so the Control tab applies exactly the same ones.
 * Every change is sent to the server and announced to the player here.
 */
final class EquipmentActions {

    private EquipmentActions() {
        // static use only
    }

    /**
     * Switches the equipment to the given mode, if the rules allow it now, and sends the change.
     *
     * @param unitDisplayPanel the display the request comes from; its client sends the change
     * @param entity           the unit the equipment is on
     * @param mounted          the equipment
     * @param modeIndex        the index of the mode in the equipment type's mode list
     *
     * @return {@code true} if the change was made (or queued for the end of the phase); {@code false} if it was
     *       refused, in which case the player has been told why
     */
    static boolean changeMode(UnitDisplayPanel unitDisplayPanel, Entity entity, Mounted<?> mounted, int modeIndex) {
        ClientGUI clientgui = unitDisplayPanel.getClientGUI();
        if ((clientgui == null) || (mounted == null) || !mounted.hasModes() || (modeIndex < 0)) {
            return false;
        }
        Game game = clientgui.getClient().getGame();
        if ((mounted.getType() instanceof MiscType miscType) && miscType.isBoobyTrap()) {
            // Verify is it is in the correct phase to arm it
            // This should be controlled by the equipment itself
            // TODO: Refactor so the equipment knows the phase they can be armed/disarmed
            if (game.getPhase().isFiring() || game.getPhase().isPhysical()) {
                if ((modeIndex == 1) && !clientgui.doYesNoDialog(Messages.getString("MekDisplay.BoobyTrapWarningTitle"),
                      Messages.getString("MekDisplay.BoobyTrapWarning"))) {
                    return false;
                }
            } else {
                clientgui.addToast(ToastLevel.WARNING, Messages.getString("MekDisplay.BoobyTrapMode"));
                return false;
            }
        }
        if ((mounted.getType() instanceof MiscType miscType)
              && miscType.hasFlag(MiscType.F_SHIELD)
              && !Game.rulesManager.getRulesPhysical().phaseChangeShield()
              && !game.getPhase().isFiring()) {
            clientgui.systemMessage(Messages.getString("MekDisplay.ShieldModePhase"));
            return false;
        }
        if ((mounted.getType() instanceof MiscType miscType)
              && miscType.isVibroblade()
              && !game.getPhase().isPhysical()) {
            clientgui.systemMessage(Messages.getString("MekDisplay.VibrobladeModePhase"));
            return false;
        }
        if ((mounted.getType() instanceof MiscType)
              && mounted.getType().hasFlag(MiscTypeFlag.S_RETRACTABLE_BLADE)
              && !game.getPhase().isMovement()) {
            clientgui.systemMessage(Messages.getString("MekDisplay.RetractableBladeModePhase"));
            return false;
        }
        // Can only charge a capacitor if the weapon has not been fired.
        if ((mounted.getType() instanceof MiscType)
              && (mounted.getLinked() != null)
              && mounted.getType().hasFlag(MiscType.F_PPC_CAPACITOR)
              && mounted.getLinked().isUsedThisRound()
              && (modeIndex == 1)) {
            clientgui.systemMessage(Messages.getString("MekDisplay.CapacitorCharging"));
            return false;
        }
        if (!resolveEcmSuiteConflict(clientgui, entity, mounted, modeIndex)) {
            return false;
        }
        mounted.setMode(modeIndex);
        clientgui.getClient().sendModeChange(entity.getId(), entity.getEquipmentNum(mounted), modeIndex);
        // These report the state the equipment has reached, or will reach, so they take the state label rather
        // than the label the player picked from the dropdown
        if (mounted.canInstantSwitch(modeIndex)) {
            clientgui.systemMessage(Messages.getString("MekDisplay.switched",
                  mounted.getName(), mounted.curMode().getStateName(mounted.getType())));
            clientgui.addToast(ToastLevel.INFO,
                  mounted.getName() + ": " + mounted.curMode().getStateName(mounted.getType()), entity);
            int weapon = unitDisplayPanel.getWeaponTab().getSelectedWeaponNum();
            unitDisplayPanel.getWeaponTab().displayMek(entity);
            unitDisplayPanel.getWeaponTab().selectWeapon(weapon);
        } else {
            String pendingModeName = mounted.pendingMode().getStateName(mounted.getType());
            if (game.getPhase().isDeployment()) {
                clientgui.systemMessage(Messages.getString("MekDisplay.willSwitchAtStart",
                      mounted.getName(), pendingModeName));
            } else {
                clientgui.systemMessage(Messages.getString("MekDisplay.willSwitchAtEnd",
                      mounted.getName(), pendingModeName));
            }
            clientgui.addToast(ToastLevel.INFO, mounted.getName() + " -> " + pendingModeName, entity);
        }
        return true;
    }

    /**
     * Starts dumping the equipment's ammo (or jettisoning the equipment), or cancels a dump already under way,
     * after asking the player. Only the unit's owner may.
     *
     * @param unitDisplayPanel the display the request comes from; its client sends the change
     * @param entity           the unit the equipment is on
     * @param mounted          the ammo bin, or the jettisonable equipment
     *
     * @return {@code true} if anything was changed
     */
    static boolean toggleDump(UnitDisplayPanel unitDisplayPanel, Entity entity, Mounted<?> mounted) {
        ClientGUI clientgui = unitDisplayPanel.getClientGUI();
        if ((clientgui == null) || (mounted == null)
              || !clientgui.getClient().getLocalPlayer().equals(entity.getOwner())) {
            return false;
        }
        boolean changed = false;
        // Check for BA dumping SRM launchers
        if ((entity instanceof BattleArmor) && (!mounted.isMissing())
              && mounted.isBodyMounted()
              && mounted.getType().hasFlag(WeaponType.F_MISSILE)
              && (mounted.getLinked() != null)
              && (mounted.getLinked().getUsableShotsLeft() > 0)) {
            boolean isDumping = !mounted.isPendingDump();
            mounted.setPendingDump(isDumping);
            clientgui.getClient().sendModeChange(entity.getId(), entity.getEquipmentNum(mounted), isDumping ? -1 : 0);
            changed = true;
        }
        if (((!(mounted.getType() instanceof AmmoType) || (mounted.getUsableShotsLeft() <= 0))
              && !mounted.isDWPMounted()) || (mounted.isDWPMounted() && mounted.isMissing())) {
            return changed;
        }
        boolean dumping = !mounted.isPendingDump();
        boolean isAmmo = mounted.getType() instanceof AmmoType;
        String titleKey;
        String messageKey;
        if (dumping) {
            titleKey = isAmmo ? "MekDisplay.Dump.title" : "MekDisplay.Jettison.title";
            messageKey = isAmmo ? "MekDisplay.Dump.message" : "MekDisplay.Jettison.message";
        } else {
            titleKey = isAmmo ? "MekDisplay.CancelDumping.title" : "MekDisplay.CancelJettison.title";
            messageKey = isAmmo ? "MekDisplay.CancelDumping.message" : "MekDisplay.CancelJettison.message";
        }
        boolean confirmed = clientgui.doYesNoDialog(Messages.getString(titleKey),
              Messages.getString(messageKey, mounted.getName()));
        if (confirmed) {
            mounted.setPendingDump(dumping);
            clientgui.getClient().sendModeChange(entity.getId(), entity.getEquipmentNum(mounted), dumping ? -1 : 0);
            changed = true;
        }
        return changed;
    }

    /**
     * Asks the player which ECM suite to leave on when the mode they just picked would put a second one into use, and
     * switches off the suites they did not keep. A unit may use only one ECM suite at a time, of any type (TM p.213,
     * CO p.200), and every mode other than {@code "Off"} counts as using the suite - ECCM and Ghost Targets included.
     *
     * <p>The choice is always between the suite already in use and the one the player just switched, so keeping the
     * suite that was already on means abandoning the requested switch. Cancelling does the same.</p>
     *
     * @param clientgui        the client GUI, used for the parent frame and to send the switches that are applied
     * @param entity           the unit
     * @param changedEquipment the equipment whose mode the player just picked
     * @param newMode          the requested mode index
     *
     * @return {@code true} if the requested mode change should go ahead, {@code false} if it should be abandoned
     */
    private static boolean resolveEcmSuiteConflict(ClientGUI clientgui, Entity entity, Mounted<?> changedEquipment,
          int newMode) {
        if (!(changedEquipment instanceof MiscMounted changedSuite)
              || !changedSuite.getType().hasFlag(MiscType.F_ECM)) {
            return true;
        }
        if ((newMode < 0) || (newMode >= changedSuite.getType().getModesCount())) {
            return true;
        }
        EquipmentMode requestedMode = changedSuite.getType().getMode(newMode);
        if (requestedMode.equals(Mounted.MODE_OFF)) {
            return true;
        }
        // Walk the mounts rather than appending the changed suite to the list of those already in use, so the
        // dialog offers the suites in mount order and its numbering runs 1, 2, 3 down the list
        List<MiscMounted> alreadyInUse = EquipmentActivation.ecmSuitesInUseNextRound(entity);
        List<MiscMounted> suitesInUse = new ArrayList<>();
        for (MiscMounted suite : entity.getMisc()) {
            if (suite.equals(changedSuite) || alreadyInUse.contains(suite)) {
                suitesInUse.add(suite);
            }
        }
        if (suitesInUse.size() < 2) {
            return true;
        }
        // Making room for this suite means switching another one off, which the server refuses for any ECM suite
        // while stealth armor is engaged or engaging - so there is no choice to offer here
        if (EquipmentActivation.isStealthOnOrActivating(entity)) {
            clientgui.systemMessage(Messages.getString("MekDisplay.EcmSuiteStealthEngaged",
                  EquipmentActivation.ecmSuiteLabel(entity, changedSuite)));
            return false;
        }
        // The mode the player just picked has not been applied yet, so it has to be named for the dialog rather
        // than read back off the equipment
        Map<MiscMounted, String> suiteModeNames = new LinkedHashMap<>();
        for (MiscMounted suite : suitesInUse) {
            suiteModeNames.put(suite, suite.equals(changedSuite)
                  ? requestedMode.getStateName(changedSuite.getType())
                  : suite.modeNextRound().getStateName(suite.getType()));
        }
        MiscMounted keptSuite = EcmSuiteChoiceDialog.showSingleChoiceDialog(clientgui.getFrame(), entity,
              suiteModeNames);
        if (!changedSuite.equals(keptSuite)) {
            clientgui.systemMessage(Messages.getString("MekDisplay.EcmSuiteNotSwitched",
                  EquipmentActivation.ecmSuiteLabel(entity, changedSuite)));
            return false;
        }
        for (MiscMounted suite : suitesInUse) {
            if (suite.equals(keptSuite)) {
                continue;
            }
            int offModeIndex = suite.setMode(Mounted.MODE_OFF);
            if (offModeIndex >= 0) {
                clientgui.getClient().sendModeChange(entity.getId(), entity.getEquipmentNum(suite), offModeIndex);
                clientgui.systemMessage(Messages.getString("MekDisplay.willSwitchAtEnd",
                      EquipmentActivation.ecmSuiteLabel(entity, suite),
                      suite.pendingMode().getStateName(suite.getType())));
            }
        }
        return true;
    }
}
