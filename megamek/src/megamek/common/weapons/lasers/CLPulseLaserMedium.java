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
import megamek.common.weapons.PulseLaserWeapon;

/**
 * @author Andrew Hunter
 */
public class CLPulseLaserMedium extends PulseLaserWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = -5538336797804604495L;

    /**
     * 
     */
    public CLPulseLaserMedium() {
        super();
        this.name = "Medium Pulse Laser";
        this.setInternalName("CLMediumPulseLaser");
        this.addLookupName("Clan Pulse Med Laser");
        this.addLookupName("Clan Medium Pulse Laser");
        this.heat = 4;
        this.damage = 7;
        this.toHitModifier = -2;
        this.shortRange = 4;
        this.mediumRange = 8;
        this.longRange = 12;
        this.extremeRange = 16;
        this.waterShortRange = 3;
        this.waterMediumRange = 5;
        this.waterLongRange = 8;
        this.waterExtremeRange = 10;
        this.tonnage = 2.0f;
        this.criticals = 1;
        this.bv = 111;
        this.cost = 60000;
        this.shortAV = 7;
        this.medAV = 7;
        this.maxRange = RANGE_MED;
        rulesRefs = "226,TM";
        techAdvancement.setTechBase(TECH_BASE_CLAN)
        	.setIntroLevel(false)
        	.setUnofficial(false)
            .setTechRating(RATING_F)
            .setAvailability(RATING_X, RATING_D, RATING_C, RATING_C)
            .setClanAdvancement(2825, 2827, 2831, DATE_NONE, DATE_NONE)
            .setClanApproximate(true, false, false,false, false)
            .setPrototypeFactions(F_CJF,F_CGB)
            .setProductionFactions(F_CJF);
    }
}
