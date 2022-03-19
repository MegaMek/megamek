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
public class CLStreakLRM15IOS extends StreakLRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 7336450815633311159L;

    /**
     *
     */
    public CLStreakLRM15IOS() {
        super();
        name = "Streak LRM 15 (I-OS)";
        setInternalName("CLIOSStreakLRM15");
        addLookupName("Clan Streak LRM-15 (IOS)");
        addLookupName("Clan Streak LRM 15 (IOS)");
        addLookupName("CLStreakLRM15 (IOS)");
        heat = 5;
        rackSize = 15;
        shortRange = 7;
        mediumRange = 14;
        longRange = 21;
        extremeRange = 28;
        tonnage = 6.5;
        criticals = 3;
        bv = 52;
        flags = flags.or(F_ONESHOT).andNot(F_PROTO_WEAPON);
        cost = 320000;
        shortAV = 15;
        medAV = 15;
        longAV = 15;
        maxRange = RANGE_LONG;
        rulesRefs = "327, TO";
        techAdvancement.setTechBase(TECH_BASE_CLAN).setTechRating(RATING_B)
            .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
            .setClanAdvancement(3058, 3081, 3088).setClanApproximate(false, true, false)
            .setPrototypeFactions(F_CNC).setProductionFactions(F_CNC)
            .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    }
}
