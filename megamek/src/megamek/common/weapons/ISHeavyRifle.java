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
 * Created on Sep 25, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.TechConstants;

/**
 * @author Jason Tighe
 */
public class ISHeavyRifle extends RifleWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = -2670817452732971454L;

    /**
     * 
     */
    public ISHeavyRifle() {
        super();
        techLevel.put(3071,TechConstants.T_IS_EXPERIMENTAL);
        name = "Heavy Rifle";
        setInternalName(name);
        addLookupName("IS Heavy Rifle");
        addLookupName("ISHeavyRifle");
        heat = 4;
        damage = 9;
        rackSize = 9;
        minimumRange = 2;
        shortRange = 6;
        mediumRange = 12;
        longRange = 18;
        extremeRange = 27;
        tonnage = 8.0f;
        criticals = 3;
        bv = 91;
        cost = 90000;
        explosive = false; // when firing incendiary ammo
        shortAV = 4;
        medAV = 8;
        longAV = 8;
        extAV = 8;
        maxRange = RANGE_MED;
        explosionDamage = 0;
        techRating = RATING_B;
        availRating = new int[]{RATING_C, RATING_F, RATING_X};
    }
}
