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
import megamek.common.weapons.lasers.LaserWeapon;

/**
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 */
public class ISLaserPrimitiveLarge extends LaserWeapon {
    private static final long serialVersionUID = 6640106383069896198L;

    public ISLaserPrimitiveLarge() {
        super();

        name = "Primitive Prototype Large Laser";
        setInternalName(this.name);
        addLookupName("IS Large Laser Prototype");
        addLookupName("ISLargeLaserPrototype");
        shortName = "Large Laser p";
        sortingName = "Laser Proto D";
        heat = 12;
        damage = 8;
        shortRange = 5;
        mediumRange = 10;
        longRange = 15;
        extremeRange = 20;
        waterShortRange = 3;
        waterMediumRange = 6;
        waterLongRange = 9;
        waterExtremeRange = 12;
        tonnage = 5.0;
        criticals = 2;
        bv = 123;
        cost = 100000;
        shortAV = 8;
        medAV = 8;
        maxRange = RANGE_MED;
        // IO Doesn't strictly define when these weapons stop production. Checked with Herb, and
        // they would always be around. This is to cover some of the back worlds in the Periphery.
        flags = flags.or(F_PROTOTYPE);
        rulesRefs = "118, IO";
        techAdvancement.setTechBase(TECH_BASE_IS)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_C)
                .setAvailability(RATING_F, RATING_X, RATING_X, RATING_X)
                .setISAdvancement(2306, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setPrototypeFactions(F_TA)
                .setProductionFactions(F_TA)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    }
}
