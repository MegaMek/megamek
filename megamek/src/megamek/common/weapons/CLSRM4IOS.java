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
public class CLSRM4IOS extends SRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -6513977729364835643L;

    /**
     *
     */
    public CLSRM4IOS() {
        super();
        techLevel.put(3071, TechConstants.T_CLAN_ADVANCED);
        name = "SRM 4 (I-OS)";
        setInternalName("CLSRM4 (IOS)");
        addLookupName("Clan IOS SRM-4");
        addLookupName("Clan SRM 4 (IOS)");
        heat = 3;
        rackSize = 4;
        shortRange = 3;
        mediumRange = 6;
        longRange = 9;
        extremeRange = 12;
        tonnage = 0.5f;
        criticals = 1;
        bv = 8;
        flags = flags.or(F_NO_FIRES).or(F_ONESHOT);
        cost = 48000;
        shortAV = 4;
        maxRange = RANGE_SHORT;
        techRating = RATING_B;
        availRating = new int[] { RATING_X, RATING_X, RATING_F };
        introDate = 3058;
        techLevel.put(3058, techLevel.get(3071));
        techLevel.put(3081, TechConstants.T_CLAN_TW);
    }
}
