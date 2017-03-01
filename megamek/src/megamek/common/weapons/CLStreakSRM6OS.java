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

import megamek.common.TechAdvancement;

/**
 * @author Sebastian Brocks
 */
public class CLStreakSRM6OS extends StreakSRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -3098137789514566838L;

    /**
     *
     */
    public CLStreakSRM6OS() {

        name = "Streak SRM 6 (OS)";
        setInternalName("CLStreakSRM6 (OS)");
        addLookupName("Clan Improved OS Streak SRM-6");
        addLookupName("Clan Streak SRM 6 (OS)");
        heat = 4;
        rackSize = 6;
        shortRange = 4;
        mediumRange = 8;
        longRange = 12;
        extremeRange = 16;
        tonnage = 3.5f;
        criticals = 2;
        flags = flags.or(F_NO_FIRES).or(F_ONESHOT);
        bv = 24;
        cost = 60000;
        shortAV = 12;
        medAV = 12;
        maxRange = RANGE_MED;
        rulesRefs = "230, TM";
        techAdvancement.setTechBase(TechAdvancement.TECH_BASE_CLAN);
        techAdvancement.setClanAdvancement(2817, 2819, 2830);
        techAdvancement.setTechRating(RATING_F);
        techAdvancement.setAvailability( new int[] { RATING_X, RATING_D, RATING_D, RATING_D });
    }
}
