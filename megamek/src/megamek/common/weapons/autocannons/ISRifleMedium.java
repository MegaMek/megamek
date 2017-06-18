package megamek.common.weapons.autocannons;

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

/**
 * @author Jason Tighe
 */
public class ISRifleMedium extends RifleWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 3540374668501692337L;

    /**
     *
     */
    public ISRifleMedium() {
        super();
        name = "Rifle (Cannon, Medium)";
        setInternalName(name);
        addLookupName("IS Medium Rifle");
        addLookupName("ISMediumRifle");
        heat = 2;
        damage = 6;
        rackSize = 6;
        minimumRange = 1;
        shortRange = 5;
        mediumRange = 10;
        longRange = 15;
        extremeRange = 22;
        tonnage = 5.0f;
        criticals = 2;
        bv = 51;
        cost = 75750;
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
