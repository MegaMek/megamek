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
public class ISLightRifle extends RifleWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = -8493561115043967893L;

    /**
     * 
     */
    public ISLightRifle() {
        super();
        techLevel.put(3071,TechConstants.T_IS_EXPERIMENTAL);
        name = "Light Rifle";
        setInternalName(name);
        addLookupName("IS Light Rifle");
        addLookupName("ISLightRifle");
        heat = 1;
        damage = 3;
        rackSize = 3;
        minimumRange = 0;
        shortRange = 4;
        mediumRange = 8;
        longRange = 12;
        extremeRange = 18;
        tonnage = 3.0f;
        criticals = 1;
        bv = 21;
        cost = 37750;
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
