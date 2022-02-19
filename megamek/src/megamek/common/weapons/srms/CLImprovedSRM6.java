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

import megamek.common.AmmoType;
import megamek.common.SimpleTechLevel;

/**
 * @author Sebastian Brocks
 */
public class CLImprovedSRM6 extends SRMWeapon {
    private static final long serialVersionUID = -2774209761562289905L;

    public CLImprovedSRM6() {
        super();
        name = "Improved SRM 6";
        setInternalName("Improved SRM 6");
        addLookupName("CLImprovedSRM6");
        heat = 4;
        rackSize = 6;
        shortRange = 4;
        mediumRange = 8;
        longRange = 12;
        extremeRange = 16;
        tonnage = 3.0;
        criticals = 2;
        bv = 79;
        cost = 80000;
        shortAV = 9;
        medAV = 9;
        maxRange = RANGE_MED;
        ammoType = AmmoType.T_SRM_IMP;
        rulesRefs = "96, IO";
        flags = flags.andNot(F_PROTO_WEAPON);
        techAdvancement.setTechBase(TECH_BASE_CLAN).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_D, RATING_X, RATING_X)
                .setClanAdvancement(2815, 2817, 2819, 2828, 3080)
                .setClanApproximate(true, false, false, true, false)
                .setPrototypeFactions(F_CCC).setProductionFactions(F_CCC)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    }

    @Override
    public String getSortingName() {
        return "SRM IMP 6";
    }
}
