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
public class CLERLaserSmall extends LaserWeapon {
    private static final long serialVersionUID = -4120464315724174929L;

    public CLERLaserSmall() {
        super();
        name = "ER Small Laser";
        setInternalName("CLERSmallLaser");
        addLookupName("Clan ER Small Laser");
        sortingName = "Laser ER B";
        heat = 2;
        damage = 5;
        shortRange = 2;
        mediumRange = 4;
        longRange = 6;
        extremeRange = 8;
        waterShortRange = 1;
        waterMediumRange = 2;
        waterLongRange = 4;
        waterExtremeRange = 4;
        tonnage = 0.5;
        criticals = 1;
        flags = flags.or(F_NO_FIRES);
        bv = 31;
        cost = 11250;
        shortAV = 5;
        maxRange = RANGE_SHORT;
        rulesRefs = "226, TM";
        techAdvancement.setTechBase(TECH_BASE_CLAN)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_E, RATING_D, RATING_C)
                .setClanAdvancement(2822, 2825, 2830, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false)
                .setPrototypeFactions(F_CJF)
                .setProductionFactions(F_CJF);
    }
    
    @Override
    public boolean isAlphaStrikePointDefense() {
        return true;
    }
}
