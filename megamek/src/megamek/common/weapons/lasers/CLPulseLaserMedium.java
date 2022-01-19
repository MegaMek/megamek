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

/**
 * @author Andrew Hunter
 * @since Sep 12, 2004
 */
public class CLPulseLaserMedium extends PulseLaserWeapon {
    private static final long serialVersionUID = -5538336797804604495L;

    public CLPulseLaserMedium() {
        super();
        name = "Medium Pulse Laser";
        setInternalName("CLMediumPulseLaser");
        addLookupName("Clan Pulse Med Laser");
        addLookupName("Clan Medium Pulse Laser");
        sortingName = "Laser Pulse C";
        heat = 4;
        damage = 7;
        toHitModifier = -2;
        shortRange = 4;
        mediumRange = 8;
        longRange = 12;
        extremeRange = 16;
        waterShortRange = 3;
        waterMediumRange = 5;
        waterLongRange = 8;
        waterExtremeRange = 10;
        tonnage = 2.0;
        criticals = 1;
        bv = 111;
        cost = 60000;
        shortAV = 7;
        medAV = 7;
        maxRange = RANGE_MED;
        rulesRefs = "226, TM";
        techAdvancement.setTechBase(TECH_BASE_CLAN)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_D, RATING_C, RATING_C)
                .setClanAdvancement(2825, 2827, 2831, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false)
                .setPrototypeFactions(F_CJF,F_CGB)
                .setProductionFactions(F_CJF);
    }
}
