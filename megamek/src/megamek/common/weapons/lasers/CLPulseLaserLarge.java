package megamek.common.weapons.lasers;

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

/**
 * @author Andrew Hunter
 */
public class CLPulseLaserLarge extends PulseLaserWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = 608317914802476438L;

    /**
     * 
     */
    public CLPulseLaserLarge() {
        super();
        this.name = "Large Pulse Laser";
        this.setInternalName("CLLargePulseLaser");
        this.addLookupName("Clan Pulse Large Laser");
        this.addLookupName("Clan Large Pulse Laser");
        this.heat = 10;
        this.damage = 10;
        this.toHitModifier = -2;
        this.shortRange = 6;
        this.mediumRange = 14;
        this.longRange = 20;
        this.extremeRange = 28;
        this.waterShortRange = 4;
        this.waterMediumRange = 10;
        this.waterLongRange = 14;
        this.waterExtremeRange = 20;
        this.tonnage = 6.0f;
        this.criticals = 2;
        this.bv = 265;
        this.cost = 175000;
        this.shortAV = 10;
        this.medAV = 10;
        this.longAV = 10;
        this.maxRange = RANGE_LONG;
        rulesRefs = "226,TM";
        techAdvancement.setTechBase(TECH_BASE_CLAN)
        	.setIntroLevel(false)
        	.setUnofficial(false)
            .setTechRating(RATING_F)
            .setAvailability(RATING_X, RATING_D, RATING_C, RATING_C)
            .setClanAdvancement(2820, 2824, 2831, DATE_NONE, DATE_NONE)
            .setClanApproximate(false, false, false,false, false)
            .setPrototypeFactions(F_CCY)
            .setProductionFactions(F_CCY);
    }
}
