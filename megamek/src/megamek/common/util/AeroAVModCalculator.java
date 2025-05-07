/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek
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
package megamek.common.util;

import megamek.common.EquipmentType;
import megamek.common.MiscType;
import megamek.common.WeaponType;
import megamek.common.weapons.lrms.LRMWeapon;
import megamek.common.weapons.srms.SRMWeapon;

/**
 * This whole class is a utility to calculate the aerospace AV modification
 * for linked Artemis, Apollo, or PPC capacitor.
 */
public class AeroAVModCalculator {
    /**
     * Computes any modification to the aerospace AV for linked Artemis, Apollo, or PPC capacitor
     *
     * @param weapon   The type of weapon
     * @param linkedBy The type of equipment the weapon is linked by
     * @param bay      Whether the weapon is part of a weapon bay
     * @return         The AV modification, if any
     */
    public static int calculateBonus(WeaponType weapon, EquipmentType linkedBy, boolean bay) {
        if (linkedBy.hasFlag(MiscType.F_ARTEMIS)
                || linkedBy.hasFlag(MiscType.F_ARTEMIS_V)) {
            // The 9 and 10 rows of the cluster hits table is only different in the 3 column
            if (weapon.getAtClass() == WeaponType.CLASS_MML) {
                if (weapon.getRackSize() >= 7) {
                    return 2;
                } else if ((weapon.getRackSize() >= 5) || linkedBy.hasFlag(MiscType.F_ARTEMIS_V)) {
                    return 1;
                }
            } else if (weapon instanceof LRMWeapon) {
                return weapon.getRackSize() / 5;
            } else if (weapon instanceof SRMWeapon) {
                return 2;
            }
        } else if (linkedBy.hasFlag(MiscType.F_ARTEMIS_PROTO) && weapon.getRackSize() == 2) {
            // The +1 cluster hit bonus only adds a missile hit for SRM2
            return 2;
        } else if (bay && linkedBy.hasFlag(MiscType.F_PPC_CAPACITOR)) {
            // PPC capacitors in weapon bays are treated as if always charged
            return 5;
        }
        return 0;
    }
}
