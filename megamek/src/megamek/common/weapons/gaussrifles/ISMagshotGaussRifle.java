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
package megamek.common.weapons.gaussrifles;

import megamek.common.AmmoType;
import megamek.common.SimpleTechLevel;

/**
 * @author Sebastian Brocks
 */
public class ISMagshotGaussRifle extends GaussWeapon {
    /**
     *
     */
    private static final long serialVersionUID = 651029127510862887L;

    /**
     *
     */
    public ISMagshotGaussRifle() {
        super();

        name = "MagShot";
        setInternalName("ISMagshotGR");
        heat = 1;
        damage = 2;
        ammoType = AmmoType.T_MAGSHOT;
        shortRange = 3;
        mediumRange = 6;
        longRange = 9;
        extremeRange = 12;
        shortAV = 2;
        medAV = 2;
        longAV = 2;
        maxRange = RANGE_SHORT;
        tonnage = 0.5;
        criticals = 2;
        bv = 15;
        cost = 8500;
        explosionDamage = 3;
        rulesRefs = "314,TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        techAdvancement.setTechBase(TECH_BASE_IS)
    	.setIntroLevel(false)
    	.setUnofficial(false)
        .setTechRating(RATING_E)
        .setAvailability(RATING_X, RATING_X, RATING_D, RATING_C)
        .setISAdvancement(3059, 3072, 3078, DATE_NONE, DATE_NONE)
        .setISApproximate(true, false, false,false, false)
        .setPrototypeFactions(F_FS)
        .setProductionFactions(F_FS)
        .setStaticTechLevel(SimpleTechLevel.STANDARD);
    }
}
