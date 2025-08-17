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

package megamek.common.comparators;

import java.util.Comparator;

import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponType;

/**
 * Comparator for sorting Weapons (Mounteds that have WeaponTypes) by BV, needed for BV calculation Only pass Mounteds
 * into this that are weapons
 *
 * @author beerockxs
 */
public class WeaponComparatorBV implements Comparator<Mounted<?>> {
    public WeaponComparatorBV() {

    }

    @Override
    public int compare(Mounted<?> obj1, Mounted<?> obj2) {
        if (obj1.getType() instanceof WeaponType
              && obj2.getType() instanceof WeaponType) {
            WeaponType weap1 = (WeaponType) obj1.getType();
            WeaponType weap2 = (WeaponType) obj2.getType();
            if (weap1 == weap2) {
                if (obj1.isRearMounted()) {
                    return -1;
                } else if (obj2.isRearMounted()) {
                    return 1;
                } else {
                    return 0;
                }
            }
            // sort heat 0 weapons at the very end
            if (weap1.heat == weap2.heat && weap1.heat == 0) {
                return 0;
            } else if (weap2.heat == 0) {
                return -1;
            } else if (weap1.heat == 0) {
                return 1;
            }
            // if same BV, lower heat first
            if (weap1.bv == weap2.bv) {
                return weap1.heat - weap2.heat;
            }
            // otherwise, higher BV first
            return Double.valueOf(weap2.bv - weap1.bv).intValue();
        }
        throw new ClassCastException("Passed Mounteds are not Weapons");
    }
}
