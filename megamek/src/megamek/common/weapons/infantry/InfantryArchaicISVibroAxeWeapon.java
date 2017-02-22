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
public class InfantryArchaicISVibroAxeWeapon extends InfantryWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -3164871600230559641L;

    public InfantryArchaicISVibroAxeWeapon() {
        super();

        name = "Blade (Vibro-axe)";
        setInternalName(name);
        addLookupName("InfantryVibroAxe");
        addLookupName("Vibro Axe");
        ammoType = AmmoType.T_NA;
        cost = 150;
        bv = 0.39;
        flags = flags.or(F_NO_FIRES).or(F_INF_POINT_BLANK).or(F_INF_ARCHAIC);
        infantryDamage = 0.42;
        infantryRange = 0;
        introDate = 2430;
        techLevel.put(2430, TechConstants.T_IS_EXPERIMENTAL);
        techLevel.put(2445, TechConstants.T_IS_ADVANCED);
        techLevel.put(2600, TechConstants.T_IS_TW_NON_BOX);
        availRating = new int[] { RATING_C,RATING_D ,RATING_D ,RATING_C};
        techRating = RATING_E;
        rulesRefs = "272, TM";
        techProgression.setTechBase(TechProgression.TECH_BASE_IS);
        techProgression.setISProgression(2430, 2445, 2600);
        techProgression.setTechRating(RATING_E);
        techProgression.setAvailability( new int[] { RATING_C, RATING_D, RATING_D, RATING_C });
    }
}
