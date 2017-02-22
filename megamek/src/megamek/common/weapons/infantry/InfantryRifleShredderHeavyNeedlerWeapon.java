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
public class InfantryRifleShredderHeavyNeedlerWeapon extends InfantryWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -3164871600230559641L;

    public InfantryRifleShredderHeavyNeedlerWeapon() {
        super();

        name = "Needler Rifle (Shredder Heavy)";
        setInternalName(name);
        addLookupName("InfantryShredder");
        addLookupName("Shredder Heavy Needler");
        ammoType = AmmoType.T_NA;
        cost = 150;
        bv = 0.31;
        flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_INF_NONPENETRATING).or(F_BALLISTIC);
        infantryDamage = 0.34;
        infantryRange = 0;
        introDate = 3046;
        techLevel.put(3046, TechConstants.T_IS_EXPERIMENTAL);
        techLevel.put(3054, TechConstants.T_IS_ADVANCED);
        techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        availRating = new int[] { RATING_X,RATING_X ,RATING_C ,RATING_C};
        techRating = RATING_D;
        rulesRefs = "273, TM";
        techProgression.setTechBase(TechProgression.TECH_BASE_IS);
        techProgression.setISProgression(3046, 3054, 3067);
        techProgression.setTechRating(RATING_D);
        techProgression.setAvailability( new int[] { RATING_X, RATING_X, RATING_C, RATING_C });
    }
}
