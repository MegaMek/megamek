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
import megamek.common.TechAdvancement;
import megamek.common.weapons.ArtilleryCannonWeapon;

/**
 * @author Sebastian Brocks
 */
public class CLLongTomCannon extends ArtilleryCannonWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -3643634306982832651L;

    public CLLongTomCannon() {
        super();

        name = "Long Tom Cannon";
        setInternalName("CLLongTomCannon");
        addLookupName("CLLongTomArtilleryCannon");
        addLookupName("CL Long Tom Cannon");
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
        rulesRefs = "285, TO";
        techAdvancement.setTechBase(TechAdvancement.TECH_BASE_CLAN);
        techAdvancement.setClanAdvancement(3032, 3079, DATE_NONE);
        techAdvancement.setTechRating(RATING_B);
        techAdvancement.setAvailability( new int[] { RATING_X, RATING_F, RATING_E, RATING_D });
    }

}

//Commented out in Weapon Type. Stats identical to IS version and Tech Progression captured in the IS version of the weapon for both Clan and IS.