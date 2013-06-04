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
public class ISLRM10IOS extends LRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -2792101005477263443L;

    /**
     *
     */
    public ISLRM10IOS() {
        super();
        techLevel.put(3071, TechConstants.T_IS_ADVANCED);
        name = "LRM 10 (I-OS)";
        setInternalName(name);
        addLookupName("IS IOS LRM-10");
        addLookupName("ISLRM10 (IOS)");
        addLookupName("IS LRM 10 (IOS)");
        heat = 4;
        rackSize = 10;
        minimumRange = 6;
        tonnage = 4.5f;
        criticals = 2;
        bv = 18;
        flags = flags.or(F_ONESHOT);
        cost = 80000;
        shortAV = 6;
        medAV = 6;
        longAV = 6;
        maxRange = RANGE_LONG;
        techRating = RATING_B;
        availRating = new int[] { RATING_X, RATING_X, RATING_F };
        introDate = 3056;
        techLevel.put(3056, techLevel.get(3071));
        techLevel.put(3081, TechConstants.T_IS_TW_NON_BOX);
    }
}
