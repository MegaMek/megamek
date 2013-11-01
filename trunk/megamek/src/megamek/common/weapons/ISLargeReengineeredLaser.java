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


/**
 * @author Sebastian Brocks
 */
public class ISLargeReengineeredLaser extends ReengineeredLaserWeapon {


    /**
     *
     */
    private static final long serialVersionUID = -7304496499826505883L;

    public ISLargeReengineeredLaser() {
        super();
        name = "Large Re-engineered Laser";
        setInternalName(name);
        addLookupName("ISLargeReengineeredLaser");
        addLookupName("ISLargeRELaser");
        heat = 10;
        damage = 9;
        shortRange = 5;
        mediumRange = 10;
        longRange = 15;
        extremeRange = 20;
        waterShortRange = 3;
        waterMediumRange = 6;
        waterLongRange = 9;
        waterExtremeRange = 12;
        tonnage = 8.0f;
        criticals = 5;
        bv = 139;
        cost = 250000;
        shortAV = 9;
        medAV = 9;
        maxRange = RANGE_MED;
        introDate = 3130;
        availRating = new int[] { RATING_X, RATING_X, RATING_E };
        techRating = RATING_E;
    }
}
