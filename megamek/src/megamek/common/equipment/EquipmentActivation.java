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
        boolean hasOperableC3Equipment = false;
        for (Mounted<?> mounted : entity.getEquipment()) {
            boolean isC3Equipment;
            if (mounted.getType() instanceof MiscType miscType) {
                isC3Equipment = miscType.hasFlag(MiscType.F_C3S) || miscType.hasFlag(MiscType.F_C3SBS)
                      || miscType.hasFlag(MiscType.F_C3I) || miscType.hasFlag(MiscType.F_NAVAL_C3)
                      || miscType.hasFlag(MiscType.F_NOVA);
            } else if (mounted.getType() instanceof WeaponType weaponType) {
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
            if (mounted.getType().hasFlag(MiscType.F_STEALTH) && !mounted.isInoperable()
                  && (mounted.curMode().equals("On") || mounted.modeNextRound().equals("On"))) {
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
            if (mounted.getType().hasFlag(MiscType.F_ECM) && !mounted.isInoperable()
                  && !mounted.isModeTurnedOffNextRound()) {
                return true;
            }
        }
        return false;
    }
}
