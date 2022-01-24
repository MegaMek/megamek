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
public class ISLaserPrimitiveMedium extends LaserWeapon {
    private static final long serialVersionUID = 1522567438781244152L;

    public ISLaserPrimitiveMedium() {
        super();

        name = "Primitive Prototype Medium Laser";
        setInternalName(this.name);
        addLookupName("IS Medium Laser Prototype");
        addLookupName("ISMediumLaserPrototype");
        shortName = "Medium Laser p";
        sortingName = "Laser Proto C";
        heat = 5;
        damage = 5;
        shortRange = 3;
        mediumRange = 6;
        longRange = 9;
        extremeRange = 12;
        waterShortRange = 2;
        waterMediumRange = 4;
        waterLongRange = 6;
        waterExtremeRange = 8;
        tonnage = 1.0;
        criticals = 1;
        bv = 46;
        cost = 40000;
        shortAV = 5;
        maxRange = RANGE_SHORT;
        // IO Doesn't strictly define when these weapons stop production. Checked with Herb, and
        // they would always be around. This is to cover some of the back worlds in the Periphery.
        flags = flags.or(F_PROTOTYPE);
        rulesRefs = "118, IO";
        techAdvancement.setTechBase(TECH_BASE_IS)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_C)
                .setAvailability(RATING_F, RATING_X, RATING_X, RATING_X)
                .setISAdvancement(2290, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setPrototypeFactions(F_TA)
                .setProductionFactions(F_TA)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    }
}
