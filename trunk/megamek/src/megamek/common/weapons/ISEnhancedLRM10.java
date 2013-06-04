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
 * @author BATTLEMASTER
 */
public class ISEnhancedLRM10 extends EnhancedLRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 3287950524687857609L;

    /**
     *
     */
    public ISEnhancedLRM10() {
        super();
        techLevel.put(3071, TechConstants.T_IS_EXPERIMENTAL);
        name = "Enhanced LRM 10";
        setInternalName(name);
        addLookupName("ISEnhancedLRM10");
        heat = 4;
        rackSize = 10;
        minimumRange = 3;
        shortRange = 7;
        mediumRange = 14;
        longRange = 21;
        extremeRange = 28;
        tonnage = 6.0f;
        criticals = 4;
        bv = 104;
        cost = 125000;
        shortAV = 6;
        medAV = 6;
        longAV = 6;
        maxRange = RANGE_LONG;
        techRating = RATING_E;
        availRating = new int[] { RATING_X, RATING_X, RATING_F };
        introDate = 3058;
        techLevel.put(3058, techLevel.get(3071));
        techLevel.put(3082, TechConstants.T_IS_TW_NON_BOX);
    }
}