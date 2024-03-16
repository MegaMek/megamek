/*
 * MegaMek
 * Copyright (c) 2005 - Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
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
package megamek.common.weapons.unofficial;

import megamek.common.weapons.lrms.LRMWeapon;

/**
 * @author Sebastian Brocks
 */
public class ISLRM1 extends LRMWeapon {
    private static final long serialVersionUID = -5976936994611000430L;

    public ISLRM1() {
        super();
        name = "LRM 1";
        setInternalName(name);
        addLookupName("IS LRM-1");
        addLookupName("ISLRM1");
        addLookupName("IS LRM 1");
        rackSize = 1;
        minimumRange = 6;
        bv = 14;
        rulesRefs = "Unofficial";
        flags = flags.or(F_NO_FIRES).andNot(F_AERO_WEAPON).andNot(F_BA_WEAPON)
                .andNot(F_MECH_WEAPON).andNot(F_TANK_WEAPON);
        techAdvancement.setTechBase(TECH_BASE_IS)
                .setIntroLevel(false)
                .setUnofficial(true)
                .setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_X)
                .setISAdvancement(DATE_NONE, DATE_NONE, 3057, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false);
    }
}
