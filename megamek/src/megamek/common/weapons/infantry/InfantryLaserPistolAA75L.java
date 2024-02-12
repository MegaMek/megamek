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


public class InfantryLaserPistolAA75L extends InfantryWeapon {

    private static final long serialVersionUID = 1L; // Update for each unique class

    public InfantryLaserPistolAA75L() {
        super();

        name = "Laser Pistol (AA-75L)";
        setInternalName(name);
        addLookupName("AA75L");
        ammoType = AmmoType.T_INFANTRY;
        cost = 1500;
        bv = 0.021;
        tonnage = 0.0001;
        flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_LASER).or(F_ENERGY);
        infantryDamage = 0.07;
        infantryRange = 1;
        // ammoWeight is not applicable (NA)
        shots = 1;
        // ammoCost is not applicable (NA)
        bursts = 1;
        rulesRefs = "Shrapnel #9";
        techAdvancement
        .setTechBase(TECH_BASE_IS)
        .setISAdvancement(DATE_NONE, DATE_NONE, DATE_ES, DATE_NONE, DATE_NONE)
        .setTechRating(RATING_C)
        .setAvailability(new int[]{RATING_C, RATING_C, RATING_C, RATING_C})
        .setISApproximate(false, false, true, false, false)
        .setProductionFactions(F_LC);
    }
}
