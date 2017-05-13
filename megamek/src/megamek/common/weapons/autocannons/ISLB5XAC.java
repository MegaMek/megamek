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
 * Created on Oct 15, 2004
 *
 */
package megamek.common.weapons.autocannons;

import megamek.common.TechAdvancement;
import megamek.common.weapons.LBXACWeapon;

/**
 * @author Andrew Hunter
 */
public class ISLB5XAC extends LBXACWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = 3466212961123086341L;

    /**
     * 
     */
    public ISLB5XAC() {
        super();
        this.name = "LB 5-X AC";
        this.setInternalName("ISLBXAC5");
        this.addLookupName("IS LB 5-X AC");
        this.heat = 1;
        this.damage = 5;
        this.rackSize = 5;
        this.minimumRange = 3;
        this.shortRange = 7;
        this.mediumRange = 14;
        this.longRange = 21;
        this.extremeRange = 28;
        this.tonnage = 8.0f;
        this.criticals = 5;
        this.bv = 83;
        this.cost = 250000;
        this.shortAV = 5;
        this.medAV = 5;
        this.longAV = 5;
        this.maxRange = RANGE_LONG;
        rulesRefs = "207,TM";
        techAdvancement.setTechBase(TECH_BASE_IS)
        	.setIntroLevel(false)
        	.setUnofficial(false)
            .setTechRating(RATING_E)
            .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
            .setISAdvancement(3055, 3058, 3060, DATE_NONE, DATE_NONE)
            .setISApproximate(true, false, false,false, false)
            .setPrototypeFactions(F_FS)
            .setProductionFactions(F_FS);
    }
}
