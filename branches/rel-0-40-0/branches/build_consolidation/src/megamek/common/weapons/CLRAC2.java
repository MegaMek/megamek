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
 * Created on Oct 19, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.TechConstants;

/**
 * @author Sebastian Brocks
 */
public class CLRAC2 extends RACWeapon {
    /**
     *
     */
    private static final long serialVersionUID = -2134880724662962943L;

    /**
     *
     */
    public CLRAC2() {
        super();
        techLevel.put(3071, TechConstants.T_CLAN_EXPERIMENTAL);
        name = "Rotary AC/2";
        setInternalName("CLRotaryAC2");
        addLookupName("Clan Rotary AC/2");
        addLookupName("Clan Rotary Assault Cannon/2");
        heat = 1;
        damage = 2;
        rackSize = 2;
        minimumRange = 0;
        shortRange = 8;
        mediumRange = 17;
        longRange = 25;
        extremeRange = 34;
        tonnage = 8.0f;
        criticals = 4;
        bv = 161;
        cost = 175000;
        techRating = RATING_F;
        shortAV = 8;
        medAV = 8;
        longAV = 8;
        extAV = 8;
        maxRange = RANGE_EXT;
        availRating = new int[] { RATING_X, RATING_X, RATING_F };
        introDate = 3073;
        techLevel.put(3073, techLevel.get(3071));
    }
}
