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
package megamek.common.weapons.lrms;

/**
 * @author Sebastian Brocks
 */
public class ISLRT20 extends LRTWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = 2708046269665179590L;

    /**
     * 
     */
    public ISLRT20() {
        super();

        this.name = "LRT 20";
        this.setInternalName(this.name);
        this.addLookupName("IS LRT-20");
        this.addLookupName("ISLRTorpedo20");
        this.addLookupName("IS LRT 20");
        this.addLookupName("ISLRT20");
        this.heat = 6;
        this.rackSize = 20;
        this.minimumRange = 6;
        this.waterShortRange = 7;
        this.waterMediumRange = 14;
        this.waterLongRange = 21;
        this.waterExtremeRange = 28;
        this.tonnage = 10.0;
        this.criticals = 5;
        this.bv = 181;
        this.cost = 250000;
        rulesRefs = "230, TM";
        techAdvancement.setTechBase(TECH_BASE_IS)
    	.setIntroLevel(false)
    	.setUnofficial(false)
        .setTechRating(RATING_C)
        .setAvailability(RATING_C, RATING_C, RATING_C, RATING_C)
        .setISAdvancement(2370, 2380, 2400, DATE_NONE, DATE_NONE)
        .setISApproximate(false, false, false,false, false)
        .setPrototypeFactions(F_TH)
        .setProductionFactions(F_TH);
    }
}
