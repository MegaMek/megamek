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
 * @author Sebastian Brocks
 */
public class ISLRM10 extends LRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 4015441487276641235L;

    /**
     *
     */
    public ISLRM10() {
        super();
        techLevel = TechConstants.T_INTRO_BOXSET;
        name = "LRM 10";
        setInternalName(name);
        addLookupName("IS LRM-10");
        addLookupName("ISLRM10");
        addLookupName("IS LRM 10");
        heat = 4;
        rackSize = 10;
        minimumRange = 6;
        tonnage = 5.0f;
        criticals = 2;
        bv = 90;
        cost = 100000;
        shortAV = 6;
        medAV = 6;
        longAV = 6;
        maxRange = RANGE_LONG;
        introDate = 2300;
        availRating = new int[]{RATING_C,RATING_C,RATING_C};
        techRating = RATING_C;
    }
}
