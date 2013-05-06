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
public class ISLRM15IOS extends LRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 1089075678687256997L;

    /**
     *
     */
    public ISLRM15IOS() {
        super();
        techLevel.put(3071,TechConstants.T_IS_ADVANCED);
        name = "LRM 15 (I-OS)";
        setInternalName(name);
        addLookupName("IS IOS LRM-15");
        addLookupName("ISLRM15 (IOS)");
        addLookupName("IS LRM 15 (IOS)");
        heat = 5;
        rackSize = 15;
        minimumRange = 6;
        tonnage = 6.5f;
        criticals = 3;
        bv = 27;
        flags = flags.or(F_ONESHOT);
        cost = 140000;
        shortAV = 9;
        medAV = 9;
        longAV = 9;
        maxRange = RANGE_LONG;
        techRating = RATING_B;
        availRating = new int[]{RATING_X, RATING_X, RATING_F};
        introDate = 3056;
        techLevel.put(3056,techLevel.get(3071));
    }
}
