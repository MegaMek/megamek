/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.test;

import megamek.common.EquipmentType;
import megamek.common.WeaponType;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.weapons.bayweapons.BayWeapon;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class ASWeaponDamageList {

    /** Utility to output all weapon damage values for AlphaStrike conversion into a list for crosschecking with ASC. */
    public static void main(String[] args) {
        List<String> wpLine;
        for (Enumeration<EquipmentType> e = EquipmentType.getAllTypes(); e.hasMoreElements(); ) {
            EquipmentType etype = e.nextElement();
            if (etype instanceof WeaponType && !etype.rulesRefs.equals("Unofficial")
                    &&!(etype instanceof BayWeapon)) {
                wpLine = new ArrayList<>();
                wpLine.add(etype.getName());
                wpLine.add(etype.getInternalName());
                wpLine.add(etype.isClan()? "-Clan-" : "-IS-");
                double mult = etype.hasFlag(WeaponType.F_ONESHOT) ? 0.1 : 1;
                double s = mult * ((WeaponType)etype).getBattleForceDamage(AlphaStrikeElement.SHORT_RANGE, null);
                String sT = s == 0 ? "--" : "" + s;
                double m = mult * ((WeaponType)etype).getBattleForceDamage(AlphaStrikeElement.MEDIUM_RANGE, null);
                String mT = m == 0 ? "--" : "" + m;
                double l = mult * ((WeaponType)etype).getBattleForceDamage(AlphaStrikeElement.LONG_RANGE, null);
                String lT = l == 0 ? "--" : "" + l;
                double ex = mult * ((WeaponType)etype).getBattleForceDamage(AlphaStrikeElement.EXTREME_RANGE, null);
                String exT = ex == 0 ? "--" : "" + ex;
                wpLine.add(sT);
                wpLine.add(mT);
                wpLine.add(lT);
                wpLine.add(exT);
                
                System.out.println(String.join("\t", wpLine));
            }
        }
    }

    private ASWeaponDamageList() { }
}