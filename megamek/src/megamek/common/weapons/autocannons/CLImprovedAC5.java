/*
 * MegaMek - Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */
package megamek.common.weapons.autocannons;

import megamek.common.AmmoType;
import megamek.common.SimpleTechLevel;

/**
 * @author Andrew Hunter
 * @since Sep 25, 2004
 */
public class CLImprovedAC5 extends ACWeapon {
    private static final long serialVersionUID = 8756042527483383101L;

    public CLImprovedAC5() {
        super();
        name = "Improved Autocannon/5";
        setInternalName("Improved Autocannon/5");
        addLookupName("CLIMPAC5");
        sortingName = "Improved Autocannon/05";
        heat = 1;
        damage = 5;
        rackSize = 5;
        minimumRange = 3;
        shortRange = 6;
        mediumRange = 12;
        longRange = 18;
        extremeRange = 24;
        tonnage = 7.0;
        criticals = 2;
        bv = 70;
        cost = 125000;
        shortAV = 5;
        medAV = 5;
        maxRange = RANGE_MED;
        explosionDamage = damage;
        ammoType = AmmoType.T_AC_IMP;
        rulesRefs = "96, IO";
        techAdvancement.setTechBase(TECH_BASE_CLAN).setTechRating(RATING_D)
                .setAvailability(RATING_X, RATING_C, RATING_X, RATING_X)
                .setClanAdvancement(DATE_NONE, 2815, 2818, 2833, 3080)
                .setClanApproximate(false, true, false, false, false)
                .setProductionFactions(F_CLAN).setReintroductionFactions(F_EI)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    }
}
