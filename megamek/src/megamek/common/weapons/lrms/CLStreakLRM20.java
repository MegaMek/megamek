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
public class CLStreakLRM20 extends StreakLRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -7125174806713066191L;

    /**
     *
     */
    public CLStreakLRM20() {
        super();
        name = "Streak LRM 20";
        setInternalName("CLStreakLRM20");
        addLookupName("Clan Streak LRM-20");
        addLookupName("Clan Streak LRM 20");
        heat = 6;
        rackSize = 20;
        shortRange = 7;
        mediumRange = 14;
        longRange = 21;
        extremeRange = 28;
        tonnage = 10.0f;
        criticals = 5;
        bv = 345;
        cost = 600000;
        shortAV = 20;
        medAV = 20;
        longAV = 20;
        maxRange = RANGE_LONG;
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
