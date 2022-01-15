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
public class CLImprovedAC20 extends ACWeapon {
    private static final long serialVersionUID = 49211848611799265L;

    public CLImprovedAC20() {
        super();

        name = "Improved Autocannon/20";
        setInternalName("Improved Autocannon/20");
        addLookupName("CLIMPAC20");
        heat = 7;
        damage = 20;
        rackSize = 20;
        shortRange = 3;
        mediumRange = 6;
        longRange = 9;
        extremeRange = 12;
        tonnage = 13.0;
        criticals = 9;
        bv = 178;
        cost = 300000;
        explosive = true; // when firing incendiary ammo
        shortAV = 20;
        maxRange = RANGE_SHORT;
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
