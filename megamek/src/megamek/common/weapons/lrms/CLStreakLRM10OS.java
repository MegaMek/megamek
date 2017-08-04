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
public class CLStreakLRM10OS extends StreakLRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 2692279526867532848L;

    /**
     *
     */
    public CLStreakLRM10OS() {
        super();
        name = "Streak LRM 10 (OS)";
        setInternalName("CLOSStreakLRM10");
        addLookupName("Clan Streak LRM-10 (OS)");
        addLookupName("Clan Streak LRM 10 (OS)");
        addLookupName("CLStreakLRM10 (OS)");
        heat = 4;
        rackSize = 10;
        shortRange = 7;
        mediumRange = 14;
        longRange = 21;
        extremeRange = 28;
        tonnage = 5.5f;
        criticals = 2;
        bv = 35;
        flags = flags.or(F_ONESHOT);
        cost = 112500;
        shortAV = 10;
        medAV = 10;
        longAV = 10;
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
