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
 * Created on Sep 12, 2004
 *
 */
package megamek.common.weapons.prototypes;

import megamek.common.SimpleTechLevel;
import megamek.common.weapons.lasers.LaserWeapon;

/**
 * @author Andrew Hunter
 */
public class CLERLaserSmallPrototype extends LaserWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 3560080195438539835L;

    /**
     *
     */
    public CLERLaserSmallPrototype() {
        super();
        name = "Prototype ER Small Laser";
        setInternalName("CLERSmallLaserPrototype");
        shortName = "ER Small Laser (P)";
        heat = 2;
        damage = 3;
        shortRange = 2;
        mediumRange = 4;
        longRange = 5;
        extremeRange = 8;
        waterShortRange = 1;
        waterMediumRange = 2;
        waterLongRange = 3;
        waterExtremeRange = 4;
        tonnage = 0.5;
        criticals = 1;
        flags = flags.or(F_NO_FIRES);
        bv = 17;
        cost = 11250;
        shortAV = 3;
        maxRange = RANGE_SHORT;
        flags = flags.or(F_PROTOTYPE).andNot(F_PROTO_WEAPON);
        rulesRefs = "97,IO";
        techAdvancement.setTechBase(TECH_BASE_CLAN)
            .setIntroLevel(false)
            .setUnofficial(false)
            .setTechRating(RATING_E)
            .setAvailability(RATING_X, RATING_D, RATING_X, RATING_X)
            .setClanAdvancement(2819, DATE_NONE, DATE_NONE, 2825, DATE_NONE)
            .setClanApproximate(true, false, false, true, false)
            .setPrototypeFactions(F_CSJ)
            .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    }
}
