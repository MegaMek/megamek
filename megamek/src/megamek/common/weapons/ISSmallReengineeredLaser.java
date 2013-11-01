/**
 * MegaMek -
 * Copyright (C) 2013 Ben Mazur (bmazur@sev.org)
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
package megamek.common.weapons;


public class ISSmallReengineeredLaser extends ReengineeredLaserWeapon {


    /**
     *
     */
    private static final long serialVersionUID = 6231212510603930740L;

    public ISSmallReengineeredLaser() {
        super();
        name = "Small Re-engineered Laser";
        setInternalName(name);
        addLookupName("ISSmallReengineeredLaser");
        addLookupName("ISSmallRELaser");
        heat = 5;
        damage = 4;
        shortRange = 1;
        mediumRange = 2;
        longRange = 3;
        extremeRange = 4;
        waterShortRange = 1;
        waterMediumRange = 2;
        waterLongRange = 2;
        waterExtremeRange = 4;
        tonnage = 1.5f;
        criticals = 1;
        flags = flags.or(F_NO_FIRES);
        bv = 12;
        cost = 25000;
        shortAV = 4;
        maxRange = RANGE_SHORT;
        atClass = CLASS_POINT_DEFENSE;
        introDate = 3130;
        availRating = new int[] { RATING_X, RATING_X, RATING_E };
        techRating = RATING_E;
    }
}
