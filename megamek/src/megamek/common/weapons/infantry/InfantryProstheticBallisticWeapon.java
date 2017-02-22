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
 * @author Dave Nawton
 */
public class InfantryProstheticBallisticWeapon extends InfantryWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -3164871600230559641L;

    public InfantryProstheticBallisticWeapon() {
        super();

        name = "Prosthetic Ballistic";
        setInternalName(name);
        addLookupName("ProstheticBallastic");
        ammoType = AmmoType.T_NA;
        cost = 500;
        bv = 0.0;
        flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_BALLISTIC);
        infantryDamage = 0.01;
        infantryRange = 0;
        //Rating and Dates not available below is compiled from various books
        introDate = 2375;
        techLevel.put(2375,TechConstants.T_ALLOWED_ALL);
        availRating = new int[]{RATING_F,RATING_E,RATING_D,RATING_D};
        techRating = RATING_D;
        techProgression.setTechBase(TechProgression.TECH_BASE_ALL);
        techProgression.setProgression(DATE_NONE, DATE_NONE, 2375);
        techProgression.setTechRating(RATING_D);
        techProgression.setAvailability( new int[] { RATING_F, RATING_E, RATING_D, RATING_D });
    }
}
