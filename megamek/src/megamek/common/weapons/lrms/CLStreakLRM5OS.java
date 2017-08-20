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
package megamek.common.weapons.lrms;

/**
 * @author Sebastian Brocks
 */
public class CLStreakLRM5OS extends StreakLRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 540083231235504476L;

    /**
     *
     */
    public CLStreakLRM5OS() {
        super();
        name = "Streak LRM 5 (OS)";
        setInternalName("CLOSStreakLRM5");
        addLookupName("Clan Streak LRM-5 (OS)");
        addLookupName("Clan Streak LRM 5 (OS)");
        addLookupName("CLStreakLRM5 (OS)");
        heat = 2;
        rackSize = 5;
        shortRange = 7;
        mediumRange = 14;
        longRange = 21;
        extremeRange = 28;
        tonnage = 2.5f;
        criticals = 1;
        bv = 17;
        flags = flags.or(F_ONESHOT);
        cost = 37500;
        shortAV = 5;
        medAV = 5;
        longAV = 5;
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
