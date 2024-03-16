/**
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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
package megamek.common.weapons.missiles;

/**
 * @author Sebastian Brocks
 */
public class ISMRM30 extends MRMWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = -4212839099494076415L;

    /**
     * 
     */
    public ISMRM30() {
        super();

        this.name = "MRM 30";
        this.setInternalName(this.name);
        this.addLookupName("MRM-30");
        this.addLookupName("ISMRM30");
        this.addLookupName("IS MRM 30");
        this.heat = 10;
        this.rackSize = 30;
        this.shortRange = 3;
        this.mediumRange = 8;
        this.longRange = 15;
        this.extremeRange = 16;
        this.tonnage = 10.0;
        this.criticals = 5;
        this.bv = 168;
        this.cost = 225000;
        this.shortAV = 18;
        this.medAV = 18;
        this.maxRange = RANGE_MED;
        rulesRefs = "229, TM";
        techAdvancement.setTechBase(TECH_BASE_IS)
    	.setIntroLevel(false)
    	.setUnofficial(false)
        .setTechRating(RATING_C)
        .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
        .setISAdvancement(3052, 3058, 3063, DATE_NONE, DATE_NONE)
        .setISApproximate(true, false, false,false, false)
        .setPrototypeFactions(F_DC)
        .setProductionFactions(F_DC);
    }
}
