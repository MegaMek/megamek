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
public class CLStreakLRM20IOS extends StreakLRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -7687203185594888323L;

    /**
     *
     */
    public CLStreakLRM20IOS() {
        super();
        techLevel.put(3071,TechConstants.T_CLAN_EXPERIMENTAL);
        name = "Streak LRM 20 (I-OS)";
        setInternalName("CLIOSStreakLRM20");
        addLookupName("Clan Streak LRM-20 (IOS)");
        addLookupName("Clan Streak LRM 20 (IOS)");
        addLookupName("CLStreakLRM20 (IOS)");
        heat = 6;
        rackSize = 20;
        shortRange = 7;
        mediumRange = 14;
        longRange = 21;
        extremeRange = 28;
        tonnage = 9.5f;
        criticals = 5;
        bv = 69;
        flags = flags.or(F_ONESHOT);
        cost = 480000;
        shortAV = 20;
        medAV = 20;
        longAV = 20;
        maxRange = RANGE_LONG;
        techRating = RATING_B;
        availRating = new int[]{RATING_X, RATING_X, RATING_F};
        introDate = 3058;
        techLevel.put(3058,techLevel.get(3071));
    }
}
