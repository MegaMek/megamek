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
public class CLStreakLRM10 extends StreakLRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 7179570524181470428L;

    /**
     *
     */
    public CLStreakLRM10() {
        super();
        techLevel.put(3071, TechConstants.T_CLAN_EXPERIMENTAL);
        name = "Streak LRM 10";
        setInternalName("CLStreakLRM10");
        addLookupName("Clan Streak LRM-10");
        addLookupName("Clan Streak LRM 10");
        heat = 4;
        rackSize = 10;
        shortRange = 7;
        mediumRange = 14;
        longRange = 21;
        extremeRange = 28;
        tonnage = 5.0f;
        criticals = 2;
        bv = 173;
        cost = 225000;
        shortAV = 10;
        medAV = 10;
        longAV = 10;
        maxRange = RANGE_LONG;
        techRating = RATING_F;
        availRating = new int[] { RATING_X, RATING_X, RATING_F };
        introDate = 3057;
        techLevel.put(3057, techLevel.get(3071));
        techLevel.put(3079, TechConstants.T_CLAN_TW);
    }
}
