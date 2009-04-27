/*
 * MegaMek -
 * Copyright (C) 2007 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.common;

import java.util.Comparator;

/**
 * Comparator for sorting Weapons (Mounteds that have WeaponTypes) by BV, needed
 * for BV calculation Only pass Mounteds into this that are weapons
 * 
 * @author beerockxs
 */
public class WeaponComparator implements Comparator<Mounted> {

    public WeaponComparator() {
    }

    public int compare(Mounted obj1, Mounted obj2) {
        if (obj1.getType() instanceof WeaponType
                && obj2.getType() instanceof WeaponType) {
            WeaponType weap1 = (WeaponType) obj1.getType();
            WeaponType weap2 = (WeaponType) obj2.getType();
            if (weap1 == weap2) {
                if (obj1.isRearMounted())
                    return -1;
                else if (obj2.isRearMounted())
                    return 1;
                return 0;
            }
            // sort heat 0 weapons at the very end
            if (weap1.heat == weap2.heat && weap1.heat == 0) {
                return 0;
            } else if (weap2.heat == 0)
                return -1;
            else if (weap1.heat == 0)
                return 1;
            // if same BV, lower heat first
            if (weap1.bv == weap2.bv)
                return weap1.heat - weap2.heat;
            // otherwise, higher BV first
            return new Double(weap2.bv - weap1.bv).intValue();
        }
        throw new ClassCastException("Passed Mounteds are not Weapons");
    }
}