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
 * @author Dave Nawton
 * @since Sep 7, 2005
 */
public class InfantryProstheticClimbingClawsWeapon extends InfantryWeapon {
    private static final long serialVersionUID = -3164871600230559641L;

    public InfantryProstheticClimbingClawsWeapon() {
        super();

        name = "Prosthetic Climbing Claws";
        setInternalName(name);
        addLookupName("ClimbingClawsProsthetic");
        ammoType = AmmoType.T_NA;
        cost = 1000;
        bv = 0.02;
        flags = flags.or(F_NO_FIRES).or(F_INF_POINT_BLANK).or(F_INF_ARCHAIC).or(F_INF_CLIMBINGCLAWS);
        // TODO Add game rules IO pg 84
        infantryDamage = 0.02;
        infantryRange = 0;
        rulesRefs = "84, IO";
        techAdvancement.setTechBase(TECH_BASE_ALL)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_E)
                .setAvailability(RATING_F, RATING_E, RATING_D, RATING_D)
                .setISAdvancement(DATE_ES, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_ES, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
    }
}
