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
public class ISLaserPrimitiveMedium extends LaserWeapon {
    /**
     *
     */
    private static final long serialVersionUID = 1522567438781244152L;

    public ISLaserPrimitiveMedium() {
        super();

        this.name = "Primitive Prototype Medium Laser";
        this.setInternalName(this.name);
        this.addLookupName("IS Medium Laser Prototype");
        this.addLookupName("ISMediumLaserPrototype");
        this.shortName = "Medium Laser p";
        this.heat = 5;
        this.damage = 5;
        this.shortRange = 3;
        this.mediumRange = 6;
        this.longRange = 9;
        this.extremeRange = 12;
        this.waterShortRange = 2;
        this.waterMediumRange = 4;
        this.waterLongRange = 6;
        this.waterExtremeRange = 8;
        this.tonnage = 1.0;
        this.criticals = 1;
        this.bv = 46;
        this.cost = 40000;
        this.shortAV = 5;
        this.maxRange = RANGE_SHORT;
        flags = flags.or(F_PROTOTYPE);
        //IO Doesn't strictly define when these weapons stop production. Checked with Herb and they would always be around
        //This to cover some of the back worlds in the Periphery.
        rulesRefs = "118, IO";
        techAdvancement.setTechBase(TECH_BASE_IS)
            .setIntroLevel(false)
            .setUnofficial(false)
            .setTechRating(RATING_C)
            .setAvailability(RATING_F, RATING_X, RATING_X, RATING_X)
            .setISAdvancement(2290, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
            .setISApproximate(false, false, false,false, false)
            .setPrototypeFactions(F_TA)
            .setProductionFactions(F_TA)
            .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    }
}
