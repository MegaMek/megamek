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
public class ISSRT2IOS extends SRTWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -1737331597344761915L;

    /**
     *
     */
    public ISSRT2IOS() {
        super();
        techLevel.put(3071, TechConstants.T_IS_ADVANCED);
        name = "SRT 2 (I-OS)";
        setInternalName("ISSRT2IOS");
        addLookupName("ISSRT2 (IOS)"); // mtf
        addLookupName("IS SRT 2 (IOS)"); // tdb
        addLookupName("IOS SRT-2"); // mep
        heat = 2;
        rackSize = 2;
        waterShortRange = 3;
        waterMediumRange = 6;
        waterLongRange = 9;
        waterExtremeRange = 12;
        tonnage = 0.5f;
        criticals = 1;
        bv = 4;
        flags = flags.or(F_NO_FIRES).or(F_ONESHOT);
        cost = 8000;
        techRating = RATING_B;
        availRating = new int[] { RATING_X, RATING_X, RATING_F };
        introDate = 3056;
        techLevel.put(3056, techLevel.get(3071));
        techLevel.put(3081, TechConstants.T_IS_TW_NON_BOX);
    }
}
