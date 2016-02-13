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
package megamek.common.weapons;

import megamek.common.TechConstants;

/**
 * @author Dave Nawton
 */
public class ISPrimRL15 extends RLWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -8464817815813827947L;

    /**
     *
     */
    public ISPrimRL15() {
        super ();
        techLevel.put(3071, TechConstants.T_IS_UNOFFICIAL);
        name = "Primitive Rocket Launcher 15";
        setInternalName("ISPrimRL15");
        heat = 4;
        rackSize = 15;
        shortRange = 4;
        mediumRange = 9;
        longRange = 15;
        extremeRange = 18;
        tonnage = 1.0f;
        criticals = 2;
        bv = 23;
        cost = 30000;
        shortAV = 9;
        medAV = 9;
        maxRange = RANGE_MED;
        introDate = 2297;
        extinctDate = 2315;    
        techLevel.put(2315, techLevel.get(3071));
        availRating = new int[] { RATING_B, RATING_X, RATING_X };
        techRating = RATING_B;
        toHitModifier = -1;
    }
}
