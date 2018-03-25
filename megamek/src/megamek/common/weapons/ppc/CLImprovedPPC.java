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
 * Created on Sep 13, 2004
 *
 */
package megamek.common.weapons.ppc;

import megamek.common.SimpleTechLevel;

/**
 * @author Andrew Hunter
 */
public class CLImprovedPPC extends PPCWeapon {
    /**
     *
     */
    private static final long serialVersionUID = 5775665622863346537L;

    /**
     *
     */
    public CLImprovedPPC() {
        super();
        name = "Improved PPC";
        setInternalName(name);
        addLookupName("Improved Particle Cannon");
        addLookupName("CLIMPPPC");
        heat = 10;
        damage = 10;
        minimumRange = 3;
        shortRange = 6;
        mediumRange = 12;
        longRange = 18;
        extremeRange = 24;
        waterShortRange = 4;
        waterMediumRange = 7;
        waterLongRange = 10;
        waterExtremeRange = 14;
        tonnage = 6.0f;
        criticals = 2;
        bv = 176;
        cost = 200000;
        shortAV = 10;
        medAV = 10;
        maxRange = RANGE_MED;
        // with a capacitor
        explosive = true;
        rulesRefs = "95,IO";
        techAdvancement.setTechBase(TECH_BASE_CLAN).setTechRating(RATING_F)
            .setAvailability(RATING_X, RATING_D, RATING_X, RATING_X)
            .setClanAdvancement(2819, 2820, DATE_NONE, 2832, 3080)
            .setClanApproximate(true, false, false,false, false)
            .setPrototypeFactions(F_CSR).setProductionFactions(F_CSR)
            .setReintroductionFactions(F_EI).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);

    }

}
