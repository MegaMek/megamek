/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.test;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.WeaponType;
import megamek.common.weapons.bayWeapons.BayWeapon;

public class ASWeaponDamageList {

    /** Utility to output all weapon damage values for AlphaStrike conversion into a list for crosschecking with ASC. */
    public static void main(String[] args) {
        List<String> wpLine;
        for (Enumeration<EquipmentType> e = EquipmentType.getAllTypes(); e.hasMoreElements(); ) {
            EquipmentType etype = e.nextElement();
            if (etype instanceof WeaponType && !etype.getRulesRefs().equals("Unofficial")
                  && !(etype instanceof BayWeapon)) {
                wpLine = new ArrayList<>();
                wpLine.add(etype.getName());
                wpLine.add(etype.getInternalName());
                wpLine.add(etype.isClan() ? "-Clan-" : "-IS-");
                double multiplier = etype.hasFlag(WeaponType.F_ONE_SHOT) ? 0.1 : 1;
                double shortRange = multiplier
                      * ((WeaponType) etype).getBattleForceDamage(AlphaStrikeElement.SHORT_RANGE, null);
                String sT = shortRange == 0 ? "--" : "" + shortRange;
                double mediumRange = multiplier
                      * ((WeaponType) etype).getBattleForceDamage(AlphaStrikeElement.MEDIUM_RANGE,
                      null);
                String mT = mediumRange == 0 ? "--" : "" + mediumRange;
                double longRange = multiplier * ((WeaponType) etype).getBattleForceDamage(AlphaStrikeElement.LONG_RANGE,
                      null);
                String lT = longRange == 0 ? "--" : "" + longRange;
                double extremeRange = multiplier
                      * ((WeaponType) etype).getBattleForceDamage(AlphaStrikeElement.EXTREME_RANGE,
                      null);
                String exT = extremeRange == 0 ? "--" : "" + extremeRange;
                wpLine.add(sT);
                wpLine.add(mT);
                wpLine.add(lT);
                wpLine.add(exT);

                System.out.println(String.join("\t", wpLine));
            }
        }
    }

    private ASWeaponDamageList() {}
}
