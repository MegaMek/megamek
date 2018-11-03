/**
 * MegaMek - Copyright (C) 2004,2005 Ben Mazur (bmazur@sev.org)
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
/*
 * Created on Sep 25, 2004
 */
package megamek.common.weapons.autocannons;

import megamek.common.AmmoType;
import megamek.common.SimpleTechLevel;

/**
 * @author Andrew Hunter
 */
public class CLImprovedAC2 extends ACWeapon {
    /**
     *
     */
    private static final long serialVersionUID = 4780847244648362671L;

    /**
     *
     */
    public CLImprovedAC2() {
        super();

        name = "Improved Autocannon/2";
        setInternalName("Improved Autocannon/2");
        addLookupName("CLIMPAC2");
        heat = 1;
        damage = 2;
        rackSize = 2;
        shortRange = 4;
        mediumRange = 8;
        longRange = 16;
        extremeRange = 32;
        tonnage = 5.0;
        criticals = 1;
        bv = 37;
        cost = 75000;
        shortAV = 20;
        maxRange = RANGE_LONG;
        explosionDamage = damage;
        ammoType = AmmoType.T_AC_IMP;
        rulesRefs = "96, IO";
        techAdvancement.setTechBase(TECH_BASE_CLAN).setTechRating(RATING_D)
            .setAvailability(RATING_X, RATING_C, RATING_X, RATING_X)
            .setClanAdvancement(DATE_NONE, 2815, 2818, 2833, 3080)
            .setClanApproximate(false, true, false,false, false)
            .setProductionFactions(F_CLAN).setReintroductionFactions(F_EI)
            .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    }
}
