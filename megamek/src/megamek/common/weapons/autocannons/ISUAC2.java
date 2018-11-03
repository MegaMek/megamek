package megamek.common.weapons.autocannons;

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
 * Created on Oct 1, 2004
 *
 */

/**
 * @author Andrew Hunter
 */
public class ISUAC2 extends UACWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = -6894947564166021652L;

    /**
     * 
     */
    public ISUAC2() {
        super();
        this.name = "Ultra AC/2";
        this.setInternalName("ISUltraAC2");
        this.addLookupName("IS Ultra AC/2");
        this.heat = 1;
        this.damage = 2;
        this.rackSize = 2;
        this.minimumRange = 3;
        this.shortRange = 8;
        this.mediumRange = 17;
        this.longRange = 25;
        this.extremeRange = 34;
        this.tonnage = 7.0;
        this.criticals = 3;
        this.bv = 56;
        this.cost = 120000;
        this.shortAV = 3;
        this.medAV = 3;
        this.longAV = 3;
        this.extAV = 3;
        this.maxRange = RANGE_EXT;
        this.explosionDamage = damage;
        rulesRefs = "208,TM";
        techAdvancement.setTechBase(TECH_BASE_IS)
        	.setIntroLevel(false)
        	.setUnofficial(false)
            .setTechRating(RATING_E)
            .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
            .setISAdvancement(3055, 3057, 3060, DATE_NONE, DATE_NONE)
            .setISApproximate(true, false, false,false, false)
            .setPrototypeFactions(F_FW)
            .setProductionFactions(F_FW);
    }
}
