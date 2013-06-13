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
public class ISLRM5IOS extends LRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 3915337270241715850L;

    /**
     *
     */
    public ISLRM5IOS() {
        super();
        techLevel.put(3071, TechConstants.T_IS_ADVANCED);
        name = "LRM 5 (I-OS)";
        setInternalName(name);
        addLookupName("IS IOS LRM-5");
        addLookupName("ISLRM5 (IOS)");
        addLookupName("IS LRM 5 (IOS)");
        heat = 2;
        rackSize = 5;
        minimumRange = 6;
        tonnage = 1.5f;
        criticals = 1;
        bv = 9;
        flags = flags.or(F_ONESHOT);
        cost = 24000;
        shortAV = 3;
        medAV = 3;
        longAV = 3;
        maxRange = RANGE_LONG;
        techRating = RATING_B;
        availRating = new int[] { RATING_X, RATING_X, RATING_F };
        introDate = 3056;
        techLevel.put(3056, techLevel.get(3071));
        techLevel.put(3081, TechConstants.T_IS_TW_NON_BOX);
    }
}
