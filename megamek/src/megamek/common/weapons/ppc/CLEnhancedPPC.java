/**
 * MegaMek - Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
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
 * Created on Sep 13, 2004
 *
 */
package megamek.common.weapons.ppc;

/**
 * @author Harold "BATTLEMASTER" N.
 */
public class CLEnhancedPPC extends PPCWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = 5108976056064542099L;

    /**
     * 
     */
    public CLEnhancedPPC() {
        super();
        this.name = "Enhanced PPC";
        this.setInternalName("CLWERPPC");
        this.addLookupName("Wolverine ER PPC");
        this.addLookupName("CLWERPPC");
        this.addLookupName("Wolverine ER PPC");
        this.addLookupName("ISEHERPPC");
        this.addLookupName("IS EH ER PPC");
        this.heat = 17;
        this.damage = 20;
        this.shortRange = 8;
        this.mediumRange = 16;
        this.longRange = 25;
        this.extremeRange = 32;
        this.waterShortRange = 6;
        this.waterMediumRange = 13;
        this.waterLongRange = 19;
        this.waterExtremeRange = 26;
        this.tonnage = 7.0f;
        this.criticals = 3;
        this.bv = 592;
        this.cost = 600000;
        this.shortAV = 20;
        this.medAV = 20;
        this.longAV = 20;
        this.maxRange = RANGE_LONG;
        rulesRefs = "95,IO";
        techAdvancement.setTechBase(TECH_BASE_CLAN)
        	.setIntroLevel(false)
        	.setUnofficial(false)
            .setTechRating(RATING_F)
            .setAvailability(RATING_X, RATING_E, RATING_X, RATING_X)
            .setClanAdvancement(2822, 2823, DATE_NONE, 2831, 3080)
            .setClanApproximate(true, false, false,false, false)
            .setPrototypeFactions(F_CWV)
            .setProductionFactions(F_CWV)
            .setReintroductionFactions(F_EI);
    }
}
