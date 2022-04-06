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
public class CLStreakSRM4 extends StreakSRMWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = -7630389410195927363L;

    /**
     * 
     */
    public CLStreakSRM4() {

        this.name = "Streak SRM 4";
        this.setInternalName("CLStreakSRM4");
        this.addLookupName("Clan Streak SRM-4");
        this.addLookupName("Clan Streak SRM 4");
        this.heat = 3;
        this.rackSize = 4;
        this.shortRange = 4;
        this.mediumRange = 8;
        this.longRange = 12;
        this.extremeRange = 16;
        this.tonnage = 2.0;
        this.criticals = 1;
        this.bv = 79;
        this.cost = 90000;
        this.shortAV = 8;
        this.medAV = 8;
        this.maxRange = RANGE_MED;
        rulesRefs = "230, TM";
        techAdvancement.setTechBase(TECH_BASE_CLAN)
        	.setIntroLevel(false)
        	.setUnofficial(false)
            .setTechRating(RATING_F)
            .setAvailability(RATING_X, RATING_D, RATING_D, RATING_D)
            .setClanAdvancement(2819, 2822, 2830, DATE_NONE, DATE_NONE)
            .setClanApproximate(false, true, false,false, false)
            .setPrototypeFactions(F_CSA)
            .setProductionFactions(F_CSA);
    }
}
