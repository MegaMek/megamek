/*

 * Copyright (C) 2007 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2007-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.comparators;

import java.util.Comparator;

import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;

/**
 * Comparator for sorting Weapons (Mounted that have WeaponTypes) by damage with ties arbitrated based on heat (lower
 * heat wins).
 *
 * @author arlith
 */
public class WeaponComparatorDamage implements Comparator<WeaponMounted> {
    /**
     * Value used to change order from ascending to descending. If descending, value will be -1 and orders will be
     * multiplied by -1.
     */
    private int ascending = 1;

    public WeaponComparatorDamage(boolean ascending) {
        if (!ascending) {
            this.ascending = -1;
        }
    }

    @Override
    public int compare(WeaponMounted lhs, WeaponMounted rhs) {
        WeaponType leftWeaponType = lhs.getType();
        WeaponType rightWeaponType = rhs.getType();

        // If types are equal, pick front facing first
        if (leftWeaponType == rightWeaponType) {
            if (lhs.isRearMounted()) {
                return -1 * ascending;
            } else if (rhs.isRearMounted()) {
                return ascending;
            } else {
                return 0;
            }
        }
        // Pick the weapon with the highest damage
        if (leftWeaponType.getDamage() > rightWeaponType.getDamage()) {
            return ascending;
        } else if (leftWeaponType.getDamage() < rightWeaponType.getDamage()) {
            return -1 * ascending;
        } else { // Break ties with heat
            if (leftWeaponType.getHeat() > rightWeaponType.getHeat()) {
                return ascending;
            } else if (leftWeaponType.getHeat() < rightWeaponType.getHeat()) {
                return -1 * ascending;
            } else {
                return 0;
            }
        }
    }
}
