/**
 * MegaMek - Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
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
 * Created on Sep 2, 2004
 *
 */
package megamek.common.weapons;


/**
 * @author Sebastian Brocks
 */
public class ISMediumReengineeredLaser extends ReengineeredLaserWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 1596494785198942212L;

    public ISMediumReengineeredLaser() {
        super();
        name = "Medium Re-engineered Laser";
        setInternalName(name);
        addLookupName("ISMediumReengineeredLaser");
        addLookupName("ISMediumRELaser");
        heat = 7;
        damage = 6;
        shortRange = 3;
        mediumRange = 6;
        longRange = 9;
        extremeRange = 12;
        waterShortRange = 2;
        waterMediumRange = 4;
        waterLongRange = 6;
        waterExtremeRange = 8;
        tonnage = 2.5f;
        criticals = 2;
        bv = 56;
        cost = 10000;
        shortAV = 6;
        maxRange = RANGE_SHORT;
        introDate = 3130;
        availRating = new int[] { RATING_X, RATING_X, RATING_E };
        techRating = RATING_E;
    }
}
