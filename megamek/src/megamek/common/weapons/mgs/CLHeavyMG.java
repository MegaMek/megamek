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

import megamek.common.AmmoType;
import megamek.common.TechAdvancement;
import megamek.common.WeaponType;
import megamek.common.weapons.MGWeapon;

/**
 * @author Andrew Hunter
 */
public class CLHeavyMG extends MGWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = -3031880020233816652L;

    /**
     * 
     */
    public CLHeavyMG() {
        super();

        this.name = "Heavy Machine Gun";
        this.setInternalName("CLHeavyMG");
        this.addLookupName("Clan Heavy Machine Gun");
        this.heat = 0;
        this.damage = 3;
        this.infDamageClass = WeaponType.WEAPON_BURST_3D6;
        this.rackSize = 3;
        this.ammoType = AmmoType.T_MG_HEAVY;
        this.shortRange = 1;
        this.mediumRange = 2;
        this.longRange = 2;
        this.extremeRange = 2;
        this.tonnage = 0.5f;
        this.criticals = 1;
        this.bv = 6;
        this.cost = 7500;
        this.shortAV = 3;
        this.maxRange = RANGE_SHORT;
        rulesRefs = "228,TM";
        techAdvancement.setTechBase(TECH_BASE_CLAN)
        	.setIntroLevel(false)
        	.setUnofficial(false)
            .setTechRating(RATING_C)
            .setAvailability(RATING_X, RATING_C, RATING_C, RATING_B)
            .setClanAdvancement(3054, 3059, 3070, DATE_NONE, DATE_NONE)
            .setClanApproximate(true, false, false,false, false)
            .setPrototypeFactions(F_CHH)
            .setProductionFactions(F_CSJ);
    }

}
