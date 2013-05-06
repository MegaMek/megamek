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
public class ISSniperCannon extends ArtilleryCannonWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -6192123762419323551L;

    public ISSniperCannon() {
        super();
        techLevel.put(3071,TechConstants.T_IS_EXPERIMENTAL);
        name = "Sniper Cannon";
        setInternalName("ISSniperCannon");
        addLookupName("ISSniperArtilleryCannon");
        addLookupName("IS Sniper Cannon");
        heat = 10;
        rackSize = 10;
        ammoType = AmmoType.T_SNIPER_CANNON;
        minimumRange = 2;
        shortRange = 4;
        mediumRange = 8;
        longRange = 12;
        extremeRange = 16;
        tonnage = 15f;
        criticals = 10;
        bv = 77;
        cost = 475000;
        shortAV = 10;
        medAV = 10;
        longAV = 10;
        maxRange = RANGE_MED;
        techRating = RATING_B;
        availRating = new int[]{RATING_X, RATING_F, RATING_E};
        introDate = 3012;
        techLevel.put(3012,techLevel.get(3071));
    }

}
