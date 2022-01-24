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
 * @author Jason Tighe
 * @since Sep 12, 2004
 */
public class CLImprovedHeavyLaserLarge extends LaserWeapon {
    private static final long serialVersionUID = 4467522144065588079L;

    public CLImprovedHeavyLaserLarge() {
        super();
        name = "Improved Heavy Large Laser";
        shortName = "Imp. Heavy Large Laser";
        setInternalName("CLImprovedHeavyLargeLaser");
        addLookupName("Clan Improved Large Heavy Laser");
        sortingName = "Laser Heavy Imp D";
        heat = 18;
        damage = 16;
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
        bv = 296;
        cost = 350000;
        shortAV = 16;
        medAV = 16;
        maxRange = RANGE_MED;
        explosionDamage = 8;
        explosive = true;
        rulesRefs = "321, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        techAdvancement.setTechBase(TECH_BASE_CLAN).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_D, RATING_D)
                .setClanAdvancement(DATE_NONE, 3069, 3085, DATE_NONE, DATE_NONE)
                .setPrototypeFactions(F_CGS)
                .setProductionFactions(F_RD)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
    }
}
