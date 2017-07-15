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
package megamek.common.weapons.missiles;

import megamek.common.weapons.StreakLRMWeapon;

/**
 * @author Sebastian Brocks
 */
public class CLStreakLRM15OS extends StreakLRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 7336450815633311159L;

    /**
     *
     */
    public CLStreakLRM15OS() {
        super();
        name = "Streak LRM 15 (OS)";
        setInternalName("CLOSStreakLRM15");
        addLookupName("Clan Streak LRM-15 (OS)");
        addLookupName("Clan Streak LRM 15 (OS)");
        addLookupName("CLStreakLRM15 (OS)");
        heat = 5;
        rackSize = 15;
        shortRange = 7;
        mediumRange = 14;
        longRange = 21;
        extremeRange = 28;
        tonnage = 7.5f;
        criticals = 3;
        bv = 52;
        flags = flags.or(F_ONESHOT);
        cost = 200000;
        shortAV = 15;
        medAV = 15;
        longAV = 15;
        maxRange = RANGE_LONG;
        // Per Herb all ProtoMech launcher use the ProtoMech Chassis progression. 
        //But LRM Tech Base and Avail Ratings.
        rulesRefs = "327,TO";
        techAdvancement.setTechBase(TECH_BASE_CLAN)
    	.setIntroLevel(false)
    	.setUnofficial(false)
        .setTechRating(RATING_F)
        .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
        .setClanAdvancement(3057, 3079, 3088, DATE_NONE, DATE_NONE)
        .setClanApproximate(false, true, false,false, false)
        .setPrototypeFactions(F_CCY)
        .setProductionFactions(F_CJF);
    }
}
