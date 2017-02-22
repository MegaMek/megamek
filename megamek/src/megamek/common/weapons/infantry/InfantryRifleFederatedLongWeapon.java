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
public class InfantryRifleFederatedLongWeapon extends InfantryWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -3164871600230559641L;

    public InfantryRifleFederatedLongWeapon() {
        super();

        name = "Rifle (Federated Long)";
        setInternalName(name);
        addLookupName("InfantryFederatedLongRifle");
        addLookupName("Federated Long Rifle");
        ammoType = AmmoType.T_NA;
        cost = 120;
        bv = 1.07;
        flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_BALLISTIC);
        infantryDamage = 0.35;
        infantryRange = 1;
        introDate = 2855;
        techLevel.put(2855, TechConstants.T_IS_EXPERIMENTAL);
        techLevel.put(2870, TechConstants.T_IS_ADVANCED);
        techLevel.put(3000, TechConstants.T_IS_TW_NON_BOX);
        availRating = new int[] { RATING_X,RATING_C ,RATING_A ,RATING_B};
        techRating = RATING_C;
        rulesRefs = "273, TM";
        techProgression.setTechBase(TechProgression.TECH_BASE_IS);
        techProgression.setISProgression(2855, 2870, 3000);
        techProgression.setTechRating(RATING_C);
        techProgression.setAvailability( new int[] { RATING_X, RATING_C, RATING_A, RATING_B });
    }
}
