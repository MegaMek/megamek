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
public class CLImprovedHeavyLaserSmall extends LaserWeapon {
    private static final long serialVersionUID = 4467522144065588079L;

    public CLImprovedHeavyLaserSmall() {
        super();
        name = "Improved Heavy Small Laser";
        shortName = "Imp. Heavy Small Laser";
        setInternalName("CLImprovedSmallHeavyLaser");
        addLookupName("CLImprovedHeavySmallLaser");
        addLookupName("Clan Improved Small Heavy Laser");
        sortingName = "Laser Heavy Imp B";
        heat = 3;
        damage = 6;
        shortRange = 1;
        mediumRange = 2;
        longRange = 3;
        extremeRange = 4;
        waterShortRange = 1;
        waterMediumRange = 2;
        waterLongRange = 2;
        waterExtremeRange = 2;
        tonnage = 0.5;
        criticals = 1;
        bv = 19;
        cost = 30000;
        shortAV = 6;
        maxRange = RANGE_SHORT;
        explosionDamage = 3;
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
