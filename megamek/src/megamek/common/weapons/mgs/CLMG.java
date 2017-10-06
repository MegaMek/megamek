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
 * Created on Oct 20, 2004
 *
 */
package megamek.common.weapons.mgs;

import megamek.common.WeaponType;

/**
 * @author Andrew Hunter
 */
public class CLMG extends MGWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = 2557643305248678454L;

    /**
     * 
     */
    public CLMG() {
        super();

        this.name = "Machine Gun";
        this.setInternalName("CLMG");
        this.addLookupName("Clan Machine Gun");
        this.heat = 0;
        this.damage = 2;
        this.infDamageClass = WeaponType.WEAPON_BURST_2D6;
        this.rackSize = 2;
        this.shortRange = 1;
        this.mediumRange = 2;
        this.longRange = 3;
        this.extremeRange = 4;
        this.tonnage = 0.25f;
        this.criticals = 1;
        this.bv = 5;
        this.cost = 5000;
        this.shortAV = 2;
        this.maxRange = RANGE_SHORT;
        this.atClass = CLASS_POINT_DEFENSE;
        rulesRefs = "228,TM";
        techAdvancement.setTechBase(TECH_BASE_CLAN)
    	.setIntroLevel(false)
    	.setUnofficial(false)
        .setTechRating(RATING_C)
        .setAvailability(RATING_X, RATING_B, RATING_B, RATING_A )
        .setClanAdvancement(2821, 2825, 2830, DATE_NONE, DATE_NONE)
        .setClanApproximate(true, false, false,false, false)
        .setPrototypeFactions(F_CSF)
        .setProductionFactions(F_CSF);
    }

}
