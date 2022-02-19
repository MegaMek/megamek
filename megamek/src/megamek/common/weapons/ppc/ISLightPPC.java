/*
 * MegaMek - Copyright (c) 2004-2005 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.common.weapons.ppc;

/**
 * @author Sebastian Brocks
 * @since Sep 13, 2004
 */
public class ISLightPPC extends PPCWeapon {
    private static final long serialVersionUID = 1376257996653555538L;

    public ISLightPPC() {
        super();
        name = "Light PPC";
        setInternalName(name);
        addLookupName("ISLightPPC");
        addLookupName("ISLPPC");
        sortingName = "PPC B";
        heat = 5;
        damage = 5;
        minimumRange = 3;
        shortRange = 6;
        mediumRange = 12;
        longRange = 18;
        extremeRange = 24;
        waterShortRange = 4;
        waterMediumRange = 7;
        waterLongRange = 10;
        waterExtremeRange = 14;
        tonnage = 3.0;
        criticals = 2;
        bv = 88;
        shortAV = 5;
        medAV = 5;
        maxRange = RANGE_MED;
        cost = 150000;
        // with a capacitor
        explosive = true;
        rulesRefs = "234, TM";
        techAdvancement.setTechBase(TECH_BASE_IS)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3064, 3067, 3068, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setPrototypeFactions(F_DC)
                .setProductionFactions(F_DC, F_FW);
    }
}
