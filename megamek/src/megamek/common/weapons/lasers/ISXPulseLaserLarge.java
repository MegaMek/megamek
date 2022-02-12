/*
 * Copyright (c) 2005 - Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.weapons.lasers;

import megamek.common.SimpleTechLevel;

/**
 * @author Sebastian Brocks
 * @since Sep 8, 2005
 */
public class ISXPulseLaserLarge extends PulseLaserWeapon {
    private static final long serialVersionUID = -8159582350685114767L;

    public ISXPulseLaserLarge() {
        super();
        name = "Large X-Pulse Laser";
        setInternalName("ISLargeXPulseLaser");
        addLookupName("IS X-Pulse Large Laser");
        addLookupName("IS Large X-Pulse Laser");
        sortingName = "Laser XPULSE D";
        heat = 14;
        damage = 9;
        toHitModifier = -2;
        shortRange = 5;
        mediumRange = 10;
        longRange = 15;
        extremeRange = 20;
        waterShortRange = 3;
        waterMediumRange = 6;
        waterLongRange = 9;
        waterExtremeRange = 12;
        tonnage = 7.0;
        criticals = 2;
        bv = 178;
        cost = 275000;
        maxRange = RANGE_MED;
        shortAV = 9;
        medAV = 9;
        rulesRefs = "321, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
            .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
            .setISAdvancement(DATE_NONE, 3047, 3078, DATE_NONE, DATE_NONE).setPrototypeFactions(F_LC,F_FS)
            .setProductionFactions(F_LC)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);
    }
}
