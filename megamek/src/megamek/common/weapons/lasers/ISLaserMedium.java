/*
 * MegaMek - Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
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
 * @since Sep 2, 2004
 */
public class ISLaserMedium extends LaserWeapon {
    private static final long serialVersionUID = 2178224725694704541L;

    public ISLaserMedium() {
        super();
        name = "Medium Laser";
        setInternalName(this.name);
        addLookupName("IS Medium Laser");
        addLookupName("ISMediumLaser");
        sortingName = "Laser C";
        heat = 3;
        damage = 5;
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
        bv = 46;
        cost = 40000;
        shortAV = 5;
        maxRange = RANGE_SHORT;
        rulesRefs = "227, TM";
        techAdvancement.setTechBase(TECH_BASE_ALL)
                .setIntroLevel(true)
                .setUnofficial(false)
                .setTechRating(RATING_C)
                .setAvailability(RATING_B, RATING_B, RATING_B, RATING_B)
                .setISAdvancement(2290, 2300, 2310, DATE_NONE, DATE_NONE)
                .setISApproximate(false, true, false, false, false)
                .setClanAdvancement(2290, 2300, 2310, 2850, DATE_NONE)
                .setClanApproximate(false, true, false, true, false)
                .setPrototypeFactions(F_TA)
                .setProductionFactions(F_TA)
                .setExtinctionFactions(F_CLAN);
    }
}