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
public class InfantrySupportISHeavyPPCWeapon extends InfantryWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -3164871600230559641L;

    public InfantrySupportISHeavyPPCWeapon() {
        super();

        name = "Particle Cannon (Support)";
        setInternalName(name);
        addLookupName("InfantrySupportPPC");
        addLookupName("InfantryHeavyPPC");
        addLookupName("Infantry Support PPC");
        ammoType = AmmoType.T_NA;
        cost = 45000;
        bv = 11.32;
        flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_ENERGY).or(F_PPC).or(F_INF_SUPPORT);
        infantryDamage = 1.58;
        infantryRange = 3;
        crew = 5;
        introDate = 2460;
        techLevel.put(2460, TechConstants.T_IS_EXPERIMENTAL);
        techLevel.put(2470, TechConstants.T_IS_ADVANCED);
        techLevel.put(2500, TechConstants.T_IS_TW_NON_BOX);
        availRating = new int[] { RATING_C,RATING_D ,RATING_C ,RATING_D};
        techRating = RATING_E;
        rulesRefs = "273, TM";
        techAdvancement.setTechBase(TechAdvancement.TECH_BASE_IS);
        techAdvancement.setISAdvancement(2460, 2470, 2500);
        techAdvancement.setTechRating(RATING_E);
        techAdvancement.setAvailability( new int[] { RATING_C, RATING_D, RATING_C, RATING_D });
    }
}
