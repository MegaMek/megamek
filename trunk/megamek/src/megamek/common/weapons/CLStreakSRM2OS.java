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
public class CLStreakSRM2OS extends StreakSRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 2219171972794110915L;

    /**
     *
     */
    public CLStreakSRM2OS() {
        techLevel.put(3071, TechConstants.T_CLAN_TW);
        name = "Streak SRM 2 (OS)";
        setInternalName("CLStreakSRM2 (OS)");
        addLookupName("Clan OS Streak SRM-2");
        addLookupName("Clan Streak SRM 2 (OS)");
        heat = 2;
        rackSize = 2;
        shortRange = 4;
        mediumRange = 8;
        longRange = 12;
        extremeRange = 16;
        tonnage = 1.5f;
        criticals = 1;
        flags = flags.or(F_NO_FIRES).or(F_ONESHOT);
        bv = 8;
        cost = 7500;
        shortAV = 4;
        medAV = 4;
        maxRange = RANGE_MED;
        introDate = 2826;
        techLevel.put(2826, techLevel.get(3071));
        availRating = new int[] { RATING_E, RATING_F, RATING_D };
        techRating = RATING_F;

    }
}
