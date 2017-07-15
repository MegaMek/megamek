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
package megamek.common.weapons.srms;

import megamek.common.weapons.StreakSRMWeapon;

/**
 * @author Sebastian Brocks
 */
public class CLStreakSRM4OS extends StreakSRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -8016331798107342544L;

    /**
     *
     */
    public CLStreakSRM4OS() {

        name = "Streak SRM 4 (OS)";
        setInternalName("CLStreakSRM4 (OS)");
        addLookupName("Clan OS Streak SRM-4");
        addLookupName("Clan Streak SRM 4 (OS)");
        heat = 3;
        rackSize = 4;
        shortRange = 4;
        mediumRange = 8;
        longRange = 12;
        extremeRange = 16;
        tonnage = 2.5f;
        criticals = 1;
        flags = flags.or(F_NO_FIRES).or(F_ONESHOT);
        bv = 16;
        cost = 45000;
        shortAV = 8;
        medAV = 8;
        maxRange = RANGE_MED;
        rulesRefs = "230,TM";
        techAdvancement.setTechBase(TECH_BASE_CLAN)
    	.setIntroLevel(false)
    	.setUnofficial(false)
        .setTechRating(RATING_F)
        .setAvailability(RATING_X, RATING_X, RATING_D, RATING_D)
        .setClanAdvancement(2819, 2822, 2830, DATE_NONE, DATE_NONE)
        .setClanApproximate(false, true, false,false, false)
        .setPrototypeFactions(F_CSA)
        .setProductionFactions(F_CSA);
    }
}
