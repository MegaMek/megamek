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
public class ISLRT15 extends LRTWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -6506265507271800879L;

    /**
     *
     */
    public ISLRT15() {
        super();
        techLevel.put(3071,TechConstants.T_IS_TW_NON_BOX);
        name = "LRT 15";
        setInternalName(name);
        addLookupName("IS LRT-15");
        addLookupName("ISLRTorpedo15");
        addLookupName("IS LRT 15");
        addLookupName("ISLRT15");
        heat = 5;
        rackSize = 15;
        minimumRange = 6;
        waterShortRange = 7;
        waterMediumRange = 14;
        waterLongRange = 21;
        waterExtremeRange = 28;
        tonnage = 7.0f;
        criticals = 3;
        bv = 136;
        cost = 175000;
        introDate = 2380;
        techLevel.put(2380,techLevel.get(3071));
        availRating = new int[]{RATING_C,RATING_C,RATING_C};
        techRating = RATING_C;
    }
}
