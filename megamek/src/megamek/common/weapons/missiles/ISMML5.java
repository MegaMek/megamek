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

/**
 * @author Sebastian Brocks
 */
public class ISMML5 extends MMLWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -546200914895806968L;

    /**
     *
     */
    public ISMML5() {
        super();
        name = "MML 5";
        setInternalName("ISMML5");
        addLookupName("IS MML-5");
        heat = 3;
        rackSize = 5;
        tonnage = 3;
        criticals = 3;
        bv = 45;
        cost = 75000;
        shortAV = 3;
        medAV = 3;
        longAV = 3;
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
