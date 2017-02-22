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
import megamek.common.TechProgression;

/**
 * @author Ben Grills
 */
public class InfantryPistolISMauserAutoPistolWeapon extends InfantryWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -3164871600230559641L;

    public InfantryPistolISMauserAutoPistolWeapon() {
        super();

        name = "Auto-Pistol (M&G)";
        setInternalName(name);
        addLookupName("InfantryMauserAutopistol");
        addLookupName("M&G Auto Pistol");
        ammoType = AmmoType.T_NA;
        cost = 60;
        bv = 0.16;
        flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_BALLISTIC);
        infantryDamage = 0.17;
        infantryRange = 0;
        introDate = 2485;
        techLevel.put(2485, TechConstants.T_IS_EXPERIMENTAL);
        techLevel.put(2495, TechConstants.T_IS_ADVANCED);
        techLevel.put(2550, TechConstants.T_IS_TW_NON_BOX);
        availRating = new int[] { RATING_C,RATING_B ,RATING_C ,RATING_B};
        techRating = RATING_C;
        rulesRefs = "273, TM";
        techProgression.setTechBase(TechProgression.TECH_BASE_IS);
        techProgression.setISProgression(2485, 2495, 2550);
        techProgression.setTechRating(RATING_C);
        techProgression.setAvailability( new int[] { RATING_C, RATING_B, RATING_C, RATING_B });
    }
}
