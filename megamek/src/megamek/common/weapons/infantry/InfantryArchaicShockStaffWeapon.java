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
package megamek.common.weapons.infantry;

import megamek.common.AmmoType;

/**
 * @author Ben Grills
 * @since Sep 7, 2005
 */
public class InfantryArchaicShockStaffWeapon extends InfantryWeapon {
    private static final long serialVersionUID = -3164871600230559641L;

    public InfantryArchaicShockStaffWeapon() {
        super();

        name = "Staff (Shock Staff)";
        setInternalName(name);
        addLookupName("InfantryShockStaff");
        addLookupName("ShockStaff");
        ammoType = AmmoType.T_NA;
        cost = 1500;
        tonnage = 0.003;
        bv = 0.0;
        flags = flags.or(F_NO_FIRES).or(F_INF_POINT_BLANK).or(F_INF_NONPENETRATING);
        infantryDamage = 0.21;
        infantryRange = 0;
        rulesRefs = "195, AToW-C";
        techAdvancement.setTechBase(TECH_BASE_IS).setISAdvancement(3074, 3077, 3130, DATE_NONE, DATE_NONE)
                .setISApproximate(true, true, false, false, false)
                .setClanAdvancement(3074, 3077, 3130, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, true, false, false, false).setPrototypeFactions(F_CC)
                .setProductionFactions(F_CC).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E);
    }
}
