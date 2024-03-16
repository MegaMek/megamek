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


public class InfantryLaserPistolKelvin000Lancer3MM extends InfantryWeapon {

    private static final long serialVersionUID = 1L; // Update for each unique class

    public InfantryLaserPistolKelvin000Lancer3MM() {
        super();

        name = "Laser Pistol (Kelvin 000 Lancer 3-MM)";
        setInternalName(name);
        addLookupName("LANCER3MM");
        ammoType = AmmoType.T_INFANTRY;
        cost = 1250;
        bv = 0.01575;
        tonnage = 0.0016;
        infantryDamage = 0.05;
        infantryRange = 1;
        shots = 1;
        bursts = 1;
        flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_LASER).or(F_ENERGY);
        rulesRefs = "Shrapnel #9";

        techAdvancement
                .setTechBase(TECH_BASE_IS)
                .setTechRating(RATING_D)
                .setAvailability(new int[]{RATING_D, RATING_C, RATING_C, RATING_C})
                .setISAdvancement(DATE_NONE, DATE_NONE, DATE_ES, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, true, false, false)
                .setProductionFactions(F_FS);
    }
}
