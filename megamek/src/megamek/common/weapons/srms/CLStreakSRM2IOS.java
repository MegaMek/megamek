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

import megamek.common.SimpleTechLevel;

/**
 * @author Sebastian Brocks
 */
public class CLStreakSRM2IOS extends StreakSRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 2219171972794110915L;

    /**
     *
     */
    public CLStreakSRM2IOS() {

        name = "Streak SRM 2 (I-OS)";
        setInternalName("CLStreakSRM2 (IOS)");
        addLookupName("Clan IOS Streak SRM-2");
        addLookupName("Clan Streak SRM 2 (IOS)");
        heat = 2;
        rackSize = 2;
        shortRange = 4;
        mediumRange = 8;
        longRange = 12;
        extremeRange = 16;
        tonnage = 0.5;
        criticals = 1;
        flags = flags.or(F_NO_FIRES).or(F_ONESHOT).andNot(F_PROTO_WEAPON);
        bv = 8;
        cost = 12000;
        shortAV = 4;
        medAV = 4;
        maxRange = RANGE_MED;
        rulesRefs = "327,TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        techAdvancement.setTechBase(TECH_BASE_CLAN)
        	.setIntroLevel(false)
        	.setUnofficial(false)
            .setTechRating(RATING_B)
            .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
            .setClanAdvancement(DATE_NONE, 3058, 3081, DATE_NONE, DATE_NONE)
            .setClanApproximate(false, false, true, false, false)
            .setPrototypeFactions(F_CNC)
            .setProductionFactions(F_CNC)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);
    }
}
