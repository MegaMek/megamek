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

/**
 * @author Jay Lawson
 */
public class SubCapCannonWeaponMedium extends SubCapCannonWeapon {
    /**
     *
     */
    private static final long serialVersionUID = 8756042527483383101L;

    /**
     *
     */
    public SubCapCannonWeaponMedium() {
        super();
        name = "Sub-Capital Cannon (Medium)";
        setInternalName(name);
        addLookupName("MediumSCC");
        this.addLookupName("Medium Sub-Capital Cannon");
        this.shortName = "Medium SCC";
        heat = 30;
        damage = 5;
        rackSize = 5;
        shortRange = 11;
        mediumRange = 22;
        longRange = 33;
        extremeRange = 44;
        tonnage = 500.0f;
        bv = 708;
        cost = 780000;
        shortAV = 5;
        medAV = 5;
        maxRange = RANGE_MED;
        rulesRefs = "343,TO";
        techAdvancement.setTechBase(TECH_BASE_ALL)
        	.setIntroLevel(false)
        	.setUnofficial(false)
            .setTechRating(RATING_E)
            .setAvailability(RATING_X, RATING_X, RATING_F, RATING_D)
            .setISAdvancement(3070, 3073, 3145, DATE_NONE, DATE_NONE)
            .setISApproximate(true, false, false,false, false)
            .setClanAdvancement(DATE_NONE, DATE_NONE, 3091, DATE_NONE, DATE_NONE)
            .setClanApproximate(false, false, false,false, false)
            .setPrototypeFactions(F_WB)
            .setProductionFactions(F_WB);
    }
}
