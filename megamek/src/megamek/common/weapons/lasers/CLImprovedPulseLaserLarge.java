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
public class CLImprovedPulseLaserLarge extends PulseLaserWeapon {
    private static final long serialVersionUID = 94533476706680275L;

    public CLImprovedPulseLaserLarge() {
        super();
        name = "Improved Large Pulse Laser";
        setInternalName("ImprovedLargePulseLaser");
        addLookupName("Improved Pulse Large Laser");
        addLookupName("ImpLargePulseLaser");
        heat = 10;
        damage = 9;
        toHitModifier = -2;
        shortRange = 3;
        mediumRange = 7;
        longRange = 10;
        extremeRange = 14;
        waterShortRange = 2;
        waterMediumRange = 5;
        waterLongRange = 7;
        waterExtremeRange = 10;
        tonnage = 6.0;
        criticals = 2;
        bv = 119;
        cost = 175000;
        shortAV = 9;
        medAV = 9;
        maxRange = RANGE_MED;
        rulesRefs = "95, IO";
        techAdvancement.setTechBase(TECH_BASE_CLAN).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_E, RATING_X, RATING_X)
                .setClanAdvancement(2815, 2818, 2820, 2826, 3080)
                .setClanApproximate(false, false, false, true, false)
                .setPrototypeFactions(F_CGS).setProductionFactions(F_CGS)
                .setReintroductionFactions(F_EI).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    }
}
