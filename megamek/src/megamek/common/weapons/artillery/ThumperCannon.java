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
package megamek.common.weapons.artillery;

import megamek.common.AmmoType;
import megamek.common.SimpleTechLevel;

/**
 * @author Sebastian Brocks
 */
public class ThumperCannon extends ArtilleryCannonWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -1951764278554798130L;

    public ThumperCannon() {
        super();

        name = "Thumper Cannon";
        setInternalName("ISThumperCannon");
        addLookupName("ISThumperArtilleryCannon");
        addLookupName("IS Thumper Cannon");
        addLookupName("CLThumper Cannon");
        addLookupName("CLThumperArtilleryCannon");
        addLookupName("CL Thumper Cannon");
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
        rulesRefs = "285,TO";
        techAdvancement.setTechBase(TECH_BASE_ALL)
            .setTechRating(RATING_B).setAvailability(RATING_X, RATING_F, RATING_E, RATING_D)
            .setISAdvancement(3012, 3079).setClanAdvancement(3032, 3079)
            .setPrototypeFactions(F_LC,F_CWF).setProductionFactions(F_LC)
            .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    }

}
