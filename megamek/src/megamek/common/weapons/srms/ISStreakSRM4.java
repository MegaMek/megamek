/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.common.weapons.srms;

/**
 * @author Sebastian Brocks
 */
public class ISStreakSRM4 extends StreakSRMWeapon {
    private static final long serialVersionUID = 2884956805670848621L;

    public ISStreakSRM4() {
        super();

        this.name = "Streak SRM 4";
        this.setInternalName("ISStreakSRM4");
        this.addLookupName("IS Streak SRM-4");
        this.addLookupName("IS Streak SRM 4");
        this.heat = 3;
        this.rackSize = 4;
        this.shortRange = 3;
        this.mediumRange = 6;
        this.longRange = 9;
        this.extremeRange = 12;
        this.tonnage = 3.0;
        this.criticals = 1;
        this.bv = 59;
        this.cost = 90000;
        this.shortAV = 8;
        this.maxRange = RANGE_SHORT;
        rulesRefs = "230, TM";
        techAdvancement.setTechBase(TECH_BASE_IS)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3055, 3058, 3060, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, true, false, false)
                .setPrototypeFactions(F_DC)
                .setProductionFactions(F_DC);
    }
}
