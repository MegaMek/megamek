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
package megamek.common.weapons.lrms;

import megamek.common.SimpleTechLevel;

/**
 * @author Sebastian Brocks
 */
public class CLStreakLRM5IOS extends StreakLRMWeapon {

    private static final long serialVersionUID = 540083231235504476L;

    public CLStreakLRM5IOS() {
        super();
        name = "Streak LRM 5 (I-OS)";
        setInternalName("CLIOSStreakLRM5");
        addLookupName("Clan Streak LRM-5 (IOS)");
        addLookupName("Clan Streak LRM 5 (IOS)");
        addLookupName("CLStreakLRM5 (IOS)");
        heat = 2;
        rackSize = 5;
        shortRange = 7;
        mediumRange = 14;
        longRange = 21;
        extremeRange = 28;
        tonnage = 1.5;
        criticals = 1;
        bv = 17;
        flags = flags.or(F_ONESHOT).andNot(F_PROTO_WEAPON);
        cost = 60000;
        shortAV = 5;
        medAV = 5;
        longAV = 5;
        maxRange = RANGE_LONG;
        rulesRefs = "327,TO";
        techAdvancement.setTechBase(TECH_BASE_CLAN).setTechRating(RATING_B)
            .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
            .setClanAdvancement(3058, 3081, 3088).setClanApproximate(false, true, false)
            .setPrototypeFactions(F_CNC).setProductionFactions(F_CNC)
            .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    }
}
