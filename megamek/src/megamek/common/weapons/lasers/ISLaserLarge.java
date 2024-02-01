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
public class ISLaserLarge extends LaserWeapon {
    private static final long serialVersionUID = 7184746139915905374L;

    public ISLaserLarge() {
        super();
        name = "Large Laser";
        setInternalName(this.name);
        addLookupName("IS Large Laser");
        addLookupName("ISLargeLaser");
        sortingName = "Laser AA D";
        heat = 8;
        damage = 8;
        shortRange = 5;
        mediumRange = 10;
        longRange = 15;
        extremeRange = 20;
        waterShortRange = 3;
        waterMediumRange = 6;
        waterLongRange = 9;
        waterExtremeRange = 12;
        tonnage = 5.0;
        criticals = 2;
        bv = 123;
        cost = 100000;
        shortAV = 8;
        medAV = 8;
        maxRange = RANGE_MED;
        rulesRefs = "227, TM";
        techAdvancement.setTechBase(TECH_BASE_ALL)
                .setIntroLevel(true)
                .setUnofficial(false)
                .setTechRating(RATING_C)
                .setAvailability(RATING_B, RATING_D, RATING_C, RATING_B)
                .setISAdvancement(2306, 2316, 2320, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(2306, 2316, 2320, 2850, DATE_NONE)
                .setClanApproximate(false, false, false, true, false)
                .setPrototypeFactions(F_TA)
                .setProductionFactions(F_TH);
    }
}