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
public class CLSRM4 extends SRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -6776541552712952370L;

    /**
     *
     */
    public CLSRM4() {
        super();
        techLevel.put(3071, TechConstants.T_CLAN_TW);
        name = "SRM 4";
        setInternalName("CLSRM4");
        addLookupName("Clan SRM-4");
        addLookupName("Clan SRM 4");
        heat = 3;
        rackSize = 4;
        shortRange = 3;
        mediumRange = 6;
        longRange = 9;
        extremeRange = 12;
        tonnage = 1.0f;
        criticals = 1;
        bv = 39;
        flags = flags.or(F_NO_FIRES);
        cost = 60000;
        shortAV = 4;
        maxRange = RANGE_SHORT;
        introDate = 2824;
        techLevel.put(2824, techLevel.get(3071));
        availRating = new int[] { RATING_X, RATING_C, RATING_B };
        techRating = RATING_F;
    }
}
