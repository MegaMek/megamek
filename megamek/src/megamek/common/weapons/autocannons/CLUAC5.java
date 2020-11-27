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
 * Created on Oct 2, 2004
 *
 */

/**
 * @author Andrew Hunter
 */
public class CLUAC5 extends UACWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = 4371171653960292873L;

    /**
     * 
     */
    public CLUAC5() {
        super();

        this.name = "Ultra AC/5";
        this.setInternalName("CLUltraAC5");
        this.addLookupName("Clan Ultra AC/5");
        this.heat = 1;
        this.damage = 5;
        this.rackSize = 5;
        this.minimumRange = 0;
        this.shortRange = 7;
        this.mediumRange = 14;
        this.longRange = 21;
        this.extremeRange = 28;
        this.tonnage = 7.0;
        this.criticals = 3;
        this.bv = 122;
        this.cost = 200000;
        this.shortAV = 7;
        this.medAV = 7;
        this.longAV = 7;
        this.maxRange = RANGE_LONG;
        this.explosionDamage = damage;
        rulesRefs = "208,TM";
        techAdvancement.setTechBase(TECH_BASE_CLAN)
    	.setIntroLevel(false)
    	.setUnofficial(false)
        .setTechRating(RATING_F)
        .setAvailability(RATING_X, RATING_D, RATING_D, RATING_C)
        .setClanAdvancement(2825, 2827, 2829, DATE_NONE, DATE_NONE)
        .setClanApproximate(true, true, false,false, false)
        .setPrototypeFactions(F_CLAN)
        .setProductionFactions(F_CLAN);
    }
}
