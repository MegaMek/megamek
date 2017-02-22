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
public class ISStreakSRM4OS extends StreakSRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -8651111887714823028L;

    /**
     *
     */
    public ISStreakSRM4OS() {
        super();

        name = "Streak SRM 4 (OS)";
        setInternalName("ISStreakSRM4OS");
        addLookupName("ISStreakSRM4 (OS)"); // mtf
        addLookupName("IS Streak SRM 4 (OS)"); // tdb
        addLookupName("OS Streak SRM-4"); // mep
        heat = 3;
        rackSize = 4;
        shortRange = 3;
        mediumRange = 6;
        longRange = 9;
        extremeRange = 12;
        tonnage = 3.5f;
        criticals = 1;
        flags = flags.or(F_NO_FIRES).or(F_ONESHOT);
        bv = 12;
        cost = 45000;
        shortAV = 8;
        maxRange = RANGE_SHORT;
        introDate = 3055;
        techLevel.put(3055, TechConstants.T_IS_TW_NON_BOX);
        availRating = new int[] { RATING_X ,RATING_X ,RATING_E ,RATING_D};
        techRating = RATING_E;
        rulesRefs = "230, TM";
        techProgression.setTechBase(TechProgression.TECH_BASE_IS);
        techProgression.setISProgression(DATE_NONE, DATE_NONE, 3055);
        techProgression.setTechRating(RATING_E);
        techProgression.setAvailability( new int[] { RATING_X, RATING_X, RATING_E, RATING_D });
    }
}
