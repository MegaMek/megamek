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

public class InfantryLaserPistolBrightStarL7 extends InfantryWeapon {

    private static final long serialVersionUID = 1L; // Update for each unique class

    public InfantryLaserPistolBrightStarL7() {
        super();

        name = "Laser Pistol (Brightstar L-7)";
        setInternalName(name);
        addLookupName("BRIGHTSTARL7");
        ammoType = AmmoType.T_INFANTRY;
        cost = 950;
        bv = 0.021;
        tonnage = 0.0011;
        infantryDamage = 0.07;
        infantryRange = 2;
        shots = 1;
        bursts = 1; // Bursts value is now always shown
        flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_LASER).or(F_ENERGY);
        rulesRefs = "Shrapnel #9";
        techAdvancement
        .setTechBase(TECH_BASE_IS)
        .setISAdvancement(DATE_NONE, DATE_NONE, DATE_ES, DATE_NONE, DATE_NONE)
        .setTechRating(RATING_E) // Assuming E-E-E-E simplifies to E
        .setAvailability(new int[]{RATING_E, RATING_E, RATING_E, RATING_E})
        .setISApproximate(false, false, true, false, false)
        .setProductionFactions(F_TH);
    }
}
