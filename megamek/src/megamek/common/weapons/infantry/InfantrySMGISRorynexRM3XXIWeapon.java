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
public class InfantrySMGISRorynexRM3XXIWeapon extends InfantryWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -3164871600230559641L;

    public InfantrySMGISRorynexRM3XXIWeapon() {
        super();

        name = "SMG (Rorynex RM-3/XXI)";
        setInternalName(name);
        addLookupName("InfantryRorynexRM3XXI");
        addLookupName("Rorynex RM-3/XXI");
        ammoType = AmmoType.T_NA;
        cost = 80;
        bv = 0.18;
        flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_BALLISTIC);
        infantryDamage = 0.20;
        infantryRange = 0;
        introDate = 2817;
        techLevel.put(2817, TechConstants.T_IS_EXPERIMENTAL);
        techLevel.put(2828, TechConstants.T_IS_ADVANCED);
        techLevel.put(2830, TechConstants.T_IS_TW_NON_BOX);
        availRating = new int[] { RATING_X,RATING_F ,RATING_D ,RATING_D};
        techRating = RATING_F;
        rulesRefs = "273, TM";
        techAdvancement.setTechBase(TechAdvancement.TECH_BASE_IS);
        techAdvancement.setISAdvancement(2817, 2828, 2830);
        techAdvancement.setTechRating(RATING_F);
        techAdvancement.setAvailability( new int[] { RATING_X, RATING_F, RATING_D, RATING_D });
    }
}
