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
public class CLImprovedHeavyLaserMedium extends LaserWeapon {
    private static final long serialVersionUID = 4467522144065588079L;

    public CLImprovedHeavyLaserMedium() {
        super();
        name = "Improved Heavy Medium Laser";
        shortName = "Imp. Heavy Medium Laser";
        setInternalName("CLImprovedMediumHeavyLaser");
        addLookupName("Clan Improved Heavy Medium Laser");
        addLookupName("CLImprovedHeavyMediumLaser");
        sortingName = "Laser Heavy Imp C";
        heat = 7;
        damage = 10;
        shortRange = 3;
        mediumRange = 6;
        longRange = 9;
        extremeRange = 12;
        waterShortRange = 2;
        waterMediumRange = 4;
        waterLongRange = 6;
        waterExtremeRange = 8;
        tonnage = 1.0;
        criticals = 2;
        bv = 93;
        cost = 150000;
        shortAV = 10;
        maxRange = RANGE_SHORT;
        explosionDamage = 5;
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
