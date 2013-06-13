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
public class ISStreakSRM4OS extends StreakSRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -8651111887714823028L;

    /**
     *
     */
    public ISStreakSRM4OS() {
        super();
        techLevel.put(3071, TechConstants.T_IS_TW_NON_BOX);
        name = "Streak SRM 4 (OS)";
        setInternalName("ISStreakSRM4OS");
        addLookupName("ISStreakSRM4 (OS)"); // mtf
        addLookupName("IS Streak SRM 4 (OS)"); // tdb
        addLookupName("OS Streak SRM-4"); // mep
        heat = 3;
        rackSize = 4;
        shortRange = 3;
        mediumRange = 6;
        longRange = 9;
        extremeRange = 12;
        tonnage = 3.5f;
        criticals = 1;
        flags = flags.or(F_NO_FIRES).or(F_ONESHOT);
        bv = 12;
        cost = 45000;
        shortAV = 8;
        maxRange = RANGE_SHORT;
        introDate = 3058;
        techLevel.put(3058, techLevel.get(3071));
        availRating = new int[] { RATING_X, RATING_X, RATING_E };
        techRating = RATING_E;
    }
}
