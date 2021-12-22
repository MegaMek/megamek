package megamek.common.weapons.lasers;

import megamek.common.SimpleTechLevel;

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
 * Created on Sep 12, 2004
 *
 */

/**
 * @author Sebastian Brocks
 */
public class ISReengineeredLaserLarge extends ReengineeredLaserWeapon {


    /**
     *
     */
    private static final long serialVersionUID = -7304496499826505883L;

    public ISReengineeredLaserLarge() {
        super();
        name = "Large Re-engineered Laser";
        setInternalName(name);
        addLookupName("ISLargeReengineeredLaser");
        addLookupName("ISLargeRELaser");
        toHitModifier = -1;
        heat = 9;
        damage = 9;
        shortRange = 5;
        mediumRange = 10;
        longRange = 15;
        extremeRange = 20;
        waterShortRange = 3;
        waterMediumRange = 6;
        waterLongRange = 9;
        waterExtremeRange = 12;
        tonnage = 8.0;
        criticals = 5;
        bv = 161;
        cost = 250000;
        shortAV = 9;
        medAV = 9;
        maxRange = RANGE_MED;
        rulesRefs = "89, IO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        techAdvancement.setTechBase(TECH_BASE_IS)
        	.setIntroLevel(false)
        	.setUnofficial(false)
            .setTechRating(RATING_E)
            .setAvailability(RATING_X, RATING_X, RATING_X, RATING_D)
            .setISAdvancement(3120, 3130, DATE_NONE, DATE_NONE, DATE_NONE)
            .setISApproximate(true, false, false,false, false)
            .setPrototypeFactions(F_FS)
            .setProductionFactions(F_FS)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);
    }
}
