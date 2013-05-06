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
public class CLStreakSRM4IOS extends StreakSRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -8016331798107342544L;

    /**
     *
     */
    public CLStreakSRM4IOS() {
        techLevel.put(3071,TechConstants.T_CLAN_ADVANCED);
        name = "Streak SRM 4 (I-OS)";
        setInternalName("CLStreakSRM4 (IOS)");
        addLookupName("Clan IOS Streak SRM-4");
        addLookupName("Clan Streak SRM 4 (IOS)");
        heat = 3;
        rackSize = 4;
        shortRange = 4;
        mediumRange = 8;
        longRange = 12;
        extremeRange = 16;
        tonnage = 1.5f;
        criticals = 1;
        flags = flags.or(F_NO_FIRES).or(F_ONESHOT);
        bv = 16;
        cost = 72000;
        shortAV = 8;
        medAV = 8;
        maxRange = RANGE_MED;
        techRating = RATING_B;
        availRating = new int[]{RATING_X, RATING_X, RATING_F};
        introDate = 3058;
        techLevel.put(3058,techLevel.get(3071));
    }
}
