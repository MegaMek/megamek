/**
 * MegaMek -
 * Copyright (C) 2000,2001,2002,2003,2004,2005,2006,2007 Ben Mazur (bmazur@sev.org)
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
package megamek.common.weapons;

import megamek.common.TechConstants;

/**
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 */
public class ISPPCPrimitive extends PPCWeapon {
    /**
     *
     */
    private static final long serialVersionUID = 1767670595802648539L;

    public ISPPCPrimitive() {
        super();
        techLevel.put(3071, TechConstants.T_IS_EXPERIMENTAL);
        name = "PPCp";
        setInternalName(name);
        addLookupName("Particle Cannon Primitive");
        addLookupName("IS PPCp");
        addLookupName("ISPPCp");
        heat = 15;
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
        setModes(new String[]{"Field Inhibitor ON", "Field Inhibitor OFF"});
        tonnage = 7.0f;
        criticals = 3;
        bv = 176;
        cost = 200000;
        shortAV = 10;
        medAV = 10;
        maxRange = RANGE_MED;
        // with a capacitor
        explosive = true;
        //Per Blake Documents Intro Date is 10 years early, with same tech levels
        introDate = 2450;
        techLevel.put(2450, techLevel.get(3071));
        availRating = new int[] { RATING_C, RATING_C, RATING_C };
        techRating = RATING_D;
    }
}
