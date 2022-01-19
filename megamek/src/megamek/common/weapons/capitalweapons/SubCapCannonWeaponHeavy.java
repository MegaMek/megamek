/*
 * MegaMek - Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
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
package megamek.common.weapons.capitalweapons;

import megamek.common.SimpleTechLevel;

/**
 * @author Jay Lawson
 * @since Sep 25, 2004
 */
public class SubCapCannonWeaponHeavy extends SubCapCannonWeapon {
    private static final long serialVersionUID = 8756042527483383101L;

    public SubCapCannonWeaponHeavy() {
        super();
        name = "Sub-Capital Cannon (Heavy)";
        setInternalName(this.name);
        addLookupName("HeavySCC");
        addLookupName("Heavy Sub-Capital Cannon");
        shortName = "Heavy SCC";
        sortingName = "Sub-Capital Cannon D";
        heat = 42;
        damage = 7;
        rackSize = 7;
        shortRange = 11;
        mediumRange = 22;
        longRange = 33;
        extremeRange = 44;
        tonnage = 700.0;
        bv = 1901;
        cost = 1300000;
        shortAV = 7;
        medAV = 7;
        maxRange = RANGE_MED;
        rulesRefs = "343, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        techAdvancement.setTechBase(TECH_BASE_ALL)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_D)
                .setISAdvancement(DATE_NONE, 3068, 3073, DATE_NONE, DATE_NONE)
                .setISApproximate(false, true, false, false, false)
                .setClanAdvancement(DATE_NONE, 3090, 3091, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, true, false, false, false)
                .setPrototypeFactions(F_WB)
                .setProductionFactions(F_WB)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
    }
}
