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
 * @since May 29, 2004
 */
public class CLChemicalLaserSmall extends CLChemicalLaserWeapon {
    private static final long serialVersionUID = 322396740172378519L;

    public CLChemicalLaserSmall() {
        name = "Small Chemical Laser";
        setInternalName("CLSmallChemicalLaser");
        addLookupName("CLSmallChemLaser");
        addLookupName("Small Chem Laser");
        sortingName = "Chem Laser B";
        heat = 1;
        rackSize = 3;
        damage = 3;
        minimumRange = WEAPON_NA;
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
        bv = 7;
        cost = 10000;
        shortAV = 3;
        maxRange = RANGE_SHORT;
        atClass = CLASS_LASER;
        rulesRefs = "320, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        techAdvancement.setTechBase(TECH_BASE_CLAN)
                .setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_E)
                .setClanAdvancement(DATE_NONE, 3059, 3083, DATE_NONE, DATE_NONE)
                .setPrototypeFactions(F_CHH)
                .setProductionFactions(F_CHH)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
    }
}
