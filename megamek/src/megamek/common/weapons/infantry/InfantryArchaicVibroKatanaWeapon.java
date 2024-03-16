/**
 * MegaMek - Copyright (C) 2004,2005 Ben Mazur (bmazur@sev.org)
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
 * Created on Sep 7, 2005
 *
 */
package megamek.common.weapons.infantry;

import megamek.common.AmmoType;

/**
 * @author Ben Grills
 */
public class InfantryArchaicVibroKatanaWeapon extends InfantryWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -3164871600230559641L;

    public InfantryArchaicVibroKatanaWeapon() {
        super();

        name = "Blade (Vibro-katana)";
        setInternalName(name);
        addLookupName("InfantryVibroKatana");
        addLookupName("Vibro Katana");
        ammoType = AmmoType.T_NA;
        cost = 350;
        bv = 0.29;
        tonnage = .003; 
        flags = flags.or(F_NO_FIRES).or(F_INF_POINT_BLANK).or(F_INF_ARCHAIC);
        infantryDamage = 0.32;
        infantryRange = 0;
        rulesRefs = "272, TM";
        techAdvancement.setTechBase(TECH_BASE_ALL).setISAdvancement(2440, 2450, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setClanAdvancement(2440, 2450, DATE_NONE, 2850, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_DC)
                .setProductionFactions(F_DC).setTechRating(RATING_E)
                .setAvailability(RATING_E, RATING_E, RATING_D, RATING_C);

    }
}
