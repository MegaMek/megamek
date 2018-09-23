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

import megamek.common.WeaponType;

/**
 * @author Andrew Hunter
 */
public class ISPulseLaserSmall extends PulseLaserWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = 2977404162226570144L;

    /**
     * 
     */
    public ISPulseLaserSmall() {
        super();
        this.name = "Small Pulse Laser";
        this.setInternalName("ISSmallPulseLaser");
        this.addLookupName("IS Small Pulse Laser");
        this.addLookupName("ISSmall Pulse Laser");
        this.heat = 2;
        this.damage = 3;
        this.infDamageClass = WeaponType.WEAPON_BURST_2D6;
        this.toHitModifier = -2;
        this.shortRange = 1;
        this.mediumRange = 2;
        this.longRange = 3;
        this.extremeRange = 4;
        this.waterShortRange = 1;
        this.waterMediumRange = 2;
        this.waterLongRange = 2;
        this.waterExtremeRange = 4;
        this.tonnage = 1.0;
        this.criticals = 1;
        this.bv = 12;
        this.cost = 16000;
        this.shortAV = 3;
        this.maxRange = RANGE_SHORT;
        this.atClass = CLASS_POINT_DEFENSE;
        this.flags = flags.or(F_BURST_FIRE);
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
