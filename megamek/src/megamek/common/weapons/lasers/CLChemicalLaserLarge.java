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
 * Created on May 29, 2004
 *
 */
package megamek.common.weapons.lasers;

import megamek.common.SimpleTechLevel;

/**
 * @author Jason Tighe
 */
public class CLChemicalLaserLarge extends CLChemicalLaserWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 322396740172378519L;

    public CLChemicalLaserLarge() {
        name = "Large Chem Laser";
        setInternalName("CLLargeChemicalLaser");
        setInternalName("CLLargeChemLaser");
        heat = 6;
        damage = 8;
        rackSize = 1;
        shortRange = 5;
        mediumRange = 10;
        longRange = 15;
        extremeRange = 20;
        waterShortRange = 3;
        waterMediumRange = 6;
        waterLongRange = 9;
        waterExtremeRange = 12;
        tonnage = 5.0;
        criticals = 2;
        bv = 99;
        cost = 75000;
        shortAV = 8;
        medAV = 8;
        maxRange = RANGE_MED;
        rulesRefs = "320,TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        techAdvancement.setTechBase(TECH_BASE_CLAN)
        	.setTechRating(RATING_E)
            .setAvailability(RATING_X, RATING_X, RATING_E, RATING_E)
            .setClanAdvancement(DATE_NONE, 3059, 3083, DATE_NONE, DATE_NONE)
            .setPrototypeFactions(F_CHH)
            .setProductionFactions(F_CHH)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);
    }
}
