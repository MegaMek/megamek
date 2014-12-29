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
public class CLSRM6OS extends SRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 5184043200202465163L;

    /**
     *
     */
    public CLSRM6OS() {
        super();
        techLevel.put(3071, TechConstants.T_CLAN_TW);
        name = "SRM 6 (OS)";
        setInternalName("CLSRM6 (OS)");
        addLookupName("Clan OS SRM-6");
        addLookupName("Clan SRM 6 (OS)");
        heat = 4;
        rackSize = 6;
        shortRange = 3;
        mediumRange = 6;
        longRange = 9;
        extremeRange = 12;
        tonnage = 2.0f;
        criticals = 1;
        bv = 12;
        flags = flags.or(F_NO_FIRES).or(F_ONESHOT);
        cost = 40000;
        shortAV = 8;
        maxRange = RANGE_SHORT;
        introDate = 2824;
        techLevel.put(2824, techLevel.get(3071));
        availRating = new int[] { RATING_X, RATING_C, RATING_B };
        techRating = RATING_F;
    }
}
