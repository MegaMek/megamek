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
import megamek.common.TechAdvancement;

/**
 * @author Sebastian Brocks
 */
public class ISStreakSRM6IOS extends StreakSRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 710360237679723033L;

    /**
     *
     */
    public ISStreakSRM6IOS() {
        super();
        techLevel.put(3071, TechConstants.T_IS_ADVANCED);
        name = "Streak SRM 6 (I-OS)";
        setInternalName("ISStreakSRM6IOS");
        addLookupName("ISStreakSRM6 (IOS)"); // mtf
        addLookupName("IS Streak SRM 6 (IOS)"); // tdb
        addLookupName("IOS Streak SRM-6"); // mep
        heat = 4;
        rackSize = 6;
        shortRange = 3;
        mediumRange = 6;
        longRange = 9;
        extremeRange = 12;
        tonnage = 4.0f;
        criticals = 2;
        flags = flags.or(F_NO_FIRES).or(F_ONESHOT);
        bv = 18;
        cost = 96000;
        shortAV = 12;
        maxRange = RANGE_SHORT;
        introDate = 3056;
        techLevel.put(3056, TechConstants.T_IS_EXPERIMENTAL);
        techLevel.put(3076, TechConstants.T_IS_ADVANCED);
        techLevel.put(3085, TechConstants.T_IS_EXPERIMENTAL);
        availRating = new int[] { RATING_X ,RATING_X ,RATING_F ,RATING_E};
        techRating = RATING_B;
        rulesRefs = "230, TM";;
        techAdvancement.setTechBase(TechAdvancement.TECH_BASE_IS);
        techAdvancement.setISAdvancement(3056, 3071, DATE_NONE);
        techAdvancement.setTechRating(RATING_B);
        techAdvancement.setAvailability( new int[] { RATING_X, RATING_X, RATING_F, RATING_E });
    }
}
