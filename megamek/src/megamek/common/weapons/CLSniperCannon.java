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
import megamek.common.TechAdvancement;

/**
 * @author Sebastian Brocks
 */
public class CLSniperCannon extends ArtilleryCannonWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -6192123762419323551L;

    public CLSniperCannon() {
        super();

        name = "Sniper Cannon";
        setInternalName("CLSniper Cannon");
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
        tonnage = 15f;
        criticals = 10;
        bv = 77;
        cost = 475000;
        shortAV = 10;
        medAV = 10;
        longAV = 10;
        maxRange = RANGE_MED;
        rulesRefs = "285, TO";
        techAdvancement.setTechBase(TechAdvancement.TECH_BASE_CLAN);
        techAdvancement.setClanAdvancement(3032, 3079, DATE_NONE);
        techAdvancement.setTechRating(RATING_B);
        techAdvancement.setAvailability( new int[] { RATING_X, RATING_F, RATING_E, RATING_D });
    }

}

//Commented out in Weapon Type. Stats identical to IS version and Tech Progression captured in the IS version of the weapon for both Clan and IS.