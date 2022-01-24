/*
 * MegaMek -
 * Copyright (C) 2000-2007 Ben Mazur (bmazur@sev.org)
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
package megamek.common.weapons.primitive;

import megamek.common.SimpleTechLevel;
import megamek.common.weapons.ppc.PPCWeapon;

/**
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 */
public class ISPPCPrimitive extends PPCWeapon {
    private static final long serialVersionUID = 1767670595802648539L;

    public ISPPCPrimitive() {
        super();

        name = "Primitive Prototype PPC";
        setInternalName(name);
        addLookupName("Particle Cannon Primitive");
        addLookupName("IS PPCp");
        addLookupName("ISPPCp");
        shortName = "PPCp";
        sortingName = "PPC Proto C";
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
        tonnage = 7.0;
        criticals = 3;
        bv = 176;
        cost = 200000;
        shortAV = 10;
        medAV = 10;
        maxRange = RANGE_MED;
        flags = flags.or(F_PROTOTYPE);
        // with a capacitor
        explosive = true;
        // IO Doesn't strictly define when these weapons stop production. Checked with Herb, and
        // they would always be around. This to cover some of the back worlds in the Periphery.
        rulesRefs = "118, IO";
        techAdvancement.setTechBase(TECH_BASE_IS)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_C)
                .setAvailability(RATING_F, RATING_X, RATING_X, RATING_X)
                .setISAdvancement(2439, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setPrototypeFactions(F_TA)
                .setProductionFactions(F_TA)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    }
}
