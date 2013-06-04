/**
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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
 * Created on Sep 8, 20045
 *
 */
package megamek.common.weapons;

import megamek.common.TechConstants;

/**
 * @author Sebastian Brocks
 */
public class ISLargeXPulseLaser extends PulseLaserWeapon {
    /**
     *
     */
    private static final long serialVersionUID = -8159582350685114767L;

    /**
     *
     */
    public ISLargeXPulseLaser() {
        super();
        techLevel.put(3071, TechConstants.T_IS_EXPERIMENTAL);
        name = "Large X-Pulse Laser";
        setInternalName("ISLargeXPulseLaser");
        addLookupName("IS X-Pulse Large Laser");
        addLookupName("IS Large X-Pulse Laser");
        heat = 14;
        damage = 9;
        toHitModifier = -2;
        shortRange = 5;
        mediumRange = 10;
        longRange = 15;
        extremeRange = 20;
        waterShortRange = 2;
        waterMediumRange = 5;
        waterLongRange = 7;
        waterExtremeRange = 10;
        tonnage = 7.0f;
        criticals = 2;
        bv = 178;
        cost = 275000;
        maxRange = RANGE_MED;
        shortAV = 9;
        medAV = 9;
        techRating = RATING_E;
        availRating = new int[] { RATING_X, RATING_X, RATING_E };
        introDate = 3057;
        techLevel.put(3057, techLevel.get(3071));
        techLevel.put(3078, TechConstants.T_IS_ADVANCED);
    }
}
