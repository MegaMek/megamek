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
import megamek.common.TechAdvancement;

/**
 * @author Ben Grills
 */
public class InfantrySupportISPortableAutocannonWeapon extends InfantryWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -3164871600230559641L;

    public InfantrySupportISPortableAutocannonWeapon() {
        super();

        name = "Autocannon (Semi-Portable)";
        setInternalName(name);
        addLookupName("InfantryPortableAutocannon");
        addLookupName("InfantrySemiPortableAutocannon");
        addLookupName("Infantry Semi Portable Autocannon");
        ammoType = AmmoType.T_NA;
        cost = 2000;
        bv = 2.35;
        flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_BALLISTIC).or(F_INF_SUPPORT);
        infantryDamage = 0.77;
        infantryRange = 1;
        crew = 2;
        introDate = 2100;
        techLevel.put(2100, TechConstants.T_IS_EXPERIMENTAL);
        techLevel.put(2255, TechConstants.T_IS_ADVANCED);
        techLevel.put(2300, TechConstants.T_IS_TW_NON_BOX);
        availRating = new int[] { RATING_C,RATING_D ,RATING_D ,RATING_C};
        techRating = RATING_C;
        rulesRefs = "273, TM";
        techAdvancement.setTechBase(TechAdvancement.TECH_BASE_IS);
        techAdvancement.setISAdvancement(2100, 2255, 2300);
        techAdvancement.setTechRating(RATING_C);
        techAdvancement.setAvailability( new int[] { RATING_C, RATING_D, RATING_D, RATING_C });
    }
}
