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
public class InfantrySupportISMk1LightAAWeapon extends InfantryWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -3164871600230559641L;

    public InfantrySupportISMk1LightAAWeapon() {
        super();

        name = "AA Weapon (Mk. 1, Light)";
        setInternalName(name);
        addLookupName("InfantryMk1LightAA");
        addLookupName("Infantry Mk 1 Light AA Weapon");
        ammoType = AmmoType.T_NA;
        cost = 1000;
        bv = 0.70;
        flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_MISSILE).or(F_INF_AA).or(F_INF_SUPPORT);
        infantryDamage = 0.23;
        infantryRange = 1;
        crew = 1;
        introDate = 2485;
        extinctDate = 2790;
        reintroDate = 3056;
        techLevel.put(2485, TechConstants.T_IS_EXPERIMENTAL);
        techLevel.put(2500, TechConstants.T_IS_ADVANCED);
        techLevel.put(2590, TechConstants.T_IS_TW_NON_BOX);
        techLevel.put(3056, TechConstants.T_IS_EXPERIMENTAL);
        techLevel.put(3066, TechConstants.T_IS_TW_NON_BOX);
        availRating = new int[] { RATING_X,RATING_F ,RATING_D ,RATING_D};
        techRating = RATING_F;
        rulesRefs = "273, TM";
        techProgression.setTechBase(TechProgression.TECH_BASE_IS);
        techProgression.setISProgression(2485, 2500, 2590, 2790, 3056);
        techProgression.setTechRating(RATING_F);
        techProgression.setAvailability( new int[] { RATING_X, RATING_F, RATING_D, RATING_D });
    }
}
