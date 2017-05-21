/**
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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
 * Created on Sep 8, 2005
 *
 */
package megamek.common.weapons.lasers;

import megamek.common.weapons.PulseLaserWeapon;

/**
 * @author Sebastian Brocks
 */
public class ISXPulseLaserMedium extends PulseLaserWeapon {
    /**
     *
     */
    private static final long serialVersionUID = -6576828912486084151L;

    /**
     *
     */
    public ISXPulseLaserMedium() {
        super();
        name = "Medium X-Pulse Laser";
        setInternalName("ISMediumXPulseLaser");
        addLookupName("IS X-Pulse Med Laser");
        addLookupName("IS Medium X-Pulse Laser");
        heat = 6;
        damage = 6;
        toHitModifier = -2;
        shortRange = 3;
        mediumRange = 6;
        longRange = 9;
        extremeRange = 12;
        waterShortRange = 2;
        waterMediumRange = 4;
        waterLongRange = 6;
        waterExtremeRange = 8;
        maxRange = RANGE_SHORT;
        shortAV = 6;
        tonnage = 2.0f;
        criticals = 1;
        bv = 71;
        cost = 110000;
        rulesRefs = "321,TO";
        techAdvancement.setTechBase(TECH_BASE_IS)
        	.setIntroLevel(false)
        	.setUnofficial(false)
            .setTechRating(RATING_E)
            .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
            .setISAdvancement(3057, 3078, 3082, DATE_NONE, DATE_NONE)
            .setISApproximate(false, false, false,false, false)
            .setPrototypeFactions(F_LC,F_FS)
            .setProductionFactions(F_LC);
    }
}
