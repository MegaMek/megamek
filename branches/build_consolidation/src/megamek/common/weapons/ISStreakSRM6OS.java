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
public class ISStreakSRM6OS extends StreakSRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 710360237679723033L;

    /**
     *
     */
    public ISStreakSRM6OS() {
        super();
        techLevel.put(3071, TechConstants.T_IS_TW_NON_BOX);
        name = "Streak SRM 6 (OS)";
        setInternalName("ISStreakSRM6OS");
        addLookupName("ISStreakSRM6 (OS)"); // mtf
        addLookupName("IS Streak SRM 6 (OS)"); // tdb
        addLookupName("OS Streak SRM-6"); // mep
        heat = 4;
        rackSize = 6;
        shortRange = 3;
        mediumRange = 6;
        longRange = 9;
        extremeRange = 12;
        tonnage = 5.0f;
        criticals = 2;
        flags = flags.or(F_NO_FIRES).or(F_ONESHOT);
        bv = 18;
        cost = 60000;
        shortAV = 12;
        maxRange = RANGE_SHORT;
        introDate = 3058;
        techLevel.put(3058, techLevel.get(3071));
        availRating = new int[] { RATING_X, RATING_X, RATING_E };
        techRating = RATING_E;
    }
}
