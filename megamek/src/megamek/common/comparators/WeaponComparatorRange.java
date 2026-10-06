/*

 * Copyright (C) 2007 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2014-2025 The MegaMek Team. All Rights Reserved.
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

import megamek.common.RangeType;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;

/**
 * Comparator for sorting Weapons (Mounted that have WeaponTypes) by Range.
 *
 * @author arlith
 */
public class WeaponComparatorRange implements Comparator<WeaponMounted> {

    /**
     * Value used to change order from ascending to descending. If descending, value will be -1 and orders will be
     * multiplied by -1.
     */
    private int ascending = 1;

    public WeaponComparatorRange(boolean ascending) {
        if (!ascending) {
            this.ascending = -1;
        }
    }

    @Override
    public int compare(WeaponMounted firstWeapon,
                       WeaponMounted secondWeapon) {
        WeaponType firstWeaponType = firstWeapon.getType();
        WeaponType secondWeaponType = secondWeapon.getType();

        // If types are equal, pick front facing first
        if (firstWeaponType == secondWeaponType) {
            if (firstWeapon.isRearMounted()) {
                return -1 * ascending;
            } else if (secondWeapon.isRearMounted()) {
                return ascending;
            } else {
                return 0;
            }
        }
        int[] firstWeaponRanges = firstWeaponType.getRanges(firstWeapon);
        int[] secondWeaponRanges = secondWeaponType.getRanges(secondWeapon);

        // Test the long range values
        if (firstWeaponRanges[RangeType.RANGE_LONG] < secondWeaponRanges[RangeType.RANGE_LONG]) {
            return -1 * ascending;
        } else if (firstWeaponRanges[RangeType.RANGE_LONG] > secondWeaponRanges[RangeType.RANGE_LONG]) {
            return ascending;
        }

        // Fall down to medium range test
        if (firstWeaponRanges[RangeType.RANGE_MEDIUM] < secondWeaponRanges[RangeType.RANGE_MEDIUM]) {
            return -1 * ascending;
        } else if (firstWeaponRanges[RangeType.RANGE_MEDIUM] > secondWeaponRanges[RangeType.RANGE_MEDIUM]) {
            return ascending;
        }

        // Now we compare short
        if (firstWeaponRanges[RangeType.RANGE_SHORT] < secondWeaponRanges[RangeType.RANGE_SHORT]) {
            return -1 * ascending;
        } else if (firstWeaponRanges[RangeType.RANGE_SHORT] > secondWeaponRanges[RangeType.RANGE_SHORT]) {
            return ascending;
        }

        // If we get here, all ranges are equals, arbitrate with heat
        if (firstWeaponType.getHeat() > secondWeaponType.getHeat()) {
            return ascending;
        } else if (firstWeaponType.getHeat() < secondWeaponType.getHeat()) {
            return -1 * ascending;
        } else {
            return 0;
        }
    }
}
