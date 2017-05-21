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

import megamek.common.weapons.PulseLaserWeapon;

/**
 * @author Andrew Hunter
 */
public class ISPulseLaserMedium extends PulseLaserWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = 2676144961105838316L;

    /**
     * 
     */
    public ISPulseLaserMedium() {
        super();
        this.name = "Medium Pulse Laser";
        this.setInternalName("ISMediumPulseLaser");
        this.addLookupName("IS Pulse Med Laser");
        this.addLookupName("IS Medium Pulse Laser");
        this.heat = 4;
        this.damage = 6;
        this.toHitModifier = -2;
        this.shortRange = 2;
        this.mediumRange = 4;
        this.longRange = 6;
        this.extremeRange = 8;
        this.waterShortRange = 2;
        this.waterMediumRange = 3;
        this.waterLongRange = 4;
        this.waterExtremeRange = 6;
        this.tonnage = 2.0f;
        this.criticals = 1;
        this.bv = 48;
        this.cost = 60000;
        this.shortAV = 6;
        this.maxRange = RANGE_SHORT;
        rulesRefs = "226,TM";
        techAdvancement.setTechBase(TECH_BASE_IS)
        	.setIntroLevel(false)
        	.setUnofficial(false)
            .setTechRating(RATING_E)
            .setAvailability(RATING_E, RATING_F, RATING_D, RATING_C)
            .setISAdvancement(2595, 2609, 3042, 2950, 3037)
            .setISApproximate(false, false, false,false, false)
            .setPrototypeFactions(F_TH)
            .setProductionFactions(F_TH)
            .setReintroductionFactions(F_DC);
    }
}
