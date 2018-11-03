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
public class CLPulseLaserSmall extends PulseLaserWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = -3257397139779601796L;

    /**
     * 
     */
    public CLPulseLaserSmall() {
        super();
        this.name = "Small Pulse Laser";
        this.setInternalName("CLSmallPulseLaser");
        this.addLookupName("Clan Pulse Small Laser");
        this.addLookupName("Clan Small Pulse Laser");
        this.heat = 2;
        this.damage = 3;
        this.infDamageClass = WeaponType.WEAPON_BURST_2D6;
        this.toHitModifier = -2;
        this.shortRange = 2;
        this.mediumRange = 4;
        this.longRange = 6;
        this.extremeRange = 8;
        this.waterShortRange = 1;
        this.waterMediumRange = 2;
        this.waterLongRange = 4;
        this.waterExtremeRange = 4;
        this.tonnage = 1.0;
        this.criticals = 1;
        this.bv = 24;
        this.cost = 16000;
        this.shortAV = 3;
        this.maxRange = RANGE_SHORT;
        this.flags = flags.or(F_BURST_FIRE);
        rulesRefs = "226,TM";
        techAdvancement.setTechBase(TECH_BASE_CLAN)
        	.setIntroLevel(false)
        	.setUnofficial(false)
            .setTechRating(RATING_F)
            .setAvailability(RATING_X, RATING_D, RATING_C, RATING_C)
            .setClanAdvancement(2825, 2829, 2831, DATE_NONE, DATE_NONE)
            .setClanApproximate(true, false, false,false, false)
            .setPrototypeFactions(F_CJF,F_CGB)
            .setProductionFactions(F_CGB);
    }

}
