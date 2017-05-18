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

import megamek.common.TechAdvancement;
import megamek.common.weapons.LaserWeapon;

/**
 * @author Andrew Hunter
 */
public class CLERMediumLaserPrototype extends LaserWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -6500204992334761841L;

    /**
     *
     */
    public CLERMediumLaserPrototype() {
        super();
        name = "ER Medium Laser (CP)";
        setInternalName("CLERMediumLaserPrototype");
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
        tonnage = 1.5f;
        criticals = 1;
        bv = 62;
        cost = 80000;
        shortAV = 5;
        medAV = 5;
        maxRange = RANGE_MED;
        techAdvancement.setTechBase(TechAdvancement.TECH_BASE_CLAN);
        techAdvancement.setClanAdvancement(2820, DATE_NONE, DATE_NONE, 2824);
        techAdvancement.setTechRating(RATING_F);
        techAdvancement.setAvailability( new int[] { RATING_X, RATING_E, RATING_X, RATING_X });
    }
}
