/**
 * MegaMek - Copyright (C) 2004,2005 Ben Mazur (bmazur@sev.org)
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
/*
 * Created on Sep 7, 2005
 *
 */
package megamek.common.weapons.infantry;

import megamek.common.AmmoType;
import megamek.common.BattleArmor;
import megamek.common.Compute;
import megamek.common.Infantry;
import megamek.common.TechConstants;

/**
 * @author Ben Grills
 */
public class InfantrySupportPortablePPCWeapon extends InfantryWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -3164871600230559641L;

    public InfantrySupportPortablePPCWeapon() {
        super();
        techLevel = TechConstants.T_TW_ALL;
        name = "Infantry Semi-Portable PPC";
        setInternalName(name);
        addLookupName("InfantrySemiPortablePPC");
        addLookupName("InfantryManPackPPC");
        ammoType = AmmoType.T_NA;
        cost = 7000;
        bv = 3.01;
        flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_ENERGY).or(F_PPC).or(F_INF_SUPPORT);
        infantryDamage = 0.72;
        infantryRange = 2;
        crew = 2;
    }
}