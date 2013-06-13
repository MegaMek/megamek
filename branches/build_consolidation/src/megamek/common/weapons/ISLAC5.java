/**
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */
/*
 * Created on Sep 25, 2004
 */
package megamek.common.weapons;

import megamek.common.TechConstants;

public class ISLAC5 extends LACWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 6131945194809316957L;

    public ISLAC5() {
        super();
        techLevel.put(3071, TechConstants.T_IS_TW_NON_BOX);
        name = "LAC/5";
        setInternalName("Light Auto Cannon/5");
        addLookupName("IS Light Auto Cannon/5");
        addLookupName("ISLAC5");
        addLookupName("IS Light Autocannon/5");
        heat = 1;
        damage = 5;
        rackSize = 5;
        shortRange = 5;
        mediumRange = 10;
        longRange = 15;
        extremeRange = 20;
        tonnage = 5.0f;
        criticals = 2;
        bv = 62;
        cost = 150000;
        explosionDamage = damage;
        maxRange = RANGE_MED;
        shortAV = 5;
        medAV = 5;
        introDate = 3068;
        techLevel.put(3068, techLevel.get(3071));
        availRating = new int[] { RATING_X, RATING_X, RATING_F };
        techRating = RATING_D;
    }
}
