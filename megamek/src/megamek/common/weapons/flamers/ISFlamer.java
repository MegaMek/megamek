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
 * Created on Sep 24, 2004
 *
 */
package megamek.common.weapons.flamers;

import megamek.common.TechAdvancement;
import megamek.common.WeaponType;

/**
 * @author Andrew Hunter
 */
public class ISFlamer extends FlamerWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = 1414639280093120062L;

    /**
     * 
     */
    public ISFlamer() {
        super();
        this.name = "Flamer";
        this.setInternalName(this.name);
        this.addLookupName("IS Flamer");
        this.addLookupName("ISFlamer");
        this.heat = 3;
        this.damage = 2;
        this.infDamageClass = WeaponType.WEAPON_BURST_4D6;
        this.shortRange = 1;
        this.mediumRange = 2;
        this.longRange = 3;
        this.extremeRange = 4;
        this.tonnage = 1f;
        this.criticals = 1;
        this.bv = 6;
        this.cost = 7500;
        this.shortAV = 2;
        this.maxRange = RANGE_SHORT;
        rulesRefs = "218,TM";
        techAdvancement.setTechBase(TECH_BASE_ALL)
        	.setIntroLevel(true)
        	.setUnofficial(false)
            .setTechRating(RATING_C)
            .setAvailability(RATING_B, RATING_B, RATING_B, RATING_A)
            .setISAdvancement(DATE_ES, DATE_ES, DATE_ES, DATE_NONE, DATE_NONE)
            .setISApproximate(false, false, false,false, false)
            .setClanAdvancement(DATE_ES, DATE_ES, DATE_ES, 2830, DATE_NONE)
            .setClanApproximate(false, false, false,false, false);
    }
}
