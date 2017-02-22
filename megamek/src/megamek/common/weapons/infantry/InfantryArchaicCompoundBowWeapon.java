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
public class InfantryArchaicCompoundBowWeapon extends InfantryWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -3164871600230559641L;

    public InfantryArchaicCompoundBowWeapon() {
        super();

        name = "Bow (Compound)";
        setInternalName(name);
        addLookupName("InfantryCompoundBow");
        addLookupName("Compound Bow");
        ammoType = AmmoType.T_NA;
        cost = 15;
        bv = 0.01;
        flags = flags.or(F_NO_FIRES).or(F_BALLISTIC).or(F_INF_ARCHAIC);
        infantryDamage = 0.01;
        infantryRange = 0;
        introDate = 1950;
        techLevel.put(1950,TechConstants.T_ALLOWED_ALL);
        availRating = new int[]{RATING_B,RATING_A,RATING_A,RATING_A};
        techRating = RATING_A;
        rulesRefs =" 272, TM";

        techProgression.setTechBase(TechProgression.TECH_BASE_ALL);
        techProgression.setProgression(DATE_NONE, DATE_NONE, 1950);
        techProgression.setTechRating(RATING_A);
        techProgression.setAvailability( new int[] { RATING_B, RATING_A, RATING_A, RATING_A });
    }
}
