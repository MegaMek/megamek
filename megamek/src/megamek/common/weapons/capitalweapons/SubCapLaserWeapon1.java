/* MegaMek - Copyright (C) 2004,2005 Ben Mazur (bmazur@sev.org)
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
 * Created on Sep 25, 2004
 *
 */
package megamek.common.weapons.capitalweapons;

import megamek.common.SimpleTechLevel;

/**
 * @author Jay Lawson
 */
public class SubCapLaserWeapon1 extends SubCapLaserWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = 8756042527483383101L;

    /**
     * 
     */
    public SubCapLaserWeapon1() {
        super();
        this.name = "Sub-Capital Laser (SCL/1)";
        this.setInternalName(this.name);
        this.addLookupName("SCL1");
        this.addLookupName("Sub-Capital Laser 1");
        this.shortName = "SCL/1";
        this.heat = 24;
        this.damage = 1;
        this.shortRange = 11;
        this.mediumRange = 22;
        this.longRange = 33;
        this.extremeRange = 44;
        this.tonnage = 150.0;
        this.bv = 237;
        this.cost = 220000;
        this.shortAV = 1;
        this.medAV = 1;
        this.longAV = 1;
        this.maxRange = RANGE_LONG;
        rulesRefs = "343,TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        techAdvancement.setTechBase(TECH_BASE_ALL)
	    	.setIntroLevel(false)
	    	.setUnofficial(false)
	        .setTechRating(RATING_E)
	        .setAvailability(RATING_X, RATING_X, RATING_F, RATING_D)
	        .setISAdvancement(DATE_NONE, 3068, 3073, DATE_NONE, DATE_NONE)
            .setISApproximate(false, true, false,false, false)
	        .setClanAdvancement(DATE_NONE, 3090, 3091, DATE_NONE, DATE_NONE)
	        .setClanApproximate(false, true, false,false, false)
	        .setPrototypeFactions(F_WB)
	        .setProductionFactions(F_WB)
	        .setStaticTechLevel(SimpleTechLevel.STANDARD);
    }
}
