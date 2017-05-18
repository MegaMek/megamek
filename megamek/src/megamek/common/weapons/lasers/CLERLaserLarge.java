/**
 * MegaMek - Copyright (C) 2004,2005 Ben Mazur (bmazur@sev.org)
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
 * Created on Sep 12, 2004
 *
 */
package megamek.common.weapons.lasers;

import megamek.common.TechAdvancement;
import megamek.common.weapons.LaserWeapon;

/**
 * @author Andrew Hunter
 */
public class CLERLaserLarge extends LaserWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = -3799823521215134292L;

    /**
     * 
     */
    public CLERLaserLarge() {
        super();
        this.name = "ER Large Laser";
        this.setInternalName("CLERLargeLaser");
        this.addLookupName("Clan ER Large Laser");
        this.heat = 12;
        this.damage = 10;
        this.shortRange = 8;
        this.mediumRange = 15;
        this.longRange = 25;
        this.extremeRange = 30;
        this.waterShortRange = 5;
        this.waterMediumRange = 10;
        this.waterLongRange = 16;
        this.waterExtremeRange = 20;
        this.tonnage = 4.0f;
        this.criticals = 1;
        this.bv = 248;
        this.cost = 200000;
        this.shortAV = 10;
        this.medAV = 10;
        this.longAV = 10;
        this.extAV = 10;
        this.maxRange = RANGE_EXT;
        rulesRefs = "2226,TM";
        techAdvancement.setTechBase(TECH_BASE_CLAN)
        	.setIntroLevel(false)
        	.setUnofficial(false)
            .setTechRating(RATING_F)
            .setAvailability(RATING_X, RATING_F, RATING_D, RATING_C)
            .setClanAdvancement(2820, 2825, 2830, DATE_NONE, DATE_NONE)
            .setClanApproximate(true, false, false,false, false)
            .setPrototypeFactions(F_CNC)
            .setProductionFactions(F_CNC);
    }
}
