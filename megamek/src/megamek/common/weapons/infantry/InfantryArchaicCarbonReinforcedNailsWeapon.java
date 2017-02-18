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
public class InfantryArchaicCarbonReinforcedNailsWeapon extends InfantryWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -3164871600230559641L;

    public InfantryArchaicCarbonReinforcedNailsWeapon() {
        super();

        name = "Blade (Carbon-Reinforced Nails)";
        setInternalName(name);
        addLookupName("InfantryCarbonFingernails");
        addLookupName("Carbon Reinforced Fingernails");
        ammoType = AmmoType.T_NA;
        cost = 1000;
        bv = 0.02;
        flags = flags.or(F_NO_FIRES).or(F_INF_POINT_BLANK).or(F_INF_ARCHAIC);
        infantryDamage = 0.02;
        infantryRange = 0;
        introDate = 2975;
        techLevel.put(2975,TechConstants.T_IS_EXPERIMENTAL);
        techLevel.put(2990,TechConstants.T_IS_ADVANCED);
        availRating = new int[]{RATING_X,RATING_D,RATING_D,RATING_E};
        techRating = RATING_D;
        rulesRefs =" 272, TM";
    }
}
