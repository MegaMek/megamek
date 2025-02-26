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
public class ISLBinaryLaserCannon extends LaserWeapon {
    private static final long serialVersionUID = -6849916948609019186L;

    public ISLBinaryLaserCannon() {
        super();
        name = "Light Blazer";
        setInternalName(name);
        addLookupName("ISLightBlazerr");
        heat = 6;
        damage = 7;
        shortRange = 3;
        mediumRange = 6;
        longRange = 9;
        extremeRange = 12;
        waterShortRange = 2;
        waterMediumRange =4;
        waterLongRange = 6;
        waterExtremeRange = 6;
        tonnage = 1.5;
        criticals = 2;
        bv = 148;
        cost = 15000;
        shortAV = 7;
        maxRange = RANGE_SHORT;
        flags = flags.andNot(F_PROTO_WEAPON);
        // Nothing to see here, move along
        rulesRefs = "Unofficial";
        techAdvancement.setTechBase(TECH_BASE_IS)
                .setTechRating(RATING_D)
                .setAvailability(RATING_X, RATING_E, RATING_E, RATING_D)
                .setISAdvancement(2852, DATE_NONE, 3077)
                .setPrototypeFactions(F_FW)
                .setProductionFactions(F_WB).setStaticTechLevel(SimpleTechLevel.UNOFFICIAL);
    }
}
