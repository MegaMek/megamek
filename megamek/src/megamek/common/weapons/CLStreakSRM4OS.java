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
import megamek.common.TechProgression;

/**
 * @author Sebastian Brocks
 */
public class CLStreakSRM4OS extends StreakSRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -8016331798107342544L;

    /**
     *
     */
    public CLStreakSRM4OS() {

        name = "Streak SRM 4 (OS)";
        setInternalName("CLStreakSRM4 (OS)");
        addLookupName("Clan OS Streak SRM-4");
        addLookupName("Clan Streak SRM 4 (OS)");
        heat = 3;
        rackSize = 4;
        shortRange = 4;
        mediumRange = 8;
        longRange = 12;
        extremeRange = 16;
        tonnage = 2.5f;
        criticals = 1;
        flags = flags.or(F_NO_FIRES).or(F_ONESHOT);
        bv = 16;
        cost = 45000;
        shortAV = 8;
        medAV = 8;
        maxRange = RANGE_MED;
        introDate = 2817;
        techLevel.put(2817, TechConstants.T_CLAN_EXPERIMENTAL);   ///EXP
        techLevel.put(2819, TechConstants.T_CLAN_ADVANCED);   ///ADV
        techLevel.put(2830, TechConstants.T_CLAN_TW);   ///COMMON
        availRating = new int[] { RATING_X, RATING_D, RATING_D, RATING_D };
        techRating = RATING_F;
        rulesRefs = "230, TM";
        techProgression.setTechBase(TechProgression.TECH_BASE_CLAN);
        techProgression.setClanProgression(2817, 2819, 2830);
        techProgression.setTechRating(RATING_F);
        techProgression.setAvailability( new int[] { RATING_X, RATING_D, RATING_D, RATING_D });
    }
}
