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
public class InfantryRifleISHeavyGyrojetGunWeapon extends InfantryWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -3164871600230559641L;

    public InfantryRifleISHeavyGyrojetGunWeapon() {
        super();

        name = "Gyrojet Gun (Heavy)";
        setInternalName(name);
        addLookupName("InfantryHeavyGyrojetGun");
        addLookupName("Heavy Gyrojet Gun");
        ammoType = AmmoType.T_NA;
        cost = 2500;
        bv = 1.07;
        flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_BALLISTIC);
        infantryDamage = 0.21;
        infantryRange = 2;
        introDate = 2580;
        techLevel.put(2580, TechConstants.T_IS_EXPERIMENTAL);
        techLevel.put(2590, TechConstants.T_IS_ADVANCED);
        techLevel.put(2600, TechConstants.T_IS_TW_NON_BOX);
        availRating = new int[] { RATING_C,RATING_D ,RATING_C ,RATING_C};
        techRating = RATING_D;
        rulesRefs = "273, TM";
        techAdvancement.setTechBase(TechAdvancement.TECH_BASE_IS);
        techAdvancement.setISAdvancement(2580, 2590, 2600);
        techAdvancement.setTechRating(RATING_D);
        techAdvancement.setAvailability( new int[] { RATING_C, RATING_D, RATING_C, RATING_C });
    }
}
