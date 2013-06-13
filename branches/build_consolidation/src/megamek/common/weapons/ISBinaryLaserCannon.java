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
package megamek.common.weapons;

import megamek.common.TechConstants;

/**
 * @author Jason Tighe
 */
public class ISBinaryLaserCannon extends LaserWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -6849916948609019186L;

    public ISBinaryLaserCannon() {
        super();
        techLevel.put(3071, TechConstants.T_IS_EXPERIMENTAL);
        name = "Blazer Cannon";
        setInternalName(name);
        addLookupName("IS Binary Laser Cannon");
        addLookupName("ISBlazer");
        addLookupName("ISBinaryLaserCannon");
        addLookupName("ISBinaryLaser");
        heat = 16;
        damage = 12;
        shortRange = 5;
        mediumRange = 10;
        longRange = 15;
        extremeRange = 20;
        waterShortRange = 3;
        waterMediumRange = 6;
        waterLongRange = 9;
        waterExtremeRange = 12;
        tonnage = 9.0f;
        criticals = 4;
        bv = 222;
        cost = 200000;
        shortAV = 16;
        medAV = 16;
        maxRange = RANGE_MED;
        techRating = RATING_D;
        availRating = new int[] { RATING_X, RATING_E, RATING_E };
        introDate = 2812;
        techLevel.put(2812, techLevel.get(3071));
        techLevel.put(3077, TechConstants.T_IS_TW_NON_BOX);

    }
}
