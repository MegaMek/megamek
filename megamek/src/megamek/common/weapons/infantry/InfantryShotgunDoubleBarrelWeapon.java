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
public class InfantryShotgunDoubleBarrelWeapon extends InfantryWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -3164871600230559641L;

    public InfantryShotgunDoubleBarrelWeapon() {
        super();

        name = "Shotgun (Double-Barrel)";
        setInternalName(name);
        addLookupName("InfantryDoubleShotgun");
        addLookupName("Double Barrel Shotgun");
        ammoType = AmmoType.T_NA;
        cost = 30;
        bv = 0.03;
        flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_BALLISTIC);
        infantryDamage = 0.03;
        infantryRange = 0;
        introDate = 1950;
        techLevel.put(1950,TechConstants.T_ALLOWED_ALL);
        availRating = new int[]{RATING_A,RATING_A,RATING_A,RATING_A};
        techRating = RATING_B;
        rulesRefs =" 273, TM";
        techProgression.setTechBase(TechProgression.TECH_BASE_ALL);
        techProgression.setProgression(DATE_NONE, DATE_NONE, 1950);
        techProgression.setTechRating(RATING_B);
        techProgression.setAvailability( new int[] { RATING_A, RATING_A, RATING_A, RATING_A });
    }
}
