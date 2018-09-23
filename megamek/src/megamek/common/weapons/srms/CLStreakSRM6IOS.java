/**
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */
package megamek.common.weapons.srms;

/**
 * @author Sebastian Brocks
 */
public class CLStreakSRM6IOS extends StreakSRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -3098137789514566838L;

    /**
     *
     */
    public CLStreakSRM6IOS() {

        name = "Streak SRM 6 (I-OS)";
        setInternalName("CLStreakSRM6 (IOS)");
        addLookupName("Clan Improved OS Streak SRM-6");
        addLookupName("Clan Streak SRM 6 (IOS)");
        heat = 4;
        rackSize = 6;
        shortRange = 4;
        mediumRange = 8;
        longRange = 12;
        extremeRange = 16;
        tonnage = 2.5;
        criticals = 2;
        flags = flags.or(F_NO_FIRES).or(F_ONESHOT);
        bv = 24;
        cost = 96000;
        shortAV = 12;
        medAV = 12;
        maxRange = RANGE_MED;
        rulesRefs = "327,TO";
        techAdvancement.setTechBase(TECH_BASE_CLAN)
        	.setIntroLevel(false)
        	.setUnofficial(false)
            .setTechRating(RATING_B)
            .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
            .setClanAdvancement(3058, 3081, 3085, DATE_NONE, DATE_NONE)
            .setClanApproximate(false, true, false,false, false)
            .setPrototypeFactions(F_CNC)
            .setProductionFactions(F_CNC);
    }
}
