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
public class ISStreakSRM2OS extends StreakSRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 4837075335425856208L;

    /**
     *
     */
    public ISStreakSRM2OS() {
        super();
        techLevel.put(3071, TechConstants.T_IS_TW_NON_BOX);
        name = "Streak SRM 2 (OS)";
        setInternalName("ISStreakSRM2OS");
        addLookupName("ISStreakSRM2 (OS)"); // mtf
        addLookupName("IS Streak SRM 2 (OS)"); // tdb
        addLookupName("OS Streak SRM-2"); // mep
        heat = 2;
        rackSize = 2;
        shortRange = 3;
        mediumRange = 6;
        longRange = 9;
        extremeRange = 12;
        tonnage = 2.0f;
        criticals = 1;
        flags = flags.or(F_NO_FIRES).or(F_ONESHOT);
        bv = 6;
        cost = 7500;
        shortAV = 4;
        maxRange = RANGE_SHORT;
        introDate = 2676;
        techLevel.put(2676, techLevel.get(3071));
        extinctDate = 2845;
        reintroDate = 3035;
        availRating = new int[] { RATING_E, RATING_F, RATING_D };
        techRating = RATING_E;
    }
}
