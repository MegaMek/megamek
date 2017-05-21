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
 * Created on Sep 25, 2004
 *
 */
package megamek.common.weapons.autocannons;

import megamek.common.weapons.RifleWeapon;

/**
 * @author Jason Tighe
 */
public class ISRifleHeavy extends RifleWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -2670817452732971454L;

    /**
     *
     */
    public ISRifleHeavy() {
        super();
        name = "Rifle (Cannon, Heavy)";
        setInternalName(name);
        addLookupName("IS Heavy Rifle");
        addLookupName("ISHeavyRifle");
        heat = 4;
        damage = 9;
        rackSize = 9;
        minimumRange = 2;
        shortRange = 6;
        mediumRange = 12;
        longRange = 18;
        extremeRange = 27;
        tonnage = 8.0f;
        criticals = 3;
        bv = 91;
        cost = 90000;
        explosive = false; // when firing incendiary ammo
        shortAV = 4;
        medAV = 8;
        longAV = 8;
        extAV = 8;
        maxRange = RANGE_MED;
        explosionDamage = 0;
        rulesRefs = "338,TO";
        techAdvancement.setTechBase(TECH_BASE_IS)
    	.setIntroLevel(false)
    	.setUnofficial(false)
        .setTechRating(RATING_B)
        .setAvailability(RATING_C, RATING_F, RATING_X, RATING_D)
        .setISAdvancement(DATE_PS, DATE_PS, 3085, 2825, 3084)
        .setISApproximate(false, false, false,true, true)
        .setPrototypeFactions(F_IS)
        .setProductionFactions(F_IS)
        .setReintroductionFactions(F_IS);
    }
}
