/**
 * MegaMek - Copyright (C) 2004,2005 Ben Mazur (bmazur@sev.org)
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
/*
 * Created on Oct 20, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.AmmoType;
import megamek.common.TechConstants;

/**
 * @author Sebastian Brocks
 */
public class ISThumperCannon extends ArtilleryCannonWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -1951764278554798130L;

    public ISThumperCannon() {
        super();
        techLevel.put(3071, TechConstants.T_IS_EXPERIMENTAL);
        name = "Thumper Cannon";
        setInternalName("ISThumperCannon");
        addLookupName("ISThumperArtilleryCannon");
        addLookupName("IS Thumper Cannon");
        heat = 5;
        rackSize = 5;
        ammoType = AmmoType.T_THUMPER_CANNON;
        minimumRange = 3;
        shortRange = 4;
        mediumRange = 9;
        longRange = 14;
        extremeRange = 18;
        tonnage = 10f;
        criticals = 7;
        bv = 41;
        cost = 200000;
        shortAV = 5;
        medAV = 5;
        longAV = 5;
        maxRange = RANGE_MED;
        techRating = RATING_B;
        availRating = new int[] { RATING_X, RATING_F, RATING_E };
        introDate = 3012;
        techLevel.put(3012, techLevel.get(3071));
        techLevel.put(3079, TechConstants.T_IS_ADVANCED);
    }

}
