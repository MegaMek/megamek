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
 * @author Andrew Hunter
 */
public class CLSmallLaser extends LaserWeapon {
    /**
     *
     */
    private static final long serialVersionUID = -6475366872597851742L;

    public CLSmallLaser() {
        super();
        techLevel.put(3071, TechConstants.T_CLAN_TW);
        name = "Small Laser";
        setInternalName("ClSmall Laser");
        addLookupName("CL Small Laser");
        addLookupName("CLSmallLaser");
        heat = 1;
        damage = 3;
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
        flags = flags.or(F_NO_FIRES);
        bv = 9;
        cost = 11250;
        atClass = CLASS_POINT_DEFENSE;
        introDate = 2400;
        techLevel.put(2400, techLevel.get(3071));
        availRating = new int[] { RATING_B, RATING_B, RATING_B };
        techRating = RATING_E;
    }
}
