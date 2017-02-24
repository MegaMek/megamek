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
package megamek.common.weapons;

import megamek.common.TechConstants;
import megamek.common.TechAdvancement;

/**
 * @author Sebastian Brocks
 */
public class ISLRT10 extends LRTWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = 9191385220979030447L;

    /**
     * 
     */
    public ISLRT10() {
        super();

        this.name = "LRT 10";
        this.setInternalName(this.name);
        this.addLookupName("IS LRT-10");
        this.addLookupName("ISLRTorpedo10");
        this.addLookupName("IS LRT 10");
        this.addLookupName("ISLRT10");
        this.heat = 4;
        this.rackSize = 10;
        this.minimumRange = 6;
        this.waterShortRange = 7;
        this.waterMediumRange = 14;
        this.waterLongRange = 21;
        this.waterExtremeRange = 28;
        this.tonnage = 5.0f;
        this.criticals = 2;
        this.bv = 90;
        this.cost = 100000;
        introDate = 2370;
        techLevel.put(2370, TechConstants.T_IS_EXPERIMENTAL);
        techLevel.put(2380, TechConstants.T_IS_ADVANCED);
        techLevel.put(2400, TechConstants.T_IS_TW_NON_BOX);
        availRating = new int[] { RATING_C ,RATING_C ,RATING_C ,RATING_C};
        techRating = RATING_C;
        rulesRefs = "229, TM";
        techAdvancement.setTechBase(TechAdvancement.TECH_BASE_IS);
        techAdvancement.setISAdvancement(2370, 2380, 2400);
        techAdvancement.setTechRating(RATING_C);
        techAdvancement.setAvailability( new int[] { RATING_C, RATING_C, RATING_C, RATING_C });
    }
}
