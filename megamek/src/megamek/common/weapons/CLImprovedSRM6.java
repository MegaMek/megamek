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

import megamek.common.AmmoType;
import megamek.common.TechConstants;
import megamek.common.TechAdvancement;

/**
 * @author Sebastian Brocks
 */
public class CLImprovedSRM6 extends SRMWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = -2774209761562289905L;

    /**
     * 
     */
    public CLImprovedSRM6() {
        super();

        this.name = "Improved SRM 6";
        this.setInternalName("Improved SRM 6");
        this.addLookupName("CLImprovedSRM6");
        this.heat = 4;
        this.rackSize = 6;
        shortRange = 4;
        mediumRange = 8;
        longRange = 12;
        extremeRange = 16;
        this.tonnage = 3.0f;
        this.criticals = 2;
        this.bv = 79;
        this.cost = 80000;
        this.shortAV = 8;
        this.maxRange = RANGE_SHORT;
        ammoType = AmmoType.T_SRM_IMP;
        introDate = 2815;
        extinctDate = 2828;
    	reintroDate =  3080;
    	techLevel.put(2815, TechConstants.T_IS_EXPERIMENTAL);   ///EXP
        techLevel.put(2817, TechConstants.T_IS_ADVANCED);   ///ADV
        techLevel.put(2819, TechConstants.T_ALLOWED_ALL);   ///COMMON
        availRating = new int[] { RATING_X, RATING_D, RATING_X, RATING_X };
        techRating = RATING_D;
        rulesRefs = "96, IO";
        techAdvancement.setTechBase(TechAdvancement.TECH_BASE_ALL);
        techAdvancement.setAdvancement(2815, 2817, 2819, 2828, 3080);
        techAdvancement.setTechRating(RATING_D);
        techAdvancement.setAvailability( new int[] { RATING_X, RATING_D, RATING_X, RATING_X });
    }
}
