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
import megamek.common.TechConstants;

/**
 * @author Ben Grills
 */
public class InfantryRifleMinolta9000Weapon extends InfantryWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -3164871600230559641L;

    public InfantryRifleMinolta9000Weapon() {
        super();
        techLevel.put(3071,TechConstants.T_IS_TW_NON_BOX);
        name = "Rifle (Minolta 9000)";
        setInternalName(name);
        addLookupName("InfantryMinolta9000");
        addLookupName("Minolta 9000 Advanced Sniper System");
        ammoType = AmmoType.T_NA;
        cost = 1000;
        bv = 1.79;
        flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_BALLISTIC);
        infantryDamage = 0.35;
        infantryRange = 2;
        introDate = 3055;
        techLevel.put(3055,techLevel.get(3071));
        availRating = new int[]{RATING_X,RATING_X,RATING_E};
        techRating = RATING_D;
    }
}
