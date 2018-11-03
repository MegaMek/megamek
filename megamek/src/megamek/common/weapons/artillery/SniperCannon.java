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
public class SniperCannon extends ArtilleryCannonWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -6192123762419323551L;

    public SniperCannon() {
        super();

        name = "Sniper Cannon";
        setInternalName("ISSniperCannon");
        addLookupName("ISSniperArtilleryCannon");
        addLookupName("IS Sniper Cannon");
        addLookupName("CLSniper Cannon");
        addLookupName("CLSniperArtilleryCannon");
        addLookupName("CL Sniper Cannon");
        heat = 10;
        rackSize = 10;
        ammoType = AmmoType.T_SNIPER_CANNON;
        minimumRange = 2;
        shortRange = 4;
        mediumRange = 8;
        longRange = 12;
        extremeRange = 16;
        tonnage = 15;
        criticals = 10;
        bv = 77;
        cost = 475000;
        shortAV = 10;
        medAV = 10;
        longAV = 10;
        maxRange = RANGE_MED;
        rulesRefs = "285,TO";
        techAdvancement.setTechBase(TECH_BASE_ALL)
            .setTechRating(RATING_B).setAvailability(RATING_X, RATING_F, RATING_E, RATING_D)
            .setISAdvancement(3012, 3079).setClanAdvancement(3032, 3079)
            .setPrototypeFactions(F_LC,F_CWF).setProductionFactions(F_LC)
            .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    }

}
