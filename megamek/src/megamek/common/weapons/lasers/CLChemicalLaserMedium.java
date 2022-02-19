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
public class CLChemicalLaserMedium extends CLChemicalLaserWeapon {
    private static final long serialVersionUID = 322396740172378519L;

    public CLChemicalLaserMedium() {
        name = "Medium Chemical Laser";
        setInternalName("CLMediumChemicalLaser");
        addLookupName("CLMediumChemLaser");
        addLookupName("Medium Chem Laser");
        sortingName = "Chem Laser C";
        heat = 2;
        damage = 5;
        rackSize = 5;
        shortRange = 3;
        mediumRange = 6;
        longRange = 9;
        extremeRange = 12;
        waterShortRange = 2;
        waterMediumRange = 4;
        waterLongRange = 6;
        waterExtremeRange = 8;
        tonnage = 1.0;
        criticals = 1;
        bv = 37;
        cost = 30000;
        shortAV = 5;
        maxRange = RANGE_SHORT;
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
