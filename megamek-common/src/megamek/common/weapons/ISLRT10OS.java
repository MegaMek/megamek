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
public class ISLRT10OS extends LRTWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 2674309948460871883L;

    /**
     *
     */
    public ISLRT10OS() {
        super();
        techLevel.put(3071, TechConstants.T_IS_TW_NON_BOX);
        name = "LRT 10 (OS)";
        setInternalName(name);
        addLookupName("IS OS LRT-10");
        addLookupName("ISLRTorpedo10 (OS)");
        addLookupName("IS LRT 10 (OS)");
        addLookupName("ISLRT10OS");
        heat = 4;
        rackSize = 10;
        minimumRange = 6;
        waterShortRange = 7;
        waterMediumRange = 14;
        waterLongRange = 21;
        waterExtremeRange = 28;
        tonnage = 5.5f;
        criticals = 2;
        bv = 18;
        flags = flags.or(F_ONESHOT);
        cost = 50000;
        introDate = 2676;
        techLevel.put(2676, techLevel.get(3071));
        availRating = new int[] { RATING_C, RATING_C, RATING_C };
        techRating = RATING_C;
    }
}
