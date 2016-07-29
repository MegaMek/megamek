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
public class ISLongTomCannon extends ArtilleryCannonWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -3643634306982832651L;

    public ISLongTomCannon() {
        super();

        name = "Long Tom Cannon";
        setInternalName("ISLongTomCannon");
        addLookupName("ISLongTomArtilleryCannon");
        addLookupName("IS Long Tom Cannon");
        heat = 20;
        rackSize = 20;
        ammoType = AmmoType.T_LONG_TOM_CANNON;
        tonnage = 20f;
        criticals = 15;
        bv = 329;
        cost = 650000;
        minimumRange = 4;
        shortRange = 6;
        mediumRange = 13;
        longRange = 20;
        extremeRange = 26;
        shortAV = 20;
        medAV = 20;
        longAV = 20;
        maxRange = RANGE_LONG;
        introDate = 3012;
        techLevel.put(3012, TechConstants.T_IS_EXPERIMENTAL);
        techLevel.put(3079, TechConstants.T_IS_ADVANCED);
        availRating = new int[] { RATING_X ,RATING_F ,RATING_E ,RATING_D};
        techRating = RATING_B;
        rulesRefs = "285, TO";
    }

}
