/*

 * Copyright (C) 2007 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common;

import java.util.Comparator;

import megamek.common.equipment.WeaponMounted;

/**
 * Comparator for sorting Weapons (Mounteds that have WeaponTypes) by damage with ties arbitrated based on heat (lower
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
    public int compare(WeaponMounted obj1, WeaponMounted obj2) {
        WeaponType weap1 = obj1.getType();
        WeaponType weap2 = obj2.getType();

        // If types are equal, pick front facing first
        if (weap1 == weap2) {
            if (obj1.isRearMounted()) {
                return -1 * ascending;
            } else if (obj2.isRearMounted()) {
                return ascending;
            } else {
                return 0;
            }
        }
        // Pick the weapon with the highest damage
        if (weap1.getDamage() > weap2.getDamage()) {
            return ascending;
        } else if (weap1.getDamage() < weap2.getDamage()) {
            return -1 * ascending;
        } else { // Break ties with heat
            if (weap1.getHeat() > weap2.getHeat()) {
                return ascending;
            } else if (weap1.getHeat() < weap2.getHeat()) {
                return -1 * ascending;
            } else {
                return 0;
            }
        }
    }
}
