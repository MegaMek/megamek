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
public class CLERLaserLarge extends LaserWeapon {
    private static final long serialVersionUID = -3799823521215134292L;

    public CLERLaserLarge() {
        super();
        name = "ER Large Laser";
        setInternalName("CLERLargeLaser");
        addLookupName("Clan ER Large Laser");
        sortingName = "Laser ER D";
        heat = 12;
        damage = 10;
        shortRange = 8;
        mediumRange = 15;
        longRange = 25;
        extremeRange = 30;
        waterShortRange = 5;
        waterMediumRange = 10;
        waterLongRange = 16;
        waterExtremeRange = 20;
        tonnage = 4.0;
        criticals = 1;
        bv = 248;
        cost = 200000;
        shortAV = 10;
        medAV = 10;
        longAV = 10;
        extAV = 10;
        maxRange = RANGE_EXT;
        rulesRefs = "226, TM";
        techAdvancement.setTechBase(TECH_BASE_CLAN)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_F, RATING_D, RATING_C)
                .setClanAdvancement(2820, 2825, 2830, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false)
                .setPrototypeFactions(F_CNC)
                .setProductionFactions(F_CNC);
    }
}
