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
public class InfantrySMGISRuganWeapon extends InfantryWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -3164871600230559641L;

    public InfantrySMGISRuganWeapon() {
        super();

        name = "SMG (Rugan)";
        setInternalName(name);
        addLookupName("InfantryRuganSMG");
        addLookupName("Rugan SMG");
        ammoType = AmmoType.T_NA;
        cost = 100;
        bv = 0.18;
        flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_BALLISTIC);
        infantryDamage = 0.20;
        infantryRange = 0;
        introDate = 2713;
        techLevel.put(2713, TechConstants.T_IS_EXPERIMENTAL);
        techLevel.put(2720, TechConstants.T_IS_ADVANCED);
        techLevel.put(2750, TechConstants.T_IS_TW_NON_BOX);
        availRating = new int[] { RATING_D,RATING_B ,RATING_C ,RATING_D};
        techRating = RATING_C;
        rulesRefs = "273, TM";
    }
}
