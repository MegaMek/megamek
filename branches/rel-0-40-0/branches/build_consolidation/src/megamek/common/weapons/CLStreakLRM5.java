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
package megamek.common.weapons;

import megamek.common.TechConstants;

/**
 * @author Sebastian Brocks
 */
public class CLStreakLRM5 extends StreakLRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 5240577239366457930L;

    /**
     *
     */
    public CLStreakLRM5() {
        super();
        techLevel.put(3071, TechConstants.T_CLAN_EXPERIMENTAL);
        name = "Streak LRM 5";
        setInternalName("CLStreakLRM5");
        addLookupName("Clan Streak LRM-5");
        addLookupName("Clan Streak LRM 5");
        heat = 2;
        rackSize = 5;
        shortRange = 7;
        mediumRange = 14;
        longRange = 21;
        extremeRange = 28;
        tonnage = 2.0f;
        criticals = 1;
        bv = 86;
        cost = 75000;
        shortAV = 5;
        medAV = 5;
        longAV = 5;
        maxRange = RANGE_LONG;
        techRating = RATING_F;
        availRating = new int[] { RATING_X, RATING_X, RATING_F };
        introDate = 3057;
        techLevel.put(3057, techLevel.get(3071));
        techLevel.put(3079, TechConstants.T_CLAN_ADVANCED);
    }
}
