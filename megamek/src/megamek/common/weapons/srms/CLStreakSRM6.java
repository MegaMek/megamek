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
public class CLStreakSRM6 extends StreakSRMWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = -6416567366717880864L;

    /**
     * 
     */
    public CLStreakSRM6() {

        this.name = "Streak SRM 6";
        this.setInternalName("CLStreakSRM6");
        this.addLookupName("Clan Streak SRM-6");
        this.addLookupName("Clan Streak SRM 6");
        this.heat = 4;
        this.rackSize = 6;
        this.shortRange = 4;
        this.mediumRange = 8;
        this.longRange = 12;
        this.extremeRange = 16;
        this.tonnage = 3.0;
        this.criticals = 2;
        this.bv = 118;
        this.cost = 120000;
        this.shortAV = 12;
        this.medAV = 12;
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
