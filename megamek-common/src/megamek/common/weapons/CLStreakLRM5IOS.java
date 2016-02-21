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
package megamek.common.weapons;

import megamek.common.TechConstants;

/**
 * @author Sebastian Brocks
 */
public class CLStreakLRM5IOS extends StreakLRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 540083231235504476L;

    /**
     *
     */
    public CLStreakLRM5IOS() {
        super();
        techLevel.put(3071, TechConstants.T_CLAN_EXPERIMENTAL);
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
        tonnage = 1.5f;
        criticals = 1;
        bv = 17;
        flags = flags.or(F_ONESHOT);
        cost = 60000;
        shortAV = 5;
        medAV = 5;
        longAV = 5;
        maxRange = RANGE_LONG;
        techRating = RATING_B;
        availRating = new int[] { RATING_X, RATING_X, RATING_F };
        introDate = 3058;
        techLevel.put(3058, techLevel.get(3071));
        techLevel.put(3079, TechConstants.T_CLAN_ADVANCED);
    }
}
