/*
 * MegaMek - Copyright (C) 2000-2005 Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2018-2024 - The MegaMek Team. All Rights Reserved.
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

package megamek.common.weapons.infantry;

import megamek.common.AmmoType;


public class InfantryLaserRifleDWSL5S extends InfantryWeapon {

    private static final long serialVersionUID = 1L; // Update for each unique class

    public InfantryLaserRifleDWSL5S() {
        super();

        name = "Laser Rifle (DWS L5S)";
        setInternalName(name);
        addLookupName("DWS L5S");
        ammoType = AmmoType.T_INFANTRY;
        cost = 2000;
        bv = 0.4375;
        tonnage = 0.0065;
        infantryDamage = 0.44;
        infantryRange = 4;
        shots = 5;
        bursts = 1;
        flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_LASER).or(F_ENERGY);
        rulesRefs = "Shrapnel #9";

        techAdvancement
                .setTechBase(TECH_BASE_IS)
                .setTechRating(RATING_C) // Assuming X-C-C-C simplifies to C
                .setAvailability(new int[]{RATING_X, RATING_C, RATING_C, RATING_C})
                .setISAdvancement(DATE_NONE, DATE_NONE, 2800, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, true, false, false)
                .setProductionFactions(F_LC);
    }
}
