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

package megamek.common.equipment;

import java.util.ArrayList;
import java.util.List;

import megamek.common.annotations.Nullable;
import megamek.common.units.Entity;

/**
 * Stateless queries for the equipment activation/deactivation rules (equipment with an {@code "Off"} mode: active
 * probes, ECM suites, C3 computers, heat sinks, gauss rifles, improved heavy lasers, and similar). Deactivated
 * equipment provides none of its game effects but is otherwise undamaged; state that encodes relationships (such as
 * C3 network membership) survives deactivation so that reactivating the equipment restores it.
 */
public final class EquipmentActivation {

    private EquipmentActivation() {
    }

    /**
     * Returns whether the player has deactivated the unit's C3 network gear (C3 slave/master, C3i, Naval C3 or Nova
     * CEWS). A switched-off unit provides and receives no network benefit, but its network membership
     * ({@code c3NetIdString}, master link, partner UUIDs) is preserved, so reactivating the equipment automatically
     * restores the previous network.
     *
     * @param entity the unit to check
     *
     * @return {@code true} if the unit mounts operable C3 network equipment and all of it is currently set to
     *       {@code "Off"}
     */
    public static boolean isC3SwitchedOff(Entity entity) {
        // An activated C3 Emergency Master may not be deliberately switched off (TO:AUE p.110)
        if (entity.isC3EmergencyMasterActive()) {
            return false;
        }
        boolean hasOperableC3Equipment = false;
        for (Mounted<?> mounted : entity.getEquipment()) {
            EquipmentType equipmentType = mounted.getType();
            boolean isC3Equipment;
            if (equipmentType instanceof MiscType miscType) {
                isC3Equipment = miscType.hasFlag(MiscType.F_C3S) || miscType.hasFlag(MiscType.F_C3SBS)
                      || miscType.hasFlag(MiscType.F_C3I) || miscType.hasFlag(MiscType.F_NAVAL_C3)
                      || miscType.hasFlag(MiscType.F_NOVA);
            } else if (equipmentType instanceof WeaponType weaponType) {
                isC3Equipment = weaponType.hasFlag(WeaponType.F_C3M) || weaponType.hasFlag(WeaponType.F_C3MBS);
            } else {
                isC3Equipment = false;
            }
            if (isC3Equipment && !mounted.isInoperable()) {
                hasOperableC3Equipment = true;
                if (!mounted.isModeTurnedOff()) {
                    return false;
                }
            }
        }
        return hasOperableC3Equipment;
    }

    /**
     * Returns whether the unit's stealth armor system is engaged now or will be engaged next round (a pending switch
     * to {@code "On"}). Used to validate that the ECM suite stealth armor depends on is not deactivated in the same
     * round the stealth system is being engaged.
     *
     * @param entity the unit to check
     *
     * @return {@code true} if an operable stealth armor system is in the {@code "On"} mode now or as its next-round
     *       mode
     */
    public static boolean isStealthOnOrActivating(Entity entity) {
        for (MiscMounted mounted : entity.getMisc()) {
            MiscType miscType = mounted.getType();
            if (miscType == null) {
                continue;
            }
            boolean isEngagedOrEngaging = mounted.curMode().equals(Mounted.MODE_ON)
                  || mounted.modeNextRound().equals(Mounted.MODE_ON);
            if (miscType.hasFlag(MiscType.F_STEALTH) && !mounted.isInoperable() && isEngagedOrEngaging) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns whether the unit will have an ECM suite operating next round, i.e. one that is operable and not
     * deactivated or switching to {@code "Off"}. Stealth armor requires such a suite, so engaging stealth is
     * refused while this returns {@code false}.
     *
     * @param entity the unit to check
     *
     * @return {@code true} if an operable ECM suite will not be in the {@code "Off"} mode next round
     */
    public static boolean hasEcmAvailableForStealth(Entity entity) {
        for (MiscMounted mounted : entity.getMisc()) {
            MiscType miscType = mounted.getType();
            if (miscType == null) {
                continue;
            }
            if (miscType.hasFlag(MiscType.F_ECM) && !mounted.isInoperable()
                  && !mounted.isModeTurnedOffNextRound()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the unit's ECM suites that will be in use next round, i.e. those that are operable and whose next-round
     * mode is anything other than {@code "Off"}. A suite set to ECCM or Ghost Targets is being used just as much as one
     * set to ECM, so every non-{@code "Off"} mode counts. The next-round mode is the one that matters because ECM
     * suites are built with {@code setInstantModeSwitch(false)}: a switch declared now takes effect in the End Phase
     * (TO:AUE p.90).
     * <p>
     * A unit may only use one ECM suite at a time, of any type (TM p.213, CO p.200), so a returned list holding more
     * than one entry describes a state the rules do not allow.
     * </p>
     *
     * @param entity the unit to check
     *
     * @return a new modifiable list of the operable ECM suites that are not deactivated next round, in mount order
     */
    public static List<MiscMounted> ecmSuitesInUseNextRound(Entity entity) {
        List<MiscMounted> suitesInUse = new ArrayList<>();
        for (MiscMounted mounted : entity.getMisc()) {
            MiscType miscType = mounted.getType();
            if (miscType == null) {
                continue;
            }
            if (miscType.hasFlag(MiscType.F_ECM) && !mounted.isInoperable()
                  && !mounted.isModeTurnedOffNextRound()) {
                suitesInUse.add(mounted);
            }
        }
        return suitesInUse;
    }

    /**
     * Returns the ECM suite to keep when the game has to resolve a multiple-suite conflict without asking the player,
     * such as a unit that deploys with several suites already active. Angel ECM wins over any other type, matching the
     * precedence {@code ECCMComparator} already applies to competing ECM fields; otherwise the first suite in mount
     * order is kept so that the choice is stable from one game to the next.
     *
     * @param ecmSuites the candidate suites, normally the result of {@link #ecmSuitesInUseNextRound(Entity)}
     *
     * @return the suite to leave on, or {@code null} if the list is empty
     */
    public static @Nullable MiscMounted preferredEcmSuite(List<MiscMounted> ecmSuites) {
        MiscMounted preferred = null;
        for (MiscMounted mounted : ecmSuites) {
            MiscType miscType = mounted.getType();
            if (miscType == null) {
                continue;
            }
            if (preferred == null) {
                preferred = mounted;
            } else if (miscType.hasFlag(MiscType.F_ANGEL_ECM)
                  && !preferred.getType().hasFlag(MiscType.F_ANGEL_ECM)) {
                preferred = mounted;
            }
        }
        return preferred;
    }

    /**
     * Returns a label that identifies one ECM suite among all of the unit's ECM suites, such as
     * {@code "ECM Suite (Guardian) #2 (Body)"}. The number counts ECM suites in mount order and is only added when the
     * unit carries more than one, because a unit can mount two suites of the same type in the same location - the
     * Mantis Light Attack VTOL (ECCM) carries two Guardian suites in its Body - and neither the equipment name nor the
     * location tells those apart. The client dialog, the lobby and the server chat all build the label here so that
     * they name the same suite the same way.
     *
     * @param entity  the unit carrying the suite
     * @param ecmSuite the ECM suite to label
     *
     * @return the display label for the suite
     */
    public static String ecmSuiteLabel(Entity entity, MiscMounted ecmSuite) {
        int suiteNumber = 0;
        int suiteCount = 0;
        for (MiscMounted mounted : entity.getMisc()) {
            MiscType miscType = mounted.getType();
            if ((miscType == null) || !miscType.hasFlag(MiscType.F_ECM)) {
                continue;
            }
            suiteCount++;
            if (mounted.equals(ecmSuite)) {
                suiteNumber = suiteCount;
            }
        }
        StringBuilder label = new StringBuilder(ecmSuite.getName());
        if (suiteCount > 1) {
            label.append(" #").append(suiteNumber);
        }
        return label.append(" (").append(entity.getLocationName(ecmSuite.getLocation())).append(')').toString();
    }
}
