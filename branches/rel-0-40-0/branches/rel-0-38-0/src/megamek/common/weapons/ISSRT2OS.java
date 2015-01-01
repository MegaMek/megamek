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
public class ISSRT2OS extends SRTWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -1737331597344761915L;

    /**
     *
     */
    public ISSRT2OS() {
        super();
        techLevel.put(3071, TechConstants.T_IS_TW_NON_BOX);
        name = "SRT 2 (OS)";
        setInternalName("ISSRT2OS");
        addLookupName("ISSRT2 (OS)"); // mtf
        addLookupName("IS SRT 2 (OS)"); // tdb
        addLookupName("OS SRT-2"); // mep
        heat = 2;
        rackSize = 2;
        waterShortRange = 3;
        waterMediumRange = 6;
        waterLongRange = 9;
        waterExtremeRange = 12;
        tonnage = 1.5f;
        criticals = 1;
        bv = 4;
        flags = flags.or(F_NO_FIRES).or(F_ONESHOT);
        cost = 5000;
        introDate = 2676;
        techLevel.put(2676, techLevel.get(3071));
        availRating = new int[] { RATING_C, RATING_C, RATING_C };
        techRating = RATING_C;
    }
}
