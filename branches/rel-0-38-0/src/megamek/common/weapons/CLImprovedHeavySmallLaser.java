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
public class CLImprovedHeavySmallLaser extends LaserWeapon {
    /**
     *
     */
    private static final long serialVersionUID = 4467522144065588079L;

    /**
     *
     */
    public CLImprovedHeavySmallLaser() {
        super();
        techLevel.put(3071, TechConstants.T_CLAN_EXPERIMENTAL);
        name = "Improved Heavy Small Laser";
        shortName = "Imp. Heavy Medium Laser";
        setInternalName("CLImprovedSmallHeavyLaser");
        addLookupName("CLImprovedHeavySmallLaser");
        addLookupName("Clan Improved Small Heavy Laser");
        heat = 3;
        damage = 6;
        shortRange = 1;
        mediumRange = 2;
        longRange = 3;
        extremeRange = 4;
        waterShortRange = 1;
        waterMediumRange = 2;
        waterLongRange = 2;
        waterExtremeRange = 4;
        tonnage = 0.5f;
        criticals = 1;
        bv = 19;
        cost = 30000;
        shortAV = 6;
        maxRange = RANGE_SHORT;
        explosionDamage = 3;
        explosive = true;
        techRating = RATING_F;
        availRating = new int[] { RATING_X, RATING_X, RATING_F };
        introDate = 3069;
        techLevel.put(3069, techLevel.get(3071));
        techLevel.put(3085, TechConstants.T_CLAN_TW);
    }
}
