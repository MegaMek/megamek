/*
 * MegaMek - Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
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
package megamek.common.weapons.prototypes;

import megamek.common.SimpleTechLevel;
import megamek.common.weapons.lasers.LaserWeapon;

/**
 * @author Andrew Hunter
 * @since Sep 12, 2004
 */
public class CLERLaserMediumPrototype extends LaserWeapon {
    private static final long serialVersionUID = -6500204992334761841L;

    public CLERLaserMediumPrototype() {
        super();
        name = "Prototype ER Medium Laser";
        setInternalName("CLERMediumLaserPrototype");
        shortName = "ER Medium Laser (P)";
        heat = 5;
        damage = 5;
        shortRange = 4;
        mediumRange = 8;
        longRange = 12;
        extremeRange = 16;
        waterShortRange = 3;
        waterMediumRange = 5;
        waterLongRange = 8;
        waterExtremeRange = 10;
        tonnage = 1.5;
        criticals = 1;
        bv = 62;
        cost = 80000;
        shortAV = 5;
        medAV = 5;
        maxRange = RANGE_MED;
        flags = flags.or(F_PROTOTYPE).andNot(F_PROTO_WEAPON);
        rulesRefs = "97, IO";
        techAdvancement.setTechBase(TECH_BASE_CLAN)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_D, RATING_X, RATING_X)
                .setClanAdvancement(2819, DATE_NONE, DATE_NONE, 2824, DATE_NONE)
                .setClanApproximate(true, false, false, true, false)
                .setPrototypeFactions(F_CSJ)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    }
}
