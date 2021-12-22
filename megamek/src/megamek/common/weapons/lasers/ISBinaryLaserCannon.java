/*
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
package megamek.common.weapons.lasers;

import megamek.common.SimpleTechLevel;

/*
 * Created on Sep 12, 2004
 *
 */

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
        name = "Binary Laser (Blazer) Cannon";
        setInternalName(name);
        addLookupName("IS Binary Laser Cannon");
        addLookupName("ISBlazer");
        addLookupName("ISBinaryLaserCannon");
        addLookupName("ISBinaryLaser");
        addLookupName("Blazer Cannon");
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
        tonnage = 9.0;
        criticals = 4;
        bv = 222;
        cost = 200000;
        shortAV = 12;
        medAV = 12;
        maxRange = RANGE_MED;
        rulesRefs = "319,TO";        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        techAdvancement.setTechBase(TECH_BASE_IS)
            .setTechRating(RATING_D)
            .setAvailability(RATING_X, RATING_E, RATING_E, RATING_D)
            .setISAdvancement(2812, DATE_NONE, 3077)
            .setPrototypeFactions(F_FW)
            .setProductionFactions(F_WB).setStaticTechLevel(SimpleTechLevel.STANDARD);
    }
}
