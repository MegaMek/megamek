/*
 * MegaMek - Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.common.weapons.lasers;

import megamek.common.SimpleTechLevel;

/**
 * @author Andrew Hunter
 * @since Sep 12, 2004
 */
public class CLHeavyLaserLarge extends LaserWeapon {
    private static final long serialVersionUID = 4467522144065588079L;

    public CLHeavyLaserLarge() {
        super();
        name = "Heavy Large Laser";
        setInternalName("CLHeavyLargeLaser");
        addLookupName("Clan Large Heavy Laser");
        sortingName = "Laser Heavy D";
        heat = 18;
        damage = 16;
        toHitModifier = 1;
        shortRange = 5;
        mediumRange = 10;
        longRange = 15;
        extremeRange = 20;
        waterShortRange = 3;
        waterMediumRange = 6;
        waterLongRange = 9;
        waterExtremeRange = 12;
        tonnage = 4.0;
        criticals = 3;
        bv = 244;
        cost = 250000;
        shortAV = 16;
        medAV = 16;
        maxRange = RANGE_MED;
        rulesRefs = "226, TM";
        //Jan 22 - Errata issued by CGL (Greekfire) for Heavy Lasers
        techAdvancement.setTechBase(TECH_BASE_CLAN)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_D, RATING_D)
                .setClanAdvancement(3057, 3058, 3059, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false)
                .setPrototypeFactions(F_CBR, F_CSA)
                .setProductionFactions(F_CSA)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);

    }
}
