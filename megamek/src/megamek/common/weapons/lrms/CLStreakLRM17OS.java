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

import megamek.common.SimpleTechLevel;

/**
 * @author Sebastian Brocks
 */
public class CLStreakLRM17OS extends StreakLRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 5240577239366457930L;

    /**
     *
     */
    public CLStreakLRM17OS() {
        super();
        name = "Streak LRM 17 (OS)";
        setInternalName("CLStreakLRM17OS");
        addLookupName("Clan Streak LRM-17 (OS)");
        addLookupName("Clan Streak LRM 17 (OS)");
        heat = 0;
        rackSize = 17;
        shortRange = 7;
        mediumRange = 14;
        longRange = 21;
        extremeRange = 28;
        tonnage = 7.3;
        criticals = 1;
        bv = 293;
        cost = 255000;
        flags = flags.or(F_ONESHOT).andNot(F_PROTO_WEAPON);
        techAdvancement.setTechBase(TECH_BASE_CLAN).setTechRating(RATING_F)
            .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
            .setClanAdvancement(3057, 3079, 3088).setClanApproximate(false, true, false)
            .setPrototypeFactions(F_CCY).setProductionFactions(F_CJF)
            .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    }
}
