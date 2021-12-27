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
package megamek.common.weapons.primitive;

import megamek.common.SimpleTechLevel;
import megamek.common.weapons.lasers.LaserWeapon;

/**
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 */
public class ISLaserPrimitiveLarge extends LaserWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 6640106383069896198L;

    public ISLaserPrimitiveLarge() {
        super();

        this.name = "Primitive Prototype Large Laser";
        this.setInternalName(this.name);
        this.addLookupName("IS Large Laser Prototype");
        this.addLookupName("ISLargeLaserPrototype");
        this.shortName = "Large Laser p";
        this.heat = 12;
        this.damage = 8;
        this.shortRange = 5;
        this.mediumRange = 10;
        this.longRange = 15;
        this.extremeRange = 20;
        this.waterShortRange = 3;
        this.waterMediumRange = 6;
        this.waterLongRange = 9;
        this.waterExtremeRange = 12;
        this.tonnage = 5.0;
        this.criticals = 2;
        this.bv = 123;
        this.cost = 100000;
        this.shortAV = 8;
        this.medAV = 8;
        this.maxRange = RANGE_MED;
        flags = flags.or(F_PROTOTYPE);
        //IO Doesn't strictly define when these weapons stop production. Checked with Herb and they would always be around
        //This to cover some of the back worlds in the Periphery.
        rulesRefs = "118, IO";
        techAdvancement.setTechBase(TECH_BASE_IS)
            .setIntroLevel(false)
            .setUnofficial(false)
            .setTechRating(RATING_C)
            .setAvailability(RATING_F, RATING_X, RATING_X, RATING_X)
            .setISAdvancement(2306, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
            .setISApproximate(false, false, false,false, false)
            .setPrototypeFactions(F_TA)
            .setProductionFactions(F_TA)
            .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    }
}
