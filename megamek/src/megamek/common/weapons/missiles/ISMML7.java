/**
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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
package megamek.common.weapons.missiles;

import megamek.common.TechAdvancement;
import megamek.common.weapons.MMLWeapon;

/**
 * @author Sebastian Brocks
 */
public class ISMML7 extends MMLWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -2143795495566407588L;

    /**
     *
     */
    public ISMML7() {
        super();
        name = "MML 7";
        setInternalName("ISMML7");
        addLookupName("IS MML-7");
        heat = 4;
        rackSize = 7;
        tonnage = 4.5f;
        criticals = 4;
        bv = 67;
        cost = 105000;
        shortAV = 4;
        medAV = 4;
        longAV = 4;
        maxRange = RANGE_LONG;
        rulesRefs = "229,TM";
        techAdvancement.setTechBase(TECH_BASE_IS)
        	.setIntroLevel(false)
        	.setUnofficial(false)
            .setTechRating(RATING_D)
            .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
            .setISAdvancement(3067, 3068, 3072, DATE_NONE, DATE_NONE)
            .setISApproximate(true, false, false,false, false)
            .setPrototypeFactions(F_MERC)
            .setProductionFactions(F_WB);
    }
}
