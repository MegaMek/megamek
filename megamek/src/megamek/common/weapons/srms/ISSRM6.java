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
package megamek.common.weapons.srms;

/**
 * @author Sebastian Brocks
 */
public class ISSRM6 extends SRMWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = -2774209761562289905L;

    /**
     * 
     */
    public ISSRM6() {
        super();
        this.name = "SRM 6";
        this.setInternalName(this.name);
        this.addLookupName("IS SRM-6");
        this.addLookupName("ISSRM6");
        this.addLookupName("IS SRM 6");
        this.heat = 4;
        this.rackSize = 6;
        this.shortRange = 3;
        this.mediumRange = 6;
        this.longRange = 9;
        this.extremeRange = 12;
        this.tonnage = 3.0;
        this.criticals = 2;
        this.bv = 59;
        this.cost = 80000;
        this.shortAV = 8;
        this.maxRange = RANGE_SHORT;
        rulesRefs = "229, TM";
        flags = flags.andNot(F_PROTO_WEAPON);
        techAdvancement.setTechBase(TECH_BASE_ALL)
        	.setIntroLevel(true)
        	.setUnofficial(false)
            .setTechRating(RATING_C)
            .setAvailability(RATING_C, RATING_C, RATING_C, RATING_C)
            .setISAdvancement(2365, 2370, 2400, DATE_NONE, DATE_NONE)
            .setISApproximate(false, false, false,false, false)
            .setClanAdvancement(2365, 2370, 2400, 2836, DATE_NONE)
            .setClanApproximate(false, false, false, false, false)
            .setPrototypeFactions(F_TH)
            .setProductionFactions(F_TH);
    }
}
