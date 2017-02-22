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
public class InfantrySupportHeavyRecoillessRifleWeapon extends InfantryWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -3164871600230559641L;

    public InfantrySupportHeavyRecoillessRifleWeapon() {
        super();

        name = "Recoilless Rifle (Heavy)";
        setInternalName(name);
        addLookupName("InfantryHRR");
        addLookupName("InfantryHeavyRecoillessRifle");
        addLookupName("Infantry Heavy Recoilless Rifle");
        ammoType = AmmoType.T_NA;
        cost = 3000;
        bv = 2.91;
        flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_BALLISTIC).or(F_INF_SUPPORT);
        infantryDamage = 0.57;
        infantryRange = 2;
        crew = 3;
        introDate = 1950;
        techLevel.put(1950, TechConstants.T_ALLOWED_ALL);
        availRating = new int[]{RATING_A,RATING_A,RATING_A};
        techRating = RATING_C;
        techProgression.setTechBase(TechProgression.TECH_BASE_ALL);
        techProgression.setProgression(DATE_NONE, DATE_NONE, 1950);
        techProgression.setTechRating(RATING_C);
        techProgression.setAvailability( new int[] { RATING_A, RATING_A, RATING_A, RATING_X });
    }
}
